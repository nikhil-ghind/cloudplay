package com.cloudplay.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Gateway configuration, bound from {@code cloudplay.gateway.*}.
 */
@ConfigurationProperties(prefix = "cloudplay.gateway")
public class GatewayProperties {

    /** Base URL of the orchestrator the gateway proxies to. */
    private String orchestratorUrl = "http://session-orchestrator:8082";

    /** Public URL of the signaling-service advertised to clients. */
    private String signalingUrl = "wss://signaling.cloudplay.example.com";

    /** Auth settings. */
    private Auth auth = new Auth();

    public String getOrchestratorUrl() {
        return orchestratorUrl;
    }

    public void setOrchestratorUrl(String orchestratorUrl) {
        this.orchestratorUrl = orchestratorUrl;
    }

    public String getSignalingUrl() {
        return signalingUrl;
    }

    public void setSignalingUrl(String signalingUrl) {
        this.signalingUrl = signalingUrl;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public static class Auth {
        private boolean enabled = true;

        /**
         * Static API keys accepted via the {@code X-API-Key} header. In a real
         * deployment this would be a JWT verifier / OIDC introspection; kept as a
         * pluggable key list here for self-containment.
         */
        private List<String> apiKeys = List.of();

        /** Paths that bypass auth (health, metrics, docs). */
        private List<String> openPaths = List.of("/actuator/**", "/api/v1/public/**");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getApiKeys() {
            return apiKeys;
        }

        public void setApiKeys(List<String> apiKeys) {
            this.apiKeys = apiKeys;
        }

        public List<String> getOpenPaths() {
            return openPaths;
        }

        public void setOpenPaths(List<String> openPaths) {
            this.openPaths = openPaths;
        }
    }
}
