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
