# CLAUDE.md (backend 규칙의 입구)

최초 작성: 2026-09-10
최종 갱신: 2026-09-11 (2절부터 8절을 backend/.claude/rules/ 일곱 파일로 나눔. 이슈 116. 사용자 요청)
독자: backend/ 아래에서 일하는 Claude Code와 코딩 에이전트

왜 이 파일이 필요한가: 루트 CLAUDE.md는 하네스 규칙이라 백엔드 코드를 어디부터 읽어야 하는지, 층마다 무엇을 하는지, 지금 어느 단계인지를 적지 않는다. 그래서 세션마다 사람이 그것을 말로 설명하게 된다. 이 파일과 `.claude/rules/`가 그 설명을 대신한다.

루트 CLAUDE.md는 그대로 적용된다. 이 파일은 그것을 덮지 않고 backend 몫만 더한다. 충돌하면 루트가 우선이다.

왜 나눴나: 한 파일이 143줄까지 자랐고 평가 반영 라운드마다 절이 는다. 공식 문서는 CLAUDE.md 하나를 200줄 아래로 두고 큰 프로젝트는 `.claude/rules/`에 주제별 파일로 나누라고 적는다. 절 번호는 옮기면서 그대로 뒀다. 커밋 메시지와 반영 기록이 backend/CLAUDE.md 3-3절이나 T1 같은 번호로 인용하고 있어서다.

확인한 출처 (2026-09-11 확인)

| 내용 | URL |
|---|---|
| 하위 디렉터리의 CLAUDE.md는 그 디렉터리의 파일을 읽을 때 로드된다. 파일 하나는 200줄 아래 | https://code.claude.com/docs/en/memory |
| `.claude/rules/`는 주제별 md 하나씩. paths 없는 규칙은 CLAUDE.md와 같은 우선순위. 중첩된 `.claude/rules/`는 필요할 때 로드된다. 백틱으로 감싼 경로는 import가 아니다 | https://code.claude.com/docs/en/memory |
| 패키지별 CLAUDE.md와 `.claude/` 배치. 로드 확인은 /context의 Memory files | https://code.claude.com/docs/en/large-codebases |

## 1. 시작할 때 읽는 순서

세 파일이면 지금 무엇을 하는지가 잡힌다. 이 순서로 읽는다. 경로는 저장소 루트 기준이다.

| 순서 | 파일 | 무엇을 얻나 |
|---|---|---|
| 1 | `harness/tasks/task-S9-{기능키}.md` | 지금 묶음의 범위, 단계 표(8절), 테스트 목록(8-1절) |
| 2 | `harness/state/progress.md`의 그 Task 마지막 행 | 어디까지 했고 다음이 무엇인지 |
| 3 | `document/11-o2o-api-spec.md`의 해당 API 절 | 경로, 필드, 상태 코드, 오류 코드 |

기능키가 무엇인지는 `harness/docs/10-6` 3절 기능별 API와 검증 연결 표의 행 이름이다. 진행 중인 계약은 `harness/tasks/`에서 가장 최근 것이다.

파일이 여럿이면 마지막 행이 아니라 그 Task의 마지막 행을 읽는다. progress.md는 세션 여럿이 같이 쓰고 순서가 보장되지 않는다.

## 2. 규칙 파일

`backend/.claude/rules/` 아래 일곱이다. 각 파일이 한 주제다. 절 번호는 이 파일에 있던 때의 번호다.

| 파일 | 절 | 무엇 |
|---|---|---|
| `answer.md` | 2 | 답변 규칙 B1. 결과 표 다음에 남은 단계 표 |
| `layers.md` | 3, 3-1, 3-2, 3-3 | 층 넷의 역할, 리포지토리 두 파일, shared 기준, 이벤트 구독은 AFTER_COMMIT |
| `build-order.md` | 4 | 묶음 하나를 아래에서 위로. 한 턴에 한 단계 |
| `testing.md` | 5 | T1부터 T6. 짝, 경계, 실제 MySQL, 상대 날짜, 고정 Clock, 상태 직접 놓기 |
| `commands.md` | 6 | JDK 21, Docker, gradle, 게이트 명령. 결과 사본 경로 |
| `dev-auth.md` | 7 | X-Dev-Actor-Id 행위자 다섯 |
| `do-not.md` | 8 | BN1부터 BN7 |

`AGENTS.md`는 여기 넣지 않는다. Codex가 읽는 평가자 규칙이라 Claude Code의 `.claude/`와 다른 도구의 파일이다.

로드 확인: 대화형 세션에서 backend/ 아래 파일을 하나 읽은 뒤 /context의 Memory files에 위 일곱이 보이면 된다. 안 보이면 이 파일 2절의 표가 경로를 가리키니 직접 연다.
