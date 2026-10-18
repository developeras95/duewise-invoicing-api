package com.duewise.common.error;

/** One invalid field in a request body, returned in the "errors" list of a 400 response. */
public record FieldViolation(String field, String message) {
}
