package com.moveinsync.config.server.service;

import com.moveinsync.config.client.model.ConfigDelta;
import com.moveinsync.config.client.model.Level;
import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.server.domain.*;
import com.moveinsync.config.server.dto.ChangeSetItemRequest;
import com.moveinsync.config.server.dto.ChangeSetRequest;
import com.moveinsync.config.server.dto.ChangeSetResponse;
import com.moveinsync.config.server.exception.InvalidConfigException;
import com.moveinsync.config.server.repository.ChangeSetRepository;
import com.moveinsync.config.server.repository.SettingDefinitionRepository;
import com.moveinsync.config.server.repository.SettingOverrideRepository;
import com.moveinsync.config.server.repository.TenantVersionRepository;
import com.moveinsync.config.server.validation.ValidationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages atomic multi-key change sets, transactional persistence, and live propagation.
 */
@Service
public class ChangeSetService {

    private static final Logger log = LoggerFactory.getLogger(ChangeSetService.class);

    private final SettingDefinitionRepository definitionRepository;
    private final SettingOverrideRepository overrideRepository;
    private final TenantVersionRepository versionRepository;
    private final ChangeSetRepository changeSetRepository;
    private final ValidationService validationService;
    private final LivePropagationBroadcaster broadcaster;
    private final Counter changesPublishedCounter;
    private final Counter rejectionsCounter;
    private final Map<String, Object> tenantLocks = new ConcurrentHashMap<>();

    // Injects repositories, validation, broadcasting services, and metrics.
    public ChangeSetService(SettingDefinitionRepository definitionRepository,
                            SettingOverrideRepository overrideRepository,
                            TenantVersionRepository versionRepository,
                            ChangeSetRepository changeSetRepository,
                            ValidationService validationService,
                            LivePropagationBroadcaster broadcaster,
                            MeterRegistry meterRegistry) {
        this.definitionRepository = definitionRepository;
        this.overrideRepository = overrideRepository;
        this.versionRepository = versionRepository;
        this.changeSetRepository = changeSetRepository;
        this.validationService = validationService;
        this.broadcaster = broadcaster;
        this.changesPublishedCounter = meterRegistry.counter("config_changes_published_total");
        this.rejectionsCounter = meterRegistry.counter("config_rejections_total");
    }

    // Atomically validates and applies a batch of configuration changes under a new monotonic version.
    @Transactional
    public ChangeSetResponse applyChangeSet(String tenantId, ChangeSetRequest request) {
        // Step 1: Pre-validate all items before touching the database or version sequence
        validateBatch(request);

        // Step 2: Atomic lock to guarantee strictly monotonic version sequence per tenant
        Object lock = tenantLocks.computeIfAbsent(tenantId, k -> new Object());
        Long newVersion;
        synchronized (lock) {
            TenantVersion tenantVersion = versionRepository.findById(tenantId)
                    .orElseGet(() -> new TenantVersion(tenantId, 0L));
            newVersion = tenantVersion.increment();
            versionRepository.saveAndFlush(tenantVersion);
        }

        // Step 3: Create audit change set entity
        ChangeSet changeSet = new ChangeSet(tenantId, newVersion, request.getAuthor(), request.getReason());

        List<ConfigDelta.Item> deltaItems = new ArrayList<>();
        List<ChangeSetResponse.ChangeSetItemDto> responseItems = new ArrayList<>();

        for (ChangeSetItemRequest itemReq : request.getItems()) {
            String key = itemReq.getSettingKey().trim();
            Level level = itemReq.getLevel();
            String officeId = itemReq.getOfficeId();
            String shiftId = itemReq.getShiftId();
            String newValue = itemReq.getValue();
            boolean isDeleted = itemReq.isDeleted();

            SettingDefinition definition = definitionRepository.findById(key).orElse(null);
            SettingType type = definition != null ? definition.getType() : SettingType.STRING;

            // Fetch existing override to record old value
            Optional<SettingOverride> existingOpt = overrideRepository.findExactOverride(
                    tenantId, key, level, officeId, shiftId
            );

            String oldValue = existingOpt.map(SettingOverride::getSettingValue).orElse(null);

            if (isDeleted) {
                existingOpt.ifPresent(overrideRepository::delete);
            } else {
                if (existingOpt.isPresent()) {
                    SettingOverride override = existingOpt.get();
                    override.setSettingValue(newValue);
                    override.setVersion(newVersion);
                    override.setAuthor(request.getAuthor());
                    override.setReason(request.getReason());
                    overrideRepository.save(override);
                } else {
                    SettingOverride override = new SettingOverride(
                            tenantId, level, officeId, shiftId, key, newValue,
                            newVersion, request.getAuthor(), request.getReason()
                    );
                    overrideRepository.save(override);
                }
            }

            ChangeSetItem csItem = new ChangeSetItem(key, level, officeId, shiftId, oldValue, newValue, type, isDeleted);
            changeSet.addItem(csItem);

            deltaItems.add(new ConfigDelta.Item(
                    key, level, officeId, shiftId, newValue, type, isDeleted,
                    request.getAuthor(), request.getReason()
            ));

            responseItems.add(new ChangeSetResponse.ChangeSetItemDto(
                    key, level.name(), officeId, shiftId, oldValue, newValue, isDeleted
            ));
        }

        changeSetRepository.save(changeSet);
        changesPublishedCounter.increment();

        log.info("Committed atomic change set v{} with {} items for tenant {}",
                newVersion, request.getItems().size(), tenantId);

        // Step 4: Broadcast delta live to all connected push clients
        ConfigDelta delta = new ConfigDelta(newVersion, tenantId, request.getAuthor(), request.getReason(), deltaItems);
        broadcaster.broadcastDelta(tenantId, delta);

        return new ChangeSetResponse(
                newVersion, tenantId, request.getAuthor(), request.getReason(),
                changeSet.getCreatedAt(), responseItems.size(), responseItems
        );
    }

    // Validates every item in the incoming batch, rejecting invalid values before publishing.
    private void validateBatch(ChangeSetRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidConfigException("batch", "empty", "Change set cannot be empty");
        }

        for (ChangeSetItemRequest item : request.getItems()) {
            if (item.isDeleted()) {
                continue; // Deletion doesn't require value validation
            }
            String key = item.getSettingKey();
            String value = item.getValue();

            Optional<SettingDefinition> defOpt = definitionRepository.findById(key);
            if (defOpt.isEmpty()) {
                rejectionsCounter.increment();
                throw new InvalidConfigException(key, value, "Setting definition not found for key '" + key + "'");
            }

            try {
                validationService.validate(key, value, defOpt.get());
            } catch (InvalidConfigException e) {
                rejectionsCounter.increment();
                throw e;
            }
        }
    }
}
