package com.after.mediaworker.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class MediaProcessingWorker {

    private static final Logger log =
            LoggerFactory.getLogger(MediaProcessingWorker.class);

    private final MediaProcessingJobQueueRepository queueRepository;
    private final Supplier<MediaProcessingTaskHandler> handlerSupplier;
    private final QueueProcessingPolicy policy;

    private final AtomicReference<ClaimedMediaProcessingJob> activeJob =
            new AtomicReference<>();

    private final ReentrantLock processLock = new ReentrantLock();

    @Autowired
    public MediaProcessingWorker(
            MediaProcessingJobQueueRepository queueRepository,
            ObjectProvider<MediaProcessingTaskHandler> handlers,
            QueueProcessingPolicy policy
    ) {
        this(
                queueRepository,
                (Supplier<MediaProcessingTaskHandler>) handlers::getIfAvailable,
                policy
        );
    }

    public MediaProcessingWorker(
            MediaProcessingJobQueueRepository queueRepository,
            MediaProcessingTaskHandler handler,
            QueueProcessingPolicy policy
    ) {
        this(
                queueRepository,
                (Supplier<MediaProcessingTaskHandler>)
                        () -> Objects.requireNonNull(handler),
                policy
        );
    }

    private MediaProcessingWorker(
            MediaProcessingJobQueueRepository queueRepository,
            Supplier<MediaProcessingTaskHandler> handlerSupplier,
            QueueProcessingPolicy policy
    ) {
        this.queueRepository = queueRepository;
        this.handlerSupplier = handlerSupplier;
        this.policy = policy;
    }

    public boolean isOperational() {
        return handlerSupplier.get() != null;
    }

    public Optional<ClaimedMediaProcessingJob> processNextAvailableJob() {

        MediaProcessingTaskHandler handler = handlerSupplier.get();

        if (handler == null || !processLock.tryLock()) {
            return Optional.empty();
        }

        try {
            Optional<ClaimedMediaProcessingJob> claimed =
                    queueRepository.claimNextPending(policy.leaseDuration());

            if (claimed.isEmpty()) {
                return Optional.empty();
            }

            ClaimedMediaProcessingJob job = claimed.get();
            activeJob.set(job);

            try {
                Exception processingFailure = null;

                try {
                    handler.process(job);
                } catch (Exception exception) {
                    processingFailure = exception;
                }

                if (processingFailure == null) {
                    if (!queueRepository.markCompleted(job)) {
                        throw new IllegalStateException(
                                "Job claim is no longer active: " + job.id()
                        );
                    }
                } else {
                    log.error(
                            "Media processing failed for job {}",
                            job.id(),
                            processingFailure
                    );

                    boolean retryable =
                            !(processingFailure
                                    instanceof NonRetryableMediaProcessingException);

                    boolean retry =
                            retryable && job.attempts() < policy.maxAttempts();

                    if (!queueRepository.markFailed(
                            job,
                            processingFailure.getMessage(),
                            retry,
                            policy.retryDelayForAttempt(job.attempts())
                    )) {
                        throw new IllegalStateException(
                                "Failed to finalize owned job: " + job.id(),
                                processingFailure
                        );
                    }
                }

                return claimed;
            } finally {
                activeJob.compareAndSet(job, null);
            }
        } finally {
            processLock.unlock();
        }
    }

    public void heartbeat() {
        ClaimedMediaProcessingJob job = activeJob.get();

        if (job == null) {
            return;
        }

        if (!queueRepository.renewLease(
                job,
                policy.leaseDuration()
        )) {
            log.warn(
                    "Lease no longer owned for media job {}",
                    job.id()
            );
        }
    }
}