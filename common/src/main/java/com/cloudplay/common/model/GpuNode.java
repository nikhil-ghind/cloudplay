package com.cloudplay.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Objects;

/**
 * A GPU streaming node registered with the registry. Capacity is expressed in
 * "session slots" (how many concurrent sessions of the node's GPU class it can
 * host). The registry tracks leases via {@link #leaseExpiresAt}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GpuNode {

    private String nodeId;
    private String region;
    private GpuClass gpuClass;
    private NodeState state;

    /** Total session slots the node can host. */
    private int totalSlots;

    /** Slots currently occupied by ALLOCATING/ACTIVE/TERMINATING sessions. */
    private int usedSlots;

    /** Advertised streamer host (the WebRTC media endpoint base). */
    private String streamerHost;
    private int streamerPort;

    private Instant registeredAt;
    private Instant lastHeartbeat;

    /** When the current heartbeat lease expires; node goes UNHEALTHY after this. */
    private Instant leaseExpiresAt;

    public GpuNode() {
    }

    @JsonIgnore
    public int availableSlots() {
        return Math.max(0, totalSlots - usedSlots);
    }

    @JsonIgnore
    public boolean hasCapacity() {
        return state == NodeState.READY && availableSlots() > 0;
    }

    @JsonIgnore
    public double utilization() {
        return totalSlots == 0 ? 1.0 : (double) usedSlots / (double) totalSlots;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
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

    public NodeState getState() {
        return state;
    }

    public void setState(NodeState state) {
        this.state = state;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }

    public int getUsedSlots() {
        return usedSlots;
    }

    public void setUsedSlots(int usedSlots) {
        this.usedSlots = usedSlots;
    }

    public String getStreamerHost() {
        return streamerHost;
    }

    public void setStreamerHost(String streamerHost) {
        this.streamerHost = streamerHost;
    }

    public int getStreamerPort() {
        return streamerPort;
    }

    public void setStreamerPort(int streamerPort) {
        this.streamerPort = streamerPort;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GpuNode gpuNode)) {
            return false;
        }
        return Objects.equals(nodeId, gpuNode.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    @Override
    public String toString() {
        return "GpuNode{nodeId='" + nodeId + "', region='" + region
                + "', gpuClass=" + gpuClass + ", state=" + state
                + ", used=" + usedSlots + "/" + totalSlots + '}';
    }
}
