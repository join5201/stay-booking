---
name: round
description: 하네스 세션을 이어받는 첫 턴, 그리고 라운드의 계약과 생성과 평가 준비와 결정표와 반영 단계에 들어갈 때 쓴다. 위치를 판정하고(T1부터 T5) 단계 선택표에서 한 행을 골라 그 단계의 곁 파일을 연다. 평가는 이 세션이 하지 않는다. 기록 행은 progress-row, 결정을 구하는 답변은 approval-request가 맡는다.
---

# 라운드

최초 작성: 2026-09-21
최종 갱신: 2026-09-21 (신설. 이슈 189)

왜 이 스킬이 필요한가: 마스터 프롬프트 셋(harness/prompts/harness-ptcf-prompt.v2.md, dev-ptcf-prompt.md, dev-ptcf-prompt.v3.md)이 같은 위치 판정 T1부터 T10을 각자 품고 있었고 단계 선택표는 harness-ptcf-prompt.md 5절에 있었다. 사람이 첫 턴에 붙여야 열렸다. 이 파일이 그 공통부의 정본이고 원본 셋은 이력으로 남는다. 시점 상태(열린 Task, 확정 문서 판)는 여기 적지 않는다. 그것은 progress.md와 계약이 갖는다.

## 1. 첫 턴 위치 판정 (T1부터 T5)

| 순서 | 하는 일 | 어디서 |
|---|---|---|
| T1 | 경로를 확인한다 | CLAUDE.md 3절 |
| T2 | 이번 Task의 마지막 행을 읽는다. 파일의 마지막 행이 아니다. 세션이 여럿이라 마지막 행은 남의 Task일 수 있다 | node harness/tools/check.mjs state harness/state/progress.md --task 이름 |
| T3 | git status, git worktree list, 열린 이슈와 PR. HEAD와 origin/main의 차이. 다른 세션의 미커밋 파일이 있으면 그 파일은 손대지 않는다 | git fetch 뒤 git log --oneline HEAD..origin/main |
| T4 | 도는 Task를 정한다. 둘 이상 열려 있으면 사용자에게 묻는다. 임의로 고르지 않는다 | |
| T5 | 2절 선택표에서 위에서부터 처음 맞는 행을 고르고 고른 근거를 첫 줄에 한 줄로 밝힌다 | |

확인 결과를 표로 낸 뒤 시작한다. 확인 없이 이어서 진행하지 않는다. 조사가 길어지면 실행 계획을 먼저 한 줄로 말한다.

## 2. 단계 선택표

정본이다. harness-ptcf-prompt.md 5절(문서 Task용)에서 옮기고 코드 Task 아홉 라운드의 실적(요청문, 대상 목록, 작업 트리 검사 스크립트)을 반영했다. 판정은 파일 존재와 progress.md의 그 Task 마지막 행과 그 산출물 판에 대한 검사 성공 기록 셋으로 한다. 폴더가 있다는 것이 그 단계가 끝났다는 뜻은 아니다.

| 조건 (위에서부터 처음 맞는 행) | 하는 일 | 여는 곁 파일 | 정지점과 종료 문장 |
|---|---|---|---|
| progress 마지막 행이 done | 끝난 Task다. 다음 Task를 묻는다 | 없음 | 정지. "이 Task는 done이다. 다음 Task를 지정해 달라." |
| 산출물은 있는데 그 판에 대한 검사 성공 기록이 progress에 없음 | 중단 지점이다. 안 돈 검사부터 다시 돌린다. 앞 단계를 다시 만들지 않는다 | 없음 | 정지. "중단 지점 확인. 그 검사부터 재개할지 확인을 기다린다." |
| harness/tasks/에 이 Task의 계약이 없음 | 계약 초안 | [contract.md](contract.md) | 승인 대기. "계약 초안 완료. 승인을 기다린다." |
| 계약 승인됨, 산출물 없음 | 생성. 문서는 generate.md 양식대로 후보를 만들고 g1 doc, 코드는 계약 8절의 단계와 9절의 명령대로 만들고 g1 code. 형식 보정은 최초 검사 뒤 최대 1회 | 없음. 정본은 harness/prompts/generate.md와 그 계약 | "생성 완료. G1 통과. 평가 준비로 갈지 확인을 기다린다." 2회 실패면 halted |
| G1 통과, harness/reviews/에 이 라운드의 A와 B 리포트 없음 | 평가 준비. 대상 목록, 계약 5절 기입과 fill, 요청문 A와 B, 작업 트리 검사 스크립트 | [eval-prep.md](eval-prep.md), [request-skeleton.md](request-skeleton.md) | 정지. "요청문 두 벌과 검사 PASS. Codex 새 작업 둘에 붙여 넣고 리포트를 harness/reviews/에 넣어 달라." |
| reviews에 A와 B 있음, harness/decisions/에 결정표 없음 | 결정표 초안. 원본 지적을 한 행씩 옮기고 결정과 이유 열은 비운다 | [decision.md](decision.md) 1절 | 정지. "결정표 준비 완료. 결정과 이유를 채워 달라." |
| 결정표에 빈 결정 없음, 그 판에 대한 g2 pre 성공 기록 없음 | g2 --mode pre. 결정표를 고치면 다시 돌린다 | [decision.md](decision.md) 2절 | 통과 "G2 통과. 반영으로 갈지 확인을 기다린다." 실패 "G2 실패 N개. 결정표 수정을 기다린다." |
| g2 pre 통과, 반영본 없음 | 수용 항목만 반영. 지적 ID별 변경 위치, 미수용 무변경 diff, apply-report | [decision.md](decision.md) 3절 | 승인 대기. "반영 후보 완료. 승인을 기다린다." |
| R1 반영 승인됨, R2 없음, 사용자가 재평가를 요청 | 평가 준비 R2. 확인 항목은 중립 문장으로 | [eval-prep.md](eval-prep.md) 4절 | 정지. 위 전달 문장 |
| R2까지 끝났거나 사용자가 재평가 불필요를 선언, 최종 확인 표가 안 참 | 최종 확인 표를 채우고 g2 --mode final. done은 쓰지 않는다 | [decision.md](decision.md) 4절 | 정지. "최종 확인 표 완료. 완료 판단을 기다린다." |
| g2 final 통과, 사용자가 완료를 확정 | done 행. 확정본을 제자리로. 병합 전이면 그 브랜치 마지막 커밋에, 병합 뒤면 다음 작업 브랜치 첫 커밋에 | progress-row 스킬 | "Task 종료." |

