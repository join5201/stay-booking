# task-S9-payment 9단계 백엔드 검증 표와 회고 표

최초 작성: 2026-09-13
최종 갱신: 2026-09-13 (개정 다섯 확정 반영. 1-15절 재실행 행, 3-2절 Y10 문장 정정, 3-4절 첫 행, 4절 개정 1 풀 기본값 보강, 6절 확정 행과 하네스 이슈 135. 그 전 같은 날 초안)

대상: 작업 계약 task-S9-payment 8절 9단계.
계약 경로: harness/tasks/task-S9-payment.md

이 문서가 이 바퀴의 두 번째 산출물이다. 첫째는 코드이고 둘째가 이 표다. 앞 묶음 task-S9-booking의 step9-verification.md와 같은 골격을 쓴다.

## 1. 검증 ID 대조표

계약 9절이 지정한 검증 ID는 T21, T26, T30 전부와 INTERNAL-01 오류 넷(400 INVALID_REQUEST, 404 RESOURCE_NOT_FOUND, 409 MOCK_EVENT_CONFLICT, 409 PAYMENT_AMOUNT_MISMATCH)과 T15, T16, T17, T20, T22, T23의 결제 몫이다. T14, T18, T19는 예약 몫뿐이라 해당 없음이다. 근거는 10-6 기능별 API와 검증 연결 표의 결제와 확정 행과 접근 범위 행이다.

쉽게 말하면 이 묶음은 장부다. 장부가 같은 영수증을 두 번 받아도 한 줄만 적는지(T21), 장부를 덮었다 다시 펴도 적다 만 줄을 마저 적는지(T26), 개발용 뒷문이 개발 모드 밖에서 잠기는지(T30)를 본다. 예약이 어떤 상태가 되는지는 장부의 몫이 아니라 예약 2차의 몫이다.

### 1-1. T21

11 검증 기준의 문장은 이렇다. 같은 거래 승인 반복, 다른 eventId로도 반복. 기대는 DUPLICATE와 이중 확정과 불필요한 환불 없음이다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| 같은 eventId 같은 body 재전달이 200 DUPLICATE이고 processedAt이 최초 처리 시각이다 | 통과 | Y16 첫째. 규칙 2. step6/http-calls.txt의 열째 호출도 같다 |
| 다른 eventId로 같은 거래 같은 결과가 200 DUPLICATE다 | 통과 | Y16 첫째. 규칙 3. http-calls.txt의 열한째 호출도 같다 |
| 실패 결과의 반복도 DUPLICATE이고 PaymentFailed는 한 번이다 | 통과 | Y16 둘째 |
| 어느 쪽도 이벤트를 다시 내지 않고 시도가 그대로다 | 통과 | Y16 둘. 커밋 뒤 구독자 CommittedPaymentEvents의 PaymentApproved 도착이 1이고 시도의 completedAt이 그대로다. 도메인에서는 Y6이 전이 없음을 돌려준다 |
| 잠금이 풀린 뒤 도착한 둘도 하나만 PROCESSED다 | 통과 | Y22 둘. 잠금을 쥔 3초 동안 요청 둘이 끝나지 않고, 풀린 뒤 결과 집합이 PROCESSED와 DUPLICATE이며 PaymentApproved가 한 번이다. 같은 eventId면 기록 하나, 다른 eventId면 기록 각자 하나 |
| 이벤트 기록이 DUPLICATE도 저장한다 | 통과 | Y22 둘째와 http-calls.txt 끝의 DB 확인. 기록 셋(PROCESSED 둘, DUPLICATE 하나). 같은 eventId 재전달은 기록을 더하지 않는다(계약 6절 이벤트 기록의 저장 범위 행) |
| 불필요한 환불 없음 | 통과 | Y20. 환불된 시도에 같은 승인이 다시 와도 환불이 하나 그대로다. 규칙 8 |
| 이중 확정 없음 | 통과(결제 몫) | 확정은 예약 2차의 구독자가 하고 그 입력인 PaymentApproved가 한 번뿐이라는 것을 Y16과 Y22가 센다. U4의 DB 강제는 Y9 둘째 |

T21 판정: 통과. 규칙 2와 3과 8이 도메인(Y6), 앱 서비스와 HTTP(Y16, Y20), 잠금 경합(Y22), 실제 서버(http-calls.txt) 넷에서 같은 답을 냈다.

### 1-2. T26

문장은 이렇다. 자동 Mock 모드에서 접수 후 앱 재시작. 기대는 저장된 시도로 결과 처리를 재개하고 중복 업무 효과가 없는 것이다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| 접수는 됐는데 결과가 안 들어간 상태를 만들 수 있다 | 통과 | Y13. 리포지토리로 REQUESTED이고 mockMode APPROVE인 시도를 직접 심는다(T6). 서비스로는 못 만든다. 커밋되면 어댑터가 바로 결과를 넣기 때문이다 |
| 러너가 그 시도를 찾아 결과를 넣는다 | 통과 | Y13. 첫 호출이 1을 돌려주고 시도가 APPROVED이며 PaymentApproved가 한 번 닿는다 |
| 두 번째 실행은 아무것도 바꾸지 않는다 | 통과 | Y13. 둘째 호출이 0이고 도착 수가 그대로다 |
| DEFER 시도는 재개하지 않는다 | 통과 | Y13. INTERNAL-01을 기다리는 시도는 REQUESTED로 남는다 |
| 어댑터와 러너가 같은 시도를 두 번 잡아도 한 번만 반영된다 | 통과 | 자동 결과의 eventId가 auto_ 뒤 attemptId로 결정적이라 규칙 2와 3이 DUPLICATE로 막는다(계약 6절 자동 결과의 eventId 행). 그 규칙 자체는 Y16과 Y22 |
| 재개 길이 INTERNAL-01과 같은 길이다 | 통과 | 러너와 어댑터가 둘 다 deliverAutoResult를 부르고 그 안이 handleMockEvent와 같은 검사 순서다(계약 7절 D-1) |
| 앱 기동에 러너가 걸려 있다 | 코드 확인 | MockAutoResultResumeRunner의 @EventListener(ApplicationReadyEvent.class). 6단계 bootRun은 후보가 0건이라 로그가 없다. 실제 재기동으로 본 것은 아니다(6절) |

