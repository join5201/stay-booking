# 사용자 결정표 task-S9-inventory-rate R1

양식: harness/prompts/decision-table.md v4 (2026-09-08)
최초 작성: 2026-09-11
최종 갱신: 2026-09-11 (결정 8건 확정. 사용자 발언은 결정 8건 초안대로 확정하고 반영 진행해라)

왜 이 파일이 필요한가: 리포트를 눈으로 읽고 반영하면 자기 결정에 불리한 지적이 조용히 빠진다(HR4). 원본 행을 하나씩 옮겨 두면 g2가 ID 집합과 심각도를 원본과 대조할 수 있다.

이 표의 행은 원본 리포트의 상세 표에서 셀 단위로 뽑아 만들었다. 결정과 이유 열은 생성 세션이 초안을 적었고 사용자가 2026-09-11 초안대로 확정했다. 초안의 근거는 지적마다 실제 파일을 열어 대조한 것이고 그 위치를 이유에 적었다.

## 대상과 원본

| 항목 | 기록 |
|---|---|
| Step | 9 |
| 작업 유형 | 코드 |
| 평가 라운드 | R1 |
| 평가 대상 절대경로와 파일 목록 | harness/out/task-S9-inventory-rate-R1/eval-target-files.md |
| 평가 대상 버전 또는 해시 | sha256:807b6239259b4107. 목록이 가리키는 프로덕션 43개와 테스트 7개의 기준 커밋은 280a69163a6a64225bb414969a134af22d5cefbe이고 파일별 sha256은 A와 B 리포트의 읽은 파일 표에 있다 |
| 승인된 작업 계약 절대경로와 버전 | harness/tasks/task-S9-inventory-rate.md sha256:896a9868f73b3eed |
| A 원본 리포트 절대경로와 버전 또는 해시 | harness/reviews/task-S9-inventory-rate-R1-A.md sha256:deff147b74ff1065 |
| B 원본 리포트 절대경로와 버전 또는 해시 | harness/reviews/task-S9-inventory-rate-R1-B.md sha256:2764d1523df98b01 |
| A 원본 지적 수 | 4 |
| B 원본 지적 수 | 4 |
| 결정표 전체 행 수 | 8 |
| 판단한 사용자 | join5201. 2026-09-11 초안 8건을 그대로 확정했다. 사용자 발언은 결정 8건 초안대로 확정하고 반영 진행해라 |
| 결정 날짜 | 2026-09-11 |

A는 치명 1, 보통 2, 확인필요 1이다. B는 치명 1, 보통 1, 확인필요 2다. 합쳐서 치명 2, 보통 3, 확인필요 3이다.

치명 둘은 같은 문제다. A-01과 B-01 모두 숙박 날짜의 오늘을 UTC로 계산한다는 지적이고, 서로 독립인 두 평가자가 같은 자리를 짚었다. 원본 행은 둘 다 남긴다.

## 지적별 결정

지적 ID 규약: S{Step}-R{라운드}-{A 또는 B}-{원본 번호 2자리}.

