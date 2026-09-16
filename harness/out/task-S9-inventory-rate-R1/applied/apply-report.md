# task-S9-inventory-rate R1 반영 기록

양식: harness/prompts/apply.md v3 (2026-09-08)
최초 작성: 2026-09-11
최종 갱신: 2026-09-11

왜 이 파일이 필요한가: 반영본만 내면 어느 지적이 어디로 갔는지 사람이 다시 대조해야 한다. 지적 ID와 변경 위치를 짝지어 두면 최종 확인에서 이 표만 보면 된다. 코드 반영이라 문서 반영과 달리 diff와 테스트 숫자와 변이 검사가 근거다.

## 1. 이번 반영

| 항목 | 내용 |
|---|---|
| Task ID와 Step | task-S9-inventory-rate, Step 9 |
| 작업 유형 | 코드 |
| 평가 라운드 | R1 |
| 평가받은 대상 버전 | 기준 커밋 280a69163a6a64225bb414969a134af22d5cefbe. 파일 목록 harness/out/task-S9-inventory-rate-R1/eval-target-files.md sha256:807b6239259b4107 |
| 반영 범위 | 결정표의 수용 6건. 조치는 5건이다. A-01과 B-01이 같은 자리라 하나로 닫힌다. 반박 2건은 코드 변경 없음 |
| 수정 후보 위치 | 브랜치 fix/task-s9-inventory-rate-r1-apply. 코드 변경의 마지막 커밋 406ed43 |
| 결과 기록 경로 | harness/out/task-S9-inventory-rate-R1/applied/apply-report.md |
| 사용자 결정 | 결정 8건 초안대로 확정. join5201, 2026-09-11 |

## 2. 반영본

기준 커밋 280a691에서 커밋 406ed43까지 backend/ 아래 바뀐 파일은 아래 일곱뿐이다. 다른 세션의 변경이 섞이지 않았다. diff 전문은 같은 폴더의 apply.diff다.

| 파일 | 무엇이 바뀌었나 | 해시 |
|---|---|---|
| backend/src/main/java/com/o2o/shared/SeoulDate.java | 신설. Asia/Seoul 상수와 today(Clock) | sha256:6950bcc6a2b09ead |
| backend/src/main/java/com/o2o/inventory/application/InventoryApplicationService.java | today()가 SeoulDate.today를 쓴다. 주석의 틀린 문장 정정 | sha256:e61d32aa9cf68272 |
| backend/src/main/java/com/o2o/shared/ClockConfiguration.java | 주석만. 빈은 systemUTC 그대로 | sha256:1d55f5e872302c21 |
| backend/src/test/java/com/o2o/inventory/application/InventoryApplicationServiceTest.java | 고정 시각을 UTC 20시로. V13 둘 추가 | sha256:a5a53be71918e23f |
| backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java | today()를 서울 기준으로. V14 둘 추가. V9에 RATE-02 과거 거부 추가 | sha256:df6b9c10b619bbae |
| backend/src/test/java/com/o2o/inventory/api/InventoryQueryApiTest.java | today()를 서울 기준으로. 과거 요금 조회 테스트 추가 | sha256:5c5b47dd333ca6d7 |
| backend/CLAUDE.md | 3-3절 이벤트 구독 규칙 E1과 E2 신설 | sha256:cc9d015d1d0dcaa9 |

문서 쪽 반영은 계약이다.

| 파일 | 무엇이 바뀌었나 | 해시 |
|---|---|---|
| harness/tasks/task-S9-inventory-rate.md | 개정 1. 5절 역추적 대상 문장과 R2 허용 입력 둘, 6절 P03 행의 지역, 8-1의 V13과 V14와 V2 행, 10절 개정 칸. fill 116건 통과 | sha256:4eb9f268cf82319a |

## 3. 지적 ID별 변경 위치

