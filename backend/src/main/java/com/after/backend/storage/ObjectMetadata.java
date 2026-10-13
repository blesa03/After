package com.after.backend.storage;

public record ObjectMetadata(
        long sizeBytes,
        String contentType
) {
}