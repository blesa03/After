package com.after.backend.storage;

import java.net.URI;
import java.time.Instant;

public record PresignedDownload(
        URI url,
        Instant expiresAt
) {
}