T26 판정: 통과. 재개와 중복 업무 효과 없음을 Y13이 닫았다. 기동 연결은 코드로 확인했고 실제 재기동 관찰은 남은 것에 둔다.

### 1-3. T30

문장은 이렇다. 개발 프로파일 밖에서 개발 헤더와 Mock 경로 사용. 기대는 개발 인증과 Mock 이벤트 경로 비활성이다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| dev가 아닌 프로파일에서 X-Dev-Actor-Id가 있어도 401 ACTOR_REQUIRED다 | 통과 | DevProfileBoundaryTest의 Y23. @ActiveProfiles verify로 띄운 컨텍스트에서 host_001로 HOST 전용 조회가 401이다. http-calls.txt의 verify 첫째 호출도 같다 |
| dev가 아닌 프로파일에서 /internal/mock-payments/events가 404다 | 통과 | 같은 테스트. 컨트롤러 빈이 컨텍스트에 없다(@Profile dev). http-calls.txt의 verify 둘째 호출도 같다 |
| 공개 API는 프로파일 밖에서도 열린다 | 통과 | 같은 테스트. 헤더 없는 GET /api/v1/properties가 200이다. http-calls.txt의 verify 셋째 호출도 같다 |
| 기본 컨텍스트(dev)에서는 둘 다 산다 | 통과 | MockPaymentEventApiTest의 Y23. 설정 파일의 spring.profiles.active=dev가 기본이라 HOST 전용 조회가 200이고 INTERNAL-01이 산다. http-calls.txt의 dev 호출 열다섯 전부가 그 증거다 |
| 실제 서버의 프로파일 | 통과 | bootRun 기동 로그. dev로 띄우면 The following 1 profile is active: dev, 환경변수 SPRING_PROFILES_ACTIVE=verify로 띄우면 verify |
| 판정 자리가 하나다 | 통과 | ActorResolver.require가 environment.matchesProfiles(dev)를 보고 아니면 ActorRequiredException이다. 컨트롤러 셋을 안 고쳤다(계약 7절 D-3 가) |

T30 판정: 통과. 계약 9절이 전부라 적은 셋(T21, T26, T30)이 다 닫혔다.

### 1-4. INTERNAL-01 오류 넷

11 INTERNAL-01의 오류 응답 표와 공통 절이 정한 넷이다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| 400 INVALID_REQUEST. 필수 누락, eventId 129자, ID 65자, outcome 다른 값, amount 0과 상한 초과와 문자열, FAILED에 failureCode 없음, APPROVED에 failureCode 있음, failureCode 다른 값, 미정의 필드, 깨진 JSON | 통과 | Y18 첫째. 열여섯 경우. http-calls.txt의 셋째부터 다섯째 호출(outcome DECLINED, failureCode 없음, 미정의 필드) |
| 400이 401보다 앞이다 | 통과 | Y18 첫째의 마지막 경우. 헤더 없이 깨진 body를 보내면 400이다. 계약 2절 검사 순서 1행대로 프레임워크가 인자를 해석하며 본다 |
| 맞는 body와 경계값은 형식을 지난다 | 통과 | Y18 둘째. 64자 ID와 amount 1과 30,000,000,000은 400이 아니라 뒤 검사의 404나 409로 간다 |
| 404 RESOURCE_NOT_FOUND. 없는 시도 | 통과 | Y17의 없는 시도 테스트. http-calls.txt의 여섯째 호출 |
| 409 MOCK_EVENT_CONFLICT. 다른 pgTransactionId | 통과 | Y17의 다른 pgTransactionId 테스트. http-calls.txt의 일곱째 호출 |
| 409 MOCK_EVENT_CONFLICT. 같은 eventId 다른 body | 통과 | Y17의 같은 eventId 다른 body 테스트. 최초 기록이 그대로다. http-calls.txt의 열둘째 호출 |
| 409 MOCK_EVENT_CONFLICT. 종착 시도에 반대 결과 | 통과 | Y17의 반대 결과 테스트. APPROVED에 FAILED와 FAILED에 APPROVED. http-calls.txt의 열셋째 호출. 계약 7절 D-4 |
| 409 PAYMENT_AMOUNT_MISMATCH. 금액 차이와 통화 차이 | 통과 | Y17의 금액과 통화 테스트. 1원 위와 아래와 USD. http-calls.txt의 여덟째 호출 |
| 거절 다섯 다 시도와 이벤트 기록이 그대로다 | 통과 | Y17 다섯 전부가 거절 뒤 시도 상태와 기록 수를 다시 읽는다. http-calls.txt 끝의 DB 확인에서 거절된 호출의 기록이 없다 |
| 검사 순서가 계약 2절 표다 | 통과 | Y17의 없는 시도와 다른 pgTransactionId 테스트가 뒤 검사의 오류(금액 1원)를 같이 실어도 앞 검사의 응답(404, 다른 거래 409)이 난다 |
| 오류 본문이 공통 ErrorResponse다 | 통과 | assertError가 code를 읽는다. 메시지는 11 오류 표의 조건 문장 그대로다 |

