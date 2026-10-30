package com.moveinsync.config.server.controller;

import com.moveinsync.config.server.domain.StagedRollout;
import com.moveinsync.config.server.repository.StagedRolloutRepository;
import com.moveinsync.config.server.security.TenantContext;
import com.moveinsync.config.server.service.StagedRolloutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for creating and promoting phased staged rollouts.
 */
@RestController
public class StagedRolloutController {

    private final StagedRolloutService rolloutService;
    private final StagedRolloutRepository rolloutRepository;

    // Injects staged rollout service and repository.
    public StagedRolloutController(StagedRolloutService rolloutService,
                                   StagedRolloutRepository rolloutRepository) {
        this.rolloutService = rolloutService;
        this.rolloutRepository = rolloutRepository;
    }

    // Creates a new staged canary rollout targeting specific pilot offices.
    @PostMapping("/api/v1/configs/{tenantId}/rollouts")
    public ResponseEntity<StagedRollout> createRollout(@PathVariable String tenantId,
                                                       @RequestParam String settingKey,
                                                       @RequestParam String canaryValue,
                                                       @RequestParam List<String> targetOffices,
                                                       @RequestParam String author,
                                                       @RequestParam(defaultValue = "Canary staged rollout") String reason) {
        TenantContext.assertCanAccessTenant(tenantId);
        StagedRollout rollout = rolloutService.createRollout(tenantId, settingKey, canaryValue, targetOffices, author, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(rollout);
    }

    // Promotes an active canary rollout tenant-wide to all offices and shifts.
    @PostMapping("/api/v1/rollouts/{rolloutId}/promote")
    public ResponseEntity<StagedRollout> promoteRollout(@PathVariable Long rolloutId,
                                                        @RequestParam String author,
                                                        @RequestParam(defaultValue = "Canary promotion") String reason) {
        StagedRollout promoted = rolloutService.promoteRollout(rolloutId, author, reason);
        return ResponseEntity.ok(promoted);
    }

    // Aborts an active canary rollout and reverts the pilot offices back to prior values.
    @PostMapping("/api/v1/rollouts/{rolloutId}/abort")
    public ResponseEntity<StagedRollout> abortRollout(@PathVariable Long rolloutId,
                                                      @RequestParam String author,
                                                      @RequestParam(defaultValue = "Canary abortion") String reason) {
        StagedRollout aborted = rolloutService.abortRollout(rolloutId, author, reason);
        return ResponseEntity.ok(aborted);
    }

    // Lists active canary rollouts for a given tenant.
    @GetMapping("/api/v1/configs/{tenantId}/rollouts")
    public ResponseEntity<List<StagedRollout>> listRollouts(@PathVariable String tenantId) {
        TenantContext.assertCanAccessTenant(tenantId);
        List<StagedRollout> rollouts = rolloutRepository.findByTenantIdAndStatus(
                tenantId, StagedRollout.RolloutStatus.CANARY_ACTIVE
        );
        return ResponseEntity.ok(rollouts);
    }
}
