# O2O 애그리거트 레퍼런스 (Step 6)

최초 작성: 2026-09-04 (자료 06 기준 재도출)
최종 갱신: 2026-09-09 v5 R1 반영 (평가 지적 2건. Property 불변식 I16 신설, ORPHAN 보관 규칙 I17 신설). 2026-09-08 v5 (08-3 결정 11건 반영: Payment 내부 요소에 정산 표식과 시도 필드 C-1과 C-4, 커맨드 23 C-11, 전역 잠금 순서 C-2, 취소 경로 순서 C-12, CRC에 PaymentApplicationService 신설 C-4, 내부 메서드 시그니처 C-9)
성격: 레퍼런스. 결정, 규칙, 표만 적는다. 근거와 판정 이유는 06-2-o2o-aggregates-explained.md의 같은 절 번호에 있다.
입력: 04 역할 후보, 04-5 v2 정책 8개, 05-2 배분표, 05-3 v10, 06-1 R4와 R6, 08-3 6절 해결안 v2.1. 구 초안 검증(06-3) 수용분 승계
후속: Step 7 DbC 계약 (06-4 v5), Step 9 패키지 구조

## 1. 애그리거트 7개 (2026-09-08 v5: Payment 내부 요소 확장)

| 루트 | 컨텍스트 | 내부 요소 | 지키는 불변식 | 식별자 |
|---|---|---|---|---|
| Property | 카탈로그 | Address VO, Region VO | I16 (2026-09-09 R1 반영. S8-R1-A-03) | PropertyId |
| RoomType | 카탈로그 | maxOccupancy, PropertyId | I14 (maxOccupancy > 0) | RoomTypeId |

Property의 불변식을 없음에서 I16으로 바꿨다 (2026-09-09 R1 반영. S8-R1-A-03). 지키는 규칙이 없는데 루트로 잡혔다는 지적을 받고 경계가 실제로 무엇을 지키는지 다시 봤다. RoomType이 PropertyId로 Property를 가리키고 CatalogApplicationService가 RegisterRoomType 처리 전에 대상 Property의 존재를 확인한다. 그 확인이 지키는 것은 객실 타입이 없는 숙소에 붙지 않는다는 규칙이고 그 규칙의 주인이 Property다. I16을 1-1 불변식 카탈로그에 등재한다.

판정을 맞추려고 없는 불변식을 만든 것이 아니다. 이미 앱 서비스가 강제하던 규칙을 루트 책임으로 올려 적은 것이다. 이 규칙까지 없다고 판단되면 Property는 엔티티로 내려야 하고 그것은 별도 결정이다.
| DailyInventory | 재고와 요금 | stayDate, totalCount, soldCount, heldCount, RoomTypeId | I1, I1a | 대리키 + 유니크(roomTypeId, stayDate) |
| DailyRate | 재고와 요금 | stayDate, rate(Money), RoomTypeId | I2 | 대리키 + 유니크(roomTypeId, stayDate) |
| Promotion | 프로모션 | Condition VO(minNights, Region, campaignPeriod), discountRate, 상태(OPEN, CLOSED) | I8(VO), I13 | PromotionId |
| Booking | 예약 | StayPeriod VO, PriceSnapshot VO(DailyPrice ×N, promotionId, promotionName, totalAmount), BookingStatus, expiresAt, idempotencyKey, UserId, userCount, RoomTypeId | I3, I4, I5, I10, I11, I12, I15 | BookingId + 유니크(idempotencyKey) |
| Payment | 결제 | BookingId, amount(Money), settledAt, settledBy, PaymentAttempt 엔티티 목록 | I6, I7, I9 (NORMAL 시도만 셈. U4와 U5는 선행조건과 DB, 3-4) | PaymentId + 유니크(bookingId) |

PaymentAttempt 엔티티의 필드.

| 필드 | NORMAL | ORPHAN |
|---|---|---|
| kind | NORMAL | ORPHAN |
| attemptId | 있음. U5로 유일 | 없음 |
| 순번 | 있음 | 없음 |
| 상태 | REQUESTED, APPROVED, FAILED, REFUNDED | REFUND_PENDING, REFUNDED |
| pgTransactionId | 콜백 시 저장 | 생성 시 저장 |
| amount | 청구액과 대조 | 도착 금액 그대로 |
| requestedAt | 있음 | 없음 |
| approvedAt | 승인 시 | 없음 |
| failedAt | 실패 시 | 없음 |
| sourceAttemptId | 없음 | 원 시도의 attemptId |
| receivedAt | 없음 | 있음 |

