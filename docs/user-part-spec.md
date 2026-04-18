# User/Auth Part Spec

## 1. User 도메인 목적
- 현재 사용자 조회
- 토스 로그인 기반 사용자 식별
- 슬라이딩 세션 기반 토큰 재발급
- 회원탈퇴 및 토스 연결 해제 처리

## 2. User 엔티티 필드
- id
- tossUserKey
- nickname
- email
- profileImageUrl
- bio
- createdAt
- updatedAt

## 3. 인증 정책
- Access Token: 1일
- Refresh Token: 30일
- 인증 방식: Sliding Session
- Access 만료 시 Refresh 로 재발급
- Refresh 재발급 시 Access / Refresh 모두 rotation
- 이전 Refresh Token 즉시 무효화
- 동시성 충돌 방지 필요

## 4. 현재 사용자 조회
- GET /users/me
- auth interceptor 기반 사용자 식별
- 응답: 프로필 + 통계(total_records, unique_shops, avg_rating)

## 5. 회원탈퇴 정책
### 앱 내부 탈퇴
- 서버가 토스 연결 끊기 요청을 보낸다.
- 토스 API 호출이 성공하면 콜백을 기다리지 않고 로컬 사용자 데이터를 즉시 완전 삭제한다.
- 사용자 세션 / refresh token 정보도 함께 정리한다.

### 토스 웹훅 탈퇴/연결 끊기
- 엔드포인트: POST /auth/webhook/toss-unlink
- 헤더: x-toss-webhook-secret
- body: { userKey, eventType }

처리 정책
- UNLINK: 토큰/세션만 초기화하고 사용자 데이터는 유지한다.
- WITHDRAWAL_TERMS: 사용자 데이터 완전 삭제
- WITHDRAWAL_TOSS: 사용자 데이터 완전 삭제

## 6. 웹훅 인증
- Toss 콘솔에 웹훅 URL 등록
- x-toss-webhook-secret 헤더 값을 서버의 TOSS_WEBHOOK_SECRET 환경변수와 비교
- body 에서 userKey, eventType 추출
- 공식 이벤트 타입은 UNLINK | WITHDRAWAL_TERMS | WITHDRAWAL_TOSS

## 7. 구현 순서
1. user domain
2. auth session domain
3. auth login
4. auth refresh logout
5. auth interceptor
6. user profile api
7. user profile test
8. user withdrawal
9. auth concurrency test
