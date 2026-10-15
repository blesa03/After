package com.after.mediaworker.processing;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ClaimedMediaProcessingJob(
        UUID id,
        UUID contributionId,
        int attempts,
        Instant startedAt
) {

    public ClaimedMediaProcessingJob {
        Objects.requireNonNull(
                id,
                "id cannot be null"
        );
        Objects.requireNonNull(
                contributionId,
                "contributionId cannot be null"
        );
        Objects.requireNonNull(
                startedAt,
                "startedAt cannot be null"
        );
        if (attempts <= 0) {
            throw new IllegalArgumentException(
                    "attempts must be greater than zero"
            );
        }
    }
}