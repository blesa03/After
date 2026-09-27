ALTER TABLE media_objects
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE media_objects
    ALTER COLUMN status TYPE media_object_status
    USING status::media_object_status;

ALTER TABLE media_objects
    ALTER COLUMN status SET DEFAULT 'UPLOADING'::media_object_status;