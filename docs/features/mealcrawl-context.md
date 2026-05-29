# mealcrawl 기능 맥락

## 1. 역할
`mealcrawl` 기능은 Java-to-Python 동기 HTTP 기반 급식 수집/후속 처리 파이프라인을 담당한다.
크롤링 데이터 import, AI 분석 후속 저장, 번역 후속 저장, 스케줄 실행을 포함한다.

## 2. 주요 패키지
- `mealcrawl.application.service`
- `mealcrawl.application.port`
- `mealcrawl.application.dto`
- `mealcrawl.domain`
- `mealcrawl.infrastructure.client`
- `mealcrawl.infrastructure.client.dto.request`
- `mealcrawl.infrastructure.client.dto.response`
- `mealcrawl.infrastructure.persistence.adapter`
- `mealcrawl.infrastructure.persistence.repository`
- `mealcrawl.infrastructure.config`

## 3. 주요 클래스
- `MealCrawlScheduler`: 스케줄 실행 진입점
- `MealCrawlOrchestrationService`: 전체 크롤링 오케스트레이션
- `MealCrawlTargetService`: 수집 대상 계산
- `MealImportService`: `meal_schedule`, `menu`, `meal_menu` import
- `MenuAiAnalysisFollowUpService`: AI 분석 후속 저장
- `MenuTranslationFollowUpService`: 번역 후속 저장
- `MealCrawlPersistenceAdapter`: 영속성 포트 구현
- `MealCrawlSchedulerLockAdapter`: advisory lock 구현
- `PythonMealClientAdapter`: Python API 호출 및 예외 처리
- `MealCrawlSchedulerLockPort`, `MealCrawlPersistencePort`, `PythonMealClientPort`: 포트 추상화

## 4. DB 사용 규칙
- 관련 테이블
  - import: `meal_schedule`, `menu`, `meal_menu`
  - AI 후속: `menu_ai_analysis`, `menu_ai_analysis_ingredient`, `menu_ai_analysis_allergy`
  - 번역 후속: `menu_translation`
  - 확정 데이터(자동 생성 금지): `meal_menu_confirmed_ingredient`, `meal_menu_confirmation_history`
- 저장 규칙
  - import 성공은 AI/번역 후속 실패와 분리되어야 한다.
  - `upsertMealMenu`는 경쟁 상태(race)를 고려해 충돌 복구 경로를 유지해야 한다.
  - 스케줄러는 PostgreSQL advisory lock으로 단일 실행을 보장한다.
- 트랜잭션 규칙
  - 외부 API 호출 중 DB transaction을 길게 유지하지 않는다.

## 5. API 규칙
- 외부 사용자용 HTTP 엔드포인트
  - `GET /api/v1/mealcrawl/weekly-meals`
  - `GET /api/v1/mealcrawl/cafeterias`
- 외부 연동 API
  - Java -> Python 동기 HTTP 호출 사용
  - 호출 경로/계약은 `PythonMealClientAdapter` 및 request/response DTO 기준으로 관리

## 6. 공통 비즈니스 규칙
- mealcrawl 기능은 Java 서버가 Python 서버를 동기 HTTP로 호출해 급식 데이터를 가져오고 DB에 저장하는 흐름을 담당한다.
- 급식 import 성공은 AI 분석/번역 후속 처리 실패와 분리되어야 한다.
- AI 분석 데이터는 초안 성격이며 확정 데이터와 동일하게 취급하지 않는다.
- `meal_menu_confirmed_ingredient`와 `meal_menu_confirmation_history`는 관리자 확정 데이터이며 크롤링 import에서 자동 생성하지 않는다.
- 외부 Python API 호출 중 DB transaction을 길게 유지하지 않는다.
- DB 쓰기는 application port와 persistence adapter 경계를 통해 처리한다.

## 7. 흐름별 비즈니스 규칙

### 7.1 Scheduler 실행
- scheduler는 설정된 주기에 따라 급식 크롤링 대상을 선택한다.
- 다중 인스턴스 환경에서 중복 실행되지 않도록 PostgreSQL advisory lock을 사용한다.
- lock 획득에 실패하면 해당 실행은 건너뛴다.
- 주간 범위 계산 시 기준일이 월요일이 아니어도 해당 주 월요일로 정규화해 월~일 범위로 처리한다.

### 7.2 Python crawl 호출
- Java 서버는 Python 서버에 급식 크롤링 요청을 보낸다.
- Python client 실패 시 원인 예외를 보존해야 한다.
- 외부 호출 실패가 서버 전체 장애로 번지지 않도록 예외 경계를 명확히 한다.

### 7.3 Meal import 저장
- `meal_schedule`, `menu`, `meal_menu`, `meal_schedule_crawl_history`에 저장한다.
- 같은 일정/순서의 `meal_menu`는 중복 생성하지 않고 upsert한다.
- `upsertMealMenu`는 동시성 경쟁 상황에서 unique collision을 고려해야 한다.

