# task-S9-payment 평가 대상 코드 목록

최초 작성: 2026-09-13
최종 갱신: 2026-09-13 (개정 1 보강 커밋 둘을 1절에 더하고 2-6절 설정 파일 설명을 갱신. 기준 커밋 9134a52. 그 전 같은 날 초안)

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
