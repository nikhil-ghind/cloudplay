package com.cloudplay.common.error;

/**
 * Factory helpers used by each service's {@code @RestControllerAdvice} to build a
 * consistent {@link ApiErrorResponse} from a {@link CloudPlayException} or a
 * generic failure.
 */
public final class ErrorResponses {

    private ErrorResponses() {
    }

    public static ApiErrorResponse from(CloudPlayException ex, String path) {
        return new ApiErrorResponse(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), path);
    }

    public static ApiErrorResponse internal(String message, String path) {
        return new ApiErrorResponse(500, ErrorCode.INTERNAL_ERROR, message, path);
    }

    public static ApiErrorResponse validation(String message, String path) {
        return new ApiErrorResponse(400, ErrorCode.VALIDATION_FAILED, message, path);
    }
}
