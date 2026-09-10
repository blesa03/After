package com.after.backend.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "after.auth")
public record AuthProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Refresh refresh
) {

    public record Jwt(
            @NotBlank String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl
    ) {
    }

    public record Refresh(
            @NotNull Duration ttl,
            @NotBlank String cookieName,
            @NotBlank String path,
            @NotBlank String sameSite,
            boolean secure
    ) {
    }
}