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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "meal_schedule_crawl_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealScheduleCrawlHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cafeteria_id", nullable = false)
    private Long cafeteriaId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static MealScheduleCrawlHistory start(Long cafeteriaId, LocalDate startDate, LocalDate endDate, LocalDateTime startedAt) {
        MealScheduleCrawlHistory history = new MealScheduleCrawlHistory();
        history.cafeteriaId = cafeteriaId;
        history.startDate = startDate;
        history.endDate = endDate;
        history.status = "STARTED";
        history.failureMessage = null;
        history.startedAt = startedAt;
        history.finishedAt = null;
        history.createdAt = LocalDateTime.now();
        return history;
    }

    public void markSuccess(LocalDateTime finishedAt) {
        this.status = "SUCCESS";
        this.failureMessage = null;
        this.finishedAt = finishedAt;
    }

    public void markFailed(String failureMessage, LocalDateTime finishedAt) {
        this.status = "FAILED";
        this.failureMessage = failureMessage;
        this.finishedAt = finishedAt;
    }
}