settledBy의 값은 APPROVAL_HANDLER, FAILURE_HANDLER, T1, T2, CANCEL 다섯이다. 관측용이고 분기에 쓰지 않는다.

검색 읽기 모델: 커맨드 없음, 애그리거트 없음.

## 2. 커맨드에서 루트로의 매핑 (23개, 2026-09-08 v5: SettlePayment 추가)

| 컨텍스트 | 커맨드 | 루트 |
|---|---|---|
| 카탈로그 | RegisterProperty, UpdateProperty | Property |
| 카탈로그 | RegisterRoomType, UpdateRoomType | RoomType |
| 재고와 요금 | OpenInventory, AdjustInventory, HoldInventory, CommitInventory, ReleaseInventory | DailyInventory |
| 재고와 요금 | RegisterRate, AdjustRate | DailyRate |
| 프로모션 | CreatePromotion, UpdatePromotion, ClosePromotion | Promotion |
| 예약 | RequestBooking, ConfirmBooking, CancelBooking, ExpireBooking | Booking |
| 결제 | RequestPayment, RecordPaymentApproval, RecordPaymentFailure, RefundPayment, SettlePayment | Payment |

## 3. 불변식 카탈로그

### 3-1. 내부 불변식 (2026-09-08 v5: I6, I7, I9에 NORMAL 한정)

| 번호 | 규칙 | 수호자 |
|---|---|---|
| I1 | totalCount >= soldCount + heldCount | DailyInventory |
| I1a | soldCount >= 0, heldCount >= 0 | DailyInventory |
| I2 | rate > 0 | DailyRate |
| I3 | checkIn < checkOut | Booking (StayPeriod VO) |
| I6 | NORMAL 시도 수 <= 3. ORPHAN 행은 세지 않는다 | Payment |
| I16 | 객실 타입은 존재하는 Property에만 붙는다 (2026-09-09 R1 반영. S8-R1-A-03) | Property |
| I17 | REFUNDED로 종결된 ORPHAN 행은 시도 목록에서 분리해 결제 이력 보관소로 옮긴다. 루트가 한 번에 다루는 ORPHAN 행은 REFUND_PENDING인 것만이다 (2026-09-09 R1 반영. S8-R1-A-04) | Payment |
| I7 | 예약당 승인 이력인 NORMAL 시도 하나 (APPROVED 또는 그로부터 전이된 REFUNDED). ORPHAN 행은 세지 않는다 | Payment |
| I9 | REQUESTED인 NORMAL 시도는 동시에 하나. ORPHAN 행은 세지 않는다 | Payment |
| I10 | 할인 배분액 합은 할인 총액과 같다 | Booking (PriceSnapshot) |
| I11 | 스냅샷 행 수는 박수와 같다 | Booking (PriceSnapshot) |
| I12 | 할인 배분액이 0보다 크면 promotionId 보유 | Booking (PriceSnapshot) |
| I14 | maxOccupancy > 0 | RoomType |
| I15 | totalAmount는 날짜별 (단가 - 할인 배분액)의 합과 같다 | Booking (PriceSnapshot) |
| I8 | campaignPeriod.start <= end (양끝 포함) | Condition VO |

### 3-2. 시간적 불변식

| 번호 | 규칙 | 수호자 |
|---|---|---|
| I4 | 생성 이후 PriceSnapshot 불변 | Booking |
| I5 | 전이는 HELD에서 CONFIRMED로, HELD에서 EXPIRED로, CONFIRMED에서 CANCELED로 셋뿐 | Booking |
| I13 | CLOSED 프로모션은 다시 열리지 않는다 | Promotion |

### 3-3. 연관 불변식

| 번호 | 규칙 | 판정 |
|---|---|---|
| A1 | 연박 N일 전부 선점 또는 전무 (R1) | 강한 일관성. 같은 트랜잭션, 규칙 3 예외 |
| A2 | HELD 종료 시 재고 반환 (R4) | 강한 일관성 채택 (동기 같은 트랜잭션) |
| A3 | 확정 시 선점을 판매로 이동 | 강한 일관성 채택 (동기 같은 트랜잭션) |
| A4 | 결제 승인 시 예약 확정 | 결과적 일관성. 구조는 06-4 v5 2-2의 P1 |
| A5 | userCount <= maxOccupancy | 신청 시점 검증 (예약 앱 서비스 선행조건) |
| A6 | 숙박 기간 모든 날짜에 요금 존재 | 신청 시점 검증 (예약 앱 서비스 선행조건) |

