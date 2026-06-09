package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ReligionIngredientMapCachePayload(
        Map<String, List<RestrictionData>> restrictionsByIngredientCode
) {
    public ReligionIngredientMapCachePayload {
        restrictionsByIngredientCode = restrictionsByIngredientCode == null ? Map.of() : Map.copyOf(restrictionsByIngredientCode);
    }

    public static ReligionIngredientMapCachePayload from(List<ReligionIngredientMappingRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ReligionIngredientMapCachePayload(Map.of());
        }
        Map<String, List<RestrictionData>> map = new LinkedHashMap<>();
        for (ReligionIngredientMappingRow row : rows) {
            map.computeIfAbsent(row.ingredientCode(), unused -> new ArrayList<>())
                    .add(new RestrictionData(row.restrictionCode(), buildNamesByLangCode(row)));
        }
        return new ReligionIngredientMapCachePayload(map);
    }

    private static Map<String, String> buildNamesByLangCode(ReligionIngredientMappingRow row) {
        Map<String, String> names = new LinkedHashMap<>();
        if (row.koreanName() != null) {
            names.put("ko", row.koreanName());
        }
        if (row.englishName() != null) {
            names.put("en", row.englishName());
        }
        return names;
    }

    public record RestrictionData(
            String restrictionCode,
            Map<String, String> namesByLangCode
    ) {
        public RestrictionData {
            namesByLangCode = namesByLangCode == null ? Map.of() : Map.copyOf(namesByLangCode);
        }
    }
}