### 7.4 AI analysis follow-up
- import 이후 필요한 대상에 대해 AI 분석을 요청한다.
- 결과는 `menu_ai_analysis`, `menu_ai_analysis_ingredient`, `menu_ai_analysis_allergy`에 저장한다.
- AI 분석 실패는 meal import 성공을 실패로 바꾸면 안 된다.
- AI 분석 ingredient는 code가 없거나 DB에 없는 code가 오더라도 한글 `ingredientName`이 있으면 `ingredient`와 `ingredient_translation(ko)`에 자동 축적한다.
- 신규 ingredient의 영어 번역은 AI 분석 트랜잭션에서 처리하지 않고, 별도 ingredient 번역 배치가 `ingredient_translation(en)`이 없는 항목만 처리한다.

### 7.6 Allergy risk matching (AI allergy code based)
- 알레르기 위험도 계산은 더 이상 `allergy_ingredient` 매핑을 사용하지 않는다.
- 메뉴별 알레르기 정보는 최신 `SUCCESS` AI 분석의 `menu_ai_analysis_allergy`를 기준으로 사용한다.
- 사용자 알레르기(`user_allergy`)와 AI 분석 알레르기 코드의 교집합이 있으면 알레르기 위험으로 판단한다.
- 종교 제한 위험도 계산은 기존 `religious_food_restriction_ingredient` 기반 재료 매칭을 유지한다.

### 7.5 Translation follow-up
- import 이후 필요한 대상에 대해 번역을 요청한다.
- 결과는 `menu_translation`에 저장한다.
- 번역 실패는 meal import 성공을 실패로 바꾸면 안 된다.

### 7.5.1 Description follow-up
- import 이후 필요한 대상에 대해 메뉴 설명 생성을 요청한다.
- Java -> Python 요청 경로는 `/api/v1/python/menus/describe/list`이다.
- 요청은 `langCode`와 최대 7개 메뉴(`menuId`, `menuName`)를 포함한다.
- 현재 설명 대상 언어는 `ko`, `en`이다.
- 결과는 `menu_description(menu_id, lang_code)`에 저장한다.
- `menu_description`에 이미 있는 `(menu_id, lang_code)`는 다시 요청하지 않는다.
- 실패/누락/빈 값/500자 초과 결과는 해당 메뉴-언어만 실패로 기록한다.
- 재시도 상태는 `menu_description_analysis`에 `(menu_id, lang_code)` 기준으로 update 저장한다.
- 설명 실패는 meal import 성공을 실패로 바꾸면 안 된다.

### 7.7 Ingredient translation follow-up
- 신규 AI ingredient는 `ingredient_translation(ko)`만 즉시 저장하고 `ingredient_translation(en)`은 생성하지 않는다.
- ingredient 번역 배치는 `ko` 번역은 있고 `en` 번역이 없는 항목을 대상으로 한다.
- Python 요청 1회당 `ingredientTranslationBatchSize`만큼 보내고, 크롤링 오케스트레이션 1회당 최대 `ingredientTranslationMaxBatchesPerRun`번 순차 처리한다.
- 같은 오케스트레이션 실행 안에서 이미 시도한 ingredient code는 제외해 실패 항목을 즉시 반복 호출하지 않는다.
- Python ingredient 번역 응답은 `ingredientCode` 기준으로 매칭하며, 성공 항목만 `ingredient_translation(en)`에 저장한다.

## 8. 주의사항
- `@Scheduled` 실행은 다중 인스턴스 환경을 전제로 lock 없이 확장하면 안 된다.
- `PythonMealClientAdapter` 예외를 일반 예외로 덮어 원인 스택을 잃지 않도록 주의한다.
- 트랜잭션 경계 변경 시 import 성공 보장 규칙과 후속 처리 분리를 함께 검토한다.

## 9. Menu Like (2026-05-03)
- Added menu-like target model based on `(cafeteria_id, menu_id)` instead of `meal_menu_id`.
- Added `POST /api/v1/meal-menus/{mealMenuId}/like` toggle API.
- Added `like { count, likedByMe }` to menu detail response (single and batch).
- Duplicate likes are blocked by DB unique constraint: `UNIQUE(user_id, cafeteria_id, menu_id)`.

## 10. Menu Review Community (2026-05-03)
- Added menu community review APIs (review CRUD, review-like toggle, comment CRUD/list).
- Review aggregation key is `(cafeteria_id, menu_id)` so same menu across different meal dates shares one community timeline.
- Review write context stores `meal_menu_id` and `meal_date`.
- Menu detail response now includes review summary count.

