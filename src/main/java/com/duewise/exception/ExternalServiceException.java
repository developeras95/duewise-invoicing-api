package com.duewise.exception;

import org.springframework.http.HttpStatus;

/**
 * A service Duewise depends on, such as the mail server, refused or failed the request. The
 * client's request itself was fine and can be retried later. Maps to 502 Bad Gateway.
 */
public class ExternalServiceException extends DuewiseException {

    public ExternalServiceException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_GATEWAY;
    }
}
