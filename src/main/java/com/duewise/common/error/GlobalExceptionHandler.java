package com.duewise.common.error;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 7807 "problem detail" response, so clients always get the same
 * shape: type, title, status, detail, instance, plus a machine-readable "code" and, for
 * validation failures, an "errors" list with one entry per invalid field.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} keeps Spring's built-in handling of
 * framework errors (unsupported media type, missing parameter, wrong HTTP method, ...), which
 * already produces problem details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Errors raised on purpose by the application (404, 409, 422). */
    @ExceptionHandler(DuewiseException.class)
    ProblemDetail handleDuewiseException(DuewiseException ex) {
        return problem(ex.getStatus(), ex.getMessage(), ex.getCode());
    }

    /** Two requests changed the same record at the same time; the later one loses. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT,
                "The resource was changed by another request. Reload it and try again.",
                "CONCURRENT_MODIFICATION");
    }

    /**
     * Safety net for database constraints, e.g. two requests creating the same code at the same
     * moment. Services check these rules first, so this should be rare.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database constraint violated: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT,
                "The request conflicts with existing data.",
                "DATA_CONFLICT");
    }

    /** Anything unexpected. The details go to the log, never to the client. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side. Please try again later.",
                "INTERNAL_ERROR");
    }

    /** @Valid request body failed Bean Validation: list every invalid field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<FieldViolation> violations = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            violations.add(new FieldViolation(error.getField(), error.getDefaultMessage()));
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            violations.add(new FieldViolation(error.getObjectName(), error.getDefaultMessage()));
        }

        ProblemDetail body = ex.getBody();
        body.setTitle("Validation failed");
        body.setDetail("One or more fields are invalid.");
        body.setProperty("code", "VALIDATION_FAILED");
        body.setProperty("errors", violations);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * A constraint on a query or path parameter failed, e.g. {@code ?size=500} when the maximum is
     * 100. Reported in the same shape as body validation errors.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<FieldViolation> violations = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String parameter = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                violations.add(new FieldViolation(parameter, error.getDefaultMessage()));
            }
        }

        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "One or more request parameters are invalid.",
                "VALIDATION_FAILED");
        body.setTitle("Validation failed");
        body.setProperty("errors", violations);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** Body is not valid JSON, or a value has the wrong type (e.g. text where a number belongs). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ProblemDetail body = problem(HttpStatus.BAD_REQUEST,
                "The request body is missing or is not valid JSON for this endpoint.",
                "MALFORMED_REQUEST");
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatusCode status, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}
