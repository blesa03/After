package com.after.backend.capsule.api.dto;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleMember;
import com.after.backend.capsule.domain.CapsuleMemberRole;
import com.after.backend.capsule.domain.CapsuleStatus;
import com.after.backend.capsule.domain.CapsuleType;

import java.time.Instant;
import java.util.UUID;

public record CapsuleDetailResponse(
        UUID id,
        String title,
        String description,
        CapsuleType type,
        CapsuleStatus status,
        Instant opensAt,
        String timezone,
        Instant sealedAt,
        Instant openedAt,
        Instant createdAt,
        Instant updatedAt,
        CapsuleMemberRole role
) {

    public static CapsuleDetailResponse from(
            CapsuleMember membership
    ) {
        return from(
                membership.getCapsule(),
                membership.getRole()
        );
    }

    public static CapsuleDetailResponse from(
            Capsule capsule,
            CapsuleMemberRole role
    ) {
        return new CapsuleDetailResponse(
                capsule.getId(),
                capsule.getTitle(),
                capsule.getDescription(),
                capsule.getType(),
                capsule.getStatus(),
                capsule.getOpensAt(),
                capsule.getTimezone(),
                capsule.getSealedAt(),
                capsule.getOpenedAt(),
                capsule.getCreatedAt(),
                capsule.getUpdatedAt(),
                role
        );
    }
}