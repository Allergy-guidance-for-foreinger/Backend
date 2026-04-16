package com.mealguide.mealguide_api.onboarding.application.port;

import com.mealguide.mealguide_api.onboarding.domain.SchoolOption;

import java.util.List;

public interface SchoolQueryPort {
    List<SchoolOption> findSchools(String langCode);
}