INTERNAL-01 오류 넷 판정: 통과. 계약 2절 검사 순서 표의 1행부터 9행이 각각 테스트 한 자리씩 갖는다.

### 1-5. T15의 결제 몫

문장은 이렇다. 진행 중 결제에 새 키로 다음 결제 요청. 기대는 PAYMENT_IN_PROGRESS와 시도 한 개다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| REQUESTED가 있는 동안 둘째 openAttempt가 거절된다 | 통과 | Y1 둘째. AttemptInProgress. 시도가 하나 그대로다 |
| 잠금 아래에서도 같다 | 통과 | Y10. 다른 트랜잭션이 Payment 행을 잠근 채 있는 동안 openAttempt가 기다렸다가 풀린 뒤 I9로 거절된다. 2.1초 |
| PAYMENT_IN_PROGRESS로 매핑 | 예약 몫 | PAY-01이 예약 2차라 그 예외의 HTTP 매핑은 그쪽이다(계약 2절 openAttempt 표 아래 문단) |
| 새 키 | 예약 몫 | Idempotency-Key는 PAY-01의 것이다 |

T15 판정: 결제 몫 통과. 예약 몫(HTTP 매핑과 멱등키)은 예약 2차.

### 1-6. T16의 결제 몫

문장은 이렇다. 1회와 2회 결제 실패. 기대는 HELD와 선점 유지와 다음 시도 가능이다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| 첫 시도가 FAILED가 된 뒤 2번이 열린다 | 통과 | Y1 셋째. 도메인. Y11 DECLINE이 커밋 뒤 자동 결과로 같은 것을 본다 |
| 실패마다 PaymentFailed가 attemptCount를 싣는다 | 통과 | Y8 둘째. 1과 2. 예약 2차의 P3 가드가 이 값을 본다(계약 6절 PaymentFailed 발행 시점 행) |
| 실패 뒤 다음 시도의 금액은 청구액과 같아야 한다 | 통과 | Y4. 다르면 AmountMismatch, 같으면 열린다 |
| HELD와 선점 유지 | 예약 몫 | 결제는 예약 상태를 모른다(06-1 R6) |

T16 판정: 결제 몫 통과.

### 1-7. T17의 결제 몫

문장은 이렇다. TTL 전 3회 결제 실패. 기대는 EXPIRED와 PAYMENT_FAILED와 재고 한 번 반환이다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| 셋째 시도는 열리고 넷째는 거절된다 | 통과 | Y2. AttemptLimitExceeded. 경계 양쪽 |
| 셋째 실패의 PaymentFailed가 attemptCount 3을 싣는다 | 통과 | Y8 둘째. 예약 2차가 이 값으로 만료를 결정한다 |
| 한도가 NORMAL 시도만 센다 | 통과 | Payment.openAttempt가 kind NORMAL만 센다(06-4 v5 1-1). ORPHAN 행은 이 묶음이 만들지 않는다(계약 7절 D-4) |
| EXPIRED와 PAYMENT_FAILED와 재고 반환 | 예약 몫 | 예약 2차의 PaymentFailed 구독자 |

T17 판정: 결제 몫 통과.

### 1-8. T20의 결제 몫

문장은 이렇다. 이미 EXPIRED인 DEFER 시도에 지연 승인. 기대는 예약은 EXPIRED, 환불 한 번, 재고 추가 반환 없음이다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| DEFER 시도의 늦은 승인이 200 PROCESSED로 들어가고 PaymentApproved가 난다 | 통과 | Y15 첫째. 규칙 4가 DEFER의 첫 결과와 모순 재전달을 가른다 |
| refund(LATE_APPROVAL)가 환불 한 번을 만든다 | 통과 | Y7 앱 서비스 몫. 멱등키가 attemptId이고 두 번째 refund는 같은 환불을 돌려준다. Mock PG의 환불 호출도 한 번이다 |
| 환불 사유 LATE_APPROVAL이 있다 | 통과 | RefundReason 열거 둘. 어느 쪽인지는 호출자가 정한다 |
| 예약이 EXPIRED를 보고 refund를 부르는 것 | 예약 몫 | 예약 2차의 PaymentApproved 구독자(08-3 결정 6) |

T20 판정: 결제 몫 통과.

### 1-9. T22의 결제 몫

문장은 이렇다. 승인 후 취소 후 같은 승인 재전달. 기대는 취소 유지와 환불 한 번 유지다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| 환불된 시도에 같은 승인이 다시 오면 200 DUPLICATE다 | 통과 | Y20. 규칙 8. 새 이벤트가 없다 |
| 환불이 하나 그대로다 | 통과 | Y20. attemptsOf의 환불 뷰가 같다. 도메인에서는 Y6 둘째(REFUNDED 뒤 같은 승인은 전이 없음) |
| 환불된 시도에 FAILED는 409다 | 통과 | Y20 끝. 종착 시도에 반대 결과(규칙 4) |
| 두 번째 refund도 무해하다 | 통과 | Y7 둘째. 상태와 refundedAt이 그대로다 |
| 취소 유지 | 예약 몫 | 예약 2차 |

