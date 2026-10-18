package com.duewise.common.error;

import org.springframework.http.HttpStatus;

/** The requested resource does not exist. Maps to 404. */
public class NotFoundException extends DuewiseException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
