package com.moveinsync.config.server;

import com.moveinsync.config.client.model.*;
import com.moveinsync.config.client.storage.SnapshotMemoryStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test proving that continuous concurrent readers never observe mixed / partial states
 * during atomic multi-key change sets.
 */
class ConcurrencyAtomicChangeSetTest {

    @Test
    @DisplayName("Continuous concurrent readers never observe a mixed state during atomic multi-key change sets")
    void testContinuousReadersNeverSeeMixedState() throws Exception {
        String tenantId = "TENANT_CONCURRENCY";

        // Initial state: Night Window is 20:00 to 06:00
        Map<ConfigKey, ResolvedSetting> initialOverrides = Map.of(
                ConfigKey.of("night_safety.start"),
                new ResolvedSetting("night_safety.start", "20:00", SettingType.STRING, Level.CLIENT, 1L, "sys", "init", Instant.now()),
                ConfigKey.of("night_safety.end"),
                new ResolvedSetting("night_safety.end", "06:00", SettingType.STRING, Level.CLIENT, 1L, "sys", "init", Instant.now())
        );

        TenantConfigSnapshot initialSnapshot = new TenantConfigSnapshot(
                tenantId, 1L, Instant.now(), initialOverrides, Map.of()
        );

        SnapshotMemoryStore memoryStore = new SnapshotMemoryStore(tenantId, initialSnapshot);

        int readerThreadsCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(readerThreadsCount + 1);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger mixedStatesDetected = new AtomicInteger(0);
        AtomicInteger totalReadsCompleted = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);

        // 8 reader threads reading continuously in tight loops
        for (int i = 0; i < readerThreadsCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    return;
                }

                while (running.get()) {
                    // Capture point-in-time immutable snapshot
                    TenantConfigSnapshot snapshot = memoryStore.getSnapshot();
                    ResolvedSetting start = snapshot.resolve("night_safety.start", null, null);
                    ResolvedSetting end = snapshot.resolve("night_safety.end", null, null);

                    if (start != null && end != null) {
                        String sVal = start.getValue();
                        String eVal = end.getValue();

                        boolean validState1 = "20:00".equals(sVal) && "06:00".equals(eVal);
                        boolean validState2 = "22:00".equals(sVal) && "05:00".equals(eVal);

                        if (!validState1 && !validState2) {
                            // Mixed state observed!
                            mixedStatesDetected.incrementAndGet();
                        }
                    }
                    totalReadsCompleted.incrementAndGet();
                }
            });
        }

        startLatch.countDown();

        // Perform multiple atomic back-and-forth updates on writer thread
        for (long v = 2; v <= 50; v++) {
            boolean isState2 = (v % 2 == 0);
            String newStart = isState2 ? "22:00" : "20:00";
            String newEnd = isState2 ? "05:00" : "06:00";

            ConfigDelta delta = new ConfigDelta(
                    v, tenantId, "operator", "Atomic shift change",
                    List.of(
                            new ConfigDelta.Item("night_safety.start", Level.CLIENT, null, null, newStart, SettingType.STRING, false, "op", "change"),
                            new ConfigDelta.Item("night_safety.end", Level.CLIENT, null, null, newEnd, SettingType.STRING, false, "op", "change")
                    )
            );

            memoryStore.applyDelta(delta);
            Thread.sleep(5);
        }

        running.set(false);
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);

        assertThat(totalReadsCompleted.get()).isGreaterThan(50_000);
        assertThat(mixedStatesDetected.get())
                .as("Atomic change sets must guarantee zero partial / mixed states")
                .isEqualTo(0);
    }
}
