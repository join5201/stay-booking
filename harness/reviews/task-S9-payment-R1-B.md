# task-S9-payment R1 평가자 B 재평가

평가일: 2026-09-14

평가 역할: B

평가 방식: 허용 입력만 사용한 정적 대조

## 판정 요약

치명 0 / 보통 2 / 확인필요 1

계약과 명세에서 결제 묶음에 배정한 주요 동작은 코드로 내려가 있다. 미채택 정책 P01, P02, P04, P05, P06, P09, P10, P11을 이 묶음이 임의의 확정값으로 구현한 흔적도 찾지 못했다. 다만 계약이 명시한 Mock PG 자체의 동일 attemptId 요청과 환불 멱등성, U5는 직접 대응 테스트가 없고, KRW 불변식은 허용 입력에서 제외된 `Money` 구현에 의존해 이 증거 경계 안에서는 확정할 수 없다.

## 상세

| 심각도 | 위치(파일:행) | 위반 축 | 문제 | 근거(코드 또는 실행 결과) | 수정 제안 |
|---|---|---|---|---|---|
| 보통 | `backend/src/main/java/com/o2o/payment/infrastructure/InProcessMockPaymentGateway.java:35`, `backend/src/test/java/com/o2o/payment/application/PaymentApplicationServiceTest.java:186` | 요구사항 역추적 | 계약은 Mock PG 요청과 환불 모두 attemptId를 멱등키로 사용하고 같은 키의 재요청은 같은 결과라고 확정했지만, 어댑터 자체에 같은 attemptId를 두 번 전달하는 직접 테스트가 없다. 서비스 테스트는 둘째 환불에서 어댑터 호출 자체를 생략하므로 어댑터의 멱등 동작을 검증하지 않는다. | `request`는 `computeIfAbsent`로 같은 거래 번호를 반환하고 `refund`는 Set에 attemptId를 넣는다. Y7 앱 서비스 테스트는 첫 환불 뒤 `hasRefunded`가 참이고 둘째 서비스 호출의 반환 뷰가 같은지만 확인한다. 요청 어댑터의 동일 키 재호출과 환불 어댑터의 동일 키 재호출 결과를 직접 단정하는 테스트는 대상 7개 파일에서 찾지 못했다. | `InProcessMockPaymentGateway`에 같은 attemptId로 `request`를 두 번 호출했을 때 거래 번호가 같고, `refund`를 두 번 호출해도 업무 효과가 하나임을 확인하는 테스트를 추가한다. |
| 보통 | `backend/src/main/java/com/o2o/payment/domain/PaymentAttempt.java:41`, `backend/src/test/java/com/o2o/payment/application/PaymentApplicationServiceTest.java:102` | 요구사항 역추적 | 계약 2-1절의 U5인 같은 attemptId의 시도 하나를 직접 확인하는 테스트가 없다. U3과 U4는 실제 MySQL 유일성 테스트가 있지만 U5는 `@Id` 선언만 있고 대응 테스트를 짚을 수 없다. | `PaymentAttempt.id`는 JPA 기본키다. `PaymentApplicationServiceTest`의 Y9 두 건은 `booking_id`와 `pg_transaction_id` 유일성만 검증한다. 대상 테스트 7개에서 중복 attemptId 저장 거부를 확인하는 테스트는 찾지 못했다. | 실제 MySQL에서 같은 attemptId를 가진 둘째 시도 저장이 기본키 제약으로 거부되는 테스트를 추가하고 U5에 연결한다. |
| 확인필요 | `backend/src/main/java/com/o2o/payment/application/PaymentApplicationService.java:92`, `backend/src/main/java/com/o2o/payment/api/MockPaymentEventRequest.java:28` | 추적성 양방향, 요구사항 역추적 | 계약 2-1절의 KRW 불변식은 `shared Money`가 지킨다고 적지만 `Money` 구현은 이번 허용 입력에 없다. 대상 코드의 `openAttempt`는 양수만 직접 검사하고, 테스트는 `Money.krw` 성공값만 사용한다. 따라서 비 KRW `Money`가 생성되거나 전달될 수 있는지와 결제 입구에서 거부되는지를 허용 입력만으로 판정할 수 없다. | INTERNAL-01의 currency는 형식 단계에서 KRW로 제한하지 않고 대상 시도 통화와 다르면 409로 처리하도록 계약이 정했으므로 그 부분은 위반이 아니다. 확인이 필요한 부분은 앱 서비스 공개 메서드 `openAttempt`가 의존하는 `Money`의 생성 불변식이다. | 다음 평가 입력에 `Money` 구현과 통화 생성 테스트를 포함하거나, 결제 대상 테스트에 비 KRW 생성 또는 전달이 불가능하다는 직접 증거를 추가한다. |

