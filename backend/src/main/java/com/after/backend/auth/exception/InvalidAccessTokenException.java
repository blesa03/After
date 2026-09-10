package com.after.backend.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException() {
        super("Invalid access token");
    }
}