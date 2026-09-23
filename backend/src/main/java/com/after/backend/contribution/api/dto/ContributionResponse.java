package com.after.backend.contribution.api.dto;

import com.after.backend.contribution.domain.Contribution;
import com.after.backend.contribution.domain.ContributionType;

import java.time.Instant;
import java.util.UUID;

public record ContributionResponse(
        UUID id,
        UUID capsuleId,
        UUID authorUserId,
        ContributionType type,
        String textContent,
        Instant createdAt,
        Instant updatedAt
) {

    public static ContributionResponse from(
            Contribution contribution
    ) {
        return new ContributionResponse(
                contribution.getId(),
                contribution
                        .getCapsule()
                        .getId(),
                contribution
                        .getAuthor()
                        .getId(),
                contribution.getType(),
                contribution.getTextContent(),
                contribution.getCreatedAt(),
                contribution.getUpdatedAt()
        );
    }
}