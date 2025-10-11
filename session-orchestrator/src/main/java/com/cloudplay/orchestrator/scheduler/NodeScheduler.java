package com.cloudplay.orchestrator.scheduler;

import com.cloudplay.common.dto.SessionRequest;
import com.cloudplay.common.model.GpuNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the best GPU node for a session from a candidate list returned by the
 * registry.
 *
 * <p><b>Algorithm — best-fit bin packing with affinity:</b></p>
 * <ol>
 *   <li>Filter to nodes in the requested region with the requested GPU class
 *       that are READY and have at least one free slot.</li>
 *   <li>Score each candidate and pick the highest score. The score favours:
 *       <ul>
 *         <li><b>Best-fit packing</b> — prefer the node that will be <em>most</em>
 *             utilized after placement, so we consolidate load and free whole
 *             nodes for the autoscaler to reclaim (the inverse of spreading).</li>
 *         <li><b>Capacity safety</b> — never exceed slots (guaranteed by the
 *             registry's atomic reserve, this is just a pre-filter).</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>The actual reservation is done atomically against the registry afterwards;
 * if the chosen node lost a race, the allocator retries with the next best.</p>
 */
@Component
public class NodeScheduler {

    private static final Logger log = LoggerFactory.getLogger(NodeScheduler.class);

    /**
     * Rank candidate nodes best-first for the given request.
     *
     * @param candidates schedulable nodes already filtered by region/gpuClass
     * @return ordered list, best node first
     */
    public List<GpuNode> rank(SessionRequest request, List<GpuNode> candidates) {
        List<GpuNode> ranked = candidates.stream()
                .filter(GpuNode::hasCapacity)
                .filter(n -> n.getGpuClass() == request.getGpuClass())
                .filter(n -> n.getRegion().equalsIgnoreCase(request.getRegion()))
                .sorted(Comparator.comparingDouble(this::placementScore).reversed())
                .toList();
        log.debug("Ranked {} candidate node(s) for request region={} gpuClass={}",
                ranked.size(), request.getRegion(), request.getGpuClass());
        return ranked;
    }

    public Optional<GpuNode> pick(SessionRequest request, List<GpuNode> candidates) {
        return rank(request, candidates).stream().findFirst();
    }

    /**
     * Best-fit score: the projected utilization of the node after one more
     * session lands on it. Higher = tighter pack. We clamp so a node about to be
     * full doesn't beat one that would land exactly on a clean boundary.
     */
    private double placementScore(GpuNode node) {
        int projectedUsed = node.getUsedSlots() + 1;
        double projectedUtil = (double) projectedUsed / (double) Math.max(1, node.getTotalSlots());
        // Tie-break by absolute free slots remaining (fewer = tighter pack).
        double tightness = 1.0 / (1.0 + node.availableSlots());
        return projectedUtil + 0.001 * tightness;
    }
}
