# settings 작업 로그

## 기록 원칙
- 개인 설정 조회/수정, 옵션 목록, 권한 정책, DTO 경계 변경을 기록한다.

## 누적 핵심 변경 요약

### 개인 설정
- 언어/알레르기/종교 제한 개인 설정 조회/수정 API 유지
- `users.language_code`, `users.religious_code`, `user_allergy` 사용 규칙 유지
- 알레르기 설정 full replacement 규칙 유지

### 옵션 목록
- `language`/`allergy`/`religious_food_restriction` 마스터 + 번역 테이블 기반 조회 유지
- 옵션 목록 책임과 개인 설정 조회/수정 책임 분리 유지

### 권한/구조
- settings API는 `USER`, `MANAGER`, `ADMIN` 인증 필요 정책 유지
- request/response DTO 분리 유지
- `settings.presentation.dto.request` / `settings.presentation.dto.response` 구조 유지

## 참고 문서
- 기능 맥락: `docs/features/settings-context.md`
- 공통 규칙: `docs/project-context.md`, `docs/database-context.md`
### 2026-05-02 (settings DTO 깨진 Swagger 설명 복구)
- What changed:
  - settings DTO(request/response)에서 깨진 Swagger `description` 문자열을 정상 한글로 복구했다.
  - 수정 범위는 DTO의 `@Schema(description=...)` 문구로 제한했고, 필드/검증/응답 구조는 변경하지 않았다.
- Why:
  - API 문서에서 설정 DTO 설명이 깨져 표시되던 문제를 수정하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/request/UpdateAllergiesRequest.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/request/UpdateLanguageRequest.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/request/UpdateReligionRequest.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/AllergyOptionItemResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/AllergyOptionsResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/AllergyUpdateResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/LanguageOptionItemResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/LanguageOptionsResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/LanguageUpdateResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/ReligionOptionItemResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/ReligionOptionsResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/ReligionUpdateResponse.java`
  - `docs/work-log/settings-work-log.md`
- DB schema changed: No
- API behavior changed: No (Swagger 설명 문구만 수정)
- Related docs updated:
  - `docs/work-log/settings-work-log.md`
- Remaining follow-ups:
  - 없음



### 2026-05-02 (settings country 설정/옵션 API 추가)
- What changed:
  - 개인 설정 API에 나라 조회/수정 엔드포인트를 추가했다.
    - `GET /api/v1/settings/country`
    - `PATCH /api/v1/settings/country`
  - 옵션 API에 나라 목록 조회 엔드포인트를 추가했다.
    - `GET /api/v1/settings/options/countries`
  - `SettingsService`, `UserPreferenceService`, `SettingsMasterQueryPort`, `UserPreferencePort`에 country 유스케이스를 추가했다.
  - `SettingsMasterPersistenceAdapter`에 country 목록 조회/존재 검증 구현을 추가했다.
  - `Country` 엔티티/`CountryJpaRepository`/country 관련 DTO를 추가했다.
  - `UserPreference`에 `countryCode` 필드를 추가하고 업데이트 로직을 반영했다.
  - `SettingsApi`/`SettingsOptionsApi` Swagger 문서를 업데이트했다.
  - 관련 서비스/컨트롤러 테스트를 보강했다.
- Why:
  - `users.country_code` 및 `country` master data 추가에 맞춰 세팅 화면에서 국가 설정과 선택지 조회를 지원하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/settings/application/port/SettingsMasterQueryPort.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/application/port/UserPreferencePort.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/application/service/SettingsService.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceService.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/domain/UserPreference.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/domain/Country.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/domain/CountryOption.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/infrastructure/persistence/repository/CountryJpaRepository.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/infrastructure/persistence/repository/UserPreferenceJpaRepository.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/infrastructure/persistence/adapter/SettingsMasterPersistenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/infrastructure/persistence/adapter/UserPreferencePersistenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/UserSettingsController.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsOptionsController.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/request/UpdateCountryRequest.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/CountryUpdateResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/CountryOptionItemResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/CountryOptionsResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsOptionsApi.java`
  - `src/test/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/settings/application/service/SettingsServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsOptionsControllerTest.java`
  - `docs/features/settings-context.md`
  - `docs/work-log/settings-work-log.md`
- DB schema changed: No
- API behavior changed:
  - settings에 country 조회/수정 및 country 옵션 조회 API가 추가됨.
- Related docs updated:
  - `docs/features/settings-context.md`
  - `docs/work-log/settings-work-log.md`
- Remaining follow-ups:
  - 현재 환경에서는 Maven wrapper 실행 오류로 자동 테스트 실행 검증이 필요함.
