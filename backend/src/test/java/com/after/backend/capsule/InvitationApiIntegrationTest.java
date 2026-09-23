package com.after.backend.capsule;

import com.after.backend.capsule.application.InvitationTokenService;
import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleMemberRole;
import com.after.backend.capsule.domain.CapsuleType;
import com.after.backend.capsule.domain.Invitation;
import com.after.backend.capsule.infrastructure.CapsuleMemberRepository;
import com.after.backend.capsule.infrastructure.CapsuleRepository;
import com.after.backend.capsule.infrastructure.InvitationRepository;
import com.after.backend.user.domain.User;
import com.after.backend.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
@AutoConfigureMockMvc
@Transactional
class InvitationApiIntegrationTest {

    @SuppressWarnings("deprecation")
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CapsuleRepository capsuleRepository;

    @Autowired
    private CapsuleMemberRepository capsuleMemberRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private InvitationTokenService invitationTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldAllowOwnerToCreateInvitation()
            throws Exception {

        User owner = createUser("owner@example.com");
        Capsule capsule = createSharedCapsule(owner);

        mockMvc.perform(
                        post(
                                "/api/capsules/{capsuleId}/invitations",
                                capsule.getId()
                        )
                                .with(as(owner))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());

        assertThat(invitationRepository.count())
                .isEqualTo(1);
    }

    @Test
    void shouldStoreOnlyTokenHash()
            throws Exception {

        User owner = createUser("owner@example.com");
        Capsule capsule = createSharedCapsule(owner);

        String rawToken =
                createInvitation(owner, capsule);

        String tokenHash =
                invitationTokenService.hash(rawToken);

        assertThat(
                invitationRepository.findByTokenHash(tokenHash)
        ).isPresent();

        assertThat(
                invitationRepository.findByTokenHash(rawToken)
        ).isEmpty();

        Invitation invitation =
                invitationRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow();

        assertThat(invitation.getTokenHash())
                .isEqualTo(tokenHash);

        assertThat(invitation.getTokenHash())
                .isNotEqualTo(rawToken);
    }

    @Test
    void shouldRejectInvitationCreationByContributor()
            throws Exception {

        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = Capsule.create(
                "Shared capsule",
                null,
                CapsuleType.SHARED,
                Instant.now().plusSeconds(3600),
                "Europe/Madrid",
                owner
        );

        capsule.addContributor(contributor);

        Capsule saved =
                capsuleRepository.saveAndFlush(capsule);

        mockMvc.perform(
                        post(
                                "/api/capsules/{capsuleId}/invitations",
                                saved.getId()
                        )
                                .with(as(contributor))
                )
                .andExpect(status().isForbidden());

        assertThat(invitationRepository.count())
                .isZero();
    }

    @Test
    void shouldRejectInvitationForPersonalCapsule()
            throws Exception {

        User owner = createUser("owner@example.com");

        Capsule capsule = Capsule.create(
                "Personal capsule",
                null,
                CapsuleType.PERSONAL,
                Instant.now().plusSeconds(3600),
                "Europe/Madrid",
                owner
        );

        Capsule saved =
                capsuleRepository.saveAndFlush(capsule);

        mockMvc.perform(
                        post(
                                "/api/capsules/{capsuleId}/invitations",
                                saved.getId()
                        )
                                .with(as(owner))
                )
                .andExpect(status().isBadRequest());

        assertThat(invitationRepository.count())
                .isZero();
    }

    @Test
    void shouldRejectInvitationCreationForSealedCapsule()
            throws Exception {

        User owner = createUser("owner@example.com");
        Capsule capsule = createSharedCapsule(owner);

        sealCapsule(capsule);

        mockMvc.perform(
                        post(
                                "/api/capsules/{capsuleId}/invitations",
                                capsule.getId()
                        )
                                .with(as(owner))
                )
                .andExpect(status().isBadRequest());

        assertThat(invitationRepository.count())
                .isZero();
    }

    @Test
    void shouldExposeInvitationWithoutAuthentication()
            throws Exception {

        User owner = createUser("owner@example.com");
        Capsule capsule = createSharedCapsule(owner);

        String token =
                createInvitation(owner, capsule);

        mockMvc.perform(
                        get(
                                "/api/invitations/{token}",
                                token
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.capsuleTitle")
                                .value("Shared capsule")
                )
                .andExpect(
                        jsonPath("$.ownerEmail")
                                .value("owner@example.com")
                )
                .andExpect(
                        jsonPath("$.opensAt")
                                .exists()
                );
    }

    @Test
    void shouldExposeOnlyMinimalInvitationInformation()
            throws Exception {

        User owner = createUser("owner@example.com");

        Capsule capsule = Capsule.create(
                "Private details",
                "This description must not be exposed",
                CapsuleType.SHARED,
                Instant.now().plusSeconds(3600),
                "Europe/Madrid",
                owner
        );

        Capsule saved =
                capsuleRepository.saveAndFlush(capsule);

        String token =
                createInvitation(owner, saved);

        mockMvc.perform(
                        get(
                                "/api/invitations/{token}",
                                token
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.capsuleTitle")
                                .value("Private details")
                )
                .andExpect(
                        jsonPath("$.ownerEmail")
                                .value("owner@example.com")
                )
                .andExpect(
                        jsonPath("$.opensAt")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.description")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.members")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.contributions")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.token")
                                .doesNotExist()
                );
    }

