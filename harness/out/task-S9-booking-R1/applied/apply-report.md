# task-S9-booking R1 반영 기록 (예약 1차와 2차 합본)

양식: harness/prompts/apply.md v3 (2026-09-08)
최초 작성: 2026-09-15
최종 갱신: 2026-09-15 (병합 뒤 실행 표를 6절에. 그 전 같은 날 최초 작성)

왜 이 파일이 필요한가: 반영본만 내면 어느 지적이 어디로 갔는지 사람이 다시 대조해야 한다. 지적 ID와 변경 위치를 짝지어 두면 최종 확인에서 이 표만 보면 된다. 코드 반영이라 문서 반영과 달리 diff와 테스트 숫자와 변이 검사가 근거다. 라운드 mvp-eval-2026-09-14 쌍 4의 반영이고 쌍 1 숙소의 기록(harness/out/task-S9-catalog-R1/applied/apply-report.md)과 같은 골격이다.

## 1. 이번 반영

| 항목 | 내용 |
|---|---|
| Task ID와 Step | task-S9-booking(1차)과 task-S9-booking-lifecycle(2차), Step 9. 라운드가 둘을 한 쌍으로 평가했다 |
| 작업 유형 | 코드 |
| 평가 라운드 | R1. 라운드 mvp-eval-2026-09-14 쌍 4 |
| 평가받은 대상 버전 | 기준 커밋 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1(PR 138 병합, main). 파일 목록 harness/out/mvp-eval-2026-09-14/eval-target-files-booking.md sha256:79859e3f5bb5bddd. 평가 브랜치 eval/mvp-2026-09-14 |
| 반영 범위 | 결정표의 수용 5건(A-01, A-03, A-04, B-01, B-02)과 거부 1건(A-02). 코드 조치는 넷이다. A-01 멱등 실행기의 검사 순서, A-03 목록 응답 모델의 shared 사용, B-01 재고 version 증가, B-02 승인과 만료의 기준 시점. A-04는 수용이되 코드 변경이 아니라 Docker를 켠 환경에서 전체 테스트를 다시 돌리는 것이 조치다. 계약 개정은 둘이다(1차 개정 4, 2차 개정 11) |
| 수정 후보 위치 | 브랜치 fix/task-s9-booking-r1-apply. 평가 브랜치 끝 821fd45(결정 확정 커밋)에서 땄다. 코드 변경은 77b7a7f부터 f8626d1까지 여덟 커밋이고 이유 하나에 커밋 하나다. 계약 개정은 2e43852(1차)와 a1344a7(2차) |
| 결과 기록 경로 | harness/out/task-S9-booking-R1/applied/apply-report.md. diff 전문은 같은 폴더 apply.diff(821fd45와 f8626d1 사이 backend/), 테스트 결과 사본은 junit/ 아래 48개 |
| 사용자 결정 | 결정 6건 초안대로 확정. join5201, 2026-09-15. 발언은 초안대로 반영해라. B-02의 기준 시점은 초안인 승인 기록의 서버 시각이다. 결정표는 harness/decisions/task-S9-booking-R1.md |
| 작업 공간 | 워크트리 C:/Dev/potenup/99_projects/o2o-payment. o2o-dev는 다른 세션(결제 쌍)이 쓰고 있어 손대지 않았다. 테스트 DB는 컨테이너 o2o-catalog-mysql의 o2o_fix_test다. 평가 시각에 꺼져 있던 Docker Desktop을 사용자가 켰고 이 세션이 컨테이너를 start했다. A-04가 요구한 실행 조건이 그것이다 |

## 2. 반영본

브랜치 시작 821fd45에서 코드 마지막 커밋 f8626d1까지 backend/ 아래 바뀐 파일은 아래 15개뿐이다(프로덕션 6, 삭제 1, 테스트 8). 기준 커밋 1bdadfe와 821fd45 사이 backend/ 차이는 backend/README.md 6절 진척 표 한 곳(PR 139)이고 평가 대상 파일은 없다. 다른 세션의 변경이 섞이지 않았다. diff 전문은 apply.diff(809행)다.

프로덕션 6개.

