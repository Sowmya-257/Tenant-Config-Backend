package com.moveinsync.config.server.service;

import com.moveinsync.config.client.model.ConfigKey;
import com.moveinsync.config.client.model.Level;
import com.moveinsync.config.server.domain.ChangeSet;
import com.moveinsync.config.server.domain.ChangeSetItem;
import com.moveinsync.config.server.domain.SettingOverride;
import com.moveinsync.config.server.domain.TenantVersion;
import com.moveinsync.config.server.dto.ChangeSetItemRequest;
import com.moveinsync.config.server.dto.ChangeSetRequest;
import com.moveinsync.config.server.dto.ChangeSetResponse;
import com.moveinsync.config.server.dto.RollbackDiffResponse;
import com.moveinsync.config.server.repository.ChangeSetRepository;
import com.moveinsync.config.server.repository.SettingOverrideRepository;
import com.moveinsync.config.server.repository.TenantVersionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Executes instantaneous configuration rollbacks to any prior version with computed diffs.
 */
@Service
public class RollbackService {

    private static final Logger log = LoggerFactory.getLogger(RollbackService.class);

    private final ChangeSetRepository changeSetRepository;
    private final SettingOverrideRepository overrideRepository;
    private final TenantVersionRepository versionRepository;
    private final ChangeSetService changeSetService;
    private final Counter rollbackCounter;

    // Injects required repositories, change set service, and rollback telemetry counter.
    public RollbackService(ChangeSetRepository changeSetRepository,
                           SettingOverrideRepository overrideRepository,
                           TenantVersionRepository versionRepository,
                           ChangeSetService changeSetService,
                           MeterRegistry meterRegistry) {
        this.changeSetRepository = changeSetRepository;
        this.overrideRepository = overrideRepository;
        this.versionRepository = versionRepository;
        this.changeSetService = changeSetService;
        this.rollbackCounter = meterRegistry.counter("config_rollbacks_total");
    }

    // Rolls back tenant configuration to any historical version V by committing a new forward version.
    @Transactional
    public RollbackDiffResponse rollbackToVersion(String tenantId, Long targetVersion, String author, String reason) {
        Long currentVersion = versionRepository.findById(tenantId)
                .map(TenantVersion::getCurrentVersion)
                .orElse(0L);

        if (targetVersion == null || targetVersion < 0 || targetVersion >= currentVersion) {
            throw new IllegalArgumentException("Target rollback version must be less than current version " + currentVersion);
        }

        // Reconstruct the exact state of overrides at targetVersion
        Map<ConfigKey, OverrideState> targetState = reconstructStateAtVersion(tenantId, targetVersion);

        // Fetch current active state
        List<SettingOverride> currentOverrides = overrideRepository.findByTenantId(tenantId);
        Map<ConfigKey, SettingOverride> currentState = new HashMap<>();
        for (SettingOverride o : currentOverrides) {
            currentState.put(new ConfigKey(o.getSettingKey(), o.getOfficeId(), o.getShiftId()), o);
        }

        List<ChangeSetItemRequest> rollbackItems = new ArrayList<>();
        List<RollbackDiffResponse.DiffEntry> restored = new ArrayList<>();
        List<RollbackDiffResponse.DiffEntry> deleted = new ArrayList<>();
        List<RollbackDiffResponse.DiffEntry> reAdded = new ArrayList<>();

        // Check for settings that exist now but didn't exist at targetVersion -> Delete them
        for (Map.Entry<ConfigKey, SettingOverride> entry : currentState.entrySet()) {
            ConfigKey key = entry.getKey();
            SettingOverride current = entry.getValue();
            if (!targetState.containsKey(key)) {
                rollbackItems.add(new ChangeSetItemRequest(
                        current.getSettingKey(), current.getLevel(), current.getOfficeId(), current.getShiftId(), null, true
                ));
                deleted.add(new RollbackDiffResponse.DiffEntry(
                        current.getSettingKey(), current.getLevel().name(), current.getOfficeId(), current.getShiftId(),
                        current.getSettingValue(), null
                ));
            }
        }

        // Check for settings in targetVersion
        for (Map.Entry<ConfigKey, OverrideState> entry : targetState.entrySet()) {
            ConfigKey key = entry.getKey();
            OverrideState target = entry.getValue();
            SettingOverride current = currentState.get(key);

            if (current == null) {
                // Was deleted after targetVersion -> Re-add it
                rollbackItems.add(new ChangeSetItemRequest(
                        target.key, target.level, target.officeId, target.shiftId, target.value, false
                ));
                reAdded.add(new RollbackDiffResponse.DiffEntry(
                        target.key, target.level.name(), target.officeId, target.shiftId, null, target.value
                ));
            } else if (!Objects.equals(current.getSettingValue(), target.value)) {
                // Modified after targetVersion -> Restore target value
                rollbackItems.add(new ChangeSetItemRequest(
                        target.key, target.level, target.officeId, target.shiftId, target.value, false
                ));
                restored.add(new RollbackDiffResponse.DiffEntry(
                        target.key, target.level.name(), target.officeId, target.shiftId, current.getSettingValue(), target.value
                ));
            }
        }

        if (rollbackItems.isEmpty()) {
            return new RollbackDiffResponse(tenantId, currentVersion, targetVersion, currentVersion, List.of(), List.of(), List.of());
        }

        // Apply as a new forward atomic version
        String rollbackReason = "Rollback to v" + targetVersion + ": " + (reason != null ? reason : "Reverting configuration");
        ChangeSetResponse csResponse = changeSetService.applyChangeSet(
                tenantId,
                new ChangeSetRequest(author != null ? author : "system_rollback", rollbackReason, rollbackItems)
        );

        rollbackCounter.increment();
        log.info("Completed instant rollback for tenant {} from v{} to historical state of v{}, committed as new v{}",
                tenantId, currentVersion, targetVersion, csResponse.getVersion());

        return new RollbackDiffResponse(
                tenantId, currentVersion, targetVersion, csResponse.getVersion(), restored, deleted, reAdded
        );
    }

    // Reconstructs the exact override map at a given version by replaying historical change sets.
    private Map<ConfigKey, OverrideState> reconstructStateAtVersion(String tenantId, Long targetVersion) {
        List<ChangeSet> history = changeSetRepository
                .findByTenantIdAndVersionGreaterThanEqualAndVersionLessThanEqualOrderByVersionAsc(tenantId, 1L, targetVersion);

        Map<ConfigKey, OverrideState> state = new HashMap<>();
        for (ChangeSet cs : history) {
            for (ChangeSetItem item : cs.getItems()) {
                ConfigKey key = new ConfigKey(item.getSettingKey(), item.getOfficeId(), item.getShiftId());
                if (item.isDeleted()) {
                    state.remove(key);
                } else {
                    state.put(key, new OverrideState(item.getSettingKey(), item.getLevel(), item.getOfficeId(), item.getShiftId(), item.getNewValue()));
                }
            }
        }
        return state;
    }

    // Internal helper holding point-in-time override state.
    private static class OverrideState {
        final String key;
        final Level level;
        final String officeId;
        final String shiftId;
        final String value;

        OverrideState(String key, Level level, String officeId, String shiftId, String value) {
            this.key = key;
            this.level = level;
            this.officeId = officeId;
            this.shiftId = shiftId;
            this.value = value;
        }
    }
}
