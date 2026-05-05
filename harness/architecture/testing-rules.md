# Testing Rules (Draft)

## 목적

작은 수정이라도 기존 동작을 깨지 않았는지 빠르게 확인할 수 있도록 한다.

## 기본 규칙

### 1. 변경 계층에 가까운 테스트 우선

작은 표현 규칙 변경:

- Mapper test 우선

요청/응답 contract 변경:

- Controller test 우선

흐름/오케스트레이션 변경:

- Service / ComplexService test 우선

### 2. 기존 테스트 패턴 재사용

이미 있는 테스트 구조를 복제/확장하는 방식이 우선이다.

예:

- `BreadControllerAutocompleteTest`
- `BreadMapperTest`
- `BreadRecordComplexServiceTest`
- `AuthServiceTest`

### 3. 응답 필드 테스트 시 camelCase 확인

새 응답 필드를 추가하면 가능하면:

- camelCase field 확인
- 의도하지 않은 snake_case field 비노출 확인

을 같이 본다.

### 4. 외부 연동은 단위 테스트 + 제한적 통합 검증

예:

- Toss
- S3
- CloudFront

단위 테스트로:

- request/response mapping
- config loading
- fallback / validation

를 검증하고,

실환경 검증이 필요하면 별도 수동 체크리스트를 둔다.

### 5. 테스트 명명은 기대 동작 중심

예:

- `mapToCatalogItemUsesPlaceholderImageForUncollectedBread`
- `autocompleteBreadsReturnsSpecResponseWithCamelCaseFields`

## 현재 프로젝트 메모

- 테스트는 JUnit 5 기반
- mapper/controller/service 테스트가 이미 고르게 존재
- 중요한 표현 규칙은 mapper test로 먼저 고정하는 게 효율적이다
