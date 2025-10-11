package com.cloudplay.registry.controller;

import com.cloudplay.common.dto.HeartbeatRequest;
import com.cloudplay.common.dto.LeaseResponse;
import com.cloudplay.common.dto.NodeRegistration;
import com.cloudplay.common.dto.SlotReservation;
import com.cloudplay.common.model.GpuClass;
import com.cloudplay.common.model.GpuNode;
import com.cloudplay.registry.service.RegistryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for the distributed node registry.
 */
@RestController
@RequestMapping("/api/v1/nodes")
public class NodeController {

    private final RegistryService registry;

    public NodeController(RegistryService registry) {
        this.registry = registry;
    }

    /** Register a node (idempotent on nodeId). Returns its lease. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaseResponse register(@Valid @RequestBody NodeRegistration registration) {
        return registry.register(registration);
    }

    /** Renew lease + report current load. */
    @PostMapping("/{nodeId}/heartbeat")
    public LeaseResponse heartbeat(@PathVariable String nodeId,
                                   @Valid @RequestBody HeartbeatRequest heartbeat) {
        return registry.heartbeat(nodeId, heartbeat);
    }

    @GetMapping("/{nodeId}")
    public GpuNode getNode(@PathVariable String nodeId) {
        return registry.getNode(nodeId);
    }

    @GetMapping
    public List<GpuNode> listNodes(@RequestParam(required = false) String region,
                                   @RequestParam(required = false) GpuClass gpuClass,
                                   @RequestParam(name = "schedulable", defaultValue = "false") boolean schedulable) {
        return registry.listNodes(region, gpuClass, schedulable);
    }

    /** Atomic slot reservation (called by the orchestrator during allocation). */
    @PostMapping("/{nodeId}/reserve")
    public GpuNode reserve(@PathVariable String nodeId, @RequestBody SlotReservation reservation) {
        return registry.reserveSlot(nodeId, reservation.getSessionId());
    }

    @PostMapping("/{nodeId}/release")
    public GpuNode release(@PathVariable String nodeId, @RequestBody SlotReservation reservation) {
        return registry.releaseSlot(nodeId, reservation.getSessionId());
    }

    /** Cordon: drain a node ahead of scale-down. */
    @PostMapping("/{nodeId}/cordon")
    public GpuNode cordon(@PathVariable String nodeId) {
        return registry.cordon(nodeId);
    }

    @DeleteMapping("/{nodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deregister(@PathVariable String nodeId) {
        registry.deregister(nodeId);
        return ResponseEntity.noContent().build();
    }
}
