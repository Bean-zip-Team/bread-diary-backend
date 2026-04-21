# MVP Backend Readiness Plan

> Archive note: 이 문서는 MVP 백엔드 준비 작업을 나누기 위한 과거 계획 문서다. 현재 완료/미완료 상태를 판단하는 기준 문서가 아니며, 최신 auth/user API 계약은 `docs/auth-user-api-contract.md`, 로컬 실행 가이드는 `docs/local-env-and-e2e-guide.md`, Toss E2E 준비는 `docs/toss-e2e-checklist.md`를 따른다.

## Goal

프론트가 붙기 전에 백엔드에서 미리 닫을 수 있는 항목을 구현, 테스트, 문서화한다.

이 계획은 `bread`, `breadrecord`, `upload`, `s3` 도메인 로직을 직접 수정하지 않는 것을 기본 원칙으로 한다. 다만 `/v1` prefix, rate limiting, access logging처럼 전체 API에 영향을 주는 공통 작업은 팀 결정 후 별도 브랜치에서 진행한다.

## Historical Baseline

- Historical branch snapshot: `feat/58-mvp-frontend-integration`
- Latest pushed commits:
  - `df1b1d7 feat: prepare auth frontend integration`
  - `d4795a5 chore: restore gradle wrapper jar`
- Verification:
  - `.\gradlew.bat test` 성공
- Already completed in current branch:
  - 최신 `develop` 반영
  - CORS 설정
  - 글로벌 에러 응답 `{ success: false, error: { code, message } }`
  - `/auth/logout` empty body + Bearer access token 지원
  - 기존 `/auth/logout` refreshToken body 호환 유지
  - Gradle wrapper jar 복구
  - auth/user/global common 관련 테스트 보강

## Source Documents To Read First

다음 작업자는 구현 전에 아래 문서를 먼저 읽고, 현재 코드가 문서와 달라졌는지 확인한다.

1. `AGENTS.md`
   - 프로젝트 구조, 응답 포맷, snake_case 주의사항, 작업 종료 보고 규칙.
2. `docs/user-part-spec.md`
   - User/Auth 도메인 목적, sliding session, 웹훅/탈퇴 정책.
3. `docs/mvp-frontend-integration-handoff.md`
   - 현재 auth/user camelCase 계약, logout 정책, Toss E2E 체크리스트.
4. `docs/codex-prompts-user/README.md`
   - user/auth 작업 순서와 인증 정책 요약.
5. `docs/codex-prompts-user/auth-login.txt`
   - Toss login, JWT claim, UserSession 생성 정책.
6. `docs/codex-prompts-user/auth-refresh-logout.txt`
   - refresh rotation, sliding session, logout 정책.
7. `docs/codex-prompts-user/auth-interceptor.txt`
   - Spring Security 없이 Bearer access token 인증하는 정책.
8. `docs/codex-prompts-user/user-profile-api.txt`
   - `/users/me` 구조와 user profile 구현 원칙.
9. `docs/codex-prompts-user/user-withdrawal.txt`
   - Toss unlink/withdrawal webhook, hard delete 정책.
10. `docs/codex-prompts-user/auth-concurrency-test.txt`
   - refresh token 동시성 검증 요구사항.

## Current API Contract Snapshot

### `POST /auth/toss`

Request:

```json
{
  "authorizationCode": "string",
  "referrer": "SANDBOX"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "userId": "uuid",
    "accessToken": "string",
    "refreshToken": "string",
    "tokenType": "Bearer",
    "accessTokenExpiresAt": "2026-04-21T10:00:00",
    "refreshTokenExpiresAt": "2026-05-21T10:00:00",
    "newUser": true
  }
}
```

### `POST /auth/refresh`

Request:

```json
{
  "refreshToken": "string"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "userId": "uuid",
    "accessToken": "string",
    "refreshToken": "string",
    "tokenType": "Bearer",
    "accessTokenExpiresAt": "2026-04-21T10:00:00",
    "refreshTokenExpiresAt": "2026-05-21T10:00:00",
    "newUser": false
  }
}
```

### `POST /auth/logout`

Headers:

```http
Authorization: Bearer {accessToken}
```

