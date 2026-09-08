# O2O 정책 식별 초안 (Step 3.5)

최초 작성: 2026-09-02
최종 갱신: 2026-09-02 v1
입력: 03-o2o-event-storming.md 이벤트 22개, 04-o2o-commands-actors.md 커맨드 22개와 발견 3 가설표, 01-o2o-ddd-plan.md Step 3.5 절과 Step 8 입력 초안, 05-o2o-glossary.md 상태 정의
후속: Step 4-2 재대조, Step 5 컨텍스트 맵, Step 8 정책 명세
FigJam 보드: https://www.figma.com/board/ffhYVMh8awMBfqLinXFMDY 영역 3 (정책 스티커 추가 예정)

문서 규칙: 새로 추가되거나 내용이 바뀐 절에는 제목 옆에 반영 날짜를 표기한다.
이 문서의 가드는 전부 [가설]이다. 가드는 애그리거트가 그 상태를 소유한다는 것이 Step 6에서 정해져야 검증되므로 Step 8에서 확정한다.

## 1. 입력 요약

이벤트 22개 중 시스템의 자동 반응이 필요한 것은 5개다. PaymentApproved, PaymentFailed, BookingConfirmed, BookingExpired, BookingCanceled.
발견 3의 다단 체인 5행을 이벤트 단위로 펼치면 이벤트 트리거 정책 7개와 시간 트리거 정책 1개가 나온다. 새로 발명한 정책은 없다.
발신자 없는 커맨드는 없고, 구독자 없는 이벤트 17개는 전부 반응 불필요로 판정했다. 단 결제 REQUESTED 상태의 시간 상한 부재가 유형 1 후보로 하나 나왔다.

## 2. 정책 표 (이벤트 트리거)

정책의 판별 기준: 트리거가 사건이고, 사람이 경계 밖에서 누르지 않으며, 시스템이 반드시 해야 하는 반응이다. 행 단위는 트리거 하나에 대상 컨텍스트 하나로 통일했다. 발견 3의 체인 행을 가른 이유는 5절에 있다.

| 번호 | 이름 | 트리거 | 가드[가설] | 실행 커맨드 | 컨텍스트를 넘는가 | 근거 (사람이 누르지 않는 이유) |
|---|---|---|---|---|---|---|
| P1 | 결제 승인 시 예약 확정 | PaymentApproved | status == HELD | ConfirmBooking | 넘음. 결제 이벤트를 예약이 구독 | 게스트는 승인 시점을 모른다. PG 콜백은 사람 부재 중에도 도착한다 |
| P2 | 승인 지연 시 자동 환불 | PaymentApproved | status == EXPIRED | RefundPayment | 넘음. 결제 이벤트를 예약이 구독하고 예약이 결제로 커맨드를 보냄 | TTL이 이긴 경합의 뒤처리. 개입할 사람이 없다. 가드를 != HELD가 아니라 == EXPIRED로 좁힌 이유는 중복 콜백이 CONFIRMED 예약을 환불하는 오폭 방지 (03 문서 5-3) |
| P3 | 확정 시 재고 확정 | BookingConfirmed | 없음 | CommitInventory x N | 넘음. 예약 이벤트에서 재고 커맨드로 | 재고 수량은 재고 컨텍스트만 바꾼다는 원칙. 확정과 수량 이동 사이에 사람의 판단이 없다 |
| P4 | 결제 실패 시 만료 | PaymentFailed | attemptCount >= 3 | ExpireBooking(PAYMENT_FAILED) | 넘음. 결제 이벤트에서 예약 커맨드로 | 3회 도달은 시스템이 세는 상태다. attemptCount가 이벤트에 실려 오므로 가드 평가가 경계를 넘지 않는다 |
| P5 | 만료 시 재고 반환 | BookingExpired | 없음 | ReleaseInventory x N (heldCount 반환) | 넘음. 예약 이벤트에서 재고 커맨드로 | HELD 종료 시 재고 반환 보장은 시스템 의무다 (01 문서 Step 7 표 4행) |
| P6 | 취소 시 환불 | BookingCanceled | 없음 | RefundPayment | 넘음. 예약 이벤트에서 결제 커맨드로 | 게스트는 취소만 누른다. 환불은 취소의 의미에 포함된 후속 의무다 |
| P7 | 취소 시 재고 반환 | BookingCanceled | 없음 | ReleaseInventory x N (soldCount 반환) | 넘음. 예약 이벤트에서 재고 커맨드로 | P6과 동일. P5와 반환 대상 카운트가 다르다는 점이 핫스팟 2의 원인 파라미터 논의로 간다 |

