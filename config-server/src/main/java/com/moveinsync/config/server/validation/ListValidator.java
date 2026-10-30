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
 * Validates comma-separated list values, checking element counts and allowed elements.
 */
@Component
public class ListValidator implements SettingValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Returns true when the setting type is LIST.
    @Override
    public boolean supports(SettingType type) {
        return type == SettingType.LIST;
    }

    // Validates list elements against allowed items and minimum size rules.
    @Override
    public void validate(String key, String value, SettingDefinition definition) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidConfigException(key, value, "List value cannot be empty");
        }
        String[] items = value.split(",");
        if (items.length == 0) {
            throw new InvalidConfigException(key, value, "List must contain at least one item");
        }

        if (definition != null && definition.getValidationRulesJson() != null && !definition.getValidationRulesJson().trim().isEmpty()) {
            try {
                JsonNode rules = objectMapper.readTree(definition.getValidationRulesJson());
                if (rules.has("allowedItems") && rules.get("allowedItems").isArray()) {
                    Set<String> allowed = new HashSet<>();
                    for (JsonNode node : rules.get("allowedItems")) {
                        allowed.add(node.asText().trim().toUpperCase());
                    }
                    for (String item : items) {
                        if (!allowed.contains(item.trim().toUpperCase())) {
                            throw new InvalidConfigException(key, value, "List contains unauthorized item '" + item.trim() + "'. Allowed: " + allowed);
                        }
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
