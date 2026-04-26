package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.mealcrawl.application.dto.CafeteriaRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.CafeteriaQueryPort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.CafeteriaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CafeteriaQueryPersistenceAdapter implements CafeteriaQueryPort {

    private final CafeteriaJpaRepository cafeteriaJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CafeteriaRow> findCafeteriasBySchoolId(Long schoolId) {
        return cafeteriaJpaRepository.findCafeteriaRowsBySchoolId(schoolId);
    }
}
