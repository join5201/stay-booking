# O2O DbC 계약과 정책 레퍼런스 (Step 7)

최초 작성: 2026-09-04
최종 갱신: 2026-09-04 v4 (블라인드 검증 수용 반영: 치명 2건 = 무해 경로의 재고 연산 생략 분기 명시, 멱등 반환을 종착 무해와 구분. 보통 = A4 문장 교체, 커맨드 매핑표, PricingService와 isApplicable 계약 행, 프로모션 스냅샷 무영향, 결제 실패 만료 재검사 가드, REQUESTED 갇힘 명시, expiresAt 불변, hold 예외 분리, 전이표 가드 통일, 동기 정책 트리거 표기 정정, R 역추적표. 승인 유실 창은 Step 8 이월 기록)
성격: 레퍼런스. 각 절 첫 줄의 쉬운 요약과 표만 담는다. 근거와 판정 이유는 06-4-o2o-contracts-explained.md의 같은 절 번호에 있다.
자료: 07 도메인 규칙 정제와 설계 계약. 1절이 자료의 Step 1, 2절이 자료의 Step 2다.
입력: 06-2 v4, 04-5, 05-3 v10. 04-5의 번호 표기(P1 등)는 이 판부터 정책 이름으로 풀어 쓴다.
검증: 06-5-o2o-contracts-blind-review.md (블라인드 감사 기록, v3에 대해 수행)
후속: Step 8 (자료 08 입수 시 대조), Step 9 구현 매핑

## 0. 머리 선언 (전 계약 공통)

쉽게 말하면: 계약은 세 가지 약속이다. 부르는 쪽이 지킬 것(선행조건), 실행한 쪽이 보장할 것(후행조건), 누구든 언제나 지킬 것(불변식). 약속을 어기면 그 자리에서 실패시켜 누구 잘못인지 바로 드러낸다.

- 한 트랜잭션 한 애그리거트 규칙의 의도적 예외 2건: 예약의 세 경로(생성, 확정, 만료와 취소)가 재고 여러 행과 Booking을 함께 수정하는 것, 재고 개설이 여러 행을 만드는 것.
- 자료 07 이탈 선언: 다른 컨텍스트로 가는 자동 반응은 원칙상 비동기 결과적 일관성인데, 재고로 가는 셋(확정 시 재고 확정, 만료 시 재고 반환, 취소 시 재고 반환)은 단일 모듈의 이점으로 같은 트랜잭션에 묶는다. MSA 전환 시 되돌린다.
- 검증 위치: 형식 검사는 컨트롤러, 규칙 검사는 애그리거트, 무결성은 DB (1-4 표).
- 위반 책임: 선행조건 위반은 부른 쪽 잘못, 후행조건과 불변식 위반은 실행한 쪽 잘못.
- 락: 비관적 락으로 확정. 재고 여러 행은 날짜 오름차순으로, Booking과 Payment는 상태 검사 전에 잠근다.
- 종착 재호출 무해: 이미 그 종착 상태면 상태 변경, 재고 연산, 이벤트 발행을 모두 생략하고 성공으로 답한다. 적용 6곳은 확정(CONFIRMED에 재확정), 만료(EXPIRED에 재만료), 취소(CANCELED에 재취소), 프로모션 종료(CLOSED에 재종료), 환불(REFUNDED에 재환불), 같은 거래번호 콜백. 무해로 답한 애그리거트 메서드는 전이가 일어나지 않았음을 호출자(앱 서비스)에 반환해, 같은 트랜잭션의 후속 재고 연산도 함께 생략되게 한다.
- 멱등 반환 (종착 무해와 성격이 다름): RequestBooking의 같은 멱등키 재요청은 기존 예약을 그대로 반환한다. 대상이 종착이 아닌 HELD여도 성립하며, 재고 선점과 이벤트 발행을 다시 하지 않는다.

## 1. Step 1: 도메인 규칙 정제와 DbC 계약 명세화

