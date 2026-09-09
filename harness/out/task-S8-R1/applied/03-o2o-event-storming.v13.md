# O2O 숙박 예약 이벤트 스토밍

최초 작성: 2026-08-27
최종 갱신: 2026-09-09 v13 (task-S8 08-3 결정 반영. 결제 이벤트 페이로드 절 신설, 5-3 종료 조건과 경합 처리를 C-6과 C-7로 교체, 6절 Payment 행에 시도 종류. v12의 09-1 델타 판정 위에서 만들었다)
단계: Big Picture 완료, Step 2.5 엔티티별 그룹핑 완료
입력: 02-o2o-feature-list.md, harness/project-sync/09-1-o2o-board-v2.md 10절, harness/decisions/decisions-09-1.md, 08-3 6절 C-6과 C-7과 C-11, harness/decisions/decisions-08-3.md
후속: 04-o2o-commands-actors.md
FigJam 보드: https://www.figma.com/board/ffhYVMh8awMBfqLinXFMDY

문서 규칙: 새로 추가되거나 내용이 바뀐 절에는 제목 옆에 반영 날짜를 표기한다.

## 1. 도메인 이벤트 목록 (22개, 2026-09-01 v9: Hold 흡수로 24개에서 22개. 2026-09-08 v12: 09-1 보드 v2 델타 판정 후에도 22개 유지)

### 판매 준비 (액터: 호스트)

| 이벤트 | 의미 |
|---|---|
| PropertyRegistered | 숙소가 등록됨 |
| PropertyUpdated | 숙소 정보가 수정됨 |
| RoomTypeRegistered | 객실 타입이 등록됨 |
| RoomTypeUpdated | 객실 타입이 수정됨 |
| InventoryOpened | 특정 기간의 판매 재고가 개설됨 |
| InventoryAdjusted | 재고 수량이 조정됨 |
| RateRegistered | 날짜별 요금이 등록됨 |
| RateAdjusted | 호스트가 등록된 요금을 수동으로 조정함 |

### 프로모션 정책 (액터: 운영자)

| 이벤트 | 의미 |
|---|---|
| PromotionCreated | 프로모션 정책이 생성됨 |
| PromotionUpdated | 프로모션 정책이 수정됨 |
| PromotionClosed | 프로모션이 종료됨 |

### 재고 점유 (액터: 예약 컨텍스트가 커맨드로 요청, 2026-09-01 v7 추가)

| 이벤트 | 의미 |
|---|---|
| InventoryHeld | 특정 날짜의 재고가 선점 수에 반영됨 |
| InventoryCommitted | 선점 수가 판매 수로 옮겨져 확정됨 |
| InventoryReleased | 점유된 재고가 반환됨 |

리뷰에서 지적된 구멍이다. 예약이 재고를 선점하는데 재고 컨텍스트에 그것을 받는 이벤트가 없었다. 재고 수량은 재고 컨텍스트만 바꾼다는 원칙을 지키려면, 예약은 재고에 커맨드를 보내고 재고가 자기 이벤트를 발행해야 한다.

### 예약 (액터: 게스트, 정책, 스케줄러. 2026-09-01 v9: Hold 흡수)

| 이벤트 | 의미 |
|---|---|
| BookingCreated | 예약이 HELD 상태로 생성됨. 숙박 기간 전체의 재고 선점이 성공했다는 뜻을 포함한다 |
| BookingConfirmed | 예약이 확정됨 |
| BookingCanceled | 확정된 예약이 취소됨 |
| BookingExpired | HELD 예약이 종료됨. TTL 만료 또는 결제 실패 한도 도달 |

v9에서 바뀐 것. 재검증에서 확정된 예약의 Hold를 닫는 전이가 없어 TTL 스케줄러가 확정 예약의 재고까지 반환하는 결함이 발견됐다. Hold를 Booking에 흡수하는 것으로 결정했고 그 결과 이벤트 셋이 사라지고 하나가 생겼다.

- StayHeld는 BookingCreated와 같은 사건이다. 3박이면 InventoryHeld가 세 번, 셋 다 성공했을 때 BookingCreated가 한 번이다
- HoldExpired와 HoldReleased는 BookingExpired 하나로 합쳐졌다
- BookingStatus는 HELD, CONFIRMED, CANCELED, EXPIRED 넷 그대로다. CONFIRMED는 EXPIRED로 갈 수 없으므로 확정 예약의 재고가 반환되는 경로가 구조적으로 막힌다

이 셋은 구독자가 없다. 후속 재고 행동이 같은 트랜잭션 안의 호출이기 때문이다. 구독자 없는 이벤트는 20개다. (2026-09-08 v12)

### 결제 (액터: 게스트가 요청하고 예약 컨텍스트가 중계, 외부 PG가 결과 통보. 2026-09-01 v10)

