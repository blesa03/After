package com.after.backend.capsule.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class CapsuleAccessDeniedException
        extends RuntimeException {

    public CapsuleAccessDeniedException(String message) {
        super(message);
    }
}