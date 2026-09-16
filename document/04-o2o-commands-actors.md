# O2O 숙박 예약 책임-협력 지도 (Step 3)

최초 작성: 2026-08-27
최종 갱신: 2026-09-08 v10 (08-3 결정 반영: SettlePayment로 커맨드 23, RefundPayment 시그니처, 발견 3 체인표를 04-5 v2 신 정책 번호로, ConfirmBooking 발신 정책에 T1과 T2)
자료: 04 책임 주도 설계(RDD)와 행위자-명령 매핑
입력: 03-o2o-event-storming.md 이벤트 22개, 08-3 6절 C-1과 C-11
FigJam 보드: https://www.figma.com/board/ffhYVMh8awMBfqLinXFMDY 영역 3

문서 규칙: 새로 추가되거나 내용이 바뀐 절에는 제목 옆에 반영 날짜를 표기한다.

## 0. 이 단계의 질문

각 이벤트를 보며 두 가지를 묻는다.
1. 이 사건이 일어나도록 만든 직접적인 요청(커맨드)은 무엇이었나
2. 그 요청을 누가 수행했나(액터)

액터는 시스템 경계 밖에서 커맨드를 보내는 주체이고, 역할은 경계 안에서 그 커맨드를 처리할 책임자다.

## 1. 커맨드-액터-이벤트 매핑 (커맨드 23개, 2026-09-08 v10)

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

ClosePromotion은 운영자의 수동 종료만 다룬다. 캠페인 기간 만료에 따른 자동 종료는 v1 범위 밖이다.

### 게스트 (예약)

| 커맨드 | 이벤트 | 역할 후보 |
|---|---|---|
| RequestBooking 예약을 요청하라 | BookingCreated | Booking |
| RequestPayment 결제를 요청하라 | PaymentRequested | Payment |
| CancelBooking 예약을 취소하라 | BookingCanceled | Booking |

CancelBooking은 게스트가 확정된 예약을 취소하는 것만 다룬다. 호스트나 운영자에 의한 취소는 v1 범위 밖이다. HELD 상태의 이탈은 TTL이 처리하므로 커맨드가 없다.

RequestBooking이 내는 이벤트가 하나로 줄었다. Hold가 Booking에 흡수되어 StayHeld가 BookingCreated에 포함됐다.

RequestPayment의 경로. 게스트가 예약 컨텍스트에 보내고, 예약 컨텍스트의 애플리케이션 서비스가 PriceSnapshot 총액을 실어 결제 컨텍스트로 넘긴다. 액터는 게스트, 받는 컨텍스트는 결제, 중간에 예약이 총액을 붙인다. 첫 시도와 재시도가 같은 커맨드다.

v7에서 이것을 BookingCreated 정책의 자동 발송으로 바꿨다가 v8에서 되돌렸다. 자동 발송으로 두면 재시도가 도메인 커맨드에서 사라지는데, 재시도는 PaymentRequested를 다시 내고 attemptCount를 올리는 상태 변화이고 게스트가 경계 밖에서 요청하는 것이므로 자료 04 정의상 커맨드다. 순환 의존의 원인은 결제가 청구액을 알려고 예약을 읽는 것이었고, 그것은 예약이 총액을 실어 보내는 것으로 끊긴다.

### 예약 컨텍스트가 다른 컨텍스트에 보내는 커맨드 (2026-09-08 v10: SettlePayment 추가)

사람도 정책도 아닌 다른 컨텍스트가 액터인 경우다. 예약의 애플리케이션 서비스가 동기로 호출한다.

| 커맨드 | 받는 컨텍스트 | 이벤트 | 역할 후보 |
|---|---|---|---|
| HoldInventory 재고를 선점하라 | 재고 | InventoryHeld | DailyInventory |
| CommitInventory 선점을 확정하라 | 재고 | InventoryCommitted | DailyInventory |
| ReleaseInventory 재고를 반환하라 | 재고 | InventoryReleased | DailyInventory |
| RefundPayment(bookingId) 환불을 처리하라 | 결제 | PaymentRefunded | Payment |
| SettlePayment(bookingId, settledBy) 후속 없음을 표시하라 | 결제 | 없음 (표식) | Payment |

SettlePayment는 이벤트를 내지 않는 유일한 커맨드다. 이 지도에서 이벤트 칸이 비는 행이 처음 생겼다.

