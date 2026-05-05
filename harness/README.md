# Bread Diary Harness

이 폴더는 Bread Diary 프로젝트의 세부 작업 규칙 문서 모음이다.

목적:

- 프로젝트 작업 규칙을 도메인별/공통 규칙으로 분리한다.
- 큰 `AGENTS.md`에 모든 내용을 몰아넣지 않고, 역할별 문서로 나눠 관리한다.
- `AGENTS.md`는 엔트리포인트로 유지하고, 세부 규칙은 `harness/`에서 읽을 수 있게 한다.

원칙:

1. `AGENTS.md`는 엔트리포인트로 짧게 유지한다.
2. 도메인별 상세 규칙은 `domains/` 아래에서 관리한다.
3. 공통 아키텍처/응답/테스트 규칙은 `architecture/` 아래에서 관리한다.
4. 실제 코드와 충돌하면 코드를 먼저 확인하고 문서를 갱신한다.
5. 이 폴더의 문서는 프로젝트 작업 기준으로 사용한다.

구성:

- `domains/auth.md`
- `domains/bread.md`
- `domains/recommendation.md`
- `domains/breadrecord.md`
- `domains/breadtype.md`
- `architecture/project-structure.md`
- `architecture/coding-conventions.md`
- `architecture/service-layer.md`
- `architecture/response-rules.md`
- `architecture/testing-rules.md`
- `workflows/documentation-rules.md`
- `workflows/git-pr-rules.md`
