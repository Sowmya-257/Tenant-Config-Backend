package com.moveinsync.config.server.controller;

import com.moveinsync.config.server.security.TenantContext;
import com.moveinsync.config.server.service.LivePropagationBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controller exposing real-time Server-Sent Events (SSE) push streaming endpoints.
 */
@RestController
@RequestMapping("/api/v1/stream")
public class LiveStreamController {

    private final LivePropagationBroadcaster broadcaster;

    // Injects the live push broadcaster.
    public LiveStreamController(LivePropagationBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    // Opens an HTTP Server-Sent Events push stream for live configuration propagation.
    @GetMapping(value = {"/{tenantId}", "/configs/{tenantId}"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTenantConfigs(@PathVariable String tenantId) {
        TenantContext.assertCanAccessTenant(tenantId);
        return broadcaster.registerClient(tenantId);
    }
}