Preferred request:

```json
{}
```

Compatibility request:

```json
{
  "refreshToken": "string"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "loggedOut": true
  }
}
```

### `GET /users/me`

Current response:

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "nickname": "string",
    "email": "string",
    "profileImageUrl": "string",
    "bio": "string",
    "stats": {
      "totalRecords": 0,
      "uniqueShops": 0,
      "avgRating": 0.0
    },
    "createdAt": "2026-04-21T10:00:00"
  }
}
```

Target response after `totalStickers` work:

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

### Error Response

```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

## Scope Rules

- 우선 담당 범위는 `user`, `auth`, `token`, 인증 공통 설정, 공통 에러/로깅이다.
- `bread`, `breadrecord`, `upload`, `s3` 도메인 로직은 직접 수정하지 않는다.
- 다른 도메인 수정이 필요해 보이면 먼저 이유와 영향 범위를 정리하고 팀 결정 후 별도 브랜치로 진행한다.
- 전역 Jackson naming strategy 변경은 피한다.
- auth/user 응답은 현재 프론트 계약에 맞춰 camelCase를 유지한다.

## Work Items

| Priority | Work Item | Scope | Status | Branch Suggestion | Notes |
|---:|---|---|---|---|---|
| 1 | `/users/me.stats.totalStickers` 추가 | user | To Do | `feat/user-total-stickers-stat` | MVP 명세에 있는 마이페이지 통계 필드. 기존 `totalRecords`, `uniqueShops`, `avgRating`과 함께 응답해야 함. |
| 2 | request/response access logging 추가 | global | To Do | `feat/request-access-logging` | 프론트 연동 중 장애 추적용. Authorization, refreshToken, authorizationCode, Toss secret 등 민감정보 마스킹 필수. |
| 3 | 로컬/운영 env 문서화 보강 | docs/auth | To Do | `docs/local-env-and-e2e-guide` | JWT, Toss, CORS, DB, webhook 설정과 실행 절차 정리. |
| 4 | 서버 `bootRun` 확인 | runtime | To Do | same as docs or verification branch | 테스트 통과와 서버 기동은 별도라 로컬 DB 기준으로 확인 필요. |
| 5 | Auth/User 수동 API 호출 테스트 | auth/user | To Do | `test/auth-manual-verification` | dev token 또는 실제 token으로 `/users/me`, `/auth/refresh`, `/auth/logout` 확인. |
| 6 | Toss 설정 누락/실패 응답 확인 | auth | To Do | `test/auth-manual-verification` | 실제 Toss code 없이도 설정 누락 시 공통 에러 응답이 안전하게 내려가는지 확인. |
| 7 | Toss webhook 수동 테스트 | auth/user | To Do | `test/auth-manual-verification` | `x-toss-webhook-secret`, `eventType` 기반 처리 확인. |
| 8 | 회원탈퇴 수동 테스트 | user/auth | To Do | `test/auth-manual-verification` | `TOSS_UNLINK_ACCESS_TOKEN` 설정이 있어야 완전 검증 가능. |
| 9 | Swagger 최신화 | auth/user/global | To Do | `docs/auth-swagger-contract` | 실제 request/response, camelCase, logout empty body, 공통 에러 응답 반영. |
| 10 | Rate limiting 최소 구현 | global/auth | Needs Decision | `feat/auth-rate-limit` | 명세에 존재. 우선 auth API부터 적용할지 팀 결정 필요. |
| 11 | `/v1` prefix 반영 방식 결정 | global/all APIs | Needs Decision | `feat/api-v1-prefix` | 전체 라우팅 영향. Spring에서 붙일지 Gateway/Nginx에서 처리할지 결정 필요. |
| 12 | 이벤트 수집 API | new domain/common | Needs Decision | `feat/event-collection` | Amplitude 직접 연동인지 자체 DB 저장인지 결정 필요. |
| 13 | Presigned upload | upload/s3 | Needs Decision | `feat/upload-presigned-url` | 현재 MVP는 multipart 직접 업로드 흐름. 구현 여부 팀 결정 필요. |
| 14 | 비로그인 저장 -> 로그인 후 자동 저장 플로우 | auth + frontend contract | Needs Decision | `docs/guest-save-login-flow` | 백엔드 임시저장 없이 프론트가 입력값/사진 보관 후 로그인 성공 시 저장 API 호출하는 계약인지 확정 필요. |
| 15 | Toss 로그인 실제 E2E | auth | Waiting External | `test/auth-toss-e2e` | 실제 프론트 authorizationCode/referrer 필요. |
| 16 | 로그아웃 실제 E2E | auth | Waiting External | `test/auth-toss-e2e` | Toss 로그인 성공 후 실제 access/refresh token pair 필요. |
| 17 | PR 병합 후 develop 재검증 | all touched scope | Waiting PR | after merge | `develop` 기준 `.\gradlew.bat test` 재실행. |