## 요구사항 역추적표

| 요구사항 | 대응 테스트 또는 코드 | 커버 여부 |
|---|---|---|
| R1 연박 중 한 날짜라도 재고 부족 시 전체 선점 실패 | 재고와 예약 묶음 책임 | 해당 없음 |
| R2 동시 요청에서도 초과 예약 0 | 재고와 예약 묶음 책임 | 해당 없음 |
| R3 동일 요청 재시도 시 중복 Booking 방지 | 예약 묶음 책임 | 해당 없음 |
| R4 HELD 종료 시 재고 반환 보장 | 예약과 재고 묶음 책임. 이 묶음은 `PaymentFailed.attemptCount`를 Y8에서 확인 | 해당 없음 |
| R5 확정 Booking 금액 불변의 결제 몫 | `Payment.open`의 최초 청구액 저장, `Payment.openAttempt`의 금액 대조, `PaymentTest` Y4 | 커버 |
| 예약당 승인된 결제 하나 I7 | `Payment.openAttempt`, `Payment.recordApproval`, `PaymentTest` Y3과 Y5 | 커버 |
| 중복 콜백이 환불이나 이중 확정을 만들지 않음 U4 | `PaymentTest` Y6, `PaymentEventTest` Y8, `MockPaymentEventApiTest` Y16, Y20, Y22 | 커버 |
| 대상 표 INTERNAL-01 | `MockPaymentEventApiTest` Y15부터 Y22, `DevProfileBoundaryTest`와 Y23 | 커버 |
| 대상 표 openAttempt | `PaymentTest` Y1부터 Y4, `PaymentApplicationServiceTest` Y9와 Y10, `PaymentAutoResultTest` Y12 | 커버 |
| 대상 표 refund | `PaymentTest` Y7, `PaymentApplicationServiceTest` Y7, `MockPaymentEventApiTest` Y20 | 커버 |
| 대상 표 attemptsOf | `PaymentApplicationServiceTest` Y14 | 커버 |
| 대상 표 Mock PG 자동 결과와 재개 | `PaymentAutoResultTest` Y11부터 Y13 | 부분 커버. 어댑터 자체 멱등성은 S9-R1-B-01 |
| 대상 표 도메인 이벤트 넷 | `PaymentEventTest` Y8, `PaymentAutoResultTest` Y11과 Y12, `MockPaymentEventApiTest` Y15와 Y16 | 커버 |
| 대상 표 T30 | `MockPaymentEventApiTest` Y23, `DevProfileBoundaryTest` Y23 | 커버 |
| I6 NORMAL 시도 수 3 이하 | `PaymentTest` Y2 | 커버 |
| I7 승인 이력 하나 | `PaymentTest` Y3과 Y5 | 커버 |
| I9 REQUESTED 시도 하나 | `PaymentTest` Y1, `PaymentApplicationServiceTest` Y10 | 커버 |
| U3 bookingId별 Payment 하나 | `PaymentApplicationServiceTest` Y9 | 커버 |
| U4 pgTransactionId 콜백 무해와 DB 유일 | `PaymentTest` Y6, `PaymentApplicationServiceTest` Y9, `MockPaymentEventApiTest` Y16과 Y22 | 커버 |
| U5 attemptId별 시도 하나 | `PaymentAttempt.id`의 `@Id` | 미커버. S9-R1-B-02 |
| 전이 폐쇄 | `PaymentTest` Y5, `MockPaymentEventApiTest` Y17 | 커버 |
| 종착 무해 | `PaymentTest` Y6과 Y7, `PaymentApplicationServiceTest` Y7, `MockPaymentEventApiTest` Y20 | 커버 |
| 금액 일치 | `PaymentTest` Y4, `MockPaymentEventApiTest` Y17 | 커버 |
| eventId 유일 | `MockPaymentEvent.id`, `MockPaymentEventApiTest` Y16과 Y22 | 커버 |
| KRW | `Money` 의존, `MockPaymentEventApiTest` Y17의 통화 불일치 | 확인필요. S9-R1-B-03 |