### 3-4. 유일성과 멱등 (애그리거트 밖, 2026-09-08 v5: U4 수단 정정, U5 신설)

| 번호 | 규칙 | 강제 수단 |
|---|---|---|
| U1 | 같은 idempotencyKey의 예약은 하나 | DB 유니크 + RequestBooking 선행조건 |
| U2 | 같은 (roomTypeId, stayDate)의 DailyInventory는 하나. DailyRate도 동일 | DB 유니크 |
| U3 | 같은 bookingId의 Payment는 하나 | DB 유니크 |
| U4 | 같은 pgTransactionId 콜백은 승인이든 실패든 무시. 이벤트 재발행 없음. NORMAL과 ORPHAN을 가리지 않는다 | payment_attempt.pg_transaction_id 단일 컬럼 유니크 + Payment 선행조건 |
| U5 | 같은 attemptId의 NORMAL 시도는 하나 | DB 유니크 |

U4의 강제 수단을 단일 컬럼 유니크로 못박는 이유는 고아를 별도 테이블로 두지 않기 때문이다. 두 테이블에 걸친 유일성은 제약 하나로 막을 수 없다.

## 4. 트랜잭션 경계 (2026-09-08 v5: 취소 순서와 정책 번호)

| 경로 | 트랜잭션 안 | 커밋 후 발행 |
|---|---|---|
| 생성 | InventoryAllocationService.hold(1) ×N, Booking 생성(스냅샷 동결) | BookingCreated, InventoryHeld ×N |
| 확정 | Booking 잠금, Payment 잠금, confirm(), commit ×N, settle | BookingConfirmed, InventoryCommitted ×N |
| 만료 | Booking 잠금, Payment 잠금, expire(reason), releaseHeld ×N, settle | BookingExpired, InventoryReleased ×N |
| 취소 | Booking 잠금, Payment 잠금, refund(재고 반환보다 먼저), settle(CANCEL), cancel(), releaseSold ×N | BookingCanceled, InventoryReleased ×N, PaymentRefunded |

취소에서 refund를 재고 반환보다 먼저 두는 이유는 재고 행 잠금을 쥔 채 PG를 부르지 않기 위해서다. 잠금 순서가 Booking, Payment, 재고이므로 이 순서면 외부 호출 시점에 재고 행 잠금이 아직 없다.

- 규칙 3(한 트랜잭션 한 애그리거트)의 의도적 예외 2건: 위 네 경로의 재고 N행과 Booking, OpenInventory의 N행 생성. 06-4 v5 0절에 선언.
- 정책 번호는 04-5 v2의 신 번호를 쓴다. P2 확정 시 재고 확정, P4 만료 시 재고 반환, P5 취소 시 환불, P6 취소 시 재고 반환은 전부 같은 트랜잭션 내부 호출이다. 비동기인 것은 P1 결제 승인 처리와 P3 결제 실패 시 만료 둘뿐이다.
- T1은 만료 또는 확정 경로의 호출자다. T2는 후속 미완 결제의 보정 호출자다. 둘 다 건별 트랜잭션이다.
- 이벤트 발행은 커밋 후다. PaymentApproved 유실은 T2가 보정한다. 감수 항목이 아니다.

## 5. 동시성 규칙 (2026-09-08 v5: 전역 잠금 순서 표)

전역 순서는 Booking, Payment, 재고 N행(날짜 오름차순)이다. 모든 경로가 이 순서를 따른다.

| 경로 | 잠금 순서 |
|---|---|
| RequestBooking | 재고 N행만 (Booking은 insert) |
| RequestPayment 중계 | Booking, Payment(openAttempt) |
| recordApproval, recordFailure | Payment만 |
| P1 결제 승인 처리 | Booking, Payment, 재고 N행 |
| P3 결제 실패 시 만료 | Booking, Payment, 재고 N행 |
| T1 TTL 만료 | Booking, Payment, 재고 N행 |
| T2 후속 미완 결제 순찰 | Booking, Payment, 재고 N행 |
| CancelBooking | Booking, Payment, 재고 N행 |

