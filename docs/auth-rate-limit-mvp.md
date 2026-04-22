# Auth Rate Limit MVP

## 목적

auth 관련 public endpoint에 대해 단일 서버 MVP 수준의 abuse 1차 방어를 추가한다.

이번 범위는 in-memory fixed window 방식이다. Redis, DB 저장, 분산 카운터, sliding window, token bucket은 사용하지 않는다.

## 적용 대상

| Endpoint | Method | 제한 | Window | 기준 |
| --- | --- | ---: | ---: | --- |
| `/auth/toss` | `POST` | 10회 | 60초 | IP |
| `/auth/refresh` | `POST` | 60회 | 60초 | IP |
| `/auth/webhook/toss-unlink` | `POST` | 60회 | 60초 | IP |

`/auth/logout`, `/events`, bread/breadrecord/upload/s3 endpoint는 이번 MVP rate limit 대상이 아니다.

## 식별 기준

이번 MVP는 `request.getRemoteAddr()`를 그대로 사용한다.

프록시 또는 로드밸런서 뒤에서 실제 client IP가 필요해지면 `RateLimitInterceptor`의 IP 추출 helper를 변경한다. 이번 브랜치에서는 `X-Forwarded-For` 처리를 적용하지 않는다.

## 저장 방식

- `ConcurrentHashMap` 기반 in-memory 저장소를 사용한다.
- key는 `RateLimitPolicy + identifier` 조합이다.
- bucket은 `windowStartMillis`, `count`만 가진다.
- 요청 처리 시 `compute(...)`로 count 증가와 초과 판단을 원자적으로 처리한다.
- 오래된 bucket은 요청 처리 중 opportunistic cleanup으로 제거한다.

서버 재시작 시 rate limit 상태는 초기화된다. 단일 서버 MVP 기준으로 의도한 동작이다.

## 응답 정책

제한을 초과하면 다음 응답을 반환한다.

```http
HTTP/1.1 429 Too Many Requests
Retry-After: <seconds>
```

```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMITED",
    "message": "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
  }
}
```

`Retry-After`는 현재 fixed window가 다시 열릴 때까지 남은 초 단위 값이다.

## 실패 정책

rate limit 초과는 `429 RATE_LIMITED`로 막는다.

rate limiter 내부 계산 오류 또는 예기치 못한 런타임 오류는 fail-open으로 처리한다. 즉, ERROR 로그만 남기고 원 요청은 통과시킨다. rate limit 장애가 로그인/refresh/webhook 기본 흐름을 막지 않기 위한 MVP 정책이다.

## 로그 정책

초과 시 WARN 로그를 남긴다.

- `action=authRateLimit`
- `result=blocked`
- `requestId`
- `endpoint`
- `identifier`
- `limit`
- `windowSeconds`
- `retryAfterSeconds`

내부 오류 시 ERROR 로그를 남긴다.

- `action=authRateLimit`
- `result=error`
- `requestId`
- `endpoint`
- `exceptionType`

로그에 남기지 않는 값:

- `Authorization` header
- access token
- refresh token
- Toss authorization code
- `x-toss-webhook-secret`
- request body / response body
- header 전체 dump
- query string 전체

## 범위 제외

- Redis, DB, Kafka, queue
- 분산 환경 대응
- sliding window, token bucket
- sessionId/userId/token claim 기반 제한
- `application.yml`, `application-local.yml` 설정 추가
- Swagger 대규모 수정
- 운영 관리자 UI
