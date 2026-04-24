# login 작업 로그

## 기록 원칙
- `login` 기능의 인증 흐름, 토큰 정책, 조회 규칙 변경을 기록한다.

## 누적 핵심 변경 요약

### 인증/계정
- Google ID token 기반 로그인 유지
- 최초 로그인 자동 회원가입 흐름 유지(`users` + `user_oauth_accounts`)
- `users.email` nullable 제약을 고려해 email 단독 조회 의존 금지
- 활성 사용자(`ACTIVE`) 중심 조회 규칙 강화

### 토큰/보안
- refresh token 저장소를 PostgreSQL이 아닌 Redis로 유지
- refresh token 회전 로직 원자성 보강(경쟁 조건 대응)
- refresh token 원문 저장 대신 해시 저장 적용
- JWT 필터에서 활성 사용자 검증 강화

### 도메인/스키마 주의
- `users.id`, `user_oauth_accounts.id`는 identity column 전제 유지
- soft delete 및 `INACTIVE` 사용자 제외 규칙 유지
- role 값은 `USER`, `ADMIN`, `MANAGER` 범위 사용

## 참고 문서
- 기능 맥락: `docs/features/login-context.md`
- 공통 규칙: `docs/project-context.md`, `docs/database-context.md`
