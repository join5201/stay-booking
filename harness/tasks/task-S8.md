# 작업 계약 task-S8

양식: harness/prompts/task-contract.md v5 (2026-09-08)
최초 작성: 2026-09-08
최종 갱신: 2026-09-08 (양식 v2에서 v5로. A와 B 평가 허용 입력 절 신설. 승인 시점에는 그 절이 양식에 없었다)

이 계약은 2026-09-08에 승인됐다. 승인 원문은 진행 한 줄이다.

## 작업

| 항목 | 내용 |
|---|---|
| Task ID와 Step | task-S8. Step 8 정책 보완의 반영 |
| 작업 유형 | 설계 문서 |
| 목표 | 08-3 결정 11건을 설계 문서 9종에 반영한 후보를 만든다 |
| 대상 API ID | 해당 없음. 11 API v3 대조는 이 Task 밖이다 |
| 완료 기준 | 아래 반영 대상 표의 문서별 확인 조건 9행을 전부 만족하고, 후보 9개가 node harness/tools/check.mjs g1 --type doc를 통과한다 |
| 변경 허용 파일과 범위 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/ 아래 후보 9개만. document/와 harness/project-sync/는 읽기만 한다 |
| 범위 밖과 유지할 전제 | 정책 축 재검증은 이 Task의 평가 라운드로 갈음한다. Step 9, 코드, 11 API v3 대조, 01 v20 승격은 범위 밖. 확정 전제는 06-2 v4와 06-4 v4가 정본이고 01은 참고다 |
| 기준 버전 | 아래 입력 표의 해시. node harness/tools/check.mjs fill이 채운다 |
| 후보 작업 공간 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/ |
| 결과 기록 경로 | C:/Dev/potenup/99_projects/o2o/harness/state/progress.md |

## 이 계약이 먼저 정하는 것

세 가지가 계약 없이는 정해지지 않는다. 셋 다 사실에서 한 갈래로만 나오므로 계약이 결정하고 근거를 옆에 적는다. 사용자 판단을 요구하지 않는다.

### 1. 판 번호

08-3 7절이 적은 목표 판 번호 중 셋이 로컬 현재 판과 충돌한다. 로컬 02, 03, 04는 이미 v8, v11, v9인데 08-3 미반영이다. 08-3의 번호는 프로젝트 계열의 이력이고 로컬 파일의 이력이 아니다.

규칙: 반영 결과의 판 번호는 로컬 현재 판에 1을 더한 값이다.

| 문서 | 로컬 현재 | 08-3 목표 | 이 Task의 판 |
|---|---|---|---|
| 02 기능 목록 | v8 | v8 | v9 |
| 03 이벤트 스토밍 | v11 | v11 | v13 |
| 04 커맨드와 액터 | v9 | v9 | v10 |
| 04-5 정책 초안 | 로컬에 없음 | v2 | v2 |
| 05-3 용어 사전 | v10 | v11 | v11 |
| 06-1 컨텍스트 맵 | 판 표기 없음 | v2 | v2 |
| 06-2 애그리거트 | v4 | v5 | v5 |
| 06-4 계약과 정책 카드 | v4 | v5 | v5 |
| 입력 팩 | v2 | v3 | v3 |

### 2. 04-5의 반입

04-5는 로컬 document/에 없고 harness/project-sync/04-5-o2o-policies-draft.md v1만 있다. v1을 입력으로 읽어 v2 후보를 만든다. project-sync 원본은 고치지 않는다. 승인 뒤 후보를 document/04-5-o2o-policies-draft.md로 옮긴다.

### 3. 06-1의 날짜 줄 결손

06-1에 최종 갱신 줄이 없다. F5 위반이고 g1 doc의 date-updated 검사에 걸린다. v2 후보에서 최초 작성과 최종 갱신 두 줄을 갖춘다.

## 반영 대상과 문서별 완료 조건

