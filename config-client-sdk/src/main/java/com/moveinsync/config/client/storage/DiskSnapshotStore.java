package com.moveinsync.config.client.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moveinsync.config.client.model.TenantConfigSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Handles saving and loading configuration snapshots to local disk for offline resilience.
 */
public class DiskSnapshotStore {

    private static final Logger log = LoggerFactory.getLogger(DiskSnapshotStore.class);
    private final Path cacheDirectory;
    private final ObjectMapper objectMapper;

    // Initializes the disk store pointing to a local cache directory.
    public DiskSnapshotStore(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        ensureDirectoryExists();
    }

    // Creates the cache directory if it does not yet exist on disk.
    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(cacheDirectory)) {
                Files.createDirectories(cacheDirectory);
            }
        } catch (IOException e) {
            log.error("Failed to create snapshot cache directory: {}", cacheDirectory, e);
        }
    }

    // Atomically saves a tenant snapshot to a local disk file using a temporary file.
    public synchronized void save(TenantConfigSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        Path targetFile = cacheDirectory.resolve("tenant_" + snapshot.getTenantId() + "_snapshot.json");
        Path tempFile = cacheDirectory.resolve("tenant_" + snapshot.getTenantId() + "_snapshot.tmp");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), snapshot);
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.info("Persisted config snapshot v{} for tenant {} to local disk", snapshot.getVersion(), snapshot.getTenantId());
        } catch (Exception e) {
            log.warn("Failed to persist config snapshot to disk: {}", e.getMessage());
        }
    }

    // Loads the last good snapshot from local disk if the server is unreachable.
    public TenantConfigSnapshot load(String tenantId) {
        Path targetFile = cacheDirectory.resolve("tenant_" + tenantId + "_snapshot.json");
        if (!Files.exists(targetFile)) {
            log.debug("No local disk snapshot found for tenant: {}", tenantId);
            return null;
        }
        try {
            TenantConfigSnapshot snapshot = objectMapper.readValue(targetFile.toFile(), TenantConfigSnapshot.class);
            log.warn("Loaded offline backup snapshot v{} for tenant {} from local disk", snapshot.getVersion(), tenantId);
            return snapshot;
        } catch (Exception e) {
            log.error("Corrupted local disk snapshot for tenant {}: {}", tenantId, e.getMessage());
            return null;
        }
    }

    // Returns whether a cached snapshot file exists on disk for the given tenant.
    public boolean hasCachedSnapshot(String tenantId) {
        Path targetFile = cacheDirectory.resolve("tenant_" + tenantId + "_snapshot.json");
        return Files.exists(targetFile);
    }
}
