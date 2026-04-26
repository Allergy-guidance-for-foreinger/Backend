# 공통 작업 로그

## 기록 원칙
- 기능 비특화 작업(문서 체계, 공통 인프라, 저장소 규칙)은 이 문서에 기록한다.
- 기능 상세 구현 변경은 각 기능 `work-log`에 기록한다.

## 최근 공통 작업

### 2026-04-24
- 문서 구조를 기능 중심으로 분리(`docs/features`, `docs/work-log`)
- `AGENTS.md` 읽기 순서 및 인코딩 보호 규칙 정비
- 공통 문서(`project-context`, `database-context`, `work-context`)를 규칙/안내 중심으로 재정리

### 2026-04-24 (features 맥락 파일 보강)
- 기능별 맥락 문서를 실작업 기준으로 보강
- `mealcrawl-context.md`, `authdebug-context.md` 신규 추가
- 공통 문서의 기능별 상세 맥락을 기능 문서로 이관

### 2026-04-25 (mealcrawl Redis weekly cache refresh)
- What changed:
  - Added weekly meal cache flow after successful meal import in `MealCrawlOrchestrationService`.
  - Added cache refresh service `WeeklyMealCacheRefreshService` and cache port `WeeklyMealCachePort`.
  - Added Redis adapter `RedisWeeklyMealCacheAdapter` with key format `meal:weekly:{schoolId}:{cafeteriaId}:{weekStartDate}` and TTL-based `SET` overwrite.
  - Added weekly DB read query for cache payload via `MealCrawlPersistencePort` and `MealMenuJpaRepository` query.
  - Updated cache query scope from school-level to cafeteria-level to avoid mixed menu data when a school has multiple cafeterias.
  - Extended cache DTO menu item with `spicyLevel` and `aiAnalyzed` fields.
  - Replaced cache menu identifier from `menuId` to `mealMenuId` to represent schedule-specific menu instances.
  - Added tests for key format, JSON cache write, and failure isolation behavior.
- Why:
  - Keep Python server responsibility limited to crawl data delivery, and handle Redis cache refresh inside Spring mealcrawl flow.
  - Ensure DB import success is not rolled back or treated as failure when Redis cache refresh fails.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/port/MealCrawlPersistencePort.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/port/WeeklyMealCachePort.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealCrawlOrchestrationService.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealCacheRefreshService.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/dto/WeeklyMealCacheRow.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/dto/WeeklyMealCachePayload.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/adapter/MealCrawlPersistenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/MealMenuJpaRepository.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/redis/RedisWeeklyMealCacheAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/config/MealCrawlProperties.java`
  - `src/main/resources/application.properties`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealCrawlOrchestrationServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealImportServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MenuAiAnalysisFollowUpServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MenuTranslationFollowUpServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealCacheRefreshServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/redis/RedisWeeklyMealCacheAdapterTest.java`
- DB schema changed: No
- API behavior changed: No public API contract change (internal crawl pipeline enhancement only)
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - Run full test suite in local environment where Maven wrapper is executable.

### 2026-04-25 (weekly meal response with user risk evaluation)
- What changed:
  - Added mealcrawl weekly meal query API controller in `mealcrawl.presentation.controller`.
  - Added final response DTO `WeeklyMealResponse` with `mealMenuId`, `spicyLevel`, `aiAnalyzed`, and `risk`.
  - Added `MenuRiskLevel` enum and `WeeklyMealResponseAssembler` for per-menu risk evaluation.
  - Added Redis cache read (hit/miss) flow and DB fallback flow in `WeeklyMealQueryService`.
  - Added mealcrawl user preference adapter to load user school/religious/allergy settings through existing ports.
  - Extended persistence port/adapter with risk evaluation queries:
    - cafeteria-school validation
    - confirmed ingredients by mealMenuId
    - AI ingredients by mealMenuId
    - allergy/religious restriction ingredient mappings
  - Kept weekly cache payload user-agnostic and computed user-specific risk at response time.
- Why:
  - Redis cache should store common weekly meal data only, while risk must be calculated per authenticated user setting.
  - `mealMenuId` is the correct UI/menu-instance identifier for weekly schedule responses.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/presentation/controller/WeeklyMealController.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/presentation/swagger/WeeklyMealApi.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/presentation/dto/response/WeeklyMealResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealQueryService.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealResponseAssembler.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealCacheRefreshService.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/port/MealUserPreferencePort.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/port/WeeklyMealCachePort.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/port/MealCrawlPersistencePort.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/dto/CurrentUserMealPreference.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/dto/MealMenuIngredientRow.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/dto/RestrictionIngredientRow.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/MenuRiskLevel.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/adapter/MealCrawlPersistenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/adapter/MealUserPreferenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/CafeteriaJpaRepository.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/redis/RedisWeeklyMealCacheAdapter.java`
  - mealcrawl service tests updated for new persistence port methods
  - new tests: `WeeklyMealResponseAssemblerTest`, `WeeklyMealQueryServiceTest`
- DB schema changed: No
- API behavior changed:
  - Added weekly meal response API for mealcrawl with user-specific risk calculation.
  - Existing APIs were not modified.
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - Execute tests in a local environment where Maven wrapper runs correctly.

### 2026-04-25 (weekly meal response language-aware menu names)
- What changed:
  - Extended `CurrentUserMealPreference` with `languageCode` from `users.language_code`.
  - Added `findTranslatedMenuNamesByMealMenuIds(mealMenuIds, langCode)` to `MealCrawlPersistencePort`.
  - Implemented translation lookup in `MealCrawlPersistenceAdapter` by joining `meal_menu` and `menu_translation`.
  - Updated `WeeklyMealResponseAssembler` to replace `menuName` with translated name when user language is non-Korean, with fallback to cached Korean name.
  - Added/updated test doubles for new persistence port method and added assembler test for translated menu name application.
- Why:
  - Keep Redis weekly payload language-agnostic while returning user-language menu names at response assembly time.
  - Avoid duplicating weekly cache by language and preserve existing cache key strategy.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/dto/CurrentUserMealPreference.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/adapter/MealUserPreferenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/port/MealCrawlPersistencePort.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/adapter/MealCrawlPersistenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealResponseAssembler.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealResponseAssemblerTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealQueryServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealImportServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MenuTranslationFollowUpServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MenuAiAnalysisFollowUpServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealCrawlOrchestrationServiceTest.java`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed:
  - No request/response shape change.
  - `menuName` value can now be localized by user language when translation exists.
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - Run mealcrawl service tests in IDE/local environment and verify translated/non-translated fallback behavior by language.