T22 판정: 결제 몫 통과.

### 1-10. T23의 결제 몫

문장은 이렇다. 이벤트 저장 이후 정책 처리 실패 유도. 기대는 부분 결과 롤백과 재전달 시 정상 완료다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| 이벤트 기록 저장 뒤 실패하면 기록과 시도 변경이 없다 | 통과 | Y21. 테스트 전용 BEFORE_COMMIT 구독자가 한 번 던져 500이고, 기록이 없고 시도가 REQUESTED다 |
| 같은 이벤트 재전달이 200 PROCESSED다 | 통과 | Y21. DUPLICATE가 아니라 PROCESSED다. 첫 시도의 기록이 남지 않았다는 뜻이다 |
| 롤백된 트랜잭션의 이벤트는 구독자에 닿지 않는다 | 통과 | Y12. 롤백된 openAttempt의 PaymentRequested가 어느 구독자에도 닿지 않고 자동 결과 어댑터도 움직이지 않는다(E2) |
| 예약 정책의 실패 | 예약 몫 | 08-3 결정 6대로 예약 정책은 REQUIRES_NEW 구독자이고 그 실패는 콜백 응답이 아니라 로그와 후속 보정이다(계약 6절 충돌 하나 행) |

T23 판정: 결제 몫 통과. 계약이 적은 대로 결제 부분의 원자성(D-2)까지다.

### 1-11. T14, T18, T19

| ID | 문장 | 왜 해당 없음인가 |
|---|---|---|
| T14 | 결제 응답 유실과 같은 키 재전송 | PAY-01의 Idempotency-Key 몫이다. 결제 컨텍스트에는 키가 없다 |
| T18 | 시계가 expiresAt과 정확히 같을 때 결제 승인 | expiresAt은 Booking의 것이다. 결제는 그 시각을 모른다 |
| T19 | 만료 스케줄러와 승인 경합 | 스케줄러와 Booking 잠금이 예약 2차다. 결제는 Payment 잠금 아래에서 승인을 한 번만 반영한다는 것(Y22)까지 준비한다 |

### 1-12. 8-1절 항목별 대조

| ID | 확인할 것 | 결과 | 테스트 |
|---|---|---|---|
| Y1 | 첫 openAttempt와 진행 중 거절과 FAILED 뒤 2번 | 통과 | PaymentTest 3건 |
| Y2 | 셋째는 열리고 넷째는 AttemptLimitExceeded | 통과 | PaymentTest 1건. 경계 양쪽 |
| Y3 | APPROVED와 REFUNDED 뒤 AlreadyApproved | 통과 | PaymentTest 2건 |
| Y4 | 금액 불일치와 일치, 청구액 0 이하 | 통과 | PaymentTest 2건 |
| Y5 | 전이 둘과 역행 셋 | 통과 | PaymentTest 5건 |
| Y6 | 같은 거래 같은 결과의 전이 없음, 다른 거래 번호, 금액과 통화, 없는 시도, REFUNDED 뒤 같은 승인 | 통과 | PaymentTest 4건 |
| Y7 | refund의 전이와 멱등, 두 번째 무해, REQUESTED와 FAILED 거절, Mock 환불 호출 한 번 | 통과 | PaymentTest 3건, PaymentApplicationServiceTest 1건 |
| Y8 | 이벤트 넷의 페이로드와 attemptCount 1, 2, 3, 거절과 중복은 발행 없음 | 통과 | PaymentEventTest 4건 |
| Y9 | booking_id 유니크와 pg_transaction_id 유니크 | 통과 | PaymentApplicationServiceTest 2건. 실제 MySQL |
| Y10 | 잠긴 Payment 행의 openAttempt 대기와 I9 거절 | 통과 | PaymentApplicationServiceTest 1건. V14 방식 |
| Y11 | APPROVE와 DECLINE의 커밋 뒤 자동 결과, DEFER 대기 | 통과 | PaymentAutoResultTest 3건 |
| Y12 | 롤백된 openAttempt의 미전달 | 통과 | PaymentAutoResultTest 1건. 통과 쪽 짝 포함 |
| Y13 | 재개 러너 | 통과 | PaymentAutoResultTest 1건 |
| Y14 | attemptsOf 투영과 빈 예약 | 통과 | PaymentApplicationServiceTest 2건 |
| Y15 | 200 PROCESSED와 4개 필드, APPROVED와 FAILED | 통과 | MockPaymentEventApiTest 2건 |
| Y16 | 같은 eventId와 다른 eventId의 DUPLICATE, 실패 결과 | 통과 | MockPaymentEventApiTest 2건 |
| Y17 | 404와 409 넷, 무변경 | 통과 | MockPaymentEventApiTest 5건 |
| Y18 | 400 열여섯 경우와 경계값 통과 | 통과 | MockPaymentEventApiTest 2건 |
| Y19 | 401 둘과 403 셋, mock_001 통과 | 통과 | MockPaymentEventApiTest 1건 |
| Y20 | 환불 뒤 같은 승인 DUPLICATE | 통과 | MockPaymentEventApiTest 1건 |
| Y21 | 기록 뒤 강제 실패 롤백과 재전달 PROCESSED | 통과 | MockPaymentEventApiTest 1건 |
| Y22 | 잠금 뒤 같은 이벤트 둘 | 통과 | MockPaymentEventApiTest 2건. 같은 eventId와 다른 eventId |
| Y23 | dev 밖 401과 404, dev 안 둘 다 산다 | 통과 | DevProfileBoundaryTest 1건, MockPaymentEventApiTest 1건 |

