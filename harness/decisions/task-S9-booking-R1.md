# 사용자 결정표 task-S9-booking R1

양식: harness/prompts/decision-table.md v4 (2026-09-08)
최초 작성: 2026-09-15
최종 갱신: 2026-09-15 (결정 6건 확정. 사용자 발언은 초안대로 반영해라. B-02의 기준 시점은 초안인 승인 기록 시각. 반영 인계 표는 반영 뒤 기입)

왜 이 파일이 필요한가: 리포트를 눈으로 읽고 반영하면 자기 결정에 불리한 지적이 조용히 빠진다(HR4). 원본 행을 하나씩 옮겨 두면 g2가 ID 집합과 심각도를 원본과 대조할 수 있다.

이 표의 행은 원본 리포트의 상세 표와 보조 표에서 셀 단위로 스크립트가 뽑아 만들었다(원문 지적, 근거, 수정 제안은 글자 그대로). 결정과 이유 열은 개발 세션이 초안을 적었고 사용자가 2026-09-15 초안대로 확정했다. 그 근거는 지적마다 실제 파일을 열어 대조한 것이다. 위치를 이유에 적었다. 라운드 mvp-eval-2026-09-14의 쌍 4다(harness/out/mvp-eval-2026-09-14/README.md). 라운드 README 5절 D-4 가에 따라 예약 1차와 2차를 한 쌍이 봤으므로 계약은 둘이다.

## 대상과 원본

| 항목 | 기록 |
|---|---|
| Step | 9 |
| 작업 유형 | 코드 |
| 평가 라운드 | R1 |
| 평가 대상 절대경로와 파일 목록 | harness/out/mvp-eval-2026-09-14/eval-target-files-booking.md |
| 평가 대상 버전 또는 해시 | sha256:79859e3f5bb5bddd. 목록이 가리키는 프로덕션 87개와 테스트 29파일 155건과 설정 1개의 기준 커밋은 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1(PR 138 병합, main)이고 파일별 sha256은 목록 2절과 A와 B 리포트의 읽은 파일 표에 있다. 평가 브랜치 eval/mvp-2026-09-14 |
| 승인된 작업 계약 절대경로와 버전 | harness/tasks/task-S9-booking.md sha256:0876574192269065, harness/tasks/task-S9-booking-lifecycle.md sha256:af5cba4e4077a2f0 |
| A 원본 리포트 절대경로와 버전 또는 해시 | harness/reviews/task-S9-booking-R1-A.md sha256:dd73a45092d4f880 |
| B 원본 리포트 절대경로와 버전 또는 해시 | harness/reviews/task-S9-booking-R1-B.md sha256:e9bd22e8ced18cf1 |
| A 원본 지적 수 | 4 |
| B 원본 지적 수 | 2 |
| 결정표 전체 행 수 | 6 |
| 판단한 사용자 | join5201. 2026-09-15 초안 6건을 그대로 확정했다. 사용자 발언은 초안대로 반영해라. B-02는 초안(승인 기록의 서버 시각)이 기준 시점이다. 요청문은 eval-request-A.md sha256:13c4460379f85759와 eval-request-B.md sha256:3bf34bf355b59a0a |
| 결정 날짜 | 2026-09-15 |

A는 치명 0, 보통 3, 확인필요 1이다. B는 치명 0, 보통 1, 확인필요 1이다. 합쳐서 치명 0, 보통 4, 확인필요 2다. 결정은 수용 5, 거부 1, 반박 0이다. 2026-09-15 사용자가 초안대로 확정했다.

치명은 없다. 여섯 건이 전부 다른 자리다. A는 멱등 실행기의 검사 순서, 목록 API의 미정의 Query, PageResponse 사본, 테스트 재실행 환경이고 B는 재고 version과 승인과 만료의 기준 시점이다. 수용 다섯 중 코드가 바뀌는 것은 넷(A-01, A-03, B-01, B-02)이고 A-04는 실행만이다. B-02는 기준 시점을 정하는 설계 결정이라 초안 하나와 대안 둘을 이유에 적었다.

### 평가 절차 기록 (2026-09-15)

