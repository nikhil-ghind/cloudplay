package com.cloudplay.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Wires the WebClient used to proxy requests to the orchestrator.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public WebClient orchestratorWebClient(GatewayProperties props) {
        return WebClient.builder()
                .baseUrl(props.getOrchestratorUrl())
                .build();
    }
}
