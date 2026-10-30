package com.moveinsync.config.server.dto;

import java.util.List;

/**
 * Detailed diff showing what configurations changed during a rollback operation.
 */
public class RollbackDiffResponse {

    private String tenantId;
    private Long fromVersion;
    private Long targetVersionRolledBackTo;
    private Long newCommittedVersion;
    private List<DiffEntry> restoredSettings;
    private List<DiffEntry> deletedSettings;
    private List<DiffEntry> reAddedSettings;

    // Default constructor.
    public RollbackDiffResponse() {}

    // Constructs a rollback diff response.
    public RollbackDiffResponse(String tenantId, Long fromVersion, Long targetVersionRolledBackTo,
                                Long newCommittedVersion, List<DiffEntry> restoredSettings,
                                List<DiffEntry> deletedSettings, List<DiffEntry> reAddedSettings) {
        this.tenantId = tenantId;
        this.fromVersion = fromVersion;
        this.targetVersionRolledBackTo = targetVersionRolledBackTo;
        this.newCommittedVersion = newCommittedVersion;
        this.restoredSettings = restoredSettings;
        this.deletedSettings = deletedSettings;
        this.reAddedSettings = reAddedSettings;
    }

    // Returns tenant ID.
    public String getTenantId() {
        return tenantId;
    }

    // Returns the version prior to rollback.
    public Long getFromVersion() {
        return fromVersion;
    }

    // Returns the historical version target that was restored.
    public Long getTargetVersionRolledBackTo() {
        return targetVersionRolledBackTo;
    }

    // Returns the newly generated forward version number.
    public Long getNewCommittedVersion() {
        return newCommittedVersion;
    }

    // Returns settings that were modified back to their prior values.
    public List<DiffEntry> getRestoredSettings() {
        return restoredSettings;
    }

    // Returns settings that were deleted because they did not exist in the target version.
    public List<DiffEntry> getDeletedSettings() {
        return deletedSettings;
    }

    // Returns settings that were re-added because they were deleted after the target version.
    public List<DiffEntry> getReAddedSettings() {
        return reAddedSettings;
    }

    /**
     * An individual setting modification in the rollback diff.
     */
    public static class DiffEntry {
        private String settingKey;
        private String level;
        private String officeId;
        private String shiftId;
        private String previousValue;
        private String restoredValue;

        // Default constructor.
        public DiffEntry() {}

        // Constructs a diff entry.
        public DiffEntry(String settingKey, String level, String officeId, String shiftId,
                         String previousValue, String restoredValue) {
            this.settingKey = settingKey;
            this.level = level;
            this.officeId = officeId;
            this.shiftId = shiftId;
            this.previousValue = previousValue;
            this.restoredValue = restoredValue;
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

        // Returns previousValue.
        public String getPreviousValue() {
            return previousValue;
        }

        // Returns restoredValue.
        public String getRestoredValue() {
            return restoredValue;
        }
    }
}
