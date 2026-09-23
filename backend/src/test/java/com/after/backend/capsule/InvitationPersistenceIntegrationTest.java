package com.after.backend.capsule;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleType;
import com.after.backend.capsule.domain.Invitation;
import com.after.backend.capsule.infrastructure.CapsuleRepository;
import com.after.backend.capsule.infrastructure.InvitationRepository;
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
class InvitationPersistenceIntegrationTest {

    @SuppressWarnings("deprecation")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            );

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private CapsuleRepository capsuleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldPersistInvitationWithTokenHash() {
        User owner = createUser("owner@example.com");
        Capsule capsule = createSharedCapsule(owner);

        Invitation invitation = new Invitation(
                capsule,
                owner,
                "hashed-token-value"
        );

        Invitation saved =
                invitationRepository.saveAndFlush(invitation);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        assertThat(saved.getCapsule().getId())
                .isEqualTo(capsule.getId());

        assertThat(saved.getCreatedByUser().getId())
                .isEqualTo(owner.getId());

        assertThat(saved.getTokenHash())
                .isEqualTo("hashed-token-value");

        assertThat(saved.getAcceptedAt()).isNull();
        assertThat(saved.getAcceptedByUser()).isNull();
        assertThat(saved.getRevokedAt()).isNull();

        assertThat(saved.isUsable()).isTrue();
    }

    @Test
    void shouldFindInvitationByTokenHash() {
        User owner = createUser("owner@example.com");
        Capsule capsule = createSharedCapsule(owner);

        invitationRepository.saveAndFlush(
                new Invitation(
                        capsule,
                        owner,
                        "hashed-token-value"
                )
        );

        assertThat(
                invitationRepository.findByTokenHash(
                        "hashed-token-value"
                )
        )
                .isPresent()
                .get()
                .extracting(Invitation::getTokenHash)
                .isEqualTo("hashed-token-value");
    }

    @Test
    void shouldRejectDuplicateTokenHash() {
        User owner = createUser("owner@example.com");
        Capsule capsule = createSharedCapsule(owner);

        invitationRepository.saveAndFlush(
                new Invitation(
                        capsule,
                        owner,
                        "same-token-hash"
                )
        );

        Invitation duplicate = new Invitation(
                capsule,
                owner,
                "same-token-hash"
        );

        assertThatThrownBy(
                () -> invitationRepository
                        .saveAndFlush(duplicate)
        )
                .isInstanceOf(
                        DataIntegrityViolationException.class
                );
    }

    @Test
    void shouldAcceptInvitationOnlyOnce() {
        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        User secondContributor =
                createUser("second@example.com");

        Capsule capsule = createSharedCapsule(owner);

        Invitation invitation = new Invitation(
                capsule,
                owner,
                "hashed-token-value"
        );

        Instant acceptedAt =
                Instant.parse("2030-01-01T12:00:00Z");

        invitation.accept(
                contributor,
                acceptedAt
        );

        assertThat(invitation.isAccepted()).isTrue();
        assertThat(invitation.isUsable()).isFalse();

        assertThat(invitation.getAcceptedAt())
                .isEqualTo(acceptedAt);

        assertThat(invitation.getAcceptedByUser())
                .isEqualTo(contributor);

        assertThatThrownBy(
                () -> invitation.accept(
                        secondContributor,
                        acceptedAt.plusSeconds(60)
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Invitation has already been accepted"
                );
    }

    @Test
    void shouldRejectRevokedInvitation() {
        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = createSharedCapsule(owner);

        Invitation invitation = new Invitation(
                capsule,
                owner,
                "hashed-token-value"
        );

        Instant revokedAt =
                Instant.parse("2030-01-01T12:00:00Z");

        invitation.revoke(revokedAt);

        assertThat(invitation.isRevoked()).isTrue();
        assertThat(invitation.isUsable()).isFalse();

        assertThat(invitation.getRevokedAt())
                .isEqualTo(revokedAt);

        assertThatThrownBy(
                () -> invitation.accept(
                        contributor,
                        revokedAt.plusSeconds(60)
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Invitation has been revoked"
                );
    }

    @Test
    void shouldRejectInvitationForPersonalCapsule() {
        User owner = createUser("owner@example.com");

        Capsule capsule = Capsule.create(
                "Personal capsule",
                null,
                CapsuleType.PERSONAL,
                Instant.parse("2035-06-15T18:00:00Z"),
                "Europe/Madrid",
                owner
        );

        capsuleRepository.saveAndFlush(capsule);

        assertThatThrownBy(
                () -> new Invitation(
                        capsule,
                        owner,
                        "hashed-token-value"
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Invitations only apply to SHARED capsules"
                );
    }

    @Test
    void shouldRejectRevokingAcceptedInvitation() {
        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = createSharedCapsule(owner);

        Invitation invitation = new Invitation(
                capsule,
                owner,
                "hashed-token-value"
        );

        invitation.accept(
                contributor,
                Instant.parse("2030-01-01T12:00:00Z")
        );

        assertThatThrownBy(
                () -> invitation.revoke(
                        Instant.parse(
                                "2030-01-01T13:00:00Z"
                        )
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Accepted invitation cannot be revoked"
                );
    }

    private Capsule createSharedCapsule(User owner) {
        Capsule capsule = Capsule.create(
                "Shared capsule",
                null,
                CapsuleType.SHARED,
                Instant.parse("2035-06-15T18:00:00Z"),
                "Europe/Madrid",
                owner
        );

        return capsuleRepository.saveAndFlush(capsule);
    }

    private User createUser(String email) {
        return userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode(
                                "test-password-123"
                        )
                )
        );
    }
}