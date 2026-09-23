# harness/

최초 작성: 2026-09-08
최종 갱신: 2026-09-23 (지금 상태 표의 설계 Step 9 행과 백엔드 행을 main 기준으로. 이슈 194. 그 전 같은 날 지금 상태 표 하네스 행의 스킬 경로와 링크, PR 193. 이슈 192. 그 전 2026-09-21 지금 상태 표의 하네스 행에 스킬 폴더 o2o-harness. PR 190. 같은 날 앞서 루트 README의 내용을 이 파일로 옮겼다. 옛 판에서 무엇이 들어가는가, 왜 한 폴더로 묶는가, 폴더 일곱의 평가 입력 칸, 한 줄 규칙, 여기 없는 것, 디렉터리 개편 이력을 가져왔다. 링크는 이 폴더 기준으로 고쳤다. 이슈 74 문단은 해결되어 뺐다. 두 파일의 앞 판 갱신 이력은 git log)

무엇이 들어가는가: 하네스 전부다. 파일 규약, 검사 스크립트, 기록 규칙, 프롬프트 양식, 그리고 그것들을 그렇게 정한 근거.

왜 한 폴더로 묶는가: 루트에 하네스 디렉터리가 아홉 개 흩어져 있으면 backend와 frontend가 붙었을 때 무엇이 하네스인지 이름으로 갈리지 않는다. 평가에 넘길 파일을 고를 때마다 디렉터리 성격을 다시 판단하게 되고, 그러다 계획 문서가 평가 입력에 섞인다. 그것이 평가 기준 8절의 첫 금지 항목이다.

## 빠른 찾기

경로를 외우지 않고 하려는 일로 찾는다. 이 표가 이 파일의 본체다.

| 하려는 일 | 열 파일 |
|---|---|
| 지금 어디까지 왔는지 본다 | [progress.md](state/progress.md). 그 Task의 마지막 행이 현재 위치다 |
| 하네스가 어떻게 도는지 한눈에 본다 | [10-17 도해](docs/10-17-o2o-harness-overview.md). 아래 그림 셋이 그 문서의 것이다 |
| 생성 세션의 규칙을 본다 | [CLAUDE.md](../CLAUDE.md) |
| 평가 세션의 규칙을 본다 | [AGENTS.md](../AGENTS.md) |
| 답변 형식과 등급을 본다 | [answer-format.md](prompts/answer-format.md) |
| 평가자에게 무엇을 넘길지 고른다 | [evaluate.md](prompts/evaluate.md)의 허용 입력 표 |
| 작업 계약을 쓴다 | [task-contract.md](prompts/task-contract.md). 지난 계약은 [tasks/](tasks/README.md) |
| 검사를 돌린다 | [tools/README.md](tools/README.md) |
| 이슈와 브랜치와 커밋 순서를 본다 | [CLAUDE.md](../CLAUDE.md) 4-1절 |
| worktree를 열거나 닫는다 | [10-10](docs/10-10-o2o-harness-worktree-split.md) 4절과 8절 |
| 결정이 무엇이었는지 찾는다 | [decisions/](decisions/README.md) |
| 같은 사고가 전에 났는지 본다 | [troubleshooting.md](state/troubleshooting.md) |
| 설계의 핵심만 빨리 읽는다 | [06-6 다이제스트](../document/06-6-o2o-design-digest.md) |
| 용어의 뜻을 찾는다 | [05-3 컨텍스트별 용어 사전](../document/05-3-o2o-glossary.md) |
| API 규격을 본다 | [11 API 명세](../document/11-o2o-api-spec.md) |
| 백엔드를 띄운다 | [backend/README.md](../backend/README.md) |

## 그림 셋

한 라운드의 아홉 단계

![아홉 단계](docs/10-17-o2o-harness-overview-fig1.svg)

폴더 지도와 평가 입력 경계

![폴더 지도](docs/10-17-o2o-harness-overview-fig2.svg)

게이트

![게이트](docs/10-17-o2o-harness-overview-fig3.svg)

셋의 근거와 고치는 법은 [10-17](docs/10-17-o2o-harness-overview.md)에 있다. 규칙이 바뀌면 정본을 먼저 고치고 그림을 다시 그린다.

## 이 저장소가 담는 것

두 가지다. 서비스 설계 문서와, 그 문서를 만들고 검증하는 절차다.

하네스는 모델을 뺀 전부다. 파일 규약, 검사 스크립트, 기록 규칙, 프롬프트 양식이 하네스이고 LLM 호출은 사람이 두 앱에서 한다. 생성과 수정은 Claude Code, 평가는 Codex의 새 작업(A와 B 분리), 전달은 사용자다. 별도 LLM API를 호출하지 않는다.

