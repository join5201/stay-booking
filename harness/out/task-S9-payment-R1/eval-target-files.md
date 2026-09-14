# task-S9-payment 평가 대상 코드 목록

최초 작성: 2026-09-13
최종 갱신: 2026-09-14 (5절 main 기준 목록 신설. 라운드 mvp-eval-2026-09-14 R1. 그 전 2026-09-13 개정 1 보강 커밋 둘을 1절에 더하고 2-6절 설정 파일 설명을 갱신. 기준 커밋 9134a52. 그 전 같은 날 초안)

계약 5절 평가 대상 코드 행이 가리키는 파일이다. 계약 표 한 칸에 예순 줄을 넣을 수 없어 목록을 여기 둔다. 오늘의 전제 결정 3에 따라 평가는 MVP 코드가 다 붙은 뒤 한 번이고 이 문서는 그 라운드에 이 묶음 몫으로 넘길 목록이다.

## 1. 버전

| 항목 | 값 |
|---|---|
| 브랜치 | feat/task-s9-payment |
| 기준 커밋 | 9134a52. 마지막 backend 커밋(2026-09-13 개정 1 보강). 그 전 초안의 기준은 d56f04d였다. 뒤 커밋은 기록과 문서다 |
| 앞 묶음과 갈라진 지점 | 34568a6 (origin/main. 2026-09-12 병합 커밋 3d3dd0e로 브랜치에 들어왔다) |

이 Task의 backend 커밋은 열하나다. 아홉은 4단계부터 6단계이고 둘은 9단계 개정 1 보강(사용자 지시 그대로 반영, 2026-09-13)이다.

| 해시 | 무엇 | 단계 |
|---|---|---|
| 0fb9876 | 결제 도메인. Payment 루트와 PaymentAttempt, 이벤트 넷, 리포지토리 인터페이스 둘, 예외 | 4 |
| ea61597 | 결제 앱 서비스. openAttempt, refund, attemptsOf, handleMockEvent, deliverAutoResult | 4 |
| 22de11a | 결제 인프라. JPA 리포지토리 넷, 프로세스 안 Mock PG, 자동 결과 어댑터, 재개 러너 | 4 |
| cdd32ea | 결제 도메인 단위 테스트 Y1부터 Y7 | 5 |
| ce2dd62 | 앱 서비스 통합과 이벤트 테스트 Y7부터 Y14 | 5 |
| a0e5d82 | INTERNAL-01 api 층. 컨트롤러, 요청과 failureCode 조합 제약, 응답, 예외 핸들러 | 6 |
| e0cc55f | T30 개발 프로파일. ActorResolver의 dev 판정과 설정 파일 둘의 spring.profiles.active=dev | 6 |
| 33b1262 | 앱 서비스 트랜잭션을 READ COMMITTED로 | 6 |
| d56f04d | INTERNAL-01과 T30 테스트 Y15부터 Y23. 커밋 뒤 구독자를 공용 CommittedPaymentEvents로 | 6 |
| 6e220b5 | 풀 기본 격리 수준을 READ COMMITTED로. 설정 파일 둘에 한 줄과 주석, PaymentApplicationService 주석 한 문단 | 9 개정 1 |
| 9134a52 | PaymentJpaRepository 잠금 조회 주석을 실제 SQL(for update of 루트)에 맞춤. 코드 변경 없음 | 9 개정 1 |

## 2. 프로덕션 코드 52개

경로는 backend/src/main/java/com/o2o/ 아래다. 새 파일 49개와 고친 파일 3개다.

### 2-1. payment/domain 27개

AlreadyApprovedException.java, AmountMismatchException.java, AttemptInProgressException.java, AttemptKind.java, AttemptLimitExceededException.java, InvalidAttemptTransitionException.java, MockEventConflictException.java, MockEventResult.java, MockMode.java, MockOutcome.java, MockPaymentEvent.java, MockPaymentEventRepository.java, NoApprovedAttemptException.java, Payment.java, PaymentApproved.java, PaymentAttempt.java, PaymentAttemptId.java, PaymentAttemptStatus.java, PaymentFailed.java, PaymentId.java, PaymentRefunded.java, PaymentRepository.java, PaymentRequested.java, PgTransactionMismatchException.java, RefundReason.java, UnknownAttemptException.java, package-info.java

### 2-2. payment/application 7개

MockEventCommand.java, MockPaymentGateway.java, PaymentApplicationService.java, PaymentAttemptView.java, PaymentSummaryView.java, RefundView.java, package-info.java

### 2-3. payment/infrastructure 8개

InProcessMockPaymentGateway.java, JpaMockPaymentEventRepository.java, JpaPaymentRepository.java, MockAutoResultAdapter.java, MockAutoResultResumeRunner.java, MockPaymentEventJpaRepository.java, PaymentJpaRepository.java, package-info.java

