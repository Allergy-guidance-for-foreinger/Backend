package com.mealguide.mealguide_api.mealcrawl.application.dto;

public record MenuDetailRow(
        Long mealMenuId,
        Long menuId,
        String menuName,
        String cornerName,
        Integer displayOrder,
        Long spicyLevel,
        String aiAnalysisStatus,
        Long schoolId
) {
}
