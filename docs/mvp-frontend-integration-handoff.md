# MVP Frontend Integration Handoff

## Current Branch

- Branch: `test/47-auth-refresh-mysql-integration`
- Latest pushed code commit: `3d738f6 fix: align auth user API contract`
- Date: 2026-04-21

## Important Scope Rules

- Primary ownership is `user`, `auth`, and `token`.
- Do not directly modify `bread`, `breadrecord`, `upload`, or `s3` unless explicitly approved.
- Keep existing `bread` and `breadrecord` snake_case response behavior.
- `user` and `auth` API request/response fields are aligned to frontend camelCase requirements.
- Do not change global Jackson naming strategy casually.

## Recently Completed

- Auth/user API contract was aligned to camelCase.
- `POST /auth/toss` request accepts `authorizationCode` and `referrer`.
- `POST /auth/refresh` request accepts `refreshToken`.
- `POST /auth/logout` request accepts `refreshToken`.
- Auth token response uses:
  - `userId`
  - `accessToken`
  - `refreshToken`
  - `tokenType`
  - `accessTokenExpiresAt`
  - `refreshTokenExpiresAt`
  - `newUser`
- `GET /users/me` response uses:
  - `id`
  - `nickname`
  - `email`
  - `profileImageUrl`
  - `bio`
  - `stats.totalRecords`
  - `stats.uniqueShops`
  - `stats.avgRating`
  - `createdAt`
- `TossAuthClient` internal Toss API JSON fields are explicitly mapped as camelCase.
- `AuthControllerTest` and `UserControllerTest` were updated for the camelCase contract.
- Full `gradle test` passed before the latest code commit.

## Current Implementation Status

### Completed Or Mostly Complete

- JWT access token creation and parsing.
- Refresh token creation, hashing, rotation, and validation.
- `UserSession` persistence and refresh expiration handling.
- `currentJti` refresh token reuse detection.
- Pessimistic write lock repository method for refresh/session rotation.
- `AuthInterceptor` based Bearer access token guard.
- Optional auth for:
  - `GET /breads`
  - `GET /breads/autocomplete`
  - `GET /breads/catalog/{breadId}`
- `POST /auth/toss` backend flow:
  - receives frontend Toss `authorizationCode` and `referrer`
  - calls Toss token API
  - calls Toss user info API
  - finds or creates `User`
  - creates `UserSession`
  - returns app access/refresh tokens
- `POST /auth/refresh`.
- `POST /auth/logout` service/controller logic.
- `GET /users/me`.
- `DELETE /users/me`.
- User stats inside `/users/me`.

### Partially Complete

- Toss OAuth E2E:
  - Backend flow exists.
  - Real Toss `authorizationCode` has not been tested yet.
  - Frontend must call Toss SDK `appLogin()` and immediately send `authorizationCode/referrer`.
- Logout API:
  - Controller/service implemented.
  - Full verification requires a real login token pair from Toss E2E.
  - Current interceptor does not whitelist `/auth/logout`, so access token may be required along with `refreshToken`.
- My page stats:
  - Included in `/users/me`.
  - Separate stats endpoint does not exist.
  - `totalStickers` is not implemented.

### Not Implemented

- `/v1` API prefix.
- Global error response format with `success=false` and `error`.
- `@ControllerAdvice` global exception handling.
- CORS configuration.
- Request/response logging policy.
- Rate limiting.
- Anonymous/guest save then login auto-save flow.
- Event collection API.
- Amplitude or custom event store integration.
- Presigned upload endpoint.

## API Spec Differences To Watch

- External MVP spec uses snake_case for auth/user, but current frontend request is camelCase.
- Current auth/user implementation is camelCase by explicit DTO annotations.
- MVP spec says `/auth/toss` request is `code` and `redirect_uri`; current implementation uses Toss App-in-Toss `authorizationCode` and `referrer`.
- MVP spec says refresh response only returns access token; current implementation rotates and returns both access and refresh tokens.
- MVP spec says logout body is empty; current implementation requires `refreshToken`.
- MVP spec says `/users/me.stats.total_stickers`; current code does not provide it.
- MVP spec includes `/v1` prefix; controllers currently do not.

