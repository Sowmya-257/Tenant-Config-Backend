package com.moveinsync.config.server.repository;

import com.moveinsync.config.client.model.Level;
import com.moveinsync.config.server.domain.SettingOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Repository for querying and persisting configuration overrides at all levels.
 */
@Repository
public interface SettingOverrideRepository extends JpaRepository<SettingOverride, Long> {

    // Finds all active overrides configured for a given tenant.
    List<SettingOverride> findByTenantId(String tenantId);

    // Finds all overrides for a specific setting key under a tenant.
    List<SettingOverride> findByTenantIdAndSettingKey(String tenantId, String settingKey);

    // Finds a specific unique override at a given level, office, and shift.
    default Optional<SettingOverride> findExactOverride(String tenantId, String key, Level level, String officeId, String shiftId) {
        List<SettingOverride> list = findByTenantIdAndSettingKey(tenantId, key);
        return list.stream()
                .filter(o -> o.getLevel() == level &&
                        Objects.equals(o.getOfficeId(), officeId) &&
                        Objects.equals(o.getShiftId(), shiftId))
                .findFirst();
    }

    // Deletes all overrides for a tenant during complete resets.
    void deleteByTenantId(String tenantId);
}
