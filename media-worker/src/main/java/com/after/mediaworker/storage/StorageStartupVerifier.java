package com.after.mediaworker.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Component
public class StorageStartupVerifier implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(StorageStartupVerifier.class);

    private final S3Client s3Client;
    private final StorageProperties properties;

    public StorageStartupVerifier(
            S3Client s3Client,
            StorageProperties properties
    ) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        s3Client.headBucket(
                HeadBucketRequest.builder()
                        .bucket(properties.bucket())
                        .build()
        );

        log.info(
                "Object storage connection verified. Bucket '{}' is available.",
                properties.bucket()
        );
    }
}