package com.cloudplay.common.model;

/**
 * GPU tier a session may request. Each class maps to a different node pool and
 * encodes a relative cost weight used by the autoscaler and scheduler.
 */
public enum GpuClass {

    /** Entry-level streaming, 1080p60. */
    STANDARD(1),

    /** High-end, 1440p120 / 4K60. */
    PERFORMANCE(2),

    /** Ray-tracing capable, multi-GPU. */
    ULTRA(4);

    private final int costWeight;

    GpuClass(int costWeight) {
        this.costWeight = costWeight;
    }

    public int getCostWeight() {
        return costWeight;
    }
}
