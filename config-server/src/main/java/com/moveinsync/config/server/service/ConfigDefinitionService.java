package com.moveinsync.config.server.service;

import com.moveinsync.config.client.model.Level;
import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.server.domain.SettingDefinition;
import com.moveinsync.config.server.dto.ChangeSetItemRequest;
import com.moveinsync.config.server.dto.ChangeSetRequest;
import com.moveinsync.config.server.exception.ResourceNotFoundException;
import com.moveinsync.config.server.repository.SettingDefinitionRepository;
import com.moveinsync.config.server.repository.TenantVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages setting definitions, schema validation metadata, and seeds standard settings and company baselines.
 */
@Service
public class ConfigDefinitionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigDefinitionService.class);

    private final SettingDefinitionRepository definitionRepository;
    private final TenantVersionRepository versionRepository;
    private final ChangeSetService changeSetService;

    // Injects repositories and change set service.
    public ConfigDefinitionService(SettingDefinitionRepository definitionRepository,
                                   TenantVersionRepository versionRepository,
                                   ChangeSetService changeSetService) {
        this.definitionRepository = definitionRepository;
        this.versionRepository = versionRepository;
        this.changeSetService = changeSetService;
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
                    "{\"allowedItems\": [\"SEDAN\", \"SUV\", \"HATCHBACK\", \"ELECTRIC_EV\", \"MINIVAN\", \"SWIFT\"]}"
            ));
            saveOrUpdate(new SettingDefinition(
                    "vendor.assignment.strategy", SettingType.ENUM,
                    "Algorithm used to distribute trips across vendors", "ROUND_ROBIN",
                    "{\"allowedValues\": [\"ROUND_ROBIN\", \"LEAST_COST\", \"PREFERRED\", \"CAPACITY_BALANCED\"]}"
            ));
            log.info("Finished seeding {} setting definitions.", definitionRepository.count());
        }

        // Ensure cab.types.allowed includes SWIFT even if database was seeded in a prior run
        definitionRepository.findById("cab.types.allowed").ifPresent(def -> {
            if (def.getValidationRulesJson() != null && !def.getValidationRulesJson().contains("SWIFT")) {
                def.setValidationRulesJson("{\"allowedItems\": [\"SEDAN\", \"SUV\", \"HATCHBACK\", \"ELECTRIC_EV\", \"MINIVAN\", \"SWIFT\"]}");
                definitionRepository.save(def);
                log.info("Updated cab.types.allowed validation rules to include SWIFT");
            }
        });

        // Seed initial company tenant overrides if empty
        if (versionRepository.count() == 0) {
            log.info("Seeding company baseline configurations for MICROSOFT, GOOGLE, AMAZON, and UBER...");

            // 1. MICROSOFT
            changeSetService.applyChangeSet("MICROSOFT", new ChangeSetRequest(
                    "ops_admin@microsoft.com", "Initial baseline setup",
                    List.of(
                            new ChangeSetItemRequest("booking.cutoff.mins", Level.CLIENT, null, null, "45m", false),
                            new ChangeSetItemRequest("cab.types.allowed", Level.CLIENT, null, null, "SEDAN,SUV,ELECTRIC_EV", false)
                    )
            ));
            changeSetService.applyChangeSet("MICROSOFT", new ChangeSetRequest(
                    "blr_transport@microsoft.com", "Bangalore EcoWorld cut-off optimization & night safety",
                    List.of(
                            new ChangeSetItemRequest("booking.cutoff.mins", Level.OFFICE, "BLR-01", null, "30m", false),
                            new ChangeSetItemRequest("night_safety.window", Level.CLIENT, null, null, "21:00-05:30", false)
                    )
            ));
            changeSetService.applyChangeSet("MICROSOFT", new ChangeSetRequest(
                    "safety_lead@microsoft.com", "Enforce Bangalore EcoWorld night escort",
                    List.of(
                            new ChangeSetItemRequest("escort.required", Level.SHIFT, "BLR-01", "NIGHT", "true", false)
                    )
            ));

            // 2. GOOGLE
            changeSetService.applyChangeSet("GOOGLE", new ChangeSetRequest(
                    "transport_ops@google.com", "Google baseline configuration",
                    List.of(
                            new ChangeSetItemRequest("booking.cutoff.mins", Level.CLIENT, null, null, "35m", false),
                            new ChangeSetItemRequest("ride.max_time.mins", Level.CLIENT, null, null, "75m", false),
                            new ChangeSetItemRequest("night_safety.window", Level.CLIENT, null, null, "22:00-06:00", false)
                    )
            ));
            changeSetService.applyChangeSet("GOOGLE", new ChangeSetRequest(
                    "sustainability@google.com", "RMZ Infinity quick cutoff and green EV fleet",
                    List.of(
                            new ChangeSetItemRequest("booking.cutoff.mins", Level.OFFICE, "BLR-01", null, "20m", false),
                            new ChangeSetItemRequest("cab.types.allowed", Level.CLIENT, null, null, "ELECTRIC_EV,SEDAN", false),
                            new ChangeSetItemRequest("vendor.assignment.strategy", Level.OFFICE, "BLR-01", null, "PREFERRED", false)
                    )
            ));

            // 3. AMAZON
            changeSetService.applyChangeSet("AMAZON", new ChangeSetRequest(
                    "in_transport@amazon.com", "Amazon India baseline setup",
                    List.of(
                            new ChangeSetItemRequest("booking.cutoff.mins", Level.CLIENT, null, null, "30m", false),
                            new ChangeSetItemRequest("ride.max_time.mins", Level.CLIENT, null, null, "60m", false),
                            new ChangeSetItemRequest("night_safety.window", Level.CLIENT, null, null, "20:00-05:00", false),
                            new ChangeSetItemRequest("cab.types.allowed", Level.CLIENT, null, null, "SUV,HATCHBACK,SEDAN", false)
                    )
            ));
            changeSetService.applyChangeSet("AMAZON", new ChangeSetRequest(
                    "hyd_ops_lead@amazon.com", "Hyderabad HQ 15m cut-off and least-cost algorithm",
                    List.of(
                            new ChangeSetItemRequest("booking.cutoff.mins", Level.OFFICE, "HYD-01", null, "15m", false),
                            new ChangeSetItemRequest("vendor.assignment.strategy", Level.CLIENT, null, null, "LEAST_COST", false),
                            new ChangeSetItemRequest("escort.required", Level.SHIFT, "HYD-01", "NIGHT", "true", false)
                    )
            ));

            // 4. UBER
            changeSetService.applyChangeSet("UBER", new ChangeSetRequest(
                    "mobility_ops@uber.com", "Uber mobility baseline setup",
                    List.of(
                            new ChangeSetItemRequest("booking.cutoff.mins", Level.CLIENT, null, null, "20m", false),
                            new ChangeSetItemRequest("ride.max_time.mins", Level.CLIENT, null, null, "45m", false),
                            new ChangeSetItemRequest("night_safety.window", Level.CLIENT, null, null, "21:30-06:00", false)
                    )
            ));
            changeSetService.applyChangeSet("UBER", new ChangeSetRequest(
                    "blr_dispatch@uber.com", "Kalyani Tech Park 10m on-demand dispatch",
                    List.of(
                            new ChangeSetItemRequest("booking.cutoff.mins", Level.OFFICE, "BLR-01", null, "10m", false),
                            new ChangeSetItemRequest("vendor.assignment.strategy", Level.CLIENT, null, null, "CAPACITY_BALANCED", false)
                    )
            ));

            log.info("Finished seeding initial company configurations.");
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