| 리포트 A/B | 원본 번호 | 지적 ID | 심각도 | 대상 위치 | 위반 축 | 위반 기준 | 원문 지적 | 원문 근거 | 수정 제안 | 결정 | 이유 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| A | 01 | S9-R1-A-01 | 치명 | `backend/src/main/java/com/o2o/inventory/application/InventoryApplicationService.java:265`, `backend/src/main/java/com/o2o/shared/ClockConfiguration.java:23`, `backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java:63` | API 계약 준수, 테스트 격리와 재현성 | 11 명세 35행 | 명세는 숙박 날짜를 Asia/Seoul 기준으로 정했지만 구현과 테스트는 UTC 날짜를 사용한다. 한국 시각 00:00부터 08:59에는 서울 기준으로 이미 지난 날짜를 등록하거나 수정할 수 있다. | `document/11-o2o-api-spec.md` 35행은 숙박 날짜의 Asia/Seoul 기준을 명시한다. 서비스 266행은 `ZoneOffset.UTC`, Clock 설정 25행은 `Clock.systemUTC()`, API 테스트 64행은 `LocalDate.now(ZoneOffset.UTC)`를 사용한다. 구현과 테스트가 같은 잘못된 기준을 공유하므로 테스트가 오류를 잡지 못한다. | Instant용 Clock은 유지하되 오늘 계산에 `ZoneId.of("Asia/Seoul")`을 사용한다. UTC와 서울 날짜가 갈리는 고정 시각 테스트를 추가한다. | 수용 | 대조 결과 사실이다. 11 명세 35행은 날짜를 Asia/Seoul, 시각을 UTC로 가른다. 서비스 264행 주석이 오늘의 기준을 UTC라고 적었는데 그것이 공통 절을 잘못 읽은 것이다. 시각이 UTC인 것과 날짜가 UTC인 것은 다르다. 한국 새벽 아홉 시간 동안 서울 기준 어제가 등록된다. 반영은 오늘 계산에 Asia/Seoul을 쓰고 그 지역을 shared 한 곳에 두는 것이다. 다음 묶음의 캠페인 날짜도 같은 기준이라 두 번째 사용처가 생기는 셈이다. 테스트는 UTC 날짜와 서울 날짜가 갈리는 고정 시각을 하나 두고, 실제 포트 테스트의 상대 날짜도 서울 기준으로 바꾼다 |
| A | 02 | S9-R1-A-02 | 보통 | `backend/src/main/java/com/o2o/inventory/infrastructure/DailyInventoryJpaRepository.java:30`, `backend/src/main/java/com/o2o/inventory/infrastructure/DailyRateJpaRepository.java:25`, `backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java:288` | 계약 하강, 동시성과 트랜잭션 | 계약 7절 D-2, 06-4 0절 | 비관적 락 구현은 있지만 실제 MySQL에서 두 트랜잭션이 경합하는 검증이 없다. 현재 테스트는 지난 version을 순차 호출로만 검사한다. | 재고와 요금 저장소에는 각각 `PESSIMISTIC_WRITE`가 있다. 그러나 대상 테스트 63건에는 동시 요청, 락 대기, 잠금 후 상태 재검사 시나리오가 없다. 계약 D-2가 선택한 비관적 락 검증이 테스트까지 내려가지 않았다. | 재고와 요금 수정 각각에 두 트랜잭션을 동시에 실행하는 MySQL 통합 테스트를 추가한다. 두 번째 요청이 잠금 이후 최신 상태를 읽고 VERSION_CONFLICT 또는 불변식 오류로 끝나는지 확인한다. | 수용 | 사실이다. 테스트 폴더 전체에 스레드나 래치를 쓰는 파일이 없다. 생성자 검증 문서도 T05의 잠금 몫을 예약 묶음으로 미룬다고 스스로 적었는데, 계약 D-2가 이번 묶음의 결정이므로 그 검증도 이번 묶음의 일이다. 미룬 판단이 틀렸다. 반영은 같은 행에 같은 version으로 두 요청을 동시에 보내 하나만 200이고 하나는 409이며 저장된 값이 200 쪽인 것을 보는 MySQL 통합 테스트다. 재고와 요금 각각 하나씩이다 |
| A | 03 | S9-R1-A-03 | 보통 | `backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java:243`, `backend/src/test/java/com/o2o/inventory/api/InventoryQueryApiTest.java:245` | 계약 하강 | 계약 8-1 V9, 11 명세 198행과 259행 | V9가 재고 등록과 수정, 요금 등록, 재고 조회만 검사한다. RATE-02의 과거 날짜 수정 거부와 RATE-03, RATE-04의 과거 날짜 조회 허용은 테스트가 없다. | `InventoryApiTest`의 V9는 재고 등록, 재고 수정, 요금 등록까지만 호출한다. `InventoryQueryApiTest`의 과거 조회는 재고 기간과 재고 단건만 호출한다. | 과거 날짜 RATE-02가 400 INVALID_DATE_RANGE인지, 과거 RATE-03과 RATE-04가 정상 조회되는지 실제 포트 테스트를 추가한다. | 수용 | 사실이다. InventoryApiTest 244행의 V9는 세 호출이고 요금 수정이 없다. InventoryQueryApiTest 246행의 과거 조회는 재고 둘뿐이다. 요금 네 경로 중 등록 하나만 V9에 걸려 있다. 반영은 요금 수정의 과거 거부 하나와 요금 기간 조회와 단건 조회의 과거 허용 둘을 기존 두 테스트에 붙이는 것이다 |
| A | 04 | S9-R1-A-04 | 확인필요 | `backend/src/main/java/com/o2o/inventory/api/InventoryController.java:50`, `backend/src/main/java/com/o2o/inventory/api/RegisterInventoryRequest.java:19`, `backend/src/main/java/com/o2o/inventory/api/RegisterRateRequest.java:20` | API 계약 준수 | 11 명세 38행과 74행 | 명세는 정의되지 않은 JSON, Path, Query 필드를 400으로 거부한다. 허용 입력에서는 알 수 없는 JSON 필드와 추가 Query 파라미터를 거부하는 전역 설정이나 테스트가 확인되지 않는다. | 대상 DTO와 컨트롤러에는 개별 필드 검증만 있고, 전달된 HTTP 기록에도 추가 필드 요청이 없다. 전역 Jackson 및 MVC 설정은 허용 입력 밖이라 읽지 않았다. | 등록 요청에 알 수 없는 JSON 필드, 조회 요청에 알 수 없는 Query 필드를 넣어 400 INVALID_REQUEST를 확인하는 실제 포트 테스트를 추가한다. | 반박 | 확인했다. JSON 쪽은 이미 되어 있다. backend/src/main/resources/application.properties 7행이 fail-on-unknown-properties=true를 전역으로 켜고, 앞 묶음의 CatalogApiTest 155행 C6이 정의되지 않은 필드를 넣은 요청이 400 INVALID_REQUEST인 것을 실제 포트로 고정한다. 전역 설정이라 이번 묶음의 DTO에도 같이 걸린다. Query 쪽은 명세가 400을 요구하지 않는다. 38행이 400이라 적은 것은 JSON 필드뿐이고 74행은 표시하지 않은 Query 필드를 지원하지 않는다고만 적는다. 평가자가 못 본 이유는 설정 파일과 앞 묶음 테스트가 허용 입력 밖이어서다. 다음 라운드 허용 입력에 application.properties를 넣는다 |
| B | 01 | S9-R1-B-01 | 치명 | `backend/src/main/java/com/o2o/inventory/application/InventoryApplicationService.java:265`, `backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java:63` | 추적성 양방향, 결정 근거의 자립성 | 11 명세 35행, P03 | 명세는 숙박 날짜를 Asia/Seoul 기준으로 정했지만 구현과 테스트는 UTC 날짜를 사용한다. 한국 시각 00:00부터 08:59에는 서울 기준으로 이미 지난 날짜를 등록하거나 수정할 수 있다. | `document/11-o2o-api-spec.md` 35행과 P03은 숙박 날짜의 Asia/Seoul 기준을 명시한다. 서비스 266행은 `ZoneOffset.UTC`, API 테스트 64행은 `LocalDate.now(ZoneOffset.UTC)`를 사용한다. | 오늘 계산을 `ZoneId.of("Asia/Seoul")` 기준으로 바꾸고 UTC 날짜와 서울 날짜가 갈리는 시각의 경계 테스트를 추가한다. | 수용 | A-01과 같은 문제다. 대조 결과와 반영 방법은 A-01 행에 적었다. 서로 독립인 두 평가자가 같은 줄을 짚었으므로 오판일 가능성은 낮다. 계약 6절의 P03 행이 날짜는 서버 기준 오늘로 판정한다고 적었는데 그 문장이 기준 지역을 비워 두어 이 착오를 막지 못했다. 반영 때 계약 6절의 그 행에 Asia/Seoul을 적는다 |
| B | 02 | S9-R1-B-02 | 보통 | `backend/src/main/java/com/o2o/inventory/domain/DailyInventory.java:123`, `backend/src/test/java/com/o2o/inventory/domain/DailyInventoryTest.java:33` | 요구사항 역추적 | 계약 5절과 8-1 V2 | I1a 검사는 코드에 있지만 `soldCount < 0`과 `heldCount < 0`을 직접 확인하는 테스트가 없다. 계약은 V2를 다음 묶음으로 이월했지만 이번 평가 요청은 불변식 셋 모두를 어느 테스트가 확인하는지 짚도록 요구한다. | `DailyInventory.validateCounts` 124행에 가드가 있다. `DailyInventoryTest`의 7개 테스트는 totalCount와 version만 바꾸며 soldCount와 heldCount의 음수 경계를 만들지 않는다. | I1a를 이번 묶음의 역추적 대상에서 제외한다고 계약과 평가 요청을 맞추거나, 실제 수량 변경 경로가 생기는 묶음에서 대응 테스트를 필수로 연결한다. | 수용 | 사실이다. 계약 5절 120행은 역추적 대상을 불변식 셋이라 적고 8-1 218행은 V2를 이월한다고 적는다. 같은 문서가 두 말을 한다. 코드는 틀리지 않았고 이월 사유도 8-1에 있다. 반영은 문서 정정이다. 계약 5절 문장을 불변식 셋 중 I1a는 8-1 V2로 이월한다로 고치고 개정 1로 기록한다. 그리고 예약 묶음 계약의 8-1에 V2를 필수로 올린다. 테스트를 이번에 억지로 만들지 않는 이유는 8-1이 이미 적었다. 수량을 내리는 경로가 없어 없는 경로를 테스트하면 그 테스트가 무엇을 지키는지 흐려진다 |
| B | 03 | S9-R1-B-03 | 확인필요 | `backend/src/main/java/com/o2o/inventory/application/InventoryApplicationService.java:102` | 추적성 양방향 | 06-2 85행부터 94행 | 06-2는 이벤트를 커밋 후 발행한다고 명시하지만 대상 코드는 트랜잭션 메서드 안에서 `publishEvent`를 호출한다. 허용 입력에는 구독자와 전달 설정이 없어 실제 처리가 커밋 후로 지연되는지 판정할 수 없다. | `document/06-2-o2o-aggregates.md` 85행부터 94행은 커밋 후 발행을 명시한다. 서비스는 102, 138, 159, 178, 195행에서 직접 발행한다. 대상 코드에는 커밋 후 전달을 보장하는 장치가 없다. | 구독자와 이벤트 전달 설정을 평가 입력에 포함하고, 롤백 시 외부 효과가 발생하지 않는 테스트를 추가한다. | 수용 | 확인했다. 06-2 94행이 이벤트 발행은 커밋 후라고 적고, 서비스 다섯 곳이 @Transactional 안에서 publishEvent를 부른다. 앞 묶음의 CatalogApplicationService 네 곳도 같다. 구독자가 하나도 없어서 지금은 겉으로 드러나지 않지만 어긋난 것은 맞다. 반영은 발행 코드를 고치는 것이 아니라 구독자 규칙이다. 스프링은 구독자를 AFTER_COMMIT으로 걸면 커밋 뒤에만 받는다. 발행 쪽을 커밋 뒤로 옮기면 트랜잭션을 되돌리는 이벤트 테스트 다섯이 전부 깨진다. 규칙을 backend/CLAUDE.md에 한 줄 적고, 첫 구독자가 생기는 묶음에서 롤백 시 구독자가 받지 않는 테스트를 필수로 올린다. 이번 라운드 코드 변경은 없다 |
| B | 04 | S9-R1-B-04 | 확인필요 | `backend/src/main/java/com/o2o/inventory/api/RegisterInventoryRequest.java`, `backend/src/main/java/com/o2o/inventory/api/RegisterRateRequest.java` | 추적성 양방향 | 11 명세 38행과 74행 | 명세는 정의되지 않은 JSON 필드를 400으로 거절한다. 대상 DTO와 테스트에서는 이를 확인할 수 없고 공통 Jackson 설정은 허용 입력 밖이다. | `document/11-o2o-api-spec.md` 38행과 74행은 미정의 필드 거절을 요구한다. 대상 API 테스트에는 미정의 필드 요청이 없다. | 공통 역직렬화 설정을 다음 평가 입력에 포함하거나 미정의 필드가 400인지 실제 포트 테스트로 고정한다. | 반박 | 확인했다. A-04와 같은 자리다. application.properties 7행의 전역 설정과 CatalogApiTest 155행 C6이 이미 있다. 평가자 스스로 공통 설정이 허용 입력 밖이라고 적었고 그 말이 맞다. 다음 라운드 허용 입력에 application.properties와 그 설정을 검사하는 앞 묶음 테스트 C6을 넣는다 |