| 이벤트 | 의미 |
|---|---|
| PaymentRequested | 게스트의 요청을 예약 컨텍스트가 스냅샷 총액을 붙여 결제에 전달함. 첫 시도와 재시도 모두 |
| PaymentApproved | 정상 시도가 승인됨. 고아 승인은 이 이벤트를 내지 않는다 |
| PaymentFailed | 정상 시도가 세 번째로 실패함. 1회와 2회 실패는 이벤트를 내지 않는다 |
| PaymentRefunded | 환불이 완료됨. 정상 환불과 고아 환불을 종류로 구분한다 |

결제가 청구액을 알기 위해 예약을 읽으면 예약과 결제가 서로 상류가 된다. 예약이 총액을 실어 보내면 결제는 예약을 모른다. 액터는 여전히 게스트다.

### 결제 이벤트 페이로드 (2026-09-09 v13 신설)

| 이벤트 | 페이로드 | 주의 |
|---|---|---|
| PaymentRequested | paymentId, bookingId, attemptId, 청구액 | |
| PaymentApproved | paymentId, bookingId, attemptId, pgTransactionId, attemptCount | 뒤 셋은 승인 처리 정책이 분기에 쓰지 않는다. 로그와 추적용이다. 분기의 입력은 Booking 상태다 |
| PaymentFailed | paymentId, bookingId, attemptId, attemptCount | attemptCount는 결제 실패 시 만료 정책의 가드가 쓴다 |
| PaymentRefunded | paymentId, bookingId, kind(NORMAL 또는 ORPHAN), 환불액 | kind로 정상 환불과 고아 환불을 가른다 |

예약 이벤트의 페이로드에서 날짜는 날짜별 수량이 아니라 날짜 목록이다. 예약 한 건의 객실 수가 1이기 때문이다.

## 2. 타임라인 (2026-09-07: 등록과 조회 구분)

호스트는 PropertyRegistered, RoomTypeRegistered, InventoryOpened, RateRegistered 순으로 간다.

운영자는 PromotionCreated 뒤에 필요하면 PromotionUpdated 또는 PromotionClosed를 낸다. 예약 흐름에서 프로모션을 조회하는 행위에는 상태 변경과 새 이벤트가 없다.

게스트와 재고와 결제는 탐색(이벤트 없음), InventoryHeld N회, BookingCreated(스냅샷 동결), PaymentRequested(재시도 포함) 순이다.

외부 결제의 분기는 넷이다. (2026-09-09 v13: 확정 우선 반영)

| 도착 | 예약 상태 | 결과 |
|---|---|---|
| PaymentApproved | HELD | BookingConfirmed (PIVOTAL), InventoryCommitted N회, 정산 표식 |
| PaymentApproved | EXPIRED | PaymentRefunded, 정산 표식 |
| PaymentApproved | CONFIRMED 또는 CANCELED | 정산 표식만 |
| PaymentFailed | HELD, 3회 도달 | BookingExpired, InventoryReleased N회, 정산 표식 |

확정 이후에는 BookingConfirmed에서 BookingCanceled로 가고 PaymentRefunded와 InventoryReleased N회가 따른다. 체크인 이후는 미정이다(핫스팟 1).

시간 트리거는 둘이다. TTL이 지난 HELD를 처리하는 T1과, 후속이 남은 결제를 찾는 T2다. T1은 미정산 승인이 있으면 BookingConfirmed로, 없으면 BookingExpired로 간다. (2026-09-08 v12)

## 3. Pivotal Event

### BookingConfirmed (1순위)

| 항목 | 이전 | 이후 |
|---|---|---|
| 재고 | 선점 수에 있음 | 판매 수로 이동 |
| 금액 | 생성 시점 스냅샷 | 같은 스냅샷이 청구액이 됨 |
| 되돌리는 방법 | 방치하면 자동 소멸 | 명시적 취소와 환불 필요 |
| 취소 커맨드 | 없음 (TTL이 처리) | CancelBooking |

재고 행이 v7에서 바뀌었다. 재고 컨텍스트 용어로는 heldCount와 soldCount 사이의 이동이다. InventoryCommitted가 이 이동을 뜻한다.

### BookingCreated (2순위, v9: StayHeld에서 이름 변경)

이 이벤트 전에는 재고가 누구의 것도 아니고, 이후에는 특정 게스트의 예약이 붙잡고 있으며 TTL 시계가 돌기 시작한다.

## 4. 이벤트 밀도와 액터 전환으로 본 컨텍스트 경계 후보 (2026-09-07)

| 구간 | 액터 | 시간 간격 | 성격 |
|---|---|---|---|
| PropertyRegistered부터 RateRegistered까지 | 호스트 | 며칠 단위 | 판매 준비. 운영 중 재고 조정과 예약 선점은 경합 가능 |
| PromotionCreated부터 PromotionClosed까지 | 운영자 | 독립 흐름 | 정책 |
| 탐색 구간 | 게스트 | 초 단위 반복 | 이벤트 없음 |
| InventoryHeld부터 BookingConfirmed까지 | 게스트와 시스템 | 분 단위 밀집 | 동시성 핵심 |
| PaymentApproved와 PaymentFailed | 외부 결제사 | 초에서 분 | 외부 경계 |
| BookingExpired | 시간 또는 결제 실패 정책 | TTL 경과 또는 3회 실패 | 아무도 요청하지 않음 |

