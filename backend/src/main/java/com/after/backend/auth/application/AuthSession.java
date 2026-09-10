package com.after.backend.auth.application;

public record AuthSession(
        AccessToken accessToken,
        String refreshToken
) {
}