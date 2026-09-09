# O2O DbC 계약과 정책 레퍼런스 (Step 7)

최초 작성: 2026-09-04
최종 갱신: 2026-09-09 v5 R1 반영 (평가 지적 8건 반영. 2-5절 R4 기산점과 상한, 0-3절 승인 기록 실패 계약, markOrphanRefunded 계약 신설, settle 후행조건, T2 복수 조건 순서, Payment 부재, R6 범위). 2026-09-08 v5 (08-3 결정 11건 반영: 정산 표식 C-1, 전역 잠금 순서 C-2, 리스너 예외 위치 C-3, 고아 승인 모델과 결제 계약 재작성 C-4, 환불 멱등키 유도 C-5, CANCELED 분기 삭제 C-6, R4 문언 C-7, expire 스킵 반환 폐기 C-8, 조회 API와 T2 잠금 C-9, PG 요청 경계 C-10, 표기와 추적성 C-11, 잠금 중 외부 호출 C-12. 2-4, 2-5, 2-6 신설)
성격: 레퍼런스. 각 절 첫 줄의 쉬운 요약과 표만 담는다. 근거와 판정 이유는 06-4-o2o-contracts-explained.md의 같은 절 번호에 있다.
자료: 07 도메인 규칙 정제와 설계 계약. 1절이 자료의 Step 1, 2절이 자료의 Step 2다.
입력: 06-2 v4, 04-5 v2, 05-3 v10, 08-3 6절 해결안 v2.1
검증: 06-5-o2o-contracts-blind-review.md (v3에 대해 수행), 08-1, 08-2, 08-3 (정책 축 3회)
후속: Step 9 구현 매핑

## 0. 머리 선언 (전 계약 공통, 2026-09-08 v5)

쉽게 말하면: 계약은 세 가지 약속이다. 부르는 쪽이 지킬 것(선행조건), 실행한 쪽이 보장할 것(후행조건), 누구든 언제나 지킬 것(불변식). 약속을 어기면 그 자리에서 실패시켜 누구 잘못인지 바로 드러낸다.

- 한 트랜잭션 한 애그리거트 규칙의 의도적 예외 2건: 예약의 세 경로(생성, 확정, 만료와 취소)가 재고 여러 행과 Booking을 함께 수정하는 것, 재고 개설이 여러 행을 만드는 것.
- 자료 07 이탈 선언: 다른 컨텍스트로 가는 자동 반응은 원칙상 비동기 결과적 일관성인데, 재고로 가는 셋(확정 시 재고 확정, 만료 시 재고 반환, 취소 시 재고 반환)은 단일 모듈의 이점으로 같은 트랜잭션에 묶는다. MSA 전환 시 되돌린다.
- 이탈 선언 추가 (C-12): PG 호출은 프로세스 내 Mock이다. 호출 타임아웃 [가설 3초]. 취소 트랜잭션과 P1의 EXPIRED 분기와 고아 환불이 잠금을 쥔 채 호출한다. v1에서 감수한다. 실 PG 전환 시 환불을 REFUND_PENDING 커밋과 T2 재시도로 바꿔 잠금 밖으로 낸다. 취소 경로는 refund를 재고 반환보다 먼저 두어 재고 행 잠금 보유 중 외부 호출이 없게 한다.
- 검증 위치: 형식 검사는 컨트롤러, 규칙 검사는 애그리거트, 무결성은 DB (1-4 표).
- 위반 책임: 선행조건 위반은 부른 쪽 잘못, 후행조건과 불변식 위반은 실행한 쪽 잘못.

### 0-1. 전역 잠금 순서 (C-2)

비관적 락으로 확정한다. 순서는 Booking, Payment, 재고 N행(날짜 오름차순)이다. 모든 경로가 이 순서를 따른다.

| 경로 | 잠금 순서 |
|---|---|
| RequestBooking | 재고 N행만 (Booking은 insert) |
| RequestPayment 중계 | Booking, Payment(openAttempt) |
| recordApproval, recordFailure | Payment만 |
| P1 결제 승인 처리 | Booking, Payment(상태 읽기, 뒤에 settle 또는 refund), 재고 N행(commit) |
| P3 결제 실패 시 만료 | Booking, Payment(settle), 재고 N행(releaseHeld) |
| T1 TTL 만료 | Booking, Payment(조회와 settle), 재고 N행 |
| T2 후속 미완 결제 순찰 | Booking, Payment, 재고 N행 |
| CancelBooking | Booking, Payment(refund), 재고 N행(releaseSold) |

08-2가 잠금 순서 교착을 막혔다고 판정한 근거는 Booking에서 재고 또는 Payment로 가는 두 갈래였는데, T1이 Payment를 먼저 잠그면서 그 근거가 무효가 됐다. 이 표가 그것을 대체한다.

### 0-2. 종착 재호출 무해와 멱등 반환

- 종착 재호출 무해: 이미 그 종착 상태면 상태 변경, 재고 연산, 이벤트 발행을 모두 생략하고 성공으로 답한다. 적용 7곳은 확정(CONFIRMED에 재확정), 만료(EXPIRED에 재만료), 취소(CANCELED에 재취소), 프로모션 종료(CLOSED에 재종료), 환불(REFUNDED에 재환불), 같은 거래번호 콜백, 그리고 settle(이미 settledAt이 있는 Payment에 재정산). 무해로 답한 애그리거트 메서드는 전이가 일어나지 않았음을 호출자에 반환해, 같은 트랜잭션의 후속 재고 연산도 함께 생략되게 한다.
- 멱등 반환 (종착 무해와 성격이 다름): RequestBooking의 같은 멱등키 재요청은 기존 예약을 그대로 반환한다. 대상이 종착이 아닌 HELD여도 성립하며, 재고 선점과 이벤트 발행을 다시 하지 않는다.

### 0-3. 리스너 예외 처리 위치와 콜백 응답 (C-3)

