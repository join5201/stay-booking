# task-S9-booking과 task-S9-booking-lifecycle R1 평가자 B 리포트

평가일: 2026-09-15  
평가자: B  
평가 축: 추적성 양방향, 결정 근거의 자립성, 요구사항 역추적  
기준 HEAD: `e1a9392c8d7857ba488a38b35fd23ee76401a689`  
작업공간 검증: `verify-eval-workspace.mjs` 10건 PASS

## 판정 요약

치명 0 / 보통 1 / 확인필요 1

## 상세

| 심각도 | 위치(파일:행) | 위반 축 | 문제 | 근거(코드 또는 실행 결과) | 수정 제안 |
|---|---|---|---|---|---|
| 보통 | `backend/src/main/java/com/o2o/inventory/domain/DailyInventory.java:142` | 추적성 양방향, 요구사항 역추적 | Hold, 확정 이동, 선점 반환, 판매 반환이 재고 `version`을 증가시키지 않는다. API 명세 공통 규칙은 Hold, 확정, 반환도 재고 version을 증가시킨다고 요구한다. | `adjust`만 115행에서 version을 증가시킨다. `hold`, `commit`, `releaseHeld`, `releaseSold`는 수량과 `updatedAt`만 바꾼다. `BookingApiTest` 553행은 예약 선점 뒤 version이 0이라고 단언해 현재 불일치를 고정한다. R1부터 R5의 수량과 금액 보장은 깨지지 않으므로 보통으로 판정한다. | 네 수량 변경 메서드가 성공할 때 version을 1씩 증가시키고, hold, commit, releaseHeld, releaseSold 각각의 성공과 롤백에서 version도 함께 검증한다. |
| 확인필요 | `backend/src/main/java/com/o2o/booking/application/PaymentOutcomeService.java:65` | 결정 근거의 자립성, 추적성 양방향 | 결제 승인이 이미 커밋된 뒤 P1과 T1이 경합하면 어느 쪽이 Booking 잠금을 먼저 얻는지에 따라 CONFIRMED 또는 EXPIRED와 환불로 갈릴 수 있다. 08-3 결정 8과 11-1의 "만료 커밋 전에 승인이 기록됐으면 확정 우선"과, 2차 계약 P1의 처리 시각이 expiresAt 이상이면 지연 승인으로 만료한다는 규칙을 동시에 만족시키는 기준 시점이 문서와 코드만으로 하나로 정해지지 않는다. | `PaymentApprovedAdapter`는 AFTER_COMMIT 뒤 `onApproved`를 부른다. `onApproved` 65행부터는 현재 시각이 due면 환불 후 만료한다. 반면 `BookingExpirationService` 71행부터는 승인 시도가 있으면 확정한다. `BookingLockContentionTest` 141행부터는 같은 선승인 상태에서 두 결과를 모두 허용한다. | 승인 기록 시각, P1 처리 시각, Booking 만료 커밋 중 어느 시각을 우선 기준으로 삼는지 계약에 한 문장으로 확정한다. 승인 기록 커밋이 기준이면 T1과 P1 모두 같은 결론을 내도록 승인 기록을 잠금 아래 재확인하고 테스트도 결과 하나만 허용한다. |

## 요구사항 역추적표

| 요구사항 | 대응 테스트 또는 코드 | 커버 여부 |
|---|---|---|
| R1. 연박 중 한 날짜라도 재고 부족 시 전체 선점 실패 | `BookingApplicationServiceTest.K7`, `InventoryAllocationServiceTest`의 N행 실패 롤백 테스트, `BookingApplicationService.requestBooking`과 `InventoryAllocationService.hold` | 커버 |
| R2. 동시 요청에서도 초과 예약 0 | `BookingApiTest.K19`, `DailyInventoryJpaRepository.findRangeForUpdate`, 날짜 오름차순 비관적 잠금 | 커버 |
| R3. 동일 요청 재시도 시 중복 Booking 방지 | `BookingApiTest.K15`, `K16`, `K17`, `IdempotencyRecordTest.K6`, 멱등 범위 유니크와 Booking의 `(userId, idempotencyKey)` 유니크 | 커버 |
| R4. HELD 종료 시 재고 반환 보장 | `PaymentOutcomeServiceTest.L7`, `L8`, `ExpireDueBookingsTest.L9`, `BookingLockContentionTest.L23`, `BookingLifecycle.expireByTtl`, `expireByPaymentFailure` | 커버. 승인 우선 경계의 결론은 `S9-R1-B-02` 확인필요 |
| R5. 확정 Booking 금액 불변 | `BookingApplicationServiceTest.K11`, `PricingPriceQuoteAdapterTest.L5`, `BookingPaymentServiceTest.L6`, Booking의 변경 경로 없는 `PriceSnapshot` | 예약 스냅샷과 청구액 전달은 커버. 이후 결제 시도의 금액 일치 몫은 결제 쌍이라 해당 없음 |