## 11. Review Anonymous Display (2026-05-03)
- Review/comment writer name no longer returns `users.name`.
- Display name format: `Anonymous N`.
- Anonymous numbering scope: `(cafeteria_id, menu_id)` participant set.
- Participant set includes top-level menu review writers + reply comment writers in the menu target.
- Same user gets stable `Anonymous N` across pages and comment endpoints in same menu target.
- Anonymous numbering is persisted in `menu_review_anonymous_participant` when the user first writes in the menu target.
- Review/comment soft delete and user withdrawal do not reuse an already assigned anonymous number.
- Withdrawn users are displayed as `Deleted user`; USER hard withdrawal sets review/comment `user_id` to null, while MANAGER/ADMIN soft withdrawal displays `Deleted user` until account recovery.

## 16. Menu Image Analysis Daily Usage Limit (2026-05-29)
- `POST /api/v1/menus/analyze-image` is limited per user by Korean calendar day.
- Limit policy uses `mealguide.mealcrawl.menu-image.daily-analysis-limit` with default `2`.
- Day boundary uses `mealguide.mealcrawl.menu-image.daily-analysis-limit-zone-id` with default `Asia/Seoul`.
- Usage count is based on `menu_image_analysis_log.created_at` for the current user:
  - `created_at >= today 00:00`
  - `created_at < tomorrow 00:00`
- All created logs count regardless of status (`PROCESSING`, `SUCCESS`, `FAILED`).
- Invalid image requests blocked before log creation do not count.
- `GET /api/v1/menus/analyze-image/usage` returns `usedCount`, `limitCount`, `remainingCount`, `limited`, and `resetAt` for client button state.
- The analyze API still performs the same count check server-side and returns a rate-limit error when the daily limit is exhausted.

## 2026-05-15 AI analysis retry policy update
- 00:00 crawl/import flow remains unchanged, but AI follow-up now runs in batches (mealguide.mealcrawl.ai-analysis-batch-size, default 5).
- AI target selection still excludes already analyzed SUCCESS menus.
- Per-result status mapping:
  - Python SUCCESS -> SUCCESS
  - Python RETRYABLE_FAILED -> RETRY_PENDING
  - Python PERMANENT_FAILED -> FAILED_PERMANENT
- Missing result rows or batch-level Python client failures during 00:00 follow-up are stored as RETRY_PENDING (attempt 1) without failing meal import.
- Added 01:00 retry scheduler (mealguide.mealcrawl.analysis-retry-cron, default   0 1 * * *) with existing PostgreSQL advisory lock.
- 01:00 retry only reads latest menu_ai_analysis rows in RETRY_PENDING with ttempt_count < 2.
- 01:00 retry result policy:
  - success -> SUCCESS (attempt 2)
  - retryable fail or batch fail -> FAILED_RETRY_EXHAUSTED (attempt 2)
  - permanent fail -> FAILED_PERMANENT (attempt 2)
- menu_ai_analysis now stores ttempt_count to track first run vs retry run.
## 12. AI retry and batching policy update (2026-05-19)
- Menu AI status is simplified to two values only: `SUCCESS`, `FAILED`.
- Retry scheduler selects latest `FAILED` rows with `attempt_count < max_attempt_count`.
- Retry is bounded by attempt count only (no extra retryable column).
- AI analysis requests are processed in batches (`aiAnalysisBatchSize`, default 10).
- AI retry requests are processed in batches (`aiAnalysisRetryBatchSize`, default uses analysis batch size).
- Menu translation requests are processed in batches (`translationBatchSize`, default 10).

## 13. Translation retry tracking aligned with AI policy (2026-05-20)
- Translation follow-up now persists status per `(menu_id, lang_code)` in `menu_translation_analysis`.
- Status model is aligned with AI follow-up (`SUCCESS`, `FAILED`, `attempt_count`).
- Existing retry scheduler (`ai_retry_scheduler`) now runs translation retry after AI retry in the same run.
- Translation retry target selection uses latest `FAILED` with `attempt_count < translationMaxAttemptCount`.
- Translation retry requests are processed in batches (`translationRetryBatchSize`, default uses translation batch size).

## 14. Menu detail response enhancement (2026-05-21)
- Menu detail response was expanded to include:
  - `allergies` (menu detected allergy list)
  - `matchedAllergies` (user allergy intersection, with `riskLevel`, `confidence`)
  - `ingredients` (`code`, `name`, `source`)
  - `matchedReligiousIngredients` with nested matched religious restriction list
- Menu-level `risk` field was removed from menu detail response (weekly meal response keeps menu-level risk).
- Religious restriction matching uses multi-select user codes (`user_religious_food_restriction`).

## 15. Menu image analysis API (2026-05-22)
- Added `POST /api/v1/menus/analyze-image` (multipart field: `image`).
- Reuses existing Python client (`PythonMealClientPort/Adapter`) for:
  - image identification (`/api/v1/python/menus/analyze-image`)
  - menu analysis (`/api/v1/menus/analyze`)
  - menu translation (`/api/v1/python/menus/translate`)
- Added `menu_image_analysis_log` persistence with status transitions: `PROCESSING -> SUCCESS|FAILED`.
- Reuses existing user preference (`CurrentUserMealPreference`) and allergy/religious risk policy (`RiskLevelPolicyResolver`) for response assembly.