## Status Legend

- `Done`: 코드 구현과 테스트가 완료됨.
- `To Do`: 백엔드에서 바로 진행 가능.
- `Needs Decision`: 팀 결정 또는 다른 담당 범위 확인이 선행되어야 함.
- `Waiting External`: 실제 Toss authorizationCode, 운영 secret 등 외부 입력이 필요함.
- `Blocked`: 현재 작업자가 직접 수정하면 범위 위반이거나 충돌 위험이 큼.

## Recommended Order

1. `feat/user-total-stickers-stat`
   - `/users/me.stats.totalStickers` 구현
   - user service/mapper/repository projection 테스트 보강
   - `.\gradlew.bat test`

2. `feat/request-access-logging`
   - 요청/응답 access logging 추가
   - 민감정보 마스킹
   - health/swagger/static 요청 제외 여부 결정
   - logging 테스트 또는 최소 단위 테스트

3. `docs/local-env-and-e2e-guide`
   - 로컬 DB 실행
   - `bootRun`
   - auth/user 수동 호출 절차
   - Toss webhook 수동 호출 절차
   - 필수 env 정리

4. `test/auth-manual-verification`
   - 로컬 서버 기동
   - `/auth/refresh`
   - `/auth/logout`
   - `/users/me`
   - `/auth/webhook/toss-unlink`
   - 실패 응답 공통 구조 확인

5. Team decision items
   - `/v1` prefix
   - Rate limiting 범위
   - Event collection 방식
   - Presigned upload MVP 포함 여부
   - 비로그인 저장 후 자동 저장 책임 분리

6. `test/auth-toss-e2e`
   - 실제 Toss authorizationCode/referrer 수신 후 전체 인증 플로우 확인
   - `/auth/toss`
   - `/users/me`
   - `/auth/refresh`
   - `/auth/logout`
   - logout 이후 refresh 실패 확인

## Branch Playbooks

각 브랜치는 `develop`에서 생성하고, 작업 전 반드시 최신 `origin/develop`을 반영한다.

공통 시작 절차:

1. `git status --short --branch`
2. `git fetch origin develop`
3. `git switch develop`
4. `git pull --ff-only origin develop`
5. `git switch -c <branch-name>`
6. 아래 브랜치별 "Read first" 파일을 읽고 요약
7. 수정 예정 파일 목록을 먼저 정리
8. 구현
9. `.\gradlew.bat test`
10. 커밋/푸시

공통 완료 보고:

- 생성 파일
- 수정 파일
- 테스트 결과
- 실제 수동 확인 여부
- 다음 브랜치가 먼저 읽을 파일
- 다음 TODO

## Branch 1: `feat/user-total-stickers-stat`

### Purpose

- `/users/me` 응답의 `stats`에 `totalStickers`를 추가한다.

### Read First

- `docs/user-part-spec.md`
- `docs/codex-prompts-user/user-profile-api.txt`
- `src/main/java/com/bean/breaddiary/domain/user/service/UserService.java`
- `src/main/java/com/bean/breaddiary/domain/user/dto/mapper/UserMapper.java`
- `src/main/java/com/bean/breaddiary/domain/user/dto/response/UserMeResponse.java`
- `src/main/java/com/bean/breaddiary/domain/user/dto/response/UserStatsResponse.java`
- `src/main/java/com/bean/breaddiary/domain/breadrecord/repository/BreadRecordRepository.java`
- `src/main/java/com/bean/breaddiary/domain/breadrecord/dto/projection/UserStatsProjection.java`
- `src/test/java/com/bean/breaddiary/domain/user/service/UserServiceTest.java`
- `src/test/java/com/bean/breaddiary/domain/user/controller/UserControllerTest.java`