첫째, 탐색 구간에 도메인 이벤트가 하나도 없다. 검색이 도메인 모델이 아니라 읽기 모델이라는 증거다.
둘째, BookingExpired는 사람이 요청하지 않는다. 시간이나 정책이 트리거라 별도 메커니즘이 필요하다.
셋째, 준비 구간과 예약 구간은 시간 간격의 차원이 다르다.
넷째, 프로모션은 운영자가 만들고 게스트 흐름에서는 읽히기만 한다.

## 5. 확정된 결정

### 5-1. 쿠폰은 v1에서 제외 (2026-09-01, v7에서 성격 재분류)

이것은 학습 범위 결정이다. 동시성 설계를 재고 한 곳에 집중시켜 학습 밀도를 높이려는 것이고, 도메인 근거는 부차적이다. 상세는 02 3-2에 있다.

### 5-2. 동사 표기 규칙 (2026-08-27, v7에서 확장. 2026-09-09 v13: Settle 추가)

| 영어 | 한국어 | 대상 |
|---|---|---|
| Register | 등록 | 없던 것을 새로 만듦 (숙소, 객실타입, 요금) |
| Create | 생성 | 없던 것을 새로 만듦 (프로모션, 예약) |
| Open | 개설 | 기간 단위로 재고를 펼침 |
| Update | 수정 | 서술적 속성을 고침 |
| Adjust | 조정 | 수량과 금액을 고침 |
| Hold | 선점 | 재고를 임시로 붙잡음 |
| Commit | 확정 (재고) | 선점 수를 판매 수로 옮김 |
| Confirm | 확정 (예약) | 결제 승인으로 예약을 성립시킴 |
| Release | 반환 | 재고의 점유 수를 내림 |
| Expire | 만료 | HELD 예약을 종료함. TTL 경과 또는 결제 실패 한도 |
| Close | 종료 | 프로모션을 닫음 |
| Cancel | 취소 | 확정된 예약을 무름 |
| Settle | 정산 표시 | 예약이 이 결제에 대해 할 후속이 없음을 표시함. 회계상 정산이 아니다 |

리뷰에서 Release가 재고 반환과 선점 해제로 갈려 1대1 규칙에 어긋난다는 지적이 있었다. Hold 흡수로 예약 쪽 Release가 사라져 1대1로 돌아왔다.

Settle을 정산으로 옮긴 것은 돈을 정산한다는 뜻이 아니다. 처리를 마무리했다는 표식이다. 오해를 막기 위해 05-3 v11 6절 정의에 같은 주의를 적었다. (2026-09-08 v12)

### 5-3. HELD 종료 조건과 경합 처리 (2026-09-01 v9: Hold 흡수 반영. 2026-09-09 v13: C-6과 C-7로 교체. 확정)

Step 8에서 재판정했고 08-3 결정 11건으로 확정됐다. 더 이상 가설이 아니다.

| 결정 | 내용 |
|---|---|
| 종료 조건 | TTL 만료와 결제 3회 실패 중 먼저 커밋되는 것. 단 만료 커밋 전에 승인이 기록됐으면 확정이 우선한다. 둘 다 BookingExpired로 끝난다 |
| HELD 이탈 | 별도 커맨드 없이 TTL이 처리 |
| 감지와 실행 | T1 스케줄러가 TTL 지난 HELD 예약을 찾는다. 미정산 승인이 있으면 확정하고 없으면 ExpireBooking을 보낸다 |
| 유실 보정 | T2 순찰 정책이 정산 표식 없는 결제와 환불 대기 고아를 찾아 마무리한다 |

두 조건이 동시에 걸리는 경합이 있다. 3회째 결제 요청이 PG에 가 있는 동안 TTL이 만료되면 BookingExpired 이후에 PaymentApproved가 도착한다.

처리는 확정 우선이다. T1이 TTL 도달을 보고도 미정산 승인이 있으면 만료시키지 않고 확정한다. 이미 EXPIRED가 커밋된 뒤 도착한 승인은 환불한다. CONFIRMED와 CANCELED에 도착한 승인은 정산 표식만 찍는다.

침묵하지 않고 표식을 찍는 이유는 침묵하면 T2가 그 결제를 영원히 후보로 다시 집기 때문이다. v11까지 있던 가드를 == EXPIRED로 좁힌 이유 문단은 이 문장으로 대체됐다. 중복 승인 자체는 결제 컨텍스트가 pgTransactionId로 걸러 이벤트를 다시 내지 않는다. (2026-09-08 v12, 08-3 C-6)

