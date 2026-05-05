# Coding Conventions (Draft)

## 기본 스타일

- 4-space indentation
- `PascalCase` for classes
- `camelCase` for fields and methods
- `UPPER_SNAKE_CASE` for constants
- lowercase package names

## 클래스/주입 스타일

- constructor injection 우선
- `@RequiredArgsConstructor` 선호
- DTO/helper는 Lombok class 기반 유지
- Java `record`는 명시적 요청 없으면 사용하지 않는다

## DTO 네이밍

의도 중심 이름 사용:

- `CreateBreadRecordRequest`
- `BreadRecordCreateResponse`
- `BreadCatalogListResponse`

## Swagger / 메시지 톤

- Swagger 설명은 한국어 유지
- user-facing 메시지도 한국어 유지
- 기존 텍스트에 인코딩 이슈가 있어도 의미를 우선 보존한다
