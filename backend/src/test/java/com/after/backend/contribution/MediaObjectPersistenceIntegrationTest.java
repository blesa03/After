package com.after.backend.contribution;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleType;
import com.after.backend.capsule.infrastructure.CapsuleMemberRepository;
import com.after.backend.capsule.infrastructure.CapsuleRepository;
import com.after.backend.contribution.domain.Contribution;
import com.after.backend.contribution.domain.MediaObject;
import com.after.backend.contribution.domain.MediaObjectStatus;
import com.after.backend.contribution.infrastructure.ContributionRepository;
import com.after.backend.user.domain.User;
import com.after.backend.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class MediaObjectPersistenceIntegrationTest {

    @SuppressWarnings("deprecation")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CapsuleRepository capsuleRepository;

    @Autowired
    private CapsuleMemberRepository
            capsuleMemberRepository;

    @Autowired
    private ContributionRepository
            contributionRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        contributionRepository.deleteAll();
        capsuleMemberRepository.deleteAll();
        capsuleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateBidirectionalMediaObjectAssociation() {
        Contribution contribution =
                createTextContribution();

        MediaObject mediaObject =
                new MediaObject(
                        UUID.randomUUID(),
                        contribution
                );

        assertThat(
                mediaObject.getContribution()
        ).isSameAs(
                contribution
        );

        assertThat(
                contribution.getMediaObject()
        ).isSameAs(
                mediaObject
        );
    }

    @Test
    void shouldRejectSecondMediaObjectForSameContribution() {
        Contribution contribution =
                createTextContribution();

        MediaObject firstMediaObject =
                new MediaObject(
                        UUID.randomUUID(),
                        contribution
                );

        assertThat(
                contribution.getMediaObject()
        ).isSameAs(
                firstMediaObject
        );

        assertThatThrownBy(
                () ->
                        new MediaObject(
                                UUID.randomUUID(),
                                contribution
                        )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Contribution already has a MediaObject"
                );
    }

    @Test
    void shouldRejectMediaObjectWithoutId() {
        Contribution contribution =
                createTextContribution();

        assertThatThrownBy(
                () ->
                        new MediaObject(
                                null,
                                contribution
                        )
        )
                .isInstanceOf(
                        NullPointerException.class
                )
                .hasMessage(
                        "id cannot be null"
                );
    }

    @Test
    void shouldRejectMediaObjectWithoutContribution() {
        assertThatThrownBy(
                () ->
                        new MediaObject(
                                UUID.randomUUID(),
                                null
                        )
        )
                .isInstanceOf(
                        NullPointerException.class
                )
                .hasMessage(
                        "contribution cannot be null"
                );
    }

    @Test
    void shouldDefaultMediaObjectStatusToUploading() {
        Contribution contribution =
                createTextContribution();

        MediaObject mediaObject =
                new MediaObject(
                        UUID.randomUUID(),
                        contribution
                );

        assertThat(
                mediaObject.getStatus()
        ).isEqualTo(
                MediaObjectStatus.UPLOADING
        );
    }

    @Test
    void shouldPersistMediaObjectUsingPostgresEnum() {
        Contribution contribution =
                createTextContribution();

        UUID mediaObjectId =
                UUID.randomUUID();

        MediaObject mediaObject =
                new MediaObject(
                        mediaObjectId,
                        contribution
                );

        mediaObject.setOriginalObjectKey(
                "capsules/test/original.jpg"
        );

        contributionRepository.saveAndFlush(
                contribution
        );

        entityManager.clear();

        MediaObject persisted =
                entityManager.find(
                        MediaObject.class,
                        mediaObjectId
                );

        assertThat(
                persisted
        ).isNotNull();

        assertThat(
                persisted.getStatus()
        ).isEqualTo(
                MediaObjectStatus.UPLOADING
        );

        assertThat(
                persisted.getOriginalObjectKey()
        ).isEqualTo(
                "capsules/test/original.jpg"
        );
    }

    @Test
    void shouldPersistMediaObjectStatusChange() {
        Contribution contribution =
                createTextContribution();

        UUID mediaObjectId =
                UUID.randomUUID();

        MediaObject mediaObject =
                new MediaObject(
                        mediaObjectId,
                        contribution
                );

        mediaObject.setStatus(
                MediaObjectStatus.PROCESSING
        );

        contributionRepository.saveAndFlush(
                contribution
        );

        entityManager.clear();

        MediaObject persisted =
                entityManager.find(
                        MediaObject.class,
                        mediaObjectId
                );

        assertThat(
                persisted
        ).isNotNull();

        assertThat(
                persisted.getStatus()
        ).isEqualTo(
                MediaObjectStatus.PROCESSING
        );
    }

    @Test
    void shouldPersistTextContributionWithoutMediaObject() {
        Contribution contribution =
                createTextContribution();

        UUID contributionId =
                contribution.getId();

        entityManager.clear();

        Contribution persisted =
                contributionRepository
                        .findById(
                                contributionId
                        )
                        .orElseThrow();

        assertThat(
                persisted.getMediaObject()
        ).isNull();

        assertThat(
                persisted.getTextContent()
        ).isEqualTo(
                "Test contribution"
        );
    }

    private Contribution createTextContribution() {
        User owner =
                userRepository.saveAndFlush(
                        new User(
                                "owner@example.com",
                                "test-password-hash"
                        )
                );

        Capsule capsule =
                Capsule.create(
                        "Test capsule",
                        "Test capsule description",
                        CapsuleType.SHARED,
                        Instant.now()
                                .plusSeconds(3600),
                        "Europe/Madrid",
                        owner
                );

        Capsule savedCapsule =
                capsuleRepository.saveAndFlush(
                        capsule
                );

        return contributionRepository
                .saveAndFlush(
                        Contribution.text(
                                savedCapsule,
                                owner,
                                "Test contribution"
                        )
                );
    }
}