## 6. 엔티티별 그룹 (Step 2.5, 2026-09-01 v9: Hold 행 제거, 후보 7개. 2026-09-09 v13: Payment 행에 시도 종류)

| 엔티티 | 이벤트 | 컨텍스트 |
|---|---|---|
| Property | Registered, Updated | 숙소 카탈로그 |
| RoomType | Registered, Updated | 숙소 카탈로그 |
| DailyInventory | Opened, Adjusted, Held, Committed, Released | 재고와 요금 |
| DailyRate | Registered, Adjusted | 재고와 요금 |
| Promotion | Created, Updated, Closed | 프로모션 |
| Booking | Created, Confirmed, Canceled, Expired | 예약 |
| Payment | Requested, Approved, Failed, Refunded. 내부에 시도 행을 갖고 종류는 NORMAL과 ORPHAN 둘이다 | 결제 |

애그리거트 후보 8개에서 7개가 됐다. FigJam 영역 2에는 Hold 섹션이 그대로 남아 있다. 이전 영역은 수정하지 않는 규칙에 따라 그대로 두고, 영역 4부터 새 구조를 쓴다.

## 7. 핫스팟 (6개)

### 핫스팟 1. 예약의 종착 상태가 없다
BookingConfirmed 이후 체크인, 체크아웃, 노쇼 이벤트가 없다. v1 범위 밖으로 명시했으나 CONFIRMED 이후 상태를 어떻게 닫을지는 미정이다.

### 핫스팟 2. InventoryReleased가 두 경로에서 발생한다 (v9: 세 경로에서 둘로. 2026-09-09 v13 해소)
BookingCanceled는 판매 수를, BookingExpired는 선점 수를 반환한다. ReleaseInventory(count, source)로 원인을 파라미터로 받는 것으로 06-2 v4에서 확정됐다.

### 핫스팟 3. Hold와 Booking이 별개 애그리거트인가 (2026-09-01 v9: 해소)
Hold를 Booking에 흡수하기로 결정했다. 근거는 셋이다. Hold의 커맨드가 전부 Booking 상태에 종속돼 있었고, RequestBooking이 직접 건드리는 애그리거트가 하나로 줄며, 확정된 예약의 Hold를 닫는 전이가 없던 결함이 CONFIRMED에서 EXPIRED로 갈 수 없다는 상태 규칙 하나로 막힌다.

### 핫스팟 4. 부분 취소가 있는가
3박 중 1박만 취소. 있으면 재고 반환과 금액 재계산이 날짜 단위로 쪼개진다. v1 범위 밖이다.

### 핫스팟 5. 예약 변경이 있는가
날짜 수정, 인원 수정. 없으면 취소 후 재예약이다. v1 범위 밖이다.

### 핫스팟 6. 판매 중인 날짜의 재고와 요금을 조정할 수 있는가 (2026-09-01 v7 확장. 2026-09-09 v13 해소)
AdjustInventory로 totalCount를 soldCount와 heldCount의 합 아래로 내리는 경우는 06-4 v4 선행조건으로 막았다.

## 8. Parking Lot

Step 4에서 정의를 확정했다. 05-3-o2o-glossary.md 참조.

## 9. 다음 단계

Step 5부터 Step 8까지 완료됐다. 다음은 Step 9 구현 매핑이다.

## 10. HTTP 계약과 이벤트의 구분 (2026-09-07)

HTTP API 개수와 도메인 커맨드 및 이벤트 개수는 일치할 필요가 없다. 목록과 상세 조회는 이벤트를 만들지 않고, 예약 요청 하나는 전 날짜의 InventoryHeld와 BookingCreated를 함께 발생시킨다. SettlePayment는 커맨드이면서 이벤트를 내지 않는다.

API 초안의 202 결제 응답은 PaymentRequested에 해당하는 접수 결과다. 승인은 Mock 결과 처리 후 확정된다. 같은 요청이나 콜백의 재전달은 새 시도, 새 확정 또는 새 환불 이벤트를 반복하지 않는다.

REFUNDED는 환불 결과를 나타낸다. API 초안의 PaymentAttempt는 승인 후 APPROVED 이력을 유지하고 Refund 객체로 환불 사실을 구분한다. 이 응답 모델은 08-3 C-4가 확정한 시도 행 모델과 다르다. 11 API v3 대조에서 맞춘다.

API 초안은 승인 처리 시각이 expiresAt 이상이면 만료 후 지연 승인 환불로 처리한다. 이 부분은 08-3 C-7의 확정 우선 결정과 어긋나며 11 API v3 대조 항목이다. TTL 값과 구체적인 동시성 구현 방식은 정책과 구현 설계에서 결정할 항목이다. (2026-09-09 v13)

## 11. 09-1 보드 v2 델타 판정 (2026-09-08 v12 신설)

