# Database Context

## 1. Purpose
This document summarizes the main table roles and DB-related business rules for Mealguide API.

Important:
- This is a summary document.
- `docs/schema.sql` is the source of truth for exact schema definitions.

## 2. Source of Truth
Always verify the following in `docs/schema.sql` before changing DB-related code:
- table names
- column names and types
- nullable rules
- primary keys / foreign keys
- unique constraints
- indexes

If this document and `docs/schema.sql` differ, follow `docs/schema.sql`.

## 3. Core Data Concepts
- user accounts and user dietary preferences
- school and cafeteria ownership structure
- meal schedules per cafeteria and date
- menu master data
- daily meal menu data
- ingredient master data
- user allergy and avoided ingredient mappings
- religious food restriction mappings
- AI analysis data
- confirmed ingredient data
- translation data

## 4. Table Summary

### `users`
- Stores user account and authentication information
- Relevant columns for authentication:
  - `id`
  - `school_id` nullable
  - `email`
  - `name`
  - `language_code` nullable, references `language(code)`
  - `religious_code` nullable, references `religious_food_restriction(code)`
  - `onboarding_completed`
  - `status`
  - `role`
  - `deleted_at`
- Login code currently treats `status` as:
  - `ACTIVE`
  - `INACTIVE`
- Login code currently treats `role` as:
  - `USER`
  - `ADMIN`
  - `MANAGER`
- Entity delete behavior uses soft delete semantics:
  - entity delete updates `status` to `INACTIVE`
  - inactive users are excluded from normal ORM selection
- New users created during first-login signup rely on PostgreSQL identity columns:
  - `users.id`
  - `user_oauth_accounts.id`

### `user_oauth_accounts`
- Stores external OAuth account mappings for users
- Relevant columns for authentication:
  - `id`
  - `user_id`
  - `provider`
  - `provider_user_id`
  - `provider_email`

### `school`, `cafeteria`
- Store school and cafeteria hierarchy
- `school_translation` stores language-specific school names and falls back to `school.name` when no requested translation exists

### `language`
- Stores selectable app language master data
- `language.code` is referenced by `users.language_code` and translation-table `lang_code` columns

### `allergy`, `ingredient`, `religious_food_restriction`
- Store master data used for dietary checks

### `user_allergy`, `user_avoided_ingredient`
- Store per-user dietary risk settings
- `user_allergy` stores the user's selected allergy master codes as full-replacement settings

### `meal_schedule`, `menu`, `meal_menu`
- Store daily cafeteria meal data and menu composition

### `menu_ai_analysis`, `menu_ai_analysis_ingredient`
- Store AI-derived ingredient analysis results

### `meal_menu_confirmed_ingredient`, `meal_menu_confirmation_history`
- Store confirmed ingredient data and admin change history

### Translation tables
- `school_translation`
- `menu_translation`
- `ingredient_translation`
- `allergy_translation`
- `religious_food_restriction_translation`
- Translation `lang_code` columns reference `language(code)`

## 5. Business Rules
- Daily meal data and menu master data are separate concerns.
- AI analysis data is draft-like and must not be treated the same as confirmed data.
- User-specific dietary checks should be based on mapped ingredient data, not only menu names.
- Translation data supplements source data but does not replace the original source-of-truth records.
- Meal crawling integration is synchronous Java-to-Python HTTP based (no queue/event broker).
- `meal_menu_confirmed_ingredient` and confirmation history are admin-confirmed data and are not auto-created by crawl import.
- Meal import success must remain intact even when AI analysis or translation follow-up fails.
- User language preference is stored in `users.language_code`.
- User allergy settings are replaced as a full set in `user_allergy`.
- `users.religious_code` can be null when the user has no selected religious food restriction.
- `users.deleted_at` indicates soft deletion and should be respected by authentication and user lookup flows.
- `users.status = INACTIVE` is also treated as a soft-deleted or disabled state in ORM-based user queries.

## 6. DB Access Layer Rule
Database access must follow the current package structure:
- `{feature}.application.port`
- `{feature}.infrastructure.persistence.adapter`
- `{feature}.infrastructure.persistence.repository`

Application services should depend on ports, not directly on repository implementation details.

## 7. Authentication-Related Notes
- Current login work uses `users` and `user_oauth_accounts` with automatic first-login signup.
- Mobile login accepts a Google ID token from the client.
- Google login should map the verified Google account to an existing user through `user_oauth_accounts`.
- `users.email` is nullable in the current schema, so login logic must not assume email-only lookup is sufficient.
- Only `ACTIVE` users should be selected by authentication queries.
- If no linked user exists on first login, a new `users` row and `user_oauth_accounts` row are created.
- First login auto-signup currently creates:
  - `school_id = null`
  - `onboarding_completed = false`
  - `status = ACTIVE`
  - `role = USER`
- Onboarding completion API stores school, allergy, and religious selections atomically and updates:
  - `users.language_code`
  - `users.school_id`
  - `users.religious_code`
  - `user_allergy` (full replacement)
  - `users.onboarding_completed = true`
- Refresh tokens are managed in Redis, not in PostgreSQL.

## 8. When This Document Must Be Updated
Update this document when:
- table roles or data responsibilities change
- user/account-related business rules change
- DB access layer conventions change
- authentication starts depending on new DB structures
- `users` semantics change
