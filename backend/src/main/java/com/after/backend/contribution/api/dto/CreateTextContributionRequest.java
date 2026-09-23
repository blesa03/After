package com.after.backend.contribution.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTextContributionRequest(
        @NotBlank
        String textContent
) {
}