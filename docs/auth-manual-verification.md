# Auth/User 수동 검증 기록

## 1. 검증 목적

이 문서는 `test/66-auth-manual-verification` 브랜치에서 auth/user/token 흐름이 로컬에서 어디까지 실제로 확인되는지 정리한 기록이다.

범위는 `auth`, `user`, `token`, 인증 인터셉터, access log 확인까지다. `bread`, `breadrecord`, `s3`, `upload` 도메인 로직은 수정하지 않았고, 공개/선택 인증 endpoint 확인을 위해 호출만 했다.

## 2. 최신 상태 메모

이 문서는 `test/66-auth-manual-verification` 브랜치에서 수행한 당시 수동 검증 기록이다. 이후 `fix/72-auth-logout-request-contract`에서 `/auth/logout` 계약이 보완되었다.

현재 최신 계약은 다음과 같다.

| 요청 형태 | 최신 상태 |
| --- | --- |
| Bearer access token + body 없음 | 허용 |
| Bearer access token + `{}` | 허용 |
| `{ "refreshToken": "..." }` | 허용 |
| `application/x-www-form-urlencoded` | 사용하지 않음, `415 INVALID_REQUEST` |

실제 Toss E2E 준비 체크리스트는 `docs/toss-e2e-checklist.md`를 기준으로 본다.

## 3. develop 반영 상태

작업 시작 시 아래 순서로 최신 상태를 확인했다.

```powershell
git fetch origin
git merge --no-edit origin/develop
```

결과는 `Already up to date.`였고 충돌은 없었다.

## 3. 참고한 파일 요약

| 파일 | 확인한 내용 |
| --- | --- |
| `docs/codex-prompts-user/mvp-backend-readiness-plan.md` | auth/user 수동 검증, Toss E2E, webhook, logout 확인이 남은 작업으로 정리되어 있음 |
| `docs/local-env-and-e2e-guide.md` | local profile, Docker MySQL, JWT/Toss/env, 수동 호출 절차 기준 확인 |
| `src/main/resources/application.yml` | JWT 기본 placeholder, Toss placeholder, CORS, 운영 DB/S3 설정 구조 확인 |
| `src/main/resources/application-local.yml` | 로컬 MySQL `localhost:3306/breaddiary`, `appuser`, MySQL driver, `ddl-auto=update` 확인 |
| `AuthController` | `/auth/toss`, `/auth/refresh`, `/auth/logout`, `/auth/webhook/toss-unlink` 매핑 확인 |
| `AuthService` | Toss 로그인, refresh rotation, logout, refresh token hash 검증 흐름 확인 |
| `JwtTokenProvider` | access/refresh JWT 생성/검증, `sub`, `sid`, `type`, `jti`, `iat`, `exp` claim 확인 |
| `TossAuthClient` | Toss 외부 API 호출과 unlink access token 필요 조건 확인 |
| `TossUserInfoDecryptor` | Toss 사용자 정보 복호화에 `TOSS_DECRYPTION_KEY`, `TOSS_AAD` 필요 확인 |
| `UserController` | `GET /users/me`, `DELETE /users/me`가 access token request attribute 기준으로 동작함 확인 |
| `UserService` | 활성 사용자 조회와 stats 조립 구조 확인 |
| `UserWithdrawalService` | 앱 탈퇴, Toss webhook, session 삭제/hard delete 흐름 확인 |
| `AuthInterceptor` | public / optional auth / protected endpoint 분기와 access token 검증 확인 |
| `AuthRequestAttributes` | `authenticatedUserId`, `authenticatedSessionId` request attribute 확인 |
| `AccessLogFilter` | method/path/status/duration/userId/sessionId/requestId/clientIp 로그 확인 |
| auth/user 테스트 | controller, service, refresh concurrency, MySQL Testcontainers, withdrawal 테스트 범위 확인 |

## 4. 사전 준비물

로컬 수동 검증에는 아래가 필요하다.

