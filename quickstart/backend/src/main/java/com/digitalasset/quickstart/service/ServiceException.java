package com.digitalasset.quickstart.service;

import org.springframework.http.HttpStatus;

import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.FormattingTuple;
import org.springframework.web.server.ResponseStatusException;

/**
 * Custom exception class for service layer errors.
 * Extends ResponseStatusException to include HTTP status codes.
 * Uses SLF4J's MessageFormatter for message formatting.
 */
 class ServiceException extends ResponseStatusException {

    ServiceException(HttpStatus status, String message, Object... args) {
        this(status, MessageFormatter.arrayFormat(message, args));
    }

    private ServiceException(HttpStatus status, FormattingTuple tuple) {
        super(status, tuple.getMessage(), tuple.getThrowable());
    }
}


