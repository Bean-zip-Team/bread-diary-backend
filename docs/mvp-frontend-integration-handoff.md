# MVP Frontend Integration Handoff

> Deprecated/archive note: this document is a historical handoff snapshot from the `feat/58-mvp-frontend-integration` work. Current auth/user API contract is `docs/auth-user-api-contract.md`; current local/E2E guide is `docs/local-env-and-e2e-guide.md`; Toss E2E readiness is `docs/toss-e2e-checklist.md`.

## Historical Branch Snapshot

- Branch: `feat/58-mvp-frontend-integration`
- Latest merged develop commit: `a135be0 Merge pull request #57 from Bean-zip-Team/test/47-auth-refresh-mysql-integration`
- Local handoff/update commits:
  - `826037f docs: update frontend integration handoff order`
  - `46aa8f5 Merge remote-tracking branch 'origin/develop' into feat/58-mvp-frontend-integration`
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
- `POST /auth/logout` accepts either an empty body with Bearer access token or an optional `refreshToken` body.
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
- Latest `develop` was merged into `feat/58-mvp-frontend-integration`.
- CORS configuration was added in `WebConfig`.
- Global error handling was added with `{ success: false, error: { code, message } }`.
- `AuthControllerTest`, `AuthServiceTest`, and `GlobalExceptionHandlerTest` cover the new auth/error behavior.
- Historical note: this snapshot previously mentioned Gradle wrapper availability issues. Current cleanup docs should use `docs/local-env-and-e2e-guide.md` and the Gradle wrapper.

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
  - Bearer access token is required by `AuthInterceptor`.
  - Empty body logs out the authenticated session from request attributes.
  - Optional `refreshToken` body remains supported and validates the refresh token before revoking.
- `GET /users/me`.
- `DELETE /users/me`.
- User stats inside `/users/me`.
- Global CORS configuration for local frontend origins and `https://bread-diary.app`.
- Global `@RestControllerAdvice` error response shape.
- Basic auth/error logging through `GlobalExceptionHandler`.

### Partially Complete

- Toss OAuth E2E:
  - Backend flow exists.
  - Real Toss `authorizationCode` has not been tested yet.
  - Frontend must call Toss SDK `appLogin()` and immediately send `authorizationCode/referrer`.
- Logout API:
  - Controller/service implemented.
  - Full verification requires a real login token pair from Toss E2E.
  - `/auth/logout` is intentionally not whitelisted, so access token is required.
  - `refreshToken` body is optional after this branch.
- My page stats:
  - Included in `/users/me`.
  - Separate stats endpoint does not exist.
  - Current implementation includes camelCase `totalStickers`.

### Not Implemented

- `/v1` API prefix.
- Event collection or external log collection infrastructure.
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
- MVP spec says logout body is empty; current implementation now supports empty body when Bearer access token is present. Existing `refreshToken` body is still accepted.
- MVP spec says `/users/me.stats.total_stickers`; current implementation provides camelCase `stats.totalStickers`.
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
- For deployed environments, set `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `TOSS_WEBHOOK_SECRET`, `TOSS_UNLINK_ACCESS_TOKEN`, `TOSS_DECRYPTION_KEY`, and `TOSS_AAD` as environment variables where needed.

## Recommended Next Branch

Current branch:

- `feat/58-mvp-frontend-integration`

Purpose:

- Remove frontend integration blockers for MVP.
- Confirm CORS.
- Confirm auth/user API contract.
- Prepare Toss E2E test.
- Keep logout API policy aligned with frontend: Bearer access token required, body optional.
- Avoid changing unrelated bread/breadrecord flows.

## Required Work Order

Follow this order before implementing anything new. Other teammates may already have
merged overlapping work into `develop`, so the next branch must first reconcile the
latest code and only then modify what is still missing.

1. Check current branch status with `git status --short --branch`.
2. Fetch latest `develop` from origin.
3. Inspect recent `develop` changes related to auth, user, token, global config,
   global error handling, CORS, tests, and docs.
4. Create or switch to `feat/mvp-frontend-integration`.
5. Merge `origin/develop` into the work branch using the safest non-destructive
   workflow.
6. If conflicts occur, list conflicted files first.
7. Resolve only conflicts inside user, auth, token, global auth/config/common,
   and related tests/docs.
8. Do not directly modify bread, breadrecord, upload, or s3. If those files
   conflict or appear necessary, stop and explain why.
9. After the merge, reread the current implementation instead of relying on this
   handoff alone.
10. Classify each auth/user/token item as done, partially done, or missing based
    on code and tests.
11. Update docs first so the MVP status reflects the merged code.
12. Implement only the remaining user/auth/token/frontend integration blockers.
13. Run focused auth/user/token tests.
14. Run the full test suite if feasible.
15. Summarize what was verified, what still needs real Toss E2E, and what must
    not be touched by this branch.

## Next Work Priority

1. For current work, use Gradle wrapper and run the relevant tests when code changes are made.
2. Confirm frontend auth/user contract with the frontend team.
3. Confirm `/auth/toss` request and response fields match frontend camelCase.
4. Verify JWT settings and Toss settings are documented for local and deployed
   environments.
5. Prepare Toss E2E test steps using a real frontend authorization code.
6. Verify `/auth/refresh`, `/auth/logout`, `/users/me`, withdrawal, and webhook
   behavior with existing tests and manual calls where possible.
7. `users/me.stats.totalStickers` is already implemented in the current contract.
8. Keep bread, breadrecord, upload, and s3 changes out of this branch unless approved.

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

Headers:

```http
Authorization: Bearer {accessToken}
```

Preferred request:

```json
{}
```

Also accepted for compatibility:

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

### Common Error Response

Response:

```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

Current code mapping:

- `400` -> `INVALID_REQUEST`
- validation failure -> `VALIDATION_FAILED`
- `401` -> `UNAUTHORIZED`
- `403` -> `FORBIDDEN`
- `404` -> `NOT_FOUND`
- `409` -> `CONFLICT`
- `413` -> `FILE_TOO_LARGE`
- `429` -> `RATE_LIMITED`
- `502` -> `TOSS_SERVER_ERROR`
- `500` -> `INTERNAL_ERROR`

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
10. Call `POST /auth/logout` with `Authorization: Bearer {accessToken}` and an empty body.
11. Confirm refresh fails after logout.

Important:

- Toss `authorizationCode` is one-time use.
- Toss `authorizationCode` expires quickly.
- If Toss API fails, check for expired/reused code, mTLS/certificate issues, environment config, DB, and CORS.
- This is an archived prompt. Current work should verify the branch with the Gradle wrapper when code changes are made.

## New Chat Starter Prompt

```text
We are continuing bread-diary-backend from the handoff in docs/mvp-frontend-integration-handoff.md.
First read AGENTS.md and docs/mvp-frontend-integration-handoff.md.
Then check git status and current branch.
Fetch latest origin/develop, inspect relevant changes, and confirm feat/58-mvp-frontend-integration
still contains the latest develop changes before editing code.
Do not modify bread/breadrecord/upload/s3 directly.
Focus on user/auth/token and frontend integration blockers.
Before editing code, summarize:
1. current branch and dirty files
2. files read
3. exact files proposed for modification
4. reason for each modification
After checking develop, classify auth/user/token items as done, partially done, or missing.
Update docs first, then implement only remaining user/auth/token work.
Start by reading `docs/auth-user-api-contract.md`, `docs/local-env-and-e2e-guide.md`, and `docs/toss-e2e-checklist.md` before using this archived handoff snapshot.
```
