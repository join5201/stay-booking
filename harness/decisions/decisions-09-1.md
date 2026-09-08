# 09-1 보드 v2 델타 사용자 결정표

최초 작성: 2026-09-08
최종 갱신: 2026-09-08 (결정 4행 기재. 결정 열 공란 0)
출처: harness/project-sync/09-1-o2o-board-v2.md 10절, harness/tasks/task-S2.md 7절
계약: task-S2. 승인 join5201, 2026-09-08

상태: 4행 전부 결정됨. 계약 승인과 같은 자리에서 사용자가 추천 넷을 그대로 채택했다.

## 1. 결정표

| 번호 | 결정 대상 | 안 | 추천 | 사용자 결정 | 이유 |
|---|---|---|---|---|---|
| D-1 | Close와 Delete 계열 10건의 v1 범위 | 가 넣는다 / 나 넣지 않는다 | 나 | 나 | 02 기능 목록에 숙소 종료와 삭제가 없고, 11 API에 DELETE 경로가 하나도 없으며, 리뷰 다섯 곳 중 어디도 이 부재를 결함으로 지적하지 않았다 |
| D-2 | 거부 계열 3건의 표현 방식 | 가 셋 다 이벤트 / 나 현재 표현 유지 / 다 일부만 승격 | 나 | 나 | 06-4 v4가 이미 예외로 명세했고 그 예외는 같은 트랜잭션 안의 롤백 신호라 발행할 이벤트가 남지 않는다. RefundFailed를 올리면 08-3 C-4의 REFUND_PENDING 결정이 뒤집힌다 |
| D-3 | 보드의 시점 처리 | 가 09-07 결정으로 갱신 / 나 09-06 시점 기록 | 나 | 나 | 06 범례 3절이 이전 영역을 수정하지 않는 규칙을 이미 갖고 있다. 보드 v2도 같은 규칙 아래 둔다 |
| D-4 | 06 범례에 v2 표기 등재 | 가 등재한다 / 나 등재하지 않는다 | 가 | 가 | 초록이 읽기 모델과 다른 컨텍스트 호출자 두 뜻을 갖고 있고 점선은 정의가 아예 없다. 범례가 제 일을 못 하는 상태다 |

## 2. 델타 13건의 처분

| 델타 | 결정 | 처분 |
|---|---|---|
| CloseProperty, PropertyClosed | D-1 나 | v1 범위 밖. 03 v12 11-1절에 판정과 근거 기록 |
| DeleteProperty, PropertyDeleted | D-1 나 | 같음 |
| CloseRoomType, RoomTypeClosed | D-1 나 | 같음 |
| DeleteRoomType, RoomTypeDeleted | D-1 나 | 같음 |
| DeletePromotion, PromotionDeleted | D-1 나 | 같음 |
| InventoryHoldRejected | D-2 나 | 도메인 이벤트로 올리지 않는다. 06-4 v4의 InventoryShortage 예외로 유지 |
| BookingRequestRejected | D-2 나 | 올리지 않는다. OccupancyExceeded와 InventoryShortage 예외로 유지 |
| RefundFailed | D-2 나 | 올리지 않는다. 08-3 C-4의 REFUND_PENDING 상태와 T2 순찰로 유지 |

커맨드 22개와 이벤트 22개를 유지한다. 08-3 C-1의 SettlePayment로 커맨드가 23이 되는 것은 task-S8 소관이고 이 결정과 무관하다.

## 3. 이 결정이 뒤집은 것

없다. 네 결정 모두 기존 문서의 판정을 유지하는 쪽이다. D-4만 범례에 표기를 더한다.

10-8 100행이 InventoryHoldRejected를 이벤트로 올리는 안을 잡아 두었던 것은 D-2 나로 닫힌다. 그 자리에는 이벤트 대신 11 API의 INVENTORY_UNAVAILABLE 오류 코드와 06-4의 InventoryShortage 예외가 이미 서 있다.

## 4. 이 결정이 여는 산출물

| 산출물 | 근거 결정 | 경로 |
|---|---|---|
| 03 v12 (S2 분기) | D-1 나, D-2 나 | harness/out/task-S2-R1/03-o2o-event-storming.v12.md |
| 06 보드 범례 v7 | D-3 나, D-4 가 | harness/out/task-S2-R1/06-o2o-board-legend.v7.md |

D-3 나는 보드 자체를 고치지 않는다는 결정이라 보드 작업 산출물이 없다.

## 5. 남은 미결

| 항목 | 내용 |
|---|---|
| 숙소 판매 종료의 v2 재검토 | D-1 나로 v1에서 뺐지만 판매 중인 날짜에 재고와 HELD 예약이 걸린 상태의 종료는 핫스팟 6과 같은 자리의 경합이다. v2 후보로 남긴다 |
| 03 판 번호 충돌 | task-S8-R1의 03 v12와 이 Task의 03 v12가 둘 다 document/03 v11에서 갈라졌고 둘 다 미승인이다. 먼저 반영되는 쪽이 v12가 되고 나중 쪽은 그 위에서 v13으로 다시 낸다 |
| S8 R1 후보 9종의 09-1 미반입 문구 | 이 결정으로 사실과 달라졌다. task-S2 계약 3절대로 이 Task는 고치지 않는다. S8 반영 단계에서 고친다 |
