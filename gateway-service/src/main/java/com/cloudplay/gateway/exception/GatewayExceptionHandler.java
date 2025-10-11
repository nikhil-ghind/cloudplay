package com.cloudplay.gateway.exception;

import com.cloudplay.common.error.ApiErrorResponse;
import com.cloudplay.common.error.CloudPlayException;
import com.cloudplay.common.error.ErrorResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

/**
 * Reactive exception handling for the gateway. Translates domain exceptions and
 * validation failures into the shared {@link ApiErrorResponse} envelope.
 */
@RestControllerAdvice
public class GatewayExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    @ExceptionHandler(CloudPlayException.class)
    public ResponseEntity<ApiErrorResponse> handleDomain(CloudPlayException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        if (ex.getHttpStatus() >= 500) {
            log.error("Gateway error: {}", ex.getMessage(), ex);
        }
        return ResponseEntity.status(ex.getHttpStatus()).body(ErrorResponses.from(ex, path));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(WebExchangeBindException ex,
                                                             ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        List<String> details = ex.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        ApiErrorResponse body = ErrorResponses.validation("Request validation failed", path);
        body.setDetails(details);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        log.error("Unexpected gateway error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponses.internal("Unexpected error", path));
    }
}