| 항목 | 값 |
|---|---|
| 백엔드 | Spring 기반 Java, MySQL |
| 프론트 | Next.js. [frontend/](../frontend/README.md) |
| 범위 | 로컬 개발과 검증까지. 배포와 운영 제외 |
| 검사 스크립트 | Node |

## 규칙 파일

| 파일 | 독자 |
|---|---|
| [CLAUDE.md](../CLAUDE.md) | 생성자. Claude Code가 읽는다 |
| [AGENTS.md](../AGENTS.md) | 평가자. Codex가 읽는다 |
| [.claude/settings.json](../.claude/settings.json) | 권한 게이트. 비밀 파일 읽기와 삭제 명령을 막는다 |
| [.coderabbit.yaml](../.coderabbit.yaml) | 리뷰 봇. 언어와 문체 규칙과 경로별 지시문. 근거는 [10-16](docs/10-16-o2o-harness-coderabbit.md) |
| [backend/CLAUDE.md](../backend/CLAUDE.md), [backend/AGENTS.md](../backend/AGENTS.md) | 백엔드 전용 규칙 |

CLAUDE.md와 AGENTS.md 둘을 잇지 않았다. AGENTS.md의 소스 수정 금지 조항을 생성자가 따르면 생성 자체가 불가능해지기 때문이다.

## 여기 없는 것 (2026-09-21 옛 판에서 가져옴)

하네스의 강제 장치는 루트 .claude/settings.json에 있다. permissions의 deny와 allow가 그것이다. 도구가 그 경로에서만 읽기 때문에 여기로 옮길 수 없다. 나중에 붙일 훅(hooks)도 같은 파일이다.

규칙 문서 둘도 루트에 있다. CLAUDE.md는 Claude Code가 작업 디렉터리에서 읽고, AGENTS.md는 Codex가 Git 루트에서 현재 디렉터리까지 내려오며 읽는다. 둘 다 이 폴더 안에 두면 읽히지 않는다.

## 디렉터리 (2026-09-21 평가 입력 칸과 한 줄 규칙을 옛 판에서 가져옴)

세 디렉터리의 성격을 이름으로 가른다. 섞어 두면 평가에 넘길 파일을 고를 때마다 성격을 다시 판단해야 하고, 계획 문서가 평가 입력에 섞이는 사고가 난다.

| 경로 | 성격 | 평가 입력이 될 수 있나 |
|---|---|---|
| [document/](../document/README.md) | O2O 서비스 베이스 설계 문서 | 된다 |
| [prompts/](prompts/README.md) | 실행 중 읽는 양식과 평가 기준 | 기준 파일만 |
| [docs/](docs/README.md) | 하네스 설계 근거와 결정 이력 | 안 된다 |

라운드마다 쌓이는 폴더는 넷이다. 파일명 규약이 각 README에 있다.

| 경로 | 무엇이 쌓이나 | 평가 입력이 될 수 있나 |
|---|---|---|
| [tasks/](tasks/README.md) | 작업 계약 | 승인된 계약만 된다 |
| [out/](out/README.md) | 생성 후보와 형식 보정 이력 | 그 라운드의 평가 대상 후보만 된다 |
| [reviews/](reviews/README.md) | 평가 리포트 원문 | 안 된다 |
| [decisions/](decisions/README.md) | 결정의 정본 | 안 된다 |

나머지 셋이다.

| 경로 | 역할 | 평가 입력이 될 수 있나 |
|---|---|---|
| [tools/](tools/README.md) | 검사 스크립트 | 안 된다 |
| [state/](state/README.md) | 진행, 트러블슈팅, 누적 지식. 전부 추가 전용 | 안 된다 |
| project-sync/ | 프로젝트 계열 원본 사본. 수정 금지. 반입 절차는 [10-15](docs/10-15-o2o-harness-ondemand-rules.md) | 입력 팩만 된다. 쓰기는 금지다 |

한 줄 규칙: harness/ 아래에서 평가자에게 나가는 것은 넷뿐이다. 승인된 Task 계약, 그 라운드의 평가 대상 후보, harness/prompts/eval-criteria-*.md 둘, harness/project-sync/의 입력 팩. 나머지는 전부 평가 입력이 아니다.

계약이 평가 입력인 근거는 [evaluate.md](prompts/evaluate.md)의 허용 입력 표다. 승인된 Task 계약이 필수 행으로 있다. 평가자가 판정 기준으로 삼는 것이 계약이기 때문이다(HR1).

## 설계 문서

