# O2O 숙박 예약 책임-협력 지도 (Step 3)

최초 작성: 2026-08-27
최종 갱신: 2026-09-07 v9 (트랜잭션 미결 표현과 HTTP 계약의 호출 주체 정리)
자료: 04 책임 주도 설계(RDD)와 행위자-명령 매핑
입력: 03-o2o-event-storming.md 이벤트 22개
FigJam 보드: https://www.figma.com/board/ffhYVMh8awMBfqLinXFMDY 영역 3

문서 규칙: 새로 추가되거나 내용이 바뀐 절에는 제목 옆에 반영 날짜를 표기한다.

## 0. 이 단계의 질문

각 이벤트를 보며 두 가지를 묻는다.
1. 이 사건이 일어나도록 만든 직접적인 요청(커맨드)은 무엇이었나
2. 그 요청을 누가 수행했나(액터)

액터는 시스템 경계 밖에서 커맨드를 보내는 주체이고, 역할은 경계 안에서 그 커맨드를 처리할 책임자다.

## 1. 커맨드-액터-이벤트 매핑 (커맨드 22개, 2026-09-01 v7)

### 호스트 (판매 준비)

| 커맨드 | 이벤트 | 역할 후보 |
|---|---|---|
| RegisterProperty 숙소를 등록하라 | PropertyRegistered | Property |
| UpdateProperty 숙소 정보를 수정하라 | PropertyUpdated | Property |
| RegisterRoomType 객실타입을 등록하라 | RoomTypeRegistered | RoomType |
| UpdateRoomType 객실타입을 수정하라 | RoomTypeUpdated | RoomType |
| OpenInventory 기간 재고를 개설하라 | InventoryOpened | DailyInventory |
| AdjustInventory 재고를 조정하라 | InventoryAdjusted | DailyInventory |
| RegisterRate 날짜별 요금을 등록하라 | RateRegistered | DailyRate |
| AdjustRate 요금을 조정하라 | RateAdjusted | DailyRate |

### 운영자 (프로모션 정책)

| 커맨드 | 이벤트 | 역할 후보 |
|---|---|---|
| CreatePromotion 프로모션을 생성하라 | PromotionCreated | Promotion |
| UpdatePromotion 프로모션을 수정하라 | PromotionUpdated | Promotion |
| ClosePromotion 프로모션을 종료하라 | PromotionClosed | Promotion |

ClosePromotion은 운영자의 수동 종료만 다룬다. 캠페인 기간 만료에 따른 자동 종료는 v1 범위 밖이다. (2026-09-01 v5)

### 게스트 (예약, 2026-09-01 v8: RequestPayment 복귀)

| 커맨드 | 이벤트 | 역할 후보 |
|---|---|---|
| RequestBooking 예약을 요청하라 | BookingCreated | Booking |
| RequestPayment 결제를 요청하라 | PaymentRequested | Payment |
| CancelBooking 예약을 취소하라 | BookingCanceled | Booking |

CancelBooking은 게스트가 확정된 예약을 취소하는 것만 다룬다. 호스트나 운영자에 의한 취소는 v1 범위 밖이다. HELD 상태의 이탈은 TTL이 처리하므로 커맨드가 없다. (2026-09-01 v5)
RequestBooking이 내는 이벤트가 하나로 줄었다. Hold가 Booking에 흡수되어 StayHeld가 BookingCreated에 포함됐다. (v7)

RequestPayment의 경로 (v8). 게스트가 예약 컨텍스트에 보내고, 예약 컨텍스트의 애플리케이션 서비스가 PriceSnapshot 총액을 실어 결제 컨텍스트로 넘긴다. 액터는 게스트, 받는 컨텍스트는 결제, 중간에 예약이 총액을 붙인다. 첫 시도와 재시도가 같은 커맨드다.
v7에서 이것을 BookingCreated 정책의 자동 발송으로 바꿨다가 v8에서 되돌렸다. 자동 발송으로 두면 재시도가 도메인 커맨드에서 사라지는데, 재시도는 PaymentRequested를 다시 내고 attemptCount를 올리는 상태 변화이고 게스트가 경계 밖에서 요청하는 것이므로 자료 04 정의상 커맨드다. 순환 의존의 원인은 결제가 청구액을 알려고 예약을 읽는 것이었고, 그것은 예약이 총액을 실어 보내는 것으로 끊긴다. 누가 누르느냐와는 무관했다.

### 예약 컨텍스트가 다른 컨텍스트에 보내는 커맨드 (2026-09-01 v8: RequestPayment 제외)

사람도 정책도 아닌 다른 컨텍스트가 액터인 경우다. 예약의 애플리케이션 서비스가 동기로 호출한다.

| 커맨드 | 받는 컨텍스트 | 이벤트 | 역할 후보 |
|---|---|---|---|
| HoldInventory 재고를 선점하라 | 재고 | InventoryHeld | DailyInventory |
| CommitInventory 선점을 확정하라 | 재고 | InventoryCommitted | DailyInventory |
| ReleaseInventory 재고를 반환하라 | 재고 | InventoryReleased | DailyInventory |
| RefundPayment 환불을 처리하라 | 결제 | PaymentRefunded | Payment |

