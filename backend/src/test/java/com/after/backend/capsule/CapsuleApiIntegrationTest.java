package com.after.backend.capsule;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleMemberRole;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class CapsuleApiIntegrationTest {

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
    void shouldCreatePersonalCapsuleWithCurrentUserAsOwner()
            throws Exception {

        User owner = createUser("owner@example.com");

        mockMvc.perform(
                        post("/api/capsules")
                                .with(as(owner))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        payload(
                                                "Personal capsule",
                                                "Private memories",
                                                "PERSONAL",
                                                Instant.now()
                                                        .plusSeconds(3600),
                                                "Europe/Madrid"
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.title")
                                .value("Personal capsule")
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("PERSONAL")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("COLLECTING")
                )
                .andExpect(
                        jsonPath("$.timezone")
                                .value("Europe/Madrid")
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("OWNER")
                );

        Capsule saved =
                capsuleRepository.findAll().getFirst();

        assertThat(
                capsuleMemberRepository
                        .findByCapsuleIdAndRole(
                                saved.getId(),
                                CapsuleMemberRole.OWNER
                        )
        )
                .isPresent()
                .get()
                .extracting(
                        membership ->
                                membership.getUser().getId()
                )
                .isEqualTo(owner.getId());
    }

    @Test
    void shouldCreateSharedCapsule()
            throws Exception {

        User owner = createUser("owner@example.com");

        mockMvc.perform(
                        post("/api/capsules")
                                .with(as(owner))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        payload(
                                                "Shared capsule",
                                                null,
                                                "SHARED",
                                                Instant.now()
                                                        .plusSeconds(7200),
                                                "Europe/Madrid"
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.type")
                                .value("SHARED")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("COLLECTING")
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("OWNER")
                );
    }

    @Test
    void shouldRejectPastOpeningDate()
            throws Exception {

        User owner = createUser("owner@example.com");

        mockMvc.perform(
                        post("/api/capsules")
                                .with(as(owner))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        payload(
                                                "Past capsule",
                                                null,
                                                "PERSONAL",
                                                Instant.now()
                                                        .minusSeconds(60),
                                                "Europe/Madrid"
                                        )
                                )
                )
                .andExpect(status().isBadRequest());

        assertThat(capsuleRepository.count())
                .isZero();

        assertThat(capsuleMemberRepository.count())
                .isZero();
    }

    @Test
    void shouldRejectBlankTitle()
            throws Exception {

        User owner = createUser("owner@example.com");

        mockMvc.perform(
                        post("/api/capsules")
                                .with(as(owner))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        payload(
                                                "   ",
                                                null,
                                                "PERSONAL",
                                                Instant.now()
                                                        .plusSeconds(3600),
                                                "Europe/Madrid"
                                        )
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidTimezone()
            throws Exception {

        User owner = createUser("owner@example.com");

        mockMvc.perform(
                        post("/api/capsules")
                                .with(as(owner))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        payload(
                                                "Invalid timezone",
                                                null,
                                                "PERSONAL",
                                                Instant.now()
                                                        .plusSeconds(3600),
                                                "Mars/Olympus"
                                        )
                                )
                )
                .andExpect(status().isBadRequest());

        assertThat(capsuleRepository.count())
                .isZero();
    }

    @Test
    void shouldListCapsuleWhenUserIsContributor()
            throws Exception {

        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = Capsule.create(
                "Shared with me",
                null,
                CapsuleType.SHARED,
                Instant.now().plusSeconds(3600),
                "Europe/Madrid",
                owner
        );

        capsule.addContributor(contributor);

        capsuleRepository.saveAndFlush(capsule);

        mockMvc.perform(
                        get("/api/capsules")
                                .with(as(contributor))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].title")
                                .value("Shared with me")
                )
                .andExpect(
                        jsonPath("$[0].role")
                                .value("CONTRIBUTOR")
                );
    }

    @Test
    void shouldNotListForeignCapsules()
            throws Exception {

        User currentUser =
                createUser("current@example.com");

        User foreignOwner =
                createUser("foreign@example.com");

        Capsule foreignCapsule = Capsule.create(
                "Not mine",
                null,
                CapsuleType.PERSONAL,
                Instant.now().plusSeconds(3600),
                "Europe/Madrid",
                foreignOwner
        );

        capsuleRepository.saveAndFlush(
                foreignCapsule
        );

        mockMvc.perform(
                        get("/api/capsules")
                                .with(as(currentUser))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(0)
                );
    }

    @Test
    void shouldReturnCapsuleDetailForMember()
            throws Exception {

        User owner = createUser("owner@example.com");

        Capsule capsule = Capsule.create(
                "Capsule detail",
                "Description",
                CapsuleType.SHARED,
                Instant.now().plusSeconds(3600),
                "Europe/Madrid",
                owner
        );

        Capsule saved =
                capsuleRepository.saveAndFlush(capsule);

        mockMvc.perform(
                        get(
                                "/api/capsules/{capsuleId}",
                                saved.getId()
                        )
                                .with(as(owner))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(saved.getId().toString())
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Capsule detail")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Description")
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("SHARED")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("COLLECTING")
                )
                .andExpect(
                        jsonPath("$.timezone")
                                .value("Europe/Madrid")
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("OWNER")
                )
                .andExpect(
                        jsonPath("$.sealedAt")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.openedAt")
                                .doesNotExist()
                );
    }

    @Test
    void shouldNotReturnForeignCapsuleDetail()
            throws Exception {

        User currentUser =
                createUser("current@example.com");

        User foreignOwner =
                createUser("foreign@example.com");

        Capsule foreignCapsule = Capsule.create(
                "Foreign capsule",
                null,
                CapsuleType.SHARED,
                Instant.now().plusSeconds(3600),
                "Europe/Madrid",
                foreignOwner
        );

        Capsule saved =
                capsuleRepository.saveAndFlush(
                        foreignCapsule
                );

        mockMvc.perform(
                        get(
                                "/api/capsules/{capsuleId}",
                                saved.getId()
                        )
                                .with(as(currentUser))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectUnauthenticatedCapsuleRequests()
            throws Exception {

        mockMvc.perform(get("/api/capsules"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/capsules")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        payload(
                                                "Unauthorized",
                                                null,
                                                "PERSONAL",
                                                Instant.now()
                                                        .plusSeconds(3600),
                                                "Europe/Madrid"
                                        )
                                )
                )
                .andExpect(status().isUnauthorized());
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

    private RequestPostProcessor as(User user) {
        return jwt().jwt(jwt ->
                jwt
                        .subject(user.getId().toString())
                        .claim("email", user.getEmail())
        );
    }

    private String payload(
            String title,
            String description,
            String type,
            Instant opensAt,
            String timezone
    ) {
        String descriptionJson =
                description == null
                        ? "null"
                        : "\"" + description + "\"";

        return """
                {
                  "title": "%s",
                  "description": %s,
                  "type": "%s",
                  "opensAt": "%s",
                  "timezone": "%s"
                }
                """.formatted(
                title,
                descriptionJson,
                type,
                opensAt,
                timezone
        );
    }
}