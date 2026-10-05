package com.after.backend.contribution;

import com.after.backend.contribution.application.MediaUploadPolicy;
import com.after.backend.contribution.domain.ContributionType;
import com.after.backend.contribution.exception.InvalidContributionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaUploadPolicyTest {

    private static final long MAX_IMAGE_SIZE =
            10_485_760L;

    private static final long MAX_AUDIO_SIZE =
            52_428_800L;

    private static final long MAX_VIDEO_SIZE =
            524_288_000L;

    private final MediaUploadPolicy policy =
            new MediaUploadPolicy(
                    MAX_IMAGE_SIZE,
                    MAX_AUDIO_SIZE,
                    MAX_VIDEO_SIZE
            );

    @ParameterizedTest
    @CsvSource({
            "IMAGE,image/jpeg",
            "IMAGE,image/png",
            "IMAGE,image/webp",
            "AUDIO,audio/mpeg",
            "AUDIO,audio/wav",
            "AUDIO,audio/x-wav",
            "AUDIO,audio/ogg",
            "AUDIO,audio/mp4",
            "AUDIO,audio/webm",
            "VIDEO,video/mp4",
            "VIDEO,video/webm",
            "VIDEO,video/quicktime"
    })
    void shouldAcceptSupportedMimeTypes(
            ContributionType type,
            String mimeType
    ) {
        assertThat(
                policy.validateAndNormalize(
                        type,
                        mimeType,
                        1L
                )
        ).isEqualTo(
                mimeType
        );
    }

    @Test
    void shouldNormalizeMimeType() {
        assertThat(
                policy.validateAndNormalize(
                        ContributionType.IMAGE,
                        " IMAGE/JPEG ",
                        1L
                )
        ).isEqualTo(
                "image/jpeg"
        );
    }

    @ParameterizedTest
    @CsvSource({
            "IMAGE,image/gif",
            "AUDIO,audio/flac",
            "VIDEO,video/x-matroska"
    })
    void shouldRejectUnsupportedMimeTypes(
            ContributionType type,
            String mimeType
    ) {
        assertThatThrownBy(
                () ->
                        policy.validateAndNormalize(
                                type,
                                mimeType,
                                1L
                        )
        )
                .isInstanceOf(
                        InvalidContributionException.class
                );
    }

    @Test
    void shouldRejectMimeTypeFromAnotherCategory() {
        assertThatThrownBy(
                () ->
                        policy.validateAndNormalize(
                                ContributionType.IMAGE,
                                "video/mp4",
                                1L
                        )
        )
                .isInstanceOf(
                        InvalidContributionException.class
                );
    }

    @Test
    void shouldRejectTextContributionType() {
        assertThatThrownBy(
                () ->
                        policy.validateAndNormalize(
                                ContributionType.TEXT,
                                "text/plain",
                                1L
                        )
        )
                .isInstanceOf(
                        InvalidContributionException.class
                );
    }

    @ParameterizedTest
    @CsvSource({
            "IMAGE,image/jpeg,10485760",
            "AUDIO,audio/mpeg,52428800",
            "VIDEO,video/mp4,524288000"
    })
    void shouldAcceptConfiguredMaximumSize(
            ContributionType type,
            String mimeType,
            long sizeBytes
    ) {
        assertThat(
                policy.validateAndNormalize(
                        type,
                        mimeType,
                        sizeBytes
                )
        ).isEqualTo(
                mimeType
        );
    }

    @ParameterizedTest
    @CsvSource({
            "IMAGE,image/jpeg,10485761",
            "AUDIO,audio/mpeg,52428801",
            "VIDEO,video/mp4,524288001"
    })
    void shouldRejectFilesAboveConfiguredMaximum(
            ContributionType type,
            String mimeType,
            long sizeBytes
    ) {
        assertThatThrownBy(
                () ->
                        policy.validateAndNormalize(
                                type,
                                mimeType,
                                sizeBytes
                        )
        )
                .isInstanceOf(
                        InvalidContributionException.class
                );
    }

    @Test
    void shouldRejectNonPositiveSize() {
        assertThatThrownBy(
                () ->
                        policy.validateAndNormalize(
                                ContributionType.IMAGE,
                                "image/jpeg",
                                0L
                        )
        )
                .isInstanceOf(
                        InvalidContributionException.class
                );
    }
}