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

### 2026-05-27 (user_oauth_accounts schema reference 보정)
- What changed:
  - `docs/schema.sql`에 누락되어 있던 `user_oauth_accounts` 테이블 정의를 추가했다.
  - `UserOauthAccount` 엔티티 기준으로 `id`, `user_id`, `provider`, `provider_user_id`, `provider_email`, `created_at`, `updated_at` 컬럼을 반영했다.
  - `uk_user_oauth_accounts_provider_user_id` unique constraint와 조회용 인덱스(`user_id`, `(provider, provider_email)`)를 문서에 추가했다.
- Why:
  - 로그인 기능 문서와 엔티티에는 `user_oauth_accounts`가 존재하지만, schema source-of-truth 문서에서 빠져 있어 DB 기준 문서가 불완전했기 때문.
- Affected files:
  - `docs/schema.sql`
  - `docs/work-log/login-work-log.md`
- DB schema changed: No runtime schema change (schema reference document correction only)
- API behavior changed: No
- Related docs updated:
  - `docs/work-log/login-work-log.md`

### 2026-05-25 (역할 기반 회원탈퇴 정책 적용)
- What changed:
  - `DELETE /auth/withdraw` API를 추가했다.
  - 탈퇴 정책을 role 기준으로 분기했다.
    - `USER`: 하드 삭제
    - `MANAGER`, `ADMIN`: 소프트 삭제(`status=INACTIVE`, `deleted_at` 설정)
  - `menu_image_analysis_log.user_id` FK를 `ON DELETE CASCADE`로 변경했다.
  - `USER` 하드 삭제 시 FK 충돌을 피하기 위해 `user_oauth_accounts`를 선삭제 후 `users`를 삭제하도록 반영했다.
  - `LoginServiceTest`에 회원탈퇴 분기 테스트를 추가했다.
- Why:
  - 일반 사용자 데이터는 탈퇴 시 완전 삭제하고, 매니저/관리자 메뉴 확정 이력은 감사 목적으로 보존하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/login/presentation/controller/AuthController.java`
  - `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
  - `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
  - `src/main/java/com/mealguide/mealguide_api/login/application/port/UserQueryPort.java`
  - `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserJpaRepository.java`
  - `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserOauthAccountJpaRepository.java`
  - `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
  - `docs/schema.sql`
  - `docs/database-context.md`
  - `docs/features/login-context.md`
  - `docs/work-log/login-work-log.md`
- DB schema changed: Yes
  - `menu_image_analysis_log.user_id -> users.id ON DELETE CASCADE`
- API behavior changed:
  - 추가: `DELETE /auth/withdraw`
- Related docs updated:
  - `docs/schema.sql`
  - `docs/database-context.md`
  - `docs/features/login-context.md`
  - `docs/work-log/login-work-log.md`
- Rollback SQL:
```sql
alter table menu_image_analysis_log
    drop constraint if exists fk_menu_image_analysis_log_user;

alter table menu_image_analysis_log
    add constraint fk_menu_image_analysis_log_user
        foreign key (user_id) references users(id);
```
- Remaining follow-ups:
  - 운영 DB 마이그레이션 스크립트/도구(Flyway/Liquibase 등) 반영은 별도 배포 절차에서 수행 필요.

### 2026-05-25 (INACTIVE 계정 로그인 차단 및 수동 복구 정책 반영)
- What changed:
  - Google 로그인에서 ACTIVE 사용자 조회 실패 시, 동일 Google 계정의 INACTIVE 사용자 존재 여부를 추가 확인하도록 변경했다.
  - INACTIVE 계정이 존재하면 신규 사용자 생성을 중단하고 `USER_INACTIVE` 에러를 반환하도록 변경했다.
  - `AuthApi` 로그인 실패 응답에 `USER_INACTIVE`를 추가했다.
  - `LoginServiceTest`에 INACTIVE 계정 로그인 차단 케이스를 추가했다.
