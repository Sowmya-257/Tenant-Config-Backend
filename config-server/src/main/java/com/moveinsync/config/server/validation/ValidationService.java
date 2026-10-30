package com.moveinsync.config.server.validation;

import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.server.domain.SettingDefinition;
import com.moveinsync.config.server.exception.InvalidConfigException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dispatches validation to the appropriate strategy based on setting type.
 */
@Service
public class ValidationService {

    private final List<SettingValidator> validators;

    // Injects all registered setting validator strategies.
    public ValidationService(List<SettingValidator> validators) {
        this.validators = validators;
    }

    // Validates a setting value against its type and definition rules before publication.
    public void validate(String key, String value, SettingDefinition definition) {
        if (definition == null) {
            return;
        }
        SettingType type = definition.getType();
        for (SettingValidator validator : validators) {
            if (validator.supports(type)) {
                validator.validate(key, value, definition);
                return;
            }
        }
        throw new InvalidConfigException(key, value, "No validator found for setting type " + type);
    }
}
