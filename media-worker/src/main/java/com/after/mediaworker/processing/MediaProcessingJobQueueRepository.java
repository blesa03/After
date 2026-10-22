package com.after.mediaworker.processing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MediaProcessingJobQueueRepository {

    private static final String CLAIM_SQL = """
            WITH next_job AS (
                SELECT id
                FROM media_processing_jobs
                WHERE status = 'PENDING'::media_processing_job_status
                  AND next_attempt_at <= CURRENT_TIMESTAMP
                ORDER BY next_attempt_at, created_at, id
                FOR UPDATE SKIP LOCKED
                LIMIT 1
            )
            UPDATE media_processing_jobs AS job
            SET
                status = 'PROCESSING'::media_processing_job_status,
                attempts = job.attempts + 1,
                started_at = CURRENT_TIMESTAMP,
                completed_at = NULL,
                error = NULL,
                claim_token = ?,
                lease_expires_at =
                    CURRENT_TIMESTAMP
                    + (?::bigint * INTERVAL '1 millisecond')
            FROM next_job
            WHERE job.id = next_job.id
            RETURNING
                job.id,
                job.contribution_id,
                job.attempts,
                job.started_at,
                job.claim_token
            """;

    private static final String COMPLETE_SQL = """
            UPDATE media_processing_jobs
            SET
                status = 'COMPLETED'::media_processing_job_status,
                completed_at = CURRENT_TIMESTAMP,
                error = NULL,
                claim_token = NULL,
                lease_expires_at = NULL
            WHERE id = ?
              AND claim_token = ?
              AND status = 'PROCESSING'::media_processing_job_status
              AND lease_expires_at > CURRENT_TIMESTAMP
            """;

    private static final String FINISH_FAILURE_SQL = """
            UPDATE media_processing_jobs
            SET
                status = ?::media_processing_job_status,
                completed_at =
                    CASE WHEN ? THEN NULL ELSE CURRENT_TIMESTAMP END,
                next_attempt_at =
                    CASE WHEN ?
                        THEN CURRENT_TIMESTAMP
                            + (?::bigint * INTERVAL '1 millisecond')
                        ELSE next_attempt_at
                    END,
                error = ?,
                claim_token = NULL,
                lease_expires_at = NULL
            WHERE id = ?
              AND claim_token = ?
              AND status = 'PROCESSING'::media_processing_job_status
              AND lease_expires_at > CURRENT_TIMESTAMP
            """;

    private static final String RENEW_SQL = """
            UPDATE media_processing_jobs
            SET lease_expires_at =
                CURRENT_TIMESTAMP
                + (?::bigint * INTERVAL '1 millisecond')
            WHERE id = ?
              AND claim_token = ?
              AND status = 'PROCESSING'::media_processing_job_status
              AND lease_expires_at > CURRENT_TIMESTAMP
            """;

    private static final String RECOVER_SQL = """
            WITH expired AS (
                SELECT id
                FROM media_processing_jobs
                WHERE status = 'PROCESSING'::media_processing_job_status
                  AND (
                      lease_expires_at <= CURRENT_TIMESTAMP
                      OR lease_expires_at IS NULL
                  )
                ORDER BY started_at, id
                FOR UPDATE SKIP LOCKED
                LIMIT ?
            )
            UPDATE media_processing_jobs AS job
            SET
                status =
                    CASE WHEN job.attempts >= ?
                        THEN 'FAILED'::media_processing_job_status
                        ELSE 'PENDING'::media_processing_job_status
                    END,
                completed_at =
                    CASE WHEN job.attempts >= ?
                        THEN CURRENT_TIMESTAMP
                        ELSE NULL
                    END,
                next_attempt_at =
                    CASE WHEN job.attempts >= ?
                        THEN job.next_attempt_at
                        ELSE CURRENT_TIMESTAMP
                            + (?::bigint * INTERVAL '1 millisecond')
                    END,
                error = 'Processing lease expired',
                claim_token = NULL,
                lease_expires_at = NULL
            FROM expired
            WHERE job.id = expired.id
            """;

    private final JdbcTemplate jdbcTemplate;

    public MediaProcessingJobQueueRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimedMediaProcessingJob> claimNextPending(
            Duration leaseDuration
    ) {
        Objects.requireNonNull(leaseDuration, "leaseDuration");

        UUID claimToken = UUID.randomUUID();

        return jdbcTemplate.query(
                CLAIM_SQL,
                (rs, rowNum) -> new ClaimedMediaProcessingJob(
                        rs.getObject("id", UUID.class),
                        rs.getObject("contribution_id", UUID.class),
                        rs.getInt("attempts"),
                        rs.getObject(
                                "started_at",
                                OffsetDateTime.class
                        ).toInstant(),
                        rs.getObject("claim_token", UUID.class)
                ),
                claimToken,
                leaseDuration.toMillis()
        ).stream().findFirst();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markCompleted(
            ClaimedMediaProcessingJob job
    ) {
        return jdbcTemplate.update(
                COMPLETE_SQL,
                job.id(),
                job.claimToken()
        ) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markFailed(
            ClaimedMediaProcessingJob job,
            String error,
            boolean retry,
            Duration retryDelay
    ) {
        String message =
                (error == null || error.isBlank())
                        ? "Media processing failed"
                        : error;

        if (message.length() > 2048) {
            message = message.substring(0, 2048);
        }

        return jdbcTemplate.update(
                FINISH_FAILURE_SQL,
                retry ? "PENDING" : "FAILED",
                retry,
                retry,
                retryDelay.toMillis(),
                message,
                job.id(),
                job.claimToken()
        ) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean renewLease(
            ClaimedMediaProcessingJob job,
            Duration duration
    ) {
        return jdbcTemplate.update(
                RENEW_SQL,
                duration.toMillis(),
                job.id(),
                job.claimToken()
        ) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recoverExpired(
            int maxAttempts,
            Duration retryDelay,
            int batchSize
    ) {
        return jdbcTemplate.update(
                RECOVER_SQL,
                batchSize,
                maxAttempts,
                maxAttempts,
                maxAttempts,
                retryDelay.toMillis()
        );
    }
}