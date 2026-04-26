# settings 기능 맥락

## 1. 역할
`settings` 기능은 개인 설정 조회/수정과 설정 옵션(마스터 데이터) 조회를 담당한다.
언어, 알레르기, 종교 제한 설정을 사용자 컨텍스트 기준으로 제공한다.

## 2. 주요 패키지
- `settings.presentation.controller`
- `settings.presentation.dto.request`
- `settings.presentation.dto.response`
- `settings.presentation.swagger`
- `settings.application.service`
- `settings.application.port`
- `settings.domain`
- `settings.infrastructure.persistence.repository`
- `settings.infrastructure.persistence.adapter`

## 3. 주요 클래스
- `UserSettingsController`: 개인 설정 조회/수정 API
- `SettingsOptionsController`: 옵션 목록 조회 API
- `UserPreferenceService`: 개인 설정 조회/수정 유스케이스
- `SettingsService`: 옵션 마스터 데이터 조회 유스케이스
- `UserPreferencePort`, `SettingsMasterQueryPort`: 포트 추상화
- `UserPreferencePersistenceAdapter`, `SettingsMasterPersistenceAdapter`: 포트 구현체
- `UserPreferenceJpaRepository`, `UserAllergyJpaRepository`, `LanguageJpaRepository`, `AllergyJpaRepository`, `ReligiousFoodRestrictionJpaRepository`
- `UpdateLanguageRequest`, `UpdateAllergiesRequest`, `UpdateReligionRequest`: 요청 DTO
- `LanguageUpdateResponse`, `AllergyUpdateResponse`, `ReligionUpdateResponse`, `LanguageOptionsResponse`, `LanguageOptionItemResponse`, `AllergyOptionsResponse`, `AllergyOptionItemResponse`, `ReligionOptionsResponse`, `ReligionOptionItemResponse`: 응답 DTO

## 4. DB 사용 규칙
- 관련 테이블
  - `users`
  - `user_allergy`
  - `language`, `allergy`, `religious_food_restriction`
  - `allergy_translation`, `religious_food_restriction_translation`
- 핵심 컬럼
  - `users.language_code`
  - `users.religious_code`
  - `user_allergy` 매핑 컬럼들
- 조회/저장 규칙
  - 개인 설정 조회/수정은 `users` + `user_allergy` 기준으로 처리한다.
  - 알레르기 수정은 full replacement 방식으로 처리한다.
  - 옵션 목록 조회는 마스터/번역 테이블을 조합해 반환한다.

## 5. API 규칙
- 외부 API 경로
  - 개인 설정
    - `GET /api/v1/settings/language`
    - `GET /api/v1/settings/allergies`
    - `GET /api/v1/settings/religion`
    - `PATCH /api/v1/settings/language`
    - `PUT /api/v1/settings/allergies`
    - `PATCH /api/v1/settings/religion`
  - 옵션 목록
    - `GET /api/v1/settings/options/languages`
    - `GET /api/v1/settings/options/allergies`
    - `GET /api/v1/settings/options/religions`
- 인증 필요 여부
  - settings API 전체는 `USER`, `MANAGER`, `ADMIN` 인증이 필요하다.
- 요청/응답 방향
  - request DTO와 response DTO를 분리 유지한다.
  - `settings.presentation.dto.request`, `settings.presentation.dto.response` 구조를 유지한다.

## 6. 공통 비즈니스 규칙
- settings 기능은 인증된 사용자의 개인 설정 조회/수정과 설정 옵션 목록 조회를 담당한다.
- 언어 설정은 `users.language_code`를 사용한다.
- 종교 제한 설정은 `users.religious_code`를 사용하며 null 가능하다.
- 알레르기 설정은 `user_allergy`를 사용하고 전체 교체(full replacement) 방식으로 저장한다.
- 옵션 목록은 `language`, `allergy`, `religious_food_restriction` 및 각 translation 테이블을 사용한다.
- request DTO와 response DTO를 분리한다.
- `settings.presentation.dto.request`와 `settings.presentation.dto.response` 구조를 유지한다.
- 설정 관련 API는 `USER`, `MANAGER`, `ADMIN` 권한을 요구한다.

## 7. API별 비즈니스 규칙

### 7.1 `GET /api/v1/settings/options/languages`
- 선택 가능한 전체 언어 목록을 반환한다.
- 각 항목은 언어 코드와 표시 이름을 포함한다.
- 인증된 사용자 기준으로 호출되는 설정 옵션 API로 관리한다.

### 7.2 `GET /api/v1/settings/options/allergies`
- 선택 가능한 전체 알레르기 목록을 반환한다.
- 표시 이름은 인증 사용자의 현재 언어 설정을 기준으로 현지화한다.
- 번역이 없으면 기본 이름으로 fallback한다.

### 7.3 `GET /api/v1/settings/options/religions`
- 선택 가능한 전체 종교 제한 목록을 반환한다.
- 표시 이름은 인증 사용자의 현재 언어 설정을 기준으로 현지화한다.
- 번역이 없으면 기본 이름으로 fallback한다.
- 종교 제한 없음은 `religiousCode = null` 정책으로 처리한다.

### 7.4 `GET /api/v1/settings/language`
- 인증 사용자의 현재 `languageCode`를 반환한다.
- code-only 응답 정책이면 표시 이름을 포함하지 않는다.

### 7.5 `PATCH /api/v1/settings/language`
- 인증 사용자의 언어 설정을 변경한다.
- 요청 `languageCode`가 존재하는 master code인지 검증한다.
- 성공 시 `users.language_code`를 업데이트한다.

### 7.6 `GET /api/v1/settings/allergies`
- 인증 사용자의 현재 알레르기 코드 목록을 반환한다.
- code-only 응답 정책이면 표시 이름을 포함하지 않는다.
- 반환 순서는 현재 코드 정책 또는 `display_order` 기준을 따른다.

### 7.7 `PUT /api/v1/settings/allergies`
- 인증 사용자의 알레르기 설정을 전체 교체한다.
- 요청 allergy code들이 모두 유효한지 검증한다.
- 중복 code가 들어오면 현재 정책에 따라 중복을 제거하거나 오류 처리한다.
- 기존 `user_allergy`를 삭제하고 새 목록을 저장한다.

### 7.8 `GET /api/v1/settings/religion`
- 인증 사용자의 현재 `religiousCode`를 반환한다.
- 종교 제한이 없으면 null을 반환한다.
- code-only 응답 정책이면 표시 이름을 포함하지 않는다.

### 7.9 `PATCH /api/v1/settings/religion`
- 인증 사용자의 종교 제한 설정을 변경한다.
- 요청 `religiousCode`가 null이면 종교 제한 없음으로 저장한다.
- null이 아니면 유효한 master code인지 검증한다.
- 성공 시 `users.religious_code`를 업데이트한다.

## 8. 주의사항
- Controller에 비즈니스 로직을 넣지 않고 service에서 유스케이스를 조정한다.
- settings 보안 정책(ROLE 기반 접근 제어)을 약화하지 않는다.
- DTO 경계를 유지하고 entity를 직접 API 응답으로 노출하지 않는다.
