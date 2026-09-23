package com.after.backend.capsule;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleType;
import com.after.backend.capsule.infrastructure.CapsuleMemberRepository;
import com.after.backend.capsule.infrastructure.CapsuleRepository;
import com.after.backend.user.domain.User;
import com.after.backend.user.infrastructure.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class CapsuleParticipantsApiIntegrationTest {

    @SuppressWarnings("deprecation")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CapsuleRepository capsuleRepository;

    @Autowired
    private CapsuleMemberRepository capsuleMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        capsuleMemberRepository.deleteAll();
        capsuleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldListSharedCapsuleParticipants()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        User contributor =
                createUser(
                        "contributor@example.com"
                );

        Capsule capsule =
                Capsule.create(
                        "Shared capsule",
                        "Shared memories",
                        CapsuleType.SHARED,
                        Instant.now()
                                .plusSeconds(3600),
                        "Europe/Madrid",
                        owner
                );

        capsule.addContributor(
                contributor
        );

        Capsule saved =
                capsuleRepository
                        .saveAndFlush(
                                capsule
                        );

        mockMvc.perform(
                        get(
                                "/api/capsules/{capsuleId}/participants",
                                saved.getId()
                        )
                                .with(as(owner))
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.length()"
                        ).value(2)
                )
                .andExpect(
                        jsonPath(
                                "$[?(@.email == 'owner@example.com')].role"
                        ).value("OWNER")
                )
                .andExpect(
                        jsonPath(
                                "$[?(@.email == 'contributor@example.com')].role"
                        ).value(
                                "CONTRIBUTOR"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$[0].joinedAt"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$[1].joinedAt"
                        ).exists()
                );
    }

    @Test
    void shouldAllowContributorToListParticipants()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        User contributor =
                createUser(
                        "contributor@example.com"
                );

        Capsule capsule =
                Capsule.create(
                        "Shared capsule",
                        null,
                        CapsuleType.SHARED,
                        Instant.now()
                                .plusSeconds(3600),
                        "Europe/Madrid",
                        owner
                );

        capsule.addContributor(
                contributor
        );

        Capsule saved =
                capsuleRepository
                        .saveAndFlush(
                                capsule
                        );

        mockMvc.perform(
                        get(
                                "/api/capsules/{capsuleId}/participants",
                                saved.getId()
                        )
                                .with(
                                        as(
                                                contributor
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.length()"
                        ).value(2)
                );
    }

    @Test
    void shouldRejectParticipantListForNonMember()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        User outsider =
                createUser(
                        "outsider@example.com"
                );

        Capsule capsule =
                Capsule.create(
                        "Shared capsule",
                        null,
                        CapsuleType.SHARED,
                        Instant.now()
                                .plusSeconds(3600),
                        "Europe/Madrid",
                        owner
                );

        Capsule saved =
                capsuleRepository
                        .saveAndFlush(
                                capsule
                        );

        mockMvc.perform(
                        get(
                                "/api/capsules/{capsuleId}/participants",
                                saved.getId()
                        )
                                .with(
                                        as(
                                                outsider
                                        )
                                )
                )
                .andExpect(
                        status().isNotFound()
                );
    }

    @Test
    void shouldRejectUnauthenticatedParticipantList()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                Capsule.create(
                        "Shared capsule",
                        null,
                        CapsuleType.SHARED,
                        Instant.now()
                                .plusSeconds(3600),
                        "Europe/Madrid",
                        owner
                );

        Capsule saved =
                capsuleRepository
                        .saveAndFlush(
                                capsule
                        );

        mockMvc.perform(
                        get(
                                "/api/capsules/{capsuleId}/participants",
                                saved.getId()
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    private User createUser(
            String email
    ) {
        return userRepository
                .saveAndFlush(
                        new User(
                                email,
                                passwordEncoder
                                        .encode(
                                                "test-password-123"
                                        )
                        )
                );
    }

    private RequestPostProcessor as(
            User user
    ) {
        return jwt().jwt(
                jwt ->
                        jwt
                                .subject(
                                        user
                                                .getId()
                                                .toString()
                                )
                                .claim(
                                        "email",
                                        user
                                                .getEmail()
                                )
        );
    }
}