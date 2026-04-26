package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CafeteriaRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.port.CafeteriaQueryPort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.CafeteriaListResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CafeteriaQueryServiceTest {

    @Test
    void returnsCafeteriasByCurrentUserSchoolId() {
        MealUserPreferencePort mealUserPreferencePort = mock(MealUserPreferencePort.class);
        CafeteriaQueryPort cafeteriaQueryPort = mock(CafeteriaQueryPort.class);
        CafeteriaQueryService service = new CafeteriaQueryService(mealUserPreferencePort, cafeteriaQueryPort);

        when(mealUserPreferencePort.getCurrentUserMealPreference(1L))
                .thenReturn(new CurrentUserMealPreference(1L, 100L, "ko", null, List.of()));
        when(cafeteriaQueryPort.findCafeteriasBySchoolId(100L))
                .thenReturn(List.of(
                        new CafeteriaRow(10L, "학생식당"),
                        new CafeteriaRow(11L, "교직원식당")
                ));

        CafeteriaListResponse response = service.getCafeteriasForCurrentUser(1L);

        assertThat(response.schoolId()).isEqualTo(100L);
        assertThat(response.cafeterias()).hasSize(2);
        assertThat(response.cafeterias().get(0).cafeteriaId()).isEqualTo(10L);
        assertThat(response.cafeterias().get(0).name()).isEqualTo("학생식당");
        verify(cafeteriaQueryPort).findCafeteriasBySchoolId(100L);
    }

    @Test
    void returnsEmptyListWhenSchoolHasNoCafeterias() {
        MealUserPreferencePort mealUserPreferencePort = mock(MealUserPreferencePort.class);
        CafeteriaQueryPort cafeteriaQueryPort = mock(CafeteriaQueryPort.class);
        CafeteriaQueryService service = new CafeteriaQueryService(mealUserPreferencePort, cafeteriaQueryPort);

        when(mealUserPreferencePort.getCurrentUserMealPreference(1L))
                .thenReturn(new CurrentUserMealPreference(1L, 100L, "ko", null, List.of()));
        when(cafeteriaQueryPort.findCafeteriasBySchoolId(100L)).thenReturn(List.of());

        CafeteriaListResponse response = service.getCafeteriasForCurrentUser(1L);

        assertThat(response.schoolId()).isEqualTo(100L);
        assertThat(response.cafeterias()).isEmpty();
    }

    @Test
    void throwsExceptionWhenCurrentUserSchoolIdIsNull() {
        MealUserPreferencePort mealUserPreferencePort = mock(MealUserPreferencePort.class);
        CafeteriaQueryPort cafeteriaQueryPort = mock(CafeteriaQueryPort.class);
        CafeteriaQueryService service = new CafeteriaQueryService(mealUserPreferencePort, cafeteriaQueryPort);

        when(mealUserPreferencePort.getCurrentUserMealPreference(1L))
                .thenReturn(new CurrentUserMealPreference(1L, null, "ko", null, List.of()));

        assertThatThrownBy(() -> service.getCafeteriasForCurrentUser(1L))
                .isInstanceOf(ServiceException.class);
    }
}
