# O2O 숙박 예약 이벤트 스토밍

최초 작성: 2026-08-27
최종 갱신: 2026-09-07 v11 (프로모션 타임라인, 수정 경합과 API 처리 경계 정정)
단계: Big Picture 완료, Step 2.5 엔티티별 그룹핑 완료
입력: 02-o2o-feature-list.md
후속: 04-o2o-commands-actors.md
FigJam 보드: https://www.figma.com/board/ffhYVMh8awMBfqLinXFMDY

문서 규칙: 새로 추가되거나 내용이 바뀐 절에는 제목 옆에 반영 날짜를 표기한다.

## 1. 도메인 이벤트 목록 (22개, 2026-09-01 v9: Hold 흡수로 24개에서 22개)

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

리뷰에서 지적된 구멍이다. 예약이 재고를 선점하는데 재고 컨텍스트에 그것을 받는 이벤트가 없었다.
재고 수량은 재고 컨텍스트만 바꾼다는 원칙을 지키려면, 예약은 재고에 커맨드를 보내고 재고가 자기 이벤트를 발행해야 한다.

### 예약 (액터: 게스트, 정책, 스케줄러. 2026-09-01 v9: Hold 흡수)

| 이벤트 | 의미 |
|---|---|
| BookingCreated | 예약이 HELD 상태로 생성됨. 숙박 기간 전체의 재고 선점이 성공했다는 뜻을 포함한다 |
| BookingConfirmed | 예약이 확정됨 |
| BookingCanceled | 확정된 예약이 취소됨 |
| BookingExpired | HELD 예약이 종료됨. TTL 만료 또는 결제 실패 한도 도달 |

v9에서 바뀐 것. 재검증에서 확정된 예약의 Hold를 닫는 전이가 없어 TTL 스케줄러가 확정 예약의 재고까지 반환하는 결함이 발견됐다. Hold를 Booking에 흡수하는 것으로 결정했고 그 결과 이벤트 셋이 사라지고 하나가 생겼다.
- StayHeld는 BookingCreated와 같은 사건이다. 3박이면 InventoryHeld가 세 번, 셋 다 성공했을 때 BookingCreated가 한 번이다
- HoldExpired와 HoldReleased는 BookingExpired 하나로 합쳐졌다. 만료를 감지하는 것과 해제하는 것을 나눌 이유가 Booking 상태 전이 하나로 표현되면서 없어졌다
- BookingStatus는 HELD, CONFIRMED, CANCELED, EXPIRED 넷 그대로다. CONFIRMED는 EXPIRED로 갈 수 없으므로 확정 예약의 재고가 반환되는 경로가 구조적으로 막힌다

### 결제 (액터: 게스트가 요청하고 예약 컨텍스트가 중계, 외부 PG가 결과 통보. 2026-09-01 v10)

| 이벤트 | 의미 |
|---|---|
| PaymentRequested | 게스트의 요청을 예약 컨텍스트가 스냅샷 총액을 붙여 결제에 전달함. 첫 시도와 재시도 모두 |
| PaymentApproved | 결제가 승인됨. 이벤트에 시도 횟수를 싣는다 |
| PaymentFailed | 결제가 실패함. 이벤트에 시도 횟수를 싣는다 |
| PaymentRefunded | 환불이 완료됨 |

결제가 청구액을 알기 위해 예약을 읽으면 예약과 결제가 서로 상류가 된다. 예약이 총액을 실어 보내면 결제는 예약을 모른다. 액터는 여전히 게스트다. v9에서 예약 컨텍스트의 자동 발송으로 바꿨다가 재시도가 모델에서 사라지는 문제로 v10에서 되돌렸다.

## 2. 타임라인 (2026-09-07: 등록과 조회 구분)

[호스트]
PropertyRegistered -> RoomTypeRegistered -> InventoryOpened -> RateRegistered

[운영자]
PromotionCreated -> 필요하면 PromotionUpdated 또는 PromotionClosed
예약 흐름에서 프로모션을 조회하는 행위에는 상태 변경과 새 이벤트가 없다.

[게스트 -> 재고 -> 결제] (v10)
(탐색: 이벤트 없음) -> InventoryHeld x N일 -> BookingCreated (스냅샷 동결) -> PaymentRequested (게스트가 요청, 재시도 포함)

[외부 결제] (v9)
- PaymentApproved, HELD이고 now < expiresAt -> BookingConfirmed (PIVOTAL) -> InventoryCommitted x N
- PaymentApproved, EXPIRED -> PaymentRefunded (TTL이 먼저 이긴 경우)
- PaymentFailed, 3회 미만 -> HELD 유지, 재시도 대기
- PaymentFailed, 3회 도달 -> BookingExpired -> InventoryReleased x N

[확정 이후]
BookingConfirmed -> BookingCanceled -> PaymentRefunded, InventoryReleased x N
BookingConfirmed -> 체크인 이후는 미정 (핫스팟 1)

