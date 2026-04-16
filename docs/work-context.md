# Work Context Log

This document records implementation history and follow-up context.

## Writing Rules
- Record the scope and result of each task clearly.
- List affected layers and changed files.
- State DB impact explicitly.
- Note related document updates and remaining issues.

---

## 2026-04-15 (12)

**Task**
- Add DB-included integration test for meal import duplicate-safe/upsert behavior.

**Affected Layers**
- `mealcrawl` test layer
- build dependency (`pom.xml`)
- `docs/work-context.md`

**Changed Files**
- `pom.xml`
- `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealImportServiceDatabaseIntegrationTest.java`
- `docs/work-context.md`

**Why**
- Unit tests alone cannot verify unique constraint collision behavior and real DB upsert semantics.
- Need integration-level proof that repeated import for same schedule is idempotent and `meal_menu` is updated safely.

**DB Impact**
- Schema changed by this task: No
- Test-only embedded DB usage: Yes (`h2` test dependency added).

**API Impact**
- External API behavior changed: No

**Implementation Notes**
- Added `@DataJpaTest` integration test with real JPA repositories + `MealCrawlPersistenceAdapter` + `MealImportService`.
- Test seeds `school` and `cafeteria`, executes same-day import twice with changed menu at same `display_order`.
- Verified:
  - `meal_schedule` count remains `1`
  - `meal_menu` count remains `1`
  - `meal_menu.menu_id` reflects latest crawl menu (upsert update path)
  - no unique collision failure in repeated import flow

**Tests**
- New integration test class added.
- Local execution still not run in this shell due Maven wrapper issue (`Cannot start maven from wrapper`).

**Documentation Updates**
- Updated `docs/work-context.md`.

---

## 2026-04-15 (11)

**Task**
- Rewrite broken Swagger texts in specific files to Korean as requested.

**Affected Layers**
- `login.presentation.swagger`
- `settings.presentation.swagger`
- `onboarding.presentation.swagger`
- `global.config.swagger`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/swagger/OnboardingApi.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/swagger/SwaggerApiSuccessResponseHandler.java`
- `docs/work-context.md`

**Why**
- User requested Korean Swagger descriptions instead of English fallback text.
- Previous encoding issue recovery left those files in English for stability; this step restores Korean wording.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Endpoint behavior changed: No
- Swagger summary/description text changed to Korean.

**Tests**
- No runtime logic change.
- Local compile/test execution still blocked in this shell (`Cannot start maven from wrapper`).

**Documentation Updates**
- Updated `docs/work-context.md`.

---

## 2026-04-15 (10)

**Task**
- Fix broken/mojibake text in Swagger-related files.

**Affected Layers**
- `login.presentation.swagger`
- `settings.presentation.swagger`
- `onboarding.presentation.swagger`
- `global.config.swagger`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/swagger/OnboardingApi.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/swagger/SwaggerApiSuccessResponseHandler.java`
- `docs/work-context.md`

**Why**
- Several files had broken character encoding and malformed string literals, causing unreadable Swagger text and potential compile failures.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Endpoint behavior changed: No
- Swagger operation/description text was normalized.

**Implementation Notes**
- Rewrote the four files in UTF-8 without BOM.
- Normalized operation summaries/descriptions and inline schema descriptions to valid readable text.
- Removed malformed text fragments that broke Java string syntax.

**Tests**
- No runtime logic changes; compile/test execution remains blocked in this environment due Maven wrapper issue (`Cannot start maven from wrapper`).

**Documentation Updates**
- Updated `docs/work-context.md`.

---

## 2026-04-15 (9)

**Task**
- Split Python client DTO package into request/response groups for mealcrawl integration.

**Affected Layers**
- `mealcrawl.infrastructure.client.dto`
- `mealcrawl.application.port`
- `mealcrawl.application.service`
- `mealcrawl.infrastructure.client`
- `mealcrawl` tests
- `docs/work-context.md`

**Changed Files**
- Moved request DTOs to:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/request/PythonMealCrawlRequest.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/request/PythonMenuAnalysisRequest.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/request/PythonMenuAnalysisTargetDto.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/request/PythonMenuTranslationRequest.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/request/PythonMenuTranslationTargetDto.java`
- Moved response DTOs to:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/response/PythonMealCrawlResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/response/PythonDailyMealDto.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/response/PythonCrawledMenuDto.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/response/PythonMenuAnalysisResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/response/PythonMenuAnalysisResultDto.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/response/PythonMenuIngredientResultDto.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/response/PythonMenuTranslationResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/response/PythonMenuTranslationResultDto.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/response/PythonTranslatedMenuNameDto.java`
- Updated imports in:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/port/PythonMealClientPort.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/*`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/PythonMealClientAdapter.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/**/*`
- `docs/work-context.md`

**Why**
- Clarify external integration contract boundaries by separating outbound request payloads and inbound response payloads.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External endpoint behavior changed: No
- Internal package path/import structure changed only.

**Tests**
- Test source imports updated to new DTO package paths.
- Local test execution remains blocked in this environment (`Cannot start maven from wrapper`).

**Documentation Updates**
- Updated `docs/work-context.md`.

---

## 2026-04-15 (8)

**Task**
- Implement synchronous Java-to-Python meal crawling integration with DB import, AI analysis follow-up, and translation follow-up.

**Affected Layers**
- `mealcrawl.application.port`
- `mealcrawl.application.service`
- `mealcrawl.domain`
- `mealcrawl.infrastructure.client`
- `mealcrawl.infrastructure.config`
- `mealcrawl.infrastructure.persistence.repository`
- `mealcrawl.infrastructure.persistence.adapter`
- `onboarding.domain` (school mapping field sync)
- application bootstrap/config
- tests
- docs

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/MealguideApiApplication.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/domain/School.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/dto/MealCrawlTarget.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/dto/MealImportResult.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/port/PythonMealClientPort.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/port/MealCrawlPersistencePort.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealCrawlTargetService.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealImportService.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/MenuAiAnalysisFollowUpService.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/MenuTranslationFollowUpService.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealCrawlOrchestrationService.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealCrawlScheduler.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/CrawlTargetSource.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/MenuIngredientCandidate.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/MenuTranslationKey.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/Cafeteria.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/MealSchedule.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/Menu.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/MealMenu.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/MenuAiAnalysis.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/MenuAiAnalysisIngredient.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/MenuAiAnalysisIngredientId.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/MenuTranslation.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/domain/MealScheduleCrawlHistory.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/PythonMealClientAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/dto/*` (crawl/analyze/translate request-response DTO records)
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/config/MealCrawlProperties.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/CafeteriaJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/MealScheduleJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/MenuJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/MealMenuJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/MenuAiAnalysisJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/MenuAiAnalysisIngredientJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/MenuTranslationJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/MealScheduleCrawlHistoryJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/adapter/MealCrawlPersistenceAdapter.java`
- `src/main/resources/application.properties`
- `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealImportServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MenuAiAnalysisFollowUpServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MenuTranslationFollowUpServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealCrawlOrchestrationServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/client/PythonMealClientAdapterTest.java`
- `docs/database-context.md`
- `docs/work-context.md`

**Why**
- Scheduler-selected crawl targets must be crawled via Python and immediately persisted in Java DB.
- Follow-up AI analysis and translation are supplementary processing and must not break meal import success.
- Confirmed ingredient tables must remain strictly admin-managed data.

**DB Impact**
- Schema changed by this task: No
- DB write behavior changed: Yes
  - Added import writes for `meal_schedule`, `menu`, `meal_menu`, `meal_schedule_crawl_history`
  - Added follow-up writes for `menu_ai_analysis`, `menu_ai_analysis_ingredient`, `menu_translation`
  - No automatic writes to `meal_menu_confirmed_ingredient` or confirmation history

**API Impact**
- External public API endpoint contract changed: No
- Added internal external-integration HTTP client calls from Java to Python:
  - crawl
  - menu analysis
  - menu translation

**Implementation Notes**
- Introduced `mealcrawl` feature package with port-service-adapter structure.
- Added `MealCrawlOrchestrationService` to coordinate crawl → import → follow-up sequence.
- Follow-up failures are isolated with try/catch so meal import success/crawl history success remain intact.
- Added scheduler entry (`MealCrawlScheduler`) and enabled scheduling on application bootstrap.
- Added configurable properties for Python base URL/paths, scheduler toggle/cron, target languages.

**Tests**
- Added service-level tests for:
  - meal import success path
  - menu reuse behavior
  - analysis target filtering
  - translation target filtering
  - orchestration resilience when follow-up fails
- Added Python client mapping unit test via `RestClient` mocking.
- Local Maven compile/test execution remains blocked in this environment (`Cannot start maven from wrapper`).

**Documentation Updates**
- Updated `docs/database-context.md`.
- Updated `docs/work-context.md`.

---

## 2026-04-15 (7)

**Task**
- Convert onboarding Swagger texts to Korean.

**Affected Layers**
- `onboarding.presentation.swagger`
- `onboarding.presentation.dto.request`
- `onboarding.presentation.dto.response`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/swagger/OnboardingApi.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/request/CompleteOnboardingRequest.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/response/CompleteOnboardingResponse.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/response/SchoolListResponse.java`
- `docs/work-context.md`

