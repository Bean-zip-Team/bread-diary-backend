# Toss E2E 준비 체크리스트

## 1. 문서 목적

이 문서는 실제 Toss 로그인 E2E를 실행하기 전에 백엔드, 프론트, Toss console, 배포 환경에서 준비해야 할 값과 검증 순서를 정리한다.

이번 문서는 실로그인 성공 결과 기록이 아니다. 현재 백엔드에는 Toss 로그인 흐름이 구현되어 있지만, 실제 성공 검증은 프론트 redirect/referrer, Toss developer console 설정, 배포 URL, 외부 secret/env가 준비된 뒤에만 가능하다.

auth/user API field naming, logout 계약, 공통 에러 응답은 `docs/auth-user-api-contract.md`를 기준으로 한다. 이 문서는 Toss E2E 준비와 실행 순서에 집중한다.

상태 표기는 다음 기준을 사용한다.

| 상태 | 의미 |
| --- | --- |
| 이미 준비됨 | 현재 코드와 테스트에서 확인 가능한 범위 |
| 조건부 가능 | 로컬 DB, user/session/token, env 값을 직접 준비하면 확인 가능한 범위 |
| 실환경 필요 | 프론트, Toss console, 배포 URL, Toss 외부 API 응답이 필요해서 백엔드 단독으로 확인할 수 없는 범위 |
| 미확인 | 아직 실제 값으로 검증하지 않은 범위 |

## 2. 현재 백엔드에서 이미 준비된 것

| 항목 | 상태 | 메모 |
| --- | --- | --- |
| `POST /auth/toss` | 이미 준비됨, 실제 성공은 실환경 필요 | `authorizationCode`, `referrer`를 받아 Toss token API와 login-me API를 호출한 뒤 앱 access/refresh token을 발급하는 흐름이 있다. |
| `GET /users/me` | 이미 준비됨, 성공 호출은 조건부 가능 | Bearer access token 인증 후 현재 사용자 정보와 stats를 반환한다. |
| `POST /auth/refresh` | 이미 준비됨, 성공 호출은 조건부 가능 | refresh token을 검증하고 access/refresh token을 모두 재발급한다. refresh token rotation과 재사용 방지가 적용되어 있다. |
| `POST /auth/logout` | 이미 준비됨, 성공 호출은 조건부 가능 | Bearer access token 기반 현재 세션 로그아웃과 refreshToken body 기반 로그아웃을 모두 지원한다. |
| `POST /auth/webhook/toss-unlink` | 이미 준비됨, 성공 호출은 조건부 가능 | `x-toss-webhook-secret` 검증 후 `UNLINK`, `WITHDRAWAL_TERMS`, `WITHDRAWAL_TOSS` 이벤트를 처리한다. |
| `DELETE /users/me` | 이미 준비됨, 성공 호출은 조건부 가능 | 현재 사용자 탈퇴를 처리한다. Toss userKey가 있으면 Toss unlink 외부 호출이 필요하다. |
| AuthInterceptor | 이미 준비됨 | Bearer access token을 검증하고 `authenticatedUserId`, `authenticatedSessionId` request attribute를 저장한다. refresh token은 일반 API 인증에 허용하지 않는다. |
| requestId/access log | 이미 준비됨 | `X-Request-Id` 요청 헤더가 있으면 sanitize 후 재사용하고, 없으면 UUID를 생성한다. 응답 헤더와 access log의 requestId가 같다. |
| CORS | 이미 준비됨 | `CORS_ALLOWED_ORIGINS` 또는 `app.cors.allowed-origins`로 origin을 설정한다. `Location`, `X-Request-Id`가 exposed header에 포함되어 있다. |
| 민감정보 비노출 원칙 | 이미 준비됨 | access log는 query string, Authorization header, body, webhook secret, token 값을 남기지 않는 방향으로 테스트되어 있다. |

## 3. 실제 Toss E2E 전에 필요한 값

실제 값은 문서, PR 본문, 로그에 남기지 않는다. 아래 항목은 모두 환경 변수, 배포 secret, Toss console, 프론트 설정에서 관리한다.

