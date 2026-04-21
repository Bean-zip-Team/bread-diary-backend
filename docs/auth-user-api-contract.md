# Auth/User API 계약 기준 문서

## 1. 문서 목적

이 문서는 현재 `bread-diary-backend`의 auth/user API 계약을 한 곳에 정리한 기준 문서다.

기준은 현재 Java 코드와 테스트다. 프론트 연동, Swagger 설명 최신화, 수동 E2E 문서를 정리할 때는 이 문서를 먼저 본다.

## 2. 공통 원칙

| 항목 | 현재 기준 |
| --- | --- |
| 응답 래퍼 | `{ "success": true, "data": ... }` 또는 `{ "success": false, "error": ... }` |
| auth/user 공식 request/response naming | camelCase |
| 인증 방식 | Bearer access token |
| refresh token 용도 | `/auth/refresh`, refreshToken body 기반 `/auth/logout`에서만 사용 |
| 임시 사용자 식별 | `X-USER-ID`를 auth/user 흐름에서 사용하지 않음 |
| requestId | `X-Request-Id` 응답 헤더와 access log의 `requestId`가 같은 값 |
| CORS exposed headers | `Location`, `X-Request-Id` |

전역 Jackson 설정은 기존 bread 계열 응답 기대와 맞물려 있으므로 이 문서의 auth/user camelCase 계약을 이유로 전역 직렬화 규칙을 바꾸지 않는다.

## 3. 인증 경계

| 구분 | Endpoint | 인증 |
| --- | --- | --- |
| Public | `POST /auth/toss` | 불필요 |
| Public | `POST /auth/refresh` | 불필요, body에 refresh token 필요 |
| Public | `POST /auth/webhook/toss-unlink` | Bearer 불필요, `x-toss-webhook-secret` 필요 |
| Protected | `POST /auth/logout` | Bearer access token 필요 |
| Protected | `GET /users/me` | Bearer access token 필요 |
| Protected | `DELETE /users/me` | Bearer access token 필요 |

`AuthInterceptor`는 access token만 일반 API 인증에 허용한다. refresh token을 Bearer token으로 보내면 인증 토큰으로 취급하지 않는다.

## 4. `POST /auth/toss`

Toss 로그인 또는 가입 후 앱 access/refresh token을 발급한다.

실제 성공 검증은 백엔드 단독으로 끝낼 수 없다. 프론트가 Toss에서 받은 `authorizationCode`, `referrer`, Toss console redirect 설정, Toss 복호화 env, Toss 외부 API 접근이 필요하다.

Request:

```json
{
  "authorizationCode": "<TOSS_AUTHORIZATION_CODE>",
  "referrer": "<TOSS_REFERRER>"
}
```

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `userId` | UUID | 앱 사용자 ID |
| `accessToken` | string | 앱 access token |
| `refreshToken` | string | 앱 refresh token |
| `tokenType` | string | 현재 `Bearer` |
| `accessTokenExpiresAt` | datetime | access token 만료 시각 |
| `refreshTokenExpiresAt` | datetime | refresh token 만료 시각 |
| `newUser` | boolean | 신규 가입 여부 |

## 5. `POST /auth/refresh`

refresh token을 검증하고 access/refresh token을 모두 재발급한다.

Request:

```json
{
  "refreshToken": "<REFRESH_TOKEN>"
}
```

Response data는 `/auth/toss`와 같은 token pair 구조다.

정책:

| 항목 | 기준 |
| --- | --- |
| refresh 성공 | access/refresh 모두 재발급 |
| refresh token rotation | 적용 |
| 이전 refresh token 재사용 | `409 CONFLICT` |
| 만료/형식 오류/세션 없음 | `401 UNAUTHORIZED` |

## 6. `POST /auth/logout`

현재 공식 호출 방식은 Bearer access token 기반 logout이다.

| 요청 형태 | 현재 계약 |
| --- | --- |
| Bearer access token + body 없음 | 허용. 현재 인증된 session을 종료 |
| Bearer access token + `{}` | 허용. body 없음과 동일 |
| `{ "refreshToken": "<REFRESH_TOKEN>" }` | 허용. 해당 refresh token의 session을 종료 |
| `application/x-www-form-urlencoded` | 사용하지 않음 |
| unsupported content type | `415 INVALID_REQUEST` |

