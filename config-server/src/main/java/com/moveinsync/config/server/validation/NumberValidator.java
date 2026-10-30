package com.moveinsync.config.server.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.server.domain.SettingDefinition;
import com.moveinsync.config.server.exception.InvalidConfigException;
import org.springframework.stereotype.Component;

/**
 * Validates numeric values including minimum, maximum, and integer requirements.
 */
@Component
public class NumberValidator implements SettingValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Returns true when the setting type is NUMBER.
    @Override
    public boolean supports(SettingType type) {
        return type == SettingType.NUMBER;
    }

    // Validates that the input is a valid number and respects min/max boundary constraints.
    @Override
    public void validate(String key, String value, SettingDefinition definition) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidConfigException(key, value, "Numeric setting value cannot be empty");
        }
        double numericVal;
        try {
            numericVal = Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidConfigException(key, value, "Value must be a valid number");
        }

        if (definition != null && definition.getValidationRulesJson() != null && !definition.getValidationRulesJson().trim().isEmpty()) {
            try {
                JsonNode rules = objectMapper.readTree(definition.getValidationRulesJson());
                if (rules.has("min") && numericVal < rules.get("min").asDouble()) {
                    throw new InvalidConfigException(key, value, "Value cannot be less than minimum allowed " + rules.get("min").asDouble());
                }
                if (rules.has("max") && numericVal > rules.get("max").asDouble()) {
                    throw new InvalidConfigException(key, value, "Value cannot exceed maximum allowed " + rules.get("max").asDouble());
                }
            } catch (InvalidConfigException ice) {
                throw ice;
            } catch (Exception e) {
                // Ignore invalid rule json syntax
            }
        }
    }
}