### 1-1. 불변식 카탈로그

쉽게 말하면: 깨지면 안 되는 규칙 목록이다. 문장이 그대로 테스트 이름이 되고, 짧은 이름으로 다른 표에서 참조한다.

| 번호 | 이름 | 테스트 문장 | 책임 위치 |
|---|---|---|---|
| I1 | 재고 총량 | 어느 시점에 조회해도 totalCount는 soldCount + heldCount 이상이다 | DailyInventory |
| I1a | 수량 하한 | soldCount와 heldCount는 음수가 될 수 없다 | DailyInventory |
| I2 | 요금 양수 | 요금은 0보다 크다 | DailyRate |
| I3 | 기간 순서 | 체크인은 체크아웃보다 앞선다 | StayPeriod VO |
| I4 | 스냅샷 동결 | 생성된 뒤 스냅샷의 어떤 값도, expiresAt도 바뀌지 않는다 | Booking |
| I5 | 전이 폐쇄 | 허용 전이 셋 밖의 상태 변경은 일어나지 않는다 | Booking |
| I6 | 시도 상한 | 결제 시도는 3개를 넘지 않는다 | Payment |
| I7 | 승인 유일 | 승인 이력(APPROVED 또는 REFUNDED) 시도는 하나를 넘지 않는다 | Payment |
| I8 | 캠페인 순서 | 캠페인 시작일은 종료일보다 늦지 않다 | Condition VO |
| I9 | 진행 유일 | REQUESTED 시도는 동시에 하나를 넘지 않는다 | Payment |
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
| U4 | 콜백 멱등 | 같은 pgTransactionId 콜백은 승인이든 실패든 무시하고 이벤트를 다시 내지 않는다 | 선행조건 + DB 유니크 |

### 1-2. 행동별 DbC 계약표

쉽게 말하면: 커맨드 하나마다 넷을 적는다. 실행 전 확인할 것(Pre), 언제나 지킬 것(Invariant), 실행 후 보장할 것(Post), 어기면 어떤 예외인가.

커맨드 22개와 계약표 행의 대응. 괄호는 값 열거.

| 컨텍스트 | 커맨드 → 계약 행 |
|---|---|
| 카탈로그 | RegisterProperty → registerProperty, UpdateProperty → updateProperty, RegisterRoomType → registerRoomType, UpdateRoomType → updateRoomType |
| 재고와 요금 | OpenInventory → openInventory, AdjustInventory → adjust, HoldInventory → hold(n), CommitInventory → commit(n), ReleaseInventory(count, source: HELD 또는 SOLD) → releaseHeld(n) 또는 releaseSold(n) (source 분기는 앱 서비스), RegisterRate → registerRate, AdjustRate → adjustRate |
| 프로모션 | CreatePromotion → create, UpdatePromotion → update, ClosePromotion → close |
| 예약 | RequestBooking → RequestBooking(앱 서비스) + Booking 생성자, ConfirmBooking → confirm(), CancelBooking → cancel(), ExpireBooking(reason: TTL_EXPIRED 또는 PAYMENT_FAILED) → expire(reason) |
| 결제 | RequestPayment → RequestPayment 중계(예약 앱 서비스) + openAttempt(결제), RecordPaymentApproval → recordApproval, RecordPaymentFailure → recordFailure, RefundPayment → refund |

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
| hold(n) | 오름차순 잠금. 행 존재, 가용 수량 n 이상 | 재고 총량, 수량 하한 | heldCount 증가, InventoryHeld 발행 | InventoryNotOpened(미개설), InventoryShortage(가용 부족). 어느 쪽이든 연박 원자성(A1)에 따라 전체 롤백 |
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

