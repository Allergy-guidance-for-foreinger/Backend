# 공통 작업 로그

## 기록 원칙
- 기능 비특화 작업(문서 체계, 공통 인프라, 저장소 규칙)은 이 문서에 기록한다.
- 기능 상세 구현 변경은 각 기능 `work-log`에 기록한다.

## 최근 공통 작업

### 2026-04-26 (cafeteria 조회 서비스 null 방어 보강)
- What changed:
  - `CafeteriaQueryService.requireSchoolId`에 `CurrentUserMealPreference` null 방어 로직을 추가했다.
  - `preference == null`일 때 `USER_NOT_FOUND` 예외를 던지도록 수정했다.
- Why:
  - 포트 구현 변경/오류 상황에서 NPE가 발생하지 않도록 서비스 계층 방어 코드를 보강하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/CafeteriaQueryService.java`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed: No (예외 처리 안정성만 보강)
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - 현재 환경의 Maven wrapper 실행 오류로 자동 테스트 실행 검증 필요.

### 2026-04-26 (인증 사용자 schoolId 기준 cafeteria 목록 조회 API 추가)
- What changed:
  - 인증된 사용자 `schoolId` 기준으로 식당 목록을 조회하는 API를 `mealcrawl` 기능에 추가했다.
  - 신규 엔드포인트 `GET /api/v1/mealcrawl/cafeterias`를 추가했다.
  - `CafeteriaQueryService`에서 현재 사용자 선호(`MealUserPreferencePort`)를 통해 `schoolId`를 가져오고, null이면 `BINDING_ERROR`를 반환하도록 처리했다.
  - `CafeteriaQueryPort`/`CafeteriaQueryPersistenceAdapter`/`CafeteriaJpaRepository` 조회 메서드를 추가해 `cafeteria.school_id = :schoolId` 조건으로 `id ASC` 정렬 조회하도록 구현했다.
  - 응답 DTO `CafeteriaListResponse`, `CafeteriaItemResponse`를 추가해 entity 직접 노출 없이 반환하도록 구성했다.
  - `CafeteriaQueryServiceTest`를 추가해 정상 조회, 빈 목록, `schoolId` null 예외를 검증했다.
- Why:
  - 인증 사용자 소속 학교에 해당하는 식당 목록을 식단 조회 흐름에서 바로 사용할 수 있도록 제공하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/presentation/controller/CafeteriaController.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/presentation/swagger/CafeteriaApi.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/presentation/dto/response/CafeteriaListResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/presentation/dto/response/CafeteriaItemResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/CafeteriaQueryService.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/port/CafeteriaQueryPort.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/dto/CafeteriaRow.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/adapter/CafeteriaQueryPersistenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/CafeteriaJpaRepository.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/CafeteriaQueryServiceTest.java`
  - `docs/features/mealcrawl-context.md`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed:
  - 신규 API `GET /api/v1/mealcrawl/cafeterias` 추가
- Related docs updated:
  - `docs/features/mealcrawl-context.md`
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - 현재 환경의 Maven wrapper 실행 오류로 테스트 자동 실행 검증 필요.

### 2026-04-26 (JSON 필드명 단언 오탐 보정)
- What changed:
  - `WeeklyMealResponseAssemblerTest`의 JSON 단언을 부분 문자열 검사에서 필드 토큰 검사로 변경했다.
  - `mealMenuId` 포함 검증은 `"mealMenuId"`로, legacy `menuId` 부재 검증은 `"menuId"` 부재로 수정했다.
- Why:
  - `mealMenuId`가 `menuId` 부분 문자열을 포함해 `doesNotContain("menuId")`가 오탐/실패할 수 있어 테스트 의도를 정확히 반영하기 위해.
- Affected files:
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealResponseAssemblerTest.java`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed: No
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - 현재 환경의 Maven wrapper 실행 오류로 테스트 자동 실행 검증 필요.

### 2026-04-26 (weekly mealType 시간순 정렬 보정)
- What changed:
  - `MealMenuJpaRepository.findWeeklyMealsForCache` 정렬을 문자열 사전순(`mealType asc`)에서 식사 시간 우선순위 정렬로 변경했다.
  - `mealType` 정렬 우선순위를 JPQL `case`로 명시했다: `BREAKFAST(1) -> LUNCH(2) -> DINNER(3) -> 기타(99)`.
  - 같은 우선순위 내 안정성을 위해 `mealType asc`, `displayOrder asc`를 후순위 정렬로 유지했다.
  - `MealImportServiceDatabaseIntegrationTest`에 `BREAKFAST/LUNCH/DINNER` 순서 보장 통합 테스트를 추가했다.
- Why:
  - 사전식 정렬에서는 `DINNER`가 `LUNCH`보다 앞서 시간순 UI가 깨질 수 있어 주간 식단 표시 일관성을 보장하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/repository/MealMenuJpaRepository.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealImportServiceDatabaseIntegrationTest.java`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed:
  - 응답 스키마는 동일하며, 같은 날짜 내 `mealType` 출력 순서가 시간순으로 보정됨.
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - 현재 환경의 Maven wrapper 실행 오류로 테스트 자동 실행 검증 필요.

