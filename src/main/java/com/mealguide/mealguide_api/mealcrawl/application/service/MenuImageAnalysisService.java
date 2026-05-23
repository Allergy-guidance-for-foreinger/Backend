package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ExternalApiException;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuImageStoragePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.*;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.PythonMealClientException;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.*;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuAiAnalysisJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuImageAnalysisLogJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuImageAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MenuImageAnalysisService {
    private final MealUserPreferencePort mealUserPreferencePort;
    private final MenuImageAnalysisLogJpaRepository logRepository;
    private final MenuImageStoragePort menuImageStoragePort;
    private final PythonMealClientPort pythonMealClientPort;
    private final MenuJpaRepository menuJpaRepository;
    private final MenuAiAnalysisJpaRepository menuAiAnalysisJpaRepository;
    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final MealCrawlProperties properties;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RiskLevelPolicyResolver riskLevelPolicyResolver;

    public MenuImageAnalysisResponse analyze(Long userId, MultipartFile image) {
        CurrentUserMealPreference preference = mealUserPreferencePort.getCurrentUserMealPreference(userId);
        MenuImageAnalysisLog log = createProcessingLog(userId);

        validateFileOrFail(log, image);
        String storagePath = uploadOrFail(log, userId, image);
        saveStoragePath(log.getId(), storagePath);

        PythonMenuImageAnalysisResultDto identified = identifyImageOrFail(log, image);
        String identifiedName = identified.identifiedFoodName();

        return menuJpaRepository.findFirstByName(identifiedName)
                .map(menu -> handleKnownMenu(log, preference, identified, menu))
                .orElseGet(() -> handleUnknownMenu(log, preference, identified));
    }

    private MenuImageAnalysisResponse handleKnownMenu(MenuImageAnalysisLog log, CurrentUserMealPreference preference, PythonMenuImageAnalysisResultDto identified, Menu menu) {
        Optional<MenuAiAnalysis> latest = menuAiAnalysisJpaRepository.findLatestByMenuId(menu.getId()).stream()
                .filter(a -> a.getStatus() == MenuAiStatus.SUCCESS)
                .findFirst();
        if (latest.isPresent()) {
            MenuImageAnalysisResponse response = assembleFromStored(preference, log.getId(), identified, latest.get(), menu.getId(), MenuImageAnalysisResponse.MenuImageAnalysisResultSource.STORED_AI_ANALYSIS);
            markSuccess(log.getId(), MenuImageAnalysisResultSource.STORED_AI_ANALYSIS, identified, null);
            return response;
        }

        PythonMenuAnalysisResultDto live = analyzeSingleMenu(menu.getId(), menu.getName());
        saveLiveKnownMenu(menu.getId(), live);
        MenuImageAnalysisResponse response = assembleFromLive(preference, log.getId(), identified, live, MenuImageAnalysisResponse.MenuImageAnalysisResultSource.LIVE_AI_ANALYSIS);
        markSuccess(log.getId(), MenuImageAnalysisResultSource.LIVE_AI_ANALYSIS, identified, null);
        return response;
    }

    private MenuImageAnalysisResponse handleUnknownMenu(MenuImageAnalysisLog log, CurrentUserMealPreference preference, PythonMenuImageAnalysisResultDto identified) {
        long tempMenuId = -log.getId();
        PythonMenuAnalysisResultDto live = analyzeSingleMenu(tempMenuId, identified.identifiedFoodName());
        String fallbackJson;
        try {
            fallbackJson = objectMapper.writeValueAsString(live);
        } catch (Exception e) {
            fallbackJson = null;
        }
        MenuImageAnalysisResponse response = assembleFromLive(preference, log.getId(), identified, live, MenuImageAnalysisResponse.MenuImageAnalysisResultSource.LIVE_AI_ANALYSIS);
        markSuccess(log.getId(), MenuImageAnalysisResultSource.LIVE_AI_ANALYSIS, identified, fallbackJson);
        return response;
    }

    private PythonMenuAnalysisResultDto analyzeSingleMenu(Long menuId, String menuName) {
        try {
            PythonMenuAnalysisResponse response = pythonMealClientPort.analyzeMenus(
                    new PythonMenuAnalysisRequest(List.of(new PythonMenuAnalysisTargetDto(menuId, menuName)))
            );
            if (response == null || response.results() == null || response.results().isEmpty()) {
                throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
            }
            return response.results().getFirst();
        } catch (PythonMealClientException e) {
            throw mapPythonException(e);
        }
    }

    private void saveLiveKnownMenu(Long menuId, PythonMenuAnalysisResultDto result) {
        Set<String> validIngredients = mealCrawlPersistencePort.findExistingIngredientCodes(extractIngredientCodes(result));
        Set<String> validAllergies = mealCrawlPersistencePort.findExistingAllergyCodes(extractAllergyCodes(result));
        mealCrawlPersistencePort.saveMenuAnalysisAndUpdateStatus(
                menuId,
                MenuAiStatus.SUCCESS,
                result.modelName(),
                result.modelVersion(),
                result.reason(),
                LocalDateTime.now(),
                1,
                toIngredientCandidates(result.ingredients()),
                validIngredients,
                toAllergyCandidates(result.allergies()),
                validAllergies,
                MenuSpicyLevel.fromValue(result.spicyLevel())
        );
    }

    private Set<String> extractIngredientCodes(PythonMenuAnalysisResultDto result) {
        Set<String> set = new HashSet<>();
        if (result.ingredients() != null) {
            for (PythonMenuIngredientResultDto ingredient : result.ingredients()) {
                if (ingredient != null && ingredient.ingredientCode() != null && !ingredient.ingredientCode().isBlank()) {
                    set.add(ingredient.ingredientCode().trim());
                }
            }
        }
        return set;
    }

    private Set<String> extractAllergyCodes(PythonMenuAnalysisResultDto result) {
        Set<String> set = new HashSet<>();
        if (result.allergies() != null) {
            for (PythonMenuAllergyResultDto allergy : result.allergies()) {
                if (allergy != null && allergy.allergyCode() != null && !allergy.allergyCode().isBlank()) {
                    set.add(allergy.allergyCode().trim());
                }
            }
        }
        return set;
    }

    private List<MenuIngredientCandidate> toIngredientCandidates(List<PythonMenuIngredientResultDto> ingredients) {
        if (ingredients == null) return List.of();
        List<MenuIngredientCandidate> out = new ArrayList<>();
        for (PythonMenuIngredientResultDto ingredient : ingredients) {
            if (ingredient == null || ingredient.ingredientCode() == null || ingredient.ingredientCode().isBlank()) continue;
            out.add(new MenuIngredientCandidate(ingredient.ingredientCode().trim(), ingredient.confidence()));
        }
        return out;
    }

    private List<MenuAllergyCandidate> toAllergyCandidates(List<PythonMenuAllergyResultDto> allergies) {
        if (allergies == null) return List.of();
        List<MenuAllergyCandidate> out = new ArrayList<>();
        for (PythonMenuAllergyResultDto allergy : allergies) {
            if (allergy == null || allergy.allergyCode() == null || allergy.allergyCode().isBlank()) continue;
            out.add(new MenuAllergyCandidate(allergy.allergyCode().trim(), allergy.confidence(), null));
        }
        return out;
    }

    private MenuImageAnalysisResponse assembleFromStored(CurrentUserMealPreference preference, Long logId, PythonMenuImageAnalysisResultDto identified, MenuAiAnalysis analysis, Long menuId, MenuImageAnalysisResponse.MenuImageAnalysisResultSource source) {
        List<PythonMenuIngredientResultDto> ingredients = loadStoredIngredients(analysis.getId());
        List<PythonMenuAllergyResultDto> allergies = loadStoredAllergies(analysis.getId());
        Long spicyLevel = loadMenuSpicyLevel(menuId);
        return assemble(preference, logId, identified, source, spicyLevel, ingredients, allergies, menuId);
    }

    private MenuImageAnalysisResponse assembleFromLive(CurrentUserMealPreference preference, Long logId, PythonMenuImageAnalysisResultDto identified, PythonMenuAnalysisResultDto live, MenuImageAnalysisResponse.MenuImageAnalysisResultSource source) {
        return assemble(preference, logId, identified, source, live.spicyLevel(), safeIngredients(live.ingredients()), safeAllergies(live.allergies()), live.menuId());
    }

    private List<PythonMenuIngredientResultDto> safeIngredients(List<PythonMenuIngredientResultDto> ingredients) { return ingredients == null ? List.of() : ingredients; }
    private List<PythonMenuAllergyResultDto> safeAllergies(List<PythonMenuAllergyResultDto> allergies) { return allergies == null ? List.of() : allergies; }

    private MenuImageAnalysisResponse assemble(CurrentUserMealPreference preference, Long logId, PythonMenuImageAnalysisResultDto identified, MenuImageAnalysisResponse.MenuImageAnalysisResultSource source, Long spicyLevel, List<PythonMenuIngredientResultDto> ingredients, List<PythonMenuAllergyResultDto> allergies, Long translationMenuId) {
        String lang = preference.languageCode() == null ? "en" : preference.languageCode();
        String identifiedName = translateIdentifiedIfNeeded(lang, identified.identifiedFoodName(), translationMenuId);
        Map<String, String> ingredientNames = loadIngredientNames(lang, extractIngredientCodes(ingredients));
        Map<String, String> allergyNames = loadAllergyNames(lang, extractAllergyCodes(allergies));

        List<MenuImageAnalysisResponse.MenuIngredientResponse> ingredientResponses = new ArrayList<>();
        for (PythonMenuIngredientResultDto ingredient : ingredients) {
            String code = ingredient.ingredientCode();
            if (code == null || code.isBlank()) continue;
            ingredientResponses.add(new MenuImageAnalysisResponse.MenuIngredientResponse(code, ingredientNames.getOrDefault(code, code)));
        }

        List<MenuImageAnalysisResponse.MenuAllergyResponse> allergyResponses = new ArrayList<>();
        List<MenuImageAnalysisResponse.MatchedAllergyResponse> matchedAllergies = new ArrayList<>();
        Set<String> userAllergies = new HashSet<>(preference.allergyCodes() == null ? List.of() : preference.allergyCodes());
        for (PythonMenuAllergyResultDto allergy : allergies) {
            String code = allergy.allergyCode();
            if (code == null || code.isBlank()) continue;
            String name = allergyNames.getOrDefault(code, code);
            allergyResponses.add(new MenuImageAnalysisResponse.MenuAllergyResponse(code, name));
            if (userAllergies.contains(code)) {
                matchedAllergies.add(new MenuImageAnalysisResponse.MatchedAllergyResponse(
                        code, name, riskLevelPolicyResolver.resolveAllergy(true, allergy.confidence()).name(), allergy.confidence()
                ));
            }
        }

        List<MenuImageAnalysisResponse.MatchedReligiousIngredientResponse> religious = mapReligious(preference, lang, ingredients, ingredientNames);
        return new MenuImageAnalysisResponse(
                logId,
                source,
                identifiedName,
                identified.identifiedFoodNameReason(),
                identified.confidence(),
                spicyLevel,
                ingredientResponses,
                allergyResponses,
                matchedAllergies,
                religious
        );
    }

    private List<MenuImageAnalysisResponse.MatchedReligiousIngredientResponse> mapReligious(CurrentUserMealPreference preference, String lang, List<PythonMenuIngredientResultDto> ingredients, Map<String, String> ingredientNames) {
        if (preference.religiousCodes() == null || preference.religiousCodes().isEmpty()) return List.of();
        Map<String, List<RestrictionMatch>> byIngredient = findRestrictionMatches(preference.religiousCodes(), extractIngredientCodes(ingredients), lang);
        List<MenuImageAnalysisResponse.MatchedReligiousIngredientResponse> responses = new ArrayList<>();
        for (PythonMenuIngredientResultDto ingredient : ingredients) {
            if (!byIngredient.containsKey(ingredient.ingredientCode())) continue;
            List<MenuImageAnalysisResponse.MatchedReligiousRestrictionResponse> restrictions = byIngredient.get(ingredient.ingredientCode()).stream()
                    .map(m -> new MenuImageAnalysisResponse.MatchedReligiousRestrictionResponse(
                            m.code(), m.name(), riskLevelPolicyResolver.resolveReligious(true, ingredient.confidence()).name()
                    )).toList();
            responses.add(new MenuImageAnalysisResponse.MatchedReligiousIngredientResponse(
                    ingredient.ingredientCode(),
                    ingredientNames.getOrDefault(ingredient.ingredientCode(), ingredient.ingredientCode()),
                    ingredient.confidence(),
                    restrictions
            ));
        }
        return responses;
    }

    private String translateIdentifiedIfNeeded(String lang, String defaultName, Long menuId) {
        if (menuId != null && menuId > 0) {
            Map<Long, String> names = mealCrawlPersistencePort.findMenuNamesByIds(Set.of(menuId));
            if (names.containsKey(menuId)) {
                String sql = "select name from menu_translation where menu_id=:menuId and lang_code=:lang";
                List<String> rows = jdbc.query(sql, new MapSqlParameterSource().addValue("menuId", menuId).addValue("lang", lang), (rs, rowNum) -> rs.getString("name"));
                if (!rows.isEmpty()) return rows.getFirst();
            }
        }
        if ("ko".equalsIgnoreCase(lang) || menuId == null) return defaultName;
        PythonMenuTranslationResponse response = pythonMealClientPort.translateMenus(new PythonMenuTranslationRequest(
                List.of(new PythonMenuTranslationTargetDto(menuId, defaultName)),
                List.of(lang)
        ));
        if (response.results() == null || response.results().isEmpty()) return defaultName;
        PythonMenuTranslationResultDto first = response.results().getFirst();
        if (first.translations() == null || first.translations().isEmpty()) return defaultName;
        return first.translations().getFirst().translatedName();
    }

    private PythonMenuImageAnalysisResultDto identifyImageOrFail(MenuImageAnalysisLog log, MultipartFile image) {
        try {
            PythonMenuImageAnalysisResponse response = pythonMealClientPort.analyzeImage(image);
            if (response == null || response.results() == null || response.results().isEmpty()) {
                failLog(log.getId(), "COM_001");
                throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
            }
            return response.results().getFirst();
        } catch (PythonMealClientException e) {
            String code = extractErrorCode(e.getResponseBody());
            failLog(log.getId(), code);
            throw mapPythonException(e);
        }
    }

    private RuntimeException mapPythonException(PythonMealClientException e) {
        String code = extractErrorCode(e.getResponseBody());
        String msg = extractErrorMessage(e.getResponseBody());
        return new ExternalApiException(
                e.getHttpStatus() == null ? HttpStatus.BAD_GATEWAY : HttpStatus.valueOf(e.getHttpStatus()),
                code == null ? "PYM_500" : code,
                msg == null ? "Python API call failed." : msg
        );
    }

    private String extractErrorCode(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "PYM_500";
        try {
            return objectMapper.readTree(responseBody).path("code").asText("PYM_500");
        } catch (Exception e) {
            return "PYM_500";
        }
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;
        try {
            return objectMapper.readTree(responseBody).path("msg").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Set<String> extractIngredientCodes(List<PythonMenuIngredientResultDto> ingredients) {
        Set<String> set = new HashSet<>();
        for (PythonMenuIngredientResultDto ingredient : ingredients) {
            if (ingredient != null && ingredient.ingredientCode() != null && !ingredient.ingredientCode().isBlank()) set.add(ingredient.ingredientCode());
        }
        return set;
    }
    private Set<String> extractAllergyCodes(List<PythonMenuAllergyResultDto> allergies) {
        Set<String> set = new HashSet<>();
        for (PythonMenuAllergyResultDto allergy : allergies) {
            if (allergy != null && allergy.allergyCode() != null && !allergy.allergyCode().isBlank()) set.add(allergy.allergyCode());
        }
        return set;
    }

    private Map<String, String> loadIngredientNames(String lang, Set<String> codes) {
        if (codes.isEmpty()) return Map.of();
        String sql = """
                select i.code, coalesce(it.name, i.name) as name
                from ingredient i
                left join ingredient_translation it on it.ingredient_code=i.code and it.lang_code=:lang
                where i.code in (:codes)
                """;
        return toNameMap(sql, lang, codes);
    }

    private Map<String, String> loadAllergyNames(String lang, Set<String> codes) {
        if (codes.isEmpty()) return Map.of();
        String sql = """
                select a.code, coalesce(at.name, a.name) as name
                from allergy a
                left join allergy_translation at on at.allergy_code=a.code and at.lang_code=:lang
                where a.code in (:codes)
                """;
        return toNameMap(sql, lang, codes);
    }

    private Map<String, String> toNameMap(String sql, String lang, Set<String> codes) {
        Map<String, String> map = new HashMap<>();
        jdbc.query(
                sql,
                new MapSqlParameterSource().addValue("lang", lang).addValue("codes", codes),
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> map.put(rs.getString("code"), rs.getString("name"))
        );
        return map;
    }

    private Map<String, List<RestrictionMatch>> findRestrictionMatches(List<String> religiousCodes, Set<String> ingredientCodes, String lang) {
        if (religiousCodes.isEmpty() || ingredientCodes.isEmpty()) return Map.of();
        String sql = """
                select rfri.ingredient_code, rfri.religious_food_restriction_code as code, coalesce(rfrt.name, rfr.name) as name
                from religious_food_restriction_ingredient rfri
                join religious_food_restriction rfr on rfr.code = rfri.religious_food_restriction_code
                left join religious_food_restriction_translation rfrt
                  on rfrt.religious_food_restriction_code = rfr.code and rfrt.lang_code = :lang
                where rfri.religious_food_restriction_code in (:codes)
                  and rfri.ingredient_code in (:ingredientCodes)
                """;
        Map<String, List<RestrictionMatch>> map = new HashMap<>();
        jdbc.query(sql, new MapSqlParameterSource().addValue("lang", lang).addValue("codes", religiousCodes).addValue("ingredientCodes", ingredientCodes), rs -> {
            map.computeIfAbsent(rs.getString("ingredient_code"), k -> new ArrayList<>())
                    .add(new RestrictionMatch(rs.getString("code"), rs.getString("name")));
        });
        return map;
    }

    private List<PythonMenuIngredientResultDto> loadStoredIngredients(Long analysisId) {
        String sql = "select ingredient_code, confidence from menu_ai_analysis_ingredient where menu_ai_analysis_id=:id";
        return jdbc.query(sql, new MapSqlParameterSource("id", analysisId), (rs, rowNum) ->
                new PythonMenuIngredientResultDto(rs.getString("ingredient_code"), rs.getBigDecimal("confidence"))
        );
    }
    private List<PythonMenuAllergyResultDto> loadStoredAllergies(Long analysisId) {
        String sql = "select allergy_code, confidence from menu_ai_analysis_allergy where menu_ai_analysis_id=:id";
        return jdbc.query(sql, new MapSqlParameterSource("id", analysisId), (rs, rowNum) ->
                new PythonMenuAllergyResultDto(rs.getString("allergy_code"), rs.getBigDecimal("confidence"))
        );
    }
    private Long loadMenuSpicyLevel(Long menuId) {
        return jdbc.query("select spicy_level from menu where id=:id", new MapSqlParameterSource("id", menuId), rs -> rs.next() ? rs.getLong("spicy_level") : null);
    }

    private String uploadOrFail(MenuImageAnalysisLog log, Long userId, MultipartFile image) {
        try {
            return menuImageStoragePort.upload(userId, image);
        } catch (Exception e) {
            failLog(log.getId(), "COM_001");
            throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR, e);
        }
    }

    private void validateFileOrFail(MenuImageAnalysisLog log, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            failLog(log.getId(), "COM_001");
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        if (image.getSize() > properties.getMenuImage().getMaxFileSizeBytes()) {
            failLog(log.getId(), "COM_001");
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        String contentType = image.getContentType();
        if (contentType == null || !properties.getMenuImage().getAllowedContentTypes().contains(contentType)) {
            failLog(log.getId(), "COM_001");
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
    }

    @Transactional
    protected MenuImageAnalysisLog createProcessingLog(Long userId) {
        return logRepository.save(MenuImageAnalysisLog.createProcessing(userId));
    }

    @Transactional
    protected void saveStoragePath(Long logId, String path) {
        MenuImageAnalysisLog log = logRepository.findById(logId).orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        log.updateImageStoragePath(path);
        logRepository.save(log);
    }

    @Transactional
    protected void failLog(Long logId, String errorCode) {
        MenuImageAnalysisLog log = logRepository.findById(logId).orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        log.markFailed(errorCode);
        logRepository.save(log);
    }

    @Transactional
    protected void markSuccess(Long logId, MenuImageAnalysisResultSource source, PythonMenuImageAnalysisResultDto identified, String fallbackJson) {
        MenuImageAnalysisLog log = logRepository.findById(logId).orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        log.markSuccess(source, identified.identifiedFoodName(), identified.confidence(), identified.identifiedFoodNameReason(), fallbackJson);
        logRepository.save(log);
    }

    private record RestrictionMatch(String code, String name) {
    }
}
