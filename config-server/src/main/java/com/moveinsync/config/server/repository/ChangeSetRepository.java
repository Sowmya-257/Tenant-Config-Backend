package com.moveinsync.config.server.repository;

import com.moveinsync.config.server.domain.ChangeSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for storing and retrieving versioned change sets and audit logs.
 */
@Repository
public interface ChangeSetRepository extends JpaRepository<ChangeSet, Long> {

    // Finds a change set by tenant and exact version number.
    Optional<ChangeSet> findByTenantIdAndVersion(String tenantId, Long version);

    // Retrieves all change sets after a specified version for delta sync.
    List<ChangeSet> findByTenantIdAndVersionGreaterThanOrderByVersionAsc(String tenantId, Long sinceVersion);

    // Retrieves all change sets for a tenant ordered newest to oldest.
    List<ChangeSet> findByTenantIdOrderByVersionDesc(String tenantId);

    // Retrieves change sets between two versions inclusive for diff calculations.
    List<ChangeSet> findByTenantIdAndVersionGreaterThanEqualAndVersionLessThanEqualOrderByVersionAsc(
            String tenantId, Long fromVersion, Long toVersion
    );
}
