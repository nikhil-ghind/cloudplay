package com.cloudplay.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Objects;

/**
 * Canonical representation of an allocated (or pending) game streaming session.
 * Shared between the orchestrator, registry and gateway so all services agree on
 * the wire format.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameSession {

    private String sessionId;
    private String userId;
    private String gameId;
    private String region;
    private GpuClass gpuClass;
    private SessionState state;

    /** Id of the GPU node this session is scheduled onto; null while PENDING. */
    private String nodeId;

    /** Host:port of the WebRTC streamer endpoint on the node; null until ACTIVE. */
    private String streamerEndpoint;

    private Instant createdAt;
    private Instant updatedAt;

    /** Optional human-readable reason for FAILED/TERMINATING transitions. */
    private String statusReason;

    public GameSession() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public GpuClass getGpuClass() {
        return gpuClass;
    }

    public void setGpuClass(GpuClass gpuClass) {
        this.gpuClass = gpuClass;
    }

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getStreamerEndpoint() {
        return streamerEndpoint;
    }

    public void setStreamerEndpoint(String streamerEndpoint) {
        this.streamerEndpoint = streamerEndpoint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GameSession that)) {
            return false;
        }
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "GameSession{sessionId='" + sessionId + "', state=" + state
                + ", nodeId='" + nodeId + "', region='" + region
                + "', gpuClass=" + gpuClass + '}';
    }
}
