package com.bean.breaddiary.global.common;

public class ValidationFailureException extends RuntimeException {

    public ValidationFailureException(String message) {
        super(message);
    }
}
