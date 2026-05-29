package com.mealguide.mealguide_api.mealcrawl.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "menu_image_analysis_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuImageAnalysisLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "image_storage_path", length = 500)
    private String imageStoragePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MenuImageAnalysisStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_source", length = 40)
    private MenuImageAnalysisResultSource resultSource;

    @Column(name = "identified_food_name", length = 200)
    private String identifiedFoodName;

    @Column(name = "identified_food_korean_name", length = 200)
    private String identifiedFoodKoreanName;

    @Column(name = "identified_food_translation_name", length = 200)
    private String identifiedFoodTranslationName;

    @Column(name = "image_confidence", precision = 5, scale = 2)
    private BigDecimal imageConfidence;

    @Column(name = "image_reason", columnDefinition = "TEXT")
    private String imageReason;

    @Column(name = "fallback_result", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String fallbackResult;

    @Column(name = "error_code", length = 30)
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static MenuImageAnalysisLog createProcessing(Long userId) {
        return createProcessing(userId, LocalDateTime.now());
    }

    public static MenuImageAnalysisLog createProcessing(Long userId, LocalDateTime createdAt) {
        MenuImageAnalysisLog log = new MenuImageAnalysisLog();
        log.userId = userId;
        log.status = MenuImageAnalysisStatus.PROCESSING;
        log.createdAt = createdAt;
        return log;
    }

    public void updateImageStoragePath(String imageStoragePath) {
        this.imageStoragePath = imageStoragePath;
    }

    public void markFailed(String errorCode) {
        this.status = MenuImageAnalysisStatus.FAILED;
        this.errorCode = errorCode;
    }

    public void markSuccess(
            MenuImageAnalysisResultSource resultSource,
            String identifiedFoodName,
            String identifiedFoodKoreanName,
            String identifiedFoodTranslationName,
            BigDecimal imageConfidence,
            String imageReason,
            String fallbackResult
    ) {
        this.status = MenuImageAnalysisStatus.SUCCESS;
        this.resultSource = resultSource;
        this.identifiedFoodName = identifiedFoodName;
        this.identifiedFoodKoreanName = identifiedFoodKoreanName;
        this.identifiedFoodTranslationName = identifiedFoodTranslationName;
        this.imageConfidence = imageConfidence;
        this.imageReason = imageReason;
        this.fallbackResult = fallbackResult;
        this.errorCode = null;
    }
}