이벤트 스토밍 보드 v2(2026-09-06 표기)가 2026-09-08에 harness/project-sync/09-1-o2o-board-v2.md로 반입됐다. 보드는 커맨드 27개와 이벤트 30개를 그리고 있고 이 문서는 22개와 22개다. 차이 13건을 판정한 결과가 이 절이다. 결정의 정본은 harness/decisions/decisions-09-1.md다.

판정 결과 이벤트 수는 22개 그대로다. 커맨드도 22개 그대로이며, 08-3 C-1의 SettlePayment로 23이 되는 것은 이 판정과 무관한 별개 사안이다.

### 11-1. Close와 Delete 계열 10건은 v1 범위 밖이다 (D-1)

보드에만 있고 이 문서에 없는 커맨드 5개와 이벤트 5개다.

| 컨텍스트 | 커맨드 | 이벤트 |
|---|---|---|
| 숙소 카탈로그 | CloseProperty | PropertyClosed |
| 숙소 카탈로그 | DeleteProperty | PropertyDeleted |
| 숙소 카탈로그 | CloseRoomType | RoomTypeClosed |
| 숙소 카탈로그 | DeleteRoomType | RoomTypeDeleted |
| 프로모션 | DeletePromotion | PromotionDeleted |

v1 범위에 넣지 않는다. 근거 셋이다.

| 근거 | 내용 |
|---|---|
| 기능 목록에 없다 | 02-o2o-feature-list.md 판매 준비 절에 숙소나 객실타입의 종료와 삭제가 없다. 이벤트는 기능에서 나오지 기능이 이벤트에서 나오지 않는다 |
| API에 경로가 없다 | 11-o2o-api-spec.md에 DELETE 경로가 하나도 없다. 프로모션 종료조차 PATCH enabled=false로 표현한다(P11) |
| 리뷰가 지적한 적이 없다 | 06-3, 06-5, 08-1, 08-2, 08-3 다섯 검증 중 어디도 이 부재를 결함으로 들지 않았다 |

프로모션은 이미 종료 개념을 갖고 있다. ClosePromotion과 PromotionClosed가 그것이고 05-3 용어 사전은 종료를 삭제가 아니라고 못 박고 있다. 보드의 DeletePromotion은 그 위에 삭제를 하나 더 얹는 것이라 v1에서 뺀다.

숙소 판매 종료만 성격이 다르므로 v2 후보로 남긴다. 판매 중인 날짜에 재고가 남아 있고 HELD 예약이 걸린 상태에서 숙소를 종료하면 재고 조정과 같은 자리의 경합이 생긴다. 핫스팟 6이 AdjustInventory에 대해 잡아 둔 문제와 구조가 같다. v2에서 다룰 때는 핫스팟 6과 함께 본다.

### 11-2. 거부 계열 3건은 도메인 이벤트로 올리지 않는다 (D-2)

보드가 점선 테두리로 그린 이벤트 셋이다.

| 보드의 이벤트 | 이 설계에서의 표현 | 근거 문서 |
|---|---|---|
| InventoryHoldRejected 선점 거부됨 | InventoryShortage 예외. HTTP로는 INVENTORY_UNAVAILABLE 오류 코드 | 06-4 1-2 RequestBooking 행, 11 API BOOK-01 |
| BookingRequestRejected 예약 요청 거부됨 | OccupancyExceeded와 InventoryShortage 예외 | 06-4 1-2 RequestBooking 행 |
| RefundFailed 환불 실패됨 | REFUND_PENDING 상태와 T2 후속 미완 결제 순찰 | 08-3 6절 C-4, 8절 결정 2번 |

도메인 이벤트로 올리지 않는 이유는 둘이다.

첫째, 앞의 둘은 발행할 자리가 없다. 예약 생성은 재고 N행 선점과 Booking 생성이 한 트랜잭션이고 전부 아니면 전무다. 거부는 그 트랜잭션을 롤백시키는 신호이므로 커밋될 상태 변화가 남지 않는다. 도메인 이벤트는 이미 일어난 사실을 뜻하는데 롤백된 시도는 일어나지 않은 것이다. 예외가 맞는 표현이다.

둘째, RefundFailed를 이벤트로 올리면 이미 수용된 결정이 뒤집힌다. 08-1이 환불 실패 사건의 부재를 지적했고 08-2가 같은 자리를 다시 짚었으며 08-3 C-4가 그 답으로 REFUND_PENDING 상태와 T2 순찰을 내놓았다. 사용자가 2026-09-07에 수용했다. 수용된 결정을 뒤집으려면 그 자체가 별도 판정이어야지 보드 대조의 부수 효과로 넘어갈 일이 아니다.

이 판정으로 구독자 없는 이벤트 수도 그대로다. 셋 중 어느 것도 이벤트 목록에 들어오지 않았기 때문이다.

### 11-3. 보드가 이 문서보다 뒤처진 지점

델타는 양방향이다. 보드가 2026-09-06이고 08-3 결정표가 2026-09-07이라 하루 차이에서 네 건이 나온다.

