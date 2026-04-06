# Project Overview

이 프로젝트는 식단표와 음식 정보를 다국어로 제공하고, 사용자별 알레르기/기피 재료/종교적 제한 정보를 반영하여 위험 여부를 안내하는 Spring 기반 백엔드 서버입니다.

## Features
- 날짜별 식단표 조회
- 메뉴 재료 정보 제공
- 알레르기 및 섭취 제한 위험 안내
- AI 분석 결과와 관리자 확정 데이터 관리
- 다국어 응답 지원

## Tech Stack
- Java 21
- Spring Boot
- PostgreSQL
- Redis
- Docker
- Swagger / OpenAPI

## Runtime Environments
### Local Development
- Spring application: IntelliJ에서 직접 실행
- PostgreSQL: 로컬 PC에 설치된 PostgreSQL 서버 사용
- Redis: Docker로 실행

### Production
- AWS EC2 환경 사용
- Spring application, PostgreSQL, Redis를 모두 Docker 컨테이너로 실행

## Documents
- Project context: `docs/project-context.md`
- CLI rules: `AGENTS.md`
- DB context: `docs/database-context.md`
- Work log: `docs/work-context.md`
- Full schema: `docs/schema.sql`

## Recommended Reading Order for CLI
1. `README.md`
2. `AGENTS.md`
3. `docs/project-context.md`
4. `docs/database-context.md`
5. `docs/work-context.md`
6. `docs/schema.sql` (DB 관련 작업인 경우 필수)
