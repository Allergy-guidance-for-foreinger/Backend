# onboarding 기능 맥락

## 1. 역할
`onboarding` 기능은 최초 로그인 이후 사용자 기본 설정을 완료시키는 흐름을 담당한다.
학교 목록 조회와 onboarding 완료 저장을 제공한다.

## 2. 주요 패키지
- `onboarding.presentation.controller`
- `onboarding.presentation.dto.request`
- `onboarding.presentation.dto.response`
- `onboarding.presentation.swagger`
- `onboarding.application.service`
- `onboarding.application.port`
- `onboarding.domain`
- `onboarding.infrastructure.persistence.repository`
- `onboarding.infrastructure.persistence.adapter`

## 3. 주요 클래스
- `OnboardingController`: 학교 목록 조회/완료 API 진입점
- `OnboardingService`: onboarding 완료 오케스트레이션
- `SchoolQueryPort`, `OnboardingCommandPort`: 조회/저장 추상화
- `SchoolPersistenceAdapter`: 학교/완료 저장 포트 구현체
- `SchoolJpaRepository`, `OnboardingUserJpaRepository`, `OnboardingUserAllergyJpaRepository`: 영속성 접근
- `CompleteOnboardingRequest`, `CompleteOnboardingResponse`, `SchoolListResponse`: 요청/응답 DTO
- `OnboardingCompletion`, `OnboardingUserAllergy`: 도메인 모델

## 4. DB 사용 규칙
- 관련 테이블
  - `users`
  - `user_allergy`
  - `school`
  - `school_translation`
- 저장 컬럼
  - `users.language_code`
  - `users.school_id`
  - `users.religious_code`
  - `users.onboarding_completed`
  - `user_allergy` (사용자 알레르기 매핑)
- 조회/저장 규칙
  - onboarding 완료 시 `languageCode`, `schoolId`, `allergyCodes`, `religiousCode`를 원자적으로 반영한다.
  - `user_allergy`는 full replacement 방식으로 갱신한다.
  - `religiousCode`는 null 허용이다.
  - 학교명 조회는 `school_translation` 우선, 미존재 시 원본 학교명 fallback 규칙을 따른다.
- 주의 사항
  - `login.domain.User`와 중복되는 별도 `users` entity 매핑을 임의로 만들지 않는다.

## 5. API 규칙
- 외부 API 경로
  - `GET /api/v1/onboarding/schools`
  - `POST /api/v1/onboarding/complete`
- 요청/응답 방향
  - `schools`: `lang` 쿼리 기반 학교 목록 응답
  - `complete`: `CompleteOnboardingRequest(languageCode, schoolId, allergyCodes, religiousCode)` -> `CompleteOnboardingResponse`
- 인증 필요 여부
  - `GET /api/v1/onboarding/schools`: 인증 불필요
  - `POST /api/v1/onboarding/complete`: `USER`/`MANAGER`/`ADMIN` 인증 필요

## 6. 공통 비즈니스 규칙
- onboarding은 최초 로그인 이후 사용자 초기 설정 완료 흐름이다.
- onboarding 완료 시 언어, 학교, 알레르기, 종교 제한 설정을 함께 저장한다.
- `users.language_code`, `users.school_id`, `users.religious_code`, `users.onboarding_completed`를 업데이트한다.
- `user_allergy`는 전체 교체(full replacement) 방식으로 저장한다.
- `religiousCode`는 사용자가 선택하지 않으면 null 가능하다.
- onboarding 완료 처리는 원자적으로 수행되어야 한다.
- `users` 테이블을 중복 entity로 매핑하지 말고 기존 login `User` 매핑 또는 update query 정책을 따른다.

## 7. API별 비즈니스 규칙

### 7.1 `GET /api/v1/onboarding/schools`
- 학교 선택 화면에 필요한 학교 목록을 제공한다.
- 요청 언어의 `school_translation`이 있으면 번역명을 사용한다.
- 요청 언어 번역이 없으면 `school.name`으로 fallback한다.
- 인증 필요 여부: 인증 불필요.

### 7.2 `POST /api/v1/onboarding/complete`
- 인증된 사용자만 호출할 수 있다.
- 요청에는 `languageCode`, `schoolId`, `allergyCodes`, `religiousCode`가 포함된다.
- `languageCode`, `schoolId`, `allergyCodes`, `religiousCode`가 유효한 master data인지 검증한다.
- `religiousCode`가 null이면 종교 제한 없음으로 저장한다.
- 기존 `user_allergy`를 삭제하고 요청 allergy 목록으로 전체 교체한다.
- 사용자 설정 저장이 모두 성공하면 `users.onboarding_completed = true`로 변경한다.
- 일부 저장만 성공한 상태가 남지 않도록 transaction 경계를 유지한다.

## 8. 주의사항
- onboarding 저장 흐름에서 트랜잭션 경계가 깨지면 사용자 상태 불일치가 발생할 수 있다.
- 학교 목록/번역 조회 로직 변경 시 fallback 규칙을 유지한다.
- 요청 필드명(`languageCode`, `schoolId`, `allergyCodes`, `religiousCode`)과 저장 컬럼 매핑을 임의로 변경하지 않는다.
