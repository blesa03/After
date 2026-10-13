package com.after.backend.contribution.api.dto;

import com.after.backend.contribution.domain.MediaObjectStatus;

import java.util.UUID;

public record MediaCompletionResponse(
        UUID contributionId,
        MediaObjectStatus status
) {
}