package com.cloudplay.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the CloudPlay registry-service: a distributed registry of GPU
 * nodes and sessions with heartbeat-based leases and atomic capacity tracking.
 */
@SpringBootApplication
@EnableScheduling
public class RegistryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistryServiceApplication.class, args);
    }
}