8-1절 밖의 둘. PaymentTest의 시도 목록 정렬과 거래 번호 한 번 붙임 1건, PaymentApplicationServiceTest의 openAttempt 인자 검사 1건(계약 2절 openAttempt 표 1행). 스물셋 전부 통과다. 이월은 없다.

### 1-13. 계약표 후행조건 대조

06-4 1-2 결제 계약표의 Post 열이다.

| 행동 | Post | 결과 |
|---|---|---|
| openAttempt | REQUESTED 시도 추가, PaymentRequested 발행. 첫 요청이면 Payment 생성 | 통과. Y1, Y8 첫째, Y11(커밋 뒤 도착) |
| recordApproval | REQUESTED에서 APPROVED로, PaymentApproved 발행(시도 수 탑재) | 통과. Y5, Y8 셋째, Y15 첫째 |
| recordFailure | REQUESTED에서 FAILED로, PaymentFailed 발행(시도 수 탑재) | 통과. Y5, Y8 둘째, Y15 둘째 |
| refund | APPROVED에서 REFUNDED로, PaymentRefunded 발행 | 통과. Y7, Y8 넷째, Y14 |
| 같은 거래번호의 무해 무시(U4)와 REFUNDED 재호출 무해 | 상태 변경 없음, 발행 없음, 전이 없음 반환 | 통과. Y6, Y7 둘째, Y8, Y16, Y20 |
| 거절된 요청 | 저장 없음, 발행 없음 | 통과. Y8 첫째, Y12, Y17, Y21 |

### 1-14. 불변식 대조

계약 2-1절의 표다.

| 번호 | 문장 | 어디서 지키나 | 확인 |
|---|---|---|---|
| I6 | NORMAL 시도 수 <= 3 | Payment.openAttempt | 통과. Y2 |
| I7 | 승인 이력인 NORMAL 시도는 하나를 넘지 않는다 | Payment.openAttempt와 recordApproval | 통과. Y3. recordApproval의 둘째 승인 거부는 코드에 있으나 그 상태를 openAttempt로 만들 수 없어(I9와 I7이 앞에서 막는다) 직접 테스트가 없다. 6절 |
| I9 | REQUESTED인 NORMAL 시도는 동시에 하나 | Payment.openAttempt. 잠금 아래 | 통과. Y1, Y10 |
| U3 | 같은 bookingId의 Payment는 하나 | DB 유니크 booking_id | 통과. Y9 첫째 |
| U4 | 같은 pgTransactionId 콜백은 상태를 바꾸지 않고 이벤트를 다시 내지 않는다 | Payment.recordApproval과 recordFailure, DB 유니크 pg_transaction_id | 통과. Y6, Y8, Y9 둘째, Y16, Y22 |
| U5 | 같은 attemptId의 시도는 하나 | DB 기본키 | 통과. 스키마. Y13이 attemptId로 심고 읽는다 |
| 전이 폐쇄 | REQUESTED에서 APPROVED 또는 FAILED, APPROVED에서 REFUNDED뿐 | PaymentAttempt | 통과. Y5 다섯, Y17의 반대 결과 테스트 |
| 종착 무해 | REFUNDED 재환불과 같은 거래 같은 결과 콜백은 무변경과 무발행과 성공 | Payment | 통과. Y6, Y7 둘째, Y16, Y20 |
| 금액 일치 | 전달 총액이 청구액과 같다. 청구액은 첫 요청이 정한다 | Payment.openAttempt | 통과. Y4. R5의 결제 몫 |
| 이벤트 유일 | 같은 eventId는 한 번 저장된다 | DB 기본키 event_id | 통과. Y16, Y17의 같은 eventId 다른 body 테스트, Y22 첫째 |
| KRW | 금액은 KRW 정수 | shared Money | 통과. Y4 둘째, Y17의 통화 차이 USD |

### 1-15. 실행 결과

| 항목 | 값 |
|---|---|
| 테스트 | 337건. 실패 0, 오류 0, 건너뜀 0 |
| 그중 이 묶음이 더한 것 | 55건. 앞 묶음까지의 282건이 그대로 통과한다 |
| g1 code | 16건 통과. 결제 테스트 여섯 파일 55건 |
| 살아 있는 서버 HTTP 호출 | step6 18회. dev 15회(200 여섯, 400 셋, 401 하나, 403 하나, 404 하나, 409 넷)와 verify 3회(401, 404, 200). 전부 명세의 상태 코드 |
| 결과 파일 | harness/out/task-S9-payment-R1/ 아래 step4, step5, step6, step9 |
| 개정 1 보강 뒤 재실행 (2026-09-13 19:55) | 337건. 실패 0. 풀 기본값 READ COMMITTED가 걸렸다는 증거는 step9/test-summary.txt의 Hikari DEBUG 줄. 다른 컨텍스트 282건도 그 수준에서 그대로 통과 |

## 2. 회고 표

단계 번호는 계약 8절 기준이다. 시간은 progress.md 그 단계 행의 실제 시간 칸이다.

