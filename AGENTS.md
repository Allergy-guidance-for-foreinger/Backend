# AGENTS.md

## 1. Goal
This file defines the working rules for CLI agents and coding assistants in this project.

The agent must understand the existing project structure and related documents first, then make minimal and accurate changes based on the user's request.

---

## 2. Mandatory Reading Order

Before starting any task, read these files in order:

1. `README.md`
2. `AGENTS.md`
3. `docs/project-context.md`
4. `docs/database-context.md`
5. `docs/work-context.md`

If the task is database-related, also read:

6. `docs/schema.sql`

Do not skip the reading step.

### Document Roles
- `README.md`: public project overview
- `docs/project-context.md`: detailed project structure, architecture, and working context
- `docs/database-context.md`: database summary and business rules
- `docs/work-context.md`: accumulated task history and change context
- `docs/schema.sql`: source-of-truth PostgreSQL schema reference

---

## 3. Project Structure Rules

The project package structure is fixed as follows:

```text
presentation
- controller
- dto
  - request
  - response
- swagger

application
- service
- port

domain

infrastructure
- persistence
  - repository
  - adapter
 
Follow these rules:

Put HTTP endpoints in presentation.controller.
Put request DTOs in presentation.dto.request.
Put response DTOs in presentation.dto.response.
Put Swagger/OpenAPI config in presentation.swagger.
Put use-case orchestration logic in application.service.
Put repository access abstractions in application.port.
Put core domain models and business concepts in domain.
Put persistence implementations in infrastructure.persistence.
Put Spring Data repositories in infrastructure.persistence.repository.
Put implementations of application ports in infrastructure.persistence.adapter.

Do not arbitrarily move classes across layers.
Do not introduce a different architecture unless the user explicitly requests it.

Shared authentication infrastructure that is reused across features should live under `global.auth`.
Feature-specific login logic should remain under the feature package.
```

## 4. Architectural Behavior Rules
   - Keep controllers thin.
   - Do not place business logic in controllers.
   - Do not return entities directly from controllers.
   - Keep request DTO and response DTO separated.
   - Use application services to coordinate use cases.
   - Use application ports to abstract access to persistence or external dependencies.
   - Keep persistence-specific details inside the infrastructure layer.
   - Preserve the current package structure and naming style.
   - Prefer minimal changes over broad restructuring.


## 5. Database Rules

For any database-related task:

1. Read docs/database-context.md first to understand table roles and business rules.
2. Verify exact schema details in docs/schema.sql.
3. Do not guess column names, constraints, indexes, or relationships from summary docs alone.
4. If docs/database-context.md and docs/schema.sql conflict, docs/schema.sql takes precedence.

Important

- docs/database-context.md is a summary document.
- docs/schema.sql is the full PostgreSQL schema reference.


## 6. Environment Rules
   Local Development
   - Spring runs directly from IntelliJ.
   - PostgreSQL runs as a locally installed server.
   - Redis runs in Docker. 

   Production
   -  Spring, PostgreSQL, and Redis all run in Docker on AWS EC2.

Do not confuse local runtime assumptions with production deployment assumptions.


## 7. Change Policy

When making changes:

- Prefer minimal changes over broad refactoring.
- Preserve the current API contract unless the user requests a change.
- Preserve the existing package structure.
- Preserve business semantics.
- Do not rename fields, methods, packages, or tables without a clear reason.
- Do not create unnecessary new abstractions.
- Do not change architectural boundaries unless explicitly required.


## 8. Documentation Update Policy

After each task, update docs/work-context.md.

If the task changes database structure or DB-related business rules, also update:

- docs/database-context.md
- docs/schema.sql

If the task changes architectural rules or package conventions, also update:

- docs/project-context.md
- AGENTS.md

If the task changes the public project overview, runtime summary, or repository introduction, also update:

- README.md


## 9. Expected Work Output

Before changing code, summarize:

- target feature or bug
- affected layers
- affected packages
- DB impact, if any

After changing code, record in docs/work-context.md:

- what was changed
- why it was changed
- which files were affected
- whether DB schema changed
- whether API behavior changed
- whether related documents were updated
- remaining issues or follow-up tasks


## 10. Prohibited Behaviors

Do not:

- ignore the reading order
- infer DB schema from memory
- change package structure arbitrarily
- move business logic into controller
- return entities directly as API responses
- mix request DTO and response DTO
- perform broad refactoring without explicit request
- change API fields without explicit need
- leave task context undocumented after finishing work