**Why**
- Onboarding API/DTO Swagger descriptions should be shown in Korean consistently with current project documentation style.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Endpoint behavior and response contract changed: No
- Swagger summary/description/schema texts changed to Korean.

**Tests**
- Documentation annotation text change only. No additional runtime test executed.

**Documentation Updates**
- Updated `docs/work-context.md`.

---

## 2026-04-15 (6)

**Task**
- Strengthen login onboarding propagation test to prevent false-positive pass with hardcoded `false`.

**Affected Layers**
- `login` tests
- `docs/work-context.md`

**Changed Files**
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `docs/work-context.md`

**Why**
- Existing login tests only used users with `onboardingCompleted=false`.
- In that shape, a buggy implementation that always returns `false` could still pass.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Updated existing-user login success test to use `onboardingCompleted=true` and assert propagation.
- Extended `createUser(...)` test helper to inject `onboardingCompleted` explicitly for each scenario.

**Tests**
- Test code updated, but local execution remains blocked by Maven wrapper runtime issue (`Cannot start maven from wrapper`).

**Documentation Updates**
- Updated `docs/work-context.md`.

---

## 2026-04-15 (5)

**Task**
- Remove duplicate `users` table mapping in onboarding and reuse the existing login `User` entity mapping via update query.

**Affected Layers**
- `onboarding.application.port`
- `onboarding.application.service`
- `onboarding.infrastructure.persistence.adapter`
- `onboarding.infrastructure.persistence.repository`
- `onboarding.domain`
- `onboarding` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/onboarding/application/port/OnboardingCommandPort.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/application/service/OnboardingService.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/infrastructure/persistence/adapter/SchoolPersistenceAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/infrastructure/persistence/repository/OnboardingUserJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/domain/OnboardingUser.java` (deleted)
- `src/test/java/com/mealguide/mealguide_api/onboarding/application/service/OnboardingServiceTest.java`
- `docs/work-context.md`

**Why**
- Mapping the same `users` row as both `login.User` and `onboarding.OnboardingUser` can introduce persistence-context inconsistency risk.
- The onboarding write path should use a single `users` entity mapping and execute a targeted update query.

**DB Impact**
- Schema changed by this task: No
- DB write behavior changed: No functional change. Same columns are updated.

**API Impact**
- External endpoint contract changed: No

**Implementation Notes**
- Replaced `findActiveUserById` entity-load flow with `existsActiveUserById` and `completeOnboarding` update command.
- `OnboardingUserJpaRepository` now targets `login.domain.User` and performs `@Modifying` update for onboarding completion fields.
- Added update result check (`updated row count > 0`) to guard against concurrent state changes.

**Tests**
- Updated onboarding service fake port to the new command method signatures.
- Local test execution remains blocked by Maven wrapper runtime issue (`Cannot start maven from wrapper`).

**Documentation Updates**
- Updated `docs/work-context.md`.

---

## 2026-04-15 (4)

**Task**
- Extend onboarding completion endpoint to save language selection together with school, allergies, and religion.

**Affected Layers**
- `onboarding.presentation.dto.request`
- `onboarding.presentation.dto.response`
- `onboarding.presentation.controller`
- `onboarding.presentation.swagger`
- `onboarding.application.service`
- `onboarding.application.port`
- `onboarding.domain`
- `onboarding.infrastructure.persistence.adapter`
- `onboarding.infrastructure.persistence.repository`
- `onboarding` tests
- `docs/database-context.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/request/CompleteOnboardingRequest.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/response/CompleteOnboardingResponse.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/controller/OnboardingController.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/swagger/OnboardingApi.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/application/service/OnboardingService.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/application/port/OnboardingCommandPort.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/domain/OnboardingCompletion.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/domain/OnboardingUser.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/infrastructure/persistence/adapter/SchoolPersistenceAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/infrastructure/persistence/repository/OnboardingUserJpaRepository.java`
- `src/test/java/com/mealguide/mealguide_api/onboarding/application/service/OnboardingServiceTest.java`
- `docs/database-context.md`
- `docs/work-context.md`

**Why**
- Onboarding flow now requires language selection at completion time, so the endpoint must validate and persist `users.language_code` in the same transaction.

**DB Impact**
- Schema changed by this task: No
- DB write behavior changed: Yes
  - Added update target: `users.language_code`
  - Existing targets remain: `users.school_id`, `users.religious_code`, `users.onboarding_completed`, `user_allergy`

**API Impact**
- `POST /api/v1/onboarding/complete` request now requires:
  - `languageCode`
- Response now includes:
  - `languageCode`

**Implementation Notes**
- Added language-code existence check through onboarding persistence port.
- Onboarding completion now updates user language, school, religious code, and onboarding flag atomically.

**Tests**
- Updated onboarding service test to pass and verify `languageCode`.
- Local test execution remains blocked by Maven wrapper runtime issue (`Cannot start maven from wrapper`).

**Documentation Updates**
- Updated `docs/database-context.md`.
- Updated `docs/work-context.md`.

---

## 2026-04-15 (3)

**Task**
- Add onboarding completion save endpoint to `OnboardingController`.

**Affected Layers**
- `onboarding.presentation.controller`
- `onboarding.presentation.dto.request`
- `onboarding.presentation.dto.response`
- `onboarding.presentation.swagger`
- `onboarding.application.service`
- `onboarding.application.port`
- `onboarding.domain`
- `onboarding.infrastructure.persistence.adapter`
- `onboarding.infrastructure.persistence.repository`
- `onboarding` tests
- `docs/database-context.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/controller/OnboardingController.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/swagger/OnboardingApi.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/request/CompleteOnboardingRequest.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/response/CompleteOnboardingResponse.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/application/service/OnboardingService.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/application/port/OnboardingCommandPort.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/domain/OnboardingCompletion.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/domain/OnboardingUser.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/domain/OnboardingUserAllergy.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/domain/OnboardingUserAllergyId.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/infrastructure/persistence/adapter/SchoolPersistenceAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/infrastructure/persistence/repository/OnboardingUserJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/onboarding/infrastructure/persistence/repository/OnboardingUserAllergyJpaRepository.java`
- `src/test/java/com/mealguide/mealguide_api/onboarding/application/service/OnboardingServiceTest.java`
- `docs/database-context.md`
- `docs/work-context.md`

**Why**
- Onboarding screen now submits school, allergy, and religious restriction in one request, so backend needs a single transactional save endpoint.

**DB Impact**
- Schema changed by this task: No
- DB write behavior changed: Yes
  - `users.school_id`, `users.religious_code`, `users.onboarding_completed`
  - `user_allergy` full replacement

**API Impact**
- Added authenticated endpoint:
  - `POST /api/v1/onboarding/complete`
- Request body:
  - `schoolId`
  - `allergyCodes`
  - `religiousCode` (nullable)
- Response body:
  - `schoolId`
  - `allergyCodes`
  - `religiousCode`
  - `onboardingCompleted`

**Implementation Notes**
- Added onboarding command port and persistence queries for active user lookup and master-code validation.
- Implemented transactional onboarding completion in `OnboardingService`.
- Kept school list endpoint public and made completion endpoint role-protected (`USER`, `MANAGER`, `ADMIN`) in controller.

**Tests**
- Updated `OnboardingServiceTest` to include onboarding completion success/failure cases.
- Local test execution remains blocked by Maven wrapper runtime issue (`Cannot start maven from wrapper`).

**Documentation Updates**
- Updated `docs/database-context.md`.
- Updated `docs/work-context.md`.

---

## 2026-04-15 (2)

**Task**
- Include user onboarding completion state in login success response.

**Affected Layers**
- `login.application.service`
- `login.presentation.dto.response`
- `global.auth.jwt.dto`
- `login` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/auth/jwt/dto/AuthTokenResult.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/dto/response/AuthResponse.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `docs/work-context.md`

**Why**
- First-login flow uses auto-signup and the frontend needs onboarding state immediately after login to decide whether to route to settings/onboarding.

**DB Impact**
- Schema changed by this task: No
- `users.onboarding_completed` already exists in `docs/schema.sql` and was reused.

**API Impact**
- `POST /auth/login` response now includes `onboardingCompleted`.
- `POST /auth/refresh` keeps token reissue behavior and returns `onboardingCompleted = null`.

**Implementation Notes**
- Added `onboardingCompleted` to login token result model and auth response DTO.
- Login flow now sets onboarding value from `User.onboardingCompleted`.
- Refresh flow keeps existing user-existence check path and does not load onboarding state.

**Tests**
- Updated `LoginServiceTest` assertions for onboarding value.
- Local test execution remains blocked by Maven wrapper runtime issue (`Cannot start maven from wrapper`).

**Documentation Updates**
- Updated `docs/work-context.md`.

---

## 2026-04-15 (1)

**Task**
- Make duplicated date-only H2 headings unique to resolve Markdown MD024 warnings.

**Affected Layers**
- `docs/work-context.md`

**Changed Files**
- `docs/work-context.md`

**Why**
- Repeated identical H2 headings (`## 2026-04-14`, `## 2026-04-06`) caused `MD024/no-duplicate-heading`.
- Adding per-date sequence suffixes improves lint stability and section navigation.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Converted date-only H2 headings to unique format: `## YYYY-MM-DD (n)`.
- Applied numbering sequentially per date across the document.

