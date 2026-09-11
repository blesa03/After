package com.after.backend.storage.s3;

import com.after.backend.storage.ObjectStorageException;
import com.after.backend.storage.ObjectStorageService;
import com.after.backend.storage.PresignedDownload;
import com.after.backend.storage.PresignedUpload;
import com.after.backend.storage.config.StorageProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class S3ObjectStorageService implements ObjectStorageService {

    private static final String OBJECT_PREFIX = "objects/";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    public S3ObjectStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            StorageProperties properties
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public PresignedUpload createPresignedUpload(
            String contentType
    ) {
        String normalizedContentType =
                requireNonBlank(contentType, "contentType");

        String objectKey =
                OBJECT_PREFIX + UUID.randomUUID();

        Instant expiresAt =
                Instant.now().plus(properties.presignTtl());

        PutObjectRequest objectRequest =
                PutObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(objectKey)
                        .contentType(normalizedContentType)
                        .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(properties.presignTtl())
                        .putObjectRequest(objectRequest)
                        .build();

        try {
            URI url = URI.create(
                    s3Presigner
                            .presignPutObject(presignRequest)
                            .url()
                            .toString()
            );

            return new PresignedUpload(
                    objectKey,
                    url,
                    expiresAt
            );
        } catch (RuntimeException exception) {
            throw new ObjectStorageException(
                    "Unable to generate presigned upload URL",
                    exception
            );
        }
    }

    @Override
    public PresignedDownload createPresignedDownload(
            String objectKey
    ) {
        String key = requireObjectKey(objectKey);

        Instant expiresAt =
                Instant.now().plus(properties.presignTtl());

        GetObjectRequest objectRequest =
                GetObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(key)
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(properties.presignTtl())
                        .getObjectRequest(objectRequest)
                        .build();

        try {
            URI url = URI.create(
                    s3Presigner
                            .presignGetObject(presignRequest)
                            .url()
                            .toString()
            );

            return new PresignedDownload(url, expiresAt);
        } catch (RuntimeException exception) {
            throw new ObjectStorageException(
                    "Unable to generate presigned download URL",
                    exception
            );
        }
    }

    @Override
    public boolean exists(String objectKey) {
        String key = requireObjectKey(objectKey);

        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(key)
                            .build()
            );

            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }

            throw new ObjectStorageException(
                    "Unable to check object existence",
                    exception
            );
        }
    }

    @Override
    public void delete(String objectKey) {
        String key = requireObjectKey(objectKey);

        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(key)
                            .build()
            );
        } catch (S3Exception exception) {
            throw new ObjectStorageException(
                    "Unable to delete object",
                    exception
            );
        }
    }

    private String requireObjectKey(String objectKey) {
        return requireNonBlank(objectKey, "objectKey");
    }

    private String requireNonBlank(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " cannot be null"
        );

        String normalized = value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank"
            );
        }

        return normalized;
    }
}