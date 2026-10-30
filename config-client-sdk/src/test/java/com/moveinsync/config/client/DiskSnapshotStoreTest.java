package com.moveinsync.config.client;

import com.moveinsync.config.client.model.ConfigKey;
import com.moveinsync.config.client.model.Level;
import com.moveinsync.config.client.model.ResolvedSetting;
import com.moveinsync.config.client.model.SettingType;
import com.moveinsync.config.client.model.TenantConfigSnapshot;
import com.moveinsync.config.client.storage.DiskSnapshotStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests persisting and loading snapshots to/from local disk for offline fault tolerance.
 */
class DiskSnapshotStoreTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Saves snapshot to disk and reloads it cleanly on restart")
    void testSaveAndLoadFromDisk() {
        DiskSnapshotStore diskStore = new DiskSnapshotStore(tempDir);

        Map<ConfigKey, ResolvedSetting> overrides = Map.of(
                ConfigKey.of("night_safety.enabled"),
                new ResolvedSetting("night_safety.enabled", "true", SettingType.BOOLEAN, Level.CLIENT, 10L, "admin", "Safety rule", Instant.now())
        );

        TenantConfigSnapshot snapshot = new TenantConfigSnapshot(
                "CLIENT_XYZ", 10L, Instant.now(), overrides, Map.of()
        );

        diskStore.save(snapshot);
        assertThat(diskStore.hasCachedSnapshot("CLIENT_XYZ")).isTrue();

        // Simulate new store instance loading from disk (server down scenario)
        DiskSnapshotStore freshStore = new DiskSnapshotStore(tempDir);
        TenantConfigSnapshot loaded = freshStore.load("CLIENT_XYZ");

        assertThat(loaded).isNotNull();
        assertThat(loaded.getTenantId()).isEqualTo("CLIENT_XYZ");
        assertThat(loaded.getVersion()).isEqualTo(10L);
        assertThat(loaded.resolve("night_safety.enabled", null, null).getValue()).isEqualTo("true");
    }
}
