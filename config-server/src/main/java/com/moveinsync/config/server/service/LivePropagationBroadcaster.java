package com.moveinsync.config.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moveinsync.config.client.model.ConfigChangeEvent;
import com.moveinsync.config.client.model.ConfigDelta;
import com.moveinsync.config.client.model.TenantConfigSnapshot;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages Server-Sent Event push streams for real-time live configuration propagation.
 */
@Service
public class LivePropagationBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(LivePropagationBroadcaster.class);

    private final Map<String, List<SseEmitter>> tenantEmitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final AtomicInteger activeConnectionsCount = new AtomicInteger(0);

    // Injects meter registry to record active connection gauge for on-call visibility.
    public LivePropagationBroadcaster(MeterRegistry meterRegistry) {
        Gauge.builder("config_live_subscribers_active", activeConnectionsCount, AtomicInteger::get)
                .description("Number of active SSE push streaming client subscribers")
                .register(meterRegistry);
    }

    // Registers a new client push listener emitter for real-time notifications.
    public SseEmitter registerClient(String tenantId) {
        // 30-minute timeout for client SSE connection
        SseEmitter emitter = new SseEmitter(1800_000L);
        tenantEmitters.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        activeConnectionsCount.incrementAndGet();

        emitter.onCompletion(() -> removeEmitter(tenantId, emitter));
        emitter.onTimeout(() -> removeEmitter(tenantId, emitter));
        emitter.onError(e -> removeEmitter(tenantId, emitter));

        // Send initial connection acknowledgement event
        try {
            ConfigChangeEvent welcome = ConfigChangeEvent.heartbeat(tenantId, 0L);
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(welcome)));
        } catch (IOException e) {
            removeEmitter(tenantId, emitter);
        }

        log.info("Client connected to live push stream for tenant {}. Active subscribers: {}",
                tenantId, activeConnectionsCount.get());
        return emitter;
    }

    // Unregisters a terminated or failed emitter stream.
    private void removeEmitter(String tenantId, SseEmitter emitter) {
        List<SseEmitter> list = tenantEmitters.get(tenantId);
        if (list != null && list.remove(emitter)) {
            activeConnectionsCount.decrementAndGet();
            log.debug("Unregistered SSE listener for tenant {}. Remaining: {}", tenantId, activeConnectionsCount.get());
        }
    }

    // Broadcasts an atomic delta change set to all connected client subscribers.
    public void broadcastDelta(String tenantId, ConfigDelta delta) {
        List<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        ConfigChangeEvent event = ConfigChangeEvent.ofDelta(tenantId, delta.getVersion(), delta);
        broadcastEvent(tenantId, emitters, event);
    }

    // Broadcasts a complete snapshot replacement to all connected client subscribers.
    public void broadcastSnapshot(String tenantId, TenantConfigSnapshot snapshot) {
        List<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        ConfigChangeEvent event = ConfigChangeEvent.ofSnapshot(tenantId, snapshot.getVersion(), snapshot);
        broadcastEvent(tenantId, emitters, event);
    }

    // Sends serialized JSON payload across all active emitters for a tenant.
    private void broadcastEvent(String tenantId, List<SseEmitter> emitters, ConfigChangeEvent event) {
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize push event: {}", e.getMessage());
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(jsonPayload));
            } catch (Exception e) {
                removeEmitter(tenantId, emitter);
            }
        }
        log.info("Broadcasted {} v{} to {} connected subscribers for tenant {}",
                event.getEventType(), event.getVersion(), emitters.size(), tenantId);
    }

    // Sends periodic heartbeat pings every 15 seconds to keep firewalls and connections open.
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeats() {
        for (Map.Entry<String, List<SseEmitter>> entry : tenantEmitters.entrySet()) {
            String tenantId = entry.getKey();
            List<SseEmitter> list = entry.getValue();
            for (SseEmitter emitter : list) {
                try {
                    ConfigChangeEvent hb = ConfigChangeEvent.heartbeat(tenantId, 0L);
                    emitter.send(SseEmitter.event().name("heartbeat").data(objectMapper.writeValueAsString(hb)));
                } catch (Exception e) {
                    removeEmitter(tenantId, emitter);
                }
            }
        }
    }

    // Returns total count of currently connected live streaming clients.
    public int getActiveConnectionsCount() {
        return activeConnectionsCount.get();
    }
}
