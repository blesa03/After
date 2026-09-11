package com.after.backend.capsule;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleMember;
import com.after.backend.capsule.domain.CapsuleMemberRole;
import com.after.backend.capsule.domain.CapsuleStatus;
import com.after.backend.capsule.domain.CapsuleType;
import com.after.backend.capsule.infrastructure.CapsuleMemberRepository;
import com.after.backend.capsule.infrastructure.CapsuleRepository;
import com.after.backend.user.domain.User;
import com.after.backend.user.infrastructure.UserRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class CapsulePersistenceIntegrationTest {

    @SuppressWarnings("deprecation")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            );

    @Autowired
    private CapsuleRepository capsuleRepository;

    @Autowired
    private CapsuleMemberRepository capsuleMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistCapsuleWithOwnerMembership() {
        User owner = createUser("owner@example.com");

        Instant opensAt =
                Instant.parse("2030-06-15T18:00:00Z");

        Capsule capsule = Capsule.create(
                "Summer 2030",
                "Open this capsule together.",
                CapsuleType.SHARED,
                opensAt,
                "Europe/Madrid",
                owner
        );

        Capsule saved =
                capsuleRepository.saveAndFlush(capsule);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus())
                .isEqualTo(CapsuleStatus.COLLECTING);

        assertThat(saved.getOpensAt())
                .isEqualTo(opensAt);

        assertThat(saved.getTimezone())
                .isEqualTo("Europe/Madrid");

        assertThat(saved.getSealedAt()).isNull();
        assertThat(saved.getOpenedAt()).isNull();

        assertThat(saved.getOwner().getId())
                .isEqualTo(owner.getId());

        assertThat(
                capsuleMemberRepository
                        .findAllByCapsuleId(saved.getId())
        )
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.getUser().getId())
                            .isEqualTo(owner.getId());

                    assertThat(member.getRole())
                            .isEqualTo(
                                    CapsuleMemberRole.OWNER
                            );

                    assertThat(member.getJoinedAt())
                            .isNotNull();
                });
    }

    @Test
    void shouldPersistContributorWithoutChangingOwner() {
        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = Capsule.create(
                "Shared capsule",
                null,
                CapsuleType.SHARED,
                Instant.parse("2030-06-15T18:00:00Z"),
                "Europe/Madrid",
                owner
        );

        capsule.addContributor(contributor);

        Capsule saved =
                capsuleRepository.saveAndFlush(capsule);

        assertThat(saved.getOwner().getId())
                .isEqualTo(owner.getId());

        assertThat(
                capsuleMemberRepository
                        .findAllByCapsuleId(saved.getId())
        )
                .hasSize(2)
                .extracting(CapsuleMember::getRole)
                .containsExactlyInAnyOrder(
                        CapsuleMemberRole.OWNER,
                        CapsuleMemberRole.CONTRIBUTOR
                );
    }

    @Test
    void shouldRejectDuplicateMembershipForSameUserAndCapsule() {
        User owner = createUser("owner@example.com");

        Capsule capsule = Capsule.create(
                "Personal capsule",
                null,
                CapsuleType.PERSONAL,
                Instant.parse("2030-06-15T18:00:00Z"),
                "Europe/Madrid",
                owner
        );

        capsuleRepository.saveAndFlush(capsule);

        CapsuleMember duplicate =
                capsule.addContributor(owner);

        assertThatThrownBy(
                () -> capsuleMemberRepository
                        .saveAndFlush(duplicate)
        )
                .isInstanceOf(
                        DataIntegrityViolationException.class
                );
    }

    @Test
    void shouldRejectSecondOwnerForSameCapsule() {
        User firstOwner =
                createUser("owner@example.com");

        User secondOwner =
                createUser("second-owner@example.com");

        Capsule capsule = Capsule.create(
                "Shared capsule",
                null,
                CapsuleType.SHARED,
                Instant.parse("2030-06-15T18:00:00Z"),
                "Europe/Madrid",
                firstOwner
        );

        capsuleRepository.saveAndFlush(capsule);

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO capsule_members (
                            id,
                            capsule_id,
                            user_id,
                            role,
                            joined_at
                        )
                        VALUES (?, ?, ?, 'OWNER', ?)
                        """,
                        UUID.randomUUID(),
                        capsule.getId(),
                        secondOwner.getId(),
                        Timestamp.from(Instant.now())
                )
        )
                .isInstanceOf(
                        DataIntegrityViolationException.class
                );
    }

    @Test
    void shouldRequireOwnerWhenCreatingCapsule() {
        assertThatThrownBy(
                () -> Capsule.create(
                        "Invalid capsule",
                        null,
                        CapsuleType.PERSONAL,
                        Instant.parse(
                                "2030-06-15T18:00:00Z"
                        ),
                        "Europe/Madrid",
                        null
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("owner cannot be null");
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
