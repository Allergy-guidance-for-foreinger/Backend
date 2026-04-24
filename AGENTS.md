# AGENTS.md

## 1. 목표
이 문서는 본 프로젝트에서 CLI 에이전트 및 코딩 어시스턴트가 따라야 할 작업 규칙을 정의한다.

에이전트는 사용자 요청을 수행하기 전에 기존 프로젝트 구조와 관련 문서를 먼저 이해하고, 최소 변경으로 정확하게 반영해야 한다.

---

## 2. 필수 문서 읽기 순서

작업 시작 전 아래 파일을 순서대로 읽는다.

1. `README.md`
2. `AGENTS.md`
3. `docs/project-context.md`
4. `docs/database-context.md`
5. `docs/work-context.md`

기능 작업의 경우 아래 파일을 추가로 읽는다.

6. `docs/features/{feature}-context.md`

DB 관련 작업의 경우 아래 파일을 추가로 읽는다.

7. `docs/schema.sql`

이전 작업 이력이 필요하면 아래 파일을 추가로 읽는다.

- `docs/work-log/{feature}-work-log.md`
- `docs/work-log/general-work-log.md`

읽기 단계를 생략하지 않는다.

### 문서 역할
- `README.md`: 공개용 프로젝트 개요
- `docs/project-context.md`: 상세 프로젝트 구조, 아키텍처, 작업 맥락
- `docs/database-context.md`: DB 요약 및 비즈니스 규칙
- `docs/work-context.md`: 누적 작업 이력 및 변경 맥락
- `docs/features/{feature}-context.md`: 기능별 맥락 및 작업 노트
- `docs/work-log/{feature}-work-log.md`: 기능별 작업 이력
- `docs/work-log/general-work-log.md`: 기능 비특화 작업 이력
- `docs/schema.sql`: PostgreSQL 스키마 기준 문서(source-of-truth)

---

## 3. 프로젝트 구조 규칙

프로젝트는 기능별 패키지를 사용하며, 각 기능 패키지는 동일한 내부 레이어 구조를 유지한다.

```text
{feature}
- presentation
  - controller
  - dto
    - request
    - response
  - swagger
- application
  - service
  - port
- domain
- infrastructure
  - persistence
    - repository
    - adapter
```

아래 규칙을 따른다.

HTTP 엔드포인트는 `{feature}.presentation.controller`에 둔다.  
요청 DTO는 `{feature}.presentation.dto.request`에 둔다.  
응답 DTO는 `{feature}.presentation.dto.response`에 둔다.  
Swagger/OpenAPI 구성은 `{feature}.presentation.swagger`에 둔다.  
유스케이스 오케스트레이션 로직은 `{feature}.application.service`에 둔다.  
저장소 접근 추상화는 `{feature}.application.port`에 둔다.  
핵심 도메인 모델과 비즈니스 개념은 `{feature}.domain`에 둔다.  
영속성 구현은 `{feature}.infrastructure.persistence`에 둔다.  
Spring Data repository는 `{feature}.infrastructure.persistence.repository`에 둔다.  
application port 구현체는 `{feature}.infrastructure.persistence.adapter`에 둔다.

레이어 간 클래스를 임의로 이동하지 않는다.  
사용자 명시 요청이 없으면 다른 아키텍처를 도입하지 않는다.

현재 기능 패키지는 `login`, `onboarding`, `settings`를 포함한다.

기능 간 공통 인증 인프라는 `global.auth` 아래에 둔다.  
기능 특화 로그인 로직은 기능 패키지 내부에 유지한다.

## 4. 아키텍처 동작 규칙
- Controller는 얇게 유지한다.
- Controller에 비즈니스 로직을 두지 않는다.
- Controller에서 entity를 직접 응답으로 반환하지 않는다.
- Request DTO와 Response DTO를 분리한다.
- 기능 `application.service`가 유스케이스를 조정한다.
- 기능 `application.port`가 영속성/외부 의존 접근을 추상화한다.
- 영속성 세부사항은 infrastructure 레이어 내부에 둔다.
- 현재 패키지 구조와 네이밍 스타일을 유지한다.
- 광범위한 재구성보다 최소 변경을 우선한다.

