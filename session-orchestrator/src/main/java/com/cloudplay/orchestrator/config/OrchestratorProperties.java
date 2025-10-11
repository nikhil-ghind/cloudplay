package com.cloudplay.orchestrator.config;

import com.cloudplay.common.registry.RegistryClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;

/**
 * Configuration for the orchestrator, bound from {@code cloudplay.orchestrator.*}.
 */
@ConfigurationProperties(prefix = "cloudplay.orchestrator")
public class OrchestratorProperties {

    @NestedConfigurationProperty
    private RegistryClientProperties registry = new RegistryClientProperties();

    @NestedConfigurationProperty
    private Autoscaler autoscaler = new Autoscaler();

    /** Public base URL of the signaling-service advertised to clients. */
    private String signalingBaseUrl = "wss://signaling.cloudplay.example.com";

    /** Max attempts to reserve a slot before giving up (handles CAS races). */
    private int maxAllocationAttempts = 5;

    public RegistryClientProperties getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryClientProperties registry) {
        this.registry = registry;
    }

    public Autoscaler getAutoscaler() {
        return autoscaler;
    }

    public void setAutoscaler(Autoscaler autoscaler) {
        this.autoscaler = autoscaler;
    }

    public String getSignalingBaseUrl() {
        return signalingBaseUrl;
    }

    public void setSignalingBaseUrl(String signalingBaseUrl) {
        this.signalingBaseUrl = signalingBaseUrl;
    }

    public int getMaxAllocationAttempts() {
        return maxAllocationAttempts;
    }

    public void setMaxAllocationAttempts(int maxAllocationAttempts) {
        this.maxAllocationAttempts = maxAllocationAttempts;
    }

    /**
     * Autoscaler tunables. The autoscaler computes a <em>desired node count</em>
     * per (region, gpuClass) pool from observed session demand and a target
     * utilization, then emits scale intents (exposed as metrics / acted on by the
     * cloud autoscaler or KEDA via the published custom metric).
     */
    public static class Autoscaler {
        private boolean enabled = true;
        private Duration evaluationInterval = Duration.ofSeconds(15);

        /** Desired steady-state node utilization (0..1). */
        private double targetUtilization = 0.70;

        /** Headroom slots to keep warm for instant joins, per pool. */
        private int warmSlotBuffer = 2;

        /** Hard floor / ceiling on nodes per pool. */
        private int minNodes = 1;
        private int maxNodes = 50;

        /** Slots a single node of each class provides (used to size scale-up). */
        private int slotsPerNode = 4;

        /** Don't scale down a pool more than this many nodes per evaluation. */
        private int maxScaleDownStep = 2;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getEvaluationInterval() {
            return evaluationInterval;
        }

        public void setEvaluationInterval(Duration evaluationInterval) {
            this.evaluationInterval = evaluationInterval;
        }

        public double getTargetUtilization() {
            return targetUtilization;
        }

        public void setTargetUtilization(double targetUtilization) {
            this.targetUtilization = targetUtilization;
        }

        public int getWarmSlotBuffer() {
            return warmSlotBuffer;
        }

        public void setWarmSlotBuffer(int warmSlotBuffer) {
            this.warmSlotBuffer = warmSlotBuffer;
        }

        public int getMinNodes() {
            return minNodes;
        }

        public void setMinNodes(int minNodes) {
            this.minNodes = minNodes;
        }

        public int getMaxNodes() {
            return maxNodes;
        }

        public void setMaxNodes(int maxNodes) {
            this.maxNodes = maxNodes;
        }

        public int getSlotsPerNode() {
            return slotsPerNode;
        }

        public void setSlotsPerNode(int slotsPerNode) {
            this.slotsPerNode = slotsPerNode;
        }

        public int getMaxScaleDownStep() {
            return maxScaleDownStep;
        }

        public void setMaxScaleDownStep(int maxScaleDownStep) {
            this.maxScaleDownStep = maxScaleDownStep;
        }
    }
}
