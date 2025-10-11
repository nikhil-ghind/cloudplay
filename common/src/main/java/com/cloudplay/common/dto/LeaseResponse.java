package com.cloudplay.common.dto;

import java.time.Instant;

/**
 * Response returned to a node after registration or heartbeat, telling it when
 * its lease expires and how soon it must heartbeat again.
 */
public class LeaseResponse {

    private String nodeId;
    private Instant leaseExpiresAt;
    private long heartbeatIntervalSeconds;

    public LeaseResponse() {
    }

    public LeaseResponse(String nodeId, Instant leaseExpiresAt, long heartbeatIntervalSeconds) {
        this.nodeId = nodeId;
        this.leaseExpiresAt = leaseExpiresAt;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public long getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(long heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }
}