## 5. 데이터베이스 규칙

DB 관련 작업 시 아래를 따른다.

1. 먼저 `docs/database-context.md`를 읽어 테이블 역할과 비즈니스 규칙을 이해한다.
2. `docs/schema.sql`에서 정확한 스키마 정보를 검증한다.
3. 요약 문서만 보고 컬럼명, 제약조건, 인덱스, 관계를 추정하지 않는다.
4. `docs/database-context.md`와 `docs/schema.sql`이 충돌하면 `docs/schema.sql`을 우선한다.

중요

- `docs/database-context.md`는 요약 문서다.
- `docs/schema.sql`은 전체 PostgreSQL 스키마 기준 문서다.

## 6. 환경 규칙
로컬 개발 환경
- Spring은 IntelliJ에서 직접 실행한다.
- PostgreSQL은 로컬 설치 서버를 사용한다.
- Redis는 Docker에서 실행한다.

운영 환경
- Spring, PostgreSQL, Redis 모두 AWS EC2의 Docker에서 실행한다.

로컬 실행 가정과 운영 배포 가정을 혼동하지 않는다.

## 7. 변경 정책

변경 시 아래를 따른다.

- 광범위한 리팩터링보다 최소 변경을 우선한다.
- 사용자 요청이 없으면 기존 API 계약을 유지한다.
- 기존 패키지 구조를 유지한다.
- 비즈니스 의미를 유지한다.
- 명확한 사유 없이 필드, 메서드, 패키지, 테이블명을 변경하지 않는다.
- 불필요한 신규 추상화를 만들지 않는다.
- 명시적 필요가 없으면 아키텍처 경계를 변경하지 않는다.

## 8. 문서 업데이트 정책

각 작업 후 `docs/work-context.md`를 업데이트한다.

작업이 DB 구조 또는 DB 관련 비즈니스 규칙을 변경한 경우 아래도 함께 업데이트한다.

- `docs/database-context.md`
- `docs/schema.sql`

작업이 아키텍처 규칙 또는 패키지 규약을 변경한 경우 아래도 함께 업데이트한다.

- `docs/project-context.md`
- `AGENTS.md`

작업이 공개 프로젝트 개요, 런타임 요약, 저장소 소개를 변경한 경우 아래도 함께 업데이트한다.

- `README.md`

## 9. 기대 작업 산출물

코드 변경 전에 아래를 요약한다.

- 대상 기능 또는 버그
- 영향 레이어
- 영향 패키지
- DB 영향 여부

코드 변경 후 `docs/work-context.md`에 아래를 기록한다.

- 무엇을 변경했는지
- 왜 변경했는지
- 어떤 파일이 영향을 받았는지
- DB 스키마 변경 여부
- API 동작 변경 여부
- 관련 문서 업데이트 여부
- 남은 이슈 또는 후속 작업

## 10. 금지 사항

아래 행위를 금지한다.

- 읽기 순서를 무시하는 행위
- 메모리 기반 DB 스키마 추정
- 임의 패키지 구조 변경
- Controller에 비즈니스 로직 배치
- Entity 직접 응답 반환
- Request DTO와 Response DTO 혼용
- 명시 요청 없는 광범위 리팩터링
- 필요 없는 API 필드 변경
- 작업 맥락 미기록 상태로 종료

## 11. 한글 인코딩 보호 규칙

- 모든 `.java`, `.md`, `.sql`, `.properties` 파일은 UTF-8 without BOM으로 유지한다.
- 한글 문자열이 포함된 파일은 필요한 최소 범위만 수정하며, 파일 전체 재작성은 지양한다.
- 기존 정상 한글 주석, Swagger 설명, 문서 문장은 요청이 없으면 수정하지 않는다.
- PowerShell `echo`, `cat`, `Set-Content`로 파일 전체를 덮어쓰지 않는다.
- 수정 후 깨진 문자(`�`, `ì`, `í`, `ê`, `ë`, `Ã`, `Â`)를 점검한다.
- Java 소스 파일 시작부에 BOM이 들어가면 안 된다.
- 요청 범위 밖에서 깨진 한글을 발견하면 임의 복구하지 말고 보고만 한다.
