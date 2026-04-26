# mealcrawl 기능 맥락

## 1. 역할
`mealcrawl` 기능은 Java-to-Python 동기 HTTP 기반 급식 수집/후속 처리 파이프라인을 담당한다.
크롤링 데이터 import, AI 분석 후속 저장, 번역 후속 저장, 스케줄 실행을 포함한다.

## 2. 주요 패키지
- `mealcrawl.application.service`
- `mealcrawl.application.port`
- `mealcrawl.application.dto`
- `mealcrawl.domain`
- `mealcrawl.infrastructure.client`
- `mealcrawl.infrastructure.client.dto.request`
- `mealcrawl.infrastructure.client.dto.response`
- `mealcrawl.infrastructure.persistence.adapter`
- `mealcrawl.infrastructure.persistence.repository`
- `mealcrawl.infrastructure.config`

## 3. 주요 클래스
- `MealCrawlScheduler`: 스케줄 실행 진입점
- `MealCrawlOrchestrationService`: 전체 크롤링 오케스트레이션
- `MealCrawlTargetService`: 수집 대상 계산
- `MealImportService`: `meal_schedule`, `menu`, `meal_menu` import
- `MenuAiAnalysisFollowUpService`: AI 분석 후속 저장
- `MenuTranslationFollowUpService`: 번역 후속 저장
- `MealCrawlPersistenceAdapter`: 영속성 포트 구현
- `MealCrawlSchedulerLockAdapter`: advisory lock 구현
- `PythonMealClientAdapter`: Python API 호출 및 예외 처리
- `MealCrawlSchedulerLockPort`, `MealCrawlPersistencePort`, `PythonMealClientPort`: 포트 추상화

## 4. DB 사용 규칙
- 관련 테이블
  - import: `meal_schedule`, `menu`, `meal_menu`
  - AI 후속: `menu_ai_analysis`, `menu_ai_analysis_ingredient`
  - 번역 후속: `menu_translation`
  - 확정 데이터(자동 생성 금지): `meal_menu_confirmed_ingredient`, `meal_menu_confirmation_history`
- 저장 규칙
  - import 성공은 AI/번역 후속 실패와 분리되어야 한다.
  - `upsertMealMenu`는 경쟁 상태(race)를 고려해 충돌 복구 경로를 유지해야 한다.
  - 스케줄러는 PostgreSQL advisory lock으로 단일 실행을 보장한다.
- 트랜잭션 규칙
  - 외부 API 호출 중 DB transaction을 길게 유지하지 않는다.

## 5. API 규칙
- 외부 사용자용 HTTP 엔드포인트: 확인 필요
- 외부 연동 API
  - Java -> Python 동기 HTTP 호출 사용
  - 호출 경로/계약은 `PythonMealClientAdapter` 및 request/response DTO 기준으로 관리

## 6. 공통 비즈니스 규칙
- mealcrawl 기능은 Java 서버가 Python 서버를 동기 HTTP로 호출해 급식 데이터를 가져오고 DB에 저장하는 흐름을 담당한다.
- 급식 import 성공은 AI 분석/번역 후속 처리 실패와 분리되어야 한다.
- AI 분석 데이터는 초안 성격이며 확정 데이터와 동일하게 취급하지 않는다.
- `meal_menu_confirmed_ingredient`와 `meal_menu_confirmation_history`는 관리자 확정 데이터이며 크롤링 import에서 자동 생성하지 않는다.
- 외부 Python API 호출 중 DB transaction을 길게 유지하지 않는다.
- DB 쓰기는 application port와 persistence adapter 경계를 통해 처리한다.

## 7. 흐름별 비즈니스 규칙

### 7.1 Scheduler 실행
- scheduler는 설정된 주기에 따라 급식 크롤링 대상을 선택한다.
- 다중 인스턴스 환경에서 중복 실행되지 않도록 PostgreSQL advisory lock을 사용한다.
- lock 획득에 실패하면 해당 실행은 건너뛴다.

### 7.2 Python crawl 호출
- Java 서버는 Python 서버에 급식 크롤링 요청을 보낸다.
- Python client 실패 시 원인 예외를 보존해야 한다.
- 외부 호출 실패가 서버 전체 장애로 번지지 않도록 예외 경계를 명확히 한다.

### 7.3 Meal import 저장
- `meal_schedule`, `menu`, `meal_menu`, `meal_schedule_crawl_history`에 저장한다.
- 같은 일정/순서의 `meal_menu`는 중복 생성하지 않고 upsert한다.
- `upsertMealMenu`는 동시성 경쟁 상황에서 unique collision을 고려해야 한다.

### 7.4 AI analysis follow-up
- import 이후 필요한 대상에 대해 AI 분석을 요청한다.
- 결과는 `menu_ai_analysis`, `menu_ai_analysis_ingredient`에 저장한다.
- AI 분석 실패는 meal import 성공을 실패로 바꾸면 안 된다.

### 7.5 Translation follow-up
- import 이후 필요한 대상에 대해 번역을 요청한다.
- 결과는 `menu_translation`에 저장한다.
- 번역 실패는 meal import 성공을 실패로 바꾸면 안 된다.

## 8. 주의사항
- `@Scheduled` 실행은 다중 인스턴스 환경을 전제로 lock 없이 확장하면 안 된다.
- `PythonMealClientAdapter` 예외를 일반 예외로 덮어 원인 스택을 잃지 않도록 주의한다.
- 트랜잭션 경계 변경 시 import 성공 보장 규칙과 후속 처리 분리를 함께 검토한다.
