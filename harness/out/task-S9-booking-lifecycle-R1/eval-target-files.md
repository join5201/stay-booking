# task-S9-booking-lifecycle 평가 대상 코드 목록

최초 작성: 2026-09-13
최종 갱신: 2026-09-13

계약 5절 평가 대상 코드 행이 가리키는 파일이다. 계약 표 한 칸에 예순 줄을 넣을 수 없어 목록을 여기 둔다. 오늘의 전제 결정 3에 따라 평가는 MVP 코드가 다 붙은 뒤 한 번이고 이 문서는 그 라운드에 이 묶음 몫으로 넘길 목록이다. 앞 묶음 task-S9-booking과 task-S9-payment의 같은 이름 문서와 같은 골격이다.

## 1. 버전

| 항목 | 값 |
|---|---|
| 브랜치 | feat/task-s9-booking-lifecycle |
| 기준 커밋 | 892223f. 마지막 backend 커밋(6-2단계 L23). 뒤 커밋은 결과 사본과 기록과 이 문서다 |
| 앞 묶음과 갈라진 지점 | 06e25a7 (origin/main. 결제 PR 133과 병합 기록 PR 136까지 들어간 main. 2단계에서 브랜치에 병합) |
| 1차 예약 코드 | 이 브랜치의 booking 패키지 diff는 1차(PR 127, ec67433) 위에 얹힌 것만이다. 1차 코드의 근거는 task-S9-booking.md |

이 Task의 backend 커밋은 스물셋이다. 4단계 넷(4-1부터 4-4)이 열, 5단계가 넷, 6단계가 다섯, 6-2단계가 넷이다(결과 사본과 기록 커밋은 뺐다).

| 해시 | 무엇 | 단계 |
|---|---|---|
| 6b9c4e6 | 가격 포트를 프로모션 PricingService 어댑터로 교체. 옛 RateOnlyPriceQuoteAdapter는 tmp/_moved/backend/ 아래로(D-4 가) | 4-1 |
| 8b8df23 | Booking 전이 넷(confirm, expireByTtl, expireByPaymentFailure, cancel)과 isDue, 컬럼 다섯, 이벤트 셋 | 4-2 |
| fce4c6c | InventoryAllocationService에 commit과 releaseHeld와 releaseSold의 N행 적용 | 4-2 |
| 84a2d77 | InventoryCommitted와 InventoryReleased 이벤트(개정 2) | 4-3 |
| a388eab | Booking 잠금 조회 findByIdForUpdate와 due 조회 findDueIds(4-4에서 앞당김) | 4-3 |
| 2a06060 | 사람 경로의 도메인 예외 넷과 전이 예외의 사정 칸 | 4-3 |
| ac7b225 | 앱 서비스. BookingLifecycle, BookingPaymentService(결제 중계), PaymentOutcomeService(P1, P3), ExpireDueBookings와 BookingExpirationService(T1 스캔과 건별 처리, 선만료 D-2), cancelBooking | 4-3 |
| 82623de | 결제 승인과 실패를 듣는 구독자 어댑터 둘(AFTER_COMMIT, try와 catch, REQUIRES_NEW 호출) | 4-4 |
| 26ac525 | TTL 만료 스케줄러와 켜고 끄기 설정(D-3 가) | 4-4 |
| c4c38dd | 테스트 설정에서 TTL 만료 스케줄러를 끈다(D-3 가) | 4-4 |
| 72b14dc | 5단계 테스트 지원 파일 일곱. 움직이는 Clock, 커밋 뒤 이벤트 기록기, 픽스처, 실패 훅 둘 | 5 |
| bcdc493 | 도메인 전이 테스트 L1부터 L3과 가격 어댑터 테스트 L5 | 5 |
| dcbd8e1 | N행 재고 연산 테스트 L4 | 5 |
| c8dc438 | 앱 서비스 테스트 L6부터 L13 | 5 |
| 5872340 | 예약 응답의 전이 다섯 필드와 payment를 실제 값으로(계약 8절은 6-2. L16과 L17이 읽어 앞당김) | 6 |
| 0153a07 | PAY-01 결제 요청과 PAY-02 시도 목록 API. 요청 모델과 응답 모델과 BodyFingerprint | 6 |
| 7dcd26c | 결제 요청 오류 넷을 11의 409 코드로 매핑 | 6 |
| c00bd0f | 결제 요청 API 테스트 L14부터 L18 | 6 |
| 03ba667 | 1차 BookingQueryApiTest K24의 CONFIRMED 0건 전제를 필터 검사로 | 6 |
| 7c120ef | BOOK-04 예약 취소 API. CancelBookingRequest와 컨트롤러 cancel | 6-2 |
| 529f571 | CANCELLATION_NOT_ALLOWED를 409로 매핑 | 6-2 |
| ee5611c | BOOK-04 API 테스트 L19부터 L22 | 6-2 |
| 892223f | T19 만료와 승인의 잠금 경합 L23 | 6-2 |

