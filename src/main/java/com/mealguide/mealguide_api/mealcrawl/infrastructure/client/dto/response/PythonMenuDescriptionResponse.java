package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import java.util.List;

public record PythonMenuDescriptionResponse(
        List<PythonMenuDescriptionResultDto> results
) {
}