| 값 | 필요 위치 | 설명 |
| --- | --- | --- |
| backend base URL | 프론트, Toss E2E 실행자 | 프론트가 호출할 백엔드 주소다. 예: 로컬 `http://localhost:8080`, 배포 `<BACKEND_BASE_URL>`. |
| frontend redirect URL | 프론트, Toss console | Toss 로그인 완료 후 authorization code를 받을 프론트 redirect 주소다. |
| `authorizationCode` | 프론트 -> 백엔드 | 프론트가 Toss 로그인 후 획득해 `POST /auth/toss`에 전달한다. 일회성 값으로 보고 재사용하지 않는다. |
| `referrer` | 프론트 -> 백엔드 | 프론트가 Toss 로그인 결과와 함께 전달한다. 백엔드는 Toss generate-token 요청에 사용한다. |
| Toss developer console redirect 설정 | Toss console | 프론트 redirect URL과 실제 로그인 진입 환경이 Toss console 설정과 일치해야 한다. |
| `TOSS_DECRYPTION_KEY` | 백엔드 env/secret | Toss 사용자 정보 복호화에 필요하다. |
| `TOSS_AAD` | 백엔드 env/secret | Toss 사용자 정보 복호화 AAD 값이다. |
| `TOSS_WEBHOOK_SECRET` | 백엔드 env/secret, webhook 호출자 | `POST /auth/webhook/toss-unlink`의 `x-toss-webhook-secret` 검증에 필요하다. |
| `TOSS_UNLINK_ACCESS_TOKEN` | 백엔드 env/secret | Toss userKey가 있는 사용자의 탈퇴/unlink 외부 호출에 필요하다. |
| `JWT_SECRET` | 백엔드 env/secret | 앱 access/refresh token 서명과 검증에 필요하다. |
| DB 연결 정보 | 백엔드 env/profile | user, session, refresh token rotation 상태 저장에 필요하다. 로컬은 `local` profile과 MySQL 준비가 필요하다. |
| `CORS_ALLOWED_ORIGINS` | 백엔드 env/config | 실제 프론트 origin을 포함해야 한다. 프론트에서 `X-Request-Id`를 읽어야 하면 exposed header 설정도 확인한다. |

## 4. 로컬 실행 전 점검

로컬에서 백엔드를 띄워 실패 응답, 인증 실패, refresh/logout 조건부 흐름을 확인하려면 다음 값이 필요하다.

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:JWT_SECRET = "<JWT_SECRET>"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000,http://localhost:5173"
$env:TOSS_WEBHOOK_SECRET = "<TOSS_WEBHOOK_SECRET>"
$env:TOSS_UNLINK_ACCESS_TOKEN = "<TOSS_UNLINK_ACCESS_TOKEN>"
$env:TOSS_DECRYPTION_KEY = "<TOSS_DECRYPTION_KEY>"
$env:TOSS_AAD = "<TOSS_AAD>"
```

```powershell
.\gradlew.bat bootRun
```

Swagger/OpenAPI 확인 경로:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

## 5. 실제 Toss 로그인 E2E 검증 순서

아래 순서는 프론트, Toss console, 백엔드 배포/env가 모두 준비된 뒤 실행한다.

1. 프론트에서 Toss 로그인 진입
   - 프론트가 실제 Toss 로그인 플로우를 시작한다.
   - redirect URL이 Toss console 설정과 일치하는지 먼저 확인한다.

2. 프론트에서 `authorizationCode`, `referrer` 획득
   - 획득한 값은 즉시 백엔드로 전달한다.
   - 같은 `authorizationCode`를 재사용하지 않는다.

3. `POST /auth/toss` 호출

```http
POST <BACKEND_BASE_URL>/auth/toss
Content-Type: application/json
X-Request-Id: <OPTIONAL_REQUEST_ID>
```

```json
{
  "authorizationCode": "<TOSS_AUTHORIZATION_CODE>",
  "referrer": "<TOSS_REFERRER>"
}
```

기대 결과:

```json
{
  "success": true,
  "data": {
    "userId": "<USER_ID>",
    "accessToken": "<ACCESS_TOKEN>",
    "refreshToken": "<REFRESH_TOKEN>",
    "tokenType": "Bearer",
    "accessTokenExpiresAt": "<DATE_TIME>",
    "refreshTokenExpiresAt": "<DATE_TIME>",
    "newUser": true
  }
}
```

4. 응답 헤더 확인
   - `X-Request-Id`가 내려오는지 확인한다.
   - 프론트 브라우저에서 해당 헤더를 읽을 수 없으면 CORS exposed header 설정을 확인한다.

5. `GET /users/me` 확인

```http
GET <BACKEND_BASE_URL>/users/me
Authorization: Bearer <ACCESS_TOKEN>
```

확인 항목:

- `success=true`
- `data.id`
- `data.nickname`
- `data.email`
- `data.stats.totalRecords`
- `data.stats.totalStickers`
- `data.stats.uniqueShops`
- `data.stats.avgRating`

6. `POST /auth/refresh` 확인

```http
POST <BACKEND_BASE_URL>/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "<REFRESH_TOKEN>"
}
```

확인 항목:

- 새 `accessToken` 발급
- 새 `refreshToken` 발급
- `newUser=false`

7. 이전 refresh token 재사용 실패 확인
   - 6단계에서 사용한 이전 refresh token으로 다시 `/auth/refresh`를 호출한다.
   - 기대 결과는 `409 CONFLICT` 계열이다.

8. `POST /auth/logout` 확인

권장 호출:

```http
POST <BACKEND_BASE_URL>/auth/logout
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

