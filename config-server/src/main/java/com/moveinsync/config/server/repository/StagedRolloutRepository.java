package com.moveinsync.config.server.repository;

import com.moveinsync.config.server.domain.StagedRollout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing staged rollout lifecycles and target office configurations.
 */
@Repository
public interface StagedRolloutRepository extends JpaRepository<StagedRollout, Long> {

    // Finds rollouts matching a specific tenant and lifecycle status.
    List<StagedRollout> findByTenantIdAndStatus(String tenantId, StagedRollout.RolloutStatus status);

    // Finds all rollouts for a given setting key in a tenant.
    List<StagedRollout> findByTenantIdAndSettingKey(String tenantId, String settingKey);
}
