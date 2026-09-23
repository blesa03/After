package com.after.backend.capsule.api.dto;

import com.after.backend.capsule.domain.CapsuleMember;

import java.time.Instant;

public record CapsuleParticipantResponse(
        String email,
        String role,
        Instant joinedAt
) {

    public static CapsuleParticipantResponse from(
            CapsuleMember membership
    ) {
        return new CapsuleParticipantResponse(
                membership.getUser().getEmail(),
                membership.getRole().name(),
                membership.getJoinedAt()
        );
    }
}