| 행동 | Pre(선행) | Invariant | Post(후행) | 위반 시 예외 |
|---|---|---|---|---|
| RequestBooking (앱 서비스) | 같은 멱등키 존재 시 멱등 반환(U1, 0절). 인원 검증(A5), 요금 존재(A6), 재고 가용(hold에 위임) | 연박 원자성(A1) | HELD 생성, expiresAt = 생성 시각 + ttl (이후 불변, I4), 스냅샷(DailyPrice ×N, promotionId, promotionName, 총액) 동결. 커밋 후 BookingCreated와 InventoryHeld 발행. 멱등 반환 경로에서는 재고 선점과 이벤트 발행 없이 기존 Booking을 그대로 반환 | OccupancyExceeded, RateNotFound, InventoryShortage. 실패 시 전체 롤백 |
| PricingService (도메인 서비스) | 숙박 기간 전 날짜에 요금 존재(A6) | 배분 합치(I10), 행수 일치(I11), 총액 합치(I15)를 만족하는 결과 생산 | DailyPrice N행과 할인 배분 반환. 프로모션 선정은 isApplicable(예약 생성 시점, 캠페인 기간 양끝 포함). 단수는 날짜별 비례 내림 후 마지막 날짜 행 가산 | RateNotFound |
| Booking 생성자 | 기간 순서(I3), 스냅샷 검증 통과 | 기간 순서, 배분 합치, 행수 일치, 할인 근거, 총액 합치 | HELD 상태의 유효한 Booking | InvalidSnapshot |
| confirm() | 잠금. 상태가 HELD. CONFIRMED 재호출 무해 | 전이 폐쇄(I5) | 전이 경로: CONFIRMED로 전이하고 전이 발생을 반환. 같은 트랜잭션에서 확정 이동(A3), 커밋 후 BookingConfirmed 발행. 무해 경로(이미 CONFIRMED): 상태 변경, 재고 연산, 이벤트 발행 없이 성공 반환(전이 없음을 반환) | EXPIRED는 거부(승인 지연 환불 정책의 영역), CANCELED는 InvalidStateTransition |
| expire(reason) | 잠금. 상태가 HELD. 원인이 TTL이면 expiresAt 경과 추가 검사. 원인이 결제 실패면 이벤트 탑재 시도 수 3 이상 재확인. EXPIRED 재호출 무해 | 전이 폐쇄 | 전이 경로: EXPIRED로 전이하고 전이 발생을 반환. 같은 트랜잭션에서 종료 반환(A2), 커밋 후 BookingExpired 발행. 무해 경로(이미 EXPIRED): 상태 변경, 재고 연산, 이벤트 발행 없이 성공 반환 | CONFIRMED와 CANCELED는 InvalidStateTransition |
| cancel() | 잠금. 상태가 CONFIRMED. CANCELED 재호출 무해 | 전이 폐쇄 | 전이 경로: CANCELED로 전이하고 전이 발생을 반환. 같은 트랜잭션에서 판매분 반환, 커밋 후 BookingCanceled 발행. 무해 경로(이미 CANCELED): 상태 변경, 재고 연산, 이벤트 발행 없이 성공 반환 | HELD와 EXPIRED는 InvalidStateTransition |
| RequestPayment 중계 (앱 서비스) | 상태가 HELD | 없음 | 스냅샷 총액을 첨부해 결제 컨텍스트 호출 | NotHeldBooking |

#### 결제

| 행동 | Pre(선행) | Invariant | Post(후행) | 위반 시 예외 |
|---|---|---|---|---|
| openAttempt | 잠금. 진행 중 시도 없음(I9), 시도 3 미만(I6), 승인 이력 없음(I7), 전달 총액과 청구액 일치. 첫 요청이면 Payment 생성(U3) | 시도 상한, 승인 유일, 진행 유일 | REQUESTED 시도 추가, PaymentRequested 발행 | AttemptInProgress, AttemptLimitExceeded, AlreadyApproved, AmountMismatch |
| recordApproval | 잠금. 같은 거래번호는 무해 무시(U4). REQUESTED 존재, 승인 이력 없음 | 승인 유일 | REQUESTED에서 APPROVED로, PaymentApproved 발행(시도 수 탑재) | NoRequestedAttempt, AlreadyApproved |
| recordFailure | 잠금. 같은 거래번호는 무해 무시. REQUESTED 존재 | 시도 상한 | REQUESTED에서 FAILED로, PaymentFailed 발행(시도 수 탑재) | NoRequestedAttempt |
| refund | 잠금. APPROVED 존재. REFUNDED 재호출 무해 | 승인 유일 | APPROVED에서 REFUNDED로, PaymentRefunded 발행 | NoApprovedAttempt |