| 단계 | 실제 시간 | 막힌 것 | 하네스가 도움이 됐나 | 하네스가 방해했나 |
|---|---|---|---|---|
| 1 계약과 결정 승인 | 28분. 승인 턴 11분 | 브랜치가 다른 워크트리에 체크아웃돼 있어 커밋이 분리된 HEAD에 남았다 | 결정 6건을 안과 추천으로 미리 적어 승인이 한 번에 끝났다 | 없음. 워크트리 사정은 하네스가 아니라 git이다 |
| 2 이슈와 브랜치 | 2분 | 브랜치를 푸시할 수 없어 한 번 halted. 사용자가 브랜치를 놓아 풀렸다 | halted 행이 막힌 이유를 다음 행에 넘겼다 | 없음 |
| 3 백엔드 틀 | 건너뜀 | 없음 | 없음 | 없음 |
| 4 도메인과 앱과 인프라 | 21분 | 세션이 한 번 끊겨 19분과 2분으로 나뉘었다 | 계약 2절의 검사 순서 표 셋(INTERNAL-01 열셋, openAttempt 열, refund 문단)이 앱 서비스와 애그리거트의 골격이 됐다 | 없음 |
| 5 테스트 | 17분 | 없음 | 8-1절 Y12가 E2를 테스트로 강제했다. Y13의 T6 심기가 8-1절 문장에 이미 있었다 | 없음 |
| 6 INTERNAL-01과 T30 | 28분 | 첫 실행에서 Y22 둘이 붉었다. REPEATABLE READ의 스냅샷 고정. 세션이 그 사이 한 번 다시 열렸다 | 8-1절 Y22의 V14 방식이 없었으면 잠금 뒤 읽기가 앞 커밋을 못 보는 구현도 초록이었다 | 없음 |
| 6-2 조회 API | 건너뜀 | 없음 | 없음 | 없음 |
| 7과 8 프론트 | 해당 없음 | 없음 | 없음 | 없음 |
| 9 검증 | 이 문서 | 없음 | 검증 ID와 테스트를 잇는 표가 I7의 둘째 승인 거부에 직접 테스트가 없다는 것과 T26의 기동 연결이 코드 확인뿐이라는 것을 드러냈다 | fill이 고칠 파일 셋(ActorResolver, 설정 둘)의 반영 뒤 해시를 입력 변경으로 읽어 4건 실패. 검사기는 맞게 동작했고 계약이 고칠 파일을 입력 표에 넣은 탓이다(4절) |

측정된 개발 시간의 합은 66분이다. 4단계 21분, 5단계 17분, 6단계 28분이다. 1단계와 2단계는 승인과 브랜치라 개발 시간이 아니다. 4단계와 6단계의 세션 중단 사이 시간은 세지 않았다.

## 3. 회고 질문 넷

### 3-1. 한 바퀴에 실제로 몇 분이 들었나

개발 66분이다. 앞 묶음 예약 1차의 44분보다 22분 많고 HTTP 입구는 셋에서 하나로 줄었다. 늘어난 자리는 셋이다.

| 시간을 먹은 것 | 크기 |
|---|---|
| 애그리거트 하나에 규칙 열(I6, I7, I9, U4, 전이 폐쇄, 종착 무해, 금액 일치, 이벤트 유일, 검사 순서 열셋) | 4단계의 큰 몫. 프로덕션 파일 49개 |
| 커밋 뒤 도착을 세는 테스트 도구(CommittedPaymentEvents)와 잠금 경합 셋(Y10, Y22 둘) | 5단계와 6단계의 큰 몫 |
| Y22의 붉은 원인을 SQL 로그로 좁히는 것 | 6단계의 큰 몫. 코드 수정은 애너테이션 한 줄이었다 |

정지는 단계마다 한 번씩이었다(계약 6절 진행 방식 행). 앞 묶음이 정지점 둘로 달린 것과 다르고, 그 대신 세션 중단 둘을 단계 경계에서 이어받을 수 있었다.

### 3-2. 하네스 때문에 늘어난 시간이 얼마인가

| 늘린 것 | 값어치 |
|---|---|
| 계약 2절 검사 순서 표 열셋과 openAttempt 열 | 앱 서비스와 컨트롤러와 애그리거트의 순서가 표를 그대로 따랐다. 규칙 2(eventId 기록)가 거래 대조와 전이 사이에 있어 애그리거트 메서드를 둘로 가른 것도 표가 시켰다 |
| 8-1절 Y1부터 Y23 | Y22가 격리 수준 결함을 잡았다. Y12가 첫 구독자 묶음의 E2를 강제했다 |
| 결과 파일 사본과 http-calls.txt | 실제 서버의 DB 상태 확인(기록 셋)이 파일로 남았다 |
| progress.md 행과 실제 시간 | 단계마다 한 번. 세션 중단 뒤 재개의 근거였다 |
| fill의 hash-stale | 9단계에서 고친 파일 셋의 해시를 갱신하는 일이 생겼다. 검사기가 아니라 계약 작성의 몫이다 |

하네스가 못 잡은 것 하나. 격리 수준은 5단계의 앱 서비스 잠금 테스트(Y10)를 지났다. Y10의 잠금 조회는 예약 ID로 루트만 찾는 문장이라 조인이 없고, 스냅샷이 잠금 뒤에 잡힌다(2026-09-13 SQL 로그로 확인. 처음 적은 이유인 잠금 뒤 읽을 것이 없어서는 틀렸다). 시도 ID로 찾는 조회는 시도 표를 조인하므로 그 첫 읽기가 잠금 전에 스냅샷을 굳힌다. 6단계의 Y22처럼 잠금 뒤 읽기가 앞 커밋의 결과를 봐야 하는 경로에서만 드러난다. 8-1절이 그 경로를 미리 적어 둔 것이지 검사기가 잡은 것이 아니다.

