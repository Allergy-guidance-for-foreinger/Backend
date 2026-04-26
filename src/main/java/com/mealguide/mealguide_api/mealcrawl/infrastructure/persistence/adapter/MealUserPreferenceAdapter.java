package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.login.application.port.UserQueryPort;
import com.mealguide.mealguide_api.login.domain.User;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.settings.application.port.UserPreferencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MealUserPreferenceAdapter implements MealUserPreferencePort {

    private final UserQueryPort userQueryPort;
    private final UserPreferencePort userPreferencePort;

    @Override
    public CurrentUserMealPreference getCurrentUserMealPreference(Long userId) {
        User user = userQueryPort.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        List<String> allergyCodes = userPreferencePort.findAllergyCodesByUserId(userId);
        return new CurrentUserMealPreference(
                user.getId(),
                user.getSchoolId(),
                user.getLanguageCode(),
                user.getReligiousCode(),
                allergyCodes
        );
    }
}
