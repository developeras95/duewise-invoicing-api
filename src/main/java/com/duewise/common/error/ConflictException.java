package com.duewise.common.error;

import org.springframework.http.HttpStatus;

/**
 * The request clashes with the current state of a resource, e.g. a duplicate code or an
 * action the resource's status does not allow. Maps to 409.
 */
public class ConflictException extends DuewiseException {

    public ConflictException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
