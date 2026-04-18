# Codex File Placement Guide

이 파일은 현재 폴더에 있는 설정 파일들을 저장소 안의 어디에 배치해야 하는지 알려주는 안내서입니다.

## 권장 사용 방법

이 폴더 전체를 저장소 안 임시 폴더 하나에 넣습니다. 예시:

- `codex-setup/`
- `codex-temp/`
- `docs/codex/`

그 다음 Codex에 아래 작업을 요청하면 됩니다.

## Codex에게 줄 작업 지시

1. 현재 폴더의 `placement-manifest.json`을 먼저 읽는다.
2. 각 파일을 manifest의 `target_path` 위치로 복사 또는 이동한다.
3. 이미 같은 파일이 있으면 덮어쓰기 전에 diff를 보여준다.
4. 파일 배치 후, 어떤 파일을 어디에 배치했는지 정리한다.
5. 그 다음부터는 저장소 루트의 `AGENTS.md`와 `docs/user-part-spec.md`를 기준으로 이후 작업을 수행한다.

## 사람이 직접 배치할 경우

- `AGENTS.md` -> 저장소 루트
- `docs/user-part-spec.md` -> 저장소의 `docs/user-part-spec.md`
- `prompts/*.txt` -> 로컬 보관용 또는 저장소의 `docs/codex-prompts/` 같은 폴더에 보관

## 추천 저장소 내 구조

- `AGENTS.md`
- `docs/user-part-spec.md`
- `docs/codex-prompts/25-user-domain.txt`
- `docs/codex-prompts/26-user-profile-service.txt`
- `docs/codex-prompts/27-user-profile-api.txt`
- `docs/codex-prompts/28-user-profile-test.txt`