레인 판별 규칙 (v6): 컨텍스트 경계를 넘는 커맨드는 보내는 컨텍스트의 레인에 둔다. 자기 컨텍스트 안에서 정책이 유발하는 커맨드만 시스템 레인에 둔다. 사람이 보낸 커맨드를 컨텍스트가 중계하는 경우(RequestPayment)는 사람 레인이다.

결제가 예약을 모르는 구조 (v7, v8 유지). 예약이 총액을 실어 보내므로 결제는 예약을 읽지 않는다. 예약과 결제의 의존이 한 방향이 되고 Step 5 화살표를 그릴 수 있다.

리뷰에서 지적된 가장 큰 구멍이었다. 예약이 재고의 heldCount를 올리는데 재고 쪽에 그것을 받는 커맨드가 없었다.

### 외부 결제 (ACL이 번역한 커맨드)

| 커맨드 | 이벤트 | 역할 후보 |
|---|---|---|
| RecordPaymentApproval 결제 승인을 기록하라 | PaymentApproved | Payment |
| RecordPaymentFailure 결제 실패를 기록하라 | PaymentFailed | Payment |

### 시스템과 스케줄러 (예약 컨텍스트 안에서 정책이 유발하는 커맨드, 2026-09-01 v8: 두 레인 통합)

| 커맨드 | 이벤트 | 역할 후보 | 발신 정책 |
|---|---|---|---|
| ConfirmBooking 예약을 확정하라 | BookingConfirmed | Booking | 결제 승인 정책 |
| ExpireBooking(reason) HELD 예약을 만료시켜라 | BookingExpired | Booking | TTL 정책(스케줄러), 결제 실패 정책 |

ExpireBooking은 원인 파라미터를 받는다. TTL_EXPIRED는 스케줄러 또는 결제 처리 중 만료를 확인한 시간 정책에서 보낸다. 조건은 status == HELD와 expiresAt <= now다. PAYMENT_FAILED는 세 번째 실패 정책에서 보낸다. 이때도 HELD 상태를 확인하며, 이미 TTL이 지났다면 TTL_EXPIRED를 우선한다. 두 원인은 같은 EXPIRED 전이를 사용한다. ReleaseInventory의 원인 표현과 구체 조율 방식은 Step 6에서 정한다. (2026-09-07 보완)
스케줄러를 별도 레인으로 두었던 것은 v8에서 시스템 레인에 합쳤다. 스케줄러는 시간을 트리거로 쓰는 정책이지 다른 종류의 액터가 아니다. 액터 유형 넷(사람, 외부 시스템, 다른 컨텍스트, 정책)은 그대로다.
ExpireBooking의 선행조건 status == HELD가 CONFIRMED 예약을 보호한다. 재검증에서 지적된 확정 예약 재고 반환 결함이 여기서 막힌다.

커맨드 22개, 이벤트 22개. v7에서 RequestBooking의 이벤트가 하나가 되어 수가 맞는다.

## 2. 이 단계에서 드러난 것

### 발견 1. RequestBooking의 실제 범위 (2026-09-07)

RequestBooking이 직접 건드리는 애그리거트는 Booking 하나다. DailyInventory는 HoldInventory 커맨드를 통해 간접으로 건드린다.

[게스트] RequestBooking
  -> 예약 컨텍스트가 재고 컨텍스트에 HoldInventory x N일 호출
     -> InventoryHeld x N (재고 컨텍스트 이벤트)
  -> 전부 성공하면
     -> PriceSnapshot 저장 (생성 시점에 동결, v8 명시)
     -> BookingCreated (예약 컨텍스트 이벤트, status = HELD)
[게스트] RequestPayment -> 예약 컨텍스트가 총액을 실어 결제 컨텍스트에 전달 -> PaymentRequested

연박 3박의 일관성 보장 대상은 DailyInventory 3개와 Booking 1개다. 같은 DB 트랜잭션을 택한다면 2종 4인스턴스가 그 범위에 들어간다. 직접 수정하는 책임과 함께 성공해야 할 변경 범위를 구분한다.
같은 트랜잭션인지 별도 조율인지의 구현 방식은 아직 미결이다. 어느 방식을 택하든 외부 API에는 전 날짜 선점과 Booking 생성의 전체 성공 또는 전체 실패를 보장해야 한다.

### 발견 2. 외부 결제 커맨드는 번역해서 만든다 (2026-09-07)

Mock 결제 어댑터가 승인 또는 실패 결과를 보낸다. ACL이 이를 RecordPaymentApproval 또는 RecordPaymentFailure로 번역하고, 처리에 성공하면 내부 PaymentApproved 또는 PaymentFailed 이벤트를 발행한다. 외부 결과와 내부 이벤트를 같은 객체로 간주하지 않는다.

