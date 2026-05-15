package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PythonMenuAnalysisStatus {
    SUCCESS,
    RETRYABLE_FAILED,
    PERMANENT_FAILED;

    @JsonCreator
    public static PythonMenuAnalysisStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "SUCCESS", "SUCCEEDED", "COMPLETED", "DONE", "OK" -> SUCCESS;
            case "RETRYABLE_FAILED", "TRANSIENT_FAILED", "TEMPORARY_FAILED" -> RETRYABLE_FAILED;
            case "PERMANENT_FAILED", "INVALID_ARGUMENT", "BAD_REQUEST" -> PERMANENT_FAILED;
            case "FAILED", "FAIL", "ERROR" -> RETRYABLE_FAILED;
            default -> null;
        };
    }
}
