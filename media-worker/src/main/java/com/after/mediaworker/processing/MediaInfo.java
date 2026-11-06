package com.after.mediaworker.processing;

import java.util.UUID;

public record MediaInfo(
        UUID contributionId,
        String type, // IMAGE, AUDIO, VIDEO
        String originalObjectKey,
        String originalFilename
) {
}