라운드 계획의 D-2 가는 쌍마다 A와 B를 새 작업으로 여는 것이다. 이 쌍은 그렇게 돌았다. B는 Codex 작업 하나(worktree 76da, HEAD e1a9392)에서 2026-09-15 04:56에 나왔고, A는 다른 작업(worktree eabd, HEAD ed40c32)에서 05:12에 나왔다. 다만 B를 쓴 작업이 그 뒤 05:20에 A도 한 판 더 썼다(sha256 앞 16자리 f2f43da471fd78c5, 보통 1과 확인필요 1). 개발 세션은 두 A 판을 사용자에게 올렸고 사용자가 새 작업 판(eabd)을 확정했다(2026-09-15, 발언 새 작업 판으로 확정해라. 착지하고 결정표 만들어라). 같은 작업의 A 판은 착지하지 않고 그 지적 둘(재고 version, Docker)은 확정된 A의 A-04와 B의 B-01이 같은 자리를 짚는다.

| 확인 | 결과 |
|---|---|
| 파일 | 요청문이 정한 경로에 A와 B 하나씩. 두 worktree 모두 다른 새 파일은 없다 |
| 보조 표 | A 4행, B 2행. 판정 요약의 등급별 수와 같다(A 0, 3, 1. B 0, 1, 1). g2 report-schema 통과 |
| 읽은 파일 | A 24개, B 54개. 전부 요청문 2절의 허용 입력 12개와 대상 목록과 전달 실행 결과 사본 안이고 해시가 현재 파일과 전부 일치한다. 금지 목록(harness/reviews/, step9-verification.md, 라운드 README, 다른 쌍 요청문, 08-3 외 결정표)은 어느 표에도 없다 |
| 서로의 인용 | A 리포트에 B의 지적 ID 0건, B 리포트에 A의 지적 ID 0건. eabd 작업의 HEAD ed40c32에는 B 리포트가 이미 착지돼 있었다(B 착지 뒤에 연 작업). A의 읽은 파일 표에 그 경로가 없고 요청문이 금지했다 |
| A의 실행 | Docker Desktop 엔진이 꺼져 있어 gradlew test가 419건 중 296건 DB 연결 실패. 검증 ID 결과는 전달 XML과 HTTP 로그 대조에 기댄다. 대상이 기준 커밋과 같은 것은 A의 24개 해시와 verify-eval-workspace.mjs 10건 PASS. 개발 세션도 같은 시각 docker ps 실패를 확인했다. 이 사정은 A-04 행이다 |
| B의 실행 | 요청문 4절대로 테스트 미실행. verify-eval-workspace.mjs 10건 PASS. 정적 대조만이고 요구사항 R1부터 R5 역추적표는 전부 커버다 |

재평가(R2)를 쓰지 않는다. 여섯 건이 전부 코드로 확인되거나 거부되어 리포트의 쓸모가 줄지 않았다.

## 지적별 결정

지적 ID 규약: S{Step}-R{라운드}-{A 또는 B}-{원본 번호 2자리}.

