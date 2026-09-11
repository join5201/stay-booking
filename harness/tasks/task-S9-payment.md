# 작업 계약 task-S9-payment (결제 컨텍스트 묶음)

최초 작성: 2026-09-12
최종 갱신: 2026-09-12
양식: harness/prompts/task-contract.md v6

이 계약은 네 번째 구현 묶음이다. task-S9-catalog, task-S9-inventory-rate, task-S9-booking 1차(PR 127), task-S9-promotion-search(PR 98)가 main a22041c에 들어간 위에 얹는다. 2026-09-11 사용자 결정 셋(harness/out/mvp-parallel-2026-09-11/README.md 1절)의 세션 Y이고, 2026-09-12 사용자 전제 셋이 그 위에 얹힌다. 첫째, 기준선은 123건이 아니라 282건 실패 0이다. 둘째, 애그리거트 루트는 06-2대로 Payment(예약당 하나)이고 PaymentAttempt는 그 안의 엔티티다. 프롬프트의 PaymentAttempt 애그리거트 표기는 그 뜻으로 읽는다. 셋째, 승인 뒤에는 한 턴에 한 단계만 하고 멈추며 커밋은 이유 하나씩 가른다.

쉽게 말하면 결제 컨텍스트는 돈을 받는 창구가 아니라 장부다. 예약이 이 예약에 이만큼 청구해 달라고 넘기면 시도 한 줄을 적고, 가짜 카드사(Mock PG)가 승인이나 거절을 알려 오면 그 줄을 닫고 예약에 사건으로 알린다. 취소나 늦은 승인이면 환불 줄을 적는다. 장부는 예약이 어떤 상태인지 모른다. 06-1 R6이 예약이 결제를 알고 결제는 예약을 모른다고 적어서다. 그래서 예약 ID는 문자열로 받고 booking 패키지를 읽지도 import하지도 않는다.

## 작업

