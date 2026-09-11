package com.after.backend.storage;

public interface ObjectStorageService {

    PresignedUpload createPresignedUpload(String contentType);

    PresignedDownload createPresignedDownload(String objectKey);

    boolean exists(String objectKey);

    void delete(String objectKey);
}