### 2-4. payment/api 7개

FailureCodeMatchesOutcome.java, FailureCodeMatchesOutcomeValidator.java, MockEventResultResponse.java, MockPaymentEventController.java, MockPaymentEventRequest.java, PaymentExceptionHandler.java, package-info.java

### 2-5. shared 고친 파일 1개

ActorResolver.java. require에 dev 프로파일 판정 한 줄과 Environment 주입, 주석이다. 근거는 계약 7절 D-3 가. 고치기 전 해시는 계약 4절 행에 남겼다.

### 2-6. 설정 파일 고친 것 2개

경로는 backend/src/ 아래다. main/resources/application.properties와 test/resources/application.properties. 각각 끝에 spring.profiles.active=dev 한 줄과 근거 주석 다섯 줄(6단계, D-3), 그 아래 spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED 한 줄과 근거 주석(9단계 개정 1 보강. main 여섯 줄, test 두 줄)이다. 다른 키는 건드리지 않았다. 근거는 계약 9절 설정 파일 변경 행과 6절 격리 수준 행.

## 3. 테스트 코드 7개

경로는 backend/src/test/java/com/o2o/ 아래다.

| 파일 | 건수 | 성격 | 8-1절 |
|---|---|---|---|
| payment/domain/PaymentTest.java | 21 | 단위. 스프링 없이 | Y1부터 Y7의 도메인 몫 |
| payment/application/PaymentApplicationServiceTest.java | 7 | MySQL 통합. 유니크 둘과 잠금 대기, 트랜잭션을 감싸지 않는다 | Y7 앱 몫, Y9, Y10, Y14 |
| payment/application/PaymentEventTest.java | 4 | MySQL 통합. 테스트 트랜잭션 안에서 ApplicationEvents로 발행을 센다 | Y8 |
| payment/application/PaymentAutoResultTest.java | 5 | MySQL 통합. 테스트 전용 AFTER_COMMIT 구독자로 도착을 센다 | Y11, Y12, Y13 |
| payment/CommittedPaymentEvents.java | 0 | 테스트 전용 구독자. 위 파일과 아래 파일이 같이 쓴다 | Y11부터 Y13, Y15부터 Y22 |
| payment/api/MockPaymentEventApiTest.java | 17 | 실제 포트. 잠금 경합 둘. dev 프로파일 | Y15부터 Y22, Y23의 dev 쪽 |
| payment/api/DevProfileBoundaryTest.java | 1 | 실제 포트. @ActiveProfiles verify 컨텍스트 | Y23의 dev 밖 쪽 |

합계 55건이다. 저장소 전체는 337건이고 나머지 282건은 앞 묶음 것이다.

## 4. 평가 대상이 아닌 것

| 무엇 | 왜 |
|---|---|
| harness/out/task-S9-payment-R1/ 아래 결과 사본과 이 문서와 step9-verification.md | 산출물의 증거이지 평가 대상 코드가 아니다 |
| harness/state/progress.md의 이 Task 행 | 기록이다. CLAUDE.md 3절이 harness/state 전체를 평가 입력에서 뺀다 |
| booking, inventory, catalog, promotion, search 패키지 | 이 묶음이 고치지 않았다. shared의 ActorResolver 하나만 고쳤다 |
| ActorRegistry.java의 낡은 프로파일 미룸 주석 | 이 계약의 변경 허용 파일이 아니라 두었다. step9-verification.md 6절 |
| 이 계약 파일 harness/tasks/task-S9-payment.md | 5절 표의 별도 행이다. 코드 목록이 아니다 |

## 5. main 기준 목록 (2026-09-14 신설. 라운드 mvp-eval-2026-09-14의 R1 몫)

1절부터 4절은 브랜치 시점(9134a52)의 기록이다. 이번 라운드의 대상은 이 절이다. 9134a52 뒤 이 묶음 파일을 고친 것은 예약 2차가 테스트 설정에 더한 스케줄러 줄뿐이라 코드는 같고, 계약 5절 평가 대상 행의 해시는 이 절을 더한 뒤 값이다.

| 항목 | 값 |
|---|---|
| 브랜치 | main |
| 기준 커밋 | 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1 (PR 138 병합, 2026-09-13 23:53) |
| 뽑은 방식 | 패키지 단위. payment 패키지 전체, shared 중 결제가 고친 ActorResolver, 설정 파일 둘. 테스트는 payment 전체. 표는 harness/out/mvp-eval-2026-09-14/build-target-lists.mjs payment 가 기준 커밋에서 낸 것이다 |

경로는 저장소 루트 기준이다. sha256은 앞 16자리이고 기준 커밋의 작업 트리 값이다. 만든 묶음은 그 파일을 처음 더한 커밋의 PR, 고친 묶음은 그 뒤 그 파일을 고친 다른 PR이다. 테스트 표의 건수는 main 전체 실행 사본(harness/out/task-S9-booking-lifecycle-R1/step9의 JUnit XML)에서 읽었다.

