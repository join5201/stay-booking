# .claude/skills/

최초 작성: 2026-09-21
최종 갱신: 2026-09-22 (플러그인 묶음을 풀어 스킬 넷을 이 폴더 바로 아래로. 동작 조건 절 다시 씀. 이슈 192. 그 전 2026-09-21 스킬 넷 전부 있음, 신설. 이슈 189)

무엇이 들어가는가: 하네스의 반복 절차를 여는 프로젝트 스킬 넷이다. 폴더마다 SKILL.md 하나가 자기 여는 조건을 머리의 description과 paths에 적는다. 2026-09-21에는 이 넷을 o2o-harness라는 플러그인 묶음(.claude-plugin/plugin.json 한 장과 skills/ 아래 넷)으로 두었는데, 2026-09-22에 묶음을 풀었다. 이유는 동작 조건 절에 있다.

왜 이 폴더가 필요한가: 절차는 harness/docs와 harness/prompts와 기록 파일 머리에 있었지만 언제 여는지의 선언이 없어 사람이 붙이거나 모델이 기억해야 열렸다(harness/docs/10-14 6-3절 B2, 10-15 4절). 스킬은 파일 머리의 description에 여는 조건을 적고 Claude Code가 상황을 보고 스스로 연다.

## 구성

| 경로 | 무엇 | 상태 |
|---|---|---|
| README.md | 이 파일 | 있음 |
| round/ | 세션 재개 T1부터 T5와 단계 선택표. 곁 파일 contract.md, eval-prep.md, request-skeleton.md, decision.md | 있음 |
| progress-row/ | 기록 파일 셋에 행을 더할 때. paths harness/state | 있음 |
| approval-request/ | 결정 쟁점을 구하는 턴의 여섯 칸 양식 | 있음 |
| guarded-paths/ | project-sync와 settings.json을 만질 때. paths 셋 | 있음 |

옛 플러그인 선언 plugin.json은 tmp/_moved/claude-skills-o2o-harness/에 있다(CLAUDE.md N2).

## 규칙

| 규칙 | 왜 |
|---|---|
| SKILL.md는 절차와 여는 조건만 적고 규칙 본문은 정본을 링크한다 | 정본이 둘이면 갈라진다(harness/docs/10-14 4절) |
| description은 여는 조건을 첫 문장에 적는다 | 목록에 실릴 때 앞부분만 남는다. 1,536자에서 잘린다 |
| SKILL.md는 500줄 이하. 긴 것은 곁 파일로 | 공식 문서 권고. 본문은 열릴 때만 문맥에 실린다 |
| 문체는 CLAUDE.md 2절 그대로 | 이 폴더도 g1 doc의 대상이다. check.mjs SCOPE에 harness-doc 성격으로 들어 있다 |
| 검사는 스킬 폴더마다 node harness/tools/check.mjs sweep 폴더 --type doc | sweep은 한 폴더만 본다. 이 README는 .claude/skills를 sweep한다 |
| 평가 입력이 아니다 | Codex는 이 폴더를 읽지 않는다(AGENTS.md E6) |
| 플러그인 묶음으로 다시 싸지 않는다 | 아래 동작 조건. 묶으면 데스크톱 앱 세션이 읽지 않는다 |

## 동작 조건 (공식 문서 2026-09-22 확인)

2026-09-21의 플러그인 묶음은 데스크톱 앱 세션에서 넷 다 스킬 목록에 오르지 않았다. 폴더 구조는 맞았고(claude plugin validate 통과) 읽는 쪽이 문제였다. 프로젝트 범위 skills-dir 플러그인은 대화형 세션의 폴더 신뢰 창을 누른 뒤에만 읽히는데, -p 실행과 SDK 세션은 그 창을 띄우지 않고 읽지도 않는다. 데스크톱 앱 세션은 SDK 세션이다. ~/.claude.json의 hasTrustDialogAccepted가 true여도 같다. 앱이 쓰는 claude.exe로 루트에서 claude plugin list를 돌리면 skipped because this workspace was not trusted when plugins were scanned가 나온다.

| 항목 | 내용 |
|---|---|
| 읽히는 자리 | 세션의 주 작업 디렉터리와 그 상위의 .claude/skills. 저장소 루트에서 열면 넷이 다 읽히고, backend나 frontend에서 열어도 상위로 올라가 읽는다 |
| 데스크톱 앱 세션 | 읽힌다. SDK의 project 설정 원천이 프로젝트 스킬을 읽는다. 2026-09-22 실제 확인. paths가 없는 둘(round, approval-request)은 이동 직후 같은 세션 목록에 올랐고, paths가 있는 둘은 그 경로의 파일을 만질 때 오른다 |
| 설치 | 없다. 폴더가 있으면 읽힌다 |
| 부르는 이름 | /round, /progress-row, /approval-request, /guarded-paths. 플러그인 접두 o2o-harness:는 없어졌다 |
| 반영 | SKILL.md 수정과 스킬 폴더 추가와 삭제는 그 세션에 바로 걸린다. .claude/skills 폴더 자체가 세션 시작 뒤에 생겼으면 재시작 |
| 끄는 법 | 폴더를 tmp/_moved/로 옮긴다. 제거 명령은 없다 |
| 확인 명령 | 앱의 claude.exe(경로는 환경 변수 CLAUDE_CODE_EXECPATH)로 루트에서 claude plugin list. skipped 경고가 없어야 한다 |

근거 위치

| 내용 | 위치 |
|---|---|
| a project @skills-dir plugin 행이 SDK 세션 칸에서 Not used, and no dialog is offered | https://code.claude.com/docs/en/permissions 의 What runs before you trust a folder 표 |
| Load only after accepting the workspace trust dialog. Trusting a parent folder or running with -p is not sufficient | https://code.claude.com/docs/en/plugins-reference 의 Skills-directory plugins 절 |
| project 설정 원천이 project skills, commands, and subagents를 읽는다 | https://code.claude.com/docs/en/agent-sdk/claude-code-features 의 settingSources 표 |
| paths 칸은 플러그인 밖 프로젝트 스킬에서도 동작한다. 스킬 폴더의 추가와 수정은 세션 중에 반영된다 | https://code.claude.com/docs/en/skills |