| 대상 | 규칙 |
|---|---|
| DailyInventory ×N | stayDate 오름차순으로 잠근다 |
| Booking | confirm, expire, cancel은 행을 잠근 뒤 status 검사. expire(TTL_EXPIRED)는 expiresAt이 now 이하인지 재검사 |
| Payment | 시도 추가, 콜백 기록, 정산 표식, 환불은 루트를 잠근 뒤 수행 |

락 방식은 06-4 v5에서 비관적 락으로 확정됐다.

## 6. CRC 카드 (2026-09-08 v5: 결제 절 재작성, 앱 서비스 신설)

한 행이 책임 하나다. 협력자는 그 행의 책임을 수행할 때 실제로 부르는 상대만 적는다. 이벤트 구독은 정책이므로 적지 않는다.

### 카탈로그

| Class | Responsibility | Collaborator |
|---|---|---|
| CatalogApplicationService<br>(앱 서비스) | 숙소와 객실 타입의 등록, 수정 플로우를 오케스트레이션하고 트랜잭션 경계를 연다 | Property, RoomType, PropertyRepository, RoomTypeRepository |
| | RegisterRoomType 처리 전에 대상 Property가 존재하는지 확인한다 | PropertyRepository (읽기) |
| Property<br>(애그리거트 루트) | 이름, 주소, 지역을 안다 | Address VO, Region VO |
| | 숙소를 등록하고 서술 속성을 수정한다. 식별자는 바꾸지 않는다 | 없음 |
| RoomType<br>(애그리거트 루트) | 소속 숙소, 이름, 최대 인원을 안다 | PropertyId VO |
| | 객실 타입을 등록하고 수정한다. maxOccupancy > 0(I14)을 검사한다 | 없음 |

### 재고와 요금

| Class | Responsibility | Collaborator |
|---|---|---|
| InventoryApplicationService<br>(앱 서비스) | 개설, 조정, 요금 등록과 조정 플로우의 트랜잭션 경계를 연다 | DailyInventory, DailyRate, 리포지토리 |
| | OpenInventory에서 기간의 날짜 행 N개를 한 트랜잭션으로 생성한다 (규칙 3 예외) | DailyInventory ×N |
| | 개설과 요금 등록 전에 roomTypeId가 카탈로그에 존재하는지 확인한다 (06-1 R1) | RoomTypeRepository (읽기) |
| InventoryAllocationService<br>(도메인 서비스) | 숙박 기간의 재고 N행을 stayDate 오름차순으로 잠근 뒤 hold, commit, release를 전 행에 적용한다. 한 행이라도 거부하면 전체를 롤백한다 (A1) | DailyInventory ×N, DailyInventoryRepository |
| DailyInventory<br>(애그리거트 루트) | 객실 타입과 날짜, 수량 셋(총, 판매, 선점)을 안다 | RoomTypeId VO |
| | hold(1), commit(1), releaseHeld(1), releaseSold(1), 조정을 한다. 모든 수량 변경에서 I1과 I1a를 검사하고 위반이면 거부한다 | 없음 |
| | 가용성(total - sold - held)을 계산해 답한다. 저장하지 않는다 | 없음 |
| DailyRate<br>(애그리거트 루트) | 객실 타입과 날짜의 1박 단가를 안다 | RoomTypeId VO, Money VO |
| | 요금을 등록하고 조정한다. rate > 0(I2)을 검사한다 | 없음 |

### 프로모션

| Class | Responsibility | Collaborator |
|---|---|---|
| Promotion<br>(애그리거트 루트) | 적용 조건, 할인율, 상태(OPEN, CLOSED)를 안다 | Condition VO |
| | 생성하고 수정하고 종료한다. 종료 후 재개 요청은 거부한다 (I13) | 없음 |
| | isApplicable(region, nights, at)을 판정한다. 상태가 OPEN이고 조건이 전부 맞을 때만 참이다 | Condition VO |
| Condition<br>(VO) | 최소 숙박일, 지역, 캠페인 기간(양끝 포함)을 안다. 생성 시 I8을 검사한다 | 없음 |

### 예약