| 리포트 A/B | 원본 번호 | 지적 ID | 심각도 | 대상 위치 | 위반 축 | 위반 기준 | 원문 지적 | 원문 근거 | 수정 제안 | 결정 | 이유 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| A | 01 | S9-R1-A-01 | 보통 | backend/src/main/java/com/o2o/booking/application/IdempotentRequestExecutor.java:52 | 멱등 규칙, 계약 하강 | 11 공통 멱등 규칙 2와 4, 평가 기준의 멱등 규칙과 계약 하강 | 기존 기록이 처리 중이면 body 비교 전에 곧바로 `REQUEST_IN_PROGRESS`를 반환한다. 같은 범위와 키에 다른 body가 들어와도 `IDEMPOTENCY_KEY_REUSED`가 아니라 `REQUEST_IN_PROGRESS`가 된다. | 11 공통 멱등 규칙 2는 같은 범위와 키의 다른 body를 409 `IDEMPOTENCY_KEY_REUSED`로 정한다. 실행기는 52행부터 56행에서 처리 중 여부를 먼저 판정하고 body 비교는 57행부터 수행한다. K17, L14, L20의 처리 중 테스트는 모두 같은 body만 재전송하고, 완료 기록의 다른 body는 K16만 확인한다. | 기존 기록을 받으면 완료 여부와 무관하게 body 해시를 먼저 비교한다. 처리 중 같은 body만 `REQUEST_IN_PROGRESS`로 보낸다. 세 멱등 API에 처리 중 다른 body 반례를 추가한다. | 수용 | 사실이다. IdempotentRequestExecutor 42행부터의 execute는 기존 기록이 있으면 54행에서 완료 여부를 먼저 보고 진행 중이면 RequestInProgressException을 던지며 body 대조(57행 sameBody)는 그 뒤다. 그래서 진행 중인 키에 다른 body가 오면 규칙 2의 IDEMPOTENCY_KEY_REUSED가 아니라 규칙 4의 REQUEST_IN_PROGRESS가 나간다. 11 공통 멱등 규칙 2(같은 범위와 키인데 body가 다르면 409 IDEMPOTENCY_KEY_REUSED)는 완료 여부를 조건으로 두지 않고, 1차 계약 2절 검사 순서 4행(64행)도 body 다름의 409를 진행 중의 409보다 앞에 적었다. 진행 중 기록도 bodyHash를 갖고 있어(IdempotencyRecord 96행 begin이 bodyHash를 받는다) 대조에 다른 정보가 필요 없다. 클라이언트 관점에서도 키 재사용은 1초 뒤 다시 보내도 풀리지 않는 오류라 먼저 알려야 맞다. 반영은 실행기의 순서를 바꾸는 것이다. 기존 기록을 받으면 완료 여부와 무관하게 body를 먼저 대조해 다르면 IDEMPOTENCY_KEY_REUSED, 같고 진행 중이면 REQUEST_IN_PROGRESS와 Retry-After, 같고 완료면 재전송. 머리 주석의 규칙 순서도 같이 고친다. 테스트는 세 멱등 API의 처리 중 테스트(BOOK-01 BookingApiTest 356행 K17, PAY-01 BookingPaymentApiTest 349행 L14, BOOK-04 BookingCancelApiTest 514행 L20) 옆에 처리 중 다른 body 반례를 하나씩 둔다. 반영 주체는 예약 묶음 |
| A | 02 | S9-R1-A-02 | 보통 | backend/src/main/java/com/o2o/booking/api/BookingController.java:192 | API 계약 준수, 계약 하강 | 11 공통의 미표시 Query 금지와 BOOK-02 처리 규칙, 평가 기준의 API 계약 준수와 계약 하강 | BOOK-02가 명세에 없는 query parameter를 거절하지 않는다. `status`, `page`, `size`만 인자로 선언했지만 Spring MVC는 기본적으로 다른 query parameter를 무시하므로 `guestId`, `hostId`, 임의 필드를 붙인 요청이 정상 조회된다. 소유권은 서버 행위자로 유지되지만 미지원 필드를 400으로 거절한다는 API 계약은 지켜지지 않는다. | 11 공통 헤더 뒤 규칙은 API에 표시하지 않은 Query 필드를 지원하지 않는다고 정하고, BOOK-02 처리 규칙은 `guestId`와 `hostId` query를 받지 않는다고 정한다. BookingQueryApiTest의 K24와 K25에는 미정의 query 반례가 없다. | 허용 query 이름을 명시적으로 검사해 나머지를 400 `INVALID_REQUEST`로 거절하고 `guestId`, `hostId`, 임의 query의 API 테스트를 추가한다. | 거부 | 거부한다. 11 공통 규칙 74행은 표시하지 않은 Path와 Query와 Body 필드를 지원하지 않는다고 적을 뿐 상태 코드를 정하지 않고, 400을 요구하는 38행의 대상은 요청의 정의되지 않은 필드, 곧 JSON body다. BOOK-02 처리 규칙 1810행의 guestId와 hostId 쿼리를 받지 않는다는 문장은 본인 예약만 반환한다는 앞 문장의 짝이다. 남의 예약을 쿼리로 고를 수 없다는 뜻이고 코드가 그렇다. BookingController 192행부터의 list는 행위자의 UserId로만 조회하고 guestId나 hostId를 읽는 인자가 없으며 BookingQueryApiTest 230행 K24(목록도 행위자를 본다)가 그것을 고정한다. T02가 남의 예약 조회에 요구하는 것도 400이 아니라 404다. JSON body 쪽 미정의 필드는 backend/src/main/resources/application.properties 7행의 fail-on-unknown-properties=true가 전역으로 켜져 있고 이번엔 그 파일이 허용 입력 안이었다. 같은 자리의 지적은 재고 묶음 R1 A-04와 숙소 묶음 R1 A-04에서 두 번 반박됐고 사용자가 두 번 확정했다. 이번엔 등급이 보통이라 반박 대신 거부다. 미정의 Query를 400으로 거절하려면 명세 74행을 고쳐 목록 API 전부(숙소, 재고와 요금, 예약, 프로모션, 검색)에 한 번에 걸어야 하고 그것은 예약 묶음 한 컨트롤러의 반영이 아니라 명세 결정이다. 사용자가 그 결정을 원하면 별도 이슈다. 반영 없음 |
| A | 03 | S9-R1-A-03 | 보통 | backend/src/main/java/com/o2o/booking/api/PageResponse.java:9 | 경계 위반 | backend 층 규칙 3-2 shared 기준, 평가 기준의 경계 위반 | 공통 Page 응답 모델의 두 번째 사본을 booking api에 다시 정의했다. 파일 자체도 catalog 사본과 같은 모양이며 두 번째 사본이라 shared 후보라고 적고 있다. | backend 층 규칙 3-2는 두 번째 컨텍스트가 쓰기 시작하면 shared로 올리도록 정한다. 현재 `PageResult`는 shared에 있지만 같은 공통 응답 모델은 컨텍스트별로 중복됐다. | 공통 Page 응답 모델을 shared의 API 중립 위치로 올리고 catalog와 booking이 함께 사용하게 한다. 컨텍스트별 응답 타입을 유지하려면 층 규칙의 예외 근거를 계약에 명시한다. | 수용 | 사실이고 절반은 이미 되어 있다. shared/PageResponse.java가 프로모션 묶음(커밋 be5e510)에서 생겨 PromotionController와 SearchController가 쓴다. 평가자는 shared의 그 파일이 대상 목록 밖이라 볼 수 없었다. 남은 사본은 booking/api/PageResponse.java와 catalog/api/PageResponse.java 둘이고 셋이 같은 모양이다. 1차 계약 10절 미해결 사항 행(356행)이 이 둘을 셋째 컨텍스트가 쓰기 시작했으니 layers.md 3-2로 올릴 때가 됐고 별도 이슈 몫이라고 이미 적었다. 반영은 booking/api 사본을 치우고 BookingController가 shared의 것을 쓰게 하는 것이며 응답 JSON은 다섯 칸 그대로라 API 테스트는 바뀌지 않는다. 치운 파일은 N2대로 tmp/_moved/로 옮긴다. catalog 사본은 이 쌍의 변경 허용 범위(1차 계약 3절 21행. booking 전체와 inventory 일부와 ActorRegistry 한 줄) 밖이라 계약이 적은 대로 별도 이슈로 남긴다. 반영 주체는 예약 묶음 |
| A | 04 | S9-R1-A-04 | 확인필요 | 직접 실행 `gradlew test` | 테스트 격리와 재현성 | 평가 기준의 테스트 격리와 재현성, 직접 실행과 전달 결과 구분 | 현재 작업공간에서 419건의 독립 재실행 성공을 확인하지 못했다. Docker Desktop 엔진이 없어 DB 컨테이너 기동이 실패했고, 전체 테스트는 419건 중 296건이 Spring 컨텍스트의 DB 연결 단계에서 실패했다. | 직접 실행은 `419 tests completed, 296 failed`와 `BUILD FAILED`였다. 반면 생성자 전달 `step9/test-summary.txt`와 XML은 419건, 실패 0, 오류 0이고 대상 테스트 22파일의 G1은 모두 PASS다. 전달 결과와 현재 직접 실행 결과가 다르므로 현재 환경에서 기능 실패인지 재현할 수 없다. | Docker Desktop과 `o2o_catalog_test`를 정상 기동한 뒤 같은 승인 명령을 다시 실행하고, 새 XML에서 419건, 실패 0, 오류 0을 확인한다. | 수용 | 사실이고 원인은 환경이다. 평가 시각에 이 PC의 Docker Desktop 엔진이 꺼져 있었다. 개발 세션도 같은 시각 docker ps가 실패했고(dockerDesktopLinuxEngine 파이프 없음) 296건 실패는 A가 적은 대로 전부 DB 연결 단계다. 전달 XML(2차 9단계 419건 실패 0)은 기준 커밋 1bdadfe의 실행이고, 대상이 그 커밋과 같은 것은 A 자신이 24개 파일 해시로 확인했고 verify-eval-workspace.mjs도 10건 PASS였다. 코드 변경은 없다. 수정 제안대로 같은 명령을 다시 돌려 419건 실패 0을 확인하는 일은 반영 라운드의 전체 테스트가 대신한다(숙소 묶음 R1 반영이 443건 실패 0을 낸 절차와 같다). 그 실행이 실패하면 이 행의 근거가 무너지므로 결과를 인계 표에 적는다. 남은 세 쌍의 A 평가자가 같은 자리에서 막히지 않도록 Docker Desktop을 먼저 켜는 것은 사용자 몫이다. 반영 주체는 예약 묶음(실행만) |
| B | 01 | S9-R1-B-01 | 보통 | `backend/src/main/java/com/o2o/inventory/domain/DailyInventory.java:142` | 추적성 양방향, 요구사항 역추적 | 추적성 양방향, 요구사항 역추적 | Hold, 확정 이동, 선점 반환, 판매 반환이 재고 `version`을 증가시키지 않는다. API 명세 공통 규칙은 Hold, 확정, 반환도 재고 version을 증가시킨다고 요구한다. | `adjust`만 115행에서 version을 증가시킨다. `hold`, `commit`, `releaseHeld`, `releaseSold`는 수량과 `updatedAt`만 바꾼다. `BookingApiTest` 553행은 예약 선점 뒤 version이 0이라고 단언해 현재 불일치를 고정한다. R1부터 R5의 수량과 금액 보장은 깨지지 않으므로 보통으로 판정한다. | 네 수량 변경 메서드가 성공할 때 version을 1씩 증가시키고, hold, commit, releaseHeld, releaseSold 각각의 성공과 롤백에서 version도 함께 검증한다. | 수용 | 사실이다. DailyInventory.java의 hold(142행), commit(157행), releaseHeld(172행), releaseSold(186행)는 수량과 updatedAt만 바꾸고 version은 adjust(109행)만 115행에서 더한다. 11 공통 규칙 43행은 재고의 Hold와 확정과 반환도 해당 재고 version을 증가시킨다고 적는다. 그 뜻은 호스트가 INV-03으로 조회한 뒤 수정하는 사이 예약이 선점을 넣었으면 그 수정이 VERSION_CONFLICT로 막혀야 한다는 것인데, 지금은 선점이 version을 안 올려 낡은 version이 통과하고 수량 검사만 남는다. BookingApiTest 553행 K22가 선점 뒤 version 0을 단언해 그 불일치를 고정한다. DailyInventory는 1차 계약 7절 D-2대로 행 잠금 뒤에 대조하므로 숙소 묶음처럼 @Version으로 바꿀 필요는 없고 네 메서드가 adjust와 같이 1을 더하면 된다. 반영은 네 메서드의 version 증가, K22의 단언을 1로 바꾸고 INV-03을 version 1로 보내야 INVENTORY_BELOW_COMMITTED가 나오는 것(version 0은 VERSION_CONFLICT가 먼저다. 그 반례도 하나), DailyInventoryTest에 네 메서드의 성공마다 version이 1 오르고 실패(부족, 음수)에서 그대로인 것. DailyInventory.java는 이 쌍의 변경 허용 범위(1차 계약 3절 21행)이고 재고 묶음 R2 대상 목록에도 같은 해시 cd47187239009880으로 들어 있다. 반영 주체는 예약 묶음이고 재고 R2 리포트가 같은 자리를 짚으면 그 결정표는 이 행을 가리킨다 |
| B | 02 | S9-R1-B-02 | 확인필요 | `backend/src/main/java/com/o2o/booking/application/PaymentOutcomeService.java:65` | 결정 근거의 자립성, 추적성 양방향 | 결정 근거의 자립성, 추적성 양방향 | 결제 승인이 이미 커밋된 뒤 P1과 T1이 경합하면 어느 쪽이 Booking 잠금을 먼저 얻는지에 따라 CONFIRMED 또는 EXPIRED와 환불로 갈릴 수 있다. 08-3 결정 8과 11-1의 "만료 커밋 전에 승인이 기록됐으면 확정 우선"과, 2차 계약 P1의 처리 시각이 expiresAt 이상이면 지연 승인으로 만료한다는 규칙을 동시에 만족시키는 기준 시점이 문서와 코드만으로 하나로 정해지지 않는다. | `PaymentApprovedAdapter`는 AFTER_COMMIT 뒤 `onApproved`를 부른다. `onApproved` 65행부터는 현재 시각이 due면 환불 후 만료한다. 반면 `BookingExpirationService` 71행부터는 승인 시도가 있으면 확정한다. `BookingLockContentionTest` 141행부터는 같은 선승인 상태에서 두 결과를 모두 허용한다. | 승인 기록 시각, P1 처리 시각, Booking 만료 커밋 중 어느 시각을 우선 기준으로 삼는지 계약에 한 문장으로 확정한다. 승인 기록 커밋이 기준이면 T1과 P1 모두 같은 결론을 내도록 승인 기록을 잠금 아래 재확인하고 테스트도 결과 하나만 허용한다. | 수용 | 사실이다. 같은 출발 상태(HELD, 승인 기록 있음, 지금이 expiresAt 이상)에서 P1은 PaymentOutcomeService 65행부터 처리 시각으로 판단해 환불하고 만료시키고, T1은 BookingExpirationService 71행부터 승인 기록이 있으면 확정한다. 어느 쪽이 Booking 잠금을 먼저 얻느냐로 결과가 갈리고 BookingLockContentionTest 96행과 101행의 L23 둘(공통 본문 141행부터)은 두 결과를 다 허용한다. 문서도 둘로 갈린다. 08-3 결정 8과 11-1은 만료 커밋 전에 승인이 기록됐으면 확정 우선이고 그 이유는 돈이 이미 승인됐는데 스케줄러가 몇 ms 먼저 돌았다고 예약을 날리지 않는 것이다. 11 명세 2179행은 전이 검사 시각을 기준으로 판단하고 정확히 expiresAt이면 만료라고 적는다. 2차 계약 7절 240행은 둘을 함께 채택했고 P1 표(357행 L7)는 명세 쪽을, T1 표(112행)는 08-3 쪽을 따랐다. 기준 시점을 하나로 정한다. 초안은 승인 기록의 서버 시각(승인 커밋에 찍힌 completedAt)이다. 승인 시각이 expiresAt 전이면 P1과 T1 모두 확정이고, expiresAt 이상이면 둘 다 환불과 TTL_EXPIRED 만료다. 이유는 08-3 11-1의 논리를 구독자의 지연에도 같이 적용하는 것이고, T18(정확히 expiresAt의 승인은 만료와 환불)과 T20(이미 EXPIRED면 환불만)은 그대로 성립한다. 명세 2179행의 제공된 결제 시각으로 과거로 돌려 승인하지 않는다는 문장은 클라이언트가 준 시각 얘기라 서버가 찍은 승인 시각과 충돌하지 않는다. 필요한 값은 이미 있다. P1은 PaymentApproved.occurredAt(attempt.completedAt)이고 T1은 attemptsOf의 승인 시도 completedAt이다. 환불은 결제 쪽이 전이 여부로 한 번만 하므로(PaymentApplicationService 130행) 두 경로가 겹쳐도 한 번이다. 반영은 셋이다. P1 65행의 조건을 처리 시각에서 승인 시각으로, T1의 확정 우선 분기에 승인 시각이 expiresAt 이상이면 환불 뒤 만료를 더하고, L23은 결과 하나(승인이 expiresAt 전이면 CONFIRMED)만 허용하게 바꾸며 승인이 expiresAt 이상인 경합 반례를 하나 더한다. 2차 계약은 개정으로 기준 시점 문장 한 줄을 넣는다. 11 명세 2179행의 문구 수정은 document/가 동결이라 사용자 몫이고 08-3 결정 8의 짝으로 남긴다. 결제 패키지는 고치지 않는다. 대안은 둘이다. 처리 시각 기준으로 통일하면 T1도 승인 기록이 있어도 만료해야 해 08-3 11-1과 정면으로 어긋나고, 지금처럼 두 결과를 허용하면 B가 짚은 대로 같은 상태의 결과가 잠금 순서에 달린다. 기준 시점의 선택은 사용자 확정 대상이다. 반영 주체는 예약 묶음 |