| 파일 | 무엇 |
|---|---|
| [01 계획](../document/01-o2o-ddd-plan.md) | DDD 설계 진행 계획. Step 정의의 출처 |
| [02 기능 목록](../document/02-o2o-feature-list.md) | 기능 목록 |
| [03 이벤트 스토밍](../document/03-o2o-event-storming.md) | 도메인 이벤트 |
| [04 커맨드와 액터](../document/04-o2o-commands-actors.md) | 책임과 협력 지도 |
| [05-1 유비쿼터스 언어](../document/05-1-o2o-ubiquitous-language.md) | 컨텍스트 간 용어 충돌 |
| [05-2 바운디드 컨텍스트](../document/05-2-o2o-bounded-contexts.md) | 컨텍스트 확정 |
| [05-3 용어 사전](../document/05-3-o2o-glossary.md) | 컨텍스트별 용어. 용어를 찾을 자리 |
| [05 용어 사전](../document/05-o2o-glossary.md) | Step 4 판. 05-3의 앞 판이다 |
| [06-1 컨텍스트 맵](../document/06-1-o2o-context-map.md) | 컨텍스트 사이의 관계 |
| [06-2 애그리거트](../document/06-2-o2o-aggregates.md) | 애그리거트 레퍼런스. [설명본](../document/06-2-o2o-aggregates-explained.md) |
| [06-3 애그리거트 검증](../document/06-3-o2o-aggregates-blind-review.md) | 06-2 초안 블라인드 리포트 |
| [06-4 계약과 정책](../document/06-4-o2o-contracts.md) | DbC 레퍼런스. [설명본](../document/06-4-o2o-contracts-explained.md) |
| [06-5 계약 검증](../document/06-5-o2o-contracts-blind-review.md) | Step 7 블라인드 기록 |
| [06-6 다이제스트](../document/06-6-o2o-design-digest.md) | 애그리거트와 CRC와 DbC 요약. 가장 빨리 읽는 자리 |
| [06 보드 범례](../document/06-o2o-board-legend.md) | 이벤트 스토밍 보드 범례 |
| [11 API 명세](../document/11-o2o-api-spec.md) | API 규격 |
| [12 문서 검토](../document/12-o2o-document-review.md) | 검토와 수정 내역. 안의 절대경로 링크는 2026-09-07 개편 이전 것이라 끊겨 있다 |
| [서비스 형태](../document/o2o-service-types.md), [공간과 숙박 형태](../document/o2o-space-stay-types.md) | 유형 정리 |

## 하네스 문서

번호가 크면 나중 것이다. 결정의 정본은 harness/decisions/이고 여기 있는 것은 근거와 이력이다.

| 파일 | 무엇 |
|---|---|
| [10-4 실행 방식](docs/10-4-o2o-harness-workflow-decisions.md) | 두 앱과 사람이 하는 일의 분담 |
| [10-5 인계](docs/10-5-o2o-harness-handoff.md) | 작업 인계 |
| [10-6 구현 계획](docs/10-6-o2o-harness-implementation-plan.md) | 적용과 구현 계획. N9 세로 진행의 근거 |
| [10-7 PDF 대조](docs/10-7-o2o-harness-pdf-crosscheck.md) | PDF 8종 대조 |
| [10-8 재대조](docs/10-8-o2o-harness-recheck.md) | 로컬 문서 기준 재대조 |
| [10-9 답변 형식 계획](docs/10-9-o2o-harness-answer-format-plan.md) | R1부터 R4의 적용 계획 |
| [10-10 worktree 분리](docs/10-10-o2o-harness-worktree-split.md) | 트리 배치와 여는 절차와 닫는 절차 |
| [10-11 보드 대조](docs/10-11-o2o-board-transcription-check.md) | 09-1 전사본과 FigJam 정본 대조 |
| [10-12 결함 수정 계획](docs/10-12-o2o-harness-fix-plan.md) | 하네스 결함 넷의 순서와 경계 |
| [10-13 대조 상대](docs/10-13-o2o-harness-reference-crosscheck.md) | 비교 대상 다섯의 선정 근거 |
| [10-14 대조 기록](docs/10-14-o2o-harness-crosscheck-promptfoo.md) | promptfoo와 superpowers와 moai-adk와 ECC |
| [10-15 내린 규칙](docs/10-15-o2o-harness-ondemand-rules.md) | CLAUDE.md에서 내린 절차의 본문 |
| [10-16 CodeRabbit](docs/10-16-o2o-harness-coderabbit.md) | 리뷰 봇 설정의 근거 |
| [10-17 도해](docs/10-17-o2o-harness-overview.md) | 위 그림 셋의 정본 |
| [10-18 대조 종합](docs/10-18-o2o-harness-crosscheck-rollup.md) | 대조 넷의 판정과 변경 전부 |
| [10-19 gentle-ai](docs/10-19-o2o-harness-crosscheck-gentle-ai.md) | 다섯째 대조. 권한 경계와 축 다섯 |

## 양식과 기준

