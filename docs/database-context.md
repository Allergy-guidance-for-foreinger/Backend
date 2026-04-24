# 데이터베이스 공통 맥락

## 1. 목적
이 문서는 프로젝트 전반의 DB 공통 규칙을 정의한다.  
기능별 DB 상세 사용 규칙은 각 기능 맥락 문서를 참조한다.

## 2. 기준 문서
- 스키마 기준(source of truth): `docs/schema.sql`
- `docs/database-context.md`와 `docs/schema.sql`이 다르면 `docs/schema.sql`을 따른다.

## 3. 공통 DB 작업 규칙
- 컬럼명, 타입, nullable, PK/FK, unique, index는 반드시 `docs/schema.sql`로 검증한다.
- 요약 문서나 기억 기반으로 스키마를 추정하지 않는다.
- 기능 서비스는 repository 구현 세부가 아니라 application port에 의존한다.
- 기능별 persistence 구현은 infrastructure 레이어에 둔다.

## 4. 기능별 DB 상세 규칙 문서
- 로그인/토큰: `docs/features/login-context.md`
- 온보딩: `docs/features/onboarding-context.md`
- 사용자 설정: `docs/features/settings-context.md`
- 급식 크롤링: `docs/features/mealcrawl-context.md`
- 로컬 인증 디버그: `docs/features/authdebug-context.md` (DB 영향 없음)

## 5. 공통 비즈니스 원칙(요약)
- soft delete/비활성 사용자(`status = INACTIVE`, `deleted_at`) 처리 규칙은 인증/조회 흐름에서 일관되게 유지한다.
- 사용자별 알레르기 설정은 full replacement 규칙을 따른다.
- AI 분석/번역 후속 처리 실패가 핵심 import 성공 상태를 깨지 않도록 분리한다.

## 6. 업데이트 규칙
- 공통 DB 작업 원칙이 변경될 때 본 문서를 업데이트한다.
- 기능별 상세 DB 규칙이 변경될 때는 해당 `docs/features/*-context.md`를 우선 업데이트한다.