근거는 성격이다. 이 커맨드는 예약 컨텍스트가 이 결제에 대해 할 일을 끝냈다는 처리 완료 표식이지 도메인에서 일어난 사건이 아니다. 이벤트로 내면 아무도 구독하지 않는 이벤트가 하나 더 생기고, 구독자 없는 이벤트 목록이 그만큼 늘어난다.

RefundPayment의 인자는 bookingId다. PG 멱등키는 결제 컨텍스트가 그 시도의 attemptId에서 유도하므로 예약이 키를 만들어 넘기지 않는다.

레인 판별 규칙: 컨텍스트 경계를 넘는 커맨드는 보내는 컨텍스트의 레인에 둔다. 자기 컨텍스트 안에서 정책이 유발하는 커맨드만 시스템 레인에 둔다. 사람이 보낸 커맨드를 컨텍스트가 중계하는 경우는 사람 레인이다.

결제가 예약을 모르는 구조는 유지된다. 예약이 총액을 실어 보내므로 결제는 예약을 읽지 않는다. v10이 추가한 SettlePayment와 조회 둘도 방향이 예약에서 결제라 순환이 생기지 않는다.

### 외부 결제 (ACL이 번역한 커맨드)

| 커맨드 | 이벤트 | 역할 후보 |
|---|---|---|
| RecordPaymentApproval 결제 승인을 기록하라 | PaymentApproved 또는 없음 | Payment |
| RecordPaymentFailure 결제 실패를 기록하라 | PaymentFailed 또는 없음 | Payment |

두 커맨드의 이벤트 칸에 또는 없음이 붙는 이유는 세 가지다. 같은 pgTransactionId의 중복 콜백은 무시하고 이벤트를 내지 않는다. 고아 승인으로 판정되면 PaymentApproved 대신 ORPHAN 행 기록과 환불이 일어난다. 실패는 3회째에만 PaymentFailed를 낸다. (2026-09-08 v10)

### 시스템과 스케줄러 (예약 컨텍스트 안에서 정책이 유발하는 커맨드, 2026-09-08 v10: 발신 정책 갱신)

| 커맨드 | 이벤트 | 역할 후보 | 발신 정책 |
|---|---|---|---|
| ConfirmBooking 예약을 확정하라 | BookingConfirmed | Booking | P1 결제 승인 처리, T1 TTL 만료, T2 후속 미완 결제 순찰 |
| ExpireBooking(reason) HELD 예약을 만료시켜라 | BookingExpired | Booking | T1 TTL 만료, P3 결제 실패 시 만료, T2 순찰 |

ConfirmBooking의 발신 정책이 셋으로 늘었다. T1이 확정을 보내는 이유는 확정 우선이다. TTL에 도달했더라도 미정산 승인이 있으면 만료가 아니라 확정으로 간다. T2가 보내는 것은 P1 이벤트가 유실된 경우의 보정이다.

ExpireBooking은 원인 파라미터를 받는다. TTL_EXPIRED는 T1이 보내고 조건은 상태가 HELD이고 expiresAt이 지났으며 미정산 승인이 없는 것이다. PAYMENT_FAILED는 P3가 보내고 조건은 상태가 HELD이고 이벤트에 실린 시도 수가 3 이상인 것이다. 두 원인은 같은 EXPIRED 전이를 사용한다.

세 정책이 같은 커맨드를 보내지만 어느 정책도 상태를 스스로 판정하지 않는다. 앱 서비스가 Booking을 잠근 뒤 상태를 읽고 그 상태에서 허용되는 메서드만 부른다. CONFIRMED나 CANCELED를 만나면 ExpireBooking을 보내지 않고 로그 후 SettlePayment로 끝낸다. (2026-09-08 v10, 08-3 C-8)

ExpireBooking의 선행조건인 상태가 HELD라는 것이 CONFIRMED 예약을 보호한다.

커맨드 23개, 이벤트 22개. v10에서 SettlePayment가 들어와 커맨드가 하나 늘었고 이벤트는 늘지 않았다.

## 2. 이 단계에서 드러난 것

### 발견 1. RequestBooking의 실제 범위

RequestBooking이 직접 건드리는 애그리거트는 Booking 하나다. DailyInventory는 HoldInventory 커맨드를 통해 간접으로 건드린다.