## 실행 기록

| 명령 | 목적 | 종료 상태 | 결과 요약 | 직접 실행 여부 |
|---|---|---|---|---|
| `node harness/out/task-S9-payment-R1/verify-eval-workspace.mjs` | 평가 작업공간 검증 | 환경 제한 | 샌드박스에서 `spawnSync git EPERM`으로 검사 0건 | 직접 실행 |
| `node harness/out/task-S9-payment-R1/verify-eval-workspace.mjs` | 같은 읽기 전용 검증 재실행 | PASS | 권한이 허용된 환경에서 10건 통과. 블라인드 금지 파일 존재 알림 1건은 해당 파일을 읽지 않는 것으로 처리 | 직접 실행 |
| 테스트와 게이트 명령 | B 정적 대조 | 미실행 | 요청문 4절에 따라 B는 명령을 실행하지 않고 문서, 코드, 테스트를 정적으로 대조함 | 미실행 |

검증 ID 결과표는 평가자 A의 담당이므로 넣지 않았다.

## 보조 표

| 원본 번호 | 지적 ID | 심각도 | 위반 기준 |
|---|---|---|---|
| 1 | S9-R1-B-01 | 보통 | 요구사항 역추적 |
| 2 | S9-R1-B-02 | 보통 | 요구사항 역추적 |
| 3 | S9-R1-B-03 | 확인필요 | 추적성 양방향, 요구사항 역추적 |

## 읽은 평가 입력 SHA256

SHA256은 요청문과 대상 목록의 표기 방식에 맞춰 앞 16자리를 기록했다.

