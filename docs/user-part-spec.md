# User Part Spec for Codex

## 목적

현재 Bread Diary 백엔드에서 user 파트를 단계적으로 구현한다.
이 프로젝트는 신규 스캐폴드가 아니라 `bread`, `breadrecord` 도메인이 이미 구현된 상태다.
새 기능은 반드시 기존 코드 스타일을 존중해 최소 수정 방식으로 추가한다.

## 현재 프로젝트 전제

- base package: `com.bean.breaddiary`
- 응답은 `global.common.ApiResponse` 래퍼 사용
- 현재 사용자 식별은 `X-USER-ID` 헤더 기반 임시 처리
- 테스트는 snake_case JSON 응답을 기대할 가능성이 높음
- 기존 create/update API는 `multipart/form-data` + `@ModelAttribute`
- Swagger와 사용자 메시지는 한국어 톤 유지
- `BreadRecord`는 `deletedAt` 기반 soft delete 사용

## 현재 진행 상태

### 완료
- `feat/25-user-domain`
- user 도메인 최소 뼈대 추가
  - `domain.user.entity.User`
  - `domain.user.repository.UserRepository`
  - `domain.user.dto.response.UserMeResponse`
  - `domain.user.dto.response.UserStatsResponse`

### 다음 작업
1. `feat/26-user-profile-service`
2. `feat/27-user-profile-api`
3. `test/28-user-profile-api`
4. `feat/29-user-withdrawal`

## 이번 user 파트 범위

### 1단계
user 도메인 기본 구조 추가

- `domain.user.entity.User`
- `domain.user.repository.UserRepository`
- `domain.user.dto.response.UserMeResponse`
- `domain.user.dto.response.UserStatsResponse`

### 2단계
현재 사용자 정보 및 통계 조회 서비스 구현

통계 항목:
- `total_records`
- `unique_shops`
- `avg_rating`

집계 시 조건:
- soft delete 된 bread record 제외
- `shopName`이 null 또는 blank인 값 처리 기준을 코드에서 명확히 통일

### 3단계
현재 사용자 정보 조회 API 구현

엔드포인트:
- `GET /users/me`

인증/식별:
- `X-USER-ID` 헤더 사용

응답 예시:
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "nickname": "빵순이",
    "email": "bread@toss.im",
    "profile_image_url": "https://cdn.breaddex.app/profiles/550e8400.webp",
    "bio": null,
    "stats": {
      "total_records": 42,
      "unique_shops": 18,
      "avg_rating": 4.2
    },
    "created_at": "2026-04-01T00:00:00Z"
  }
}
```

### 4단계
현재 사용자 정보 조회 검증

검증 대상:
- `GET /users/me` 정상 조회
- `X-USER-ID` 헤더 누락 또는 잘못된 형식 처리
- `ApiResponse` 구조 유지
- snake_case 응답 필드 유지
- 통계 필드 직렬화 확인

### 5단계
회원탈퇴 구현

엔드포인트:
- `DELETE /users/me`

권장 정책:
- `User`는 soft delete로 처리한다.
- `deletedAt` 필드를 추가한다.
- 탈퇴 시 해당 사용자의 `BreadRecord`도 soft delete 처리하는 방향을 우선 검토한다.
- 기존 인증 체계를 새로 만들지 않고 현재 `X-USER-ID` 흐름을 유지한다.

응답 예시:
```json
{
  "success": true,
  "data": null
}
```

## 구현 원칙

- 기존 `bread`, `breadrecord` 구현을 먼저 읽고 같은 스타일로 작업한다.
- 없는 구조를 새로 크게 만들지 않는다.
- 새로운 인증 체계나 전역 예외 체계를 임의로 도입하지 않는다.
- controller는 얇게, 서비스는 명확하게 유지한다.
- 필요 이상으로 리팩터링하지 않는다.
- DTO, 엔티티, 리포지토리 네이밍은 기존 프로젝트 스타일에 맞춘다.
- `User`에 soft delete를 도입하면, 이후 조회 로직도 활성 사용자 기준으로 일관되게 맞춘다.

## 브랜치 순서

1. `feat/25-user-domain` ✅
2. `feat/26-user-profile-service`
3. `feat/27-user-profile-api`
4. `test/28-user-profile-api`
5. `feat/29-user-withdrawal`

## 각 단계 종료 시 Codex가 정리해야 할 것

- 이번 브랜치 생성 파일
- 이번 브랜치 수정 파일
- 다음 브랜치가 먼저 읽어야 할 파일
- 남은 TODO
