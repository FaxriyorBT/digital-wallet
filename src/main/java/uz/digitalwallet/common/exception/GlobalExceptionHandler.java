package uz.digitalwallet.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every error into an RFC 9457 "problem detail" response with an extra {@code code} field,
 * so clients can react to {@code INSUFFICIENT_FUNDS} instead of parsing messages.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
        return problem(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        ErrorCode code = switch (ex) {
            case BadCredentialsException _ -> ErrorCode.INVALID_CREDENTIALS;
            case LockedException _ -> ErrorCode.ACCOUNT_BLOCKED;
            case InvalidBearerTokenException _ -> ErrorCode.INVALID_ACCESS_TOKEN;
            default -> ErrorCode.AUTHENTICATION_REQUIRED;
        };
        ResponseEntity<ProblemDetail> response = problem(code, code.getMessage());
        if (code.getStatus() != HttpStatus.UNAUTHORIZED) {
            return response;
        }
        return ResponseEntity.status(response.getStatusCode())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response.getBody());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return problem(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.info("Optimistic lock conflict: {}", ex.getMessage());
        return problem(ErrorCode.CONCURRENT_UPDATE, ErrorCode.CONCURRENT_UPDATE.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(ErrorCode.DATA_CONFLICT, ErrorCode.DATA_CONFLICT.getMessage());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ProblemDetail> handleUnknownProperty(PropertyReferenceException ex) {
        return problem(ErrorCode.INVALID_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return handleExceptionInternal(ex, validationProblem(ex.getBody(), errors), headers, status, request);
    }

    /**
     * Constraints on @RequestParam / @RequestHeader / @PathVariable arguments, e.g. a wallet number that fails the Luhn check.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> result.getResolvableErrors().forEach(error ->
                errors.putIfAbsent(result.getMethodParameter().getParameterName(), error.getDefaultMessage())));
        return handleExceptionInternal(ex, validationProblem(ex.getBody(), errors), headers, status, request);
    }

    /**
     * Errors raised by Spring MVC itself (unreadable JSON, missing header, unknown URL...) get a code too,
     * so clients can always rely on the field being present.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey("code"))) {
            String code = statusCode.value() == HttpStatus.BAD_REQUEST.value()
                    ? ErrorCode.INVALID_REQUEST.name()
                    : HttpStatus.valueOf(statusCode.value()).name();
            problem.setProperty("code", code);
        }
        return response;
    }

    private static ProblemDetail validationProblem(ProblemDetail body, Map<String, String> errors) {
        body.setDetail(ErrorCode.VALIDATION_FAILED.getMessage());
        body.setProperty("code", ErrorCode.VALIDATION_FAILED.name());
        body.setProperty("errors", errors);
        return body;
    }

    private static ResponseEntity<ProblemDetail> problem(ErrorCode code, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(code.getStatus(), detail);
        body.setTitle(code.getStatus().getReasonPhrase());
        body.setProperty("code", code.name());
        return ResponseEntity.status(code.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