- 리스너 본체는 REQUIRES_NEW 트랜잭션이고 예외를 던진다. 그래야 부분 커밋(확정은 됐는데 재고 확정 N건이 실패)이 롤백된다. 예외를 잡아 로그하는 것은 트랜잭션 밖의 얇은 어댑터다.
- 구현 형태: @TransactionalEventListener가 붙은 어댑터가 try와 catch로 @Transactional(REQUIRES_NEW) 서비스 메서드를 호출한다. 같은 클래스 안의 self-invocation은 프록시를 타지 않으므로 두 빈으로 나눈다.
- REQUIRES_NEW 안에서 예외를 삼키면 프록시가 정상 종료로 보고 커밋한다. 그것이 부분 커밋의 원인이다.
- PG 콜백 HTTP 응답은 recordApproval과 recordFailure 본체의 결과와 무관하게 항상 2xx다. UnknownAttempt는 로그로 남긴다. PG 재전송으로 복구되는 경우가 없으므로(U4, 유도 복구는 T2) 5xx는 무의미하다.
- 승인 기록 트랜잭션 자체가 롤백된 경우와 기록 후 리스너가 실패한 경우를 가른다. 뒤의 것은 T2 (a)가 잡지만 앞의 것은 APPROVED 기록도 ORPHAN 기록도 남지 않아 (a)(b)(c) 어디에도 안 걸린다 (2026-09-09 R1 반영. S8-R1-A-02).
- 승인 기록이 남지 않은 실패의 계약. 콜백 응답은 그대로 2xx이되 원 콜백 페이로드(pgTransactionId, 금액, 승인 시각, 수신 시각)를 별도 수신 원장에 먼저 커밋하고 그 뒤에 recordApproval을 연다. 원장 커밋이 recordApproval 트랜잭션 밖에 있어야 롤백돼도 남는다. 재처리 입력이 그 원장이다.
- 수신 원장에 있고 대응하는 시도 기록이 없는 행은 T2의 네 번째 후보 조건이다. 처리는 (c)와 같은 고아 승인 경로를 탄다. pgTransactionId 전역 유일성이 중복 처리를 막는다.
- 리스너에서 Booking을 못 찾는 것은 불변 위반이다. openAttempt는 예약 컨텍스트가 존재하는 Booking에 대해서만 연다. 예외로 던지고 로그한다. T2 대상이 아니다.

## 1. Step 1: 도메인 규칙 정제와 DbC 계약 명세화

### 1-1. 불변식 카탈로그 (2026-09-08 v5: I6, I7, I9에 NORMAL 한정. U4 강제 수단, U5, R6 추가)

쉽게 말하면: 깨지면 안 되는 규칙 목록이다. 문장이 그대로 테스트 이름이 되고, 짧은 이름으로 다른 표에서 참조한다.

| 번호 | 이름 | 테스트 문장 | 책임 위치 |
|---|---|---|---|
| I1 | 재고 총량 | 어느 시점에 조회해도 totalCount는 soldCount + heldCount 이상이다 | DailyInventory |
| I1a | 수량 하한 | soldCount와 heldCount는 음수가 될 수 없다 | DailyInventory |
| I2 | 요금 양수 | 요금은 0보다 크다 | DailyRate |
| I3 | 기간 순서 | 체크인은 체크아웃보다 앞선다 | StayPeriod VO |
| I4 | 스냅샷 동결 | 생성된 뒤 스냅샷의 어떤 값도, expiresAt도 바뀌지 않는다 | Booking |
| I5 | 전이 폐쇄 | 허용 전이 셋 밖의 상태 변경은 일어나지 않는다 | Booking |
| I6 | 시도 상한 | NORMAL 시도는 3개를 넘지 않는다. ORPHAN 행은 세지 않는다 | Payment |
| I7 | 승인 유일 | 승인 이력(APPROVED 또는 REFUNDED)인 NORMAL 시도는 하나를 넘지 않는다. ORPHAN 행은 세지 않는다 | Payment |
| I8 | 캠페인 순서 | 캠페인 시작일은 종료일보다 늦지 않다 | Condition VO |
| I9 | 진행 유일 | REQUESTED인 NORMAL 시도는 동시에 하나를 넘지 않는다. ORPHAN 행은 세지 않는다 | Payment |
| I10 | 배분 합치 | 날짜별 할인 배분액의 합은 할인 총액과 같다 | PriceSnapshot VO |
| I11 | 행수 일치 | 스냅샷의 날짜 행 수는 박수와 같다 | PriceSnapshot VO |
| I12 | 할인 근거 | 할인이 있는 스냅샷은 프로모션 식별자를 가진다 | PriceSnapshot VO |
| I13 | 종료 불가역 | 종료된 프로모션은 다시 열리지 않는다 | Promotion |
| I14 | 인원 양수 | 최대 인원은 0보다 크다 | RoomType |
| I15 | 총액 합치 | 스냅샷 총액은 날짜별 (단가 - 할인 배분액)의 합과 같다 | PriceSnapshot VO |

컨텍스트를 넘는 규칙 (연관).

| 번호 | 이름 | 테스트 문장 | 책임 위치 |
|---|---|---|---|
| A1 | 연박 원자성 | 연박 선점은 전부 성공하거나 전무다 (R1) | InventoryAllocationService + 트랜잭션 |
| A2 | 종료 반환 | HELD가 종료되면 같은 트랜잭션에서 재고가 반환된다 (R4) | BookingApplicationService |
| A3 | 확정 이동 | 확정되면 같은 트랜잭션에서 선점이 판매로 이동한다 | BookingApplicationService |
| A4 | 승인 후속 | PaymentApproved 이후 HELD였던 예약은 CONFIRMED로 끝나거나, TTL이 먼저 이겼으면 EXPIRED + 승인분 환불로 끝난다 (결과적) | 2절의 정책 |
| A5 | 인원 검증 | 인원은 신청 시점의 maxOccupancy를 넘지 않는다 | BookingApplicationService 선행조건 |
| A6 | 요금 존재 | 숙박 기간 모든 날짜에 요금이 있어야 신청된다 | BookingApplicationService 선행조건 |

애그리거트가 지킬 수 없는 규칙 (유일성과 멱등).

| 번호 | 이름 | 규칙 | 강제 수단 |
|---|---|---|---|
| U1 | 멱등키 유일 | 같은 idempotencyKey의 예약은 하나 (R3) | DB 유니크 + 선행조건 |
| U2 | 날짜행 유일 | 같은 (roomTypeId, stayDate)의 재고 행과 요금 행은 각각 하나 | DB 유니크 |
| U3 | 결제 유일 | 같은 bookingId의 Payment는 하나 | DB 유니크 |
| U4 | 콜백 멱등 | 같은 pgTransactionId 콜백은 승인이든 실패든 무시하고 이벤트를 다시 내지 않는다. NORMAL과 ORPHAN을 가리지 않는다 | payment_attempt.pg_transaction_id 단일 컬럼 유니크 + 선행조건 |
| U5 | 시도 식별자 유일 | 같은 attemptId의 NORMAL 시도는 하나 | DB 유니크 |

### 1-2. 행동별 DbC 계약표 (2026-09-08 v5: 커맨드 23, 결제 표 교체)

쉽게 말하면: 커맨드 하나마다 넷을 적는다. 실행 전 확인할 것(Pre), 언제나 지킬 것(Invariant), 실행 후 보장할 것(Post), 어기면 어떤 예외인가.

커맨드 23개와 계약표 행의 대응. 괄호는 값 열거.

