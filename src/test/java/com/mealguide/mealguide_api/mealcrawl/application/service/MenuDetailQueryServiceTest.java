package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuLikePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuReadCachePort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import com.mealguide.mealguide_api.review.application.port.MenuReviewPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuLikeTarget;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuDetailBatchResponse;
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
import static org.mockito.Mockito.when;

class MenuDetailQueryServiceTest {

    @Test
    void batchFailsWhenMealMenuIdsEmpty() {
        MenuDetailQueryService service = new MenuDetailQueryService(
                mock(MealUserPreferencePort.class),
                mock(MealCrawlPersistencePort.class),
                defaultCachePort(),
                mock(MenuLikePort.class),
                mock(MenuReviewPort.class),
                defaultProperties(),
                defaultRiskResolver()
        );

        assertThatThrownBy(() -> service.getMenuDetails(1L, List.of()))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BINDING_ERROR);
    }

    @Test
    void batchFailsWhenMealMenuIdsExceedLimit() {
        MenuDetailQueryService service = new MenuDetailQueryService(
                mock(MealUserPreferencePort.class),
                mock(MealCrawlPersistencePort.class),
                defaultCachePort(),
                mock(MenuLikePort.class),
                mock(MenuReviewPort.class),
                defaultProperties(),
                defaultRiskResolver()
        );
        List<Long> ids = java.util.stream.LongStream.rangeClosed(1, 31).boxed().toList();

        assertThatThrownBy(() -> service.getMenuDetails(1L, ids))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BINDING_ERROR);
    }

    @Test
    void batchReturnsMenusInRequestOrderAndDeduplicates() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuLikePort menuLikePort = mock(MenuLikePort.class);
        MenuReviewPort menuReviewPort = mock(MenuReviewPort.class);
        MenuReadCachePort cachePort = defaultCachePort();
        MenuDetailQueryService service = new MenuDetailQueryService(
                preferencePort, persistencePort, cachePort, menuLikePort, menuReviewPort, defaultProperties(), defaultRiskResolver()
        );

        stubPreference(preferencePort);
        when(persistencePort.findMenuDetailsByMealMenuIds(Set.of(20L, 10L))).thenReturn(List.of(
                new MenuDetailRow(10L, 1L, 1L, "Menu-10", "A", 2, 1L, "SUCCESS", 100L),
                new MenuDetailRow(20L, 1L, 2L, "Menu-20", "B", 1, 2L, "SUCCESS", 100L)
        ));
        when(persistencePort.findTranslatedMenuNamesByMealMenuIds(Set.of(20L, 10L), "en"))
                .thenReturn(java.util.Map.of(10L, "Menu-10-en", 20L, "Menu-20-en"));
        when(persistencePort.findMenuDescriptionsByMealMenuIds(Set.of(20L, 10L), "en"))
                .thenReturn(java.util.Map.of(20L, "Menu-20 description"));
        when(persistencePort.findConfirmedIngredientsForMenuDetails(Set.of(20L, 10L), "en"))
                .thenReturn(List.of(
                        new MealMenuIngredientRow(10L, "PORK", "Pork"),
                        new MealMenuIngredientRow(20L, "RICE", "Rice")
                ));
        when(persistencePort.findAiIngredientsForMenuDetails(anySet(), eq("en"))).thenReturn(List.of());
        when(persistencePort.findAllergiesByMealMenuIds(Set.of(20L, 10L), "en"))
                .thenReturn(List.of(new MealMenuAllergyRow(10L, "PORK", "Pork", null)));
        when(persistencePort.findAllergiesByMealMenuIds(Set.of(20L, 10L), "ko"))
                .thenReturn(List.of(new MealMenuAllergyRow(10L, "PORK", "돼지고기", null)));
        when(persistencePort.findReligiousMatchedIngredientsByMealMenuIds(Set.of(20L, 10L), List.of("HALAL"), "en"))
                .thenReturn(List.of());
        when(menuLikePort.countLikesByTargets(Set.of(
                new MenuLikeTarget(1L, 1L),
                new MenuLikeTarget(1L, 2L)
        ))).thenReturn(java.util.Map.of(new MenuLikeTarget(1L, 1L), 2L, new MenuLikeTarget(1L, 2L), 1L));
        when(menuLikePort.findLikedTargetsByUser(1L, Set.of(
                new MenuLikeTarget(1L, 1L),
                new MenuLikeTarget(1L, 2L)
        ))).thenReturn(Set.of(new MenuLikeTarget(1L, 2L)));
        when(menuReviewPort.countActiveReviewsByTargets(Set.of(
                new MenuLikeTarget(1L, 1L),
                new MenuLikeTarget(1L, 2L)
        ))).thenReturn(java.util.Map.of(new MenuLikeTarget(1L, 1L), 10L, new MenuLikeTarget(1L, 2L), 7L));

        MenuDetailBatchResponse response = service.getMenuDetails(1L, List.of(20L, 10L, 20L));

        assertThat(response.menus()).extracting(MenuDetailResponse::mealMenuId).containsExactly(20L, 10L);
        assertThat(response.menus().get(0).like().likedByMe()).isTrue();
        assertThat(response.menus().get(1).like().count()).isEqualTo(2L);
        assertThat(response.menus().get(0).review().count()).isEqualTo(7L);
        assertThat(response.menus().get(0).description()).isEqualTo("Menu-20 description");
        assertThat(response.menus().get(1).description()).isNull();
    }

    @Test
    void batchFailsWhenAnyMealMenuIdDoesNotExist() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuLikePort menuLikePort = mock(MenuLikePort.class);
        MenuReviewPort menuReviewPort = mock(MenuReviewPort.class);
        MenuReadCachePort cachePort = defaultCachePort();
        MenuDetailQueryService service = new MenuDetailQueryService(
                preferencePort, persistencePort, cachePort, menuLikePort, menuReviewPort, defaultProperties(), defaultRiskResolver()
        );

        stubPreference(preferencePort);
        when(persistencePort.findMenuDetailsByMealMenuIds(Set.of(10L, 99L))).thenReturn(List.of(
                new MenuDetailRow(10L, 1L, 1L, "Menu-10", "A", 1, 1L, "SUCCESS", 100L)
        ));

        assertThatThrownBy(() -> service.getMenuDetails(1L, List.of(10L, 99L)))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BINDING_ERROR);
    }

    @Test
    void batchIncludesMatchedAndUnmatchedMenusTogether() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuLikePort menuLikePort = mock(MenuLikePort.class);
        MenuReviewPort menuReviewPort = mock(MenuReviewPort.class);
        MenuReadCachePort cachePort = defaultCachePort();
        MenuDetailQueryService service = new MenuDetailQueryService(
                preferencePort, persistencePort, cachePort, menuLikePort, menuReviewPort, defaultProperties(), defaultRiskResolver()
        );

        stubPreference(preferencePort);
        when(persistencePort.findMenuDetailsByMealMenuIds(Set.of(10L, 20L))).thenReturn(List.of(
                new MenuDetailRow(10L, 1L, 1L, "Menu-10", "A", 1, 1L, "SUCCESS", 100L),
                new MenuDetailRow(20L, 1L, 2L, "Menu-20", "B", 2, 2L, "SUCCESS", 100L)
        ));
        when(persistencePort.findTranslatedMenuNamesByMealMenuIds(Set.of(10L, 20L), "en")).thenReturn(java.util.Map.of());
        when(persistencePort.findConfirmedIngredientsForMenuDetails(Set.of(10L, 20L), "en"))
                .thenReturn(List.of(
                        new MealMenuIngredientRow(10L, "PORK", "Pork"),
                        new MealMenuIngredientRow(20L, "RICE", "Rice")
                ));
        when(persistencePort.findAiIngredientsForMenuDetails(anySet(), eq("en"))).thenReturn(List.of());
        when(persistencePort.findAllergiesByMealMenuIds(Set.of(10L, 20L), "en"))
                .thenReturn(List.of(new MealMenuAllergyRow(10L, "PORK", "Pork", null)));
        when(persistencePort.findAllergiesByMealMenuIds(Set.of(10L, 20L), "ko"))
                .thenReturn(List.of(new MealMenuAllergyRow(10L, "PORK", "돼지고기", null)));
        when(persistencePort.findReligiousMatchedIngredientsByMealMenuIds(Set.of(10L, 20L), List.of("HALAL"), "en"))
                .thenReturn(List.of());
        when(menuLikePort.countLikesByTargets(Set.of(
                new MenuLikeTarget(1L, 1L),
                new MenuLikeTarget(1L, 2L)
        ))).thenReturn(java.util.Map.of());
        when(menuLikePort.findLikedTargetsByUser(1L, Set.of(
                new MenuLikeTarget(1L, 1L),
                new MenuLikeTarget(1L, 2L)
        ))).thenReturn(Set.of());
        when(menuReviewPort.countActiveReviewsByTargets(Set.of(
                new MenuLikeTarget(1L, 1L),
                new MenuLikeTarget(1L, 2L)
        ))).thenReturn(java.util.Map.of());

        MenuDetailBatchResponse response = service.getMenuDetails(1L, List.of(10L, 20L));

        MenuDetailResponse first = response.menus().get(0);
        MenuDetailResponse second = response.menus().get(1);
        assertThat(first.matchedAllergies()).hasSize(1);
        assertThat(first.matchedAllergies().get(0).riskLevel()).isEqualTo("DANGER");
        assertThat(second.matchedAllergies()).isEmpty();
    }

    @Test
    void singleStillWorksViaBatchPath() {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        MenuLikePort menuLikePort = mock(MenuLikePort.class);
        MenuReviewPort menuReviewPort = mock(MenuReviewPort.class);
        MenuReadCachePort cachePort = defaultCachePort();
        MenuDetailQueryService service = new MenuDetailQueryService(
                preferencePort, persistencePort, cachePort, menuLikePort, menuReviewPort, defaultProperties(), defaultRiskResolver()
        );

        stubPreference(preferencePort);
        when(persistencePort.findMenuDetailsByMealMenuIds(Set.of(10L))).thenReturn(List.of(
                new MenuDetailRow(10L, 1L, 1L, "Menu-10", "A", 1, 1L, "SUCCESS", 100L)
        ));
        when(persistencePort.findTranslatedMenuNamesByMealMenuIds(Set.of(10L), "en"))
                .thenReturn(java.util.Map.of(10L, "Menu-10-en"));
        when(persistencePort.findMenuDescriptionsByMealMenuIds(Set.of(10L), "en"))
                .thenReturn(java.util.Map.of(10L, "English description"));
        when(persistencePort.findConfirmedIngredientsForMenuDetails(Set.of(10L), "en"))
                .thenReturn(List.of(new MealMenuIngredientRow(10L, "RICE", "Rice")));
        when(persistencePort.findAiIngredientsForMenuDetails(anySet(), eq("en"))).thenReturn(List.of());
        when(persistencePort.findAllergiesByMealMenuIds(Set.of(10L), "en")).thenReturn(List.of());
        when(persistencePort.findAllergiesByMealMenuIds(Set.of(10L), "ko")).thenReturn(List.of());
        when(persistencePort.findReligiousMatchedIngredientsByMealMenuIds(Set.of(10L), List.of("HALAL"), "en")).thenReturn(List.of());
        when(menuLikePort.countLikesByTargets(Set.of(new MenuLikeTarget(1L, 1L))))
                .thenReturn(java.util.Map.of(new MenuLikeTarget(1L, 1L), 5L));
        when(menuLikePort.findLikedTargetsByUser(1L, Set.of(new MenuLikeTarget(1L, 1L))))
                .thenReturn(Set.of(new MenuLikeTarget(1L, 1L)));
        when(menuReviewPort.countActiveReviewsByTargets(Set.of(new MenuLikeTarget(1L, 1L))))
                .thenReturn(java.util.Map.of(new MenuLikeTarget(1L, 1L), 3L));

        MenuDetailResponse response = service.getMenuDetail(1L, 10L);

        assertThat(response.mealMenuId()).isEqualTo(10L);
        assertThat(response.menuName()).isEqualTo("Menu-10-en");
        assertThat(response.description()).isEqualTo("English description");
        assertThat(response.like().count()).isEqualTo(5L);
        assertThat(response.like().likedByMe()).isTrue();
        assertThat(response.review().count()).isEqualTo(3L);
    }

    private void stubPreference(MealUserPreferencePort preferencePort) {
        when(preferencePort.getCurrentUserMealPreference(1L))
                .thenReturn(new CurrentUserMealPreference(1L, 100L, "en", List.of("HALAL"), List.of("PORK")));
    }

    private RiskLevelPolicyResolver defaultRiskResolver() {
        return new RiskLevelPolicyResolver(defaultProperties());
    }

    private MealCrawlProperties defaultProperties() {
        return new MealCrawlProperties();
    }

    private MenuReadCachePort defaultCachePort() {
        MenuReadCachePort cachePort = mock(MenuReadCachePort.class);
        when(cachePort.findMenuDetailBase(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
        when(cachePort.findMenuDetailRiskData(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(Optional.empty());
        when(cachePort.findReligionIngredientMap())
                .thenReturn(Optional.empty());
        return cachePort;
    }
}