| 파일 | 무엇이 바뀌었나 | 해시 |
|---|---|---|
| backend/src/main/java/com/o2o/inventory/domain/DailyInventory.java | hold, commit, releaseHeld, releaseSold가 수량을 바꾼 뒤 version에 1을 더한다(151, 168, 183, 198행). adjust의 것(117행)은 그대로. version 필드 주석에 이유 | sha256:f3db009b458c97b5 |
| backend/src/main/java/com/o2o/booking/application/IdempotentRequestExecutor.java | 기존 기록이 있으면 body 대조(59행)를 완료 여부(62행)보다 먼저 한다. 머리 주석을 검사 순서에 맞게 다시 썼다 | sha256:be3da6a8a4d76d10 |
| backend/src/main/java/com/o2o/booking/domain/Booking.java | acceptsApprovalAt(approvedAt) 신설(368행). 승인 시각이 expiresAt 전이면 참. 정확히 expiresAt이면 거짓(T18) | sha256:1f73daae67145e06 |
| backend/src/main/java/com/o2o/booking/application/PaymentOutcomeService.java | P1이 처리 시각 대신 이벤트의 occurredAt(68행)으로 확정과 지연 승인을 가른다(73행). 처리 시각은 전이의 시각에만 쓴다. 클래스 주석에 기준 시점 문단 | sha256:bafcebb0f322e6de |
| backend/src/main/java/com/o2o/booking/application/BookingExpirationService.java | T1이 승인 시도의 completedAt(81행, approvedAtOf 97행)으로 가른다. 만료 전 승인은 확정(82행), 만료 이상 승인은 환불(90행) 뒤 만료. 결제 뷰와 환불 사유 import 넷 | sha256:fee430d354774c2a |
| backend/src/main/java/com/o2o/booking/api/BookingController.java | shared의 PageResponse를 import(35행). BOOK-02 주석에 응답 모델의 출처 | sha256:cb78f0179d648c9e |

삭제 1개. backend/src/main/java/com/o2o/booking/api/PageResponse.java를 git rm으로 뺐고 사본은 tmp/_moved/backend-booking-api-2026-09-15/PageResponse.java에 있다(N2. tmp/는 추적 제외).

테스트 8개.

| 파일 | 무엇이 바뀌었나 | 해시 |
|---|---|---|
| backend/src/test/java/com/o2o/inventory/domain/DailyInventoryAllocationTest.java | K26 셋(147, 168, 182행). 성공마다 1 증가, 실패면 그대로, 선점 뒤의 낡은 version 조정은 VersionConflictException | sha256:df59638c939e7d38 |
| backend/src/test/java/com/o2o/booking/api/BookingApiTest.java | K22 기존 하나(578행)가 선점 뒤 version 1을 보내고 응답 version 1을 단언. K22 새 하나(600행) 선점 전 version 0은 409 VERSION_CONFLICT, 1은 200. K27 하나(402행) 처리 중 다른 body | sha256:e812411f52cc28b1 |
| backend/src/test/java/com/o2o/booking/api/BookingPaymentApiTest.java | L24 하나(388행). PAY-01 처리 중 다른 body | sha256:f22b4fbc3bcee4ac |
| backend/src/test/java/com/o2o/booking/api/BookingCancelApiTest.java | L25 하나(559행). BOOK-04 처리 중 다른 body | sha256:134174437c5c9404 |
| backend/src/test/java/com/o2o/booking/application/PaymentOutcomeServiceTest.java | L7 하나(131행). 승인 시각이 만료 전이면 처리 시각이 만료 뒤라도 확정이고 환불이 없다 | sha256:49baddad104a2c53 |
| backend/src/test/java/com/o2o/booking/application/ExpireDueBookingsTest.java | L9 하나(127행). 만료 시각 이상의 승인 기록은 확정 대신 환불 뒤 만료. 두 번째 호출은 SKIPPED이고 환불은 하나 | sha256:0f5ed97e857f9caa |
| backend/src/test/java/com/o2o/booking/application/BookingLockContentionTest.java | 공통 본문이 승인 시각을 인자로 받는다. L23 둘(103, 108행)은 결과 하나(CONFIRMED)만 허용. L26 둘(113, 119행) 신설. 승인 시각이 정확히 expiresAt이면 어느 순서든 EXPIRED와 환불 하나 | sha256:7d0d6ac680258698 |
| backend/src/test/java/com/o2o/booking/domain/BookingTest.java | K3 공개 메서드 목록에 acceptsApprovalAt(99행). 검사 내용 불변 | sha256:a141550f4740ec85 |

문서 쪽 반영은 계약 둘이다.

