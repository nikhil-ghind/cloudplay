package com.cloudplay.orchestrator.scheduler;

import com.cloudplay.common.dto.SessionRequest;
import com.cloudplay.common.model.GpuClass;
import com.cloudplay.common.model.GpuNode;
import com.cloudplay.common.model.NodeState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeSchedulerTest {

    private final NodeScheduler scheduler = new NodeScheduler();

    private GpuNode node(String id, int used, int total, NodeState state, GpuClass cls, String region) {
        GpuNode n = new GpuNode();
        n.setNodeId(id);
        n.setRegion(region);
        n.setGpuClass(cls);
        n.setState(state);
        n.setUsedSlots(used);
        n.setTotalSlots(total);
        n.setStreamerHost("10.0.0." + id.hashCode() % 250);
        n.setStreamerPort(50000);
        return n;
    }

    private SessionRequest request() {
        return new SessionRequest("u1", "g1", "us-east-1", GpuClass.STANDARD);
    }

    @Test
    void bestFitPrefersMoreUtilizedNode() {
        GpuNode tight = node("tight", 3, 4, NodeState.READY, GpuClass.STANDARD, "us-east-1");
        GpuNode loose = node("loose", 0, 4, NodeState.READY, GpuClass.STANDARD, "us-east-1");

        Optional<GpuNode> picked = scheduler.pick(request(), List.of(loose, tight));
        assertTrue(picked.isPresent());
        assertEquals("tight", picked.get().getNodeId(), "best-fit packs onto the fuller node");
    }

    @Test
    void filtersOutWrongRegionGpuClassAndUnschedulable() {
        GpuNode wrongRegion = node("r", 0, 4, NodeState.READY, GpuClass.STANDARD, "eu-west-1");
        GpuNode wrongClass = node("c", 0, 4, NodeState.READY, GpuClass.ULTRA, "us-east-1");
        GpuNode full = node("f", 4, 4, NodeState.READY, GpuClass.STANDARD, "us-east-1");
        GpuNode cordoned = node("d", 0, 4, NodeState.CORDONED, GpuClass.STANDARD, "us-east-1");
        GpuNode good = node("g", 1, 4, NodeState.READY, GpuClass.STANDARD, "us-east-1");

        List<GpuNode> ranked = scheduler.rank(request(),
                List.of(wrongRegion, wrongClass, full, cordoned, good));
        assertEquals(1, ranked.size());
        assertEquals("g", ranked.get(0).getNodeId());
    }

    @Test
    void emptyWhenNoCandidates() {
        assertTrue(scheduler.pick(request(), List.of()).isEmpty());
    }
}
