package com.after.mediaworker.processing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "worker.polling",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MediaWorkerScheduler {

    private final MediaProcessingWorker worker;
    private final MediaProcessingJobQueueRepository repository;
    private final QueueProcessingPolicy policy;

    public MediaWorkerScheduler(
            MediaProcessingWorker worker,
            MediaProcessingJobQueueRepository repository,
            QueueProcessingPolicy policy,
            @Value("${worker.lease.heartbeat-interval-ms:10000}")
            long heartbeatIntervalMs
    ) {
        this.worker = worker;
        this.repository = repository;
        this.policy = policy;

        if (heartbeatIntervalMs < 1
                || heartbeatIntervalMs * 2 >= policy.leaseDurationMs()) {
            throw new IllegalArgumentException(
                    "Heartbeat interval must be less than half the lease duration"
            );
        }
    }

    @Scheduled(fixedDelayString = "${worker.polling.fixed-delay:1000}")
    public void poll() {
        if (!worker.isOperational()) {
            return;
        }

        repository.recoverExpired(
                policy.maxAttempts(),
                policy.retryDelayForAttempt(1),
                100
        );

        worker.processNextAvailableJob();
    }

    @Scheduled(
            fixedDelayString =
                    "${worker.lease.heartbeat-interval-ms:10000}"
    )
    public void heartbeat() {
        worker.heartbeat();
    }
}