| 컨텍스트 | 커맨드에서 계약 행으로 |
|---|---|
| 카탈로그 | RegisterProperty에서 registerProperty, UpdateProperty에서 updateProperty, RegisterRoomType에서 registerRoomType, UpdateRoomType에서 updateRoomType |
| 재고와 요금 | OpenInventory에서 openInventory, AdjustInventory에서 adjust, HoldInventory에서 hold(n), CommitInventory에서 commit(n), ReleaseInventory(count, source: HELD 또는 SOLD)에서 releaseHeld(n) 또는 releaseSold(n) (source 분기는 앱 서비스), RegisterRate에서 registerRate, AdjustRate에서 adjustRate |
| 프로모션 | CreatePromotion에서 create, UpdatePromotion에서 update, ClosePromotion에서 close |
| 예약 | RequestBooking에서 RequestBooking(앱 서비스)과 Booking 생성자, ConfirmBooking에서 confirm(), CancelBooking에서 cancel(), ExpireBooking(reason: TTL_EXPIRED 또는 PAYMENT_FAILED)에서 expire(reason) |
| 결제 | RequestPayment에서 RequestPayment 중계(예약 앱 서비스)와 openAttempt(결제), RecordPaymentApproval에서 recordApproval, RecordPaymentFailure에서 recordFailure, RefundPayment(bookingId)에서 refund, SettlePayment(bookingId, settledBy)에서 settle |

SettlePayment가 23번째 커맨드다. 이벤트를 내지 않는 유일한 커맨드이며 근거는 2-2 아래 설명에 있다.

#### 카탈로그

| 행동 | Pre(선행) | Invariant | Post(후행) | 위반 시 예외 |
|---|---|---|---|---|
| registerProperty | 없음 (이름, 주소, 지역의 형식 완비는 컨트롤러 검증) | 없음 | Property 생성, PropertyRegistered 발행 | 없음 |
| updateProperty | 대상 존재 | 없음 | 서술 속성 변경, PropertyUpdated 발행 | NotFound |
| registerRoomType | Property 존재(서비스), 인원 양수 | 인원 양수(I14) | RoomType 생성, RoomTypeRegistered 발행 | PropertyNotFound, InvalidOccupancy |
| updateRoomType | 대상 존재, 인원 양수. 하향 허용(기존 예약 무영향) | 인원 양수(I14) | RoomTypeUpdated 발행 | InvalidOccupancy |

#### 재고와 요금

| 행동 | Pre(선행) | Invariant | Post(후행) | 위반 시 예외 |
|---|---|---|---|---|
| openInventory | roomType 존재(서비스), totalCount > 0 [가설], 기간 상한 이내 [가설 366일], 중복 없음(U2) | 재고 총량(I1), 수량 하한(I1a) | 날짜 행 N개 생성(판매와 선점 0), InventoryOpened 발행 | DuplicateInventory, 전체 롤백 |
| adjust | 잠금. 바꾼 totalCount가 soldCount + heldCount 이상 | 재고 총량, 수량 하한 | totalCount 변경, InventoryAdjusted 발행 | InventoryBelowOccupied |
| hold(n) | 오름차순 잠금. 행 존재, 가용 수량 n 이상. v1에서 n은 1이다 | 재고 총량, 수량 하한 | heldCount 증가, InventoryHeld 발행 | InventoryNotOpened(미개설), InventoryShortage(가용 부족). 어느 쪽이든 연박 원자성(A1)에 따라 전체 롤백 |
| commit(n) | 잠금. heldCount가 n 이상 | 재고 총량, 수량 하한 | 선점을 판매로 이동, InventoryCommitted 발행 | InsufficientHold |
| releaseHeld(n) | 잠금. 원천이 HELD, heldCount가 n 이상 | 수량 하한 | 선점 감소, InventoryReleased(HELD) 발행 | InsufficientHold |
| releaseSold(n) | 잠금. 원천이 SOLD, soldCount가 n 이상 | 수량 하한 | 판매 감소, InventoryReleased(SOLD) 발행 | InsufficientSold |
| registerRate | roomType 존재(서비스), 요금 양수, 중복 없음 | 요금 양수(I2) | DailyRate 생성, RateRegistered 발행 | DuplicateRate, InvalidRate |
| adjustRate | 대상 존재, 요금 양수 | 요금 양수 | RateAdjusted 발행. 기존 스냅샷 무영향(스냅샷 동결, I4) | InvalidRate |

#### 프로모션

| 행동 | Pre(선행) | Invariant | Post(후행) | 위반 시 예외 |
|---|---|---|---|---|
| create | 조건 완비 | 캠페인 순서(I8) | OPEN으로 생성, PromotionCreated 발행 | InvalidPeriod |
| update | 상태가 OPEN | 캠페인 순서 | PromotionUpdated 발행. 기존 스냅샷 무영향(스냅샷 동결, I4) | ClosedPromotion |
| close | 상태가 OPEN. CLOSED 재호출 무해 | 종료 불가역(I13) | CLOSED, PromotionClosed 발행. 기존 스냅샷 무영향(I4) | 없음 (재호출 무해) |
| isApplicable(region, nights, at) 조회 | 없음 (조회) | 없음 | 상태가 OPEN이고 지역과 최소 숙박일이 맞고 at(예약 생성 시각)이 캠페인 기간 안(양끝 포함)일 때만 참 | 없음 (거짓 반환) |

#### 예약

confirm, expire, cancel 세 행은 v4 그대로다. C-8이 08-2의 expire 스킵 반환 항목을 폐기했다. 분기 주체는 앱 서비스이고 애그리거트 계약은 바뀌지 않는다.

| 행동 | Pre(선행) | Invariant | Post(후행) | 위반 시 예외 |
|---|---|---|---|---|
| RequestBooking (앱 서비스) | 같은 멱등키 존재 시 멱등 반환(U1, 0-2절). 인원 검증(A5), 요금 존재(A6), 재고 가용(hold에 위임) | 연박 원자성(A1) | HELD 생성, expiresAt = 생성 시각 + ttl (이후 불변, I4), 스냅샷(DailyPrice ×N, promotionId, promotionName, 총액) 동결. 커밋 후 BookingCreated와 InventoryHeld 발행. 멱등 반환 경로에서는 재고 선점과 이벤트 발행 없이 기존 Booking을 그대로 반환 | OccupancyExceeded, RateNotFound, InventoryShortage. 실패 시 전체 롤백. 동시 진입으로 U1 유니크에 걸리면 예외를 잡아 기존 예약을 재조회해 멱등 반환한다 |
| PricingService (도메인 서비스) | 숙박 기간 전 날짜에 요금 존재(A6) | 배분 합치(I10), 행수 일치(I11), 총액 합치(I15)를 만족하는 결과 생산 | DailyPrice N행과 할인 배분 반환. 프로모션 선정은 isApplicable(예약 생성 시점, 캠페인 기간 양끝 포함). 단수는 날짜별 비례 내림 후 마지막 날짜 행 가산 | RateNotFound |
| Booking 생성자 | 기간 순서(I3), 스냅샷 검증 통과 | 기간 순서, 배분 합치, 행수 일치, 할인 근거, 총액 합치 | HELD 상태의 유효한 Booking | InvalidSnapshot |
| confirm() | 잠금. 상태가 HELD. CONFIRMED 재호출 무해 | 전이 폐쇄(I5) | 전이 경로: CONFIRMED로 전이하고 전이 발생을 반환. 같은 트랜잭션에서 확정 이동(A3), 커밋 후 BookingConfirmed 발행. 무해 경로(이미 CONFIRMED): 상태 변경, 재고 연산, 이벤트 발행 없이 성공 반환(전이 없음을 반환) | EXPIRED는 거부(승인 지연 환불의 영역), CANCELED는 InvalidStateTransition |
| expire(reason) | 잠금. 상태가 HELD. 원인이 TTL이면 expiresAt 경과 추가 검사. 원인이 결제 실패면 이벤트 탑재 시도 수 3 이상 재확인. EXPIRED 재호출 무해 | 전이 폐쇄 | 전이 경로: EXPIRED로 전이하고 전이 발생을 반환. 같은 트랜잭션에서 종료 반환(A2), 커밋 후 BookingExpired 발행. 무해 경로(이미 EXPIRED): 상태 변경, 재고 연산, 이벤트 발행 없이 성공 반환 | CONFIRMED와 CANCELED는 InvalidStateTransition |
| cancel() | 잠금. 상태가 CONFIRMED. CANCELED 재호출 무해 | 전이 폐쇄 | 전이 경로: CANCELED로 전이하고 전이 발생을 반환. 같은 트랜잭션에서 refund를 먼저 부르고 그다음 판매분 반환, 커밋 후 BookingCanceled 발행. 무해 경로(이미 CANCELED): 상태 변경, 재고 연산, 이벤트 발행 없이 성공 반환 | HELD와 EXPIRED는 InvalidStateTransition |
| RequestPayment 중계 (앱 서비스) | Booking 잠금. 상태가 HELD | 없음 | 스냅샷 총액을 첨부해 결제 컨텍스트 호출 | NotHeldBooking |

