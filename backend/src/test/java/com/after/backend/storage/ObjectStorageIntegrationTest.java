package com.after.backend.storage;

import com.after.backend.storage.config.StorageConfig;
import com.after.backend.storage.config.StorageProperties;
import com.after.backend.storage.s3.S3ObjectStorageService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ObjectStorageIntegrationTest {

    private static final String ACCESS_KEY = "after";
    private static final String SECRET_KEY =
            "after-minio-dev";
    private static final String BUCKET = "after-media";

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> minio =
            new GenericContainer<>(
                    DockerImageName.parse("minio/minio:latest")
            )
                    .withEnv(
                            "MINIO_ROOT_USER",
                            ACCESS_KEY
                    )
                    .withEnv(
                            "MINIO_ROOT_PASSWORD",
                            SECRET_KEY
                    )
                    .withCommand("server", "/data")
                    .withExposedPorts(9000)
                    .waitingFor(
                            Wait.forHttp("/minio/health/live")
                                    .forPort(9000)
                                    .forStatusCode(200)
                    );

    private static S3Client s3Client;
    private static S3Presigner s3Presigner;
    private static ObjectStorageService storage;
    private static HttpClient httpClient;
    private static String endpoint;

    @BeforeAll
    static void setUp() {
        endpoint = "http://"
                + minio.getHost()
                + ":"
                + minio.getMappedPort(9000);

        StorageProperties properties =
                new StorageProperties(
                        endpoint,
                        ACCESS_KEY,
                        SECRET_KEY,
                        BUCKET,
                        "us-east-1",
                        Duration.ofMinutes(15)
                );

        StorageConfig config = new StorageConfig();

        s3Client = config.s3Client(properties);
        s3Presigner = config.s3Presigner(properties);

        s3Client.createBucket(
                CreateBucketRequest.builder()
                        .bucket(BUCKET)
                        .build()
        );

        storage = new S3ObjectStorageService(
                s3Client,
                s3Presigner,
                properties
        );

        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    static void tearDown() {
        if (s3Presigner != null) {
            s3Presigner.close();
        }

        if (s3Client != null) {
            s3Client.close();
        }
    }

    @Test
    void shouldGenerateProviderIndependentObjectKeys() {
        PresignedUpload first =
                storage.createPresignedUpload(
                        "image/jpeg"
                );

        PresignedUpload second =
                storage.createPresignedUpload(
                        "image/jpeg"
                );

        assertThat(first.objectKey())
                .startsWith("objects/")
                .isNotEqualTo(second.objectKey());

        assertThat(first.objectKey())
                .doesNotContain("photo.jpg")
                .doesNotContain("image.jpeg");
    }

    @Test
    void shouldUploadAndDownloadUsingPresignedUrls()
            throws Exception {

        String content = "After object storage test";

        PresignedUpload upload =
                storage.createPresignedUpload(
                        "text/plain"
                );

        HttpRequest uploadRequest =
                HttpRequest.newBuilder(upload.url())
                        .header(
                                "Content-Type",
                                "text/plain"
                        )
                        .PUT(
                                HttpRequest.BodyPublishers
                                        .ofString(content)
                        )
                        .build();

        HttpResponse<String> uploadResponse =
                httpClient.send(
                        uploadRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

        assertThat(uploadResponse.statusCode())
                .isBetween(200, 299);

        assertThat(storage.exists(upload.objectKey()))
                .isTrue();

        PresignedDownload download =
                storage.createPresignedDownload(
                        upload.objectKey()
                );

        HttpRequest downloadRequest =
                HttpRequest.newBuilder(download.url())
                        .GET()
                        .build();

        HttpResponse<String> downloadResponse =
                httpClient.send(
                        downloadRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

        assertThat(downloadResponse.statusCode())
                .isEqualTo(200);

        assertThat(downloadResponse.body())
                .isEqualTo(content);
    }

    @Test
    void shouldKeepBucketPrivate()
            throws Exception {

        PresignedUpload upload =
                storage.createPresignedUpload(
                        "text/plain"
                );

        HttpRequest uploadRequest =
                HttpRequest.newBuilder(upload.url())
                        .header(
                                "Content-Type",
                                "text/plain"
                        )
                        .PUT(
                                HttpRequest.BodyPublishers
                                        .ofString("private")
                        )
                        .build();

        httpClient.send(
                uploadRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        URI unsignedObjectUrl = URI.create(
                endpoint
                        + "/"
                        + BUCKET
                        + "/"
                        + upload.objectKey()
        );

        HttpResponse<String> anonymousResponse =
                httpClient.send(
                        HttpRequest.newBuilder(
                                        unsignedObjectUrl
                                )
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString()
                );

        assertThat(anonymousResponse.statusCode())
                .isEqualTo(403);
    }

    @Test
    void shouldDeleteObject()
            throws Exception {

        PresignedUpload upload =
                storage.createPresignedUpload(
                        "text/plain"
                );

        HttpRequest request =
                HttpRequest.newBuilder(upload.url())
                        .header(
                                "Content-Type",
                                "text/plain"
                        )
                        .PUT(
                                HttpRequest.BodyPublishers
                                        .ofString("delete me")
                        )
                        .build();

        httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(storage.exists(upload.objectKey()))
                .isTrue();

        storage.delete(upload.objectKey());

        assertThat(storage.exists(upload.objectKey()))
                .isFalse();
    }

    @Test
    void shouldReportMissingObject() {
        assertThat(
                storage.exists(
                        "objects/"
                                + java.util.UUID.randomUUID()
                )
        ).isFalse();
    }
}