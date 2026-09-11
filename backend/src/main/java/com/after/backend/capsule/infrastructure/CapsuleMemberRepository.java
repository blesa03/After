package com.after.backend.capsule.infrastructure;

import com.after.backend.capsule.domain.CapsuleMember;
import com.after.backend.capsule.domain.CapsuleMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapsuleMemberRepository
        extends JpaRepository<CapsuleMember, UUID> {

    List<CapsuleMember> findAllByCapsuleId(UUID capsuleId);

    Optional<CapsuleMember> findByCapsuleIdAndRole(
            UUID capsuleId,
            CapsuleMemberRole role
    );

    boolean existsByCapsuleIdAndUserId(
            UUID capsuleId,
            UUID userId
    );
}