package com.cloudplay.gateway.client;

import com.cloudplay.common.dto.SessionRequest;
import com.cloudplay.common.dto.SessionResponse;
import com.cloudplay.common.error.CommonExceptions;
import com.cloudplay.common.error.ErrorCode;
import com.cloudplay.common.error.CloudPlayException;
import com.cloudplay.common.model.GpuNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Typed reactive client the gateway uses to call the orchestrator's REST API.
 * Maps upstream HTTP errors back into {@link CloudPlayException}s so the gateway
 * returns a consistent error envelope.
 */
@Component
public class OrchestratorClient {

    private final WebClient webClient;

    public OrchestratorClient(WebClient orchestratorWebClient) {
        this.webClient = orchestratorWebClient;
    }

    public Mono<SessionResponse> createSession(SessionRequest request) {
        return webClient.post()
                .uri("/api/v1/sessions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SessionResponse.class)
                .onErrorMap(this::translate);
    }

    public Mono<SessionResponse> getSession(String sessionId) {
        return webClient.get()
                .uri("/api/v1/sessions/{id}", sessionId)
                .retrieve()
                .bodyToMono(SessionResponse.class)
                .onErrorMap(this::translate);
    }

    public Flux<SessionResponse> listSessions() {
        return webClient.get()
                .uri("/api/v1/sessions")
                .retrieve()
                .bodyToFlux(SessionResponse.class)
                .onErrorMap(this::translate);
    }

    public Mono<SessionResponse> terminateSession(String sessionId) {
        return webClient.delete()
                .uri("/api/v1/sessions/{id}", sessionId)
                .retrieve()
                .bodyToMono(SessionResponse.class)
                .onErrorMap(this::translate);
    }

    public Flux<GpuNode> listNodes() {
        return webClient.get()
                .uri("/api/v1/nodes")
                .retrieve()
                .bodyToFlux(GpuNode.class)
                .onErrorMap(this::translate);
    }

    private Throwable translate(Throwable t) {
        if (t instanceof CloudPlayException) {
            return t;
        }
        if (t instanceof WebClientResponseException wcre) {
            HttpStatus status = HttpStatus.resolve(wcre.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                return new CommonExceptions.SessionNotFoundException("(upstream) " + wcre.getMessage());
            }
            if (status == HttpStatus.SERVICE_UNAVAILABLE) {
                return new CloudPlayException(ErrorCode.NO_CAPACITY, 503,
                        "No capacity available upstream", wcre);
            }
            return new CloudPlayException(ErrorCode.UPSTREAM_UNAVAILABLE, 502,
                    "Orchestrator returned " + wcre.getStatusCode(), wcre);
        }
        return new CommonExceptions.UpstreamUnavailableException("session-orchestrator", t);
    }
}