### Expected Files

- Modify: `src/main/java/com/bean/breaddiary/domain/user/dto/response/UserStatsResponse.java`
- Modify: `src/main/java/com/bean/breaddiary/domain/user/dto/mapper/UserMapper.java`
- Modify: `src/main/java/com/bean/breaddiary/domain/breadrecord/dto/projection/UserStatsProjection.java`
- Modify: `src/main/java/com/bean/breaddiary/domain/breadrecord/repository/BreadRecordRepository.java`
- Modify: `src/test/java/com/bean/breaddiary/domain/user/service/UserServiceTest.java`
- Modify: `src/test/java/com/bean/breaddiary/domain/user/controller/UserControllerTest.java`
- Optional: repository/data JPA test if current service test cannot cover duplicate bread counting.

### Expected Response

```json
{
  "success": true,
  "data": {
    "stats": {
      "totalRecords": 42,
      "totalStickers": 15,
      "uniqueShops": 18,
      "avgRating": 4.2
    }
  }
}
```

### Implementation Notes

- 기존 `UserStatsResponse`에 `totalStickers` 필드를 추가한다.
- 기존 stats projection/repository 쿼리에서 유저가 기록한 고유 `bread_id` 수를 계산한다.
- soft delete된 bread record는 제외한다.
- 응답은 auth/user 정책에 맞춰 camelCase `totalStickers`로 내려간다.
- bread/breadrecord 비즈니스 로직은 직접 바꾸지 않는다.
- repository projection 쿼리 수정은 `/users/me` 통계 조회를 위한 최소 조회 변경으로 제한한다.
- `avgRating`은 기록 없을 때 기존처럼 `0.0`을 유지한다.
- `uniqueShops`는 기존 정책대로 `null`, `""`, `" "`를 제외해야 한다.

### Acceptance Criteria

- 기록 없는 유저는 `totalStickers = 0`.
- 같은 bread를 여러 번 기록해도 `totalStickers = 1`.
- 서로 다른 bread를 기록하면 고유 bread 수만큼 증가.
- soft delete된 기록은 제외.
- `/users/me` JSON path: `$.data.stats.totalStickers`.
- snake_case 전역 설정 아래에서도 auth/user 응답은 camelCase 유지.

### Required Tests

- `UserMapper` 또는 `UserServiceTest`
  - projection null -> `totalStickers = 0`
  - projection value -> response value 유지
- `UserControllerTest`
  - `$.data.stats.totalStickers`
  - `$.data.stats.total_stickers` 미존재
- 가능하면 repository test
  - same bread duplicate records -> 1
  - deleted record excluded

## Branch 2: `feat/request-access-logging`

### Purpose

- 프론트 연동 중 요청/응답 흐름을 추적할 수 있게 access logging을 추가한다.

### Read First

- `src/main/java/com/bean/breaddiary/global/config/WebConfig.java`
- `src/main/java/com/bean/breaddiary/global/interceptor/AuthInterceptor.java`
- `src/main/java/com/bean/breaddiary/global/common/GlobalExceptionHandler.java`
- `src/test/java/com/bean/breaddiary/global/interceptor/AuthInterceptorTest.java`

### Expected Files

- Add: `src/main/java/com/bean/breaddiary/global/filter/AccessLogFilter.java` or equivalent
- Add: `src/main/java/com/bean/breaddiary/global/config/FilterConfig.java` if filter registration is needed
- Add: `src/test/java/com/bean/breaddiary/global/filter/AccessLogFilterTest.java`
- Optional modify: `WebConfig` only if using interceptor instead of filter

### Logging Policy

Log one line per request:

- method
- URI
- status
- durationMs
- authenticated userId if available
- sessionId if available
- request id if introduced
- client IP if safely available

Do not log:

- `Authorization`
- access token
- refresh token
- Toss `authorizationCode`
- Toss `x-toss-webhook-secret`
- `TOSS_*` config values
- AWS keys
- S3 signed URLs
- multipart file contents

