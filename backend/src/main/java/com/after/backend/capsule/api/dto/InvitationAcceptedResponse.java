package com.after.backend.capsule.api.dto;

import java.util.UUID;

public record InvitationAcceptedResponse(
        UUID capsuleId,
        String role
) {
}