원문 지적과 근거와 수정 제안은 상세 표의 문장을 스크립트로 그대로 옮겼다. 요약하거나 자르지 않았다. 위반 기준 열은 보조 표의 값이다.

## 사용자 추가 지적

| 추가 ID | 심각도 | 대상 위치 | 지적 | 근거 | 처리 |
|---|---|---|---|---|---|

개발 세션이 대조하면서 평가자 둘이 못 본 코드 결함을 더 찾지 못했다. 이 절은 비운다.

## 치명 지적의 오판 판단 기록

치명이 없다. 오판 정정을 주장하는 행이 없어 이 절은 비운다.

## 반영 전 G2 확인

- [x] 평가 대상, 계약 둘, A/B 리포트의 버전이 결정표와 일치한다.
- [x] A와 B 원본의 지적 ID 집합과 결정표의 지적 ID 집합이 같다. 누락, 추가, 중복이 없다.
- [x] 원본의 각 행과 결정표를 대조했고, 결정과 이유 외의 정보가 동일하다. 스크립트가 셀을 그대로 옮겼다.
- [x] 전체 행 수가 A와 B 원본 지적 수의 합과 같다. 지적 수가 0이면 리포트가 실제로 정상 완료되었는지 확인했고 파싱 실패나 평가 미완료를 0건으로 취급하지 않았다.
- [x] 모든 행의 결정이 수용, 거부, 반박 중 하나다. 빈 결정이 없다.
- [x] 모든 거부에 이유가 있고, 반박은 원본 심각도가 확인필요인 행에만 있다. 반박은 없다.
- [x] 치명 오판 정정을 주장한 행마다 근거, 대안, 안티패턴, 사용자 결정, 날짜가 원본 지적에 연결되어 있다. 해당 행 없음.