| 문서 | 반영할 08-3 항목 | 확인 조건 |
|---|---|---|
| 06-4 v5 | C-1 정산 표식 정의와 찍는 규칙과 되돌리는 규칙, C-2 잠금 순서, C-3 리스너 예외 위치, C-4 recordApproval과 recordFailure 계약과 ORPHAN kind, C-5 환불 멱등키 유도, C-6 CANCELED 분기 삭제, C-8 expire 스킵 반환 폐기, C-9 예약에서 결제로의 조회 계약과 T2 잠금, C-10 openAttempt와 PG 경계, C-12 잠금 중 PG 호출 | 계약표에 settledAt과 settledBy 행, 상태전이표에 ORPHAN과 REFUND_PENDING, 정책 카드에 T2 조회 조건 (a)(b)(c), 잠금 순서 한 줄이 전역 규칙으로 명시 |
| 06-2 v5 | C-1, C-2, C-4의 애그리거트 쪽 | Payment 애그리거트에 kind와 settledAt과 settledBy, 잠금 순서가 검사 전 루트 잠금 절에 반영, 애그리거트가 PG를 호출하지 않는다는 문장 |
| 04-5 v2 | C-1의 T2 순찰 정책 | 정책 목록에 T2 행 추가. 정책 수와 커버리지 검사 결과 갱신 |
| 04 v10 | C-1의 SettlePayment | 커맨드 22개가 23개로. SettlePayment의 액터와 이벤트 매핑 행 |
| 03 v13 | C-7 R4 문언 | R4 문언이 먼저 커밋되는 것과 확정 우선으로 교체. InventoryHoldRejected 추가는 09-1 미반입이라 미결로 표시 |
| 02 v9 | C-7의 3-3 문장 | 3-3 문장이 R4와 같은 문언으로 |
| 06-1 v2 | 08-2 F 확인필요 3의 종결 결과 | R6 흐르는 것에 SettlePayment와 T1 T2 조회 추가, R6 근거 문구 수정, 구독자 없는 이벤트 17개를 20개로. 최종 갱신 줄 신설 |
| 05-3 v11 | C-1, C-4, C-5의 용어 | 정산 표식, 고아 승인, ORPHAN, REFUND_PENDING, 환불 멱등키 다섯 항목이 정의와 소유 컨텍스트를 갖고 등재 |
| 입력 팩 v3 | 08-3 반영 결과와 로컬 경로 | 상위 입력 경로가 로컬 절대경로로, 배경 요약이 08-3 반영본 기준으로 |

미결 하나. 03의 InventoryHoldRejected는 09-1 보드 v2가 반입돼야 근거가 생긴다. 이 Task에서는 미결로 표시하고 이벤트 수는 22개를 유지한다.

## 입력과 적용 규칙

| 자료 | 절대경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 01 참고 | C:/Dev/potenup/99_projects/o2o/harness/out/h2/01-o2o-ddd-plan.v20.md | sha256:5ff40a01743ddd00 | 1절 확정 전제와 Step 8 절만 |
| 08-3 원문 | C:/Dev/potenup/99_projects/o2o/harness/project-sync/08-3-o2o-policies-v2-attack-review.md | sha256:060e4ef217b1fe97 | 6절 C-1부터 C-12, 7절 반영 목록, 8절 결정표 |
| 08-2 원문 | C:/Dev/potenup/99_projects/o2o/harness/project-sync/08-2-o2o-policies-resolution-review.md | sha256:5f5da9f2db768266 | 6절 해결안 v2, 7절 반영 목록 |
| 08-1 원문 | C:/Dev/potenup/99_projects/o2o/harness/project-sync/08-1-o2o-policies-blind-review.md | sha256:6103bf1c05968e98 | 7절 해결안 |
| 사용자 결정표 | C:/Dev/potenup/99_projects/o2o/harness/decisions/decisions-08-3.md | sha256:1b8580aa86c18fd8 | 전문 |
| 대상 06-4 v4 | C:/Dev/potenup/99_projects/o2o/document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 전문 |
| 대상 06-4 설명 | C:/Dev/potenup/99_projects/o2o/document/06-4-o2o-contracts-explained.md | sha256:3b4c9e463af5de17 | 전문 |
| 대상 06-2 v4 | C:/Dev/potenup/99_projects/o2o/document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 전문 |
| 대상 06-2 설명 | C:/Dev/potenup/99_projects/o2o/document/06-2-o2o-aggregates-explained.md | sha256:c1607d414af91b93 | 전문 |
| 대상 04-5 v1 | C:/Dev/potenup/99_projects/o2o/harness/project-sync/04-5-o2o-policies-draft.md | sha256:a99b36c86c567240 | 전문 |
| 대상 04 v9 | C:/Dev/potenup/99_projects/o2o/document/04-o2o-commands-actors.md | sha256:3ebf3af5b34b2b34 | 전문 |
| 대상 03 v11 | C:/Dev/potenup/99_projects/o2o/document/03-o2o-event-storming.md | sha256:34a8544cba55728b | 전문 |
| 대상 02 v8 | C:/Dev/potenup/99_projects/o2o/document/02-o2o-feature-list.md | sha256:2a3d3ca2d8b63809 | 전문 |
| 대상 06-1 | C:/Dev/potenup/99_projects/o2o/document/06-1-o2o-context-map.md | sha256:95bc2b1079d3b739 | 전문 |
| 대상 05-3 v10 | C:/Dev/potenup/99_projects/o2o/document/05-3-o2o-glossary.md | sha256:62a87a62baf5846e | 전문 |
| 대상 입력 팩 v2 | C:/Dev/potenup/99_projects/o2o/harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 적용할 문서 양식 | C:/Dev/potenup/99_projects/o2o/harness/prompts/07-o2o-ptcf-prompt.v3.md | sha256:45e9c98c3bb1a94d | Format 절 F1부터 F16 |
| 실제 평가 기준 | C:/Dev/potenup/99_projects/o2o/harness/prompts/eval-criteria-ddd.md | sha256:0ec4e54137d66085 | 축, 심각도, 출력 스키마 |