**Documentation Updates**
- Updated `docs/work-context.md`.

---

## 2026-04-14 (1)

**Task**
- Remove UTF-8 BOM from `SettingsApi` to fix Java compile failure (`illegal character: '\\ufeff'`).

**Affected Layers**
- `settings.presentation.swagger`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `docs/work-context.md`

**Why**
- `SettingsApi.java` was saved with BOM, causing IntelliJ/Javac parse errors before `package` declaration.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Endpoint paths/contracts changed: No
- Compile issue only; Swagger text remains Korean.

**Tests**
- No logic change. Build/test execution was not run in this shell.

**Documentation Updates**
- Updated `docs/work-context.md`.

**Task**
- Fix broken text encoding in `SettingsApi` Swagger descriptions.

**Affected Layers**
- `settings.presentation.swagger`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `docs/work-context.md`

**Why**
- Swagger Korean texts in `SettingsApi` were broken due file encoding corruption.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Endpoint paths and contracts changed: No
- Swagger summary/description texts are restored in readable Korean.

**Tests**
- No runtime logic change; no additional test execution was run.

**Documentation Updates**
- Updated `docs/work-context.md`.

**Task**
- Split settings option-list endpoints into a dedicated controller.

**Affected Layers**
- `settings.presentation.controller`
- `settings.presentation.swagger`
- `settings` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/UserSettingsController.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsOptionsController.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsOptionsApi.java`
- `src/test/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsOptionsControllerTest.java`
- `docs/work-context.md`

**Why**
- The option-list lookup endpoints should be managed separately from personal settings read/update endpoints for clearer controller responsibilities.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Endpoint paths/response contracts changed: No
- Controller ownership changed:
  - `UserSettingsController`: 개인 설정 조회/수정
  - `SettingsOptionsController`: 옵션 목록 조회

**Tests**
- Updated option-list controller test target to `SettingsOptionsController`.
- Local Maven test execution remains blocked by wrapper/runtime issue in current environment.

**Documentation Updates**
- Updated `docs/work-context.md`.

**Task**
- Convert settings Swagger operation texts from English to Korean.

**Affected Layers**
- `settings.presentation.swagger`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `docs/work-context.md`

**Why**
- Swagger UI operation summary/description text should be provided in Korean for the current project usage context.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Endpoint paths and request/response contracts changed: No
- Swagger summary/description and success description texts are now Korean.

**Tests**
- No runtime logic change. Additional test execution was not run in this environment.

**Documentation Updates**
- Updated `docs/work-context.md`.

**Task**
- Add settings option-list APIs for language, allergy, and religion selection screens.

**Affected Layers**
- `settings.presentation.controller`
- `settings.presentation.dto.response`
- `settings.presentation.swagger`
- `settings.application.service` (reused)
- `settings` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/UserSettingsController.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/LanguageOptionItemResponse.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/LanguageOptionsResponse.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/AllergyOptionItemResponse.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/AllergyOptionsResponse.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/ReligionOptionItemResponse.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/ReligionOptionsResponse.java`
- `src/test/java/com/mealguide/mealguide_api/settings/presentation/controller/UserSettingsControllerTest.java`
- `docs/work-context.md`

**Why**
- The settings screen needs full selectable option lists and separate highlighting of user-selected values.
- Allergy and religion labels must be localized using the authenticated user's current language setting.

**DB Impact**
- Schema changed by this task: No
- Existing tables/translation queries are reused through current settings services and ports.

**API Impact**
- Added authenticated endpoints:
  - `GET /api/v1/settings/options/languages`
  - `GET /api/v1/settings/options/allergies`
  - `GET /api/v1/settings/options/religions`
- Response behavior:
  - language options: full list with `code`, `name`, `englishName`
  - allergy options: full list with `code`, `name` localized by user language
  - religion options: full list with `code`, `name` localized by user language
- Existing personal settings endpoints remain code-only and unchanged.

**Tests**
- Added `UserSettingsControllerTest` for option-list endpoint mapping and user-language propagation.
- Maven wrapper execution is still blocked in current shell environment, so local test run was not completed.

**Documentation Updates**
- Updated `docs/work-context.md`.

**Task**
- Revert individual personal settings GET responses back to code-only output.

**Affected Layers**
- `settings.presentation.controller`
- `settings.presentation.swagger`
- `settings.presentation.dto.response`
- `settings.application.service`
- `settings.domain`
- `settings` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceService.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/UserSettingsController.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `src/test/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceServiceTest.java`
- `src/main/java/com/mealguide/mealguide_api/settings/domain/UserLanguagePreference.java` (deleted)
- `src/main/java/com/mealguide/mealguide_api/settings/domain/UserAllergyPreference.java` (deleted)
- `src/main/java/com/mealguide/mealguide_api/settings/domain/UserReligionPreference.java` (deleted)
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/LanguagePreferenceResponse.java` (deleted)
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/AllergyPreferenceItemResponse.java` (deleted)
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/AllergyPreferenceResponse.java` (deleted)
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/ReligionPreferenceResponse.java` (deleted)
- `docs/work-context.md`

**Why**
- The requested product direction is that user personal setting GET APIs should return only stored code values.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Reverted GET response shape to code-only:
  - `GET /api/v1/settings/language` -> `languageCode`
  - `GET /api/v1/settings/allergies` -> `allergyCodes`
  - `GET /api/v1/settings/religion` -> `religiousCode`
- Update endpoint contracts remain unchanged.

**Tests**
- Updated `UserPreferenceServiceTest` expectations back to code-only results.
- Maven wrapper test execution is still blocked in current shell environment.

**Documentation Updates**
- Updated `docs/work-context.md`.

**Task**
- Extend individual settings GET responses to return both code and user-language display name for language, allergy, and religion.

**Affected Layers**
- `settings.presentation.controller`
- `settings.presentation.dto.response`
- `settings.presentation.swagger`
- `settings.application.service`
- `settings.domain`
- `settings` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceService.java`
- `src/main/java/com/mealguide/mealguide_api/settings/domain/UserLanguagePreference.java`
- `src/main/java/com/mealguide/mealguide_api/settings/domain/UserAllergyPreference.java`
- `src/main/java/com/mealguide/mealguide_api/settings/domain/UserReligionPreference.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/UserSettingsController.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/LanguagePreferenceResponse.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/AllergyPreferenceItemResponse.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/AllergyPreferenceResponse.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/ReligionPreferenceResponse.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `src/test/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceServiceTest.java`
- `docs/work-context.md`

**Why**
- The client requested usable display names alongside stored codes for personal settings GET APIs.
- Display names need to follow the authenticated user's language preference rather than returning code-only values.

**DB Impact**
- Schema changed by this task: No
- Existing master/translation tables are reused through current query ports.

**API Impact**
- `GET /api/v1/settings/language` now returns `languageCode` and `languageName`.
- `GET /api/v1/settings/allergies` now returns `allergies` list with `allergyCode` and `allergyName`.
- `GET /api/v1/settings/religion` now returns `religiousCode` and `religiousName`.
- Update endpoints (`PATCH/PUT`) response contracts remain unchanged.

**Implementation Notes**
- Added GET-only preference response DTOs so update response DTOs stay intact.
- `UserPreferenceService` now resolves names using:
  - language master (`language.name` / `language.english_name`)
  - allergy/religion translated option queries with fallback behavior from existing adapters
- Allergy GET response preserves user allergy code ordering and maps each code to its localized display name.

**Tests**
- Updated `UserPreferenceServiceTest` expectations for new GET return models.
- Local test command failed because Maven wrapper execution is broken in the current shell (`Cannot start maven from wrapper`).

