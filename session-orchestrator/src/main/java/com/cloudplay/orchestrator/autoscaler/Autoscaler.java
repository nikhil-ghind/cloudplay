package com.cloudplay.orchestrator.autoscaler;

import com.cloudplay.common.model.GpuClass;
import com.cloudplay.common.model.GpuNode;
import com.cloudplay.common.model.NodeState;
import com.cloudplay.common.registry.RegistryClient;
import com.cloudplay.orchestrator.config.OrchestratorProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Demand-driven autoscaler. On a fixed interval it pulls the live node inventory
 * from the registry, groups nodes into (region, gpuClass) pools, and computes a
 * <b>desired node count</b> for each pool from session demand and a target
 * utilization.
 *
 * <h3>Sizing formula</h3>
 * For a pool with {@code usedSlots} occupied slots, target utilization {@code U},
 * {@code slotsPerNode} {@code S}, and a warm buffer {@code B} slots:
 * <pre>
 *   requiredSlots = ceil(usedSlots / U) + B
 *   desiredNodes  = clamp( ceil(requiredSlots / S), minNodes, maxNodes )
 * </pre>
 * Scale-down is rate-limited by {@code maxScaleDownStep} to avoid thrashing and
 * to give in-flight sessions time to drain.
 *
 * <p>The decisions are published as Micrometer gauges
 * ({@code cloudplay_autoscaler_desired_nodes}, {@code ..._current_nodes},
 * {@code ..._utilization}) per pool, so the platform's HPA / KEDA / cluster
 * autoscaler can act on them as a custom external metric. The orchestrator does
 * not provision VMs itself — it expresses intent.</p>
 */
@Component
public class Autoscaler {

    private static final Logger log = LoggerFactory.getLogger(Autoscaler.class);

    private final RegistryClient registry;
    private final OrchestratorProperties props;
    private final MeterRegistry meters;

    /** Latest decision per pool, exposed via the controller and gauges. */
    private final Map<String, ScaleDecision> latest = new ConcurrentHashMap<>();
    /** Gauge backing fields, kept stable so Micrometer holds references. */
    private final Map<String, AtomicInteger> desiredGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> currentGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<Double>> utilGauges = new ConcurrentHashMap<>();

    public Autoscaler(RegistryClient registry, OrchestratorProperties props, MeterRegistry meters) {
        this.registry = registry;
        this.props = props;
        this.meters = meters;
    }

    @Scheduled(fixedDelayString = "${cloudplay.orchestrator.autoscaler.evaluation-interval-millis:15000}")
    public void evaluate() {
        if (!props.getAutoscaler().isEnabled()) {
            return;
        }
        List<GpuNode> nodes = registry.listAllNodes().collectList().block();
        if (nodes == null) {
            log.warn("Autoscaler could not read node inventory from registry; skipping pass");
            return;
        }
        Map<String, ScaleDecision> decisions = computeDecisions(nodes);
        decisions.forEach((key, decision) -> {
            latest.put(key, decision);
            publishGauges(decision);
            if (decision.delta() != 0) {
                log.info("Autoscaler {} pool {}: {} -> {} nodes (util={}%, used={}/{})",
                        decision.direction(), key, decision.currentNodes(), decision.desiredNodes(),
                        Math.round(decision.utilization() * 100), decision.usedSlots(), decision.totalSlots());
            }
        });
    }

    /**
     * Pure function (testable) that turns a node inventory into per-pool scale
     * decisions.
     */
    public Map<String, ScaleDecision> computeDecisions(List<GpuNode> nodes) {
        OrchestratorProperties.Autoscaler cfg = props.getAutoscaler();

        // Group live nodes into pools.
        Map<String, PoolAccumulator> pools = new HashMap<>();
        for (GpuNode n : nodes) {
            if (n.getState() == NodeState.DECOMMISSIONED) {
                continue;
            }
            String key = poolKey(n.getRegion(), n.getGpuClass());
            PoolAccumulator acc = pools.computeIfAbsent(key,
                    k -> new PoolAccumulator(n.getRegion(), n.getGpuClass()));
            acc.nodes++;
            acc.usedSlots += n.getUsedSlots();
            acc.totalSlots += n.getTotalSlots();
        }

        Map<String, ScaleDecision> out = new HashMap<>();
        for (Map.Entry<String, PoolAccumulator> e : pools.entrySet()) {
            PoolAccumulator p = e.getValue();

            int requiredSlots = (int) Math.ceil(p.usedSlots / Math.max(0.01, cfg.getTargetUtilization()))
                    + cfg.getWarmSlotBuffer();
            int rawDesired = (int) Math.ceil((double) requiredSlots / Math.max(1, cfg.getSlotsPerNode()));
            int desired = clamp(rawDesired, cfg.getMinNodes(), cfg.getMaxNodes());

            // Rate-limit scale-down to avoid thrash.
            if (desired < p.nodes) {
                desired = Math.max(desired, p.nodes - cfg.getMaxScaleDownStep());
            }

            double util = p.totalSlots == 0 ? 0.0 : (double) p.usedSlots / (double) p.totalSlots;
            out.put(e.getKey(), new ScaleDecision(
                    p.region, p.gpuClass, p.nodes, desired, p.usedSlots, p.totalSlots, util));
        }
        return out;
    }

    public Map<String, ScaleDecision> latestDecisions() {
        return Map.copyOf(latest);
    }

    private void publishGauges(ScaleDecision d) {
        String key = d.poolKey();
        desiredGauges.computeIfAbsent(key, k -> registerGauge(
                "cloudplay.autoscaler.desired.nodes", d, new AtomicInteger())).set(d.desiredNodes());
        currentGauges.computeIfAbsent(key, k -> registerGauge(
                "cloudplay.autoscaler.current.nodes", d, new AtomicInteger())).set(d.currentNodes());
        utilGauges.computeIfAbsent(key, k -> {
            AtomicReference<Double> ref = new AtomicReference<>(0.0);
            meters.gauge("cloudplay.autoscaler.utilization",
                    List.of(io.micrometer.core.instrument.Tag.of("region", d.region()),
                            io.micrometer.core.instrument.Tag.of("gpuClass", d.gpuClass().name())),
                    ref, r -> r.get());
            return ref;
        }).set(d.utilization());
    }

    private AtomicInteger registerGauge(String name, ScaleDecision d, AtomicInteger holder) {
        meters.gauge(name,
                List.of(io.micrometer.core.instrument.Tag.of("region", d.region()),
                        io.micrometer.core.instrument.Tag.of("gpuClass", d.gpuClass().name())),
                holder, AtomicInteger::get);
        return holder;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String poolKey(String region, GpuClass gpuClass) {
        return region + "/" + gpuClass;
    }

    private static final class PoolAccumulator {
        final String region;
        final GpuClass gpuClass;
        int nodes;
        int usedSlots;
        int totalSlots;

        PoolAccumulator(String region, GpuClass gpuClass) {
            this.region = region;
            this.gpuClass = gpuClass;
        }
    }
}
