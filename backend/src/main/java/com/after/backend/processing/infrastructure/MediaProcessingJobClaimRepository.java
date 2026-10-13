package com.after.backend.processing.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MediaProcessingJobClaimRepository {

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

    private final JdbcTemplate jdbcTemplate;

    public MediaProcessingJobClaimRepository(
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
}