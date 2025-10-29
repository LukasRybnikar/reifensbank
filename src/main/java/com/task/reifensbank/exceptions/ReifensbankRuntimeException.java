package com.task.reifensbank.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ReifensbankRuntimeException extends RuntimeException {

    private final HttpStatus status;
    private final String message;

    public ReifensbankRuntimeException() {
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.message = "Generic exception message. We are just soo sorry :(";
    }
}
