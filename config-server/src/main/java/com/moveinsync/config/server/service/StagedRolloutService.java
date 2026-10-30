package com.moveinsync.config.server.service;

import com.moveinsync.config.client.model.Level;
import com.moveinsync.config.server.domain.StagedRollout;
import com.moveinsync.config.server.dto.ChangeSetItemRequest;
import com.moveinsync.config.server.dto.ChangeSetRequest;
import com.moveinsync.config.server.dto.ChangeSetResponse;
import com.moveinsync.config.server.exception.ResourceNotFoundException;
import com.moveinsync.config.server.repository.StagedRolloutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Orchestrates phased rollouts across pilot offices before tenant-wide promotion.
 */
@Service
public class StagedRolloutService {

    private static final Logger log = LoggerFactory.getLogger(StagedRolloutService.class);

    private final StagedRolloutRepository rolloutRepository;
    private final ChangeSetService changeSetService;

    // Injects staged rollout repository and atomic change set service.
    public StagedRolloutService(StagedRolloutRepository rolloutRepository,
                                ChangeSetService changeSetService) {
        this.rolloutRepository = rolloutRepository;
        this.changeSetService = changeSetService;
    }

    // Deploys a new setting value to selected pilot offices as an active canary.
    @Transactional
    public StagedRollout createRollout(String tenantId, String settingKey, String canaryValue,
                                       List<String> targetOffices, String author, String reason) {
        String officesCsv = String.join(",", targetOffices);
        StagedRollout rollout = new StagedRollout(tenantId, settingKey, canaryValue, officesCsv, author, reason);

        // Apply OFFICE-level overrides for each target pilot office in an atomic change set
        List<ChangeSetItemRequest> items = new ArrayList<>();
        for (String office : targetOffices) {
            items.add(new ChangeSetItemRequest(settingKey, Level.OFFICE, office.trim(), null, canaryValue, false));
        }

        ChangeSetResponse csResponse = changeSetService.applyChangeSet(
                tenantId,
                new ChangeSetRequest(author, "Canary staged rollout: " + reason, items)
        );

        rollout.setCanaryVersion(csResponse.getVersion());
        StagedRollout saved = rolloutRepository.save(rollout);
        log.info("Started staged rollout #{} for setting '{}' to offices [{}] under tenant {}",
                saved.getId(), settingKey, officesCsv, tenantId);
        return saved;
    }

    // Promotes the canary value tenant-wide and cleans up office-specific overrides.
    @Transactional
    public StagedRollout promoteRollout(Long rolloutId, String author, String reason) {
        StagedRollout rollout = rolloutRepository.findById(rolloutId)
                .orElseThrow(() -> new ResourceNotFoundException("Rollout #" + rolloutId + " not found"));

        if (rollout.getStatus() != StagedRollout.RolloutStatus.CANARY_ACTIVE) {
            throw new IllegalStateException("Rollout #" + rolloutId + " is not in CANARY_ACTIVE state");
        }

        List<String> offices = Arrays.asList(rollout.getTargetOfficesCsv().split(","));
        List<ChangeSetItemRequest> items = new ArrayList<>();

        // 1. Promote to CLIENT level so all offices inherit it
        items.add(new ChangeSetItemRequest(rollout.getSettingKey(), Level.CLIENT, null, null, rollout.getCanaryValue(), false));

        // 2. Remove the canary OFFICE level overrides so client level cleanly applies
        for (String office : offices) {
            items.add(new ChangeSetItemRequest(rollout.getSettingKey(), Level.OFFICE, office.trim(), null, null, true));
        }

        ChangeSetResponse response = changeSetService.applyChangeSet(
                rollout.getTenantId(),
                new ChangeSetRequest(author, "Promote rollout #" + rolloutId + ": " + reason, items)
        );

        rollout.setStatus(StagedRollout.RolloutStatus.PROMOTED);
        rollout.setPromotedVersion(response.getVersion());
        log.info("Promoted staged rollout #{} to all offices at version {}", rolloutId, response.getVersion());
        return rolloutRepository.save(rollout);
    }

    // Aborts an active canary rollout and reverts pilot offices.
    @Transactional
    public StagedRollout abortRollout(Long rolloutId, String author, String reason) {
        StagedRollout rollout = rolloutRepository.findById(rolloutId)
                .orElseThrow(() -> new ResourceNotFoundException("Rollout #" + rolloutId + " not found"));

        if (rollout.getStatus() != StagedRollout.RolloutStatus.CANARY_ACTIVE) {
            throw new IllegalStateException("Rollout #" + rolloutId + " is not in CANARY_ACTIVE state");
        }

        List<String> offices = Arrays.asList(rollout.getTargetOfficesCsv().split(","));
        List<ChangeSetItemRequest> items = new ArrayList<>();

        // Remove the canary OFFICE overrides to revert back to parent client/global settings
        for (String office : offices) {
            items.add(new ChangeSetItemRequest(rollout.getSettingKey(), Level.OFFICE, office.trim(), null, null, true));
        }

        changeSetService.applyChangeSet(
                rollout.getTenantId(),
                new ChangeSetRequest(author, "Abort canary rollout #" + rolloutId + ": " + reason, items)
        );

        rollout.setStatus(StagedRollout.RolloutStatus.ABORTED);
        log.warn("Aborted staged rollout #{} for setting '{}'", rolloutId, rollout.getSettingKey());
        return rolloutRepository.save(rollout);
    }
}
