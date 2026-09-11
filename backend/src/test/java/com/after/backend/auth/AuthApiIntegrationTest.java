package com.after.backend.auth;

import com.after.backend.auth.infrastructure.RefreshTokenRepository;
import com.after.backend.user.domain.User;
import com.after.backend.user.infrastructure.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "after.auth.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef"
})
@ActiveProfiles("prod")
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

    private static final String REFRESH_COOKIE =
            "after_refresh";

    private static final String PASSWORD =
            "correct-password-123";

    @SuppressWarnings("deprecation")
@Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserWithNormalizedEmailAndHashedPassword()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "TEST@EXAMPLE.COM",
                                  "password": "correct-password-123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email")
                        .value("test@example.com"));

        User user = userRepository
                .findByEmail("test@example.com")
                .orElseThrow();

        assertThat(user.getPasswordHash())
                .isNotEqualTo(PASSWORD);

        assertThat(
                passwordEncoder.matches(
                        PASSWORD,
                        user.getPasswordHash()
                )
        ).isTrue();
    }

    @Test
    void shouldRejectInvalidRegistrationEmail()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "correct-password-123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidRegistrationPassword()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateNormalizedEmail()
            throws Exception {

        register("test@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "TEST@EXAMPLE.COM",
                                  "password": "another-password-123"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldLoginAndReturnAccessTokenAndRefreshCookie()
            throws Exception {

        register("test@example.com");

        MvcResult result = login(
                "TEST@EXAMPLE.COM",
                PASSWORD
        );

        result.getResponse();

        String setCookie = result.getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Strict");
        assertThat(setCookie).contains("Path=/api/auth");

        Cookie refreshCookie = refreshCookie(result);

        assertThat(refreshCookie.getValue()).isNotBlank();

        assertThat(refreshTokenRepository.findAll())
                .hasSize(1)
                .allSatisfy(token ->
                        assertThat(token.getTokenHash())
                                .isNotEqualTo(
                                        refreshCookie.getValue()
                                )
                );
    }

    @Test
    void shouldRejectIncorrectCredentials()
            throws Exception {

        register("test@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com",
                                  "password": "incorrect-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedProtectedEndpoint()
            throws Exception {

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCurrentUserForValidBearerToken()
            throws Exception {

        register("test@example.com");

        MvcResult login = login(
                "test@example.com",
                PASSWORD
        );

        String accessToken = accessToken(login);

        mockMvc.perform(get("/api/auth/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("test@example.com"));
    }

    @Test
    void shouldRotateRefreshTokenAndInvalidatePreviousToken()
            throws Exception {

        register("test@example.com");

        MvcResult login = login(
                "test@example.com",
                PASSWORD
        );

        Cookie firstCookie = refreshCookie(login);

        MvcResult refresh = mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(
                                        new Cookie(
                                                REFRESH_COOKIE,
                                                firstCookie.getValue()
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie secondCookie = refreshCookie(refresh);

        assertThat(secondCookie.getValue())
                .isNotEqualTo(firstCookie.getValue());

        assertThat(refreshTokenRepository.findAll())
                .hasSize(2);

        long revokedTokens = refreshTokenRepository
                .findAll()
                .stream()
                .filter(token ->
                        token.getRevokedAt() != null
                )
                .count();

        assertThat(revokedTokens).isEqualTo(1);

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(
                                        new Cookie(
                                                REFRESH_COOKIE,
                                                firstCookie.getValue()
                                        )
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLogoutRevokeRefreshAndClearCookie()
            throws Exception {

        register("test@example.com");

        MvcResult login = login(
                "test@example.com",
                PASSWORD
        );

        Cookie refreshCookie = refreshCookie(login);

        MvcResult logout = mockMvc.perform(
                        post("/api/auth/logout")
                                .cookie(
                                        new Cookie(
                                                REFRESH_COOKIE,
                                                refreshCookie.getValue()
                                        )
                                )
                )
                .andExpect(status().isNoContent())
                .andReturn();

        String setCookie = logout.getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("Max-Age=0");

        assertThat(refreshTokenRepository.findAll())
                .hasSize(1)
                .allSatisfy(token ->
                        assertThat(token.getRevokedAt())
                                .isNotNull()
                );

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(
                                        new Cookie(
                                                REFRESH_COOKIE,
                                                refreshCookie.getValue()
                                        )
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private MvcResult login(
            String email,
            String password
    ) throws Exception {

        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.expiresIn")
                        .value(900))
                .andReturn();
    }

    private Cookie refreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse()
                .getCookie(REFRESH_COOKIE);

        assertThat(cookie).isNotNull();

        return cookie;
    }

    @SuppressWarnings("deprecation")
private String accessToken(MvcResult result)
            throws Exception {

        return jsonMapper
                .readTree(
                        result.getResponse()
                                .getContentAsString()
                )
                .get("accessToken")
                .asText();
    }
}