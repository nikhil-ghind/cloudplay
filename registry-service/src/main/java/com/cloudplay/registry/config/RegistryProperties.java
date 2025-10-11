package com.cloudplay.registry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Tunables for lease management. Bound from {@code cloudplay.registry.*}.
 */
@Component
@ConfigurationProperties(prefix = "cloudplay.registry")
public class RegistryProperties {

    /** How long a heartbeat extends the lease for. */
    private Duration leaseTtl = Duration.ofSeconds(30);

    /** Interval at which nodes are asked to heartbeat (advertised to nodes). */
    private Duration heartbeatInterval = Duration.ofSeconds(10);

    /** How often the reaper scans for expired leases. */
    private Duration reaperInterval = Duration.ofSeconds(5);

    /** Grace period after a node goes UNHEALTHY before it is decommissioned. */
    private Duration decommissionGrace = Duration.ofMinutes(2);

    public Duration getLeaseTtl() {
        return leaseTtl;
    }

    public void setLeaseTtl(Duration leaseTtl) {
        this.leaseTtl = leaseTtl;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public Duration getReaperInterval() {
        return reaperInterval;
    }

    public void setReaperInterval(Duration reaperInterval) {
        this.reaperInterval = reaperInterval;
    }

    public Duration getDecommissionGrace() {
        return decommissionGrace;
    }

    public void setDecommissionGrace(Duration decommissionGrace) {
        this.decommissionGrace = decommissionGrace;
    }
}
