package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PythonMenuAnalysisStatus {
    SUCCESS,
    FAILED;

    @JsonCreator
    public static PythonMenuAnalysisStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "SUCCESS", "SUCCEEDED", "COMPLETED", "DONE", "OK" -> SUCCESS;
            case "FAILED", "FAIL", "ERROR" -> FAILED;
            default -> null;
        };
    }
}
