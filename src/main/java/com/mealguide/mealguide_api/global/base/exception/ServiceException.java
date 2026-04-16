package com.mealguide.mealguide_api.global.base.exception;

import lombok.Getter;

import java.util.Objects;

@Getter
public class ServiceException extends RuntimeException{
    private final ErrorCode errorCode;

    public ServiceException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
        this.errorCode = errorCode;
    }

    public ServiceException(ErrorCode errorCode, Throwable cause) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage(), cause);
        this.errorCode = errorCode;
    }
}

