# Response Rules (Draft)

## 목적

응답 형식과 JSON naming 규칙을 일관되게 유지한다.

## 기본 규칙

### 1. 공통 응답 래퍼 사용

응답은 기존 `ApiResponse` 패턴을 따른다.

기본 구조:

```json
{
  "success": true,
  "data": {}
}
```

에러는 `ApiErrorResponse` 규칙을 따른다.

### 2. JSON 필드명은 camelCase 유지

API 필드명은 camelCase를 기본으로 본다.

예:

- `imageUrl`
- `isCollected`
- `totalRecordCount`

새 API를 추가할 때도 특별한 이유가 없으면 camelCase를 유지한다.

### 3. DTO는 의도 중심 이름 사용

예:

- `BreadCatalogListResponse`
- `BreadAutocompleteItemResponse`
- `BreadRecordCreateResponse`

### 4. 표현 규칙은 응답 계층에서 맞춘다

응답 전용 규칙은 Mapper/Response DTO 계층에서 맞춘다.

예:

- `photoThumbnailUrl` 파생
- placeholder 이미지 URL 파생
- 평균값 반올림

### 5. optional auth 응답도 구조는 동일

비로그인/로그인 여부에 따라 일부 값만 달라지고, 응답 구조는 동일하게 유지한다.

예:

- 비로그인 시 `isCollected=false`
- 로그인 시 실제 수집 여부 반영

## 현재 프로젝트 메모

- 현재 코드/테스트에는 snake_case 기대가 일부 남아 있을 수 있다.
- 이후 규칙 정리가 진행되면 신규 API와 기존 API 정렬 방향도 함께 맞춘다.
