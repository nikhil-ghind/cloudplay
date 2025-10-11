package com.cloudplay.gateway.controller;

import com.cloudplay.common.dto.SessionRequest;
import com.cloudplay.common.dto.SessionResponse;
import com.cloudplay.gateway.client.OrchestratorClient;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Public, authenticated session API at the edge. Validates requests, then proxies
 * to the orchestrator. This is the surface external clients (game launchers,
 * partner platforms) integrate against.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class GatewaySessionController {

    private final OrchestratorClient orchestrator;

    public GatewaySessionController(OrchestratorClient orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SessionResponse> create(@Valid @RequestBody SessionRequest request) {
        return orchestrator.createSession(request);
    }

    @GetMapping("/{sessionId}")
    public Mono<SessionResponse> get(@PathVariable String sessionId) {
        return orchestrator.getSession(sessionId);
    }

    @GetMapping
    public Flux<SessionResponse> list() {
        return orchestrator.listSessions();
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<SessionResponse> terminate(@PathVariable String sessionId) {
        return orchestrator.terminateSession(sessionId);
    }
}
