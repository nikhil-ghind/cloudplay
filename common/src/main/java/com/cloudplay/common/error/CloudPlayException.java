package com.cloudplay.common.error;

/**
 * Base class for all domain exceptions. Carries an {@link ErrorCode} and an HTTP
 * status so the per-service exception handlers can translate uniformly.
 */
public class CloudPlayException extends RuntimeException {

    private final ErrorCode code;
    private final int httpStatus;

    public CloudPlayException(ErrorCode code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public CloudPlayException(ErrorCode code, int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public ErrorCode getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
