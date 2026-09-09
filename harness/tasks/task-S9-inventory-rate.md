# 작업 계약 task-S9-inventory-rate (재고와 요금 묶음)

최초 작성: 2026-09-09
최종 갱신: 2026-09-09
양식: harness/prompts/task-contract.md v6

이 계약은 두 번째 구현 묶음이다. 첫 묶음 task-S9-catalog가 2026-09-09 최종 완료됐고 그 묶음이 세운 것 위에 얹는다.

## 작업

| 항목 | 내용 |
|---|---|
| Task ID와 Step | task-S9-inventory-rate. Step 9 구현. 기능 묶음은 재고와 요금 |
| 작업 유형 | 코드 |
| 목표 | INV-01부터 INV-05, RATE-01부터 RATE-04까지 아홉 개 API를 백엔드와 테스트와 백엔드 검증까지 내린다. 프론트는 이 Task에 없다 |
| 대상 API ID | INV-01, INV-02, INV-03, INV-04, INV-05, RATE-01, RATE-02, RATE-03, RATE-04 |
| 선행 작업 | task-S9-catalog. 완료됨. RoomType 애그리거트와 ActorResolver와 PageQuery를 이 Task가 그대로 쓴다 |
| 완료 기준 | 넷을 모두 만족해야 한다. 첫째, 9절 계약 테스트 ID가 전부 통과이거나 미실행 사유와 함께 기록된다. 둘째, 8절 단계가 전부 끝난다. 셋째, 빌드 도구가 낸 기계 판독 결과 파일에서 실행 수가 0이 아니고 실패 수가 0이다. 넷째, 모든 단계의 실제 시간이 harness/state/progress.md에 분 단위로 기입돼 있다 |
| 변경 허용 파일과 범위 | backend/ 전체, harness/out/task-S9-inventory-rate-R1/ 아래 실행 결과, harness/state/progress.md, 이 계약 파일. document/와 harness/project-sync/와 harness/docs/는 읽기만 한다. frontend/는 이 Task에 없다 |
| 범위 밖과 유지할 전제 | 아래 3절 |
| 기준 버전 | 문서는 4절 입력 표의 sha256. 코드는 작업 브랜치의 커밋 해시와 5절 평가 대상 행의 파일 목록 |
| 후보 작업 공간 | backend/. 후보와 정본의 구분은 git 브랜치가 맡는다. task-S9-catalog 결정 D-2 나를 이어받는다 |
| 결과 기록 경로 | harness/state/progress.md |

## 1. 이 Task가 첫 묶음과 다른 점

| 항목 | task-S9-catalog | 이 Task |
|---|---|---|
| 애그리거트 | Property, RoomType | DailyInventory, DailyRate |
| 식별자 | 자연키 없음. ID 하나 | 대리키 + 유니크(roomTypeId, stayDate). 06-2 1절 |
| 잠금 | 없었다. 저장할 때 version 대조만 | 06-4 0절이 비관적 락을 명시한다. 7절 D-2에서 정한다 |
| 컨텍스트 넘는 선행조건 | 없음 | roomType 존재 확인. 06-1 R1, 06-2 6절 CRC 세 번째 행 |
| 한 트랜잭션 한 애그리거트 | 지켰다 | INV-02가 의도적 예외다. 06-4 0절이 예외 2건 중 하나로 명시 |
| 원자성 요구 | 없었다 | INV-02는 한 날짜라도 겹치면 전부 실패다. 11 명세 138행, T03 |

세 번째 행과 여섯 번째 행이 이번 묶음의 난이도다. 첫 묶음에는 잠금도 원자성도 없었다.

## 2. 대상 API 9개와 설계 근거

