package com.after.mediaworker.processing.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class WorkspaceManager {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceManager.class);
    private static final String WORKSPACE_PREFIX = "media-worker-";

    /**
     * Crea un directorio temporal único para procesar un job.
     */
    public Path createWorkspace(UUID jobId) throws IOException {
        Path workspace = Files.createTempDirectory(WORKSPACE_PREFIX + jobId.toString());
        log.debug("Created workspace for job {}: {}", jobId, workspace.toAbsolutePath());
        return workspace;
    }

    /**
     * Elimina el directorio temporal y todo su contenido.
     */
    public void cleanupWorkspace(Path workspace) {
        if (workspace == null || !Files.exists(workspace)) {
            return;
        }
        
        try (Stream<Path> pathStream = Files.walk(workspace)) {
            pathStream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete temporary file: {}", path.toAbsolutePath(), e);
                        }
                    });
            log.debug("Cleaned up workspace: {}", workspace.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to walk workspace directory for cleanup: {}", workspace.toAbsolutePath(), e);
        }
    }
}