- Why:
  - 소프트 삭제된 매니저/관리자 계정의 무단 재활성화를 막고, 서버 관리자 DB 수동 복구 정책을 강제하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/global/base/exception/ErrorCode.java`
  - `src/main/java/com/mealguide/mealguide_api/login/application/port/UserQueryPort.java`
  - `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
  - `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
  - `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserOauthAccountJpaRepository.java`
  - `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
  - `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
  - `docs/features/login-context.md`
  - `docs/database-context.md`
  - `docs/work-log/login-work-log.md`
- DB schema changed: No
- API behavior changed:
  - `POST /auth/login`에서 소프트 삭제 계정은 로그인 실패(`USER_INACTIVE`)
- Related docs updated:
  - `docs/features/login-context.md`
  - `docs/database-context.md`
  - `docs/work-log/login-work-log.md`
- Remaining follow-ups:
  - 서버 관리자 수동 복구 SQL 운영 Runbook 별도 정리 필요.

### 2026-05-25 (MANAGER/ADMIN 탈퇴 시 댓글 비노출 정리)
- What changed:
  - `MANAGER`, `ADMIN` 소프트 삭제 시 해당 사용자가 작성한 `menu_review`를 일괄 소프트 삭제하도록 반영했다.
  - `MANAGER`, `ADMIN` 소프트 삭제 시 해당 사용자가 작성한 `menu_review_comment`를 일괄 소프트 삭제하도록 반영했다.
  - `MANAGER`, `ADMIN` 소프트 삭제 시 해당 사용자가 남긴 `menu_review_like`를 삭제하도록 반영했다.
  - 댓글/좋아요 정리 후 영향 리뷰의 `like_count`, `comment_count`를 재정합하도록 반영했다.
  - 댓글 일괄 삭제 후 영향받은 `menu_review.comment_count`를 활성 댓글 기준으로 재정합하도록 반영했다.
  - 현재 스키마에 대댓글(parent-child) 구조가 없어, 정책은 단일 레벨 댓글에 적용됨을 문서에 명시했다.
- Why:
  - 탈퇴한 관리자/매니저가 작성한 리뷰/댓글을 비노출 처리하고, 리뷰 댓글 수 표시와 실제 노출 댓글 수의 정합성을 맞추기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
  - `docs/features/login-context.md`
  - `docs/work-log/login-work-log.md`
- DB schema changed: No
- API behavior changed:
  - `MANAGER`, `ADMIN` 탈퇴 이후 해당 사용자의 리뷰/댓글은 조회에서 노출되지 않음.
- Related docs updated:
  - `docs/features/login-context.md`
  - `docs/work-log/login-work-log.md`
- Remaining follow-ups:
  - 대댓글 정책 적용이 필요하면 `menu_review_comment`에 parent-comment 구조를 도입하는 별도 스키마 변경이 필요.

### 2026-05-25 (USER 하드 삭제 시 리뷰 카운트 정합성 보강)
- What changed:
  - `USER` 하드 삭제 전에 해당 사용자가 남긴 리뷰 좋아요/댓글으로 영향받는 `review_id`를 수집하도록 반영했다.
  - 하드 삭제 후 영향 리뷰의 `like_count`, `comment_count`를 DB 현재 상태 기준으로 재계산하도록 반영했다.
- Why:
  - `users` cascade 삭제로 좋아요/댓글 행은 제거되지만, 역정규화 카운트가 남아 불일치할 수 있는 문제를 방지하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
  - `docs/work-log/login-work-log.md`
- DB schema changed: No
- API behavior changed: No

### 2026-05-25 (USER 하드 삭제 전 non-cascade 참조 안전 분기)
- What changed:
  - `meal_menu_confirmed_ingredient.confirmed_by_user_id`, `meal_menu_confirmation_history.changed_by_user_id` 참조 존재 여부를 조회하는 로직을 추가했다.
  - `USER`라도 위 non-cascade 참조가 존재하면 하드 삭제 대신 소프트 삭제로 분기하도록 `withdraw` 로직을 보완했다.
- Why:
  - 역할 변경 이력 등으로 `USER`가 non-cascade 참조를 가진 경우 하드 삭제 시 FK 제약 위반으로 탈퇴가 실패하는 문제를 방지하기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/login/application/port/UserQueryPort.java`
  - `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
  - `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
  - `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
  - `docs/work-log/login-work-log.md`
- DB schema changed: No
- API behavior changed:
  - `USER` 탈퇴도 non-cascade 참조가 있으면 소프트 삭제로 처리됨.

### 2026-05-26 (JWT access token role claim 추가 및 필터 role 조회 제거)
- What changed:
  - `TokenClaims`에 `role` 필드를 추가했다.
  - access token 발급 시 `role` 클레임을 포함하도록 `JwtTokenProvider`를 수정했다.
  - access token 파싱 시 `role` 클레임을 필수 검증하도록 `JwtTokenProvider`를 수정했다.
  - `JwtAuthenticationFilter`에서 `findActiveRoleById` DB 조회를 제거하고, 토큰의 `role`로 principal authority를 구성하도록 변경했다.
  - 관련 테스트(`JwtAuthenticationFilterTest`, `LoginServiceTest`)를 새 `TokenClaims` 시그니처에 맞게 수정했다.
- Why:
  - JWT payload에 role이 없어 요청마다 DB role 조회가 발생하던 병목을 제거해 connection timeout 위험을 낮추기 위해.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/global/auth/domain/TokenClaims.java`
  - `src/main/java/com/mealguide/mealguide_api/global/auth/jwt/JwtTokenProvider.java`
  - `src/main/java/com/mealguide/mealguide_api/global/auth/security/JwtAuthenticationFilter.java`
  - `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
  - `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
  - `docs/work-log/login-work-log.md`
- DB schema changed: No
- API behavior changed:
  - 내부 인증 처리 변경: access token에 유효한 `role` 클레임이 없으면 인증 실패.
- Related docs updated:
  - `docs/work-log/login-work-log.md`
- Remaining follow-ups:
  - 없음 (개발 단계 가정에서 구 토큰 호환 미고려).

### 2026-05-26 (refresh 재발급 AUTH_004 오류 수정)
- What changed:
  - `refresh` 경로에서 access token 재발급용 `AuthenticatedUser` 생성 시 `role`을 `null`로 넣던 문제를 수정했다.
  - `LoginService.refresh`에서 `findActiveRoleById`로 활성 사용자의 role을 조회해 `AuthenticatedUser.role`에 주입하도록 변경했다.
  - 관련 `LoginServiceTest` refresh 케이스 mock 설정에 `findActiveRoleById` 스텁을 추가했다.
- Why:
  - access token 발급 시 `role` 클레임이 필수로 바뀐 이후, refresh 경로에서 role 누락으로 `JWT_INVALID(AUTH_004)`가 발생했기 때문.
- Affected files:
  - `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
  - `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
  - `docs/work-log/login-work-log.md`
- DB schema changed: No
- API behavior changed:
  - `POST /auth/refresh`가 정상 refresh token에 대해 다시 정상 재발급됨.
- Related docs updated:
  - `docs/work-log/login-work-log.md`
- Remaining follow-ups:
  - 없음.
