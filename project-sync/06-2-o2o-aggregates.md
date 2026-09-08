# O2O 애그리거트 레퍼런스 (Step 6)

최초 작성: 2026-09-04 (자료 06 기준 재도출)
최종 갱신: 2026-09-04 v4 (자기 감사 반영: U4 분류 모순 해소, I3을 내부 불변식으로 재분류, 잠금 순서 검사를 이월 목록에 추가)
성격: 레퍼런스. 결정, 규칙, 표만 적는다. 근거와 판정 이유는 06-2-o2o-aggregates-explained.md의 같은 절 번호에 있다.
입력: 04 역할 후보, 04-5 정책 8개, 05-2 배분표, 05-3 v9, 06-1 R4와 R6. 구 초안 검증(06-3) 수용분 승계
후속: Step 7 DbC 계약 (06-4), Step 8 정책 명세, Step 9 패키지 구조

## 1. 애그리거트 7개

| 루트 | 컨텍스트 | 내부 요소 | 지키는 불변식 | 식별자 |
|---|---|---|---|---|
| Property | 카탈로그 | Address VO, Region VO | 없음 | PropertyId |
| RoomType | 카탈로그 | maxOccupancy, PropertyId | I14 (maxOccupancy > 0) | RoomTypeId |
| DailyInventory | 재고와 요금 | stayDate, totalCount, soldCount, heldCount, RoomTypeId | I1, I1a | 대리키 + 유니크(roomTypeId, stayDate) |
| DailyRate | 재고와 요금 | stayDate, rate(Money), RoomTypeId | I2 | 대리키 + 유니크(roomTypeId, stayDate) |
| Promotion | 프로모션 | Condition VO(minNights, Region, campaignPeriod), discountRate, 상태(OPEN, CLOSED) | I8(VO), I13 | PromotionId |
| Booking | 예약 | StayPeriod VO, PriceSnapshot VO(DailyPrice ×N, promotionId, promotionName, totalAmount), BookingStatus, expiresAt, idempotencyKey, UserId, userCount, RoomTypeId | I3, I4, I5, I10, I11, I12, I15 | BookingId + 유니크(idempotencyKey) |
| Payment | 결제 | BookingId, amount(Money), PaymentAttempt 엔티티 목록 | I6, I7, I9 (U4는 선행조건, 3-4) | PaymentId + 유니크(bookingId) |

검색 읽기 모델: 커맨드 없음, 애그리거트 없음.

## 2. 커맨드 → 루트 매핑 (22개)

| 컨텍스트 | 커맨드 | 루트 |
|---|---|---|
| 카탈로그 | RegisterProperty, UpdateProperty | Property |
| 카탈로그 | RegisterRoomType, UpdateRoomType | RoomType |
| 재고와 요금 | OpenInventory, AdjustInventory, HoldInventory, CommitInventory, ReleaseInventory | DailyInventory |
| 재고와 요금 | RegisterRate, AdjustRate | DailyRate |
| 프로모션 | CreatePromotion, UpdatePromotion, ClosePromotion | Promotion |
| 예약 | RequestBooking, ConfirmBooking, CancelBooking, ExpireBooking | Booking |
| 결제 | RequestPayment, RecordPaymentApproval, RecordPaymentFailure, RefundPayment | Payment |

## 3. 불변식 카탈로그

### 3-1. 내부 불변식 (2026-09-04 v4: I3 이동, I14와 I15 등재)

| 번호 | 규칙 | 수호자 |
|---|---|---|
| I1 | totalCount >= soldCount + heldCount | DailyInventory |
| I1a | soldCount >= 0, heldCount >= 0 | DailyInventory |
| I2 | rate > 0 | DailyRate |
| I3 | checkIn < checkOut | Booking (StayPeriod VO) |
| I6 | 결제 시도 수 <= 3 | Payment |
| I7 | 예약당 승인 이력 하나 (APPROVED 또는 그로부터 전이된 REFUNDED) | Payment |
| I9 | REQUESTED 시도는 동시에 하나 | Payment |
| I10 | 할인 배분액 합 == 할인 총액 | Booking (PriceSnapshot) |
| I11 | 스냅샷 행 수 == 박수 | Booking (PriceSnapshot) |
| I12 | 할인 배분액 > 0이면 promotionId 보유 | Booking (PriceSnapshot) |
| I14 | maxOccupancy > 0 | RoomType |
| I15 | totalAmount == 날짜별 (단가 - 할인 배분액)의 합 | Booking (PriceSnapshot) |
| I8 | campaignPeriod.start <= end (양끝 포함) | Condition VO |

### 3-2. 시간적 불변식

