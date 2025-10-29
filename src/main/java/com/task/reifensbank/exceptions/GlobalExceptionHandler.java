package com.task.reifensbank.exceptions;

import com.task.reifensbank.util.ErrorCodeGenerator;
import com.task.reifensbank.util.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String STATUS = "status";
    private static final String PATH = "path";
    private static final String TIMESTAMP = "timestamp";
    private static final String CODE = "code";

    @ExceptionHandler(ReifensbankHttpException.class)
    public ResponseEntity<Map<String, Object>> handleReifensbankHttpException(
            ReifensbankHttpException ex,
            HttpServletRequest request
    ) {
        String errorCode = ErrorCodeGenerator.generateHexCode();
        LogService.logError(errorCode, request.getRequestURI(), ex);

        Map<String, Object> body = Map.of(
                ERROR, ex.getClass().getSimpleName(),
                MESSAGE, ex.getMessage(),
                STATUS, ex.getStatus().value(),
                PATH, request.getRequestURI(),
                TIMESTAMP, OffsetDateTime.now().toString(),
                CODE, errorCode
        );

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler({ReifensbankRuntimeException.class, Exception.class})
    public ResponseEntity<Map<String, Object>> handleReifensbankException(
            ReifensbankRuntimeException ex,
            HttpServletRequest request
    ) {
        String errorCode = ErrorCodeGenerator.generateHexCode();
        LogService.logError(errorCode, request.getRequestURI(), ex);

        Map<String, Object> body = Map.of(
                ERROR, ex.getClass().getSimpleName(),
                MESSAGE, "Something went wrong :(. Please contact support with error code: " + errorCode,
                STATUS, ex.getStatus().value(),
                PATH, request.getRequestURI(),
                TIMESTAMP, OffsetDateTime.now().toString(),
                CODE, errorCode
        );

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleGenericRuntimeException(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        String errorCode = ErrorCodeGenerator.generateHexCode();
        LogService.logError(errorCode, request.getRequestURI(), ex);

        Map<String, Object> body = Map.of(
                ERROR, "InternalServerError",
                MESSAGE, "An unexpected error occurred. Please contact support with error code: " + errorCode,
                STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                PATH, request.getRequestURI(),
                TIMESTAMP, OffsetDateTime.now().toString(),
                CODE, errorCode
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
