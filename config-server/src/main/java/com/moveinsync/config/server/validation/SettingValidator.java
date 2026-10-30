package com.moveinsync.config.server.validation;

import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.server.domain.SettingDefinition;

/**
 * Strategy interface for polymorphic validation of setting values against defined schemas.
 */
public interface SettingValidator {

    // Returns whether this validator supports the given setting type.
    boolean supports(SettingType type);

    // Validates the raw value against definition constraints, throwing InvalidConfigException on error.
    void validate(String key, String value, SettingDefinition definition);
}
