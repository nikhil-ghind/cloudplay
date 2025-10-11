package com.cloudplay.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the gateway-service: the public edge of CloudPlay. It
 * authenticates requests and routes them to the orchestrator (sessions, nodes)
 * while exposing a single, stable external API surface.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