| 파일 | 무엇이 바뀌었나 | 해시 |
|---|---|---|
| harness/tasks/task-S9-booking.md | 개정 4. 0절 신설(고치는 곳 셋, 개정하지 않은 것 넷), 5절 R2 허용 입력에 shared PageResponse 행, 8-1의 K26과 K27, 10절 개정 4 행과 마지막 성공 단계 행. fill 151건 중 3건 실패이고 셋 전부 개정 전부터 있던 것(catalog와 inventory 계약과 application.properties의 낡은 해시. 2026-09-12 이후 그쪽이 바뀌었다) | sha256:36838aa51307d9f5 |
| harness/tasks/task-S9-booking-lifecycle.md | 개정 11. 0절 신설(기준 시점 한 문장과 이유, 고치는 곳 넷, 개정하지 않은 것 넷), 2절 P1 표의 시각 열과 T1 표의 승인 기록 행 둘, 8-1의 L7과 L9와 L23 문구와 L24부터 L26, 10절 개정 11 행과 마지막 성공 단계 행. fill 226건 중 6건 실패이고 여섯 전부 이번 반영이 바꾼 파일의 해시(1차 계약 둘, Booking, IdempotentRequestExecutor, BookingController, BookingApiTest)다. 4절 입력 표는 계약 승인 시점의 기록이라 고치지 않았고 0-3절이 그 이유를 적는다 | sha256:4fffcf69c5667fa1 |

## 3. 지적 ID별 변경 위치

| 지적 ID | 심각도 | 결정 | 변경 위치 | 무엇을 넣었나 |
|---|---|---|---|---|
| S9-R1-A-01 | 보통 | 수용 | IdempotentRequestExecutor의 execute, BookingApiTest K27, BookingPaymentApiTest L24, BookingCancelApiTest L25 | 기존 기록이 있으면 완료 여부와 무관하게 body 해시를 먼저 대조한다. 처리 중인 같은 키에 다른 body는 409 IDEMPOTENCY_KEY_REUSED이고 Retry-After가 없다. 세 멱등 API에 처리 중 다른 body 반례 하나씩. 잠금은 테스트 트랜잭션이 재고 행이나 예약 행을 잡아 첫 요청을 2초 붙든다 |
| S9-R1-A-02 | 보통 | 거부 | 코드 변경 없음 | 11 공통 규칙 74행은 표시하지 않은 Query 필드를 지원하지 않는다고만 적고 상태 코드를 정하지 않는다. 400 목록(38행)은 JSON body 오류뿐이다. 명세 결정이라 별도 이슈 몫. BookingController의 BOOK-02 처리와 K24는 그대로다(5절) |
| S9-R1-A-03 | 보통 | 수용 | BookingController의 import, booking/api/PageResponse.java 삭제(tmp/_moved로), 1차 계약 5절 R2 행 | shared/PageResponse.java는 프로모션 묶음(be5e510)이 이미 올렸고 평가자는 대상 목록 밖이라 못 봤다. booking 사본을 치우고 shared의 것을 쓴다. R2 평가자가 읽을 수 있게 계약 5절에 열었다. catalog/api의 사본은 이 계약 범위 밖이라 별도 이슈다(1차 계약 10절 미해결 사항) |
| S9-R1-A-04 | 확인필요 | 수용 | 코드 변경 없음. 6절의 전체 테스트 실행 | Docker Desktop이 켜진 환경에서 같은 명령으로 전체 테스트를 다시 돌렸다. 430건 실패 0(평가 전 419건에 이번 반영의 11건). A가 본 296건 실패는 전부 DB 연결 단계였고 코드 원인이 아니었다 |
| S9-R1-B-01 | 보통 | 수용 | DailyInventory의 hold와 commit과 releaseHeld와 releaseSold, DailyInventoryAllocationTest K26 셋, BookingApiTest K22 둘 | 수량을 바꾸는 다섯 경로 전부가 version에 1을 더한다(11 공통 규칙 43행). 호스트가 INV-03으로 본 version이 선점 뒤에는 낡아 409 VERSION_CONFLICT로 막힌다. 553행의 version 0 단언은 1로 바뀌었다 |
| S9-R1-B-02 | 확인필요 | 수용 | Booking.acceptsApprovalAt, PaymentOutcomeService.onApproved, BookingExpirationService.expireIfDue, BookingLockContentionTest L23과 L26, PaymentOutcomeServiceTest L7, ExpireDueBookingsTest L9, 2차 계약 0-1절과 P1 표와 T1 표 | 기준 시점은 승인 기록의 서버 시각 하나다. P1은 이벤트 occurredAt, T1은 승인 시도의 completedAt을 expiresAt과 대조한다. 만료 전 승인은 둘 다 확정, 만료 이상 승인은 둘 다 환불 뒤 TTL_EXPIRED. 같은 상태에서 잠금 순서와 무관하게 답이 하나다. 결제 패키지는 고치지 않았다(값이 이미 있다) |

## 4. 결정표와 다르게 간 것과 실측

