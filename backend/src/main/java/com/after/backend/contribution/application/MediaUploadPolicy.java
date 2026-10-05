package com.after.backend.contribution.application;

import com.after.backend.contribution.domain.ContributionType;
import com.after.backend.contribution.exception.InvalidContributionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class MediaUploadPolicy {

    private static final Set<String>
            IMAGE_MIME_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private static final Set<String>
            AUDIO_MIME_TYPES =
            Set.of(
                    "audio/mpeg",
                    "audio/wav",
                    "audio/x-wav",
                    "audio/ogg",
                    "audio/mp4",
                    "audio/webm"
            );

    private static final Set<String>
            VIDEO_MIME_TYPES =
            Set.of(
                    "video/mp4",
                    "video/webm",
                    "video/quicktime"
            );

    private final long maxImageSize;
    private final long maxAudioSize;
    private final long maxVideoSize;

    public MediaUploadPolicy(
            @Value(
                    "${after.media.max-image-size:10485760}"
            )
            long maxImageSize,
            @Value(
                    "${after.media.max-audio-size:52428800}"
            )
            long maxAudioSize,
            @Value(
                    "${after.media.max-video-size:524288000}"
            )
            long maxVideoSize
    ) {
        this.maxImageSize = maxImageSize;
        this.maxAudioSize = maxAudioSize;
        this.maxVideoSize = maxVideoSize;
    }

    public String validateAndNormalize(
            ContributionType type,
            String mimeType,
            long sizeBytes
    ) {
        if (
                type == null
                        || type == ContributionType.TEXT
        ) {
            throw new InvalidContributionException(
                    "Contribution type must be IMAGE, AUDIO or VIDEO"
            );
        }

        if (mimeType == null) {
            throw new InvalidContributionException(
                    "MIME type is required"
            );
        }

        String normalizedMimeType =
                mimeType
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (normalizedMimeType.isBlank()) {
            throw new InvalidContributionException(
                    "MIME type is required"
            );
        }

        if (
                !allowedMimeTypes(type)
                        .contains(
                                normalizedMimeType
                        )
        ) {
            throw new InvalidContributionException(
                    "MIME type is not supported for contribution type "
                            + type
            );
        }

        if (sizeBytes <= 0) {
            throw new InvalidContributionException(
                    "File size must be greater than zero"
            );
        }

        if (
                sizeBytes
                        > maxSizeBytes(type)
        ) {
            throw new InvalidContributionException(
                    "File exceeds the maximum allowed size for "
                            + type
            );
        }

        return normalizedMimeType;
    }

    private Set<String> allowedMimeTypes(
            ContributionType type
    ) {
        return switch (type) {
            case IMAGE ->
                    IMAGE_MIME_TYPES;

            case AUDIO ->
                    AUDIO_MIME_TYPES;

            case VIDEO ->
                    VIDEO_MIME_TYPES;

            case TEXT ->
                    Set.of();
        };
    }

    private long maxSizeBytes(
            ContributionType type
    ) {
        return switch (type) {
            case IMAGE ->
                    maxImageSize;

            case AUDIO ->
                    maxAudioSize;

            case VIDEO ->
                    maxVideoSize;

            case TEXT ->
                    0L;
        };
    }
}