package com.after.backend.capsule.api.dto;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleMember;
import com.after.backend.capsule.domain.CapsuleMemberRole;
import com.after.backend.capsule.domain.CapsuleStatus;
import com.after.backend.capsule.domain.CapsuleType;

import java.time.Instant;
import java.util.UUID;

public record CapsuleSummaryResponse(
        UUID id,
        String title,
        CapsuleType type,
        CapsuleStatus status,
        Instant opensAt,
        String timezone,
        CapsuleMemberRole role
) {

    public static CapsuleSummaryResponse from(
            CapsuleMember membership
    ) {
        Capsule capsule = membership.getCapsule();

        return new CapsuleSummaryResponse(
                capsule.getId(),
                capsule.getTitle(),
                capsule.getType(),
                capsule.getStatus(),
                capsule.getOpensAt(),
                capsule.getTimezone(),
                membership.getRole()
        );
    }
}