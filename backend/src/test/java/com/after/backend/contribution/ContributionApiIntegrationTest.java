package com.after.backend.contribution;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleType;
import com.after.backend.capsule.infrastructure.CapsuleMemberRepository;
import com.after.backend.capsule.infrastructure.CapsuleRepository;
import com.after.backend.contribution.domain.Contribution;
import com.after.backend.contribution.infrastructure.ContributionRepository;
import com.after.backend.user.domain.User;
import com.after.backend.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class ContributionApiIntegrationTest {

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
    private CapsuleMemberRepository
            capsuleMemberRepository;

    @Autowired
    private ContributionRepository
            contributionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void shouldAllowOwnerToCreateTextContribution()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        mockMvc.perform(
                        post(
                                "/api/capsules/{capsuleId}/contributions/text",
                                capsule.getId()
                        )
                                .with(as(owner))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "textContent": "A future memory"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("TEXT")
                )
                .andExpect(
                        jsonPath("$.textContent")
                                .value(
                                        "A future memory"
                                )
                )
                .andExpect(
                        jsonPath("$.authorUserId")
                                .value(
                                        owner.getId()
                                                .toString()
                                )
                );
    }

    @Test
    void shouldAllowContributorToCreateTextContribution()
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
                        post(
                                "/api/capsules/{capsuleId}/contributions/text",
                                saved.getId()
                        )
                                .with(
                                        as(
                                                contributor
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "textContent": "Contributor memory"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.authorUserId")
                                .value(
                                        contributor
                                                .getId()
                                                .toString()
                                )
                );
    }

    @Test
    void shouldRejectNonMemberCreatingContribution()
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
                createSharedCapsule(
                        owner
                );

        mockMvc.perform(
                        post(
                                "/api/capsules/{capsuleId}/contributions/text",
                                capsule.getId()
                        )
                                .with(
                                        as(
                                                outsider
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "textContent": "No access"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isNotFound()
                );

        assertThat(
                contributionRepository.count()
        ).isZero();
    }

    @Test
    void shouldRejectBlankTextContribution()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        mockMvc.perform(
                        post(
                                "/api/capsules/{capsuleId}/contributions/text",
                                capsule.getId()
                        )
                                .with(as(owner))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "textContent": "   "
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );

        assertThat(
                contributionRepository.count()
        ).isZero();
    }

    @Test
    void shouldOnlyListOwnTextContributions()
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

        contributionRepository
                .saveAndFlush(
                        Contribution.text(
                                saved,
                                owner,
                                "Owner memory"
                        )
                );

        contributionRepository
                .saveAndFlush(
                        Contribution.text(
                                saved,
                                contributor,
                                "Contributor memory"
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/capsules/{capsuleId}/contributions/text",
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
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$[0].textContent"
                        ).value(
                                "Contributor memory"
                        )
                );
    }

    @Test
    void shouldAllowAuthorToEditOwnText()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        Contribution contribution =
                contributionRepository
                        .saveAndFlush(
                                Contribution.text(
                                        capsule,
                                        owner,
                                        "Original"
                                )
                        );

        mockMvc.perform(
                        put(
                                "/api/capsules/{capsuleId}/contributions/{contributionId}/text",
                                capsule.getId(),
                                contribution.getId()
                        )
                                .with(as(owner))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "textContent": "Updated"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.textContent")
                                .value("Updated")
                );
    }

    @Test
    void shouldRejectEditingAnotherUsersText()
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

        Contribution contribution =
                contributionRepository
                        .saveAndFlush(
                                Contribution.text(
                                        saved,
                                        owner,
                                        "Owner memory"
                                )
                        );

        mockMvc.perform(
                        put(
                                "/api/capsules/{capsuleId}/contributions/{contributionId}/text",
                                saved.getId(),
                                contribution.getId()
                        )
                                .with(
                                        as(
                                                contributor
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "textContent": "Changed"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        assertThat(
                contributionRepository
                        .findById(
                                contribution.getId()
                        )
                        .orElseThrow()
                        .getTextContent()
        ).isEqualTo(
                "Owner memory"
        );
    }

    @Test
    void shouldAllowAuthorToDeleteOwnText()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        Contribution contribution =
                contributionRepository
                        .saveAndFlush(
                                Contribution.text(
                                        capsule,
                                        owner,
                                        "Delete me"
                                )
                        );

        mockMvc.perform(
                        delete(
                                "/api/capsules/{capsuleId}/contributions/{contributionId}",
                                capsule.getId(),
                                contribution.getId()
                        )
                                .with(as(owner))
                )
                .andExpect(
                        status().isNoContent()
                );

        assertThat(
                contributionRepository
                        .existsById(
                                contribution.getId()
                        )
        ).isFalse();
    }

    @Test
    void shouldRejectModificationWhenCapsuleIsSealed()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        Contribution contribution =
                contributionRepository
                        .saveAndFlush(
                                Contribution.text(
                                        capsule,
                                        owner,
                                        "Original"
                                )
                        );

        jdbcTemplate.update(
                """
                UPDATE capsules
                SET status = 'SEALED'
                WHERE id = ?
                """,
                capsule.getId()
        );

        entityManager.clear();

        mockMvc.perform(
                        put(
                                "/api/capsules/{capsuleId}/contributions/{contributionId}/text",
                                capsule.getId(),
                                contribution.getId()
                        )
                                .with(as(owner))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "textContent": "Updated"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
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

    private Capsule createSharedCapsule(
            User owner
    ) {
        Capsule capsule =
                Capsule.create(
                        "Shared capsule",
                        "Shared capsule description",
                        CapsuleType.SHARED,
                        Instant.now()
                                .plusSeconds(3600),
                        "Europe/Madrid",
                        owner
                );

        return capsuleRepository
                .saveAndFlush(
                        capsule
                );
    }

    private RequestPostProcessor as(
            User user
    ) {
        return jwt().jwt(
                jwt ->
                        jwt
                                .subject(
                                        user.getId()
                                                .toString()
                                )
                                .claim(
                                        "email",
                                        user.getEmail()
                                )
        );
    }
}