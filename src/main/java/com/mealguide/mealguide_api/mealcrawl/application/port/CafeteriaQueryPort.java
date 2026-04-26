package com.mealguide.mealguide_api.mealcrawl.application.port;

import com.mealguide.mealguide_api.mealcrawl.application.dto.CafeteriaRow;

import java.util.List;

public interface CafeteriaQueryPort {

    List<CafeteriaRow> findCafeteriasBySchoolId(Long schoolId);
}