Recommended log examples:

```text
ACCESS method=POST path=/auth/toss status=200 durationMs=123 userId=- sessionId=-
ACCESS method=GET path=/users/me status=200 durationMs=35 userId=... sessionId=...
```

### Exclusions

Consider excluding:

- `/swagger-ui/**`
- `/v3/api-docs/**`
- static resources
- `OPTIONS` preflight logs, unless debugging CORS

### Acceptance Criteria

- Access logs do not expose sensitive values.
- Error responses are still handled by `GlobalExceptionHandler`.
- AuthInterceptor behavior is unchanged.
- `.\gradlew.bat test` passes.

## Branch 3: `docs/local-env-and-e2e-guide`

### Purpose

- 프론트가 붙기 전 백엔드 실행/검증 절차를 팀이 동일하게 수행할 수 있게 정리한다.

### Expected File

- Add: `docs/local-auth-user-verification.md`

### Must Include

- Docker MySQL 실행 명령
- `application-local.yml` 사용 방법
- 필수 env 목록
- `.\gradlew.bat test`
- `.\gradlew.bat bootRun`
- Swagger URL
- auth/user curl examples
- Toss webhook curl examples
- 실패 응답 예시
- 실제 Toss E2E에서 필요한 프론트 입력값

### Env Variables

```text
JWT_SECRET
CORS_ALLOWED_ORIGINS
TOSS_WEBHOOK_SECRET
TOSS_UNLINK_ACCESS_TOKEN
TOSS_DECRYPTION_KEY
TOSS_AAD
```

### Acceptance Criteria

- 새 팀원이 문서만 보고 로컬 서버를 띄울 수 있어야 한다.
- 실제 Toss code 없이도 webhook과 common error response를 확인할 수 있어야 한다.
- 실제 Toss code를 받으면 `/auth/toss -> /users/me -> /auth/refresh -> /auth/logout` 순서로 테스트할 수 있어야 한다.

## Branch 4: `test/auth-manual-verification`

### Purpose

- 실제 프론트 전에도 가능한 수동 호출 검증을 끝낸다.

### Read First

- `docs/local-auth-user-verification.md`
- `src/main/java/com/bean/breaddiary/domain/auth/controller/AuthController.java`
- `src/main/java/com/bean/breaddiary/domain/user/controller/UserController.java`
- `src/main/java/com/bean/breaddiary/global/interceptor/AuthInterceptor.java`

### Checklist

1. `.\gradlew.bat test`
2. `.\gradlew.bat bootRun`
3. Swagger 접속 확인
4. `/auth/refresh` invalid token -> `UNAUTHORIZED`
5. `/auth/logout` missing Bearer -> `UNAUTHORIZED`
6. `/users/me` missing Bearer -> `UNAUTHORIZED`
7. `/auth/webhook/toss-unlink` wrong secret -> `UNAUTHORIZED`
8. `/auth/webhook/toss-unlink` correct secret + unknown userKey -> success true
9. 설정 누락 시 `INTERNAL_ERROR` 또는 적절한 공통 에러 응답 확인

### Output

- 테스트에 사용한 env
- 호출한 endpoint
- 응답 status
- 응답 body
- 실패 원인과 조치

## Decision Item: `feat/auth-rate-limit`

### Decision Needed

- 라이브러리 도입 여부
- 인메모리로 MVP만 막을지, Redis 같은 외부 저장소를 쓸지
- API별 제한 수치 확정

### Spec Baseline

- 인증 API: 1분 10회
- 빵 기록 생성: 1시간 30회
- 이미지 업로드: 1시간 30회
- 기타 API: 1분 100회

### Recommended MVP Scope

1. 먼저 auth API만 적용한다.
2. `/auth/toss`, `/auth/refresh`, `/auth/logout` 대상.
3. IP 기반 + 가능하면 user/session 기반.
4. 초과 시 `429 RATE_LIMITED`.

### Risk

- 전체 API rate limit은 bread/breadrecord/upload/s3에도 영향을 준다.
- 이 브랜치에서는 auth-only로 시작하거나 팀 결정 후 공통 적용한다.

