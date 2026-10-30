package com.moveinsync.config.server.domain;

import com.moveinsync.config.client.model.Level;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Stores a specific configuration override for a tenant at CLIENT, OFFICE, or SHIFT level.
 */
@Entity
@Table(name = "setting_overrides", indexes = {
        @Index(name = "idx_tenant_key", columnList = "tenant_id, setting_key"),
        @Index(name = "idx_tenant_level", columnList = "tenant_id, level_name")
})
public class SettingOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_name", nullable = false, length = 20)
    private Level level;

    @Column(name = "office_id", length = 100)
    private String officeId;

    @Column(name = "shift_id", length = 100)
    private String shiftId;

    @Column(name = "setting_key", nullable = false, length = 120)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 1000)
    private String settingValue;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Default constructor for JPA persistence.
    public SettingOverride() {}

    // Constructs a setting override for a specific level, office, and shift.
    public SettingOverride(String tenantId, Level level, String officeId, String shiftId,
                           String settingKey, String settingValue, Long version,
                           String author, String reason) {
        this.tenantId = tenantId;
        this.level = level;
        this.officeId = officeId;
        this.shiftId = shiftId;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.version = version;
        this.author = author;
        this.reason = reason;
        this.updatedAt = Instant.now();
    }

    // Returns the primary key of this override.
    public Long getId() {
        return id;
    }

    // Returns the tenant identifier.
    public String getTenantId() {
        return tenantId;
    }

    // Sets the tenant identifier.
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    // Returns the hierarchy level of this override.
    public Level getLevel() {
        return level;
    }

    // Sets the hierarchy level of this override.
    public void setLevel(Level level) {
        this.level = level;
    }

    // Returns the office ID or null if client-wide.
    public String getOfficeId() {
        return officeId;
    }

    // Sets the office ID.
    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    // Returns the shift ID or null if office-wide.
    public String getShiftId() {
        return shiftId;
    }

    // Sets the shift ID.
    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    // Returns the setting key.
    public String getSettingKey() {
        return settingKey;
    }

    // Sets the setting key.
    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    // Returns the configured override value.
    public String getSettingValue() {
        return settingValue;
    }

    // Sets the configured override value.
    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    // Returns the version number when this override was created.
    public Long getVersion() {
        return version;
    }

    // Sets the version number.
    public void setVersion(Long version) {
        this.version = version;
    }

    // Returns the author of this change.
    public String getAuthor() {
        return author;
    }

    // Sets the author of this change.
    public void setAuthor(String author) {
        this.author = author;
    }

    // Returns the business reason for this change.
    public String getReason() {
        return reason;
    }

    // Sets the business reason.
    public void setReason(String reason) {
        this.reason = reason;
    }

    // Returns the last update timestamp.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Updates the timestamp before database update.
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
