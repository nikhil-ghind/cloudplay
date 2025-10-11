package com.cloudplay.common.dto;

import com.cloudplay.common.model.GameSession;
import com.cloudplay.common.model.GpuClass;
import com.cloudplay.common.model.SessionState;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Client-facing view of a session. Derived from {@link GameSession} but omits
 * internal scheduling detail that should not be exposed at the edge.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionResponse {

    private String sessionId;
    private String userId;
    private String gameId;
    private String region;
    private GpuClass gpuClass;
    private SessionState state;

    /** URL the player connects the signaling WebSocket to once ACTIVE. */
    private String signalingUrl;

    /** Streamer media endpoint advertised to the player. */
    private String streamerEndpoint;

    private String statusReason;
    private Instant createdAt;
    private Instant updatedAt;

    public static SessionResponse fromSession(GameSession session, String signalingUrl) {
        SessionResponse r = new SessionResponse();
        r.sessionId = session.getSessionId();
        r.userId = session.getUserId();
        r.gameId = session.getGameId();
        r.region = session.getRegion();
        r.gpuClass = session.getGpuClass();
        r.state = session.getState();
        r.streamerEndpoint = session.getStreamerEndpoint();
        r.statusReason = session.getStatusReason();
        r.createdAt = session.getCreatedAt();
        r.updatedAt = session.getUpdatedAt();
        if (session.getState() == SessionState.ACTIVE) {
            r.signalingUrl = signalingUrl;
        }
        return r;
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

    public String getSignalingUrl() {
        return signalingUrl;
    }

    public void setSignalingUrl(String signalingUrl) {
        this.signalingUrl = signalingUrl;
    }

    public String getStreamerEndpoint() {
        return streamerEndpoint;
    }

    public void setStreamerEndpoint(String streamerEndpoint) {
        this.streamerEndpoint = streamerEndpoint;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
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
}
