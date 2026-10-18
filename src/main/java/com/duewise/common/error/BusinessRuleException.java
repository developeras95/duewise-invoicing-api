package com.duewise.common.error;

import org.springframework.http.HttpStatus;

/**
 * The request is well-formed but breaks a business rule, e.g. paying more than the balance
 * due or using an inactive currency. Maps to 422.
 */
public class BusinessRuleException extends DuewiseException {

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }

    // 422. Looked up by number because its constant was renamed from UNPROCESSABLE_ENTITY
    // to UNPROCESSABLE_CONTENT (RFC 9110) across Spring versions.
    private static final HttpStatus UNPROCESSABLE = HttpStatus.valueOf(422);

    @Override
    public HttpStatus getStatus() {
        return UNPROCESSABLE;
    }
}
