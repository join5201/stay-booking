# O2O 컨텍스트 맵 (Step 5)

최초 작성: 2026-09-02
최종 갱신: 2026-09-08 v2 (08-3 결정 반영: R6 흐르는 것에 SettlePayment와 조회 API 둘 추가, R6 근거 문구 정정, 구독자 없는 이벤트 20개, 정책 번호를 04-5 v2 신 번호로, 도식에 T2)
파일 번호가 06-1인 이유: 06은 보드 범례가 선점하고 있어 Step 5 산출물을 06-1로 둔다.
입력: 05-2-o2o-bounded-contexts.md (노드 6개), 04-5 v2 (경계 넘는 정책 화살표), 04-6-o2o-blind-rederivation.md (관계 유형 후보, 공유 커널 판정), 08-3 6절 C-9와 C-11
후속: Step 9 패키지 구조

문서 규칙: 새로 추가되거나 내용이 바뀐 절에는 제목 옆에 반영 날짜를 표기한다.

## 1. 입력 요약

노드는 컨텍스트 5개와 읽기 모델 1개(검색), 외부 시스템 1개(PG)다. 스케줄러는 예약 내부 메커니즘이라 노드가 아니다.

경계를 넘는 흐름은 Step 3.5에서 전부 드러났다. 결제에서 예약으로 이벤트 구독 2, 예약에서 재고로 커맨드 3, 예약에서 결제로 커맨드 3과 조회 2, 그리고 읽기 4갈래다.

동기와 비동기, 트랜잭션 묶음은 06-2 v5 4절과 06-4 v5 2-3에서 확정됐다. 이 문서의 [가설] 표기 중 그 둘이 다룬 항목은 해제됐다.

## 2. 관계표 (2026-09-08 v2: R6 갱신)

U가 Upstream(모델을 제공하는 쪽), D가 Downstream(그 모델에 의존하는 쪽)이다.

| # | U에서 D로 | 흐르는 것 | 동기와 비동기 | 관계 유형[가설] | 근거 |
|---|---|---|---|---|---|
| R1 | 카탈로그에서 재고와 요금으로 | roomTypeId 존재 확인 읽기 (개설, 등록 시) | 동기 읽기 | Conformist (카탈로그 어휘 그대로 수용, 번역 없음) | OpenInventory와 RegisterRate가 RoomType 없이 성립 불가 |
| R2 | 카탈로그에서 예약으로 | maxOccupancy 읽기 (인원 검증), roomTypeId 참조 | 동기 읽기 | Conformist | userCount가 maxOccupancy 이하인지 검증 (05-3) |
| R3 | 카탈로그에서 프로모션으로 | Region 값 읽기 (적용 조건 판정) | 동기 읽기 | Conformist | Region은 카탈로그 소유 (05-1) |
| R4 | 재고와 요금에서 예약으로 | 위로는 날짜별 단가 읽기(스냅샷 동결). 아래로는 HoldInventory ×N, CommitInventory ×N, ReleaseInventory ×N 커맨드 | 셋 다 동기이며 각 경로의 트랜잭션과 같은 트랜잭션 (06-2 v5 4절) | Customer-Supplier (재고가 Supplier). 단가는 스냅샷으로 번역해 동결 | R1 연박 원자성, R2 초과 예약 0, R4 반환 보장. P2, P4, P6 |
| R5 | 프로모션에서 예약으로 | 자동 적용 할인 읽기, 식별자와 이름까지 스냅샷 복사 | 동기 읽기 (예약 생성 시 1회) | ACL 성격 (읽은 값을 예약 소유 PriceSnapshot VO로 번역) | R5 금액 불변. 프로모션에 인스턴스가 없어 적용 사실이 스냅샷에만 남음 |
| R6 | 결제에서 예약으로 | 위로는 PaymentApproved, PaymentFailed 이벤트 구독 (P1, P3). 아래로는 RequestPayment 중계(총액 첨부), RefundPayment(bookingId), SettlePayment(bookingId, settledBy) 커맨드와 paymentFollowUp(bookingId), findFollowUpCandidates(threshold, limit) 조회 (P1, P3, P5, T1, T2) | 콜백 유래 이벤트는 비동기. 커맨드와 조회는 동기이며 호출자의 트랜잭션 안이다 | Customer-Supplier (결제가 Supplier). 모델 의존은 한 방향이다. 예약이 결제를 알고, 결제는 예약을 모른다 | 04 v8 순환 절단. 08-3 C-9. T1과 T2의 가드가 결제를 조회하지만 방향은 여전히 예약에서 결제 한 방향이라 순환이 없다 |
| R7 | 외부 PG에서 결제로 | 승인과 실패 콜백 | 비동기, 중복 도착 전제 | ACL (RecordPaymentApproval과 RecordPaymentFailure로 번역, pgTransactionId 멱등) | 발견 2, 05-3 멱등 규칙 |
| R8 | 카탈로그, 재고와 요금, 프로모션에서 검색으로 | 검색과 조회용 읽기 | 읽기 전용 | 읽기 모델 (Published Language 성격) | 발견 4. 커맨드 없음 |

