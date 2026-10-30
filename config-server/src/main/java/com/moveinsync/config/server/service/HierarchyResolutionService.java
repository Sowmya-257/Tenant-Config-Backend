package com.moveinsync.config.server.service;

import com.moveinsync.config.client.model.*;
import com.moveinsync.config.server.domain.SettingDefinition;
import com.moveinsync.config.server.domain.SettingOverride;
import com.moveinsync.config.server.domain.TenantVersion;
import com.moveinsync.config.server.exception.ResourceNotFoundException;
import com.moveinsync.config.server.repository.SettingDefinitionRepository;
import com.moveinsync.config.server.repository.SettingOverrideRepository;
import com.moveinsync.config.server.repository.TenantVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Resolves setting values using hierarchical precedence: SHIFT -> OFFICE -> CLIENT -> GLOBAL.
 */
@Service
@Transactional(readOnly = true)
public class HierarchyResolutionService {

    private final SettingDefinitionRepository definitionRepository;
    private final SettingOverrideRepository overrideRepository;
    private final TenantVersionRepository versionRepository;

    // Injects required repositories for definition and override lookups.
    public HierarchyResolutionService(SettingDefinitionRepository definitionRepository,
                                      SettingOverrideRepository overrideRepository,
                                      TenantVersionRepository versionRepository) {
        this.definitionRepository = definitionRepository;
        this.overrideRepository = overrideRepository;
        this.versionRepository = versionRepository;
    }

    // Resolves a single setting following the 4-level hierarchy rule.
    public ResolvedSetting resolve(String tenantId, String key, String officeId, String shiftId) {
        TenantConfigSnapshot snapshot = buildTenantSnapshot(tenantId);
        ResolvedSetting resolved = snapshot.resolve(key, officeId, shiftId);
        if (resolved == null) {
            throw new ResourceNotFoundException("Setting '" + key + "' not found for tenant " + tenantId);
        }
        return resolved;
    }

    // Explains the hierarchical evaluation steps and winning level for a setting.
    public ExplainResult explain(String tenantId, String key, String officeId, String shiftId) {
        TenantConfigSnapshot snapshot = buildTenantSnapshot(tenantId);
        return snapshot.explain(key, officeId, shiftId);
    }

    // Constructs a complete immutable point-in-time snapshot for a given tenant.
    public TenantConfigSnapshot buildTenantSnapshot(String tenantId) {
        Long currentVersion = versionRepository.findById(tenantId)
                .map(TenantVersion::getCurrentVersion)
                .orElse(0L);

        List<SettingDefinition> definitions = definitionRepository.findAll();
        Map<String, ResolvedSetting> globalDefaults = new HashMap<>();
        for (SettingDefinition def : definitions) {
            globalDefaults.put(def.getKey(), new ResolvedSetting(
                    def.getKey(), def.getDefaultValue(), def.getType(), Level.GLOBAL,
                    0L, "system", "Global default definition", def.getUpdatedAt()
            ));
        }

        List<SettingOverride> overrides = overrideRepository.findByTenantId(tenantId);
        Map<ConfigKey, ResolvedSetting> overrideMap = new HashMap<>();
        for (SettingOverride o : overrides) {
            ConfigKey ck = new ConfigKey(o.getSettingKey(), o.getOfficeId(), o.getShiftId());
            SettingType type = definitions.stream()
                    .filter(d -> d.getKey().equalsIgnoreCase(o.getSettingKey()))
                    .findFirst()
                    .map(SettingDefinition::getType)
                    .orElse(SettingType.STRING);

            overrideMap.put(ck, new ResolvedSetting(
                    o.getSettingKey(), o.getSettingValue(), type, o.getLevel(),
                    o.getVersion(), o.getAuthor(), o.getReason(), o.getUpdatedAt()
            ));
        }

        return new TenantConfigSnapshot(tenantId, currentVersion, Instant.now(), overrideMap, globalDefaults);
    }
}
