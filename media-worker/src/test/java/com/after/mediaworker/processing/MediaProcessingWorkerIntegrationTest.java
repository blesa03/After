package com.after.mediaworker.processing;

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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        properties = {
                "worker.polling.enabled=false"
        }
)
class MediaProcessingWorkerIntegrationTest {

    @SuppressWarnings("deprecation")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            );

    @MockitoBean
    private S3Client s3Client;

    @Autowired
    private MediaProcessingJobQueueRepository
            queueRepository;

    @Autowired
    private MediaProcessingWorker
            defaultWorker;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void initSchema(
            @Autowired JdbcTemplate jdbcTemplate
    ) {
        jdbcTemplate.execute(
                """
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1
                        FROM pg_type
                        WHERE typname = 'media_processing_job_status'
                    ) THEN
                        CREATE TYPE media_processing_job_status AS ENUM (
                            'PENDING',
                            'PROCESSING',
                            'COMPLETED',
                            'FAILED'
                        );
                    END IF;
                END
                $$;
                """
        );

        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS media_processing_jobs (
                    id UUID PRIMARY KEY,
                    contribution_id UUID NOT NULL,
                    status media_processing_job_status
                        NOT NULL
                        DEFAULT 'PENDING'::media_processing_job_status,
                    attempts INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMPTZ
                        NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,
                    started_at TIMESTAMPTZ,
                    completed_at TIMESTAMPTZ,
                    error TEXT,
                    CONSTRAINT chk_media_processing_jobs_attempts
                        CHECK (attempts >= 0)
                )
                """
        );
    }

    @BeforeEach
    void cleanQueue() {
        jdbcTemplate.update(
                "DELETE FROM media_processing_jobs"
        );
    }

    @Test
    void shouldClaimProcessAndCompletePendingJob() {
        UUID jobId = insertPendingJob();

        AtomicReference<String> statusDuringProcessing =
                new AtomicReference<>();
        AtomicReference<Integer> attemptsDuringProcessing =
                new AtomicReference<>();

        MediaProcessingWorker worker =
                new MediaProcessingWorker(
                        queueRepository,
                        claimed -> {
                            Map<String, Object> row =
                                    jobRow(claimed.id());
                            statusDuringProcessing.set(
                                    (String) row.get("status")
                            );
                            attemptsDuringProcessing.set(
                                    (Integer) row.get("attempts")
                            );
                        }
                );

        Optional<ClaimedMediaProcessingJob> processed =
                worker.processNextAvailableJob();

        assertThat(processed).isPresent();
        assertThat(processed.get().id()).isEqualTo(jobId);
        assertThat(processed.get().attempts()).isEqualTo(1);
        assertThat(processed.get().startedAt()).isNotNull();

        assertThat(statusDuringProcessing.get())
                .isEqualTo("PROCESSING");
        assertThat(attemptsDuringProcessing.get())
                .isEqualTo(1);

        Map<String, Object> completedRow = jobRow(jobId);
        assertThat(completedRow.get("status"))
                .isEqualTo("COMPLETED");
        assertThat(completedRow.get("attempts"))
                .isEqualTo(1);
        assertThat(completedRow.get("started_at"))
                .isNotNull();
        assertThat(completedRow.get("completed_at"))
                .isNotNull();
        assertThat(completedRow.get("error"))
                .isNull();
    }

    @Test
    void shouldMarkJobFailedWhenProcessingThrowsException() {
        UUID jobId = insertPendingJob();

        MediaProcessingWorker failingWorker =
                new MediaProcessingWorker(
                        queueRepository,
                        claimed -> {
                            throw new IllegalStateException(
                                    "Simulated transcoding error"
                            );
                        }
                );

        Optional<ClaimedMediaProcessingJob> processed =
                failingWorker.processNextAvailableJob();

        assertThat(processed).isPresent();
        assertThat(processed.get().id()).isEqualTo(jobId);

        Map<String, Object> failedRow = jobRow(jobId);
        assertThat(failedRow.get("status"))
                .isEqualTo("FAILED");
        assertThat(failedRow.get("attempts"))
                .isEqualTo(1);
        assertThat(failedRow.get("started_at"))
                .isNotNull();
        assertThat(failedRow.get("completed_at"))
                .isNotNull();
        assertThat(failedRow.get("error"))
                .isEqualTo("Simulated transcoding error");
    }

    @Test
    void shouldReturnEmptyWhenNoPendingJobsExist() {
        assertThat(defaultWorker.processNextAvailableJob())
                .isEmpty();
    }

    @Test
    void shouldNotClaimSameJobAcrossTwoConcurrentWorkers()
            throws Exception {

        UUID jobId = insertPendingJob();

        MediaProcessingWorker workerOne =
                new MediaProcessingWorker(
                        queueRepository,
                        claimed -> {}
                );

        MediaProcessingWorker workerTwo =
                new MediaProcessingWorker(
                        queueRepository,
                        claimed -> {}
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Optional<ClaimedMediaProcessingJob>> callOne =
                () -> {
                    ready.countDown();
                    if (!start.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Timed out waiting for concurrent start"
                        );
                    }
                    return workerOne.processNextAvailableJob();
                };

        Callable<Optional<ClaimedMediaProcessingJob>> callTwo =
                () -> {
                    ready.countDown();
                    if (!start.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Timed out waiting for concurrent start"
                        );
                    }
                    return workerTwo.processNextAvailableJob();
                };

        try {
            Future<Optional<ClaimedMediaProcessingJob>> futureOne =
                    executor.submit(callOne);
            Future<Optional<ClaimedMediaProcessingJob>> futureTwo =
                    executor.submit(callTwo);

            assertThat(
                    ready.await(30, TimeUnit.SECONDS)
            ).isTrue();

            start.countDown();

            List<ClaimedMediaProcessingJob> claimedJobs =
                    List.of(
                                    futureOne.get(30, TimeUnit.SECONDS),
                                    futureTwo.get(30, TimeUnit.SECONDS)
                            )
                            .stream()
                            .flatMap(Optional::stream)
                            .toList();

            assertThat(claimedJobs).hasSize(1);
            assertThat(claimedJobs.get(0).id())
                    .isEqualTo(jobId);

            Map<String, Object> finalRow = jobRow(jobId);
            assertThat(finalRow.get("status"))
                    .isEqualTo("COMPLETED");
            assertThat(finalRow.get("attempts"))
                    .isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldDistributeTwoPendingJobsAcrossTwoConcurrentWorkersWithoutOverlap()
            throws Exception {

        UUID firstJobId = insertPendingJob();
        UUID secondJobId = insertPendingJob();

        CountDownLatch bothClaimed = new CountDownLatch(2);

        MediaProcessingTaskHandler barrierHandler =
                claimed -> {
                    bothClaimed.countDown();
                    if (!bothClaimed.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Second worker did not claim concurrently"
                        );
                    }
                };

        MediaProcessingWorker workerOne =
                new MediaProcessingWorker(
                        queueRepository,
                        barrierHandler
                );

        MediaProcessingWorker workerTwo =
                new MediaProcessingWorker(
                        queueRepository,
                        barrierHandler
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<Optional<ClaimedMediaProcessingJob>> futureOne =
                    executor.submit(
                            workerOne::processNextAvailableJob
                    );
            Future<Optional<ClaimedMediaProcessingJob>> futureTwo =
                    executor.submit(
                            workerTwo::processNextAvailableJob
                    );

            ClaimedMediaProcessingJob claimedOne =
                    futureOne.get(30, TimeUnit.SECONDS)
                            .orElseThrow();
            ClaimedMediaProcessingJob claimedTwo =
                    futureTwo.get(30, TimeUnit.SECONDS)
                            .orElseThrow();

            assertThat(claimedOne.id())
                    .isNotEqualTo(claimedTwo.id());
            assertThat(
                    List.of(claimedOne.id(), claimedTwo.id())
            ).containsExactlyInAnyOrder(
                    firstJobId,
                    secondJobId
            );

            assertThat(jobRow(firstJobId).get("status"))
                    .isEqualTo("COMPLETED");
            assertThat(jobRow(secondJobId).get("status"))
                    .isEqualTo("COMPLETED");
        } finally {
            executor.shutdownNow();
        }
    }

    private UUID insertPendingJob() {
        UUID jobId = UUID.randomUUID();
        UUID contributionId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO media_processing_jobs (
                    id,
                    contribution_id,
                    status,
                    attempts
                )
                VALUES (
                    ?,
                    ?,
                    'PENDING'::media_processing_job_status,
                    0
                )
                """,
                jobId,
                contributionId
        );

        return jobId;
    }

    private Map<String, Object> jobRow(UUID jobId) {
        return jdbcTemplate.queryForMap(
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