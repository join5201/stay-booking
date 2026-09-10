# harness/docs/

무엇이 들어가는가: 하네스 엔지니어링 문서다. 하네스를 왜 이렇게 만들었는지의 근거와 결정 이력.

왜 분리하는가: document/는 O2O 서비스의 베이스 설계 문서 자리다. 하네스 문서는 대상이 다르다. 서비스가 아니라 서비스를 만드는 절차를 다룬다. 섞어 두면 평가자에게 넘길 파일을 고를 때 매번 성격을 다시 판단해야 하고, 계획 문서가 평가 입력에 섞이는 사고가 난다(harness/prompts/eval-criteria-ddd.md 8절 첫 항목).

세 디렉터리의 성격 구분

| 경로 | 성격 |
|---|---|
| document/ | O2O 서비스의 베이스 설계 문서. 평가 대상이 되는 것 |
| harness/prompts/ | 실행 중 읽는 양식과 평가 기준. 매 라운드 사용 |
| harness/docs/ | 하네스 설계 근거와 결정 이력. 참고용. 평가 입력이 아니다 |

현재 파일

| 파일 | 내용 |
|---|---|
| 10-4-o2o-harness-workflow-decisions.md | 실행 방식 결정 v4 |
| 10-5-o2o-harness-handoff.md | 작업 인계 v2 |
| 10-6-o2o-harness-implementation-plan.md | 실제 적용과 구현 계획 v2 |
| 10-7-o2o-harness-pdf-crosscheck.md | PDF 8종 대조 |
| 10-8-o2o-harness-recheck.md | 로컬 문서 기준 재대조 |
| 10-9-o2o-harness-answer-format-plan.md | 답변 형식 R1부터 R4의 적용 계획 v2. 2026-09-08 사용자 승인 (2026-09-08 추가) |
| 10-10-o2o-harness-worktree-split.md | 세션별 worktree 분리. 배치와 절차와 남는 제약 (2026-09-09 추가) |
| 10-11-o2o-board-transcription-check.md | 09-1 전사본과 FigJam 정본 대조. 전 항목 일치 (2026-09-09 추가) |
| 10-13-o2o-harness-reference-crosscheck.md | 하네스 대조 상대 다섯. 별 순 선정 근거와 대조 지점 표 (2026-09-09 추가) |
| 10-14-o2o-harness-crosscheck-promptfoo.md | promptfoo 대조 1건과 나머지 넷의 대조 방법. 축 다섯과 판정 (2026-09-09 추가) |

주의: 여기 있는 결정은 이력이다. 결정의 정본은 harness/decisions/다(harness/decisions/README.md).