### 1-3. 상태전이와 가드 표

쉽게 말하면: 상태가 오갈 수 있는 길만 적고, 나머지 길은 전부 막혀 있다고 명시한다.

#### 예약 상태 (BookingStatus)

| From | Trigger | Guard | To | Side-Effect |
|---|---|---|---|---|
| HELD | 결제 승인 시 예약 확정 정책 | 잠금 후 상태가 HELD | CONFIRMED | BookingConfirmed 발행 + 확정 이동 |
| HELD | TTL 만료 정책 (스케줄러) | 잠금 후 상태가 HELD, expiresAt 경과 | EXPIRED | BookingExpired 발행 + 선점 반환 |
| HELD | 결제 실패 시 만료 정책 | 잠금 후 상태가 HELD, 이벤트 탑재 시도 수 3 이상 | EXPIRED | BookingExpired 발행 + 선점 반환 |
| CONFIRMED | 이용자의 취소 | 잠금 후 상태가 CONFIRMED | CANCELED | BookingCanceled 발행 + 판매분 반환과 환불 |

금지 전이: CONFIRMED에서 EXPIRED로, HELD에서 CANCELED로, 그리고 EXPIRED와 CANCELED에서 나가는 모든 전이. 재호출 무해: 각 종착 상태로의 같은 요청 반복.

#### 결제 시도 상태 (PaymentAttempt)

| From | Trigger | Guard | To | Side-Effect |
|---|---|---|---|---|
| (생성) | 결제 요청 | 진행 유일, 시도 상한, 승인 유일 | REQUESTED | PaymentRequested 발행 |
| REQUESTED | 승인 콜백 | 거래번호 신규 | APPROVED | PaymentApproved 발행 |
| REQUESTED | 실패 콜백 | 거래번호 신규 | FAILED | PaymentFailed 발행 |
| APPROVED | 환불 | 없음 | REFUNDED | PaymentRefunded 발행 |

금지 전이: 모든 역행. FAILED는 종착이고 재시도는 새 시도 생성이다. Payment 자체는 저장 상태 없이 시도 목록에서 계산한다.
알려진 갇힘: REQUESTED에서 나가는 길은 콜백 둘뿐이라, 콜백이 영영 오지 않으면 시도가 REQUESTED에 머물고 진행 유일(I9)이 새 시도까지 막는다. 시간 상한 도입 판정 전까지는 수동 개입 대상이다 (Step 8 이월, 3절).

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
| 원자성 조율 | 도메인 서비스 | 연박 원자성 (InventoryAllocationService) |
| 유일성과 무결성 | DB | 1-1의 U 계열 유니크 제약 |

## 2. Step 2: 정책 추가, 자동 규칙 발견

### 2-1. 이벤트에서 커맨드로의 트리거 맵핑

쉽게 말하면: 어떤 사건이 사람 손 없이 다음 행동을 부르는가. 22개 이벤트 중 다섯과 시간 트리거 하나다. 나머지 17개 중 11개는 검색 화면 갱신에만 쓰인다(2-3).

| 이벤트 | 자동으로 유발하는 행동 |
|---|---|
| PaymentApproved (결제 승인됨) | 예약 확정, 또는 이미 만료됐으면 환불 (가드로 분기) |
| PaymentFailed (결제 실패됨) | 3회째면 예약 만료 |
| BookingConfirmed (예약 확정됨) | 선점을 판매로 확정 |
| BookingExpired (예약 만료됨) | 선점 재고 반환 |
| BookingCanceled (예약 취소됨) | 환불과 판매분 재고 반환 |
| 시간 (스케줄러가 기한 지난 HELD를 폴링) | 예약 만료 |

