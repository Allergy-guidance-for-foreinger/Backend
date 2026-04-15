# Project Context

## 1. Purpose
Mealguide API is a Spring Boot backend for serving cafeteria meal information and user-specific dietary risk guidance for a mobile client.

Main goals:
- Expose daily meal menus by date and cafeteria
- Provide menu and ingredient information in a structured form
- Reflect allergy, avoided ingredient, and religious restriction settings per user
- Support AI analysis data and confirmed ingredient data separately
- Provide translated response data when needed

## 2. Runtime Environments

### Local Development
- Spring application: run directly from IntelliJ
- PostgreSQL: locally installed server
- Redis: run in Docker

### Production
- AWS EC2
- Spring application, PostgreSQL, and Redis all run in Docker

## 3. Tech Stack
- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Redis
- Spring Security
- Swagger / OpenAPI

## 4. Package Structure

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

global
- auth
  - domain
  - port
  - jwt
  - redis
  - security

login
- application
  - service
  - port
- presentation
  - controller
  - dto
  - swagger
- infrastructure
  - persistence
  - redis
  - jwt
  - security
- domain

authdebug
- presentation
  - controller
  - dto

onboarding
- presentation
  - controller
  - dto.response
  - swagger
- application
  - service
  - port
- domain
- infrastructure.persistence
  - repository
  - adapter

settings
- presentation
  - controller
  - dto.request
  - dto.response
  - swagger
- application
  - service
  - port
- domain
- infrastructure.persistence
  - repository
  - adapter
```

### Package Roles

#### presentation
- feature `presentation.controller`: HTTP endpoint entry points
- feature `presentation.dto.request`: request DTOs
- feature `presentation.dto.response`: response DTOs
- feature `presentation.swagger`: endpoint documentation helpers

#### application
- feature `application.service`: use-case orchestration
- feature `application.port`: abstraction for persistence and external dependencies

#### domain
- feature domain entities
- feature domain concepts and enums
- authentication/session domain records when needed

#### infrastructure
- feature `infrastructure.persistence.repository`: Spring Data JPA repositories
- feature `infrastructure.persistence.adapter`: implementations of application ports for persistence
- `security`: Spring Security integration
- `redis`: Redis-backed adapters
- `jwt`: JWT generation and parsing
- `config`: infrastructure-level configuration

#### global.auth
- shared authentication domain records such as authenticated user and token claims
- shared token provider and refresh token ports
- shared JWT implementation
- shared Redis refresh token storage
- shared Spring Security principal, filter, and auth error handlers

#### login
- authentication-related implementation is grouped under the `login` feature package
- the feature keeps Google login use case orchestration, request/response DTOs, Google token verification, and user lookup logic
- shared global concerns such as `global.base` and common config remain outside the feature package

#### onboarding
- onboarding-specific APIs and persistence are grouped under the `onboarding` feature package
- school list lookup uses `onboarding.presentation`, `onboarding.application`, `onboarding.domain`, and `onboarding.infrastructure`

#### settings
- user personal settings and selectable settings master data are grouped under the `settings` feature package
- language, allergy, and religious restriction settings use `settings.presentation`, `settings.application`, `settings.domain`, and `settings.infrastructure`

#### authdebug
- local/dev-only support package for authentication test tooling
- currently provides config data needed by the static auth test page

## 5. Architecture Rules
- Controllers stay thin.
- Business logic does not belong in controllers.
- Request DTO and response DTO are separated.
- Entities are not returned directly from controllers.
- Feature `application.service` coordinates use cases.
- Feature `application.port` abstracts persistence and infrastructure concerns.
- Persistence-specific details stay in the feature `infrastructure`.
- Feature `infrastructure.persistence.adapter` implements feature `application.port`.
- Feature `infrastructure.persistence.repository` contains Spring Data JPA repositories.
- For the login feature, user lookup and Google login flow stay under `login.*`.
- Shared auth infrastructure belongs under `global.auth.*`.
- Local/dev-only auth test support code belongs under `authdebug.*` and should stay disabled unless explicitly enabled by configuration.

## 6. Database Documents
- Database summary and rules: `docs/database-context.md`
- Source-of-truth schema: `docs/schema.sql`

When exact columns, constraints, indexes, or relationships matter, always follow `docs/schema.sql`.

## 7. Recommended Reading Order for CLI
1. `README.md`
2. `AGENTS.md`
3. `docs/project-context.md`
4. `docs/database-context.md`
5. `docs/work-context.md`
6. `docs/schema.sql` for DB-related work

## 8. Working Rules
- Preserve the current package structure.
- Prefer minimal changes over refactoring.
- Reuse existing global response and exception handling where possible.
- For DB-related changes, check `docs/schema.sql` before implementing.
- Update `docs/work-context.md` after each task.

## 9. Domain Notes
Key concepts frequently used in the project:
- user account and preference data
- selectable language master data
- cafeteria and meal schedule data
- menu master and daily meal menu data
- ingredient master and menu-ingredient relations
- allergy / avoided ingredient / religious restriction rules
- AI analysis results
- confirmed ingredient data
- translation data

## 10. Documentation Update Rule
Update this document when any of the following change:
- package structure or package conventions
- architecture boundary rules
- core domain concepts
- runtime environment assumptions
