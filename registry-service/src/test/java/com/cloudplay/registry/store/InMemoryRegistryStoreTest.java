package com.cloudplay.registry.store;

import com.cloudplay.common.error.CommonExceptions;
import com.cloudplay.common.model.GpuClass;
import com.cloudplay.common.model.GpuNode;
import com.cloudplay.common.model.NodeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRegistryStoreTest {

    private InMemoryRegistryStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryRegistryStore();
    }

    private GpuNode node(String id, int slots) {
        GpuNode n = new GpuNode();
        n.setNodeId(id);
        n.setRegion("us-east-1");
        n.setGpuClass(GpuClass.STANDARD);
        n.setState(NodeState.READY);
        n.setTotalSlots(slots);
        n.setUsedSlots(0);
        return n;
    }

    @Test
    void reserveAndReleaseAdjustUsedSlots() {
        store.upsertNode(node("n1", 2));

        GpuNode afterReserve = store.reserveSlot("n1", "sess-a");
        assertEquals(1, afterReserve.getUsedSlots());

        GpuNode afterRelease = store.releaseSlot("n1", "sess-a");
        assertEquals(0, afterRelease.getUsedSlots());
    }

    @Test
    void reserveIsIdempotentPerSession() {
        store.upsertNode(node("n1", 2));
        store.reserveSlot("n1", "sess-a");
        GpuNode again = store.reserveSlot("n1", "sess-a");
        assertEquals(1, again.getUsedSlots(), "re-reserving same session must not double count");
    }

    @Test
    void reserveThrowsWhenFull() {
        store.upsertNode(node("n1", 1));
        store.reserveSlot("n1", "sess-a");
        assertThrows(CommonExceptions.SlotConflictException.class,
                () -> store.reserveSlot("n1", "sess-b"));
    }

    @Test
    void concurrentReservationsNeverOversubscribe() throws InterruptedException {
        int slots = 8;
        store.upsertNode(node("n1", slots));
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger granted = new AtomicInteger();

        for (int i = 0; i < 50; i++) {
            final String sessionId = "sess-" + i;
            pool.submit(() -> {
                try {
                    store.reserveSlot("n1", sessionId);
                    granted.incrementAndGet();
                } catch (CommonExceptions.SlotConflictException ignored) {
                    // expected once full
                }
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));

        assertEquals(slots, granted.get());
        assertEquals(slots, store.findNode("n1").orElseThrow().getUsedSlots());
    }
}
