package com.after.backend.capsule.api.dto;

import com.after.backend.capsule.domain.CapsuleType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateCapsuleRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        String description,

        @NotNull
        CapsuleType type,

        @NotNull
        @Future
        Instant opensAt,

        @NotBlank
        @Size(max = 64)
        String timezone

) {
}