P2의 정책 위치는 [가설]이다. 트리거와 실행 커맨드가 둘 다 결제 컨텍스트 것이지만 가드가 예약 상태를 읽으므로, 예약이 구독해 자기 상태를 보고 결제에 RefundPayment를 보내는 배치가 자연스럽다. 04 문서도 RefundPayment를 예약이 보내는 커맨드로 분류했다. 소속 확정은 이번 단계 범위 밖이다.

P4의 status == HELD는 정책 가드가 아니라 ExpireBooking의 선행조건으로 두었다 (04 문서 v8). 정책 가드에 두는지 커맨드 선행조건에 두는지는 Step 8에서 확정한다.

## 3. 시간 트리거 정책 표

이벤트가 없어 1절 순회로는 나오지 않는 것들이다.

| 번호 | 이름 | 트리거 | 가드[가설] | 실행 커맨드 | 컨텍스트를 넘는가 | 근거 |
|---|---|---|---|---|---|---|
| T1 | TTL 만료 | 시간 (스케줄러가 HELD 예약을 폴링) | status == HELD, expiresAt <= now | ExpireBooking(TTL_EXPIRED) | 안 넘음. 예약 컨텍스트 내부 | 트리거가 사람이 아니라 시간이다. BookingCreated 구독이 아니라 폴링인 이유는 만료가 이벤트 발생 시점이 아니라 시간 경과로 판정되기 때문이다 |

시간 트리거 후보로 검토했으나 채택하지 않은 것.

| 후보 | 판정 |
|---|---|
| 프로모션 자동 종료 | v1 범위 밖으로 확정돼 있다 (04 문서, 05 문서 8절) |
| 결제 시도 타임아웃 (REQUESTED 상한) | [추측] 4-3의 유형 1 결함 후보. 도입 여부를 Step 8에서 판정한다. 아래 참조 |

## 4. 커버리지 검사 결과

### 4-1. 구독자 없는 이벤트 17개와 판정

| 이벤트 | 판정 | 이유 |
|---|---|---|
| PropertyRegistered, PropertyUpdated, RoomTypeRegistered, RoomTypeUpdated | 불필요 | 도메인 반응 없음. 검색 읽기 모델 갱신은 프로젝션이지 정책이 아니다 (발견 4) |
| InventoryOpened, InventoryAdjusted | 불필요 | 동일. AdjustInventory의 하한 검사는 커맨드 선행조건이지 반응이 아니다 |
| RateRegistered | 불필요 | 도메인 반응 없음 |
| RateAdjusted | 불필요 | 확정 예약이 영향받지 않는 것은 반응이 아니라 PriceSnapshot 시간적 불변식으로 보장된다. 반응이 없는 것이 곧 요구사항이다 |
| PromotionCreated, PromotionUpdated, PromotionClosed | 불필요 | 예약 흐름은 프로모션을 읽기만 한다 (발견 5) |
| InventoryHeld, InventoryCommitted, InventoryReleased | 불필요 | 예약 컨텍스트가 동기 커맨드의 반환값으로 결과를 아는 구조다 (발견 1). 이벤트 구독으로 후속이 이어지지 않는다. 단 이 판정은 3-2에서 동기 호출이 유지된다는 전제에 종속된다. 비동기로 바뀌면 InventoryHeld 구독 정책이 필요해진다 |
| BookingCreated | 불필요 | RequestPayment는 게스트 커맨드다 (v7에서 자동 발송으로 바꿨다가 재시도 소실로 v8 원복). TTL 시계는 생성 시 expiresAt 기록과 T1 폴링으로 처리되므로 구독이 필요 없다 |
| PaymentRequested | 불필요 | PG 호출은 RequestPayment 처리 내부의 인프라 동작이다. 도메인 커맨드 22개에 대응물이 없다 |
| PaymentRefunded | 불필요 | 알림과 정산이 v1 범위 밖이라 후속이 없다 |

빠진 정책으로 판정한 이벤트: 없음.

### 4-2. 발신자 없는 커맨드

없음. 22개 전부 발신자가 있다.

| 발신자 유형 | 커맨드 | 수 |
|---|---|---|
| 사람 (호스트) | RegisterProperty, UpdateProperty, RegisterRoomType, UpdateRoomType, OpenInventory, AdjustInventory, RegisterRate, AdjustRate | 8 |
| 사람 (운영자) | CreatePromotion, UpdatePromotion, ClosePromotion | 3 |
| 사람 (게스트) | RequestBooking, RequestPayment(예약이 중계), CancelBooking | 3 |
| 외부 시스템 (PG, ACL 번역) | RecordPaymentApproval, RecordPaymentFailure | 2 |
| 다른 컨텍스트 (예약의 앱 서비스) | HoldInventory | 1 |
| 정책 | ConfirmBooking(P1), CommitInventory(P3), ExpireBooking(P4, T1), ReleaseInventory(P5, P7), RefundPayment(P2, P6) | 5 |