게스트가 RequestBooking을 보내면 예약 컨텍스트가 재고 컨텍스트에 HoldInventory를 N일치 호출하고, 재고가 InventoryHeld를 N개 낸다. 전부 성공하면 PriceSnapshot을 생성 시점에 동결해 저장하고 BookingCreated를 낸다. 상태는 HELD다. 그다음 게스트가 RequestPayment를 보내면 예약 컨텍스트가 총액을 실어 결제 컨텍스트에 전달하고 PaymentRequested가 난다.

연박 3박의 일관성 보장 대상은 DailyInventory 3개와 Booking 1개다. 같은 DB 트랜잭션을 택하므로 2종 4인스턴스가 그 범위에 들어간다. 직접 수정하는 책임과 함께 성공해야 할 변경 범위를 구분한다.

같은 트랜잭션인지 별도 조율인지는 06-2 v4에서 같은 트랜잭션으로 확정됐다. 외부 API에는 전 날짜 선점과 Booking 생성의 전체 성공 또는 전체 실패를 보장한다.

### 발견 2. 외부 결제 커맨드는 번역해서 만든다

Mock 결제 어댑터가 승인 또는 실패 결과를 보낸다. ACL이 이를 RecordPaymentApproval 또는 RecordPaymentFailure로 번역하고, 처리에 성공하면 내부 PaymentApproved 또는 PaymentFailed 이벤트를 발행한다. 외부 결과와 내부 이벤트를 같은 객체로 간주하지 않는다.

콜백 HTTP 응답은 처리 결과와 무관하게 항상 2xx다. PG 재전송으로 복구되는 경우가 없기 때문이다. 복구는 순찰 정책이 한다. (2026-09-08 v10, 08-3 C-3)

### 발견 3. 정책 체인 (2026-09-08 v10: 04-5 v2 신 번호와 08-3 v2.1 반영. 확정)

| 정책 | 체인 | 가드 |
|---|---|---|
| P1 결제 승인 처리 | PaymentApproved에서 상태 분기. HELD면 ConfirmBooking과 CommitInventory N회와 SettlePayment. EXPIRED면 RefundPayment와 SettlePayment. CONFIRMED나 CANCELED면 SettlePayment | Booking 잠금 후 상태 |
| P3 결제 실패 시 만료 | PaymentFailed에서 ExpireBooking(PAYMENT_FAILED)와 ReleaseInventory N회와 SettlePayment | 이벤트에 실린 시도 수가 3 이상, 상태가 HELD |
| P5 취소 시 환불 | 취소 트랜잭션 안에서 RefundPayment와 SettlePayment(CANCEL). 재고 반환보다 먼저 | 없음 |
| T1 TTL 만료 | 미정산 승인이 있으면 ConfirmBooking 경로. 없으면 ExpireBooking(TTL_EXPIRED)와 ReleaseInventory N회와 SettlePayment | 상태가 HELD, expiresAt 경과 |
| T2 후속 미완 결제 순찰 | 조회 조건별로 위 네 경로 중 하나를 재실행하고 모든 분기가 SettlePayment로 끝난다 | 정산 표식 없음 또는 환불 대기 고아 |

v7에 있던 예약 생성에서 RequestPayment로 가는 자동 발송 행은 v8에서 삭제했다. RequestPayment는 게스트 커맨드다.

v9까지 있던 승인 지연 행은 v10에서 P1 안으로 합쳤다. 승인 처리와 승인 지연 환불을 별도 핸들러로 두면 둘이 각자 다른 시점에 상태를 읽어 승인이 환불도 확정도 없이 남는 창이 열린다. 한 핸들러가 잠근 뒤 분기한다. (08-3 뿌리 결정)

이 표는 v10에서 확정이다. Step 8이 종결됐다.

attemptCount는 Payment 애그리거트가 NORMAL 시도만 세고 PaymentFailed 이벤트에 실어 보낸다.

### 발견 4. 검색과 조회에는 커맨드가 없다

읽기 모델로 분리한다. 단 예약 목록과 상세 조회는 검색이 아니라 예약 컨텍스트의 조회 API다.

