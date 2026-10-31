package com.moveinsync.config.server.controller;

import com.moveinsync.config.server.domain.ChangeSet;
import com.moveinsync.config.server.dto.ChangeSetRequest;
import com.moveinsync.config.server.dto.ChangeSetResponse;
import com.moveinsync.config.server.repository.ChangeSetRepository;
import com.moveinsync.config.server.security.TenantContext;
import com.moveinsync.config.server.service.ChangeSetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for publishing atomic change sets and viewing version history.
 */
@RestController
@RequestMapping("/api/v1/configs/{tenantId}")
public class ChangeSetController {

    private final ChangeSetService changeSetService;
    private final ChangeSetRepository changeSetRepository;

    // Injects the change set service and audit repository.
    public ChangeSetController(ChangeSetService changeSetService,
                               ChangeSetRepository changeSetRepository) {
        this.changeSetService = changeSetService;
        this.changeSetRepository = changeSetRepository;
    }

    // Publishes an atomic multi-key change set under a strictly monotonic version.
    @PostMapping("/changeset")
    public ResponseEntity<ChangeSetResponse> applyChangeSet(@PathVariable String tenantId,
                                                            @Valid @RequestBody ChangeSetRequest request) {
        TenantContext.assertCanModifyTenant(tenantId);
        ChangeSetResponse response = changeSetService.applyChangeSet(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Retrieves historical audit trail of all change sets applied to this tenant.
    @GetMapping("/history")
    public ResponseEntity<List<ChangeSet>> getHistory(@PathVariable String tenantId) {
        TenantContext.assertCanAccessTenant(tenantId);
        List<ChangeSet> history = changeSetRepository.findByTenantIdOrderByVersionDesc(tenantId);
        return ResponseEntity.ok(history);
    }
}
