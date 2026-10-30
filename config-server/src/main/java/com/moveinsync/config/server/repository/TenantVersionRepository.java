package com.moveinsync.config.server.repository;

import com.moveinsync.config.server.domain.TenantVersion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for reading and atomically advancing tenant version counters.
 */
@Repository
public interface TenantVersionRepository extends JpaRepository<TenantVersion, String> {

    // Retrieves tenant version record with a pessimistic write lock for strictly monotonic increment.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM TenantVersion v WHERE v.tenantId = :tenantId")
    Optional<TenantVersion> findByTenantIdForUpdate(@Param("tenantId") String tenantId);
}
