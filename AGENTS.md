# Repository Guidelines

## Project Overview

This repository is a Java 21 Spring Boot backend for Bread Diary.

- Project name: `bread-diary-backend`
- Gradle root project name: `bread-diary`
- Base package: `com.bean.breaddiary`
- Main class: `BreadDiaryApplication`
- JPA Auditing is enabled

## Tech Stack

- Java 21
- Gradle
- Spring Boot 4.0.5
- Spring Data JPA
- Spring Validation
- Spring Web MVC
- springdoc-openapi
- MapStruct
- AWS SDK S3
- MySQL

## Project Structure

Main code lives under `src/main/java/com/bean/breaddiary`.

Organize code by domain and follow the existing project structure.

- `domain/<feature>/controller` for HTTP endpoints
- `domain/<feature>/service` for business logic
- `domain/<feature>/repository` for Spring Data JPA access
- `domain/<feature>/entity` for persistence models
- `domain/<feature>/dto/request` for request DTOs
- `domain/<feature>/dto/response` for response DTOs
- `domain/<feature>/dto/mapper` for MapStruct mappers
- `global/` for shared concerns such as S3 and common response wrappers

Tests belong in `src/test/java/com/bean/breaddiary`, mirroring the production package structure.

## Existing Implemented Domains

This is not a blank scaffold.

The following domains already exist and must be used as reference before implementing anything new:

- `domain.bread`
- `domain.breadrecord`

Always inspect similar existing code first, then extend in the same style.

## Current Domain Characteristics

### Bread
- `stickerNumber` is unique
- `name` is unique
- has `breadType`
- has `imageUrl`
- if `createdBy` exists, it means a user-created bread

### BreadRecord
- has `userId`
- has `bread` as ManyToOne
- has `photoUrl`
- has `shopName`
- has `eatenDate`
- has `rating`
- has `review`
- has `isPublic`
- uses `deletedAt` for soft delete

## API and Serialization Rules

- Reuse the existing `global.common.ApiResponse` response wrapper
- Keep the response structure consistent with `{ success, data }`
- Do not break existing snake_case response expectations
- Be careful when changing JSON serialization behavior
- Multipart request fields follow snake_case naming
- Existing create/update endpoints use `multipart/form-data` with `@ModelAttribute`

## Current Authentication / User Identification Rules

- Authentication is not fully implemented yet
- Use `X-USER-ID` as the temporary user identification mechanism
- Do not introduce a new authentication or authorization system unless explicitly requested

## Service Layer Rules

- Keep controllers thin
- Put business logic in services
- If the existing flow uses service and complex orchestration patterns, follow the existing structure instead of inventing a new one
- Prefer minimal targeted changes over broad refactoring

## Coding Conventions

- Use 4-space indentation
- Use `PascalCase` for classes
- Use `camelCase` for fields and methods
- Use `UPPER_SNAKE_CASE` for constants
- Keep package names lowercase
- Prefer constructor injection with `@RequiredArgsConstructor`
- Name DTOs by intent, for example `CreateBreadRecordRequest`, `BreadRecordCreateResponse`

## Swagger / Message Tone

- Keep Swagger descriptions in Korean
- Keep user-facing messages in Korean
- Some existing text may have encoding issues; preserve meaning rather than aggressively rewriting everything

## Testing Rules

- Use JUnit 5
- Add focused tests for controller/service changes when reasonable
- Avoid over-expanding test infrastructure unless necessary
- Existing tests may expect snake_case JSON fields, so preserve that behavior

## Build / Run Commands

Use the Gradle wrapper where available.

Typical commands:
- `./gradlew test`
- `./gradlew build`
- `./gradlew bootRun`
- `./gradlew clean build`

Note:
- the repository may currently have environment/setup limitations
- do not assume runtime configuration files already exist under `src/main/resources`

## Git / PR Rules

- Do not work directly on `main` or `develop`
- Branch from `develop`
- PR target is `develop`
- Follow Conventional Commits
- Branch names may follow team issue-based naming such as `feat/25-user-domain`

## Working Rules for Codex

Before making changes:
1. Read the relevant existing domain code first
2. Summarize the reference files briefly
3. Check whether similar controller/service/repository/DTO patterns already exist
4. Reuse existing conventions before creating anything new

When responding with implementation:
1. First list which files will be created or modified
2. Then provide the code changes
3. At the end, list:
   - created files
   - modified files
   - follow-up TODOs
   - what the next branch should read first