#### 결제 (2026-09-08 v5: C-4로 전면 교체)

Payment 애그리거트는 PG를 호출하지 않는다. 외부 호출은 결제 앱 서비스가 한다.

| 행동 | Pre(선행) | Invariant | Post(후행) | 위반 시 예외 |
|---|---|---|---|---|
| openAttempt | 잠금. 진행 중 NORMAL 시도 없음(I9), NORMAL 시도 3 미만(I6), NORMAL 승인 이력 없음(I7), 전달 총액과 청구액 일치. 첫 요청이면 Payment 생성(U3) | 시도 상한, 승인 유일, 진행 유일 | attemptId를 발급하고 REQUESTED NORMAL 시도 추가, PaymentRequested 발행. 앱 서비스가 같은 트랜잭션 안에서 attemptId를 멱등키로 Mock PG에 요청을 보낸다(C-10 채택안 가) | AttemptInProgress, AttemptLimitExceeded, AlreadyApproved, AmountMismatch |
| recordApproval(attemptId, pgTransactionId, amount) | Payment 잠금. 같은 pgTransactionId가 어느 행에든 있으면 무해 무시(U4, 전이 없음 반환). attemptId의 NORMAL 시도 존재 | 승인 유일(I7, NORMAL만) | (1) 시도가 REQUESTED이고 NORMAL 승인 이력이 없고 amount가 청구액과 같으면 APPROVED로 바꾸고 pgTransactionId와 approvedAt을 저장하고 settledAt을 null로 되돌리고 PaymentApproved를 발행하고 전이 발생을 반환한다. (2) 그 외(시도가 종착이거나 이미 NORMAL 승인 이력이 있거나 금액 불일치)이면 ORPHAN 행을 REFUND_PENDING으로 추가하고 PaymentApproved를 발행하지 않고 고아 환불 필요를 반환한다. 앱 서비스가 같은 트랜잭션에서 pgTransactionId를 멱등키로 PG 환불을 호출하고, 성공이면 REFUNDED로 바꾸고 PaymentRefunded(kind ORPHAN)를 발행하고, 실패면 로그 후 REFUND_PENDING으로 커밋한다 | UnknownAttempt (attemptId 없음). AlreadyApproved, NoRequestedAttempt, AttemptAlreadyClosed는 폐기 |
| recordFailure(attemptId, pgTransactionId) | Payment 잠금. 같은 pgTransactionId 무해 무시. attemptId의 NORMAL 시도 존재 | 시도 상한(I6, NORMAL만) | 시도가 REQUESTED면 FAILED로 바꾸고 pgTransactionId와 failedAt을 저장한다. NORMAL FAILED 수가 3이 되면 settledAt을 null로 되돌리고 PaymentFailed를 발행한다(시도 수 탑재). 전이 발생을 반환한다. 종착이면 무해 무시(전이 없음 반환) | UnknownAttempt |
| settle(settledBy) | Payment 잠금 | 없음 | settledAt이 null이면 settledAt과 settledBy를 함께 기록한다. 이미 있으면 무해(전이 없음 반환)이고 기존 settledBy를 덮지 않는다. 관측 대상은 최초 정산 주체다. 종착 무해 7곳째다 (2026-09-09 R1 반영. S8-R1-A-05) | 없음 |
| refund(bookingId) | Payment 잠금. NORMAL APPROVED 존재. REFUNDED 재호출 무해 | 승인 유일 | APPROVED에서 REFUNDED로 바꾸고 PaymentRefunded(kind NORMAL)를 발행한다. PG 호출은 앱 서비스가 그 시도의 attemptId를 멱등키로 수행한다 | NoApprovedAttempt |

반환 규약은 전이 발생과 전이 없음 둘이다. 고아 분기의 고아 환불 필요는 전이 발생의 한 종류로 값을 붙인다. 전이 발생 APPROVED, 전이 발생 ORPHAN_PENDING. 값이 셋으로 늘지만 규약의 종류는 둘이다.

정산 표식의 정의와 규칙은 2-5절에 있다.

### 1-3. 상태전이와 가드 표 (2026-09-08 v5: 결제 시도 표에 kind 열과 ORPHAN 행)

쉽게 말하면: 상태가 오갈 수 있는 길만 적고, 나머지 길은 전부 막혀 있다고 명시한다.

#### 예약 상태 (BookingStatus)

| From | Trigger | Guard | To | Side-Effect |
|---|---|---|---|---|
| HELD | 결제 승인 처리 (P1) | 잠금 후 상태가 HELD | CONFIRMED | BookingConfirmed 발행 + 확정 이동 |
| HELD | TTL 만료 (T1) | 잠금 후 상태가 HELD, expiresAt 경과, 미정산 승인 없음 | EXPIRED | BookingExpired 발행 + 선점 반환 |
| HELD | 결제 실패 시 만료 (P3) | 잠금 후 상태가 HELD, 이벤트 탑재 시도 수 3 이상 | EXPIRED | BookingExpired 발행 + 선점 반환 |
| CONFIRMED | 이용자의 취소 | 잠금 후 상태가 CONFIRMED | CANCELED | BookingCanceled 발행 + 환불 후 판매분 반환 |

금지 전이: CONFIRMED에서 EXPIRED로, HELD에서 CANCELED로, 그리고 EXPIRED와 CANCELED에서 나가는 모든 전이. 재호출 무해: 각 종착 상태로의 같은 요청 반복.

