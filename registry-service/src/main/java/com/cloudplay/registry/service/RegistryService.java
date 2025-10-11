package com.cloudplay.registry.service;

import com.cloudplay.common.dto.HeartbeatRequest;
import com.cloudplay.common.dto.LeaseResponse;
import com.cloudplay.common.dto.NodeRegistration;
import com.cloudplay.common.error.CommonExceptions;
import com.cloudplay.common.model.GpuClass;
import com.cloudplay.common.model.GpuNode;
import com.cloudplay.common.model.NodeState;
import com.cloudplay.registry.config.RegistryProperties;
import com.cloudplay.registry.store.RegistryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Core registry logic: node registration, lease renewal via heartbeats, slot
 * reservation/release, and capacity queries. Delegates persistence to the
 * pluggable {@link RegistryStore}.
 */
@Service
public class RegistryService {

    private static final Logger log = LoggerFactory.getLogger(RegistryService.class);

    private final RegistryStore store;
    private final RegistryProperties props;

    public RegistryService(RegistryStore store, RegistryProperties props) {
        this.store = store;
        this.props = props;
    }

    /** Register (or re-register) a node and grant an initial lease. */
    public LeaseResponse register(NodeRegistration reg) {
        Instant now = Instant.now();
        GpuNode node = store.findNode(reg.getNodeId()).orElseGet(GpuNode::new);
        node.setNodeId(reg.getNodeId());
        node.setRegion(reg.getRegion());
        node.setGpuClass(reg.getGpuClass());
        node.setTotalSlots(reg.getTotalSlots());
        node.setStreamerHost(reg.getStreamerHost());
        node.setStreamerPort(reg.getStreamerPort());
        node.setState(NodeState.READY);
        if (node.getRegisteredAt() == null) {
            node.setRegisteredAt(now);
            node.setUsedSlots(0);
        }
        node.setLastHeartbeat(now);
        node.setLeaseExpiresAt(now.plus(props.getLeaseTtl()));
        store.upsertNode(node);
        log.info("Registered node {} region={} gpuClass={} slots={}",
                node.getNodeId(), node.getRegion(), node.getGpuClass(), node.getTotalSlots());
        return leaseResponse(node);
    }

    /** Renew a node's lease and update its reported load. */
    public LeaseResponse heartbeat(String nodeId, HeartbeatRequest hb) {
        GpuNode node = requireNode(nodeId);
        Instant now = Instant.now();
        node.setLastHeartbeat(now);
        node.setLeaseExpiresAt(now.plus(props.getLeaseTtl()));
        // Reconcile reported load, but never below what the registry has reserved
        // for in-flight allocations it knows about.
        node.setUsedSlots(Math.min(node.getTotalSlots(), Math.max(0, hb.getUsedSlots())));
        if (node.getState() == NodeState.UNHEALTHY) {
            // Node recovered before decommission grace elapsed.
            node.setState(NodeState.READY);
            log.info("Node {} recovered from UNHEALTHY via heartbeat", nodeId);
        }
        store.upsertNode(node);
        return leaseResponse(node);
    }

    public GpuNode reserveSlot(String nodeId, String sessionId) {
        return store.reserveSlot(nodeId, sessionId);
    }

    public GpuNode releaseSlot(String nodeId, String sessionId) {
        return store.releaseSlot(nodeId, sessionId);
    }

    public GpuNode getNode(String nodeId) {
        return requireNode(nodeId);
    }

    public void deregister(String nodeId) {
        requireNode(nodeId);
        store.deleteNode(nodeId);
        log.info("Deregistered node {}", nodeId);
    }

    /** Cordon a node: keep existing sessions, refuse new allocations. */
    public GpuNode cordon(String nodeId) {
        GpuNode node = requireNode(nodeId);
        node.setState(NodeState.CORDONED);
        store.upsertNode(node);
        return node;
    }

    /**
     * List nodes, optionally filtered by region/gpuClass and whether they are
     * currently schedulable (READY with free capacity).
     */
    public List<GpuNode> listNodes(String region, GpuClass gpuClass, boolean schedulableOnly) {
        return store.listNodes().stream()
                .filter(n -> region == null || region.equalsIgnoreCase(n.getRegion()))
                .filter(n -> gpuClass == null || gpuClass == n.getGpuClass())
                .filter(n -> !schedulableOnly || n.hasCapacity())
                .sorted(Comparator.comparingInt(GpuNode::availableSlots).reversed())
                .toList();
    }

    private GpuNode requireNode(String nodeId) {
        return store.findNode(nodeId)
                .orElseThrow(() -> new CommonExceptions.NodeNotFoundException(nodeId));
    }

    private LeaseResponse leaseResponse(GpuNode node) {
        return new LeaseResponse(node.getNodeId(), node.getLeaseExpiresAt(),
                props.getHeartbeatInterval().toSeconds());
    }

    /** Used by the reaper. */
    public Optional<GpuNode> findNode(String nodeId) {
        return store.findNode(nodeId);
    }

    public List<GpuNode> allNodes() {
        return store.listNodes();
    }

    public void persist(GpuNode node) {
        store.upsertNode(node);
    }
}