## 계약 역추적표

### 1차 계약

| 계약 항목 | 대응 테스트 또는 코드 | 판정 |
|---|---|---|
| BOOK-01 | `BookingApiTest.K13`부터 `K23`, `BookingController.request`, `BookingApplicationService.requestBooking` | 커버 |
| BOOK-02 | `BookingQueryApiTest.K24`, `BookingController.list`, `JpaBookingRepository.findByUserId` | 커버 |
| BOOK-03 | `BookingQueryApiTest.K25`, `BookingController.get`, `BookingApplicationService.getBooking` | 커버 |
| HoldInventory | `DailyInventoryAllocationTest.K4`, `BookingApplicationServiceTest.K7`, `BookingApiTest.K19`, `InventoryAllocationService.lock`과 `hold` | 커버. 재고 version은 `S9-R1-B-01` |
| I3 | `StayPeriodTest.K1`, `StayPeriod` | 커버 |
| I4 | `BookingTest.K3`, `PriceSnapshotTest.K2`, `BookingApplicationServiceTest.K11` | 커버 |
| I10, I11, I12, I15 | `PriceSnapshotTest.K2`, `PriceSnapshot.of` | 커버 |
| I1, I1a | `DailyInventoryAllocationTest.K4`, `K5`, `InventoryAllocationServiceTest.L4` | 커버 |
| U1 | `IdempotencyRecordTest.K6`, `BookingApiTest.K15`부터 `K19`, 두 DB 유니크 | 커버 |
| A1 | `BookingApplicationServiceTest.K7`, `InventoryAllocationServiceTest.L4` | 커버 |
| A5 | `BookingApplicationServiceTest.K9`, `BookingApiTest.K23` | 커버 |
| A6 | `BookingApplicationServiceTest.K8`, `BookingApiTest.K23`, `PricingPriceQuoteAdapterTest.L5` | 커버 |

### 2차 계약

| 계약 항목 | 대응 테스트 또는 코드 | 판정 |
|---|---|---|
| PAY-01 | `BookingPaymentApiTest.L14`부터 `L17`, `BookingPaymentServiceTest.L6`, `BookingController.requestPayment` | 예약 중계 몫 커버 |
| PAY-02 | `BookingPaymentApiTest.L18`, `BookingController.paymentAttempts` | 예약 중계 몫 커버 |
| BOOK-04 | `BookingCancelApiTest.L19`부터 `L22`, `CancelBookingTest.L10`, `L11` | 커버 |
| P1 결제 승인 처리 | `PaymentOutcomeServiceTest.L7`, `PaymentOutcomeService.onApproved` | 경계 결론은 `S9-R1-B-02` 확인필요 |
| P3 결제 실패 시 만료 | `PaymentOutcomeServiceTest.L8`, `PaymentOutcomeService.onFailed` | 커버 |
| T1 TTL 만료 | `ExpireDueBookingsTest.L9`, `ApprovalLossRecoveryTest.L13`, `BookingExpirationService.expireIfDue` | 커버. 승인 경합 결론은 `S9-R1-B-02` 확인필요 |
| 가격 포트 어댑터 | `PricingPriceQuoteAdapterTest.L5`, `PricingPriceQuoteAdapter.quote` | 커버 |
| Booking 전이 셋과 이벤트 셋 | `BookingTransitionTest.L1`부터 `L3`, `BookingLifecycleEventTest.L12` | 커버 |
| 재고 N행 적용 | `InventoryAllocationServiceTest.L4`, `BookingLifecycle` | 커버. 재고 version은 `S9-R1-B-01` |
| Booking 응답의 payment | `BookingPaymentApiTest.L16`부터 `L18`, `BookingCancelApiTest.L19`, `L21`, `BookingResponse` | 커버 |
| 2-1절 불변식 표 | 위 I4, I5, R4, 종착 무해, 잠금, 시간, 재고, 취소 날짜, 스냅샷 테스트 | 커버. 승인 우선 시점만 확인필요 |

## 실행 기록

