package com.mealguide.mealguide_api.global.base.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ExternalApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ExternalApiException(HttpStatus status, String code, String msg) {
        super(msg);
        this.status = status;
        this.code = code;
    }
}
