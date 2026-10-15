package com.after.mediaworker.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MediaProcessingWorker {

    private static final Logger log =
            LoggerFactory.getLogger(
                    MediaProcessingWorker.class
            );

    private final MediaProcessingJobQueueRepository
            queueRepository;

    private final MediaProcessingTaskHandler
            taskHandler;

    public MediaProcessingWorker(
            MediaProcessingJobQueueRepository queueRepository,
            MediaProcessingTaskHandler taskHandler
    ) {
        this.queueRepository = queueRepository;
        this.taskHandler = taskHandler;
    }

    public Optional<ClaimedMediaProcessingJob>
    processNextAvailableJob() {

        Optional<ClaimedMediaProcessingJob> claimedOpt =
                queueRepository.claimNextPending();

        if (claimedOpt.isEmpty()) {
            return Optional.empty();
        }

        ClaimedMediaProcessingJob claimed =
                claimedOpt.get();

        try {
            taskHandler.process(claimed);
            queueRepository.markCompleted(claimed.id());
        } catch (Exception exception) {
            log.error(
                    "Failed to process media job {} for contribution {}",
                    claimed.id(),
                    claimed.contributionId(),
                    exception
            );
            queueRepository.markFailed(
                    claimed.id(),
                    exception.getMessage()
            );
        }

        return Optional.of(claimed);
    }
}