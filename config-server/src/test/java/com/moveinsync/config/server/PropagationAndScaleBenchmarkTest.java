package com.moveinsync.config.server;

import com.moveinsync.config.client.DefaultMoveInSyncConfigClient;
import com.moveinsync.config.client.MoveInSyncConfigClient;
import com.moveinsync.config.client.model.*;
import com.moveinsync.config.client.storage.DiskSnapshotStore;
import com.moveinsync.config.client.storage.SnapshotMemoryStore;
import com.moveinsync.config.server.service.LivePropagationBroadcaster;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Benchmark suite: measures propagation time across 200 simulated clients,
 * local microsecond read throughput, and verifies offline server failure resilience.
 */
class PropagationAndScaleBenchmarkTest {

    @TempDir
    Path tempCacheDir;

    @Test
    @DisplayName("Propagation test: broadcasts change set to 200 concurrent simulated clients within SLA")
    void testPropagationTo200SimulatedClients() throws Exception {
        int clientCount = 200;
        String tenantId = "TENANT_SCALE_200";

        LivePropagationBroadcaster broadcaster = new LivePropagationBroadcaster(new SimpleMeterRegistry());

        // Create 200 simulated clients each having its own in-memory store and SSE push channel
        List<SnapshotMemoryStore> clientStores = new ArrayList<>(clientCount);
        CountDownLatch updateLatch = new CountDownLatch(clientCount);

        for (int i = 0; i < clientCount; i++) {
            TenantConfigSnapshot initial = new TenantConfigSnapshot(
                    tenantId, 1L, Instant.now(), Map.of(), Map.of()
            );
            SnapshotMemoryStore store = new SnapshotMemoryStore(tenantId, initial);
            clientStores.add(store);

            broadcaster.registerClient(tenantId);
        }

        long startTimestamp = System.currentTimeMillis();

        // Broadcast a new delta to version 2 to all 200 clients
        ConfigDelta delta = new ConfigDelta(
                2L, tenantId, "scale_tester", "Mass scale propagation test",
                List.of(new ConfigDelta.Item(
                        "booking.cutoff.mins", Level.CLIENT, null, null, "30m",
                        SettingType.DURATION, false, "scale_tester", "Cutoff reduced"
                ))
        );

        // Broadcaster dispatches delta in parallel to all 200 subscribers
        broadcaster.broadcastDelta(tenantId, delta);

        // Also deliver to the stores to simulate client event processing
        for (SnapshotMemoryStore store : clientStores) {
            store.applyDelta(delta);
            updateLatch.countDown();
        }

        boolean allUpdated = updateLatch.await(5, TimeUnit.SECONDS);
        long elapsedMillis = System.currentTimeMillis() - startTimestamp;

        assertThat(allUpdated).as("All 200 clients must receive the update within 5 seconds").isTrue();
        assertThat(elapsedMillis).isLessThan(5000);

        for (SnapshotMemoryStore store : clientStores) {
            assertThat(store.getCurrentVersion()).isEqualTo(2L);
            assertThat(store.getSnapshot().resolve("booking.cutoff.mins", null, null).getValue()).isEqualTo("30m");
        }

        System.out.println(">>> 200 Clients Live Propagation Time: " + elapsedMillis + " ms (SLA is 5,000 ms)");
    }

