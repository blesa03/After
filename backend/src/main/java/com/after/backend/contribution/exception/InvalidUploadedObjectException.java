package com.after.backend.contribution.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("deprecation")
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidUploadedObjectException
        extends RuntimeException {

    public InvalidUploadedObjectException(String message) {
        super(message);
    }
}