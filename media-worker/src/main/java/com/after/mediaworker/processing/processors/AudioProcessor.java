package com.after.mediaworker.processing.processors;

import com.after.mediaworker.processing.MediaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.nio.file.Path;

@Component
public class AudioProcessor implements MediaProcessor {
    private static final Logger log = LoggerFactory.getLogger(AudioProcessor.class);

    @Override
    public boolean supports(String type) {
        return "AUDIO".equalsIgnoreCase(type);
    }

    @Override
    public void process(MediaInfo info, Path localOriginalFile, Path workspace) {
        log.info("Executing audio processing pipeline for {}...", info.originalFilename());
        // Lógica de compresión de audio y waveform en futuras tareas
    }
}