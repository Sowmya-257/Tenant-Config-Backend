package com.moveinsync.config.server.dto;

import com.moveinsync.config.client.model.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload representing a single item in an atomic change set batch.
 */
public class ChangeSetItemRequest {

    @NotBlank(message = "Setting key cannot be blank")
    private String settingKey;

    @NotNull(message = "Level must be specified (CLIENT, OFFICE, SHIFT)")
    private Level level;

    private String officeId;
    private String shiftId;
    private String value;
    private boolean deleted = false;

    // Default constructor for deserialization.
    public ChangeSetItemRequest() {}

    // Constructs a change set item request.
    public ChangeSetItemRequest(String settingKey, Level level, String officeId, String shiftId, String value, boolean deleted) {
        this.settingKey = settingKey;
        this.level = level;
        this.officeId = officeId;
        this.shiftId = shiftId;
        this.value = value;
        this.deleted = deleted;
    }

    // Returns setting key.
    public String getSettingKey() {
        return settingKey;
    }

    // Sets setting key.
    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    // Returns hierarchy level.
    public Level getLevel() {
        return level;
    }

    // Sets hierarchy level.
    public void setLevel(Level level) {
        this.level = level;
    }

    // Returns office ID.
    public String getOfficeId() {
        return officeId;
    }

    // Sets office ID.
    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    // Returns shift ID.
    public String getShiftId() {
        return shiftId;
    }

    // Sets shift ID.
    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    // Returns new setting value.
    public String getValue() {
        return value;
    }

    // Sets new setting value.
    public void setValue(String value) {
        this.value = value;
    }

    // Returns whether this item requests deletion of an existing override.
    public boolean isDeleted() {
        return deleted;
    }

    // Sets deletion flag.
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
