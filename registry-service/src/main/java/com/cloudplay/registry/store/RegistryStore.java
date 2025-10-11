package com.cloudplay.registry.store;

import com.cloudplay.common.model.GpuNode;

import java.util.List;
import java.util.Optional;

/**
 * Pluggable persistence abstraction for the registry. Implementations must
 * provide <em>atomic</em> slot reservation/release so that concurrent
 * allocations cannot oversubscribe a node (compare-and-set semantics).
 *
 * <p>Two implementations are provided:
 * <ul>
 *   <li>{@link InMemoryRegistryStore} — single-instance, lock-based (default).</li>
 *   <li>{@link RedisRegistryStore} — distributed, backed by Redis with a Lua
 *       script for the atomic reserve operation (the {@code redis} profile).</li>
 * </ul>
 * This mirrors an etcd-style lease registry while staying dependency-light.</p>
 */
public interface RegistryStore {

    /** Insert or overwrite a node record. */
    GpuNode upsertNode(GpuNode node);

    Optional<GpuNode> findNode(String nodeId);

    List<GpuNode> listNodes();

    void deleteNode(String nodeId);

    /**
     * Atomically reserve one capacity slot on {@code nodeId} for {@code sessionId}.
     *
     * @return the updated node if a slot was reserved
     * @throws com.cloudplay.common.error.CommonExceptions.SlotConflictException
     *         if no slot was free (lost the CAS race / fully booked)
     * @throws com.cloudplay.common.error.CommonExceptions.NodeNotFoundException
     *         if the node does not exist
     */
    GpuNode reserveSlot(String nodeId, String sessionId);

    /** Atomically release a previously reserved slot. Idempotent. */
    GpuNode releaseSlot(String nodeId, String sessionId);
}
