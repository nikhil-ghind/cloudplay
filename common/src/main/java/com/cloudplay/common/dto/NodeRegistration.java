package com.cloudplay.common.dto;

import com.cloudplay.common.model.GpuClass;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload a GPU node sends to register itself with the registry
 * (POST /api/v1/nodes).
 */
public class NodeRegistration {

    @NotBlank
    private String nodeId;

    @NotBlank
    private String region;

    @NotNull
    private GpuClass gpuClass;

    @Min(1)
    private int totalSlots;

    @NotBlank
    private String streamerHost;

    @Min(1)
    private int streamerPort;

    public NodeRegistration() {
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

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
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
}