| API ID | 엔드포인트 | 인증 | 계약표 행 | 불변식 |
|---|---|---|---|---|
| INV-01 | POST /api/v1/room-types/{roomTypeId}/inventories | HOST 소유자 | openInventory | I1, I1a |
| INV-02 | POST /api/v1/room-types/{roomTypeId}/inventories/bulk | HOST 소유자 | openInventory | I1, I1a |
| INV-03 | PATCH /api/v1/room-types/{roomTypeId}/inventories/{date} | HOST 소유자 | adjust | I1, I1a |
| INV-04 | GET /api/v1/room-types/{roomTypeId}/inventories | HOST 소유자 | 없음. 조회 | 없음 |
| INV-05 | GET /api/v1/room-types/{roomTypeId}/inventories/{date} | HOST 소유자 | 없음. 조회 | 없음 |
| RATE-01 | POST /api/v1/room-types/{roomTypeId}/rates | HOST 소유자 | registerRate | I2 |
| RATE-02 | PATCH /api/v1/room-types/{roomTypeId}/rates/{date} | HOST 소유자 | adjustRate | I2 |
| RATE-03 | GET /api/v1/room-types/{roomTypeId}/rates | HOST 소유자 | 없음. 조회 | 없음 |
| RATE-04 | GET /api/v1/room-types/{roomTypeId}/rates/{date} | HOST 소유자 | 없음. 조회 | 없음 |

묶음의 근거는 10-6 기능별 API와 검증 연결 표의 재고와 요금 행이다. 그 행이 INV-01~05와 RATE-01~04를 한 묶음으로 묶고 검증 ID를 T03부터 T07로 준다.

아홉 개 전부 인증이 HOST이고 대상 숙소의 소유자다. 게스트 경로가 하나도 없다. 재고를 게스트가 보는 것은 SEARCH 계열이고 그것은 다른 묶음이다.

### 2-1. 불변식 셋

| 번호 | 문장 | 어디서 지키나 |
|---|---|---|
| I1 | totalCount는 soldCount + heldCount 이상이다 | DailyInventory |
| I1a | soldCount와 heldCount는 음수가 될 수 없다 | DailyInventory |
| I2 | 요금은 0보다 크다 | DailyRate |

첫 묶음은 불변식이 I14 하나뿐이었다. 이번은 셋이고 그중 둘이 같은 애그리거트에 걸린다.

### 2-2. 이번 묶음이 만들지 않는 것

hold, commit, releaseHeld, releaseSold 네 행동은 계약표에 있으나 이번 Task에서 만들지 않는다. 호출자가 예약 컨텍스트뿐이고 그 컨텍스트가 아직 없기 때문이다. 근거는 06-4 1-2 재고와 요금 표의 Pre 칸이 전부 오름차순 잠금과 예약 경로를 전제한다는 것이다.

InventoryAllocationService도 같은 이유로 만들지 않는다. 06-2 6절 CRC가 그 서비스의 책임을 숙박 기간의 재고 N행 잠금과 hold, commit, release로 적는다. 전부 예약 경로다.

## 3. 범위 밖과 유지할 전제

| 항목 | 왜 밖인가 |
|---|---|
| 프론트와 연결 테스트 | 별도 Task. 사용자 결정이고 task-S9-catalog 개정 3과 같은 처리다 |
| hold, commit, release | 2-2절 |
| 프로모션과 검색 | 다음 묶음. 10-6 표의 다른 행 |
| 예약과 결제 | 그다음 묶음들 |
| SettlePayment와 Close와 Delete 계열 | task-S2 결정 D-1 나. v1 범위 밖 |
| 패키지 재배치와 식별자 타입 변경 | 사용자가 미뤘다. 2026-09-09 |
| 하네스 파일 수정 | 이 Task 동안 동결한다. 첫 묶음의 동결 규칙을 이어받는다 |

유지할 전제. 첫 묶음이 세운 shared 패키지(Actor, ActorResolver, ActorRegistry, PageQuery, PageResult, ErrorResponse, VersionConflictException)를 그대로 쓴다. 같은 것을 다시 만들지 않는다. 층 이름도 domain, application, api, infrastructure 넷을 그대로 쓴다.

## 4. 입력과 적용 규칙

