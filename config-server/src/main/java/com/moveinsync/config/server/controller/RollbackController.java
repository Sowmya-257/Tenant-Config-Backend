package com.moveinsync.config.server.controller;

import com.moveinsync.config.server.dto.RollbackDiffResponse;
import com.moveinsync.config.server.security.TenantContext;
import com.moveinsync.config.server.service.RollbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for executing instant rollbacks to any prior version.
 */
@RestController
@RequestMapping("/api/v1/configs/{tenantId}/rollback")
public class RollbackController {

    private final RollbackService rollbackService;

    // Injects the instant rollback service.
    public RollbackController(RollbackService rollbackService) {
        this.rollbackService = rollbackService;
    }

    // Rolls back to a previous configuration version and returns the computed diff.
    @PostMapping
    public ResponseEntity<RollbackDiffResponse> rollback(@PathVariable String tenantId,
                                                         @RequestParam Long targetVersion,
                                                         @RequestParam(defaultValue = "admin_rollback") String author,
                                                         @RequestParam(required = false) String reason) {
        TenantContext.assertCanModifyTenant(tenantId);
        RollbackDiffResponse diff = rollbackService.rollbackToVersion(tenantId, targetVersion, author, reason);
        return ResponseEntity.ok(diff);
    }
}
