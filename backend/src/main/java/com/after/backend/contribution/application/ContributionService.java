package com.after.backend.contribution.application;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleMember;
import com.after.backend.capsule.domain.CapsuleStatus;
import com.after.backend.capsule.infrastructure.CapsuleMemberRepository;
import com.after.backend.contribution.api.dto.ContributionResponse;
import com.after.backend.contribution.api.dto.CreateMediaContributionRequest;
import com.after.backend.contribution.api.dto.CreateTextContributionRequest;
import com.after.backend.contribution.api.dto.MediaUploadResponse;
import com.after.backend.contribution.api.dto.UpdateTextContributionRequest;
import com.after.backend.contribution.domain.Contribution;
import com.after.backend.contribution.domain.ContributionType;
import com.after.backend.contribution.domain.MediaObject;
import com.after.backend.contribution.domain.MediaObjectStatus;
import com.after.backend.contribution.exception.ContributionAccessDeniedException;
import com.after.backend.contribution.exception.ContributionNotFoundException;
import com.after.backend.contribution.exception.InvalidContributionException;
import com.after.backend.contribution.infrastructure.ContributionRepository;
import com.after.backend.storage.ObjectStorageService;
import com.after.backend.storage.PresignedUpload;
import com.after.backend.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ContributionService {

    private final ContributionRepository
            contributionRepository;

    private final CapsuleMemberRepository
            capsuleMemberRepository;

    private final ObjectStorageService
            objectStorageService;

    private final MediaUploadPolicy
            mediaUploadPolicy;

    public ContributionService(
            ContributionRepository contributionRepository,
            CapsuleMemberRepository capsuleMemberRepository,
            ObjectStorageService objectStorageService,
            MediaUploadPolicy mediaUploadPolicy
    ) {
        this.contributionRepository =
                contributionRepository;

        this.capsuleMemberRepository =
                capsuleMemberRepository;

        this.objectStorageService =
                objectStorageService;

        this.mediaUploadPolicy =
                mediaUploadPolicy;
    }

    @Transactional
    public MediaUploadResponse
    createMediaContribution(
            User user,
            UUID capsuleId,
            CreateMediaContributionRequest request
    ) {
        CapsuleMember membership =
                requireMembership(
                        user,
                        capsuleId
                );

        Capsule capsule =
                membership.getCapsule();

        requireCollecting(
                capsule
        );

        String mimeType =
                mediaUploadPolicy
                        .validateAndNormalize(
                                request.type(),
                                request.mimeType(),
                                request.sizeBytes()
                        );

        PresignedUpload upload =
                objectStorageService
                        .createPresignedUpload(
                                mimeType
                        );

        Contribution contribution =
                Contribution.media(
                        capsule,
                        user,
                        request.type()
                );

        MediaObject mediaObject =
                new MediaObject(
                        UUID.randomUUID(),
                        contribution
                );

        mediaObject.setOriginalObjectKey(
                upload.objectKey()
        );

        mediaObject.setOriginalFilename(
                request.originalFilename()
        );

        mediaObject.setOriginalMimeType(
                mimeType
        );

        mediaObject.setSizeBytes(
                request.sizeBytes()
        );

        mediaObject.setStatus(
                MediaObjectStatus.UPLOADING
        );

        Contribution saved =
                contributionRepository
                        .saveAndFlush(
                                contribution
                        );

        return new MediaUploadResponse(
                saved.getId(),
                upload.url().toString(),
                "PUT",
                upload.expiresAt(),
                upload.headers()
        );
    }

    @Transactional
    public ContributionResponse createText(
            User user,
            UUID capsuleId,
            CreateTextContributionRequest request
    ) {
        CapsuleMember membership =
                requireMembership(
                        user,
                        capsuleId
                );

        Capsule capsule =
                membership.getCapsule();

        requireCollecting(capsule);

        Contribution contribution;

        try {
            contribution =
                    Contribution.text(
                            capsule,
                            user,
                            request.textContent()
                    );
        } catch (
                IllegalArgumentException |
                NullPointerException exception
        ) {
            throw new InvalidContributionException(
                    exception.getMessage()
            );
        }

        Contribution saved =
                contributionRepository
                        .saveAndFlush(
                                contribution
                        );

        return ContributionResponse.from(
                saved
        );
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse>
    listOwnText(
            User user,
            UUID capsuleId
    ) {
        requireMembership(
                user,
                capsuleId
        );

        return contributionRepository
                .findAllByCapsuleIdAndAuthorIdAndTypeOrderByCreatedAtAsc(
                        capsuleId,
                        user.getId(),
                        ContributionType.TEXT
                )
                .stream()
                .map(
                        ContributionResponse::from
                )
                .toList();
    }

    @Transactional
    public ContributionResponse updateText(
            User user,
            UUID capsuleId,
            UUID contributionId,
            UpdateTextContributionRequest request
    ) {
        CapsuleMember membership =
                requireMembership(
                        user,
                        capsuleId
                );

        requireCollecting(
                membership.getCapsule()
        );

        Contribution contribution =
                contributionRepository
                        .findByIdAndCapsuleId(
                                contributionId,
                                capsuleId
                        )
                        .orElseThrow(
                                ContributionNotFoundException::new
                        );

        requireAuthor(
                user,
                contribution
        );

        try {
            contribution.updateText(
                    request.textContent()
            );
        } catch (
                IllegalArgumentException |
                IllegalStateException |
                NullPointerException exception
        ) {
            throw new InvalidContributionException(
                    exception.getMessage()
            );
        }

        Contribution saved =
                contributionRepository
                        .saveAndFlush(
                                contribution
                        );

        return ContributionResponse.from(
                saved
        );
    }

    @Transactional
    public void delete(
            User user,
            UUID capsuleId,
            UUID contributionId
    ) {
        CapsuleMember membership =
                requireMembership(
                        user,
                        capsuleId
                );

        requireCollecting(
                membership.getCapsule()
        );

        Contribution contribution =
                contributionRepository
                        .findByIdAndCapsuleId(
                                contributionId,
                                capsuleId
                        )
                        .orElseThrow(
                                ContributionNotFoundException::new
                        );

        requireAuthor(
                user,
                contribution
        );

        contributionRepository.delete(
                contribution
        );
    }

    private CapsuleMember requireMembership(
            User user,
            UUID capsuleId
    ) {
        return capsuleMemberRepository
                .findWithCapsuleByCapsuleIdAndUserId(
                        capsuleId,
                        user.getId()
                )
                .orElseThrow(
                        ContributionNotFoundException::new
                );
    }

    private void requireCollecting(
            Capsule capsule
    ) {
        if (
                capsule.getStatus()
                        != CapsuleStatus.COLLECTING
        ) {
            throw new InvalidContributionException(
                    "Contributions can only be modified while the capsule is COLLECTING"
            );
        }
    }

    private void requireAuthor(
            User user,
            Contribution contribution
    ) {
        if (
                !contribution
                        .getAuthor()
                        .getId()
                        .equals(
                                user.getId()
                        )
        ) {
            throw new ContributionAccessDeniedException(
                    "Only the author can modify this contribution"
            );
        }
    }
}