CommitInventory, ReleaseInventory, RefundPayment는 04 문서에서 예약 컨텍스트가 보내는 커맨드로 분류됐고 여기서는 정책이 발신자다. 모순이 아니다. 정책이 예약 컨텍스트 안에 살면서 앱 서비스로 구현되는 그림이고, 그 호출이 동기 같은 트랜잭션인지는 3-2에서 정한다.

### 4-3. 세 유형 검사 결과

유형 1. 짝 없는 전이.

| 검사 대상 | 결과 |
|---|---|
| BookingStatus | 구멍 없음. HELD에서 나가는 길 둘(P1이 CONFIRMED로, P4와 T1이 EXPIRED로), CONFIRMED에서 나가는 길 하나(CancelBooking). CANCELED와 EXPIRED는 의도된 종착. CONFIRMED 이후 체크인 쪽 종착 부재는 핫스팟 1로 이미 등재돼 있고 v1 범위 밖 |
| heldCount | 구멍 없음. 올리는 길(HoldInventory)과 내리는 길 둘(P3의 Commit, P5의 Release)이 짝이 맞는다. 확정 예약의 재고가 반환되던 결함은 CONFIRMED에서 EXPIRED로 전이 불가 규칙과 ExpireBooking 선행조건 status == HELD로 막혀 있음을 재확인했다. 단 InventoryHeld x k까지 성공하고 BookingCreated 전에 실패하면 대응하는 Booking이 없는 고아 선점이 남는데, 이것은 3-2가 동기 같은 트랜잭션이면 롤백으로 구조적으로 막히고 다른 안이면 보상 문제가 된다. 3-2 결정(Step 6)에 종속 |
| soldCount | 구멍 없음. 올리는 길(Commit)과 내리는 길(P7의 Release). 체크아웃 후에도 남는 것은 핫스팟 1과 같은 자리 |
| PaymentStatus | 결함 후보 1건. REQUESTED에서 나가는 길이 PG 콜백뿐이다. 콜백이 영영 오지 않으면 시도가 REQUESTED에 머물고, RequestPayment 선행조건(REQUESTED 상태의 시도가 없을 것, 01 문서 4-2)이 게스트의 재시도를 막는다. 예약 쪽은 T1이 결국 만료시키므로 재고 누수는 없지만, HELD가 살아 있는 동안 게스트가 재시도조차 못 하는 구간이 생긴다. [추측] REQUESTED에 시간 상한을 두는 결제 시도 타임아웃 정책이 필요할 수 있다. 검증 방법: TTL 값과 PG 응답 시간 상한을 비교해 재시도 불가 구간의 길이를 계산해 본다. 도입 판정은 Step 8 |

유형 2. 중복 도착.

| 검사 대상 | 결과 |
|---|---|
| PaymentApproved 중복 (P1, P2) | 닫혀 있음. 1차 방어는 결제 컨텍스트의 pgTransactionId 멱등(이벤트 재발행 자체를 막음), 2차 방어는 P1 가드 == HELD와 P2 가드 == EXPIRED. CONFIRMED 예약에 승인이 다시 와도 두 정책 모두 침묵한다 |
| PaymentFailed 중복 (P4) | ExpireBooking 선행조건 status == HELD가 두 번째를 막는다. 단 거부가 오류인지 무시인지는 멱등 규칙이라 Step 8 |
| BookingConfirmed 중복 (P3), BookingExpired 중복 (P5), BookingCanceled 중복 (P6, P7) | 결함 후보 1건. Commit과 Release가 두 번 실행되면 수량이 이중 이동한다. heldCount가 음수가 되는 방향은 totalCount >= soldCount + heldCount 불변식으로도 잡히지 않는다. 3-2가 동기 같은 트랜잭션이면 이벤트가 프로세스 내 호출이라 중복 전달 자체가 없지만, 그 결정 전까지는 열린 구멍이다. 멱등 규칙은 이번 단계 제외 항목이므로 Step 8로 넘긴다 |

유형 3. 경합.

| 검사 대상 | 결과 |
|---|---|
| T1 대 P1 (TTL 만료 대 결제 승인) | 닫혀 있음. 두 정책이 같은 Booking을 반대 방향으로 민다. 어느 쪽이 먼저 커밋하든 진 쪽은 가드에서 침묵하고, T1이 이긴 경우의 잔여 승인은 P2가 환불로 흡수한다. 단 같은 Booking 인스턴스에 대한 커맨드가 직렬화된다는 전제가 깔려 있고 그 락 전략은 Step 6과 7 소관이므로 [가설] 표시 |
| P4 대 T1 | 경합이지만 안전. 둘 다 EXPIRED로 미는 같은 방향이고 늦은 쪽은 선행조건에서 침묵한다 |
| AdjustInventory 대 P3, P5, P7 | 호스트와 정책이 같은 DailyInventory를 동시에 민다. 방향 충돌이 아니라 수량 경합이고, AdjustInventory 하한 선행조건과 핫스팟 6, Step 7 계약으로 이미 추적되고 있다. 새 정책은 불필요 |

