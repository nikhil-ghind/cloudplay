package com.cloudplay.common.error;

/**
 * Concrete domain exceptions shared across services. Grouped here as static
 * nested types so callers use a single import.
 */
public final class CommonExceptions {

    private CommonExceptions() {
    }

    public static class SessionNotFoundException extends CloudPlayException {
        public SessionNotFoundException(String sessionId) {
            super(ErrorCode.SESSION_NOT_FOUND, 404, "Session not found: " + sessionId);
        }
    }

    public static class NodeNotFoundException extends CloudPlayException {
        public NodeNotFoundException(String nodeId) {
            super(ErrorCode.NODE_NOT_FOUND, 404, "Node not found: " + nodeId);
        }
    }

    public static class NoCapacityException extends CloudPlayException {
        public NoCapacityException(String region, String gpuClass) {
            super(ErrorCode.NO_CAPACITY, 503,
                    "No GPU capacity available for region=" + region + " gpuClass=" + gpuClass);
        }
    }

    public static class SlotConflictException extends CloudPlayException {
        public SlotConflictException(String nodeId) {
            super(ErrorCode.SLOT_CONFLICT, 409,
                    "Could not reserve a slot on node " + nodeId + " (concurrent allocation won)");
        }
    }

    public static class LeaseExpiredException extends CloudPlayException {
        public LeaseExpiredException(String nodeId) {
            super(ErrorCode.LEASE_EXPIRED, 410, "Lease expired for node: " + nodeId);
        }
    }

    public static class UnauthorizedException extends CloudPlayException {
        public UnauthorizedException(String message) {
            super(ErrorCode.UNAUTHORIZED, 401, message);
        }
    }

    public static class UpstreamUnavailableException extends CloudPlayException {
        public UpstreamUnavailableException(String upstream, Throwable cause) {
            super(ErrorCode.UPSTREAM_UNAVAILABLE, 502,
                    "Upstream service unavailable: " + upstream, cause);
        }
    }
}
