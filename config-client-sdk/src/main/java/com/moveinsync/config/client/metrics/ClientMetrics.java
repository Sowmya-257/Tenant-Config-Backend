package com.moveinsync.config.client.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects microsecond-level metrics for client read and synchronization performance.
 */
public class ClientMetrics {

    private final AtomicLong readsTotal = new AtomicLong(0);
    private final AtomicLong readDurationNanosTotal = new AtomicLong(0);
    private final AtomicLong updatesReceived = new AtomicLong(0);
    private final AtomicLong deltasApplied = new AtomicLong(0);
    private final AtomicLong fullSnapshotsApplied = new AtomicLong(0);
    private final AtomicLong rejectionsTotal = new AtomicLong(0);
    private final AtomicLong reconnectsTotal = new AtomicLong(0);
    private final AtomicLong lastSyncLagMillis = new AtomicLong(0);

    // Records the latency of a single local configuration read.
    public void recordRead(long nanos) {
        readsTotal.incrementAndGet();
        readDurationNanosTotal.addAndGet(nanos);
    }

    // Increments counter when a change event arrives from the server.
    public void recordUpdateReceived() {
        updatesReceived.incrementAndGet();
    }

    // Increments counter when a delta is applied to the local snapshot.
    public void recordDeltaApplied() {
        deltasApplied.incrementAndGet();
    }

    // Increments counter when a full snapshot replaces local memory.
    public void recordSnapshotApplied() {
        fullSnapshotsApplied.incrementAndGet();
    }

    // Increments counter when an out-of-order or stale version is rejected.
    public void recordRejection() {
        rejectionsTotal.incrementAndGet();
    }

    // Increments counter when a reconnect attempt occurs.
    public void recordReconnect() {
        reconnectsTotal.incrementAndGet();
    }

    // Records the propagation delay between server publish time and client receive time.
    public void recordSyncLag(long lagMillis) {
        lastSyncLagMillis.set(Math.max(0, lagMillis));
    }

    // Returns the total number of reads performed by this client.
    public long getReadsTotal() {
        return readsTotal.get();
    }

    // Returns average read latency in microseconds.
    public double getAverageReadLatencyMicros() {
        long count = readsTotal.get();
        if (count == 0) return 0.0;
        return (double) readDurationNanosTotal.get() / (count * 1_000.0);
    }

    // Returns the total number of update events received.
    public long getUpdatesReceived() {
        return updatesReceived.get();
    }

    // Returns the total number of deltas successfully applied.
    public long getDeltasApplied() {
        return deltasApplied.get();
    }

    // Returns the total number of full snapshots applied.
    public long getFullSnapshotsApplied() {
        return fullSnapshotsApplied.get();
    }

    // Returns the total number of rejected stale or out-of-order versions.
    public long getRejectionsTotal() {
        return rejectionsTotal.get();
    }

    // Returns the count of client reconnects.
    public long getReconnectsTotal() {
        return reconnectsTotal.get();
    }

    // Returns the most recent synchronization lag in milliseconds.
    public long getLastSyncLagMillis() {
        return lastSyncLagMillis.get();
    }
}
