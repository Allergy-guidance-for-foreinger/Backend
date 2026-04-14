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
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

    @Id
    private Long id;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "religious_code", length = 30)
    private String religiousCode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateLanguageCode(String languageCode) {
        this.languageCode = languageCode;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateReligiousCode(String religiousCode) {
        this.religiousCode = religiousCode;
        this.updatedAt = LocalDateTime.now();
    }
}