| 지적 ID | 심각도 | 결정 | 변경 위치 | 무엇을 넣었나 |
|---|---|---|---|---|
| S9-R1-A-01 | 치명 | 수용 | shared/SeoulDate.java 신설, InventoryApplicationService.java 265행 today(), ClockConfiguration.java 주석, InventoryApplicationServiceTest.java V13 둘, InventoryApiTest.java와 InventoryQueryApiTest.java의 today() | 오늘을 UTC가 아니라 Asia/Seoul로 자른다. Clock은 Instant를 주는 자리로 남긴다. 고정 시각 2026-10-01T20:00:00Z에서 UTC 날짜 10월 1일은 거부되고 서울 날짜 10월 2일은 등록된다 |
| S9-R1-B-01 | 치명 | 수용 | 같음. 계약 6절 P03 행 | A-01과 같은 조치가 닫는다. 계약 P03 행에 Asia/Seoul을 적어 지역을 비워 둔 문장을 고쳤다 |
| S9-R1-A-02 | 보통 | 수용 | InventoryApiTest.java V14 둘 | 테스트 트랜잭션이 행을 잠근 채 다른 스레드로 서버에 수정을 보낸다. 잠금이 풀리기 전 2초 동안 끝나지 않고, 풀린 뒤 그때의 version과 대조해 409를 낸다. 저장된 값은 잠금을 쥔 쪽이다. 재고와 요금 각각 |
| S9-R1-A-03 | 보통 | 수용 | InventoryApiTest.java V9 끝, InventoryQueryApiTest.java V9 과거 요금 조회 | RATE-02 과거 날짜 400 INVALID_DATE_RANGE. RATE-03과 RATE-04 과거 날짜 200 |
| S9-R1-A-04 | 확인필요 | 반박 | 코드 변경 없음. 계약 5절 R2 허용 입력 둘 | application.properties와 CatalogApiTest C6을 R2 허용 입력에 넣어 평가자가 근거를 볼 수 있게 했다 |
| S9-R1-B-02 | 보통 | 수용 | 계약 5절 역추적 대상 문장, 8-1 V2 행 | 불변식 셋 중 I1a는 V2로 이월했다고 적어 같은 문서의 두 말을 하나로. 예약 묶음 계약의 8-1 필수 항목으로 기록 |
| S9-R1-B-03 | 확인필요 | 수용 | backend/CLAUDE.md 3-3절 | 구독자는 @TransactionalEventListener로 건다(E1). 첫 구독자 묶음은 롤백 시 미수신 테스트를 필수로 올린다(E2). 발행 코드는 바꾸지 않았다 |
| S9-R1-B-04 | 확인필요 | 반박 | 코드 변경 없음. A-04와 같은 자리 | 같음 |

## 4. 미수용 항목 무변경 확인

반박 둘(A-04, B-04)이 가리킨 InventoryController.java, RegisterInventoryRequest.java, RegisterRateRequest.java는 기준 커밋과 같다. 2절의 일곱 파일 밖은 backend/ 아래 어느 파일도 바뀌지 않았다. git diff --stat 280a691 406ed43 -- backend/ 가 그 일곱만 낸다.

수용 항목과 미수용 항목이 겹치는 자리는 없다. A-04와 B-04가 짚은 DTO와 컨트롤러는 다른 지적이 건드리지 않는다.

## 5. 검사

| 명령 | 결과 |
|---|---|
| JAVA_HOME=JDK 21 ./backend/gradlew.bat -p backend test --rerun-tasks | BUILD SUCCESSFUL. 123건, 실패 0, 오류 0, 건너뜀 0. 평가 전 118건에 다섯이 늘었다 |
| node harness/tools/check.mjs g1 backend/src/main/java/com/o2o/shared/SeoulDate.java --type code --artifact applied/junit/*.xml 13개 | 30건 통과. 테스트 123건 |
| node harness/tools/check.mjs fill harness/tasks/task-S9-inventory-rate.md | 116건 통과 |

테스트 결과 사본은 같은 폴더의 junit/ 아래 13개다.

새 테스트가 실제로 결함을 잡는지 변이 검사로 확인했다. 고친 코드를 잠깐 되돌리고 테스트가 빨개지는지 본 것이다. 파일의 존재나 초록만 보지 않는다(루트 CLAUDE.md 5-1절).

| 되돌린 것 | 결과 | 뜻 |
|---|---|---|
| today()를 ZoneOffset.UTC로 | V13_오늘은_UTC가_아니라_서울_날짜다 실패. 9건 중 1건 | V13이 A-01의 결함을 잡는다. 서비스가 Clock의 zone으로 잘라도 같은 이유로 잡힌다 |
| adjustInventory의 findForUpdate를 findByRoomTypeIdAndStayDate로 | V14_잠긴_재고_행은 실패. 24건 중 1건. 순차 호출인 V6은 통과 | V14만 잠금 부재를 잡는다. A-02가 말한 대로 순차 호출 테스트는 잠금이 없어도 초록이다 |

되돌린 코드는 검사 뒤 원래대로 복구했고 2절의 해시가 복구된 상태다.

## 6. 남은 문제

| 항목 | 상태 | 왜 |
|---|---|---|
| g2 기계 검사 | 미완료 | 원본 리포트에 보조 표가 없다. 이슈 104. 하네스 세션 몫 |
| R2 재평가 | 대기 | 반영본을 새 Codex 작업 둘에서 다시 본다. 요청문은 harness/out/task-S9-inventory-rate-R2/에 만들 예정이고 기준 커밋은 406ed43이다. 허용 입력은 계약 5절의 R2 행 둘이 늘었다 |
| E2 규칙의 테스트 | 이월 | 구독자가 생기는 묶음의 8-1이 받는다 |
| V2 | 이월 | 예약 묶음 계약의 8-1 필수 항목 |

## 7. 재평가

재평가는 최대 1회이고 아직 쓰지 않았다. 치명 둘을 코드로 닫았으므로 R2는 그 반영이 맞는지와 새 테스트 다섯이 계약 8-1과 줄이 맞는지를 보는 것이 된다. 자료 전달은 사용자가 한다.
