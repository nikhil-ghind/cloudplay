package com.cloudplay.common.registry;

import java.time.Duration;

/**
 * Connection settings for {@link RegistryClient}. Services bind this from their
 * own configuration (e.g. cloudplay.registry.* in application.yml).
 */
public class RegistryClientProperties {

    /** Base URL of the registry-service, e.g. http://registry-service:8083 */
    private String baseUrl = "http://registry-service:8083";

    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration responseTimeout = Duration.ofSeconds(5);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(Duration responseTimeout) {
        this.responseTimeout = responseTimeout;
    }
}