### 발견 3. 정책 체인 (2026-09-01 v7: Hold 흡수와 환불 가드 반영, 가설)

| 경로 | 체인 | 가드 |
|---|---|---|
| 결제 승인 | PaymentApproved -> ConfirmBooking -> CommitInventory x N | status == HELD, now < expiresAt |
| 승인 지연 | PaymentApproved -> RefundPayment | status == EXPIRED |
| 결제 실패 | PaymentFailed -> ExpireBooking(PAYMENT_FAILED) -> ReleaseInventory x N | attemptCount >= 3 |
| TTL 만료 | (스케줄러) ExpireBooking(TTL_EXPIRED) -> BookingExpired -> ReleaseInventory x N | status == HELD, expiresAt <= now |
| 확정 취소 | BookingCanceled -> RefundPayment, ReleaseInventory x N | 없음 |

v7에 있던 "예약 생성 -> RequestPayment" 자동 발송 행은 v8에서 삭제했다. RequestPayment는 게스트 커맨드다.

승인 지연 가드가 != HELD가 아니라 == EXPIRED인 이유. PG 콜백은 중복 도착이 흔하다. CONFIRMED 예약에 PaymentApproved가 다시 오면 != HELD는 참이 되어 정상 결제를 환불한다. 중복 승인은 결제 컨텍스트가 pgTransactionId로 걸러 이벤트 자체를 다시 내지 않는다.
attemptCount는 Payment 애그리거트가 세고 PaymentFailed 이벤트에 실어 보낸다.
이 표는 Step 8에서 정식 명세하기 전까지 가설이다.

### 발견 4. 검색과 조회에는 커맨드가 없다

읽기 모델로 분리한다. 단 예약 목록과 상세 조회는 검색이 아니라 예약 컨텍스트의 조회 API다. (2026-09-01 v5 수정)

### 발견 5. Promotion은 예약 흐름에서 커맨드를 받지 않는다

운영자가 보내는 셋뿐이다. 예약에서 프로모션으로 가는 의존이 읽기 전용이라는 뜻이다.

## 3. 역할(Role) 후보 7개 (2026-09-01 v7: Hold 흡수)

| 역할 후보 | 담당 커맨드 | 수 |
|---|---|---|
| Property | RegisterProperty, UpdateProperty | 2 |
| RoomType | RegisterRoomType, UpdateRoomType | 2 |
| DailyInventory | OpenInventory, AdjustInventory, HoldInventory, CommitInventory, ReleaseInventory | 5 |
| DailyRate | RegisterRate, AdjustRate | 2 |
| Promotion | CreatePromotion, UpdatePromotion, ClosePromotion | 3 |
| Booking | RequestBooking, ConfirmBooking, CancelBooking, ExpireBooking | 4 |
| Payment | RequestPayment, RecordPaymentApproval, RecordPaymentFailure, RefundPayment | 4 |

DailyInventory가 5개로 가장 많다.
Payment의 식별 단위(예약당 하나인지 시도당 하나인지)는 미정이다. attemptCount와 "예약당 승인 하나"를 Payment 불변식으로 두려면 예약당 하나여야 한다. Step 6 입력으로 01-o2o-ddd-plan.md 5절 쟁점 5에 적었다. (v8) 재고 점유 커맨드가 들어가면서 이 역할이 동시성의 중심임이 커맨드 수로도 드러난다.

## 4. 다음 단계 (2026-09-01 v6)
Step 4 결과는 둘로 나뉜다. 바운디드 컨텍스트 명세와 커맨드 배분은 FigJam 영역 4, 용어는 05-o2o-glossary.md.

## 5. API 호출 주체와 내부 커맨드 (2026-09-07)

| API 동작 | 호출 주체 | 처리 |
|---|---|---|
| 숙소, 객실, 재고와 요금 변경 | 소유 HOST | 카탈로그 또는 재고 컨텍스트 |
| 프로모션 변경과 수동 종료 | OPERATOR | CreatePromotion, UpdatePromotion, ClosePromotion |
| 예약 생성 | GUEST | RequestBooking, 내부 HoldInventory |
| 결제 시도 접수 | 소유 GUEST | 예약이 총액을 붙여 RequestPayment 중계 |
| 결제 결과 전달 | 로컬 MOCK_SYSTEM | ACL을 거쳐 승인 또는 실패 기록 |
| 예약 취소 | 소유 GUEST | CancelBooking, 내부 환불과 재고 반환 |
| TTL 만료 | 시간 정책 | ExpireBooking, 내부 재고 반환 |

ConfirmBooking, ReleaseInventory, RefundPayment는 프론트용 독립 API가 아니다. 프로모션의 enabled=false는 수동 종료 의도를 전달하는 API 초안이며, 전체 커맨드를 일반 필드 수정 하나로 합치라는 뜻은 아니다. 결제와 TTL이 경합할 때는 서버 현재 시각과 예약 상태를 함께 검사한다.