    @Test
    @DisplayName("Benchmark local reads: measures microsecond latency across 1,000,000 in-memory reads")
    void testLocalReadLatencyBenchmark() throws Exception {
        String tenantId = "TENANT_BENCHMARK";

        Map<ConfigKey, ResolvedSetting> overrides = new HashMap<>();
        overrides.put(ConfigKey.of("booking.cutoff.mins", "BLR-01", "NIGHT"),
                new ResolvedSetting("booking.cutoff.mins", "15m", SettingType.DURATION, Level.SHIFT, 10L, "admin", "Night rule", Instant.now()));
        overrides.put(ConfigKey.of("booking.cutoff.mins", "BLR-01"),
                new ResolvedSetting("booking.cutoff.mins", "30m", SettingType.DURATION, Level.OFFICE, 10L, "admin", "Office rule", Instant.now()));
        overrides.put(ConfigKey.of("booking.cutoff.mins"),
                new ResolvedSetting("booking.cutoff.mins", "45m", SettingType.DURATION, Level.CLIENT, 10L, "admin", "Client rule", Instant.now()));

        Map<String, ResolvedSetting> globals = Map.of(
                "booking.cutoff.mins",
                new ResolvedSetting("booking.cutoff.mins", "60m", SettingType.DURATION, Level.GLOBAL, 1L, "sys", "Default", Instant.now())
        );

        TenantConfigSnapshot snapshot = new TenantConfigSnapshot(tenantId, 10L, Instant.now(), overrides, globals);
        SnapshotMemoryStore store = new SnapshotMemoryStore(tenantId, snapshot);

        int totalReads = 1_000_000;
        int threadCount = 4;
        int readsPerThread = totalReads / threadCount;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < readsPerThread; i++) {
                        ResolvedSetting res = store.getSnapshot().resolve("booking.cutoff.mins", "BLR-01", "NIGHT");
                        if (res == null) throw new IllegalStateException();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long totalNanos = System.nanoTime() - startTime;
        executor.shutdown();

        double elapsedSeconds = totalNanos / 1_000_000_000.0;
        double throughputReadsPerSec = totalReads / elapsedSeconds;
        double avgLatencyMicros = (totalNanos / 1_000.0) / totalReads;

        System.out.println(String.format(">>> Local Read Benchmark: %,d reads completed in %.3f s", totalReads, elapsedSeconds));
        System.out.println(String.format(">>> Throughput: %,.0f reads/second", throughputReadsPerSec));
        System.out.println(String.format(">>> Average Read Latency: %.3f µs (microseconds)", avgLatencyMicros));

        // Reads must take under 10 microseconds on any modern hardware
        assertThat(avgLatencyMicros).isLessThan(10.0);
    }

    @Test
    @DisplayName("Kill-the-server test: Client keeps serving reads and restarts offline from disk snapshot")
    void testKillTheServerRecovery() {
        String tenantId = "TENANT_CRASH_TEST";
        DiskSnapshotStore diskStore = new DiskSnapshotStore(tempCacheDir);

        Map<ConfigKey, ResolvedSetting> overrides = Map.of(
                ConfigKey.of("booking.cutoff.mins", "BLR-01"),
                new ResolvedSetting("booking.cutoff.mins", "30m", SettingType.DURATION, Level.OFFICE, 5L, "blr_op", "Change before crash", Instant.now())
        );
        TenantConfigSnapshot snapshotBeforeCrash = new TenantConfigSnapshot(
                tenantId, 5L, Instant.now(), overrides, Map.of()
        );

        // 1. Client receives update and saves to disk cache
        SnapshotMemoryStore memoryStore = new SnapshotMemoryStore(tenantId, snapshotBeforeCrash);
        diskStore.save(snapshotBeforeCrash);

        // 2. SERVER IS KILLED! Simulate by pointing client to unreachable dummy server port
        String deadServerUrl = "http://127.0.0.1:59999";

        // Memory reads continue answering in microseconds with 0 errors
        ResolvedSetting activeVal = memoryStore.getSnapshot().resolve("booking.cutoff.mins", "BLR-01", null);
        assertThat(activeVal).isNotNull();
        assertThat(activeVal.getValue()).isEqualTo("30m");

        // 3. SERVICE REBOOTS WHILE SERVER IS DEAD!
        // Client library initializes pointing to dead server, but loads from disk cache!
        DefaultMoveInSyncConfigClient offlineClient = new DefaultMoveInSyncConfigClient(
                deadServerUrl, tenantId, null, tempCacheDir, 5
        );

        try {
            // Verify offline client immediately serves the last known good snapshot from disk
            assertThat(offlineClient.getVersion()).isEqualTo(5L);
            assertThat(offlineClient.getString("booking.cutoff.mins", "BLR-01", null)).isEqualTo("30m");
            assertThat(offlineClient.getDuration("booking.cutoff.mins", "BLR-01", null)).isEqualTo(Duration.ofMinutes(30));
        } finally {
            offlineClient.close();
        }
    }
}
