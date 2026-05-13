package com.mealguide.mealguide_api.mealcrawl.domain;

import java.io.Serializable;
import java.util.Objects;

public class MenuAiAnalysisAllergyId implements Serializable {

    private Long menuAiAnalysisId;
    private String allergyCode;

    public MenuAiAnalysisAllergyId() {
    }

    public MenuAiAnalysisAllergyId(Long menuAiAnalysisId, String allergyCode) {
        this.menuAiAnalysisId = menuAiAnalysisId;
        this.allergyCode = allergyCode;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MenuAiAnalysisAllergyId that)) {
            return false;
        }
        return Objects.equals(menuAiAnalysisId, that.menuAiAnalysisId)
                && Objects.equals(allergyCode, that.allergyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuAiAnalysisId, allergyCode);
    }
}