개발 Task의 코드 단계(뼈대, 도메인, 테스트, API, 화면, 연결 테스트, 검증 표)는 그 계약 8절이 정한다. 이 표는 그 단계들을 생성 한 행으로 본다. 단계마다 정지하고 단계마다 기록 행 하나인 것은 같다.

## 3. 선택 규칙

| 규칙 | 왜 |
|---|---|
| 한 턴에 한 행. 정지점에서 사용자 입력 없이 다음 행으로 넘어가지 않는다 | 승인이 게이트다 |
| 어느 행의 입력 파일이라도 없으면 그 행을 고르지 않고 없는 파일 목록을 보고한다 | 없는 것을 지어내지 않는다 |
| 형식 보정 1회와 재평가 1회는 별개 카운터다. 초과하면 halted로 기록하고 정지한다 | 횟수 소진은 완료 근거가 아니다 |
| 평가는 이 세션이 하지 않는다. 요청문을 만들고 멈춘다. 리포트가 harness/reviews/에 들어와야 다음 행이 열린다 | 10-4 1절. 블라인드 평가 |
| 모든 행의 끝에 progress.md 한 행. 실제 시간을 적는다 | 다음 턴의 판정 근거다(HR6) |
| g1 재요청에는 실패 목록만 넣는다. R1의 평가 기록을 R2 요청에 넣지 않는다 | HR3. 형식 보정과 재평가는 다른 것이다 |
| 문서가 틀렸다고 판단되면 어느 행에서든 멈춘다. 코드를 문서에 맞추지도 문서를 코드에 맞추지도 않는다 | 정본 관계를 뒤집지 않는다 |

## 4. 매 턴 규칙 (T6부터 T10)

| 순서 | 하는 일 |
|---|---|
| T6 | 직전 산출물과 이번 입력을 절대경로로 명시하고 시작한다 |
| T7 | 기존 파일을 고치기 전에 diff 요약을 보인다. 새 파일은 승인 없이 만든다. 삭제는 하지 않는다 |
| T8 | 결론이 안 나는 항목은 미결로 남기고 어디서 닫히는지 적는다. 개발 Task는 코드 덩어리마다 그것을 낳은 설계 문서의 위치를 적고, 못 찾으면 그 코드를 쓰지 않는다 |
| T9 | 단계가 끝나면 progress.md에 한 행. progress-row 스킬 |
| T10 | main에 바로 커밋하지 않는다. 이슈, 브랜치, 최소 커밋, 검사, origin/main 병합과 union 검사, PR. CLAUDE.md 4-1 |

## 5. 정본

| 무엇 | 어디 |
|---|---|
| 계약 양식 | harness/prompts/task-contract.md |
| 생성 요청 양식 | harness/prompts/generate.md |
| 평가 요청 양식 | harness/prompts/evaluate.md |
| 결정표 양식 | harness/prompts/decision-table.md |
| 반영 요청 양식 | harness/prompts/apply.md |
| 평가 기준 | harness/prompts/eval-criteria-ddd.md, eval-criteria-code.md |
| 실행 방식 | harness/docs/10-4 1절과 2절 |
| 원본 선택표 (이력) | harness/prompts/harness-ptcf-prompt.md 5절, dev-ptcf-prompt.md 2절, dev-ptcf-prompt.v3.md 3절 |