| Class | Responsibility | Collaborator |
|---|---|---|
| BookingApplicationService<br>(앱 서비스) | 예약 컨텍스트의 트랜잭션 경계를 열고, 커밋 후 이벤트를 발행한다 | Booking, BookingRepository |
| | RequestBooking: 인원을 검증하고(A5), 가격 계산을 맡긴 뒤, 재고 선점과 예약 생성을 한 트랜잭션으로 묶는다 | RoomTypeRepository (읽기), PricingService, InventoryAllocationService, Booking |
| | confirmHeld(bookingId, settledBy): Booking을 잠그고 confirm한 뒤 같은 트랜잭션에서 재고를 확정하고(A3) 정산 표식을 찍는다. P1과 T1의 확정 분기가 공유한다 | Booking, InventoryAllocationService, 결제 컨텍스트 API |
| | ExpireBooking과 CancelBooking: Booking을 잠그고 전이시킨 뒤 같은 트랜잭션에서 재고를 반환한다 (A2) | Booking, InventoryAllocationService |
| | 상태에 따라 부를 메서드를 고른다. CONFIRMED나 CANCELED를 만나면 expire를 부르지 않고 로그 후 정산 표식만 찍는다 | Booking (읽기), 결제 컨텍스트 API |
| | RequestPayment 중계: Booking을 잠그고 status가 HELD인지 검사한 뒤 스냅샷 총액을 첨부해 결제 컨텍스트로 넘긴다 | Booking (읽기), 결제 컨텍스트 API |
| | T1과 T2의 스케줄러 진입점을 제공하고 paymentFollowUp과 findFollowUpCandidates를 호출한다 | 결제 컨텍스트 API, Booking, InventoryAllocationService |
| PricingService<br>(도메인 서비스) | 숙박 기간의 날짜별 단가와 적용 가능한 프로모션을 읽어 DailyPrice 행 N개와 할인 배분을 계산해 돌려준다. 배분 합계와 총액의 일치를 만들어 내는 쪽이고, 검증하는 쪽은 Booking 생성자다 | DailyRateRepository (읽기), PromotionRepository (읽기) |
| Booking<br>(애그리거트 루트) | 이용자, 대상 객실 타입, 숙박 기간, 인원, 금액 스냅샷, 상태, 만료 시각, 멱등키를 안다 | UserId VO, RoomTypeId VO, StayPeriod VO, PriceSnapshot VO |
| | 생성 시 기간 순서(I3)를 확인하고 스냅샷을 검증(I10, I11, I12, I15)한 뒤 동결한다(I4) | PriceSnapshot VO |
| | confirm은 HELD에서만, expire(reason)는 HELD에서만(TTL이면 expiresAt이 now 이하인지 재검사), cancel은 CONFIRMED에서만 받는다. 전이표 밖 요청은 거부한다(I5) | 없음 |
| PriceSnapshot<br>(VO) | 날짜별 가격 행 N개와 적용 프로모션의 식별자, 이름, 총액을 안다. 생성 후 불변이다 | DailyPrice VO |

### 결제

| Class | Responsibility | Collaborator |
|---|---|---|
| PaymentApplicationService<br>(앱 서비스) | 결제 컨텍스트의 트랜잭션 경계를 열고 외부 PG를 호출한다. 애그리거트 대신 외부와 말하는 유일한 자리다 | Payment, PaymentRepository, Mock PG |
| | openAttempt 뒤 같은 트랜잭션에서 attemptId를 멱등키로 PG에 요청을 보낸다 | Payment, Mock PG |
| | 고아 환불 필요를 받으면 같은 트랜잭션에서 pgTransactionId를 멱등키로 PG 환불을 부르고, 성공이면 markOrphanRefunded를 부른다. 실패는 잡아 로그하고 REFUND_PENDING으로 커밋한다 | Payment, Mock PG |
| | refund 뒤 그 시도의 attemptId를 멱등키로 PG 환불을 부른다 | Payment, Mock PG |
| | paymentFollowUp과 findFollowUpCandidates 조회를 예약 컨텍스트에 제공한다 | PaymentRepository (읽기) |
| Payment<br>(애그리거트 루트) | 대상 예약, 청구 총액, 정산 표식, 시도 목록을 안다 | BookingId VO, Money VO, PaymentAttempt |
| | 새 NORMAL 시도를 연다. 루트를 잠근 뒤 REQUESTED 없음(I9), 3회 미만(I6), 승인 이력 없음(I7)을 NORMAL 행만 세어 검사하고 통과 시에만 시도를 추가하며 attemptId를 발급한다 | PaymentAttempt |
| | 승인과 실패를 기록한다. 같은 pgTransactionId 콜백은 무시하고 이벤트를 재발행하지 않는다(U4) | PaymentAttempt |
| | 고아 승인을 ORPHAN 행으로 기록하고 고아 환불 필요를 반환한다. 종착 시도에 온 승인, 이미 승인 이력이 있는 승인, 금액 불일치 셋이 고아다 | PaymentAttempt |
| | 정산 표식을 찍고 되돌린다. settle은 이미 찍혀 있으면 무해다. 승인 기록과 3회째 실패 기록이 되돌린다 | 없음 |
| | 환불을 처리한다. NORMAL APPROVED 시도가 있을 때만 받고 그 시도를 REFUNDED로 전이시킨다 | PaymentAttempt |
| | PG를 호출하지 않는다. 외부 호출은 전부 PaymentApplicationService가 한다 | 없음 |
| PaymentAttempt<br>(엔티티) | 종류(NORMAL 또는 ORPHAN), 순번, 상태, 시도 식별자, PG 거래 번호, 시각들을 안다. 루트를 통해서만 접근된다 | 없음 |
| | NORMAL은 REQUESTED에서 APPROVED 또는 FAILED로, APPROVED에서 REFUNDED로 전이한다. ORPHAN은 REFUND_PENDING에서 REFUNDED로만 전이한다. 역행하지 않으며 재시도는 새 NORMAL 시도 생성이다 | 없음 |

