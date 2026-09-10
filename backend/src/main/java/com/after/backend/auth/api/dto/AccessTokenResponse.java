package com.after.backend.auth.api.dto;

import com.after.backend.auth.application.AccessToken;

public record AccessTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    public static AccessTokenResponse from(AccessToken token) {
        return new AccessTokenResponse(
                token.value(),
                "Bearer",
                token.expiresInSeconds()
        );
    }
}