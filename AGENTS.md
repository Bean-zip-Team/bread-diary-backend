# Repository Guidelines

## Project Structure & Module Organization
This is a Java 21 Spring Boot backend built with Gradle. Main code lives in `src/main/java/com/bean/breaddiary`, split by domain (`bread`, `breadrecord`, `breadtype`, `auth`, `event`, `recommendation`, `user`) plus shared code under `global`. Tests mirror production packages in `src/test/java`. Runtime resources live in `src/main/resources`. Detailed contributor rules are intentionally split into `harness/`: see `harness/architecture/` for shared conventions, `harness/domains/` for domain notes, and `harness/workflows/` for delivery rules.

## Build, Test, and Development Commands
- `./gradlew test` — run the JUnit 5 test suite.
- `./gradlew build` — compile, test, and package the app.
- `./gradlew bootRun` — start the API locally.
- `./gradlew clean build` — rebuild from scratch when outputs/config look stale.
Use the Gradle wrapper committed to the repository.

## Coding Style & Naming Conventions
Use 4-space indentation, `PascalCase` for classes, `camelCase` for fields/methods, and lowercase package names. Prefer constructor injection and Lombok class-based DTOs (`@RequiredArgsConstructor`, `@Getter`, `@Setter`); do not introduce Java records unless explicitly requested. Keep mapping logic in Mapper classes. Regular services should depend only on their own repository and mapper; cross-domain flows belong in `ComplexService`. Put Swagger/OpenAPI annotations on controllers and DTOs. Reuse existing patterns from `domain/bread` and `domain/breadrecord` before introducing new structure.

## Testing Guidelines
This project uses JUnit 5, Spring Boot test starters, H2, and Testcontainers. Add tests in the matching package and name them `*Test`, for example `BreadRecordComplexServiceTest`. Prefer the nearest-layer test for the change: mapper tests for formatting/mapping rules, controller tests for request/response contracts, and service/complex-service tests for orchestration.

## Commit & Pull Request Guidelines
Follow Conventional Commit prefixes such as `feat:`, `fix:`, and `chore:` with a short imperative subject. Keep work in one logical commit or a small related set. Do not default to long PR or issue prose; provide a brief work summary, the files to stage, and a recommended commit message unless more is requested.
