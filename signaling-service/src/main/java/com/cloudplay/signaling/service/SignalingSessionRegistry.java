package com.cloudplay.signaling.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks the live signaling sessions for this signaling-service instance. Maps a
 * CloudPlay sessionId to its {@link SignalingSession} (player + streamer pair).
 *
 * <p>For multi-replica deployments, sticky routing on sessionId (configured at
 * the gateway / ingress) keeps both peers of a session on the same instance, so
 * a local map is sufficient and avoids cross-node relay.</p>
 */
@Component
public class SignalingSessionRegistry {

    private final ConcurrentMap<String, SignalingSession> sessions = new ConcurrentHashMap<>();

    public SignalingSessionRegistry(MeterRegistry meters) {
        meters.gauge("cloudplay.signaling.active.sessions", sessions, ConcurrentMap::size);
    }

    public SignalingSession getOrCreate(String sessionId) {
        return sessions.computeIfAbsent(sessionId, SignalingSession::new);
    }

    public SignalingSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    public int size() {
        return sessions.size();
    }
}
