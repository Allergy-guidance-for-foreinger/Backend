package com.mealguide.mealguide_api.mealcrawl.application.port;

import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;

public interface MealUserPreferencePort {

    CurrentUserMealPreference getCurrentUserMealPreference(Long userId);
}
