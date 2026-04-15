package com.mealguide.mealguide_api.onboarding.domain;

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
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingUser {

    @Id
    private Long id;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "religious_code", length = 30)
    private String religiousCode;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void completeOnboarding(String languageCode, Long schoolId, String religiousCode) {
        this.languageCode = languageCode;
        this.schoolId = schoolId;
        this.religiousCode = religiousCode;
        this.onboardingCompleted = true;
        this.updatedAt = LocalDateTime.now();
    }
}
