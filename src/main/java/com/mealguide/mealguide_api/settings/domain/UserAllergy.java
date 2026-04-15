package com.mealguide.mealguide_api.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_allergy")
@IdClass(UserAllergyId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAllergy {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "allergy_code", nullable = false, length = 30)
    private String allergyCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static UserAllergy create(Long userId, String allergyCode) {
        UserAllergy userAllergy = new UserAllergy();
        userAllergy.userId = userId;
        userAllergy.allergyCode = allergyCode;
        userAllergy.createdAt = LocalDateTime.now();
        return userAllergy;
    }
}
