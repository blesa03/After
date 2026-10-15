package com.after.mediaworker.processing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(
        value = MediaProcessingTaskHandler.class,
        ignored = NoOpMediaProcessingTaskHandler.class
)
public class NoOpMediaProcessingTaskHandler
        implements MediaProcessingTaskHandler {

    @Override
    public void process(
            ClaimedMediaProcessingJob job
    ) {
        // Placeholder task handler for T21 queue consumption lifecycle.
    }
}