### 3-3. 빌드와 테스트가 잡은 결함과 사람이 잡은 결함의 비율

| 잡은 주체 | 무엇 | 건수 |
|---|---|---|
| 빌드와 테스트 | Y22 둘. Hibernate 7의 루트만 잠그는 조회가 조인한 시도 표를 잠금 없이 읽어 REPEATABLE READ 스냅샷이 잠금 전에 굳는 것 | 1 |
| 검사기 | 없음. fill의 hash-stale 4건은 의도된 변경(D-3)의 기록 방식 문제이지 결함이 아니다 | 0 |
| 나 | 계약 8절이 앱 서비스 메서드로 적은 resumeAutoResults가 러너에 있어야 하는 것(자기 호출은 REQUIRES_NEW 프록시를 안 탄다), 계약 8-1절 마지막 문단의 @EventListener로는 기록 뒤 실패를 만들 수 없는 것(발행이 기록보다 앞이라 BEFORE_COMMIT이어야 한다), 6단계 기록 두 곳(이벤트 기록 넷을 셋으로, 시각 19:00을 18:58로) | 4 |
| 사용자 | 없음. 단계 정지점에서 지시만 있었다 | 0 |

앞 묶음은 사용자 0, 테스트 1, 나 3이었다. 이번도 같은 모양이다. 테스트가 잡은 하나가 가장 무거웠고 사람이 잡은 넷은 계약과 기록의 어긋남이다.

### 3-4. 동결 중 관찰이 몇 건 쌓였고 그중 실제로 필요한 것은 몇 건인가

troubleshooting.md에 더할 만한 것 다섯이다. 전부 progress.md 교훈 칸이나 안 해 본 것 칸에 있다. 확정 반영에서 첫 행과 개정 2부터 5를 여섯 행으로 올렸다(2026-09-13 19:53).

| 무엇 | 고쳐야 하나 |
|---|---|
| Hibernate 7이 루트만 잠그는 조회(for update of 루트)에서 조인한 자식 표를 잠금 없이 읽고, MySQL REPEATABLE READ가 그 첫 읽기에서 스냅샷을 고정한다. 잠금 뒤 자식을 다시 읽어 앞 커밋을 봐야 하는 경로는 READ COMMITTED가 필요하다 | 그렇다. 이 묶음은 고쳤다. 확정 반영에서 풀 기본값도 READ COMMITTED로 바꿔(개정 1 보강) 다른 컨텍스트와 바깥 트랜잭션까지 같은 수준이라 조인 유무 점검이 필요 없어졌다. 규칙으로 올릴지는 한 번 더 나면 |
| 시도 표를 먼저 잠그는 안은 루트를 쥔 채 시도를 고치는 쪽과 교착한다 | 아니다. 안 해 볼 것 목록. 잠금 순서는 루트 하나(08-3 결정 3) |
| Spring Data save()가 할당 ID 엔티티에 select 뒤 insert(merge)를 낸다 | 아니다. 알아 둘 것. 이벤트 기록 한 건에 질의 하나가 더 붙는다 |
| 실제 서버 검증의 데이터 심기는 컨테이너 안 mysql로 하고 비밀번호는 컨테이너 환경변수 이름으로만 쓴다 | 아니다. 방식으로 남긴다. backend/.env를 읽지 않는 약속과 맞다 |
| bootRun을 백그라운드로 띄우고 taskkill로 내리면 작업이 실패 코드 1로 보고된다 | 아니다. 예상된 것. 기동 로그의 profile is active 줄을 증거로 읽는다 |

## 4. 계약과 다르게 만든 것 (개정 후보. 2026-09-13 다섯 다 확정, 개정 1 보강)

계약 10절 개정 칸에 올릴 다섯이다. 다섯 다 응답과 상태는 계약과 같고 자리나 도구가 다르다. 2026-09-13 사용자가 대안 분석을 본 뒤 그대로 반영하라고 해 다섯을 계약 10절에 확정했다. 개정 1은 풀 기본값까지 보탰고(아래 실제 칸), 개정 5의 양식과 검사기 몫은 하네스 이슈 135로 넘겼다.

