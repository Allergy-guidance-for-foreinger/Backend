package com.mealguide.mealguide_api.onboarding.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingUserAllergyId implements Serializable {

    private Long userId;
    private String allergyCode;

    public OnboardingUserAllergyId(Long userId, String allergyCode) {
        this.userId = userId;
        this.allergyCode = allergyCode;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof OnboardingUserAllergyId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(allergyCode, that.allergyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, allergyCode);
    }
}