## Known Local Working Tree Notes

These were already dirty before this handoff commit and are intentionally not included in the auth/user contract commit:

- `docs/codex-prompts-user/auth-concurrency-test.txt`
- `docs/codex-prompts-user/auth-session-domain.txt`
- `docs/codex-prompts-user/user-profile-api.txt`
- `docs/codex-prompts-user/user-profile-test.txt`
- `docs/codex-prompts-user/user-withdrawal.txt`
- `.gradle-codex/`

Also note:

- `src/main/resources/application.yml` is ignored by git.
- A local JWT setting may exist there, but it is not tracked.
- For deployed environments, set `JWT_SECRET` as an environment variable.

## Recommended Next Branch

Suggested branch:

- `feat/mvp-frontend-integration`

Purpose:

- Remove frontend integration blockers for MVP.
- Confirm CORS.
- Confirm auth/user API contract.
- Prepare Toss E2E test.
- Decide logout API policy.
- Avoid changing unrelated bread/breadrecord flows.

## Next Work Priority

1. Check current branch status and latest `develop`.
2. Create or switch to `feat/mvp-frontend-integration`.
3. Read:
   - `AGENTS.md`
   - `build.gradle`
   - `src/main/java/com/bean/breaddiary/domain/auth/**`
   - `src/main/java/com/bean/breaddiary/domain/user/**`
   - `src/main/java/com/bean/breaddiary/global/config/**`
   - `src/main/java/com/bean/breaddiary/global/interceptor/**`
   - `src/test/java/com/bean/breaddiary/domain/auth/controller/AuthControllerTest.java`
   - `src/test/java/com/bean/breaddiary/domain/user/controller/UserControllerTest.java`
4. Verify CORS status.
5. Decide whether `/auth/logout` should:
   - require access token plus refresh token, or
   - be whitelisted and use refresh token only, or
   - use access token/session id only.
6. Prepare frontend API contract examples.
7. Run auth/user focused tests.
8. Run full test suite if feasible.

## Frontend Contract Snapshot

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

Current request:

```json
{
  "refreshToken": "string"
}
```

Current response:

```json
{
  "success": true,
  "data": {
    "loggedOut": true
  }
}
```

### `GET /users/me`

Response:

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

## Toss E2E Test Checklist

1. Backend server is running.
2. DB is connected.
3. `JWT_SECRET` is configured.
4. Frontend calls Toss SDK `appLogin()`.
5. Frontend receives `authorizationCode` and `referrer`.
6. Frontend immediately calls `POST /auth/toss`.
7. Backend returns `accessToken` and `refreshToken`.
8. Call `GET /users/me` with `Authorization: Bearer {accessToken}`.
9. Call `POST /auth/refresh` with `refreshToken`.
10. Call `POST /auth/logout`.
11. Confirm refresh fails after logout.

Important:

- Toss `authorizationCode` is one-time use.
- Toss `authorizationCode` expires quickly.
- If Toss API fails, check for expired/reused code, mTLS/certificate issues, environment config, DB, and CORS.

## New Chat Starter Prompt

```text
We are continuing bread-diary-backend from the handoff in docs/mvp-frontend-integration-handoff.md.
First read AGENTS.md and docs/mvp-frontend-integration-handoff.md.
Then check git status and current branch.
Do not modify bread/breadrecord/upload/s3 directly.
Focus on user/auth/token and frontend integration blockers.
Before editing code, summarize:
1. current branch and dirty files
2. files read
3. exact files proposed for modification
4. reason for each modification
Start by planning CORS, logout policy, global error handling scope, and Toss E2E readiness.
```