| 번호 | 규칙 | 수호자 |
|---|---|---|
| I4 | 생성 이후 PriceSnapshot 불변 | Booking |
| I5 | 전이는 HELD → CONFIRMED, HELD → EXPIRED, CONFIRMED → CANCELED 셋뿐 | Booking |
| I13 | CLOSED 프로모션은 다시 열리지 않는다 | Promotion |

### 3-3. 연관 불변식 (1초 테스트 판정 포함)

| 번호 | 규칙 | 판정 |
|---|---|---|
| A1 | 연박 N일 전부 선점 또는 전무 (R1) | 강한 일관성. 같은 트랜잭션, 규칙 3 예외 |
| A2 | HELD 종료 시 재고 반환 (R4) | 강한 일관성 채택 (동기 같은 트랜잭션) |
| A3 | 확정 시 선점을 판매로 이동 | 강한 일관성 채택 (동기 같은 트랜잭션) |
| A4 | 결제 승인 시 예약 확정 | 결과적 일관성. 구조는 Step 8 |
| A5 | userCount <= maxOccupancy | 신청 시점 검증 (예약 앱 서비스 선행조건) |
| A6 | 숙박 기간 모든 날짜에 요금 존재 | 신청 시점 검증 (예약 앱 서비스 선행조건) |

### 3-4. 유일성과 멱등 (애그리거트 밖)

| 번호 | 규칙 | 강제 수단 |
|---|---|---|
| U1 | 같은 idempotencyKey의 예약은 하나 | DB 유니크 + RequestBooking 선행조건 |
| U2 | 같은 (roomTypeId, stayDate)의 DailyInventory는 하나. DailyRate도 동일 | DB 유니크 |
| U3 | 같은 bookingId의 Payment는 하나 | DB 유니크 |
| U4 | 같은 pgTransactionId 콜백은 승인이든 실패든 무시. 이벤트 재발행 없음 | Payment 선행조건 + DB 유니크 |

## 4. 트랜잭션 경계

| 경로 | 트랜잭션 안 | 커밋 후 발행 |
|---|---|---|
| 생성 | InventoryAllocationService.hold ×N → Booking 생성(스냅샷 동결) | BookingCreated, InventoryHeld ×N |
| 확정 | Booking 잠금 → confirm() → commit ×N | BookingConfirmed, InventoryCommitted ×N |
| 만료, 취소 | Booking 잠금 → expire(reason) 또는 cancel() → release ×N (source 지정) | BookingExpired 또는 BookingCanceled, InventoryReleased ×N |

- 규칙 3(한 트랜잭션 한 애그리거트)의 의도적 예외 2건: 위 세 경로의 재고 N행 + Booking, OpenInventory의 N행 생성. Step 7 계약표 머리에 선언.
- P3, P5, P7은 동기 호출로 명세 (Stateless 확정). 취소의 RefundPayment(P6)는 Step 8.
- T1: 스케줄러가 만료 경로의 호출자.
- 이벤트 발행은 커밋 후. PaymentApproved 유실 시 승인 후 예약 부재 상태(TTL 만료, 수동 환불 대상)는 학습 범위로 감수.

## 5. 동시성 규칙

| 대상 | 규칙 |
|---|---|
| DailyInventory ×N | stayDate 오름차순으로 잠근다 |
| Booking | confirm, expire, cancel은 행을 잠근 뒤 status 검사. expire(TTL_EXPIRED)는 expiresAt <= now 재검사 |
| Payment | 시도 추가와 콜백 기록은 루트를 잠근 뒤 수행 |

락 방식은 Step 7(06-4)에서 비관적 락으로 확정됐다.

