# Work Context Log

This document records implementation history and follow-up context.

## Writing Rules
- Record the scope and result of each task clearly.
- List affected layers and changed files.
- State DB impact explicitly.
- Note related document updates and remaining issues.

---

## 2026-04-06

### Task
- Implement mobile Google ID Token login, JWT authentication, and Redis refresh token flow.
- Accept `idToken` and `deviceId` from the client.
- Issue server-managed `accessToken` and `refreshToken`.
- Group authentication code under the `login` feature package and place Swagger descriptions on the implemented interface.

### Affected Layers
- `login.presentation`
- `login.application`
- `login.domain`
- `login.infrastructure`
- `global.config.security`

### Changed Files
- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/java/com/mealguide/mealguide_api/login/**`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `src/test/java/com/mealguide/mealguide_api/login/**`
- `docs/project-context.md`
- `docs/database-context.md`
- `docs/schema.sql`
- `docs/work-context.md`

### Why
- Mobile authentication must be stateless and based on `Authorization: Bearer {accessToken}`.
- Google login needs server-issued tokens for consistent API authentication.
- Refresh tokens should be managed in Redis with TTL instead of additional DB schema changes.
- `deviceId` is required to manage refresh tokens per device.

### DB Impact
- Schema changed by this task: No
- Referenced tables: `users`
- New tables or columns added by this task: No

### API Impact
- Added endpoints:
  - `POST /auth/login`
  - `POST /auth/refresh`
  - `POST /auth/logout`
- Request DTOs:
  - login: `idToken`, `deviceId`
  - refresh: `refreshToken`
  - logout: optional `refreshToken`
- Response DTO:
  - `accessToken`
  - `refreshToken`
  - `expiresIn`
  - `refreshExpiresIn`

### Implementation Notes
- Google ID Token is verified through Google `tokeninfo` API.
- Refresh token is stored in Redis with key format `auth:refresh:{userId}:{deviceId}`.
- Refresh compares JWT claims and Redis value, then rotates the refresh token.
- Logout removes the Redis refresh token for the authenticated user and device.
- `SecurityConfig` whitelists `/auth/login` and `/auth/refresh`.

### Tests
- Added or updated tests for:
  - login success
  - login failure when user does not exist
  - refresh success with rotation
  - refresh failure when Redis token is missing
  - refresh failure after logout
  - JWT authentication filter principal population
- `mvn test` could not be fully verified in the current sandboxed environment.

### Documentation Updates
- Updated `docs/project-context.md`
- Updated `docs/database-context.md`
- Updated `docs/schema.sql`
- Updated `docs/work-context.md`

### Remaining Issues
- Build and test execution should be re-run once Maven repository access is available.
- Google token verification currently depends on external `tokeninfo` API availability.

---

## 2026-04-06

### Task
- Adjust login code to match the updated user-related schema.
- Remove obsolete `users.password_hash` mapping.
- Update Google login lookup to use `user_oauth_accounts`.

### Affected Layers
- `login.domain`
- `login.application.port`
- `login.application.service`
- `login.infrastructure.persistence.repository`
- `login.infrastructure.persistence.adapter`
- `login` tests
- `docs/database-context.md`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserOauthAccount.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/port/UserQueryPort.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserOauthAccountJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/database-context.md`
- `docs/work-context.md`

### Why
- The current schema no longer has `users.password_hash`.
- Google-linked users are now represented by `user_oauth_accounts`.
- Login must follow schema truth instead of relying on nullable `users.email` alone.

### DB Impact
- Schema changed by this task: No
- This task only aligned code with the already changed schema.
- Referenced tables:
  - `users`
  - `user_oauth_accounts`

### API Impact
- External endpoint contract changed: No
- Internal login lookup behavior changed:
  - first lookup by `provider = GOOGLE` and `provider_user_id = Google subject`
  - fallback lookup by `provider_email` when needed

### Implementation Notes
- Added `UserOauthAccount` entity mapped to `user_oauth_accounts`.
- Added repository methods for Google provider subject/email lookup.
- Updated `UserQueryPort` and adapter to resolve users through OAuth mapping.
- Removed obsolete `passwordHash` field usage from entity and tests.

### Tests
- Updated login service tests to mock OAuth account based user lookup.
- Updated JWT filter tests to match the updated `User` entity shape.
- Full `mvn test` execution is still not verified in this environment.

### Documentation Updates
- Updated `docs/database-context.md`
- Updated `docs/work-context.md`

### Remaining Issues
- If the actual provider value in data is not `GOOGLE`, repository lookup constant must be adjusted to match production data.
- If user linking policy changes again, login lookup rules should be revisited with the latest schema and seed data.

---

## 2026-04-06

### Task
- Align response DTOs with the reduced `AuthTokenResult`.
- Change user status in the login domain to an enum with active and inactive values.
- Ensure inactive users are excluded at query time.

### Affected Layers
- `login.domain`
- `login.presentation.dto.response`
- `login.infrastructure.persistence.repository`
- `login.infrastructure.persistence.adapter`
- `login.application.service`
- `login` tests
- `docs/database-context.md`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserStatus.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/dto/response/AuthResponse.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserOauthAccountJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/database-context.md`
- `docs/work-context.md`

### Why
- `AuthTokenResult` no longer includes token type or user information.
- Response DTO conversion needed to match the current service result shape.
- Authentication should only resolve active users, and inactive users should be excluded before service-level use.

### DB Impact
- Schema changed by this task: No
- Query behavior changed:
  - `users.status = ACTIVE` is now enforced in authentication-related lookups

### API Impact
- Auth response fields are now:
  - `accessToken`
  - `refreshToken`
  - `expiresIn`
  - `refreshExpiresIn`

### Implementation Notes
- Added `UserStatus` enum with `ACTIVE` and `INACTIVE`.
- Changed `User.status` to enum mapping with `EnumType.STRING`.
- Updated repository methods so OAuth account lookup and user id lookup only return `ACTIVE` users.
- Removed old response conversion assumptions about `tokenType` and user payload.

### Tests
- Updated tests to set `User.status` using `UserStatus.ACTIVE`.
- Full `mvn test` execution is still not verified in this environment.

### Documentation Updates
- Updated `docs/database-context.md`
- Updated `docs/work-context.md`

### Remaining Issues
- If stored `users.status` values differ from `ACTIVE` and `INACTIVE`, the enum and repository filters must be adjusted to match real data.

---

## 2026-04-06

### Task
- Reorganize shared authentication classes into the global package.
- Keep feature-specific Google login logic inside the `login` package.

### Affected Layers
- `global.auth`
- `global.config.security`
- `login.application.service`
- `login.presentation`
- test imports
- `docs/project-context.md`
- `AGENTS.md`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/global/auth/domain/AuthenticatedUser.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/domain/TokenClaims.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/domain/TokenType.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/port/TokenProviderPort.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/port/RefreshTokenPort.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/jwt/JwtProperties.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/jwt/JwtTokenProvider.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/redis/RedisRefreshTokenAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/AuthenticatedUserPrincipal.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/JwtAuthenticationFilter.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/RestAuthenticationEntryPoint.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/RestAccessDeniedHandler.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/controller/AuthController.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/project-context.md`
- `AGENTS.md`
- `docs/work-context.md`

### Why
- JWT, refresh token storage, principal, filter, and auth error handlers are shared infrastructure concerns.
- Google token verification and OAuth user lookup are still login feature concerns and remain under `login`.
- This keeps common auth code out of the feature package without broad refactoring of feature-specific logic.

### DB Impact
- Schema changed by this task: No
- Query behavior changed by this task: No

### API Impact
- External API contract changed: No

### Implementation Notes
- Moved shared auth records, ports, JWT implementation, Redis refresh token adapter, and Spring Security auth classes to `global.auth`.
- Updated imports in service, controller, Swagger interface, security config, and tests.
- Removed duplicated shared auth classes from the `login` package after migration.

### Documentation Updates
- Updated `docs/project-context.md`
- Updated `AGENTS.md`
- Updated `docs/work-context.md`

### Remaining Issues
- Build and tests still need to be executed in an environment where Maven repository access works.

---

## 2026-04-06

### Task
- Minimize JWT claim contents for access token and refresh token.
- Adjust logout flow to work without `deviceId` in the access token.

### Affected Layers
- `global.auth.domain`
- `global.auth.jwt`
- `login.application.service`
- `login.presentation`
- tests
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/global/auth/domain/TokenClaims.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/jwt/JwtTokenProvider.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/dto/request/LogoutRequest.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/controller/AuthController.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/work-context.md`

### Why
- Access token should contain only `sub`, `type`, and expiration.
- Refresh token should contain only `sub`, `deviceId`, `type`, and expiration.
- Because access token no longer contains `deviceId`, logout must identify the target refresh token through the submitted refresh token.

### API Impact
- `POST /auth/logout` now effectively requires `refreshToken` in the request body.
- Token response fields did not change.

### Implementation Notes
- `TokenClaims` now contains only `userId`, `deviceId`, and `tokenType`.
- Access token generation now writes only `sub` and `type` claims.
- Refresh token generation writes `sub`, `deviceId`, and `type` claims.
- Logout parses the refresh token, validates the authenticated user id, extracts `deviceId`, and deletes the matching Redis key.
- JWT authentication filter now authenticates using only `sub` from the access token and reloads user data from the database.

### Tests
- Updated refresh token tests to use the reduced claim shape.
- Updated JWT filter tests to use access token claims without `deviceId`.

### Remaining Issues
- Full build and test execution still needs to be run in an environment where Maven can access its local repository normally.

---

## 2026-04-06

### Task
- Add a custom annotation for directly injecting the authenticated user id.

### Affected Layers
- `global.auth.annotation`
- `login.presentation`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/global/auth/annotation/CurrentUserId.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/controller/AuthController.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `docs/work-context.md`

### Why
- Using `@AuthenticationPrincipal` and then manually reading `userId` repeats the same pattern.
- A dedicated annotation makes controller signatures shorter and keeps intent explicit.

### Implementation Notes
- Added `@CurrentUserId` as a meta-annotation over `@AuthenticationPrincipal(expression = "userId")`.
- Also marked it hidden for Swagger parameter generation.
- Updated logout endpoint to inject `Long currentUserId` directly.

---

## 2026-04-06

### Task
- Change `users.role` handling to an enum.
- Define supported roles as `USER`, `ADMIN`, and `MANAGER`.

### Affected Layers
- `login.domain`
- `global.auth.domain`
- `global.auth.security`
- tests
- `docs/database-context.md`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserRole.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/domain/AuthenticatedUser.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/AuthenticatedUserPrincipal.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/database-context.md`
- `docs/work-context.md`

### Why
- Role values should be constrained to known values instead of using free-form strings.
- Security authority mapping should be derived from the enum directly.
- The requested default role for future first-time user creation is `USER`.

### DB Impact
- Schema changed by this task: No
- Entity mapping for `users.role` now uses `EnumType.STRING`.

### API Impact
- External API contract changed: No

### Implementation Notes
- Added `UserRole` enum with `USER`, `ADMIN`, and `MANAGER`.
- Changed `User.role` to enum mapping.
- Changed authenticated user and principal role fields to use `UserRole`.
- Spring Security authority mapping now always uses `ROLE_{enumName}`.
- Added `UserRole.defaultRole()` returning `USER` for use by future user creation flows.
- No automatic first-login user creation was added because the current schema requires `school_id` and the project does not define a safe rule for deriving it during login.

### Remaining Issues
- If a first-login auto-signup flow is needed, a valid `school_id` resolution rule must be defined before creating `users` rows.

---

## 2026-04-06

### Task
- Make `users.school_id` nullable.
- Add automatic signup on first Google login.

### Affected Layers
- `login.domain`
- `login.application.port`
- `login.application.service`
- `login.infrastructure.persistence.adapter`
- tests
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserOauthAccount.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/port/UserQueryPort.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

### Why
- The requested behavior is to auto-register users on first login.
- The current schema required `school_id`, which blocked safe user creation during login.

### DB Impact
- Schema changed by this task: Yes
- Changes:
  - `users.school_id` is now nullable
  - `users.id` uses identity generation
  - `user_oauth_accounts.id` uses identity generation

### API Impact
- External API contract changed: No
- Behavior changed:
  - first Google login now auto-creates a user and linked OAuth account instead of returning `USER_NOT_FOUND`

### Implementation Notes
- `LoginService.login` now creates a user when no mapped account exists.
- New first-login user defaults:
  - `schoolId = null`
  - `status = ACTIVE`
  - `role = USER`
- `UserPersistenceAdapter` saves both `users` and `user_oauth_accounts`.

### Tests
- Replaced the missing-user failure case with first-login auto-signup success coverage.

### Remaining Issues
- Full build and test execution still needs to be run in an environment where Maven can access its local repository normally.

---

## 2026-04-06

### Task
- Add ORM-level soft delete behavior for `users`.

### Affected Layers
- `login.domain`
- `docs/database-context.md`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `docs/database-context.md`
- `docs/work-context.md`

### Why
- A user delete operation should disable the account by changing `status` to `INACTIVE` instead of physically deleting the row.

### DB Impact
- Schema changed by this task: No
- ORM delete behavior changed:
  - entity delete now executes `UPDATE users SET status = 'INACTIVE' WHERE id = ?`

### Implementation Notes
- Added Hibernate `@SQLDelete` to `User` so entity delete operations become status updates.
- Added Hibernate `@SQLRestriction("status <> 'INACTIVE'")` so inactive users are excluded from normal ORM selections.
- Existing repository-level `ACTIVE` filtering remains in place.

### Remaining Issues
- `@SQLDelete` does not apply to JPQL bulk delete or native delete queries. Those must be avoided or handled separately if introduced later.

---

## 2026-04-06

### Task
- Add a separate local/dev-only package for verifying the auth controller flow without a frontend-provided Google ID token.

### Affected Layers
- `authdebug.application`
- `authdebug.presentation`
- `login.application.service`
- `global.config.security`
- `src/main/resources`
- `docs/project-context.md`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/authdebug/application/service/AuthDebugService.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/controller/AuthDebugController.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/dto/request/AuthDebugLoginRequest.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/swagger/AuthDebugApi.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `src/main/resources/application.properties`
- `src/main/resources/application-local.properties`
- `src/main/resources/application-dev.properties`
- `docs/project-context.md`
- `docs/work-context.md`

### Why
- There is no frontend code available yet to obtain a real Google ID token.
- The current auth flow still needs a practical way to verify signup, JWT issuance, refresh rotation, and logout behavior end-to-end.
- The verification code should live in a separate package and stay disabled outside explicitly enabled environments.

### DB Impact
- Schema changed by this task: No

### API Impact
- Added local/dev-only verification endpoints:
  - `POST /auth-debug/login`
  - `POST /auth-debug/refresh`
  - `POST /auth-debug/logout`
- `POST /auth-debug/login` accepts:
  - `subject`
  - `email`
  - `name`
  - `emailVerified`
  - `deviceId`

### Implementation Notes
- Added `authdebug.*` as a separate package for manual auth verification.
- `AuthDebugController` implements Swagger descriptions through `AuthDebugApi`, matching the existing controller style.
- `AuthDebugService` reuses `LoginService` so the same signup, JWT issuance, Redis refresh-token validation, refresh rotation, and logout logic are exercised.
- `LoginService` now exposes `loginVerifiedGoogleUser(...)` so trusted debug input can reuse the real login flow after skipping Google token verification.
- Debug endpoints are guarded by `mealguide.auth-debug.enabled`.
- Default value is `false` in `application.properties` and `true` in `application-local.properties` and `application-dev.properties`.
- Security whitelist now includes debug login and refresh endpoints so they follow the same access pattern as the main auth endpoints.

### Tests
- Build verification was attempted, but `mvn` is not installed in the current environment.
- Runtime verification should be done through Swagger or direct HTTP calls in a local/dev profile with `mealguide.auth-debug.enabled=true`.

### Remaining Issues
- Debug endpoints should not be enabled in production.
- Full compile and test verification still needs an environment with Maven installed.

---

## 2026-04-06

### Task
- Register a `RestClient.Builder` bean required by Google ID token verification.

### Affected Layers
- `global.config.base`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/global/config/base/RestClientConfig.java`
- `docs/work-context.md`

### Why
- `GoogleTokenInfoClient` uses constructor injection for `RestClient.Builder`.
- The current project did not define that bean, so application startup failed before the auth flow could run.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No

### Implementation Notes
- Added `RestClientConfig` under `global.config.base`.
- Registered a single shared `RestClient.Builder` bean with `RestClient.builder()`.
- Registered a shared `ObjectMapper` bean with `findAndRegisterModules()` because auth error handlers also require constructor injection.
- `GoogleTokenInfoClient` can now be created without changing its constructor or feature-layer wiring.

### Remaining Issues
- Runtime verification still needs to be done in an environment with Maven installed and the required local services available.

---

## 2026-04-06

### Task
- Fix first-login auto-signup insert failures caused by null primary keys in `users` and `user_oauth_accounts`.

### Affected Layers
- `login.domain`
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserOauthAccount.java`
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

### Why
- The running PostgreSQL schema did not provide identity defaults for `users.id`, so first-login signup failed with a null primary key insert.
- Auto-signup needs a key generation strategy that does not depend on the table column already being configured as identity.

### DB Impact
- Schema reference changed by this task: Yes
- `users` and `user_oauth_accounts` id generation is now documented with PostgreSQL sequences:
  - `users_id_seq`
  - `user_oauth_accounts_id_seq`

### API Impact
- External API contract changed: No

### Implementation Notes
- Changed `User.id` to `GenerationType.SEQUENCE` using `users_id_seq`.
- Changed `UserOauthAccount.id` to `GenerationType.SEQUENCE` using `user_oauth_accounts_id_seq`.
- This allows Hibernate to obtain ids before insert, avoiding dependence on DB-side identity defaults for first-login signup.

### Remaining Issues
- If the actual local database does not yet have these sequences, Hibernate `ddl-auto=update` in local should create them, but an existing manually managed environment may still need a one-time schema sync.

---

## 2026-04-06

### Task
- Revert user id generation back to database identity columns instead of sequence-based generation.

### Affected Layers
- `login.domain`
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/login/domain/User.java`
- `src/main/java/com/mealguide/mealguide_api/login/domain/UserOauthAccount.java`
- `docs/schema.sql`
- `docs/database-context.md`
- `docs/work-context.md`

### Why
- The preferred strategy is for PostgreSQL table columns to own auto-increment behavior directly.
- Authentication auto-signup should follow the DB schema instead of compensating in JPA for a mismatched local database state.

### DB Impact
- Schema reference changed by this task: Yes
- `users.id` and `user_oauth_accounts.id` are documented again as `GENERATED BY DEFAULT AS IDENTITY`.
- Existing local databases must also have the actual columns updated to identity if they are still plain `BIGINT NOT NULL`.

### API Impact
- External API contract changed: No

### Implementation Notes
- Changed `User.id` back to `GenerationType.IDENTITY`.
- Changed `UserOauthAccount.id` back to `GenerationType.IDENTITY`.
- Removed the temporary sequence-based schema documentation.
- The previous null primary key error indicates the local DB schema is currently behind the documented schema.

### Remaining Issues
- The running PostgreSQL database must be aligned so that `users.id` and `user_oauth_accounts.id` are true identity columns. Without that DB change, first-login auto-signup will fail again.

---

## 2026-04-06

### Task
- Change Swagger descriptions to Korean and remove interface-based controller overriding for auth controllers.

### Affected Layers
- `login.presentation.controller`
- `authdebug.presentation.controller`
- `login.presentation.swagger`
- `authdebug.presentation.swagger`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/login/presentation/controller/AuthController.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/controller/AuthDebugController.java`
- `src/main/java/com/mealguide/mealguide_api/login/presentation/swagger/AuthApi.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/swagger/AuthDebugApi.java`
- `docs/work-context.md`

### Why
- Swagger descriptions should be written in Korean for current project usage.
- Controllers should keep the existing interface-based Swagger structure while hiding explicit overriding noise from controller source.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No
- Swagger display text changed to Korean.

### Implementation Notes
- Restored interface-based Swagger definitions for `AuthController` and `AuthDebugController`.
- Controllers implement the Swagger interfaces again.
- Removed explicit `@Override` annotations from controller source so overriding is not visibly exposed.

### Remaining Issues
- Build verification is still pending because Maven is not available in the current environment.

---

## 2026-04-06

### Task
- Add a small local HTML page that can receive a real Google ID token and call the auth APIs.

### Affected Layers
- `src/main/resources/static`
- `docs/work-context.md`

### Changed Files
- `src/main/resources/static/auth-test.html`
- `docs/work-context.md`

### Why
- A real Google ID token is required to verify the actual Google token validation flow.
- There is no frontend code in the current project, so a minimal browser test page is needed.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No
- Added a static test page served by Spring:
  - `GET /auth-test.html`

### Implementation Notes
- Added a single-page browser test tool under `static`.
- The page uses Google Identity Services to obtain a real ID token.
- After Google login, the same page can call:
  - `POST /auth/login`
  - `POST /auth/refresh`
  - `POST /auth/logout`
- The page also shows the current token state for easier manual verification.

### Remaining Issues
- The Google OAuth client must allow the local origin used to open this page.
- Real end-to-end verification still depends on local PostgreSQL and Redis being available.

---

## 2026-04-06

### Task
- Open the local auth test page through security whitelist settings.

### Affected Layers
- `global.config.security`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `docs/work-context.md`

### Why
- The static test page was being blocked by Spring Security because public GET routes were empty.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No
- Public GET access now allows:
  - `/`
  - `/index.html`
  - `/auth-test.html`
  - `/favicon.ico`
  - `/error`

### Implementation Notes
- Added the auth test page and minimal static routes to `PUBLIC_WHITELIST`.
- Authenticated APIs remain protected except for the intended login and refresh entry points.

---

## 2026-04-06

### Task
- Remove manual Google Client ID input from the auth test page and load it from server configuration.

### Affected Layers
- `authdebug.presentation.controller`
- `authdebug.presentation.dto.response`
- `global.config.security`
- `src/main/resources/static`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/controller/AuthDebugConfigController.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/dto/response/AuthDebugConfigResponse.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `src/main/resources/static/auth-test.html`
- `docs/work-context.md`

### Why
- The Google Client ID is already supplied through server configuration.
- The local auth test page should use that value automatically instead of requiring duplicate manual input.

### DB Impact
- Schema changed by this task: No

### API Impact
- Added local/dev-only config endpoint:
  - `GET /auth-debug/config`
- The auth test page now reads Google Client ID from that endpoint.

### Implementation Notes
- Added `AuthDebugConfigController` for local/dev-only config exposure.
- Added `AuthDebugConfigResponse` as a response DTO instead of returning raw values directly.
- Added `/auth-debug/config` to the public GET whitelist.
- Updated `auth-test.html` so the page fetches the configured Google Client ID on load and removes the manual input field.

---

## 2026-04-06

### Task
- Remove the refresh-token rotation race condition by introducing an atomic Redis rotation operation.

### Affected Layers
- `global.auth.port`
- `global.auth.redis`
- `login.application.service`
- `login` tests
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/global/auth/port/RefreshTokenPort.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/redis/RedisRefreshTokenAdapter.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `docs/work-context.md`

### Why
- The previous refresh flow used separate read, compare, and save steps.
- Concurrent refresh requests using the same refresh token could both pass validation and each receive a new token pair.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No
- Internal refresh-token rotation semantics changed to atomic compare-and-set.

### Implementation Notes
- Added `rotateIfMatch(...)` to `RefreshTokenPort`.
- Implemented the Redis operation with a single Lua script that performs compare-and-set plus TTL update atomically.
- Changed `LoginService.refresh()` to rely on the atomic rotation result instead of separate `findByUserIdAndDeviceId()` and `save()` calls.
- Updated the in-memory test double to match the new port contract.

### Remaining Issues
- Full runtime verification is still pending because Maven is not available in the current environment.

---

## 2026-04-06

### Task
- Stop storing raw refresh token values in Redis and store only token hashes.

### Affected Layers
- `global.auth.redis`
- `login` tests
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/global/auth/redis/RedisRefreshTokenAdapter.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `docs/work-context.md`

### Why
- Storing raw refresh tokens in Redis makes session theft possible if Redis read access is exposed.
- Refresh token persistence should keep only a derived hash value, and submitted tokens should be hashed again before comparison.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No
- Internal Redis persistence now stores only SHA-256 hashes of refresh tokens.

### Implementation Notes
- `RedisRefreshTokenAdapter` now hashes refresh token values before save.
- Atomic rotation also hashes both the expected token and the new token before compare-and-set.
- Login service tests were updated so the in-memory adapter mirrors the same hashed-storage semantics.

### Remaining Issues
- The current implementation uses unsalted SHA-256 because comparison must remain deterministic for rotation. If stronger protection is needed later, an HMAC-based keyed hash using a server secret would be preferable.

---

## 2026-04-06

### Task
- Reduce refresh-path database load by replacing full user fetch with an active-user existence check.

### Affected Layers
- `login.application.port`
- `login.application.service`
- `login.infrastructure.persistence.repository`
- `login.infrastructure.persistence.adapter`
- `login` tests
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/login/application/port/UserQueryPort.java`
- `src/main/java/com/mealguide/mealguide_api/login/application/service/LoginService.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/repository/UserJpaRepository.java`
- `src/main/java/com/mealguide/mealguide_api/login/infrastructure/persistence/adapter/UserPersistenceAdapter.java`
- `src/test/java/com/mealguide/mealguide_api/login/application/service/LoginServiceTest.java`
- `docs/work-context.md`

### Why
- The refresh flow only needs to confirm that the user is still active before issuing new tokens.
- Loading the full `User` entity on every refresh is unnecessary under the current token-claim design.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No

### Implementation Notes
- Added `existsActiveById(...)` to `UserQueryPort`.
- Implemented the existence check with `existsByIdAndDeletedAtIsNullAndStatus(...)`.
- `LoginService.refresh()` now checks active-user existence and builds the minimal `AuthenticatedUser` directly from token claims.

---

## 2026-04-06

### Task
- Reduce JWT filter database load by replacing full user fetch with an active-user existence check.

### Affected Layers
- `global.auth.security`
- `login.application.port`
- `login` tests
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/AuthenticatedUserPrincipal.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/JwtAuthenticationFilter.java`
- `src/test/java/com/mealguide/mealguide_api/login/infrastructure/security/JwtAuthenticationFilterTest.java`
- `docs/work-context.md`

### Why
- The JWT filter runs on every authenticated request.
- Under the current token design, loading the full `User` entity is unnecessary when only active-user validation and principal userId population are needed.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No

### Implementation Notes
- `JwtAuthenticationFilter` now uses `existsActiveById(...)` instead of `findById(...)`.
- Added `AuthenticatedUserPrincipal.authenticated(...)` for minimal principal construction from token claims.
- The filter now populates authentication without loading user email, name, or role from the database.

### Remaining Issues
- Because access tokens intentionally omit role claims, role-based authorization would require either reintroducing a user lookup or adding role information to the token.

---

## 2026-04-06

### Task
- Fix `RestClientConfig` bean creation failure caused by an unavailable `Jackson2ObjectMapperBuilder`.

### Affected Layers
- `global.config.base`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/global/config/base/RestClientConfig.java`
- `docs/work-context.md`

### Why
- The current configuration required `Jackson2ObjectMapperBuilder` injection, but that builder bean was not available in the running context.
- The auth/security infrastructure still needs a shared `ObjectMapper` and `RestClient.Builder` without depending on extra auto-configured beans.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No

### Implementation Notes
- `RestClientConfig` now provides an `ObjectMapper` only as a fallback with `@ConditionalOnMissingBean(ObjectMapper.class)`.
- If Spring Boot auto-configures the default Jackson mapper, that bean is used unchanged.
- If no `ObjectMapper` bean exists in the running context, the fallback bean uses `JsonMapper.builder().findAndAddModules().build()`.
- `RestClient.Builder` registration remains unchanged.

---

## 2026-04-06

### Task
- Remove `ObjectMapper` bean dependency from security exception handlers to avoid startup failure.

### Affected Layers
- `global.auth.security`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/RestAccessDeniedHandler.java`
- `src/main/java/com/mealguide/mealguide_api/global/auth/security/RestAuthenticationEntryPoint.java`
- `docs/work-context.md`

### Why
- The running context still reported missing `ObjectMapper` injection for security handlers.
- These handlers only need simple JSON serialization for error responses, so they should not block startup on a shared bean.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No

### Implementation Notes
- Replaced constructor-injected `ObjectMapper` usage with an internal static `JsonMapper`.
- Security error response shape stays the same.

---

## 2026-04-06

### Task
- Add minimum length validation for JWT secrets at configuration binding time.

### Affected Layers
- `global.auth.jwt`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/global/auth/jwt/JwtProperties.java`
- `docs/work-context.md`

### Why
- `Keys.hmacShaKeyFor()` requires a sufficiently long secret key and otherwise fails at runtime.
- Configuration errors should be rejected during property binding instead of surfacing later during JWT provider initialization.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No

### Implementation Notes
- Added `@Size(min = 32)` to both `accessSecret` and `refreshSecret`.
- Existing `@NotBlank` and expiration-time validation remain unchanged.

---

## 2026-04-06

### Task
- Centralize the JJWT library version in Maven properties.

### Affected Layers
- `pom.xml`
- `docs/work-context.md`

### Changed Files
- `pom.xml`
- `docs/work-context.md`

### Why
- The same JJWT version string was repeated across multiple dependencies.
- Defining the version once in Maven properties makes future updates safer and more consistent.

### DB Impact
- Schema changed by this task: No

### API Impact
- External API contract changed: No

### Implementation Notes
- Added `jjwt.version` to the Maven `<properties>` section.
- Updated `jjwt-api`, `jjwt-impl`, and `jjwt-jackson` to use `${jjwt.version}`.

---

## 2026-04-06

### Task
- Remove the Swagger-based auth debug testing APIs and keep only the HTML-based test flow.

### Affected Layers
- `authdebug`
- `global.config.security`
- `docs/project-context.md`
- `docs/work-context.md`

### Changed Files
- `src/main/java/com/mealguide/mealguide_api/authdebug/application/service/AuthDebugService.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/controller/AuthDebugController.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/dto/request/AuthDebugLoginRequest.java`
- `src/main/java/com/mealguide/mealguide_api/authdebug/presentation/swagger/AuthDebugApi.java`
- `src/main/java/com/mealguide/mealguide_api/global/config/security/SecurityConfig.java`
- `docs/project-context.md`
- `docs/work-context.md`

### Why
- The local HTML page is now the intended way to test the real Google login flow.
- Separate Swagger endpoints for bypass-based auth testing are no longer needed and should be removed to reduce confusion.

### DB Impact
- Schema changed by this task: No

### API Impact
- Removed local/dev-only debug testing endpoints:
  - `POST /auth-debug/login`
  - `POST /auth-debug/refresh`
  - `POST /auth-debug/logout`
- Kept local/dev-only config endpoint:
  - `GET /auth-debug/config`

### Implementation Notes
- Deleted the debug login service, controller, request DTO, and Swagger interface.
- Removed the debug login and refresh routes from the security whitelist.
- Updated project context so `authdebug.*` now describes only local/dev support code for the HTML test page.
