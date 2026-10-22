package com.after.mediaworker.processing;

public class NonRetryableMediaProcessingException
        extends RuntimeException {

    public NonRetryableMediaProcessingException(String message) {
        super(message);
    }

    public NonRetryableMediaProcessingException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}