| 자료 | 경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 01 전체 | document/01-o2o-ddd-plan.md | sha256:17569703c90a18df | 전문 |
| 대상 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절 전부, 재고 절, 요금 절, 응답 모델의 DailyInventory와 DailyRate와 InventoryRange와 RateRange, 검증 기준의 T03부터 T07 |
| 대상 06-2 애그리거트 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 1절, 2절 재고와 요금 행, 3-1 I1과 I1a와 I2, 4절, 5절, 6절 재고와 요금 CRC |
| 대상 06-4 계약 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 0절 머리 선언, 1-1 I1과 I1a와 I2, 1-2 재고와 요금 계약표, 1-4 검증 책임 위치 |
| 대상 06-1 컨텍스트 맵 | document/06-1-o2o-context-map.md | sha256:95bc2b1079d3b739 | 카탈로그가 상류인 관계 행. R1 |
| 대상 05-3 용어 | document/05-3-o2o-glossary.md | sha256:62a87a62baf5846e | 재고와 요금 용어 행. 클래스와 필드 이름의 출처다 |
| 대상 02 기능 목록 | document/02-o2o-feature-list.md | sha256:2a3d3ca2d8b63809 | 재고와 요금 기능 행 |
| 대상 03 이벤트 스토밍 | document/03-o2o-event-storming.md | sha256:3334e5a73cb8f896 | 재고와 요금 커맨드와 이벤트 행 |
| 확정 전제 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 1절 확정 전제와 2절 요구사항 |
| 구현 계획 10-6 | harness/docs/10-6-o2o-harness-implementation-plan.md | sha256:6289683579908585 | 3절 실행 순서와 기능별 API와 검증 연결 표 |
| 앞 묶음의 계약 | harness/tasks/task-S9-catalog.md | sha256:bd62431b07c6d3f9 | 3절 범위 밖, 6절 정책 적용, 8절 단계표 |
| 적용할 코드 양식 | harness/prompts/dev-ptcf-prompt.v3.md | sha256:d4531cd7eeddb42d | Format 절 |
| 실제 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 | 축, 심각도, 출력 스키마 |

10-6과 앞 묶음의 계약은 이 표에만 있고 5절에는 없다. harness/docs/는 하네스 설계 근거라 평가 입력이 될 수 없고(CLAUDE.md 3절), 앞 묶음의 계약은 이 묶음 코드의 근거가 아니다.

## 5. A와 B 평가 허용 입력 (HR1)

| 자료 | 경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 코드 | 생성 후 기입 | 생성 후 기입 | 파일 목록과 커밋 해시 |
| 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 이 작업 계약 | harness/tasks/task-S9-inventory-rate.md | 자기 해시 없음 | 전문 |
| 실제 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 | 축, 심각도, 출력 스키마 |
| 대상이 참조하는 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절과 재고 절과 요금 절 |
| 대상이 참조하는 06-2 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 1절, 3-1, 5절, 6절 재고와 요금 |
| 대상이 참조하는 06-4 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 0절, 1-1, 1-2 재고와 요금, 1-4 |

이 표에 넣지 않는 것: 01 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, harness/docs/ 전체, harness/state/ 전체, 생성 대화.

요구사항 역추적 축에 대한 지시. 입력 팩 2절의 요구사항은 R1부터 R5 다섯이다. 이번 묶음에 걸리는 것은 없다. R1과 R2는 연박 선점과 초과 예약이라 예약 경로가 있어야 하고 그 경로는 2-2절로 범위 밖이다. R5는 스냅샷 동결이라 Booking이 있어야 한다. 평가자는 다섯을 해당 없음으로 적고 미커버로 적지 않는다. 이 Task의 역추적 대상은 2절의 API 9개와 2-1절의 불변식 셋이다.

## 6. 정책 적용