원문 지적과 근거는 상세 표의 문장을 그대로 옮겼다. 요약하거나 자르지 않았다.

## 사용자 추가 지적

| 추가 ID | 심각도 | 대상 위치 | 지적 | 근거 | 처리 |
|---|---|---|---|---|---|
| S9-R1-U-01 | 확인필요 | harness/prompts/eval-criteria-code.md 5절 | 출력 스키마 절 제목이 보조 표 추가라고 적는데 스키마 본문에 보조 표가 없다. 두 평가자가 스키마대로 썼고 둘 다 보조 표를 내지 않았다. g2가 그 표로 심각도를 읽어서 이 결정표의 기계 검사가 막힌다 | 5절 코드 블록에 판정 요약, 상세, 요구사항 역추적표, 실행 기록, 검증 ID 결과 다섯만 있다. decision-table.md v4는 원본 리포트에 원본 번호, 지적 ID, 심각도 세 열의 보조 표가 있어야 한다고 적는다 | 하네스 세션 몫. 이슈 104로 넘겼다. 이번 라운드는 아래 G2 확인 절에 미완료로 적는다 |

## 치명 지적의 오판 판단 기록

치명 둘을 모두 수용했다. 오판 정정을 주장하는 행이 없어 이 절은 비운다.

## 반영 전 G2 확인

