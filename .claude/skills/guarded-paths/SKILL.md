---
name: guarded-paths
description: harness/project-sync 아래 원본 사본을 반입하거나 그 아래 낡은 경로를 발견했을 때, 그리고 .claude/settings.json의 deny와 allow와 ask와 defaultMode와 hooks를 고칠 때 쓴다. 두 절차의 본문은 harness/docs/10-15 2절과 3절이다.
paths:
  - harness/project-sync/**
  - harness/out/import/**
  - .claude/settings.json
---

# 보호 경로

최초 작성: 2026-09-21
최종 갱신: 2026-09-25 (2절 넷째 행에서 안 넣은 것이 hooks 하나로 줄었다. 이슈 204. 그 전 2026-09-21 신설. 이슈 189)

왜 이 스킬이 필요한가: 두 절차는 2026-09-10에 CLAUDE.md에서 harness/docs/10-15로 내려갔고 방아쇠는 CLAUDE.md의 포인터 두 줄뿐이었다. 10-15 4절이 그것을 유일한 방아쇠라고 적었다. 이 스킬은 그 파일들을 만질 때 열린다. 본문은 옮기지 않는다.

## 1. project-sync 반입

| 단계 | 누가 | 무엇 |
|---|---|---|
| 1 | 모델 | harness/out/import/ 아래에 반입 후보를 만든다 |
| 2 | 사람 | 후보를 확인한다 |
| 3 | 사람 | harness/project-sync/로 옮긴다. 모델은 이 폴더에 쓸 수 없다. settings.json의 deny가 막고 그것은 의도다 |
| 4 | 모델 | progress.md에 반입 행을 남긴다 |

따라 오는 제약 둘과 원문 사본과 전사본의 구분은 harness/docs/10-15 2절.

## 2. settings.json 수정

| 규칙 | 왜 |
|---|---|
| Bash와 PowerShell 두 문법을 두 벌로 적는다 | 한쪽만 고치면 그 규칙이 다른 쪽 경로에서 조용히 사라진다 |
| 경로 구분자는 슬래시 | allow는 명령 문자열 앞부분을 그대로 비교한다 |
| 고친 뒤 node harness/tools/check.mjs settings .claude/settings.json | 짝과 겹침을 센다 |
| deny와 allow와 ask와 defaultMode를 그렇게 정한 이유와 안 넣은 것(hooks)의 이유를 먼저 읽는다 | harness/docs/10-15 3절과 3-1절 |
