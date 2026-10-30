package com.moveinsync.config.server.service;

import com.moveinsync.config.client.model.ConfigDelta;
import com.moveinsync.config.client.model.TenantConfigSnapshot;
import com.moveinsync.config.server.domain.ChangeSet;
import com.moveinsync.config.server.domain.ChangeSetItem;
import com.moveinsync.config.server.domain.TenantVersion;
import com.moveinsync.config.server.repository.ChangeSetRepository;
import com.moveinsync.config.server.repository.TenantVersionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles delta synchronizations and snapshot downloads for reconnecting clients.
 */
@Service
@Transactional(readOnly = true)
public class SyncService {

    private final TenantVersionRepository versionRepository;
    private final ChangeSetRepository changeSetRepository;
    private final HierarchyResolutionService resolutionService;
    private final Counter syncRequestsCounter;

    // Injects repositories, resolution engine, and sync telemetry counters.
    public SyncService(TenantVersionRepository versionRepository,
                       ChangeSetRepository changeSetRepository,
                       HierarchyResolutionService resolutionService,
                       MeterRegistry meterRegistry) {
        this.versionRepository = versionRepository;
        this.changeSetRepository = changeSetRepository;
        this.resolutionService = resolutionService;
        this.syncRequestsCounter = meterRegistry.counter("config_sync_requests_total");
    }

    // Returns the latest monotonic version number for a tenant.
    public Long getLatestVersion(String tenantId) {
        syncRequestsCounter.increment();
        return versionRepository.findById(tenantId)
                .map(TenantVersion::getCurrentVersion)
                .orElse(0L);
    }

    // Returns a complete full snapshot of all active configurations for a tenant.
    public TenantConfigSnapshot getFullSnapshot(String tenantId) {
        syncRequestsCounter.increment();
        return resolutionService.buildTenantSnapshot(tenantId);
    }

    // Returns only the change deltas that occurred since the client's last known version.
    public List<ConfigDelta> getDeltasSince(String tenantId, Long sinceVersion) {
        syncRequestsCounter.increment();
        List<ChangeSet> missedSets = changeSetRepository
                .findByTenantIdAndVersionGreaterThanOrderByVersionAsc(tenantId, sinceVersion != null ? sinceVersion : 0L);

        List<ConfigDelta> result = new ArrayList<>();
        for (ChangeSet cs : missedSets) {
            List<ConfigDelta.Item> deltaItems = new ArrayList<>();
            for (ChangeSetItem item : cs.getItems()) {
                deltaItems.add(new ConfigDelta.Item(
                        item.getSettingKey(), item.getLevel(), item.getOfficeId(), item.getShiftId(),
                        item.getNewValue(), item.getType(), item.isDeleted(), cs.getAuthor(), cs.getReason()
                ));
            }
            result.add(new ConfigDelta(cs.getVersion(), cs.getTenantId(), cs.getAuthor(), cs.getReason(), deltaItems));
        }
        return result;
    }
}
