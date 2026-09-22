package com.after.backend.capsule.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateCapsuleRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        String description,

        @NotNull
        @Future
        Instant opensAt,

        @NotBlank
        String timezone
) {
}