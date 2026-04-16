package com.mealguide.mealguide_api.mealcrawl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "menu")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "spicy_level", nullable = false)
    private Long spicyLevel;

    @Column(name = "ai_analysis_status", nullable = false, length = 20)
    private String aiAnalysisStatus;

    @Column(name = "latest_ai_analyzed_at")
    private LocalDateTime latestAiAnalyzedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Menu create(String name, String aiAnalysisStatus) {
        Menu menu = new Menu();
        menu.name = name;
        menu.spicyLevel = 0L;
        menu.aiAnalysisStatus = aiAnalysisStatus;
        menu.latestAiAnalyzedAt = null;
        menu.createdAt = LocalDateTime.now();
        return menu;
    }

    public void updateAiAnalysis(String aiAnalysisStatus, LocalDateTime analyzedAt) {
        this.aiAnalysisStatus = aiAnalysisStatus;
        this.latestAiAnalyzedAt = analyzedAt;
    }
}

