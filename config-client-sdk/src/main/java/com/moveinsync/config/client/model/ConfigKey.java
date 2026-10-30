package com.moveinsync.config.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Unique identifier for a setting scoped to an optional office and shift.
 */
public class ConfigKey {

    private final String key;
    private final String officeId;
    private final String shiftId;

    // Creates a config lookup key with setting name, office, and shift.
    public ConfigKey(String key, String officeId, String shiftId) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Config key cannot be null or empty");
        }
        this.key = key.trim();
        this.officeId = (officeId == null || officeId.trim().isEmpty()) ? null : officeId.trim();
        this.shiftId = (shiftId == null || shiftId.trim().isEmpty()) ? null : shiftId.trim();
    }

    // Parses a serialized string key format back into a ConfigKey instance for Jackson.
    @JsonCreator
    public static ConfigKey parse(String serialized) {
        if (serialized == null || serialized.trim().isEmpty()) {
            throw new IllegalArgumentException("Serialized config key cannot be empty");
        }
        String clean = serialized.trim();
        String k;
        String o = null;
        String s = null;

        if (clean.contains("#")) {
            String[] shiftSplit = clean.split("#", 2);
            s = shiftSplit[1].trim();
            clean = shiftSplit[0].trim();
        }
        if (clean.contains("@")) {
            String[] officeSplit = clean.split("@", 2);
            k = officeSplit[0].trim();
            o = officeSplit[1].trim();
        } else {
            k = clean;
        }
        return new ConfigKey(k, o, s);
    }

    // Creates a tenant-level config key without office or shift scope.
    public static ConfigKey of(String key) {
        return new ConfigKey(key, null, null);
    }

    // Creates an office-level config key without a shift scope.
    public static ConfigKey of(String key, String officeId) {
        return new ConfigKey(key, officeId, null);
    }

    // Creates a fully scoped config key with both office and shift.
    public static ConfigKey of(String key, String officeId, String shiftId) {
        return new ConfigKey(key, officeId, shiftId);
    }

    // Returns the base setting name.
    public String getKey() {
        return key;
    }

    // Returns the associated office id or null if not office-specific.
    public String getOfficeId() {
        return officeId;
    }

    // Returns the associated shift id or null if not shift-specific.
    public String getShiftId() {
        return shiftId;
    }

    // Checks equality by matching setting key, office, and shift.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigKey configKey)) return false;
        return Objects.equals(key, configKey.key) &&
                Objects.equals(officeId, configKey.officeId) &&
                Objects.equals(shiftId, configKey.shiftId);
    }

    // Computes hash code for reliable map caching.
    @Override
    public int hashCode() {
        return Objects.hash(key, officeId, shiftId);
    }

    // Formats the key into a clear string representation.
    @com.fasterxml.jackson.annotation.JsonValue
    @Override
    public String toString() {
        return key + (officeId != null ? "@" + officeId : "") + (shiftId != null ? "#" + shiftId : "");
    }
}
