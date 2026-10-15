package com.after.mediaworker.processing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
                WHERE status =
                    'PENDING'::media_processing_job_status
                ORDER BY created_at ASC, id ASC
                FOR UPDATE SKIP LOCKED
                LIMIT 1
            )
            UPDATE media_processing_jobs AS job
            SET
                status =
                    'PROCESSING'::media_processing_job_status,
                attempts = job.attempts + 1,
                started_at = CURRENT_TIMESTAMP,
                completed_at = NULL,
                error = NULL
            FROM next_job
            WHERE job.id = next_job.id
            RETURNING
                job.id,
                job.contribution_id,
                job.attempts,
                job.started_at
            """;

    private static final String COMPLETE_SQL = """
            UPDATE media_processing_jobs
            SET
                status =
                    'COMPLETED'::media_processing_job_status,
                completed_at = CURRENT_TIMESTAMP,
                error = NULL
            WHERE id = ?
              AND status =
                    'PROCESSING'::media_processing_job_status
            """;

    private static final String FAIL_SQL = """
            UPDATE media_processing_jobs
            SET
                status =
                    'FAILED'::media_processing_job_status,
                completed_at = CURRENT_TIMESTAMP,
                error = ?
            WHERE id = ?
              AND status =
                    'PROCESSING'::media_processing_job_status
            """;

    private final JdbcTemplate jdbcTemplate;

    public MediaProcessingJobQueueRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public Optional<ClaimedMediaProcessingJob>
    claimNextPending() {

        return jdbcTemplate.query(
                CLAIM_SQL,
                (rs, rowNum) ->
                        new ClaimedMediaProcessingJob(
                                rs.getObject(
                                        "id",
                                        UUID.class
                                ),
                                rs.getObject(
                                        "contribution_id",
                                        UUID.class
                                ),
                                rs.getInt("attempts"),
                                rs.getObject(
                                        "started_at",
                                        OffsetDateTime.class
                                ).toInstant()
                        )
        ).stream().findFirst();
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public boolean markCompleted(
            UUID jobId
    ) {
        Objects.requireNonNull(
                jobId,
                "jobId cannot be null"
        );

        return jdbcTemplate.update(
                COMPLETE_SQL,
                jobId
        ) == 1;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public boolean markFailed(
            UUID jobId,
            String error
    ) {
        Objects.requireNonNull(
                jobId,
                "jobId cannot be null"
        );

        String normalizedError =
                (error == null || error.isBlank())
                        ? "Media processing failed"
                        : error;

        return jdbcTemplate.update(
                FAIL_SQL,
                normalizedError,
                jobId
        ) == 1;
    }
}