## 7. 확정 커맨드 시그니처 메모 (2026-09-08 v5)

- ReleaseInventory(count, source). source는 HELD 또는 SOLD. 내부는 releaseHeld(n), releaseSold(n). v1에서 n은 1이다
- ExpireBooking(reason). reason은 TTL_EXPIRED 또는 PAYMENT_FAILED
- SettlePayment(bookingId, settledBy). 이벤트를 내지 않는다
- RefundPayment(bookingId). 멱등키는 그 시도의 attemptId에서 유도한다
- confirmHeld(bookingId, settledBy)는 커맨드가 아니라 예약 앱 서비스의 내부 메서드다. P1과 T1이 공유한다
- attemptCount는 저장하지 않는다. NORMAL 시도 수의 계산값이다
- FAILED 후 재시도는 새 NORMAL PaymentAttempt 생성이다 (상태 역행 없음)

## 8. [가설] 목록 (2026-09-08 v5)

- PricingService 소속 예약 (Step 9)
- source 타입은 재고 커맨드 API 소속 (Step 9)
- T1 30초, T2 60초, 임계 30초, 배치 100, PG 호출 타임아웃 3초 (Step 9 설정값)

락 방식, 미개설 날짜, expiresAt 재검사, Money 반올림은 06-4 v4에서 해제됐다. 결제와 예약 사이 트랜잭션 구조는 08-3 C-1과 C-9에서 해제됐다.

## 9. 단계 이월 (2026-09-08 v5)

Step 8은 종결됐다. 결제 구간 트랜잭션(A4), 가드 확정, ExpireBooking 중복 거부 의미론, 결제 시도 타임아웃 넷 모두 08-3 6절에서 닫혔다. 근거는 06-4 v5 3절의 종결 표에 있다.

Step 9. 대리키 매핑, source 위치, 서비스 배치, 커밋 후 발행 메커니즘, idempotencyKey 유니크의 갭 락 거동 테스트, 설정값.

## 10. 이력과 범위

- 2026-09-04 자료 06 기준 재도출. 구 초안 제거, 검증(06-3) 수용분 승계. 경계는 구 초안과 동일.
- 2026-09-04 v4 자기 감사: U4를 1절에서 선행조건으로 정정, I3을 내부로 재분류, I14와 I15 등재, 환불 전이 반영.
- 2026-09-08 v5: 08-3 결정 11건 반영. 애그리거트 경계는 바뀌지 않았다. Payment 내부 요소와 협력자, 잠금 순서, 트랜잭션 경계의 순서가 바뀌었다.
- 다루지 않은 핫스팟: 1(CONFIRMED 종착), 4(부분 취소. 열리면 ReleaseInventory에 날짜 지정 필요), 5(예약 변경).
- FigJam 영역 6은 이 문서 1절과 6절 기준으로 그린다.

## 11. 반영하지 않은 것

| 항목 | 이유 |
|---|---|
| InventoryHoldRejected 이벤트 추가 | 09-1 보드 v2가 project-sync에 반입되지 않았다 |

이 판은 2026-09-09 사용자 최종 완료 확정을 받은 확정본이다. task-S8 08-3 반영과 R1 평가를 담는다.
