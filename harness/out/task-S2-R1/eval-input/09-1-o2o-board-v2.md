# O2O 이벤트 스토밍 보드 v2 (09-1)

최초 작성: 2026-09-08 (반입)
최종 갱신: 2026-09-08 (반입)
보드 표기 날짜: 2026-09-06. 보드 범례 상자의 표기는 v2 표기 (치명 3건 반영, 2026-09-06)
원본: FigJam 보드 https://www.figma.com/board/ffhYVMh8awMBfqLinXFMDY
반입 방식: 사용자가 제공한 보드 렌더 이미지 1장을 스티키 단위로 전사

## 0. 반입 주의 (이 파일의 지위)

| 항목 | 내용 |
|---|---|
| 이 파일은 무엇인가 | 보드 v2의 이미지 전사본이다. FigJam export 원본이 아니다 |
| 왜 구분하는가 | project-sync의 다른 파일은 프로젝트 계열 원문 사본이다. 이 파일만 전사본이라 대조 근거의 등급이 다르다 |
| 전사한 것 | 스티키의 영문 이름, 한국어 문구, 배지, 테두리 종류, 섹션 배치 |
| 전사하지 않은 것 | 좌표, 정확한 hex 색상, 연결선의 곡률, 보드의 다른 영역 |
| 다툼이 생기면 | FigJam 원본이 정본이다. 이 파일은 원본을 다시 열 때까지의 대조용이다 |
| 지금 어디 있나 | harness/out/import/. 반입 후보다. project-sync가 아니다 |
| 왜 여기 있나 | .claude/settings.json의 deny에 Edit(./harness/project-sync/**)가 있어 쓰기가 거부됐다. 2026-09-08 사용자가 out에 먼저 두는 안을 선택했다 |
| 어떻게 넘어가나 | 사용자가 이 파일을 harness/project-sync/09-1-o2o-board-v2.md로 옮기면 반입이 끝난다. 옮기기 전까지 이 파일은 대조 근거가 아니다 |

## 1. 보드 범례 상자 (전사)

표기 이름: v2 표기 (치명 3건 반영, 2026-09-06)

| 표기 | 보드에 적힌 뜻 |
|---|---|
| 회색 카드 | 근거 메모: 경계와 트랜잭션 근거. 상태를 바꾸지 않는다 |
| 분홍 카드 | 핫스팟: 아직 결정하지 않은 지점 |
| 초록 배지 같은 트랜잭션 | 컨텍스트를 넘지만 호출자의 트랜잭션 안에서 실행되는 내부 호출(강한 일관성). 없는 보라 카드는 이벤트 구독(결과적 일관성) |
| ×N | 숙박일 수만큼 반복. 전부 아니면 전무 |
| (HELD) | 가드. Booking 잠금 후 상태가 HELD일 때만 실행 |

점선 테두리는 범례 상자에 정의가 없다. 보드에서 점선이 붙은 스티키는 2절부터 6절의 테두리 칸에 점선으로 표시했다.

## 2. Catalog(숙소 카탈로그)

액터 레인: 호스트

| 커맨드 | 커맨드 문구 | 이벤트 | 이벤트 문구 | 테두리 |
|---|---|---|---|---|
| RegisterProperty | 숙소를 등록하라 | PropertyRegistered | 숙소 등록됨 | 실선 |
| UpdateProperty | 숙소 정보를 수정하라 | PropertyUpdated | 숙소 정보 수정됨 | 실선 |
| CloseProperty | 숙소 판매를 종료하라 | PropertyClosed | 숙소 판매 종료됨 | 점선 |
| DeleteProperty | 종료된 숙소를 삭제하라 | PropertyDeleted | 숙소 삭제됨 | 점선 |
| RegisterRoomType | 객실타입을 등록하라 | RoomTypeRegistered | 객실타입 등록됨 | 실선 |
| UpdateRoomType | 객실타입을 수정하라 | RoomTypeUpdated | 객실타입 수정됨 | 실선 |
| CloseRoomType | 객실타입 판매를 종료하라 | RoomTypeClosed | 객실타입 판매 종료됨 | 점선 |
| DeleteRoomType | 종료된 객실타입을 삭제하라 | RoomTypeDeleted | 객실타입 삭제됨 | 점선 |

## 3. 재고와 요금

액터 레인: 호스트, 그리고 예약 컨텍스트(초록 카드)

### 3-1. 호스트 레인

| 커맨드 | 커맨드 문구 | 이벤트 | 이벤트 문구 | 테두리 |
|---|---|---|---|---|
| OpenInventory | 기간 재고를 개설하라 | InventoryOpened | 재고 개설됨 | 실선 |
| AdjustInventory | 재고를 조정하라 | InventoryAdjusted | 재고 조정됨 | 실선 |
| RegisterRate | 날짜별 요금을 등록하라 | RateRegistered | 요금 등록됨 | 실선 |
| AdjustRate | 요금을 조정하라 | RateAdjusted | 요금 조정됨 | 실선 |

### 3-2. 예약 컨텍스트 레인 (초록 카드. 네 행 모두 같은 트랜잭션 배지)

| 호출 시점 카드 | 커맨드 | 커맨드 문구 | 이벤트 | 이벤트 문구 | 테두리 |
|---|---|---|---|---|---|
| 예약 컨텍스트 RequestBooking 처리 중 | HoldInventory ×N | 재고를 선점하라 (체크인부터 체크아웃 전일까지, 전부 아니면 전무) | InventoryHeld | 재고 선점됨 | 실선 |
| 같은 행 | 같은 커맨드 | 같은 문구 | InventoryHoldRejected | 선점 거부됨 (재고 부족) | 점선 |
| 예약 컨텍스트 P3 예약 확정 시 | CommitInventory ×N | 선점을 확정하라 | InventoryCommitted | 선점이 확정됨 | 실선 |
| 예약 컨텍스트 P5 예약 만료 시 | ReleaseInventory ×N | 재고를 반환하라 (선점분) | InventoryReleased | 재고 반환됨 | 실선 |
| 예약 컨텍스트 P7 예약 취소 시 | ReleaseInventory ×N | 재고를 반환하라 (판매분) | InventoryReleased | 재고 반환됨 | 실선 |

HoldInventory 하나가 이벤트 둘에 걸친다. 성공은 InventoryHeld, 거부는 InventoryHoldRejected다.

## 4. 프로모션

액터 레인: 운영자

| 커맨드 | 커맨드 문구 | 이벤트 | 이벤트 문구 | 테두리 |
|---|---|---|---|---|
| CreatePromotion | 프로모션을 생성하라 | PromotionCreated | 프로모션 생성됨 | 실선 |
| UpdatePromotion | 프로모션을 수정하라 | PromotionUpdated | 프로모션 수정됨 | 실선 |
| ClosePromotion | 프로모션을 종료하라 | PromotionClosed | 프로모션 종료됨 | 실선 |
| DeletePromotion | 종료된 프로모션을 삭제하라 | PromotionDeleted | 프로모션 삭제됨 | 점선 |

## 5. 예약

액터 레인: 이용자

### 5-1. 이용자 커맨드

| 커맨드 | 커맨드 문구 | 이벤트 | 이벤트 문구 | 테두리 |
|---|---|---|---|---|
| RequestBooking | 예약을 요청하라 (멱등키 포함) | BookingCreated | 예약 생성됨 (HELD) | 실선 |
| 같은 커맨드 | 같은 문구 | BookingRequestRejected | 예약 요청 거부됨 (선점 거부, 인원 초과) | 점선 |
| CancelBooking | 예약을 취소하라 | BookingCanceled | 예약 취소됨 | 실선 |

### 5-2. 정책 카드 (보라)

| 정책 카드 문구 | 번호 | 가드 | 실행 커맨드 | 이벤트 | 배지 |
|---|---|---|---|---|---|
| 결제 승인 시 | P1 | 가드: 예약이 HELD | ConfirmBooking 예약을 확정하라 (HELD만) | BookingConfirmed 예약 확정됨 (Commit 커밋 후 발행) | 없음 |
| 결제 3회 실패 시 | P4 | 가드: 예약이 HELD | ExpireBooking 예약을 만료하라 (HELD만) | BookingExpired 예약 만료됨 | 없음 |
| TTL 만료 시 (스케줄러가 감지) | T1 | 가드: 예약이 HELD | ExpireBooking 예약을 만료하라 (HELD만) | BookingExpired 예약 만료됨 | 없음 |
| 예약 확정 시 | P3 | 없음 | 재고 CommitInventory ×N | 3절 참조 | 같은 트랜잭션 |
| 예약 만료 시 | P5 | 없음 | 재고 ReleaseInventory ×N (선점분) | 3절 참조 | 같은 트랜잭션 |
| 예약 취소 시 | P7 | 없음 | 재고 ReleaseInventory ×N (판매분) | 3절 참조 | 같은 트랜잭션 |
| 예약 취소 시 | P6 | 없음 | 결제 RefundPayment | 6절 참조 | 없음 |
| 결제 승인 시 (예약이 이미 만료됨) | P2 | 예약이 이미 만료됨 | 결제 RefundPayment | 6절 참조 | 없음 |

정책 번호는 보드 표기 그대로다. P1부터 P7과 T1이며 T2는 보드에 없다.

## 6. 결제

액터 레인: 이용자, 그리고 PG (Mock) 분홍 카드

| 호출 주체 | 커맨드 | 커맨드 문구 | 이벤트 | 이벤트 문구 | 테두리 |
|---|---|---|---|---|---|
| 이용자 | RequestPayment | 결제를 요청하라 | PaymentRequested | 결제 요청됨 | 실선 |
| PG (Mock) | RecordPaymentApproval | 결제 승인을 기록하라 | PaymentApproved | 결제 승인됨 | 실선 |
| PG (Mock) | RecordPaymentFailure | 결제 실패를 기록하라 | PaymentFailed | 결제 실패됨 | 실선 |
| 예약 컨텍스트 P2 승인 지연 시, P6 예약 취소 시 (초록 카드) | RefundPayment | 환불을 처리하라 | PaymentRefunded | 환불 완료됨 | 실선 |
| 같은 행 | 같은 커맨드 | 같은 문구 | RefundFailed | 환불 실패됨 | 점선 |

SettlePayment는 보드에 없다.

## 7. 근거 메모 (회색 카드 전사)

| 번호 | 제목 | 본문 |
|---|---|---|
| 1 | 생성 트랜잭션 | 재고 N행 선점(체크인부터 체크아웃 전일까지) + Booking 생성 = 한 트랜잭션. 전부 아니면 전무. 선점 거부면 Booking 없음. 멱등키 유일(같은 키 재요청은 기존 예약 반환) |
| 2 | 확정 트랜잭션 | Booking 잠금 + confirm + Commit ×N = 한 트랜잭션. Commit 실패면 전체 롤백, HELD 유지. BookingConfirmed는 커밋 후 발행. 완료 조건 = InventoryCommitted |
| 3 | 경합 규칙 | P1, P4, T1은 Booking 잠금 후 상태 재검사. HELD 아니면 무해 무시(재고, 결제 후속 없음). 승자 = 먼저 커밋한 전이. CONFIRMED에서 EXPIRED로 가는 전이 없음 |

## 8. 핫스팟 (분홍 카드 전사)

| 제목 | 본문 |
|---|---|
| 승인과 TTL 동시 도착 | 잠금 안에서 승인 기록이 있으면 확정 우선인가, 먼저 커밋한 쪽이 이기는가. 08-3.8절 결정 대기. 결정 전까지 T1 가드는 HELD + expiresAt 경과 |

## 9. 보드 총계

| 컨텍스트 | 커맨드 | 이벤트 |
|---|---|---|
| Catalog(숙소 카탈로그) | 8 | 8 |
| 재고와 요금 | 7 | 8 |
| 프로모션 | 4 | 4 |
| 예약 | 4 | 5 |
| 결제 | 4 | 5 |
| 합계 | 27 | 30 |

재고와 요금의 커맨드 7은 OpenInventory, AdjustInventory, RegisterRate, AdjustRate, HoldInventory, CommitInventory, ReleaseInventory다. ReleaseInventory는 스티키가 둘이지만 같은 커맨드라 하나로 센다. InventoryReleased도 같은 이유로 하나로 센다.

## 10. 이 보드가 문서 계열과 다른 지점 (대조 결과, 2026-09-08)

반입 목적이 대조이므로 결과를 여기 남긴다. 판정과 반영은 이 파일이 아니라 결정과 후보에서 한다.

### 10-1. 보드에만 있는 것 13개

| 컨텍스트 | 항목 | 종류 | 문서 계열 상태 |
|---|---|---|---|
| Catalog | CloseProperty, PropertyClosed | 커맨드, 이벤트 | 어느 문서에도 없음 |
| Catalog | DeleteProperty, PropertyDeleted | 커맨드, 이벤트 | 어느 문서에도 없음 |
| Catalog | CloseRoomType, RoomTypeClosed | 커맨드, 이벤트 | 어느 문서에도 없음 |
| Catalog | DeleteRoomType, RoomTypeDeleted | 커맨드, 이벤트 | 어느 문서에도 없음 |
| 프로모션 | DeletePromotion, PromotionDeleted | 커맨드, 이벤트 | 어느 문서에도 없음 |
| 재고와 요금 | InventoryHoldRejected | 이벤트 | 미결로 등재됨. 이 반입 이전에 하네스가 추적하던 유일한 항목 |
| 예약 | BookingRequestRejected | 이벤트 | 없음. 06-4 v4는 OccupancyExceeded와 InventoryShortage 예외로 모델링 |
| 결제 | RefundFailed | 이벤트 | 없음. 08-1과 08-2가 부재를 지적했고 08-3 C-4가 REFUND_PENDING 상태로 다르게 결정 |

커맨드 5개와 이벤트 8개다.

### 10-2. 문서 계열에만 있는 것 4건

| 항목 | 문서 근거 | 보드 상태 |
|---|---|---|
| SettlePayment | 08-3 C-1 수용, 04 v10에서 커맨드 23 | 없음 |
| T2 후속 미완 결제 순찰 | 04-5 v2 3절 신설 | 없음 |
| 정책 번호 재배정 | 04-5 v2 0절. P2가 P1에 흡수되고 뒤가 한 칸씩 당겨짐 | 구 번호 P1부터 P7과 T1 사용 |
| 확정 우선 결정 | 08-3 C-7과 11번, 2026-09-07 수용 | 8절 핫스팟에 결정 대기로 남음. 7절 근거 메모 3도 확정 우선 예외가 빠짐 |

보드는 2026-09-06이고 결정표는 2026-09-07이다. 네 건 전부 하루 차이에서 나온다.

### 10-3. 06 보드 범례와 충돌하는 표기 3건

| 보드 표기 | document의 06-o2o-board-legend.md v6 정의 |
|---|---|
| 초록 스티키를 예약 컨텍스트 호출자에 사용 | 초록은 읽기 모델. 다른 컨텍스트는 연파랑 |
| 점선 테두리 | 정의 없음 |
| 이 보드의 레이아웃 | 3절 영역 표가 영역 4까지만 기술. 이 보드에 해당하는 영역 번호가 없음 |

### 10-4. 일치하는 것

컨텍스트 5개의 이름과 배치, 정책 8갈래의 트리거와 실행 커맨드 연결, ×N 반복, 같은 트랜잭션 경계 3곳(생성, 확정, 만료와 취소), HELD 가드, 액터 이름 이용자, PG를 외부 시스템으로 두는 배치가 문서 계열과 일치한다. 같은 트랜잭션 경계 3곳은 06-2 v4의 A1, A2, A3 강한 일관성 채택과 같다.

## 11. 이 반입이 여는 결정

| 번호 | 결정할 것 | 걸린 문서 |
|---|---|---|
| 1 | Close와 Delete 계열 10개를 v1 범위에 넣는가 | 02, 03, 04, 05-2, 05-3, 06-2, 06-4, 11 |
| 2 | 거부 계열 3개(InventoryHoldRejected, BookingRequestRejected, RefundFailed)를 도메인 이벤트로 두는가 예외와 상태로 두는가 | 03, 04-5, 06-4. 이벤트 수가 22인지 25인지가 여기서 갈림 |
| 3 | 보드를 2026-09-07 결정으로 갱신하는가 2026-09-06 시점 기록으로 두는가 | 06 보드 범례 |
| 4 | 06 보드 범례에 v2 표기(점선, 초록 카드, 영역 번호)를 추가하는가 | 06 보드 범례 |

이 파일은 전사와 대조까지만 담는다. 위 네 건의 판정은 사용자 결정이다.
