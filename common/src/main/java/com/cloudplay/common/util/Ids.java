package com.cloudplay.common.util;

import java.util.UUID;

/**
 * Small id helpers shared across services so generated ids have consistent,
 * recognisable prefixes.
 */
public final class Ids {

    private Ids() {
    }

    public static String sessionId() {
        return "sess-" + shortUuid();
    }

    public static String nodeId() {
        return "node-" + shortUuid();
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