의존 순환 검사: 없음. 예약과 결제 사이가 유일한 양방향 흐름인데, 모델 지식 기준으로는 결제가 예약을 모르므로 U와 D가 한 방향(결제가 U)으로 정리된다. 08-3이 추가한 SettlePayment 커맨드와 T1, T2의 조회도 모두 예약에서 결제 방향이라 이 판정이 유지된다. 예약과 재고도 커맨드는 예약이 보내지만 모델 지식은 예약이 재고 API를 아는 한 방향이다.

이벤트 고아 검사: 04-5 v2 4-1에서 완료. 구독자 없는 이벤트는 20개다. BookingConfirmed, BookingExpired, BookingCanceled 셋이 v1의 17개에 더해졌다. 후속이 같은 트랜잭션 내부 호출이라 구독자가 필요 없기 때문이다. 20개 전부 불필요 판정을 유지한다.

## 3. 도식 (FigJam 옮기기용, 2026-09-08 v2)

```
카탈로그(U) ──R1 읽기──▶ 재고와 요금(D)
카탈로그(U) ──R2 읽기──▶ 예약(D)
카탈로그(U) ──R3 읽기──▶ 프로모션(D)

재고와 요금(U) ══R4══▶ 예약(D)   단가 읽기(스냅샷)↓, Hold/Commit/Release 커맨드↑
프로모션(U) ──R5 읽기+스냅샷──▶ 예약(D)
결제(U) ══R6══▶ 예약(D)          Approved/Failed 구독↓
                                 RequestPayment 중계, Refund, Settle 커맨드↑
                                 paymentFollowUp, findFollowUpCandidates 조회↑

외부 PG(U) ──R7 ACL──▶ 결제(D)
카탈로그, 재고와 요금, 프로모션 ──R8 읽기 전용──▶ [검색 읽기 모델]

(스케줄러 T1과 T2는 예약 내부. 노드 아님)
```

굵은 화살표(R4, R6)가 시스템 보장 요구사항이 걸린 자리이고, Step 8 정책 명세가 이 두 선 위에서 이뤄졌다.

## 4. 공유 커널 후보 (04-6 판정 승계)

단일 모듈이므로 공유 커널은 공유 패키지로 구현된다. 확정은 Step 9 패키지 구조에서 한다.

| 후보 | 판정 | 근거 |
|---|---|---|
| ID 값 타입 (PropertyId, RoomTypeId, BookingId, UserId 등) | 적합 | 안정, 보편, 단순 3기준 통과 |
| Money | 적합 | 할인 배분 단수 규칙이 06-4 v4에서 확정돼 [가설] 해제 |
| StayPeriod | 적합 | 반열림 규칙(체크인 포함, 체크아웃 미포함)이 05-3에 확정돼 조건 해소 |
| PriceSnapshot | 부적합 | 예약 소유 VO. 공유하면 가격 용어 충돌(05-1)이 코드로 굳는다 |
| BookingStatus | 부적합 | 예약 봉인. 다른 컨텍스트는 알 필요 없음 |
| 반환 원인 구분 | 재고 커맨드 API 소속 [가설]. Step 9 | ReleaseInventory(count, source)로 06-2 v4에서 결정 |

## 5. 확정한 것과 남은 것 (2026-09-08 v2)

확정: 노드 구성(5와 검색과 PG), 관계 8개의 U와 D 방향과 흐르는 것, 순환 없음, 공유 커널 중 ID 타입과 StayPeriod와 Money, R4와 R6의 동기와 비동기 표기.

[가설] 잔여: 관계 유형 명칭(특히 R4의 Customer-Supplier가 Partnership으로 재판정될 가능성), R5의 ACL 명칭, 반환 원천 타입의 소속.

Step 9로: 공유 패키지 구조, 반환 원천 위치.

이번 판이 뒤 단계를 바꾸는 것 한 줄: R6 위의 흐름이 이벤트 둘에서 커맨드 셋과 조회 둘까지로 늘었으므로 Step 9의 패키지 구조에서 결제 컨텍스트가 예약에 노출하는 포트가 다섯 개가 된다.

## 6. 반영하지 않은 것

| 항목 | 이유 |
|---|---|
| InventoryHoldRejected 이벤트 추가 | 09-1 보드 v2가 project-sync에 반입되지 않았다. 이벤트 수 22와 구독자 없는 20개를 유지한다 |

이 판은 08-3 결정 11건의 반영 후보다. 승인 전에는 확정본으로 인용하지 않는다.
