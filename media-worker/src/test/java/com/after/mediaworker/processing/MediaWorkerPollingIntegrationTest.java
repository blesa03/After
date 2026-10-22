package com.after.mediaworker.processing;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "worker.polling.enabled=true",
        "worker.polling.fixed-delay=50",
        "worker.lease.heartbeat-interval-ms=100",
        "spring.flyway.enabled=false"
})
class MediaWorkerPollingIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @TestConfiguration
    static class HandlerConfiguration {

        @Bean
        MediaProcessingTaskHandler testTaskHandler() {
            return job -> {
                // Simulated successful processing for integration tests.
            };
        }
    }

    @MockitoBean
    private S3Client s3Client;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void migrate(@Autowired DataSource dataSource) {
        Path migrations = Path.of(
                "..", "backend", "src", "main",
                "resources", "db", "migration"
        ).toAbsolutePath().normalize();

        Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:" + migrations)
                .load()
                .migrate();
    }

    @Test
    void schedulingAutomaticallyClaimsAndCompletesJob()
            throws Exception {

        UUID user = UUID.randomUUID();
        UUID capsule = UUID.randomUUID();
        UUID contribution = UUID.randomUUID();
        UUID job = UUID.randomUUID();

        jdbc.update(
                """
                INSERT INTO users(
                    id, email, password_hash,
                    created_at, updated_at
                )
                VALUES (
                    ?, ?, 'hash',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """,
                user,
                "scheduled-" + user + "@example.com"
        );

        jdbc.update(
                """
                INSERT INTO capsules(
                    id, title, type, status, opens_at, timezone,
                    created_at, updated_at
                )
                VALUES (
                    ?, 'Polling test', 'PERSONAL', 'COLLECTING',
                    CURRENT_TIMESTAMP + INTERVAL '1 day',
                    'Europe/Madrid',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """,
                capsule
        );

        jdbc.update(
                """
                INSERT INTO contributions(
                    id, capsule_id, author_user_id, type,
                    created_at, updated_at
                )
                VALUES (
                    ?, ?, ?, 'IMAGE',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """,
                contribution,
                capsule,
                user
        );

        jdbc.update(
                """
                INSERT INTO media_processing_jobs(
                    id, contribution_id
                )
                VALUES (?, ?)
                """,
                job,
                contribution
        );

        Instant deadline =
                Instant.now().plus(Duration.ofSeconds(8));

        String status = null;

        while (Instant.now().isBefore(deadline)) {
            status = jdbc.queryForObject(
                    """
                    SELECT status::text
                    FROM media_processing_jobs
                    WHERE id = ?
                    """,
                    String.class,
                    job
            );

            if ("COMPLETED".equals(status)) {
                break;
            }

            Thread.sleep(50);
        }

        assertThat(status).isEqualTo("COMPLETED");

        assertThat(jdbc.queryForObject(
                """
                SELECT attempts
                FROM media_processing_jobs
                WHERE id = ?
                """,
                Integer.class,
                job
        )).isEqualTo(1);
    }
}