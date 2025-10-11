package com.cloudplay.registry.store;

import com.cloudplay.common.error.CommonExceptions;
import com.cloudplay.common.model.GpuNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default single-instance registry store. Capacity mutations are guarded by a
 * per-node lock so reserve/release are atomic within the JVM. Used when the
 * {@code redis} profile is NOT active.
 */
@Component
@Profile("!redis")
@ConditionalOnMissingBean(RedisRegistryStore.class)
public class InMemoryRegistryStore implements RegistryStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRegistryStore.class);

    private final ConcurrentHashMap<String, GpuNode> nodes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> nodeLocks = new ConcurrentHashMap<>();
    /** Tracks which sessions hold a slot on each node, to keep release idempotent. */
    private final ConcurrentHashMap<String, Set<String>> nodeSessions = new ConcurrentHashMap<>();

    private ReentrantLock lockFor(String nodeId) {
        return nodeLocks.computeIfAbsent(nodeId, k -> new ReentrantLock());
    }

    @Override
    public GpuNode upsertNode(GpuNode node) {
        nodes.put(node.getNodeId(), node);
        nodeSessions.computeIfAbsent(node.getNodeId(), k -> ConcurrentHashMap.newKeySet());
        return node;
    }

    @Override
    public Optional<GpuNode> findNode(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    @Override
    public List<GpuNode> listNodes() {
        return new ArrayList<>(nodes.values());
    }

    @Override
    public void deleteNode(String nodeId) {
        nodes.remove(nodeId);
        nodeSessions.remove(nodeId);
        nodeLocks.remove(nodeId);
    }

    @Override
    public GpuNode reserveSlot(String nodeId, String sessionId) {
        ReentrantLock lock = lockFor(nodeId);
        lock.lock();
        try {
            GpuNode node = nodes.get(nodeId);
            if (node == null) {
                throw new CommonExceptions.NodeNotFoundException(nodeId);
            }
            Set<String> sessions = nodeSessions.computeIfAbsent(nodeId, k -> ConcurrentHashMap.newKeySet());
            if (sessions.contains(sessionId)) {
                // Idempotent re-reservation by the same session.
                return node;
            }
            if (!node.hasCapacity()) {
                throw new CommonExceptions.SlotConflictException(nodeId);
            }
            node.setUsedSlots(node.getUsedSlots() + 1);
            sessions.add(sessionId);
            log.debug("Reserved slot on {} for {} -> {}/{}",
                    nodeId, sessionId, node.getUsedSlots(), node.getTotalSlots());
            return node;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public GpuNode releaseSlot(String nodeId, String sessionId) {
        ReentrantLock lock = lockFor(nodeId);
        lock.lock();
        try {
            GpuNode node = nodes.get(nodeId);
            if (node == null) {
                throw new CommonExceptions.NodeNotFoundException(nodeId);
            }
            Set<String> sessions = nodeSessions.computeIfAbsent(nodeId, k -> ConcurrentHashMap.newKeySet());
            if (sessions.remove(sessionId)) {
                node.setUsedSlots(Math.max(0, node.getUsedSlots() - 1));
                log.debug("Released slot on {} for {} -> {}/{}",
                        nodeId, sessionId, node.getUsedSlots(), node.getTotalSlots());
            }
            return node;
        } finally {
            lock.unlock();
        }
    }
}
