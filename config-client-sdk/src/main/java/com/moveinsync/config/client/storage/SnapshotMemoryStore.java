package com.moveinsync.config.client.storage;

import com.moveinsync.config.client.model.ConfigDelta;
import com.moveinsync.config.client.model.TenantConfigSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory thread-safe storage for the tenant configuration snapshot.
 */
public class SnapshotMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(SnapshotMemoryStore.class);

    private final String tenantId;
    private final AtomicReference<TenantConfigSnapshot> currentSnapshot;
    private final AtomicLong rejectedStaleVersions;

    // Initializes memory store with an optional initial snapshot.
    public SnapshotMemoryStore(String tenantId, TenantConfigSnapshot initialSnapshot) {
        this.tenantId = tenantId;
        this.currentSnapshot = new AtomicReference<>(initialSnapshot);
        this.rejectedStaleVersions = new AtomicLong(0);
    }

    // Atomically replaces the current snapshot with a newer version.
    public boolean updateSnapshot(TenantConfigSnapshot newSnapshot) {
        if (newSnapshot == null) {
            return false;
        }
        while (true) {
            TenantConfigSnapshot current = currentSnapshot.get();
            long currentVer = (current != null) ? current.getVersion() : -1L;
            if (newSnapshot.getVersion() < currentVer) {
                rejectedStaleVersions.incrementAndGet();
                log.warn("Rejected stale snapshot v{} (current is v{}) for tenant {}",
                        newSnapshot.getVersion(), currentVer, tenantId);
                return false;
            }
            if (currentSnapshot.compareAndSet(current, newSnapshot)) {
                log.info("Applied full snapshot v{} for tenant {}", newSnapshot.getVersion(), tenantId);
                return true;
            }
        }
    }

    // Applies a delta change set atomically to produce a new immutable snapshot.
    public boolean applyDelta(ConfigDelta delta) {
        if (delta == null) {
            return false;
        }
        while (true) {
            TenantConfigSnapshot current = currentSnapshot.get();
            long currentVer = (current != null) ? current.getVersion() : 0L;

            // Reject stale versions (versions must strictly move forward)
            if (delta.getVersion() <= currentVer) {
                rejectedStaleVersions.incrementAndGet();
                log.warn("Rejected stale delta v{} (current is v{}) for tenant {}",
                        delta.getVersion(), currentVer, tenantId);
                return false;
            }

            // Check if delta is exactly next expected version
            if (current == null) {
                log.warn("Cannot apply delta v{} without a base snapshot for tenant {}", delta.getVersion(), tenantId);
                return false;
            }

            TenantConfigSnapshot updated = current.applyDelta(delta);
            if (currentSnapshot.compareAndSet(current, updated)) {
                log.info("Applied delta v{} (author: '{}', reason: '{}') for tenant {}",
                        delta.getVersion(), delta.getAuthor(), delta.getReason(), tenantId);
                return true;
            }
        }
    }

    // Returns the current snapshot reference for reading.
    public TenantConfigSnapshot getSnapshot() {
        return currentSnapshot.get();
    }

    // Returns the current version number or -1 if uninitialized.
    public long getCurrentVersion() {
        TenantConfigSnapshot s = currentSnapshot.get();
        return s != null ? s.getVersion() : -1L;
    }

    // Returns the total count of stale or out-of-order versions rejected.
    public long getRejectedStaleVersionsCount() {
        return rejectedStaleVersions.get();
    }
}