| 항목 | 결정표 | 실제 | 왜 |
|---|---|---|---|
| B-02의 비교 자리 | P1 65행의 조건을 승인 시각으로 | 비교를 Booking.acceptsApprovalAt 한 곳에 두고 P1과 T1이 같은 메서드를 부른다 | 두 서비스가 각자 isBefore를 쓰면 경계(정확히 expiresAt)가 다시 갈릴 수 있다. 도메인 한 곳이면 T18 경계가 한 줄이다 |
| B-02의 테스트 | L23을 결과 하나로, 경합 반례 하나 더한다 | L23 둘과 L26 둘에 더해 L7 하나(P1 처리 시각이 만료 뒤라도 승인이 만료 전이면 확정)와 L9 하나(T1의 만료 이상 승인은 환불 뒤 만료)를 넣었다 | 경합 테스트는 스레드 둘의 잠금 순서를 보는 것이라 두 서비스 각각의 단독 분기를 짧게 고정하는 테스트가 따로 있어야 변이 검사가 어느 쪽 분기를 잡는지 가려진다 |
| K3 guard | 없음 | BookingTest K3의 공개 인스턴스 메서드 목록에 acceptsApprovalAt을 더했다 | K3는 Booking의 공개 메서드 목록을 통째로 단언한다. 첫 전체 실행에서 그 한 건이 실패했고 새 메서드는 상태를 바꾸지 않는 조회라 목록에 넣는 것이 맞다 |
| 개정 번호 | 2차 계약 개정 | 코드 주석과 커밋 e9d00cf, 594980b의 본문이 개정 3이라고 적었다. 2차 계약은 개정 10까지 있었으므로 11이 맞고 f8626d1이 주석을 정정했다. 커밋 메시지는 그대로다 | 커밋 메시지를 고치려면 이력을 다시 쓰게 되므로 정정 커밋으로 남겼다 |
| A-04의 확인 대상 | 기준 커밋 1bdadfe의 419건 | 반영 뒤 430건으로 확인했다. 기준 커밋 자체의 재실행은 따로 하지 않았다 | 반영본이 기준 커밋을 포함하고 새 11건 밖의 419건이 그대로 통과했다. 기준 커밋만 따로 돌리면 같은 419건이 한 번 더 돈다 |

## 5. 미수용 항목 무변경 확인

거부 A-02가 짚은 자리는 BookingController의 BOOK-02 처리(list 메서드의 파라미터와 검사)와 K24다. BookingController는 A-03으로 바뀌었지만 diff는 import 한 줄과 BOOK-02 주석뿐이고 파라미터와 검사와 응답은 그대로다(apply.diff의 BookingController 부분). BookingApiTest의 K24는 바뀌지 않았다.

2절의 15개 밖은 backend/ 아래 어느 파일도 바뀌지 않았다. git diff --name-only 821fd45 f8626d1 -- backend/ 가 그 15개만 낸다. 결제 패키지(payment/)는 한 파일도 바뀌지 않았다. 동결 대상(backend/build.gradle, backend/CLAUDE.md, harness/prompts, harness/tools, document)은 손대지 않았다.

## 6. 검사

