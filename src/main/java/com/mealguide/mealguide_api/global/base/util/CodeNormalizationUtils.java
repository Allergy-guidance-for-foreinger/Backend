package com.mealguide.mealguide_api.global.base.util;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CodeNormalizationUtils {

    private CodeNormalizationUtils() {
    }

    public static List<String> normalizeRequiredCodes(List<String> codes, ErrorCode errorCode) {
        if (codes == null) {
            throw new ServiceException(errorCode);
        }
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String code : codes) {
            if (code == null || code.isBlank()) {
                throw new ServiceException(errorCode);
            }
            deduplicated.add(code.trim());
        }
        return new ArrayList<>(deduplicated);
    }
}
