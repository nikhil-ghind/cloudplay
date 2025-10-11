package com.cloudplay.common.model;

/**
 * Lifecycle states for a game streaming session.
 *
 * <pre>
 *   PENDING --> ALLOCATING --> ACTIVE --> TERMINATING --> TERMINATED
 *                   |                          ^
 *                   +-----> FAILED ------------+
 * </pre>
 */
public enum SessionState {

    /** Session request accepted, not yet scheduled onto a node. */
    PENDING,

    /** A node has been selected; GPU resources are being reserved. */
    ALLOCATING,

    /** Session is running and the WebRTC stream is (or may be) live. */
    ACTIVE,

    /** Allocation failed (no capacity, node lost, timeout). */
    FAILED,

    /** Graceful teardown in progress. */
    TERMINATING,

    /** Session has been torn down and resources released. */
    TERMINATED;

    public boolean isTerminal() {
        return this == TERMINATED || this == FAILED;
    }

    public boolean consumesCapacity() {
        return this == ALLOCATING || this == ACTIVE || this == TERMINATING;
    }
}
