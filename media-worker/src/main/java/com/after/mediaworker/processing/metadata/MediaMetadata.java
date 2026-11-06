package com.after.mediaworker.processing.metadata;

public record MediaMetadata(
        Integer width,
        Integer height,
        Long durationMs
) {
}