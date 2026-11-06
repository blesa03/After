package com.after.mediaworker.processing.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;

@Service
public class FfprobeMetadataExtractor {

    private static final Logger log =
            LoggerFactory.getLogger(FfprobeMetadataExtractor.class);

    private final JsonMapper jsonMapper;

    public FfprobeMetadataExtractor(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public MediaMetadata extract(Path filePath) throws Exception {
        log.debug(
                "Extracting metadata with ffprobe for: {}",
                filePath.getFileName()
        );

        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffprobe",
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                filePath.toAbsolutePath().toString()
        );

        Process process = processBuilder.start();
        JsonNode root = jsonMapper.readTree(process.getInputStream());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            log.warn(
                    "ffprobe exited with code {} for file {}",
                    exitCode,
                    filePath.getFileName()
            );
            return new MediaMetadata(null, null, null);
        }

        Integer width = null;
        Integer height = null;
        Long durationMs = null;

        if (root.has("streams") && root.get("streams").isArray()) {
            for (JsonNode stream : root.get("streams")) {
                if (stream.has("width") && stream.has("height")) {
                    width = stream.get("width").asInt();
                    height = stream.get("height").asInt();
                    break;
                }
            }
        }

        if (root.has("format") && root.get("format").has("duration")) {
            try {
                double durationSec = Double.parseDouble(
                        root.get("format").get("duration").asText()
                );
                durationMs = Math.round(durationSec * 1000.0);
            } catch (NumberFormatException ignored) {
                log.warn(
                        "Unable to parse duration from ffprobe format block"
                );
            }
        }

        return new MediaMetadata(width, height, durationMs);
    }
}