| 명령 | 목적 | 종료 상태 | 결과 요약 | 직접 실행 여부 |
|---|---|---|---|---|
| `git branch --show-current`, `git branch --all --contains HEAD`, `git rev-parse HEAD` | 격리 worktree의 브랜치와 HEAD 확인 | 성공 | 현재 checkout은 `(no branch)`이고 HEAD는 `e1a9392c8d7857ba488a38b35fd23ee76401a689`이다. HEAD를 포함하는 로컬 `eval/mvp-2026-09-14`를 확인했다. | 직접 실행 |
| `node harness/out/task-S9-booking-R1/verify-eval-workspace.mjs` | 평가 작업공간 격리와 대상 해시 확인 | 성공 | 10건 PASS. `ws.git`, `ws.commit`, `ws.agents`, `ws.list`, `ws.target-count`, `ws.target-exists`, `ws.target-hash`, `ws.target-drift`, `ws.clean`, `ws.doc-hash`. 금지 파일 존재 알림은 받았고 해당 파일은 읽지 않았다. | 직접 실행 |
| 테스트와 빌드 | 평가자 B의 정적 대조 | 미실행 | 요청문 4절에 따라 실행하지 않았다. A의 검증 ID 결과를 대신 판정하지 않았다. | 미실행 |

## 보조 표

| 원본 번호 | 지적 ID | 심각도 | 위반 기준 |
|---|---|---|---|
| 1 | S9-R1-B-01 | 보통 | 추적성 양방향, 요구사항 역추적 |
| 2 | S9-R1-B-02 | 확인필요 | 결정 근거의 자립성, 추적성 양방향 |

## 읽은 파일과 SHA256

아래 값은 실제 파일에서 다시 계산한 SHA256의 앞 16자리다. 합본 대상 전체는 작업공간 검증기의 `ws.target-hash`와 `ws.target-drift` PASS로 목록의 파일별 해시와 일치함을 확인했다.

### 평가 입력과 기준 문서

| 파일 | SHA256 앞 16자리 |
|---|---|
| `harness/out/task-S9-booking-R1/eval-request-B.md` | `3bf34bf355b59a0a` |
| `harness/out/mvp-eval-2026-09-14/eval-target-files-booking.md` | `79859e3f5bb5bddd` |
| `harness/project-sync/o2o-review-input-pack.md` | `1608942c0815f911` |
| `harness/prompts/eval-criteria-code.md` | `c5ed835951fe09a5` |
| `harness/tasks/task-S9-booking.md` | `0876574192269065` |
| `harness/tasks/task-S9-booking-lifecycle.md` | `af5cba4e4077a2f0` |
| `document/11-o2o-api-spec.md` | `3f2613a77b649903` |
| `document/06-2-o2o-aggregates.md` | `113c6734b5525e1d` |
| `document/06-4-o2o-contracts.md` | `edacbe47d6d63e3f` |
| `document/06-1-o2o-context-map.md` | `95bc2b1079d3b739` |
| `harness/decisions/decisions-08-3.md` | `1b8580aa86c18fd8` |
| `backend/src/main/resources/application.properties` | `a53c90a2ad96f493` |
| `backend/.claude/rules/layers.md` | `cb9e4f68bdc4c43` |

### 근거로 읽은 프로덕션 코드

| 파일 | SHA256 앞 16자리 |
|---|---|
| `backend/src/main/java/com/o2o/booking/api/BookingController.java` | `33de44b118ea1e93` |
| `backend/src/main/java/com/o2o/booking/api/BookingResponse.java` | `88c05e5c9b59a622` |
| `backend/src/main/java/com/o2o/booking/api/RequestBookingRequest.java` | `6f32bc6549d52e39` |
| `backend/src/main/java/com/o2o/booking/application/BookingApplicationService.java` | `d0e71e27a80d056a` |
| `backend/src/main/java/com/o2o/booking/application/BookingLifecycle.java` | `f3356d010ca578b3` |
| `backend/src/main/java/com/o2o/booking/application/BookingPaymentService.java` | `b54e888b24a7ffad` |
| `backend/src/main/java/com/o2o/booking/application/BookingExpirationService.java` | `410f54e2d20f69a1` |
| `backend/src/main/java/com/o2o/booking/application/ExpireDueBookings.java` | `4791bb0518eb28bc` |
| `backend/src/main/java/com/o2o/booking/application/PaymentOutcomeService.java` | `16ab8a1390db47b1` |
| `backend/src/main/java/com/o2o/booking/application/IdempotentRequestExecutor.java` | `1a2c6324b2461d2f` |
| `backend/src/main/java/com/o2o/booking/domain/Booking.java` | `9274fc81a0fd1e9e` |
| `backend/src/main/java/com/o2o/booking/domain/PriceSnapshot.java` | `1938dbe211a03c8f` |
| `backend/src/main/java/com/o2o/booking/domain/StayPeriod.java` | `701d1c2de5b87716` |
| `backend/src/main/java/com/o2o/booking/infrastructure/BookingJpaRepository.java` | `00e22fb02b138889` |
| `backend/src/main/java/com/o2o/booking/infrastructure/JpaBookingRepository.java` | `f2ecebc4af2ca5ad` |
| `backend/src/main/java/com/o2o/booking/infrastructure/PricingPriceQuoteAdapter.java` | `0072b1cde0b1f7c3` |
| `backend/src/main/java/com/o2o/booking/infrastructure/PaymentApprovedAdapter.java` | `243b743d785a26e0` |
| `backend/src/main/java/com/o2o/inventory/domain/DailyInventory.java` | `cd47187239009880` |
| `backend/src/main/java/com/o2o/inventory/domain/InventoryAllocationService.java` | `3cfccfd6ce098a56` |
| `backend/src/main/java/com/o2o/inventory/infrastructure/DailyInventoryJpaRepository.java` | `c455d74d17a29d46` |

