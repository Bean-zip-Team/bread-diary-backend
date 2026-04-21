# 로컬 실행 및 Auth/User E2E 수동 검증 가이드

## 1. 문서 목적

이 문서는 팀원이 같은 방식으로 `bread-diary-backend`를 로컬에서 실행하고, 현재 구현된 auth/user 흐름을 수동 검증할 수 있도록 정리한 가이드다.

대상 독자는 백엔드 팀원, 다음 브랜치 작업자, 프론트 협업자다. 이 문서는 코드 기준으로 확인된 내용만 적고, 실제 비밀값은 포함하지 않는다.

## 2. 현재 실행 전제

| 항목 | 현재 기준 |
| --- | --- |
| Java | Java 21 |
| Gradle | Gradle wrapper 사용 가능. `gradle/wrapper/gradle-wrapper.jar` 존재 |
| Spring Boot | 4.0.5 |
| DB | MySQL 필요. 테스트 일부는 H2/Testcontainers 사용 |
| Docker | 로컬 MySQL 또는 Testcontainers 실행에 필요 |
| Swagger | springdoc 기본 경로 사용 |
| 설정 파일 | `src/main/resources/application.yml`, `src/main/resources/application-local.yml` 존재 |

테스트만 실행할 때는 H2와 Testcontainers 기반 테스트가 함께 사용된다. 애플리케이션을 `bootRun`으로 띄울 때는 MySQL 연결 설정이 필요하다.

## 3. 로컬 DB 실행

`application-local.yml`은 로컬 MySQL을 `localhost:3306`의 `breaddiary` DB로 바라본다. 아래 값은 예시이며, 실제 팀 공용 비밀번호나 개인 비밀번호는 문서에 남기지 않는다.

### Docker run 예시

```powershell
$env:MYSQL_DATABASE = "breaddiary"
$env:MYSQL_USER = "appuser"
$env:MYSQL_PASSWORD = "<LOCAL_DB_PASSWORD>"
$env:MYSQL_ROOT_PASSWORD = "<LOCAL_DB_ROOT_PASSWORD>"

docker run --name bread-diary-mysql `
  -e MYSQL_DATABASE=$env:MYSQL_DATABASE `
  -e MYSQL_USER=$env:MYSQL_USER `
  -e MYSQL_PASSWORD=$env:MYSQL_PASSWORD `
  -e MYSQL_ROOT_PASSWORD=$env:MYSQL_ROOT_PASSWORD `
  -p 3306:3306 `
  -d mysql:8.0
```

### docker-compose 예시

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: bread-diary-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_DATABASE: breaddiary
      MYSQL_USER: appuser
      MYSQL_PASSWORD: "<LOCAL_DB_PASSWORD>"
      MYSQL_ROOT_PASSWORD: "<LOCAL_DB_ROOT_PASSWORD>"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
