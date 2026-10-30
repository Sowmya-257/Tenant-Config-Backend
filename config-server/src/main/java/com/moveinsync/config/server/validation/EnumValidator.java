package com.moveinsync.config.server.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.server.domain.SettingDefinition;
import com.moveinsync.config.server.exception.InvalidConfigException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates enum setting values against allowed predefined choices.
 */
@Component
public class EnumValidator implements SettingValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Returns true when the setting type is ENUM.
    @Override
    public boolean supports(SettingType type) {
        return type == SettingType.ENUM;
    }

    // Validates that the supplied value matches one of the allowed enum options.
    @Override
    public void validate(String key, String value, SettingDefinition definition) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidConfigException(key, value, "Enum value cannot be empty");
        }
        String cleanValue = value.trim();

        if (definition != null && definition.getValidationRulesJson() != null && !definition.getValidationRulesJson().trim().isEmpty()) {
            try {
                JsonNode rules = objectMapper.readTree(definition.getValidationRulesJson());
                if (rules.has("allowedValues") && rules.get("allowedValues").isArray()) {
                    Set<String> allowed = new HashSet<>();
                    for (JsonNode node : rules.get("allowedValues")) {
                        allowed.add(node.asText().trim().toUpperCase());
                    }
                    if (!allowed.contains(cleanValue.toUpperCase())) {
                        throw new InvalidConfigException(key, value, "Value is not among allowed enum choices: " + allowed);
                    }
                }
            } catch (InvalidConfigException ice) {
                throw ice;
            } catch (Exception e) {
                // Ignore parse error
            }
        }
    }
}
