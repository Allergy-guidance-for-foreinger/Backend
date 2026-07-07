package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuReligiousMatchRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailIngredientRow;
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
import java.util.Collections;
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

    private static final String SOURCE_AI = "AI";
    private static final String AI_STATUS_SUCCESS = "SUCCESS";
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
        MenuDetailReadData readData = loadMenuDetailReadData(targetIds, languageCode);
        Map<Long, MenuDetailBaseCachePayload> baseById = readData.baseById();
        if (baseById.size() != targetIds.size()) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }

        boolean hasOtherSchoolMenu = baseById.values().stream()
                .anyMatch(detail -> !preference.schoolId().equals(detail.schoolId()));
        if (hasOtherSchoolMenu) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }

        Map<Long, MenuDetailRiskDataCachePayload> riskDataById = readData.riskDataById();
        Map<Long, List<MealMenuReligiousMatchRow>> religiousMatchesByMealMenuId =
                buildReligiousMatches(riskDataById, preference.religiousCodes(), languageCode, readData.religionMap());
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
        Set<String> userAllergyCodes = preference.allergyCodes() == null
                ? Collections.emptySet()
                : new HashSet<>(preference.allergyCodes());
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

    private MenuDetailReadData loadMenuDetailReadData(Set<Long> mealMenuIds, String languageCode) {
        Map<Long, MenuDetailRow> detailsById = mealCrawlPersistencePort.findMenuDetailsByMealMenuIds(mealMenuIds).stream()
                .collect(Collectors.toMap(
                        MenuDetailRow::mealMenuId,
                        row -> row,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));
        Map<Long, String> translatedNamesByMealMenuId = mapOrEmpty("ko".equals(languageCode)
                ? Map.of()
                : mealCrawlPersistencePort.findTranslatedMenuNamesByMealMenuIds(mealMenuIds, languageCode));
        Map<Long, String> descriptionsByMealMenuId = mapOrEmpty(
                mealCrawlPersistencePort.findMenuDescriptionsByMealMenuIds(mealMenuIds, languageCode)
        );
        Map<Long, SelectedIngredientSelection> ingredientSelections = resolveSelectedIngredients(mealMenuIds, languageCode);
        Map<Long, List<MealMenuAllergyRow>> allergiesByMealMenuId = listOrEmpty(mealCrawlPersistencePort
                .findAllergiesByMealMenuIds(mealMenuIds, languageCode))
                .stream()
                .collect(Collectors.groupingBy(MealMenuAllergyRow::mealMenuId));

        Map<Long, MenuDetailBaseCachePayload> baseById = new LinkedHashMap<>();
        Map<Long, MenuDetailRiskDataCachePayload> riskDataById = new LinkedHashMap<>();
        detailsById.forEach((mealMenuId, detail) -> {
            SelectedIngredientSelection ingredients = ingredientSelections.getOrDefault(
                    mealMenuId,
                    new SelectedIngredientSelection(null, List.of())
            );
            List<MealMenuAllergyRow> allergies = allergiesByMealMenuId.getOrDefault(mealMenuId, List.of());
            baseById.put(mealMenuId, new MenuDetailBaseCachePayload(
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
                                    ingredients.source()
                            ))
                            .toList(),
                    allergies.stream()
                            .map(allergy -> new MenuDetailBaseCachePayload.AllergyData(
                                    allergy.allergyCode(),
                                    allergy.allergyName(),
                                    allergy.confidence()
                            ))
                            .toList()
            ));
            riskDataById.put(mealMenuId, new MenuDetailRiskDataCachePayload(
                    ingredients.source(),
                    ingredients.ingredients().stream()
                            .map(ingredient -> new MenuDetailRiskDataCachePayload.IngredientData(
                                    ingredient.code(),
                                    ingredient.confidence()
                            ))
                            .toList(),
                    allergies.stream()
                            .map(allergy -> new MenuDetailRiskDataCachePayload.AllergyData(
                                    allergy.allergyCode(),
                                    allergy.confidence()
                            ))
                            .toList()
            ));
        });
        return new MenuDetailReadData(baseById, riskDataById, loadReligionIngredientMap());
    }

    private Map<Long, List<MealMenuReligiousMatchRow>> buildReligiousMatches(
            Map<Long, MenuDetailRiskDataCachePayload> riskDataById,
            List<String> religiousCodes,
            String languageCode,
            ReligionIngredientMapCachePayload religionMap
    ) {
        if (religiousCodes == null || religiousCodes.isEmpty() || riskDataById == null || riskDataById.isEmpty()) {
            return Map.of();
        }
        boolean hasIngredientCodes = riskDataById.values().stream()
                .anyMatch(riskData -> riskData != null
                        && riskData.ingredients() != null
                        && riskData.ingredients().stream().anyMatch(ingredient -> ingredient != null && ingredient.code() != null));
        if (!hasIngredientCodes) {
            return Map.of();
        }
        Set<String> selectedReligiousCodes = new HashSet<>(religiousCodes);
        Map<Long, List<MealMenuReligiousMatchRow>> result = new LinkedHashMap<>();
        if (religionMap == null || religionMap.restrictionsByIngredientCode() == null) {
            return result;
        }

        riskDataById.forEach((mealMenuId, riskData) -> {
            if (riskData == null || riskData.ingredients() == null) {
                return;
            }
            for (MenuDetailRiskDataCachePayload.IngredientData ingredient : riskData.ingredients()) {
                if (ingredient.code() == null) {
                    continue;
                }
                List<ReligionIngredientMapCachePayload.RestrictionData> restrictions =
                        religionMap.restrictionsByIngredientCode().getOrDefault(ingredient.code(), List.of());
                for (ReligionIngredientMapCachePayload.RestrictionData restriction : restrictions) {
                    if (selectedReligiousCodes.contains(restriction.restrictionCode())) {
                        result.computeIfAbsent(mealMenuId, unused -> new ArrayList<>())
                                .add(new MealMenuReligiousMatchRow(
                                        mealMenuId,
                                        ingredient.code(),
                                        ingredient.code(),
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

    private ReligionIngredientMapCachePayload loadReligionIngredientMap() {
        // Redis read/write cache is temporarily disabled until the menu detail query path is optimized first.
        // return menuReadCachePort.findReligionIngredientMap()
        //         .orElseGet(() -> {
        //             ReligionIngredientMapCachePayload loaded = loadReligionIngredientMapFromDb();
        //             menuReadCachePort.upsertReligionIngredientMap(loaded, readCacheTtl());
        //             return loaded;
        //         });
        return loadReligionIngredientMapFromDb();
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

    private Map<Long, SelectedIngredientSelection> resolveSelectedIngredients(Set<Long> mealMenuIds, String languageCode) {
        return listOrEmpty(mealCrawlPersistencePort.findSelectedIngredientsForMenuDetails(mealMenuIds, languageCode))
                .stream()
                .collect(Collectors.groupingBy(
                        MenuDetailIngredientRow::mealMenuId,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), rows -> {
                            String source = rows.stream()
                                    .map(MenuDetailIngredientRow::source)
                                    .filter(java.util.Objects::nonNull)
                                    .findFirst()
                                    .orElse(null);
                            List<SelectedIngredient> ingredients = rows.stream()
                                    .map(row -> new SelectedIngredient(
                                            row.ingredientCode(),
                                            row.ingredientName(),
                                            row.confidence()
                                    ))
                                    .toList();
                            return new SelectedIngredientSelection(source, ingredients);
                        })
                ));
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

    private <K, V> Map<K, V> mapOrEmpty(Map<K, V> values) {
        return values == null ? Map.of() : values;
    }

    private record IngredientSelection(
            String source,
            List<MenuDetailResponse.IngredientResponse> ingredients
    ) {
    }

    private record SelectedIngredientSelection(
            String source,
            List<SelectedIngredient> ingredients
    ) {
    }

    private record SelectedIngredient(
            String code,
            String name,
            BigDecimal confidence
    ) {
    }

    private record MenuDetailReadData(
            Map<Long, MenuDetailBaseCachePayload> baseById,
            Map<Long, MenuDetailRiskDataCachePayload> riskDataById,
            ReligionIngredientMapCachePayload religionMap
    ) {
    }
}