[시간 -> 시스템] (v9)
BookingCreated 이후 TTL 경과, 아직 HELD -> BookingExpired -> InventoryReleased x N

## 3. Pivotal Event

### BookingConfirmed (1순위)

| | 이전 | 이후 |
|---|---|---|
| 재고 | 선점 수(heldCount)에 있음 | 판매 수(soldCount)로 이동 |
| 금액 | 생성 시점 스냅샷 (v10: 확정이 아니라 생성에서 동결) | 같은 스냅샷이 청구액이 됨 |
| 되돌리는 방법 | 방치하면 자동 소멸 | 명시적 취소 + 환불 필요 |
| 취소 커맨드 | 없음 (TTL이 처리) | CancelBooking |

재고 행이 v7에서 바뀌었다. 재고 컨텍스트 용어로는 heldCount와 soldCount 사이의 이동이다. InventoryCommitted가 이 이동을 뜻한다.

### BookingCreated (2순위, v9: StayHeld에서 이름 변경)

이 이벤트 전에는 재고가 누구의 것도 아니고, 이후에는 특정 게스트의 예약이 붙잡고 있으며 TTL 시계가 돌기 시작한다.

## 4. 이벤트 밀도와 액터 전환으로 본 컨텍스트 경계 후보 (2026-09-07)

| 구간 | 액터 | 시간 간격 | 성격 |
|---|---|---|---|
| PropertyRegistered ~ RateRegistered | 호스트 | 며칠 단위 | 판매 준비. 운영 중 재고 조정과 예약 선점은 경합 가능 |
| PromotionCreated ~ PromotionClosed | 운영자 | 독립 흐름 | 정책 |
| 탐색 구간 | 게스트 | 초 단위 반복 | 이벤트 없음 |
| InventoryHeld ~ BookingConfirmed | 게스트 + 시스템 | 분 단위 밀집 | 동시성 핵심 |
| PaymentApproved / PaymentFailed | 외부 결제사 | 초~분 | 외부 경계 |
| BookingExpired | 시간 또는 결제 실패 정책 | TTL 경과 또는 3회 실패 | 아무도 요청하지 않음 |

첫째, 탐색 구간에 도메인 이벤트가 하나도 없다. 검색이 도메인 모델이 아니라 읽기 모델이라는 증거다.
둘째, BookingExpired는 사람이 요청하지 않는다. 시간이나 정책이 트리거라 별도 메커니즘이 필요하다.
셋째, 준비 구간과 예약 구간은 시간 간격의 차원이 다르다.
넷째, 프로모션은 운영자가 만들고 게스트 흐름에서는 읽히기만 한다.

## 5. 확정된 결정

### 5-1. 쿠폰은 v1에서 제외 (2026-09-01, v7에서 성격 재분류)

이것은 학습 범위 결정이다. 동시성 설계를 재고 한 곳에 집중시켜 학습 밀도를 높이려는 것이고, 도메인 근거는 부차적이다.
리뷰에서 도메인 근거처럼 포장했다는 지적이 있어 성격을 바로잡는다. 상세는 02-o2o-feature-list.md 3-2.

### 5-2. 동사 표기 규칙 (2026-08-27, v7에서 확장)

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
| Release | 반환 | 재고의 점유 수를 내림 (v10: 예약 쪽 해제는 ExpireBooking에 흡수되어 사라짐) |
| Expire | 만료 | HELD 예약을 종료함. TTL 경과 또는 결제 실패 한도 |
| Close | 종료 | 프로모션을 닫음 |
| Cancel | 취소 | 확정된 예약을 무름 |

리뷰에서 Release가 재고 반환과 선점 해제로 갈려 1대1 규칙에 어긋난다는 지적이 있었다. Hold 흡수로 예약 쪽 Release가 사라져 1대1로 돌아왔다. (v10)

### 5-3. HELD 종료 조건과 역할 분담 (2026-09-01 v9: Hold 흡수 반영)

Step 8에서 재판정한다. 자료 03이 정책 조기 도입을 오류로 명시하므로 지금 단계에서는 가설이다.

| 결정 | 내용 |
|---|---|
| 종료 조건 | TTL 만료와 결제 3회 실패 중 먼저 오는 것. 둘 다 BookingExpired로 끝난다 |
| HELD 이탈 | 별도 커맨드 없이 TTL이 처리 |
| 감지와 실행 | 스케줄러가 TTL 지난 HELD 예약을 찾아 ExpireBooking을 보낸다. 감지와 해제가 커맨드 하나로 합쳐졌다 (v9) |