    @Test
    void shouldRejectUnknownInvitationToken()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/invitations/{token}",
                                "unknown-token"
                        )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectUsedInvitationToken()
            throws Exception {

        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = createSharedCapsule(owner);

        String token =
                createInvitation(owner, capsule);

        mockMvc.perform(
                        post(
                                "/api/invitations/{token}/accept",
                                token
                        )
                                .with(as(contributor))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/invitations/{token}",
                                token
                        )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRevokedInvitationToken()
            throws Exception {

        User owner = createUser("owner@example.com");
        Capsule capsule = createSharedCapsule(owner);

        String token =
                createInvitation(owner, capsule);

        Invitation invitation =
                invitationRepository
                        .findByTokenHash(
                                invitationTokenService.hash(token)
                        )
                        .orElseThrow();

        invitation.revoke(Instant.now());

        invitationRepository.saveAndFlush(invitation);

        mockMvc.perform(
                        get(
                                "/api/invitations/{token}",
                                token
                        )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowAuthenticatedUserToAcceptInvitation()
            throws Exception {

        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = createSharedCapsule(owner);

        String token =
                createInvitation(owner, capsule);

        mockMvc.perform(
                        post(
                                "/api/invitations/{token}/accept",
                                token
                        )
                                .with(as(contributor))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.capsuleId")
                                .value(
                                        capsule.getId().toString()
                                )
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("CONTRIBUTOR")
                );
    }

    @Test
    void shouldCreateContributorMembershipOnAcceptance()
            throws Exception {

        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = createSharedCapsule(owner);

        String token =
                createInvitation(owner, capsule);

        mockMvc.perform(
                        post(
                                "/api/invitations/{token}/accept",
                                token
                        )
                                .with(as(contributor))
                )
                .andExpect(status().isOk());

        assertThat(
                capsuleMemberRepository
                        .existsByCapsuleIdAndUserId(
                                capsule.getId(),
                                contributor.getId()
                        )
        ).isTrue();

        assertThat(
                capsuleMemberRepository
                        .findWithCapsuleByCapsuleIdAndUserId(
                                capsule.getId(),
                                contributor.getId()
                        )
                        .orElseThrow()
                        .getRole()
        ).isEqualTo(CapsuleMemberRole.CONTRIBUTOR);
    }

    @Test
    void shouldMarkInvitationAsAccepted()
            throws Exception {

        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = createSharedCapsule(owner);

        String token =
                createInvitation(owner, capsule);

        mockMvc.perform(
                        post(
                                "/api/invitations/{token}/accept",
                                token
                        )
                                .with(as(contributor))
                )
                .andExpect(status().isOk());

        Invitation invitation =
                invitationRepository
                        .findByTokenHash(
                                invitationTokenService.hash(token)
                        )
                        .orElseThrow();

        assertThat(invitation.isAccepted())
                .isTrue();

        assertThat(invitation.getAcceptedAt())
                .isNotNull();

        assertThat(invitation.getAcceptedByUser())
                .isNotNull();

        assertThat(
                invitation
                        .getAcceptedByUser()
                        .getId()
        ).isEqualTo(contributor.getId());
    }

    @Test
    void shouldRejectAcceptingInvitationTwice()
            throws Exception {

        User owner = createUser("owner@example.com");
        User firstUser =
                createUser("first@example.com");
        User secondUser =
                createUser("second@example.com");

        Capsule capsule = createSharedCapsule(owner);

        String token =
                createInvitation(owner, capsule);

        mockMvc.perform(
                        post(
                                "/api/invitations/{token}/accept",
                                token
                        )
                                .with(as(firstUser))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post(
                                "/api/invitations/{token}/accept",
                                token
                        )
                                .with(as(secondUser))
                )
                .andExpect(status().isBadRequest());

        assertThat(
                capsuleMemberRepository
                        .existsByCapsuleIdAndUserId(
                                capsule.getId(),
                                secondUser.getId()
                        )
        ).isFalse();
    }

    @Test
    void shouldRejectExistingMemberFromAcceptingInvitation()
            throws Exception {

        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = Capsule.create(
                "Shared capsule",
                null,
                CapsuleType.SHARED,
                Instant.now().plusSeconds(3600),
                "Europe/Madrid",
                owner
        );

        capsule.addContributor(contributor);

        Capsule saved =
                capsuleRepository.saveAndFlush(capsule);

        String token =
                createInvitation(owner, saved);

        mockMvc.perform(
                        post(
                                "/api/invitations/{token}/accept",
                                token
                        )
                                .with(as(contributor))
                )
                .andExpect(status().isBadRequest());

        Invitation invitation =
                invitationRepository
                        .findByTokenHash(
                                invitationTokenService.hash(token)
                        )
                        .orElseThrow();

        assertThat(invitation.isAccepted())
                .isFalse();
    }

    @Test
    void shouldRejectAcceptanceForSealedCapsule()
            throws Exception {

        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = createSharedCapsule(owner);

        String token =
                createInvitation(owner, capsule);

        sealCapsule(capsule);

        mockMvc.perform(
                        post(
                                "/api/invitations/{token}/accept",
                                token
                        )
                                .with(as(contributor))
                )
                .andExpect(status().isBadRequest());

        assertThat(
                capsuleMemberRepository
                        .existsByCapsuleIdAndUserId(
                                capsule.getId(),
                                contributor.getId()
                        )
        ).isFalse();

        Invitation invitation =
                invitationRepository
                        .findByTokenHash(
                                invitationTokenService.hash(token)
                        )
                        .orElseThrow();

        assertThat(invitation.isAccepted())
                .isFalse();
    }

    @Test
    void shouldRejectRevokedInvitationAcceptance()
            throws Exception {

        User owner = createUser("owner@example.com");
        User contributor =
                createUser("contributor@example.com");

        Capsule capsule = createSharedCapsule(owner);

        String token =
                createInvitation(owner, capsule);

        Invitation invitation =
                invitationRepository
                        .findByTokenHash(
                                invitationTokenService.hash(token)
                        )
                        .orElseThrow();

        invitation.revoke(Instant.now());

        invitationRepository.saveAndFlush(invitation);

        mockMvc.perform(
                        post(
                                "/api/invitations/{token}/accept",
                                token
                        )
                                .with(as(contributor))
                )
                .andExpect(status().isBadRequest());

        assertThat(
                capsuleMemberRepository
                        .existsByCapsuleIdAndUserId(
                                capsule.getId(),
                                contributor.getId()
                        )
        ).isFalse();
    }

    @Test
    void shouldRejectUnauthenticatedAcceptance()
            throws Exception {

        User owner = createUser("owner@example.com");
        Capsule capsule = createSharedCapsule(owner);

        String token =
                createInvitation(owner, capsule);

        mockMvc.perform(
                        post(
                                "/api/invitations/{token}/accept",
                                token
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedInvitationCreation()
            throws Exception {

        User owner = createUser("owner@example.com");
        Capsule capsule = createSharedCapsule(owner);

        mockMvc.perform(
                        post(
                                "/api/capsules/{capsuleId}/invitations",
                                capsule.getId()
                        )
                )
                .andExpect(status().isUnauthorized());

        assertThat(invitationRepository.count())
                .isZero();
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

    private Capsule createSharedCapsule(User owner) {
        Capsule capsule = Capsule.create(
                "Shared capsule",
                "Shared capsule description",
                CapsuleType.SHARED,
                Instant.now().plusSeconds(3600),
                "Europe/Madrid",
                owner
        );

        return capsuleRepository.saveAndFlush(capsule);
    }

    private String createInvitation(
            User owner,
            Capsule capsule
    ) throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/capsules/{capsuleId}/invitations",
                                        capsule.getId()
                                )
                                        .with(as(owner))
                        )
                        .andExpect(status().isCreated())
                        .andReturn();

        String response =
                result.getResponse().getContentAsString();

        return extractToken(response);
    }

    private String extractToken(String response) {
        String prefix = "{\"token\":\"";
        String suffix = "\"}";

        if (
                !response.startsWith(prefix)
                        || !response.endsWith(suffix)
        ) {
            throw new IllegalStateException(
                    "Unexpected invitation response: "
                            + response
            );
        }

        return response.substring(
                prefix.length(),
                response.length() - suffix.length()
        );
    }

    private void sealCapsule(Capsule capsule) {
        jdbcTemplate.update(
                """
                UPDATE capsules
                SET status = 'SEALED'
                WHERE id = ?
                """,
                capsule.getId()
        );

        entityManager.clear();
    }

    private RequestPostProcessor as(User user) {
        return jwt().jwt(jwt ->
                jwt
                        .subject(user.getId().toString())
                        .claim(
                                "email",
                                user.getEmail()
                        )
        );
    }
}