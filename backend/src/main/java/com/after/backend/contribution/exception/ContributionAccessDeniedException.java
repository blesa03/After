package com.after.backend.contribution.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ContributionAccessDeniedException
        extends RuntimeException {

    public ContributionAccessDeniedException(
            String message
    ) {
        super(message);
    }
}