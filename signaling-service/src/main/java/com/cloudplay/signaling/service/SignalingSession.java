package com.cloudplay.signaling.service;

import com.cloudplay.signaling.model.PeerRole;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Holds the two peers (player + streamer) for a single game session and lets the
 * handler look up the counterpart to relay a message to.
 */
public class SignalingSession {

    private final String sessionId;
    private final ConcurrentMap<PeerRole, WebSocketSession> peers = new ConcurrentHashMap<>();

    public SignalingSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void addPeer(PeerRole role, WebSocketSession ws) {
        peers.put(role, ws);
    }

    public WebSocketSession peer(PeerRole role) {
        return peers.get(role);
    }

    public WebSocketSession counterpart(PeerRole role) {
        return peers.get(role.other());
    }

    public boolean hasPeer(PeerRole role) {
        WebSocketSession s = peers.get(role);
        return s != null && s.isOpen();
    }

    /** Remove a peer; returns true if the session is now empty. */
    public boolean removePeer(PeerRole role) {
        peers.remove(role);
        return peers.isEmpty();
    }

    public boolean isEmpty() {
        return peers.isEmpty();
    }
}
