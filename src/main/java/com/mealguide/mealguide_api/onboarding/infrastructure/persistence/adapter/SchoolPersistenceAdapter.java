package com.mealguide.mealguide_api.onboarding.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.onboarding.application.port.SchoolQueryPort;
import com.mealguide.mealguide_api.onboarding.domain.SchoolOption;
import com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository.SchoolJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SchoolPersistenceAdapter implements SchoolQueryPort {

    private final SchoolJpaRepository schoolJpaRepository;

    @Override
    public List<SchoolOption> findSchools(String langCode) {
        return schoolJpaRepository.findSchoolOptions(langCode);
    }
}