| 항목 | 내용 |
|---|---|
| Task ID와 Step | task-S9-payment. Step 9 구현. 기능 묶음은 10-6 3절 표의 결제와 확정 행 중 결제 컨텍스트 몫(INTERNAL-01과 내부 처리)과 접근 범위 행의 T30이다. 같은 행의 PAY-01과 PAY-02와 ConfirmBooking은 예약 2차(task-S9-booking-lifecycle) 몫이다 |
| 작업 유형 | 코드 |
| 목표 | 결제 컨텍스트를 백엔드와 테스트와 백엔드 검증 표까지 내린다. Payment 애그리거트(시도 목록과 환불 상태), 예약 2차가 부를 앱 서비스 공개 메서드 셋(openAttempt, refund, attemptsOf), Mock PG 어댑터(APPROVE와 DECLINE 자동 결과, DEFER 대기, 재시작 뒤 재개), INTERNAL-01, 도메인 이벤트 넷, 개발 프로파일 밖 비활성(T30). 예약당 승인 이력이 하나를 넘지 않고(I7), 같은 거래의 콜백이 몇 번 와도 이벤트가 한 번만 나며(U4, T21), 자동 결과가 앱 재시작 뒤에도 한 번만 반영된다(T26) |
| 대상 API ID | INTERNAL-01. 내부 처리 RequestPayment(openAttempt), RecordPaymentApproval, RecordPaymentFailure, RefundPayment. 검증 T30 |
| 선행 작업 | task-S9-catalog와 task-S9-inventory-rate와 task-S9-booking 1차와 task-S9-promotion-search. 전부 main에 있다. 이 묶음이 그 코드에서 쓰는 것은 shared(Actor, ActorResolver, ActorRegistry의 mock_001, ErrorResponse, Money, ClockConfiguration, ApiTime)뿐이다. booking, inventory, promotion, search, catalog 패키지의 파일은 import하지 않는다. inventory는 모양의 본보기로 읽기만 한다 |
| 완료 기준 | 넷을 모두 만족해야 한다. 첫째, 9절 계약 테스트 ID가 전부 통과이거나 미실행 사유와 함께 기록된다. 둘째, 8절 단계가 전부 끝난다. 셋째, backend/build/test-results/test/*.xml의 실행 수가 0이 아니고 failures와 errors 합이 0이며 앞 묶음까지의 282건이 그대로 통과한다. 넷째, 모든 단계의 실제 시간이 harness/state/progress.md에 분 단위로 기입돼 있다 |
| 변경 허용 파일과 범위 | backend/src/main/java/com/o2o/payment/ 전체(domain, application, infrastructure, api)와 그 테스트 backend/src/test/java/com/o2o/payment/. backend/src/main/java/com/o2o/shared/ActorResolver.java 하나(T30, 7절 D-3). backend/src/main/resources/application.properties와 backend/src/test/resources/application.properties에 프로파일 줄 하나씩(D-3. diff 요약 뒤 9절에 적는다). harness/out/task-S9-payment-R1/ 아래 실행 결과. harness/state/progress.md 행 추가. 이 계약 파일. shared의 나머지 기존 파일, catalog와 inventory 패키지, document/, harness/의 나머지는 읽기만 한다. booking, promotion, search 패키지는 읽지도 않는다 |
| 범위 밖과 유지할 전제 | 아래 3절 |
| 기준 버전 | 문서는 4절 입력 표의 sha256. 코드는 작업 브랜치 feat/task-s9-payment의 커밋 해시(origin/main a22041c 위)와 5절 평가 대상 행의 파일 목록 |
| 후보 작업 공간 | backend/. 워크트리 C:/Dev/potenup/99_projects/o2o-dev, 브랜치 feat/task-s9-payment. 후보와 정본의 구분은 git 브랜치가 맡는다. 테스트 DB는 o2o_payment_test이고 셸 환경변수 SPRING_DATASOURCE_URL로 넘긴다(9절) |
| 결과 기록 경로 | harness/state/progress.md |

## 1. 이 Task가 앞 묶음과 다른 점

| 항목 | task-S9-booking 1차 | 이 Task |
|---|---|---|
| 애그리거트 | Booking. 값 객체 둘을 품는다 | Payment. 예약당 하나(U3). 안에 PaymentAttempt 엔티티 목록을 품고 attemptCount는 저장하지 않고 센다(06-2 1절 결제 행, 6절 결제 CRC, 7절). 2026-09-12 사용자 전제 |
| 식별자 | BookingId + 유니크(userId, idempotencyKey) | PaymentId + 유니크(bookingId). 시도는 PaymentAttemptId + 유니크(pgTransactionId). 06-2 3-4 U3과 U4. 08-3 결정 2가 시도 한 테이블에 kind를 두고 pgTransactionId 유일을 DB 제약 하나로 막으라고 적는다 |
| 잠금 | 재고 N행을 날짜 오름차순 | Payment 루트 한 행. 06-2 5절 Payment 행이 시도 추가와 콜백 기록은 루트를 잠근 뒤 하라고 적는다. 08-3 결정 3의 순서(Booking, Payment, 재고)에서 이 세션의 경로는 Payment만 잠근다. 예약 2차가 그 앞에 Booking을 잠근다 |
| HTTP 입구 | 셋. GUEST | 하나. INTERNAL-01, MOCK_SYSTEM. 나머지 입구는 앱 서비스의 공개 메서드 셋이고 호출자는 예약 2차다 |
| 멱등 | Idempotency-Key와 아홉 규칙 | Idempotency-Key를 쓰지 않는다. eventId와 거래 결과로 중복을 가린다(INTERNAL-01 처리 규칙 2와 3). pgTransactionId 유니크(U4)가 발행 중복을 막는다 |
| 외부 시스템 | 없음 | Mock PG. 프로세스 안 어댑터다. 요청은 같은 트랜잭션 안(08-3 결정 9), 결과는 커밋 뒤에 INTERNAL-01과 같은 길로 들어온다(7절 D-1). 06-1 R7의 ACL 자리다 |
| 이벤트 | 발행만. 구독자 없음 | 발행 넷(PaymentRequested, PaymentApproved, PaymentFailed, PaymentRefunded)과 첫 구독자 하나(Mock 자동 결과 어댑터). layers.md 3-3 E2에 따라 롤백된 요청의 이벤트를 구독자가 받지 않는 테스트(8-1 Y12)가 필수다 |
| 다른 컨텍스트 호출 | 예약이 재고 도메인 서비스를 부른다 | 없다. 결제는 예약을 모른다(06-1 R6). 예약 ID는 문자열이고 예약 리포지토리를 읽지 않는다(11 명세 결제 접수와 환불 절 첫 줄) |
| 프로파일 | 없음 | dev 프로파일 하나가 생긴다(T30, 7절 D-3). 설정 파일 둘에 처음으로 줄이 더해진다 |
| 재시작 | 없음 | 저장된 REQUESTED와 mockMode로 자동 결과를 재개하는 러너(T26) |
| 응답 모델 | Booking 20개 필드 | MockEventResult 4개 필드. PaymentAttempt 11개와 Refund 7개는 예약 2차가 PaymentSummary로 낼 때의 모양이라 이 세션은 앱 서비스 뷰로 준비만 한다 |

같은 거래의 콜백 중복과 재시작 재개와 프로파일 경계가 이번 묶음의 난이도다. 예약 묶음의 난이도가 N행 잠금과 멱등 기록이었다면 이번은 한 행 잠금 아래에서 상태 기계의 종착 무해를 지키는 것이다.

## 2. 대상과 설계 근거

| 대상 | 모양 | 인증 | 계약표 행 | 불변식 |
|---|---|---|---|---|
| INTERNAL-01 | POST /internal/mock-payments/events. 200 MockEventResult | MOCK_SYSTEM | recordApproval(attemptId, pgTransactionId, amount), recordFailure(attemptId, pgTransactionId). 06-4 1-2 결제 | I7, U4, 전이 폐쇄(06-4 1-3) |
| openAttempt(String bookingId, Money amount, MockMode mockMode) | 앱 서비스 공개 메서드. REQUESTED 시도 뷰를 돌려준다 | 없음. 호출자가 예약 2차 | openAttempt와 첫 요청의 Payment 생성. 06-4 1-2 결제 | I6, I7, I9, U3, 전달 총액과 청구액 일치 |
| refund(PaymentAttemptId attemptId, RefundReason reason) | 앱 서비스 공개 메서드. 환불 뷰를 돌려준다. 이미 환불됐으면 그것을 돌려준다 | 없음. 호출자가 예약 2차 | refund. 06-4 1-2 결제. REFUNDED 재호출 무해(06-4 0절) | I7의 승인 이력 유지, 종착 무해 |
| attemptsOf(String bookingId) | 앱 서비스 공개 메서드. attemptNumber 오름차순 목록과 attemptCount와 approvedAttemptId와 환불 뷰 | 없음. 호출자가 예약 2차 | 조회. 06-4 v5 2-6이 조회는 결제가 제공하고 예약이 부른다고 적는다 | 없음 |
| Mock PG 어댑터 | APPROVE와 DECLINE은 자동 결과, DEFER는 INTERNAL-01 대기, 재시작 뒤 REQUESTED 재개 | 없음. 내부 | recordApproval과 recordFailure를 INTERNAL-01과 같은 길로 부른다(7절 D-1) | U4 |
| 도메인 이벤트 넷 | PaymentRequested, PaymentApproved, PaymentFailed, PaymentRefunded | 없음 | 06-4 1-2 결제 네 행의 Post 열. 페이로드는 6절 | 없음 |
| T30 | dev 프로파일 밖에서 X-Dev-Actor-Id 어댑터와 /internal 경로 비활성 | 없음 | 11 인증과 접근 제어 셋째 문단(58행) | 없음 |

묶음의 근거는 10-6 3절 표의 결제와 확정 행이다. 그 행이 PAY-01부터 INTERNAL-01과 ConfirmBooking을 한 묶음으로 적지만, 오늘의 전제 결정 2가 N9를 세션 단위로 읽어 결제 컨텍스트 몫만 떼어 냈다. PAY-01과 PAY-02는 예약이 받아 결제에 넘기는 중계라(11 명세 결제 접수와 환불 절 첫 줄) 예약 2차다. F21에 따라 INTERNAL-01 말고 다른 HTTP 경로를 만들지 않는다.

INTERNAL-01의 검사 순서. 앞 검사에서 거절되면 뒤 검사는 하지 않는다. 층은 06-4 1-4 검증 책임 위치 표를 따른다. 규칙 번호는 11 명세 INTERNAL-01 중복과 결과 처리 절(2259행부터)이다.

| 순서 | 무엇 | 실패 시 응답 | 층 |
|---|---|---|---|
| 1 | body 형식. 필수 여섯, 미정의 필드, outcome이 APPROVED 또는 FAILED, eventId 1자 이상 128자 이하, ID 둘 64자 이하, amount 1 이상 30,000,000,000 이하, failureCode는 FAILED에서 필수이고 MOCK_DECLINED만이며 APPROVED에서는 보내지 않음 | 400 INVALID_REQUEST | api. 프레임워크가 인자를 해석하며 보므로 행위자보다 앞이다(예약 계약 개정 3과 같다) |
| 2 | X-Dev-Actor-Id가 등록된 MOCK_SYSTEM | 401 ACTOR_REQUIRED, 403 ACCESS_DENIED | api (ActorResolver). dev 프로파일 밖이면 경로 자체가 없어 404다(D-3) |
| 3 | paymentAttemptId의 NORMAL 시도 존재. 규칙 1 | 404 RESOURCE_NOT_FOUND | application |
| 4 | 그 시도를 가진 Payment 잠금. PESSIMISTIC_WRITE | 해당 없음. 같은 순간의 둘은 줄을 선다 | infrastructure |
| 5 | pgTransactionId가 그 시도의 것. 규칙 1 | 409 MOCK_EVENT_CONFLICT | domain (Payment) |
| 6 | amount와 currency가 그 시도의 것. 규칙 1. 이벤트의 amount는 Money로 만들지 않고 long으로 대조한다. Money의 상한 1,000,000,000이 형식 검사의 상한보다 낮아서다(6절 P03 행) | 409 PAYMENT_AMOUNT_MISMATCH | domain (Payment) |
| 7 | eventId 기록 조회. 규칙 2. body 해시가 같으면 재전달 | 같으면 200 DUPLICATE와 기록의 processedAt. 다르면 409 MOCK_EVENT_CONFLICT | application |
| 8 | 같은 거래에 같은 결과가 이미 반영됐나. 규칙 3과 U4 | 200 DUPLICATE와 시도의 completedAt. 새 이벤트 발행 없음 | domain (Payment) |
| 9 | 종착 시도에 반대 결과. 규칙 4. FAILED에 APPROVED, APPROVED나 REFUNDED에 FAILED | 409 MOCK_EVENT_CONFLICT. 7절 D-4 | domain (Payment) |
| 10 | 전이. REQUESTED에서 APPROVED(completedAt, failureCode null) 또는 FAILED(completedAt, failureCode MOCK_DECLINED). 규칙 6의 시도 부분 | 해당 없음 | domain (PaymentAttempt) |
| 11 | PaymentApproved 또는 PaymentFailed 발행. attemptCount 탑재. 트랜잭션 안(layers.md 3-3) | 해당 없음 | application |
| 12 | 이벤트 기록 저장. eventId, 시도 ID, body 해시, result, processedAt. 시도 결과와 같은 트랜잭션(규칙 7의 결제 부분, 7절 D-2) | 해당 없음 | application, DB |
| 13 | 커밋. 200 PROCESSED. 규칙 5와 7의 예약 정책 부분은 예약 2차의 AFTER_COMMIT 구독자가 별도 트랜잭션에서 한다(08-3 결정 6) | 해당 없음 | application |

3부터 9에서 거절되면 아무것도 저장하지 않는다. 규칙 7이 실패하면 부분 반영 없이 롤백하고 재전달을 허용한다고 적어서다. 응답 모델 MockEventResult의 4개 필드(eventId, paymentAttemptId, result, processedAt)는 11 명세 2594행 표 그대로다(BN1). DUPLICATE의 processedAt은 같은 표가 최초 업무 처리 완료 시각이라 적으므로 최초 처리의 시각이다.

openAttempt의 검사 순서. 호출자는 예약 2차이고 이 세션은 앱 서비스 통합 테스트로 본다.

| 순서 | 무엇 | 실패 시 | 층 |
|---|---|---|---|
| 1 | bookingId가 비어 있지 않고 64자 이하, amount가 KRW 양수 | IllegalArgument. 호출자 잘못 | application |
| 2 | bookingId로 Payment를 잠그며 읽는다. 없으면 청구액 amount로 새로 만든다(U3. 06-4 1-2 openAttempt Pre의 첫 요청이면 Payment 생성) | 같은 예약의 첫 요청 둘이 같은 순간 오면 유니크가 둘째를 막는다. 예약 2차의 중계가 Booking을 먼저 잠그므로(06-4 v5 0-1) 그 경우는 생기지 않고 이 세션은 재시도를 만들지 않는다 | application, DB |
| 3 | 진행 중 NORMAL 시도 없음(I9) | AttemptInProgress | domain |
| 4 | NORMAL 시도 3 미만(I6) | AttemptLimitExceeded | domain |
| 5 | NORMAL 승인 이력 없음(I7. APPROVED 또는 REFUNDED) | AlreadyApproved | domain |
| 6 | 전달 총액과 청구액 일치 | AmountMismatch | domain |
| 7 | 시도 추가. attemptNumber = NORMAL 수 + 1, REQUESTED, kind NORMAL, requestedAt = 지금, mockMode | 해당 없음 | domain |
| 8 | Mock PG 요청. 같은 트랜잭션 안(08-3 결정 9). attemptId가 멱등키(06-4 v5 openAttempt Post). 돌아온 pgTransactionId를 시도에 적는다 | 해당 없음. 프로세스 안 호출이라 실패가 없다 | application (포트), infrastructure (어댑터) |
| 9 | 저장. PaymentRequested 발행 | 해당 없음 | application |
| 10 | 커밋 뒤. mockMode가 APPROVE나 DECLINE이면 자동 결과 어댑터가 D-1대로 결과를 넣는다. DEFER는 INTERNAL-01을 기다린다 | 해당 없음 | infrastructure |

셋과 넷과 다섯의 순서는 06-4 1-2 openAttempt Pre 열의 순서다. 11 PAY-01 처리 규칙도 진행 중 시도를 시도 한도보다 앞에 둔다. 예외 넷의 HTTP 매핑은 이 세션이 하지 않는다. PAY-01의 PAYMENT_IN_PROGRESS와 PAYMENT_ATTEMPTS_EXHAUSTED는 예약 2차가 그 예외를 매핑한다(프롬프트 접점 표).

refund의 검사 순서. attemptId로 그 시도를 가진 Payment를 잠근다. NORMAL 시도가 APPROVED면 REFUNDED로 바꾸고 reason과 refundedAt을 적고, Mock PG 환불을 attemptId를 멱등키로 같은 트랜잭션에서 부르고(08-3 결정 4와 10), PaymentRefunded를 발행한다. 이미 REFUNDED면 상태 변경과 PG 호출과 발행 없이 기존 환불을 돌려준다(06-4 0절 종착 무해의 환불 자리). REQUESTED나 FAILED면 NoApprovedAttempt, 없는 시도면 UnknownAttempt다. reason은 11 응답 모델 Refund의 BOOKING_CANCELED와 LATE_APPROVAL 둘이고 어느 쪽인지는 호출자인 예약 2차가 정한다.

attemptsOf는 NORMAL 시도만 attemptNumber 오름차순으로 돌려준다(11 PAY-02 처리 규칙). 각 시도는 11 응답 모델 PaymentAttempt의 11개 필드 그대로이고 status는 REQUESTED, APPROVED, FAILED 셋 중 하나로 낸다. 환불된 시도는 내부 상태가 REFUNDED라도 APPROVED로 내고 환불 뷰를 따로 준다(7절 D-5). approvedAttemptId는 환불 뒤에도 그 시도를 가리킨다(11 응답 모델 PaymentSummary). Payment가 없는 예약은 attemptCount 0과 빈 목록과 환불 null이다.

### 2-1. 불변식과 선행조건

| 번호 | 문장 | 어디서 지키나 |
|---|---|---|
| I6 | NORMAL 시도 수 <= 3 | Payment.openAttempt. 06-4 v5 1-1이 NORMAL 한정으로 좁혔다 |
| I7 | 승인 이력(APPROVED 또는 REFUNDED)인 NORMAL 시도는 하나를 넘지 않는다 | Payment.openAttempt(승인 이력 있으면 거부)와 Payment.recordApproval(둘째 승인 거부) |
| I9 | REQUESTED인 NORMAL 시도는 동시에 하나 | Payment.openAttempt. 잠금 아래에서 검사한다 |
| U3 | 같은 bookingId의 Payment는 하나 | DB 유니크(booking_id). 앱 서비스의 첫 요청 생성은 잠금 조회 뒤다 |
| U4 | 같은 pgTransactionId 콜백은 승인이든 실패든 상태를 바꾸지 않고 이벤트를 다시 내지 않는다 | Payment.recordApproval과 recordFailure(같은 결과면 전이 없음 반환)와 DB 유니크(pg_transaction_id 단일 컬럼. 06-4 v5 1-1 U4) |
| U5 | 같은 attemptId의 시도는 하나 | DB 기본키. 06-4 v5 1-1 |
| 전이 폐쇄 | REQUESTED에서 APPROVED 또는 FAILED로, APPROVED에서 REFUNDED로만. 역행 없음. FAILED는 종착이고 재시도는 새 시도 | PaymentAttempt. 06-4 1-3 결제 시도 표, 05-3 6절 PaymentStatus |
| 종착 무해 | REFUNDED에 재환불, 같은 거래번호 같은 결과의 콜백은 상태 변경과 이벤트 발행을 생략하고 성공으로 답한다. 전이 없음을 호출자에 돌려준다 | Payment. 06-4 0절 종착 재호출 무해 |
| 금액 일치 | openAttempt의 전달 총액이 청구액과 같다. 청구액은 첫 요청이 정한다 | Payment.openAttempt. 06-4 1-2 openAttempt Pre |
| 이벤트 유일 | 같은 eventId는 한 번 저장된다 | DB 기본키(event_id). 11 INTERNAL-01 규칙 2 |
| KRW | 금액은 KRW 정수 | shared Money. 6절 P03 행 |

I6과 I7과 I9가 06-2 1절 결제 행의 불변식 전부다. U4는 06-2 v4가 선행조건으로 정정한 것이라 1절이 아니라 3-4에 있다.

### 2-2. 이번 묶음이 만들지 않는 것

| 항목 | 누가 | 왜 |
|---|---|---|
| PAY-01과 PAY-02와 그 멱등 적용 | 예약 2차 | 예약이 받아 결제에 넘기는 중계다. 11 명세 결제 접수와 환불 절 첫 줄 |
| ConfirmBooking, CommitInventory, ExpireBooking 둘, 3회 실패 만료, 지연 승인 환불의 예약 쪽 처리, 취소 | 예약 2차 | 06-4 2-2 정책 카드 P1과 P3와 P5의 소속이 전부 예약이다. 결제 이벤트를 받아 예약 상태를 보는 코드는 예약 쪽에만 있을 수 있다(06-4 v5 2-3 마지막 문단) |
| PaymentApproved와 PaymentFailed의 구독자 | 예약 2차 | 위와 같다. 구독 방식은 layers.md 3-3 E1과 08-3 결정 6 |
| settledAt과 SettlePayment와 paymentFollowUp과 T2 순찰 | 7절 D-6 | 08-3 결정 1의 정산 표식은 T2 순찰의 후보 집합을 유한하게 두는 장치이고 T2는 오늘의 MVP 밖이다 |
| INTERNAL-01의 이벤트 기록 밖의 수신 원장 | 없음 | 06-4 v5 0-3의 원장은 승인 기록 트랜잭션이 롤백돼도 남는 재처리 입력이라 실 PG 전제다. 11 규칙 7은 롤백하고 재전달을 허용하라 적고 Mock 러너는 재전달할 수 있다 |
| 실 PG, 부분 환불, 결제 시도 타임아웃 | 없음 | 08-3 결정 4의 전환 조건과 결정 11의 11-3. REQUESTED 갇힘은 예약 TTL이 안전망이다 |
| Refund 별도 테이블 | 7절 D-5 | 환불은 시도의 상태다. 11 응답 모델 Refund는 그 상태의 투영으로 낸다 |
| INTERNAL-01 말고 다른 HTTP 경로 | 없음 | 프롬프트 F21 |

## 3. 범위 밖과 유지할 전제

| 항목 | 왜 밖인가 |
|---|---|
| 프론트와 연결 테스트와 배포와 CI | 별도 Task. 앞 세 묶음과 같은 처리다 |
| 예약 2차 항목 전부. 2-2절 | 두 세션의 PR이 main에 들어간 지금 예약 2차 계약 task-S9-booking-lifecycle.md가 이 묶음의 PR을 선행 조건으로 기다린다 |
| booking, promotion, search 패키지 | 읽지도 import하지도 않는다. 예약 ID는 문자열이다. 2026-09-12 사용자 지시 |
| inventory와 catalog 패키지 | 읽기만 한다. inventory가 모양의 본보기다. 예외 핸들러는 컨텍스트마다 하나라 payment/api에 따로 둔다 |
| shared의 기존 파일 | ActorResolver 하나(T30, D-3)만 예외다. ActorRegistry는 고치지 않는다. mock_001이 이미 MOCK_SYSTEM으로 등록돼 있다. 그 파일 머리 주석이 프로파일 분리를 하지 않는다고 적은 것은 이번 묶음으로 낡은 문장이 되며 그 한 줄은 별도 shared 이슈 몫으로 남긴다 |
| shared Money의 상한 | 1,000,000,000이다. 11 응답 모델 PaymentAttempt의 amount 상한 30,000,000,000보다 낮다. 이 세션은 shared Money를 고치지 않는다. 6절 P03 행 |
| 설계 문서 수정 | 틀렸으면 멈추고 보고한다. document/는 동결 |
| 하네스 파일 수정 | harness/prompts, harness/tools, harness/docs, harness/project-sync, CLAUDE.md, AGENTS.md, .claude, backend/CLAUDE.md, backend/.claude, backend/build.gradle은 동결. 설정 파일 둘은 D-3의 프로파일 줄 하나씩만 예외다. harness/state 기록 파일의 행 추가와 이 계약 파일이 예외다 |
| Codex 평가 | 오늘의 전제 결정 3. MVP 코드가 다 붙은 뒤 한 번 |
| 다중 객실과 부분 환불 | 08-3 결정 11의 11-5와 결정 4의 전환 조건 |

유지할 전제. shared 패키지(Actor, ActorRole, ActorResolver, ActorRegistry, ErrorResponse, SharedExceptionHandler, Money, ApiTime, ClockConfiguration의 UTC Clock)를 그대로 쓴다. 층 이름 넷 domain, application, api, infrastructure를 그대로 쓴다. 리포지토리는 인터페이스와 어댑터 두 파일이다(layers.md 3-1). 앱 서비스는 트랜잭션 안에서 publishEvent를 부르고 구독자가 AFTER_COMMIT을 지킨다(layers.md 3-3). 새 값 객체와 열거(PaymentId, PaymentAttemptId, MockMode, RefundReason 등)는 두 번째 컨텍스트가 쓰기 전까지 payment/domain에 둔다(layers.md 3-2. 6절 값 객체 행). 도메인은 스프링 웹과 스프링 데이터를 모른다. 시각은 shared Clock의 Instant이고 응답 시각은 ApiTime 형식이다.

## 4. 입력과 적용 규칙

| 자료 | 경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 01 전체 | document/01-o2o-ddd-plan.md | sha256:17569703c90a18df | 전문 |
| 대상 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절 전부(인증과 접근 제어 45행부터, 공통 헤더 61행부터, 멱등 처리 94행부터, 에러 표 112행부터), 예약과 결제 절의 PAY-01과 PAY-02(1895행부터 2016행. 이 세션이 만들지 않는 중계의 모양), 내부 처리와 Mock 이벤트 절 전체(2147행부터 2283행. 특히 결제 접수와 환불 2197행부터, INTERNAL-01 2215행부터), 응답 모델 PaymentAttempt와 Refund와 PaymentSummary와 PaymentAttemptList와 MockEventResult(2514행부터 2604행), 검증 기준 T14부터 T23과 T26과 T30(2665행부터), 검토할 정책 P03과 P07과 P08 |
| 대상 06-2 애그리거트 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 1절 결제 행, 2절 결제 행, 3-1 I6과 I7과 I9, 3-4 U3과 U4, 4절 커밋 후 발행 문단, 5절 Payment 행, 6절 결제 CRC, 7절 attemptCount와 재시도 문장 |
| 대상 06-4 계약 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 0절 락과 종착 무해 선언, 1-1 I6과 I7과 I9와 U3과 U4, 1-2 결제 네 행, 1-3 결제 시도 상태와 알려진 갇힘, 1-4 검증 책임 위치. v5는 브랜치 docs/task-s8-land에만 있어 git show로 읽었다. 0-1 잠금 순서 표의 결제 행 셋, 0-3 리스너 예외 위치, 1-1의 NORMAL 한정과 U4 강제 수단과 U5, 1-2 결제 표 전면 교체분, 1-3 kind 열, 2-3 정책 소속, 2-4 이벤트 페이로드, 2-6 조회 API. 정본은 main의 v4에 08-3 결정 11건을 얹은 것이고 v5는 그 얹은 결과의 원문으로 본다 |
| 대상 06-1 컨텍스트 맵 | document/06-1-o2o-context-map.md | sha256:95bc2b1079d3b739 | 2절 R6과 R7, 순환 검사 문단. 4절 shared 후보 |
| 대상 05-3 용어 | document/05-3-o2o-glossary.md | sha256:62a87a62baf5846e | 6절 결제 용어. PaymentAttempt, PaymentStatus, attemptCount |
| 대상 02 기능 목록 | document/02-o2o-feature-list.md | sha256:2a3d3ca2d8b63809 | Mock 결제와 Mock 환불 행, 예약당 승인 하나 문장, 3-7 결제는 예약을 모른다 |
| 대상 03 이벤트 스토밍 | document/03-o2o-event-storming.md | sha256:3334e5a73cb8f896 | 결제 이벤트 넷 행과 결제 커맨드 넷 행 |
| 확정 전제 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 1절 확정 전제와 2절 요구사항 R1부터 R5 |
| 정책 결정 08-3 | harness/decisions/decisions-08-3.md | sha256:1b8580aa86c18fd8 | 표 11행 전부와 표 아래 문장. 특히 1, 2, 3, 4, 6, 9, 10, 11의 11-3과 11-6. 9절 근거와 11절 되돌릴 목록 |
| 구현 계획 10-6 | harness/docs/10-6-o2o-harness-implementation-plan.md | sha256:eec97503b53ffff0 | 3절 기능별 API와 검증 연결 표의 결제와 확정 행과 접근 범위 행 |
| 앞 묶음의 계약 하나 | harness/tasks/task-S9-inventory-rate.md | sha256:4eb9f268cf82319a | 7절 D-2 잠금, 8-1 V14 두 트랜잭션 방식, 9절 실행 환경 |
| 앞 묶음의 계약 둘 | harness/tasks/task-S9-booking.md | sha256:0876574192269065 | 2절 검사 순서 표의 형식, 6절 값 객체 자리와 P07 행, 7절 D-1 이월 판단, 8-1 K12 이벤트 테스트, 9절 |
| 오늘의 전제 | harness/out/mvp-parallel-2026-09-11/README.md | sha256:7cbeaa454ed78eec | 1절 결정 셋, 2절 세션 셋, 4절 병합 순서와 접점 |
| 이 세션의 프롬프트 | harness/out/mvp-parallel-2026-09-11/prompt-Y-payment.md | sha256:4b5b8277fb8d34fc | Task 절의 만든다와 만들지 않는다 표, 접점 표, 결정 후보 D-1부터 D-4, 동결과 게이트, 첫 턴 절 |
| 적용할 코드 양식 | harness/prompts/dev-ptcf-prompt.v3.md | sha256:4fd959abf491efe0 | Format 절과 3절 단계 번호 |
| 실제 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 | 축, 심각도, 출력 스키마 |
| 백엔드 층 규칙 | backend/.claude/rules/layers.md | sha256:cb9e4f68bdc4c43c | 3절 전부. 특히 3-2와 3-3 |
| 백엔드 테스트 규칙 | backend/.claude/rules/testing.md | sha256:0639a76427f449a3 | T1부터 T6 |
| 고칠 shared 파일 | backend/src/main/java/com/o2o/shared/ActorResolver.java | sha256:67e8187be496b0cc | 전문. D-3이 이 파일의 require에 프로파일 판정을 더한다 |
| 고칠 설정 파일 하나 | backend/src/main/resources/application.properties | sha256:f091e83f098bd0d1 | 전문. D-3의 줄 하나가 붙는다 |
| 고칠 설정 파일 둘 | backend/src/test/resources/application.properties | sha256:91aabc6eb58fa044 | 전문. 같다 |
| 본보기 코드 | backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java | sha256:df6b9c10b619bbae | V14의 TransactionTemplate 두 트랜잭션 경합 방식. Y10과 Y22가 같은 방식이다 |

10-6과 앞 묶음의 계약과 오늘의 전제와 프롬프트는 이 표에만 있고 5절에는 없다. harness/docs/와 harness/out/의 계획 문서는 평가 입력이 될 수 없고(CLAUDE.md 3절), 앞 묶음의 계약은 이 묶음 코드의 근거가 아니다.

## 5. A와 B 평가 허용 입력 (HR1)

오늘의 전제 결정 3에 따라 평가는 MVP 코드가 다 붙은 뒤 한 번이다. 이 표는 그 라운드에서 이 묶음 몫으로 넘길 것이다.

| 자료 | 경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 코드 | 생성 후 기입 | 생성 후 기입 | 브랜치 feat/task-s9-payment의 payment 패키지 전체와 ActorResolver와 설정 파일 둘의 diff와 테스트. 목록 파일은 9단계에 harness/out/task-S9-payment-R1/eval-target-files.md로 만든다 |
| 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 이 작업 계약 | harness/tasks/task-S9-payment.md | 자기 해시 없음 | 전문 |
| 실제 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 | 축, 심각도, 출력 스키마 |
| 대상이 참조하는 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절, PAY-01과 PAY-02, 내부 처리와 Mock 이벤트 절, 응답 모델, 검증 기준 |
| 대상이 참조하는 06-2 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 1절, 3-1, 3-4, 4절, 5절, 6절 결제, 7절 |
| 대상이 참조하는 06-4 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 0절, 1-1, 1-2 결제, 1-3, 1-4 |
| 대상이 참조하는 06-1 | document/06-1-o2o-context-map.md | sha256:95bc2b1079d3b739 | 2절 R6과 R7 |
| 대상이 참조하는 08-3 결정 | harness/decisions/decisions-08-3.md | sha256:1b8580aa86c18fd8 | 1, 2, 3, 4, 6, 9, 10, 11과 표 아래의 11 v2와 부딪히는 번호 문장. 코드가 11 명세 대신 이 결정을 따른 자리(6절 충돌 행)를 평가자가 위반으로 적지 않게 한다 |
| 백엔드 층 규칙 | backend/.claude/rules/layers.md | sha256:cb9e4f68bdc4c43c | 3-2 shared 기준, 3-3 이벤트 규칙 E1과 E2 |
| 전역 역직렬화 설정 | backend/src/main/resources/application.properties | sha256:f091e83f098bd0d1 | 7행 fail-on-unknown-properties와 D-3의 프로파일 줄 |

이 표에 넣지 않는 것: 01 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, harness/docs/ 전체, harness/out/의 계획 문서, harness/state/ 전체, 생성 대화.

요구사항 역추적 축에 대한 지시. 입력 팩 2절의 요구사항 R1부터 R5 중 이번 묶음에 걸리는 것은 R5(확정 Booking 금액 불변)의 결제 몫뿐이다. 청구액은 첫 요청이 정하고 이후 시도는 그 값과 같아야 한다(2-1절 금액 일치). R1부터 R4는 재고와 예약 몫이라 해당 없음으로 적고 미커버로 적지 않는다. 02 기능 목록의 예약당 승인된 결제는 하나이고 중복 콜백이 환불이나 이중 확정을 만들지 않는다는 문장(52행)이 이 묶음의 요구사항이고 I7과 U4가 그것이다. 이 Task의 역추적 대상은 2절의 대상 표와 2-1절의 불변식 전부다.

## 6. 정책 적용

| 정책 ID 또는 쟁점 | 적용할 값 또는 판단 | 상태와 사용자 확인 |
|---|---|---|
| P03 통화와 달력 | 채택. currency는 KRW 고정이고 amount는 정수 원이다. 청구액과 시도 금액은 shared Money다. Money의 상한 1,000,000,000은 11 요금 단가 규칙에서 왔고 결제 금액 상한 30,000,000,000보다 낮다. 30박에 최고 단가인 청구액은 Money가 막는데 그것은 예약 묶음의 스냅샷도 같이 걸리는 shared의 한계다. 이 세션은 shared Money를 고치지 않고 INTERNAL-01의 형식 검사 상한만 명세대로 30,000,000,000으로 둔다. 시각은 UTC Instant이고 응답은 ApiTime 형식이다 | 확정. 계약 승인. Money 상한은 별도 shared 이슈 몫 |
| P07 로컬 행위자 | 채택. INTERNAL-01은 MOCK_SYSTEM만이고 mock_001이 ActorRegistry에 있다. 등록 파일을 고치지 않는다. 다른 역할은 403, 헤더 없음은 401 | 확정. 계약 승인 |
| P08 원자성 구현 후보 | 채택. Payment 루트를 PESSIMISTIC_WRITE로 잠그고 시도 결과와 이벤트 기록을 한 트랜잭션에 묶는다. 7절 D-2 | 확정. 계약 승인 |
| P01, P02, P04, P05, P06, P09, P10, P11 | 이번 작업과 무관. P05의 취소 환불은 예약 2차가 refund를 BOOKING_CANCELED로 부르는 것이고 이 세션은 값을 받기만 한다 | 확인 대기 |
| 08-3 결정 1 정산 표식 | 이번 묶음에 넣지 않는다. 7절 D-6. recordApproval과 3회째 recordFailure의 settledAt 되돌림 후행조건은 그 필드가 생길 때 같이 붙는다 | 계약 승인으로 확정 |
| 08-3 결정 2 고아 승인 모델 | 표 모양은 채택. 시도 한 테이블에 kind(NORMAL, ORPHAN)를 두고 pg_transaction_id를 단일 컬럼 유니크로 건다. 고아 행을 만드는 경로의 범위는 7절 D-4 | 계약 승인으로 확정 |
| 08-3 결정 3 잠금 순서 | 채택. 이 세션의 경로는 전부 Payment 한 행이다. Booking을 먼저 잠그는 것은 호출자인 예약 2차의 몫이다(06-4 v5 0-1 표의 RequestPayment 중계와 CancelBooking 행) | 확정. 08-3 수용. join5201, 2026-09-07 |
| 08-3 결정 4 환불 멱등키 | 채택. 발급하지 않고 유도한다. NORMAL 환불은 attemptId, ORPHAN 환불은 pgTransactionId. Mock PG 어댑터의 refund가 그 키를 받고 같은 키 재요청은 같은 결과다. 11 응답 모델 Refund의 id도 발급하지 않고 attemptId에서 유도한다(refund_ 접두어와 attemptId의 뒷부분) | 확정. 08-3 수용 |
| 08-3 결정 6 리스너 예외 위치 | 채택. 자동 결과 어댑터(D-1)는 @TransactionalEventListener가 붙은 얇은 어댑터가 try와 catch로 REQUIRES_NEW 앱 서비스 메서드를 부르는 형태다. 예약 2차의 구독자도 같다. INTERNAL-01의 HTTP 응답은 결제 부분이 저장되면 200이고 저장 전에 거절되면 11의 4xx다(아래 충돌 행) | 확정. 08-3 수용 |
| 08-3 결정 9 openAttempt와 PG 요청 | 채택. Mock PG 요청은 openAttempt와 같은 트랜잭션 안이다. 결과 전달은 7절 D-1 | 확정. 08-3 수용 |
| 08-3 결정 10 잠금 보유 중 PG 호출 | 채택. refund는 Payment 잠금을 쥔 채 Mock 환불을 부른다. v1 감수. 재고 잠금을 뒤로 미는 순서는 예약 2차의 취소 경로 몫 | 확정. 08-3 수용 |
| 08-3 결정 11의 11-3 시도 타임아웃 미도입 | 채택. REQUESTED에 머문 DEFER 시도는 예약 TTL이 안전망이다. 이 세션은 REQUESTED를 닫는 스케줄러를 만들지 않는다 | 확정. 08-3 수용 |
| 08-3 결정 5, 7, 8, 11의 나머지 | 이번 묶음과 무관. 예약 상태 기계 몫 | 확정. 08-3 수용 |
| 11 명세와 08-3의 충돌 하나. INTERNAL-01 규칙 7의 200 시점 | 11은 이벤트와 시도 결과와 예약 정책과 환불과 재고 변경이 함께 저장된 뒤 200이라 적고, 08-3 결정 6은 예약 정책을 REQUIRES_NEW 리스너에 두고 콜백은 항상 2xx라 적는다. 08-3을 따른다. 이 세션의 200은 이벤트 기록과 시도 결과와 이벤트 발행까지의 커밋을 뜻하고 예약 정책은 별도 트랜잭션이다. 예약 정책의 실패는 콜백 응답으로 알리지 않고 로그와 후속 보정으로 잡는다 | 계약 승인으로 확정. 7절 D-2 |
| 11 명세와 06-2의 충돌 둘. 같은 거래 반대 결과의 응답 | 11 규칙 4는 종착 시도에 반대 결과가 오면 409 MOCK_EVENT_CONFLICT이고, 06-2 U4는 같은 pgTransactionId 콜백은 승인이든 실패든 무시라 적는다. 둘 다 상태를 바꾸지 않고 이벤트를 내지 않는 점은 같고 HTTP 상태만 다르다. INTERNAL-01의 응답 계약은 11에만 있으므로 409를 낸다. U4의 무시는 실질(무변경, 무발행)로 지킨다 | 계약 승인으로 확정. 7절 D-4 |
| 11 명세와 06-4의 충돌 셋. 환불 뒤 시도 상태 | 11은 환불 후에도 시도가 APPROVED이고 Refund 모델로 구분하며, 06-4 1-3과 05-3 6절은 APPROVED에서 REFUNDED로 전이한다. 내부는 06-4를 따르고 응답은 11로 투영한다 | 계약 승인으로 확정. 7절 D-5 |
| 06-4 v5와 v4의 차이. PaymentFailed 발행 시점 | v5 1-2는 3회째 실패에만 PaymentFailed를 발행하고, main의 v4 1-2와 03 이벤트 행과 프롬프트 접점 표는 실패마다 attemptCount를 실어 발행한다. 실패마다 발행한다. 정본은 main v4에 08-3 결정을 얹은 것이고 발행 시점은 08-3 결정 11건 어디에도 없어서다. 예약 2차의 P3 가드가 attemptCount 3 이상을 본다(06-4 2-2 P3) | 계약 승인으로 확정 |
| 이벤트 페이로드 | 06-4 v5 2-4에 프롬프트 접점 표를 더한다. PaymentRequested는 paymentId, bookingId, paymentAttemptId, amount, currency, mockMode, occurredAt. PaymentApproved는 paymentId, bookingId, paymentAttemptId, pgTransactionId, amount, currency, attemptCount, occurredAt. PaymentFailed는 paymentId, bookingId, paymentAttemptId, pgTransactionId, amount, currency, attemptCount, failureCode, occurredAt. PaymentRefunded는 paymentId, bookingId, paymentAttemptId, kind, amount, currency, reason, occurredAt. mockMode가 PaymentRequested에 실리는 이유는 자동 결과 어댑터의 분기 입력이라서다 | 계약 승인으로 확정 |
| attemptCount의 뜻 | NORMAL 시도 수. 06-2 7절대로 저장하지 않고 attempts에서 센다. 이벤트 시점에는 I9 때문에 다른 REQUESTED가 없어 완료된 시도 수와 같다. 프롬프트 접점 표의 완료된 시도 수가 그것이다 | 계약 승인으로 확정 |
| 값 객체와 열거의 자리 | payment/domain에 둔다. PaymentId, PaymentAttemptId, MockMode, RefundReason, AttemptKind, PaymentAttemptStatus. 프롬프트는 shared 새 파일이라 적었으나 layers.md 3-2가 두 번째 컨텍스트가 쓰기 시작할 때 올리라 적고 예약 계약 D-1이 같은 판단을 했다. 예약 2차가 PaymentAttemptId를 쓰기 시작하면 그때 올린다. 06-1 R6대로 예약이 결제를 아는 방향이라 예약 2차가 payment.application과 payment.domain의 공개 타입을 import하는 것은 설계와 맞다 | 계약 승인으로 확정 |
| ID 형식 | PaymentId는 pay_, PaymentAttemptId는 attempt_, Mock 거래 번호는 mock_tx_ 뒤에 UUID 32자. 11 공통 규칙의 64자 이하 문자열이다. eventId는 호출자가 준다 | 확정. 앞 묶음과 같은 규칙 |
| 이벤트 기록의 저장 범위 | PROCESSED와 DUPLICATE 둘 다 result와 함께 저장한다. 같은 eventId가 다시 오면 규칙 2가 그 기록으로 답한다. 4xx로 거절된 이벤트는 저장하지 않는다(규칙 7) | 계약 승인으로 확정 |
| 자동 결과의 eventId | auto_ 뒤에 attemptId. 결정적이라 재시작 뒤 같은 시도를 다시 재개해도 규칙 2와 3이 DUPLICATE로 막는다(T26) | 계약 승인으로 확정. 7절 D-1 |
| 병렬 조치. 테스트 DB | o2o_payment_test. 모든 gradle 명령 앞에 export SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3307/o2o_payment_test 를 붙이고 같은 명령에서 echo로 값을 남긴다. do-not.md BN6의 승인이 이 행이다 | 확정. join5201, 2026-09-11 |
| 진행 방식 | 승인 뒤 한 턴에 한 단계만 하고 멈춘다. 오늘의 전제 결정 1(정지점 둘)보다 이 지시가 뒤이므로 우선한다. 커밋은 이유 하나씩 가른다 | 확정. join5201, 2026-09-12 |

확인 대기인 정책 여덟은 이번 코드가 의존하지 않는다. 미결 정책에 의존하는 구현을 확정하지 않는다(N4). 08-3 결정 11건은 2026-09-07 전부 수용됐으므로 확정값으로 쓴다.

## 7. 결정 6건의 안과 추천

프롬프트가 넷을 요구했고 초안을 쓰며 둘이 더 나왔다. D-5와 D-6이다.

### D-1. 자동 결과(APPROVE, DECLINE)를 어느 경계에서 전달하나

08-3 결정 9는 PG 요청을 openAttempt와 같은 트랜잭션에 둔다. 11 명세 결제 접수와 환불 절은 자동 결과를 시도 저장 후 전달하고 저장된 REQUESTED와 mockMode로 재시작 뒤에도 재개하며 중복 전달은 같은 업무를 반복하지 않는다고 적는다. 202는 접수 결과이지 승인 결과가 아니다. 요청과 결과를 갈라 읽으면 둘이 부딪히지 않는다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 요청은 트랜잭션 안, 결과는 커밋 뒤. openAttempt가 시도를 REQUESTED로 저장하고 Mock PG 요청으로 pgTransactionId를 받은 뒤 PaymentRequested를 발행하고 커밋한다. payment/infrastructure의 자동 결과 어댑터가 AFTER_COMMIT으로 그 이벤트를 받아 mockMode가 APPROVE나 DECLINE이면 REQUIRES_NEW 앱 서비스 메서드를 불러 INTERNAL-01과 같은 처리 길(2절 표 3부터 13)에 결정적 eventId(auto_ 뒤 attemptId)와 시도의 pgTransactionId와 금액과 결과를 넣는다. 재시작 재개는 ApplicationReadyEvent 러너가 REQUESTED이고 mockMode가 자동인 시도를 찾아 같은 메서드를 부르는 것이다 | 구독자와 러너 두 파일이 생긴다. 결과 반영이 별도 트랜잭션이라 테스트가 커밋 뒤를 봐야 하고 E2 테스트(Y12)가 필수가 된다. 대신 202의 REQUESTED 스냅샷이 자연스럽고, INTERNAL-01과 자동 결과가 한 길이라 중복 판정이 하나이며, T26이 실제로 검사할 것을 갖는다 |
| 나 | 결과도 같은 트랜잭션 안. openAttempt가 시도를 저장하고 바로 recordApproval이나 recordFailure까지 하고 커밋한다 | 11의 시도 저장 후 전달과 재시작 재개 문장이 죽는다. REQUESTED가 저장된 채 남는 경우가 없어 T26이 빈 검사가 된다. openAttempt가 돌려주는 시도가 이미 APPROVED라 PAY-01의 202 REQUESTED 예시와 어긋난다. PaymentApproved가 예약의 PAY-01 트랜잭션 안에서 발행돼 예약 구독자가 자기 요청의 커밋을 기다리는 모양이 된다 |
| 다 | 결과를 스케줄러가 REQUESTED를 폴링해 넣는다 | 지연이 주기만큼 생기고 T1 스케줄러가 없는 지금 첫 스케줄러가 결제에 생긴다. 08-3 9-9가 피하려던 보정 시스템이다 |

추천은 가다. 두 문서를 다 만족하고 자동 결과와 수동 이벤트가 같은 코드를 지나 중복 판정이 갈라지지 않는다. 어댑터의 REQUIRES_NEW는 08-3 결정 6의 형태 그대로다. AFTER_COMMIT 단계에서는 원래 트랜잭션의 자원이 아직 묶여 있어 새 트랜잭션을 열지 않으면 변경이 커밋되지 않는데, 그것이 결정 6이 REQUIRES_NEW를 요구한 이유와 같다.

결정: 대기.

### D-2. INTERNAL-01의 트랜잭션 경계

11 규칙 7은 이벤트 처리 기록과 시도 결과와 예약 정책과 환불과 재고 변경이 모두 저장된 뒤 200이고 실패하면 롤백하고 재전달을 허용하라 적는다. 08-3 결정 6은 예약 정책을 REQUIRES_NEW 리스너에 두고 콜백 응답은 항상 2xx라 적는다. 예약 정책은 이 세션 밖이므로 이 세션이 정하는 것은 이벤트 기록과 시도 결과와 이벤트 발행까지의 경계다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 한 트랜잭션. handleMockEvent가 시도 존재를 확인하고 Payment를 잠근 뒤 규칙 1부터 4를 검사하고 전이하고 이벤트를 발행하고 이벤트 기록을 저장하고 커밋한다. 예외가 나면 전부 롤백이라 기록도 남지 않고 재전달이 PROCESSED가 된다(T23의 결제 몫, Y21). 저장 전에 거절되는 3부터 9는 쓰기가 없어 롤백할 것도 없다 | 예약 정책의 실패를 응답으로 알릴 수 없다. 08-3 결정 6이 그렇게 정했고 6절 충돌 행에 적었다 |
| 나 | 두 트랜잭션. 이벤트 기록을 먼저 REQUIRES_NEW로 저장하고(06-4 v5 0-3의 수신 원장) 그다음 시도 결과를 처리한다 | 처리가 실패한 뒤 재전달이 오면 기록이 이미 있어 규칙 2가 DUPLICATE로 막는다. 그것을 피하려면 기록에 미처리 상태가 생기고 11 규칙 7의 부분 반영 없이 롤백과 어긋난다. 실 PG에서 원장이 필요한 이유(롤백돼도 남는 재처리 입력)가 Mock 러너에는 없다 |

추천은 가다. Payment 잠금 아래에서 규칙 2와 3을 검사하므로 같은 이벤트 둘이 같은 순간 와도 둘째가 첫째의 커밋을 본 뒤 DUPLICATE로 답한다(Y22). 잠금이 규칙 2보다 앞인 이유가 그것이다.

결정: 대기.

### D-3. 개발 프로파일의 실현 (T30)

11 인증과 접근 제어 셋째 문단이 개발 프로파일 밖에서는 X-Dev-Actor-Id 어댑터와 /internal 경로를 비활성화하라고 적는다. 지금 코드에는 프로파일이 없고 ActorRegistry 머리 주석이 이번 바퀴는 프로파일 분리를 하지 않는다고 적어 두었다. T30이 이 묶음에 들어오면서 그 유예가 끝난다.

키와 API는 빌드가 쓰는 jar에서 확인했다(P5). spring.profiles.active는 spring-boot-4.1.1.jar의 설정 메타데이터에 있고 타입은 문자열 목록이다. Environment.matchesProfiles(String...)는 해상된 spring-core-7.0.9.jar의 Environment 인터페이스에 있다. @Profile은 spring-context-7.0.9.jar에, @TransactionalEventListener와 TransactionPhase는 spring-tx-7.0.9.jar에 있다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 스프링 프로파일 dev. 설정 파일 둘에 spring.profiles.active=dev 한 줄씩 더해 bootRun과 테스트가 기본으로 dev다. ActorResolver.require가 Environment로 dev 프로파일 여부를 보고 밖이면 헤더가 있어도 ActorRequired(401)를 던진다. INTERNAL-01 컨트롤러에 @Profile("dev")를 붙여 밖에서는 경로가 없다(404). T30 테스트는 @ActiveProfiles로 dev가 아닌 프로파일을 켠 컨텍스트에서 둘을 본다 | 설정 파일 둘에 줄이 생긴다. 프롬프트의 동결 예외가 그것을 허용한다. 고치는 shared 파일이 ActorResolver 하나다. 프로파일 밖 컨텍스트가 테스트 JVM에 하나 더 뜨고 create-drop이 표를 한 번 더 비운다. 앞 묶음 테스트가 데이터를 스스로 심으므로 괜찮고, 깨지면 그 테스트만 @WebMvcTest 슬라이스로 바꾼다 |
| 나 | 전용 속성 o2o.dev-auth.enabled. @ConditionalOnProperty로 빈을 켜고 끈다 | 명세 문장이 프로파일이라 적는데 다른 장치를 만든다. 검증되지 않은 키 하나가 는다(N6은 이 계약이 정의하면 풀리지만 명세와 어긋나는 이름이다) |
| 다 | 프로파일 dev를 쓰되 ActorRegistry에 @Profile을 붙이고 ActorResolver는 ObjectProvider로 등록부 부재를 401로 바꾼다 | shared 파일 둘을 고친다. 프롬프트가 ActorResolver를 고치는 유일한 자리라 적었다 |

추천은 가다. 명세의 단어와 같고 shared 변경이 한 파일이다. 프로파일 밖에서 인증 필요 API가 전부 401인 것은 대체 인증이 없는 v1에서 명세가 말한 비활성 그 자체다. 공개 API는 그대로 열린다.

결정: 대기.

### D-4. ORPHAN 시도의 범위

08-3 결정 2는 금액 불일치와 승인 이력 중복도 고아 취급이라 적고, 06-4 v5 recordApproval의 (2) 분기는 시도가 종착이거나 승인 이력이 있거나 금액이 다르면 ORPHAN 행을 REFUND_PENDING으로 더해 환불하라 적는다. 11 INTERNAL-01 규칙 1은 없는 시도 404, 다른 거래 ID와 금액 차이 409이고 규칙 4는 종착 시도의 반대 결과가 409다. v5의 고아 분기가 받는 콜백은 전부 11이 거절하는 콜백이다. Mock에서는 거래 번호가 접수 때 정해지므로 다른 거래 번호의 승인은 있을 수 없는 콜백이고, 실 PG에서는 돈이 실제로 움직인 뒤라 거절 대신 환불이 맞다. 둘의 전제가 다르다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 11대로 거절한다. 없는 시도 404 RESOURCE_NOT_FOUND, 다른 거래 ID 409 MOCK_EVENT_CONFLICT, 금액이나 통화 차이 409 PAYMENT_AMOUNT_MISMATCH, 같은 eventId 다른 body와 종착 시도의 반대 결과 409 MOCK_EVENT_CONFLICT. 거절은 아무것도 저장하지 않는다. ORPHAN 행을 만드는 경로는 v1에 없다. 08-3 결정 2는 표 모양(kind 열, 단일 컬럼 유니크, recordApproval의 반환값에 고아 자리)으로만 채택하고 고아 생성 분기는 실 PG 전환 시 되돌릴 목록(08-3 11절)과 같은 성격으로 둔다 | kind 열에 ORPHAN 값이 있으나 v1 코드가 그 값을 쓰지 않는다. 08-3 결정 2의 고아 환불이 코드에 없다. 프롬프트의 검증 ID 오류 넷은 전부 산다 |
| 나 | 08-3과 v5대로 고아로 기록한다. 시도 존재만 확인하고 거래 ID가 다르거나 금액이 다르거나 시도가 종착이면 ORPHAN 행을 만들고 같은 트랜잭션에서 pgTransactionId를 키로 Mock 환불을 부르고 REFUNDED로 커밋하며 200 PROCESSED로 답한다. 없는 시도는 로그 후 200이다(v5 0-3) | 11의 404와 409 셋이 죽고 프롬프트가 검증 ID로 적은 INTERNAL-01 오류 넷 중 셋이 사라진다. Mock 러너가 잘못 보낸 이벤트가 가짜 환불 행이 된다. 테스트가 고아 환불 성공과 실패(REFUND_PENDING) 둘을 더 봐야 한다 |

추천은 가다. INTERNAL-01은 개발 프로파일의 테스트 러너와 Mock 어댑터가 쓰는 경로이고(11 INTERNAL-01 처리 규칙 첫 줄) 그 응답 계약은 11에만 있다. 08-3 결정 2가 지키려는 것은 실제 승인이 모델에 안 붙을 때 돈을 돌려주는 일인데 Mock에는 그 돈이 없다. 표 모양을 08-3대로 두므로 실 PG 전환 때 고아 분기를 붙일 자리는 남는다. 6절의 충돌 행 둘이 이 판단을 적는다.

결정: 대기.

### D-5. 환불을 어떻게 표현하나

11 응답 모델 Refund는 7개 필드의 별도 객체이고 환불 후에도 시도는 APPROVED다. 06-4 1-3과 05-3 6절은 시도가 APPROVED에서 REFUNDED로 전이하며 06-2 1절의 Payment 내부 요소에 Refund는 없다. 프롬프트는 Refund 기록이라 적었다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 환불은 시도의 상태다. PaymentAttempt가 APPROVED에서 REFUNDED로 전이하고 refundReason과 refundedAt을 갖는다. 11의 Refund 객체는 그 시도에서 투영한다. id는 attemptId에서 유도(08-3 결정 4의 같은 원리), paymentAttemptId, amount, currency, status REFUNDED, reason, refundedAt. 시도의 status는 투영에서 APPROVED로 낸다(11 PAY-02 처리 규칙). I7의 승인 이력이 APPROVED 또는 REFUNDED라는 문장이 그대로 코드가 된다 | 투영에 REFUNDED를 APPROVED로 바꾸는 줄이 생기고 주석이 11의 문장을 인용해야 한다. 별도 테이블이 없어 같은 시도에 환불 하나라는 규칙은 상태가 종착이라는 것으로 지켜진다 |
| 나 | 별도 엔티티 Refund. payment 안에 refund 테이블을 두고 유니크(payment_attempt_id)를 건다. 시도는 내부에서도 APPROVED로 남는다 | 06-4 1-3의 REFUNDED 전이와 05-3 6절이 코드에 없다. I7의 REFUNDED 셈이 조인이 된다. 테이블과 유니크와 리포지토리 행이 는다. decisions-08-3.md 표 아래 문장이 결정 2가 11의 Refund 모델과 부딪혀 11 v3에서 고친다고 적으므로 11의 모양에 맞춘 테이블은 v3에서 다시 바뀔 자리다 |

추천은 가다. 설계 문서 셋이 같은 말을 하고 11은 응답 모양만 정한다. 응답은 투영으로 11 그대로 낸다.

결정: 대기.

### D-6. 정산 표식(settledAt)과 조회 API를 지금 넣나

08-3 결정 1은 수용됐고 06-4 v5 1-2 recordApproval과 recordFailure의 후행조건에 settledAt 되돌림이 있으며 2-6이 paymentFollowUp 조회 API를 적는다. 쓰는 쪽은 예약의 P1, P3, T1, T2, 취소 트랜잭션이고 전부 이 세션 밖이다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 지금 넣는다. Payment에 settledAt과 settledBy, settle(settledBy), paymentFollowUp(bookingId) 뷰, recordApproval과 3회째 recordFailure의 되돌림 | 오늘 쓰는 쪽이 없다. T2 순찰이 없으면 settledAt은 아무도 읽지 않는 열이고 BN5(계약 범위 밖 미리 만들기)에 가깝다. 테스트가 셋 는다 |
| 나 | 예약 2차 계약이 결제 패키지를 열어 붙인다. 이 세션은 recordApproval과 recordFailure의 반환값(전이 발생, 전이 없음)을 남겨 그 자리에 되돌림 한 줄이 붙게 한다. attemptsOf의 approvedAttemptId가 T1의 확정 우선 판정(08-3 결정 11의 11-1)에 필요한 것을 이미 준다 | 예약 2차가 payment 패키지의 파일을 고친다. 병렬의 날이 끝난 뒤라 소유 경계는 계약이 다시 정한다. 08-3 결정 1이 코드에 늦게 들어간다 |

추천은 나다. 프롬프트의 만든다 열에 없고 오늘 읽는 쪽이 없다. 예약 2차가 T1과 T2를 만들 때 같은 계약 안에서 붙이는 것이 필드와 독자를 한 번에 놓는 길이다.

결정: 대기.

## 8. 세로 진행 단계와 각 단계의 완료 조건

2026-09-12 사용자 지시에 따라 승인 뒤에는 한 턴에 한 단계만 하고 멈춘다. 단계마다 progress.md 행과 실제 시간을 남기고 커밋은 이유 하나씩 가른다. 번호는 dev-ptcf-prompt.v3.md 3절과 같다.

| 단계 | 내용 | 완료 조건 | 정지 |
|---|---|---|---|
| 1 | 이 계약과 결정 6건 승인 | 10절 승인 칸과 7절 결정 6건이 값을 갖는다 | 승인 대기 |
| 2 | 이슈와 브랜치 | 이슈 하나(제목에 task-S9-payment). 브랜치 feat/task-s9-payment는 origin/main a22041c로 fast-forward돼 있다. PR을 초안으로 열고 본문에 Refs로 잇는다. CLAUDE.md 4-1 순서 | 멈춘다 |
| 3 | 해당 없음 | 백엔드 틀과 MySQL은 앞 묶음이 세웠다. 테스트 DB만 9절대로 바꿔 넘긴다 | 건너뛴다 |
| 4 | 도메인과 앱 서비스와 인프라 코드. payment/domain의 Payment, PaymentId, PaymentAttempt, PaymentAttemptId, PaymentAttemptStatus, AttemptKind, MockMode, MockOutcome, RefundReason, MockPaymentEvent, MockEventResult, 리포지토리 인터페이스 둘, 이벤트 넷, 예외들. payment/application의 PaymentApplicationService(openAttempt, refund, attemptsOf, handleMockEvent, deliverAutoResult, resumeAutoResults), Mock PG 포트, 뷰 셋과 커맨드. payment/infrastructure의 JPA 구현 넷, 프로세스 안 Mock PG 어댑터, 자동 결과 어댑터(D-1), 재개 러너(T26) | 06-2 6절 결제 CRC의 책임 행마다 대응 코드가 있고 주석에 절 번호가 있다. 2-1절의 불변식이 애그리거트 안에 있다. 기존 282건이 그대로 통과한다. 커밋은 domain, application과 infrastructure로 가른다 | 멈춘다 |
| 5 | 테스트. 도메인 단위와 앱 서비스 통합과 이벤트 | 8-1절의 5단계 항목 Y1부터 Y14에 테스트가 있고 실패와 통과가 짝이다. 결과 사본이 step5에 있다 | 멈춘다 |
| 6 | INTERNAL-01과 T30. payment/api의 컨트롤러와 요청과 응답과 예외 핸들러. ActorResolver의 프로파일 판정과 설정 파일 둘의 줄 하나씩(D-3. 기존 파일이라 diff 요약을 먼저 보인다) | 경로가 명세의 요청과 응답과 상태 코드 그대로다. 8-1절의 6단계 항목 Y15부터 Y23이 붙는다. bootRun으로 띄운 실제 서버에 INTERNAL-01을 친 기록이 http-calls.txt로 남고 상태 코드가 명세와 같다. 커밋은 api, shared와 설정, 테스트로 가른다 | 멈춘다 |
| 6-2 | 해당 없음 | 조회 API가 없다. attemptsOf는 HTTP가 아니라 앱 서비스 메서드다 | 건너뛴다 |
| 7 | 해당 없음 | 프론트는 별도 Task | 고르지 않는다 |
| 8 | 해당 없음 | 프론트는 별도 Task | 고르지 않는다 |
| 9 | 백엔드 검증 표 | T21, T26, T30과 INTERNAL-01 오류 넷의 대조표가 닫히고 T15, T16, T17, T20, T22, T23은 결제 몫까지 닫힌 것으로 적는다. 회고 표가 채워진다. 5절 평가 대상 행을 기입하고 fill을 다시 돌린다. PR 전에 origin/main을 병합하고 union 검사를 돌린다 | 사용자 완료 판단 |

모든 단계에 공통으로 걸리는 완료 조건 둘. 첫째, harness/state/progress.md에 그 단계의 행이 추가되고 실제 시간 칸이 분 단위로 채워진다. 둘째, 하네스 파일을 고치지 않는다.

3단계를 건너뛰는 이유. 앞 묶음이 backend/와 MySQL 컨테이너와 결과 파일 경로를 확정했고 9절이 그 값을 그대로 쓴다. 테스트 DB 이름만 환경변수로 바꾸며 그것은 2026-09-11 승인된 병렬 조치다.

### 8-1. 단계별 테스트 목록

불변식 하나에 테스트 하나가 최소다. 이번 묶음은 불변식 셋에 종착 무해와 중복 판정과 재시작 재개와 프로파일 경계가 붙는다. ID의 Y는 결제 세션 Y다. 앞 묶음의 C와 V와 K와 겹치지 않게 골랐다.

| ID | 단계 | 무엇을 확인하나 | 근거 |
|---|---|---|---|
| Y1 | 5 | 첫 openAttempt가 Payment를 만들고 NORMAL 1번 REQUESTED 시도를 더한다. 같은 Payment에 REQUESTED가 있는 동안 둘째 openAttempt는 AttemptInProgress이고, 첫 시도가 FAILED가 된 뒤에는 2번이 열린다 | I9, 06-4 1-2 openAttempt, T15와 T16의 결제 몫 |
| Y2 | 5 | 실패 셋 뒤 넷째 openAttempt는 AttemptLimitExceeded다. 셋째는 열린다. 경계 양쪽 | I6, T17의 결제 몫(넷째 거절) |
| Y3 | 5 | APPROVED 시도가 있으면 openAttempt는 AlreadyApproved다. REFUNDED 뒤에도 같다 | I7, 06-4 1-2 openAttempt Pre |
| Y4 | 5 | 둘째 시도의 amount가 청구액과 다르면 AmountMismatch이고 같으면 열린다. 청구액은 첫 요청이 정한다 | 06-4 1-2 openAttempt Pre의 전달 총액과 청구액 일치, R5 결제 몫 |
| Y5 | 5 | recordApproval이 REQUESTED를 APPROVED로 바꾸고 completedAt을 적고 failureCode가 null이다. recordFailure는 FAILED와 MOCK_DECLINED다. 역행(APPROVED에 FAILED, FAILED에 APPROVED)은 예외다 | 06-4 1-3 결제 시도 표, 전이 폐쇄 |
| Y6 | 5 | 같은 pgTransactionId 같은 결과의 두 번째 기록은 전이 없음을 돌려주고 시도가 그대로다. 다른 pgTransactionId는 PgTransactionMismatch다 | U4, 06-4 0절 종착 무해 |
| Y7 | 5 | refund가 APPROVED를 REFUNDED로 바꾸고 reason과 refundedAt을 적으며 멱등키가 attemptId다. 두 번째 refund는 상태와 refundedAt을 바꾸지 않고 같은 환불을 돌려준다. REQUESTED와 FAILED에 refund는 NoApprovedAttempt다 | 06-4 1-2 refund, 08-3 결정 4, 11 결제 접수와 환불 절의 같은 승인 시도에 환불은 한 개, T20의 결제 몫 |
| Y8 | 5 | 도메인 이벤트 넷의 페이로드. PaymentApproved와 PaymentFailed의 attemptCount가 그 시점의 NORMAL 시도 수이고 셋째 실패의 attemptCount가 3이다 | 6절 이벤트 페이로드 행, 06-1 R6의 attemptCount는 이벤트에 실려 온다, T17의 결제 몫 |
| Y9 | 5 | MySQL. payment의 booking_id 유니크(U3)와 payment_attempt의 pg_transaction_id 유니크(U4)가 같은 값의 둘째 행을 거부한다 | 06-2 3-4, 06-4 1-4 유일성은 DB, T3 |
| Y10 | 5 | MySQL. 한 트랜잭션이 Payment 행을 잠근 채 있는 동안 같은 예약의 openAttempt가 기다렸다가 잠금이 풀린 뒤 I9로 거절된다. 앞 묶음 V14의 두 트랜잭션 방식 | 06-2 5절 Payment 행, 08-3 결정 3 |
| Y11 | 5 | MySQL. mockMode APPROVE로 openAttempt하면 돌려받은 뷰는 REQUESTED인데 커밋 뒤 시도는 APPROVED이고 PaymentApproved가 테스트 전용 구독자에 한 번 닿는다. DECLINE은 FAILED와 PaymentFailed 한 번. DEFER는 REQUESTED로 남고 아무 결과 이벤트도 없다 | 7절 D-1, 11 결제 접수와 환불 절의 자동 결과 문장 |
| Y12 | 5 | MySQL. 바깥 트랜잭션이 openAttempt를 감싸고 롤백하면 시도가 없고 PaymentRequested가 어느 구독자에도 닿지 않으며 자동 결과 어댑터가 아무것도 하지 않는다 | layers.md 3-3 E2. 이 묶음이 첫 구독자를 만든다 |
| Y13 | 5 | MySQL. REQUESTED이고 mockMode APPROVE인 시도를 DB에 직접 심고(T6) 재개 러너를 부르면 APPROVED가 되고 PaymentApproved가 한 번 난다. 다시 부르면 아무것도 바뀌지 않고 이벤트도 없다. DEFER 시도는 재개하지 않는다 | T26, 11 결제 접수와 환불 절의 재시작 문장 |
| Y14 | 5 | attemptsOf가 attemptNumber 오름차순이고 attemptCount와 approvedAttemptId가 맞다. 환불된 시도는 status APPROVED로 나오고 환불 뷰 7개 필드가 채워지며 approvedAttemptId는 유지된다. Payment가 없는 예약은 0과 빈 목록과 null이다 | 11 PAY-02 처리 규칙, 응답 모델 PaymentSummary와 Refund, 7절 D-5 |
| Y15 | 6 | INTERNAL-01이 DEFER REQUESTED 시도에 APPROVED를 받아 200과 MockEventResult 4개 필드를 내고 시도가 APPROVED이며 PaymentApproved가 한 번 난다. FAILED와 failureCode MOCK_DECLINED는 시도를 FAILED로 만든다 | 11 INTERNAL-01 응답과 규칙 6의 시도 부분 |
| Y16 | 6 | 같은 eventId 같은 body 재전달은 200 DUPLICATE와 최초 processedAt이다. 다른 eventId로 같은 거래 같은 결과도 200 DUPLICATE다. 어느 쪽도 이벤트를 다시 내지 않고 시도가 그대로다 | T21, 규칙 2와 3, U4 |
| Y17 | 6 | 없는 시도 404 RESOURCE_NOT_FOUND. 다른 pgTransactionId 409 MOCK_EVENT_CONFLICT. 금액 차이와 통화 차이 409 PAYMENT_AMOUNT_MISMATCH. 같은 eventId 다른 body 409 MOCK_EVENT_CONFLICT. APPROVED 시도에 FAILED 409 MOCK_EVENT_CONFLICT. 다섯 다 시도와 이벤트 기록이 그대로다 | INTERNAL-01 오류 넷, 규칙 1과 2와 4, 7절 D-4 |
| Y18 | 6 | body 형식. eventId 없음, 129자, outcome 다른 값, amount 0, FAILED에 failureCode 없음, APPROVED에 failureCode 있음, failureCode 다른 값, 미정의 필드는 400 INVALID_REQUEST. 맞는 body는 통과 | 11 INTERNAL-01 요청 표, 공통 절 38행 |
| Y19 | 6 | 헤더 없음 401 ACTOR_REQUIRED. guest_001과 host_001은 403 ACCESS_DENIED. mock_001은 통과 | 11 인증과 접근 제어의 MOCK_SYSTEM 행, P07 |
| Y20 | 6 | APPROVED 뒤 refund(BOOKING_CANCELED)로 환불된 시도에 같은 승인 이벤트가 다시 오면 200 DUPLICATE이고 환불이 하나 그대로이며 새 이벤트가 없다 | T22의 결제 몫, 규칙 8 |
| Y21 | 6 | 이벤트 기록 뒤 강제 실패(테스트 전용 트랜잭션 안 구독자가 한 번 예외)로 롤백되면 이벤트 기록과 시도 변경이 없고, 같은 이벤트 재전달은 200 PROCESSED다 | T23의 결제 몫, 규칙 7, 7절 D-2 |
| Y22 | 6 | MySQL. 한 트랜잭션이 Payment 행을 잠근 채 있는 동안 같은 이벤트 둘을 보내면 잠금이 풀린 뒤 하나가 PROCESSED이고 다른 하나가 DUPLICATE이며 이벤트 기록은 각자 하나씩이고 PaymentApproved는 한 번이다 | 규칙 2와 3, 06-2 5절, D-2의 잠금이 규칙 2보다 앞 |
| Y23 | 6 | dev가 아닌 프로파일로 띄운 컨텍스트에서 X-Dev-Actor-Id host_001로 HOST 전용 조회를 치면 401이고 POST /internal/mock-payments/events는 404다. 기본 컨텍스트(dev)에서는 둘 다 산다 | T30, 11 인증과 접근 제어 셋째 문단, 7절 D-3 |

Y9부터 Y13과 Y22는 MySQL 통합 테스트다. 메모리 저장소로 대신하지 않는다(BN4, T3). Y10과 Y22는 앞 묶음 InventoryApiTest의 V14처럼 TransactionTemplate으로 Payment 행을 먼저 잠근 뒤 요청을 보내고 잠금을 풀어 순서를 만든다. Y11과 Y12와 Y13은 테스트 전용 @TransactionalEventListener 구독자로 커밋 뒤 전달과 롤백 뒤 미전달을 본다(예약 묶음 K12와 같은 방식). Y21의 강제 실패 구독자는 @EventListener로 트랜잭션 안에서 한 번 던지고 두 번째부터는 통과시킨다.

## 9. 실행과 검증

| 항목 | 내용 |
|---|---|
| 작업 디렉터리 | backend/ |
| 실행 환경 | Spring Boot 4.1.1(Spring Framework 7.0.9), Gradle 9.7.1 wrapper, Java 툴체인 21(JAVA_HOME은 C:/Users/user/.jdks/ms-21.0.11), MySQL 9.7.2 컨테이너 o2o-catalog-mysql. 환경변수 이름은 O2O_MYSQL_ROOT_PASSWORD, O2O_MYSQL_USER, O2O_MYSQL_PASSWORD(값은 backend/.env이고 저장소에 없으며 모델은 그 파일을 읽지 않는다), 그리고 SPRING_DATASOURCE_URL(아래 행) |
| 실행할 명령 | docker compose -f backend/docker-compose.yml up -d로 DB를 올린다. export SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3307/o2o_payment_test 를 앞에 붙이고 echo로 값을 남긴 뒤 JAVA_HOME 21로 ./backend/gradlew.bat -p backend test를 돌린다. 예상 결과는 BUILD SUCCESSFUL과 실패 0이고 실행 수는 282 + 이번 묶음 테스트 수다. 6단계는 같은 환경변수로 bootRun을 띄우고 curl로 INTERNAL-01을 친다. 게이트는 node harness/tools/check.mjs g1 <소스> --type code --artifact <junit.xml>와 node harness/tools/check.mjs fill harness/tasks/task-S9-payment.md |
| 테스트 DB | 127.0.0.1:3307, o2o_payment_test. 설정 파일의 기본값 o2o_catalog_test를 셸 환경변수 SPRING_DATASOURCE_URL이 덮는다. 2026-09-11 사용자가 승인한 병렬 조치이고 6절에 적었다. 2026-09-12 첫 턴 기준선 실측에서 이 값으로 282건 실패 0이었고 junit XML의 접속 문자열이 o2o_payment_test였다 |
| 데이터 초기화 허용 범위 | 테스트 프로파일의 ddl-auto가 create-drop이라 o2o_payment_test 스키마 전체다. bootRun의 대상 DB는 설정 파일 기본값 o2o_catalog_test이고 ddl-auto update라 표가 생기기만 한다. 6단계의 실제 서버도 SPRING_DATASOURCE_URL로 o2o_payment_test를 가리켜 다른 세션의 개발 DB에 표를 만들지 않는다. 운영 DB는 없다 |
| 빌드 출력과 로그 경로 | backend/build/test-results/test/*.xml. 사본을 harness/out/task-S9-payment-R1/step{단계}/에 남긴다. 실제 서버 기록은 harness/out/task-S9-payment-R1/step6/http-calls.txt |
| 계약 테스트 ID | 8-1절의 Y1부터 Y23. 검증 ID는 T21, T26, T30 전부와 INTERNAL-01 오류 넷(400 INVALID_REQUEST, 404 RESOURCE_NOT_FOUND, 409 MOCK_EVENT_CONFLICT, 409 PAYMENT_AMOUNT_MISMATCH)과 T15, T16, T17, T20, T22, T23의 결제 몫 |
| A와 B 평가 범위 | eval-criteria-code.md의 축 전부. 오늘의 전제 결정 3에 따라 MVP 코드가 다 붙은 뒤 한 번이며 이 묶음 몫은 5절 표다 |
| 필수 검증을 실행하지 못했을 때 | progress.md의 실패 원인 칸에 명령과 출력을 적고 결과를 halted로 남긴다. 미실행을 통과로 적지 않는다 |
| 설정 파일 변경 | D-3 가가 승인되면 backend/src/main/resources/application.properties와 backend/src/test/resources/application.properties 각각에 spring.profiles.active=dev 한 줄과 근거 주석이 붙는다. 6단계에서 diff 요약을 보인 뒤 반영하고 이 행에 반영 사실을 적는다. 프롬프트 동결 절의 유일한 허용 변경이다 |

T15부터 T23이 결제 몫인 이유. 아홉 검증 대부분이 예약 상태(HELD, EXPIRED, CANCELED)와 재고 반환을 함께 본다. 이 세션은 결제 컨텍스트가 내는 예외와 이벤트와 중복 판정까지 닫고 예약 상태와 재고 몫은 예약 2차가 닫는다. T14와 T18과 T19는 예약 몫뿐이라 해당 없음이다.

## 10. 승인과 진행

| 항목 | 기록 |
|---|---|
| 작업 계약 승인 | 대기. 결정 6건은 7절 |
| 마지막 성공 단계 | 1단계 초안(2026-09-12). 계약 작성과 fill 통과 |
| 실제 사용 시간 (1단계) | 28분. 2026-09-12 04:53부터 05:21까지. 상태 확인 표, 기준선 282건 실측, 문서 읽기, 계약 작성, fill 통과 |
| 미해결 사항과 다음 작업 | 첫 턴 상태 확인에서 브랜치 feat/task-s9-payment가 워크트리 o2o-payment에 체크아웃돼 있어 이 워크트리(o2o-dev, a22041c에서 분리된 HEAD)로 가져오지 못했다. 사용자가 그 워크트리에서 브랜치를 놓아 주면 이 워크트리에서 체크아웃하고 origin/main으로 fast-forward한다. 승인 뒤 2단계 |
| 최종 산출물과 버전 | 작업 후 기록 |
| 실제 사용 시간 | 미측정 |
| 최종 완료 판단 | 대기 |