| 번호 | 계약 | 실제 | 왜 |
|---|---|---|---|
| 개정 1 | 2절 검사 순서 표 4행과 7절 D-2가 Payment 잠금(PESSIMISTIC_WRITE)만 적고 격리 수준을 적지 않았다 | PaymentApplicationService의 트랜잭션이 전부 READ COMMITTED다. deliverAutoResult는 REQUIRES_NEW에 READ COMMITTED. 확정 반영(2026-09-13)에서 풀 기본값도 READ COMMITTED로 바꿨다(설정 파일 둘의 spring.datasource.hikari.transaction-isolation). 바깥 트랜잭션이 REQUIRED로 앱 서비스를 감싸면(예약 2차 중계) 스프링이 서비스의 지정을 무시하므로 서비스 지정만으로는 구멍이 남아서다. 서비스 지정은 뜻을 적는 용도로 남겼다 | Y22가 붉었다. 루트만 잠그는 조회가 조인한 시도 표를 잠금 없이 읽어 REPEATABLE READ 스냅샷이 잠금 전에 굳고, 잠금 뒤 읽는 시도와 이벤트 기록이 앞 커밋을 못 봤다. 잠금 순서는 그대로다 |
| 개정 2 | 8절 4단계가 resumeAutoResults를 PaymentApplicationService의 메서드로 적었다 | infrastructure의 MockAutoResultResumeRunner.resumeAutoResults다. 앱 서비스에는 findAutoAttemptsToResume과 deliverAutoResult가 있다 | 앱 서비스 안에서 자기 메서드를 부르면 REQUIRES_NEW 프록시를 타지 않는다(06-4 v5 0-3의 리스너 예외 위치와 같은 이유) |
| 개정 3 | 8-1절 마지막 문단이 Y21의 강제 실패 구독자를 @EventListener로 트랜잭션 안에서 던진다고 적었다 | @TransactionalEventListener(phase = BEFORE_COMMIT)로 커밋 직전에 한 번 던진다 | @EventListener는 발행 시점에 터지는데 발행(순서 11)이 이벤트 기록 저장(순서 12)보다 앞이라 기록 뒤 실패가 아니다. T23의 문장이 이벤트 저장 이후의 실패다 |
| 개정 4 | 8-1절 Y22가 같은 이벤트 둘에 이벤트 기록은 각자 하나씩이라 적었다 | 테스트 둘로 갈랐다. 같은 eventId 둘은 기록 하나(규칙 2), 다른 eventId 둘은 기록 각자 하나씩(규칙 3) | 6절 이벤트 기록의 저장 범위 행대로 같은 eventId가 다시 오면 기록으로 답하고 더하지 않는다. 각자 하나씩은 다른 eventId일 때만 참이다 |
| 개정 5 | 4절 입력 표가 고칠 파일 셋(ActorResolver, 설정 파일 둘)의 고치기 전 해시를 적었다 | 9단계에서 반영 뒤 해시로 갱신하고 원래 값을 읽을 범위 칸에 남겼다. 5절의 설정 파일 행도 같다 | fill은 기록된 해시와 실제가 다르면 실패시킨다. 고칠 파일은 입력이자 산출이라 반영 뒤 갱신 규칙이 계약에 있어야 했다 |

## 5. 이 Task의 완료 조건 대조

계약 작업 표의 완료 기준 넷이다.

| 조건 | 결과 |
|---|---|
| 9절 계약 테스트 ID가 전부 통과이거나 미실행 사유와 함께 기록 | 충족. T21, T26, T30 전부와 INTERNAL-01 오류 넷은 통과. T15, T16, T17, T20, T22, T23은 계약이 처음부터 결제 몫으로 적었고 그 몫은 통과이며 예약 몫의 사유는 1-5부터 1-10. T14, T18, T19는 해당 없음(1-11) |
| 8절 단계가 전부 끝난다 | 충족. 3단계와 6-2단계는 건너뜀, 7과 8단계는 해당 없음, 1, 2, 4, 5, 6단계는 applied, 9단계는 이 문서이고 정지가 사용자 완료 판단이다 |
| 기계 판독 결과 파일에서 실행 수가 0이 아니고 실패 수가 0이며 앞 묶음까지의 282건이 그대로 통과 | 충족. 337건, 실패 0. 앞 282건 포함 |
| 모든 단계의 실제 시간이 분 단위로 기입 | 충족. 1, 2, 4, 5, 6단계 전부 progress.md에 있고 9단계는 이 단계의 행에 적는다 |

## 6. 남은 것

| 무엇 | 어디로 |
|---|---|
| 개정 후보 다섯을 계약 10절에 확정 | 끝(2026-09-13). 계약 2절, 3절, 6절, 7절, 8절, 8-1절, 9절, 10절에 반영 표시. 개정 1은 풀 기본값 보강까지 |
| 고칠 파일 행 유형(반영 전과 후 해시)을 양식과 fill에 | 하네스 이슈 135. 하네스 세션 몫 |
| PAY-01과 PAY-02, openAttempt와 refund와 attemptsOf의 호출자, PaymentApproved와 PaymentFailed의 구독자(확정, 만료, 지연 승인 환불, 취소 환불) | 예약 2차 계약 task-S9-booking-lifecycle.md. 이 PR이 main에 들어온 뒤 |
| T15, T16, T17, T20, T22, T23의 예약 몫과 T14, T18, T19 | 예약 2차 |
| 실제 앱 재기동으로 재개 러너를 보는 것 | 예약 2차의 6단계 실제 서버 검증에서 REQUESTED APPROVE 시도를 심고 재기동. 이 묶음은 러너 메서드 호출(Y13)까지다 |
| I7의 둘째 승인 거부(recordApproval)에 직접 테스트 | 그 상태를 만들 경로가 생기면. 지금은 I9와 I7이 openAttempt에서 막아 도달 불가다 |
| 다른 컨텍스트의 잠금 조회에 조인이 있는지 점검 | 필요 없어졌다. 풀 기본값 READ COMMITTED(개정 1 보강)가 조인 유무와 무관하게 전 컨텍스트에 걸리고 282건이 그대로 통과했다 |
| ActorRegistry의 프로파일 미룸 주석이 낡았다 | ActorResolver가 T30을 맡았다고 ActorResolver 주석에 적었다. ActorRegistry는 이 계약의 변경 허용 파일이 아니라 두었다. 별도 이슈 몫 |
| Money 상한 1,000,000,000과 INTERNAL-01 형식 상한 30,000,000,000의 차이 | 계약 6절 P03 행. 별도 shared 이슈 몫 |
| 블라인드 평가 | 오늘의 전제 결정 3. MVP 코드가 다 붙은 뒤 한 번. 이 묶음 몫은 eval-target-files.md |
| 프론트 화면과 연결 테스트 | 별도 Task. 착수 시점은 사용자가 정한다 |
