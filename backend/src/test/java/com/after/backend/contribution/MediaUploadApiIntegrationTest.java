package com.after.backend.contribution;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleType;
import com.after.backend.capsule.infrastructure.CapsuleMemberRepository;
import com.after.backend.capsule.infrastructure.CapsuleRepository;
import com.after.backend.contribution.infrastructure.ContributionRepository;
import com.after.backend.storage.ObjectStorageService;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
class MediaUploadApiIntegrationTest {

    private static final String ACCESS_KEY =
            "after";

    private static final String SECRET_KEY =
            "after-minio-dev";

    private static final String BUCKET =
            "after-media";

    @SuppressWarnings("deprecation")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            );

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> minio =
            new GenericContainer<>(
                    DockerImageName.parse(
                            "bitnamilegacy/minio:2025.7.23-debian-12-r5"
                    )
            )
                    .withEnv(
                            "MINIO_ROOT_USER",
                            ACCESS_KEY
                    )
                    .withEnv(
                            "MINIO_ROOT_PASSWORD",
                            SECRET_KEY
                    )
                    .withEnv(
                            "MINIO_DEFAULT_BUCKETS",
                            BUCKET
                    )
                    .withExposedPorts(
                            9000
                    )
                    .waitingFor(
                            Wait
                                    .forHttp(
                                            "/minio/health/live"
                                    )
                                    .forPort(
                                            9000
                                    )
                                    .forStatusCode(
                                            200
                                    )
                                    .withStartupTimeout(
                                            Duration.ofMinutes(2)
                                    )
                    );

    @DynamicPropertySource
    static void configureStorage(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "storage.endpoint",
                () ->
                        "http://"
                                + minio.getHost()
                                + ":"
                                + minio.getMappedPort(
                                        9000
                                )
        );

        registry.add(
                "storage.access-key",
                () -> ACCESS_KEY
        );

        registry.add(
                "storage.secret-key",
                () -> SECRET_KEY
        );

        registry.add(
                "storage.bucket",
                () -> BUCKET
        );

        registry.add(
                "storage.region",
                () -> "us-east-1"
        );

        registry.add(
                "storage.presign-ttl",
                () -> "15m"
        );
    }

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

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private ObjectStorageService
            objectStorageService;

    @BeforeEach
    void prepareTest() {
        contributionRepository.deleteAll();
        capsuleMemberRepository.deleteAll();
        capsuleRepository.deleteAll();
        userRepository.deleteAll();

        ensureBucketExists();
    }

    @Test
    void shouldInitiateImageUploadAndPersistUploadingMediaObject()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        MvcResult result =
                initiateUpload(
                        owner,
                        capsule.getId(),
                        "IMAGE",
                        "photo.jpg",
                        "image/jpeg",
                        1024L
                )
                        .andExpect(
                                status().isCreated()
                        )
                        .andExpect(
                                jsonPath(
                                        "$.contributionId"
                                ).isNotEmpty()
                        )
                        .andExpect(
                                jsonPath(
                                        "$.uploadUrl"
                                ).isNotEmpty()
                        )
                        .andExpect(
                                jsonPath(
                                        "$.httpMethod"
                                ).value(
                                        "PUT"
                                )
                        )
                        .andExpect(
                                jsonPath(
                                        "$.expiresAt"
                                ).isNotEmpty()
                        )
                        .andExpect(
                                jsonPath(
                                        "$.headers['Content-Type']"
                                ).value(
                                        "image/jpeg"
                                )
                        )
                        .andReturn();

        UUID contributionId =
                UUID.fromString(
                        jsonField(
                                result,
                                "contributionId"
                        )
                );

        assertThat(
                jdbcTemplate.queryForObject(
                        """
                        SELECT type::text
                        FROM contributions
                        WHERE id = ?
                        """,
                        String.class,
                        contributionId
                )
        ).isEqualTo(
                "IMAGE"
        );

        Map<String, Object> mediaObject =
                jdbcTemplate.queryForMap(
                        """
                        SELECT
                            status::text AS status,
                            original_object_key,
                            processed_object_key,
                            thumbnail_object_key,
                            original_filename,
                            original_mime_type,
                            processed_mime_type,
                            size_bytes,
                            duration_ms,
                            width,
                            height
                        FROM media_objects
                        WHERE contribution_id = ?
                        """,
                        contributionId
                );

        assertThat(
                mediaObject.get(
                        "status"
                )
        ).isEqualTo(
                "UPLOADING"
        );

        assertThat(
                mediaObject.get(
                        "original_object_key"
                )
        )
                .asString()
                .startsWith(
                        "objects/"
                );

        assertThat(
                mediaObject.get(
                        "original_filename"
                )
        ).isEqualTo(
                "photo.jpg"
        );

        assertThat(
                mediaObject.get(
                        "original_mime_type"
                )
        ).isEqualTo(
                "image/jpeg"
        );

        assertThat(
                mediaObject.get(
                        "size_bytes"
                )
        ).isEqualTo(
                1024L
        );

        assertThat(
                mediaObject.get(
                        "processed_object_key"
                )
        ).isNull();

        assertThat(
                mediaObject.get(
                        "thumbnail_object_key"
                )
        ).isNull();

        assertThat(
                mediaObject.get(
                        "processed_mime_type"
                )
        ).isNull();

        assertThat(
                mediaObject.get(
                        "duration_ms"
                )
        ).isNull();

        assertThat(
                mediaObject.get(
                        "width"
                )
        ).isNull();

        assertThat(
                mediaObject.get(
                        "height"
                )
        ).isNull();
    }

    @Test
    void shouldAllowContributorToInitiateMediaUpload()
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
                                .plusSeconds(
                                        3600
                                ),
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

        initiateUpload(
                contributor,
                saved.getId(),
                "AUDIO",
                "memory.mp3",
                "audio/mpeg",
                2048L
        )
                .andExpect(
                        status().isCreated()
                );
    }

    @Test
    void shouldRejectNonMemberMediaUpload()
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

        initiateUpload(
                outsider,
                capsule.getId(),
                "IMAGE",
                "photo.jpg",
                "image/jpeg",
                1024L
        )
                .andExpect(
                        status().isNotFound()
                );

        assertThat(
                contributionRepository.count()
        ).isZero();
    }

    @Test
    void shouldRejectMediaUploadWhenCapsuleIsSealed()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
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

        initiateUpload(
                owner,
                capsule.getId(),
                "IMAGE",
                "photo.jpg",
                "image/jpeg",
                1024L
        )
                .andExpect(
                        status().isBadRequest()
                );

        assertThat(
                contributionRepository.count()
        ).isZero();
    }

    @Test
    void shouldRejectTextContributionType()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        initiateUpload(
                owner,
                capsule.getId(),
                "TEXT",
                "memory.txt",
                "text/plain",
                1024L
        )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void shouldRejectUnsupportedMimeType()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        initiateUpload(
                owner,
                capsule.getId(),
                "IMAGE",
                "animation.gif",
                "image/gif",
                1024L
        )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void shouldRejectMimeTypeFromDifferentCategory()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        initiateUpload(
                owner,
                capsule.getId(),
                "IMAGE",
                "video.mp4",
                "video/mp4",
                1024L
        )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void shouldRejectFileAboveConfiguredMaximum()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        initiateUpload(
                owner,
                capsule.getId(),
                "IMAGE",
                "large.jpg",
                "image/jpeg",
                10_485_761L
        )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void shouldRejectNonPositiveSize()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        initiateUpload(
                owner,
                capsule.getId(),
                "IMAGE",
                "photo.jpg",
                "image/jpeg",
                0L
        )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void shouldRejectFilenameLongerThanPersistenceLimit()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        initiateUpload(
                owner,
                capsule.getId(),
                "IMAGE",
                "a".repeat(
                        256
                ),
                "image/jpeg",
                1024L
        )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void shouldUploadDirectlyUsingReturnedPresignedContract()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        Capsule capsule =
                createSharedCapsule(
                        owner
                );

        MvcResult result =
                initiateUpload(
                        owner,
                        capsule.getId(),
                        "IMAGE",
                        "photo.jpg",
                        "image/jpeg",
                        4L
                )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn();

        UUID contributionId =
                UUID.fromString(
                        jsonField(
                                result,
                                "contributionId"
                        )
                );

        String uploadUrl =
                jsonField(
                        result,
                        "uploadUrl"
                );

        String contentType =
                jsonHeader(
                        result,
                        "Content-Type"
                );

        String objectKey =
                jdbcTemplate.queryForObject(
                        """
                        SELECT original_object_key
                        FROM media_objects
                        WHERE contribution_id = ?
                        """,
                        String.class,
                        contributionId
                );

        byte[] content =
                new byte[] {
                        1,
                        2,
                        3,
                        4
                };

        HttpRequest uploadRequest =
                HttpRequest
                        .newBuilder(
                                URI.create(
                                        uploadUrl
                                )
                        )
                        .header(
                                "Content-Type",
                                contentType
                        )
                        .PUT(
                                HttpRequest
                                        .BodyPublishers
                                        .ofByteArray(
                                                content
                                        )
                        )
                        .build();

        HttpResponse<String> uploadResponse =
                HttpClient
                        .newHttpClient()
                        .send(
                                uploadRequest,
                                HttpResponse
                                        .BodyHandlers
                                        .ofString()
                        );

        assertThat(
                uploadResponse.statusCode()
        ).isBetween(
                200,
                299
        );

        assertThat(
                objectStorageService.exists(
                        objectKey
                )
        ).isTrue();

        objectStorageService.delete(
                objectKey
        );
    }

    private ResultActions initiateUpload(
            User user,
            UUID capsuleId,
            String type,
            String originalFilename,
            String mimeType,
            long sizeBytes
    ) throws Exception {

        return mockMvc.perform(
                post(
                        "/api/capsules/{capsuleId}/contributions/media",
                        capsuleId
                )
                        .with(
                                as(user)
                        )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                """
                                {
                                  "type": "%s",
                                  "originalFilename": "%s",
                                  "mimeType": "%s",
                                  "sizeBytes": %d
                                }
                                """
                                        .formatted(
                                                type,
                                                originalFilename,
                                                mimeType,
                                                sizeBytes
                                        )
                        )
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
                                .plusSeconds(
                                        3600
                                ),
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

    @SuppressWarnings("deprecation")
    private String jsonField(
            MvcResult result,
            String field
    ) throws Exception {

        return jsonMapper
                .readTree(
                        result
                                .getResponse()
                                .getContentAsString()
                )
                .get(field)
                .asText();
    }

    @SuppressWarnings("deprecation")
    private String jsonHeader(
            MvcResult result,
            String header
    ) throws Exception {

        return jsonMapper
                .readTree(
                        result
                                .getResponse()
                                .getContentAsString()
                )
                .get("headers")
                .get(header)
                .asText();
    }

    private void ensureBucketExists() {
        try {
            s3Client.createBucket(
                    CreateBucketRequest
                            .builder()
                            .bucket(
                                    BUCKET
                            )
                            .build()
            );
        } catch (
                S3Exception exception
        ) {
            if (
                    exception.statusCode()
                            != 409
            ) {
                throw exception;
            }
        }
    }
}