#### 결제 시도 상태 (PaymentAttempt)

kind는 NORMAL과 ORPHAN 둘이다. NORMAL은 순번을 갖고 I6, I7, I9의 셈 대상이다. ORPHAN은 순번이 없고 셈 대상이 아니며 sourceAttemptId, pgTransactionId, amount, receivedAt을 갖는다.

| kind | From | Trigger | Guard | To | Side-Effect |
|---|---|---|---|---|---|
| NORMAL | (생성) | 결제 요청 | 진행 유일, 시도 상한, 승인 유일 | REQUESTED | PaymentRequested 발행 |
| NORMAL | REQUESTED | 승인 콜백 | 거래번호 신규, 금액 일치 | APPROVED | PaymentApproved 발행, settledAt 되돌림 |
| NORMAL | REQUESTED | 실패 콜백 | 거래번호 신규 | FAILED | 3회째면 PaymentFailed 발행, settledAt 되돌림 |
| NORMAL | APPROVED | 환불 | 없음 | REFUNDED | PaymentRefunded(NORMAL) 발행 |
| ORPHAN | (생성) | 승인 콜백의 고아 분기 | 거래번호 신규 | REFUND_PENDING | 없음. 앱 서비스가 PG 환불 시도 |
| ORPHAN | REFUND_PENDING | 환불 성공 | 없음 | REFUNDED | PaymentRefunded(ORPHAN) 발행 |

금지 전이: 모든 역행. FAILED는 종착이고 재시도는 새 NORMAL 시도 생성이다. Payment 자체는 저장 상태 없이 시도 목록에서 계산한다.

알려진 갇힘: REQUESTED에서 나가는 길은 콜백 둘뿐이라, 콜백이 영영 오지 않으면 NORMAL 시도가 REQUESTED에 머물고 진행 유일(I9)이 새 시도까지 막는다. 결제 시도 타임아웃은 v1에서 도입하지 않는다. TTL 만료가 최종 안전망이다. REQUESTED인 채로 TTL에 도달하면 T1이 예약을 만료시킨다. 실 PG 전환 시 타임아웃 스케줄러를 이월 목록에서 꺼낸다.

#### 프로모션 상태

| From | Trigger | Guard | To | Side-Effect |
|---|---|---|---|---|
| OPEN | 운영자의 종료 | 없음 | CLOSED | PromotionClosed 발행 |

금지 전이: CLOSED에서 OPEN으로 (종료 불가역).

### 1-4. 검증 책임 위치 표

쉽게 말하면: 어떤 검사를 누가 하는가.

| 검증 종류 | 위치 | 이 프로젝트의 예 |
|---|---|---|
| 형식 검증 (널, 타입, 포맷) | 컨트롤러 | 요청 DTO 검증 |
| 규칙 검증 (선행조건, 불변식, 전이) | 애그리거트와 VO | 1-1의 I 계열, 1-3의 전이표 |
| 컨텍스트를 넘는 선행조건 | 앱 서비스 | 인원 검증, 요금 존재, 결제 중계의 HELD 검사 |
| 상태에 따른 분기 선택 | 앱 서비스 | T1과 P3가 CONFIRMED를 만나면 expire를 부르지 않는다 (C-8) |
| 외부 호출 | 앱 서비스 | PG 요청, PG 환불. 애그리거트는 부르지 않는다 (C-4) |
| 원자성 조율 | 도메인 서비스 | 연박 원자성 (InventoryAllocationService) |
| 유일성과 무결성 | DB | 1-1의 U 계열 유니크 제약 |

## 2. Step 2: 정책 추가, 자동 규칙 발견

### 2-1. 이벤트에서 커맨드로의 트리거 맵핑 (2026-09-08 v5: 취소 행과 시간 행)

쉽게 말하면: 어떤 사건이 사람 손 없이 다음 행동을 부르는가. 22개 이벤트 중 둘과 시간 트리거 둘이다.

| 이벤트 또는 트리거 | 자동으로 유발하는 행동 |
|---|---|
| PaymentApproved (결제 승인됨) | Booking 상태로 분기. HELD면 확정, EXPIRED면 환불, CONFIRMED나 CANCELED면 정산 표식만 |
| PaymentFailed (결제 실패됨) | 3회째면 예약 만료 |
| BookingConfirmed (예약 확정됨) | 없음. 선점 확정은 확정 트랜잭션 내부 호출이다 |
| BookingExpired (예약 만료됨) | 없음. 선점 반환은 만료 트랜잭션 내부 호출이다 |
| BookingCanceled (예약 취소됨) | 없음. 환불과 판매분 반환은 취소 트랜잭션 내부 호출이다 |
| 시간 T1 (기한 지난 HELD 폴링) | 미정산 승인이 있으면 확정, 없으면 만료 |
| 시간 T2 (후속 미완 결제 폴링) | 2-5의 조회 조건별 분기 |

예약발 세 이벤트는 커밋 후 발행되는 기록이며 구독 대상이 아니다. 대응하는 재고 행동은 같은 트랜잭션 안의 호출이다 (2-2, 2-3).

### 2-2. 정책 카드 (2026-09-08 v5: 08-3 6절 v2.1 목록으로 교체. 정책 8)

쉽게 말하면: 자동 반응 규칙 카드다. 언제, 어떤 조건이면, 무엇을 하고, 얼마나 빨리 맞추며, 같은 사건이 두 번 와도 왜 안전한가. 가드는 전부 확정이고, 모든 정책은 중간 상태를 보관하지 않는다.

| 정책 | 트리거 | 가드 | 행동 | 일관성 | 실패 시 |
|---|---|---|---|---|---|
| P1 결제 승인 처리 | PaymentApproved | Booking 잠금, Payment 잠금, 상태 읽기 | HELD면 confirmHeld(APPROVAL_HANDLER). EXPIRED면 refund와 settle. CONFIRMED나 CANCELED면 settle만 | 결과적. 리스너는 REQUIRES_NEW이고 결제 호출은 같은 트랜잭션 | 예외는 트랜잭션 밖에서 로그. 유실과 예외는 T2가 잡는다 |
| P2 확정 시 재고 확정 | 확정 트랜잭션 내부 호출 | 없음 | commit(1) ×N | 강한 | 해당 없음 |
| P3 결제 실패 시 만료 | PaymentFailed | 시도 수 3 이상(이벤트 탑재값). Booking 잠금, Payment 잠금, 상태 읽기 | HELD면 expire(PAYMENT_FAILED)와 releaseHeld ×N와 settle. EXPIRED면 settle만. CONFIRMED나 CANCELED면 WARN 로그 후 settle | 결과적 | 동일 |
| P4 만료 시 재고 반환 | 만료 트랜잭션 내부 호출 | 없음 | releaseHeld(1) ×N | 강한 | 해당 없음 |
| P5 취소 시 환불 | 취소 트랜잭션 내부 호출 | 없음 | refund(bookingId)를 attemptId 멱등키로. 그다음 settle(CANCEL). 재고 반환보다 먼저 | 강한. 이탈 선언 | 롤백하고 취소 실패. 재시도는 같은 멱등키 |
| P6 취소 시 재고 반환 | 취소 트랜잭션 내부 호출 | 없음 | releaseSold(1) ×N | 강한 | 해당 없음 |
| T1 TTL 만료 | 시간 | HELD이고 기한 경과. Booking 잠금, Payment 잠금, paymentFollowUp 조회 | 미정산 APPROVED가 있으면 confirmHeld(T1). 없으면 expire(TTL_EXPIRED)와 releaseHeld ×N와 settle(T1) | 내부 트리거. 결제 조회와 SettlePayment는 동기 같은 트랜잭션 (이탈 선언) | 다음 주기 재후보 |
| T2 후속 미완 결제 순찰 | 시간 | 2-5의 조회 조건 (a)(b)(c) | 2-5의 건별 처리. 모든 분기가 settle로 끝난다 | 내부 트리거. 결제 조회와 SettlePayment는 동기 같은 트랜잭션 (이탈 선언) | 다음 주기 재후보 |