| 정책 ID 또는 쟁점 | 적용할 값 또는 판단 | 상태와 사용자 확인 |
|---|---|---|
| P03 통화와 달력 | 채택. currency는 KRW 고정이고 amount는 정수 원이다. 날짜는 서버 기준 오늘로 판정한다 | 확정. 계약 승인. join5201, 2026-09-09 |
| P07 로컬 행위자 | 채택. 앞 묶음과 같다. X-Dev-Actor-Id 헤더로 행위자를 받고 body로 소유자를 정하지 않는다 | 확정. 앞 묶음에서 확인 |
| P10 판매 제약 | 채택. min_los와 closed 필드를 만들지 않는다 | 확정. 앞 묶음에서 확인 |
| P01, P02, P04, P05, P06, P08, P09, P11 | 이번 작업과 무관. 예약과 결제와 프로모션 계열이다 | 확인 대기 |
| 08-3 결정 1부터 11 | 이번 작업과 무관 | 확정. join5201, 2026-09-08 |
| 기간 상한 366일 | 채택. 06-4 계약표가 가설로 적고 11 명세 137행이 최대 366일로 확정한다. 명세를 따른다 | 확정. 계약 승인. join5201, 2026-09-09 |
| totalCount 0 등록 | 허용. 11 명세 139행 | 확정. 계약 승인. join5201, 2026-09-09 |
| 과거 날짜 | 조회는 허용, 등록과 수정은 서버의 오늘 이상. 11 명세 198행과 259행 | 확정. 계약 승인. join5201, 2026-09-09 |
| D-1 N9와의 충돌 | 가. N9 문구를 고치고 프론트는 별도 Task 계열로 둔다 | 확정. join5201, 2026-09-09 |
| D-2 재고 수정의 동시성 | 가. 행을 잠근 뒤 version을 대조하고 그 안에서 I1을 검사한다 | 확정. join5201, 2026-09-09 |
| D-3 블라인드 평가 | 가. 이번 묶음을 A와 B 평가에 넘긴다 | 확정. join5201, 2026-09-09 |

위 표에서 확인 대기였던 P03과 기간 상한 366일과 totalCount 0과 과거 날짜 네 행은 계약 승인으로 확정됐다. 네 행 모두 적용할 값이 11 명세 그대로이고 계약이 그 값을 명시한 채 승인됐다.

미결 정책에 의존하는 구현을 확정하지 않는다(N4).

## 7. 결정 3건의 안과 추천

### D-1. N9와 이 Task의 충돌을 어떻게 푸나

task-S9-catalog 125행이 이 자리를 예고했다. 프론트 없이 다음 묶음으로 넘어가면 CLAUDE.md N9가 걸린다고 적었고 그 시점에 정하기로 미뤘다. 지금이 그 시점이다.

쉽게 말하면 N9는 기능 하나를 프론트까지 다 내린 뒤에 다음 기능으로 가라는 규칙인데, 사용자는 백엔드를 API 기준으로 먼저 완성한 뒤 프론트로 가는 방식을 세 번 지시했다. 둘이 부딪힌다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | N9 문구를 고친다. 묶음의 백엔드와 테스트와 검증이 끝나면 다음 묶음으로 갈 수 있고, 프론트는 별도 Task 계열로 둔다 | CLAUDE.md 수정이라 승인이 필요하다. 10-6 3절의 근거 문장도 같이 본다 |
| 나 | N9를 그대로 두고 이 Task를 막는다 | 사용자 지시와 정면으로 충돌한다 |

추천은 가다. 규칙이 사용자 결정을 막고 있는 상태이고, 규칙 쪽이 현실을 못 따라간 것이다.

결정: 가. join5201, 2026-09-09. N9 문구 수정은 이 Task 밖의 별도 이슈와 PR로 낸다.

### D-2. 재고 수정의 동시성을 어떻게 처리하나

문서 둘이 다른 것을 요구한다.

| 어디 | 무엇을 요구하나 |
|---|---|
| 06-4 0절과 1-2 adjust의 Pre | 비관적 락. 행을 잠근 뒤 검사 |
| 11 명세 INV-03 요청 본문과 T05 | version 필드와 VERSION_CONFLICT. 낙관적 대조 |

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 둘 다 한다. 행을 잠근 뒤 version을 대조하고 그 안에서 I1을 검사한다 | 코드가 늘고 통합 테스트가 필요하다. 문서 둘을 모두 만족한다 |
| 나 | version 대조만 한다. 비관 락은 예약 묶음에서 도입한다 | 06-4 0절과 어긋난다. 다음 묶음에서 다시 손대야 한다 |

추천은 가다. 잠금과 version 대조는 막는 것이 다르다. 잠금은 같은 순간의 두 요청을 줄 세우고, version 대조는 오래된 화면을 보고 보낸 요청을 거절한다. T04와 T05가 각각 그 둘을 요구한다.

결정: 가. join5201, 2026-09-09. INV-03과 RATE-02가 대상이고 잠금은 행 단위 비관 락이다.

### D-3. 이번 묶음을 블라인드 평가에 넘기나

