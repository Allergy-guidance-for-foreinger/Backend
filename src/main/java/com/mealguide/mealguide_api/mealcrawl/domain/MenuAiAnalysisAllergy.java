package com.mealguide.mealguide_api.mealcrawl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "menu_ai_analysis_allergy")
@IdClass(MenuAiAnalysisAllergyId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuAiAnalysisAllergy {

    @Id
    @Column(name = "menu_ai_analysis_id", nullable = false)
    private Long menuAiAnalysisId;

    @Id
    @Column(name = "allergy_code", nullable = false, length = 30)
    private String allergyCode;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static MenuAiAnalysisAllergy create(
            Long menuAiAnalysisId,
            String allergyCode,
            BigDecimal confidence,
            String reason
    ) {
        MenuAiAnalysisAllergy allergy = new MenuAiAnalysisAllergy();
        allergy.menuAiAnalysisId = menuAiAnalysisId;
        allergy.allergyCode = allergyCode;
        allergy.confidence = confidence;
        allergy.reason = reason;
        allergy.createdAt = LocalDateTime.now();
        return allergy;
    }
}

