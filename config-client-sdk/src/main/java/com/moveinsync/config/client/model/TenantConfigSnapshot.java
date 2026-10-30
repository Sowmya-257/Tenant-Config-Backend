package com.moveinsync.config.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.*;

/**
 * An immutable point-in-time snapshot of all configurations for a tenant.
 */
public class TenantConfigSnapshot {

    private final String tenantId;
    private final Long version;
    private final Instant snapshotTimestamp;
    private final Map<ConfigKey, ResolvedSetting> overrides;
    private final Map<String, ResolvedSetting> globalDefaults;

    // Creates an immutable tenant configuration snapshot.
    @JsonCreator
    public TenantConfigSnapshot(@JsonProperty("tenantId") String tenantId,
                                @JsonProperty("version") Long version,
                                @JsonProperty("snapshotTimestamp") Instant snapshotTimestamp,
                                @JsonProperty("overrides") Map<ConfigKey, ResolvedSetting> overrides,
                                @JsonProperty("globalDefaults") Map<String, ResolvedSetting> globalDefaults) {
        this.tenantId = tenantId;
        this.version = version != null ? version : 0L;
        this.snapshotTimestamp = snapshotTimestamp != null ? snapshotTimestamp : Instant.now();
        this.overrides = overrides != null ? Collections.unmodifiableMap(new HashMap<>(overrides)) : Collections.emptyMap();
        this.globalDefaults = globalDefaults != null ? Collections.unmodifiableMap(new HashMap<>(globalDefaults)) : Collections.emptyMap();
    }

    // Resolves a setting following the hierarchy: SHIFT -> OFFICE -> CLIENT -> GLOBAL.
    public ResolvedSetting resolve(String key, String officeId, String shiftId) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        String cleanKey = key.trim();

        // 1. Check SHIFT level if shift is specified
        if (shiftId != null && !shiftId.trim().isEmpty() && officeId != null && !officeId.trim().isEmpty()) {
            ConfigKey shiftKey = ConfigKey.of(cleanKey, officeId.trim(), shiftId.trim());
            ResolvedSetting shiftVal = overrides.get(shiftKey);
            if (shiftVal != null) {
                return shiftVal;
            }
        }

        // 2. Check OFFICE level if office is specified
        if (officeId != null && !officeId.trim().isEmpty()) {
            ConfigKey officeKey = ConfigKey.of(cleanKey, officeId.trim());
            ResolvedSetting officeVal = overrides.get(officeKey);
            if (officeVal != null) {
                return officeVal;
            }
        }

        // 3. Check CLIENT level (tenant override)
        ConfigKey clientKey = ConfigKey.of(cleanKey);
        ResolvedSetting clientVal = overrides.get(clientKey);
        if (clientVal != null) {
            return clientVal;
        }