| 항목 | 필요 여부 | 비고 |
| --- | --- | --- |
| Java 21 | 필수 | 프로젝트 toolchain 기준 |
| Gradle wrapper | 필수 | `gradle-wrapper.jar` 존재, `.\gradlew.bat` 사용 가능 |
| Docker MySQL | 필수 | local profile로 실제 서버를 띄우려면 MySQL 필요 |
| JWT secret | auth 성공 검증 필수 | `JWT_SECRET=<JWT_SECRET>` |
| Toss 설정값 | Toss 실로그인 조건부 필수 | `authorizationCode`, `referrer`, Toss 외부 API 접근, 복호화 설정 필요 |
| Webhook secret | webhook 성공 검증 필수 | `TOSS_WEBHOOK_SECRET=<TOSS_WEBHOOK_SECRET>` |
| Toss unlink access token | Toss userKey가 있는 앱 탈퇴 검증 필수 | `TOSS_UNLINK_ACCESS_TOKEN=<TOSS_UNLINK_ACCESS_TOKEN>` |

## 5. 로컬 서버 실행 방법

Docker MySQL 예시:

```powershell
docker run --name bread-diary-mysql `
  -e MYSQL_DATABASE=breaddiary `
  -e MYSQL_USER=appuser `
  -e MYSQL_PASSWORD="<LOCAL_DB_PASSWORD>" `
  -e MYSQL_ROOT_PASSWORD="<LOCAL_DB_ROOT_PASSWORD>" `
  -p 3306:3306 `
  -d mysql:8.0
```

서버 실행 예시:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:JWT_SECRET = "<JWT_SECRET>"
$env:TOSS_WEBHOOK_SECRET = "<TOSS_WEBHOOK_SECRET>"
$env:TOSS_UNLINK_ACCESS_TOKEN = "<TOSS_UNLINK_ACCESS_TOKEN>"
$env:TOSS_DECRYPTION_KEY = "<TOSS_DECRYPTION_KEY>"
$env:TOSS_AAD = "<TOSS_AAD>"

.\gradlew.bat bootRun
```

Swagger 확인:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

## 6. 인증 경계

| 구분 | Endpoint | 확인 결과 |
| --- | --- | --- |
| Public | `GET /bread-types` | 인증 없이 200 |
| Public | `POST /auth/toss` | 인증 없이 진입 가능, 실제 성공은 Toss 외부값 필요 |
| Public | `POST /auth/refresh` | 인증 없이 진입 가능, refresh token body 필요 |
| Public | `POST /auth/webhook/toss-unlink` | 인증 없이 진입 가능, `x-toss-webhook-secret` 별도 필요 |
| Optional auth | `GET /breads` | 토큰 없으면 익명 200, 잘못된 Bearer 있으면 401 |
| Optional auth | `GET /breads/autocomplete` | 토큰 없으면 익명 200 |
| Optional auth | `GET /breads/catalog/{breadId}` | 토큰 없으면 익명 진입 가능, 실제 breadId 필요 |
| Protected | `GET /users/me` | Bearer access token 필수 |
| Protected | `DELETE /users/me` | Bearer access token 필수 |
| Protected | `POST /auth/logout` | Bearer access token 필수 |

현재 auth/user 흐름은 `X-USER-ID`가 아니라 JWT Bearer access token 기준으로 동작한다.

## 7. 수동 검증 결과

실제 검증 환경:

| 항목 | 값 |
| --- | --- |
| 날짜 | 2026-04-21 |
| profile | `local` |
| DB | Docker MySQL 8.0, `localhost:3306/breaddiary` |
| 서버 | `.\gradlew.bat bootRun`, port `8080` |
| JWT secret | 로컬 검증용 placeholder 사용 |
| Webhook secret | 로컬 검증용 placeholder 사용 |

수동 검증을 위해 로컬 DB에 검증용 user/session row를 직접 생성하고, 같은 JWT secret으로 access/refresh token을 생성했다. 실제 토큰 값과 secret은 문서에 남기지 않는다.

| 항목 | 실제 결과 | 비고 |
| --- | --- | --- |
| `GET /bread-types` 인증 없음 | `200` | `success=true`, bread type 목록 반환 |
| `GET /breads` 인증 없음 | `200` | 로컬 seed 없음 기준 `items=[]` |
| `GET /breads/autocomplete?q=manual` 인증 없음 | `200` | 로컬 seed 없음 기준 `items=[]` |
| `GET /breads` 잘못된 Bearer | `401` | optional auth라도 잘못된 토큰이 있으면 실패 |
| `GET /users/me` 토큰 없음 | `401` | `Bearer Access Token이 필요합니다.` |
| `GET /users/me` 잘못된 토큰 | `401` | 토큰 형식 오류 |
| `GET /users/me` 정상 access token | `200` | 사용자 정보와 stats 반환 |
| `POST /auth/refresh` 잘못된 refresh token | `401` | 토큰 형식 오류 |
| `POST /auth/refresh` 정상 refresh token | `200` | access/refresh 모두 재발급 |
| `POST /auth/refresh` 이전 refresh token 재사용 | `409` | `이미 회전된 리프레시 토큰입니다.` |
| `POST /auth/logout` 토큰 없음 | `401` | 보호 API로 차단 |
| `POST /auth/logout` `{}` body + 정상 access token | `400` | 당시 결과. 최신 구현에서는 `200`, `loggedOut=true` |
| `POST /auth/logout` body 없음 + `Content-Type: application/json` + 정상 access token | `200` | `loggedOut=true` |
| 로그아웃 후 `POST /auth/refresh` | `401` | 세션 없음으로 실패 |
| `POST /auth/toss` blank body 값 | `400` | validation 실패 |
| `POST /auth/webhook/toss-unlink` secret 없음 | `400` | 필수 header 없음 |
| `POST /auth/webhook/toss-unlink` 잘못된 secret | `401` | webhook 인증 실패 |
| `POST /auth/webhook/toss-unlink` 정상 secret + unknown userKey | `200` | `processed=true`, `eventType=UNLINK` |
| `DELETE /users/me` tossUserKey 없는 로컬 사용자 | `200` | `withdrawn=true` |
| 탈퇴 후 `GET /users/me` | `401` | 세션 삭제로 차단 |
| 탈퇴 후 `POST /auth/refresh` | `401` | 세션 삭제로 차단 |

`GET /users/me` 정상 응답에서 확인한 stats:

```json
{
  "success": true,
  "data": {
    "stats": {
      "totalRecords": 0,
      "totalStickers": 0,
      "uniqueShops": 0,
      "avgRating": 0.0
    }
  }
}
```

## 8. 요청 예시

PowerShell에서는 JSON body가 있는 요청은 `Invoke-WebRequest`를 쓰는 편이 안전하다. `curl.exe -d`는 따옴표 escaping을 잘못하면 서버가 JSON parse error를 낸다.

### users/me 실패 확인

```powershell
curl.exe -i "http://localhost:8080/users/me"
```

기대 결과:

```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Bearer Access Token이 필요합니다."
  }
}
```

### users/me 성공 확인

```powershell
curl.exe -i "http://localhost:8080/users/me" `
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### refresh

```powershell
$body = @{ refreshToken = "<REFRESH_TOKEN>" } | ConvertTo-Json -Compress
Invoke-WebRequest `
  -Method Post `
  -Uri "http://localhost:8080/auth/refresh" `
  -ContentType "application/json" `
  -Body $body
