# AGENTS.md

## Project Overview
- Backend service for the `confhub` application.
- Stack: Java 17, Spring Boot 4, Maven, PostgreSQL, Spring Security, JPA, WebSocket, Thymeleaf.
- Main code lives under `src/main/java/com/capstone/confhub`.
- Tests live under `src/test/java/com/capstone/confhub`.
- Runtime templates and product documentation live under `src/main/resources`.

## Repository Layout
- `src/main/java/com/capstone/confhub/controller`: HTTP controllers.
- `src/main/java/com/capstone/confhub/service`: business logic.
- `src/main/java/com/capstone/confhub/repository`: Spring Data repositories.
- `src/main/java/com/capstone/confhub/entity`: JPA entities.
- `src/main/java/com/capstone/confhub/dto`: request and response DTOs.
- `src/main/java/com/capstone/confhub/security`: authentication and authorization code.
- `src/main/resources/templates`: email and report templates.
- `scripts`: local reporting and workbook generation helpers.

## Working Conventions
- Keep changes targeted and consistent with existing Spring patterns.
- Prefer fixing issues at the service or domain boundary instead of patching controller behavior.
- Do not rename packages, move files, or reorganize modules unless explicitly requested.
- Treat `.env`, `application.properties`, and any credentials or API keys as sensitive.
- Avoid modifying generated output under `target` or ad hoc artifacts under `output` unless the task is specifically about them.

## Build And Validation
- Install and run with Maven wrapper: `./mvnw`.
- Common commands:
  - `./mvnw test`
  - `./mvnw spring-boot:run`
  - `./mvnw clean package`
- Prefer running the smallest relevant test scope first, then broader validation if needed.

## Implementation Notes
- Follow existing Lombok and Spring annotations already used in the touched area.
- Reuse DTOs, repository methods, and service patterns before adding new abstractions.
- Keep API changes synchronized with any related docs in `src/main/resources` when behavior changes materially.
- When editing templates, preserve placeholder names expected by the mail/report generation flow.

## Testing Expectations
- Add or update focused tests when changing service logic, controller behavior, or security-sensitive flows.
- Prefer controller tests for request/response behavior and service tests for business rules.
- Do not fix unrelated failing tests as part of a focused task; note them separately instead.

## Agent Guidance
- Check for deeper `AGENTS.md` files before editing subdirectories with specialized rules.
- Read large files in chunks.
- Before using new dependencies or major refactors, confirm the need with the user.
