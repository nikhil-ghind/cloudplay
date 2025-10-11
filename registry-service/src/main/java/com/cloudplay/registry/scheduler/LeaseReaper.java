package com.cloudplay.registry.scheduler;

import com.cloudplay.common.model.GpuNode;
import com.cloudplay.common.model.NodeState;
import com.cloudplay.registry.config.RegistryProperties;
import com.cloudplay.registry.service.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Periodically scans the registry for nodes whose heartbeat lease has expired
 * and transitions them: READY/CORDONED -> UNHEALTHY, then UNHEALTHY ->
 * DECOMMISSIONED (removed) once the decommission grace period elapses.
 *
 * <p>This is the liveness mechanism that keeps the distributed registry honest
 * even when a node crashes without deregistering.</p>
 */
@Component
public class LeaseReaper {

    private static final Logger log = LoggerFactory.getLogger(LeaseReaper.class);

    private final RegistryService registry;
    private final RegistryProperties props;

    public LeaseReaper(RegistryService registry, RegistryProperties props) {
        this.registry = registry;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${cloudplay.registry.reaper-interval-millis:5000}")
    public void reap() {
        Instant now = Instant.now();
        int markedUnhealthy = 0;
        int decommissioned = 0;

        for (GpuNode node : registry.allNodes()) {
            Instant lease = node.getLeaseExpiresAt();
            if (lease == null) {
                continue;
            }
            if (node.getState() != NodeState.UNHEALTHY
                    && node.getState() != NodeState.DECOMMISSIONED
                    && now.isAfter(lease)) {
                node.setState(NodeState.UNHEALTHY);
                registry.persist(node);
                markedUnhealthy++;
                log.warn("Node {} lease expired (last heartbeat {}); marked UNHEALTHY",
                        node.getNodeId(), node.getLastHeartbeat());
            } else if (node.getState() == NodeState.UNHEALTHY
                    && node.getLastHeartbeat() != null
                    && now.isAfter(node.getLastHeartbeat().plus(props.getDecommissionGrace()))) {
                registry.deregister(node.getNodeId());
                decommissioned++;
                log.warn("Node {} exceeded decommission grace; removed from pool", node.getNodeId());
            }
        }

        if (markedUnhealthy > 0 || decommissioned > 0) {
            log.info("Lease reaper pass: {} marked UNHEALTHY, {} decommissioned",
                    markedUnhealthy, decommissioned);
        }
    }
}
