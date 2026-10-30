package com.moveinsync.config.client.sync;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Calculates exponential backoff with randomized jitter to prevent reconnect storms.
 */
public class JitteredBackoff {

    private final long baseDelayMillis;
    private final long maxDelayMillis;

    // Constructs backoff configuration with base delay and maximum ceiling.
    public JitteredBackoff(long baseDelayMillis, long maxDelayMillis) {
        this.baseDelayMillis = Math.max(50, baseDelayMillis);
        this.maxDelayMillis = Math.max(this.baseDelayMillis, maxDelayMillis);
    }

    // Computes randomized wait time for a reconnect retry attempt using Full Jitter.
    public long calculateDelayMillis(int retryAttempt) {
        int cappedAttempt = Math.min(retryAttempt, 10);
        long exponentialCeiling = (long) (baseDelayMillis * Math.pow(2, cappedAttempt));
        long boundedCeiling = Math.min(exponentialCeiling, maxDelayMillis);
        return ThreadLocalRandom.current().nextLong(0, boundedCeiling + 1);
    }

    // Returns configured base delay in milliseconds.
    public long getBaseDelayMillis() {
        return baseDelayMillis;
    }

    // Returns configured maximum delay ceiling in milliseconds.
    public long getMaxDelayMillis() {
        return maxDelayMillis;
    }
}