두 조건이 동시에 걸리는 경합이 있다. 3회째 결제 요청이 PG에 가 있는 동안 TTL이 만료되면 BookingExpired 이후에 PaymentApproved가 도착한다.
ConfirmBooking은 HELD이고 처리 시각이 expiresAt 전인지 검사한다. TTL이 지난 HELD는 먼저 만료시키고, EXPIRED 예약에 대한 승인은 자동 환불한다. 가드를 != HELD가 아니라 == EXPIRED로 좁힌 이유는, 중복 승인으로 CONFIRMED 예약을 환불하지 않기 위해서다. 중복 승인 자체는 결제 컨텍스트가 pgTransactionId로 걸러낸다. (2026-09-07 보완)

## 6. 엔티티별 그룹 (Step 2.5, 2026-09-01 v9: Hold 행 제거, 후보 7개)

| 엔티티 | 이벤트 | 컨텍스트 |
|---|---|---|
| Property | Registered, Updated | 숙소 카탈로그 |
| RoomType | Registered, Updated | 숙소 카탈로그 |
| DailyInventory | Opened, Adjusted, Held, Committed, Released | 재고와 요금 |
| DailyRate | Registered, Adjusted | 재고와 요금 |
| Promotion | Created, Updated, Closed | 프로모션 |
| Booking | Created, Confirmed, Canceled, Expired | 예약 |
| Payment | Requested, Approved, Failed, Refunded | 결제 |

애그리거트 후보 8개에서 7개가 됐다. FigJam 영역 2에는 Hold 섹션이 그대로 남아 있다. 이전 영역은 수정하지 않는 규칙에 따라 그대로 두고, 영역 4부터 새 구조를 쓴다.

## 7. 핫스팟 (6개)

### 핫스팟 1. 예약의 종착 상태가 없다
BookingConfirmed 이후 체크인, 체크아웃, 노쇼 이벤트가 없다. v1 범위 밖으로 명시했으나 CONFIRMED 이후 상태를 어떻게 닫을지는 미정이다.

### 핫스팟 2. InventoryReleased가 두 경로에서 발생한다 (v9: 세 경로에서 둘로)
BookingCanceled(soldCount 반환)와 BookingExpired(heldCount 반환). 결제 3회 실패와 TTL 만료가 BookingExpired로 합쳐지면서 셋이 둘이 됐다. 재고 쪽에서 둘을 커맨드 하나로 받을지(ReleaseInventory에 원인 파라미터), 둘로 나눌지는 Step 6에서 정한다.

### 핫스팟 3. Hold와 Booking이 별개 애그리거트인가 (2026-09-01 v9: 해소)
Hold를 Booking에 흡수하기로 결정했다. 근거는 셋이다. Hold의 커맨드가 전부 Booking 상태에 종속돼 있었고, RequestBooking이 직접 건드리는 애그리거트가 하나로 줄며, 확정된 예약의 Hold를 닫는 전이가 없던 결함이 CONFIRMED에서 EXPIRED로 갈 수 없다는 상태 규칙 하나로 막힌다. Step 6에서 원래 결론낼 사항이었으나 재검증에서 발견된 결함 때문에 앞당겼다.

### 핫스팟 4. 부분 취소가 있는가
3박 중 1박만 취소. 있으면 재고 반환과 금액 재계산이 날짜 단위로 쪼개진다.

### 핫스팟 5. 예약 변경이 있는가
날짜 수정, 인원 수정. 없으면 취소 후 재예약.

### 핫스팟 6. 판매 중인 날짜의 재고와 요금을 조정할 수 있는가 (2026-09-01 v7 확장)
요금뿐 아니라 재고도 걸린다. AdjustInventory로 totalCount를 soldCount + heldCount 아래로 내리는 경우가 정의되지 않았다. Step 7 선행조건으로 명세한다.

## 8. Parking Lot
Step 4에서 정의를 확정했다. 05-o2o-glossary.md 2절 참조. (2026-09-01 v8: 참조 정정)

## 9. 다음 단계
Step 5에서 컨텍스트 간 의존성 화살표와 Upstream/Downstream을 정한다.

## 10. HTTP 계약과 이벤트의 구분 (2026-09-07)

HTTP API 개수와 도메인 커맨드 및 이벤트 개수는 일치할 필요가 없다. 목록과 상세 조회는 이벤트를 만들지 않고, 예약 요청 하나는 전 날짜의 InventoryHeld와 BookingCreated를 함께 발생시킨다.

API 초안의 202 결제 응답은 PaymentRequested에 해당하는 접수 결과다. 승인은 Mock 결과 처리 후 확정된다. 같은 요청이나 콜백의 재전달은 새 시도, 새 확정 또는 새 환불 이벤트를 반복하지 않는다.

REFUNDED는 환불 결과를 나타낸다. API 초안의 PaymentAttempt는 승인 후 APPROVED 이력을 유지하고 Refund 객체로 환불 사실을 구분한다. 이 응답 모델이 Payment 애그리거트의 식별 단위를 확정하지는 않는다.

API 초안은 승인 처리 시각이 expiresAt 이상이면 만료 후 지연 승인 환불로 처리한다. TTL 값과 구체적인 동시성 구현 방식은 정책과 구현 설계에서 결정할 항목이다.
