package com.after.mediaworker.processing;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "worker.polling.enabled=false",
        "spring.flyway.enabled=false"
})
class MediaProcessingWorkerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @MockitoBean
    private S3Client s3Client;

    @Autowired
    private MediaProcessingJobQueueRepository queue;

    @Autowired
    private MediaProcessingWorker applicationWorker;

    @Autowired
    private QueueProcessingPolicy policy;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void migrate(@Autowired DataSource dataSource) {
        Path migrations = Path.of(
                "..", "backend", "src", "main",
                "resources", "db", "migration"
        ).toAbsolutePath().normalize();

        assertThat(Files.isDirectory(migrations)).isTrue();

        Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:" + migrations)
                .load()
                .migrate();
    }

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM media_processing_jobs");
        jdbc.update("DELETE FROM contributions");
        jdbc.update("DELETE FROM capsules");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void actualFlywaySchemaIncludesT19AndT21Migrations() {
        assertThat(jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version IN ('7', '8')
                  AND success = TRUE
                """,
                Integer.class
        )).isEqualTo(2);

        assertThat(jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'media_processing_jobs'
                  AND column_name IN (
                      'claim_token',
                      'lease_expires_at',
                      'next_attempt_at'
                  )
                """,
                Integer.class
        )).isEqualTo(3);
    }

    @Test
    void claimsTransitionsAndCompletes() {
        UUID id = insertPendingJob();

        MediaProcessingWorker worker = worker(job -> {
            Map<String, Object> during = row(job.id());

            assertThat(during.get("status"))
                    .isEqualTo("PROCESSING");

            assertThat(during.get("attempts"))
                    .isEqualTo(1);
        });

        Optional<ClaimedMediaProcessingJob> processed =
                worker.processNextAvailableJob();

        assertThat(processed).isPresent();
        assertThat(processed.get().id()).isEqualTo(id);
        assertThat(processed.get().claimToken()).isNotNull();

        assertThat(row(id).get("status"))
                .isEqualTo("COMPLETED");

        assertThat(row(id).get("error")).isNull();
        assertThat(row(id).get("completed_at")).isNotNull();
    }

    @Test
    void emptyQueueDoesNotClaim() {
        assertThat(worker(job -> {})
                .processNextAvailableJob()).isEmpty();
    }

    @Test
    void noopHandlerNeverConsumesPendingJob() {
        UUID id = insertPendingJob();

        assertThat(applicationWorker.isOperational()).isFalse();

        assertThat(applicationWorker.processNextAvailableJob())
                .isEmpty();

        assertThat(row(id).get("status"))
                .isEqualTo("PENDING");

        assertThat(row(id).get("attempts"))
                .isEqualTo(0);
    }

    @Test
    void nonRetryableFailureIsTerminal() {
        UUID id = insertPendingJob();

        worker(job -> {
            throw new NonRetryableMediaProcessingException(
                    "invalid media"
            );
        }).processNextAvailableJob();

        assertThat(row(id).get("status"))
                .isEqualTo("FAILED");

        assertThat(row(id).get("attempts"))
                .isEqualTo(1);

        assertThat(row(id).get("error"))
                .isEqualTo("invalid media");
    }

    @Test
    void retryableFailuresStopAfterMaxAttempts() {
        UUID id = insertPendingJob();

        MediaProcessingWorker worker = worker(job -> {
            throw new IllegalStateException("transient");
        });

        for (int attempt = 1;
             attempt <= policy.maxAttempts();
             attempt++) {

            assertThat(worker.processNextAvailableJob())
                    .isPresent();

            assertThat(row(id).get("attempts"))
                    .isEqualTo(attempt);

            if (attempt < policy.maxAttempts()) {
                assertThat(row(id).get("status"))
                        .isEqualTo("PENDING");

                assertThat(row(id).get("completed_at"))
                        .isNull();

                jdbc.update(
                        """
                        UPDATE media_processing_jobs
                        SET next_attempt_at =
                            CURRENT_TIMESTAMP - INTERVAL '1 second'
                        WHERE id = ?
                        """,
                        id
                );
            }
        }

        assertThat(row(id).get("status"))
                .isEqualTo("FAILED");

        assertThat(row(id).get("completed_at"))
                .isNotNull();
    }

    @Test
    void backoffPreventsImmediateRedelivery() {
        UUID id = insertPendingJob();

        MediaProcessingWorker worker = worker(job -> {
            throw new IllegalArgumentException("temporary");
        });

        worker.processNextAvailableJob();

        assertThat(worker.processNextAvailableJob()).isEmpty();

        assertThat(row(id).get("status"))
                .isEqualTo("PENDING");

        assertThat(row(id).get("attempts"))
                .isEqualTo(1);
    }

    @Test
    void sameJobCannotBeClaimedWhileFirstHandlerIsActive()
            throws Exception {

        UUID id = insertPendingJob();

        CountDownLatch processing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        MediaProcessingWorker first = worker(job -> {
            processing.countDown();

            assertThat(release.await(15, TimeUnit.SECONDS))
                    .isTrue();
        });

        MediaProcessingWorker second = worker(job -> {});

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        try {
            Future<Optional<ClaimedMediaProcessingJob>> future =
                    executor.submit(first::processNextAvailableJob);

            assertThat(processing.await(15, TimeUnit.SECONDS))
                    .isTrue();

            assertThat(second.processNextAvailableJob())
                    .isEmpty();

            release.countDown();

            assertThat(future.get(15, TimeUnit.SECONDS))
                    .isPresent();

            assertThat(row(id).get("status"))
                    .isEqualTo("COMPLETED");

            assertThat(row(id).get("attempts"))
                    .isEqualTo(1);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void twoWorkersCanOwnDistinctJobsInParallel()
            throws Exception {

        UUID a = insertPendingJob();
        UUID b = insertPendingJob();

        CountDownLatch bothProcessing = new CountDownLatch(2);

        MediaProcessingTaskHandler handler = job -> {
            bothProcessing.countDown();

            assertThat(
                    bothProcessing.await(15, TimeUnit.SECONDS)
            ).isTrue();
        };

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<Optional<ClaimedMediaProcessingJob>> f1 =
                    executor.submit(
                            worker(handler)::processNextAvailableJob
                    );

            Future<Optional<ClaimedMediaProcessingJob>> f2 =
                    executor.submit(
                            worker(handler)::processNextAvailableJob
                    );

            List<UUID> ids = List.of(
                    f1.get(15, TimeUnit.SECONDS)
                            .orElseThrow().id(),
                    f2.get(15, TimeUnit.SECONDS)
                            .orElseThrow().id()
            );

            assertThat(ids)
                    .containsExactlyInAnyOrder(a, b);

            assertThat(row(a).get("status"))
                    .isEqualTo("COMPLETED");

            assertThat(row(b).get("status"))
                    .isEqualTo("COMPLETED");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void leaseExpiryRecoversJobAndRejectsOriginalOwner() {
        UUID id = insertPendingJob();

        ClaimedMediaProcessingJob first =
                queue.claimNextPending(
                        policy.leaseDuration()
                ).orElseThrow();

        assertThat(queue.renewLease(
                first,
                policy.leaseDuration()
        )).isTrue();

        assertThat(queue.recoverExpired(
                policy.maxAttempts(),
                Duration.ofSeconds(1),
                100
        )).isZero();

        jdbc.update(
                """
                UPDATE media_processing_jobs
                SET lease_expires_at =
                    CURRENT_TIMESTAMP - INTERVAL '1 second'
                WHERE id = ?
                """,
                id
        );

        assertThat(queue.recoverExpired(
                policy.maxAttempts(),
                Duration.ofSeconds(1),
                100
        )).isEqualTo(1);

        assertThat(row(id).get("status"))
                .isEqualTo("PENDING");

        assertThat(queue.markCompleted(first))
                .isFalse();

        assertThat(queue.markFailed(
                first,
                "late",
                false,
                Duration.ZERO
        )).isFalse();

        jdbc.update(
                """
                UPDATE media_processing_jobs
                SET next_attempt_at =
                    CURRENT_TIMESTAMP - INTERVAL '1 second'
                WHERE id = ?
                """,
                id
        );

        ClaimedMediaProcessingJob second =
                queue.claimNextPending(
                        policy.leaseDuration()
                ).orElseThrow();

        assertThat(second.claimToken())
                .isNotEqualTo(first.claimToken());

        assertThat(queue.markCompleted(second))
                .isTrue();

        assertThat(row(id).get("attempts"))
                .isEqualTo(2);

        assertThat(row(id).get("status"))
                .isEqualTo("COMPLETED");
    }

    @Test
    void recoveryStopsRetryingWhenBudgetIsExhausted() {
        UUID id = insertPendingJob();

        jdbc.update(
                """
                UPDATE media_processing_jobs
                SET
                    status = 'PROCESSING',
                    attempts = ?,
                    lease_expires_at =
                        CURRENT_TIMESTAMP - INTERVAL '1 second',
                    claim_token = ?
                WHERE id = ?
                """,
                policy.maxAttempts(),
                UUID.randomUUID(),
                id
        );

        assertThat(queue.recoverExpired(
                policy.maxAttempts(),
                Duration.ofSeconds(1),
                100
        )).isEqualTo(1);

        assertThat(row(id).get("status"))
                .isEqualTo("FAILED");

        assertThat(row(id).get("completed_at"))
                .isNotNull();
    }

    @Test
    void disabledPollingHasNoSchedulerBean(
            @Autowired org.springframework.context.ApplicationContext context
    ) {
        assertThat(
                context.getBeansOfType(MediaWorkerScheduler.class)
        ).isEmpty();
    }

    private MediaProcessingWorker worker(
            MediaProcessingTaskHandler handler
    ) {
        return new MediaProcessingWorker(
                queue,
                handler,
                policy
        );
    }

    private UUID insertPendingJob() {
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
                VALUES (?, ?, 'hash',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                user,
                "worker-test-" + user + "@example.com"
        );

        jdbc.update(
                """
                INSERT INTO capsules(
                    id, title, type, status, opens_at, timezone,
                    created_at, updated_at
                )
                VALUES (
                    ?, 'Worker test', 'PERSONAL', 'COLLECTING',
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

        return job;
    }

    private Map<String, Object> row(UUID jobId) {
        return jdbc.queryForMap(
                """
                SELECT
                    status::text AS status,
                    attempts,
                    started_at,
                    completed_at,
                    error
                FROM media_processing_jobs
                WHERE id = ?
                """,
                jobId
        );
    }
}