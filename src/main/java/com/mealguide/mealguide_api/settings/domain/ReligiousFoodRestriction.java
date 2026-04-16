package com.mealguide.mealguide_api.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "religious_food_restriction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReligiousFoodRestriction {

    @Id
    @Column(length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

