# O2O 바운디드 컨텍스트 확정 (Step 4-2)

최초 작성: 2026-09-01 (05-o2o-glossary.md 0절로 작성)
최종 갱신: 2026-09-02 v7-split (파일 분리. 내용 변경 없음)
자매 문서: 05-1-o2o-ubiquitous-language.md (경계 근거가 된 용어 충돌), 05-3-o2o-glossary.md (컨텍스트별 정의)
경계 판정 근거와 소유 데이터 명세: FigJam 보드 영역 4 (https://www.figma.com/board/ffhYVMh8awMBfqLinXFMDY)

문서 규칙: 새로 추가되거나 내용이 바뀐 절에는 제목 옆에 반영 날짜를 표기한다.

## 1. 컨텍스트 목록

| 컨텍스트 | 영문 | 성격 |
|---|---|---|
| 숙소 카탈로그 | Catalog | 호스트가 판매할 숙소와 객실 타입을 정의 |
| 재고와 요금 | Inventory | 날짜별 판매 가능 수량과 단가 |
| 프로모션 | Promotion | 자동 적용 할인 정책 |
| 예약 | Booking | 예약 생성(재고 선점 포함)과 확정, 취소, 만료 |
| 결제 | Payment | 외부 PG와의 결제 요청과 결과 기록 |
| 검색 | Search | 읽기 모델. 컨텍스트가 아님 |

이 5분할은 2026-09-02 블라인드 백지 재도출 검증을 통과했다. 쟁점 2건(요금의 소속, 결제 분리)과 판정 근거는 04-6-o2o-blind-rederivation.md 4절.

## 2. 컨텍스트별 커맨드와 이벤트 배분 (2026-09-01 v4 추가)

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

## 3. 다음 단계

Step 5 컨텍스트 맵에서 이 여섯 사이의 화살표(방향, 동기/비동기, 관계 유형)를 확정한다. 입력은 04-5의 경계 넘는 정책 화살표와 04-6의 관계표 후보, 공유 커널 판정.
