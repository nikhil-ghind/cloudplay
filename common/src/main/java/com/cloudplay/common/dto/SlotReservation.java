package com.cloudplay.common.dto;

/**
 * Internal request the orchestrator sends to the registry to atomically reserve
 * or release a capacity slot on a node for a given session.
 * (POST /api/v1/nodes/{nodeId}/reserve, POST .../release)
 */
public class SlotReservation {

    private String sessionId;

    public SlotReservation() {
    }

    public SlotReservation(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
