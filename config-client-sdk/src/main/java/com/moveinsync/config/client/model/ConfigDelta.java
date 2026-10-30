package com.moveinsync.config.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Represents a discrete versioned delta change set broadcast to clients.
 */
public class ConfigDelta {

    private final Long version;
    private final String tenantId;
    private final String author;
    private final String reason;
    private final List<Item> items;

    // Constructs a config delta with version, author, and changed items.
    @JsonCreator
    public ConfigDelta(@JsonProperty("version") Long version,
                       @JsonProperty("tenantId") String tenantId,
                       @JsonProperty("author") String author,
                       @JsonProperty("reason") String reason,
                       @JsonProperty("items") List<Item> items) {
        this.version = version;
        this.tenantId = tenantId;
        this.author = author;
        this.reason = reason;
        this.items = items != null ? Collections.unmodifiableList(items) : Collections.emptyList();
    }

    // Returns the version number of this delta.
    public Long getVersion() {
        return version;
    }

    // Returns the target tenant for this change set.
    public String getTenantId() {
        return tenantId;
    }

    // Returns the author who submitted this change.
    public String getAuthor() {
        return author;
    }

    // Returns the business reason for this change.
    public String getReason() {
        return reason;
    }

    // Returns the list of changed setting items in this delta.
    public List<Item> getItems() {
        return items;
    }

    /**
     * An individual setting change inside an atomic delta change set.
     */
    public static class Item {
        private final String key;
        private final Level level;
        private final String officeId;
        private final String shiftId;
        private final String value;
        private final SettingType type;
        private final boolean deleted;
        private final String author;
        private final String reason;

        // Constructs a single change item in a delta.
        @JsonCreator
        public Item(@JsonProperty("key") String key,
                    @JsonProperty("level") Level level,
                    @JsonProperty("officeId") String officeId,
                    @JsonProperty("shiftId") String shiftId,
                    @JsonProperty("value") String value,
                    @JsonProperty("type") SettingType type,
                    @JsonProperty("deleted") boolean deleted,
                    @JsonProperty("author") String author,
                    @JsonProperty("reason") String reason) {
            this.key = key;
            this.level = level != null ? level : Level.CLIENT;
            this.officeId = officeId;
            this.shiftId = shiftId;
            this.value = value;
            this.type = type != null ? type : SettingType.STRING;
            this.deleted = deleted;
            this.author = author;
            this.reason = reason;
        }

        // Returns the setting key.
        public String getKey() {
            return key;
        }

        // Returns the hierarchy level of the override.
        public Level getLevel() {
            return level;
        }

        // Returns the target office or null.
        public String getOfficeId() {
            return officeId;
        }

        // Returns the target shift or null.
        public String getShiftId() {
            return shiftId;
        }

        // Returns the updated setting value.
        public String getValue() {
            return value;
        }

        // Returns the setting data type.
        public SettingType getType() {
            return type;
        }

        // Returns whether this item represents an override deletion.
        public boolean isDeleted() {
            return deleted;
        }

        // Returns the author of this change.
        public String getAuthor() {
            return author;
        }

        // Returns the reason for this change.
        public String getReason() {
            return reason;
        }
    }
}
