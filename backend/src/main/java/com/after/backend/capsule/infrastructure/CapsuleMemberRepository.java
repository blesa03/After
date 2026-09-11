package com.after.backend.capsule.infrastructure;

import com.after.backend.capsule.domain.CapsuleMember;
import com.after.backend.capsule.domain.CapsuleMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT membership
            FROM CapsuleMember membership
            JOIN FETCH membership.capsule capsule
            WHERE membership.user.id = :userId
            ORDER BY capsule.createdAt DESC
            """)
    List<CapsuleMember> findAllWithCapsuleByUserId(
            @Param("userId") UUID userId
    );

    @Query("""
            SELECT membership
            FROM CapsuleMember membership
            JOIN FETCH membership.capsule capsule
            WHERE capsule.id = :capsuleId
              AND membership.user.id = :userId
            """)
    Optional<CapsuleMember>
    findWithCapsuleByCapsuleIdAndUserId(
            @Param("capsuleId") UUID capsuleId,
            @Param("userId") UUID userId
    );
}