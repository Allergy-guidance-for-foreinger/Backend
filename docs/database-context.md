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

## 7. menu_like table (2026-05-03)
- Added `menu_like` table for menu preference by `(cafeteria_id, menu_id)`.
- Uniqueness: `UNIQUE(user_id, cafeteria_id, menu_id)`.
- Indexes: `idx_menu_like_target(cafeteria_id, menu_id)`, `idx_menu_like_user(user_id)`.
- FK policy follows existing user cleanup policy with `ON DELETE CASCADE` on `user_id`.

## 8. menu_review tables (2026-05-03)
- Added `menu_review`, `menu_review_like`, `menu_review_comment` tables.
- Review list target is `(cafeteria_id, menu_id)` and stores `meal_menu_id`, `meal_date` as write context.
- Review supports multiple posts by same user on same menu (no uniqueness for user/menu).
- `menu_review_like` prevents duplicate likes per user with `UNIQUE(review_id, user_id)`.
- Review/comment delete is soft-delete by `deleted_at`.
- Review list API uses page/size and latest-first ordering by date and recency.
- Comment list API uses page/size and oldest-first ordering.

## 9. AI allergy analysis mapping (2026-05-13)
- `allergy_ingredient` 매핑 테이블은 제거됐다.
- `menu_ai_analysis_allergy`는 AI가 메뉴에서 감지한 알레르기 코드를 저장한다.
- 알레르기 위험도는 `user_allergy.allergy_code`와 `menu_ai_analysis_allergy.allergy_code` 교집합으로 계산한다.
- 종교 제한 위험도는 기존 `religious_food_restriction_ingredient` 재료 매핑을 유지한다.
## 10. AI status column length update (2026-05-19)
- Extended `menu.ai_analysis_status` and `menu_ai_analysis.status` length from 20 to 40.
- Reason: (historical) previous retry status value `FAILED_RETRY_EXHAUSTED` exceeded 20 characters and caused insert failure before status simplification.

## 11. AI status simplification and retry criteria update (2026-05-19)
- Menu AI status model is simplified to `SUCCESS` and `FAILED`.
- Retry selection now uses latest `FAILED` records with `attempt_count < max_attempt_count`.

## 12. Translation analysis status tracking and retry criteria (2026-05-20)
- Added `menu_translation_analysis` table to track translation follow-up attempts per `(menu_id, lang_code)`.
- Status model uses `SUCCESS` and `FAILED` with `attempt_count` (latest-row upsert per `(menu_id, lang_code)`).
- Retry selection uses latest `FAILED` records with `attempt_count < max_attempt_count`.
- `menu_translation` remains as final translated value storage, while retry-state is tracked in `menu_translation_analysis`.

## 13. Menu image analysis log (2026-05-22)
- Added `menu_image_analysis_log` for image-based analysis request tracking.
- Stores `image_storage_path` (Firebase object path only), `status`, `result_source`, identified food summary, Korean/translated identified names, optional `fallback_result` JSONB, and `error_code`.
- `fallback_result` is used only when identified food is not mapped to existing `menu`.
