package com.cloudplay.registry.store;

import com.cloudplay.common.error.CommonExceptions;
import com.cloudplay.common.model.GpuNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Distributed registry store backed by Redis. Node records are stored as JSON in
 * a hash ({@code cloudplay:nodes}). Slot reservation is performed with a Lua
 * script so the read-check-write is atomic across all registry replicas — this
 * is what lets the registry run as a horizontally-scaled, shared-state service
 * (etcd-style leases without etcd).
 *
 * <p>Active only under the {@code redis} Spring profile.</p>
 */
@Component
@Profile("redis")
public class RedisRegistryStore implements RegistryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRegistryStore.class);

    private static final String NODES_HASH = "cloudplay:nodes";
    private static final String SESSIONS_KEY_PREFIX = "cloudplay:node-sessions:";

    /**
     * Atomic reserve: parse the node JSON, verify READY + capacity, bump
     * usedSlots, record the session in a set, and write back. Returns the new
     * JSON, "NOTFOUND", "CONFLICT", or "DUP" (already reserved by this session).
     */
    private static final String RESERVE_SCRIPT =
            "local nodeJson = redis.call('HGET', KEYS[1], ARGV[1]) " +
            "if not nodeJson then return 'NOTFOUND' end " +
            "if redis.call('SISMEMBER', KEYS[2], ARGV[2]) == 1 then return 'DUP' end " +
            "local used = tonumber(string.match(nodeJson, '\"usedSlots\"%s*:%s*(%d+)')) " +
            "local total = tonumber(string.match(nodeJson, '\"totalSlots\"%s*:%s*(%d+)')) " +
            "local state = string.match(nodeJson, '\"state\"%s*:%s*\"(%w+)\"') " +
            "if state ~= 'READY' or used >= total then return 'CONFLICT' end " +
            "local newJson = string.gsub(nodeJson, '(\"usedSlots\"%s*:%s*)%d+', '%1' .. (used + 1)) " +
            "redis.call('HSET', KEYS[1], ARGV[1], newJson) " +
            "redis.call('SADD', KEYS[2], ARGV[2]) " +
            "return newJson";

    private static final String RELEASE_SCRIPT =
            "local nodeJson = redis.call('HGET', KEYS[1], ARGV[1]) " +
            "if not nodeJson then return 'NOTFOUND' end " +
            "if redis.call('SREM', KEYS[2], ARGV[2]) == 0 then return nodeJson end " +
            "local used = tonumber(string.match(nodeJson, '\"usedSlots\"%s*:%s*(%d+)')) " +
            "local newUsed = math.max(0, used - 1) " +
            "local newJson = string.gsub(nodeJson, '(\"usedSlots\"%s*:%s*)%d+', '%1' .. newUsed) " +
            "redis.call('HSET', KEYS[1], ARGV[1], newJson) " +
            "return newJson";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<String> reserveScript;
    private final DefaultRedisScript<String> releaseScript;

    public RedisRegistryStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.reserveScript = new DefaultRedisScript<>(RESERVE_SCRIPT, String.class);
        this.releaseScript = new DefaultRedisScript<>(RELEASE_SCRIPT, String.class);
        log.info("RedisRegistryStore active (distributed mode)");
    }

    private String toJson(GpuNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialise node " + node.getNodeId(), e);
        }
    }

    private GpuNode fromJson(String json) {
        try {
            return objectMapper.readValue(json, GpuNode.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialise node record", e);
        }
    }

    private String sessionsKey(String nodeId) {
        return SESSIONS_KEY_PREFIX + nodeId;
    }

    @Override
    public GpuNode upsertNode(GpuNode node) {
        redis.<String, String>opsForHash().put(NODES_HASH, node.getNodeId(), toJson(node));
        return node;
    }

    @Override
    public Optional<GpuNode> findNode(String nodeId) {
        Object raw = redis.opsForHash().get(NODES_HASH, nodeId);
        return raw == null ? Optional.empty() : Optional.of(fromJson(raw.toString()));
    }

    @Override
    public List<GpuNode> listNodes() {
        List<GpuNode> result = new ArrayList<>();
        for (Object v : redis.opsForHash().values(NODES_HASH)) {
            result.add(fromJson(v.toString()));
        }
        return result;
    }

    @Override
    public void deleteNode(String nodeId) {
        redis.opsForHash().delete(NODES_HASH, nodeId);
        redis.delete(sessionsKey(nodeId));
    }

    @Override
    public GpuNode reserveSlot(String nodeId, String sessionId) {
        String result = redis.execute(reserveScript,
                List.of(NODES_HASH, sessionsKey(nodeId)), nodeId, sessionId);
        return switch (result) {
            case "NOTFOUND" -> throw new CommonExceptions.NodeNotFoundException(nodeId);
            case "CONFLICT" -> throw new CommonExceptions.SlotConflictException(nodeId);
            case "DUP" -> findNode(nodeId).orElseThrow(() -> new CommonExceptions.NodeNotFoundException(nodeId));
            default -> fromJson(result);
        };
    }

    @Override
    public GpuNode releaseSlot(String nodeId, String sessionId) {
        String result = redis.execute(releaseScript,
                List.of(NODES_HASH, sessionsKey(nodeId)), nodeId, sessionId);
        if ("NOTFOUND".equals(result)) {
            throw new CommonExceptions.NodeNotFoundException(nodeId);
        }
        return fromJson(result);
    }

    /** Exposed for the lease reaper to scrub orphaned session sets. */
    public Set<String> sessionsOnNode(String nodeId) {
        Set<String> members = redis.opsForSet().members(sessionsKey(nodeId));
        return members == null ? Set.of() : members;
    }
}