### backend/src/main/java/com/o2o/payment/api 7개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/payment/api/FailureCodeMatchesOutcome.java | sha256:db5bc260eca12935 | 결제 | 없음 | a0e5d82 |
| backend/src/main/java/com/o2o/payment/api/FailureCodeMatchesOutcomeValidator.java | sha256:ab8c1076bb5a9f5e | 결제 | 없음 | a0e5d82 |
| backend/src/main/java/com/o2o/payment/api/MockEventResultResponse.java | sha256:398a964bbaf06832 | 결제 | 없음 | a0e5d82 |
| backend/src/main/java/com/o2o/payment/api/MockPaymentEventController.java | sha256:48fa0e65241f5635 | 결제 | 없음 | a0e5d82 |
| backend/src/main/java/com/o2o/payment/api/MockPaymentEventRequest.java | sha256:f2b93f656a94a83d | 결제 | 없음 | a0e5d82 |
| backend/src/main/java/com/o2o/payment/api/PaymentExceptionHandler.java | sha256:3ab8ce787019c574 | 결제 | 없음 | a0e5d82 |
| backend/src/main/java/com/o2o/payment/api/package-info.java | sha256:e93ca6ac6193bd83 | 결제 | 없음 | a0e5d82 |

### backend/src/main/java/com/o2o/payment/application 7개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/payment/application/MockEventCommand.java | sha256:edb6f454c04420f3 | 결제 | 없음 | ea61597 |
| backend/src/main/java/com/o2o/payment/application/MockPaymentGateway.java | sha256:cf381adf3f781d08 | 결제 | 없음 | ea61597 |
| backend/src/main/java/com/o2o/payment/application/PaymentApplicationService.java | sha256:ab1b91e47881635f | 결제 | 없음 | 6e220b5 |
| backend/src/main/java/com/o2o/payment/application/PaymentAttemptView.java | sha256:9ffeeb0ca47cd9d5 | 결제 | 없음 | ea61597 |
| backend/src/main/java/com/o2o/payment/application/PaymentSummaryView.java | sha256:1449c9f28657ef6c | 결제 | 없음 | ea61597 |
| backend/src/main/java/com/o2o/payment/application/RefundView.java | sha256:9639df24c3aad354 | 결제 | 없음 | ea61597 |
| backend/src/main/java/com/o2o/payment/application/package-info.java | sha256:e8133cc9192da34c | 결제 | 없음 | ea61597 |

### backend/src/main/java/com/o2o/payment/domain 27개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/payment/domain/AlreadyApprovedException.java | sha256:04f3451dedd909f1 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/AmountMismatchException.java | sha256:ddf417aa45e058bf | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/AttemptInProgressException.java | sha256:854c2da0b4badfe3 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/AttemptKind.java | sha256:ef33f689b6fbe168 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/AttemptLimitExceededException.java | sha256:a0ee26cecfd648ff | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/InvalidAttemptTransitionException.java | sha256:8ccd869b44c1262f | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/MockEventConflictException.java | sha256:d69963abbed788ab | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/MockEventResult.java | sha256:d85dbe27d72a12cf | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/MockMode.java | sha256:06c086880b91be3f | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/MockOutcome.java | sha256:95d8041d8651714b | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/MockPaymentEvent.java | sha256:13c00e1554930628 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/MockPaymentEventRepository.java | sha256:f9263d1fbc373a7c | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/NoApprovedAttemptException.java | sha256:684a81c5b9a7016c | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/Payment.java | sha256:47907dd46ea3e610 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/PaymentApproved.java | sha256:485c7276e31f8b92 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/PaymentAttempt.java | sha256:ffddff505a1f857a | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/PaymentAttemptId.java | sha256:3b720c82c02c7982 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/PaymentAttemptStatus.java | sha256:a7d74b89162e1ef8 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/PaymentFailed.java | sha256:1c0f9928640dd2b9 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/PaymentId.java | sha256:0d1e501d040b4e8b | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/PaymentRefunded.java | sha256:f8b3fd0cc69ee3db | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/PaymentRepository.java | sha256:7d6517182ab5e443 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/PaymentRequested.java | sha256:2da1bceabf95524b | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/PgTransactionMismatchException.java | sha256:7908c65ad94194b5 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/RefundReason.java | sha256:cca4a5006f383449 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/UnknownAttemptException.java | sha256:5fe7cae0d5cb9205 | 결제 | 없음 | 0fb9876 |
| backend/src/main/java/com/o2o/payment/domain/package-info.java | sha256:13096494dd3e11ca | 결제 | 없음 | 0fb9876 |

