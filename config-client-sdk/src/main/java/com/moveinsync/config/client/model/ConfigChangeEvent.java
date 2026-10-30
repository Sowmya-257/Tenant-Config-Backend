package com.moveinsync.config.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Message payload emitted over push streams when configuration changes occur.
 */
public class ConfigChangeEvent {

    public enum EventType {
        DELTA,
        FULL_SNAPSHOT,
        HEARTBEAT
    }

    private final EventType eventType;
    private final String tenantId;
    private final Long version;
    private final ConfigDelta delta;
    private final TenantConfigSnapshot snapshot;
    private final Instant timestamp;

    // Constructs a live push notification event.
    @JsonCreator
    public ConfigChangeEvent(@JsonProperty("eventType") EventType eventType,
                             @JsonProperty("tenantId") String tenantId,
                             @JsonProperty("version") Long version,
                             @JsonProperty("delta") ConfigDelta delta,
                             @JsonProperty("snapshot") TenantConfigSnapshot snapshot,
                             @JsonProperty("timestamp") Instant timestamp) {
        this.eventType = eventType;
        this.tenantId = tenantId;
        this.version = version;
        this.delta = delta;
        this.snapshot = snapshot;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    // Creates an event containing a delta change set.
    public static ConfigChangeEvent ofDelta(String tenantId, Long version, ConfigDelta delta) {
        return new ConfigChangeEvent(EventType.DELTA, tenantId, version, delta, null, Instant.now());
    }

    // Creates an event containing a full snapshot replacement.
    public static ConfigChangeEvent ofSnapshot(String tenantId, Long version, TenantConfigSnapshot snapshot) {
        return new ConfigChangeEvent(EventType.FULL_SNAPSHOT, tenantId, version, null, snapshot, Instant.now());
    }

    // Creates a heartbeat event to keep the push stream alive.
    public static ConfigChangeEvent heartbeat(String tenantId, Long version) {
        return new ConfigChangeEvent(EventType.HEARTBEAT, tenantId, version, null, null, Instant.now());
    }

    // Returns the type of push event.
    public EventType getEventType() {
        return eventType;
    }

    // Returns the tenant id.
    public String getTenantId() {
        return tenantId;
    }

    // Returns the new version after this event.
    public Long getVersion() {
        return version;
    }

    // Returns the delta payload if event type is DELTA.
    public ConfigDelta getDelta() {
        return delta;
    }

    // Returns the snapshot payload if event type is FULL_SNAPSHOT.
    public TenantConfigSnapshot getSnapshot() {
        return snapshot;
    }

    // Returns the emission timestamp of this event.
    public Instant getTimestamp() {
        return timestamp;
    }
}