body 없음 또는 `{}`를 사용할 수 있다.

```json
{}
```

기대 결과:

```json
{
  "success": true,
  "data": {
    "loggedOut": true
  }
}
```

9. logout 이후 refresh 실패 확인
   - logout으로 세션이 종료된 뒤 해당 세션의 refresh token으로 `/auth/refresh`를 호출한다.
   - 기대 결과는 `401 UNAUTHORIZED` 계열이다.

10. `DELETE /users/me` 확인
    - 새 로그인 또는 아직 유효한 access token이 필요하다.
    - Toss userKey가 있는 사용자는 `TOSS_UNLINK_ACCESS_TOKEN`과 Toss 외부 API 접근이 필요하다.

```http
DELETE <BACKEND_BASE_URL>/users/me
Authorization: Bearer <ACCESS_TOKEN>
```

11. 필요 시 webhook/unlink 확인

```http
POST <BACKEND_BASE_URL>/auth/webhook/toss-unlink
Content-Type: application/json
x-toss-webhook-secret: <TOSS_WEBHOOK_SECRET>
```

```json
{
  "userKey": "<TOSS_USER_KEY>",
  "eventType": "UNLINK"
}
```

`eventType`은 현재 `UNLINK`, `WITHDRAWAL_TERMS`, `WITHDRAWAL_TOSS`를 사용한다.

## 6. logout 최신 계약

상세 계약 기준은 `docs/auth-user-api-contract.md`다. Toss E2E에서 확인해야 할 logout 요약은 아래와 같다.

`POST /auth/logout`은 보호 API이므로 Bearer access token이 필요하다.

| 요청 형태 | 상태 | 설명 |
| --- | --- | --- |
| Bearer access token + body 없음 | 허용 | 현재 인증된 access token의 sessionId로 로그아웃한다. |
| Bearer access token + `{}` | 허용 | body 없음과 동일하게 현재 인증 세션을 로그아웃한다. |
| `{ "refreshToken": "..." }` | 허용 | refresh token을 검증한 뒤 해당 세션을 로그아웃한다. 호환 경로로 유지한다. |
| `application/x-www-form-urlencoded` | 사용하지 않음 | JSON API 계약이 아니므로 프론트에서 사용하지 않는다. |
| unsupported content type | `415 INVALID_REQUEST` | 공통 에러 응답으로 내려간다. |

프론트 권장 호출은 다음 중 하나다.