```

`application-local.yml`의 DB 값과 Docker 값을 맞춰야 한다. 비밀번호에 특수문자가 있으면 YAML과 PowerShell에서 따옴표로 감싸는 편이 안전하다.

## 4. 필수 환경변수 / 설정값

실제 값은 로컬 `.env`, IDE Run Configuration, OS 환경변수, 배포 secret 등으로 관리한다. 문서나 PR 본문에 실제 값을 남기지 않는다.

| 이름 | 코드상 연결 설정 | 용도 | 로컬 필수 여부 | 예시 형식 |
| --- | --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `local` profile 활성화 | 권장 | `local` |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | DB URL override | 선택 | `jdbc:mysql://localhost:3306/breaddiary?...` |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | DB 사용자 override | 선택 | `appuser` |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | DB 비밀번호 override | 선택 | `<LOCAL_DB_PASSWORD>` |
| `JWT_SECRET` | `app.auth.jwt.secret` | JWT 서명/검증 secret | auth 검증 시 필수 | `<JWT_SECRET>` |
| `CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins` | 프론트 origin 허용 | 프론트 연동 시 필요 | `http://localhost:3000,http://localhost:5173` |
| `TOSS_BASE_URL` | `app.auth.toss.base-url` | Toss API base URL | 기본값 있음 | `https://apps-in-toss-api.toss.im` |
| `TOSS_WEBHOOK_SECRET` | `app.auth.toss.webhook-secret` | Toss webhook header 검증 | webhook 검증 시 필수 | `<TOSS_WEBHOOK_SECRET>` |
| `TOSS_UNLINK_ACCESS_TOKEN` | `app.auth.toss.unlink-access-token` | Toss 연결 끊기 API 호출 | 회원탈퇴/연결끊기 검증 시 필수 | `<TOSS_UNLINK_ACCESS_TOKEN>` |
| `TOSS_DECRYPTION_KEY` | `app.auth.toss.decryption-key` | Toss 사용자 정보 복호화 | 실 Toss 로그인 검증 시 필요 | `<BASE64_AES_KEY>` |
| `TOSS_AAD` | `app.auth.toss.aad` | Toss 사용자 정보 복호화 AAD | 실 Toss 로그인 검증 시 필요 | `<TOSS_AAD>` |
| `app.aws.s3.region` | `app.aws.s3.region` | S3 region | 이미지 업로드 시 필요 | `ap-northeast-2` |
| `app.aws.s3.bucket` | `app.aws.s3.bucket` | S3 bucket | 이미지 업로드 시 필요 | `<S3_BUCKET>` |
| `app.aws.s3.public-base-url` | `app.aws.s3.public-base-url` | S3 공개 URL | 이미지 업로드 시 필요 | `https://<bucket>.s3.<region>.amazonaws.com` |
| `cloud.aws.credentials.accessKey` | `cloud.aws.credentials.accessKey` | AWS access key | 이미지 업로드 시 필요 | `<AWS_ACCESS_KEY_ID>` |
| `cloud.aws.credentials.secretKey` | `cloud.aws.credentials.secretKey` | AWS secret key | 이미지 업로드 시 필요 | `<AWS_SECRET_ACCESS_KEY>` |

현재 코드에서 `JWT_ACCESS_TOKEN_EXPIRES_IN`, `JWT_REFRESH_TOKEN_EXPIRES_IN` 환경변수는 사용하지 않는다. Access Token TTL은 1일, Refresh Token TTL은 30일이며 `UserSessionService` 내부 상수로 계산한다.

현재 코드에서 `TOSS_CLIENT_ID`, `TOSS_CLIENT_SECRET`, `TOSS_REDIRECT_URI`는 백엔드 설정값으로 직접 주입받지 않는다. 이 값들은 Toss 콘솔 또는 프론트 인증 플로우 설정 영역으로 분리해서 관리한다.

`application.yml`에는 AWS credential 형태의 값이 직접 들어가 있다. 새 문서나 새 PR에는 이 값을 복사하지 말고, 추후 env/secret 분리를 권장한다.

## 5. 서버 실행 방법

### 테스트 실행

```powershell
.\gradlew.bat test
```

이 문서 작성 시점에는 위 명령이 성공했다.

### 로컬 profile로 서버 실행

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

PowerShell에서 환경변수를 같이 지정하려면 아래처럼 실행한다.

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:JWT_SECRET = "<JWT_SECRET>"
$env:TOSS_WEBHOOK_SECRET = "<TOSS_WEBHOOK_SECRET>"
$env:TOSS_UNLINK_ACCESS_TOKEN = "<TOSS_UNLINK_ACCESS_TOKEN>"
$env:TOSS_DECRYPTION_KEY = "<TOSS_DECRYPTION_KEY>"
$env:TOSS_AAD = "<TOSS_AAD>"