예약발 세 행(BookingConfirmed, BookingExpired, BookingCanceled의 재고 행동)은 개념상 대응이고, 구현은 이벤트 구독이 아니라 같은 트랜잭션 안의 호출이다 (2-2, 2-3).

### 2-2. 정책 카드

쉽게 말하면: 자동 반응 규칙 카드다. 언제(트리거), 어떤 조건이면(가드), 무엇을 하고(행동), 얼마나 빨리 맞추며(일관성), 같은 사건이 두 번 와도 왜 안전한가(멱등 전략). 가드는 전부 확정이고, 모든 정책은 중간 상태를 보관하지 않는다(Stateless).

| 정책 이름 | 트리거 | 가드 | 행동 | 일관성 | 멱등 전략 |
|---|---|---|---|---|---|
| 결제 승인 시 예약 확정 | PaymentApproved | 예약 상태가 HELD | 예약 확정 + 확정 이동 | 결과적 (비동기, 별도 트랜잭션) | 콜백 멱등(U4)이 재발행 차단 + 확정 재호출 무해 |
| 승인 지연 시 자동 환불 | PaymentApproved | 예약 상태가 EXPIRED | 환불 | 결과적 (비동기) | 콜백 멱등 + 환불 재호출 무해 |
| 확정 시 재고 확정 | 확정 트랜잭션 내부 호출 (BookingConfirmed는 커밋 후 발행되는 기록이며 구독 대상이 아님) | 없음 | 선점을 판매로 이동 (N일) | 강한 (동기, 같은 트랜잭션. 이탈 선언) | 불필요 (동기 단일 실행. confirm 무해 경로에서는 호출 자체가 생략됨) |
| 결제 실패 시 만료 | PaymentFailed | 시도 수 3 이상 (이벤트 탑재값) | 예약 만료 (원인: 결제 실패) | 결과적 (비동기) | 콜백 멱등 + 만료 재호출 무해 |
| 만료 시 재고 반환 | 만료 트랜잭션 내부 호출 (BookingExpired는 커밋 후 발행되는 기록) | 없음 | 선점 재고 반환 (N일) | 강한 (동기, 같은 트랜잭션) | 불필요 (expire 무해 경로에서는 호출 생략) |
| 취소 시 환불 | BookingCanceled | 없음 | 환불 | 결과적 (비동기) | 환불 재호출 무해 |
| 취소 시 재고 반환 | 취소 트랜잭션 내부 호출 (BookingCanceled는 커밋 후 발행되는 기록) | 없음 | 판매분 재고 반환 (N일) | 강한 (동기, 같은 트랜잭션) | 불필요 (cancel 무해 경로에서는 호출 생략) |
| TTL 만료 | 시간 (스케줄러 폴링) | 상태가 HELD이고 기한 경과 | 예약 만료 (원인: TTL) | 내부 (동기. 만료 트랜잭션 그 자체) | 만료 재호출 무해 |

결제 승인 시 예약 확정과 승인 지연 시 자동 환불은 같은 트리거를 가드로 분기한다. 가드가 상호 배타(HELD와 EXPIRED)라 둘 다 발동하는 일은 없고, CONFIRMED나 CANCELED에 도착한 승인에는 둘 다 침묵한다.

### 2-3. 정책 분류와 소속

쉽게 말하면: 즉시 맞출 것과 나중에 맞춰도 되는 것을 가른다.