멱등 전략을 셋으로 나눈다.

| 층 | 무엇을 막는가 | 수단 |
|---|---|---|
| 발행 중복 | 같은 pgTransactionId 콜백이 이벤트를 다시 내는 것 | U4 단일 컬럼 유니크 |
| 배달 중복 | 같은 이벤트가 두 번 배달되는 것 | 종착 재호출 무해 7곳. settle 포함 |
| 유실 | 이벤트가 배달되지 않는 것 | T2 순찰. 유일한 보정 수단이다 |

SettlePayment는 이벤트를 내지 않는다. 예약 컨텍스트의 처리 완료 표식이지 도메인 사건이 아니기 때문이다. 이벤트 없는 커맨드는 이것이 처음이다.

P1이 CONFIRMED나 CANCELED에 도착한 승인에 침묵하지 않고 정산 표식을 찍는 것이 v4와 다른 점이다. 침묵하면 T2가 그 Payment를 영원히 후보로 다시 집는다.

### 2-3. 정책 분류와 소속 (2026-09-08 v5: 이탈 선언에 T1과 T2, 반응 불필요 20개)

쉽게 말하면: 즉시 맞출 것과 나중에 맞춰도 되는 것을 가른다.

| 분류 | 해당 정책 | 처리 |
|---|---|---|
| 내부 정책 (같은 컨텍스트 트리거) | T1, T2 | 동기, 강한 일관성 |
| 외부 정책이지만 강한 일관성으로 앞당김 (이탈 선언) | P2 확정 시 재고 확정, P4 만료 시 재고 반환, P6 취소 시 재고 반환, P5 취소 시 환불, T1과 T2의 결제 컨텍스트 조회와 SettlePayment | 동기, 같은 트랜잭션. 앱 서비스의 호출로 구현 (이벤트 구독 아님) |
| 외부 정책 (자료 원칙대로) | P1 결제 승인 처리, P3 결제 실패 시 만료 | 비동기, 결과적 일관성. 커밋 후 발행된 이벤트를 별도 트랜잭션에서 처리 |
| 읽기 모델 갱신 (Projection) | 카탈로그 4, 재고와 요금 4, 프로모션 3 이벤트가 검색을 갱신 | 지연 허용. 정책 카드 없음 |

자동 반응이 필요한 이벤트는 둘이다. PaymentApproved와 PaymentFailed. 나머지 20개는 반응이 필요 없다. BookingConfirmed, BookingExpired, BookingCanceled가 이 20개에 들어가는 이유는 후속이 같은 트랜잭션 내부 호출이기 때문이다.

정책의 소속(구독자)은 전부 예약 컨텍스트다. 결제는 예약을 모른다는 전제 때문에, 결제 이벤트를 받아 예약 상태를 보는 코드는 예약 쪽에만 있을 수 있다.

### 2-4. 이벤트 페이로드 (2026-09-08 v5 신설)

| 이벤트 | 페이로드 | 주의 |
|---|---|---|
| BookingCreated | bookingId, userId, roomTypeId, 날짜 목록, 총액, expiresAt | n = 1이므로 날짜별 수량이 아니라 날짜 목록이다 |
| InventoryHeld | roomTypeId, 날짜 목록 | 같음 |
| InventoryCommitted | roomTypeId, 날짜 목록 | 같음 |
| InventoryReleased | roomTypeId, 날짜 목록, source(HELD 또는 SOLD) | 같음 |
| PaymentRequested | paymentId, bookingId, attemptId, 청구액 | |
| PaymentApproved | paymentId, bookingId, attemptId, pgTransactionId, attemptCount | attemptId와 pgTransactionId와 attemptCount는 P1이 분기에 쓰지 않는다. 로그와 추적용이다. P1의 분기 입력은 Booking 상태다 |
| PaymentFailed | paymentId, bookingId, attemptId, attemptCount | attemptCount는 P3의 가드가 쓴다 |
| PaymentRefunded | paymentId, bookingId, kind(NORMAL 또는 ORPHAN), 환불액 | |
| BookingConfirmed, BookingExpired, BookingCanceled | bookingId, 사유(만료만) | 구독자 없음 |

### 2-5. 정산 표식과 스케줄러 명세 (2026-09-08 v5 신설. C-1)

#### 정의

settledAt이 있는 Payment는 예약 컨텍스트가 이 Payment를 보고 할 후속(확정, 만료, 환불)이 현재 없다는 뜻이다. 단위는 Payment다.

settledBy(APPROVAL_HANDLER, FAILURE_HANDLER, T1, T2, CANCEL)는 관측용이다. 보정 비율을 재는 데 쓰고 분기에는 쓰지 않는다.

#### 찍는 규칙 (예외 없음)

예약 컨텍스트가 Booking 잠금 아래에서 Payment 상태를 읽고 분기를 끝낸 트랜잭션은 그 결과가 전이든 무해든 스킵이든 환불이든 SettlePayment로 끝난다.

해당 트랜잭션은 P1 전 분기, P3 전 분기, T1 전 분기, T2 전 분기, 취소 트랜잭션(환불 뒤)이다.

#### 되돌리는 규칙

결제 컨텍스트가 예약 후속을 요구하는 새 사실을 기록하면 settledAt을 null로 되돌린다. 해당 사실은 둘이다. recordApproval이 REQUESTED 시도를 APPROVED로 바꿀 때, recordFailure가 3회째 실패를 기록할 때.

고아 승인은 예약 후속이 아니라 결제 내부 후속이므로 되돌리지 않는다. 아래 조회 조건 (c)가 settledAt과 독립인 이유다.

#### T2 후보 조회

잠금 없이 조회한다. 배치 크기 [가설 100], candidateSince 오름차순 정렬.

