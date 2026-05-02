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
