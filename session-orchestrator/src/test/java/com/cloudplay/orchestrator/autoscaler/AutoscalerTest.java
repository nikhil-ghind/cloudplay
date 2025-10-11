package com.cloudplay.orchestrator.autoscaler;

import com.cloudplay.common.model.GpuClass;
import com.cloudplay.common.model.GpuNode;
import com.cloudplay.common.model.NodeState;
import com.cloudplay.common.registry.RegistryClient;
import com.cloudplay.orchestrator.config.OrchestratorProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoscalerTest {

    private Autoscaler autoscaler;
    private OrchestratorProperties props;

    @BeforeEach
    void setUp() {
        props = new OrchestratorProperties();
        OrchestratorProperties.Autoscaler cfg = props.getAutoscaler();
        cfg.setTargetUtilization(0.70);
        cfg.setWarmSlotBuffer(2);
        cfg.setSlotsPerNode(4);
        cfg.setMinNodes(1);
        cfg.setMaxNodes(50);
        cfg.setMaxScaleDownStep(2);

        RegistryClient registry = Mockito.mock(RegistryClient.class);
        autoscaler = new Autoscaler(registry, props, new SimpleMeterRegistry());
    }

    private GpuNode node(String id, int used, int total) {
        GpuNode n = new GpuNode();
        n.setNodeId(id);
        n.setRegion("us-east-1");
        n.setGpuClass(GpuClass.STANDARD);
        n.setState(NodeState.READY);
        n.setUsedSlots(used);
        n.setTotalSlots(total);
        return n;
    }

    @Test
    void scalesUpWhenUtilizationHigh() {
        // 2 nodes, 4 slots each = 8 total, 7 used (87.5% util) -> needs more.
        List<GpuNode> nodes = List.of(node("a", 4, 4), node("b", 3, 4));
        Map<String, ScaleDecision> decisions = autoscaler.computeDecisions(nodes);

        ScaleDecision d = decisions.get("us-east-1/STANDARD");
        // required = ceil(7/0.7) + 2 = 10 + 2 = 12 slots -> ceil(12/4) = 3 nodes
        assertEquals(3, d.desiredNodes());
        assertEquals("SCALE_UP", d.direction());
    }

    @Test
    void scalesDownButRateLimited() {
        // 6 nodes mostly idle: desired would be small, but step-limited to -2.
        List<GpuNode> nodes = List.of(
                node("a", 0, 4), node("b", 0, 4), node("c", 0, 4),
                node("d", 0, 4), node("e", 0, 4), node("f", 1, 4));
        Map<String, ScaleDecision> decisions = autoscaler.computeDecisions(nodes);

        ScaleDecision d = decisions.get("us-east-1/STANDARD");
        assertEquals(4, d.desiredNodes(), "scale-down limited to 2 nodes per pass (6 -> 4)");
        assertEquals("SCALE_DOWN", d.direction());
    }

    @Test
    void neverBelowMinNodes() {
        List<GpuNode> nodes = List.of(node("a", 0, 4));
        Map<String, ScaleDecision> decisions = autoscaler.computeDecisions(nodes);
        ScaleDecision d = decisions.get("us-east-1/STANDARD");
        assertTrue(d.desiredNodes() >= props.getAutoscaler().getMinNodes());
    }
}
