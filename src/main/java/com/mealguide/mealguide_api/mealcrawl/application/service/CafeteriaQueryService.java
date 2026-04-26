package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CafeteriaRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.port.CafeteriaQueryPort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.CafeteriaItemResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.CafeteriaListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeteriaQueryService {

    private final MealUserPreferencePort mealUserPreferencePort;
    private final CafeteriaQueryPort cafeteriaQueryPort;

    public CafeteriaListResponse getCafeteriasForCurrentUser(Long userId) {
        CurrentUserMealPreference preference = mealUserPreferencePort.getCurrentUserMealPreference(userId);
        Long schoolId = requireSchoolId(preference);

        List<CafeteriaItemResponse> cafeterias = cafeteriaQueryPort.findCafeteriasBySchoolId(schoolId).stream()
                .map(this::toCafeteriaItemResponse)
                .toList();

        return new CafeteriaListResponse(schoolId, cafeterias);
    }

    private Long requireSchoolId(CurrentUserMealPreference preference) {
        if (preference == null) {
            throw new ServiceException(ErrorCode.USER_NOT_FOUND);
        }
        if (preference.schoolId() == null) {
            throw new ServiceException(ErrorCode.ESSENTIAL_FIELD_MISSING_ERROR);
        }
        return preference.schoolId();
    }

    private CafeteriaItemResponse toCafeteriaItemResponse(CafeteriaRow row) {
        return new CafeteriaItemResponse(row.cafeteriaId(), row.name());
    }
}
