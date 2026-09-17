package com.orderflow.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidStateTransitionException extends IllegalStateException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