| 파일 | SHA256 앞 16자리 |
|---|---|
| `AGENTS.md` | `sha256:6cf0a192aab3bba8` |
| `backend/AGENTS.md` | `sha256:132a0c23c5f10904` |
| `harness/out/task-S9-payment-R1/eval-request-B.md` | `sha256:f513785b4c81de9c` |
| `harness/out/task-S9-payment-R1/eval-target-files.md` | `sha256:4afdd900b3dc8f02` |
| `harness/project-sync/o2o-review-input-pack.md` | `sha256:1608942c0815f911` |
| `harness/prompts/eval-criteria-code.md` | `sha256:c5ed835951fe09a5` |
| `harness/tasks/task-S9-payment.md` | `sha256:71a624284e313402` |
| `document/11-o2o-api-spec.md` | `sha256:3f2613a77b649903` |
| `document/06-2-o2o-aggregates.md` | `sha256:113c6734b5525e1d` |
| `document/06-4-o2o-contracts.md` | `sha256:edacbe47d6d63e3f` |
| `document/06-1-o2o-context-map.md` | `sha256:95bc2b1079d3b739` |
| `harness/decisions/decisions-08-3.md` | `sha256:1b8580aa86c18fd8` |
| `backend/.claude/rules/layers.md` | `sha256:cb9e4f68bdc4c43c` |
| `backend/src/main/resources/application.properties` | `sha256:a53c90a2ad96f493` |
| `backend/src/test/resources/application.properties` | `sha256:7a925f9016cc6666` |
| `backend/src/main/java/com/o2o/shared/ActorResolver.java` | `sha256:c40ff8e6d5a4547b` |
| `backend/src/main/java/com/o2o/payment/api/FailureCodeMatchesOutcome.java` | `sha256:db5bc260eca12935` |
| `backend/src/main/java/com/o2o/payment/api/FailureCodeMatchesOutcomeValidator.java` | `sha256:ab8c1076bb5a9f5e` |
| `backend/src/main/java/com/o2o/payment/api/MockEventResultResponse.java` | `sha256:398a964bbaf06832` |
| `backend/src/main/java/com/o2o/payment/api/MockPaymentEventController.java` | `sha256:48fa0e65241f5635` |
| `backend/src/main/java/com/o2o/payment/api/MockPaymentEventRequest.java` | `sha256:f2b93f656a94a83d` |
| `backend/src/main/java/com/o2o/payment/api/package-info.java` | `sha256:e93ca6ac6193bd83` |
| `backend/src/main/java/com/o2o/payment/api/PaymentExceptionHandler.java` | `sha256:3ab8ce787019c574` |
| `backend/src/main/java/com/o2o/payment/application/MockEventCommand.java` | `sha256:edb6f454c04420f3` |
| `backend/src/main/java/com/o2o/payment/application/MockPaymentGateway.java` | `sha256:cf381adf3f781d08` |
| `backend/src/main/java/com/o2o/payment/application/package-info.java` | `sha256:e8133cc9192da34c` |
| `backend/src/main/java/com/o2o/payment/application/PaymentApplicationService.java` | `sha256:ab1b91e47881635f` |
| `backend/src/main/java/com/o2o/payment/application/PaymentAttemptView.java` | `sha256:9ffeeb0ca47cd9d5` |
| `backend/src/main/java/com/o2o/payment/application/PaymentSummaryView.java` | `sha256:1449c9f28657ef6c` |
| `backend/src/main/java/com/o2o/payment/application/RefundView.java` | `sha256:9639df24c3aad354` |
| `backend/src/main/java/com/o2o/payment/domain/AlreadyApprovedException.java` | `sha256:04f3451dedd909f1` |
| `backend/src/main/java/com/o2o/payment/domain/AmountMismatchException.java` | `sha256:ddf417aa45e058bf` |
| `backend/src/main/java/com/o2o/payment/domain/AttemptInProgressException.java` | `sha256:854c2da0b4badfe3` |
| `backend/src/main/java/com/o2o/payment/domain/AttemptKind.java` | `sha256:ef33f689b6fbe168` |
| `backend/src/main/java/com/o2o/payment/domain/AttemptLimitExceededException.java` | `sha256:a0ee26cecfd648ff` |
| `backend/src/main/java/com/o2o/payment/domain/InvalidAttemptTransitionException.java` | `sha256:8ccd869b44c1262f` |
| `backend/src/main/java/com/o2o/payment/domain/MockEventConflictException.java` | `sha256:d69963abbed788ab` |
| `backend/src/main/java/com/o2o/payment/domain/MockEventResult.java` | `sha256:d85dbe27d72a12cf` |
| `backend/src/main/java/com/o2o/payment/domain/MockMode.java` | `sha256:06c086880b91be3f` |
| `backend/src/main/java/com/o2o/payment/domain/MockOutcome.java` | `sha256:95d8041d8651714b` |
| `backend/src/main/java/com/o2o/payment/domain/MockPaymentEvent.java` | `sha256:13c00e1554930628` |
| `backend/src/main/java/com/o2o/payment/domain/MockPaymentEventRepository.java` | `sha256:f9263d1fbc373a7c` |
| `backend/src/main/java/com/o2o/payment/domain/NoApprovedAttemptException.java` | `sha256:684a81c5b9a7016c` |
| `backend/src/main/java/com/o2o/payment/domain/package-info.java` | `sha256:13096494dd3e11ca` |
| `backend/src/main/java/com/o2o/payment/domain/Payment.java` | `sha256:47907dd46ea3e610` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentApproved.java` | `sha256:485c7276e31f8b92` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentAttempt.java` | `sha256:ffddff505a1f857a` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentAttemptId.java` | `sha256:3b720c82c02c7982` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentAttemptStatus.java` | `sha256:a7d74b89162e1ef8` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentFailed.java` | `sha256:1c0f9928640dd2b9` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentId.java` | `sha256:0d1e501d040b4e8b` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentRefunded.java` | `sha256:f8b3fd0cc69ee3db` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentRepository.java` | `sha256:7d6517182ab5e443` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentRequested.java` | `sha256:2da1bceabf95524b` |
| `backend/src/main/java/com/o2o/payment/domain/PgTransactionMismatchException.java` | `sha256:7908c65ad94194b5` |
| `backend/src/main/java/com/o2o/payment/domain/RefundReason.java` | `sha256:cca4a5006f383449` |
| `backend/src/main/java/com/o2o/payment/domain/UnknownAttemptException.java` | `sha256:5fe7cae0d5cb9205` |
| `backend/src/main/java/com/o2o/payment/infrastructure/InProcessMockPaymentGateway.java` | `sha256:9cf8feb1d46b2796` |
| `backend/src/main/java/com/o2o/payment/infrastructure/JpaMockPaymentEventRepository.java` | `sha256:efb2cde46f85a7ac` |
| `backend/src/main/java/com/o2o/payment/infrastructure/JpaPaymentRepository.java` | `sha256:75c26de276f29ad3` |
| `backend/src/main/java/com/o2o/payment/infrastructure/MockAutoResultAdapter.java` | `sha256:07e7585a7dc2b0af` |
| `backend/src/main/java/com/o2o/payment/infrastructure/MockAutoResultResumeRunner.java` | `sha256:262fda55fa54258d` |
| `backend/src/main/java/com/o2o/payment/infrastructure/MockPaymentEventJpaRepository.java` | `sha256:0b8eb81aa8e66687` |
| `backend/src/main/java/com/o2o/payment/infrastructure/package-info.java` | `sha256:0bc5ae1a1226afb7` |
| `backend/src/main/java/com/o2o/payment/infrastructure/PaymentJpaRepository.java` | `sha256:a7f7baecb9015628` |
| `backend/src/test/java/com/o2o/payment/api/DevProfileBoundaryTest.java` | `sha256:e0b93bae5f524e4e` |
| `backend/src/test/java/com/o2o/payment/api/MockPaymentEventApiTest.java` | `sha256:1cb5ae1f18772c96` |
| `backend/src/test/java/com/o2o/payment/application/PaymentApplicationServiceTest.java` | `sha256:1191ff3790bdfc8f` |
| `backend/src/test/java/com/o2o/payment/application/PaymentAutoResultTest.java` | `sha256:9efe7c5a7ecf9ad3` |
| `backend/src/test/java/com/o2o/payment/application/PaymentEventTest.java` | `sha256:22da6313a4125edb` |
| `backend/src/test/java/com/o2o/payment/CommittedPaymentEvents.java` | `sha256:49824d40d2284240` |
| `backend/src/test/java/com/o2o/payment/domain/PaymentTest.java` | `sha256:c363455340cd0f2b` |

## 블라인드 경계 확인

기존 `harness/reviews/task-S9-payment-R1-B.md`, 같은 쌍의 다른 리포트, 과거 평가 리포트와 결정표, 이전 평가 작업, `step9-verification.md`는 읽지 않았다. 실행 결과 사본도 이번 B 정적 판정의 근거로 사용하지 않았다. 평가자 A의 검증 ID 판정을 대신하지 않았다.
