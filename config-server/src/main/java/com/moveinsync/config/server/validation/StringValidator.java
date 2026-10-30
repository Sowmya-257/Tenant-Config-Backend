package com.moveinsync.config.server.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.server.domain.SettingDefinition;
import com.moveinsync.config.server.exception.InvalidConfigException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validates generic string settings with regex pattern and length constraints.
 */
@Component
public class StringValidator implements SettingValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Returns true when the setting type is STRING.
    @Override
    public boolean supports(SettingType type) {
        return type == SettingType.STRING;
    }

    // Validates string length and regex pattern matches if configured.
    @Override
    public void validate(String key, String value, SettingDefinition definition) {
        if (value == null) {
            throw new InvalidConfigException(key, "null", "String setting value cannot be null");
        }

        if (definition != null && definition.getValidationRulesJson() != null && !definition.getValidationRulesJson().trim().isEmpty()) {
            try {
                JsonNode rules = objectMapper.readTree(definition.getValidationRulesJson());
                if (rules.has("minLength") && value.length() < rules.get("minLength").asInt()) {
                    throw new InvalidConfigException(key, value, "String length must be at least " + rules.get("minLength").asInt());
                }
                if (rules.has("maxLength") && value.length() > rules.get("maxLength").asInt()) {
                    throw new InvalidConfigException(key, value, "String length cannot exceed " + rules.get("maxLength").asInt());
                }
                if (rules.has("pattern")) {
                    String regex = rules.get("pattern").asText();
                    if (!Pattern.matches(regex, value)) {
                        throw new InvalidConfigException(key, value, "Value does not match required regex pattern: " + regex);
                    }
                }
            } catch (InvalidConfigException ice) {
                throw ice;
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
    }
}
