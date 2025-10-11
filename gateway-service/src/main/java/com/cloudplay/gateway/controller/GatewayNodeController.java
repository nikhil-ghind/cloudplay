package com.cloudplay.gateway.controller;

import com.cloudplay.common.model.GpuNode;
import com.cloudplay.gateway.client.OrchestratorClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Operational view of the GPU node pool, proxied from the orchestrator.
 */
@RestController
@RequestMapping("/api/v1/nodes")
public class GatewayNodeController {

    private final OrchestratorClient orchestrator;

    public GatewayNodeController(OrchestratorClient orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public Flux<GpuNode> list() {
        return orchestrator.listNodes();
    }
}
