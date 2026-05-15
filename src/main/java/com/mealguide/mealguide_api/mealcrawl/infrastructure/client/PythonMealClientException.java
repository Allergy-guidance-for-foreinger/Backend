package com.mealguide.mealguide_api.mealcrawl.infrastructure.client;

public class PythonMealClientException extends RuntimeException {

    private final Integer httpStatus;
    private final String responseBody;
    private final boolean retryable;

    public PythonMealClientException(String message, Integer httpStatus, String responseBody, boolean retryable, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.retryable = retryable;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
