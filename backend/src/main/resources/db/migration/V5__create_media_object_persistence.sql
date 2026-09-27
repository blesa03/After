CREATE TYPE media_object_status AS ENUM ('UPLOADING', 'PROCESSING', 'READY', 'FAILED');

CREATE TABLE media_objects (
    id UUID PRIMARY KEY,
    contribution_id UUID NOT NULL UNIQUE REFERENCES contributions(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADING',
    original_object_key VARCHAR(1024),
    processed_object_key VARCHAR(1024),
    thumbnail_object_key VARCHAR(1024),
    original_filename VARCHAR(255),
    original_mime_type VARCHAR(100),
    processed_mime_type VARCHAR(100),
    size_bytes BIGINT,
    duration_ms BIGINT,
    width INTEGER,
    height INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_media_objects_contribution_id ON media_objects(contribution_id);