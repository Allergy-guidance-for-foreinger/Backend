package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MatchedAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.NamedIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuDetailResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuDetailQueryServiceTest {

    @Test
    void confirmedIngredientsArePrioritizedWithConfirmedSource() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuDetailQueryService service = new MenuDetailQueryService(preferencePort, persistencePort);

        stubCommon(preferencePort, persistencePort, "en");
        when(persistencePort.findConfirmedIngredientsForMenuDetail(10L, "en"))
                .thenReturn(List.of(new NamedIngredientRow("PORK", "Pork")));

        MenuDetailResponse response = service.getMenuDetail(1L, 10L);

        assertThat(response.ingredients()).hasSize(1);
        assertThat(response.ingredients().get(0).source()).isEqualTo("CONFIRMED");
        verify(persistencePort, never()).findAiIngredientsForMenuDetail(10L, "en");
    }

    @Test
    void aiSuccessIngredientsAreUsedWhenNoConfirmedIngredients() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuDetailQueryService service = new MenuDetailQueryService(preferencePort, persistencePort);

        stubCommon(preferencePort, persistencePort, "en");
        when(persistencePort.findConfirmedIngredientsForMenuDetail(10L, "en")).thenReturn(List.of());
        when(persistencePort.findAiIngredientsForMenuDetail(10L, "en"))
                .thenReturn(List.of(new NamedIngredientRow("PORK", "Pork")));

        MenuDetailResponse response = service.getMenuDetail(1L, 10L);

        assertThat(response.ingredients()).hasSize(1);
        assertThat(response.ingredients().get(0).source()).isEqualTo("AI");
    }

    @Test
    void allergyMatchReturnsMatchedAllergiesAndDanger() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuDetailQueryService service = new MenuDetailQueryService(preferencePort, persistencePort);

        stubCommon(preferencePort, persistencePort, "en");
        when(persistencePort.findConfirmedIngredientsForMenuDetail(10L, "en"))
                .thenReturn(List.of(new NamedIngredientRow("PORK", "Pork")));
        when(persistencePort.findMatchedAllergies(1L, Set.of("PORK"), "en"))
                .thenReturn(List.of(new MatchedAllergyRow("PORK", "Pork", "PORK", "Pork")));

        MenuDetailResponse response = service.getMenuDetail(1L, 10L);

        assertThat(response.risk().riskLevel()).isEqualTo("DANGER");
        assertThat(response.matchedAllergies()).hasSize(1);
        assertThat(response.matchedAllergies().get(0).message()).isEqualTo("Ingredient matching my allergy: Pork");
    }

    @Test
    void noAllergyOrReligionMatchReturnsSafe() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuDetailQueryService service = new MenuDetailQueryService(preferencePort, persistencePort);

        stubCommon(preferencePort, persistencePort, "en");
        when(persistencePort.findConfirmedIngredientsForMenuDetail(10L, "en"))
                .thenReturn(List.of(new NamedIngredientRow("RICE", "Rice")));

        MenuDetailResponse response = service.getMenuDetail(1L, 10L);

        assertThat(response.risk().riskLevel()).isEqualTo("SAFE");
    }

    @Test
    void noIngredientsReturnsUnknown() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuDetailQueryService service = new MenuDetailQueryService(preferencePort, persistencePort);

        stubCommon(preferencePort, persistencePort, "en");
        when(persistencePort.findConfirmedIngredientsForMenuDetail(10L, "en")).thenReturn(List.of());
        when(persistencePort.findAiIngredientsForMenuDetail(10L, "en")).thenReturn(List.of());

        MenuDetailResponse response = service.getMenuDetail(1L, 10L);

        assertThat(response.risk().riskLevel()).isEqualTo("UNKNOWN");
        assertThat(response.ingredients()).isEmpty();
    }

    @Test
    void koreanLanguageReturnsKoreanMatchedMessage() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuDetailQueryService service = new MenuDetailQueryService(preferencePort, persistencePort);

        stubCommon(preferencePort, persistencePort, "ko");
        when(persistencePort.findConfirmedIngredientsForMenuDetail(10L, "ko"))
                .thenReturn(List.of(new NamedIngredientRow("PORK", "돼지고기")));
        when(persistencePort.findMatchedAllergies(1L, Set.of("PORK"), "ko"))
                .thenReturn(List.of(new MatchedAllergyRow("PORK", "돼지고기", "PORK", "돼지고기")));

        MenuDetailResponse response = service.getMenuDetail(1L, 10L);

        assertThat(response.matchedAllergies().get(0).message()).isEqualTo("내 알러지와 겹치는 식재료: 돼지고기");
    }

    @Test
    void fallsBackToDefaultNamesWhenTranslationMissing() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuDetailQueryService service = new MenuDetailQueryService(preferencePort, persistencePort);

        stubCommon(preferencePort, persistencePort, "en");
        when(persistencePort.findTranslatedMenuNameByMealMenuId(10L, "en")).thenReturn(Optional.empty());

        MenuDetailResponse response = service.getMenuDetail(1L, 10L);

        assertThat(response.menuName()).isEqualTo("돈불고기");
    }

    @Test
    void accessingOtherSchoolMenuThrowsException() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuDetailQueryService service = new MenuDetailQueryService(preferencePort, persistencePort);

        when(preferencePort.getCurrentUserMealPreference(1L))
                .thenReturn(new CurrentUserMealPreference(1L, 100L, "en", "HALAL", List.of("PORK")));
        when(persistencePort.findMenuDetailByMealMenuId(10L))
                .thenReturn(Optional.of(new MenuDetailRow(10L, 5L, "돈불고기", "한식", 1, 1L, "SUCCESS", 999L)));

        assertThatThrownBy(() -> service.getMenuDetail(1L, 10L))
                .isInstanceOf(ServiceException.class);
    }

    private void stubCommon(
            MealUserPreferencePort preferencePort,
            MealCrawlPersistencePort persistencePort,
            String languageCode
    ) {
        when(preferencePort.getCurrentUserMealPreference(1L))
                .thenReturn(new CurrentUserMealPreference(1L, 100L, languageCode, "HALAL", List.of("PORK")));
        when(persistencePort.findMenuDetailByMealMenuId(10L))
                .thenReturn(Optional.of(new MenuDetailRow(10L, 5L, "돈불고기", "한식", 1, 1L, "SUCCESS", 100L)));
        when(persistencePort.findTranslatedMenuNameByMealMenuId(10L, languageCode))
                .thenReturn(Optional.of("돈불고기"));
        when(persistencePort.findMatchedAllergies(eq(1L), anySet(), eq(languageCode)))
                .thenReturn(List.of());
        when(persistencePort.findReligiousRestrictionIngredients("HALAL")).thenReturn(List.of());
        when(persistencePort.findConfirmedIngredientsForMenuDetail(10L, languageCode))
                .thenReturn(List.of(new NamedIngredientRow("RICE", "Rice")));
    }
}
