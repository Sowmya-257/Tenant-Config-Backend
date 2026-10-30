package com.moveinsync.config.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moveinsync.config.client.metrics.ClientMetrics;
import com.moveinsync.config.client.model.*;
import com.moveinsync.config.client.storage.DiskSnapshotStore;
import com.moveinsync.config.client.storage.SnapshotMemoryStore;
import com.moveinsync.config.client.sync.JitteredBackoff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standard production implementation of MoveInSync embedded configuration client.
 */
public class DefaultMoveInSyncConfigClient implements MoveInSyncConfigClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultMoveInSyncConfigClient.class);

    private final String serverBaseUrl;
    private final String tenantId;
    private final String authToken;
    private volatile HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SnapshotMemoryStore memoryStore;
    private final DiskSnapshotStore diskStore;
    private final ClientMetrics metrics;
    private final JitteredBackoff jitteredBackoff;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService sseWorker;
    private final AtomicBoolean running = new AtomicBoolean(true);

    // Creates and initializes the client, loading initial snapshot from server or disk cache.
    public DefaultMoveInSyncConfigClient(String serverBaseUrl,
                                         String tenantId,
                                         String authToken,
                                         Path cacheDir,
                                         long reconcileIntervalSeconds) {
        this.serverBaseUrl = serverBaseUrl.endsWith("/") ? serverBaseUrl.substring(0, serverBaseUrl.length() - 1) : serverBaseUrl;
        this.tenantId = tenantId;
        this.authToken = authToken;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.diskStore = new DiskSnapshotStore(cacheDir);
        this.metrics = new ClientMetrics();
        this.jitteredBackoff = new JitteredBackoff(200, 3000);
        this.memoryStore = new SnapshotMemoryStore(tenantId, null);

        // Bootstrap: load from server, or fallback to disk if server is unreachable
        bootstrapSnapshot();

        // Start background synchronization
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "config-reconciler-" + tenantId);
            t.setDaemon(true);
            return t;
        });

        this.sseWorker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "config-sse-" + tenantId);
            t.setDaemon(true);
            return t;
        });

        startPeriodicReconciliation(reconcileIntervalSeconds);
        startSsePushListener();
    }

    // Lazily initializes and returns the HTTP client.
    private HttpClient getOrCreateHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    try {
                        httpClient = HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(3))
                                .build();
                    } catch (Exception e) {
                        log.warn("Could not initialize HttpClient (offline/sandbox environment): {}", e.getMessage());
                        return null;
                    }
                }
            }
        }
        return httpClient;
    }

    // Loads initial config from server, or falls back to disk cache if server is offline.
    private void bootstrapSnapshot() {
        boolean loadedFromServer = fetchSnapshotFromServer();
        if (!loadedFromServer) {
            TenantConfigSnapshot diskSnapshot = diskStore.load(tenantId);
            if (diskSnapshot != null) {
                memoryStore.updateSnapshot(diskSnapshot);
                log.warn("Server unavailable; started using offline disk snapshot v{} for tenant {}",
                        diskSnapshot.getVersion(), tenantId);
            } else {
                TenantConfigSnapshot empty = new TenantConfigSnapshot(tenantId, 0L, Instant.now(), Map.of(), Map.of());
                memoryStore.updateSnapshot(empty);
                log.warn("Starting with blank snapshot v0 for tenant {}", tenantId);
            }
        }
    }

    // Fetches full snapshot from the configuration server over HTTP.
    private boolean fetchSnapshotFromServer() {
        try {
            HttpClient client = getOrCreateHttpClient();
            if (client == null) {
                return false;
            }
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serverBaseUrl + "/api/v1/sync/snapshot?tenantId=" + tenantId))
                    .timeout(Duration.ofSeconds(4))
                    .GET();
            if (authToken != null && !authToken.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + authToken);
            }

            HttpResponse<String> resp = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                TenantConfigSnapshot snapshot = objectMapper.readValue(resp.body(), TenantConfigSnapshot.class);
                memoryStore.updateSnapshot(snapshot);
                diskStore.save(snapshot);
                metrics.recordSnapshotApplied();
                return true;
            } else {
                log.warn("Snapshot fetch returned HTTP {}: {}", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.warn("Failed to reach config server during bootstrap: {}", e.getMessage());
        }
        return false;
    }

    // Starts background SSE push stream to receive real-time configuration changes.
    private void startSsePushListener() {
        sseWorker.submit(() -> {
            int retryAttempt = 0;
            while (running.get()) {
                try {
                    connectToPushStream();
                    retryAttempt = 0; // Reset backoff on successful connection
                } catch (Exception e) {
                    if (!running.get()) break;
                    metrics.recordReconnect();
                    long delay = jitteredBackoff.calculateDelayMillis(retryAttempt++);
                    log.debug("SSE push disconnected; retrying in {} ms: {}", delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // Heal state after reconnect
                    reconcileWithServer();
                }
            }
        });
    }

    // Establishes HTTP streaming connection and processes incoming server events.
    private void connectToPushStream() throws Exception {
        HttpClient client = getOrCreateHttpClient();
        if (client == null) {
            throw new IllegalStateException("HttpClient unavailable");
        }
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/api/v1/stream/configs/" + tenantId))
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofMinutes(10))
                .GET();
        if (authToken != null && !authToken.isEmpty()) {
            reqBuilder.header("Authorization", "Bearer " + authToken);
        }

        HttpResponse<java.io.InputStream> response = client.send(
                reqBuilder.build(), HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("SSE endpoint responded with status " + response.statusCode());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            StringBuilder eventData = new StringBuilder();
            while (running.get() && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("data:")) {
                    eventData.append(line.substring(5).trim());
                } else if (line.isEmpty() && eventData.length() > 0) {
                    processIncomingEventJson(eventData.toString());
                    eventData.setLength(0);
                }
            }
        }
    }

    // Deserializes and applies push event, routing deltas or full snapshots into memory.
    public void processIncomingEventJson(String json) {
        try {
            ConfigChangeEvent event = objectMapper.readValue(json, ConfigChangeEvent.class);
            metrics.recordUpdateReceived();
            long lag = Duration.between(event.getTimestamp(), Instant.now()).toMillis();
            metrics.recordSyncLag(lag);

            if (event.getEventType() == ConfigChangeEvent.EventType.DELTA && event.getDelta() != null) {
                applyDeltaEvent(event.getDelta());
            } else if (event.getEventType() == ConfigChangeEvent.EventType.FULL_SNAPSHOT && event.getSnapshot() != null) {
                boolean applied = memoryStore.updateSnapshot(event.getSnapshot());
                if (applied) {
                    diskStore.save(event.getSnapshot());
                    metrics.recordSnapshotApplied();
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse incoming change event JSON: {}", json, e);
        }
    }

    // Applies a delta or triggers reconciliation if a version gap is observed.
    private void applyDeltaEvent(ConfigDelta delta) {
        long currentVersion = memoryStore.getCurrentVersion();
        long incomingVersion = delta.getVersion();

        if (incomingVersion <= currentVersion) {
            metrics.recordRejection();
            log.warn("Dropping out-of-order delta v{} (current is v{})", incomingVersion, currentVersion);
            return;
        }

        if (incomingVersion == currentVersion + 1) {
            boolean applied = memoryStore.applyDelta(delta);
            if (applied) {
                diskStore.save(memoryStore.getSnapshot());
                metrics.recordDeltaApplied();
            }
        } else {
            // Version gap detected: e.g., current is 40, incoming is 43 (missed 41, 42)
            log.info("Detected version gap: current v{}, received v{}; catching up via delta sync",
                    currentVersion, incomingVersion);
            reconcileWithServer();
        }
    }

    // Schedules a periodic background poller to guarantee bounded staleness under 5 seconds.
    private void startPeriodicReconciliation(long intervalSeconds) {
        long interval = Math.max(1, intervalSeconds);
        scheduler.scheduleWithFixedDelay(this::reconcileWithServer, interval, interval, TimeUnit.SECONDS);
    }

    // Checks latest version on server and fetches missing changes if client has fallen behind.
    public void reconcileWithServer() {
        if (!running.get()) return;
        try {
            HttpClient client = getOrCreateHttpClient();
            if (client == null) return;
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serverBaseUrl + "/api/v1/sync/version?tenantId=" + tenantId))
                    .timeout(Duration.ofSeconds(3))
                    .GET();
            if (authToken != null && !authToken.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + authToken);
            }

            HttpResponse<String> resp = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                long serverVersion = Long.parseLong(resp.body().trim());
                long localVersion = memoryStore.getCurrentVersion();
                if (serverVersion > localVersion) {
                    log.info("Reconciliation triggered: local v{} < server v{}", localVersion, serverVersion);
                    syncDeltasOrSnapshot(localVersion, serverVersion);
                }
            }
        } catch (Exception e) {
            log.debug("Periodic version check failed (server down?): {}", e.getMessage());
        }
    }

    // Downloads deltas since local version or retrieves full snapshot if the gap is too large.
    private void syncDeltasOrSnapshot(long sinceVersion, long targetVersion) {
        try {
            HttpClient client = getOrCreateHttpClient();
            if (client == null) return;
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serverBaseUrl + "/api/v1/sync/delta?tenantId=" + tenantId + "&sinceVersion=" + sinceVersion))
                    .timeout(Duration.ofSeconds(4))
                    .GET();
            if (authToken != null && !authToken.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + authToken);
            }

            HttpResponse<String> resp = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                List<ConfigDelta> deltas = objectMapper.readValue(resp.body(), new TypeReference<List<ConfigDelta>>() {});
                for (ConfigDelta delta : deltas) {
                    if (delta.getVersion() > memoryStore.getCurrentVersion()) {
                        memoryStore.applyDelta(delta);
                        metrics.recordDeltaApplied();
                    }
                }
                diskStore.save(memoryStore.getSnapshot());
                return;
            }
        } catch (Exception e) {
            log.warn("Delta sync failed; falling back to full snapshot: {}", e.getMessage());
        }
        // Fallback to full snapshot
        fetchSnapshotFromServer();
    }

    // Returns the client tenant identifier this instance represents.
    @Override
    public String getTenantId() {
        return tenantId;
    }

    // Returns the current snapshot version active in memory.
    @Override
    public long getVersion() {
        return memoryStore.getCurrentVersion();
    }

    // Resolves a string configuration value for the entire client.
    @Override
    public String getString(String key) {
        return getString(key, null, null);
    }

    // Resolves a string configuration value for a specific office and shift.
    @Override
    public String getString(String key, String officeId, String shiftId) {
        ResolvedSetting s = getResolved(key, officeId, shiftId);
        return s != null ? s.getValue() : null;
    }

    // Resolves a numeric configuration value for the entire client.
    @Override
    public Long getNumber(String key) {
        return getNumber(key, null, null);
    }

    // Resolves a numeric configuration value for a specific office and shift.
    @Override
    public Long getNumber(String key, String officeId, String shiftId) {
        String val = getString(key, officeId, shiftId);
        return val != null ? Long.parseLong(val.trim()) : null;
    }

    // Resolves a duration configuration value for the entire client.
    @Override
    public Duration getDuration(String key) {
        return getDuration(key, null, null);
    }

    // Resolves a duration configuration value for a specific office and shift.
    @Override
    public Duration getDuration(String key, String officeId, String shiftId) {
        String val = getString(key, officeId, shiftId);
        if (val == null) return null;
        String trimmed = val.trim().toLowerCase();
        if (trimmed.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        } else if (trimmed.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        } else if (trimmed.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        } else {
            // default parse as minutes
            return Duration.ofMinutes(Long.parseLong(trimmed));
        }
    }

    // Resolves an operating time window for the entire client.
    @Override
    public TimeWindow getTimeWindow(String key) {
        return getTimeWindow(key, null, null);
    }

    // Resolves an operating time window for a specific office and shift.
    @Override
    public TimeWindow getTimeWindow(String key, String officeId, String shiftId) {
        String val = getString(key, officeId, shiftId);
        return val != null ? TimeWindow.parse(val) : null;
    }

    // Resolves a boolean flag for the entire client.
    @Override
    public Boolean getBoolean(String key) {
        return getBoolean(key, null, null);
    }

    // Resolves a boolean flag for a specific office and shift.
    @Override
    public Boolean getBoolean(String key, String officeId, String shiftId) {
        String val = getString(key, officeId, shiftId);
        return val != null ? Boolean.parseBoolean(val.trim()) : null;
    }

    // Resolves a list of strings for the entire client.
    @Override
    public List<String> getList(String key) {
        return getList(key, null, null);
    }

    // Resolves a list of strings for a specific office and shift.
    @Override
    public List<String> getList(String key, String officeId, String shiftId) {
        String val = getString(key, officeId, shiftId);
        if (val == null || val.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = val.split(",");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            result.add(p.trim());
        }
        return Collections.unmodifiableList(result);
    }

    // Returns full metadata and resolution breakdown for a setting in microseconds.
    @Override
    public ResolvedSetting getResolved(String key, String officeId, String shiftId) {
        long startNanos = System.nanoTime();
        try {
            TenantConfigSnapshot snapshot = memoryStore.getSnapshot();
            if (snapshot == null) {
                return null;
            }
            return snapshot.resolve(key, officeId, shiftId);
        } finally {
            metrics.recordRead(System.nanoTime() - startNanos);
        }
    }

    // Explains how the setting resolved across SHIFT, OFFICE, CLIENT, and GLOBAL levels.
    @Override
    public ExplainResult explain(String key, String officeId, String shiftId) {
        TenantConfigSnapshot snapshot = memoryStore.getSnapshot();
        if (snapshot == null) {
            throw new SettingNotFoundException("Client uninitialized or no snapshot available");
        }
        return snapshot.explain(key, officeId, shiftId);
    }

    // Captures an immutable snapshot reference for multi-key atomic reads.
    @Override
    public TenantConfigSnapshot captureSnapshot() {
        return memoryStore.getSnapshot();
    }

    // Manually triggers a delta or snapshot synchronization check with the server.
    @Override
    public void forceSync() {
        reconcileWithServer();
    }

    // Returns client performance counters and latency metrics.
    @Override
    public ClientMetrics getMetrics() {
        return metrics;
    }

    // Shuts down background threads, listeners, and releases resources.
    @Override
    public void close() {
        if (running.compareAndSet(true, false)) {
            scheduler.shutdownNow();
            sseWorker.shutdownNow();
            log.info("Closed MoveInSync config client for tenant {}", tenantId);
        }
    }
}
