package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ReligionIngredientMapCachePayload(
        Map<String, List<RestrictionData>> restrictionsByIngredientCode
) {
    public ReligionIngredientMapCachePayload {
        if (restrictionsByIngredientCode == null || restrictionsByIngredientCode.isEmpty()) {
            restrictionsByIngredientCode = Map.of();
        } else {
            Map<String, List<RestrictionData>> copied = new LinkedHashMap<>();
            for (Map.Entry<String, List<RestrictionData>> entry : restrictionsByIngredientCode.entrySet()) {
                if (entry.getKey() != null) {
                    copied.put(entry.getKey(), entry.getValue() == null ? List.of() : entry.getValue().stream()
                            .filter(Objects::nonNull)
                            .toList());
                }
            }
            restrictionsByIngredientCode = Map.copyOf(copied);
        }
    }

    public static ReligionIngredientMapCachePayload from(List<ReligionIngredientMappingRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ReligionIngredientMapCachePayload(Map.of());
        }
        Map<String, List<RestrictionData>> map = new LinkedHashMap<>();
        for (ReligionIngredientMappingRow row : rows) {
            if (row == null || row.ingredientCode() == null || row.restrictionCode() == null) {
                continue;
            }
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
            if (namesByLangCode == null || namesByLangCode.isEmpty()) {
                namesByLangCode = Map.of();
            } else {
                Map<String, String> copied = new LinkedHashMap<>();
                namesByLangCode.forEach((langCode, name) -> {
                    if (langCode != null && name != null) {
                        copied.put(langCode, name);
                    }
                });
                namesByLangCode = Map.copyOf(copied);
            }
        }
    }
}
