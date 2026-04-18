# Bread Diary Codex Setup Bundle

이 폴더는 Codex가 바로 참고할 수 있도록 정리한 파일 묶음입니다.

## 포함 파일

- `AGENTS.md`
  - 저장소 루트에 두는 공통 작업 규칙 파일
- `docs/user-part-spec.md`
  - user 파트 구현 범위와 응답 스펙 요약
- `prompts/25-user-domain.txt`
  - user 도메인 기본 구조 브랜치용 프롬프트
- `prompts/26-user-profile-service.txt`
  - 사용자 정보/통계 조회 서비스 브랜치용 프롬프트
- `prompts/27-user-profile-api.txt`
  - `/users/me` API 브랜치용 프롬프트
- `prompts/28-user-profile-test.txt`
  - 테스트/검증 브랜치용 프롬프트

## 사용 순서

1. `AGENTS.md`를 저장소 루트에 넣습니다.
2. `docs/user-part-spec.md`를 저장소 안 문서 폴더에 넣습니다.
3. 각 브랜치에서 해당하는 `prompts/*.txt` 내용을 Codex에 그대로 넣습니다.

## 권장 위치

- 저장소 루트: `AGENTS.md`
- 저장소 내 문서 폴더 예시: `docs/user-part-spec.md`
- 프롬프트 파일은 로컬 보관 또는 팀 문서 폴더에 저장
