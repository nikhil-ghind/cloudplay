package com.cloudplay.orchestrator.config;

import com.cloudplay.common.registry.RegistryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires shared beans: the registry client (from the common module) configured
 * from orchestrator properties.
 */
@Configuration
public class OrchestratorConfig {

    @Bean
    public RegistryClient registryClient(OrchestratorProperties props) {
        return new RegistryClient(props.getRegistry());
    }
}
