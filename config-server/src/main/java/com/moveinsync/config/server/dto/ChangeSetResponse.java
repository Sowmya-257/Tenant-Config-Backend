package com.moveinsync.config.server.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response payload confirming committed atomic change set version and applied items.
 */
public class ChangeSetResponse {

    private Long version;
    private String tenantId;
    private String author;
    private String reason;
    private Instant createdAt;
    private int itemsCount;
    private List<ChangeSetItemDto> items;

    // Default constructor.
    public ChangeSetResponse() {}

    // Constructs a change set response.
    public ChangeSetResponse(Long version, String tenantId, String author, String reason,
                             Instant createdAt, int itemsCount, List<ChangeSetItemDto> items) {
        this.version = version;
        this.tenantId = tenantId;
        this.author = author;
        this.reason = reason;
        this.createdAt = createdAt;
        this.itemsCount = itemsCount;
        this.items = items;
    }

    // Returns version.
    public Long getVersion() {
        return version;
    }

    // Returns tenantId.
    public String getTenantId() {
        return tenantId;
    }

    // Returns author.
    public String getAuthor() {
        return author;
    }

    // Returns reason.
    public String getReason() {
        return reason;
    }

    // Returns creation timestamp.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Returns items count.
    public int getItemsCount() {
        return itemsCount;
    }

    // Returns items details.
    public List<ChangeSetItemDto> getItems() {
        return items;
    }

    /**
     * DTO describing an applied change item.
     */
    public static class ChangeSetItemDto {
        private String settingKey;
        private String level;
        private String officeId;
        private String shiftId;
        private String oldValue;
        private String newValue;
        private boolean deleted;

        // Default constructor.
        public ChangeSetItemDto() {}

        // Constructs a change item DTO.
        public ChangeSetItemDto(String settingKey, String level, String officeId, String shiftId,
                                String oldValue, String newValue, boolean deleted) {
            this.settingKey = settingKey;
            this.level = level;
            this.officeId = officeId;
            this.shiftId = shiftId;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.deleted = deleted;
        }

        // Returns settingKey.
        public String getSettingKey() {
            return settingKey;
        }

        // Returns level.
        public String getLevel() {
            return level;
        }

        // Returns officeId.
        public String getOfficeId() {
            return officeId;
        }

        // Returns shiftId.
        public String getShiftId() {
            return shiftId;
        }

        // Returns oldValue.
        public String getOldValue() {
            return oldValue;
        }

        // Returns newValue.
        public String getNewValue() {
            return newValue;
        }

        // Returns deleted flag.
        public boolean isDeleted() {
            return deleted;
        }
    }
}
