package com.moveinsync.config.server.domain;

import com.moveinsync.config.client.model.SettingType;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Metadata definition and validation rules for a configurable setting.
 */
@Entity
@Table(name = "setting_definitions")
public class SettingDefinition {

    @Id
    @Column(name = "setting_key", nullable = false, unique = true, length = 120)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "setting_type", nullable = false, length = 30)
    private SettingType type;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "default_value", nullable = false, length = 500)
    private String defaultValue;

    @Column(name = "validation_rules_json", length = 1000)
    private String validationRulesJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Default constructor for JPA persistence.
    public SettingDefinition() {}

    // Constructs a setting definition with key, type, description, and default value.
    public SettingDefinition(String key, SettingType type, String description, String defaultValue, String validationRulesJson) {
        this.key = key;
        this.type = type;
        this.description = description;
        this.defaultValue = defaultValue;
        this.validationRulesJson = validationRulesJson;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Returns the setting key identifier.
    public String getKey() {
        return key;
    }

    // Sets the setting key identifier.
    public void setKey(String key) {
        this.key = key;
    }

    // Returns the data type of this setting.
    public SettingType getType() {
        return type;
    }

    // Sets the data type of this setting.
    public void setType(SettingType type) {
        this.type = type;
    }

    // Returns the human-readable description.
    public String getDescription() {
        return description;
    }

    // Sets the human-readable description.
    public void setDescription(String description) {
        this.description = description;
    }

    // Returns the system-wide global default value.
    public String getDefaultValue() {
        return defaultValue;
    }

    // Sets the system-wide global default value.
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    // Returns the validation rules stored as JSON.
    public String getValidationRulesJson() {
        return validationRulesJson;
    }

    // Sets the validation rules JSON string.
    public void setValidationRulesJson(String validationRulesJson) {
        this.validationRulesJson = validationRulesJson;
    }

    // Returns the creation timestamp.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Returns the last update timestamp.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Updates the last update timestamp before persisting changes.
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