### backend/src/main/java/com/o2o/payment/infrastructure 8개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/payment/infrastructure/InProcessMockPaymentGateway.java | sha256:9cf8feb1d46b2796 | 결제 | 없음 | 22de11a |
| backend/src/main/java/com/o2o/payment/infrastructure/JpaMockPaymentEventRepository.java | sha256:efb2cde46f85a7ac | 결제 | 없음 | 22de11a |
| backend/src/main/java/com/o2o/payment/infrastructure/JpaPaymentRepository.java | sha256:75c26de276f29ad3 | 결제 | 없음 | 22de11a |
| backend/src/main/java/com/o2o/payment/infrastructure/MockAutoResultAdapter.java | sha256:07e7585a7dc2b0af | 결제 | 없음 | 22de11a |
| backend/src/main/java/com/o2o/payment/infrastructure/MockAutoResultResumeRunner.java | sha256:262fda55fa54258d | 결제 | 없음 | 22de11a |
| backend/src/main/java/com/o2o/payment/infrastructure/MockPaymentEventJpaRepository.java | sha256:0b8eb81aa8e66687 | 결제 | 없음 | 22de11a |
| backend/src/main/java/com/o2o/payment/infrastructure/PaymentJpaRepository.java | sha256:a7f7baecb9015628 | 결제 | 없음 | 9134a52 |
| backend/src/main/java/com/o2o/payment/infrastructure/package-info.java | sha256:0bc5ae1a1226afb7 | 결제 | 없음 | 22de11a |

### backend/src/main/java/com/o2o/shared 1개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/shared/ActorResolver.java | sha256:c40ff8e6d5a4547b | 숙소 | 결제 | e0cc55f |

### 테스트 7개

| 경로 | sha256 | 건수 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|---|
| backend/src/test/java/com/o2o/payment/CommittedPaymentEvents.java | sha256:49824d40d2284240 | 0 (테스트 지원 파일) | 결제 | 없음 | d56f04d |
| backend/src/test/java/com/o2o/payment/api/DevProfileBoundaryTest.java | sha256:e0b93bae5f524e4e | 1 | 결제 | 없음 | d56f04d |
| backend/src/test/java/com/o2o/payment/api/MockPaymentEventApiTest.java | sha256:1cb5ae1f18772c96 | 17 | 결제 | 없음 | d56f04d |
| backend/src/test/java/com/o2o/payment/application/PaymentApplicationServiceTest.java | sha256:1191ff3790bdfc8f | 7 | 결제 | 없음 | ce2dd62 |
| backend/src/test/java/com/o2o/payment/application/PaymentAutoResultTest.java | sha256:9efe7c5a7ecf9ad3 | 5 | 결제 | 없음 | d56f04d |
| backend/src/test/java/com/o2o/payment/application/PaymentEventTest.java | sha256:22da6313a4125edb | 4 | 결제 | 없음 | ce2dd62 |
| backend/src/test/java/com/o2o/payment/domain/PaymentTest.java | sha256:c363455340cd0f2b | 21 | 결제 | 없음 | cdd32ea |

### 설정 파일 2개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/resources/application.properties | sha256:a53c90a2ad96f493 | 숙소 | 결제 | 6e220b5 |
| backend/src/test/resources/application.properties | sha256:7a925f9016cc6666 | 숙소 | 결제, 예약 2차 | c4c38dd |

### 5-1. 합계

프로덕션 50개와 설정 2다. payment 49(api 7, application 7, domain 27, infrastructure 8), shared 1. 2절의 52는 설정 둘을 프로덕션에 센 수라 같은 것이다. 테스트 7파일 55건이고 그중 CommittedPaymentEvents.java는 지원 파일이다. 저장소 전체는 56파일 419건이다.

### 5-2. 이번 라운드에서 평가 대상이 아닌 것

4절과 같다. 예약 2차가 만든 PAY-01과 PAY-02의 중계와 결제 결과 처리(booking 패키지)는 예약 쌍이 본다.

### 5-3. 실행 결과 파일

| 단계 | 경로 | 무엇 |
|---|---|---|
| 4 | harness/out/task-S9-payment-R1/step4/ | 요약 test-summary.txt |
| 5 | harness/out/task-S9-payment-R1/step5/ | JUnit XML 34개와 요약 |
| 6 | harness/out/task-S9-payment-R1/step6/ | JUnit XML 36개와 http-calls.txt와 요약 |
| 9 | harness/out/task-S9-payment-R1/step9/ | JUnit XML 36개와 요약. 브랜치 시점 9134a52 |
| main 전체 | harness/out/task-S9-booking-lifecycle-R1/step9/ | JUnit XML 48개, 419건. 기준 커밋의 코드로 돈 결과다. 이 묶음 몫은 payment 여섯 |

단계 사본은 그 단계 시점의 코드로 돈 결과다. 기준 커밋의 코드로 돈 것은 마지막 행이다.