## 5. 04 문서 발견 3 가설표와의 차이

추가된 행: 없음. 이벤트 순회에서 새 반응 규칙은 나오지 않았다.
삭제된 행: 없음.
바뀐 행: 다섯 행 전부, 다단 체인을 이벤트 단위로 분해했다.

| 발견 3의 행 | 이 문서 | 가른 이유 |
|---|---|---|
| PaymentApproved에서 ConfirmBooking 거쳐 CommitInventory x N | P1 + P3 | 체인의 둘째 홉은 트리거가 PaymentApproved가 아니라 BookingConfirmed다. 묶어 두면 BookingConfirmed가 구독자 없는 이벤트로 보이는데, 확정 예약 재고 결함이 정확히 그 유형이었다. 갈라야 커버리지 검사가 그 자리를 감시한다. 또 묶인 표기는 ConfirmBooking이 가드에서 거부됐을 때 CommitInventory만 나가는 경로를 그릴 수 있게 한다 |
| PaymentFailed에서 ExpireBooking 거쳐 ReleaseInventory x N | P4 + P5 | 같은 이유. 재고 반환의 트리거는 BookingExpired다 |
| 스케줄러의 ExpireBooking에서 BookingExpired 거쳐 ReleaseInventory x N | T1 + P5 | 같은 이유에 더해, 두 체인이 BookingExpired로 수렴하므로 재고 반환 정책이 두 번 적히던 중복이 P5 하나로 합쳐진다. 원인이 무엇이든 만료면 반환한다는 규칙이 한 행이 된다 |
| BookingCanceled에서 RefundPayment와 ReleaseInventory x N | P6 + P7 | 대상 컨텍스트가 다르고(결제, 재고), P7의 반환 대상(soldCount)이 P5(heldCount)와 달라 핫스팟 2의 원인 파라미터 논의와 직결된다. 행 단위를 트리거 하나에 대상 컨텍스트 하나로 통일했다. Step 8의 사가 판정에서 P6과 P7을 한 정책으로 재합칠 수 있다 |
| 승인 지연 (PaymentApproved에서 RefundPayment) | P2 그대로 | 변경 없음 |

01 문서 Step 8 입력 초안 6행과 대조하면 이 문서는 8행이다. 차이는 위 분해와 동일하다 (초안 1행이 P1과 P3으로, 초안 6행이 P6과 P7로).

## 6. 확정한 것 / 가설로 남긴 것 / 넘기는 것

확정한 것.
- 이벤트 트리거 정책 7개와 시간 트리거 정책 1개의 존재, 각 트리거와 실행 커맨드의 연결.
- 발신자 없는 커맨드 없음. 구독자 없는 이벤트 17개 전부 반응 불필요.
- RequestPayment는 정책이 아니라 게스트 커맨드다 (v8 결정 유지, 이번 순회에서 재확인).
- 정책 화살표가 경계를 넘는 지점: 결제에서 예약으로 이벤트 구독 2개(PaymentApproved, PaymentFailed), 예약에서 재고로 커맨드 3갈래(P3, P5, P7), 예약에서 결제로 커맨드 2갈래(P2, P6). 예약이 모든 정책의 허브다.

가설로 남긴 것.
- 모든 가드 (P1, P2, P4, T1의 조건식과 P3, P5, P6, P7의 가드 없음 판정 자체도 포함).
- P2의 정책 위치 (예약 구독 배치).
- P4의 status == HELD를 정책 가드와 커맨드 선행조건 중 어디에 둘지.
- InventoryHeld 등 재고 이벤트의 구독 불필요 판정 (3-2에서 동기 유지가 전제).
- 유형 3의 Booking 커맨드 직렬화 전제.

Step 4-2와 Step 5에 넘기는 것.
- 위 경계 넘는 화살표 목록. 컨텍스트 배분(05 문서 0-1절)과 어긋나는 지점은 발견되지 않았고, Step 5에서 이 화살표가 통합 지점과 일치하는지 대조한다.

Step 8에 넘기는 것.
- 가드 전체 확정과 사가 판정 (3-2 결정 이후).
- Commit과 Release의 중복 실행 방지 (유형 2 결함 후보).
- 결제 시도 타임아웃 도입 여부 (유형 1 결함 후보, REQUESTED 상한).
- ExpireBooking 중복 거부의 의미론 (오류인가 무시인가).

용어 사전 반영 확인: 이번 단계에서 새로 생긴 도메인 용어는 없다. 정책 이름 8개는 기존 용어의 조합이고 Step 8 확정 후 05 문서에 정책 절을 추가할지 정한다.