07 v3은 후보다. 승인 전이므로 이 Task에서는 문체와 형식 조항만 적용하고 종료 문장 F16은 문서 산출물에 적용하지 않는다. 설계 문서는 대화 응답이 아니라서 종료 문장이 붙을 자리가 없다. g1 실행 시 --end 옵션으로 각 문서의 마지막 줄 규칙을 따로 준다.

## A와 B 평가 허용 입력 (HR1, 2026-09-08 신설)

위 표는 생성자가 읽을 목록이다. 평가자에게 넘기는 목록은 이 표다. 두 표를 같은 것으로 취급하면 감사 문서와 결정표가 평가 폴더로 들어간다.

| 자료 | 절대경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 06-4 v5 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/06-4-o2o-contracts.v5.md | sha256:c32cc10985cb86c2 | 전문 |
| 평가 대상 06-2 v5 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/06-2-o2o-aggregates.v5.md | sha256:d260e2d76196eb0f | 전문 |
| 평가 대상 04-5 v2 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/04-5-o2o-policies-draft.v2.md | sha256:7435d1e4f714070c | 전문 |
| 평가 대상 04 v10 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/04-o2o-commands-actors.v10.md | sha256:fa0463666b950db6 | 전문 |
| 평가 대상 03 v13 | 생성 후 기입 | 생성 후 기입 | 전문 |
| 평가 대상 02 v9 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/02-o2o-feature-list.v9.md | sha256:ee9bcf474a8f9f8a | 전문 |
| 평가 대상 06-1 v2 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/06-1-o2o-context-map.v2.md | sha256:90c016f3ec3e7c9a | 전문 |
| 평가 대상 05-3 v11 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/05-3-o2o-glossary.v11.md | sha256:276f6cf7cc25805b | 전문 |
| 평가 대상 입력 팩 v3 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/o2o-review-input-pack.v3.md | sha256:41d7582034ad30da | 전문 |
| 입력 팩 | C:/Dev/potenup/99_projects/o2o/harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 이 작업 계약 | C:/Dev/potenup/99_projects/o2o/harness/tasks/task-S8.md | 자기 해시 없음 | 전문 |
| 실제 평가 기준 | C:/Dev/potenup/99_projects/o2o/harness/prompts/eval-criteria-ddd.md | sha256:0ec4e54137d66085 | 축, 심각도, 출력 스키마 |

이 표에 넣지 않는 것: 01 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, harness/docs/ 전체, harness/state/ 전체, 생성 대화. 평가 폴더를 만들 때는 이 표만 보고 복사한다.

08-1과 08-2와 08-3을 넣지 않는다. 셋 다 과거 감사 문서이고 양식의 제외 목록 두 번째 항목에 해당한다. decisions-08-3.md도 사용자 결정표라 넣지 않는다. 위 생성용 표에는 넷이 다 있는데 그것은 생성자가 읽을 목록이기 때문이다. 08-3의 무엇이 어디로 갔어야 하는지는 이 계약의 반영 대상 표가 알려 준다. 평가자는 그 표와 후보를 대조하지 원본 감사 문서를 대조하지 않는다. 그것이 블라인드 평가다.

입력 팩이 두 줄인 것은 오기가 아니다. 아래 입력 팩 행은 project-sync의 현재 판이고 배경 자료다. 위 평가 대상 입력 팩 v3은 이번에 만든 후보이고 평가 대상이다. 배경까지 후보로 주면 평가자가 자기가 평가할 문서를 배경으로 삼는다.

