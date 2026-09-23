package com.after.backend.contribution.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidContributionException
        extends RuntimeException {

    public InvalidContributionException(
            String message
    ) {
        super(message);
    }
}