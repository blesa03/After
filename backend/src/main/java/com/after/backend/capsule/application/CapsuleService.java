package com.after.backend.capsule.application;

import com.after.backend.capsule.api.dto.CapsuleDetailResponse;
import com.after.backend.capsule.api.dto.CapsuleSummaryResponse;
import com.after.backend.capsule.api.dto.CreateCapsuleRequest;
import com.after.backend.capsule.api.dto.UpdateCapsuleRequest;
import com.after.backend.capsule.api.dto.CapsuleParticipantResponse;
import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleMember;
import com.after.backend.capsule.domain.CapsuleMemberRole;
import com.after.backend.capsule.domain.CapsuleStatus;
import com.after.backend.capsule.exception.CapsuleAccessDeniedException;
import com.after.backend.capsule.exception.CapsuleNotFoundException;
import com.after.backend.capsule.exception.InvalidCapsuleRequestException;
import com.after.backend.capsule.infrastructure.CapsuleMemberRepository;
import com.after.backend.capsule.infrastructure.CapsuleRepository;
import com.after.backend.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class CapsuleService {

    private final CapsuleRepository capsuleRepository;
    private final CapsuleMemberRepository capsuleMemberRepository;

    public CapsuleService(
            CapsuleRepository capsuleRepository,
            CapsuleMemberRepository capsuleMemberRepository
    ) {
        this.capsuleRepository = capsuleRepository;
        this.capsuleMemberRepository = capsuleMemberRepository;
    }

    @Transactional
    public CapsuleDetailResponse create(
            User owner,
            CreateCapsuleRequest request
    ) {
        validateOpeningTime(request.opensAt());
        validateTimezone(request.timezone());

        Capsule capsule = Capsule.create(
                request.title(),
                request.description(),
                request.type(),
                request.opensAt(),
                request.timezone(),
                owner
        );

        Capsule saved =
                capsuleRepository.saveAndFlush(capsule);

        return CapsuleDetailResponse.from(
                saved,
                CapsuleMemberRole.OWNER
        );
    }

    @Transactional(readOnly = true)
    public List<CapsuleSummaryResponse> listFor(
            User user
    ) {
        return capsuleMemberRepository
                .findAllWithCapsuleByUserId(user.getId())
                .stream()
                .map(CapsuleSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CapsuleDetailResponse getFor(
            User user,
            UUID capsuleId
    ) {
        CapsuleMember membership =
                capsuleMemberRepository
                        .findWithCapsuleByCapsuleIdAndUserId(
                                capsuleId,
                                user.getId()
                        )
                        .orElseThrow(
                                CapsuleNotFoundException::new
                        );

        return CapsuleDetailResponse.from(membership);
    }

    @Transactional
    public CapsuleDetailResponse update(
            User user,
            UUID capsuleId,
            UpdateCapsuleRequest request
    ) {
        CapsuleMember membership =
                capsuleMemberRepository
                        .findWithCapsuleByCapsuleIdAndUserId(
                                capsuleId,
                                user.getId()
                        )
                        .orElseThrow(
                                CapsuleNotFoundException::new
                        );

        if (membership.getRole() != CapsuleMemberRole.OWNER) {
            throw new CapsuleAccessDeniedException(
                    "Only the owner can edit the capsule"
            );
        }

        Capsule capsule = membership.getCapsule();

        if (capsule.getStatus() != CapsuleStatus.COLLECTING) {
            throw new InvalidCapsuleRequestException(
                    "Only COLLECTING capsules can be edited"
            );
        }

        validateOpeningTime(request.opensAt());
        validateTimezone(request.timezone());

        capsule.update(
                request.title(),
                request.description(),
                request.opensAt(),
                request.timezone()
        );

        capsuleRepository.saveAndFlush(capsule);

        return CapsuleDetailResponse.from(membership);
    }

    private void validateOpeningTime(Instant opensAt) {
        if (
                opensAt == null ||
                !opensAt.isAfter(Instant.now())
        ) {
            throw new InvalidCapsuleRequestException(
                    "opensAt must be in the future"
            );
        }
    }

    private void validateTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new InvalidCapsuleRequestException(
                    "timezone is required"
            );
        }

        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new InvalidCapsuleRequestException(
                    "timezone must be a valid zone ID"
            );
        }
    }
    @Transactional(readOnly = true)
        public List<CapsuleParticipantResponse> listParticipants(
                User user,
                UUID capsuleId
        ) {
        if (
                !capsuleMemberRepository.existsByCapsuleIdAndUserId(
                        capsuleId,
                        user.getId()
                )
        ) {
                throw new CapsuleNotFoundException();
        }

        return capsuleMemberRepository
                .findAllByCapsuleId(capsuleId)
                .stream()
                .map(CapsuleParticipantResponse::from)
                .toList();
    }
}