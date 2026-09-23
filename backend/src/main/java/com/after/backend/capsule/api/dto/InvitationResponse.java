package com.after.backend.capsule.api.dto;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.Invitation;

import java.time.Instant;

public record InvitationResponse(
        String capsuleTitle,
        String ownerEmail,
        Instant opensAt
) {

    public static InvitationResponse from(
            Invitation invitation
    ) {
        Capsule capsule = invitation.getCapsule();

        return new InvitationResponse(
                capsule.getTitle(),
                capsule.getOwner().getEmail(),
                capsule.getOpensAt()
        );
    }
}