- [x] 평가 대상, 계약, A/B 리포트의 버전이 결정표와 일치한다.
- [ ] A와 B 원본의 지적 ID 집합과 결정표의 지적 ID 집합이 같다. 누락, 추가, 중복이 없다. 사람이 대조했고 여덟이 일치한다. 기계 검사는 원본에 보조 표가 없어 대조를 시작하지 못했다
- [x] 원본의 각 행과 결정표를 대조했고, 결정과 이유 외의 정보가 동일하다.
- [x] 전체 행 수가 A와 B 원본 지적 수의 합과 같다. 지적 수가 0이면 리포트가 실제로 정상 완료되었는지 확인했고 파싱 실패나 평가 미완료를 0건으로 취급하지 않았다.
- [x] 모든 행의 결정이 수용, 거부, 반박 중 하나다. 빈 결정이 없다.
- [x] 모든 거부에 이유가 있고, 반박은 원본 심각도가 확인필요인 행에만 있다.
- [x] 치명 오판 정정을 주장한 행마다 근거, 대안, 안티패턴, 사용자 결정, 날짜가 원본 지적에 연결되어 있다. 해당 행 없음.

| 확인 항목 | 기록 |
|---|---|
| 확인자와 날짜 | g2 실행은 2026-09-11 Claude Code. 사람 서명 대기 |
| G2 결과 | 미완료 |
| 미완료 사유 | 원본 리포트 둘에 보조 표가 없어 g2가 report-schema에서 멈춘다. 원인은 사용자 추가 지적 U-01이다. 평가자 잘못이 아니라 기준 파일의 누락이다. 푸는 길은 둘이다. 평가자에게 보조 표 추가를 요청해 원문을 평가자가 완성하게 하거나, 하네스가 g2를 상세 표의 ID와 심각도 열로도 읽게 고친다. 원문을 생성 세션이 손대지 않는다 |