```

### logout

최신 구현에서는 access token 기반 로그아웃을 할 때 body 없음과 `{}` body를 모두 허용한다. `Content-Type`은 `application/json`을 사용한다.

```powershell
curl.exe -i -X POST "http://localhost:8080/auth/logout" `
  -H "Authorization: Bearer <ACCESS_TOKEN>" `
  -H "Content-Type: application/json"
```

### webhook

```powershell
$body = @{
  userKey = "<TOSS_USER_KEY>"
  eventType = "UNLINK"
} | ConvertTo-Json -Compress

Invoke-WebRequest `
  -Method Post `
  -Uri "http://localhost:8080/auth/webhook/toss-unlink" `
  -Headers @{ "x-toss-webhook-secret" = "<TOSS_WEBHOOK_SECRET>" } `
  -ContentType "application/json" `
  -Body $body
```

## 9. Toss 로그인 검증 범위

`POST /auth/toss`의 코드 경로는 구현되어 있지만, 로컬 단독으로 실로그인 성공까지 검증할 수는 없다.

성공 검증에 필요한 값:

| 값 | 필요 이유 |
| --- | --- |
| Toss `authorizationCode` | `/auth/toss` 요청 필수값 |
| Toss `referrer` | Toss generate-token API 요청 필수값 |
| Toss developer console 설정 | 프론트 redirect URI와 Toss 앱 설정이 맞아야 code 발급 가능 |
| `TOSS_DECRYPTION_KEY` | Toss 사용자 정보 복호화 필요 |
| `TOSS_AAD` | Toss 사용자 정보 복호화 AAD 필요 |
| Toss 외부 API 접근 | `generate-token`, `login-me` 호출 필요 |

이번 브랜치에서 확인한 것은 `/auth/toss` validation 실패 응답까지다. 실제 성공 흐름은 프론트가 Toss에서 받은 `authorizationCode`와 `referrer`를 백엔드로 넘긴 뒤 확인해야 한다.