| 명령 | 결과 |
|---|---|
| JAVA_HOME=JDK 21, SPRING_DATASOURCE_URL=o2o_fix_test, backend 디렉터리에서 ./gradlew test --rerun-tasks (Git Bash) | BUILD SUCCESSFUL. 48클래스 430건, 실패 0, 오류 0, 건너뜀 0. 평가 전 419건에 11이 늘었다(K26 셋, K22 하나, K27 하나, L24 하나, L25 하나, L7 하나, L9 하나, L26 둘). Docker Desktop이 켜진 환경이라 A-04의 조건을 채운다 |
| node harness/tools/check.mjs g1 <파일> --type code --artifact applied/junit/*.xml 48개 | 2절의 프로덕션 6개와 테스트 8개 전부 100건 통과. 테스트 430건 |
| node harness/tools/check.mjs fill harness/tasks/task-S9-booking.md | 151건 중 3건 실패. 셋 전부 개정 전부터 있던 것이고 새 행은 통과. 2절 문서 표 참조 |
| node harness/tools/check.mjs fill harness/tasks/task-S9-booking-lifecycle.md | 226건 중 6건 실패. 여섯 전부 이번 반영이 바꾼 파일의 해시이고 0-3절이 이유를 적는다 |
| node harness/tools/check.mjs g2 harness/decisions/task-S9-booking-R1.md --mode final | 결정표 인계 표를 채운 뒤 실행. 결과는 결정표 인계 표에 |

테스트 결과 사본은 같은 폴더의 junit/ 아래 48개다.

병합 뒤 실행. PR 전에 origin/main(cd9de62. 숙소 쌍의 R1 반영 PR 145와 프론트 PR 149부터 154)을 브랜치에 병합했고(8ab9a5a) 충돌은 없었다. union 검사 8건 통과. 병합한 나무에서 같은 명령을 다시 돌렸다.

| 명령 | 결과 |
|---|---|
| 병합 커밋 8ab9a5a에서 ./gradlew test --rerun-tasks (같은 환경) | BUILD SUCCESSFUL. 50클래스 454건, 실패 0, 오류 0, 건너뜀 0(2026-09-15 19:11). 숙소 반영의 443건에 이번 11건이 더해진 수와 같다. XML 50개는 같은 폴더의 junit-merged/ 아래 |

새 테스트가 실제로 결함을 잡는지 변이 검사로 확인했다. 고친 코드를 잠깐 되돌리고 테스트가 빨개지는지 본 것이다. 파일의 존재나 초록만 보지 않는다(루트 CLAUDE.md 5-1절).

| 되돌린 것 | 돌린 테스트 | 결과 | 뜻 |
|---|---|---|---|
| 실행기의 순서를 되돌림(완료 여부를 body 대조보다 먼저) | K27, L24, L25, K17 넷 | 4건 중 3건 실패(K27, L24, L25). 완료 뒤 다른 body의 K17은 통과 | 새 셋이 A-01의 결함을 잡는다. 기존 K17은 완료 기록만 보므로 순서 결함을 못 잡았다 |
| 네 메서드의 version 증가를 뗌(adjust의 것은 남김) | DailyInventoryAllocationTest, DailyInventoryTest, BookingApiTest의 K22 둘. 22건 | 22건 중 5건 실패(K26 셋, K22 둘). 나머지 17건 통과 | K26과 K22가 B-01의 결함을 잡는다. 수량 검사는 version과 무관하게 통과한다 |
| P1을 처리 시각 기준으로 되돌리고 T1의 지연 승인 분기를 뗌 | BookingLockContentionTest, PaymentOutcomeServiceTest, ExpireDueBookingsTest. 20건 | 20건 중 4건 실패(L7 새 하나, L9 새 하나, L23 승인 먼저, L26 만료 먼저). 나머지 16건 통과 | L7이 P1의 분기를, L9가 T1의 분기를, L23과 L26이 잠금 순서에 따른 갈림을 잡는다. 기존 L7 둘(T18 경계와 1초 전)은 되돌려도 통과하므로 그것만으로는 B-02를 못 잡았다 |

되돌린 코드는 검사 뒤 원래대로 복구했고 2절의 해시가 복구된 상태다. 변이 검사가 Gradle의 결과 폴더를 지우므로 전체 테스트를 마지막에 한 번 더 돌려 그 XML을 junit/에 복사했다.

## 7. 남은 문제

| 항목 | 상태 | 왜 |
|---|---|---|
| catalog/api의 PageResponse 사본 | 별도 이슈 | 1차 계약 3절 범위 밖이다. 1차 계약 10절 미해결 사항이 이미 별도 이슈 몫으로 적었다. 이 반영으로 사본은 catalog 하나만 남았다 |
| 11 명세 2179행의 문구 | 사용자 몫 | 전이 검사 시각을 기준으로 판단한다는 문장이 P1의 새 기준(승인 시각)과 어긋난다. document/가 동결이라 계약 0-1절에 기준 시점을 적고 명세는 사용자가 고친다 |
| 표시하지 않은 Query 필드의 상태 코드 | 별도 이슈 | A-02 거부의 짝. 명세가 정하면 코드가 따른다. 11 공통 규칙 74행 |
| 재고 R2 | 이 반영을 가리킨다 | B-01이 고친 DailyInventory는 재고 묶음의 파일이다. 재고 쌍의 결정표는 이 결정표의 B-01 행을 가리키면 된다 |
| 병합 | 사용자 몫 | PR을 올리고 멈춘다. 병합은 사용자가 한다 |
| 1차 계약 fill의 세 건 | 그대로 | 2026-09-12 이후 다른 묶음이 바꾼 파일의 해시이고 이번 반영 범위 밖이다(N7) |

## 8. 재평가

재평가는 최대 1회이고 아직 쓰지 않았다. 결정표가 R2를 쓰지 않는다고 적었다(여섯 건이 전부 코드로 확인되거나 거부되어 리포트의 쓸모가 줄지 않았다). 사용자가 R2를 원하면 기준 커밋은 f8626d1이고 허용 입력은 두 계약 5절의 R2 행이다.