```http
POST /auth/logout
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

또는

```json
{}
```

## 7. 현재 가능한 것과 아직 불가능한 것

### 로컬만으로 가능한 검증

- 서버 bootRun과 Swagger 접근 확인
- `GET /bread-types` 같은 public API 200 확인
- token 없음/잘못된 token의 `401` 확인
- `/auth/toss` 요청 body validation 실패 확인
- `/auth/webhook/toss-unlink`의 missing header `400`, wrong secret `401` 확인
- access log의 `requestId`, `method`, `path`, `status`, `durationMs` 형태 확인
- `X-Request-Id` 응답 헤더 확인

### DB에 테스트용 user/session/token을 준비하면 가능한 검증

- `GET /users/me` 성공
- `POST /auth/refresh` 성공과 refresh token rotation
- 이전 refresh token 재사용 `409`
- `POST /auth/logout` 성공
- logout 이후 refresh `401`
- Toss userKey가 없는 로컬 사용자 `DELETE /users/me` 성공
- webhook secret이 맞고 unknown userKey인 경우 `processed=true` 응답

### 프론트 + Toss console + 배포 URL이 있어야 가능한 검증

- 실제 Toss 로그인 성공
- 실제 Toss `authorizationCode`와 `referrer`로 `/auth/toss` 성공
- Toss login-me 응답 복호화 성공
- 프론트 브라우저에서 CORS, `X-Request-Id`, auth/user camelCase 응답 확인
- 실제 로그인 token pair로 `/users/me -> /auth/refresh -> /auth/logout` 전체 플로우 확인

### 현재 외부값 부족으로 아직 못 한 검증

- 실제 Toss authorization code 기반 로그인 성공
- 실제 Toss console redirect 설정 일치 여부
- 실제 Toss 암호화 사용자 정보 복호화
- Toss userKey가 있는 사용자의 실제 unlink API 성공
- 실배포 origin에서의 CORS 동작

## 8. 실패 시 체크 포인트

| 증상/status | 확인할 것 |
| --- | --- |
| `400 BAD_REQUEST` | JSON body 형식, `authorizationCode`, `referrer`, webhook body, 필수 값 누락을 확인한다. |
| `401 UNAUTHORIZED` | Bearer access token, refresh token, token type, 만료 여부, webhook secret, `JWT_SECRET`, Toss invalid grant 여부를 확인한다. |
| `409 CONFLICT` | refresh token이 이미 rotation된 이전 값인지 확인한다. 재사용 실패는 기대 동작이다. |
| `415 INVALID_REQUEST` | `Content-Type`이 `application/json`인지 확인한다. `application/x-www-form-urlencoded`는 사용하지 않는다. |
| `404 NOT_FOUND` 또는 프론트 redirect 실패 | frontend redirect URL, Toss console redirect 설정, 프론트 라우팅, backend base URL을 확인한다. |
| CORS 에러 | `CORS_ALLOWED_ORIGINS`에 실제 프론트 origin이 포함되어 있는지 확인한다. 프론트에서 `X-Request-Id`를 읽어야 하면 exposed header도 확인한다. |
| Toss 연동 실패 | Toss console redirect 설정, `authorizationCode` 재사용/만료, `referrer`, `TOSS_DECRYPTION_KEY`, `TOSS_AAD`, `TOSS_UNLINK_ACCESS_TOKEN`, Toss API 접근 가능 여부를 확인한다. |
| `500 INTERNAL_ERROR` | `JWT_SECRET`, DB 연결, 필수 env 누락, 서버 로그의 `requestId`를 확인한다. |
| `502 TOSS_SERVER_ERROR` | Toss 외부 API 장애 또는 비정상 응답 가능성을 확인한다. 서버 로그의 `TOSS action`, `status`, `errorCode`, `requestId`를 확인한다. |

## 9. 로그와 민감정보 주의

다음 값은 문서, PR, 로그, 이슈 댓글에 남기지 않는다.

- Authorization header
- access token
- refresh token
- Toss authorization code
- `x-toss-webhook-secret`
- `TOSS_*` secret 값
- request body / response body 원문
- presigned URL full query
- production credential

문제 추적 시에는 `X-Request-Id`와 서버 access log의 `requestId`를 기준으로 맞춰 본다.

## 10. 다음 작업자 메모

- 이 문서는 체크리스트이며, 실제 Toss 성공 검증 결과가 아니다.
- 실제 E2E 브랜치는 프론트에서 `authorizationCode`와 `referrer`를 전달할 수 있고 Toss console redirect/env가 준비된 뒤 시작한다.
- auth/user 응답은 현재 프론트 연동 기준 camelCase를 사용한다.
- bread, breadrecord, s3, upload 도메인 로직은 이 문서 작업 범위가 아니다.
- 운영 secret 외부화와 실제 secret rotation은 별도 운영/보안 브랜치에서 다룬다.