### 2026-04-26 (AI ingredient 존재 판별 쿼리 정합성 보정)
- What changed:
  - `MealCrawlPersistenceAdapter.findMealMenuIdsHavingAiIngredients` SQL을 최신 SUCCESS 분석 기준으로 재작성했다.
  - 기존의 “SUCCESS 분석 존재 여부” 판별에서 “최신 SUCCESS 분석에 재료 행(menu_ai_analysis_ingredient) 존재 여부” 판별로 변경했다.
  - `findAiIngredientsByMealMenuIds`와 동일한 최신 분석 기준을 맞춰 두 메서드 간 의미 불일치를 제거했다.
- Why:
  - 최신 SUCCESS 분석에 재료가 없는데 이전 SUCCESS 분석만 존재하는 경우를 AI 재료 존재로 오판하는 위험을 줄이기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/infrastructure/persistence/adapter/MealCrawlPersistenceAdapter.java`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed: No direct API contract change
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - 현재 환경에서 Maven wrapper 실행 오류가 있어 자동 테스트 실행 검증 필요.

### 2026-04-26 (weekly meal fallback 응답 번역명 일관화)
- What changed:
  - `WeeklyMealQueryService`에서 payload 로드 직후 번역 메뉴명 맵을 1회 조회하고, 성공 경로와 예외 fallback 경로 모두 동일 맵을 사용하도록 변경했다.
  - `toResponseWithUnknownRisk`가 `translatedMenuNamesByMealMenuId`를 받아 번역 메뉴명을 적용하도록 수정했다.
  - `WeeklyMealResponseAssembler`에 `assemble(payload, preference, translatedMenuNames)` 오버로드와 `resolveTranslatedMenuNames(...)`를 추가해 QueryService가 번역 맵을 선조회할 수 있게 했다.
  - `WeeklyMealQueryServiceTest`에 “assembler 예외 발생 시에도 fallback에서 번역명 유지” 테스트를 추가했다.
- Why:
  - 위험도 계산 예외 시점에만 메뉴명이 원문으로 노출되는 다국어 불일치를 제거하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealQueryService.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealResponseAssembler.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealQueryServiceTest.java`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed:
  - 응답 스키마 변화 없음.
  - 위험도 fallback(UNKNOWN) 응답에서도 사용자 언어 번역 메뉴명이 유지됨.
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - 현재 환경의 Maven wrapper 실행 오류로 테스트 실행 검증 필요.

### 2026-04-26 (weekly cache key 주 시작일 정규화)
- What changed:
  - `WeekStartDateNormalizer`를 추가해 입력 날짜를 월요일(week start)로 정규화하는 공통 로직을 도입했다.
  - `MealCrawlTargetService.resolveWeeklyTargets`에서 스케줄러 크롤링 타깃 시작일/종료일을 월~일 기준으로 계산하도록 변경했다.
  - `WeeklyMealQueryService`에서 조회 파라미터 `weekStartDate`를 정규화한 값으로 캐시 조회/DB fallback을 수행하도록 변경했다.
  - `WeeklyMealCacheRefreshService`에서 캐시 refresh/DB 로드 시 `weekStartDate`를 정규화해 캐시 키와 조회 범위를 일관화했다.
  - 관련 테스트(`MealCrawlTargetServiceTest`, `WeeklyMealQueryServiceTest`, `WeeklyMealCacheRefreshServiceTest`)에 비월요일 입력 정규화 검증을 추가했다.
