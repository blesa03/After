package com.after.backend.contribution.api.dto;

import com.after.backend.contribution.domain.ContributionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMediaContributionRequest(

        @NotNull
        ContributionType type,

        @NotBlank
        @Size(max = 255)
        String originalFilename,

        @NotBlank
        @Size(max = 100)
        String mimeType,

        @NotNull
        @Positive
        Long sizeBytes

) {
}