| 분류 | 해당 정책 | 처리 |
|---|---|---|
| 내부 정책 (같은 컨텍스트 트리거) | TTL 만료 | 동기, 강한 일관성 |
| 외부 정책이지만 강한 일관성으로 앞당김 (이탈 선언) | 확정 시 재고 확정, 만료 시 재고 반환, 취소 시 재고 반환 | 동기, 같은 트랜잭션. 앱 서비스의 호출로 구현 (이벤트 구독 아님) |
| 외부 정책 (자료 원칙대로) | 결제 승인 시 예약 확정, 승인 지연 시 자동 환불, 결제 실패 시 만료, 취소 시 환불 | 비동기, 결과적 일관성. 커밋 후 발행된 이벤트를 별도 트랜잭션에서 처리 |
| 읽기 모델 갱신 (Projection) | 카탈로그 4, 재고와 요금 4, 프로모션 3 이벤트가 검색을 갱신 | 지연 허용. 정책 카드 없음 |

정책의 소속(구독자)은 전부 예약 컨텍스트다. 결제는 예약을 모른다는 전제 때문에, 결제 이벤트를 받아 예약 상태를 보는 코드는 예약 쪽에만 있을 수 있다.

## 3. 확정, 가설, 이월

이번 단계 확정.
- 비관적 락 셋 일괄. 경로 간 잠금 교차 순환 없음(갭 락만 Step 9 테스트).
- 미개설 날짜 선점 실패. TTL 만료의 기한 재검사. 종착 재호출 무해 6곳 + 멱등 반환 1곳(0절). 무해 경로는 재고 연산과 이벤트 발행까지 생략.
- 할인 단수는 마지막 날짜 행 가산. 스냅샷 총액 저장(총액 합치, I15). 최대 인원 하향 허용. 환불은 APPROVED에서 REFUNDED로의 전이. Money 공유 커널 적합 (06-1 후보 판정에서 반올림 규칙 미정으로 가설이던 것을 단수 규칙 확정으로 해제).
- 정책 가드 전부, 일관성 유형 전부, 소속(예약), 전 정책 Stateless.

요구사항 역추적 (시스템 보장 다섯).

| 요구사항 | 대응 |
|---|---|
| R1 연박 전체 선점 실패 | A1 연박 원자성, hold(n) 계약(전체 롤백), RequestBooking Invariant |
| R2 초과 예약 0 | I1 재고 총량, I1a 수량 하한, 0절 락 선언, hold와 commit의 잠금 후 검사 |
| R3 중복 예약 방지 | U1 멱등키 유일, RequestBooking의 멱등 반환(재고 선점과 이벤트 재발행 없음) |
| R4 HELD 종료 시 재고 반환 | A2 종료 반환, expire(reason) 계약, 1-3 예약 전이표, TTL 만료와 결제 실패 시 만료 정책 |
| R5 확정 금액 불변 | I4 스냅샷 동결, I15 총액 합치, adjustRate와 프로모션 update, close의 스냅샷 무영향, openAttempt의 총액 대조 |

[가설] 잔여.
- ttl 기본 10분, 개설 상한 366일, 통화 KRW 정수. 설정값, Step 9.
- PricingService 소속(예약), 반환 원천 타입의 위치(재고 API). Step 9.

이월.
- Step 8 (자료 08 입수 시): 2절과 대조 보완, 결제 시도 타임아웃(REQUESTED 상한) 도입 여부, 사가 필요성 재확인.
- Step 8 추가 (블라인드 검증 지적): 승인 유실 창. 결제 승인 시 예약 확정과 승인 지연 시 자동 환불이 별도 핸들러로 각자 다른 시점에 가드를 평가하면, 환불 쪽이 HELD를 보고 침묵한 직후 TTL이 만료시키고 확정 쪽이 EXPIRED를 보고 거부해 승인이 환불도 확정도 없이 남는 경로가 열린다. 한 핸들러 통합(잠금 후 분기), expire 후행조건 확장(승인 이력 있으면 환불 유발), 미환불 승인 순찰 정책(T2) 중 택일을 Step 8에서 판정한다.
- Step 9: 예외 이름 확정, 비동기 이벤트 전달 메커니즘(시그니처 확인 필요), 갭 락 테스트, 설정값, 계약표의 테스트 변환(1-1 문장이 테스트 이름).
