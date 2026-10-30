package com.moveinsync.config.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Tracks the current strictly monotonic version counter for a tenant.
 */
@Entity
@Table(name = "tenant_versions")
public class TenantVersion {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "current_version", nullable = false)
    private Long currentVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Default constructor for JPA.
    public TenantVersion() {}

    // Constructs a tenant version tracker starting at a specified version.
    public TenantVersion(String tenantId, Long currentVersion) {
        this.tenantId = tenantId;
        this.currentVersion = currentVersion != null ? currentVersion : 0L;
        this.updatedAt = Instant.now();
    }

    // Returns the tenant identifier.
    public String getTenantId() {
        return tenantId;
    }

    // Returns the current monotonic version number.
    public Long getCurrentVersion() {
        return currentVersion;
    }

    // Sets the current version number.
    public void setCurrentVersion(Long currentVersion) {
        this.currentVersion = currentVersion;
    }

    // Increments the version number by one and updates timestamp.
    public Long increment() {
        this.currentVersion++;
        this.updatedAt = Instant.now();
        return this.currentVersion;
    }

    // Returns the last update timestamp.
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