## 6. CRC 카드 (2026-09-04 v3: 책임 행 단위 분리, 행별 협력자 대응)

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
| | hold(n), commit(n), releaseHeld(n), releaseSold(n), 조정을 한다. 모든 수량 변경에서 I1과 I1a를 검사하고 위반이면 거부한다 | 없음 |
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
| | ConfirmBooking: Booking을 잠그고 confirm한 뒤 같은 트랜잭션에서 재고를 확정한다 (A3) | Booking, InventoryAllocationService |
| | ExpireBooking과 CancelBooking: Booking을 잠그고 전이시킨 뒤 같은 트랜잭션에서 재고를 반환한다 (A2). 만료 경로의 호출자는 T1 스케줄러다 | Booking, InventoryAllocationService |
| | RequestPayment 중계: status == HELD를 검사하고 스냅샷 총액을 첨부해 결제 컨텍스트로 넘긴다 | Booking (읽기), 결제 컨텍스트 API |
| PricingService<br>(도메인 서비스) | 숙박 기간의 날짜별 단가와 적용 가능한 프로모션을 읽어 DailyPrice 행 N개와 할인 배분을 계산해 돌려준다. 배분 합계와 총액의 일치를 만들어 내는 쪽이고, 검증하는 쪽은 Booking 생성자다 | DailyRateRepository (읽기), PromotionRepository (읽기) |
| Booking<br>(애그리거트 루트) | 이용자, 대상 객실 타입, 숙박 기간, 인원, 금액 스냅샷, 상태, 만료 시각, 멱등키를 안다 | UserId VO, RoomTypeId VO, StayPeriod VO, PriceSnapshot VO |
| | 생성 시 기간 순서(I3)를 확인하고 스냅샷을 검증(I10, I11, I12, I15)한 뒤 동결한다(I4) | PriceSnapshot VO |
| | confirm은 HELD에서만, expire(reason)는 HELD에서만(TTL이면 expiresAt <= now 재검사), cancel은 CONFIRMED에서만 받는다. 전이표 밖 요청은 거부한다(I5) | 없음 |
| PriceSnapshot<br>(VO) | 날짜별 가격 행 N개와 적용 프로모션의 식별자, 이름, 총액을 안다. 생성 후 불변이다 | DailyPrice VO |

### 결제

| Class | Responsibility | Collaborator |
|---|---|---|
| Payment<br>(애그리거트 루트) | 대상 예약, 청구 총액, 시도 목록을 안다 | BookingId VO, Money VO, PaymentAttempt |
| | 새 시도를 연다. 루트를 잠근 뒤 REQUESTED 없음(I9), 3회 미만(I6), 승인 이력 없음(I7)을 검사하고 통과 시에만 시도를 추가한다 | PaymentAttempt |
| | 승인과 실패를 기록한다. 같은 pgTransactionId 콜백은 무시하고 이벤트를 재발행하지 않는다(U4). 승인은 승인 이력이 이미 있으면 거부한다(I7) | PaymentAttempt |
| | 환불을 처리한다. APPROVED 시도가 있을 때만 받고 그 시도를 REFUNDED로 전이시킨다 | PaymentAttempt |
| PaymentAttempt<br>(엔티티) | 순번, 상태, PG 거래 번호, 요청 시각을 안다. 루트를 통해서만 접근된다 | 없음 |
| | REQUESTED에서 APPROVED 또는 FAILED로 전이하고, APPROVED에서 REFUNDED로 전이한다. 역행하지 않으며 재시도는 새 시도 생성이다 | 없음 |

## 7. 확정 커맨드 시그니처 메모

- ReleaseInventory(count, source). source는 HELD 또는 SOLD. 내부는 releaseHeld(n), releaseSold(n)
- ExpireBooking(reason). reason은 TTL_EXPIRED 또는 PAYMENT_FAILED
- attemptCount는 저장하지 않는다. attempts.size() 계산값
- FAILED 후 재시도는 새 PaymentAttempt 생성 (상태 역행 없음)

## 8. [가설] 목록 (2026-09-04 v4: Step 7 확정분 이관)

- PricingService 소속 예약 (Step 9)
- source 타입은 재고 커맨드 API 소속 (Step 9)
- 결제와 예약 사이 트랜잭션 구조 (Step 8)

락 방식, 미개설 날짜, expiresAt 재검사, Money 반올림은 Step 7(06-4)에서 확정으로 해제됐다.

## 9. 단계 이월

Step 7 (06-4에서 수행됨). 카탈로그 전체(3절)가 계약표 행. 선행조건 확정, 상태 전이표 4종, 락 방식 확정, 경로 간 애그리거트 잠금 순서 일관성 검사(생성: 재고 먼저, 확정과 만료: Booking 먼저), 계약표 머리 선언 2건.
Step 8. 결제 구간 트랜잭션(A4), 가드 확정, ExpireBooking 중복 거부 의미론, 결제 시도 타임아웃.
Step 9. 대리키 매핑, source 위치, 서비스 배치, 커밋 후 발행 메커니즘, idempotencyKey 유니크의 갭 락 거동 테스트.

## 10. 이력과 범위

- 2026-09-04 자료 06 기준 재도출. 구 초안 제거, 검증(06-3) 수용분 승계. 경계는 구 초안과 동일.
- 2026-09-04 v4 자기 감사: U4를 1절에서 선행조건으로 정정, I3을 내부로 재분류, I14와 I15 등재, 환불 전이(APPROVED → REFUNDED) 반영.
- 다루지 않은 핫스팟: 1(CONFIRMED 종착), 4(부분 취소. 열리면 ReleaseInventory에 날짜 지정 필요), 5(예약 변경).
- FigJam 영역 6은 이 문서 1절과 6절 기준으로 그린다.