| 항목 | 이 문서 계열의 상태 | 보드 |
|---|---|---|
| SettlePayment | 08-3 C-1 수용. 04의 다음 판에서 커맨드 23 | 없음 |
| T2 후속 미완 결제 순찰 | 04-5의 다음 판에서 신설 | 없음 |
| 정책 번호 재배정 | 04-5가 정본. P2가 P1에 흡수되고 뒤가 한 칸씩 당겨짐 | 구 번호 P1부터 P7과 T1 |
| 확정 우선 | 08-3 C-7과 11번 수용 | 핫스팟에 결정 대기로 남음 |

보드를 이 결정으로 갱신하지 않는다. 06-o2o-board-legend.md 3절이 이전 영역을 수정하지 않는다는 규칙을 이미 갖고 있고 보드 v2도 같은 규칙 아래 둔다. 대신 무엇이 뒤처졌는지는 09-1 원문 10-2절과 이 표가 기록한다.

### 11-4. 보드 커맨드 27개의 처분 (2026-09-09 R1 반영 신설. S2-R1-A-01, S2-R1-B-04)

11절 머리와 12절이 커맨드 22개 유지라는 총계만 적고 이름 목록을 싣지 않았다. 총계만으로는 보드의 커맨드 각각이 어떻게 처분됐는지 짚을 수 없다. 유지하는 22개를 컨텍스트별로 적고 보드 27개와 잇는다.

유지하는 커맨드 22개다. 출처는 04-o2o-commands-actors.md v9의 커맨드 표와 역할별 담당 커맨드 표이고, 컨텍스트 배정은 05-2-o2o-bounded-contexts.md 2절을 따른다.

| 컨텍스트 | 커맨드 | 수 |
|---|---|---|
| 숙소 카탈로그 | RegisterProperty, UpdateProperty, RegisterRoomType, UpdateRoomType | 4 |
| 재고와 요금 | OpenInventory, AdjustInventory, RegisterRate, AdjustRate, HoldInventory, CommitInventory, ReleaseInventory | 7 |
| 프로모션 | CreatePromotion, UpdatePromotion, ClosePromotion | 3 |
| 예약 | RequestBooking, CancelBooking, ConfirmBooking, ExpireBooking | 4 |
| 결제 | RequestPayment, RecordPaymentApproval, RecordPaymentFailure, RefundPayment | 4 |

합 22다.

보드 27개와의 대응이다. 27에서 11-1이 제외한 5를 빼면 22가 된다.

| 보드의 커맨드 | 처분 | 위치 |
|---|---|---|
| RegisterProperty, UpdateProperty, RegisterRoomType, UpdateRoomType | 유지 | 위 표 숙소 카탈로그 |
| CloseProperty, DeleteProperty, CloseRoomType, DeleteRoomType | v1 범위 밖 | 11-1 |
| OpenInventory, AdjustInventory, RegisterRate, AdjustRate | 유지 | 위 표 재고와 요금 |
| HoldInventory, CommitInventory, ReleaseInventory | 유지 | 위 표 재고와 요금 |
| CreatePromotion, UpdatePromotion, ClosePromotion | 유지 | 위 표 프로모션 |
| DeletePromotion | v1 범위 밖 | 11-1 |
| RequestBooking, CancelBooking | 유지 | 위 표 예약 |
| ConfirmBooking, ExpireBooking | 유지 | 위 표 예약. 보드는 정책이 발신하는 커맨드로 그린다 |
| RequestPayment, RecordPaymentApproval, RecordPaymentFailure | 유지 | 위 표 결제 |
| RefundPayment | 유지 | 위 표 결제. 보드는 예약에서 결제로 가는 컨텍스트 간 호출로 그린다 |

커맨드를 새로 추가한 것이 아니다. 이미 04 v9에 있던 목록을 이 문서에 명시했을 뿐이다.

한 가지는 이번 라운드에서 닫지 못한다. 이 문서의 이전 판 대비 근거 없는 추가가 없다는 것은 기준판 발췌가 평가 허용 입력에 없어 독립 확인이 안 된다. S2-R1-B-04가 지적한 그 부분은 확인 불가로 남긴다.

### 11-5. 상위 요구사항 R2와 R3의 대응 위치 (2026-09-09 R1 반영 신설. S2-R1-B-01, S2-R1-B-02)

입력 팩 2절이 요구사항을 적는다. 이 문서가 그중 R2와 R3에 어떻게 대응하는지가 본문에 없었다.

| 요구사항 | 문안 | 확정 규칙의 위치 | 이 문서의 연결 자리 | 커버 |
|---|---|---|---|---|
| R2 | 동시 요청에서도 초과 예약 0 | 06-4 1-2 hold(n) 행. 오름차순 잠금, 행 존재와 가용 수량 n 이상, 연박 원자성 A1에 따라 전체 롤백 | 7절 핫스팟 6, 11-2절 | 전체 (2026-09-09 R1 반영. S8-R1-B-03) |
| R3 | 동일 요청 재시도 시 중복 Booking 방지 | 06-4 v4 1-2 RequestBooking 행. 같은 멱등키 존재 시 멱등 반환(U1). 멱등 반환 경로에서는 재고 선점과 이벤트 발행 없이 기존 Booking을 그대로 반환 | 10절, 아래 문단 | 전체 |

