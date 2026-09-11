package com.example.pruebatecnica.infrastructure.web;

import com.example.pruebatecnica.domain.AppException;
import java.net.URI;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messages;

    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(AppException.class)
    ResponseEntity<Object> handleApplication(AppException exception) {
        HttpStatus status = switch (exception.code()) {
            case SHOW_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UPSTREAM_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
            case UPSTREAM_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case DATABASE_UNAVAILABLE, UPSTREAM_RATE_LIMITED -> HttpStatus.SERVICE_UNAVAILABLE;
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
        };
        String key = switch (exception.code()) {
            case SHOW_NOT_FOUND -> "error.show.notFound";
            case UPSTREAM_UNAVAILABLE -> "error.upstream";
            case UPSTREAM_TIMEOUT -> "error.upstream.timeout";
            case DATABASE_UNAVAILABLE -> "error.database";
            case UPSTREAM_RATE_LIMITED -> "error.upstream.rateLimit";
            case INVALID_REQUEST -> "error.invalid";
        };
        log.warn("event=request.failed code={} status={}", exception.code(), status.value());
        var response = ResponseEntity.status(status);
        if (exception.code() == AppException.Code.UPSTREAM_RATE_LIMITED) {
            response.header(HttpHeaders.RETRY_AFTER, "10");
        }
        return response.body(problem(status, key, exception.code().name()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception exception) {
        log.error("event=request.unexpected_failure cause={}", exception.getClass().getName(), exception);
        return ResponseEntity.internalServerError()
                .body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal", "INTERNAL_ERROR"));
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return new ResponseEntity<>(problem(status, "error.http." + status.value(),
                "HTTP_" + status.value()), headers, status);
    }

    private ProblemDetail problem(HttpStatusCode status, String key, String code) {
        var locale = LocaleContextHolder.getLocale();
        String fallback = messages.getMessage("error.http.generic", null, locale);
        var detail = ProblemDetail.forStatusAndDetail(status, messages.getMessage(key, null, fallback, locale));
        detail.setTitle(messages.getMessage("title.error", null, locale));
        detail.setType(URI.create("urn:prueba-tecnica:error:" + code.toLowerCase(java.util.Locale.ROOT)));
        detail.setProperty("code", code);
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("requestId", MDC.get("requestId"));
        return detail;
    }
}
