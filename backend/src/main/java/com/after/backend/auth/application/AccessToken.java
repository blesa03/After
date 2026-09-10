package com.after.backend.auth.application;

public record AccessToken(
        String value,
        long expiresInSeconds
) {
}