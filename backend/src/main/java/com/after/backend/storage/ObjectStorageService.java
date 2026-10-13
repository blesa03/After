package com.after.backend.storage;

import java.util.Optional;

public interface ObjectStorageService {

    PresignedUpload createPresignedUpload(String contentType);

    PresignedDownload createPresignedDownload(String objectKey);

    boolean exists(String objectKey);

    Optional<ObjectMetadata> getMetadata(String objectKey);

    void delete(String objectKey);
}