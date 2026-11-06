package com.after.mediaworker.processing.processors;

import com.after.mediaworker.processing.MediaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.nio.file.Path;

@Component
public class ImageProcessor implements MediaProcessor {
    private static final Logger log = LoggerFactory.getLogger(ImageProcessor.class);

    @Override
    public boolean supports(String type) {
        return "IMAGE".equalsIgnoreCase(type);
    }

    @Override
    public void process(MediaInfo info, Path localOriginalFile, Path workspace) {
        log.info("Executing image processing pipeline for {}...", info.originalFilename());
        // Lógica de conversión a WebP y generación de thumbnail en futuras tareas
    }
}