| 조건 | 내용 | candidateSince |
|---|---|---|
| (a) | settledAt이 null이고 status가 APPROVED(REFUNDED 아님)인 NORMAL 시도가 있고 approvedAt이 now에서 임계를 뺀 시각 이하 | approvedAt |
| (b) | settledAt이 null이고 NORMAL FAILED 시도 수가 3 이상이고 승인 이력(APPROVED 또는 REFUNDED)인 NORMAL 시도가 없고 마지막 failedAt이 now에서 임계를 뺀 시각 이하 | 마지막 failedAt |
| (c) | kind가 ORPHAN이고 status가 REFUND_PENDING인 행이 있고 receivedAt이 now에서 임계를 뺀 시각 이하. settledAt 무관 | receivedAt |

(a) 또는 (b) 또는 (c)다. 임계 [가설 30초].

#### T2 건별 처리

건별 트랜잭션이다. 잠금 순서는 Booking, Payment, 조건 재검사, 분기다.

| 상태와 조건 | 처리 |
|---|---|
| HELD와 (a) | confirm, commit ×N, settle(T2) |
| HELD와 (b) | expire(PAYMENT_FAILED), releaseHeld ×N, settle(T2) |
| EXPIRED와 (a) | refund, settle(T2) |
| CONFIRMED와 (a) | settle(T2). P1 유실의 자가 치유다 |
| CONFIRMED, EXPIRED, CANCELED와 (b) | settle(T2). 후속 없음 |
| (c) | 고아 환불 재시도. 성공이면 REFUNDED, 실패면 REFUND_PENDING 유지하고 다음 주기. settle과 무관 |
| (a)와 (c)가 함께 성립 | (a)를 먼저 처리하고 settle한 뒤 같은 트랜잭션에서 (c)를 처리한다. settle은 예약 후속이 없다는 뜻이고 고아 환불은 결제 내부 후속이라 서로를 지우지 않는다 (2026-09-09 R1 반영. S8-R1-A-08, S8-R1-B-02) |
| (b)와 (c)가 함께 성립 | 같음. (b)를 먼저 처리하고 settle한 뒤 (c)를 처리한다 |
| Booking에 대응하는 Payment가 없음 | T1과 T2의 settle 호출을 생략한다. Payment 잠금 대상이 없으므로 settle은 무해가 아니라 호출 자체가 성립하지 않는다. Booking 만료와 재고 반환은 Payment 조회 결과 exists=false를 확인한 그 트랜잭션에서 커밋한다 (2026-09-09 R1 반영. S8-R1-A-09) |
| Payment에 대응하는 Booking이 없음 | 불변 위반(U3, openAttempt는 예약이 연다). 로그하고 스킵한다. settle하지 않는다. 운영자가 볼 수 있게 남긴다 |

찍는 규칙의 예외 없음은 예약 컨텍스트 분기를 끝낸 트랜잭션에 대한 것이다. (c)만 성립한 건은 예약 후속 분기를 돌지 않으므로 그 규칙의 대상이 아니고 settle하지 않는다. 두 규칙은 어긋나지 않고 적용 대상이 다르다 (2026-09-09 R1 반영. S8-R1-A-08, S8-R1-B-02).

#### 좀비 판정

(a)와 (b)는 처리 뒤 반드시 settle되므로 후보에서 빠진다. (c)는 실제 미환불이 있는 동안만 후보다. 정상 확정분은 P1이 settle하므로 후보가 아니다. 후보 집합은 후속이 남은 건과 미환불 고아로 유한하다.

#### R4 종료 보장의 기산점과 상한 (2026-09-09 R1 반영 신설. S8-R1-A-01, S8-R1-B-01)

R4는 리스너 유실 시에도 늦어도 T2 주기 안에 종료된다고 적었는데 무엇부터 재는지가 없었다. 기산점이 없으면 같은 문장이 두 가지로 읽히고 그중 하나는 이 명세로 만족되지 않는다.

기산점을 candidateSince로 고정한다. candidateSince는 위 조회 표가 조건별로 정한 시각이며 그 시각에 그 Payment가 T2 후보 자격을 얻는다.

| 재는 구간 | 상한 | 이 명세가 만족하나 |
|---|---|---|
| candidateSince부터 종료 커밋까지 | T2 주기 1회분에 배치 처리 시간을 더한 값 | 만족한다. 다음 순찰이 반드시 그 건을 집는다 |
| 승인 또는 실패 기록 시각부터 종료 커밋까지 | T2 임계에 위 상한을 더한 값 | 만족한다 |

두 번째 구간이 R4가 말하는 것이라면 상한은 주기 하나가 아니라 임계에 주기를 더한 값이다. 그것은 요구사항 문언을 바꾸는 일이라 이 반영에서 정하지 않는다. 4절에 미결로 남긴다.

배치 적체는 상한에 포함된다. 한 주기의 후보가 배치 크기를 넘으면 candidateSince 오름차순이므로 오래된 것부터 처리되고 나머지는 다음 주기로 밀린다. 밀린 건의 상한은 밀린 주기 수만큼 늘어난다. 배치 크기를 후보 유입률보다 크게 잡는 것이 설정 조건이다.

#### 설정값 제약

T2 임계 < T2 주기 < TTL. T1 주기는 정확성과 무관하다. 확정 우선이므로 T1과 승인의 결과가 폴링 위상에 좌우되지 않는다. 정렬 키는 candidateSince다.

[가설] T1 30초, T2 60초, 임계 30초, 배치 100. 설정값이고 도메인 결정이 아니다.

#### markOrphanRefunded 계약 (2026-09-09 R1 반영 신설. S8-R1-A-06)

06-2 6절의 PaymentApplicationService가 고아 환불 성공 뒤에 부르는 변경 메서드다. 상태전이표만으로는 어느 ORPHAN 행을 고르는지와 재호출 무해 조건을 확인할 수 없어 계약으로 적는다.

| 항목 | 내용 |
|---|---|
| 선행조건 | Payment 잠금. pgTransactionId로 행을 특정한다. 그 행의 kind가 ORPHAN이고 status가 REFUND_PENDING 또는 REFUNDED |
| 불변식 | pgTransactionId 전역 유일(U4). 한 pgTransactionId에 대응하는 시도 행은 하나다 |
| 후행조건 | status를 REFUNDED로 바꾸고 환불 시각을 기록한다. 이미 REFUNDED면 무해이고 전이 없음을 반환하며 이벤트를 발행하지 않는다. 종착 무해 8곳째다 |
| 위반 예외 | 행이 없으면 UnknownAttempt. kind가 NORMAL이면 KindMismatch. 둘 다 로그로 남기고 콜백 응답은 2xx다 |

행 선택 기준이 pgTransactionId인 이유는 그것이 전역 유일이기 때문이다. attemptId로 고르면 같은 Payment에 ORPHAN이 여럿일 때 어느 것인지 정해지지 않는다.

### 2-6. 예약에서 결제로의 조회 API 계약 (2026-09-08 v5 신설. C-9)

결제 컨텍스트가 제공하고 예약 컨텍스트가 호출한다. 방향은 예약에서 결제 한 방향이며 06-1 R6의 Customer-Supplier와 일치한다.

