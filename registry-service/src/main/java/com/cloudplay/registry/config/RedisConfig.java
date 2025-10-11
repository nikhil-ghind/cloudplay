package com.cloudplay.registry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Provides the JSON mapper used by the Redis store to (de)serialise node records
 * (with java.time support and ISO-8601 dates). Marked {@code @Primary} so
 * it is unambiguously injected into {@code RedisRegistryStore} alongside any
 * auto-configured mapper. The Redis connection factory and StringRedisTemplate
 * are auto-configured by Spring Boot from {@code spring.data.redis.*}.
 */
@Configuration
public class RedisConfig {

    @Bean
    @Primary
    public ObjectMapper registryObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
