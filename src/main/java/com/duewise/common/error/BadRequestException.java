package com.duewise.common.error;

import org.springframework.http.HttpStatus;

/**
 * The request is invalid in a way Bean Validation annotations cannot express, e.g. a field that is
 * required only on update. Maps to 400.
 */
public class BadRequestException extends DuewiseException {

    public BadRequestException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
