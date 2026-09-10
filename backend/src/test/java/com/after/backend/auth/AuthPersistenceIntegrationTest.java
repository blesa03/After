package com.after.backend.auth;

import com.after.backend.auth.domain.RefreshToken;
import com.after.backend.auth.infrastructure.RefreshTokenRepository;
import com.after.backend.user.domain.User;
import com.after.backend.user.infrastructure.UserRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class AuthPersistenceIntegrationTest {

    @SuppressWarnings("deprecation")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldPersistUserWithNormalizedEmailAndEncodedPassword() {
        String rawPassword = "super-secret-password";
        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = new User(
                "  TEST@EXAMPLE.COM  ",
                passwordHash
        );

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");

        assertThat(saved.getPasswordHash())
                .isNotEqualTo(rawPassword);

        assertThat(
                passwordEncoder.matches(
                        rawPassword,
                        saved.getPasswordHash()
                )
        ).isTrue();

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateEmail() {
        User first = new User(
                "test@example.com",
                passwordEncoder.encode("password-one")
        );

        User second = new User(
                "  TEST@EXAMPLE.COM ",
                passwordEncoder.encode("password-two")
        );

        userRepository.saveAndFlush(first);

        assertThatThrownBy(
                () -> userRepository.saveAndFlush(second)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAssociateRefreshTokenWithUser() {
        User user = userRepository.saveAndFlush(
                new User(
                        "test@example.com",
                        passwordEncoder.encode("password")
                )
        );

        RefreshToken refreshToken = new RefreshToken(
                user,
                "example-refresh-token-hash",
                Instant.now().plusSeconds(3600)
        );

        RefreshToken saved =
                refreshTokenRepository.saveAndFlush(refreshToken);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId())
                .isEqualTo(user.getId());

        assertThat(saved.getTokenHash())
                .isEqualTo("example-refresh-token-hash");
    }

    @Test
    void shouldNotReturnExpiredRefreshTokenAsUsable() {
        User user = userRepository.saveAndFlush(
                new User(
                        "expired@example.com",
                        passwordEncoder.encode("password")
                )
        );

        RefreshToken token = refreshTokenRepository.saveAndFlush(
                new RefreshToken(
                        user,
                        "expired-token-hash",
                        Instant.now().plusSeconds(1)
                )
        );

        Instant afterExpiration =
                token.getExpiresAt().plusSeconds(1);

        assertThat(
                refreshTokenRepository.findUsableByTokenHash(
                        token.getTokenHash(),
                        afterExpiration
                )
        ).isEmpty();
    }

    @Test
    void shouldNotReturnRevokedRefreshTokenAsUsable() {
        User user = userRepository.saveAndFlush(
                new User(
                        "revoked@example.com",
                        passwordEncoder.encode("password")
                )
        );

        RefreshToken token = new RefreshToken(
                user,
                "revoked-token-hash",
                Instant.now().plusSeconds(3600)
        );

        token.revoke(Instant.now());

        refreshTokenRepository.saveAndFlush(token);

        assertThat(
                refreshTokenRepository.findUsableByTokenHash(
                        token.getTokenHash(),
                        Instant.now()
                )
        ).isEmpty();
    }

    @Test
    void shouldReturnActiveRefreshTokenAsUsable() {
        User user = userRepository.saveAndFlush(
                new User(
                        "active@example.com",
                        passwordEncoder.encode("password")
                )
        );

        RefreshToken token = refreshTokenRepository.saveAndFlush(
                new RefreshToken(
                        user,
                        "active-token-hash",
                        Instant.now().plusSeconds(3600)
                )
        );

        assertThat(
                refreshTokenRepository.findUsableByTokenHash(
                        token.getTokenHash(),
                        Instant.now()
                )
        ).contains(token);
    }
}