# 프로젝트 맥락

## 1. 목적
Mealguide API는 모바일 클라이언트에 급식 정보와 사용자별 식이 관련 안내를 제공하는 Spring Boot 백엔드다.

## 2. 실행 환경
### 로컬 개발
- Spring 애플리케이션: IntelliJ에서 직접 실행
- PostgreSQL: 로컬 설치 서버 사용
- Redis: Docker 실행

### 운영
- AWS EC2
- Spring 애플리케이션, PostgreSQL, Redis 모두 Docker 실행

## 3. 공통 아키텍처 규칙
- 기능 패키지 구조를 유지한다.
  - `{feature}.presentation.controller`
  - `{feature}.presentation.dto.request`
  - `{feature}.presentation.dto.response`
  - `{feature}.presentation.swagger`
  - `{feature}.application.service`
  - `{feature}.application.port`
  - `{feature}.domain`
  - `{feature}.infrastructure.persistence.repository`
  - `{feature}.infrastructure.persistence.adapter`
- Controller는 얇게 유지하고 비즈니스 로직은 service에 둔다.
- request DTO/response DTO를 분리한다.
- entity를 직접 API 응답으로 반환하지 않는다.
- persistence 상세 구현은 infrastructure에 격리한다.
- 공통 인증 인프라는 `global.auth.*`, 기능별 로그인 유스케이스는 `login.*` 경계를 유지한다.

## 4. 기능 맥락 문서
기능별 상세 책임, 주요 클래스, DB/API/비즈니스 규칙은 아래 문서를 우선 참조한다.

- `docs/features/login-context.md`
- `docs/features/onboarding-context.md`
- `docs/features/settings-context.md`
- `docs/features/mealcrawl-context.md`
- `docs/features/authdebug-context.md`

## 5. 문서 읽기 순서
1. `README.md`
2. `AGENTS.md`
3. `docs/project-context.md`
4. `docs/database-context.md`
5. `docs/work-context.md`
6. 기능 작업 시 `docs/features/{feature}-context.md`
7. DB 작업 시 `docs/schema.sql`
8. 이력 확인 시 `docs/work-log/{feature}-work-log.md`, `docs/work-log/general-work-log.md`

## 6. 문서 업데이트 규칙
- 공통 아키텍처/패키지 규칙 변경 시 본 문서를 업데이트한다.
- 기능별 상세 변경은 해당 `docs/features/*-context.md`를 우선 업데이트한다.