| 파일 | 언제 여나 |
|---|---|
| [task-contract.md](prompts/task-contract.md) | 계약 단계 |
| [generate.md](prompts/generate.md) | 생성 단계 |
| [evaluate.md](prompts/evaluate.md) | 평가 A와 B 요청. 허용 입력 표가 여기 있다 |
| [decision-table.md](prompts/decision-table.md) | 결정 단계 |
| [apply.md](prompts/apply.md) | 반영 단계 |
| [eval-criteria-ddd.md](prompts/eval-criteria-ddd.md) | 설계 문서 평가 기준. 평가자에게 나간다 |
| [eval-criteria-code.md](prompts/eval-criteria-code.md) | 코드 작업 평가 기준. 평가자에게 나간다 |
| [answer-format.md](prompts/answer-format.md) | 답변 등급과 결과 절 다섯 행 |
| [07 v3](prompts/07-o2o-ptcf-prompt.v3.md) | 설계용 마스터 프롬프트 |
| [harness v2](prompts/harness-ptcf-prompt.v2.md) | 하네스 세션 인계용 |
| [dev v3](prompts/dev-ptcf-prompt.v3.md) | 개발 세션용 |

마스터 프롬프트는 최신 판만 적었다. 앞 판은 같은 폴더에 판 번호 없는 이름으로 남아 있다.

## 지금 상태 (2026-09-23 스킬 경로. 같은 날 설계 Step 9 행과 백엔드 행)

| 항목 | 상태 |
|---|---|
| 설계 Step 1부터 7 | 완료 |
| 설계 Step 8 | task-S8 R1 반영과 결정 완료. 확정본의 document/ 착지가 남았다 |
| 설계 Step 9 | 백엔드 계약 여섯 전부 main 병합(task-S9-catalog, inventory-rate, promotion-search, booking, payment, booking-lifecycle). MVP 블라인드 평가 라운드는 2026-09-16 마감. 숙소와 예약과 결제는 R1 반영 병합(PR 145, 156, 175). 재고와 요금은 R2 보류라 최종 완료 판단이 미완료. 프로모션과 검색 R1은 패스. 프론트 계약은 아래 프론트 행 |
| 하네스 H0부터 H6 | 완료. 검사기와 양식과 기록 규칙과 작업 계약이 다 있다. 반복 절차는 스킬 폴더 [.claude/skills](../.claude/skills/README.md)의 스킬 넷(round, progress-row, approval-request, guarded-paths)이 연다(PR 190, PR 193) |
| 백엔드 | 묶음 일곱의 40 단위 전부 main(PR 138에서 40 중 40). 검증 시나리오 30 중 30 통과. 묶음별 표는 [backend/](../backend/README.md) 6절 |
| 프론트 | task-S9-frontend T1부터 T10 main 병합(PR 152, 154, 159, 161, 164, 168, 171, 173, 176, 183). R1 평가 라운드 준비 끝(PR 187. 프론트 평가 축 여섯, 요청문 A와 B, 검사 스크립트). 다음은 Codex A와 B 리포트와 결정표. [frontend/](../frontend/README.md) |

이 표는 요약이고 정본이 아니다. 현재 위치의 정본은 [progress.md](state/progress.md)이고 그 Task의 마지막 행이다.

## 디렉터리 개편 이력 (2026-09-21 옛 판에서 가져옴)

| 날짜 | 변경 |
|---|---|
| 2026-09-07 | 참고 문서 26개를 루트에서 document/로. 10-4~10-8을 harness-docs/로 분리 |
| 2026-09-08 | 하네스 디렉터리 아홉 개를 harness/ 아래로. harness-prompts/tools/는 harness/tools/로 올림 |

docs/ 아래 10-4~10-8은 이력 문서라 본문의 경로를 고치지 않았다. 그 문서들이 적은 레이아웃은 2026-09-08 이전의 것이다. 특히 10-8 5-2의 디렉터리 도면은 지금 구조와 다르다. 현재 구조의 정본은 루트 CLAUDE.md 3절이다.

## 추적하지 않는 것

tmp/, output/, Claude outputs/, _to_delete/, .tmp_hla_check/는 [.gitignore](../.gitignore)로 제외했다. 과거 산출물, PDF, 생성 이미지, 중복 사본이다. 파일 자체는 로컬에 그대로 있다. 치우는 파일은 지우지 않고 tmp/_moved/로 옮긴다.

## 이 색인을 믿어도 되는 이유 (2026-09-21 이슈 74 문단 삭제)

링크가 끊기면 검사가 잡는다.

    node harness/tools/check.mjs g1 harness/README.md --type doc

link.exists가 이 파일의 모든 상대경로 링크를 연다. 파일이 옮겨지거나 이름이 바뀌면 실패한다.

검사가 못 잡는 것도 있다. 설명 한 줄이 낡는 것은 못 잡는다. 각 디렉터리의 README.md가 무엇이 그 폴더에 들어가는지의 정본이고, 이 파일은 어디 있는지만 답한다.
