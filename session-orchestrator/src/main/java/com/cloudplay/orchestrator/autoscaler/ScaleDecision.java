package com.cloudplay.orchestrator.autoscaler;

import com.cloudplay.common.model.GpuClass;

/**
 * The autoscaler's decision for one (region, gpuClass) node pool.
 */
public record ScaleDecision(
        String region,
        GpuClass gpuClass,
        int currentNodes,
        int desiredNodes,
        int usedSlots,
        int totalSlots,
        double utilization) {

    public int delta() {
        return desiredNodes - currentNodes;
    }

    public String direction() {
        int d = delta();
        if (d > 0) {
            return "SCALE_UP";
        }
        if (d < 0) {
            return "SCALE_DOWN";
        }
        return "STEADY";
    }

    public String poolKey() {
        return region + "/" + gpuClass;
    }
}
