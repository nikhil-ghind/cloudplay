package com.cloudplay.common.dto;

import jakarta.validation.constraints.Min;

/**
 * Periodic heartbeat a node sends to renew its lease and report live load.
 * (POST /api/v1/nodes/{nodeId}/heartbeat)
 */
public class HeartbeatRequest {

    /** Current number of occupied session slots reported by the node agent. */
    @Min(0)
    private int usedSlots;

    public HeartbeatRequest() {
    }

    public HeartbeatRequest(int usedSlots) {
        this.usedSlots = usedSlots;
    }

    public int getUsedSlots() {
        return usedSlots;
    }

    public void setUsedSlots(int usedSlots) {
        this.usedSlots = usedSlots;
    }
}
