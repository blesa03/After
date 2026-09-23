package com.after.backend.contribution.infrastructure;

import com.after.backend.contribution.domain.Contribution;
import com.after.backend.contribution.domain.ContributionType;
import org.springframework.data.jpa.repository.JpaRepository;

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

    Optional<Contribution>
    findByIdAndCapsuleId(
            UUID id,
            UUID capsuleId
    );
}