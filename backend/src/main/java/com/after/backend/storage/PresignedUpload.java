package com.after.backend.storage;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

public record PresignedUpload(
        String objectKey,
        URI url,
        Instant expiresAt,
        Map<String, String> headers
) {

    public PresignedUpload {
        headers = Map.copyOf(headers);
    }
}