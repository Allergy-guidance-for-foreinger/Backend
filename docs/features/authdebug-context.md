# authdebug 기능 맥락

## 1. 역할
`authdebug` 기능은 로컬/개발 환경에서 인증 테스트를 지원하는 보조 기능이다.  
현재는 정적 `auth-test.html`이 사용할 설정 값을 제공하는 역할에 집중한다.

## 2. 주요 패키지
- `authdebug.presentation.controller`
- `authdebug.presentation.dto.response`

## 3. 주요 클래스
- `AuthDebugConfigController`: `/auth-debug/config` 제공
- `AuthDebugConfigResponse`: 디버그 설정 응답 DTO

## 4. DB 사용 규칙
DB 영향 없음

## 5. API 규칙
- 외부 API 경로
  - `GET /auth-debug/config`
- 인증 필요 여부
  - 로컬/개발 전용 설정에서만 활성화되며 운영 환경 활성화 금지
- 요청/응답 방향
  - Google OAuth client id 등 테스트 페이지 초기화에 필요한 최소 설정만 반환

## 6. 비즈니스 규칙
- `mealguide.auth-debug.enabled=true`일 때만 활성화한다.
- Swagger 기반 debug login/refresh/logout API는 제거된 상태를 유지한다.
- `/auth-debug/config`는 테스트 보조 목적 외로 확장하지 않는다.

## 7. 주의사항
- 운영 환경에서 `authdebug`를 활성화하면 안 된다.
- 실제 인증 정책 우회 경로를 다시 도입하지 않는다.
