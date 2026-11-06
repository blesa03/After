package com.after.mediaworker.processing;

import com.after.mediaworker.processing.metadata.FfprobeMetadataExtractor;
import com.after.mediaworker.processing.metadata.MediaMetadata;
import com.after.mediaworker.processing.processors.MediaProcessor;
import com.after.mediaworker.processing.workspace.WorkspaceManager;
import com.after.mediaworker.storage.MediaStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@ConditionalOnProperty(name = "worker.pipeline.enabled", havingValue = "true")
public class MediaProcessingPipeline implements MediaProcessingTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(MediaProcessingPipeline.class);

    private final MediaInfoRepository mediaInfoRepository;
    private final WorkspaceManager workspaceManager;
    private final MediaStorageClient storageClient;
    private final FfprobeMetadataExtractor metadataExtractor;
    private final List<MediaProcessor> processors;

    public MediaProcessingPipeline(
            MediaInfoRepository mediaInfoRepository,
            WorkspaceManager workspaceManager,
            MediaStorageClient storageClient,
            FfprobeMetadataExtractor metadataExtractor,
            List<MediaProcessor> processors
    ) {
        this.mediaInfoRepository = mediaInfoRepository;
        this.workspaceManager = workspaceManager;
        this.storageClient = storageClient;
        this.metadataExtractor = metadataExtractor;
        this.processors = processors;
    }

    @Override
    public void process(ClaimedMediaProcessingJob job) throws Exception {
        log.info("Starting common media pipeline for job: {}", job.id());

        // 1. Obtener Contribution y MediaObject
        MediaInfo info = mediaInfoRepository.findByContributionId(job.contributionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Media info not found for contribution: " + job.contributionId()
                ));

        Path workspace = null;
        try {
            // 2 & 3. Trabajar en directorio temporal
            workspace = workspaceManager.createWorkspace(job.id());
            Path localOriginalFile = workspace.resolve(info.originalFilename());

            // Descargar original_object_key
            storageClient.downloadToLocal(info.originalObjectKey(), localOriginalFile);

            // 4 & 5. Ejecutar ffprobe cuando corresponda y recopilar metadata
            MediaMetadata metadata = metadataExtractor.extract(localOriginalFile);
            mediaInfoRepository.updateMetadata(
                    info.contributionId(),
                    metadata.width(),
                    metadata.height(),
                    metadata.durationMs()
            );

            // 6. Delegar al procesador por tipo
            MediaProcessor processor = processors.stream()
                    .filter(p -> p.supports(info.type()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unsupported media type for processing: " + info.type()
                    ));

            processor.process(info, localOriginalFile, workspace);

        } finally {
            // 7. Limpiar ficheros temporales
            workspaceManager.cleanupWorkspace(workspace);
        }
    }
}