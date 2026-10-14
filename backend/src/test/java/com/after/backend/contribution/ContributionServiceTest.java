package com.after.backend.contribution;

import com.after.backend.capsule.domain.Capsule;
import com.after.backend.capsule.domain.CapsuleMember;
import com.after.backend.capsule.domain.CapsuleStatus;
import com.after.backend.capsule.infrastructure.CapsuleMemberRepository;
import com.after.backend.contribution.application.ContributionService;
import com.after.backend.contribution.application.MediaUploadPolicy;
import com.after.backend.contribution.api.dto.MediaCompletionResponse;
import com.after.backend.contribution.domain.Contribution;
import com.after.backend.contribution.domain.MediaObject;
import com.after.backend.contribution.domain.MediaObjectStatus;
import com.after.backend.contribution.exception.InvalidUploadedObjectException;
import com.after.backend.contribution.exception.MediaCompletionConflictException;
import com.after.backend.contribution.infrastructure.ContributionRepository;
import com.after.backend.processing.infrastructure.MediaProcessingJobRepository;
import com.after.backend.storage.ObjectMetadata;
import com.after.backend.storage.ObjectStorageService;
import com.after.backend.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContributionServiceTest {

    @Mock
    private ContributionRepository contributionRepository;

    @Mock
    private CapsuleMemberRepository capsuleMemberRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private MediaUploadPolicy mediaUploadPolicy;

    @Mock
    private MediaProcessingJobRepository mediaProcessingJobRepository;

    @InjectMocks
    private ContributionService contributionService;

    private User author;
    private Capsule capsule;
    private CapsuleMember membership;
    private Contribution contribution;
    private MediaObject mediaObject;
    private UUID capsuleId;
    private UUID contributionId;

    @BeforeEach
    void setUp() {
        capsuleId = UUID.randomUUID();
        contributionId = UUID.randomUUID();

        author = mock(User.class);
        when(author.getId()).thenReturn(UUID.randomUUID());

        capsule = mock(Capsule.class);
        when(capsule.getStatus()).thenReturn(CapsuleStatus.COLLECTING);

        membership = mock(CapsuleMember.class);
        when(membership.getCapsule()).thenReturn(capsule);

        contribution = mock(Contribution.class);
        when(contribution.getId()).thenReturn(contributionId);
        when(contribution.getAuthor()).thenReturn(author);

        mediaObject = new MediaObject(UUID.randomUUID(), contribution);
        mediaObject.setOriginalObjectKey("objects/test.jpg");
        mediaObject.setOriginalMimeType("image/jpeg");
        mediaObject.setSizeBytes(1024L);
        mediaObject.setStatus(MediaObjectStatus.UPLOADING);

        when(contribution.getMediaObject()).thenReturn(mediaObject);
    }

    @Test
    void shouldCompleteMediaUploadSuccessfully() {
        when(capsuleMemberRepository.findWithCapsuleByCapsuleIdAndUserId(capsuleId, author.getId()))
                .thenReturn(Optional.of(membership));
        when(contributionRepository.findByIdAndCapsuleIdForUpdate(contributionId, capsuleId))
                .thenReturn(Optional.of(contribution));
        when(objectStorageService.getMetadata("objects/test.jpg"))
                .thenReturn(Optional.of(new ObjectMetadata(1024L, "image/jpeg")));

        MediaCompletionResponse response = contributionService.completeMediaUpload(author, capsuleId, contributionId);

        assertThat(response.contributionId()).isEqualTo(contributionId);
        assertThat(response.status()).isEqualTo(MediaObjectStatus.PROCESSING);
        assertThat(mediaObject.getStatus()).isEqualTo(MediaObjectStatus.PROCESSING);
        verify(mediaProcessingJobRepository, times(1)).saveAndFlush(any());
    }

    @Test
    void shouldRejectCompletionWhenAlreadyFinalized() {
        mediaObject.setStatus(MediaObjectStatus.PROCESSING);

        when(capsuleMemberRepository.findWithCapsuleByCapsuleIdAndUserId(capsuleId, author.getId()))
                .thenReturn(Optional.of(membership));
        when(contributionRepository.findByIdAndCapsuleIdForUpdate(contributionId, capsuleId))
                .thenReturn(Optional.of(contribution));

        assertThatThrownBy(() -> contributionService.completeMediaUpload(author, capsuleId, contributionId))
                .isInstanceOf(MediaCompletionConflictException.class)
                .hasMessage("Media upload has already been finalized");

        verify(mediaProcessingJobRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectWhenStoredObjectDoesNotExist() {
        when(capsuleMemberRepository.findWithCapsuleByCapsuleIdAndUserId(capsuleId, author.getId()))
                .thenReturn(Optional.of(membership));
        when(contributionRepository.findByIdAndCapsuleIdForUpdate(contributionId, capsuleId))
                .thenReturn(Optional.of(contribution));
        when(objectStorageService.getMetadata("objects/test.jpg"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> contributionService.completeMediaUpload(author, capsuleId, contributionId))
                .isInstanceOf(MediaCompletionConflictException.class)
                .hasMessage("Uploaded object does not exist");

        verify(mediaProcessingJobRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectWhenStoredSizeDoesNotMatch() {
        when(capsuleMemberRepository.findWithCapsuleByCapsuleIdAndUserId(capsuleId, author.getId()))
                .thenReturn(Optional.of(membership));
        when(contributionRepository.findByIdAndCapsuleIdForUpdate(contributionId, capsuleId))
                .thenReturn(Optional.of(contribution));
        when(objectStorageService.getMetadata("objects/test.jpg"))
                .thenReturn(Optional.of(new ObjectMetadata(500L, "image/jpeg")));

        assertThatThrownBy(() -> contributionService.completeMediaUpload(author, capsuleId, contributionId))
                .isInstanceOf(InvalidUploadedObjectException.class)
                .hasMessage("Uploaded object size does not match the declared size");

        verify(mediaProcessingJobRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectWhenStoredMimeTypeDoesNotMatch() {
        when(capsuleMemberRepository.findWithCapsuleByCapsuleIdAndUserId(capsuleId, author.getId()))
                .thenReturn(Optional.of(membership));
        when(contributionRepository.findByIdAndCapsuleIdForUpdate(contributionId, capsuleId))
                .thenReturn(Optional.of(contribution));
        when(objectStorageService.getMetadata("objects/test.jpg"))
                .thenReturn(Optional.of(new ObjectMetadata(1024L, "image/png")));

        assertThatThrownBy(() -> contributionService.completeMediaUpload(author, capsuleId, contributionId))
                .isInstanceOf(InvalidUploadedObjectException.class)
                .hasMessage("Uploaded object MIME type does not match the declared type");

        verify(mediaProcessingJobRepository, never()).saveAndFlush(any());
    }
}