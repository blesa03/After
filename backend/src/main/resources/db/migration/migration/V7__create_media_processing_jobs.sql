CREATE TYPE media_processing_job_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED'
);

CREATE TABLE media_processing_jobs (
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

    CONSTRAINT fk_media_processing_jobs_contribution
        FOREIGN KEY (contribution_id)
        REFERENCES contributions(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_media_processing_jobs_attempts
        CHECK (attempts >= 0)
);

CREATE INDEX idx_media_processing_jobs_contribution
    ON media_processing_jobs(contribution_id);

CREATE INDEX idx_media_processing_jobs_pending
    ON media_processing_jobs(created_at, id)
    WHERE status = 'PENDING'::media_processing_job_status;