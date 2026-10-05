package com.after.backend.contribution.api.dto;

import com.after.backend.contribution.domain.ContributionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateMediaContributionRequest(
        @NotNull ContributionType type,
        @NotBlank String originalFilename,
        @NotBlank String mimeType,
        @NotNull @Positive Long sizeBytes
) {}