## 2. 프로덕션 코드 41개와 옮긴 파일 1개

경로는 backend/src/main/java/com/o2o/ 아래다. 새 파일 30개와 고친 파일 11개다. origin/main 06e25a7과의 diff 그대로다.

### 2-1. booking/domain 13개 (새 11, 고침 2)

새 파일: BookingCanceled.java, BookingConfirmed.java, BookingExpired.java, BookingExpiredException.java, CancellationNotAllowedException.java, ExpirationNotDueException.java, ExpirationReason.java, FailedAttemptsBelowLimitException.java, InvalidStateTransitionException.java, PaymentAttemptsExhaustedException.java, PaymentInProgressException.java

고친 파일: Booking.java(전이 넷과 isDue, 컬럼 다섯 status와 expirationReason과 cancellationReason과 confirmedAt과 canceledAt과 expiredAt, 이벤트 발행 대상 셋. 1차 시점 해시는 계약 4절 행), BookingRepository.java(findByIdForUpdate, findDueIds)

### 2-2. booking/application 8개 (새 7, 고침 1)

새 파일: BookingExpirationService.java, BookingLifecycle.java, BookingPaymentService.java, CancelBookingCommand.java, ExpireDueBookings.java, PaymentOutcomeService.java, RequestPaymentCommand.java

고친 파일: BookingApplicationService.java(cancelBooking. 1차 시점 해시는 계약 4절 행)

### 2-3. booking/infrastructure 8개 (새 5, 고침 3)와 옮긴 파일 1개

새 파일: BookingExpireScheduler.java, BookingExpireSchedulerConfiguration.java, PaymentApprovedAdapter.java, PaymentFailedAdapter.java, PricingPriceQuoteAdapter.java

고친 파일: BookingJpaRepository.java(잠금 조회와 due 조회 질의), JpaBookingRepository.java(둘의 구현), package-info.java(설명)

옮긴 파일: RateOnlyPriceQuoteAdapter.java는 브랜치에서 지워졌고 tmp/_moved/backend/ 아래에 원본이 있다(D-4 가. 1단계 시점 해시는 계약 4절 어댑터 행). 평가 대상이 아니다.

### 2-4. booking/api 9개 (새 5, 고침 4)

새 파일: BodyFingerprint.java, CancelBookingRequest.java, PaymentAttemptListResponse.java, PaymentAttemptResponse.java, RequestPaymentRequest.java

고친 파일: BookingController.java(PAY-01, PAY-02, BOOK-04, toResponse, 조회의 payment 채움), BookingExceptionHandler.java(409 다섯. BOOKING_EXPIRED, BOOKING_STATE_CONFLICT, PAYMENT_IN_PROGRESS, PAYMENT_ATTEMPTS_EXHAUSTED, CANCELLATION_NOT_ALLOWED), BookingResponse.java(전이 다섯 필드와 payment. PaymentSummaryResponse와 RefundResponse), package-info.java(설명). 1차 시점 해시는 계약 4절 행

### 2-5. inventory/domain 3개 (새 2, 고침 1)

새 파일: InventoryCommitted.java, InventoryReleased.java(개정 2. source HELD 또는 SOLD)

