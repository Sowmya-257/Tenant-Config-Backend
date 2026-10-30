package com.moveinsync.config.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

/**
 * Encapsulates the resolved value of a setting along with its origin metadata.
 */
public class ResolvedSetting {

    private final String key;
    private final String value;
    private final SettingType type;
    private final Level resolvedLevel;
    private final Long version;
    private final String author;
    private final String reason;
    private final Instant updatedAt;

    // Constructs a resolved setting with its resolved level and audit history.
    @JsonCreator
    public ResolvedSetting(@JsonProperty("key") String key,
                           @JsonProperty("value") String value,
                           @JsonProperty("type") SettingType type,
                           @JsonProperty("resolvedLevel") Level resolvedLevel,
                           @JsonProperty("version") Long version,
                           @JsonProperty("author") String author,
                           @JsonProperty("reason") String reason,
                           @JsonProperty("updatedAt") Instant updatedAt) {
        this.key = key;
        this.value = value;
        this.type = type != null ? type : SettingType.STRING;
        this.resolvedLevel = resolvedLevel != null ? resolvedLevel : Level.GLOBAL;
        this.version = version != null ? version : 0L;
        this.author = author != null ? author : "system";
        this.reason = reason;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    // Returns the setting key.
    public String getKey() {
        return key;
    }

    // Returns the raw string value of the setting.
    public String getValue() {
        return value;
    }

    // Returns the data type of the setting.
    public SettingType getType() {
        return type;
    }

    // Returns the hierarchy level that supplied this winning value.
    public Level getResolvedLevel() {
        return resolvedLevel;
    }

    // Returns the change set version number when this was written.
    public Long getVersion() {
        return version;
    }

    // Returns the author who published this setting value.
    public String getAuthor() {
        return author;
    }

    // Returns the business reason recorded for this change.
    public String getReason() {
        return reason;
    }

    // Returns the timestamp when this setting was updated.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Checks equality between two resolved settings.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolvedSetting that)) return false;
        return Objects.equals(key, that.key) &&
                Objects.equals(value, that.value) &&
                type == that.type &&
                resolvedLevel == that.resolvedLevel &&
                Objects.equals(version, that.version);
    }

    // Computes hash code for this resolved setting.
    @Override
    public int hashCode() {
        return Objects.hash(key, value, type, resolvedLevel, version);
    }

    // Returns a readable string summarizing the resolved setting.
    @Override
    public String toString() {
        return "ResolvedSetting{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", type=" + type +
                ", resolvedLevel=" + resolvedLevel +
                ", version=" + version +
                '}';
    }
}
