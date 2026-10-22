-- Job ownership, bounded retries and crash recovery.
-- Deploy after stopping pre-lease worker instances.

ALTER TABLE media_processing_jobs
    ADD COLUMN claim_token UUID,
    ADD COLUMN lease_expires_at TIMESTAMPTZ,
    ADD COLUMN next_attempt_at TIMESTAMPTZ
        NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Give jobs already PROCESSING during the upgrade a grace period.

UPDATE media_processing_jobs
SET lease_expires_at =
    CURRENT_TIMESTAMP + INTERVAL '60 seconds'
WHERE status = 'PROCESSING'::media_processing_job_status
  AND lease_expires_at IS NULL;

CREATE INDEX idx_media_processing_jobs_due
    ON media_processing_jobs(
        next_attempt_at,
        created_at,
        id
    )
    WHERE status = 'PENDING'::media_processing_job_status;

CREATE INDEX idx_media_processing_jobs_expired
    ON media_processing_jobs(lease_expires_at)
    WHERE status = 'PROCESSING'::media_processing_job_status;