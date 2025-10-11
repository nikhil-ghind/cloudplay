package com.cloudplay.signaling.controller;

import com.cloudplay.signaling.service.SignalingSessionRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight status endpoint reporting how many signaling sessions this
 * instance is currently relaying.
 */
@RestController
@RequestMapping("/api/v1/signaling")
public class SignalingStatusController {

    private final SignalingSessionRegistry registry;

    public SignalingStatusController(SignalingSessionRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of(
                "activeSessions", registry.size(),
                "endpoint", "/signaling?sessionId={sessionId}");
    }
}
