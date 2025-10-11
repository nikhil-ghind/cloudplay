package com.cloudplay.common.error;

/**
 * Stable, machine-readable error codes returned across all CloudPlay services.
 */
public enum ErrorCode {

    VALIDATION_FAILED,
    SESSION_NOT_FOUND,
    NODE_NOT_FOUND,
    NO_CAPACITY,
    SLOT_CONFLICT,
    LEASE_EXPIRED,
    UNAUTHORIZED,
    FORBIDDEN,
    UPSTREAM_UNAVAILABLE,
    INTERNAL_ERROR
}
