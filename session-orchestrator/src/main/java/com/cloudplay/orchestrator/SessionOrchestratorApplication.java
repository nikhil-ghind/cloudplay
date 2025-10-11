package com.cloudplay.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the session-orchestrator: the brain of CloudPlay. It allocates
 * GPU-backed sessions by querying the registry, picking a node, reserving a slot,
 * and managing the session lifecycle. It also runs the autoscaler loop.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class SessionOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionOrchestratorApplication.class, args);
    }
}
