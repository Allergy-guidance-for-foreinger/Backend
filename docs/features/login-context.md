# login 기능 맥락

## 1. 역할
`login` 기능은 모바일 클라이언트의 Google ID token 기반 인증을 처리하고, access token/refresh token 발급과 재발급, 로그아웃을 담당한다.  
최초 로그인 시 계정이 없으면 자동 회원가입을 수행한다.

## 2. 주요 패키지
- `login.presentation.controller`
- `login.presentation.dto.request`
- `login.presentation.dto.response`
- `login.presentation.swagger`
- `login.application.service`
- `login.application.port`
- `login.domain`
- `login.infrastructure.persistence.repository`
- `login.infrastructure.persistence.adapter`
- `login.infrastructure.google`
- 경계 연계: `global.auth.*` (공통 인증 인프라)

## 3. 주요 클래스
- `AuthController`: `/auth/login`, `/auth/refresh`, `/auth/logout` 엔드포인트 제공
- `LoginService`: 로그인, 토큰 재발급, 로그아웃 유스케이스 오케스트레이션
- `UserQueryPort`: 사용자 조회/검증 추상화
- `UserPersistenceAdapter`: `UserQueryPort` 구현체
- `UserJpaRepository`, `UserOauthAccountJpaRepository`: 사용자/연동 계정 영속성 접근
- `User`, `UserOauthAccount`, `UserRole`, `UserStatus`: 인증 도메인 모델
- `GoogleTokenInfoClient`: Google ID token 검증 연동
- `RedisRefreshTokenAdapter` (`global.auth.redis`): refresh token 저장/회전
- `JwtAuthenticationFilter` (`global.auth.security`): 인증 필터에서 활성 사용자 검증

## 4. DB 사용 규칙
- 관련 테이블
  - `users`
  - `user_oauth_accounts`
- 핵심 컬럼
  - `users.id` (identity)
  - `users.email` (nullable)
  - `users.status`, `users.role`, `users.deleted_at`
  - `user_oauth_accounts.id` (identity)
  - `user_oauth_accounts.user_id`, `provider`, `provider_user_id`, `provider_email`
- 조회/저장 규칙
  - 인증 조회 대상은 `ACTIVE` 사용자만 포함한다.
  - `users.deleted_at` 또는 `status = INACTIVE` 사용자는 일반 인증 조회에서 제외한다.
  - `users.email`이 nullable이므로 email 단독 조회에 의존하지 않는다.
  - Google 계정 매핑은 `user_oauth_accounts` 기준으로 처리한다.
  - 최초 로그인 자동 회원가입은 `users` + `user_oauth_accounts`를 함께 생성한다.
- 주의 사항
  - `users.id`, `user_oauth_accounts.id`는 PostgreSQL identity column 기준이어야 한다.

## 5. API 규칙
- 외부 API 경로
  - `POST /auth/login`
  - `POST /auth/refresh`
  - `POST /auth/logout`
- 요청/응답 방향
  - `login`: `LoginRequest(idToken, deviceId)` -> `AuthResponse(accessToken, refreshToken, expiresIn, refreshExpiresIn, role, onboardingCompleted, schoolId)`
  - `refresh`: `RefreshTokenRequest(refreshToken)` -> `AuthResponse(...)`
  - `logout`: `LogoutRequest(refreshToken)` -> 성공 응답
- 인증 필요 여부
  - `POST /auth/login`: 인증 불필요
  - `POST /auth/refresh`: 인증 불필요(유효한 refresh token 필요)
  - `POST /auth/logout`: 인증 필요(`@CurrentUserId`)

## 6. 비즈니스 규칙
- Google ID token 검증 성공 시 사용자 계정을 식별한다.
- 계정이 없으면 자동 회원가입을 수행한다.
- refresh token은 PostgreSQL이 아니라 Redis에서 관리한다.
- access token은 요청 인증, refresh token은 access token 재발급 용도로 분리한다.
- 역할 값은 `USER`, `ADMIN`, `MANAGER`만 사용한다.
- `global.auth`는 공통 인증 인프라, `login.*`은 로그인 유스케이스 책임을 유지한다.

## 7. 주의사항
- refresh token 회전은 원자적 비교-교체가 필요하다(경쟁 상태 방지).
- JWT 필터/재발급 경로에서 비활성 사용자 차단 규칙을 동일하게 유지한다.
- 사용자 조회 최적화 시에도 활성 사용자 검증 규칙은 약화하면 안 된다.
- 깨진 한글이나 Swagger 문구 이슈가 반복된 이력이 있으므로 인코딩(UTF-8 without BOM)을 항상 점검한다.
