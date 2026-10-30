package com.moveinsync.config.server.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Tracks staged canary rollouts targeted to specific pilot offices before full promotion.
 */
@Entity
@Table(name = "staged_rollouts")
public class StagedRollout {

    public enum RolloutStatus {
        CANARY_ACTIVE,
        PROMOTED,
        ABORTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "setting_key", nullable = false, length = 120)
    private String settingKey;

    @Column(name = "canary_value", nullable = false, length = 1000)
    private String canaryValue;

    @Column(name = "target_offices_csv", nullable = false, length = 1000)
    private String targetOfficesCsv;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RolloutStatus status = RolloutStatus.CANARY_ACTIVE;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "canary_version")
    private Long canaryVersion;

    @Column(name = "promoted_version")
    private Long promotedVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Default constructor for JPA.
    public StagedRollout() {}

    // Constructs a staged rollout with target pilot offices and canary value.
    public StagedRollout(String tenantId, String settingKey, String canaryValue,
                         String targetOfficesCsv, String author, String reason) {
        this.tenantId = tenantId;
        this.settingKey = settingKey;
        this.canaryValue = canaryValue;
        this.targetOfficesCsv = targetOfficesCsv;
        this.author = author;
        this.reason = reason;
        this.status = RolloutStatus.CANARY_ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Returns rollout record ID.
    public Long getId() {
        return id;
    }

    // Returns tenant identifier.
    public String getTenantId() {
        return tenantId;
    }

    // Returns the setting key being rolled out.
    public String getSettingKey() {
        return settingKey;
    }

    // Returns the canary value tested on pilot offices.
    public String getCanaryValue() {
        return canaryValue;
    }

    // Returns comma-separated list of target pilot offices.
    public String getTargetOfficesCsv() {
        return targetOfficesCsv;
    }

    // Returns the current status of the rollout (CANARY_ACTIVE, PROMOTED, ABORTED).
    public RolloutStatus getStatus() {
        return status;
    }

    // Sets the rollout status.
    public void setStatus(RolloutStatus status) {
        this.status = status;
    }

    // Returns the author who created the rollout.
    public String getAuthor() {
        return author;
    }

    // Returns the business reason for the rollout.
    public String getReason() {
        return reason;
    }

    // Returns the version number when canary was activated.
    public Long getCanaryVersion() {
        return canaryVersion;
    }

    // Sets the canary version.
    public void setCanaryVersion(Long canaryVersion) {
        this.canaryVersion = canaryVersion;
    }

    // Returns the version number when promoted globally.
    public Long getPromotedVersion() {
        return promotedVersion;
    }

    // Sets the promoted version.
    public void setPromotedVersion(Long promotedVersion) {
        this.promotedVersion = promotedVersion;
    }

    // Returns creation timestamp.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Returns last update timestamp.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Updates timestamp before persisting modifications.
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
