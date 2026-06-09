package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuReligiousMatchRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailBaseCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRiskDataCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.ReligionIngredientMapCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.ReligionIngredientMappingRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuLikePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuReadCachePort;
import com.mealguide.mealguide_api.review.application.port.MenuReviewPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuLikeTarget;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuDetailBatchResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuDetailQueryService {

    private static final String SOURCE_CONFIRMED = "CONFIRMED";
    private static final String SOURCE_AI = "AI";
    private static final String AI_STATUS_SUCCESS = "SUCCESS";
    private static final String LANGUAGE_INDEPENDENT_LOOKUP_LANG_CODE = "ko";
    private static final int MAX_BATCH_SIZE = 30;
    private static final BigDecimal MATCHED_CONFIDENCE = BigDecimal.ONE;

    private final MealUserPreferencePort mealUserPreferencePort;
    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final MenuReadCachePort menuReadCachePort;
    private final MenuLikePort menuLikePort;
    private final MenuReviewPort menuReviewPort;
    private final MealCrawlProperties mealCrawlProperties;
    private final RiskLevelPolicyResolver riskLevelPolicyResolver;

    public MenuDetailResponse getMenuDetail(Long userId, Long mealMenuId) {
        MenuDetailBatchResponse response = getMenuDetails(userId, List.of(mealMenuId));
        return response.menus().getFirst();
    }

    public MenuDetailBatchResponse getMenuDetails(Long userId, List<Long> mealMenuIds) {
        List<Long> normalizedIds = normalizeMealMenuIds(mealMenuIds);
        CurrentUserMealPreference preference = mealUserPreferencePort.getCurrentUserMealPreference(userId);
        if (preference.schoolId() == null) {
            throw new ServiceException(ErrorCode.ESSENTIAL_FIELD_MISSING_ERROR);
        }

        Set<Long> targetIds = new LinkedHashSet<>(normalizedIds);
        String languageCode = normalizeLanguageCode(preference.languageCode());
        Map<Long, MenuDetailBaseCachePayload> baseById = loadMenuDetailBases(targetIds, languageCode);
        if (baseById.size() != targetIds.size()) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }

        boolean hasOtherSchoolMenu = baseById.values().stream()
                .anyMatch(detail -> !preference.schoolId().equals(detail.schoolId()));
        if (hasOtherSchoolMenu) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }

        Map<Long, MenuDetailRiskDataCachePayload> riskDataById = loadMenuDetailRiskData(targetIds);
        Map<Long, List<MealMenuReligiousMatchRow>> religiousMatchesByMealMenuId =
                buildReligiousMatches(baseById, riskDataById, preference.religiousCodes(), languageCode);
        Map<Long, MenuLikeTarget> likeTargetsByMealMenuId = new LinkedHashMap<>();
        for (MenuDetailBaseCachePayload detailRow : baseById.values()) {
            likeTargetsByMealMenuId.put(
                    detailRow.mealMenuId(),
                    new MenuLikeTarget(detailRow.cafeteriaId(), detailRow.menuId())
            );
        }
        Set<MenuLikeTarget> likeTargets = new HashSet<>(likeTargetsByMealMenuId.values());
        Map<MenuLikeTarget, Long> likeCountByTarget = menuLikePort.countLikesByTargets(likeTargets);
        Set<MenuLikeTarget> likedTargetsByUser = menuLikePort.findLikedTargetsByUser(userId, likeTargets);
        Map<MenuLikeTarget, Long> reviewCountByTarget = menuReviewPort.countActiveReviewsByTargets(likeTargets);

        List<MenuDetailResponse> menus = new ArrayList<>(normalizedIds.size());
        Set<String> userAllergyCodes = new HashSet<>(preference.allergyCodes());
        for (Long mealMenuId : normalizedIds) {
            MenuDetailBaseCachePayload detail = baseById.get(mealMenuId);
            IngredientSelection ingredientSelection = new IngredientSelection(
                    riskDataById.get(mealMenuId) == null ? null : riskDataById.get(mealMenuId).ingredientSource(),
                    detail.ingredients().stream()
                            .map(ingredient -> new MenuDetailResponse.IngredientResponse(
                                    ingredient.code(),
                                    ingredient.name(),
                                    ingredient.source()
                            ))
                            .toList()
            );

            List<MenuDetailResponse.MatchedAllergyResponse> matchedAllergies = detail.allergies()
                    .stream()
                    .filter(row -> userAllergyCodes.contains(row.code()))
                    .map(row -> new MenuDetailResponse.MatchedAllergyResponse(
                            row.code(),
                            row.name(),
                            riskLevelPolicyResolver.resolveAllergy(
                                    true,
                                    row.confidence() == null ? MATCHED_CONFIDENCE : row.confidence()
                            ).name(),
                            row.confidence()
                    ))
                    .toList();
            List<MenuDetailResponse.AllergyResponse> allergies = detail.allergies().stream()
                    .map(row -> new MenuDetailResponse.AllergyResponse(
                            row.code(),
                            row.name(),
                            SOURCE_AI
                    ))
                    .toList();
            List<MenuDetailResponse.MatchedReligiousIngredientResponse> matchedReligiousIngredients =
                    mapMatchedReligiousIngredients(
                            ingredientSelection,
                            religiousMatchesByMealMenuId.getOrDefault(mealMenuId, List.of())
                    );
            MenuLikeTarget likeTarget = likeTargetsByMealMenuId.get(mealMenuId);
            long likeCount = likeCountByTarget.getOrDefault(likeTarget, 0L);
            boolean likedByMe = likedTargetsByUser.contains(likeTarget);

            menus.add(new MenuDetailResponse(
                    detail.mealMenuId(),
                    detail.menuName(),
                    detail.description(),
                    detail.cornerName(),
                    detail.displayOrder(),
                    detail.spicyLevel(),
                    detail.aiAnalyzed(),
                    allergies,
                    matchedAllergies,
                    ingredientSelection.ingredients(),
                    matchedReligiousIngredients,
                    new MenuDetailResponse.LikeResponse(likeCount, likedByMe),
                    new MenuDetailResponse.ReviewSummaryResponse(reviewCountByTarget.getOrDefault(likeTarget, 0L))
            ));
        }
        return new MenuDetailBatchResponse(menus);
    }

    private Map<Long, MenuDetailBaseCachePayload> loadMenuDetailBases(Set<Long> mealMenuIds, String languageCode) {
        Map<Long, MenuDetailBaseCachePayload> result = new LinkedHashMap<>();
        Set<Long> missing = new LinkedHashSet<>();
        for (Long mealMenuId : mealMenuIds) {
            menuReadCachePort.findMenuDetailBase(mealMenuId, languageCode)
                    .ifPresentOrElse(
                            payload -> result.put(mealMenuId, payload),
                            () -> missing.add(mealMenuId)
                    );
        }
        if (!missing.isEmpty()) {
            Map<Long, MenuDetailBaseCachePayload> loaded = loadMenuDetailBasesFromDb(missing, languageCode);
            loaded.forEach((mealMenuId, payload) -> {
                result.put(mealMenuId, payload);
                menuReadCachePort.upsertMenuDetailBase(mealMenuId, languageCode, payload, readCacheTtl());
            });
        }
        return result;
    }

    private Map<Long, MenuDetailBaseCachePayload> loadMenuDetailBasesFromDb(Set<Long> mealMenuIds, String languageCode) {
        Map<Long, MenuDetailRow> detailsById = mealCrawlPersistencePort.findMenuDetailsByMealMenuIds(mealMenuIds).stream()
                .collect(Collectors.toMap(
                        MenuDetailRow::mealMenuId,
                        row -> row,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));
        Map<Long, String> translatedMenuNames = mealCrawlPersistencePort.findTranslatedMenuNamesByMealMenuIds(mealMenuIds, languageCode);
        if (translatedMenuNames == null) {
            translatedMenuNames = Map.of();
        }
        Map<Long, String> translatedNamesByMealMenuId = translatedMenuNames;
        Map<Long, String> menuDescriptions = mealCrawlPersistencePort.findMenuDescriptionsByMealMenuIds(mealMenuIds, languageCode);
        if (menuDescriptions == null) {
            menuDescriptions = Map.of();
        }
        Map<Long, String> descriptionsByMealMenuId = menuDescriptions;
        Map<Long, IngredientSelection> ingredientSelections = resolveIngredients(mealMenuIds, languageCode);
        Map<Long, List<MealMenuAllergyRow>> allergiesByMealMenuId = listOrEmpty(mealCrawlPersistencePort
                .findAllergiesByMealMenuIds(mealMenuIds, languageCode))
                .stream()
                .collect(Collectors.groupingBy(MealMenuAllergyRow::mealMenuId));

        Map<Long, MenuDetailBaseCachePayload> result = new LinkedHashMap<>();
        detailsById.forEach((mealMenuId, detail) -> {
            IngredientSelection ingredients = ingredientSelections.getOrDefault(mealMenuId, new IngredientSelection(null, List.of()));
            result.put(mealMenuId, new MenuDetailBaseCachePayload(
                    detail.mealMenuId(),
                    detail.cafeteriaId(),
                    detail.menuId(),
                    detail.schoolId(),
                    translatedNamesByMealMenuId.getOrDefault(mealMenuId, detail.menuName()),
                    descriptionsByMealMenuId.get(mealMenuId),
                    detail.cornerName(),
                    detail.displayOrder(),
                    detail.spicyLevel(),
                    AI_STATUS_SUCCESS.equals(detail.aiAnalysisStatus()),
                    ingredients.ingredients().stream()
                            .map(ingredient -> new MenuDetailBaseCachePayload.IngredientData(
                                    ingredient.code(),
                                    ingredient.name(),
                                    ingredient.source()
                            ))
                            .toList(),
                    allergiesByMealMenuId.getOrDefault(mealMenuId, List.of()).stream()
                            .map(allergy -> new MenuDetailBaseCachePayload.AllergyData(
                                    allergy.allergyCode(),
                                    allergy.allergyName(),
                                    allergy.confidence()
                            ))
                            .toList()
            ));
        });
        return result;
    }

    private Map<Long, MenuDetailRiskDataCachePayload> loadMenuDetailRiskData(Set<Long> mealMenuIds) {
        Map<Long, MenuDetailRiskDataCachePayload> result = new LinkedHashMap<>();
        Set<Long> missing = new LinkedHashSet<>();
        for (Long mealMenuId : mealMenuIds) {
            menuReadCachePort.findMenuDetailRiskData(mealMenuId)
                    .ifPresentOrElse(
                            payload -> result.put(mealMenuId, payload),
                            () -> missing.add(mealMenuId)
                    );
        }
        if (!missing.isEmpty()) {
            Map<Long, MenuDetailRiskDataCachePayload> loaded = loadMenuDetailRiskDataFromDb(missing);
            loaded.forEach((mealMenuId, payload) -> {
                result.put(mealMenuId, payload);
                menuReadCachePort.upsertMenuDetailRiskData(mealMenuId, payload, readCacheTtl());
            });
        }
        return result;
    }

    private Map<Long, MenuDetailRiskDataCachePayload> loadMenuDetailRiskDataFromDb(Set<Long> mealMenuIds) {
        // Risk-data cache is language-independent and stores only codes/source/confidence.
        // Use Korean as a stable lookup language because display names are cached separately in menu detail base.
        Map<Long, IngredientSelection> ingredientSelections = resolveIngredients(mealMenuIds, LANGUAGE_INDEPENDENT_LOOKUP_LANG_CODE);
        List<MealMenuAllergyRow> allergyRows = listOrEmpty(mealCrawlPersistencePort.findAllergiesByMealMenuIds(
                mealMenuIds,
                LANGUAGE_INDEPENDENT_LOOKUP_LANG_CODE
        ));
        Map<String, List<ReligionIngredientMapCachePayload.RestrictionData>> restrictionsByIngredientCode =
                loadReligionIngredientMap().restrictionsByIngredientCode();
        Set<String> allReligiousCodes = restrictionsByIngredientCode == null ? Set.of() : restrictionsByIngredientCode.values().stream()
                .flatMap(List::stream)
                .map(ReligionIngredientMapCachePayload.RestrictionData::restrictionCode)
                .collect(Collectors.toSet());
        Map<Long, Map<String, BigDecimal>> religiousConfidenceByMealMenuId = new LinkedHashMap<>();
        if (!allReligiousCodes.isEmpty()) {
            for (MealMenuReligiousMatchRow row : listOrEmpty(mealCrawlPersistencePort.findReligiousMatchedIngredientsByMealMenuIds(
                    mealMenuIds,
                    List.copyOf(allReligiousCodes),
                    LANGUAGE_INDEPENDENT_LOOKUP_LANG_CODE
            ))) {
                religiousConfidenceByMealMenuId
                        .computeIfAbsent(row.mealMenuId(), unused -> new LinkedHashMap<>())
                        .putIfAbsent(row.ingredientCode(), row.confidence());
            }
        }
        Map<Long, List<MealMenuAllergyRow>> allergiesByMealMenuId = allergyRows.stream()
                .collect(Collectors.groupingBy(MealMenuAllergyRow::mealMenuId));

        Map<Long, MenuDetailRiskDataCachePayload> result = new LinkedHashMap<>();
        for (Long mealMenuId : mealMenuIds) {
            IngredientSelection selection = ingredientSelections.getOrDefault(mealMenuId, new IngredientSelection(null, List.of()));
            Map<String, BigDecimal> confidenceByIngredient = religiousConfidenceByMealMenuId.getOrDefault(mealMenuId, Map.of());
            result.put(mealMenuId, new MenuDetailRiskDataCachePayload(
                    selection.source(),
                    selection.ingredients().stream()
                            .map(ingredient -> new MenuDetailRiskDataCachePayload.IngredientData(
                                    ingredient.code(),
                                    confidenceByIngredient.get(ingredient.code())
                            ))
                            .toList(),
                    allergiesByMealMenuId.getOrDefault(mealMenuId, List.of()).stream()
                            .map(allergy -> new MenuDetailRiskDataCachePayload.AllergyData(
                                    allergy.allergyCode(),
                                    allergy.confidence()
                            ))
                            .toList()
            ));
        }
        return result;
    }

    private Map<Long, List<MealMenuReligiousMatchRow>> buildReligiousMatches(
            Map<Long, MenuDetailBaseCachePayload> baseById,
            Map<Long, MenuDetailRiskDataCachePayload> riskDataById,
            List<String> religiousCodes,
            String languageCode
    ) {
        if (religiousCodes == null || religiousCodes.isEmpty()) {
            return Map.of();
        }
        Set<String> selectedReligiousCodes = new HashSet<>(religiousCodes);
        ReligionIngredientMapCachePayload religionMap = loadReligionIngredientMap();
        Map<Long, List<MealMenuReligiousMatchRow>> result = new LinkedHashMap<>();
        if (religionMap.restrictionsByIngredientCode() == null) {
            return result;
        }

        riskDataById.forEach((mealMenuId, riskData) -> {
            if (riskData == null || riskData.ingredients() == null) {
                return;
            }
            Map<String, String> ingredientNamesByCode = findIngredientNamesByCode(baseById.get(mealMenuId));
            for (MenuDetailRiskDataCachePayload.IngredientData ingredient : riskData.ingredients()) {
                List<ReligionIngredientMapCachePayload.RestrictionData> restrictions =
                        religionMap.restrictionsByIngredientCode().getOrDefault(ingredient.code(), List.of());
                for (ReligionIngredientMapCachePayload.RestrictionData restriction : restrictions) {
                    if (selectedReligiousCodes.contains(restriction.restrictionCode())) {
                        result.computeIfAbsent(mealMenuId, unused -> new ArrayList<>())
                                .add(new MealMenuReligiousMatchRow(
                                        mealMenuId,
                                        ingredient.code(),
                                        ingredientNamesByCode.getOrDefault(ingredient.code(), ingredient.code()),
                                        ingredient.confidence(),
                                        restriction.restrictionCode(),
                                        resolveRestrictionName(restriction, languageCode)
                                ));
                    }
                }
            }
        });
        return result;
    }

    private Map<String, String> findIngredientNamesByCode(MenuDetailBaseCachePayload base) {
        if (base == null || base.ingredients() == null || base.ingredients().isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (MenuDetailBaseCachePayload.IngredientData ingredient : base.ingredients()) {
            if (ingredient.code() != null && ingredient.name() != null) {
                result.putIfAbsent(ingredient.code(), ingredient.name());
            }
        }
        return result;
    }

    private ReligionIngredientMapCachePayload loadReligionIngredientMap() {
        return menuReadCachePort.findReligionIngredientMap()
                .orElseGet(() -> {
                    ReligionIngredientMapCachePayload loaded = loadReligionIngredientMapFromDb();
                    menuReadCachePort.upsertReligionIngredientMap(loaded, readCacheTtl());
                    return loaded;
                });
    }

    private ReligionIngredientMapCachePayload loadReligionIngredientMapFromDb() {
        return ReligionIngredientMapCachePayload.from(mealCrawlPersistencePort.findReligionIngredientMappings());
    }

    private String resolveRestrictionName(ReligionIngredientMapCachePayload.RestrictionData restriction, String languageCode) {
        if (restriction.namesByLangCode() == null || restriction.namesByLangCode().isEmpty()) {
            return restriction.restrictionCode();
        }
        return restriction.namesByLangCode().getOrDefault(
                normalizeLanguageCode(languageCode),
                restriction.namesByLangCode().getOrDefault("ko", restriction.restrictionCode())
        );
    }

    private List<Long> normalizeMealMenuIds(List<Long> mealMenuIds) {
        if (mealMenuIds == null || mealMenuIds.isEmpty() || mealMenuIds.size() > MAX_BATCH_SIZE) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        LinkedHashSet<Long> deduplicated = new LinkedHashSet<>();
        for (Long mealMenuId : mealMenuIds) {
            if (mealMenuId == null || mealMenuId <= 0) {
                throw new ServiceException(ErrorCode.BINDING_ERROR);
            }
            deduplicated.add(mealMenuId);
        }
        if (deduplicated.isEmpty()) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        return List.copyOf(deduplicated);
    }

    private Map<Long, IngredientSelection> resolveIngredients(Set<Long> mealMenuIds, String languageCode) {
        Map<Long, List<MenuDetailResponse.IngredientResponse>> confirmedByMenuId = listOrEmpty(mealCrawlPersistencePort
                .findConfirmedIngredientsForMenuDetails(mealMenuIds, languageCode))
                .stream()
                .collect(Collectors.groupingBy(
                        MealMenuIngredientRow::mealMenuId,
                        Collectors.mapping(
                                row -> new MenuDetailResponse.IngredientResponse(
                                        row.ingredientCode(),
                                        row.ingredientName(),
                                        SOURCE_CONFIRMED
                                ),
                                Collectors.toList()
                        )
                ));

        Map<Long, IngredientSelection> selectedByMenuId = new LinkedHashMap<>();
        Set<Long> unresolvedMenuIds = new HashSet<>();
        for (Long mealMenuId : mealMenuIds) {
            List<MenuDetailResponse.IngredientResponse> confirmed = confirmedByMenuId.getOrDefault(mealMenuId, List.of());
            if (!confirmed.isEmpty()) {
                selectedByMenuId.put(mealMenuId, new IngredientSelection(SOURCE_CONFIRMED, confirmed));
            } else {
                unresolvedMenuIds.add(mealMenuId);
            }
        }

        if (!unresolvedMenuIds.isEmpty()) {
            Map<Long, List<MenuDetailResponse.IngredientResponse>> aiByMenuId = listOrEmpty(mealCrawlPersistencePort
                    .findAiIngredientsForMenuDetails(unresolvedMenuIds, languageCode))
                    .stream()
                    .collect(Collectors.groupingBy(
                            MealMenuIngredientRow::mealMenuId,
                            Collectors.mapping(
                                    row -> new MenuDetailResponse.IngredientResponse(
                                            row.ingredientCode(),
                                            row.ingredientName(),
                                            SOURCE_AI
                                    ),
                                    Collectors.toList()
                            )
                    ));
            for (Long mealMenuId : unresolvedMenuIds) {
                List<MenuDetailResponse.IngredientResponse> ai = aiByMenuId.getOrDefault(mealMenuId, List.of());
                selectedByMenuId.put(mealMenuId, new IngredientSelection(ai.isEmpty() ? null : SOURCE_AI, ai));
            }
        }
        return selectedByMenuId;
    }

    private List<MenuDetailResponse.MatchedReligiousIngredientResponse> mapMatchedReligiousIngredients(
            IngredientSelection ingredientSelection,
            List<MealMenuReligiousMatchRow> religiousMatchRows
    ) {
        if (religiousMatchRows.isEmpty() || ingredientSelection.ingredients().isEmpty()) {
            return List.of();
        }
        Map<String, List<MealMenuReligiousMatchRow>> byIngredientCode = religiousMatchRows.stream()
                .collect(Collectors.groupingBy(MealMenuReligiousMatchRow::ingredientCode));
        return ingredientSelection.ingredients().stream()
                .filter(ingredient -> byIngredientCode.containsKey(ingredient.code()))
                .map(ingredient -> {
                    List<MealMenuReligiousMatchRow> matches = byIngredientCode.get(ingredient.code());
                    List<MenuDetailResponse.MatchedReligiousRestrictionResponse> matchedRestrictions =
                            matches.stream()
                                    .map(row -> new MenuDetailResponse.MatchedReligiousRestrictionResponse(
                                            row.restrictionCode(),
                                            row.restrictionName(),
                                            riskLevelPolicyResolver.resolveReligious(
                                                    true,
                                                    row.confidence() == null ? MATCHED_CONFIDENCE : row.confidence()
                                            ).name()
                                    ))
                                    .toList();
                    java.math.BigDecimal confidence = null;
                    if (SOURCE_AI.equals(ingredientSelection.source())) {
                        confidence = matches.stream()
                                .map(MealMenuReligiousMatchRow::confidence)
                                .filter(java.util.Objects::nonNull)
                                .findFirst()
                                .orElse(null);
                    }
                    return new MenuDetailResponse.MatchedReligiousIngredientResponse(
                            ingredient.code(),
                            ingredient.name(),
                            confidence,
                            matchedRestrictions
                    );
                })
                .toList();
    }

    private String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return "ko";
        }
        return languageCode.trim().toLowerCase(Locale.ROOT);
    }

    private Duration readCacheTtl() {
        return Duration.ofSeconds(mealCrawlProperties.getReadCacheTtlSeconds());
    }

    private <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record IngredientSelection(
            String source,
            List<MenuDetailResponse.IngredientResponse> ingredients
    ) {
    }
}
