package com.after.backend.contribution.api.dto;

import java.util.UUID;

public record MediaUploadResponse(
        UUID contributionId,
        String uploadUrl,
        String httpMethod
) {}