R2를 전체 커버로 올렸다 (2026-09-09 R1 반영. S8-R1-B-03). task-S2 R1에서 부분으로 적은 이유는 AdjustInventory로 totalCount를 soldCount + heldCount 아래로 내리는 경우가 미정의라는 것이었는데, 그것은 R2가 요구하는 동시 요청의 수량 상한과 다른 문제다. R2가 막는 것은 동시에 들어온 예약이 가용을 넘겨 잡는 것이고 그 자리는 06-4 hold(n)의 오름차순 잠금과 가용 검사와 연박 원자성이 전부 덮는다.

호스트가 판매 중인 날짜의 재고를 줄이는 경우는 R2가 아니라 별도 규칙의 자리다. 7절 핫스팟 6이 그것을 Step 7 이월로 적고 그 미결은 그대로 남는다. 미결이 남은 것과 R2가 부분 커버인 것은 다르다. 이미 정의된 하한을 미결 근거로 쓰지 않는다.

R3의 상위 근거는 09-1 5-1절의 RequestBooking 멱등키 포함과 7절 메모 1이다. 11 API v2 BOOK-01은 Idempotency-Key를 필수 헤더로 두고 새 업무 요청은 새 키, 재전송은 같은 키라고 적는다. 10절은 같은 요청의 재전달에서 새 시도와 새 확정과 새 환불 이벤트가 반복되지 않는다고만 적었고 Booking 생성 자체의 중복 방지는 적지 않았다. RequestBooking과 BookingCreated에 대한 그 연결을 여기 둔다.

동시성 구현 방식과 멱등 전략 자체는 이 판정에서 정하지 않는다.

### 11-6. 11-1과 11-2 근거의 원문 발췌 (2026-09-09 R1 반영 신설. S2-R1-B-05)

11-1과 11-2가 근거로 든 문서의 해당 행을 여기 옮긴다. 평가 허용 입력에 그 원문이 없어 독립 확인이 안 된다는 지적을 문서 안에서 닫는다.

| 근거로 든 것 | 원문 위치 | 발췌 |
|---|---|---|
| 02에 종료와 삭제 기능이 없다 | 02 v8 판매 준비 절 | 숙소 등록과 수정과 조회, 객실 타입 등록과 수정과 조회, 날짜별 재고 개설과 조정과 조회에 기간 일괄 개설, 날짜별 요금 등록과 조정과 조회. 네 줄뿐이고 종료나 삭제가 없다 |
| 11 API에 DELETE 경로가 없다 | 11 v2 전문 | DELETE 메서드 경로 0건 |
| 05-3의 종료 정의 | 05-3 v10 프로모션 용어 | 종료 Close. 더 이상 적용되지 않게 닫는 것. 삭제가 아니다. 종료된 프로모션은 수정할 수 없다 |
| 06-4의 선점 거부 예외 | 06-4 v4 1-2 hold(n) 행 | InventoryNotOpened(미개설), InventoryShortage(가용 부족). 어느 쪽이든 연박 원자성(A1)에 따라 전체 롤백 |
| 06-4의 예약 요청 거부 예외 | 06-4 v4 1-2 RequestBooking 행 | OccupancyExceeded, RateNotFound, InventoryShortage. 실패 시 전체 롤백 |
| 11 BOOK-01의 멱등키 | 11 v2 BOOK-01 요청 헤더 | Idempotency-Key 필수. 8~128자. 새 업무 요청은 새 키, 재전송은 같은 키 |

과거 리뷰 다섯(06-3, 06-5, 08-1, 08-2, 08-3)이 이 부재를 지적한 적이 없다는 근거는 발췌하지 않는다. 부재의 증명은 그 문서들을 통째로 넣어야 가능한데 그것은 CLAUDE.md N3와 평가 허용 입력의 제외 목록에 걸린다. 그 항목은 확인 불가로 남긴다.

## 12. 반영하지 않은 것 (2026-09-08 v12 신설)

