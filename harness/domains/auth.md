# Auth Domain Guide (Draft)

## 이 도메인의 역할

로그인, 토큰 발급/재발급, 세션 관리, Toss 연동을 담당한다.

## 핵심 포인트

### 1. 인증은 Bearer Access Token 기준

- 보호 API는 `Authorization: Bearer <access_token>`
- `X-USER-ID` 같은 별도 헤더를 도입하지 않는다.

### 2. 세션 검증은 토큰 + 세션 둘 다 본다

현재 인증 흐름은:

- JWT 파싱
- token type 확인
- 만료 확인
- `UserSession` 활성 상태 확인
- user/session 정합성 확인

즉 JWT만 맞다고 끝나지 않는다.

### 3. Toss 연동은 일반 HTTP 호출이 아님

Toss API는 현재 mTLS 설정이 필요하다.

주의:

- Toss 호출은 전용 RestClient 설정을 우선 확인
- `app.auth.toss.mtls.*` 설정 누락 여부 확인
- 환경설정 문제와 비즈니스 로직 문제를 분리해서 본다

### 4. JWT secret은 필수 설정

`app.auth.jwt.secret` 누락 시 인증 기능이 정상 동작하지 않는다.

Auth 작업 시:

- secret 존재 여부
- refresh/session 관련 설정

을 함께 본다.

### 5. 응답/예외는 공통 규칙 사용

Auth도 예외 없이 `ApiResponse` / `ApiErrorResponse` 규칙을 따른다.

## 작업 전 확인 추천 파일

- `domain/auth/controller/AuthController.java`
- `domain/auth/service/AuthService.java`
- `domain/auth/service/JwtTokenProvider.java`
- `domain/auth/config/TossRestClientConfig.java`
- `global/interceptor/AuthInterceptor.java`
