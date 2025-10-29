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

    @ExceptionHandler({ReifensbankRuntimeException.class, Exception.class})
    public ResponseEntity<Map<String, Object>> handleReifensbankException(
            ReifensbankRuntimeException ex,
            HttpServletRequest request
    ) {

        String errorCode = ErrorCodeGenerator.generateHexCode();
        LogService.logError(errorCode, request.getRequestURI(), ex);

        Map<String, Object> body = Map.of(
                "error", ex.getClass().getSimpleName(),
                "message", "Something went wrong :(. Please contact support with error code: " + errorCode,
                "status", ex.getStatus().value(),
                "path", request.getRequestURI(),
                "timestamp", OffsetDateTime.now().toString()
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
                "error", "InternalServerError",
                "message", "An unexpected error occurred. Please contact support with error code: " + errorCode,
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "path", request.getRequestURI(),
                "timestamp", OffsetDateTime.now().toString()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

}
