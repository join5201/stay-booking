# harness/prompts/

최초 작성: 2026-09-08
최종 갱신: 2026-09-08 (신설)

무엇이 들어가는가: 실행 중에 읽는 양식과 기준이다. 매 라운드 열어 채우거나 그대로 인용한다.

왜 이 README가 필요한가: 이 폴더에만 평가 입력이 되는 파일과 되지 않는 파일이 섞여 있다. 평가 폴더를 만들 때 여기서 무엇을 뽑을지 매번 판단하면 언젠가 양식이나 마스터 프롬프트가 평가자에게 넘어간다. 그것이 평가 기준 8절의 첫 금지 항목이다. 아래 표의 마지막 칸이 그 판단을 대신한다.

## 파일

| 파일 | 무엇 | 평가 입력이 될 수 있나 |
|---|---|---|
| eval-criteria-ddd.md | 설계 문서 평가 기준 v1. ddd-blind-review 스킬 원본 발췌 | 된다 |
| eval-criteria-code.md | 코드 작업 평가 기준 v0 초안. 첫 코드 Task로 검증한 뒤 v1로 올린다 | 된다 |
| task-contract.md | 작업 계약 빈 양식 v5 | 계약 자체는 평가 입력이다. 이 빈 양식은 아니다 |
| generate.md | 생성 요청 양식 v3 | 안 된다 |
| evaluate.md | Codex 평가 요청 양식 v4 | 안 된다 |
| decision-table.md | 사용자 결정표 양식 v4 | 안 된다 |
| apply.md | 반영 요청 양식 v3 | 안 된다 |
| answer-format.md | 답변 형식 규격 v2. 등급과 골격과 게이트 | 안 된다 |
| 07-o2o-ptcf-prompt.v3.md | O2O 설계 문서용 마스터 프롬프트 v3 후보 | 안 된다 |
| harness-ptcf-prompt.md | 하네스 준비 단계용 마스터 프롬프트 v2 | 안 된다 |
| harness-ptcf-prompt.v2.md | 하네스 실행 단계용 마스터 프롬프트 v2. 세션 인계용 | 안 된다 |
| dev-ptcf-prompt.md | 개발 세션용 프롬프트 v1 | 안 된다 |

한 줄 규칙: 이 폴더에서 평가자에게 넘기는 것은 eval-criteria-*.md 둘뿐이다.

task-contract.md 행을 풀어 적는다. 빈 양식은 평가 입력이 아니고, 그 양식을 채워 harness/tasks/에 놓은 계약은 평가 입력이다. 평가자가 판정 기준으로 삼는 것은 승인된 계약이지 양식이 아니다.

## 양식과 기준의 차이

| 성격 | 무엇 | 누가 채우나 |
|---|---|---|
| 양식 | 빈칸이 있는 틀. contract, generate, evaluate, decision-table, apply | 사람과 모델이 채운다 |
| 기준 | 채우지 않고 그대로 인용하는 것. eval-criteria 둘, answer-format | 아무도 채우지 않는다 |
| 마스터 프롬프트 | 세션 첫 턴에 붙이는 것. 07, harness-ptcf 둘, dev-ptcf | 사람이 붙인다 |

버전 표기가 둘로 갈리는 것도 이 성격 차이에서 온다. 양식은 버전 한 줄만 두고, 기준과 마스터 프롬프트는 문서라서 최초 작성일과 최종 갱신일 두 줄을 둔다.

## 여기 없는 것

채운 결과물은 여기 두지 않는다. 계약은 harness/tasks/, 생성 후보와 평가 요청은 harness/out/, 결정표는 harness/decisions/, 리포트는 harness/reviews/다.

프로젝트 계열 원본 사본도 여기 두지 않는다. harness/project-sync/다. 07 v2 원본이 거기 있고 이 폴더의 07 v3은 그것을 입력으로 만든 후보다.
