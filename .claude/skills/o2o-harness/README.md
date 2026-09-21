# .claude/skills/o2o-harness/

최초 작성: 2026-09-21
최종 갱신: 2026-09-21 (스킬 넷 전부 있음. 그 전 같은 날 round, 신설. 이슈 189)

무엇이 들어가는가: 하네스의 반복 절차를 여는 스킬 묶음이다. .claude-plugin/plugin.json 한 장이 이 폴더를 o2o-harness라는 플러그인으로 만들고, skills/ 아래 SKILL.md 하나하나가 각자의 여는 조건을 가진 스킬이다.

왜 이 폴더가 필요한가: 절차는 harness/docs와 harness/prompts와 기록 파일 머리에 있었지만 언제 여는지의 선언이 없어 사람이 붙이거나 모델이 기억해야 열렸다(harness/docs/10-14 6-3절 B2, 10-15 4절). 스킬은 파일 머리의 description에 여는 조건을 적고 Claude Code가 상황을 보고 스스로 연다. 낱개 스킬 대신 폴더 하나로 묶은 것은 2026-09-21 사용자 결정이다.

## 구성

| 경로 | 무엇 | 상태 |
|---|---|---|
| .claude-plugin/plugin.json | 묶음 선언. name이 o2o-harness | 있음 |
| README.md | 이 파일 | 있음 |
| skills/round/ | 세션 재개 T1부터 T5와 단계 선택표. 곁 파일 contract.md, eval-prep.md, request-skeleton.md, decision.md | 있음 |
| skills/progress-row/ | 기록 파일 셋에 행을 더할 때. paths harness/state | 있음 |
| skills/approval-request/ | 결정 쟁점을 구하는 턴의 여섯 칸 양식 | 있음 |
| skills/guarded-paths/ | project-sync와 settings.json을 만질 때. paths 셋 | 있음 |

## 규칙

| 규칙 | 왜 |
|---|---|
| SKILL.md는 절차와 여는 조건만 적고 규칙 본문은 정본을 링크한다 | 정본이 둘이면 갈라진다(harness/docs/10-14 4절) |
| description은 여는 조건을 첫 문장에 적는다 | 목록에 실릴 때 앞부분만 남는다. 1,536자에서 잘린다 |
| SKILL.md는 500줄 이하. 긴 것은 곁 파일로 | 공식 문서 권고. 본문은 열릴 때만 문맥에 실린다 |
| 문체는 CLAUDE.md 2절 그대로 | 이 폴더도 g1 doc의 대상이다. check.mjs SCOPE에 harness-doc 성격으로 들어 있다 |
| 검사는 스킬 폴더마다 node harness/tools/check.mjs sweep 폴더 --type doc | sweep은 한 폴더만 본다. 이 묶음은 폴더 다섯이다 |
| 평가 입력이 아니다 | Codex는 이 폴더를 읽지 않는다(AGENTS.md E6) |

## 동작 조건 (공식 문서 2026-09-21 확인)

| 항목 | 내용 |
|---|---|
| 읽히는 자리 | 세션의 주 작업 디렉터리의 .claude/skills. 저장소 루트에서 세션을 열어야 한다. backend나 frontend에서 열면 이 묶음은 안 읽힌다 |
| 설치 | 없다. 다음 세션부터 그 자리에서 읽힌다. 폴더 신뢰 뒤에만 |
| 부르는 이름 | /o2o-harness:이름. 다른 스킬과 겹치지 않으면 /이름으로도 |
| 반영 | SKILL.md 수정은 그 세션에 바로 걸린다. 훅 같은 다른 부품은 다시 읽기나 재시작 |
| 끄는 법 | 폴더를 tmp/_moved/로 옮기거나 이름으로 끈다. 제거 명령은 없다 |