**Documentation Updates**
- Updated `docs/work-context.md`.

**Remaining Issues**
- Re-run tests after Maven wrapper/runtime is fixed in the local environment.

**Task**
- Make individual settings lookup operations explicitly bearer-token protected in Swagger.

**Affected Layers**
- `settings.presentation.swagger`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `docs/work-context.md`

**Why**
- Settings update endpoints succeeded with an access token, but individual settings GET execution from Swagger returned an authentication failure.
- The controller already requires `USER`, `MANAGER`, or `ADMIN` by `@PreAuthorize`, so the fix keeps security on the controller and makes the Swagger operations explicitly require the `Access Token` security scheme.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Endpoint paths and response bodies changed: No
- Swagger now marks the individual settings lookup API group as requiring `Access Token`.

**Tests**
- Full Maven verification remains blocked by the current Maven wrapper/runtime issue.

**Documentation Updates**
- Updated `docs/work-context.md`.

**Remaining Issues**
- Restart the running Spring application and refresh Swagger UI before retesting so the OpenAPI document is regenerated.

---

## 2026-04-14 (2)

**Task**
- Replace all-in-one settings lookup with individual personal setting lookup endpoints.

**Affected Layers**
- `settings.presentation.controller`
- `settings.presentation.swagger`
- `settings.application.service`
- `settings.domain`
- `settings` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsController.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `src/main/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceService.java`
- `src/main/java/com/mealguide/mealguide_api/settings/domain/UserSettings.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/UserSettingsResponse.java`
- `src/test/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceServiceTest.java`
- `docs/work-context.md`

**Why**
- The client will hardcode mostly fixed language, religion, and allergy option lists.
- The backend settings API should focus on the authenticated user's saved personal setting values, not master option lookup.
- All-in-one `getMySettings` was broader than needed when individual setting lookup is preferred.

**DB Impact**
- Schema changed by this task: No
- Existing personal setting tables and columns are still used:
  - `users.language_code`
  - `users.religious_code`
  - `user_allergy`

**API Impact**
- Removed the all-in-one personal settings lookup endpoint:
  - `GET /api/v1/settings`
- Removed settings master lookup controller endpoints:
  - `GET /api/v1/settings/languages`
  - `GET /api/v1/settings/allergies?lang={langCode}`
  - `GET /api/v1/settings/religions?lang={langCode}`
- Added authenticated individual personal setting lookup endpoints:
  - `GET /api/v1/settings/language`
  - `GET /api/v1/settings/allergies`
  - `GET /api/v1/settings/religion`
- Existing update endpoints remain unchanged:
  - `PATCH /api/v1/users/me/language`
  - `PUT /api/v1/users/me/allergies`
  - `PATCH /api/v1/users/me/religion`

**Implementation Notes**
- `SettingsController` now exposes only personal setting lookup endpoints.
- Removed the temporary `UserSettings` aggregate response and DTO.
- `UserPreferenceService` now has separate read methods for language, allergies, and religion.
- Master lookup persistence remains available internally because update validation still needs to verify submitted codes.

**Tests**
- Replaced the all-in-one settings lookup test with individual language, allergy, and religion lookup tests.
- Full Maven verification remains blocked by the current Maven wrapper/runtime issue.

**Documentation Updates**
- Updated `docs/work-context.md`.

**Remaining Issues**
- Re-run Maven tests once Maven wrapper execution is fixed or a local Maven install is available.

---

## 2026-04-14 (3)

**Task**
- Add personal settings lookup to `SettingsController`.

**Affected Layers**
- `settings.presentation.controller`
- `settings.presentation.dto.response`
- `settings.presentation.swagger`
- `settings.application.service`
- `settings.application.port`
- `settings.domain`
- `settings.infrastructure.persistence`
- `settings` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsController.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/UserSettingsResponse.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
- `src/main/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceService.java`
- `src/main/java/com/mealguide/mealguide_api/settings/application/port/UserPreferencePort.java`
- `src/main/java/com/mealguide/mealguide_api/settings/domain/UserSettings.java`
- `src/main/java/com/mealguide/mealguide_api/settings/infrastructure/persistence/adapter/UserPreferencePersistenceAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/settings/infrastructure/persistence/repository/UserAllergyJpaRepository.java`
- `src/test/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceServiceTest.java`
- `docs/work-context.md`

**Why**
- The settings screen needs to read the authenticated user's currently saved language, allergy, and religious restriction selections.
- The endpoint should stay under the settings feature while keeping the controller thin.

**DB Impact**
- Schema changed by this task: No
- New read access uses existing tables:
  - `users`
  - `user_allergy`
  - `allergy`

**API Impact**
- Added authenticated endpoint:
  - `GET /api/v1/settings`
- Response fields:
  - `languageCode`
  - `allergyCodes`
  - `religiousCode`
- `allergyCodes` are returned in allergy `display_order` order.

**Implementation Notes**
- `SettingsController` delegates personal settings lookup to `UserPreferenceService`.
- `UserPreferenceService.getSettings` validates the current active user and reads selected allergy codes through `UserPreferencePort`.
- Added `UserSettings` domain record and `UserSettingsResponse` DTO.
- Updated `SettingsApi` Swagger declarations and removed no-auth Swagger markers from settings endpoints because settings now require user-level access.

**Tests**
- Added a focused `UserPreferenceServiceTest` case for personal settings lookup.
- Full Maven verification remains blocked by the current Maven wrapper/runtime issue.

**Documentation Updates**
- Updated `docs/work-context.md`.

**Remaining Issues**
- Re-run Maven tests once Maven wrapper execution is fixed or a local Maven install is available.

---

## 2026-04-14 (4)

**Task**
- Align the login `User` domain entity with the current `users` table structure.

