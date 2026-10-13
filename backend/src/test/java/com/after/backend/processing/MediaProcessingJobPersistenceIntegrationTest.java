package com.after.backend.processing;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleType;
import com.after.backend.capsule.infrastructure.CapsuleMemberRepository;
import com.after.backend.capsule.infrastructure.CapsuleRepository;
import com.after.backend.contribution.domain.Contribution;
import com.after.backend.contribution.domain.ContributionType;
import com.after.backend.contribution.domain.MediaObject;
import com.after.backend.contribution.domain.MediaObjectStatus;
import com.after.backend.contribution.infrastructure.ContributionRepository;
import com.after.backend.processing.domain.MediaProcessingJob;
import com.after.backend.processing.domain.MediaProcessingJobStatus;
import com.after.backend.processing.infrastructure.ClaimedMediaProcessingJob;
import com.after.backend.processing.infrastructure.MediaProcessingJobClaimRepository;
import com.after.backend.processing.infrastructure.MediaProcessingJobRepository;
import com.after.backend.user.domain.User;
import com.after.backend.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class MediaProcessingJobPersistenceIntegrationTest {

    @SuppressWarnings("deprecation")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CapsuleRepository capsuleRepository;

    @Autowired
    private CapsuleMemberRepository capsuleMemberRepository;

    @Autowired
    private ContributionRepository contributionRepository;

    @Autowired
    private MediaProcessingJobRepository jobRepository;

    @Autowired
    private MediaProcessingJobClaimRepository claimRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanDatabase() {
        jobRepository.deleteAll();
        contributionRepository.deleteAll();
        capsuleMemberRepository.deleteAll();
        capsuleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldPersistPendingJobWithDefaultFields() {
        MediaProcessingJob created = createJob();
        UUID jobId = created.getId();

        entityManager.clear();

        MediaProcessingJob persisted =
                jobRepository.findById(jobId).orElseThrow();

        assertThat(persisted.getId()).isEqualTo(jobId);
        assertThat(persisted.getStatus())
                .isEqualTo(MediaProcessingJobStatus.PENDING);
        assertThat(persisted.getAttempts()).isZero();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getStartedAt()).isNull();
        assertThat(persisted.getCompletedAt()).isNull();
        assertThat(persisted.getError()).isNull();
    }

    @Test
    void shouldClaimPendingJobAndPersistProcessingState() {
        MediaProcessingJob job = createJob();

        ClaimedMediaProcessingJob claimed =
                claimRepository.claimNextPending().orElseThrow();

        assertThat(claimed.id()).isEqualTo(job.getId());
        assertThat(claimed.contributionId())
                .isEqualTo(job.getContribution().getId());
        assertThat(claimed.attempts()).isEqualTo(1);
        assertThat(claimed.startedAt()).isNotNull();

        entityManager.clear();

        MediaProcessingJob persisted =
                jobRepository.findById(job.getId()).orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(MediaProcessingJobStatus.PROCESSING);
        assertThat(persisted.getAttempts()).isEqualTo(1);
        assertThat(persisted.getStartedAt()).isNotNull();
    }

    @Test
    void shouldReturnEmptyWhenQueueHasNoPendingJobs() {
        assertThat(claimRepository.claimNextPending())
                .isEmpty();
    }

    @Test
    void shouldNotClaimSameJobTwice() {
        MediaProcessingJob job = createJob();

        Optional<ClaimedMediaProcessingJob> first =
                claimRepository.claimNextPending();

        Optional<ClaimedMediaProcessingJob> second =
                claimRepository.claimNextPending();

        assertThat(first).isPresent();
        assertThat(first.orElseThrow().id())
                .isEqualTo(job.getId());
        assertThat(second).isEmpty();

        entityManager.clear();

        assertThat(
                jobRepository.findById(job.getId())
                        .orElseThrow()
                        .getAttempts()
        ).isEqualTo(1);
    }

    @Test
    void shouldIgnoreNonPendingJobs() {
        for (String status : List.of(
                "PROCESSING",
                "COMPLETED",
                "FAILED"
        )) {
            MediaProcessingJob job = createJob();

            jdbcTemplate.update(
                    """
                    UPDATE media_processing_jobs
                    SET status =
                        CAST(? AS media_processing_job_status)
                    WHERE id = ?
                    """,
                    status,
                    job.getId()
            );
        }

        assertThat(claimRepository.claimNextPending())
                .isEmpty();
    }

    @Test
    void shouldSkipLockedJobAndClaimNextPending()
            throws Exception {

        MediaProcessingJob lockedJob = createJob();
        MediaProcessingJob availableJob = createJob();

        jdbcTemplate.update(
                """
                UPDATE media_processing_jobs
                SET created_at =
                    CURRENT_TIMESTAMP - INTERVAL '1 hour'
                WHERE id = ?
                """,
                lockedJob.getId()
        );

        try (Connection connection =
                     dataSource.getConnection()) {

            connection.setAutoCommit(false);

            try {
                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     SELECT id
                                     FROM media_processing_jobs
                                     WHERE id = ?
                                     FOR UPDATE
                                     """
                             )) {

                    statement.setObject(
                            1,
                            lockedJob.getId()
                    );

                    try (ResultSet rows =
                                 statement.executeQuery()) {
                        assertThat(rows.next()).isTrue();
                    }
                }

                ClaimedMediaProcessingJob claimed =
                        claimRepository
                                .claimNextPending()
                                .orElseThrow();

                assertThat(claimed.id())
                        .isEqualTo(availableJob.getId());

            } finally {
                connection.rollback();
            }
        }

        ClaimedMediaProcessingJob remaining =
                claimRepository.claimNextPending().orElseThrow();

        assertThat(remaining.id())
                .isEqualTo(lockedJob.getId());
    }

    @Test
    void shouldPreventConcurrentDoubleClaim()
            throws Exception {

        MediaProcessingJob job = createJob();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Optional<ClaimedMediaProcessingJob>> task =
                () -> {
                    ready.countDown();

                    if (!start.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Concurrent test start timed out"
                        );
                    }

                    return claimRepository.claimNextPending();
                };

        try {
            Future<Optional<ClaimedMediaProcessingJob>> first =
                    executor.submit(task);

            Future<Optional<ClaimedMediaProcessingJob>> second =
                    executor.submit(task);

            assertThat(
                    ready.await(30, TimeUnit.SECONDS)
            ).isTrue();

            start.countDown();

            List<ClaimedMediaProcessingJob> claimed =
                    List.of(
                            first.get(30, TimeUnit.SECONDS),
                            second.get(30, TimeUnit.SECONDS)
                    )
                            .stream()
                            .flatMap(Optional::stream)
                            .toList();

            assertThat(claimed).hasSize(1);
            assertThat(claimed.get(0).id())
                    .isEqualTo(job.getId());

            entityManager.clear();

            MediaProcessingJob persisted =
                    jobRepository.findById(job.getId())
                            .orElseThrow();

            assertThat(persisted.getStatus())
                    .isEqualTo(MediaProcessingJobStatus.PROCESSING);
            assertThat(persisted.getAttempts()).isEqualTo(1);

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRejectNegativeAttemptCount() {
        MediaProcessingJob job = createJob();

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        UPDATE media_processing_jobs
                        SET attempts = -1
                        WHERE id = ?
                        """,
                        job.getId()
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectJobWithoutExistingContribution() {
        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO media_processing_jobs (
                            id,
                            contribution_id
                        )
                        VALUES (?, ?)
                        """,
                        UUID.randomUUID(),
                        UUID.randomUUID()
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    private MediaProcessingJob createJob() {
        User owner = userRepository.saveAndFlush(
                new User(
                        "owner-" + UUID.randomUUID()
                                + "@example.com",
                        "test-password-hash"
                )
        );

        Capsule capsule = capsuleRepository.saveAndFlush(
                Capsule.create(
                        "Processing test capsule",
                        null,
                        CapsuleType.SHARED,
                        Instant.now().plusSeconds(3600),
                        "Europe/Madrid",
                        owner
                )
        );

        Contribution contribution = Contribution.media(
                capsule,
                owner,
                ContributionType.IMAGE
        );

        MediaObject mediaObject = new MediaObject(
                UUID.randomUUID(),
                contribution
        );

        mediaObject.setStatus(
                MediaObjectStatus.PROCESSING
        );

        mediaObject.setOriginalObjectKey(
                "capsules/test/" + UUID.randomUUID() + ".jpg"
        );

        Contribution savedContribution =
                contributionRepository.saveAndFlush(
                        contribution
                );

        return jobRepository.saveAndFlush(
                new MediaProcessingJob(savedContribution)
        );
    }
}