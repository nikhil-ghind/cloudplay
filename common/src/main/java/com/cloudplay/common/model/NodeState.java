package com.cloudplay.common.model;

/**
 * Health/availability states for a GPU streaming node registered in the registry.
 */
public enum NodeState {

    /** Node has registered and is accepting new sessions. */
    READY,

    /** Node is registered but draining: existing sessions kept, no new allocations. */
    CORDONED,

    /** Node missed its heartbeat lease window and is considered unhealthy. */
    UNHEALTHY,

    /** Node has been removed from the pool. */
    DECOMMISSIONED;

    public boolean isSchedulable() {
        return this == READY;
    }
}