- Why:
  - 크롤링 캐시 갱신(쓰기)과 주간 식단 조회(읽기)에서 동일 주간 데이터가 서로 다른 키로 분리되는 문제를 방지하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeekStartDateNormalizer.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealCrawlTargetService.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealQueryService.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealCacheRefreshService.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/MealCrawlTargetServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealQueryServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealCacheRefreshServiceTest.java`
  - `docs/features/mealcrawl-context.md`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed:
  - `weekStartDate`에 비월요일 날짜가 들어와도 같은 주의 월요일 기준으로 처리됨.
- Related docs updated:
  - `docs/features/mealcrawl-context.md`
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - 현재 환경에서 Maven wrapper 실행 오류(`Cannot start maven from wrapper`)로 테스트 실행 검증 필요.

### 2026-04-26 (weekly meal risk reason message 다국어 보정)
- What changed:
  - `WeeklyMealResponseAssembler`의 위험 사유 메시지 하드코딩(영어)을 제거하고 사용자 언어코드 기반 메시지 선택 로직을 추가했다.
  - `ko` 언어코드일 때 한국어 메시지, 그 외 언어는 영어 메시지를 반환하도록 적용했다.
  - `WeeklyMealResponseAssemblerTest`에 영어 메시지 검증과 한국어 메시지 검증 케이스를 추가했다.
- Why:
  - 다국어 응답 방향과 맞지 않는 영어 하드코딩 메시지로 인한 사용자 혼란을 줄이기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealResponseAssembler.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealResponseAssemblerTest.java`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed:
  - 응답 스키마는 동일하고, `risk.reasons[].message` 값이 사용자 언어코드에 따라 한국어/영어로 달라질 수 있음.
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - 현재 환경에서 Maven wrapper 실행 오류가 있어 테스트 자동 실행 검증 필요.

### 2026-04-26 (weekly meal cache aiAnalyzed 상태 판정 보정)
- What changed:
  - `WeeklyMealCacheRefreshService.isAiAnalyzed`를 `PENDING 이외 = true`에서 `SUCCESS만 true`로 변경했다.
  - AI 상태가 `FAILURE`일 때 `aiAnalyzed=false`가 유지되도록 `WeeklyMealCacheRefreshServiceTest`에 회귀 검증 케이스를 추가했다.
- Why:
  - 분석 실패 상태를 분석 완료로 노출하면 사용자에게 오해를 줄 수 있어, 성공 상태만 완료로 표시하도록 보정하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealCacheRefreshService.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealCacheRefreshServiceTest.java`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed:
  - 응답 스키마 변경은 없고, `aiAnalyzed` 값 산정 로직만 변경됨(`FAILURE` 등 비성공 상태는 `false`).
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - 현재 환경의 Maven wrapper 실행 오류가 지속되면 IDE 환경에서 테스트 재확인 필요.

### 2026-04-26 (weekly meal risk 조립 DB 조회 최적화)
- What changed:
  - `WeeklyMealResponseAssembler`에서 확정/AI 재료 존재 여부 확인용 별도 조회(`findMealMenuIdsHavingConfirmedIngredients`, `findMealMenuIdsHavingAiIngredients`)를 제거했다.
  - 실제 재료 조회 결과를 `groupByMealMenuId`로 묶은 뒤 `keySet`을 사용해 확정/AI 대상 `mealMenuId`를 판별하도록 변경했다.
  - AI 조회 대상은 `mealMenuIds - confirmedMealMenuIds`로 제한해 기존 우선순위(확정 우선, AI 후순위)를 유지했다.
- Why:
  - 주간 응답 조립 시 불필요한 존재 확인 조회를 줄여 DB 호출 횟수를 감소시키기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/WeeklyMealResponseAssembler.java`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed: No
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - 현재 환경에서 Maven wrapper 실행 오류(`Cannot start maven from wrapper`)로 테스트를 실행하지 못함. 로컬 IDE/정상 wrapper 환경에서 `WeeklyMealResponseAssemblerTest` 재검증 필요.

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

### 2026-04-26 (cafeteria schoolId null error code semantic alignment)
- What changed:
  - Changed `CafeteriaQueryService.requireSchoolId` null-school handling from `BINDING_ERROR` to `ESSENTIAL_FIELD_MISSING_ERROR`.
  - Updated `CafeteriaApi` swagger failure response to `ESSENTIAL_FIELD_MISSING_ERROR`.
  - Updated `CafeteriaQueryServiceTest` to assert exact error code for null `schoolId`.
- Why:
  - Null `schoolId` is a missing required business field, not a request binding conversion failure.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/application/service/CafeteriaQueryService.java`
  - `src/main/java/com/mealguide/mealguide_api/mealcrawl/presentation/swagger/CafeteriaApi.java`
  - `src/test/java/com/mealguide/mealguide_api/mealcrawl/application/service/CafeteriaQueryServiceTest.java`
  - `docs/work-log/general-work-log.md`
- DB schema changed: No
- API behavior changed: No response schema change (error code semantics updated)
- Related docs updated:
  - `docs/work-log/general-work-log.md`
- Remaining follow-ups:
  - Maven wrapper execution issue remains in this environment; run local test verification where wrapper works.
