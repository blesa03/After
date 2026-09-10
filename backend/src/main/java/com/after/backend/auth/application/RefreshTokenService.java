package com.after.backend.auth.application;

import com.after.backend.auth.config.AuthProperties;
import com.after.backend.auth.domain.RefreshToken;
import com.after.backend.auth.exception.InvalidRefreshTokenException;
import com.after.backend.auth.infrastructure.RefreshTokenRepository;
import com.after.backend.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AuthProperties properties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        return issue(user, Instant.now());
    }

    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {
        Instant now = Instant.now();

        RefreshToken current = refreshTokenRepository
                .findUsableByTokenHashForUpdate(
                        hash(rawToken),
                        now
                )
                .orElseThrow(InvalidRefreshTokenException::new);

        current.revoke(now);

        return issue(current.getUser(), now);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository
                .findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    private IssuedRefreshToken issue(
            User user,
            Instant createdAt
    ) {
        String rawToken = generate();

        RefreshToken refreshToken = new RefreshToken(
                user,
                hash(rawToken),
                createdAt.plus(properties.refresh().ttl())
        );

        refreshTokenRepository.save(refreshToken);

        return new IssuedRefreshToken(user, rawToken);
    }

    private String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }
}