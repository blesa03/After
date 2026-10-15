package com.after.mediaworker.processing;

@FunctionalInterface
public interface MediaProcessingTaskHandler {

    void process(
            ClaimedMediaProcessingJob job
    ) throws Exception;
}