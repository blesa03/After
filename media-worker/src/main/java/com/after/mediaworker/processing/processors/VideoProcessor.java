package com.after.mediaworker.processing.processors;

import com.after.mediaworker.processing.MediaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.nio.file.Path;

@Component
public class VideoProcessor implements MediaProcessor {
    private static final Logger log = LoggerFactory.getLogger(VideoProcessor.class);

    @Override
    public boolean supports(String type) {
        return "VIDEO".equalsIgnoreCase(type);
    }

    @Override
    public void process(MediaInfo info, Path localOriginalFile, Path workspace) {
        log.info("Executing video processing pipeline for {}...", info.originalFilename());
        // Lógica de transcodificación a H.264/WebM y thumbnail en futuras tareas
    }
}