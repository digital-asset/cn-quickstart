package com.digitalasset.quickstart.service;

import org.springframework.http.HttpStatus;

import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.FormattingTuple;
import org.springframework.web.server.ResponseStatusException;


class ServiceException extends ResponseStatusException {
    private final HttpStatus code;

    public ServiceException(HttpStatus code, String message, Object... args) {
        this(code, MessageFormatter.arrayFormat(message, args));
    }

    private ServiceException(HttpStatus code, FormattingTuple tuple) {
        super(code, tuple.getMessage(), tuple.getThrowable());
        this.code = code;
    }

    public HttpStatus getCode() {
        return code;
    }
}


