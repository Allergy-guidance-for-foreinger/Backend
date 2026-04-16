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
@Table(name = "meal_schedule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cafeteria_id", nullable = false)
    private Long cafeteriaId;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static MealSchedule create(Long cafeteriaId, LocalDate mealDate, String mealType) {
        MealSchedule mealSchedule = new MealSchedule();
        mealSchedule.cafeteriaId = cafeteriaId;
        mealSchedule.mealDate = mealDate;
        mealSchedule.mealType = mealType;
        mealSchedule.createdAt = LocalDateTime.now();
        return mealSchedule;
    }
}

