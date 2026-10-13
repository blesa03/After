package com.after.backend.contribution.infrastructure;

import com.after.backend.contribution.domain.Contribution;
import com.after.backend.contribution.domain.ContributionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContributionRepository
        extends JpaRepository<Contribution, UUID> {

    List<Contribution>
    findAllByCapsuleIdAndAuthorIdAndTypeOrderByCreatedAtAsc(
            UUID capsuleId,
            UUID authorId,
            ContributionType type
    );

    Optional<Contribution> findByIdAndCapsuleId(
            UUID id,
            UUID capsuleId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT contribution
            FROM Contribution contribution
            WHERE contribution.id = :id
              AND contribution.capsule.id = :capsuleId
            """)
    Optional<Contribution> findByIdAndCapsuleIdForUpdate(
            @Param("id") UUID id,
            @Param("capsuleId") UUID capsuleId
    );
}