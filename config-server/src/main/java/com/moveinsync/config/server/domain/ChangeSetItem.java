package com.moveinsync.config.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.moveinsync.config.client.model.Level;
import com.moveinsync.config.client.model.SettingType;
import jakarta.persistence.*;

/**
 * An individual setting change within an atomic change set.
 */
@Entity
@Table(name = "change_set_items")
public class ChangeSetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_set_id", nullable = false)
    @JsonIgnore
    private ChangeSet changeSet;

    @Column(name = "setting_key", nullable = false, length = 120)
    private String settingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_name", nullable = false, length = 20)
    private Level level;

    @Column(name = "office_id", length = 100)
    private String officeId;

    @Column(name = "shift_id", length = 100)
    private String shiftId;

    @Column(name = "old_value", length = 1000)
    private String oldValue;

    @Column(name = "new_value", length = 1000)
    private String newValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "setting_type", length = 30)
    private SettingType type;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    // Default constructor for JPA.
    public ChangeSetItem() {}

    // Constructs a change set item detailing a specific setting update.
    public ChangeSetItem(String settingKey, Level level, String officeId, String shiftId,
                         String oldValue, String newValue, SettingType type, boolean deleted) {
        this.settingKey = settingKey;
        this.level = level;
        this.officeId = officeId;
        this.shiftId = shiftId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.type = type != null ? type : SettingType.STRING;
        this.deleted = deleted;
    }

    // Returns the ID.
    public Long getId() {
        return id;
    }

    // Returns the parent change set.
    public ChangeSet getChangeSet() {
        return changeSet;
    }

    // Links this item to a parent change set.
    public void setChangeSet(ChangeSet changeSet) {
        this.changeSet = changeSet;
    }

    // Returns the setting key.
    public String getSettingKey() {
        return settingKey;
    }

    // Returns the hierarchy level.
    public Level getLevel() {
        return level;
    }

    // Returns the office ID.
    public String getOfficeId() {
        return officeId;
    }

    // Returns the shift ID.
    public String getShiftId() {
        return shiftId;
    }

    // Returns the previous value prior to this change.
    public String getOldValue() {
        return oldValue;
    }

    // Returns the new value being applied.
    public String getNewValue() {
        return newValue;
    }

    // Returns the data type.
    public SettingType getType() {
        return type;
    }

    // Returns whether this item represents an override deletion.
    public boolean isDeleted() {
        return deleted;
    }
}