| 확인 항목 | 기록 |
|---|---|
| 확인자와 날짜 | g2 실행은 2026-09-15 Claude Code(개발 세션). 사람 서명 대기 |
| G2 결과 | PASS. --mode pre 60건 통과(2026-09-15 18:23). version-match는 대상 목록과 계약 둘과 리포트 둘의 다섯 해시 전부 일치 |
| 미완료 사유 | 없음. 사용자가 2026-09-15 결정 6건을 확정했고 B-02의 기준 시점은 초안(승인 기록 시각)으로 정해졌다. 기계 검사는 위 결과 |

## 반영과 최종 확인 인계

| 확인 대상 | 기록 |
|---|---|
| 반영본 절대경로와 버전 또는 해시 | 반영 후 작성 |
| 수용 항목 반영 확인 | 반영 후 작성. 수용 5건의 반영 위치는 이유 열에 적었다. 반영 순서 제안은 B-01(재고 version), A-01(멱등 순서), B-02(기준 시점. 사용자 선택 뒤), A-03(PageResponse), A-04(전체 테스트 실행) |
| 미수용 항목 무변경 확인 | 반영 후 작성. 거부 하나(A-02)가 짚은 BookingController의 list는 바꾸지 않는다 |
| 남은 실제 치명 지적 | 없음 |
| 오판 정정 완료 항목 | 없음 |
| 마지막 반영본에 필요한 검증과 결과 | 반영 후 작성. Docker Desktop을 켠 뒤 전체 테스트, g1 code, 계약 둘 fill, 처리 중 다른 body 반례 셋, 선점 뒤 version 반례, 승인 시각 기준 경합 테스트 |
| 코드 작업의 직접 실행 근거 | 반영 후 작성 |
| 평가 대상 소스와 테스트의 무변경 확인 | 평가 전후 대조. A가 적은 24개와 B가 적은 54개 파일의 sha256이 대상 목록의 앞 16자리와 같고 현재 파일과 같다. verify-eval-workspace.mjs가 두 worktree에서 10건 PASS였고 평가 뒤 git status에 리포트 말고 바뀐 파일이 없다 |
| 검증 미완료 사항 | 사용자 확정. A의 전체 테스트가 Docker 엔진 부재로 돌지 못한 것은 반영 라운드의 전체 테스트 실행이 대신한다(A-04 행) |
| 사용자 최종 완료 판단과 날짜 | 미완료. 결정 6건은 2026-09-15 확정됐고 반영 전이다 |
| 실제 판단 및 확인에 사용한 시간 | 초안 작성 약 50분. 사용자 판단 시간은 미측정 |
