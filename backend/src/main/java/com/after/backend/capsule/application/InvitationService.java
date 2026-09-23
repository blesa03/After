package com.after.backend.capsule.application;

import com.after.backend.capsule.api.dto.InvitationAcceptedResponse;
import com.after.backend.capsule.api.dto.InvitationCreatedResponse;
import com.after.backend.capsule.api.dto.InvitationResponse;
import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleMember;
import com.after.backend.capsule.domain.CapsuleMemberRole;
import com.after.backend.capsule.domain.CapsuleStatus;
import com.after.backend.capsule.domain.CapsuleType;
import com.after.backend.capsule.domain.Invitation;
import com.after.backend.capsule.exception.CapsuleAccessDeniedException;
import com.after.backend.capsule.exception.CapsuleNotFoundException;
import com.after.backend.capsule.exception.InvalidInvitationException;
import com.after.backend.capsule.exception.InvitationNotFoundException;
import com.after.backend.capsule.infrastructure.CapsuleMemberRepository;
import com.after.backend.capsule.infrastructure.CapsuleRepository;
import com.after.backend.capsule.infrastructure.InvitationRepository;
import com.after.backend.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class InvitationService {

    private final CapsuleRepository capsuleRepository;
    private final CapsuleMemberRepository capsuleMemberRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationTokenService tokenService;

    public InvitationService(
            CapsuleRepository capsuleRepository,
            CapsuleMemberRepository capsuleMemberRepository,
            InvitationRepository invitationRepository,
            InvitationTokenService tokenService
    ) {
        this.capsuleRepository = capsuleRepository;
        this.capsuleMemberRepository = capsuleMemberRepository;
        this.invitationRepository = invitationRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public InvitationCreatedResponse create(
            User user,
            UUID capsuleId
    ) {
        Capsule capsule = capsuleRepository
                .findById(capsuleId)
                .orElseThrow(CapsuleNotFoundException::new);

        CapsuleMember membership =
                capsuleMemberRepository
                        .findWithCapsuleByCapsuleIdAndUserId(
                                capsuleId,
                                user.getId()
                        )
                        .orElseThrow(CapsuleNotFoundException::new);

        if (membership.getRole() != CapsuleMemberRole.OWNER) {
            throw new CapsuleAccessDeniedException(
                    "Only the owner can create invitations"
            );
        }

        if (capsule.getType() != CapsuleType.SHARED) {
            throw new InvalidInvitationException(
                    "Invitations only apply to SHARED capsules"
            );
        }

        if (capsule.getStatus() != CapsuleStatus.COLLECTING) {
            throw new InvalidInvitationException(
                    "Invitations require a COLLECTING capsule"
            );
        }

        String token = tokenService.generateToken();
        String tokenHash = tokenService.hash(token);

        Invitation invitation = new Invitation(
                capsule,
                user,
                tokenHash
        );

        invitationRepository.saveAndFlush(invitation);

        return new InvitationCreatedResponse(token);
    }

    @Transactional(readOnly = true)
    public InvitationResponse get(String token) {
        Invitation invitation =
                findByRawToken(token);

        if (!invitation.isUsable()) {
            throw new InvalidInvitationException(
                    "Invitation is no longer valid"
            );
        }

        return InvitationResponse.from(invitation);
    }

    @Transactional
    public InvitationAcceptedResponse accept(
            User user,
            String token
    ) {
        Invitation invitation =
                findByRawToken(token);

        if (invitation.isAccepted()) {
            throw new InvalidInvitationException(
                    "Invitation has already been accepted"
            );
        }

        if (invitation.isRevoked()) {
            throw new InvalidInvitationException(
                    "Invitation has been revoked"
            );
        }

        Capsule capsule = invitation.getCapsule();

        if (capsule.getStatus() != CapsuleStatus.COLLECTING) {
            throw new InvalidInvitationException(
                    "Capsule is no longer collecting contributions"
            );
        }

        if (capsule.getType() != CapsuleType.SHARED) {
            throw new InvalidInvitationException(
                    "Invitation does not belong to a SHARED capsule"
            );
        }

        if (
                capsuleMemberRepository
                        .existsByCapsuleIdAndUserId(
                                capsule.getId(),
                                user.getId()
                        )
        ) {
            throw new InvalidInvitationException(
                    "User is already a member of this capsule"
            );
        }

        CapsuleMember membership =
                capsule.addContributor(user);

        capsuleMemberRepository.save(membership);

        invitation.accept(
                user,
                Instant.now()
        );

        invitationRepository.saveAndFlush(invitation);

        return new InvitationAcceptedResponse(
                capsule.getId(),
                CapsuleMemberRole.CONTRIBUTOR.name()
        );
    }

    private Invitation findByRawToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvitationNotFoundException();
        }

        String tokenHash = tokenService.hash(token);

        return invitationRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(InvitationNotFoundException::new);
    }
}