## 10. 현재 가능한 것 / 조건부 가능한 것 / 불가능한 것

| 구분 | 항목 | 상태 |
| --- | --- | --- |
| 바로 가능 | `GET /bread-types` | 로컬 DB와 서버만 있으면 200 확인 가능 |
| 바로 가능 | `GET /breads`, `GET /breads/autocomplete` 익명 호출 | seed가 없으면 빈 목록으로 200 |
| 바로 가능 | 인증 실패 응답 | 토큰 없음/잘못된 토큰 공통 에러 확인 가능 |
| 조건부 가능 | `/users/me` 성공 | 로컬 DB에 user/session과 유효 access token 필요 |
| 조건부 가능 | `/auth/refresh` 성공/rotation | 로컬 DB에 user/session과 유효 refresh token/hash/currentJti 필요 |
| 조건부 가능 | `/auth/logout` 성공 | 유효 access token 필요, body 없음 또는 `{}` 허용 |
| 조건부 가능 | webhook 성공 | `TOSS_WEBHOOK_SECRET` 설정 필요 |
| 조건부 가능 | `DELETE /users/me` | tossUserKey가 없으면 로컬 단독 가능, tossUserKey가 있으면 Toss unlink 외부 API 필요 |
| 현재 불가 | `/auth/toss` 실로그인 성공 | 실제 Toss authorizationCode/referrer와 Toss 설정 필요 |
| 현재 불가 | Toss userKey가 있는 앱 탈퇴 실연동 | `TOSS_UNLINK_ACCESS_TOKEN`과 Toss 외부 API 성공 필요 |

## 11. 실행한 명령어

```powershell
git fetch origin
git merge --no-edit origin/develop
docker ps
Test-NetConnection -ComputerName localhost -Port 3306
.\gradlew.bat test --tests "*AuthControllerTest" --tests "*UserControllerTest" --tests "*AuthInterceptorTest"
.\gradlew.bat bootRun
Test-NetConnection -ComputerName localhost -Port 8080
.\gradlew.bat test
```

HTTP 수동 호출은 `curl.exe`와 `Invoke-WebRequest`를 함께 사용했다. JSON body가 필요한 요청은 PowerShell escaping 이슈를 피하려고 `Invoke-WebRequest`를 우선 사용했다.

## 12. 테스트 결과

| 명령어 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AuthControllerTest" --tests "*UserControllerTest" --tests "*AuthInterceptorTest"` | 성공 |
| `.\gradlew.bat bootRun` with local profile | 성공, Tomcat 8080 기동 확인 |
| `.\gradlew.bat test` | 성공 |

## 13. 남은 TODO

- 해결됨: 최신 구현에서 `/auth/logout`은 body 없음, `{}`, `{ "refreshToken": "..." }`를 허용한다.
- 해결됨: `application/x-www-form-urlencoded` 같은 unsupported content type은 `415 INVALID_REQUEST`로 응답한다. 프론트는 `application/json`을 사용한다.
- `/dev/auth/token`은 `AuthInterceptor` whitelist에는 있지만 controller 구현은 확인되지 않았다. 수동 검증 편의가 필요하면 별도 결정이 필요하다.
- Toss 실로그인은 프론트/Toss 콘솔/authorizationCode/referrer 준비 후 별도 E2E로 확인한다.
- Toss userKey가 있는 앱 탈퇴는 실제 `TOSS_UNLINK_ACCESS_TOKEN`과 Toss API 성공이 있어야 완전 검증 가능하다.
- `application.yml`의 실제 키처럼 보이는 AWS credential은 env/secret 분리 대상이다.

## 14. 다음 브랜치 작업자가 먼저 읽을 파일

1. `docs/auth-manual-verification.md`
2. `docs/local-env-and-e2e-guide.md`
3. `src/main/java/com/bean/breaddiary/domain/auth/controller/AuthController.java`
4. `src/main/java/com/bean/breaddiary/domain/auth/service/AuthService.java`
5. `src/main/java/com/bean/breaddiary/domain/user/controller/UserController.java`
6. `src/main/java/com/bean/breaddiary/domain/user/service/UserWithdrawalService.java`
7. `src/main/java/com/bean/breaddiary/global/interceptor/AuthInterceptor.java`
8. `src/main/java/com/bean/breaddiary/global/filter/AccessLogFilter.java`
