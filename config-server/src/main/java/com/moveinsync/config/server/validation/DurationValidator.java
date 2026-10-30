package com.moveinsync.config.server.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.server.domain.SettingDefinition;
import com.moveinsync.config.server.exception.InvalidConfigException;
import org.springframework.stereotype.Component;

/**
 * Validates duration settings, guaranteeing positive non-negative time intervals.
 */
@Component
public class DurationValidator implements SettingValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Returns true when the setting type is DURATION.
    @Override
    public boolean supports(SettingType type) {
        return type == SettingType.DURATION;
    }

    // Validates that the input represents a positive duration and satisfies duration bounds.
    @Override
    public void validate(String key, String value, SettingDefinition definition) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidConfigException(key, value, "Duration value cannot be empty");
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("-")) {
            throw new InvalidConfigException(key, value, "Duration cannot be negative");
        }

        long minutes;
        try {
            String lower = trimmed.toLowerCase();
            if (lower.endsWith("m")) {
                minutes = Long.parseLong(lower.substring(0, lower.length() - 1));
            } else if (lower.endsWith("h")) {
                minutes = Long.parseLong(lower.substring(0, lower.length() - 1)) * 60;
            } else if (lower.endsWith("s")) {
                minutes = Math.max(1, Long.parseLong(lower.substring(0, lower.length() - 1)) / 60);
            } else {
                minutes = Long.parseLong(lower);
            }
        } catch (NumberFormatException e) {
            throw new InvalidConfigException(key, value, "Invalid duration format. Expected format like '30m', '2h', or integer minutes");
        }

        if (minutes < 0) {
            throw new InvalidConfigException(key, value, "Duration cannot be negative");
        }

        if (definition != null && definition.getValidationRulesJson() != null && !definition.getValidationRulesJson().trim().isEmpty()) {
            try {
                JsonNode rules = objectMapper.readTree(definition.getValidationRulesJson());
                if (rules.has("minMinutes") && minutes < rules.get("minMinutes").asLong()) {
                    throw new InvalidConfigException(key, value, "Duration cannot be less than minimum " + rules.get("minMinutes").asLong() + " minutes");
                }
                if (rules.has("maxMinutes") && minutes > rules.get("maxMinutes").asLong()) {
                    throw new InvalidConfigException(key, value, "Duration cannot exceed maximum " + rules.get("maxMinutes").asLong() + " minutes");
                }
            } catch (InvalidConfigException ice) {
                throw ice;
            } catch (Exception e) {
                // Ignore invalid rule json syntax
            }
        }
    }
}
