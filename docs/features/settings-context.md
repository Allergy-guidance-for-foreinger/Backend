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
- `LanguageUpdateResponse`, `AllergyUpdateResponse`, `ReligionUpdateResponse`, `LanguageOptionsResponse`, `LanguageOptionItemResponse`, `AllergyOptionsResponse`, `AllergyOptionItemResponse`, `ReligionOptionsResponse`, `ReligionOptionItemResponse`: ?? DTO

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

## 6. 비즈니스 규칙
- 옵션 목록 조회 책임과 개인 설정 조회/수정 책임을 분리한다.
- 개인 알레르기 설정은 전체 교체(full replacement)로 일관 처리한다.
- 옵션 목록 조회 시 사용자 언어 컨텍스트 반영이 필요한 항목(`allergies`, `religions`)은 사용자 언어를 기반으로 번역 값을 반환한다.

## 7. 주의사항
- Controller에 비즈니스 로직을 넣지 않고 service에서 유스케이스를 조정한다.
- settings 보안 정책(ROLE 기반 접근 제어)을 약화하지 않는다.
- DTO 경계를 유지하고 entity를 직접 API 응답으로 노출하지 않는다.
