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

## 6. 비즈니스 규칙
- onboarding 완료 시 사용자 기본 설정은 부분 성공 없이 일괄 반영되어야 한다.
- 완료 처리 후 `users.onboarding_completed = true`를 보장한다.
- 알레르기 설정은 증분 업데이트가 아닌 전체 교체로 처리한다.
- 종교 제한은 미선택(null) 상태를 정상 입력으로 허용한다.

## 7. 주의사항
- onboarding 저장 흐름에서 트랜잭션 경계가 깨지면 사용자 상태 불일치가 발생할 수 있다.
- 학교 목록/번역 조회 로직 변경 시 fallback 규칙을 유지한다.
- 요청 필드명(`languageCode`, `schoolId`, `allergyCodes`, `religiousCode`)과 저장 컬럼 매핑을 임의로 변경하지 않는다.
