# 작업 맥락 요약

## 1. 목적
이 문서는 최근 작업 요약과 이력 조회 경로를 안내한다.  
기능별 상세 변경 이력은 `docs/work-log/*-work-log.md`를 우선 참조한다.

## 2. 최근 작업 요약

### 2026-04-24
- 문서 구조를 기능 중심으로 분리했다.
  - `docs/features/*-context.md`
  - `docs/work-log/*-work-log.md`
- 공통 규칙 문서(`AGENTS.md`, `project-context`, `database-context`)를 재정리했다.
- 한국어 문서 전환을 진행했다.

### 2026-04-24 (이번 작업)
- `docs/features/login-context.md`
- `docs/features/onboarding-context.md`
- `docs/features/settings-context.md`
- `docs/features/mealcrawl-context.md` 신규 추가
- `docs/features/authdebug-context.md` 신규 추가
- 공통 문서에서 기능별 상세 내용을 기능 맥락 문서로 이관/요약 반영

## 3. 이력 조회 가이드
- 공통/횡단 작업: `docs/work-log/general-work-log.md`
- 로그인: `docs/work-log/login-work-log.md`
- 온보딩: `docs/work-log/onboarding-work-log.md`
- 설정: `docs/work-log/settings-work-log.md`
- mealcrawl/authdebug 상세는 현재 기능 맥락 문서를 우선 참조하고, 필요 시 `general-work-log`에 기록한다.

## 4. 기록 원칙
- 새 작업 시 이 문서에는 최근 요약만 추가한다.
- 상세 변경 내역(파일, 원인, 영향, 주의사항)은 해당 기능 `work-log`에 기록한다.
