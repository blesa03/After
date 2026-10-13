package com.after.backend.contribution.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MediaCompletionConflictException
        extends RuntimeException {

    public MediaCompletionConflictException(String message) {
        super(message);
    }
}