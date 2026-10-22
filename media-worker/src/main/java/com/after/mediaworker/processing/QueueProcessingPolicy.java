package com.after.mediaworker.processing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public record QueueProcessingPolicy(
        int maxAttempts,
        long baseRetryDelayMs,
        long maxRetryDelayMs,
        long leaseDurationMs
) {
    public QueueProcessingPolicy(
            @Value("${worker.retry.max-attempts:3}")
            int maxAttempts,

            @Value("${worker.retry.base-delay-ms:10000}")
            long baseRetryDelayMs,

            @Value("${worker.retry.max-delay-ms:60000}")
            long maxRetryDelayMs,

            @Value("${worker.lease.duration-ms:60000}")
            long leaseDurationMs
    ) {
        this.maxAttempts = maxAttempts;
        this.baseRetryDelayMs = baseRetryDelayMs;
        this.maxRetryDelayMs = maxRetryDelayMs;
        this.leaseDurationMs = leaseDurationMs;

        if (maxAttempts < 1
                || baseRetryDelayMs < 1
                || maxRetryDelayMs < baseRetryDelayMs
                || leaseDurationMs < 1000) {
            throw new IllegalArgumentException(
                    "Invalid media worker retry/lease configuration"
            );
        }
    }

    public Duration leaseDuration() {
        return Duration.ofMillis(leaseDurationMs);
    }

    public Duration retryDelayForAttempt(int attempts) {
        long delay = baseRetryDelayMs;

        for (int i = 1;
             i < attempts && delay < maxRetryDelayMs;
             i++) {

            delay = delay > maxRetryDelayMs / 2
                    ? maxRetryDelayMs
                    : delay * 2;
        }

        return Duration.ofMillis(
                Math.min(delay, maxRetryDelayMs)
        );
    }
}