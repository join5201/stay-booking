# 평가 준비

최초 작성: 2026-09-21
최종 갱신: 2026-09-21 (신설. 이슈 189)

왜 이 파일이 필요한가: 라운드 아홉 번을 매번 이전 라운드 폴더를 베껴 만들었고 순서를 적은 파일은 harness/out/mvp-eval-2026-09-14/README.md 6절뿐이었다. 그 README도 그 라운드 자체를 적은 파일은 없다고 적었다. 요청문 양식은 harness/prompts/evaluate.md이고 여기서는 그것을 실제 라운드 폴더로 만드는 순서를 적는다.

## 1. 전제 (바뀌지 않는 것)

| 항목 | 값 | 근거 |
|---|---|---|
| 전달 | 사람이 Codex 새 작업에 요청문을 붙인다. A와 B는 서로 다른 새 작업. 한 작업에서 둘 다 하지 않는다 | 10-4 1절 |
| 역할 | A는 그 작업의 기능과 계약, B는 공통 품질. 축은 기준 파일이 정한다 | evaluate.md 머리 |
| 평가 기준 | 문서는 harness/prompts/eval-criteria-ddd.md, 코드는 eval-criteria-code.md | |
| 허용 입력 | 계약 5절 표(HR1). 해시는 fill이 채운 sha256(HR2) | task-contract.md |
| 읽지 않는 것 | 01 전체, 이전 리포트, 결정표, harness/docs, harness/state, harness/decisions, harness/reviews, 생성 대화, 생성자의 검증 표(stepN/verification.md). R2도 같다 | evaluate.md 3항, N3 |
| 지적 ID | S{Step}-R{라운드}-{A 또는 B}-{원본 번호 2자리} | CLAUDE.md 3절 |
| 평가자의 작업 폴더 | 별도 트리(o2o-dev). 본 트리는 세션 여럿이 브랜치를 바꾼다 | 10-10 3절 |
| 결과 | 리포트는 harness/reviews/task-S{Step}-{키}-R{n}-A.md와 -B.md. 그 파일 하나만 만든다 | |

## 2. 순서

| 순서 | 하는 일 | 산출물 | 근거 |
|---|---|---|---|
| 1 | 기준 커밋을 정한다. origin/main의 머리 또는 그 라운드 PR의 병합 커밋. 전체 테스트 건수는 평가 시점 origin/main의 값으로 적는다 | 커밋 해시 | 결제 R1 A-05 교훈 |
| 2 | 대상 목록. git ls-files와 sha256 앞 16자리. 경로, 성격, 줄 수, 해시. 다른 묶음이 고친 파일은 그 사실을 적는다. 평가자 규칙 파일(AGENTS.md)은 대상이 아니다 | harness/out/task-S{Step}-{키}-R{n}/eval-target-files.md | 이슈 87 |
| 3 | 계약 5절 평가 대상 행에 목록 경로와 해시 기입, fill 재실행 | 계약 | HR2 |
| 4 | 요청문 A와 B. [request-skeleton.md](request-skeleton.md)의 7절을 채운다. 출력 절에 보조 표(원본 번호, 지적 ID, 심각도, 위반 기준)를 요구한다 | eval-request-A.md, eval-request-B.md | evaluate.md 5-1항 |
| 5 | 작업 트리 검사 스크립트. 폴더가 이 저장소인가, 기준 커밋이 HEAD의 조상인가, 규칙 파일이 있는가, 대상이 전부 있는가, 내용이 해시 그대로인가, 미커밋이 없는가, 요청문의 자료 해시가 실제와 같은가. 블라인드 금지 파일의 존재는 알림만 낸다 | verify-eval-workspace.mjs | 이슈 87 |
| 6 | 코드 라운드는 테스트 DB를 A 세션마다 다르게 배정한다. 테스트 설정의 create-drop이 같은 DB에서 서로의 표를 지운다 | 요청문 5절 | mvp-eval README D-6 |
| 7 | 기록 행, 커밋(이유 하나씩), origin/main 병합과 union 검사, PR. 사용자에게 붙여 넣기 순서를 답변으로 | progress.md, PR | CLAUDE.md 4-1 |
| 8 | 정지. 평가는 이 세션이 하지 않는다. 리포트가 harness/reviews/에 들어오면 [decision.md](decision.md) | | |

## 3. 본보기

| 무엇 | 어디 |
|---|---|
| 코드 라운드 한 벌 | harness/out/task-S9-frontend-R1/ (요청문 둘, 목록, 검사 스크립트) |
| 라운드 계획과 결정 6건 | harness/out/mvp-eval-2026-09-14/README.md |
| 문서 라운드 | harness/out/task-S8-R1/ (evaluate-A.md, evaluate-B.md, eval-input/) |

## 4. R2

| 규칙 | 왜 |
|---|---|
| 이전 리포트와 결정표는 R2도 읽지 않는다 | 블라인드가 유지된다 |
| 확인 항목은 R1 지적을 중립 문장으로 옮긴다. 출처는 결정표의 반영과 최종 확인 인계 표 | 옮기는 사람의 해석이 들어가므로 사용자가 대조할 수 있게 출처를 남긴다 |
| 축은 R1과 같이 전부 다시 본다. 반영 diff만 보지 않는다 | 반영 뒤 다른 묶음이 같은 파일을 또 고쳤을 수 있다 |
| 대상은 반영본. 반영 뒤 바뀐 파일도 목록에 넣는다 | 지금 코드를 판정해야 한다 |
| 재평가는 최대 1회. 소진은 완료 근거가 아니다 | 10-4 2절 |