.\gradlew.bat bootRun
```

### Swagger / OpenAPI

서버가 `8080` 포트로 뜬 뒤 아래 경로를 확인한다.

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

이 문서 작성 시점에는 local profile `bootRun`이 기동되었고, `GET /bread-types`가 `200 OK`로 응답하는 것까지 확인했다.

## 6. 인증 구조 요약

현재 주요 auth/user 흐름은 JWT Bearer Token 기준이다. `AuthInterceptor`가 access token을 검증하고, 인증 성공 시 request attribute에 `authenticatedUserId`, `authenticatedSessionId`를 넣는다.

### Public endpoint

인증 없이 통과한다.

| Method | Endpoint | 비고 |
| --- | --- | --- |
| `OPTIONS` | 전체 | CORS preflight |
| `GET` | `/bread-types` | 빵 종류 공개 조회 |
| `POST` | `/auth/toss` | Toss 로그인 |
| `POST` | `/auth/refresh` | refresh token으로 재발급 |
| `POST` | `/auth/webhook/toss-unlink` | Toss webhook. 별도 `x-toss-webhook-secret` 필요 |
| `POST` | `/dev/auth/token` | 인터셉터 whitelist에는 있으나 현재 controller는 코드상 확인되지 않음 |
| `GET` | `/swagger-ui/**`, `/v3/api-docs/**` | Swagger/OpenAPI |

### Optional auth endpoint

Authorization header가 없으면 비로그인으로 처리하고, 있으면 access token을 검증한다.

| Method | Endpoint |
| --- | --- |
| `GET` | `/breads` |
| `GET` | `/breads/autocomplete` |
| `GET` | `/breads/catalog/{breadId}` |

### Protected endpoint

Bearer access token이 필요하다.

| Method | Endpoint | 비고 |
| --- | --- | --- |
| `GET` | `/users/me` | 현재 사용자 프로필/통계 |
| `DELETE` | `/users/me` | 회원탈퇴 |
| `POST` | `/auth/logout` | body 없음/`{}`이면 현재 access token의 session 종료, refreshToken body도 허용 |
| `GET` | `/breads/{recordId}` | 기록 상세 |
| `POST` | `/breads` | 기록 생성. multipart/S3 필요 |
| `POST` | `/breads/new` | 신규 빵 + 기록 생성. multipart/S3 필요 |
| `PUT` | `/breads/{recordId}` | 기록 수정. multipart |
| `DELETE` | `/breads/{recordId}` | 기록 삭제 |

현재 auth/user/bread controller 흐름에서 `X-USER-ID` 임시 인증 방식은 사용하지 않는다.

## 7. 수동 검증 체크리스트

아래 예시는 서버가 `http://localhost:8080`에서 실행 중이라는 전제다.

### 공개 API

| API | 목적 | 인증 | 로컬 단독 검증 | 막는 요소 |
| --- | --- | --- | --- | --- |
| `GET /bread-types` | 빵 종류 목록 조회 | 불필요 | 가능 | DB/서버 기동 필요 |
| `GET /breads` | 카탈로그 목록 조회 | 선택 | 조건부 가능 | DB seed 여부에 따라 결과 비어 있을 수 있음 |
| `GET /breads/autocomplete` | 카탈로그 자동완성 | 선택 | 조건부 가능 | DB seed 여부에 따라 결과 비어 있을 수 있음 |
| `GET /breads/catalog/{breadId}` | 카탈로그 빵 프로필 | 선택 | 조건부 가능 | 실제 `breadId` 데이터 필요 |

```powershell
curl.exe -i "http://localhost:8080/bread-types"
curl.exe -i "http://localhost:8080/breads"
curl.exe -i "http://localhost:8080/breads/autocomplete?q=croissant"
curl.exe -i "http://localhost:8080/breads/catalog/<BREAD_ID>"
```

기대 응답은 성공 시 공통 래퍼 형태다.

```json
{
  "success": true,
  "data": {}
}
```

### Auth/User API

| API | 목적 | 인증 | 로컬 단독 검증 | 막는 요소 |
| --- | --- | --- | --- | --- |
| `POST /auth/toss` | Toss authorization code 로그인/가입 | 불필요 | 실패 응답까지 가능 | 실제 Toss `authorizationCode`, `referrer`, Toss 외부 API 필요 |
| `POST /auth/refresh` | access/refresh token 재발급 | 불필요 | 실패 응답까지 가능 | 유효한 refresh token은 Toss 로그인 성공 후 확보 가능 |
| `POST /auth/logout` | 현재 session 로그아웃 | access token 필요 | 실패 응답까지 가능 | 유효한 access token/session 필요 |
| `POST /auth/webhook/toss-unlink` | Toss unlink/withdrawal webhook 처리 | secret header 필요 | 조건부 가능 | `TOSS_WEBHOOK_SECRET`, DB user 데이터 필요 |
| `GET /users/me` | 현재 사용자 조회 | access token 필요 | 실패 응답까지 가능 | 유효한 access token/session 필요 |

#### `POST /auth/toss`

```powershell
curl.exe -i -X POST "http://localhost:8080/auth/toss" `
  -H "Content-Type: application/json" `
  -d '{ "authorizationCode": "<TOSS_AUTHORIZATION_CODE>", "referrer": "<TOSS_REFERRER>" }'
```

실제 성공 검증은 Toss가 발급한 authorization code와 referrer가 필요하다. 백엔드 단독으로는 성공 로그인 완료까지 확인할 수 없다.

#### `POST /auth/refresh`

```powershell
curl.exe -i -X POST "http://localhost:8080/auth/refresh" `
  -H "Content-Type: application/json" `
  -d '{ "refreshToken": "<REFRESH_TOKEN>" }'
```

유효하지 않은 token이면 공통 에러 응답이 내려와야 한다.

```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "<message>"
  }
}
```

#### `POST /auth/logout`

최신 정책은 Bearer access token으로 현재 session을 종료하는 방식이다. body 없음과 `{}` body를 모두 허용하며, refresh token body도 호환 경로로 남아 있다.

| 요청 형태 | 상태 |
| --- | --- |
| Bearer access token + body 없음 | 허용 |
| Bearer access token + `{}` | 허용 |
| `{ "refreshToken": "..." }` | 허용 |
| `application/x-www-form-urlencoded` | 사용하지 않음 |
| unsupported content type | `415 INVALID_REQUEST` |

```powershell
curl.exe -i -X POST "http://localhost:8080/auth/logout" `
  -H "Authorization: Bearer <ACCESS_TOKEN>" `
  -H "Content-Type: application/json" `
  -d '{}'
```

성공 응답 예시:

```json
{
  "success": true,
  "data": {
    "loggedOut": true
  }
}
```

#### `POST /auth/webhook/toss-unlink`

```powershell
curl.exe -i -X POST "http://localhost:8080/auth/webhook/toss-unlink" `
  -H "Content-Type: application/json" `
  -H "x-toss-webhook-secret: <TOSS_WEBHOOK_SECRET>" `
  -d '{ "userKey": "<TOSS_USER_KEY>", "eventType": "UNLINK" }'
```

`eventType`는 `UNLINK`, `WITHDRAWAL_TERMS`, `WITHDRAWAL_TOSS` 중 하나다.

성공 응답 예시:

```json
{
  "success": true,
  "data": {
    "processed": true,
    "eventType": "UNLINK"
  }
}
```

#### `GET /users/me`

```powershell
curl.exe -i "http://localhost:8080/users/me" `
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

성공 응답에는 현재 사용자 정보와 통계가 포함된다.

```json
{
  "success": true,
  "data": {
    "id": "<USER_ID>",
    "nickname": "<NICKNAME>",
    "email": "<EMAIL>",
    "profileImageUrl": null,
    "bio": null,
    "stats": {
      "totalRecords": 0,
      "totalStickers": 0,
      "uniqueShops": 0,
      "avgRating": 0.0
    },
    "createdAt": "<DATE_TIME>"
  }
}
```

## 8. Toss 로그인 E2E 주의사항

현재 백엔드에는 `POST /auth/toss` 구현이 있다. 하지만 로컬에서 실제 성공 로그인까지 확인하려면 아래 외부값과 외부 설정이 필요하다.

| 필요 항목 | 이유 |
| --- | --- |
| Toss `authorizationCode` | `/auth/toss` 요청 필수값 |
| `referrer` | Toss token 발급 요청 필수값 |
| Toss developer console 설정 | 프론트 redirect URI와 앱 설정 필요 |
| 프론트 redirect URI | Toss 로그인 후 authorization code 수신 필요 |
| `TOSS_DECRYPTION_KEY`, `TOSS_AAD` | 암호화된 Toss 사용자 정보 복호화 필요 |
| 네트워크 접근 | Toss 외부 API 호출 필요 |

백엔드 단독으로 가능한 것은 요청 validation, 잘못된 code에 대한 실패 응답, refresh/logout/webhook의 실패 응답 확인 정도다.

현재 상태는 “코드 구현됨, 실 Toss 연동 성공 검증은 외부값 필요”로 본다.

## 9. 현재 로컬 검증 상태 요약

| 항목 | 구현 여부 | 테스트 여부 | 로컬 수동 검증 가능 여부 | 막는 요소 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `GET /bread-types` | 구현됨 | 일부 controller/service 테스트 존재 | 가능 | DB/서버 기동 | 문서 작성 시점 `200 OK` 확인 |
| `GET /breads` | 구현됨 | 일부 controller/service 테스트 존재 | 조건부 가능 | DB seed | 선택 인증 |
| `GET /breads/autocomplete` | 구현됨 | 일부 controller 테스트 존재 | 조건부 가능 | DB seed | 선택 인증 |
| `GET /breads/catalog/{breadId}` | 구현됨 | 일부 controller 테스트 존재 | 조건부 가능 | 실제 `breadId` | 선택 인증 |
| `POST /auth/toss` | 구현됨 | `AuthControllerTest`, `AuthServiceTest` | 조건부 가능 | 실제 Toss code/referrer | 실연동 미완료 |
| `POST /auth/refresh` | 구현됨 | 단위/동시성/MySQL 통합 테스트 | 조건부 가능 | 유효한 refresh token 필요 | Toss 로그인 성공 후 완전 확인 |
| `POST /auth/logout` | 구현됨 | controller/service 테스트 | 조건부 가능 | 유효한 access token/session 필요 | empty body, `{}`, refreshToken body 지원 |
| `POST /auth/webhook/toss-unlink` | 구현됨 | controller/service 테스트 | 조건부 가능 | webhook secret, user 데이터 | eventType 분기 |
| `GET /users/me` | 구현됨 | `UserControllerTest` | 조건부 가능 | 유효한 access token/session 필요 | JWT 기준 |
| Access logging | 구현됨 | `AccessLogFilterTest` | 가능 | 서버 기동 | requestId는 access log와 `X-Request-Id` 응답 헤더에 남음 |

## 10. 알려진 제한 사항 / TODO

- Dev token 발급용 실제 controller는 코드상 확인되지 않았다. 수동 auth/user 검증을 쉽게 하려면 별도 dev token 전략이나 seed 전략이 필요할 수 있다.
- Toss 실로그인 성공 검증은 프론트에서 실제 `authorizationCode`와 `referrer`를 받아와야 가능하다.
- S3가 필요한 multipart 기록 생성 API는 로컬 S3 mock 또는 테스트용 AWS 설정 없이는 완전한 수동 검증이 어렵다.
- `requestId`는 access log와 `X-Request-Id` 응답 헤더에 함께 남는다. 프론트에서 읽어야 하면 CORS exposed header 설정을 확인한다.
- `application.yml`의 민감값 형태 설정은 env/secret 기반으로 분리하는 것이 안전하다.
- `/v1` prefix는 현재 controller 코드 기준으로 붙어 있지 않다. Gateway/Nginx에서 처리할지, Spring에서 처리할지는 별도 결정이 필요하다.

## 11. 팀 공유용 1분 체크리스트

1. Docker MySQL을 띄운다.
2. `application-local.yml` 또는 환경변수의 DB 값을 Docker 값과 맞춘다.
3. `JWT_SECRET`, Toss webhook/복호화 관련 env를 설정한다.
4. `.\gradlew.bat test`로 테스트를 확인한다.
5. `.\gradlew.bat bootRun --args='--spring.profiles.active=local'`로 서버를 띄운다.
6. Swagger를 연다: `http://localhost:8080/swagger-ui.html`.
7. 공개 API를 먼저 확인한다: `GET /bread-types`.
8. 인증 실패 응답을 확인한다: token 없이 `GET /users/me`.
9. webhook secret을 넣고 `POST /auth/webhook/toss-unlink`를 확인한다.
10. 실제 Toss 로그인은 프론트가 받은 `authorizationCode`와 `referrer`가 준비된 뒤 확인한다.
