# 작업 계약 task-S9-booking-lifecycle (예약과 선점 묶음. 2차: 결제 중계와 확정과 만료와 취소)

최초 작성: 2026-09-13
최종 갱신: 2026-09-13 (계약 승인. 결정 5건 추천대로. 6절 확정 칸 아홉, 7절 결정 칸 다섯, 10절. join5201, 2026-09-13. 결제 코드의 실제 이름은 결제 PR이 main에 들어간 뒤 2단계에서 대조한다)
양식: harness/prompts/task-contract.md v6

이 계약은 다섯 번째 구현 묶음이다. task-S9-catalog, task-S9-inventory-rate, task-S9-booking 1차(PR 127), task-S9-promotion-search(PR 98)가 main에 있고 task-S9-payment(브랜치 feat/task-s9-payment, 계약 승인 2026-09-12)가 그 위에 얹힐 예정이다. 1차 계약 7절 D-5 나에 따라 2차는 새 계약이고 이 파일이 그것이다. 사용자가 2026-09-13에 결제 계약 승인(2026-09-12)만 보고 초안을 미리 쓰라고 했으므로 결제 쪽 접점은 승인된 결제 계약의 이름(openAttempt, refund, attemptsOf, PaymentApproved, PaymentFailed)으로 적는다. 결제 PR이 main에 들어가면 2단계에서 실제 코드의 이름과 대조하고 다르면 10절 개정 칸에 적는다.

쉽게 말하면 1차가 방 잡기였다면 2차는 잡은 방의 그 뒤다. 손님이 결제를 누르면 예약이 결제 장부에 이만큼 청구해 달라고 넘기고(PAY-01), 장부가 승인이나 거절을 알려 오면 예약이 그것을 듣고 확정하거나(재고를 선점에서 판매로), 세 번 거절이면 방을 풀고(만료), 10분이 지나도 소식이 없으면 시계가 방을 푼다(TTL 만료). 확정된 예약은 체크인 전날까지 손님이 취소할 수 있고 그러면 돈을 돌려주고 판매분을 반환한다(BOOK-04). 늦게 온 승인은 이미 풀린 방에는 붙일 수 없으니 돈만 돌려준다. 그리고 1차가 요금만 더했던 가격 계산을 프로모션 세션의 계산기로 바꿔 할인이 예약에 들어오게 한다.

## 작업

