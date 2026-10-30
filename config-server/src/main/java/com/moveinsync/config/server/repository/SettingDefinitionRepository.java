package com.moveinsync.config.server.repository;

import com.moveinsync.config.server.domain.SettingDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing setting definitions and global defaults.
 */
@Repository
public interface SettingDefinitionRepository extends JpaRepository<SettingDefinition, String> {
}
