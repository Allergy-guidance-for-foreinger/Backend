# onboarding 작업 로그

## 기록 원칙
- `onboarding` 완료 플로우, 학교 조회/번역, 사용자 초기 설정 저장 규칙 변경을 기록한다.

## 누적 핵심 변경 요약

### 완료 플로우
- `languageCode`, `schoolId`, `allergyCodes`, `religiousCode` 저장 흐름 유지
- `users.language_code`, `users.school_id`, `users.religious_code`, `users.onboarding_completed` 갱신 유지
- `user_allergy` full replacement 규칙 유지
- 완료 저장은 원자적 처리 원칙 유지
- `religiousCode` null 허용 규칙 유지

### 학교 조회
- 학교 목록 조회 시 `school_translation` fallback 규칙 유지

### 구조/매핑 주의
- `login.User`와 중복되는 `users` entity 매핑 임의 추가 금지

## 참고 문서
- 기능 맥락: `docs/features/onboarding-context.md`
- 공통 규칙: `docs/project-context.md`, `docs/database-context.md`

### 2026-05-02 (onboarding DTO 깨진 Swagger 설명 복구)
- What changed:
  - onboarding DTO 3개 파일의 깨진 Swagger `description` 문자열을 정상 문구로 정리했다.
  - 수정 범위는 `@Schema(description=...)` 문구로 제한하고, DTO 필드/검증/구조는 변경하지 않았다.
- Why:
  - API 문서에서 한글 문자열이 깨져 표시되던 문제를 복구하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/request/CompleteOnboardingRequest.java`
  - `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/response/CompleteOnboardingResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/response/SchoolListResponse.java`
  - `docs/work-log/onboarding-work-log.md`
- DB schema changed: No
- API behavior changed: No (Swagger 설명 문구만 수정)
- Related docs updated:
  - `docs/work-log/onboarding-work-log.md`
- Remaining follow-ups:
  - 없음

### 2026-05-02 (온보딩 countryCode 저장 지원)
- What changed:
  - `CompleteOnboardingRequest`/`CompleteOnboardingResponse`/`OnboardingCompletion`에 `countryCode`를 추가했다.
  - `OnboardingService`에서 `countryCode` 필수 검증 및 `country.code` 존재 검증을 추가했다.
  - 온보딩 완료 저장 시 `users.country_code`를 함께 업데이트하도록 `OnboardingCommandPort` 및 persistence 구현을 확장했다.
  - `OnboardingApi` Swagger 설명/에러(`INVALID_COUNTRY_CODE`)를 반영했다.
  - `OnboardingServiceTest`에 countryCode 정상/오류 케이스를 추가했다.
- Why:
  - users 테이블에 추가된 `country_code`를 온보딩 완료 처리에서 함께 저장하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/request/CompleteOnboardingRequest.java`
  - `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/dto/response/CompleteOnboardingResponse.java`
  - `src/main/java/com/mealguide/mealguide_api/onboarding/domain/OnboardingCompletion.java`
  - `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/controller/OnboardingController.java`
  - `src/main/java/com/mealguide/mealguide_api/onboarding/application/service/OnboardingService.java`
  - `src/main/java/com/mealguide/mealguide_api/onboarding/application/port/OnboardingCommandPort.java`
  - `src/main/java/com/mealguide/mealguide_api/onboarding/infrastructure/persistence/adapter/SchoolPersistenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/onboarding/infrastructure/persistence/repository/OnboardingUserJpaRepository.java`
  - `src/main/java/com/mealguide/mealguide_api/onboarding/presentation/swagger/OnboardingApi.java`
  - `src/test/java/com/mealguide/mealguide_api/onboarding/application/service/OnboardingServiceTest.java`
  - `docs/features/onboarding-context.md`
  - `docs/work-log/onboarding-work-log.md`
- DB schema changed: No
- API behavior changed:
  - `POST /api/v1/onboarding/complete` 요청/응답에 `countryCode`가 추가됨.
- Related docs updated:
  - `docs/features/onboarding-context.md`
  - `docs/work-log/onboarding-work-log.md`
- Remaining follow-ups:
  - 현재 환경에서는 Maven wrapper 실행 오류로 자동 테스트 실행 검증이 필요함.