| 항목 | 내용 |
|---|---|
| Task ID와 Step | task-S9-booking-lifecycle. Step 9 구현. 기능 묶음은 10-6 3절 표의 결제와 확정 행 중 예약 몫(PAY-01, PAY-02, ConfirmBooking)과 취소와 만료 행 전부(BOOK-04, ExpireBooking, RefundPayment 중계)와 접근 범위 행의 소유권 몫(T02의 결제 구간)이다. 결제 컨텍스트 몫(INTERNAL-01, Payment, Mock PG, T30)은 task-S9-payment다 |
| 작업 유형 | 코드 |
| 목표 | 예약 상태 기계의 전이 셋(confirm, expire, cancel)과 두 세션의 접점 셋(가격 계산기, 결제 앱 서비스, 결제 이벤트 구독)을 붙여 예약 묶음을 닫는다. Booking 응답의 payment가 채워지고, 재고가 확정과 만료와 취소마다 정확히 한 번 움직이며(R4), 확정된 예약의 청구액이 요금과 프로모션 변경에 흔들리지 않고(R5), 같은 승인 콜백이 몇 번 와도 확정과 환불이 한 번이다 |
| 대상 API ID | PAY-01, PAY-02, BOOK-04. 내부 처리 RequestPayment 중계, ConfirmBooking과 CommitInventory, ExpireBooking(TTL_EXPIRED, PAYMENT_FAILED)과 선점 반환, CancelBooking과 RefundPayment 중계와 판매분 반환, TTL 스케줄러. BOOK-01의 가격 포트 교체와 BOOK-02와 BOOK-03의 payment 채움 |
| 선행 작업 | task-S9-payment의 PR이 main에 들어가야 한다. 이 묶음이 결제에서 쓰는 것은 앱 서비스 공개 메서드 셋(openAttempt, refund, attemptsOf)과 이벤트 둘(PaymentApproved, PaymentFailed)과 값 객체(PaymentAttemptId, MockMode, RefundReason)와 뷰다. 프로모션에서 쓰는 것은 PricingService.quote 하나다. 나머지 선행 묶음은 전부 main에 있다 |
| 완료 기준 | 넷을 모두 만족해야 한다. 첫째, 9절 계약 테스트 ID가 전부 통과이거나 미실행 사유와 함께 기록된다. 둘째, 8절 단계가 전부 끝난다. 셋째, backend/build/test-results/test/*.xml의 실행 수가 0이 아니고 failures와 errors 합이 0이며 앞 묶음까지의 테스트(282건에 결제 묶음 수를 더한 것. 2단계에서 숫자를 적는다)가 그대로 통과한다. 넷째, 모든 단계의 실제 시간이 harness/state/progress.md에 분 단위로 기입돼 있다 |
| 변경 허용 파일과 범위 | backend/src/main/java/com/o2o/booking/ 전체와 그 테스트 backend/src/test/java/com/o2o/booking/. backend/src/main/java/com/o2o/inventory/domain/InventoryAllocationService.java(commit과 releaseHeld와 releaseSold의 N행 적용)와 그 테스트. backend/src/test/resources/application.properties에 7절 D-3의 줄 하나(승인 시). harness/out/task-S9-booking-lifecycle-R1/ 아래 실행 결과. harness/state/progress.md 행 추가. 이 계약 파일. payment, promotion, search, catalog 패키지와 inventory의 나머지와 shared의 기존 파일은 고치지 않는다 |
| 범위 밖과 유지할 전제 | 아래 3절 |
| 기준 버전 | 문서는 4절 입력 표의 sha256. 코드는 작업 브랜치 feat/task-s9-booking-lifecycle의 커밋 해시(결제 PR을 병합한 origin/main 위)와 5절 평가 대상 행의 파일 목록 |
| 후보 작업 공간 | backend/. 워크트리는 사용자가 정한다(초안은 세션 B의 임시 워크트리에서 썼다). 브랜치 feat/task-s9-booking-lifecycle. 후보와 정본의 구분은 git 브랜치가 맡는다 |
| 결과 기록 경로 | harness/state/progress.md |

## 1. 이 Task가 앞 묶음과 다른 점

| 항목 | task-S9-booking 1차 | 이 Task |
|---|---|---|
| 애그리거트 | Booking을 새로 만든다. HELD 생성만 | 같은 Booking에 전이 셋을 더한다. confirm, expire(reason), cancel(reason). 컬럼 다섯이 는다(expiration_reason, cancellation_reason, confirmed_at, canceled_at, expired_at). version이 전이마다 오른다 |
| 잠금 | 재고 N행만. Booking은 insert | 08-3 결정 3의 전역 순서 Booking, Payment, 재고 N행(날짜 오름차순). 모든 경로가 Booking을 먼저 잠근다. 06-4 v5 0-1 표 |
| 다른 컨텍스트 호출 | 재고 도메인 서비스 하나 | 셋. 재고(commit, releaseHeld, releaseSold), 결제 앱 서비스(openAttempt, refund, attemptsOf), 프로모션 도메인 서비스(PricingService.quote). 방향은 전부 예약에서 밖으로다(06-1 R6, 접점 표) |
| 이벤트 | 발행만. BookingCreated, InventoryHeld | 발행 셋이 늘고(BookingConfirmed, BookingExpired, BookingCanceled) 첫 프로덕션 구독자 둘이 생긴다(PaymentApproved, PaymentFailed). layers.md 3-3 E1과 E2, 08-3 결정 6의 두 빈 형태 |
| 시간 | Clock으로 지금을 읽어 expiresAt을 정한다 | 시간이 상태를 바꾼다. TTL 스케줄러가 기한 지난 HELD를 만료시키고, 승인 처리 시각이 expiresAt 이상이면 확정 대신 환불이다. 처리 시각이 정확히 expiresAt이면 만료다(11 상태 전이와 시간 경계) |
| HTTP 입구 | 셋. 전부 GUEST | 셋이 는다. PAY-01과 BOOK-04는 멱등이고 PAY-02는 조회다. 셋 다 GUEST이고 남의 예약은 404다 |
| 멱등 | 경로 하나(/api/v1/bookings) | 경로 셋. 범위의 경로 칸에 예약 ID가 들어간다(/api/v1/bookings/{id}/payment-attempts, /cancellations). 같은 IdempotencyRecord와 실행기를 쓴다(1차 D-1 가) |
| 응답 모델 | Booking 20개 필드. payment는 0과 null과 빈 배열과 null 고정 | payment가 결제의 attemptsOf로 채워진다(PaymentSummary 4개, PaymentAttempt 11개, Refund 7개). PAY-01은 PaymentAttempt, PAY-02는 PaymentAttemptList 3개 |
| 가격 | 포트 뒤에서 요금만 합산. 할인 0 | 포트 뒤 어댑터를 프로모션의 PricingService로 바꾼다. appliedPromotion과 discountAmount가 실제 값이 된다. T12와 T13의 프로모션 절반이 닫힌다 |
| 스케줄러 | 없음 | 첫 스케줄러. 주기와 켜고 끄기는 설정값(7절 D-3) |

경합이 이번 묶음의 난이도다. 1차가 같은 날짜에 온 두 예약의 경합이었다면 이번은 같은 예약에 온 승인과 만료의 경합이고(T19), 그 답은 Booking 한 행의 잠금과 처리 시각 기준 판정이다. 결제 이벤트가 유실됐을 때의 자가 치유는 7절 D-1이 정한다.

## 2. 대상과 설계 근거

| 대상 | 모양 | 인증 | 계약표 행 | 불변식 |
|---|---|---|---|---|
| PAY-01 | POST /api/v1/bookings/{bookingId}/payment-attempts. 202 PaymentAttempt와 Location. Idempotency-Key 필수 | GUEST, 소유자 | RequestPayment 중계(예약 앱 서비스) + openAttempt(결제). 06-4 1-2 예약 표 마지막 행 | I4(청구액은 스냅샷 총액), 잠금 순서 |
| PAY-02 | GET /api/v1/bookings/{bookingId}/payment-attempts. 200 PaymentAttemptList | GUEST, 소유자 | 조회. 06-4 v5 2-6이 조회는 결제가 제공하고 예약이 부른다고 적는다. attemptsOf | 없음 |
| BOOK-04 | POST /api/v1/bookings/{bookingId}/cancellations. 200 Booking. Idempotency-Key 필수. body의 reason은 선택이고 최대 300자 | GUEST, 소유자 | cancel(). 06-4 1-2 예약 표. RefundPayment 중계와 releaseSold ×N이 같은 트랜잭션 | I5, R4의 판매분 반환 한 번, 종착 무해 |
| P1 결제 승인 처리 | PaymentApproved 구독자. Booking 상태로 분기. HELD면 확정, EXPIRED면 지연 승인 환불, 그 외 무시 | 없음. 내부 | confirm() + commit ×N, 또는 refund(LATE_APPROVAL). 06-4 2-2 정책 카드 첫 둘, 11 INTERNAL-01 규칙 5 | I5, I7의 예약 몫(확정은 승인 시도 하나로), R4 |
| P3 결제 실패 시 만료 | PaymentFailed 구독자. attemptCount 3 이상이고 HELD면 만료(PAYMENT_FAILED). TTL이 지났으면 TTL_EXPIRED 우선 | 없음. 내부 | expire(PAYMENT_FAILED) + releaseHeld ×N. 06-4 2-2 결제 실패 시 만료, 11 규칙 6 | I5, R4 |
| T1 TTL 만료 | 스케줄러. status HELD이고 expiresAt <= 지금인 예약을 건별 트랜잭션으로 만료. 승인 시도가 있으면 확정 우선(08-3 결정 11의 11-1) | 없음. 내부 | expire(TTL_EXPIRED) + releaseHeld ×N. 확정 우선 분기는 P1과 같은 confirm() + commit ×N. 06-4 2-2 TTL 만료, 11 P01 | I5, R4, 잠금 순서 |
| 가격 포트 어댑터 | booking/infrastructure에서 프로모션의 PricingService.quote를 부르고 결과를 booking의 PriceSnapshot으로 옮긴다 | 없음 | 06-4 1-2 PricingService 행. 1차 D-4 가의 교체 자리 | I10, I11, I12, I15(옮긴 스냅샷도 같은 검증을 지난다) |
| Booking 전이 셋과 이벤트 셋 | confirm, expire(reason), cancel(reason). 전이 발생 여부를 돌려준다. BookingConfirmed, BookingExpired, BookingCanceled | 없음 | 06-4 1-2 confirm, expire, cancel 행. 1-3 예약 상태 표 | I5, 종착 무해 |
| 재고 N행 적용 | InventoryAllocationService에 commit ×N, releaseHeld ×N, releaseSold ×N. 잠금은 lock과 같은 findRangeForUpdate | 없음 | 06-4 1-2 commit, releaseHeld, releaseSold 행. 1차 8-1 K5가 한 행 메서드를 닫았다 | I1, I1a |
| Booking 응답의 payment | BookingResponse가 attemptsOf의 결과로 PaymentSummary를 채운다. BOOK-01부터 BOOK-04가 같은 변환을 쓴다 | 없음 | 11 응답 모델 PaymentSummary(2546행), PaymentAttempt(2514행), Refund(2532행) | 없음 |

묶음의 근거는 10-6 3절 표의 결제와 확정 행과 취소와 만료 행이다. 결제 계약이 결제 컨텍스트 몫을 떼어 갔으므로 남은 것이 전부 여기다. 셋(확정, 만료, 취소)을 한 계약으로 받는 이유는 1차 D-5가 적었듯 같은 상태 기계(I5)를 나눠 가져서다.

PAY-01의 검사 순서. 앞 검사에서 거절되면 뒤 검사는 하지 않는다. 층은 06-4 1-4 검증 책임 위치 표를 따른다. 1부터 5는 1차 계약 2절 검사 순서 표(개정 2와 3 반영)와 같은 모양이다. 11 명세 결제 접수와 환불 절이 새 시도 처리 순서를 멱등 재전송 확인, TTL 만료 여부, 예약 상태, 진행 중 시도, 시도 한도로 적고 이 표가 그것을 따른다.

| 순서 | 무엇 | 실패 시 응답 | 층 |
|---|---|---|---|
| 1 | body 형식. mockMode는 선택이고 APPROVE, DECLINE, DEFER 중 하나. 생략은 APPROVE. 미정의 필드 거절. body 없음은 빈 객체와 같다 | 400 INVALID_REQUEST | api. 프레임워크가 인자를 해석하며 본다 |
| 2 | X-Dev-Actor-Id가 등록된 GUEST | 401 ACTOR_REQUIRED, 403 ACCESS_DENIED | api (ActorResolver) |
| 3 | Idempotency-Key 존재와 형식 | 400 IDEMPOTENCY_KEY_REQUIRED | api |
| 4 | 멱등 기록 조회. 범위는 행위자 ID + POST + /api/v1/bookings/{bookingId}/payment-attempts + 키. 경로에 예약 ID가 들어가므로 다른 예약에 같은 키를 써도 다른 범위다 | 완료 기록이고 body 같음이면 최초 응답 재전송과 Idempotency-Replayed: true. body 다름이면 409 IDEMPOTENCY_KEY_REUSED. 진행 중이면 409 REQUEST_IN_PROGRESS와 Retry-After: 1 | application |
| 5 | 진행 중 기록 삽입 | 해당 없음 | application, DB |
| 6 | 예약 존재와 소유. 없거나 남의 것이면 자원 정보 없이 404 | 404 RESOURCE_NOT_FOUND. 5의 기록은 규칙 7로 지워진다 | application |
| 7 | 만료 시각을 지난 HELD면 먼저 만료와 선점 반환을 저장한다. 별도 트랜잭션이다(7절 D-2) | 저장 뒤 409 BOOKING_EXPIRED. 이미 EXPIRED도 409 BOOKING_EXPIRED | application (REQUIRES_NEW), domain, 재고 |
| 8 | Booking 잠금. PESSIMISTIC_WRITE | 해당 없음. 같은 예약의 요청 둘은 줄을 선다 | infrastructure |
| 9 | 잠금 아래에서 상태 재확인. HELD이고 만료 시각 전 | CONFIRMED와 CANCELED는 409 BOOKING_STATE_CONFLICT. EXPIRED와 만료 시각을 지난 HELD는 409 BOOKING_EXPIRED(7 뒤에 넘어온 좁은 창. 만료 저장은 T1이 한다) | application, domain |
| 10 | 결제 openAttempt(bookingId, 스냅샷 총액, mockMode). 결제가 Payment를 잠그고 I9, I6, I7, 금액 일치를 본다 | AttemptInProgress는 409 PAYMENT_IN_PROGRESS. AttemptLimitExceeded는 409 PAYMENT_ATTEMPTS_EXHAUSTED. AlreadyApproved는 409 BOOKING_STATE_CONFLICT(HELD인데 승인 이력이 있는 것은 P1 유실 창이고 7절 D-1이 닫는다). AmountMismatch는 500(청구액은 첫 요청이 스냅샷에서 정하므로 나올 수 없다) | application(예약이 매핑), 결제 |
| 11 | 응답 202. body는 결제가 돌려준 REQUESTED 시도 뷰를 11 PaymentAttempt 11개 필드로. Location은 /api/v1/bookings/{bookingId}/payment-attempts | 해당 없음 | api |
| 12 | 멱등 기록을 완료로 바꾸고 최초 응답을 저장. 시도 저장과 같은 트랜잭션(규칙 6) | 해당 없음 | application |
| 13 | 커밋. 그 뒤 결제의 자동 결과 어댑터가 APPROVE와 DECLINE을 처리하고, 그 커밋 뒤 이 묶음의 P1 또는 P3가 돈다. 응답 body는 11이 적은 대로 접수 결과(REQUESTED)다 | 해당 없음 | application |

6부터 10에서 거절되면 5의 진행 중 기록을 지운다(규칙 7). 7만 예외적으로 커밋을 남긴다. 11 명세가 HELD는 먼저 만료와 재고 반환을 저장한다고 적기 때문이고 그 방법은 7절 D-2다.

BOOK-04의 검사 순서. 1부터 6은 PAY-01과 같고 경로만 /cancellations다. body는 reason 하나이며 선택이고 최대 300자, 생략은 빈 문자열, 미정의 필드는 400이다.

| 순서 | 무엇 | 실패 시 응답 | 층 |
|---|---|---|---|
| 7 | Booking 잠금 | 해당 없음 | infrastructure |
| 8 | 상태가 CONFIRMED | HELD, EXPIRED, CANCELED는 409 BOOKING_STATE_CONFLICT. HELD 이탈은 TTL이 하고(11 BOOK-04 규칙), CANCELED에 새 키로 온 재취소는 사람 경로라 오류다(08-3 결정 5) | domain |
| 9 | 서울 오늘이 checkIn보다 앞 | 409 CANCELLATION_NOT_ALLOWED. 체크인 당일부터 불가. 6절 P05 | application (SeoulDate.today) |
| 10 | 결제 refund(approvedAttemptId, BOOKING_CANCELED). 결제가 Payment를 잠근다. 재고 잠금보다 앞이다(08-3 결정 10) | NoApprovedAttempt는 500(CONFIRMED는 승인 시도가 있어야 하므로 불변 위반) | application, 결제 |
| 11 | cancel(reason). CANCELED, canceledAt, cancellationReason, version 증가 | 해당 없음 | domain |
| 12 | 재고 N행 잠금과 releaseSold ×N | InsufficientSoldException은 500(불변 위반. 전체 롤백) | 재고 |
| 13 | 응답 200 Booking 20개 필드. payment.refund가 채워지고 attempts의 승인 시도는 APPROVED 그대로다(11 PAY-02 규칙) | 해당 없음 | api |
| 14 | 멱등 기록 완료. 커밋. 그 뒤 BookingCanceled가 구독자에 닿는다(프로덕션 구독자는 없다) | 해당 없음 | application |

10부터 12가 한 트랜잭션인 근거는 08-3 결정 11의 11-2(취소 환불 동기)와 결정 7과 11 BOOK-04 규칙(취소 상태, 환불 기록, 재고 반환이 함께 저장돼야 200)이다. 06-4 v4 2-2의 취소 시 환불 카드는 결과적 일관성으로 적혀 있으나 08-3이 그것을 동기로 뒤집었고 결정 11건은 전부 수용됐다(6절 충돌 행).

P1 결제 승인 처리의 분기. 결제의 INTERNAL-01 또는 자동 결과 어댑터가 커밋한 뒤 AFTER_COMMIT으로 받는다. 어댑터는 booking/infrastructure에 있고 try와 catch로 booking/application의 REQUIRES_NEW 메서드를 부른다(08-3 결정 6, 06-4 v5 0-3). 잠금 순서는 Booking, Payment, 재고 N행이다.

| 잠금 뒤 Booking 상태 | 처리 시각 | 처리 | 근거 |
|---|---|---|---|
| HELD | expiresAt 전 | confirm(), commit ×N. 커밋 뒤 BookingConfirmed | 06-4 2-2 결제 승인 시 예약 확정, 11 상태 전이 표 둘째 행 |
| HELD | expiresAt 이상(같음 포함) | expire(TTL_EXPIRED), releaseHeld ×N, refund(paymentAttemptId, LATE_APPROVAL). 커밋 뒤 BookingExpired | 11 시간 경계(정확히 expiresAt이면 만료), T18 |
| EXPIRED | 무관 | refund(paymentAttemptId, LATE_APPROVAL). 재고 변경 없음. 이미 REFUNDED면 결제가 무해로 답한다 | 06-4 2-2 승인 지연 시 자동 환불, 11 규칙 5, T20 |
| CONFIRMED, CANCELED | 무관 | 로그 후 무시. 상태와 재고와 환불 변경 없음 | 08-3 결정 5. 같은 거래 재전달은 결제가 DUPLICATE로 막아 여기 오지 않고(T21, T22), 다른 시도의 둘째 승인은 결제 I7이 막는다 |
| 예약 없음 | 무관 | 불변 위반. 예외로 던지고 어댑터가 로그 | 06-4 v5 0-3 마지막 문단 |

P3 결제 실패 시 만료의 분기. 같은 형태다. 이벤트의 attemptCount가 3 미만이면 아무것도 하지 않는다(T16). 3 이상이고 HELD면 처리 시각이 expiresAt 전일 때 expire(PAYMENT_FAILED)이고 이상일 때 expire(TTL_EXPIRED)다(11 시간 경계. 이미 확정된 종료 원인을 덮어쓰지 않고 아직 HELD면 TTL_EXPIRED 우선). 둘 다 releaseHeld ×N이 같은 트랜잭션이다. EXPIRED면 무해, CONFIRMED와 CANCELED면 로그 후 무시다.

T1 TTL 만료의 건별 처리. 스케줄러가 잠금 없이 due 목록(status HELD, expiresAt <= 지금, 배치 크기만큼, expiresAt 오름차순)을 읽고 건마다 REQUIRES_NEW 트랜잭션을 연다. 잠금 뒤 재확인이 없으면 승인 처리와 같은 예약을 두 번 만진다.

| 잠금 뒤 상태 | 결제 조회(attemptsOf) | 처리 | 근거 |
|---|---|---|---|
| HELD이고 expiresAt <= 지금 | approvedAttemptId 있음 | confirm(), commit ×N. 확정 우선 | 08-3 결정 11의 11-1과 결정 8. P1이 유실된 예약의 자가 치유 자리(7절 D-1) |
| HELD이고 expiresAt <= 지금 | 없음 | expire(TTL_EXPIRED), releaseHeld ×N | 06-4 2-2 TTL 만료, 11 상태 전이 표 다섯째 행 |
| HELD이고 expiresAt > 지금 | 무관 | 스킵. 목록을 읽은 뒤 잠금을 기다리는 동안 시계가 앞선 경우를 위한 재확인 | 종착 무해와 같은 결 |
| CONFIRMED, EXPIRED, CANCELED | 무관 | 스킵. 잠금 대기 중 다른 경로가 먼저 전이했다 | 종착 무해 |

응답 모델. PaymentAttempt 11개 필드(id, bookingId, attemptNumber, status, amount, currency, pgTransactionId, mockMode, requestedAt, completedAt, failureCode)는 11 명세 2514행 표 그대로이고 값은 결제의 뷰에서 옮긴다. Refund 7개 필드(id, paymentAttemptId, amount, currency, status, reason, refundedAt)도 같다. PaymentSummary는 attemptCount, approvedAttemptId, attempts, refund 넷이다. Booking 20개 필드 중 1차가 고정으로 두었던 다섯(expirationReason, cancellationReason, confirmedAt, canceledAt, expiredAt)과 payment와 version이 실제 값이 된다. 새 필드는 없다(BN1).

### 2-1. 불변식과 선행조건

| 번호 | 문장 | 어디서 지키나 |
|---|---|---|
| I4 | 생성 이후 PriceSnapshot 불변. 청구액은 스냅샷 총액이다 | Booking(1차). RequestPayment 중계가 스냅샷 총액을 넘기고 결제의 금액 일치 검사가 이후 시도를 첫 청구액에 묶는다. R5 |
| I5 | 전이는 HELD에서 CONFIRMED, HELD에서 EXPIRED, CONFIRMED에서 CANCELED 셋뿐 | Booking.confirm, expire, cancel. 금지 전이는 InvalidStateTransition. 06-4 1-3 |
| I7의 예약 몫 | 확정은 승인 시도 하나로 한 번 | confirm의 종착 무해와 결제 I7. 둘째 승인은 결제가 거부하고 같은 거래 재전달은 결제가 DUPLICATE로 막는다 |
| R4 | HELD 종료 시 선점 반환이 정확히 한 번. 종료 조건은 TTL과 3회 실패 중 먼저 커밋되는 것이고 만료 커밋 전에 승인이 기록됐으면 확정이 우선 | expire의 전이 발생 반환값으로 releaseHeld ×N을 한 번만. T1의 확정 우선 분기. 08-3 결정 8 |
| 판매분 반환 한 번 | CANCELED 전이가 일어난 트랜잭션에서만 releaseSold ×N | cancel의 전이 발생 반환값 |
| 종착 무해 | CONFIRMED에 재확정, EXPIRED에 재만료, CANCELED에 재취소는 상태와 재고와 이벤트 변경 없이 전이 없음을 돌려준다 | Booking 전이 셋. 06-4 0절 |
| 잠금 순서 | Booking, Payment, 재고 N행(날짜 오름차순). 모든 경로 | 앱 서비스의 호출 순서. 08-3 결정 3, 06-4 v5 0-1 |
| 시간 경계 | 처리 시각이 expiresAt 이상이면 승인은 확정이 아니라 만료와 환불이다. 같음도 만료 | P1과 P3의 분기. shared Clock 하나로 판정 |
| 재고 총량과 하한 | I1, I1a | DailyInventory(앞 묶음). N행 적용은 한 행이라도 거절되면 전체 롤백 |
| 취소 날짜 | 서울 오늘이 checkIn보다 앞일 때만 취소 | CancelBooking 앱 서비스. 6절 P05 |
| 스냅샷 검증 | 어댑터가 옮긴 스냅샷도 I10, I11, I12, I15를 지난다 | booking PriceSnapshot 생성자(1차 K2) |

### 2-2. 이번 묶음이 만들지 않는 것

| 항목 | 누가 | 왜 |
|---|---|---|
| T2 후속 미완 결제 순찰과 정산 표식(settledAt, settle, paymentFollowUp, findFollowUpCandidates) | 7절 D-1 | 결제 계약 D-6 나가 예약 2차 몫으로 넘겼고 결제 계약 2-2절은 T2를 오늘의 MVP 밖으로 적었다. 넣을지는 사용자 결정이다 |
| 프로모션 CLOSED 상태와 종료 불가역(V19, I13) | 없음 | v1 밖. 2026-09-12 사용자 결정 |
| 검색 프로젝션과 예약 이벤트의 프로덕션 구독자 | 없음 | v1 밖. 2026-09-12 사용자 결정. BookingConfirmed와 BookingExpired와 BookingCanceled는 발행만 하고 테스트 구독자만 둔다 |
| 호스트나 운영자의 취소, 부분 취소, 예약 변경, 취소 수수료 | 없음 | 11 BOOK-04 규칙과 P05 |
| 실 PG, 부분 환불, 시도 타임아웃, 다중 객실 | 없음 | 08-3 결정 4와 11의 11-3과 11-5 |
| INTERNAL-01과 Payment와 Mock PG와 T30 | 결제 묶음 | 결제 컨텍스트 몫이다. 이 묶음은 그 결과를 이벤트와 뷰로 받기만 한다 |
| PaymentAttemptId 등 결제 값 객체의 shared 이동 | 7절 D-5 | 사용자 결정 |
| 결제 시도 목록 조회의 일괄 메서드 | 없음 | 6절 목록 조회 행. 페이지 하나에 최대 100번 attemptsOf를 부르는 것을 v1이 감수한다 |

## 3. 범위 밖과 유지할 전제

| 항목 | 왜 밖인가 |
|---|---|
| 프론트와 연결 테스트와 배포와 CI | 별도 Task. 앞 묶음과 같은 처리다 |
| 2026-09-12 사용자 결정 넷 | 프로모션 CLOSED 상태와 검색 프로젝션은 v1 밖, 패키지 재배치와 식별자 타입은 보류 유지, 프론트 Codex 블라인드 평가는 한다. 이 계약은 넷을 다시 묻지 않는다 |
| payment 패키지 | import는 하되 고치지 않는다. 예약이 결제를 아는 방향이다(06-1 R6). 결제 계약 6절 값 객체 행이 예약 2차가 payment.application과 payment.domain을 import한다고 적었다 |
| promotion 패키지 | PricingService.quote 하나만 부른다. 프로모션 리포지토리와 나머지는 읽지도 부르지도 않는다. search 패키지는 import하지 않는다 |
| inventory와 catalog 패키지 | inventory/domain의 InventoryAllocationService 하나만 고친다. 1차가 같은 파일을 고친 자리다. 나머지는 읽기만 한다 |
| shared의 기존 파일 | 고치지 않는다. ActorRegistry의 guest_001과 guest_002와 mock_001을 그대로 쓴다 |
| 설계 문서 수정 | 틀렸으면 멈추고 보고한다. document/는 동결 |
| 하네스 파일 수정 | harness/prompts, harness/tools, harness/docs, harness/project-sync, CLAUDE.md, AGENTS.md, .claude, backend/CLAUDE.md, backend/.claude, backend/build.gradle은 동결. 설정 파일은 7절 D-3의 테스트 줄 하나만 예외다(승인 시). harness/state 기록 파일의 행 추가와 이 계약 파일이 예외다 |
| Codex 평가 | 오늘의 전제 결정 3. MVP 코드가 다 붙은 뒤 한 번 |

유지할 전제. shared 패키지와 층 이름 넷과 리포지토리 두 파일 규칙과 AFTER_COMMIT 구독 규칙을 그대로 쓴다. 1차의 IdempotencyRecord와 IdempotentRequestExecutor를 그대로 쓴다. 결제의 이름은 승인된 결제 계약 2절의 것이고 실제 코드와 다르면 2단계에서 이 계약을 개정한다. T4와 T5대로 날짜는 오늘에서 세고 시각은 Clock을 고정한다. T6대로 이번 묶음이 만들 수 없는 상태(체크인이 오늘인 CONFIRMED 예약, 만료 시각이 지난 HELD)는 DB에 직접 놓고 주석에 이유를 적는다.

## 4. 입력과 적용 규칙

| 자료 | 경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 01 전체 | document/01-o2o-ddd-plan.md | sha256:17569703c90a18df | 전문 |
| 대상 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절 전부(인증과 접근 제어 45행부터, 멱등 처리 94행부터, 에러 표 112행부터), 예약과 결제 절의 PAY-01(1895행부터), PAY-02(1971행부터), BOOK-04(2017행부터), 내부 처리와 Mock 이벤트 절 전체(2147행부터 2283행. Hold와 재고 표, 상태 전이와 시간 경계, 가격과 프로모션, 결제 접수와 환불, INTERNAL-01 규칙 5와 6과 7과 8), 응답 모델 PaymentAttempt와 Refund와 PaymentSummary와 Booking과 PaymentAttemptList(2512행부터 2592행), 검증 기준 T12부터 T25와 T29, 정책 P01과 P05 |
| 대상 06-2 애그리거트 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 1절 예약 행, 3-1 I4와 I5, 4절 커밋 후 발행 문단, 5절 Booking 행, 6절 예약 CRC, 7절 |
| 대상 06-4 계약 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 0절 머리 선언(락, 종착 무해), 1-2 예약 표 전부와 결제 표, 1-3 예약 상태 표, 1-4, 2-1, 2-2 정책 카드 여덟, 2-3. v5는 브랜치 docs/task-s8-land에만 있어 git show로 읽는다. 0-1 잠금 순서 표의 예약 경로 다섯, 0-3 리스너 예외 위치, 2-3 정책 소속, 2-4 이벤트 페이로드, 2-5 T1과 T2, 2-6 조회 API |
| 대상 06-1 컨텍스트 맵 | document/06-1-o2o-context-map.md | sha256:95bc2b1079d3b739 | 2절 R5와 R6과 R7, 4절 shared 후보 |
| 대상 02 기능 목록 | document/02-o2o-feature-list.md | sha256:2a3d3ca2d8b63809 | 예약 확정과 취소와 만료 행, 3-3 종료 조건 문장, 3-7 |
| 대상 03 이벤트 스토밍 | document/03-o2o-event-storming.md | sha256:3334e5a73cb8f896 | 예약 이벤트 넷 행과 예약 커맨드 넷 행, 정책 행 |
| 확정 전제 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 1절 확정 전제와 2절 요구사항 R4와 R5 |
| 정책 결정 08-3 | harness/decisions/decisions-08-3.md | sha256:1b8580aa86c18fd8 | 표 11행 전부와 표 아래 문장. 특히 3, 5, 6, 7, 8, 10, 11의 11-1과 11-2와 11-4와 11-6. 9절 근거 |
| 구현 계획 10-6 | harness/docs/10-6-o2o-harness-implementation-plan.md | sha256:eec97503b53ffff0 | 3절 기능별 API와 검증 연결 표의 결제와 확정 행과 취소와 만료 행과 접근 범위 행 |
| 앞 묶음의 계약 하나 | harness/tasks/task-S9-booking.md | sha256:0876574192269065 | 2절 검사 순서 표(개정 반영판), 2-2절, 7절 D-1과 D-4와 D-5, 8-1 K5와 K12와 K17, 9절, 10절 개정 |
| 앞 묶음의 계약 둘 | 생성 후 기입 | 생성 후 기입 | harness/tasks/task-S9-payment.md. 결제 PR이 main에 들어간 뒤 2단계에서 경로와 해시를 채운다. 2절 대상 표와 openAttempt와 refund 검사 순서, 6절 이벤트 페이로드 행과 값 객체 행, 7절 D-1과 D-5와 D-6, 8-1 Y21 |
| 앞 묶음의 계약 셋 | harness/tasks/task-S9-promotion-search.md | sha256:6ebe329e1cfca4f4 | 2절 PricingService 접점, 7절 D-1과 D-2의 이월(V17, V18, V19), 10절 |
| 오늘의 전제 | harness/out/mvp-parallel-2026-09-11/README.md | sha256:7cbeaa454ed78eec | 1절 결정 셋, 4절 병합 순서와 접점 |
| 이 세션의 프롬프트 | harness/out/mvp-parallel-2026-09-11/prompt-B-booking.md | sha256:6aa29f27984d2c16 | 머리의 2차 범위 문장, 접점 표, 검증 표의 2차로 열 |
| 적용할 코드 양식 | harness/prompts/dev-ptcf-prompt.v3.md | sha256:4fd959abf491efe0 | Format 절과 3절 단계 번호 |
| 실제 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 | 축, 심각도, 출력 스키마 |
| 백엔드 층 규칙 | backend/.claude/rules/layers.md | sha256:cb9e4f68bdc4c43c | 3절 전부. 특히 3-3 E1과 E2 |
| 백엔드 테스트 규칙 | backend/.claude/rules/testing.md | sha256:0639a76427f449a3 | T1부터 T6 |
| 고칠 도메인 파일 | backend/src/main/java/com/o2o/booking/domain/Booking.java | sha256:8e1b4b3d367e2be9 | 전문. 전이 셋과 컬럼 다섯이 붙는다 |
| 고칠 앱 서비스 파일 | backend/src/main/java/com/o2o/booking/application/BookingApplicationService.java | sha256:d92fccfdfb835b50 | 전문. TTL 값과 requestBooking의 가격 포트 호출 자리 |
| 고칠 실행기 파일 | backend/src/main/java/com/o2o/booking/application/IdempotentRequestExecutor.java | sha256:1a2c6324b2461d2f | 전문. 읽기만. 7절 D-2가 이 실행기의 트랜잭션 경계를 전제한다 |
| 고칠 API 파일 | backend/src/main/java/com/o2o/booking/api/BookingController.java | sha256:1ec6d1744c8a1498 | 전문. PAY-01과 PAY-02와 BOOK-04가 붙는다 |
| 고칠 응답 파일 | backend/src/main/java/com/o2o/booking/api/BookingResponse.java | sha256:26fc81b2902016b1 | 전문. payment와 전이 필드가 채워진다 |
| 고칠 핸들러 파일 | backend/src/main/java/com/o2o/booking/api/BookingExceptionHandler.java | sha256:0038f1ca9b6b3711 | 전문. 오류 코드 여섯이 는다 |
| 고칠 어댑터 파일 | backend/src/main/java/com/o2o/booking/infrastructure/RateOnlyPriceQuoteAdapter.java | sha256:5af13dc13beeb1b8 | 전문. 7절 D-4가 이 파일의 자리를 정한다 |
| 고칠 재고 파일 | backend/src/main/java/com/o2o/inventory/domain/InventoryAllocationService.java | sha256:947d5cfebc654e82 | 전문. commit과 releaseHeld와 releaseSold의 N행 적용이 붙는다 |
| 부를 프로모션 파일 | backend/src/main/java/com/o2o/promotion/domain/PricingService.java | sha256:79d53b1a99e45185 | quote의 시그니처와 주석. 읽기만 |
| 부를 결제 파일 | 생성 후 기입 | 생성 후 기입 | backend/src/main/java/com/o2o/payment/application/PaymentApplicationService.java와 이벤트 둘과 값 객체 셋. 결제 PR이 main에 들어간 뒤 2단계에서 채운다. 읽기만 |
| 본보기 테스트 | backend/src/test/java/com/o2o/booking/api/BookingApiTest.java | sha256:bdb1cf2a930d1328 | K17과 K19의 두 트랜잭션 경합 방식. L23이 같은 방식이다 |
| 본보기 테스트 둘 | backend/src/test/java/com/o2o/booking/application/BookingEventTest.java | sha256:38c74fdb92ca6728 | 테스트 전용 구독자 형태. L12와 L13이 같은 방식이다 |

10-6과 앞 묶음의 계약과 오늘의 전제와 프롬프트는 이 표에만 있고 5절에는 없다. 생성 후 기입인 두 행은 결제 PR이 main에 들어가기 전에는 이 브랜치에 파일이 없어서다. 2단계의 완료 조건이 그 두 행을 채우고 fill을 다시 돌리는 것이다.

## 5. A와 B 평가 허용 입력 (HR1)

오늘의 전제 결정 3에 따라 평가는 MVP 코드가 다 붙은 뒤 한 번이다. 이 표는 그 라운드에서 이 묶음 몫으로 넘길 것이다.

| 자료 | 경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 코드 | 생성 후 기입 | 생성 후 기입 | 브랜치 feat/task-s9-booking-lifecycle의 booking 패키지 diff와 InventoryAllocationService diff와 테스트. 목록 파일은 9단계에 harness/out/task-S9-booking-lifecycle-R1/eval-target-files.md로 만든다 |
| 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 이 작업 계약 | harness/tasks/task-S9-booking-lifecycle.md | 자기 해시 없음 | 전문 |
| 실제 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 | 축, 심각도, 출력 스키마 |
| 대상이 참조하는 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절, PAY-01과 PAY-02와 BOOK-04, 내부 처리와 Mock 이벤트 절, 응답 모델, 검증 기준, 정책 P01과 P05 |
| 대상이 참조하는 06-2 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 1절, 3-1, 4절, 5절, 6절 예약, 7절 |
| 대상이 참조하는 06-4 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 0절, 1-2 예약과 결제, 1-3, 1-4, 2-1, 2-2, 2-3 |
| 대상이 참조하는 06-1 | document/06-1-o2o-context-map.md | sha256:95bc2b1079d3b739 | 2절 R5와 R6 |
| 대상이 참조하는 08-3 결정 | harness/decisions/decisions-08-3.md | sha256:1b8580aa86c18fd8 | 3, 5, 6, 7, 8, 10, 11. 코드가 11 명세 대신 이 결정을 따른 자리(6절 충돌 행)를 평가자가 위반으로 적지 않게 한다 |
| 대상이 참조하는 1차 계약 | harness/tasks/task-S9-booking.md | sha256:0876574192269065 | 2절 검사 순서 표와 7절 D-1과 D-4. 1차 코드의 근거이고 2차가 그 위에 얹힌다 |
| 백엔드 층 규칙 | backend/.claude/rules/layers.md | sha256:cb9e4f68bdc4c43c | 3-2 shared 기준, 3-3 이벤트 규칙 E1과 E2 |

이 표에 넣지 않는 것: 01 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, harness/docs/ 전체, harness/out/의 계획 문서, harness/state/ 전체, 생성 대화.

요구사항 역추적 축에 대한 지시. 입력 팩 2절의 요구사항 R1부터 R5 중 이번 묶음에 걸리는 것은 R4(HELD 종료 시 재고 반환 보장)와 R5(확정 Booking 금액 불변)의 예약 몫이다. R4는 expire의 전이 발생 반환값과 T1의 확정 우선이고 R5는 중계가 스냅샷 총액을 넘기는 것과 어댑터 교체 뒤에도 스냅샷이 동결되는 것(T13의 프로모션 절반)이다. R1부터 R3은 앞 묶음 몫이라 해당 없음으로 적고 미커버로 적지 않는다.

## 6. 정책 적용

| 정책 ID 또는 쟁점 | 적용할 값 또는 판단 | 상태와 사용자 확인 |
|---|---|---|
| P01 Hold TTL | 채택. 10분은 1차 값 그대로(o2o.booking.hold-ttl, 기본값 PT10M). 스케줄러 주기는 로컬 기본 1초이고 설정값이다(7절 D-3). 08-3 결정 11의 11-4가 적은 T1 30초는 가설이고 도메인 결정이 아니라 적혀 있어 11 P01의 1초를 기본값으로 둔다 | 확정. 계약 승인. join5201, 2026-09-13 |
| P03 통화와 달력 | 채택. 청구액은 스냅샷 총액을 shared Money로 넘긴다. Money 상한 1,000,000,000은 결제 계약 P03 행이 적은 shared의 한계이고 이 묶음도 고치지 않는다. 취소 날짜 판정은 서울 오늘이다 | 확정. 앞 묶음 |
| P05 취소 | 채택. 본인의 CONFIRMED 예약을 서울 오늘이 checkIn보다 앞일 때 전체 취소한다. 체크인 당일은 불가(409 CANCELLATION_NOT_ALLOWED). 수수료 0, 전액 환불. reason은 선택이고 빈 문자열 기본 | 확정. 계약 승인. join5201, 2026-09-13 |
| P07 로컬 행위자 | 채택. 셋 다 GUEST. 남의 예약은 404 RESOURCE_NOT_FOUND(T02) | 확정. 앞 묶음 |
| P08 원자성 | 채택. 확정과 만료와 취소가 각각 Booking과 재고 N행을 한 트랜잭션에 묶는다(06-4 0절 이탈 선언). 취소는 환불까지 같은 트랜잭션(11-2) | 확정. 08-3 수용 |
| P02, P04, P06, P09, P10, P11 | 이번 작업과 무관. P04와 P09는 프로모션 묶음이 닫았고 이 묶음은 그 계산기를 부르기만 한다 | 확인 대기 |
| 08-3 결정 3 잠금 순서 | 채택. Booking, Payment, 재고 N행. 2절 경로 표마다 적었다 | 확정. 08-3 수용 |
| 08-3 결정 5 분기는 앱 서비스 | 채택. 전이 메서드는 금지 전이를 예외로 던지고 종착 무해만 전이 없음으로 답한다. 정책 경로(P1, P3, T1)에서 CONFIRMED나 CANCELED를 만나면 앱 서비스가 로그 후 무시하고 사람 경로(BOOK-04)에서는 409다 | 확정. 08-3 수용 |
| 08-3 결정 6 리스너 예외 위치 | 채택. 구독자 둘은 @TransactionalEventListener가 붙은 booking/infrastructure 어댑터가 try와 catch로 booking/application의 REQUIRES_NEW 메서드를 부르는 두 빈 형태다. 실패는 로그와 7절 D-1의 치유로 잡고 콜백 응답에 나타나지 않는다 | 확정. 08-3 수용 |
| 08-3 결정 7과 11-2 취소 환불 동기 | 채택. 취소 트랜잭션 안에서 refund를 부르고 실패하면 전부 롤백이다. CANCELED와 미환불 승인이 함께 있는 상태는 생기지 않는다 | 확정. 08-3 수용 |
| 08-3 결정 8과 11-1 확정 우선 | 채택. T1이 잠금 뒤 승인 시도를 보면 만료 대신 확정한다. P3는 처리 시각이 expiresAt 이상이면 TTL_EXPIRED를 우선한다 | 확정. 08-3 수용 |
| 08-3 결정 10 잠금 보유 중 PG 호출 | 채택. 취소는 refund를 재고 잠금보다 먼저 둔다. Booking 잠금을 쥔 채 Mock 환불을 부르는 것은 v1 감수 | 확정. 08-3 수용 |
| 08-3 결정 11의 11-6 종착 멱등 반환 | 채택. BOOK-01의 같은 키 재전송은 예약이 EXPIRED여도 최초 HELD 응답을 재전송한다(T29). 1차 실행기가 이미 그렇게 동작하고 이 묶음은 그것을 테스트로 닫는다 | 확정. 08-3 수용 |
| 08-3 결정 1, 2, 4, 9와 11의 11-3과 11-5 | 결제 몫이거나 v1 밖. 결정 1의 정산 표식은 7절 D-1 | 확정. 08-3 수용 |
| 11 명세와 08-3의 충돌 하나. INTERNAL-01 규칙 7의 200 시점과 T23 | 결제 계약이 08-3 결정 6을 따르기로 했으므로 결제 몫은 커밋되고 예약 정책은 별도 트랜잭션이다. 그래서 정책 실패 뒤 같은 이벤트 재전달은 DUPLICATE이고 예약 정책을 다시 돌리지 않는다. T23의 재전달 시 정상 완료는 재전달이 아니라 7절 D-1의 치유로 닫힌다. 부분 결과 롤백은 REQUIRES_NEW의 롤백이 그대로 만족한다 | 확정. 계약 승인. join5201, 2026-09-13. 7절 D-1 |
| 11 명세와 06-4 v4의 충돌 둘. 취소 환불의 일관성 | v4 2-2 카드는 결과적이고 11 BOOK-04와 08-3 11-2는 동기다. 08-3을 따른다. 위 결정 7 행 | 확정. 계약 승인. join5201, 2026-09-13 |
| 11 명세와 06-4의 차이 셋. confirm의 EXPIRED 거부 | 06-4 1-2 confirm 행은 EXPIRED를 거부(승인 지연 환불 정책의 영역)라 적고 11 규칙 5는 EXPIRED면 지연 승인 환불이라 적는다. 둘은 같은 말이다. confirm은 EXPIRED에서 InvalidStateTransition을 던지고 P1은 confirm을 부르기 전에 상태로 분기해 환불 경로로 간다 | 확정. 계약 승인. join5201, 2026-09-13 |
| 이벤트 페이로드 | 결제 계약 6절의 PaymentApproved(paymentId, bookingId, paymentAttemptId, pgTransactionId, amount, currency, attemptCount, occurredAt)와 PaymentFailed(위에 failureCode)를 그대로 받는다. 이 묶음의 셋은 06-4 v5 2-4대로 bookingId와 사유(만료만)이고 occurredAt을 더한다 | 확정. 계약 승인. join5201, 2026-09-13 |
| Booking 응답의 payment 채움 | 예약 조회마다 결제의 attemptsOf를 한 번 부른다. BOOK-02 목록은 항목마다 한 번이라 최대 100번이다. 결제 패키지에 일괄 메서드를 더하지 않는다. v1 감수 | 확정. 계약 승인. join5201, 2026-09-13 |
| 오류 코드 매핑 | 결제 예외 넷을 예약 앱 서비스가 booking 예외로 감싸고 핸들러가 11의 코드로 바꾼다. AttemptInProgress는 PaymentInProgressException으로 감싸 PAYMENT_IN_PROGRESS, AttemptLimitExceeded는 PaymentAttemptsExhaustedException으로 감싸 PAYMENT_ATTEMPTS_EXHAUSTED, AlreadyApproved는 InvalidStateTransition으로 감싸 BOOKING_STATE_CONFLICT, NoApprovedAttempt와 AmountMismatch는 감싸지 않고 INTERNAL_ERROR(불변 위반). 예약 예외는 BookingExpired가 BOOKING_EXPIRED, InvalidStateTransition이 BOOKING_STATE_CONFLICT, CancellationNotAllowed가 CANCELLATION_NOT_ALLOWED. 핸들러는 booking 예외만 알고 payment 예외 클래스를 import하지 않는다 | 확정. 계약 승인. join5201, 2026-09-13 |
| version | 전이마다 1씩 오른다. HELD 0, CONFIRMED 1, CANCELED 2, EXPIRED 1. 11 BOOK-04 예시가 취소 뒤 2다 | 확정. 계약 승인. join5201, 2026-09-13 |
| 진행 방식 | 승인 뒤 한 턴에 한 단계만 하고 멈춘다. 커밋은 이유 하나씩 가른다(층 하나, 테스트 파일 하나, 결과 사본, 기록 행이 각각 따로) | 확정. join5201, 2026-09-12 |

확인 대기인 정책 여섯은 이번 코드가 의존하지 않는다. 미결 정책에 의존하는 구현을 확정하지 않는다(N4). 08-3 결정 11건은 2026-09-07 전부 수용됐으므로 확정값으로 쓴다.

## 7. 결정 5건의 안과 추천

### D-1. T2 순찰과 정산 표식을 이번에 넣나

결제 계약 D-6 나는 settledAt과 settle과 paymentFollowUp을 예약 2차가 T1과 T2를 만들 때 결제 패키지에 붙이라고 했다. 그런데 같은 계약 2-2절은 T2가 오늘의 MVP 밖이라 적었고 08-3 결정 6을 따르면 정책 실패 뒤 재전달은 DUPLICATE라 예약 정책이 다시 돌지 않는다. 그 빈자리를 무엇이 메우는지가 이 결정이다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 06-4 v5 2-5대로 넣는다. 결제 패키지에 settledAt과 settledBy와 settle과 paymentFollowUp과 findFollowUpCandidates를 더하고 예약에 T2 순찰(주기 60초, 임계 30초, 배치 100. 전부 설정값)을 만든다. P1과 P3와 T1과 취소가 분기 끝에 settle을 부른다 | 결제 패키지를 이 묶음이 고친다. 후속 조건 셋(a, b, c) 중 c는 고아가 v1에 없어 빈 조건이다. 테스트가 대여섯 는다. 오늘 읽는 쪽이 T2 하나뿐인 열 둘이 생긴다 |
| 나 | 넣지 않는다. 유실된 P1 확정(HELD인데 승인 이력 있음)은 T1이 만료 시각에 확정 우선으로 닫는다. 유실된 지연 승인 환불(EXPIRED인데 미환불 승인)은 로그로 남기고 v1은 손으로 본다. 결제 패키지는 고치지 않는다 | 유실된 확정이 최대 TTL 남은 시간만큼 늦게 닫힌다. EXPIRED의 유실 환불은 자동으로 닫히지 않는다. 08-3 결정 1의 정산 표식이 코드에 없는 채로 남고 실 PG 전환 목록에 T2를 더한다 |

추천은 나다. 11 명세의 검증 30개 어디에도 T2가 없고 T23의 재전달 완료는 08-3 결정 6 아래에서는 어느 안이든 재전달로 닫히지 않는다. 가의 열 둘은 오늘 독자가 T2뿐이라 BN5에 가깝고, 결제 계약 승인이 T2를 MVP 밖으로 적은 것과도 맞다. 나의 유실 환불은 Mock PG에서 돈이 오가지 않는 v1의 감수 범위다.

결정: 나. join5201, 2026-09-13. T2 순찰과 정산 표식은 넣지 않는다. 유실된 확정은 T1의 확정 우선이 닫고 유실된 지연 승인 환불은 로그로 남긴다.

### D-2. PAY-01이 만료 시각 지난 HELD를 먼저 만료시키는 트랜잭션

11 명세 결제 접수와 환불 절은 HELD가 만료 시각을 지났으면 먼저 만료와 재고 반환을 저장하고 BOOKING_EXPIRED로 답하라 적는다. 1차 실행기는 작업을 트랜잭션 하나로 감싸고 예외가 나면 전부 되돌린다. 409를 내려면 예외를 던져야 하고 그러면 만료도 되돌아간다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 잠금 전에 별도 트랜잭션. 중계가 Booking을 잠그기 전에 REQUIRES_NEW 메서드로 만료 시각 지난 HELD를 만료시키고(잠금과 재확인과 releaseHeld ×N) 커밋한 뒤 BookingExpired를 던진다. 잠근 뒤에 만료 시각이 지난 좁은 창은 409만 내고 만료 저장은 T1에 맡긴다 | 예외 경로에 커밋이 하나 있다. 잠금 뒤의 좁은 창(밀리초)에서는 11 문장이 T1 주기만큼 늦게 만족된다 |
| 나 | 실행기가 4xx 응답을 커밋할 수 있게 고친다. 작업이 예외 대신 4xx StoredResponse를 돌려주면 트랜잭션은 커밋하고 기록만 지운다 | 1차 실행기를 고친다. 4xx를 캐시하지 않는다는 규칙 7과 커밋을 한 곳에서 다루게 되어 실행기가 두 가지 뜻을 갖는다 |
| 다 | 만료 저장을 하지 않고 409만 낸다. 만료는 T1이 한다 | 11 문장을 어긴다. 손님이 409를 받고 GET을 치면 잠시 HELD가 보인다 |

추천은 가다. 실행기를 그대로 두고 11 문장을 정상 경로에서 만족한다. 좁은 창의 처리는 11이 GET에 잠시 HELD가 보여도 승인 가능성을 보장하지 않는다고 적은 것과 같은 결이다.

결정: 가. join5201, 2026-09-13. 잠금 전 REQUIRES_NEW 선만료. 잠금 뒤의 좁은 창은 409만 내고 만료 저장은 T1에 맡긴다.

### D-3. TTL 스케줄러의 주기와 켜고 끄기

첫 스케줄러다. 주기와 배치 크기는 설정값이고(08-3 11-4) 테스트는 긴 실제 대기를 하지 않는다(11 상태 전이와 시간 경계, testing.md T5). 테스트 컨텍스트에서 백그라운드 스레드가 같은 DB를 스캔하면 T6으로 놓은 만료 시각 지난 HELD를 테스트보다 먼저 집어 갈 수 있다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 설정 키 셋을 이 계약이 정의한다. o2o.booking.expire-scan-interval(기본 PT1S), o2o.booking.expire-batch-size(기본 100), o2o.booking.expire-scheduler.enabled(기본 true). 스케줄러 빈은 enabled가 true일 때만 뜬다. 테스트 설정 파일에 enabled=false 한 줄을 더하고 T1 테스트는 스캔 메서드를 직접 부른다. 6단계의 실제 서버에서 켜진 것을 본다 | 동결 파일인 테스트 설정에 한 줄이 붙는다(결제 계약 D-3과 같은 예외 처리). 스케줄 배선 자체는 테스트가 아니라 실제 서버 기록이 증거다 |
| 나 | 설정 키 둘(주기, 배치)만 두고 늘 켠다. T1 테스트는 스캔 메서드를 직접 부르고 백그라운드 실행은 종착 무해라 결과가 같다고 본다 | 테스트 컨텍스트 사이 create-drop 중에 스캔이 돌면 로그에 오류가 남는다. 만료 이벤트 수를 세는 테스트가 스레드 순서에 흔들릴 수 있다 |

추천은 가다. 테스트가 결정적이고 키 셋은 이 계약이 정의하므로 N6에 걸리지 않는다. 키와 API(@Scheduled의 fixedDelayString, @ConditionalOnProperty)는 4단계에서 빌드가 쓰는 jar에서 확인하고 못 찾으면 멈춘다.

결정: 가. join5201, 2026-09-13. 설정 키 셋과 테스트 설정의 enabled=false 한 줄. 4-4단계에서 붙인다.

### D-4. 요금만 합산하는 1차 어댑터의 자리

1차 D-4 가는 요금만 합산하는 어댑터를 프로덕션에 두고 2차에서 PricingService 어댑터로 바꾼다고 적었다. 바꾸면 옛 어댑터가 남는다. N2는 파일을 지우지 않고 tmp/_moved/로 옮기라 적는다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 새 어댑터 PricingPriceQuoteAdapter를 유일한 PriceQuotePort 빈으로 두고 옛 파일은 tmp/_moved/backend/로 옮긴다. git에는 삭제로 남고 로컬에는 원본이 남는다 | 옛 어댑터의 테스트(1차 K8 요금 없음 경로)를 새 어댑터로 옮겨 다시 통과시켜야 한다. PricingService도 요금이 없으면 프로모션 패키지의 RateNotConfiguredException을 던지므로 어댑터가 booking의 같은 이름 예외로 바꾸면 응답이 같다 |
| 나 | 둘 다 두고 설정 키로 고른다 | 읽는 쪽이 없는 코드가 남고 키가 하나 는다. 6단계 실제 서버가 어느 쪽인지 기록해야 한다 |

추천은 가다. 1차 D-4가 교체를 약속했고 두 어댑터를 유지할 이유가 없다.

결정: 가. join5201, 2026-09-13. 새 어댑터가 유일한 PriceQuotePort 빈. 옛 파일은 tmp/_moved/backend/로 옮긴다.

### D-5. 결제 값 객체를 shared로 올리나

layers.md 3-2는 두 번째 컨텍스트가 쓰기 시작하면 shared로 올린다고 적고, 결제 계약 6절 값 객체 행은 예약 2차가 PaymentAttemptId를 쓰기 시작하면 그때 올린다고 적었다. 그런데 06-1 R6은 예약이 결제를 아는 고객 공급자 관계이고 2026-09-12 사용자 결정은 패키지 재배치를 보류로 뒀다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 올리지 않는다. 예약이 payment.domain의 PaymentAttemptId와 MockMode와 RefundReason을 import한다. R6의 방향 그대로이고 결제 패키지를 고치지 않는다 | 3-2 문장을 한 번 어긴다. 그 이유(공급자 타입을 고객이 쓰는 것은 공유 커널이 아니다)를 layers.md에 적는 것은 하네스 세션 몫이라 이슈로 남긴다 |
| 나 | shared로 올린다. 결제 패키지의 import 셋을 고친다 | 이 묶음이 결제 패키지를 고치고 패키지 재배치 보류 결정과 부딪힌다 |

추천은 가다. 재배치는 보류이고 결제를 고치지 않는 것이 병렬의 날 뒤에도 안전하다.

결정: 가. join5201, 2026-09-13. 올리지 않는다. layers.md 3-2 문장의 예외 사유는 하네스 세션 몫으로 이슈에 남긴다.

## 8. 세로 진행 단계와 각 단계의 완료 조건

2026-09-12 사용자 지시에 따라 승인 뒤에는 한 턴에 한 단계만 하고 멈춘다. 4단계는 커밋 하나 크기(층 하나)마다 멈춘다. 단계마다 progress.md 행과 실제 시간을 남기고 커밋은 이유 하나씩 가른다. 번호는 dev-ptcf-prompt.v3.md 3절과 같다.

| 단계 | 내용 | 완료 조건 | 정지 |
|---|---|---|---|
| 1 | 이 계약과 결정 5건 승인 | 10절 승인 칸과 7절 결정 5건이 값을 갖는다 | 승인 대기 |
| 2 | 이슈와 브랜치와 접점 대조 | 결제 PR이 main에 있다. 브랜치에 origin/main을 병합한다. 이슈 하나(제목에 task-S9-booking-lifecycle), PR 초안. 결제 코드의 이름(메서드 셋, 이벤트 둘, 값 객체 셋, 예외 넷, 뷰)을 이 계약과 대조해 다르면 10절 개정 칸에 적는다. 4절의 생성 후 기입 두 행을 채우고 fill을 돌린다. 기준선 테스트 수를 작업 표 완료 기준에 적는다 | 멈춘다 |
| 3 | 해당 없음 | 백엔드 틀과 MySQL은 앞 묶음이 세웠다 | 건너뛴다 |
| 4-1 | 가격 포트 교체. PricingPriceQuoteAdapter, 옛 어댑터 이동(D-4) | requestBooking이 할인이 있는 스냅샷을 만든다. 기존 테스트 전부 통과 | 멈춘다 |
| 4-2 | 도메인. Booking의 전이 셋과 컬럼 다섯과 InvalidStateTransition과 이벤트 셋. InventoryAllocationService의 N행 적용 셋 | 컴파일과 기존 테스트 통과 | 멈춘다 |
| 4-3 | 앱 서비스. RequestPayment 중계와 예외 매핑, PaymentOutcomeService(P1, P3), ExpireDueBookings(T1 스캔과 건별 처리), CancelBooking, 만료 시각 지난 HELD의 선만료(D-2) | 컴파일과 기존 테스트 통과 | 멈춘다 |
| 4-4 | 인프라. 구독자 어댑터 둘, 스케줄러와 설정 키(D-3), Booking 잠금 조회 | 컴파일과 기존 테스트 통과. 기동이 산다 | 멈춘다 |
| 5 | 테스트. 도메인 단위와 앱 서비스 통합 | 8-1절의 L1부터 L13에 테스트가 있고 실패 케이스와 통과 케이스가 짝이다. 파일마다 커밋 하나 | 멈춘다 |
| 6 | 결제 중계 API 둘. PAY-01, PAY-02 | 경로가 명세의 요청과 응답과 상태 코드 그대로다. 8-1절의 L14부터 L18이 붙는다. bootRun으로 띄운 실제 서버의 HTTP 호출 결과(APPROVE, DECLINE 셋, DEFER 뒤 INTERNAL-01)가 http-calls.txt로 남는다 | 멈춘다 |
| 6-2 | 취소 API와 응답 채움. BOOK-04, Booking 응답의 payment, T22와 T29와 T19 | 8-1절의 L19부터 L23이 붙는다. 실제 서버 호출 기록을 같은 파일에 잇는다 | 멈춘다 |
| 7 | 해당 없음 | 프론트는 별도 Task | 고르지 않는다 |
| 8 | 해당 없음 | 프론트는 별도 Task | 고르지 않는다 |
| 9 | 백엔드 검증 표 | T14부터 T20과 T22부터 T25와 T29의 대조표가 닫히고 T12와 T13의 프로모션 절반과 T01과 T02의 결제 구간과 T23의 예약 몫까지 닫힌 것으로 적는다. 회고 표가 채워진다. 5절 평가 대상 행을 기입하고 fill을 다시 돌린다. PR 전에 origin/main을 병합하고 union 검사를 돌린다 | 사용자 완료 판단 |

모든 단계에 공통으로 걸리는 완료 조건 둘. 첫째, harness/state/progress.md에 그 단계의 행이 추가되고 실제 시간 칸이 분 단위로 채워진다. 둘째, 하네스 파일을 고치지 않는다.

### 8-1. 단계별 테스트 목록

ID의 L은 lifecycle의 L이다. 앞 묶음의 C, V, K, Y와 겹치지 않는다. 통합 테스트는 1차처럼 트랜잭션을 감싸지 않고 실제 MySQL에서 롤백과 잠금을 본다(T3). 결제 상태는 결제 앱 서비스를 실제로 불러 만들고, 이번 묶음이 만들 수 없는 상태(체크인이 오늘인 CONFIRMED, 만료 시각이 지난 HELD)는 DB에 직접 놓는다(T6).

| ID | 단계 | 무엇을 확인하나 | 근거 |
|---|---|---|---|
| L1 | 5 | Booking.confirm. HELD에서 CONFIRMED, confirmedAt, version 1, 전이 발생 true. CONFIRMED에 재확정은 변화 없이 false. EXPIRED와 CANCELED는 InvalidStateTransition | I5, 06-4 1-2 confirm, 종착 무해 |
| L2 | 5 | Booking.expire. HELD이고 expiresAt <= 지금이면 TTL_EXPIRED로 EXPIRED와 expiredAt. expiresAt 전이면 거부. PAYMENT_FAILED는 시도 수 3에서 통과하고 2에서 거부. EXPIRED에 재만료는 false. CONFIRMED와 CANCELED는 InvalidStateTransition | I5, 06-4 1-2 expire의 Pre 둘 |
| L3 | 5 | Booking.cancel. CONFIRMED에서 CANCELED, canceledAt, cancellationReason(null은 빈 문자열), version 2. CANCELED에 재취소는 false. HELD와 EXPIRED는 InvalidStateTransition | I5, 06-4 1-2 cancel |
| L4 | 5 | InventoryAllocationService의 commit ×N, releaseHeld ×N, releaseSold ×N이 N행 전부에 적용되고 한 행이 부족하면 예외로 나간다. 통합에서 그 예외가 앞 날짜의 변경까지 되돌린다 | I1, I1a, A1, 06-4 1-2 commit과 release 행 |
| L5 | 5 | 가격 포트 어댑터. 프로모션이 있는 객실의 예약이 appliedPromotion과 날짜별 discountAmount를 갖고 총액이 날짜 합과 같다. 프로모션 조건 밖이면 할인 0. 예상 총액이 할인 전 금액이면 PRICE_CHANGED(T12 프로모션 절반). 예약 뒤 프로모션을 수정해도 스냅샷과 총액이 그대로다(T13 프로모션 절반). 요금 없는 날짜는 RATE_NOT_CONFIGURED(1차 K8 이월) | 11 가격과 프로모션 절, I4, R5, 프로모션 계약 V17과 V18 |
| L6 | 5 | RequestPayment 중계. 유효한 HELD는 openAttempt를 부르고 REQUESTED 뷰를 돌려주며 청구액이 스냅샷 총액이다. EXPIRED는 BookingExpired. 만료 시각 지난 HELD는 먼저 EXPIRED와 선점 반환이 저장된 뒤 BookingExpired(D-2). CONFIRMED와 CANCELED는 InvalidStateTransition. 진행 중 시도가 있으면 booking의 PaymentInProgressException으로, 3회면 PaymentAttemptsExhaustedException으로 감싼다 | 11 결제 접수와 환불 절 처리 순서, 2절 PAY-01 표 7부터 10 |
| L7 | 5 | P1 승인 처리. HELD이고 expiresAt 전이면 CONFIRMED와 날짜마다 held 1 감소와 sold 1 증가와 BookingConfirmed 1건. expiresAt과 정확히 같으면 EXPIRED(TTL_EXPIRED)와 held 감소와 환불 1건(T18). EXPIRED면 환불 1건과 재고 변화 없음(T20). CONFIRMED와 CANCELED면 변화 없음 | 2절 P1 표, 11 시간 경계, R4 |
| L8 | 5 | P3 실패 처리. attemptCount 1과 2는 HELD와 held 유지(T16). 3이고 expiresAt 전이면 EXPIRED(PAYMENT_FAILED)와 held 1 감소가 한 번(T17). 3이고 expiresAt 이상이면 TTL_EXPIRED. 이미 EXPIRED면 변화 없음 | 2절 P3 문단, 11 규칙 6, R4 |
| L9 | 5 | T1 스캔. 만료 시각 지난 HELD가 EXPIRED(TTL_EXPIRED)와 held 감소. 지나지 않은 HELD는 그대로. 지났지만 승인 시도가 있으면 CONFIRMED와 commit(확정 우선). 이미 CONFIRMED면 스킵. 배치 크기보다 많으면 오래된 것부터 배치만큼 | 2절 T1 표, 08-3 11-1, P01 |
| L10 | 5 | 취소. CONFIRMED이고 서울 오늘이 checkIn 전이면 CANCELED, 환불 1건(BOOKING_CANCELED), 날짜마다 sold 1 감소, BookingCanceled 1건. HELD와 EXPIRED와 CANCELED는 InvalidStateTransition. checkIn이 오늘이면 CancellationNotAllowed이고 어제면 같다. 전날은 통과 | 2절 BOOK-04 표, P05, T24, T25 |
| L11 | 5 | 취소 원자성. 환불 뒤 재고 반환에서 테스트 전용 예외를 내면 CANCELED도 환불도 남지 않는다. 다시 취소하면 정상 완료 | 11 BOOK-04 규칙(부분 결과 없음), 08-3 11-2 |
| L12 | 5 | 이벤트. BookingConfirmed와 BookingExpired와 BookingCanceled가 커밋 뒤 테스트 전용 구독자에 닿고 롤백된 경로에서는 닿지 않는다 | layers.md 3-3 E1과 E2, 1차 K12 방식 |
| L13 | 5 | T23의 예약 몫. P1 서비스가 테스트 전용 훅으로 한 번 예외를 내면 예약은 HELD, 재고는 그대로, 결제는 APPROVED로 남는다(부분 결과 롤백). 같은 이벤트 재전달은 결제가 DUPLICATE로 답한다(결제 Y21과 짝). 시계를 expiresAt 뒤로 돌려 T1 스캔을 부르면 확정 우선으로 CONFIRMED와 commit이다 | 6절 충돌 하나 행, 7절 D-1 나 |
| L14 | 6 | PAY-01 202. Location, PaymentAttempt 11개 필드, status REQUESTED, amount가 스냅샷 총액, mockMode 생략은 APPROVE. 같은 키 재전송은 최초 응답과 Idempotency-Replayed: true이고 시도 수가 그대로(T14). 같은 키 다른 body는 409 IDEMPOTENCY_KEY_REUSED. 진행 중은 409 REQUEST_IN_PROGRESS(1차 K17 방식) | 11 PAY-01, 멱등 규칙, T14 |
| L15 | 6 | PAY-01 오류. 400 IDEMPOTENCY_KEY_REQUIRED, 400 INVALID_REQUEST(mockMode 값 밖, 미정의 필드), 401, 403(HOST), 404(남의 예약, 없는 예약. 같은 본문), 409 BOOKING_EXPIRED(EXPIRED. 그리고 만료 시각 지난 HELD에서 응답 뒤 GET이 EXPIRED이고 held가 반환됨), 409 BOOKING_STATE_CONFLICT(CONFIRMED), 409 PAYMENT_IN_PROGRESS(DEFER 시도 뒤 새 키. T15), 409 PAYMENT_ATTEMPTS_EXHAUSTED(DECLINE 셋 뒤 넷째. 단 셋째 실패가 만료시키므로 넷째는 BOOKING_EXPIRED가 먼저다. 그래서 한도는 앱 서비스 L6에서 보고 여기서는 순서를 확인한다) | 11 PAY-01 에러 표, T02, T15 |
| L16 | 6 | APPROVE 흐름. PAY-01 뒤 GET 상세가 CONFIRMED, confirmedAt, version 1, payment.attemptCount 1, approvedAttemptId, attempts[0].status APPROVED, refund null. 재고는 held 0과 sold 1 | 11 상태 전이 표, PaymentSummary, T14 |
| L17 | 6 | DECLINE과 DEFER 흐름. DECLINE 뒤 GET이 HELD와 attempts[0].status FAILED와 failureCode MOCK_DECLINED(T16). DECLINE 셋이면 EXPIRED와 expirationReason PAYMENT_FAILED와 held 0(T17). DEFER는 REQUESTED로 남고 INTERNAL-01로 승인을 넣으면 CONFIRMED다 | 11 결제 접수와 환불 절, T16, T17 |
| L18 | 6 | PAY-02 200. bookingId, attemptCount, items가 attemptNumber 오름차순. 시도 없으면 0과 빈 배열. 401, 403, 404 | 11 PAY-02, PaymentAttemptList |
| L19 | 6-2 | BOOK-04 200. Booking 20개 필드, status CANCELED, cancellationReason, canceledAt, confirmedAt 유지, version 2, payment.refund 7개 필드(reason BOOKING_CANCELED), attempts의 승인 시도는 APPROVED 그대로. 재고 sold 0. 같은 키 재전송은 최초 응답이고 환불과 반환이 한 번(T24). reason 생략은 빈 문자열 | 11 BOOK-04, Refund, T24 |
| L20 | 6-2 | BOOK-04 오류. 400 키, 400 reason 301자(300자 통과), 401, 403, 404, 409 BOOKING_STATE_CONFLICT(HELD, EXPIRED, 취소된 예약에 새 키), 409 CANCELLATION_NOT_ALLOWED(체크인이 서울 오늘인 CONFIRMED를 DB에 놓고. T6). 어느 경우도 수량 변경 없음(T25) | 11 BOOK-04 에러 표, T25 |
| L21 | 6-2 | 응답 채움. BOOK-02 목록의 항목과 BOOK-03 상세가 같은 PaymentSummary를 낸다. 시도 없는 예약은 0과 null과 빈 배열과 null 그대로 | PaymentSummary, 1차 K13과 K25 |
| L22 | 6-2 | T22와 T29. 승인 뒤 취소 뒤 같은 승인 이벤트를 INTERNAL-01로 다시 넣으면 200 DUPLICATE이고 CANCELED와 환불 한 번이 유지된다. 만료된 예약의 BOOK-01 성공 키를 재전송하면 최초 HELD 응답이고 GET은 EXPIRED다 | T22, T29, 08-3 11-6 |
| L23 | 6-2 | T19 경합. 테스트 트랜잭션이 Booking 행을 잠근 채 T1 건별 처리와 P1 승인 처리를 스레드 둘로 보내면 둘 다 기다리고, 풀면 CONFIRMED 하나 또는 EXPIRED와 환불 하나 중 한 경로만 남는다. 재고는 어느 쪽이든 held 0 | T19, 08-3 결정 3, 1차 K19 방식 |

## 9. 실행과 검증

| 항목 | 내용 |
|---|---|
| 작업 디렉터리 | backend/ |
| 실행 환경 | Spring Boot 4.1.1, Gradle 9.7.1 wrapper, Java 툴체인 21(JAVA_HOME은 C:/Users/user/.jdks/ms-21.0.11), MySQL 9.7.2 컨테이너 o2o-catalog-mysql. 환경변수 이름은 O2O_MYSQL_ROOT_PASSWORD, O2O_MYSQL_USER, O2O_MYSQL_PASSWORD. 값은 backend/.env이고 저장소에 없으며 모델은 그 파일을 읽지 않는다. 결제 묶음이 더한 dev 프로파일이 기본이다 |
| 실행할 명령 | docker compose -f backend/docker-compose.yml up -d로 DB를 올린다. cd backend && ./gradlew test로 테스트를 돌린다. 예상 결과는 BUILD SUCCESSFUL과 실패 0이고 실행 수는 기준선 + 이번 묶음 테스트 수다. 6단계와 6-2단계는 ./gradlew bootRun으로 띄우고 curl로 친다. 테스트와 bootRun이 같은 DB를 쓰므로 테스트 전에 bootRun을 내린다(1차 6단계 교훈) |
| 테스트 DB | 127.0.0.1:3307, o2o_catalog_test. 설정 파일 기본값 그대로. 병렬 세션이 끝난 뒤라 부딪힐 세션이 없다 |
| 데이터 초기화 허용 범위 | 테스트 프로파일의 ddl-auto가 create-drop이라 테스트 DB 스키마 전체다. 개발 DB o2o_catalog는 ddl-auto update라 booking 표에 컬럼 다섯이 더해진다. 운영 DB는 없다 |
| 빌드 출력과 로그 경로 | backend/build/test-results/test/*.xml. 사본을 harness/out/task-S9-booking-lifecycle-R1/step{단계}/에 남긴다. 실제 서버 기록은 step6/http-calls.txt와 step6-2/http-calls.txt |
| 계약 테스트 ID | 8-1절의 L1부터 L23. 검증 ID는 T14, T15, T16, T17, T18, T19, T20, T22, T23, T24, T25, T29 전부(결제 몫은 결제 묶음이 먼저 닫았다)와 T12와 T13의 프로모션 절반과 T01과 T02의 결제 구간. T21과 T26과 T30은 결제 묶음, T27과 T28은 프로모션 묶음이다 |
| A와 B 평가 범위 | eval-criteria-code.md의 축 전부. 오늘의 전제 결정 3에 따라 MVP 코드가 다 붙은 뒤 한 번이며 이 묶음 몫은 5절 표다 |
| 필수 검증을 실행하지 못했을 때 | progress.md의 실패 원인 칸에 명령과 오류 첫 줄을 남기고 결과를 halted로 적는다. 9절 계약 테스트 ID 중 미실행이 있으면 9단계 검증 표에 미실행 사유를 적고 완료 기준 첫째를 미충족으로 둔다 |

T23이 예약 몫까지 닫히는 뜻. 11의 문장(재전달 시 정상 완료)이 아니라 6절 충돌 하나 행의 해석(부분 결과 롤백, 재전달은 DUPLICATE, 치유는 D-1)으로 닫는다. 검증 표에 그 차이를 적는다.

## 10. 승인과 진행

| 항목 | 기록 |
|---|---|
| 작업 계약 승인 | 승인. join5201, 2026-09-13. 결정 5건은 추천대로 D-1 나, D-2부터 D-5 가 |
| 개정 | 없음. 2단계의 접점 대조에서 결제 코드의 이름이 다르면 여기 적는다 |
| 마지막 성공 단계 | 1단계 승인(2026-09-13). 초안은 결제 계약 승인(2026-09-12) 뒤 사용자 지시로 결제 PR 병합 전에 미리 썼다 |
| 실제 사용 시간 (1단계) | 약 48분. 초안 약 45분(문맥 압축 뒤 추정)과 승인 기록 3분 |
| 미해결 사항과 다음 작업 | 결제 PR이 main에 들어가면 2단계(이슈, origin/main 병합, 접점 대조, 생성 후 기입 3건 채움). 결제 세션은 2026-09-13 18:02에 4단계까지 끝냈고 아직 main에 없다 |
| 최종 산출물과 버전 | 작업 후 기록 |
| 실제 사용 시간 | 미측정. 단계마다 기입 |
| 최종 완료 판단 | 대기 |
