package com.cloudplay.common.registry;

import com.cloudplay.common.dto.SlotReservation;
import com.cloudplay.common.error.CommonExceptions;
import com.cloudplay.common.error.ErrorCode;
import com.cloudplay.common.error.CloudPlayException;
import com.cloudplay.common.model.GpuClass;
import com.cloudplay.common.model.GpuNode;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * Reactive client used by the orchestrator (and other services) to talk to the
 * registry-service. Wraps the registry's REST API behind typed methods and
 * translates transport/HTTP errors into {@link CloudPlayException}s.
 *
 * <p>This lives in {@code common} so every service shares one definition of how
 * to reach the registry, keeping wire formats consistent.</p>
 */
public class RegistryClient {

    private static final Logger log = LoggerFactory.getLogger(RegistryClient.class);

    private final WebClient webClient;

    public RegistryClient(RegistryClientProperties props) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) props.getConnectTimeout().toMillis())
                .responseTimeout(props.getResponseTimeout())
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(props.getResponseTimeout().toMillis(),
                                TimeUnit.MILLISECONDS)));

        this.webClient = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .build();
        log.info("RegistryClient initialised against {}", props.getBaseUrl());
    }

    /** List all schedulable nodes matching a region and GPU class. */
    public Flux<GpuNode> listSchedulableNodes(String region, GpuClass gpuClass) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/nodes")
                        .queryParam("region", region)
                        .queryParam("gpuClass", gpuClass.name())
                        .queryParam("schedulable", true)
                        .build())
                .retrieve()
                .bodyToFlux(GpuNode.class)
                .onErrorMap(this::translate);
    }

    public Mono<GpuNode> getNode(String nodeId) {
        return webClient.get()
                .uri("/api/v1/nodes/{id}", nodeId)
                .retrieve()
                .bodyToMono(GpuNode.class)
                .onErrorMap(this::translate);
    }

    /**
     * Atomically reserve a capacity slot on a node for a session. The registry
     * uses a compare-and-set so concurrent allocations to the last slot result
     * in a {@link CommonExceptions.SlotConflictException} for the loser.
     */
    public Mono<GpuNode> reserveSlot(String nodeId, String sessionId) {
        return webClient.post()
                .uri("/api/v1/nodes/{id}/reserve", nodeId)
                .bodyValue(new SlotReservation(sessionId))
                .retrieve()
                .bodyToMono(GpuNode.class)
                .onErrorMap(this::translate);
    }

    /** Release a previously reserved slot (on teardown or allocation failure). */
    public Mono<GpuNode> releaseSlot(String nodeId, String sessionId) {
        return webClient.post()
                .uri("/api/v1/nodes/{id}/release", nodeId)
                .bodyValue(new SlotReservation(sessionId))
                .retrieve()
                .bodyToMono(GpuNode.class)
                .onErrorMap(this::translate);
    }

    /** Aggregate capacity snapshot for the autoscaler. */
    public Flux<GpuNode> listAllNodes() {
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
                return new CommonExceptions.NodeNotFoundException("(from registry) " + wcre.getMessage());
            }
            if (status == HttpStatus.CONFLICT) {
                return new CommonExceptions.SlotConflictException("(from registry)");
            }
            return new CloudPlayException(ErrorCode.UPSTREAM_UNAVAILABLE,
                    502, "Registry returned " + wcre.getStatusCode(), wcre);
        }
        return new CommonExceptions.UpstreamUnavailableException("registry-service", t);
    }
}
