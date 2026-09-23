package com.after.backend.contribution.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ContributionNotFoundException
        extends RuntimeException {

    public ContributionNotFoundException() {
        super("Contribution not found");
    }
}