### 근거로 읽은 테스트

| 파일 | SHA256 앞 16자리 |
|---|---|
| `backend/src/test/java/com/o2o/booking/api/BookingApiTest.java` | `bdb1cf2a930d1328` |
| `backend/src/test/java/com/o2o/booking/api/BookingPaymentApiTest.java` | `699cb424b52918c9` |
| `backend/src/test/java/com/o2o/booking/api/BookingCancelApiTest.java` | `5fd4c92e2792d3dc` |
| `backend/src/test/java/com/o2o/booking/api/BookingQueryApiTest.java` | `09921de0811c7f5d` |
| `backend/src/test/java/com/o2o/booking/application/BookingApplicationServiceTest.java` | `e0df9e17f5f23dea` |
| `backend/src/test/java/com/o2o/booking/application/BookingEventTest.java` | `38c74fdb92ca6728` |
| `backend/src/test/java/com/o2o/booking/application/BookingLifecycleEventTest.java` | `6addce89c523df61` |
| `backend/src/test/java/com/o2o/booking/application/BookingLockContentionTest.java` | `01d80ef2776e7aaa` |
| `backend/src/test/java/com/o2o/booking/application/BookingPaymentServiceTest.java` | `467bdabbe02defd3` |
| `backend/src/test/java/com/o2o/booking/application/CancelBookingTest.java` | `615ee5d297943efe` |
| `backend/src/test/java/com/o2o/booking/application/ExpireDueBookingsTest.java` | `2558163ad91e6aee` |
| `backend/src/test/java/com/o2o/booking/application/PaymentOutcomeServiceTest.java` | `05c6d775879dc231` |
| `backend/src/test/java/com/o2o/booking/application/ApprovalLossRecoveryTest.java` | `0b2b3795748e5f23` |
| `backend/src/test/java/com/o2o/booking/domain/BookingTest.java` | `082cc50067dc8218` |
| `backend/src/test/java/com/o2o/booking/domain/BookingTransitionTest.java` | `2b18688ca7113d05` |
| `backend/src/test/java/com/o2o/booking/domain/IdempotencyRecordTest.java` | `1013fd0ccc7a592` |
| `backend/src/test/java/com/o2o/booking/domain/PriceSnapshotTest.java` | `02e9fb88faf50178` |
| `backend/src/test/java/com/o2o/booking/domain/StayPeriodTest.java` | `8265012793ae76d4` |
| `backend/src/test/java/com/o2o/booking/infrastructure/PricingPriceQuoteAdapterTest.java` | `7cbdb4205c61f457` |
| `backend/src/test/java/com/o2o/inventory/domain/DailyInventoryAllocationTest.java` | `6223da00ca7f9bfb` |
| `backend/src/test/java/com/o2o/inventory/domain/InventoryAllocationServiceTest.java` | `0f4232c145a47ca5` |

## 자체 점검

| 항목 | 결과 |
|---|---|
| 판정 요약과 상세 등급별 수 | 치명 0, 보통 1, 확인필요 1로 일치 |
| 상세 행과 보조 표 행 수 | 각각 2행으로 일치 |
| 지적 ID | `S9-R1-B-01`, `S9-R1-B-02`로 연속이며 형식 일치 |
| 담당 표 | 평가자 B의 세 축과 요구사항 역추적만 포함. 검증 ID 결과표 없음 |
| 요구사항 수 | R1부터 R5까지 5개. R5 결제 쌍 몫은 해당 없음으로 분리 |
| 금지 입력 | 기존 리포트, 다른 평가자 리포트, `step9-verification.md`, 허용되지 않은 결정표를 읽지 않음 |
| 파일 변경 | 이 리포트 한 파일만 생성 |
