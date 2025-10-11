package com.cloudplay.orchestrator.controller;

import com.cloudplay.common.dto.SessionRequest;
import com.cloudplay.common.dto.SessionResponse;
import com.cloudplay.common.model.GameSession;
import com.cloudplay.orchestrator.config.OrchestratorProperties;
import com.cloudplay.orchestrator.service.SessionAllocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Session lifecycle REST API exposed by the orchestrator.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionAllocationService allocation;
    private final OrchestratorProperties props;

    public SessionController(SessionAllocationService allocation, OrchestratorProperties props) {
        this.allocation = allocation;
        this.props = props;
    }

    /** Allocate a GPU-backed session. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse allocate(@Valid @RequestBody SessionRequest request) {
        GameSession session = allocation.allocate(request);
        return toResponse(session);
    }

    @GetMapping("/{sessionId}")
    public SessionResponse get(@PathVariable String sessionId) {
        return toResponse(allocation.get(sessionId));
    }

    @GetMapping
    public List<SessionResponse> list() {
        return allocation.list().stream().map(this::toResponse).toList();
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<SessionResponse> terminate(@PathVariable String sessionId) {
        GameSession terminated = allocation.terminate(sessionId);
        return ResponseEntity.accepted().body(toResponse(terminated));
    }

    private SessionResponse toResponse(GameSession session) {
        String signalingUrl = props.getSignalingBaseUrl()
                + "/signaling?sessionId=" + session.getSessionId();
        return SessionResponse.fromSession(session, signalingUrl);
    }
}
