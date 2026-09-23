package com.after.backend.contribution.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTextContributionRequest(
        @NotBlank
        String textContent
) {
}