package com.mealguide.mealguide_api.settings.domain;

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
@Table(name = "religious_food_restriction_translation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReligiousFoodRestrictionTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "religious_food_restriction_code", nullable = false, length = 30)
    private String religiousFoodRestrictionCode;

    @Column(name = "lang_code", nullable = false, length = 10)
    private String langCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_auto_translated", nullable = false)
    private boolean autoTranslated;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

