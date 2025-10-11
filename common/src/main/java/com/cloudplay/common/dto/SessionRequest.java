package com.cloudplay.common.dto;

import com.cloudplay.common.model.GpuClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for allocating a new game streaming session
 * (POST /api/v1/sessions).
 */
public class SessionRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "gameId is required")
    private String gameId;

    @NotBlank(message = "region is required")
    private String region;

    @NotNull(message = "gpuClass is required")
    private GpuClass gpuClass;

    public SessionRequest() {
    }

    public SessionRequest(String userId, String gameId, String region, GpuClass gpuClass) {
        this.userId = userId;
        this.gameId = gameId;
        this.region = region;
        this.gpuClass = gpuClass;
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
}