예약이 결제에 보내는 paymentFollowUp과 findFollowUpCandidates도 커맨드가 아니라 조회다. 상태를 바꾸지 않으므로 이 지도의 커맨드 수에 넣지 않는다. 06-1 v2 R6에 흐름으로 적혀 있다. (2026-09-08 v10)

### 발견 5. Promotion은 예약 흐름에서 커맨드를 받지 않는다

운영자가 보내는 셋뿐이다. 예약에서 프로모션으로 가는 의존이 읽기 전용이라는 뜻이다.

## 3. 역할 후보 7개 (2026-09-08 v10: Payment 5개)

| 역할 후보 | 담당 커맨드 | 수 |
|---|---|---|
| Property | RegisterProperty, UpdateProperty | 2 |
| RoomType | RegisterRoomType, UpdateRoomType | 2 |
| DailyInventory | OpenInventory, AdjustInventory, HoldInventory, CommitInventory, ReleaseInventory | 5 |
| DailyRate | RegisterRate, AdjustRate | 2 |
| Promotion | CreatePromotion, UpdatePromotion, ClosePromotion | 3 |
| Booking | RequestBooking, ConfirmBooking, CancelBooking, ExpireBooking | 4 |
| Payment | RequestPayment, RecordPaymentApproval, RecordPaymentFailure, RefundPayment, SettlePayment | 5 |

DailyInventory와 Payment가 5개로 가장 많다.

Payment의 식별 단위는 예약당 하나로 06-2 v4에서 확정됐다. attemptCount와 예약당 승인 하나를 Payment 불변식으로 두려면 예약당 하나여야 하기 때문이다. 재고 점유 커맨드가 들어가면서 이 역할이 동시성의 중심임이 커맨드 수로도 드러난다.

## 4. 다음 단계

Step 4 결과는 둘로 나뉜다. 바운디드 컨텍스트 명세와 커맨드 배분은 FigJam 영역 4, 용어는 05-3-o2o-glossary.md다.

## 5. API 호출 주체와 내부 커맨드 (2026-09-08 v10: 정산 표식 행)

| API 동작 | 호출 주체 | 처리 |
|---|---|---|
| 숙소, 객실, 재고와 요금 변경 | 소유 HOST | 카탈로그 또는 재고 컨텍스트 |
| 프로모션 변경과 수동 종료 | OPERATOR | CreatePromotion, UpdatePromotion, ClosePromotion |
| 예약 생성 | GUEST | RequestBooking, 내부 HoldInventory |
| 결제 시도 접수 | 소유 GUEST | 예약이 총액을 붙여 RequestPayment 중계 |
| 결제 결과 전달 | 로컬 MOCK_SYSTEM | ACL을 거쳐 승인 또는 실패 기록 |
| 예약 취소 | 소유 GUEST | CancelBooking, 내부 환불과 정산 표식과 재고 반환 |
| TTL 만료 | 시간 정책 T1 | 확정 또는 ExpireBooking, 내부 재고 반환과 정산 표식 |
| 후속 미완 결제 보정 | 시간 정책 T2 | 조회 조건별 분기, 내부 정산 표식 |

ConfirmBooking, ReleaseInventory, RefundPayment, SettlePayment는 프론트용 독립 API가 아니다. 프로모션의 enabled를 false로 두는 것은 수동 종료 의도를 전달하는 API 초안이며, 전체 커맨드를 일반 필드 수정 하나로 합치라는 뜻은 아니다. 결제와 TTL이 경합할 때는 서버 현재 시각과 예약 상태와 결제의 정산 표식을 함께 검사한다.

## 6. 반영하지 않은 것 (2026-09-08 v10)

| 항목 | 이유 |
|---|---|
| 게스트를 이용자로 개명 | 작업 계약 task-S8의 반영 대상 표가 이 문서에 대해 지정한 것은 커맨드 23, SettlePayment, RefundPayment 시그니처, 발견 3 체인표, ConfirmBooking 발신 정책 다섯이다. 액터 개명은 08-3 반영 목록이 아니므로 범위를 넓히지 않았다 |
| InventoryHoldRejected | 09-1 보드 v2가 project-sync에 반입되지 않았다. 이벤트 22를 유지한다 |

이 판은 2026-09-09 사용자 최종 완료 확정을 받은 확정본이다. task-S8 08-3 반영과 R1 평가를 담는다.
