package com.after.backend.storage;

import java.net.URI;
import java.time.Instant;

public record PresignedUpload(
        String objectKey,
        URI url,
        Instant expiresAt
) {
}