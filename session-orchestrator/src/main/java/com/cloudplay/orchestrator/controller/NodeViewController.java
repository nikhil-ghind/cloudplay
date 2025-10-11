package com.cloudplay.orchestrator.controller;

import com.cloudplay.common.model.GpuClass;
import com.cloudplay.common.model.GpuNode;
import com.cloudplay.common.registry.RegistryClient;
import com.cloudplay.orchestrator.autoscaler.Autoscaler;
import com.cloudplay.orchestrator.autoscaler.ScaleDecision;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Read-only views over the node inventory (proxied from the registry) and the
 * autoscaler's latest decisions. Useful for dashboards and ops.
 */
@RestController
@RequestMapping("/api/v1")
public class NodeViewController {

    private final RegistryClient registry;
    private final Autoscaler autoscaler;

    public NodeViewController(RegistryClient registry, Autoscaler autoscaler) {
        this.registry = registry;
        this.autoscaler = autoscaler;
    }

    @GetMapping("/nodes")
    public List<GpuNode> nodes(@RequestParam(required = false) String region,
                               @RequestParam(required = false) GpuClass gpuClass) {
        List<GpuNode> all = registry.listAllNodes().collectList().block();
        if (all == null) {
            return List.of();
        }
        return all.stream()
                .filter(n -> region == null || region.equalsIgnoreCase(n.getRegion()))
                .filter(n -> gpuClass == null || gpuClass == n.getGpuClass())
                .toList();
    }

    @GetMapping("/autoscaler/decisions")
    public Map<String, ScaleDecision> decisions() {
        return autoscaler.latestDecisions();
    }
}