        // 4. Fallback to GLOBAL default
        return globalDefaults.get(cleanKey);
    }

    // Explains which levels were evaluated and which level won for a setting.
    public ExplainResult explain(String key, String officeId, String shiftId) {
        String cleanKey = key.trim();
        List<ExplainResult.LevelCandidate> candidates = new ArrayList<>();
        ResolvedSetting winner = null;

        // Inspect Shift level
        if (shiftId != null && !shiftId.trim().isEmpty() && officeId != null && !officeId.trim().isEmpty()) {
            ConfigKey shiftKey = ConfigKey.of(cleanKey, officeId.trim(), shiftId.trim());
            ResolvedSetting shiftVal = overrides.get(shiftKey);
            if (shiftVal != null) {
                winner = shiftVal;
                candidates.add(new ExplainResult.LevelCandidate(Level.SHIFT, officeId + "#" + shiftId, shiftVal.getValue(), true));
            } else {
                candidates.add(new ExplainResult.LevelCandidate(Level.SHIFT, officeId + "#" + shiftId, null, false));
            }
        }

        // Inspect Office level
        if (officeId != null && !officeId.trim().isEmpty()) {
            ConfigKey officeKey = ConfigKey.of(cleanKey, officeId.trim());
            ResolvedSetting officeVal = overrides.get(officeKey);
            boolean isWinner = (winner == null && officeVal != null);
            if (isWinner) {
                winner = officeVal;
            }
            candidates.add(new ExplainResult.LevelCandidate(Level.OFFICE, officeId, officeVal != null ? officeVal.getValue() : null, isWinner));
        }

        // Inspect Client level
        ConfigKey clientKey = ConfigKey.of(cleanKey);
        ResolvedSetting clientVal = overrides.get(clientKey);
        boolean isClientWinner = (winner == null && clientVal != null);
        if (isClientWinner) {
            winner = clientVal;
        }
        candidates.add(new ExplainResult.LevelCandidate(Level.CLIENT, tenantId, clientVal != null ? clientVal.getValue() : null, isClientWinner));

        // Inspect Global level
        ResolvedSetting globalVal = globalDefaults.get(cleanKey);
        boolean isGlobalWinner = (winner == null && globalVal != null);
        if (isGlobalWinner) {
            winner = globalVal;
        }
        candidates.add(new ExplainResult.LevelCandidate(Level.GLOBAL, "GLOBAL", globalVal != null ? globalVal.getValue() : null, isGlobalWinner));

        if (winner == null) {
            throw new SettingNotFoundException("Setting key '" + cleanKey + "' not found in tenant " + tenantId);
        }

        return new ExplainResult(cleanKey, winner.getValue(), winner.getResolvedLevel(), winner.getAuthor(), winner.getVersion(), candidates);
    }

    // Creates an updated snapshot copy after applying a delta change set.
    public TenantConfigSnapshot applyDelta(ConfigDelta delta) {
        if (delta == null || delta.getItems() == null || delta.getItems().isEmpty()) {
            return this;
        }
        Map<ConfigKey, ResolvedSetting> newOverrides = new HashMap<>(this.overrides);
        Map<String, ResolvedSetting> newGlobals = new HashMap<>(this.globalDefaults);

        for (ConfigDelta.Item item : delta.getItems()) {
            if (item.getLevel() == Level.GLOBAL) {
                if (item.isDeleted()) {
                    newGlobals.remove(item.getKey());
                } else {
                    ResolvedSetting s = new ResolvedSetting(
                            item.getKey(), item.getValue(), item.getType(), Level.GLOBAL,
                            delta.getVersion(), item.getAuthor(), item.getReason(), Instant.now()
                    );
                    newGlobals.put(item.getKey(), s);
                }
            } else {
                ConfigKey ck = new ConfigKey(item.getKey(), item.getOfficeId(), item.getShiftId());
                if (item.isDeleted()) {
                    newOverrides.remove(ck);
                } else {
                    ResolvedSetting s = new ResolvedSetting(
                            item.getKey(), item.getValue(), item.getType(), item.getLevel(),
                            delta.getVersion(), item.getAuthor(), item.getReason(), Instant.now()
                    );
                    newOverrides.put(ck, s);
                }
            }
        }

        return new TenantConfigSnapshot(this.tenantId, delta.getVersion(), Instant.now(), newOverrides, newGlobals);
    }

    // Returns the tenant id for this snapshot.
    public String getTenantId() {
        return tenantId;
    }

    // Returns the version of this snapshot.
    public Long getVersion() {
        return version;
    }

    // Returns the creation timestamp of this snapshot.
    public Instant getSnapshotTimestamp() {
        return snapshotTimestamp;
    }

    // Returns an unmodifiable view of all overrides in this snapshot.
    public Map<ConfigKey, ResolvedSetting> getOverrides() {
        return overrides;
    }

    // Returns an unmodifiable view of all global defaults in this snapshot.
    public Map<String, ResolvedSetting> getGlobalDefaults() {
        return globalDefaults;
    }
}