앞 묶음에서 나로 정하면서 다음 묶음의 결정으로 미뤘다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 넘긴다. 5절 목록을 harness/out/task-S9-inventory-rate-R1/eval-input/에 복사하고 평가 요청 2벌을 만든다 | 사용자가 Codex 새 작업 둘에 전달하고 리포트를 받아 와야 한다. 시간이 든다 |
| 나 | 이번에도 게이트만 쓴다 | 코드 블라인드 평가가 또 한 바퀴 미뤄진다. 하네스가 아직 코드로는 한 번도 완주하지 않았다 |

추천은 가다. 이 묶음은 잠금과 원자성이 있어 첫 묶음보다 지적이 나올 자리가 많다. 평가 축이 실제로 지적을 만들어 내는지 확인해야 eval-criteria-code.md가 초안에서 v1로 오른다.

결정: 가. join5201, 2026-09-09. 6-2단계가 끝난 뒤 5절 평가 대상 행을 채우고 평가 요청 2벌을 만든다.

## 8. 세로 진행 단계와 각 단계의 완료 조건

한 턴에 한 단계다. 각 단계 끝에서 멈추고 사용자 확인을 받는다. 번호는 dev-ptcf-prompt.v3.md 3절과 같다.

| 단계 | 내용 | 완료 조건 | 정지 |
|---|---|---|---|
| 1 | 이 계약과 결정 3건 승인 | 10절 승인 칸과 7절 결정 3건이 값을 갖는다 | 승인 대기 |
| 2 | 이슈와 브랜치 | 이슈 하나, 브랜치 하나. CLAUDE.md 4-1 순서 | 확인 |
| 3 | 해당 없음 | 백엔드 틀과 MySQL은 앞 묶음이 이미 세웠다 | 건너뛴다 |
| 4 | 도메인 코드. DailyInventory, DailyRate, 리포지토리 둘, 앱 서비스 | 06-2 6절 재고와 요금 CRC의 책임 행마다 대응 코드가 있고 주석에 절 번호가 있다. 불변식 셋이 애그리거트 안에 있다 | 확인 |
| 5 | 테스트 | 8-1절의 5단계 항목에 테스트가 있고 실패 케이스와 통과 케이스가 짝이다 | 확인 |
| 6 | 등록과 수정 API. INV-01, INV-02, INV-03, RATE-01, RATE-02 | 다섯 경로가 명세의 요청과 응답과 상태 코드 그대로다. 8-1절의 6단계 항목이 붙는다. 로컬 HTTP 호출 결과가 파일로 남는다 | 확인 |
| 6-2 | 조회 API 넷. INV-04, INV-05, RATE-03, RATE-04 | 네 경로가 명세 그대로이고 8-1절의 6-2단계 항목이 붙는다 | 확인 |
| 7 | 해당 없음 | 프론트는 별도 Task | 고르지 않는다 |
| 8 | 해당 없음 | 프론트는 별도 Task | 고르지 않는다 |
| 9 | 백엔드 검증 표 | T03과 T04와 T05의 대조표가 나오고 T06과 T07은 부분으로 적는다. 회고 표가 채워진다 | 사용자 완료 판단 |

모든 단계에 공통으로 걸리는 완료 조건 둘. 첫째, harness/state/progress.md에 그 단계의 행이 추가되고 실제 시간 칸이 분 단위로 채워진다. 둘째, 하네스 파일을 고치지 않는다.

3단계를 건너뛰는 이유. 앞 묶음이 backend/와 MySQL 컨테이너와 결과 파일 경로를 이미 확정했고 9절이 그 값을 그대로 쓴다. 다시 세우지 않는다.

### 8-1. 단계별 테스트 목록

불변식 하나에 테스트 하나가 최소다. 이번 묶음은 불변식 셋에 원자성과 잠금이 더 붙는다.