고친 파일: InventoryAllocationService.java(commit과 releaseHeld와 releaseSold의 N행 적용. 1단계 시점 해시는 계약 4절 행). 재고 패키지의 다른 파일은 건드리지 않았다(계약 작업 표 변경 허용 파일)

### 2-6. 설정 파일 고친 것 1개

backend/src/test/resources/application.properties 끝에 o2o.booking.expire-scheduler.enabled=false 한 줄과 근거 주석 두 줄(D-3 가. 테스트는 T1을 runOnce로 직접 부른다). main 쪽 설정 파일은 건드리지 않았다. 설정 키 셋(o2o.booking.expire-scan-interval PT1S, expire-batch-size 100, expire-scheduler.enabled true)의 기본값은 코드에 있다.

## 3. 테스트 코드 22개

경로는 backend/src/test/java/com/o2o/ 아래다. 테스트가 든 새 파일 12개(82건)와 지원 파일 7개, 고친 1차 파일 2개, 설정 1개(2-6절)다.

### 3-1. 테스트가 든 새 파일 12개, 82건

| 파일 | 건수 | 8-1절 |
|---|---|---|
| booking/domain/BookingTransitionTest.java | 12 | L1, L2, L3 |
| inventory/domain/InventoryAllocationServiceTest.java | 5 | L4 |
| booking/infrastructure/PricingPriceQuoteAdapterTest.java | 5 | L5 |
| booking/application/BookingPaymentServiceTest.java | 9 | L6 |
| booking/application/PaymentOutcomeServiceTest.java | 10 | L7, L8 |
| booking/application/ExpireDueBookingsTest.java | 4 | L9 |
| booking/application/CancelBookingTest.java | 6 | L10, L11 |
| booking/application/BookingLifecycleEventTest.java | 4 | L12 |
| booking/application/ApprovalLossRecoveryTest.java | 2 | L13 |
| booking/api/BookingPaymentApiTest.java | 14 | L14, L15, L16, L17, L18 |
| booking/api/BookingCancelApiTest.java | 9 | L19, L20, L21, L22 |
| booking/application/BookingLockContentionTest.java | 2 | L23 |

### 3-2. 지원 파일 7개 (booking/ 바로 아래)

BookingFixtures.java(숙소와 객실과 재고와 요금과 HELD 준비, 결제 집합체 심기 seedApproved와 seedFailed), BookingLifecycleTestConfiguration.java(@TestConfiguration. 시계와 구독자와 픽스처와 훅 둘), CommittedBookingEvents.java(커밋 뒤 도착한 예약 이벤트 셋과 재고 이벤트 둘을 센다), FailingOnceInventoryAllocationService.java와 FailingOncePaymentOutcomeService.java(운영 빈의 @Primary 하위 클래스. 무장하면 한 번 예외), MutableClock.java(@Primary Clock. set과 advance와 reset), TestHookException.java

### 3-3. 고친 1차 파일 2개

booking/domain/BookingTest.java(공개 변경 메서드 목록을 빈 목록에서 전이 넷과 isDue 다섯으로. 검사 의도는 같다), booking/api/BookingQueryApiTest.java(K24의 CONFIRMED 0건 전제를 내 HELD가 CONFIRMED 목록에 없음과 전부 CONFIRMED로. 2차 테스트가 같은 손님의 CONFIRMED를 같은 DB에 남겨 전체 실행에서 깨졌다. 커밋 03ba667)

## 4. 실행 증거

| 무엇 | 어디 |
|---|---|
| 테스트 419건 실패 0(기준선 337 + 82) | harness/out/task-S9-booking-lifecycle-R1/step9/ XML 48개와 test-summary.txt |
| g1 code 12개 PASS(검사 72건, 테스트 82건) | step9/test-summary.txt |
| 실제 서버 호출 기록 | step6/http-calls.txt(PAY-01, PAY-02, 스케줄러 TTL 만료), step6-2/http-calls.txt(BOOK-04, T22, T29) |
| 단계별 요약 | step4-1부터 step6-2의 test-summary.txt |
| 검증 표와 회고 표 | harness/out/task-S9-booking-lifecycle-R1/step9-verification.md |
