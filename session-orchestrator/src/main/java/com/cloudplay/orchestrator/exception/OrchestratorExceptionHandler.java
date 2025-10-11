package com.cloudplay.orchestrator.exception;

import com.cloudplay.common.error.ApiErrorResponse;
import com.cloudplay.common.error.CloudPlayException;
import com.cloudplay.common.error.ErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class OrchestratorExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorExceptionHandler.class);

    @ExceptionHandler(CloudPlayException.class)
    public ResponseEntity<ApiErrorResponse> handleDomain(CloudPlayException ex, HttpServletRequest req) {
        if (ex.getHttpStatus() >= 500) {
            log.error("Orchestrator error: {}", ex.getMessage(), ex);
        }
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ErrorResponses.from(ex, req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        ApiErrorResponse body = ErrorResponses.validation("Request validation failed", req.getRequestURI());
        body.setDetails(details);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected orchestrator error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponses.internal("Unexpected error", req.getRequestURI()));
    }
}