## 반영과 최종 확인 인계

| 확인 대상 | 기록 |
|---|---|
| 반영본 경로와 버전 또는 해시 | 반영 후 작성 |
| 수용 항목 반영 확인 | 반영 후 작성. 대상은 A-01과 B-01의 지역 기준, A-02의 동시 요청 테스트 둘, A-03의 요금 과거 테스트 셋, B-02의 계약 문장 정정, B-03의 구독자 규칙 한 줄 |
| 미수용 항목 무변경 확인 | 반영 후 작성. 반박 둘은 코드 변경이 없어야 한다 |
| 남은 실제 치명 지적 | 반영 전이라 A-01과 B-01 |
| 오판 정정 완료 항목 | 없음 |
| 마지막 반영본에 필요한 검증과 결과 | 반영 후 작성. 전체 테스트와 g1 code, 그리고 R2 재평가 |
| 코드 작업의 직접 실행 근거 | 반영 후 작성 |
| 평가 대상 소스와 테스트의 무변경 확인 | 평가 전후 대조. A와 B가 적은 50개 파일의 sha256이 서로 같고 verify-eval-workspace.mjs의 ws.target-drift가 PASS였다. 평가 중 바뀐 파일은 없다 |
| 검증 미완료 사항 | g2 기계 검사. 사유는 위 G2 확인 절 |
| 사용자 최종 완료 판단과 날짜 | 미완료. 반영과 R2 전이다 |
| 실제 판단 및 확인에 사용한 시간 | 초안 작성 28분. 사용자 판단 시간은 미측정 |
