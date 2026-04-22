package com.bean.breaddiary.global.common;

import com.bean.breaddiary.global.logging.RequestLogContext;
import com.bean.breaddiary.global.ratelimit.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String DEFAULT_VALIDATION_MESSAGE = "요청 값이 올바르지 않습니다.";
    private static final String RATE_LIMITED_MESSAGE = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.";

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode = exception.getStatusCode();
        String code = mapStatusCode(statusCode);
        String message = resolveMessage(exception.getReason(), defaultMessage(statusCode));

        logByStatus(statusCode, code, message, request, exception);

        return ResponseEntity
                .status(statusCode)
                .body(ApiErrorResponse.failure(code, message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        return validationFailure(resolveFieldErrorMessage(exception), request, exception);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(
            BindException exception,
            HttpServletRequest request
    ) {
        return validationFailure(resolveFieldErrorMessage(exception), request, exception);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String message = exception.getConstraintViolations()
                .stream()
                .findFirst()
                .map(violation -> resolveMessage(violation.getMessage(), DEFAULT_VALIDATION_MESSAGE))
                .orElse(DEFAULT_VALIDATION_MESSAGE);

        return validationFailure(message, request, exception);
    }

    @ExceptionHandler(ValidationFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailureException(
            ValidationFailureException exception,
            HttpServletRequest request
    ) {
        return validationFailure(
                resolveMessage(exception.getMessage(), DEFAULT_VALIDATION_MESSAGE),
                request,
                exception
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Request failed: code={} method={} path={} requestId={} retryAfterSeconds={} exceptionType={}",
                "RATE_LIMITED",
                request.getMethod(),
                request.getRequestURI(),
                RequestLogContext.currentRequestIdOrDefault(),
                exception.getRetryAfterSeconds(),
                exception.getClass().getSimpleName()
        );

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()))
                .body(ApiErrorResponse.failure("RATE_LIMITED", RATE_LIMITED_MESSAGE));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequestException(
            Exception exception,
            HttpServletRequest request
    ) {
        String message = "요청 형식이 올바르지 않습니다.";

        log.warn(
                "Invalid request: code={} method={} path={} requestId={} errorType={}",
                "INVALID_REQUEST",
                request.getMethod(),
                request.getRequestURI(),
                RequestLogContext.currentRequestIdOrDefault(),
                exception.getClass().getSimpleName()
        );

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.failure("INVALID_REQUEST", message));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        String message = "?붿껌 ?뺤떇???щ컮瑜댁? ?딆뒿?덈떎.";

        log.warn(
                "Invalid request: code={} method={} path={} requestId={} errorType={}",
                "INVALID_REQUEST",
                request.getMethod(),
                request.getRequestURI(),
                RequestLogContext.currentRequestIdOrDefault(),
                exception.getClass().getSimpleName()
        );

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiErrorResponse.failure("INVALID_REQUEST", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected server error: code={} method={} path={} requestId={} exceptionType={}",
                "INTERNAL_ERROR",
                request.getMethod(),
                request.getRequestURI(),
                RequestLogContext.currentRequestIdOrDefault(),
                exception.getClass().getSimpleName()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.failure("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
    }

    private ResponseEntity<ApiErrorResponse> validationFailure(
            String message,
            HttpServletRequest request,
            Exception exception
    ) {
        log.warn(
                "Validation failed: code={} method={} path={} requestId={} errorType={}",
                "VALIDATION_FAILED",
                request.getMethod(),
                request.getRequestURI(),
                RequestLogContext.currentRequestIdOrDefault(),
                exception.getClass().getSimpleName()
        );

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.failure("VALIDATION_FAILED", message));
    }

    private String resolveFieldErrorMessage(BindException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();

        if (fieldError == null) {
            return DEFAULT_VALIDATION_MESSAGE;
        }

        return resolveMessage(fieldError.getDefaultMessage(), DEFAULT_VALIDATION_MESSAGE);
    }

    private String mapStatusCode(HttpStatusCode statusCode) {
        int status = statusCode.value();

        if (status == HttpStatus.BAD_REQUEST.value()) {
            return "INVALID_REQUEST";
        }

        if (status == HttpStatus.UNAUTHORIZED.value()) {
            return "UNAUTHORIZED";
        }

        if (status == HttpStatus.FORBIDDEN.value()) {
            return "FORBIDDEN";
        }

        if (status == HttpStatus.NOT_FOUND.value()) {
            return "NOT_FOUND";
        }

        if (status == HttpStatus.CONFLICT.value()) {
            return "CONFLICT";
        }

        if (status == HttpStatus.CONTENT_TOO_LARGE.value()) {
            return "FILE_TOO_LARGE";
        }

        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return "RATE_LIMITED";
        }

        if (status == HttpStatus.BAD_GATEWAY.value()) {
            return "TOSS_SERVER_ERROR";
        }

        return statusCode.is5xxServerError()
                ? "INTERNAL_ERROR"
                : "INVALID_REQUEST";
    }

    private String defaultMessage(HttpStatusCode statusCode) {
        int status = statusCode.value();

        if (status == HttpStatus.UNAUTHORIZED.value()) {
            return "인증이 필요합니다.";
        }

        if (status == HttpStatus.FORBIDDEN.value()) {
            return "권한이 없습니다.";
        }

        if (status == HttpStatus.NOT_FOUND.value()) {
            return "요청한 리소스를 찾을 수 없습니다.";
        }

        if (status == HttpStatus.CONFLICT.value()) {
            return "이미 처리된 요청입니다.";
        }

        if (statusCode.is5xxServerError()) {
            return "서버 내부 오류가 발생했습니다.";
        }

        return "요청 값이 올바르지 않습니다.";
    }

    private String resolveMessage(String message, String defaultMessage) {
        return StringUtils.hasText(message) ? message : defaultMessage;
    }

    private void logByStatus(
            HttpStatusCode statusCode,
            String code,
            String message,
            HttpServletRequest request,
            ResponseStatusException exception
    ) {
        if (statusCode.is5xxServerError()) {
            log.error(
                    "Request failed: code={} method={} path={} requestId={} message={} exceptionType={}",
                    code,
                    request.getMethod(),
                    request.getRequestURI(),
                    RequestLogContext.currentRequestIdOrDefault(),
                    message,
                    exception.getClass().getSimpleName()
            );
            return;
        }

        log.warn(
                "Request failed: code={} method={} path={} requestId={} message={} exceptionType={}",
                code,
                request.getMethod(),
                request.getRequestURI(),
                RequestLogContext.currentRequestIdOrDefault(),
                message,
                exception.getClass().getSimpleName()
        );
    }
}
