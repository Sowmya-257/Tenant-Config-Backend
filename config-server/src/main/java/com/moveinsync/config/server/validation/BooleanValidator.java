package com.moveinsync.config.server.validation;

import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.server.domain.SettingDefinition;
import com.moveinsync.config.server.exception.InvalidConfigException;
import org.springframework.stereotype.Component;

/**
 * Validates boolean flags ensuring strict true/false values.
 */
@Component
public class BooleanValidator implements SettingValidator {

    // Returns true when the setting type is BOOLEAN.
    @Override
    public boolean supports(SettingType type) {
        return type == SettingType.BOOLEAN;
    }

    // Validates that the input is exactly 'true' or 'false' (case-insensitive).
    @Override
    public void validate(String key, String value, SettingDefinition definition) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidConfigException(key, value, "Boolean value cannot be empty");
        }
        String clean = value.trim().toLowerCase();
        if (!clean.equals("true") && !clean.equals("false")) {
            throw new InvalidConfigException(key, value, "Expected boolean 'true' or 'false'");
        }
    }
}
