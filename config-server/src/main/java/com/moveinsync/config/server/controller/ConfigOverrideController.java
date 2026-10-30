package com.moveinsync.config.server.controller;

import com.moveinsync.config.client.model.ExplainResult;
import com.moveinsync.config.client.model.ResolvedSetting;
import com.moveinsync.config.server.domain.SettingOverride;
import com.moveinsync.config.server.repository.SettingOverrideRepository;
import com.moveinsync.config.server.security.TenantContext;
import com.moveinsync.config.server.service.HierarchyResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for evaluating hierarchical overrides and explain-mode diagnostics.
 */
@RestController
@RequestMapping("/api/v1/configs/{tenantId}")
public class ConfigOverrideController {

    private final HierarchyResolutionService resolutionService;
    private final SettingOverrideRepository overrideRepository;

    // Injects resolution service and override repository.
    public ConfigOverrideController(HierarchyResolutionService resolutionService,
                                    SettingOverrideRepository overrideRepository) {
        this.resolutionService = resolutionService;
        this.overrideRepository = overrideRepository;
    }

    // Resolves a setting following the hierarchy: SHIFT -> OFFICE -> CLIENT -> GLOBAL.
    @GetMapping("/resolve")
    public ResponseEntity<ResolvedSetting> resolve(@PathVariable String tenantId,
                                                   @RequestParam String key,
                                                   @RequestParam(required = false) String officeId,
                                                   @RequestParam(required = false) String shiftId) {
        TenantContext.assertCanAccessTenant(tenantId);
        ResolvedSetting resolved = resolutionService.resolve(tenantId, key, officeId, shiftId);
        return ResponseEntity.ok(resolved);
    }

    // Explains how the setting was resolved across all hierarchy levels.
    @GetMapping("/explain")
    public ResponseEntity<ExplainResult> explain(@PathVariable String tenantId,
                                                 @RequestParam String key,
                                                 @RequestParam(required = false) String officeId,
                                                 @RequestParam(required = false) String shiftId) {
        TenantContext.assertCanAccessTenant(tenantId);
        ExplainResult explain = resolutionService.explain(tenantId, key, officeId, shiftId);
        return ResponseEntity.ok(explain);
    }

    // Lists all raw active overrides for this tenant.
    @GetMapping("/overrides")
    public ResponseEntity<List<SettingOverride>> listOverrides(@PathVariable String tenantId) {
        TenantContext.assertCanAccessTenant(tenantId);
        List<SettingOverride> overrides = overrideRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(overrides);
    }
}
