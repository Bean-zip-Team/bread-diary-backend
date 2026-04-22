# Event Collection MVP

## 1. 목적

이 문서는 `POST /events` MVP 계약과 구현 범위를 정리한다.

이번 MVP의 서버 역할은 이벤트를 받아 외부 analytics 도구로 best-effort 전달하는 수집 게이트웨이다. 서버는 이벤트 의미나 프론트 재전송 정책을 판단하지 않는다.

## 2. 서버 인증 정책

`POST /events`는 optional auth endpoint다.

| 요청 상태 | 서버 동작 |
| --- | --- |
| Authorization header 없음 | 익명 이벤트로 허용 |
| 유효한 Bearer access token 있음 | `userId`, `sessionId`를 request attribute 기준으로 연결 |
| invalid/expired/refresh token 있음 | 기존 인증 정책대로 `401 UNAUTHORIZED` |

서버는 invalid token 요청을 익명 이벤트로 자동 downgrade하지 않는다. 익명 fallback은 프론트가 Authorization header를 제거하고 다시 보내는 별도 요청으로 처리한다.

## 3. 프론트 전송 정책

프론트는 이벤트별 전송 정책을 가진다.

| 정책 | 설명 |
| --- | --- |
| 익명 허용 | Authorization 없이도 바로 전송 가능 |
| 인증 우선 | Authorization으로 먼저 전송, `401`이면 refresh 후 1회 재시도 |
| 드롭 | 인증 실패 시 익명으로 보내지 않고 버림 |

`auth_toss_succeeded`, `logout_clicked`, `bread_record_create_succeeded`는 서버 인증 필수 이벤트가 아니다. 프론트에서 인증 우선 이벤트로 취급한다.

## 4. API 계약

```http
POST /events
Content-Type: application/json
Authorization: Bearer <ACCESS_TOKEN> optional
```

Request:

```json
{
  "eventName": "screen_viewed",
  "anonymousId": "client-generated-anonymous-id",
  "occurredAt": "2026-04-22T12:00:00Z",
  "properties": {
    "screen": "home"
  }
}
```

Success:

```http
202 Accepted
```

```json
{
  "success": true,
  "data": {
    "accepted": true
  }
}
```

## 5. 식별자 규칙

| 상태 | 규칙 |
| --- | --- |
| 인증 없음 | `anonymousId` 필수 |
| 인증 성공 | `anonymousId` 선택 |
| `userId`도 없고 `anonymousId`도 없음 | `400 VALIDATION_FAILED` |
| 인증 성공 + `anonymousId` 있음 | 외부 전송 시 `user_id`, `device_id` 함께 사용 가능 |
| 인증 성공 + `anonymousId` 없음 | 외부 전송 시 `user_id` 사용 |
| 익명 이벤트 | 외부 전송 시 `device_id=anonymousId` 사용 |

## 6. eventName allowlist

`eventName`은 lower_snake_case이며 아래 값만 허용한다.

- `app_opened`
- `screen_viewed`
- `auth_toss_started`
- `auth_toss_succeeded`
- `bread_catalog_viewed`
- `bread_detail_viewed`
- `bread_record_create_started`
- `bread_record_create_succeeded`
- `logout_clicked`

allowlist에 없는 값은 `400 VALIDATION_FAILED`로 거부한다.

## 7. properties 정책

`properties`는 선택값이다. `null`이면 빈 map으로 처리한다.

제한:

- 최대 key 수: 30
- key 최대 길이: 60
- string value 최대 길이: 200
- 허용 value type: string, number, boolean, null
- 차단 value type: object, array, file/binary 계열

민감 key가 포함되면 `400 VALIDATION_FAILED`로 거부한다.

민감 key:

- `password`
- `token`
- `refresh_token`
- `access_token`
- `authorization`
- `cookie`
- `email`
- `phone`

민감 key 실패 응답은 구체 key를 노출하지 않는다.

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "이벤트 속성에 허용되지 않는 항목이 포함되어 있습니다."
  }
}
```

## 8. occurredAt 정책

- `occurredAt`은 선택값이다.
- 없으면 서버 수신 시각을 사용한다.
- 있으면 ISO-8601 offset datetime으로 파싱한다.
- 파싱 실패는 `400 VALIDATION_FAILED`다.

## 9. Amplitude 전송 정책

- `AMPLITUDE_API_KEY` 환경변수만 사용한다.
- 이번 MVP에서 `application.yml`은 수정하지 않는다.
- Amplitude endpoint는 코드 내부 상수로 둔다.
- API key가 없으면 외부 호출 없이 `202 Accepted`를 반환한다.
- Amplitude 4xx/5xx/네트워크 실패는 로그만 남기고 `202 Accepted`를 반환한다.

로그에 남기는 값:

- `eventName`
- `requestId`
- status
- exceptionType

로그에 남기지 않는 값:

- properties
- request body
- response body 원문
- API key
- Authorization header
- token류
- email/phone 등 개인정보

## 10. 이번 MVP 범위 밖

- DB event table
- Redis/Kafka
- retry queue
- batch 전송
- event persistence
- 관리자 UI
- 이벤트 조회 API
- 서버 내부 도메인 이벤트 자동 발행
- bread/breadrecord 기존 로직 수정
- Amplitude 외 provider 추상화
- `application.yml` analytics 설정 추가
- user agent/IP/device fingerprint 수집
