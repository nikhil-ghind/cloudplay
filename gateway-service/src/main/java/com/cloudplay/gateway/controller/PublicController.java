package com.cloudplay.gateway.controller;

import com.cloudplay.gateway.config.GatewayProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Unauthenticated public endpoints (matched by the auth filter's open paths).
 * Advertises connection details clients need before they authenticate.
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final GatewayProperties props;

    public PublicController(GatewayProperties props) {
        this.props = props;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "service", "cloudplay-gateway",
                "version", "1.0.0",
                "signalingUrl", props.getSignalingUrl(),
                "authRequired", props.getAuth().isEnabled());
    }
}
