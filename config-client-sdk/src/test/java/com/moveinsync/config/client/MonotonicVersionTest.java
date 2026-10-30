package com.moveinsync.config.client;

import com.moveinsync.config.client.model.ConfigDelta;
import com.moveinsync.config.client.model.Level;
import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.client.model.TenantConfigSnapshot;
import com.moveinsync.config.client.storage.SnapshotMemoryStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that version numbers must strictly move forward and stale versions are rejected.
 */
class MonotonicVersionTest {

    @Test
    @DisplayName("Monotonic version test: version 41 arriving after version 43 is rejected")
    void testStaleVersionRejected() {
        TenantConfigSnapshot baseSnapshot = new TenantConfigSnapshot(
                "TENANT_A", 43L, Instant.now(), Map.of(), Map.of()
        );
        SnapshotMemoryStore store = new SnapshotMemoryStore("TENANT_A", baseSnapshot);

        assertThat(store.getCurrentVersion()).isEqualTo(43L);

        // Attempt to apply older version 41
        ConfigDelta staleDelta = new ConfigDelta(
                41L, "TENANT_A", "delayed_sender", "Delayed network packet",
                List.of(new ConfigDelta.Item(
                        "booking.cutoff.mins", Level.CLIENT, null, null, "10",
                        SettingType.NUMBER, false, "delayed_sender", "Late update"
                ))
        );

        boolean appliedStale = store.applyDelta(staleDelta);
        assertThat(appliedStale).isFalse();
        assertThat(store.getCurrentVersion()).isEqualTo(43L);
        assertThat(store.getRejectedStaleVersionsCount()).isEqualTo(1L);

        // Attempt to apply same version 43 again (duplicate)
        ConfigDelta duplicateDelta = new ConfigDelta(
                43L, "TENANT_A", "repeat_sender", "Duplicate packet", List.of()
        );
        boolean appliedDuplicate = store.applyDelta(duplicateDelta);
        assertThat(appliedDuplicate).isFalse();
        assertThat(store.getCurrentVersion()).isEqualTo(43L);
        assertThat(store.getRejectedStaleVersionsCount()).isEqualTo(2L);

        // Newer version 44 is accepted
        ConfigDelta forwardDelta = new ConfigDelta(
                44L, "TENANT_A", "valid_operator", "Legitimate new change",
                List.of(new ConfigDelta.Item(
                        "booking.cutoff.mins", Level.CLIENT, null, null, "25",
                        SettingType.NUMBER, false, "valid_operator", "New rule"
                ))
        );
        boolean appliedForward = store.applyDelta(forwardDelta);
        assertThat(appliedForward).isTrue();
        assertThat(store.getCurrentVersion()).isEqualTo(44L);
    }
}
