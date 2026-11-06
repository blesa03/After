package com.after.mediaworker.processing;

import com.after.mediaworker.processing.metadata.FfprobeMetadataExtractor;
import com.after.mediaworker.processing.metadata.MediaMetadata;
import com.after.mediaworker.processing.processors.MediaProcessor;
import com.after.mediaworker.processing.workspace.WorkspaceManager;
import com.after.mediaworker.storage.MediaStorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaProcessingPipelineTest {

    private MediaInfoRepository mediaInfoRepository;
    private WorkspaceManager workspaceManager;
    private MediaStorageClient storageClient;
    private FfprobeMetadataExtractor metadataExtractor;
    private MediaProcessor imageProcessor;
    private MediaProcessor videoProcessor;
    private MediaProcessingPipeline pipeline;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mediaInfoRepository = mock(MediaInfoRepository.class);
        workspaceManager = mock(WorkspaceManager.class);
        storageClient = mock(MediaStorageClient.class);
        metadataExtractor = mock(FfprobeMetadataExtractor.class);
        imageProcessor = mock(MediaProcessor.class);
        videoProcessor = mock(MediaProcessor.class);

        when(imageProcessor.supports("IMAGE")).thenReturn(true);
        when(videoProcessor.supports("VIDEO")).thenReturn(true);

        pipeline = new MediaProcessingPipeline(
                mediaInfoRepository,
                workspaceManager,
                storageClient,
                metadataExtractor,
                List.of(imageProcessor, videoProcessor)
        );
    }

    @Test
    void shouldExecuteCommonPipelineAndDelegateToMatchingProcessor() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID contributionId = UUID.randomUUID();
        ClaimedMediaProcessingJob job = new ClaimedMediaProcessingJob(
                jobId,
                contributionId,
                1,
                Instant.now(),
                UUID.randomUUID()
        );

        MediaInfo mediaInfo = new MediaInfo(
                contributionId,
                "IMAGE",
                "objects/raw-photo.jpg",
                "raw-photo.jpg"
        );

        Path workspace = tempDir.resolve("job-workspace");
        Path localFile = workspace.resolve("raw-photo.jpg");

        when(mediaInfoRepository.findByContributionId(contributionId))
                .thenReturn(Optional.of(mediaInfo));
        when(workspaceManager.createWorkspace(jobId)).thenReturn(workspace);
        when(metadataExtractor.extract(localFile))
                .thenReturn(new MediaMetadata(1920, 1080, null));

        pipeline.process(job);

        InOrder inOrder = inOrder(
                mediaInfoRepository,
                workspaceManager,
                storageClient,
                metadataExtractor,
                imageProcessor
        );

        inOrder.verify(mediaInfoRepository).findByContributionId(contributionId);
        inOrder.verify(workspaceManager).createWorkspace(jobId);
        inOrder.verify(storageClient).downloadToLocal("objects/raw-photo.jpg", localFile);
        inOrder.verify(metadataExtractor).extract(localFile);
        inOrder.verify(mediaInfoRepository).updateMetadata(contributionId, 1920, 1080, null);
        inOrder.verify(imageProcessor).process(mediaInfo, localFile, workspace);
        inOrder.verify(workspaceManager).cleanupWorkspace(workspace);

        verify(videoProcessor, never()).process(any(), any(), any());
    }

    @Test
    void shouldCleanupWorkspaceWhenProcessorFails() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID contributionId = UUID.randomUUID();
        ClaimedMediaProcessingJob job = new ClaimedMediaProcessingJob(
                jobId,
                contributionId,
                1,
                Instant.now(),
                UUID.randomUUID()
        );

        MediaInfo mediaInfo = new MediaInfo(
                contributionId,
                "IMAGE",
                "objects/photo.jpg",
                "photo.jpg"
        );

        Path workspace = tempDir.resolve("failing-workspace");
        Path localFile = workspace.resolve("photo.jpg");

        when(mediaInfoRepository.findByContributionId(contributionId))
                .thenReturn(Optional.of(mediaInfo));
        when(workspaceManager.createWorkspace(jobId)).thenReturn(workspace);
        when(metadataExtractor.extract(localFile))
                .thenReturn(new MediaMetadata(800, 600, null));

        doThrow(new RuntimeException("Transcoding failure"))
                .when(imageProcessor).process(mediaInfo, localFile, workspace);

        assertThatThrownBy(() -> pipeline.process(job))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Transcoding failure");

        verify(workspaceManager).cleanupWorkspace(workspace);
    }

    @Test
    void shouldThrowExceptionWhenUnsupportedMediaType() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID contributionId = UUID.randomUUID();
        ClaimedMediaProcessingJob job = new ClaimedMediaProcessingJob(
                jobId,
                contributionId,
                1,
                Instant.now(),
                UUID.randomUUID()
        );

        MediaInfo mediaInfo = new MediaInfo(
                contributionId,
                "AUDIO",
                "objects/track.mp3",
                "track.mp3"
        );

        Path workspace = tempDir.resolve("unsupported-workspace");

        when(mediaInfoRepository.findByContributionId(contributionId))
                .thenReturn(Optional.of(mediaInfo));
        when(workspaceManager.createWorkspace(jobId)).thenReturn(workspace);
        when(metadataExtractor.extract(any()))
                .thenReturn(new MediaMetadata(null, null, 120_000L));

        assertThatThrownBy(() -> pipeline.process(job))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported media type");

        verify(workspaceManager).cleanupWorkspace(workspace);
    }
}