| 항목 | 이유 |
|---|---|
| 이벤트와 커맨드 수의 변경 | 델타 13건이 전부 v1 범위 밖이나 현재 표현 유지로 판정됐다. 22개와 22개가 그대로다 |
| 04, 04-5, 05-2, 05-3, 06-2, 06-4, 11의 수정 | D-1과 D-2가 둘 다 유지 쪽이라 다른 문서에 옮길 변경이 없다. 판정 기록만 이 문서에 남는다 |
| 08-3 결정 11건의 반영 | task-S8 소관이다. 이 판은 그 반영을 담지 않는다 |
| 보드 자체의 갱신 | D-3이 시점 기록으로 두는 쪽이라 보드를 열지 않았다 |
| 범례의 v2 표기 등재 | D-4로 수용됐으나 대상 문서가 06-o2o-board-legend.md다. 같은 라운드의 별도 후보에 있다 |
| 숙소 판매 종료의 설계 | v2 후보로만 남겼다. 11-1의 마지막 문단이 그 자리다 |
| 이전 판 대비 추가 여부의 독립 확인 | 기준판 발췌가 평가 허용 입력에 없다. S2-R1-B-04의 그 부분은 확인 불가로 남긴다 |
| 과거 리뷰 다섯의 지적 부재 증명 | 부재 증명은 문서 전체를 넣어야 가능하고 N3와 허용 입력 제외 목록에 걸린다. S2-R1-B-05의 그 부분은 확인 불가로 남긴다 |
| 동시성 구현 방식과 멱등 전략 | 11-5가 대응 위치만 연결한다. 방식 자체는 Step 7 소관이다 |

판 번호 주의. 이 판은 document/03-o2o-event-storming.md v11에서 갈라졌고 task-S8-R1의 03 v12와 형제다. 둘 다 미승인이며 같은 번호를 쓴다. 고치는 절이 겹치지 않으므로 내용 충돌은 없다. 먼저 반영되는 쪽이 v12가 되고 나중 쪽은 그 위에서 v13으로 다시 낸다.

## 13. 08-3 결정 반영 기록 (2026-09-09 v13 신설)

task-S8이 08-3 정책 결정 11건을 설계 문서 9종에 반영한다. 이 문서가 받는 몫은 셋이다.

| 08-3 항목 | 이 문서의 반영 위치 | 무엇이 바뀌었나 |
|---|---|---|
| C-6 CANCELED 분기 삭제와 C-7 R4 문언 | 5-3절 | 종료 조건과 경합 처리를 확정 우선으로 교체했다. T1이 TTL 도달을 봐도 미정산 승인이 있으면 만료시키지 않는다 |
| C-11 결제 이벤트 페이로드 | 1절의 결제 이벤트 페이로드 소절 신설 | 결제 이벤트가 실어야 할 값을 이벤트별로 적었다 |
| C-1 정산 표식 | 5-2절과 6절 | 동사 표기에 Settle을 넣고 6절 Payment 행에 시도 종류를 넣었다 |

닫힌 미결이 하나 있다.

| 미결 | 무엇이었나 | 어떻게 닫혔나 |
|---|---|---|
| InventoryHoldRejected 이벤트 추가 | task-S8 계약이 09-1 보드 v2 미반입을 이유로 미결로 남겼다 | 09-1이 2026-09-08에 반입됐고 이 문서 11-2절이 도메인 이벤트로 올리지 않는 것으로 판정했다. 이벤트 22개 유지도 그 절이 확인한다 |

task-S8 계약의 반영 대상 표는 이 문서의 몫을 C-7 R4 문언 하나로 적고 InventoryHoldRejected를 미결로 달았다. 그 미결이 다른 Task에서 닫혔으므로 이 판은 미결 없이 나온다.

이 판이 v13인 이유. document/03-o2o-event-storming.md가 task-S2 R1 반영으로 2026-09-09에 v12로 확정됐고 이 판은 그 위에서 만들었다. 판 번호 순서는 2026-09-08 사용자 결정이며 harness/decisions/decisions-09-1.md 4절이 근거다.

절 제목의 날짜 주석 주의. 이 판을 만들면서 task-S8-R1의 03 v12 후보가 지웠던 기존 날짜 주석 일곱 개를 되살렸고, 새 주석으로 덮은 다섯 개는 옛 것과 병기했다. D4는 바뀐 절에 날짜를 적으라는 규칙이지 안 바뀐 절의 날짜를 지우라는 규칙이 아니다.

task-S2 R1 평가 반영 기록. 지적 6건 중 이 문서 대상은 5건이다. 이 문서에는 Task 둘의 R1이 겹치므로 Task 이름을 붙여 적는다.

| 지적 ID | 심각도 | 반영 위치 |
|---|---|---|
| S2-R1-A-01 | 보통 | 11-4 |
| S2-R1-B-01 | 보통 | 11-5 |
| S2-R1-B-02 | 보통 | 11-5 |
| S2-R1-B-04 | 확인필요 | 11-4. 이전 판 대비 확인은 12절에 확인 불가로 남김 |
| S2-R1-B-05 | 확인필요 | 11-6. 과거 리뷰 지적 부재는 12절에 확인 불가로 남김 |

S2-R1-B-03은 06-o2o-board-legend.md 대상이라 이 파일에 반영하지 않았다. 같은 라운드의 별도 반영본이 그 자리다.

이 판은 08-3 결정 11건의 반영 후보다. 승인 전에는 확정본으로 인용하지 않는다.
