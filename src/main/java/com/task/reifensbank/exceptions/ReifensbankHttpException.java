package com.task.reifensbank.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ReifensbankHttpException extends RuntimeException {

    private final HttpStatus status;
    private final String message;

    public ReifensbankHttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.message = message;
    }
}