| ID | 단계 | 무엇을 확인하나 | 근거 |
|---|---|---|---|
| V1 | 5 | totalCount를 soldCount + heldCount 아래로 내리면 거부된다 | I1, T04 |
| V2 | 5 | soldCount와 heldCount가 음수가 되지 않는다 | I1a |
| V3 | 5 | 요금 0과 음수가 거부된다 | I2 |
| V4 | 5 | 같은 roomTypeId와 stayDate로 두 번 등록하면 두 번째가 거부된다 | U2, 유니크 |
| V5 | 6 | INV-02 일괄 등록 중 한 날짜라도 이미 있으면 전부 실패하고 새 날짜도 안 남는다 | T03. MySQL 통합 테스트 |
| V6 | 6 | 지난 version으로 수정하면 409 VERSION_CONFLICT이고 최근 변경이 남는다 | T05 |
| V7 | 6 | 다른 HOST의 객실 타입에 재고를 등록하면 404다 | T02, 11 인증과 접근 제어 |
| V8 | 6 | 기간 상한 366일을 넘기면 거부된다 | 11 명세 137행 |
| V9 | 6 | 과거 날짜 등록과 수정이 거부되고 조회는 허용된다 | 11 명세 198행과 259행 |
| V10 | 6-2 | 없는 날짜가 items가 아니라 missingDates에 담긴다 | 11 명세 259행 |
| V11 | 6-2 | 날짜 단건 조회에서 레코드가 없으면 404다 | 11 명세 295행과 500행 |
| V12 | 6 | 계약표 Post 열의 이벤트 넷이 실제로 발행된다. InventoryOpened, InventoryAdjusted, RateRegistered, RateAdjusted | 06-4 1-2 Post |

V5와 V6은 MySQL 통합 테스트다. 메모리 저장소로 대신하지 않는다. 롤백과 유니크와 잠금은 DB가 하는 일이라 메모리로 바꾸면 확인하려던 것이 사라진다.

## 9. 실행과 검증

| 항목 | 내용 |
|---|---|
| 작업 디렉터리 | backend/ |
| 실행 환경 | Spring Boot 4.1.1, Gradle 9.7.1 wrapper, Java 툴체인 21, MySQL 9.7.2 컨테이너. 환경변수 이름은 O2O_MYSQL_ROOT_PASSWORD, O2O_MYSQL_USER, O2O_MYSQL_PASSWORD. 값은 backend/.env이고 저장소에 없다 |
| 실행할 명령 | docker compose -f backend/docker-compose.yml up -d로 DB를 올린다. cd backend && JAVA_HOME=<JDK 21 경로> ./gradlew test로 테스트를 돌린다. 예상 결과는 BUILD SUCCESSFUL과 실패 0 |
| 테스트 DB | 127.0.0.1:3307, o2o_catalog_test. 앞 묶음과 같은 컨테이너를 쓴다 |
| 데이터 초기화 허용 범위 | 테스트 프로파일의 ddl-auto가 create-drop이라 테스트 DB 스키마 전체다. 운영 DB는 없다 |
| 빌드 출력과 로그 경로 | backend/build/test-results/test/*.xml. 사본을 harness/out/task-S9-inventory-rate-R1/에 남긴다 |
| 계약 테스트 ID | 8-1절의 V1부터 V12. 검증 ID는 T03, T04, T05 전부와 T06, T07 부분 |
| A와 B 평가 범위 | eval-criteria-code.md의 축 전부. 결정 D-3이 가일 때만 해당한다 |
| 필수 검증을 실행하지 못했을 때 | progress.md의 실패 원인 칸에 명령과 출력을 적고 결과를 halted로 남긴다. 미실행을 통과로 적지 않는다 |

T06과 T07이 부분인 이유. 둘 다 예약이 있어야 닫힌다. T06은 체크아웃 날짜에 재고와 요금이 없어도 예약이 가능한지를 보고, T07은 숙박 날짜 중 하나에 재고나 요금이 없을 때 Booking과 Hold가 안 생기는지를 본다. 이번 묶음은 그 두 시나리오의 재고와 요금 쪽 준비 상태까지만 만든다.

## 10. 승인과 진행

| 항목 | 기록 |
|---|---|
| 작업 계약 승인 | 승인. join5201, 2026-09-09 |
| 마지막 성공 단계 | 1단계 계약과 결정 승인 |
| 미해결 사항과 다음 작업 | 결정 3건 전부 가로 확정됐다. 다음은 2단계 이슈와 브랜치다. D-1이 요구하는 CLAUDE.md N9 수정은 이 Task 밖의 별도 PR로 낸다 |
| 최종 산출물과 버전 | 작업 후 기록 |
| 실제 사용 시간 | 미측정 |
| 최종 완료 판단 | 대기 |
