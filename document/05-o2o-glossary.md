# O2O 숙박 예약 용어 사전 (Step 4)

최초 작성: 2026-09-01
최종 갱신: 2026-09-07 v7 (검색 인원 조건, 결제 시도와 환불, API 용어 정리)
자료: 05 경계를 통해 모델을 보호하는 방법
바운디드 컨텍스트 명세와 의존 관계: FigJam 보드 영역 4 (https://www.figma.com/board/ffhYVMh8awMBfqLinXFMDY)

문서 규칙: 새로 추가되거나 내용이 바뀐 절에는 제목 옆에 반영 날짜를 표기한다.

## 0. 컨텍스트 목록

명세는 FigJam에 있다. 여기서는 용어 사전을 읽는 데 필요한 이름만 적는다.

| 컨텍스트 | 영문 | 성격 |
|---|---|---|
| 숙소 카탈로그 | Catalog | 호스트가 판매할 숙소와 객실 타입을 정의 |
| 재고와 요금 | Inventory | 날짜별 판매 가능 수량과 단가 |
| 프로모션 | Promotion | 자동 적용 할인 정책 |
| 예약 | Booking | 예약 생성(재고 선점 포함)과 확정, 취소, 만료 |
| 결제 | Payment | 외부 PG와의 결제 요청과 결과 기록 |
| 검색 | Search | 읽기 모델. 컨텍스트가 아님 |

### 0-1. 컨텍스트별 커맨드와 이벤트 배분 (2026-09-01 v4 추가)

경계 판정 근거와 소유 데이터는 FigJam에 있다. 여기 표는 문서 간 대조용이다. 커맨드 22개와 이벤트 22개가 빠짐없이 한 컨텍스트에만 속하는지 확인하는 용도다. (v5: Hold 흡수로 숫자 변경)

| 컨텍스트 | 받는 커맨드 | 내는 이벤트 |
|---|---|---|
| 숙소 카탈로그 | RegisterProperty, UpdateProperty, RegisterRoomType, UpdateRoomType (4) | PropertyRegistered, PropertyUpdated, RoomTypeRegistered, RoomTypeUpdated (4) |
| 재고와 요금 | OpenInventory, AdjustInventory, RegisterRate, AdjustRate, HoldInventory, CommitInventory, ReleaseInventory (7) | InventoryOpened, InventoryAdjusted, RateRegistered, RateAdjusted, InventoryHeld, InventoryCommitted, InventoryReleased (7) |
| 프로모션 | CreatePromotion, UpdatePromotion, ClosePromotion (3) | PromotionCreated, PromotionUpdated, PromotionClosed (3) |
| 예약 | RequestBooking, ConfirmBooking, CancelBooking, ExpireBooking (4) | BookingCreated, BookingConfirmed, BookingCanceled, BookingExpired (4) |
| 결제 | RequestPayment, RecordPaymentApproval, RecordPaymentFailure, RefundPayment (4) | PaymentRequested, PaymentApproved, PaymentFailed, PaymentRefunded (4) |
| 검색 | 없음 | 없음 |
| 합계 | 22 | 22 |

받는 커맨드 기준이다. 예약 컨텍스트가 보내는 커맨드(HoldInventory, CommitInventory, ReleaseInventory, RefundPayment)와 게스트가 보내고 예약이 중계하는 RequestPayment는 받는 쪽에 센다.

## 1. 액터

| 한국어 | 영문 | 정의 |
|---|---|---|
| 게스트 | Guest | 숙소를 예약하고 결제하는 사람. v1에서는 guestId로만 식별하고 회원 정보는 다루지 않는다 |
| 호스트 | Host | 숙소를 등록하고 재고와 요금을 관리하는 사람 |
| 운영자 | Operator | 플랫폼 측에서 프로모션을 만드는 사람 |
| 외부 PG | Payment Gateway | 결제 결과를 통보하는 외부 시스템 역할. 이번 v1에서는 Mock 어댑터로 대체 |

## 2. 컨텍스트를 넘나들며 뜻이 갈리는 용어 (2026-09-07)

한 단어가 여러 컨텍스트에서 다른 것을 가리킨다. 코드에서 반드시 이름을 갈라야 하는 것들이다. 컨텍스트 열의 검색은 읽기 모델이지만 용어 충돌을 보이기 위해 같은 열에 둔다.

| 한국어 | 컨텍스트 | 영문 | 정의 |
|---|---|---|---|
| 객실 | 숙소 카탈로그 | RoomType | 호스트가 판매 단위로 등록한 객실 종류. API 초안은 이름, 설명과 최대 인원을 제공 |
| 객실 | 재고와 요금 | DailyInventory | 특정 날짜에 팔 수 있는 객실 수. 숫자만 있다 |
| 객실 | 예약 | roomTypeId | 예약 대상을 가리키는 식별자 |
| 가격 | 재고와 요금 | Rate | 호스트가 등록한 날짜별 단가. API 초안은 오늘 이후 날짜만 수정하며 과거 조회는 허용 |
| 가격 | 예약 | Price | 요금에 프로모션을 적용해 계산한 결과. 예약 생성 시 스냅샷으로 동결 (v6: 확정에서 생성으로) |
| 가격 | 결제 | Amount | 실제로 PG에 청구한 액수 |
| 상태 | 예약 | BookingStatus | HELD, CONFIRMED, CANCELED, EXPIRED |
| 상태 | 결제 시도 API | PaymentAttempt.status | REQUESTED, APPROVED, FAILED. 환불 후에도 승인 이력은 APPROVED로 유지 |
| 상태 | 환불 API | Refund.status | REFUNDED. 결제 시도와 별도 응답 객체로 환불을 표현 |
| 확정 | 재고와 요금 | Commit | heldCount를 soldCount로 옮기는 것 |
| 확정 | 예약 | Confirm | 결제 승인으로 예약이 성립하는 것 |
| 반환 | 재고와 요금 | Release | heldCount 또는 soldCount를 내리는 것. 예약 쪽의 해제(ReleaseHold)는 v5에서 ExpireBooking에 흡수되어 사라졌다 |
| 지역 | 숙소 카탈로그 | Region | 숙소가 속한 행정 구역. 카탈로그가 소유 |
| 지역 | 프로모션 | Region | 적용 조건의 하나. 카탈로그의 값을 읽어 판정 |
| 지역 | 검색 | Region | 검색 필터. 카탈로그의 값을 읽음 |
| 기간 | 프로모션 | campaignPeriod | 프로모션이 유효한 달력상 기간 |
| 기간 | 예약 | StayPeriod | 체크인부터 체크아웃까지의 숙박 기간 |
| 선점 | 재고와 요금 | Hold (heldCount) | 특정 날짜의 heldCount를 1 올리는 수량 연산. 누가 잡았는지 모른다 (v4 추가) |
| 선점 | 예약 | HELD | Booking의 상태. 게스트 하나가 숙박 기간 전체를 결제 전까지 붙잡고 있고 TTL이 돈다 (v5: Hold 애그리거트가 아니라 BookingStatus) |
| 인원 | 숙소 카탈로그 | maxOccupancy | 객실 타입이 수용할 수 있는 상한 (v4 추가) |
| 인원 | 예약 | guestCount | 이 예약으로 실제 묵는 사람 수 (v4 추가) |
| 인원 | 검색 | guestCount | maxOccupancy >= guestCount를 만족하는 객실 타입만 보여준다 |

가격 셋을 구분하지 않으면 확정 금액이 이후 요금 변경에 영향받지 않는다는 요구사항을 코드로 표현할 수 없다.
상태 둘을 구분하지 않으면 외부 PG의 모델이 우리 예약 상태를 오염시킨다.
확정이 재고와 예약에서 다른 영어를 갖는 것은 의도적이다. 재고의 Commit은 수량 이동이고 예약의 Confirm은 계약 성립이다.
선점은 재고 쪽에서는 Hold라는 수량 연산이고 예약 쪽에서는 HELD라는 상태다. 재고는 예약을 모른다.

## 3. 숙소 카탈로그 용어

| 한국어 | 영문 | 정의 |
|---|---|---|
| 숙소 | Property | 판매 대상이 되는 건물 또는 시설 단위. 하나의 주소와 지역을 가진다 |
| 객실 타입 | RoomType | 숙소가 판매하는 객실의 종류 |
| 호실 | Room | 실제 배정되는 개별 방. v1 범위 밖이라 쓰지 않는다 |
| 최대 인원 | maxOccupancy | 객실 타입 하나에 묵을 수 있는 최대 인원. 예약의 인원 검증에 쓰인다 |
| 등록 | Register | 없던 것을 새로 만드는 것 |
| 수정 | Update | 서술적 속성을 고치는 것 |

## 4. 재고와 요금 용어

| 한국어 | 영문 | 정의 |
|---|---|---|
| 재고 | Inventory | 특정 객실 타입의 특정 날짜에 저장된 판매 가능 수량 |
| 가용성 | Availability | totalCount에서 soldCount와 heldCount를 뺀 계산 결과. 저장하지 않는다 |
| 총 수량 | totalCount | 그날 팔 수 있는 전체 객실 수 |
| 판매 수 | soldCount | 확정된 예약이 점유한 수 |
| 선점 수 | heldCount | Hold가 임시로 점유한 수 |
| 개설 | Open | 기간을 지정해 날짜별 재고를 한꺼번에 만드는 것 |
| 조정 | Adjust | 수량이나 금액을 고치는 것. 불변식 검사가 따른다 |
| 선점 | Hold | heldCount를 올리는 것 |
| 확정 | Commit | heldCount를 내리고 soldCount를 올리는 것 |
| 반환 | Release | heldCount 또는 soldCount를 내리는 것 |
| 요금 | Rate | 호스트가 등록한 날짜별 1박 단가 |

재고와 가용성을 구분하는 것이 이 컨텍스트의 핵심이다. 재고는 저장된 값이고 가용성은 매번 계산되는 값이다.

## 5. 프로모션 용어 (2026-09-07)

| 한국어 | 영문 | 정의 |
|---|---|---|
| 프로모션 | Promotion | 운영자가 만든 자동 적용 할인 정책 |
| 적용 조건 | Condition | 최소 숙박일, 지역, 기간 |
| 할인율 | discountRate | 정률 할인의 비율 |
| 캠페인 기간 | campaignPeriod | 프로모션이 유효한 달력상 기간. 숙박 기간과 다르다 |
| 할인 배분 | Allocation | 숙박 날짜별 할인액을 정하는 것. API 초안은 날짜별 할인액을 버림한 뒤 합산하며, 이 계산 정책은 제안 상태 |
| 생성 | Create | 없던 프로모션을 새로 만드는 것 |
| 종료 | Close | 더 이상 적용되지 않게 닫는 것. 삭제가 아니다 |

캠페인 기간과 숙박 기간을 반드시 갈라 쓴다. 9월에 파는 12월 숙박 상품에 붙는 할인이라면 캠페인 기간은 9월이고 숙박 기간은 12월이다.

## 6. 예약 용어

| 한국어 | 영문 | 정의 |
|---|---|---|
| 예약 | Booking | 게스트가 특정 객실 타입을 특정 기간 묵겠다고 만든 계약. 생성 시점부터 재고를 붙잡는다 |
| 선점 중 | HELD | 생성됐고 결제가 끝나지 않은 상태. 재고를 임시로 붙잡고 있고 TTL이 돈다 (v5) |
| TTL | ttl | HELD가 유지되는 시간 |
| 확정 | CONFIRMED | 결제 승인으로 예약이 성립한 상태 |
| 취소 | CANCELED | 확정된 예약을 게스트가 무른 상태 |
| 만료 | EXPIRED | TTL 만료 또는 결제 실패 한도 도달로 HELD가 종료된 상태. 붙잡았던 재고는 반환된다 (v5: 정의 확장) |
| 만료시키기 | ExpireBooking(reason) | HELD 예약을 EXPIRED로 보내는 커맨드. reason은 TTL_EXPIRED 또는 PAYMENT_FAILED (v6) |
| 체크인 | checkIn | 숙박이 시작되는 날. 이 날부터 재고를 점유한다 |
| 체크아웃 | checkOut | 숙박이 끝나는 날. 이 날은 재고를 점유하지 않는다 |
| 숙박 기간 | StayPeriod | 체크인부터 체크아웃까지. 박수는 두 날짜의 차이 |
| 연박 | Multi-night | 박수가 2 이상 |
| 인원 | guestCount | 묵는 사람 수. 카탈로그의 maxOccupancy를 넘을 수 없다 |
| 게스트 식별자 | guestId | 예약을 만든 게스트. v1에서 Booking이 가진 유일한 게스트 정보 (v4 추가) |
| 금액 스냅샷 | PriceSnapshot | 생성 시점의 날짜별 단가와 할인액을 복사해 보관한 것. 결제 청구액의 근거 (v6: 시점 정정) |
| 멱등키 | idempotencyKey | 같은 요청이 반복돼도 예약이 하나만 생기게 하는 식별자 |

만료와 해제를 갈라 쓰던 가설은 v5에서 폐기됐다. Hold를 Booking에 흡수하면서 둘이 HELD에서 EXPIRED로 가는 전이 하나가 됐다. 상태 전이는 HELD → CONFIRMED, HELD → EXPIRED, CONFIRMED → CANCELED 셋뿐이다. CONFIRMED → EXPIRED는 없다.

## 7. 결제 용어

| 한국어 | 영문 | 정의 |
|---|---|---|
| 승인 | Approved | PG가 결제를 성공 처리한 것 |
| 실패 | Failed | PG가 결제를 거절한 것 |
| 환불 | Refunded | 승인된 결제를 되돌린 것 |
| 콜백 | Callback | PG가 결과를 우리에게 통보하는 호출 |
| 청구액 | Amount | PG에 청구한 실제 금액 |
| 결제 시도 횟수 | attemptCount | 이 예약에 대한 결제 시도 누적 수. 3을 넘을 수 없다 |
| 거래 식별자 | pgTransactionId | PG가 발급한 거래 번호. 같은 번호의 승인 콜백이 다시 오면 무시한다 (v5: 멱등 규칙) |

## 8. v1 범위 밖

- 회원: 게스트는 guestId로만 식별
- 쿠폰: 전체
- 호실 배정: 객실 타입까지만
- 알림, 정산, 리뷰
- 호스트나 운영자에 의한 예약 취소
- 프로모션 자동 종료

## 9. API 구현 용어 (2026-09-07)

| 용어 | 의미 |
|---|---|
| PriceQuote | 조회 시점 예상 금액. 재고 선점이나 예약 금액 고정을 뜻하지 않음 |
| PriceSnapshot | 예약 생성 시 저장한 날짜별 단가, 할인액과 적용 프로모션 정보 |
| expectedTotalAmount | 사용자가 확인한 예상 총액. 서버 청구액의 원본이 아닌 재계산 금액과의 비교값 |
| PaymentAttempt | 서로 다른 결제 시도. 요청 재전송은 새 시도가 아님 |
| PaymentSummary | 시도 수, 승인 시도와 환불을 묶는 API 응답. 애그리거트 구조를 확정하지 않음 |
| Idempotency-Key | 같은 행위자, 메서드와 자원 경로의 재전송을 구분하는 키 |
| version | 수정 충돌 검사용 버전. 재고 선점, 확정과 반환도 증가시킴 |
| X-Dev-Actor-Id | 로컬 행위자 fixture 선택용 헤더. 실제 신원 인증 수단은 아님 |
| G1 | 반영 전 산출물 검사. 문서는 형식과 필수 항목, 코드는 빌드와 테스트 및 변경 범위를 검사 |
| G2 | 사용자 결정과 원본 지적의 누락, 변조 및 버전을 대조하는 반영 전 검사 |

Payment 애그리거트의 식별 단위와 내부 상태 모델은 아직 미결이다. 위 API 응답 이름을 그대로 JPA 엔티티와 일대일로 대응시키지 않는다.
