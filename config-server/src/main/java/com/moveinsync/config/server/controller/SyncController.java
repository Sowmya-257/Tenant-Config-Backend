package com.moveinsync.config.server.controller;

import com.moveinsync.config.client.model.ConfigDelta;
import com.moveinsync.config.client.model.TenantConfigSnapshot;
import com.moveinsync.config.server.security.TenantContext;
import com.moveinsync.config.server.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller serving snapshots, deltas, and version checks to client libraries.
 */
@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService syncService;

    // Injects the client sync service.
    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    // Returns the latest monotonic version number for a tenant.
    @GetMapping("/version")
    public ResponseEntity<Long> getVersion(@RequestParam String tenantId) {
        TenantContext.assertCanAccessTenant(tenantId);
        Long version = syncService.getLatestVersion(tenantId);
        return ResponseEntity.ok(version);
    }

    // Downloads the complete in-memory snapshot for bootstrap and full recovery.
    @GetMapping("/snapshot")
    public ResponseEntity<TenantConfigSnapshot> getSnapshot(@RequestParam String tenantId) {
        TenantContext.assertCanAccessTenant(tenantId);
        TenantConfigSnapshot snapshot = syncService.getFullSnapshot(tenantId);
        return ResponseEntity.ok(snapshot);
    }

    // Returns only the incremental deltas that occurred since a client's last seen version.
    @GetMapping("/delta")
    public ResponseEntity<List<ConfigDelta>> getDeltas(@RequestParam String tenantId,
                                                       @RequestParam(defaultValue = "0") Long sinceVersion) {
        TenantContext.assertCanAccessTenant(tenantId);
        List<ConfigDelta> deltas = syncService.getDeltasSince(tenantId, sinceVersion);
        return ResponseEntity.ok(deltas);
    }
}