| API | 반환 | 잠금 |
|---|---|---|
| paymentFollowUp(bookingId) | PaymentFollowUpView { exists, settledAt, approvedAttempt(attemptId, approvedAt) 또는 없음, normalFailedCount, lastFailedAt, orphanPendingCount } | T1, T2, P1은 Payment 잠금을 함께 요청한다. 순서는 0-1절 |
| findFollowUpCandidates(threshold, limit) | PaymentId와 bookingId 목록 | 잠금 없음 |

Payment가 없으면 exists는 false이고 나머지는 비어 있다. T1은 이것을 후속 없음으로 읽고 만료 경로로 간다.

확정 경로는 T1과 P1이 공유한다. 공통 메서드는 confirmHeld(bookingId, settledBy)이고 내용은 confirm, commit ×N, settle이다. P1의 4분기와 T1의 2분기는 각자 두고 확정 분기에서만 이 메서드를 부른다. settledBy는 호출자가 넘긴다.

T2 건별 처리는 Booking 잠금, Payment 잠금, 조건 재검사, 분기다. 다중 인스턴스는 이 잠금과 settle 무해와 REFUNDED 무해로 안전하다.

## 3. 확정, 가설, 이월 (2026-09-08 v5)

이번 단계 확정.
- 비관적 락과 전역 잠금 순서(0-1절). 갭 락만 Step 9 테스트.
- 미개설 날짜 선점 실패. TTL 만료의 기한 재검사. 종착 재호출 무해 7곳과 멱등 반환 1곳(0-2절). 무해 경로는 재고 연산과 이벤트 발행까지 생략.
- 할인 단수는 마지막 날짜 행 가산. 스냅샷 총액 저장(I15). 최대 인원 하향 허용. 환불은 APPROVED에서 REFUNDED로의 전이. Money 공유 커널 적합.
- 정책 8개의 가드 전부, 일관성 유형 전부, 소속(예약), 전 정책 Stateless.
- 정산 표식의 정의와 규칙(2-5). 고아 승인을 ORPHAN 시도 행으로(1-3). 환불 멱등키 유도(1-2 결제). 확정 우선(1-3 예약 전이표의 T1 가드).

요구사항 역추적 (시스템 보장 여섯).

| 요구사항 | 대응 |
|---|---|
| R1 연박 전체 선점 실패 | A1 연박 원자성, hold(n) 계약(전체 롤백), RequestBooking Invariant |
| R2 초과 예약 0 | I1 재고 총량, I1a 수량 하한, 0-1절 잠금 순서, hold와 commit의 잠금 후 검사 |
| R3 중복 예약 방지 | U1 멱등키 유일, RequestBooking의 멱등 반환(재고 선점과 이벤트 재발행 없음), 동시 진입의 재조회 멱등 반환 |
| R4 HELD 종료 시 재고 반환 | 종료 조건은 TTL 만료와 결제 3회 실패 중 먼저 커밋되는 것이다. 단 만료 커밋 전에 승인이 기록됐으면 확정이 우선한다. 리스너 유실 시에도 늦어도 T2 주기 안에 종료된다. A2 종료 반환, expire(reason) 계약, 1-3 예약 전이표, P3와 T1과 T2 |
| R5 확정 금액 불변 | I4 스냅샷 동결, I15 총액 합치, adjustRate와 프로모션 update, close의 스냅샷 무영향, openAttempt의 총액 대조 |
| R6 승인 후속 완결 | I7(NORMAL만), U4 단일 컬럼 유니크, U5 attemptId 유일, C-4의 고아 규칙, P1의 CONFIRMED와 CANCELED 무해 분기, C-5 환불 멱등키. 승인된 결제는 확정으로 소비되거나 환불로 되돌려지고 그 사실이 정산 표식으로 남는다. 대상 범위는 NORMAL과 ORPHAN 둘 다이며 남는 기록이 서로 다르다. NORMAL은 settledAt이 그 기록이고 ORPHAN은 시도 행의 REFUNDED 상태가 그 기록이다. 고아 승인은 예약 후속이 아니라 결제 내부 후속이라 settledAt을 쓰지 않는다 (2026-09-09 R1 반영. S8-R1-B-06) |

[가설] 잔여.
- ttl 기본 10분, 개설 상한 366일, 통화 KRW 정수. T1 30초, T2 60초, 임계 30초, 배치 100. PG 호출 타임아웃 3초. 설정값, Step 9.
- PricingService 소속(예약), 반환 원천 타입의 위치(재고 API). Step 9.

04-5 6절 이월 4건의 종결 근거.

| 이월 항목 | 종결 근거 |
|---|---|
| 정책 가드 확정 | 2-2 정책 카드에서 8개 전부 확정 |
| Commit과 Release 중복 처리 | 확정과 만료 트랜잭션 내부 호출이라 중복이 발생하지 않는다 |
| 결제 시도 타임아웃 | v1 미도입. TTL이 최종 안전망(1-3 알려진 갇힘) |
| ExpireBooking 중복 거부의 의미론 | 애그리거트 계약은 오류, 정책 경로의 앱 서비스는 무시하고 로그, 사람 경로의 앱 서비스는 오류 (C-8) |

Step 9 이월.
- 예외 이름 확정, 갭 락 테스트, 설정값, 대리키 매핑, 반환 원천 위치, 서비스 배치, 계약표의 테스트 변환(1-1 문장이 테스트 이름).
- 비동기 이벤트 전달 메커니즘은 C-3에서 확정 제안됐다. 이월 해제.

실 PG 전환 시 되돌릴 것.
- 취소 환불을 비동기와 Outbox로.
- 결제 시도 타임아웃 스케줄러 도입.
- PG 요청을 커밋 후 호출로(C-10).
- 환불을 REFUND_PENDING 커밋과 T2 재시도로 바꿔 잠금 밖으로(C-12).
- 고아 환불 재시도 상한과 운영 알림(C-5).

## 4. 반영하지 않은 것

| 항목 | 이유 |
|---|---|
| InventoryHoldRejected 이벤트 추가 | 09-1 보드 v2가 2026-09-08 반입됐고 03 v12 11-2절이 도메인 이벤트로 올리지 않는 것으로 판정했다. 이벤트 수 22를 유지한다 (2026-09-09 R1 반영. S8-R1-A-10, S8-R1-B-05) |
| R4의 기산점을 승인 또는 실패 기록 시각으로 읽을 때의 상한 | 그 경우 상한이 임계에 주기를 더한 값이 된다. 요구사항 문언을 바꾸는 일이라 별도 결정이다. 2-5절에 두 구간을 다 적어 두었다 |
| 입력 팩 v3과 02의 R4와 R6 문언 | 기산점과 R6 범위의 명시는 이 문서 2-5절과 3절에 두었다. 상위 요구사항 문언은 고치지 않는다. 고치려면 별도 결정이 필요하다 |

이 판은 2026-09-09 사용자 최종 완료 확정을 받은 확정본이다. task-S8 08-3 반영과 R1 평가를 담는다.
