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
