package com.moveinsync.config.server.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.client.model.TimeWindow;
import com.moveinsync.config.server.domain.SettingDefinition;
import com.moveinsync.config.server.exception.InvalidConfigException;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Validates operating time windows like night safety and booking periods.
 */
@Component
public class TimeWindowValidator implements SettingValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Returns true when the setting type is TIME_WINDOW.
    @Override
    public boolean supports(SettingType type) {
        return type == SettingType.TIME_WINDOW;
    }

    // Validates time format, distinct start/end, and checks overnight window permissions.
    @Override
    public void validate(String key, String value, SettingDefinition definition) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidConfigException(key, value, "Time window value cannot be empty");
        }

        TimeWindow window;
        try {
            window = TimeWindow.parse(value.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigException(key, value, e.getMessage());
        }

        LocalTime start = window.getStartTime();
        LocalTime end = window.getEndTime();

        boolean allowOvernight = true;
        if (definition != null && definition.getValidationRulesJson() != null && !definition.getValidationRulesJson().trim().isEmpty()) {
            try {
                JsonNode rules = objectMapper.readTree(definition.getValidationRulesJson());
                if (rules.has("allowOvernight")) {
                    allowOvernight = rules.get("allowOvernight").asBoolean();
                }
            } catch (Exception e) {
                // Default allow overnight
            }
        }

        // If overnight is not permitted, end time must strictly be after start time
        if (!allowOvernight && !start.isBefore(end)) {
            throw new InvalidConfigException(key, value, "Time window ends before it starts (" + start + " to " + end + ") and overnight window is not permitted");
        }
    }
}
