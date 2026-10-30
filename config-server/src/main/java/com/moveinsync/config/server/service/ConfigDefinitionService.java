package com.moveinsync.config.server.service;

import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.server.domain.SettingDefinition;
import com.moveinsync.config.server.exception.ResourceNotFoundException;
import com.moveinsync.config.server.repository.SettingDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages setting definitions, schema validation metadata, and seeds standard settings.
 */
@Service
public class ConfigDefinitionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigDefinitionService.class);

    private final SettingDefinitionRepository definitionRepository;

    // Injects setting definition repository.
    public ConfigDefinitionService(SettingDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    // Automatically seeds standard MoveInSync operational settings into the database at startup.
    @Override
    @Transactional
    public void run(String... args) {
        if (definitionRepository.count() == 0) {
            log.info("Seeding standard MoveInSync setting definitions...");
            saveOrUpdate(new SettingDefinition(
                    "booking.cutoff.mins", SettingType.DURATION,
                    "Booking cut-off time prior to trip departure", "60m",
                    "{\"minMinutes\": 0, \"maxMinutes\": 720}"
            ));
            saveOrUpdate(new SettingDefinition(
                    "ride.max_time.mins", SettingType.DURATION,
                    "Maximum permissible travel time per employee ride", "90m",
                    "{\"minMinutes\": 10, \"maxMinutes\": 360}"
            ));
            saveOrUpdate(new SettingDefinition(
                    "night_safety.window", SettingType.TIME_WINDOW,
                    "Time window during which night escort and security rules are enforced", "20:00-06:00",
                    "{\"allowOvernight\": true}"
            ));
            saveOrUpdate(new SettingDefinition(
                    "escort.required", SettingType.BOOLEAN,
                    "Whether security escort is mandatory for women traveling during night window", "true",
                    "{}"
            ));
            saveOrUpdate(new SettingDefinition(
                    "cab.types.allowed", SettingType.LIST,
                    "Allowed vehicle categories for employee transportation", "SEDAN,SUV,HATCHBACK,ELECTRIC_EV",
                    "{\"allowedItems\": [\"SEDAN\", \"SUV\", \"HATCHBACK\", \"ELECTRIC_EV\", \"MINIVAN\"]}"
            ));
            saveOrUpdate(new SettingDefinition(
                    "vendor.assignment.strategy", SettingType.ENUM,
                    "Algorithm used to distribute trips across vendors", "ROUND_ROBIN",
                    "{\"allowedValues\": [\"ROUND_ROBIN\", \"LEAST_COST\", \"PREFERRED\", \"CAPACITY_BALANCED\"]}"
            ));
            log.info("Finished seeding {} setting definitions.", definitionRepository.count());
        }
    }

    // Saves or updates a setting definition.
    @Transactional
    public SettingDefinition saveOrUpdate(SettingDefinition definition) {
        return definitionRepository.save(definition);
    }

    // Retrieves all defined configuration settings across the platform.
    @Transactional(readOnly = true)
    public List<SettingDefinition> getAllDefinitions() {
        return definitionRepository.findAll();
    }

    // Retrieves a single setting definition by its unique key.
    @Transactional(readOnly = true)
    public SettingDefinition getDefinition(String key) {
        return definitionRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting definition not found for key '" + key + "'"));
    }

    // Deletes a setting definition by key.
    @Transactional
    public void deleteDefinition(String key) {
        if (!definitionRepository.existsById(key)) {
            throw new ResourceNotFoundException("Setting definition not found for key '" + key + "'");
        }
        definitionRepository.deleteById(key);
    }
}
