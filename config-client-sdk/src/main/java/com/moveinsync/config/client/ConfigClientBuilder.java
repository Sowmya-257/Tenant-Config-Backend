package com.moveinsync.config.client;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Fluent builder for creating MoveInSyncConfigClient instances.
 */
public class ConfigClientBuilder {

    private String serverUrl = "http://localhost:8080";
    private String tenantId;
    private String authToken;
    private Path cacheDirectory = Paths.get(System.getProperty("user.home"), ".moveinsync", "config_cache");
    private long reconcileIntervalSeconds = 3;

    // Sets the base URL of the remote configuration service.
    public ConfigClientBuilder serverUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    // Sets the client tenant identifier for multi-tenant isolation.
    public ConfigClientBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    // Sets the authentication token for secure communication.
    public ConfigClientBuilder authToken(String authToken) {
        this.authToken = authToken;
        return this;
    }

    // Sets the local directory used to persist backup snapshots on disk.
    public ConfigClientBuilder cacheDirectory(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        return this;
    }

    // Sets the background reconciliation check interval in seconds.
    public ConfigClientBuilder reconcileIntervalSeconds(long reconcileIntervalSeconds) {
        this.reconcileIntervalSeconds = reconcileIntervalSeconds;
        return this;
    }

    // Constructs and initializes a fully configured MoveInSyncConfigClient instance.
    public MoveInSyncConfigClient build() {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID must be specified for MoveInSyncConfigClient");
        }
        return new DefaultMoveInSyncConfigClient(
                serverUrl,
                tenantId.trim(),
                authToken,
                cacheDirectory,
                reconcileIntervalSeconds
        );
    }
}
