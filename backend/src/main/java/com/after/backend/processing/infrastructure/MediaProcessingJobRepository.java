package com.after.backend.processing.infrastructure;

import com.after.backend.processing.domain.MediaProcessingJob;
import com.after.backend.processing.domain.MediaProcessingJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaProcessingJobRepository
        extends JpaRepository<MediaProcessingJob, UUID> {

    List<MediaProcessingJob>
    findAllByStatusOrderByCreatedAtAsc(
            MediaProcessingJobStatus status
    );
}