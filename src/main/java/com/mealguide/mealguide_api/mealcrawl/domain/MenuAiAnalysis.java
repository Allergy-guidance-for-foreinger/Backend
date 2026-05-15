package com.mealguide.mealguide_api.mealcrawl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "menu_ai_analysis")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuAiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MenuAiStatus status;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static MenuAiAnalysis create(
            Long menuId,
            MenuAiStatus status,
            String modelName,
            String modelVersion,
            String reason,
            LocalDateTime analyzedAt,
            int attemptCount
    ) {
        MenuAiAnalysis analysis = new MenuAiAnalysis();
        analysis.menuId = menuId;
        analysis.status = status;
        analysis.modelName = modelName;
        analysis.modelVersion = modelVersion;
        analysis.reason = reason;
        analysis.analyzedAt = analyzedAt;
        analysis.attemptCount = attemptCount;
        analysis.createdAt = LocalDateTime.now();
        return analysis;
    }
}

