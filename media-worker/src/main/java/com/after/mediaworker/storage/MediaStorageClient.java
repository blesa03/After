package com.after.mediaworker.storage;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.nio.file.Path;

@Service
public class MediaStorageClient {

    private final S3Client s3Client;
    private final StorageProperties properties;

    public MediaStorageClient(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    /**
     * Descarga un objeto desde S3 a una ruta local.
     */
    public void downloadToLocal(String objectKey, Path destinationPath) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        s3Client.getObject(getObjectRequest, destinationPath);
    }
}