평가 대상 03은 생성 후 기입이다. task-S2가 03 v12를 가져가기로 2026-09-08 확정돼 이 Task의 03은 v13이고, v13은 task-S2 반영본 위에서 다시 만든다. 그때까지 이 행은 채우지 않는다. 이 행이 남아 있는 동안에는 평가 요청 단계로 가지 않는다.

## 정책 적용

| 정책 ID 또는 쟁점 | 적용할 값 또는 판단 | 상태와 사용자 확인 |
|---|---|---|
| 08-3 결정 1부터 11 | 전부 수용. 근거는 decisions-08-3.md 9절 | 확정. join5201, 2026-09-07 |
| 판 번호 | 로컬 현재 판에 1을 더한다 | 이 계약에서 결정. 대안 없음. 목표 번호를 그대로 쓰면 같은 파일 이력에 같은 판이 둘 생겨 HR2의 버전 대조가 깨진다 |
| 04-5 반입 | project-sync v1을 입력으로 v2 후보 생성, 승인 뒤 document/로 | 이 계약에서 결정. 대안 없음. 로컬에 04-5가 없고 v1은 project-sync에만 있으며 착지점은 04, 05, 06과 같은 document/다 |
| InventoryHoldRejected | 09-1 미반입. 03에서 미결 표시, 이벤트 수 22 유지 | 미결. 09-1 반입 시 재개 |
| 11 API v2의 P01부터 P11 | 이번 작업과 무관 | 해당 없음 |
| 01 v20 승격 | 범위 밖. 이 Task는 01을 고치지 않는다 | 미결. 별도 승인 |

## 실행과 검증

| 항목 | 내용 |
|---|---|
| 작업 디렉터리 | C:/Dev/potenup/99_projects/o2o |
| 실행 환경 | Node 24.14.1. 환경변수 없음 |
| 실행할 명령 | node harness/tools/check.mjs fill harness/tasks/task-S8.md 로 이 계약의 해시를 채우고 기록된 해시를 대조한다. 후보 9개마다 node harness/tools/check.mjs g1 <후보> --type doc --end "이 판은 08-3 결정 11건의 반영 후보다. 승인 전에는 확정본으로 인용하지 않는다." --require "반영하지 않은 것" 을 돌린다. 결정표는 반영 전에 --mode pre, 최종 확인에 --mode final로 두 번 돌린다 |
| 테스트 DB | DB 사용 없음 |
| 데이터 초기화 허용 범위 | 없음 |
| 빌드 출력과 로그 경로 | 해당 없음 |
| 계약 테스트 ID | 해당 없음. 문서 작업이다 |
| A와 B 평가 범위 | A는 eval-criteria-ddd.md 3절의 정책과 Saga 축. B는 공통 축 3개 |
| 필수 검증을 실행하지 못했을 때 | 미실행 명령과 사유를 progress.md의 실패 원인 칸에 적고 결과를 halted로 기록한다. 통과로 기록하지 않는다 |

이 Task는 하네스의 첫 실전 바퀴다. 하네스 자체의 결함이 나오면 harness/state/troubleshooting.md에 행을 추가한다. 형식 보정 1회와 재평가 1회 카운터에도 넣지 않는다.

다만 계속 진행할 수 있는 범위를 가른다. 검사기 결함으로 게이트가 오류를 내면 그 게이트에 의존하지 않는 독립 작업만 계속한다. 게이트 자체를 통과하지 못한 채로 다음 단계로 넘어가지 않는다. 구체적으로 G1을 통과하지 못한 후보를 평가에 넘기지 않고, G2를 통과하지 못한 결정표로 반영하지 않는다. 검사기를 고친 뒤 그 게이트를 다시 돌려 통과시키고 나서 진행한다. (2026-09-08 HRV-12)

## 승인과 진행

| 항목 | 기록 |
|---|---|
| 작업 계약 승인 | 승인. join5201, 2026-09-08 |
| 마지막 성공 단계 | 계약 승인 |
| 미해결 사항과 다음 작업 | 계약 자체의 승인. 09-1 미반입으로 03의 이벤트 항목 하나가 미결 |
| 최종 산출물과 버전 | 작업 후 기록 |
| 실제 사용 시간 | 미측정 |
| 최종 완료 판단 | 미판단 |

작업 계약 승인과 최종 완료 승인은 구분한다. 이 양식을 작성했다는 사실만으로 승인하거나 실행한 것으로 처리하지 않는다.
