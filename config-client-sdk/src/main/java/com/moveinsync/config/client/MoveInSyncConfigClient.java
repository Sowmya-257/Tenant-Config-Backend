package com.moveinsync.config.client;

import com.moveinsync.config.client.metrics.ClientMetrics;
import com.moveinsync.config.client.model.ExplainResult;
import com.moveinsync.config.client.model.ResolvedSetting;
import com.moveinsync.config.client.model.TenantConfigSnapshot;
import com.moveinsync.config.client.model.TimeWindow;

import java.io.Closeable;
import java.time.Duration;
import java.util.List;

/**
 * Public client interface embedded into services for instant in-memory configuration reads.
 */
public interface MoveInSyncConfigClient extends Closeable {

    // Returns the client tenant identifier this instance represents.
    String getTenantId();

    // Returns the current snapshot version active in memory.
    long getVersion();

    // Resolves a string configuration value for the entire client.
    String getString(String key);

    // Resolves a string configuration value for a specific office and shift.
    String getString(String key, String officeId, String shiftId);

    // Resolves a numeric configuration value for the entire client.
    Long getNumber(String key);

    // Resolves a numeric configuration value for a specific office and shift.
    Long getNumber(String key, String officeId, String shiftId);

    // Resolves a duration configuration value for the entire client.
    Duration getDuration(String key);

    // Resolves a duration configuration value for a specific office and shift.
    Duration getDuration(String key, String officeId, String shiftId);

    // Resolves an operating time window for the entire client.
    TimeWindow getTimeWindow(String key);

    // Resolves an operating time window for a specific office and shift.
    TimeWindow getTimeWindow(String key, String officeId, String shiftId);

    // Resolves a boolean flag for the entire client.
    Boolean getBoolean(String key);

    // Resolves a boolean flag for a specific office and shift.
    Boolean getBoolean(String key, String officeId, String shiftId);

    // Resolves a list of strings for the entire client.
    List<String> getList(String key);

    // Resolves a list of strings for a specific office and shift.
    List<String> getList(String key, String officeId, String shiftId);

    // Returns full metadata and resolution breakdown for a setting.
    ResolvedSetting getResolved(String key, String officeId, String shiftId);

    // Explains how the setting resolved across SHIFT, OFFICE, CLIENT, and GLOBAL levels.
    ExplainResult explain(String key, String officeId, String shiftId);

    // Captures an immutable snapshot reference for multi-key atomic reads.
    TenantConfigSnapshot captureSnapshot();

    // Manually triggers a delta or snapshot synchronization check with the server.
    void forceSync();

    // Returns client performance counters and latency metrics.
    ClientMetrics getMetrics();

    // Shuts down background threads, listeners, and releases resources.
    @Override
    void close();
}
