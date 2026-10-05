package com.after.backend.contribution.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MediaUploadResponse(
        UUID contributionId,
        String uploadUrl,
        String httpMethod,
        Instant expiresAt,
        Map<String, String> headers
) {

    public MediaUploadResponse {
        headers = Map.copyOf(headers);
    }
}