## Decision Item: `feat/api-v1-prefix`

### Decision Needed

- Spring Boot에서 `server.servlet.context-path=/v1`로 처리할지.
- Controller path를 모두 `/v1`로 바꿀지.
- API Gateway/Nginx/Load Balancer에서 `/v1`을 strip/proxy할지.

### Risk

- 전체 endpoint에 영향.
- bread/breadrecord/upload/s3 테스트가 모두 바뀔 수 있다.
- 팀 결정 없이 구현하지 않는다.

### Acceptance Criteria If Implemented

- `/v1/auth/toss`
- `/v1/auth/refresh`
- `/v1/auth/logout`
- `/v1/users/me`
- 기존 Swagger path 확인
- 전체 테스트 업데이트

## Decision Item: `feat/event-collection`

### Decision Needed

- Amplitude 직접 연동인지 자체 DB 저장인지.
- 이벤트 payload 스키마.
- 인증 필수 여부.
- 실패해도 주요 API 흐름을 막지 않을지.

### Suggested Minimal API

```http
POST /events
Authorization: Bearer {accessToken} optional or required by decision
Content-Type: application/json
```

```json
{
  "eventName": "bread_record_created",
  "properties": {
    "breadId": "uuid"
  },
  "occurredAt": "2026-04-21T10:00:00Z"
}
```

### Risk

- 새 도메인/저장소 작업이다.
- auth/user/token 범위를 벗어나므로 별도 담당/결정이 필요하다.

## Decision Item: `feat/upload-presigned-url`

### Decision Needed

- MVP에서 presigned upload를 실제로 쓸지.
- 현재 multipart 직접 업로드를 유지할지.
- 리사이즈/썸네일 책임을 서버가 가질지.

### Current Understanding

- 현재 MVP 저장 플로우는 `POST /breads`, `POST /breads/new` multipart 직접 업로드.
- presigned upload는 명세에 있지만 2차 이후 대용량 파일 대응으로 밀릴 수 있다.

### Risk

- upload/s3 담당 범위.
- 이 계획 문서에서는 작업 목록에 포함하지만 직접 구현하지 않는다.

## Decision Item: `docs/guest-save-login-flow`

### Decision Needed

- 비로그인 사용자가 입력한 기록/사진을 백엔드가 임시 저장할지.
- 프론트가 메모리/로컬 임시 저장 후 로그인 성공 시 저장 API를 호출할지.

### Recommended Contract

- 백엔드는 별도 guest draft 저장소를 만들지 않는다.
- 프론트가 입력값과 파일을 보관한다.
- 로그인 성공 후 받은 access token으로 기존 저장 API를 호출한다.
- 저장 API는 기존 `POST /breads`, `POST /breads/new`를 사용한다.

### Risk

- 실제 저장 API는 bread/breadrecord/upload/s3 범위다.
- auth 쪽은 로그인 성공 후 token 반환 계약만 안정화한다.

## Environment Variables To Verify

```text
JWT_SECRET
CORS_ALLOWED_ORIGINS
TOSS_WEBHOOK_SECRET
TOSS_UNLINK_ACCESS_TOKEN
TOSS_DECRYPTION_KEY
TOSS_AAD
```

## Manual Verification Checklist

Before frontend E2E:

1. `.\gradlew.bat test`
2. Docker MySQL or configured DB is running.
3. `.\gradlew.bat bootRun`
4. Swagger loads.
5. Auth/user endpoints return common error format on invalid requests.
6. Toss webhook rejects wrong secret.
7. Toss webhook accepts correct secret.
8. Logout with missing/invalid token returns `UNAUTHORIZED`.

After frontend gets real Toss code:

1. `POST /auth/toss`
2. `GET /users/me`
3. `POST /auth/refresh`
4. `POST /auth/logout`
5. `POST /auth/refresh` with old refresh token must fail.

## Not Done Until Real E2E

The backend can be implementation-ready before real Toss E2E, but these items cannot be marked fully verified without a real frontend/Toss authorization code:

- Real Toss login success
- Real login token pair based logout
- Real refresh after login
- Real withdrawal with Toss unlink access token
