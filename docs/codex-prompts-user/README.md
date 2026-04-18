# Codex 프롬프트 묶음

사용 순서

1. auth-session-domain.txt
2. auth-login.txt
3. auth-refresh-logout.txt
4. auth-interceptor.txt
5. user-profile-api.txt
6. user-profile-test.txt
7. user-withdrawal.txt
8. auth-concurrency-test.txt

브랜치 표기 규칙

- 고정 숫자를 넣지 않았습니다.
- 프롬프트 안에는 모두 `feat/<issue-number>-브랜치명` 형식으로 적었습니다.
- 실제 사용할 때 `<issue-number>` 부분만 직접 바꿔서 쓰면 됩니다.

인증 정책 요약

- Spring Security 미사용
- Access Token 만료: 1일
- Refresh Token 만료: 30일
- Sliding Session
- Refresh 성공 시 Access / Refresh 모두 재발급
- Refresh Token Rotation 적용
- 동시성 처리 고려