**Affected Layers**
- `login.domain`
- `login` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/test/java/com/mealguide/mealguide_api/login/domain/UserTest.java`
- `docs/work-context.md`

**Why**
- `docs/schema.sql` defines `users.language_code` and `users.onboarding_completed`, but the login `User` entity did not map those columns.
- First-login user creation should explicitly match the current schema defaults and nullable preference fields.

**DB Impact**
- Schema changed by this task: No
- Code was aligned to the already documented `users` schema.

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Added `languageCode` mapped to `users.language_code`.
- Added `onboardingCompleted` mapped to `users.onboarding_completed`.
- First Google-login user creation now sets:
  - `languageCode = null`
  - `religiousCode = null`
  - `onboardingCompleted = false`

**Tests**
- Added a domain test for first-login `User` default values.
- Full Maven test execution remains blocked by unavailable dependencies/network in this environment.

**Documentation Updates**
- Updated `docs/work-context.md`.

**Remaining Issues**
- Re-run Maven tests once dependencies are available.

---

## 2026-04-14 (5)

**Task**
- Implement onboarding school lookup API.
- Implement settings master lookup APIs for languages, allergies, and religious food restrictions.
- Implement authenticated user personal settings update APIs for language, allergies, and religion.
- Group new code by feature package (`onboarding.*`, `settings.*`) with internal presentation/application/domain/infrastructure layers.

**Affected Layers**
- `onboarding.presentation`
- `onboarding.application`
- `onboarding.domain`
- `onboarding.infrastructure.persistence`
- `settings.presentation`
- `settings.application`
- `settings.domain`
- `settings.infrastructure.persistence`
- `global.config.security`
- `global.base.exception`
- documentation

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/onboarding/**`
- `src/main/java/com/mealguide/mealguide_api/settings/**`
- `src/main/java/com/mealguide/mealguide_api/global/base/exception/ErrorCode.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `src/test/java/com/mealguide/mealguide_api/onboarding/**`
- `src/test/java/com/mealguide/mealguide_api/settings/**`
- `AGENTS.md`
- `docs/project-context.md`
- `docs/database-context.md`
- `docs/schema.sql`
- `docs/work-context.md`

**Why**
- The mobile onboarding page needs a selectable school list with language-specific display names.
- The personal settings page needs selectable language, allergy, and religion master data.
- Authenticated users need APIs to update stored language preference, replace allergy selections, and update or clear religious food restriction settings.
- The latest package convention is feature grouping with each feature containing its own controller/service/port/domain/persistence packages.

**DB Impact**
- Runtime schema redesign: No
- Schema reference corrected: Yes
- Corrected `docs/schema.sql` so string code primary keys such as `language.code`, `allergy.code`, `ingredient.code`, and `religious_food_restriction.code` are plain `VARCHAR` primary keys instead of identity columns.
- Corrected numeric identity columns to PostgreSQL `GENERATED BY DEFAULT AS IDENTITY` syntax.
- Referenced tables:
  - `language`
  - `school`
  - `school_translation`
  - `users`
  - `allergy`
  - `allergy_translation`
  - `user_allergy`
  - `religious_food_restriction`
  - `religious_food_restriction_translation`

**API Impact**
- Added public lookup endpoints:
  - `GET /api/v1/onboarding/schools?lang={langCode}`
  - `GET /api/v1/settings/languages`
  - `GET /api/v1/settings/allergies?lang={langCode}`
  - `GET /api/v1/settings/religions?lang={langCode}`
- Added authenticated settings update endpoints:
  - `PATCH /api/v1/users/me/language`
  - `PUT /api/v1/users/me/allergies`
  - `PATCH /api/v1/users/me/religion`
- School, allergy, and religion lookups use translation rows when available and fall back to base names.
- Allergy update is a full replacement and removes duplicate request codes while preserving first-seen order.
- Religion update accepts `null` and clears `users.religious_code`.

**Implementation Notes**
- Controllers remain thin and delegate to application services.
- Application services validate blank input, unknown language codes, unknown allergy codes, unknown religious codes, and missing authenticated users.
- Persistence is accessed through feature application ports implemented by feature persistence adapters.
- `settings.domain.UserPreference` maps the settings-related columns of `users` and provides explicit update methods for language and religion.
- Public GET lookup routes were added to the Spring Security whitelist; `/api/v1/users/me/**` remains authenticated.

**Tests**
- Added focused service tests for:
  - onboarding school response values including fallback-shaped results
  - language lookup mapping
  - allergy lookup mapping
  - religion lookup mapping
  - language update success
  - language update invalid code failure
  - allergy replacement success and duplicate handling
  - allergy replacement invalid code failure
  - religion update success
  - religion invalid code failure
  - religion clear success
- `mvn test` could not be completed because Maven is not installed on PATH and the Maven wrapper could not resolve Spring Boot `4.0.5` without network access. The user declined the escalated network-enabled Maven run.

**Documentation Updates**
- Updated `AGENTS.md` with feature package structure rules.
- Updated `docs/project-context.md` with `onboarding` and `settings` feature package conventions.
- Updated `docs/database-context.md` with language, user preference, translation, and user allergy table semantics.
- Updated `docs/schema.sql` to correct documented PostgreSQL identity syntax and code primary key definitions.
- Updated `docs/work-context.md`.

**Remaining Issues**
- Re-run `mvn test` in an environment with Maven dependencies available.
- Existing `docs/schema.sql` seed data still contains mojibake text from prior encoding issues; this task only corrected schema definitions related to the implemented APIs.

## 2026-04-06 (1)

**Task**
- Implement mobile Google ID Token login, JWT authentication, and Redis refresh token flow.
- Accept `idToken` and `deviceId` from the client.
- Issue server-managed `accessToken` and `refreshToken`.
- Group authentication code under the `login` feature package and place Swagger descriptions on the implemented interface.

**Affected Layers**
- `login.presentation`
- `login.application`
- `login.domain`
- `login.infrastructure`
- `global.config.security`

**Changed Files**
- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/java/com/mealguide/mealguide_api/login/**`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `src/test/java/com/mealguide/mealguide_api/login/**`
- `docs/project-context.md`
- `docs/database-context.md`
- `docs/schema.sql`
- `docs/work-context.md`

**Why**
- Mobile authentication must be stateless and based on `Authorization: Bearer {accessToken}`.
- Google login needs server-issued tokens for consistent API authentication.
- Refresh tokens should be managed in Redis with TTL instead of additional DB schema changes.
- `deviceId` is required to manage refresh tokens per device.

**DB Impact**
- Schema changed by this task: No
- Referenced tables: `users`
- New tables or columns added by this task: No

**API Impact**
- Added endpoints:
  - `POST /auth/login`
  - `POST /auth/refresh`
  - `POST /auth/logout`
- Request DTOs:
  - login: `idToken`, `deviceId`
  - refresh: `refreshToken`
  - logout: optional `refreshToken`
- Response DTO:
  - `accessToken`
  - `refreshToken`
  - `expiresIn`
  - `refreshExpiresIn`

**Implementation Notes**
- Google ID Token is verified through Google `tokeninfo` API.
- Refresh token is stored in Redis with key format `auth:refresh:{userId}:{deviceId}`.
- Refresh compares JWT claims and Redis value, then rotates the refresh token.
- Logout removes the Redis refresh token for the authenticated user and device.
- `SecurityConfig` whitelists `/auth/login` and `/auth/refresh`.

**Tests**
- Added or updated tests for:
  - login success
  - login failure when user does not exist
  - refresh success with rotation
  - refresh failure when Redis token is missing
  - refresh failure after logout
  - JWT authentication filter principal population
- `mvn test` could not be fully verified in the current sandboxed environment.

**Documentation Updates**
- Updated `docs/project-context.md`
- Updated `docs/database-context.md`
- Updated `docs/schema.sql`
- Updated `docs/work-context.md`

**Remaining Issues**
- Build and test execution should be re-run once Maven repository access is available.
- Google token verification currently depends on external `tokeninfo` API availability.

---

## 2026-04-06 (2)

**Task**
- Adjust login code to match the updated user-related schema.
- Remove obsolete `users.password_hash` mapping.
- Update Google login lookup to use `user_oauth_accounts`.

**Affected Layers**
- `login.domain`
- `login.application.port`
- `login.application.service`
- `login.infrastructure.persistence.repository`
- `login.infrastructure.persistence.adapter`
- `login` tests
- `docs/database-context.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserOauthAccount.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/port/UserQueryPort.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserOauthAccountJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/database-context.md`
- `docs/work-context.md`

**Why**
- The current schema no longer has `users.password_hash`.
- Google-linked users are now represented by `user_oauth_accounts`.
- Login must follow schema truth instead of relying on nullable `users.email` alone.

**DB Impact**
- Schema changed by this task: No
- This task only aligned code with the already changed schema.
- Referenced tables:
  - `users`
  - `user_oauth_accounts`

**API Impact**
- External endpoint contract changed: No
- Internal login lookup behavior changed:
  - first lookup by `provider = GOOGLE` and `provider_user_id = Google subject`
  - fallback lookup by `provider_email` when needed

**Implementation Notes**
- Added `UserOauthAccount` entity mapped to `user_oauth_accounts`.
- Added repository methods for Google provider subject/email lookup.
- Updated `UserQueryPort` and adapter to resolve users through OAuth mapping.
- Removed obsolete `passwordHash` field usage from entity and tests.

**Tests**
- Updated login service tests to mock OAuth account based user lookup.
- Updated JWT filter tests to match the updated `User` entity shape.
- Full `mvn test` execution is still not verified in this environment.

**Documentation Updates**
- Updated `docs/database-context.md`
- Updated `docs/work-context.md`

**Remaining Issues**
- If the actual provider value in data is not `GOOGLE`, repository lookup constant must be adjusted to match production data.
- If user linking policy changes again, login lookup rules should be revisited with the latest schema and seed data.

---

## 2026-04-06 (3)

**Task**
- Align response DTOs with the reduced `AuthTokenResult`.
- Change user status in the login domain to an enum with active and inactive values.
- Ensure inactive users are excluded at query time.

**Affected Layers**
- `login.domain`
- `login.presentation.dto.response`
- `login.infrastructure.persistence.repository`
- `login.infrastructure.persistence.adapter`
- `login.application.service`
- `login` tests
- `docs/database-context.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserStatus.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/dto/response/AuthResponse.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserOauthAccountJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/database-context.md`
- `docs/work-context.md`

**Why**
- `AuthTokenResult` no longer includes token type or user information.
- Response DTO conversion needed to match the current service result shape.
- Authentication should only resolve active users, and inactive users should be excluded before service-level use.

**DB Impact**
- Schema changed by this task: No
- Query behavior changed:
  - `users.status = ACTIVE` is now enforced in authentication-related lookups

**API Impact**
- Auth response fields are now:
  - `accessToken`
  - `refreshToken`
  - `expiresIn`
  - `refreshExpiresIn`

**Implementation Notes**
- Added `UserStatus` enum with `ACTIVE` and `INACTIVE`.
- Changed `User.status` to enum mapping with `EnumType.STRING`.
- Updated repository methods so OAuth account lookup and user id lookup only return `ACTIVE` users.
- Removed old response conversion assumptions about `tokenType` and user payload.

**Tests**
- Updated tests to set `User.status` using `UserStatus.ACTIVE`.
- Full `mvn test` execution is still not verified in this environment.

**Documentation Updates**
- Updated `docs/database-context.md`
- Updated `docs/work-context.md`

**Remaining Issues**
- If stored `users.status` values differ from `ACTIVE` and `INACTIVE`, the enum and repository filters must be adjusted to match real data.

---

## 2026-04-06 (4)

**Task**
- Reorganize shared authentication classes into the global package.
- Keep feature-specific Google login logic inside the `login` package.

**Affected Layers**
- `global.auth`
- `global.config.security`
- `login.application.service`
- `login.presentation`
- test imports
- `docs/project-context.md`
- `AGENTS.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/auth/domain/AuthenticatedUser.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/domain/TokenClaims.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/domain/TokenType.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/port/TokenProviderPort.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/port/RefreshTokenPort.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/jwt/JwtProperties.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/jwt/JwtTokenProvider.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/redis/RedisRefreshTokenAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/AuthenticatedUserPrincipal.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/JwtAuthenticationFilter.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/RestAuthenticationEntryPoint.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/RestAccessDeniedHandler.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/controller/AuthController.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/project-context.md`
- `AGENTS.md`
- `docs/work-context.md`

**Why**
- JWT, refresh token storage, principal, filter, and auth error handlers are shared infrastructure concerns.
- Google token verification and OAuth user lookup are still login feature concerns and remain under `login`.
- This keeps common auth code out of the feature package without broad refactoring of feature-specific logic.

**DB Impact**
- Schema changed by this task: No
- Query behavior changed by this task: No

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Moved shared auth records, ports, JWT implementation, Redis refresh token adapter, and Spring Security auth classes to `global.auth`.
- Updated imports in service, controller, Swagger interface, security config, and tests.
- Removed duplicated shared auth classes from the `login` package after migration.

**Documentation Updates**
- Updated `docs/project-context.md`
- Updated `AGENTS.md`
- Updated `docs/work-context.md`

**Remaining Issues**
- Build and tests still need to be executed in an environment where Maven repository access works.

---

## 2026-04-06 (5)

**Task**
- Minimize JWT claim contents for access token and refresh token.
- Adjust logout flow to work without `deviceId` in the access token.

**Affected Layers**
- `global.auth.domain`
- `global.auth.jwt`
- `login.application.service`
- `login.presentation`
- tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/auth/domain/TokenClaims.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/jwt/JwtTokenProvider.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/dto/request/LogoutRequest.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/controller/AuthController.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/work-context.md`

**Why**
- Access token should contain only `sub`, `type`, and expiration.
- Refresh token should contain only `sub`, `deviceId`, `type`, and expiration.
- Because access token no longer contains `deviceId`, logout must identify the target refresh token through the submitted refresh token.

**API Impact**
- `POST /auth/logout` now effectively requires `refreshToken` in the request body.
- Token response fields did not change.

**Implementation Notes**
- `TokenClaims` now contains only `userId`, `deviceId`, and `tokenType`.
- Access token generation now writes only `sub` and `type` claims.
- Refresh token generation writes `sub`, `deviceId`, and `type` claims.
- Logout parses the refresh token, validates the authenticated user id, extracts `deviceId`, and deletes the matching Redis key.
- JWT authentication filter now authenticates using only `sub` from the access token and reloads user data from the database.

**Tests**
- Updated refresh token tests to use the reduced claim shape.
- Updated JWT filter tests to use access token claims without `deviceId`.

**Remaining Issues**
- Full build and test execution still needs to be run in an environment where Maven can access its local repository normally.

---

## 2026-04-06 (6)

**Task**
- Add a custom annotation for directly injecting the authenticated user id.

**Affected Layers**
- `global.auth.annotation`
- `login.presentation`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/auth/annotation/CurrentUserId.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/controller/AuthController.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `docs/work-context.md`

**Why**
- Using `@AuthenticationPrincipal` and then manually reading `userId` repeats the same pattern.
- A dedicated annotation makes controller signatures shorter and keeps intent explicit.

**Implementation Notes**
- Added `@CurrentUserId` as a meta-annotation over `@AuthenticationPrincipal(expression = "userId")`.
- Also marked it hidden for Swagger parameter generation.
- Updated logout endpoint to inject `Long currentUserId` directly.

---

## 2026-04-06 (7)

**Task**
- Change `users.role` handling to an enum.
- Define supported roles as `USER`, `ADMIN`, and `MANAGER`.

**Affected Layers**
- `login.domain`
- `global.auth.domain`
- `global.auth.security`
- tests
- `docs/database-context.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserRole.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/domain/AuthenticatedUser.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/AuthenticatedUserPrincipal.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/database-context.md`
- `docs/work-context.md`

**Why**
- Role values should be constrained to known values instead of using free-form strings.
- Security authority mapping should be derived from the enum directly.
- The requested default role for future first-time user creation is `USER`.

**DB Impact**
- Schema changed by this task: No
- Entity mapping for `users.role` now uses `EnumType.STRING`.

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Added `UserRole` enum with `USER`, `ADMIN`, and `MANAGER`.
- Changed `User.role` to enum mapping.
- Changed authenticated user and principal role fields to use `UserRole`.
- Spring Security authority mapping now always uses `ROLE_{enumName}`.
- Added `UserRole.defaultRole()` returning `USER` for use by future user creation flows.
- No automatic first-login user creation was added because the current schema requires `school_id` and the project does not define a safe rule for deriving it during login.

**Remaining Issues**
- If a first-login auto-signup flow is needed, a valid `school_id` resolution rule must be defined before creating `users` rows.

---

## 2026-04-06 (8)

**Task**
- Make `users.school_id` nullable.
- Add automatic signup on first Google login.

**Affected Layers**
- `login.domain`
- `login.application.port`
- `login.application.service`
- `login.infrastructure.persistence.adapter`
- tests
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserOauthAccount.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/port/UserQueryPort.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

**Why**
- The requested behavior is to auto-register users on first login.
- The current schema required `school_id`, which blocked safe user creation during login.

**DB Impact**
- Schema changed by this task: Yes
- Changes:
  - `users.school_id` is now nullable
  - `users.id` uses identity generation
  - `user_oauth_accounts.id` uses identity generation

**API Impact**
- External API contract changed: No
- Behavior changed:
  - first Google login now auto-creates a user and linked OAuth account instead of returning `USER_NOT_FOUND`

**Implementation Notes**
- `LoginService.login` now creates a user when no mapped account exists.
- New first-login user defaults:
  - `schoolId = null`
  - `status = ACTIVE`
  - `role = USER`
- `UserPersistenceAdapter` saves both `users` and `user_oauth_accounts`.

**Tests**
- Replaced the missing-user failure case with first-login auto-signup success coverage.

**Remaining Issues**
- Full build and test execution still needs to be run in an environment where Maven can access its local repository normally.

---

## 2026-04-06 (9)

**Task**
- Add ORM-level soft delete behavior for `users`.

**Affected Layers**
- `login.domain`
- `docs/database-context.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `docs/database-context.md`
- `docs/work-context.md`

**Why**
- A user delete operation should disable the account by changing `status` to `INACTIVE` instead of physically deleting the row.

**DB Impact**
- Schema changed by this task: No
- ORM delete behavior changed:
  - entity delete now executes `UPDATE users SET status = 'INACTIVE' WHERE id = ?`

**Implementation Notes**
- Added Hibernate `@SQLDelete` to `User` so entity delete operations become status updates.
- Added Hibernate `@SQLRestriction("status <> 'INACTIVE'")` so inactive users are excluded from normal ORM selections.
- Existing repository-level `ACTIVE` filtering remains in place.

**Remaining Issues**
- `@SQLDelete` does not apply to JPQL bulk delete or native delete queries. Those must be avoided or handled separately if introduced later.

---

## 2026-04-06 (10)

**Task**
- Add a separate local/dev-only package for verifying the auth controller flow without a frontend-provided Google ID token.

**Affected Layers**
- `authdebug.application`
- `authdebug.presentation`
- `login.application.service`
- `global.config.security`
- `src/main/resources`
- `docs/project-context.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/authdebug/application/service/AuthDebugService.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/controller/AuthDebugController.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/dto/request/AuthDebugLoginRequest.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/swagger/AuthDebugApi.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `src/main/resources/application.properties`
- `src/main/resources/application-local.properties`
- `src/main/resources/application-dev.properties`
- `docs/project-context.md`
- `docs/work-context.md`

**Why**
- There is no frontend code available yet to obtain a real Google ID token.
- The current auth flow still needs a practical way to verify signup, JWT issuance, refresh rotation, and logout behavior end-to-end.
- The verification code should live in a separate package and stay disabled outside explicitly enabled environments.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Added local/dev-only verification endpoints:
  - `POST /auth-debug/login`
  - `POST /auth-debug/refresh`
  - `POST /auth-debug/logout`
- `POST /auth-debug/login` accepts:
  - `subject`
  - `email`
  - `name`
  - `emailVerified`
  - `deviceId`

**Implementation Notes**
- Added `authdebug.*` as a separate package for manual auth verification.
- `AuthDebugController` implements Swagger descriptions through `AuthDebugApi`, matching the existing controller style.
- `AuthDebugService` reuses `LoginService` so the same signup, JWT issuance, Redis refresh-token validation, refresh rotation, and logout logic are exercised.
- `LoginService` now exposes `loginVerifiedGoogleUser(...)` so trusted debug input can reuse the real login flow after skipping Google token verification.
- Debug endpoints are guarded by `mealguide.auth-debug.enabled`.
- Default value is `false` in `application.properties` and `true` in `application-local.properties` and `application-dev.properties`.
- Security whitelist now includes debug login and refresh endpoints so they follow the same access pattern as the main auth endpoints.

**Tests**
- Build verification was attempted, but `mvn` is not installed in the current environment.
- Runtime verification should be done through Swagger or direct HTTP calls in a local/dev profile with `mealguide.auth-debug.enabled=true`.

**Remaining Issues**
- Debug endpoints should not be enabled in production.
- Full compile and test verification still needs an environment with Maven installed.

---

## 2026-04-06 (11)

**Task**
- Register a `RestClient.Builder` bean required by Google ID token verification.

**Affected Layers**
- `global.config.base`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/config/base/RestClientConfig.java`
- `docs/work-context.md`

**Why**
- `GoogleTokenInfoClient` uses constructor injection for `RestClient.Builder`.
- The current project did not define that bean, so application startup failed before the auth flow could run.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Added `RestClientConfig` under `global.config.base`.
- Registered a single shared `RestClient.Builder` bean with `RestClient.builder()`.
- Registered a shared `ObjectMapper` bean with `findAndRegisterModules()` because auth error handlers also require constructor injection.
- `GoogleTokenInfoClient` can now be created without changing its constructor or feature-layer wiring.

**Remaining Issues**
- Runtime verification still needs to be done in an environment with Maven installed and the required local services available.

---

## 2026-04-06 (12)

**Task**
- Fix first-login auto-signup insert failures caused by null primary keys in `users` and `user_oauth_accounts`.

**Affected Layers**
- `login.domain`
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserOauthAccount.java`
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

**Why**
- The running PostgreSQL schema did not provide identity defaults for `users.id`, so first-login signup failed with a null primary key insert.
- Auto-signup needs a key generation strategy that does not depend on the table column already being configured as identity.

**DB Impact**
- Schema reference changed by this task: Yes
- `users` and `user_oauth_accounts` id generation is now documented with PostgreSQL sequences:
  - `users_id_seq`
  - `user_oauth_accounts_id_seq`

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Changed `User.id` to `GenerationType.SEQUENCE` using `users_id_seq`.
- Changed `UserOauthAccount.id` to `GenerationType.SEQUENCE` using `user_oauth_accounts_id_seq`.
- This allows Hibernate to obtain ids before insert, avoiding dependence on DB-side identity defaults for first-login signup.

**Remaining Issues**
- If the actual local database does not yet have these sequences, Hibernate `ddl-auto=update` in local should create them, but an existing manually managed environment may still need a one-time schema sync.

---

## 2026-04-06 (13)

**Task**
- Revert user id generation back to database identity columns instead of sequence-based generation.

**Affected Layers**
- `login.domain`
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserOauthAccount.java`
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

**Why**
- The preferred strategy is for PostgreSQL table columns to own auto-increment behavior directly.
- Authentication auto-signup should follow the DB schema instead of compensating in JPA for a mismatched local database state.

**DB Impact**
- Schema reference changed by this task: Yes
- `users.id` and `user_oauth_accounts.id` are documented again as `GENERATED BY DEFAULT AS IDENTITY`.
- Existing local databases must also have the actual columns updated to identity if they are still plain `BIGINT NOT NULL`.

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Changed `User.id` back to `GenerationType.IDENTITY`.
- Changed `UserOauthAccount.id` back to `GenerationType.IDENTITY`.
- Removed the temporary sequence-based schema documentation.
- The previous null primary key error indicates the local DB schema is currently behind the documented schema.

**Remaining Issues**
- The running PostgreSQL database must be aligned so that `users.id` and `user_oauth_accounts.id` are true identity columns. Without that DB change, first-login auto-signup will fail again.

---

## 2026-04-06 (14)

**Task**
- Change Swagger descriptions to Korean and remove interface-based controller overriding for auth controllers.

**Affected Layers**
- `login.presentation.controller`
- `authdebug.presentation.controller`
- `login.presentation.swagger`
- `authdebug.presentation.swagger`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/presentation/controller/AuthController.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/controller/AuthDebugController.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/swagger/AuthDebugApi.java`
- `docs/work-context.md`

**Why**
- Swagger descriptions should be written in Korean for current project usage.
- Controllers should keep the existing interface-based Swagger structure while hiding explicit overriding noise from controller source.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No
- Swagger display text changed to Korean.

**Implementation Notes**
- Restored interface-based Swagger definitions for `AuthController` and `AuthDebugController`.
- Controllers implement the Swagger interfaces again.
- Removed explicit `@Override` annotations from controller source so overriding is not visibly exposed.

**Remaining Issues**
- Build verification is still pending because Maven is not available in the current environment.

---

## 2026-04-06 (15)

**Task**
- Add a small local HTML page that can receive a real Google ID token and call the auth APIs.

**Affected Layers**
- `src/main/resources/static`
- `docs/work-context.md`

**Changed Files**
- `src/main/resources/static/auth-test.html`
- `docs/work-context.md`

**Why**
- A real Google ID token is required to verify the actual Google token validation flow.
- There is no frontend code in the current project, so a minimal browser test page is needed.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No
- Added a static test page served by Spring:
  - `GET /auth-test.html`

**Implementation Notes**
- Added a single-page browser test tool under `static`.
- The page uses Google Identity Services to obtain a real ID token.
- After Google login, the same page can call:
  - `POST /auth/login`
  - `POST /auth/refresh`
  - `POST /auth/logout`
- The page also shows the current token state for easier manual verification.

**Remaining Issues**
- The Google OAuth client must allow the local origin used to open this page.
- Real end-to-end verification still depends on local PostgreSQL and Redis being available.

---

## 2026-04-06 (16)

**Task**
- Open the local auth test page through security whitelist settings.

**Affected Layers**
- `global.config.security`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `docs/work-context.md`

**Why**
- The static test page was being blocked by Spring Security because public GET routes were empty.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No
- Public GET access now allows:
  - `/`
  - `/index.html`
  - `/auth-test.html`
  - `/favicon.ico`
  - `/error`

**Implementation Notes**
- Added the auth test page and minimal static routes to `PUBLIC_WHITELIST`.
- Authenticated APIs remain protected except for the intended login and refresh entry points.

---

## 2026-04-06 (17)

**Task**
- Remove manual Google Client ID input from the auth test page and load it from server configuration.

**Affected Layers**
- `authdebug.presentation.controller`
- `authdebug.presentation.dto.response`
- `global.config.security`
- `src/main/resources/static`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/controller/AuthDebugConfigController.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/dto/response/AuthDebugConfigResponse.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `src/main/resources/static/auth-test.html`
- `docs/work-context.md`

**Why**
- The Google Client ID is already supplied through server configuration.
- The local auth test page should use that value automatically instead of requiring duplicate manual input.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Added local/dev-only config endpoint:
  - `GET /auth-debug/config`
- The auth test page now reads Google Client ID from that endpoint.

**Implementation Notes**
- Added `AuthDebugConfigController` for local/dev-only config exposure.
- Added `AuthDebugConfigResponse` as a response DTO instead of returning raw values directly.
- Added `/auth-debug/config` to the public GET whitelist.
- Updated `auth-test.html` so the page fetches the configured Google Client ID on load and removes the manual input field.

---

## 2026-04-06 (18)

**Task**
- Remove the refresh-token rotation race condition by introducing an atomic Redis rotation operation.

**Affected Layers**
- `global.auth.port`
- `global.auth.redis`
- `login.application.service`
- `login` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/auth/port/RefreshTokenPort.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/redis/RedisRefreshTokenAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `docs/work-context.md`

**Why**
- The previous refresh flow used separate read, compare, and save steps.
- Concurrent refresh requests using the same refresh token could both pass validation and each receive a new token pair.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No
- Internal refresh-token rotation semantics changed to atomic compare-and-set.

**Implementation Notes**
- Added `rotateIfMatch(...)` to `RefreshTokenPort`.
- Implemented the Redis operation with a single Lua script that performs compare-and-set plus TTL update atomically.
- Changed `LoginService.refresh()` to rely on the atomic rotation result instead of separate `findByUserIdAndDeviceId()` and `save()` calls.
- Updated the in-memory test double to match the new port contract.

**Remaining Issues**
- Full runtime verification is still pending because Maven is not available in the current environment.

---

## 2026-04-06 (19)

**Task**
- Stop storing raw refresh token values in Redis and store only token hashes.

**Affected Layers**
- `global.auth.redis`
- `login` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/auth/redis/RedisRefreshTokenAdapter.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `docs/work-context.md`

**Why**
- Storing raw refresh tokens in Redis makes session theft possible if Redis read access is exposed.
- Refresh token persistence should keep only a derived hash value, and submitted tokens should be hashed again before comparison.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No
- Internal Redis persistence now stores only SHA-256 hashes of refresh tokens.

**Implementation Notes**
- `RedisRefreshTokenAdapter` now hashes refresh token values before save.
- Atomic rotation also hashes both the expected token and the new token before compare-and-set.
- Login service tests were updated so the in-memory adapter mirrors the same hashed-storage semantics.

**Remaining Issues**
- The current implementation uses unsalted SHA-256 because comparison must remain deterministic for rotation. If stronger protection is needed later, an HMAC-based keyed hash using a server secret would be preferable.

---

## 2026-04-06 (20)

**Task**
- Reduce refresh-path database load by replacing full user fetch with an active-user existence check.

**Affected Layers**
- `login.application.port`
- `login.application.service`
- `login.infrastructure.persistence.repository`
- `login.infrastructure.persistence.adapter`
- `login` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/login/application/port/UserQueryPort.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `docs/work-context.md`

**Why**
- The refresh flow only needs to confirm that the user is still active before issuing new tokens.
- Loading the full `User` entity on every refresh is unnecessary under the current token-claim design.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Added `existsActiveById(...)` to `UserQueryPort`.
- Implemented the existence check with `existsByIdAndDeletedAtIsNullAndStatus(...)`.
- `LoginService.refresh()` now checks active-user existence and builds the minimal `AuthenticatedUser` directly from token claims.

---

## 2026-04-06 (21)

**Task**
- Reduce JWT filter database load by replacing full user fetch with an active-user existence check.

**Affected Layers**
- `global.auth.security`
- `login.application.port`
- `login` tests
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/AuthenticatedUserPrincipal.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/JwtAuthenticationFilter.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/work-context.md`

**Why**
- The JWT filter runs on every authenticated request.
- Under the current token design, loading the full `User` entity is unnecessary when only active-user validation and principal userId population are needed.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No

**Implementation Notes**
- `JwtAuthenticationFilter` now uses `existsActiveById(...)` instead of `findById(...)`.
- Added `AuthenticatedUserPrincipal.authenticated(...)` for minimal principal construction from token claims.
- The filter now populates authentication without loading user email, name, or role from the database.

**Remaining Issues**
- Because access tokens intentionally omit role claims, role-based authorization would require either reintroducing a user lookup or adding role information to the token.

---

## 2026-04-06 (22)

**Task**
- Fix `RestClientConfig` bean creation failure caused by an unavailable `Jackson2ObjectMapperBuilder`.

**Affected Layers**
- `global.config.base`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/config/base/RestClientConfig.java`
- `docs/work-context.md`

**Why**
- The current configuration required `Jackson2ObjectMapperBuilder` injection, but that builder bean was not available in the running context.
- The auth/security infrastructure still needs a shared `ObjectMapper` and `RestClient.Builder` without depending on extra auto-configured beans.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No

**Implementation Notes**
- `RestClientConfig` now provides an `ObjectMapper` only as a fallback with `@ConditionalOnMissingBean(ObjectMapper.class)`.
- If Spring Boot auto-configures the default Jackson mapper, that bean is used unchanged.
- If no `ObjectMapper` bean exists in the running context, the fallback bean uses `JsonMapper.builder().findAndAddModules().build()`.
- `RestClient.Builder` registration remains unchanged.

---

## 2026-04-06 (23)

**Task**
- Remove `ObjectMapper` bean dependency from security exception handlers to avoid startup failure.

**Affected Layers**
- `global.auth.security`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/RestAccessDeniedHandler.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/RestAuthenticationEntryPoint.java`
- `docs/work-context.md`

**Why**
- The running context still reported missing `ObjectMapper` injection for security handlers.
- These handlers only need simple JSON serialization for error responses, so they should not block startup on a shared bean.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Replaced constructor-injected `ObjectMapper` usage with an internal static `JsonMapper`.
- Security error response shape stays the same.

---

## 2026-04-06 (24)

**Task**
- Add minimum length validation for JWT secrets at configuration binding time.

**Affected Layers**
- `global.auth.jwt`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/global/auth/jwt/JwtProperties.java`
- `docs/work-context.md`

**Why**
- `Keys.hmacShaKeyFor()` requires a sufficiently long secret key and otherwise fails at runtime.
- Configuration errors should be rejected during property binding instead of surfacing later during JWT provider initialization.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Added `@Size(min = 32)` to both `accessSecret` and `refreshSecret`.
- Existing `@NotBlank` and expiration-time validation remain unchanged.

---

## 2026-04-06 (25)

**Task**
- Centralize the JJWT library version in Maven properties.

**Affected Layers**
- `pom.xml`
- `docs/work-context.md`

**Changed Files**
- `pom.xml`
- `docs/work-context.md`

**Why**
- The same JJWT version string was repeated across multiple dependencies.
- Defining the version once in Maven properties makes future updates safer and more consistent.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- External API contract changed: No

**Implementation Notes**
- Added `jjwt.version` to the Maven `<properties>` section.
- Updated `jjwt-api`, `jjwt-impl`, and `jjwt-jackson` to use `${jjwt.version}`.

---

## 2026-04-06 (26)

**Task**
- Remove the Swagger-based auth debug testing APIs and keep only the HTML-based test flow.

**Affected Layers**
- `authdebug`
- `global.config.security`
- `docs/project-context.md`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/authdebug/application/service/AuthDebugService.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/controller/AuthDebugController.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/dto/request/AuthDebugLoginRequest.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/swagger/AuthDebugApi.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `docs/project-context.md`
- `docs/work-context.md`

**Why**
- The local HTML page is now the intended way to test the real Google login flow.
- Separate Swagger endpoints for bypass-based auth testing are no longer needed and should be removed to reduce confusion.

**DB Impact**
- Schema changed by this task: No

**API Impact**
- Removed local/dev-only debug testing endpoints:
  - `POST /auth-debug/login`
  - `POST /auth-debug/refresh`
  - `POST /auth-debug/logout`
- Kept local/dev-only config endpoint:
  - `GET /auth-debug/config`

**Implementation Notes**
- Deleted the debug login service, controller, request DTO, and Swagger interface.
- Removed the debug login and refresh routes from the security whitelist.
- Updated project context so `authdebug.*` now describes only local/dev support code for the HTML test page.

---

## 2026-04-14 (6)

**Task**
- Restrict settings-related APIs to authenticated users with `USER` level or higher.

**Affected Layers**
- `settings.presentation.controller`
- `global.auth.security`
- `global.config.security`
- `login.application.port`
- `login.infrastructure.persistence`
- `docs/work-context.md`

**Changed Files**
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsController.java`
- `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/UserPreferenceController.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/AuthenticatedUserPrincipal.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/JwtAuthenticationFilter.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/port/UserQueryPort.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserJpaRepository.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/work-context.md`

**Why**
- Settings master lookup and user preference update APIs should not be public.
- `hasRole`/`hasAnyRole` checks require the authenticated principal to carry Spring Security authorities.

**DB Impact**
- Schema changed by this task: No
- Runtime DB access changed: Yes. JWT authentication now reads the active user's `role` from `users` to create `ROLE_USER`, `ROLE_MANAGER`, or `ROLE_ADMIN`.

**API Impact**
- External response contract changed: No
- Access policy changed:
  - `/api/v1/settings/**` now requires `USER`, `MANAGER`, or `ADMIN`.
  - `/api/v1/users/me/**` now requires `USER`, `MANAGER`, or `ADMIN`.
  - `/api/v1/onboarding/schools` remains public.

**Implementation Notes**
- Added a `UserQueryPort.findActiveRoleById` query method and adapter implementation.
- Updated `JwtAuthenticationFilter` to reject missing/inactive users while also loading the active role.
- Updated `AuthenticatedUserPrincipal` to support role-backed authorities without changing the old empty-authority factory method.
- Removed settings master lookup endpoints from the public GET whitelist.
- Added `@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")` on the settings controllers instead of adding role-specific route matchers in `SecurityConfig`.
- Added a JWT filter test assertion that a valid user token receives `ROLE_USER`.

**Remaining Issues**
- Maven verification is still blocked in the current shell because `mvn` is not available and wrapper dependency resolution requires network access.
