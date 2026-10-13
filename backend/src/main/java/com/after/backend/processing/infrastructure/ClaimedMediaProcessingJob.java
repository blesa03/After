package com.after.backend.processing.infrastructure;

import java.time.Instant;
import java.util.UUID;

public record ClaimedMediaProcessingJob(
        UUID id,
        UUID contributionId,
        int attempts,
        Instant startedAt
) {
}