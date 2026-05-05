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

### 2026-05-05 (대표/부가 알레르기 옵션 API 분리 및 allergy_group 반영)
- What changed:
  - `settings.domain.AllergyGroup` enum(`PRIMARY`, `ADDITIONAL`)을 추가했다.
  - `Allergy` entity에 `allergyGroup` 필드(`allergy_group`)를 추가했다.
  - 기존 `GET /api/v1/settings/options/allergies`를 제거했다.
  - 신규 옵션 API를 추가했다.
    - `GET /api/v1/settings/options/allergies/primary`
    - `GET /api/v1/settings/options/allergies/additional`
  - `AllergyJpaRepository` 조회를 그룹 기준 쿼리로 변경했다.
  - `SettingsMasterQueryPort`/`SettingsMasterPersistenceAdapter`/`SettingsService`를 대표/부가 조회 유스케이스로 분리했다.
  - `PUT /api/v1/settings/allergies` 저장 로직은 유지했고, `allergy.code` 존재 검증 기반이라 `PRIMARY`/`ADDITIONAL` 모두 저장 가능함을 확인했다.
  - 온보딩 `POST /api/v1/onboarding/complete`도 동일한 code 존재 검증 기반이라 `ADDITIONAL` 저장이 가능함을 테스트로 확인했다.
  - `user_avoided_ingredient`, `UserAvoidedIngredient`, `avoidedIngredient`, `avoided_ingredient` 키워드 사용처를 검색했고 코드/문서 내 사용처가 없음을 확인했다.
  - `ingredient`, `allergy_ingredient`, `religious_food_restriction_ingredient` 관련 구조는 변경하지 않았다.
  - 관련 테스트를 추가/수정했다.
- Why:
  - 실제 DB의 `allergy_group` 반영 상태와 API 설계를 일치시키고, 대표/부가 옵션을 분리 제공하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/settings/domain/AllergyGroup.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/domain/Allergy.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/infrastructure/persistence/repository/AllergyJpaRepository.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/application/port/SettingsMasterQueryPort.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/infrastructure/persistence/adapter/SettingsMasterPersistenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/application/service/SettingsService.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsOptionsController.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsOptionsApi.java`
  - `src/test/java/com/mealguide/mealguide_api/settings/infrastructure/persistence/repository/AllergyJpaRepositoryTest.java`
  - `src/test/java/com/mealguide/mealguide_api/settings/application/service/SettingsServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsOptionsControllerTest.java`
  - `src/test/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsOptionsControllerSecurityTest.java`
  - `src/test/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/onboarding/application/service/OnboardingServiceTest.java`
  - `docs/features/settings-context.md`
  - `docs/features/onboarding-context.md`
  - `docs/work-log/settings-work-log.md`
- DB schema changed: No (`docs/schema.sql` 미수정)
- API behavior changed:
  - 제거: `GET /api/v1/settings/options/allergies`
  - 추가: `GET /api/v1/settings/options/allergies/primary`, `GET /api/v1/settings/options/allergies/additional`
  - 유지: `PUT /api/v1/settings/allergies`
- Related docs updated:
  - `docs/features/settings-context.md`
  - `docs/features/onboarding-context.md`
  - `docs/work-log/settings-work-log.md`
- Remaining follow-ups:
  - 없음
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
- DB schema changed: Yes
  - `country` 테이블 추가 (code PK, name, created_at)
  - users.country_code VARCHAR(10)` 컬럼 및 `fk_users_country` 외래키 추가
  - idx_users_country_code` 인덱스 추가
- API behavior changed:
  - settings에 country 조회/수정 및 country 옵션 조회 API가 추가됨.
- Related docs updated:
  - `docs/features/settings-context.md`
  - `docs/work-log/settings-work-log.md`
- Remaining follow-ups:
  - 현재 환경에서는 Maven wrapper 실행 오류로 자동 테스트 실행 검증이 필요함.

### 2026-05-02 (학교 옵션 및 사용자 학교 설정 API 추가)
- What changed:
  - settings options API에 학교 목록 조회를 추가했다.
    - `GET /api/v1/settings/options/schools`
  - 개인 settings API에 사용자 학교 조회/수정을 추가했다.
    - `GET /api/v1/settings/school`
    - `PATCH /api/v1/settings/school`
  - `SettingsService`/`UserPreferenceService`에 학교 옵션 조회, 사용자 학교 조회/수정 유스케이스를 추가했다.
  - `SettingsMasterQueryPort`/`SettingsMasterPersistenceAdapter`에 학교 목록 조회 및 `schoolId` 존재 검증을 추가했다.
  - settings 패키지에 `School` 엔티티/`SchoolOption`/`SchoolJpaRepository`를 추가하고
    `school_translation` fallback 쿼리(번역 우선, 원문 fallback)를 구현했다.
  - 학교 관련 request/response DTO 및 Swagger 문서를 추가했다.
  - 기존 settings 언어/알레르기/종교/국가 API는 유지했다.
- Why:
  - 학교 목록 조회 책임을 settings로 이동하고, 사용자 학교 설정 조회/수정 기능을 settings에서 일관되게 제공하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/settings/application/port/SettingsMasterQueryPort.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/application/service/SettingsService.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceService.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/domain/UserPreference.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/domain/School.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/domain/SchoolOption.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/infrastructure/persistence/repository/SchoolJpaRepository.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/infrastructure/persistence/adapter/SettingsMasterPersistenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsOptionsController.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/controller/UserSettingsController.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsOptionsApi.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/swagger/SettingsApi.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/request/UpdateSchoolRequest.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/SchoolOptionItemResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/SchoolOptionsResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/SchoolSettingResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/settings/presentation/dto/response/SchoolUpdateResponse.java`
  - `src/test/java/com/mealguide/mealguide_api/settings/application/service/SettingsServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/settings/application/service/UserPreferenceServiceTest.java`
  - `src/test/java/com/mealguide/mealguide_api/settings/presentation/controller/SettingsOptionsControllerTest.java`
  - `docs/features/settings-context.md`
  - `docs/work-log/settings-work-log.md`
- DB schema changed: No
- API behavior changed:
  - `GET /api/v1/settings/options/schools` 추가
  - `GET /api/v1/settings/school` 추가
  - `PATCH /api/v1/settings/school` 추가
- Related docs updated:
  - `docs/features/settings-context.md`
  - `docs/work-log/settings-work-log.md`
- Remaining follow-ups:
  - 현재 환경에서는 Maven wrapper 실행 오류로 자동 테스트 실행 검증이 필요함.