Response data:

```json
{
  "loggedOut": true
}
```

logout 이후 해당 session의 refresh token은 재사용할 수 없어야 한다.

## 7. `GET /users/me`

현재 인증된 사용자의 프로필과 통계를 반환한다.

인증:

```http
Authorization: Bearer <ACCESS_TOKEN>
```

Response data:

| Field | Type | 설명 |
| --- | --- | --- |
| `id` | UUID | 사용자 ID |
| `nickname` | string | 닉네임 |
| `email` | string/null | 이메일 |
| `profileImageUrl` | string/null | 프로필 이미지 URL |
| `bio` | string/null | 자기소개 |
| `stats.totalRecords` | number | 삭제되지 않은 기록 수 |
| `stats.totalStickers` | number | 삭제되지 않은 기록 기준 고유 빵 스티커 수 |
| `stats.uniqueShops` | number | 삭제되지 않은 기록 기준 고유 구매처 수 |
| `stats.avgRating` | number/null | 삭제되지 않은 기록 기준 평균 평점 |
| `createdAt` | datetime/null | 계정 생성 시각 |

auth/user 응답은 camelCase가 기준이다. `total_stickers` 같은 snake_case field를 공식 계약으로 문서화하지 않는다.

## 8. `DELETE /users/me`

현재 인증된 사용자를 탈퇴 처리한다.

Toss userKey가 있는 사용자는 Toss unlink 외부 API 호출이 필요하다. Toss 외부 API 성공 여부와 env 설정에 따라 실환경 검증이 필요할 수 있다.

Response data:

```json
{
  "withdrawn": true
}
```

## 9. `POST /auth/webhook/toss-unlink`

Toss unlink/withdrawal webhook을 처리한다.

Header:

```http
x-toss-webhook-secret: <TOSS_WEBHOOK_SECRET>
```

Request:

```json
{
  "userKey": "<TOSS_USER_KEY>",
  "eventType": "UNLINK"
}
```

`eventType` 값:

| Value | 처리 기준 |
| --- | --- |
| `UNLINK` | 사용자 데이터는 유지하고 session/token 정보를 정리 |
| `WITHDRAWAL_TERMS` | 사용자 데이터 삭제 |
| `WITHDRAWAL_TOSS` | 사용자 데이터 삭제 |

Response data:

```json
{
  "processed": true,
  "eventType": "UNLINK"
}
```

실제 secret, userKey 원문은 문서, 로그, PR, 이슈 댓글에 남기지 않는다.

## 10. 공통 에러 응답

Error response shape:

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "<MESSAGE>"
  }
}
```

주요 status/code:

| Status | Code | 대표 상황 |
| --- | --- | --- |
| `400` | `VALIDATION_FAILED` | request validation 실패 |
| `400` | `INVALID_REQUEST` | 잘못된 JSON, 누락 header, 일반 bad request |
| `401` | `UNAUTHORIZED` | Bearer token 없음/오류, refresh token 오류, webhook secret 오류 |
| `403` | `FORBIDDEN` | 권한 없음 |
| `404` | `NOT_FOUND` | 대상 리소스 없음 |
| `409` | `CONFLICT` | refresh token 재사용 등 충돌 |
| `415` | `INVALID_REQUEST` | unsupported content type |
| `429` | `RATE_LIMITED` | rate limit 초과. 현재 정책 결정 필요 |
| `502` | `TOSS_SERVER_ERROR` | Toss 외부 연동 실패 |
| `500` | `INTERNAL_ERROR` | 예상하지 못한 서버 오류 |

## 11. 민감정보 비노출 원칙

다음 값은 문서 예시, 로그, PR, 이슈 댓글에 실제값을 남기지 않는다.

- Authorization header 전체값
- access token
- refresh token
- Toss authorization code
- `x-toss-webhook-secret`
- `TOSS_*` secret 값
- request body / response body 원문 덤프
- multipart 파일 내용
- query string 전체
- presigned URL full query
- production credential

문제 추적은 `X-Request-Id` 응답 헤더와 서버 로그의 `requestId`를 기준으로 한다.
