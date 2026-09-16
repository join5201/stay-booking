# task-S9-payment R1 평가자 A 재평가

평가일: 2026-09-14

평가 역할: A

평가 기준 커밋: `1bdadfe5a78722f1e708d96bafc0f728eddfc4a1`

작업공간: `C:/Dev/potenup/99_projects/o2o-dev`

## 판정 요약

치명 1 / 보통 3 / 확인필요 1

작업공간 검증은 샌드박스 안에서 `spawnSync git EPERM`으로 한 번 실패했다. 동일한 읽기 전용 명령을 제한 밖에서 다시 실행해 10건 모두 PASS를 확인했다. 대상 파일 해시는 `eval-target-files.md` 5절의 앞 16자리와 모두 일치했다.

직접 전체 테스트는 테스트 태스크를 새로 실행해 443건, 실패 0, 오류 0, 건너뜀 0으로 끝났다. 결제 대상 6개 테스트 클래스는 계약과 같은 55건이다. 요청문의 전체 기준선 419건보다 24건 많지만 추가된 범위 밖 테스트를 읽을 수 없어 원인은 확인필요로 남겼다.

## 상세

| 심각도 | 위치(파일:행) | 위반 축 | 문제 | 근거(코드 또는 실행 결과) | 수정 제안 |
|---|---|---|---|---|---|
| 치명 | `backend/src/main/java/com/o2o/payment/application/PaymentApplicationService.java:202`, `:213`, `:246`; `backend/src/main/java/com/o2o/payment/domain/MockPaymentEvent.java:30`; `backend/src/test/java/com/o2o/payment/api/MockPaymentEventApiTest.java:543`, `:560`, `:588` | 멱등 규칙, 동시성과 트랜잭션, 계약 하강 | 서로 다른 Payment의 시도에 같은 eventId가 동시에 들어오면 INTERNAL-01 규칙 2의 결과를 보장하지 못한다. Payment 루트 잠금은 서로 다른 두 루트를 직렬화하지 않는다. 두 트랜잭션이 모두 eventId 미존재를 본 뒤 같은 기본키를 저장하면 한쪽은 커밋 시 유니크 충돌로 500이 될 수 있다. 계약상 다른 body면 409 MOCK_EVENT_CONFLICT여야 한다. 현재 Y22는 같은 Payment 한 건에 대해서만 경합한다. | `process`는 시도 소유 Payment를 먼저 잠그고 eventId를 일반 조회한 뒤 마지막에 저장한다. `event_id`는 기본키지만 전역 eventId 경쟁을 선점하거나 유니크 충돌 뒤 기존 body를 다시 비교하는 처리가 없다. 직접 MySQL 테스트 55건은 통과했으나 서로 다른 Payment를 사용하는 같은 eventId 경합 테스트가 없다. | eventId를 원자적으로 선점하거나 유니크 충돌 시 기존 기록을 다시 읽어 bodyHash에 따라 DUPLICATE 또는 MOCK_EVENT_CONFLICT로 끝내라. 서로 다른 bookingId와 Payment 두 개에 같은 eventId를 동시에 보내는 MySQL 경합 테스트를 추가하라. |
| 보통 | `backend/src/test/java/com/o2o/payment/application/PaymentAutoResultTest.java:187`; `backend/src/main/java/com/o2o/payment/infrastructure/MockAutoResultResumeRunner.java:36` | 계약 하강, 테스트 격리와 재현성 | T26의 앱 재시작 뒤 재개가 직접 검증되지 않는다. Y13은 이미 뜬 컨텍스트에서 REQUESTED 행을 심고 `resumeAutoResults()`를 직접 호출한다. `ApplicationReadyEvent` 연결과 기존 DB 행을 가진 실제 재기동 경로는 실행하지 않는다. | 직접 실행에서 Y13은 통과했다. 그러나 테스트 192행부터 208행까지는 수동 시드와 메서드 직접 호출뿐이다. T26은 접수 후 앱 재시작과 자동 재개를 요구한다. | 기존 REQUESTED 행을 보존한 테스트 DB로 컨텍스트를 다시 띄우거나 별도 프로세스를 재기동해 `ApplicationReadyEvent`가 자동 재개하는지 확인하라. |
| 보통 | `backend/src/main/java/com/o2o/payment/domain/Payment.java:188`; `backend/src/test/java/com/o2o/payment/domain/PaymentTest.java:119`, `:130` | 계약 하강 | `recordApproval`의 I7 방어선인 기존 승인 이력 검사 분기가 테스트로 내려가지 않았다. 현재 Y3은 승인 또는 환불 뒤 `openAttempt` 거절만 확인한다. 다른 시도의 승인 이력이 이미 있는 상태에서 `recordApproval`이 둘째 승인을 거절하는 계약은 직접 확인하지 않는다. | 06-4 1-2의 recordApproval Pre는 승인 이력 없음을 요구한다. 구현은 188행부터 190행에서 `AlreadyApprovedException`을 던지지만 허용된 테스트 55건에는 이 분기를 직접 실행하는 사례가 없다. | DB fixture로 기존 승인 이력과 별도 REQUESTED 시도를 준비하고 서비스의 승인 처리로 들어가 `AlreadyApprovedException`, 상태 무변경, 이벤트 미발행을 확인하라. |
| 보통 | `backend/src/main/java/com/o2o/payment/domain/MockPaymentEventRepository.java:9`; `backend/src/main/java/com/o2o/payment/infrastructure/MockPaymentEventJpaRepository.java:11`; `document/06-2-o2o-aggregates.md:19` | Repository 단위 | 애그리거트 목록에는 Payment만 결제 루트로 정의돼 있는데 별도 MockPaymentEvent에 domain Repository와 Spring Data Repository가 정의돼 있다. eventId 처리 원장은 필요하지만 현재 구조는 애그리거트 루트가 아닌 기술 기록을 도메인 Repository 단위로 노출한다. | 06-2 1절 결제 행은 Payment를 루트로 두고 PaymentAttempt만 내부 요소로 둔다. 코드에는 PaymentRepository 외에 MockPaymentEventRepository가 추가돼 있다. 작업 계약은 리포지토리 인터페이스 둘을 요구하지만 이 기록을 새 애그리거트 루트나 Repository 예외로 선언하지 않는다. | 기술 멱등 원장 포트를 application 또는 infrastructure 경계로 옮기거나, MockPaymentEvent를 독립 애그리거트 루트로 볼 근거와 경계를 계약에 명시하라. |
| 확인필요 | `harness/out/task-S9-payment-R1/eval-request-A.md:89`; `backend/build/test-results/test/` | 테스트 격리와 재현성 | 요청문 기준선은 419건인데 직접 실행은 443건이다. 결제 대상 55건은 일치하고 모두 통과했으므로 결제 코드 실패로 판정하지 않았다. 허용 입력 밖의 추가 테스트 파일을 열 수 없어 24건 증가가 평가 브랜치의 정상 갱신인지 작업공간 혼입인지 판단할 수 없다. | 작업공간 검증 10건은 PASS했고 대상 해시는 모두 일치했다. 직접 XML 집계는 50파일 443건이며 생성자 전달 main 사본은 48파일 419건이다. | 현재 평가 브랜치 기준 전체 테스트 수를 요청문에 갱신하거나, 24건을 추가한 테스트 파일 목록과 기준 커밋을 허용 입력으로 제공해 기준선 차이를 닫아라. |

## 실행 기록

| 명령 | 목적 | 종료 상태 | 결과 요약 | 직접 실행 여부 |
|---|---|---|---|---|
| `node harness/out/task-S9-payment-R1/verify-eval-workspace.mjs` | 평가 작업공간 검증 | 최초 실패 | 샌드박스의 `spawnSync git EPERM`으로 검사 0건 통과, 1건 실패 | 직접 실행 |
| 같은 작업공간 검증 명령 재실행 | 환경 제한을 제거한 동일 검증 | PASS | `ws.git`, `ws.commit`, `ws.agents`, `ws.list`, `ws.target-count`, `ws.target-exists`, `ws.target-hash`, `ws.target-drift`, `ws.clean`, `ws.doc-hash` 10건 PASS | 직접 실행 |
| `docker compose -f backend/docker-compose.yml up -d` | 테스트 DB 기동 | 최초 실패 | 샌드박스에서 Docker named pipe 접근 거부 | 직접 실행 |
| 같은 Docker 명령 재실행 | 테스트 DB 기동 | PASS | `o2o-catalog-mysql` Running, 컨테이너는 내리지 않음 | 직접 실행 |
| `$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3307/o2o_payment_test'; $env:JAVA_HOME='C:/Users/user/.jdks/ms-21.0.11'; .\backend\gradlew.bat -p backend test` | 전체 테스트 | 최초 실패 | 샌드박스에서 Gradle 배포 잠금 파일 접근 거부 | 직접 실행 |
| 같은 Gradle 명령 재실행 | 전체 테스트 | PASS | `BUILD SUCCESSFUL in 3m 31s`, XML 50파일 443건, 실패 0, 오류 0, 건너뜀 0. 결제 6파일 55건 모두 통과. `test`는 실행됐고 리소스 처리 2개만 UP-TO-DATE | 직접 실행 |
| `node harness/tools/check.mjs g1 backend/src/main/java/com/o2o/payment/domain/Payment.java --type code --artifact backend/build/test-results/test/TEST-com.o2o.payment.domain.PaymentTest.xml` | 도메인 G1 | PASS | 검사 6건, 테스트 21건, 실패 0 | 직접 실행 |
| `node harness/tools/check.mjs g1 backend/src/main/java/com/o2o/payment/application/PaymentApplicationService.java --type code --artifact backend/build/test-results/test/TEST-com.o2o.payment.application.PaymentApplicationServiceTest.xml` | 앱 서비스 G1 | PASS | 검사 6건, 테스트 7건, 실패 0 | 직접 실행 |
| `node harness/tools/check.mjs g1 backend/src/main/java/com/o2o/payment/api/MockPaymentEventController.java --type code --artifact backend/build/test-results/test/TEST-com.o2o.payment.api.MockPaymentEventApiTest.xml` | API G1 | PASS | 검사 6건, 테스트 17건, 실패 0 | 직접 실행 |
| `harness/out/task-S9-payment-R1/step4/test-summary.txt` | 브랜치 시점 4단계 결과 | 전달 결과 | 282건, 실패 0 | 생성자 전달 |
| `harness/out/task-S9-payment-R1/step5/test-summary.txt`와 결제 XML 4개 | 브랜치 시점 5단계 결과 | 전달 결과 | 전체 319건, 결제 37건, 실패 0 | 생성자 전달 |
| `harness/out/task-S9-payment-R1/step6/test-summary.txt`, `http-calls.txt`, 결제 XML 6개 | 브랜치 시점 6단계 결과 | 전달 결과 | 전체 337건, 결제 55건, 실패 0. 실제 서버 INTERNAL-01과 프로파일 경계 기록 포함 | 생성자 전달 |
| `harness/out/task-S9-payment-R1/step9/test-summary.txt`와 결제 XML 6개 | 브랜치 시점 9단계 결과 | 전달 결과 | 전체 337건, 결제 55건, 실패 0 | 생성자 전달 |
| `harness/out/task-S9-booking-lifecycle-R1/step9/test-summary.txt`와 결제 XML 6개 | 기준 커밋 코드의 main 결과 | 전달 결과 | 전체 419건, 결제 55건, 실패 0 | 생성자 전달 |

직접 테스트 종료 중 Hibernate가 일부 외래 키 삭제에 실패했다는 로그를 냈다. Gradle은 테스트 실패로 집계하지 않았고 종료 상태는 성공이었다. 이 로그를 기능 통과 근거로 쓰지 않았다.

## 검증 ID 결과

| ID | 통과 / 실패 / 미실행 | 근거 |
|---|---|---|
| T21 | 통과 | Y16과 Y22 직접 실행. 같은 거래의 같은 결과 재전달은 DUPLICATE이고 이벤트는 한 번이다. 단 서로 다른 Payment의 같은 eventId 경합은 S9-R1-A-01로 별도 실패다. |
| T26 | 실패 | Y13 자체는 통과했지만 실제 앱 재시작과 `ApplicationReadyEvent` 자동 호출을 실행하지 않았다. S9-R1-A-02. |
| T30 | 통과 | Y23 직접 실행. dev에서 경로와 행위자 어댑터가 활성이고 verify 프로파일에서 인증 필요 API 401, 내부 경로 404, 공개 API 200이다. |
| INTERNAL-01 400 INVALID_REQUEST | 통과 | Y18 직접 실행. 필수값, 길이, 타입, 범위, outcome, failureCode 조합, 미정의 필드를 확인했다. |
| INTERNAL-01 404 RESOURCE_NOT_FOUND | 통과 | Y17 직접 실행. 없는 시도를 404로 반환했다. |
| INTERNAL-01 409 MOCK_EVENT_CONFLICT | 통과 | Y17 직접 실행. 거래 ID 불일치, 같은 eventId의 다른 body, 종착 상태 반대 결과를 확인했다. 서로 다른 Payment의 동시 같은 eventId는 S9-R1-A-01로 실패다. |
| INTERNAL-01 409 PAYMENT_AMOUNT_MISMATCH | 통과 | Y17과 Y18 직접 실행. 금액과 통화 불일치 및 경계값을 확인했다. |
| T15 결제 몫 | 통과 | Y1과 Y10 직접 실행. REQUESTED가 있는 동안 다음 시도는 잠금 뒤 PAYMENT_IN_PROGRESS에 대응하는 예외로 거절된다. |
| T16 결제 몫 | 통과 | Y1, Y8, Y11, Y15 직접 실행. 실패 뒤 새 시도가 열리고 attemptCount가 증가한다. |
| T17 결제 몫 | 통과 | Y2와 Y8 직접 실행. 셋째 실패 attemptCount 3과 넷째 시도 거절을 확인했다. 예약 만료 몫은 범위 밖이다. |
| T20 결제 몫 | 통과 | Y7과 Y20 직접 실행. 지연 승인 뒤 환불의 종착 무해와 추가 환불 및 이벤트 미발행을 확인했다. 예약 EXPIRED 유지와 재고 몫은 범위 밖이다. |
| T22 결제 몫 | 통과 | Y20 직접 실행. 환불 뒤 같은 승인 재전달이 DUPLICATE이고 환불과 승인 이벤트가 늘지 않는다. |
| T23 결제 몫 | 통과 | Y21 직접 실행. 커밋 전 강제 실패에서 시도와 이벤트 기록이 롤백되고 재전달은 PROCESSED다. 예약 정책의 별도 트랜잭션 몫은 범위 밖이다. |
| Y1 | 통과 | `PaymentTest` 직접 실행, 3건. |
| Y2 | 통과 | `PaymentTest` 직접 실행, 경계 양쪽. |
| Y3 | 통과 | `PaymentTest` 직접 실행. 단 recordApproval의 I7 분기는 S9-R1-A-03. |
| Y4 | 통과 | `PaymentTest` 직접 실행, 금액 불일치와 허용값. |
| Y5 | 통과 | `PaymentTest` 직접 실행, 승인과 실패 전이 및 역행 거절. |
| Y6 | 통과 | `PaymentTest` 직접 실행, 같은 결과 무해와 거래 및 금액 대조. |
| Y7 | 통과 | `PaymentTest`, `PaymentApplicationServiceTest` 직접 실행. |
| Y8 | 통과 | `PaymentEventTest` 직접 실행, 이벤트 넷과 attemptCount. |
| Y9 | 통과 | `PaymentApplicationServiceTest` 직접 실행, MySQL 유니크 둘. |
| Y10 | 통과 | `PaymentApplicationServiceTest` 직접 실행, 실제 MySQL 잠금 대기. |
| Y11 | 통과 | `PaymentAutoResultTest` 직접 실행, APPROVE, DECLINE, DEFER. |
| Y12 | 통과 | `PaymentAutoResultTest` 직접 실행, 롤백 뒤 구독 미도착. |
| Y13 | 통과 | `PaymentAutoResultTest` 직접 실행, 수동 러너 호출의 재개와 재호출 무해. 실제 재시작은 T26 실패로 분리. |
| Y14 | 통과 | `PaymentApplicationServiceTest` 직접 실행, 정렬, 요약, 환불 투영, 빈 결과. |
| Y15 | 통과 | `MockPaymentEventApiTest` 직접 실행, 실제 포트의 승인과 실패. |
| Y16 | 통과 | `MockPaymentEventApiTest` 직접 실행, eventId 및 거래 결과 중복. |
| Y17 | 통과 | `MockPaymentEventApiTest` 직접 실행, 오류 분기와 무변경. |
| Y18 | 통과 | `MockPaymentEventApiTest` 직접 실행, body 형식과 경계. |
| Y19 | 통과 | `MockPaymentEventApiTest` 직접 실행, 401, 403, MOCK_SYSTEM 허용. |
| Y20 | 통과 | `MockPaymentEventApiTest` 직접 실행, 환불 뒤 승인 재전달. |
| Y21 | 통과 | `MockPaymentEventApiTest` 직접 실행, 강제 롤백과 재전달. |
| Y22 | 통과 | `MockPaymentEventApiTest` 직접 실행, 같은 Payment의 동시 요청 둘. 전역 eventId 경합은 포함하지 않는다. |
| Y23 | 통과 | `MockPaymentEventApiTest`, `DevProfileBoundaryTest` 직접 실행. |

## 보조 표

| 원본 번호 | 지적 ID | 심각도 | 위반 기준 |
|---:|---|---|---|
| 1 | S9-R1-A-01 | 치명 | INTERNAL-01 규칙 2, eventId 유일, 동시성과 트랜잭션, 계약 하강 |
| 2 | S9-R1-A-02 | 보통 | T26, 계약 하강, 테스트 격리와 재현성 |
| 3 | S9-R1-A-03 | 보통 | I7, 06-4 1-2 recordApproval Pre, 계약 하강 |
| 4 | S9-R1-A-04 | 보통 | Repository 단위, 06-2 1절 애그리거트 목록 |
| 5 | S9-R1-A-05 | 확인필요 | 전체 테스트 기준선, 테스트 격리와 재현성 |

## 담당 확인

| 항목 | 담당 | 반영 |
|---|---|---|
| 구조 축 4개 | 평가자 A | 상세와 보조 표에 반영 |
| 코드 전용 축 5개 | 평가자 A | 상세와 검증 ID 결과에 반영 |
| 요구사항 역추적표 | 평가자 B | 이 리포트에서 제외 |
| 소스, 테스트, 설정 수정 | 평가자 아님 | 수정 없음 |

## 읽은 파일과 SHA256

아래 SHA256은 실제 파일의 앞 16자리다. 요청문에 기록된 해시와 비교할 때도 같은 길이를 사용했다. 대상 목록에 든 59개 파일은 전부 실제 값과 일치했다.

### 지침과 계약, 문서

| 파일 | sha256 앞 16자리 |
|---|---|
| `AGENTS.md` | `6cf0a192aab3bba8` |
| `backend/AGENTS.md` | `132a0c23c5f10904` |
| `harness/out/task-S9-payment-R1/eval-request-A.md` | `dd55c06ce877934e` |
| `harness/out/task-S9-payment-R1/eval-target-files.md` | `4afdd900b3dc8f02` |
| `harness/project-sync/o2o-review-input-pack.md` | `1608942c0815f911` |
| `harness/prompts/eval-criteria-code.md` | `c5ed835951fe09a5` |
| `harness/tasks/task-S9-payment.md` | `71a624284e313402` |
| `document/11-o2o-api-spec.md` | `3f2613a77b649903` |
| `document/06-2-o2o-aggregates.md` | `113c6734b5525e1d` |
| `document/06-4-o2o-contracts.md` | `edacbe47d6d63e3f` |
| `document/06-1-o2o-context-map.md` | `95bc2b1079d3b739` |
| `harness/decisions/decisions-08-3.md` | `1b8580aa86c18fd8` |
| `backend/.claude/rules/layers.md` | `cb9e4f68bdc4c43c` |

### 프로덕션과 설정 파일

| 파일 | sha256 앞 16자리 |
|---|---|
| `backend/src/main/java/com/o2o/payment/api/FailureCodeMatchesOutcome.java` | `db5bc260eca12935` |
| `backend/src/main/java/com/o2o/payment/api/FailureCodeMatchesOutcomeValidator.java` | `ab8c1076bb5a9f5e` |
| `backend/src/main/java/com/o2o/payment/api/MockEventResultResponse.java` | `398a964bbaf06832` |
| `backend/src/main/java/com/o2o/payment/api/MockPaymentEventController.java` | `48fa0e65241f5635` |
| `backend/src/main/java/com/o2o/payment/api/MockPaymentEventRequest.java` | `f2b93f656a94a83d` |
| `backend/src/main/java/com/o2o/payment/api/PaymentExceptionHandler.java` | `3ab8ce787019c574` |
| `backend/src/main/java/com/o2o/payment/api/package-info.java` | `e93ca6ac6193bd83` |
| `backend/src/main/java/com/o2o/payment/application/MockEventCommand.java` | `edb6f454c04420f3` |
| `backend/src/main/java/com/o2o/payment/application/MockPaymentGateway.java` | `cf381adf3f781d08` |
| `backend/src/main/java/com/o2o/payment/application/PaymentApplicationService.java` | `ab1b91e47881635f` |
| `backend/src/main/java/com/o2o/payment/application/PaymentAttemptView.java` | `9ffeeb0ca47cd9d5` |
| `backend/src/main/java/com/o2o/payment/application/PaymentSummaryView.java` | `1449c9f28657ef6c` |
| `backend/src/main/java/com/o2o/payment/application/RefundView.java` | `9639df24c3aad354` |
| `backend/src/main/java/com/o2o/payment/application/package-info.java` | `e8133cc9192da34c` |
| `backend/src/main/java/com/o2o/payment/domain/AlreadyApprovedException.java` | `04f3451dedd909f1` |
| `backend/src/main/java/com/o2o/payment/domain/AmountMismatchException.java` | `ddf417aa45e058bf` |
| `backend/src/main/java/com/o2o/payment/domain/AttemptInProgressException.java` | `854c2da0b4badfe3` |
| `backend/src/main/java/com/o2o/payment/domain/AttemptKind.java` | `ef33f689b6fbe168` |
| `backend/src/main/java/com/o2o/payment/domain/AttemptLimitExceededException.java` | `a0ee26cecfd648ff` |
| `backend/src/main/java/com/o2o/payment/domain/InvalidAttemptTransitionException.java` | `8ccd869b44c1262f` |
| `backend/src/main/java/com/o2o/payment/domain/MockEventConflictException.java` | `d69963abbed788ab` |
| `backend/src/main/java/com/o2o/payment/domain/MockEventResult.java` | `d85dbe27d72a12cf` |
| `backend/src/main/java/com/o2o/payment/domain/MockMode.java` | `06c086880b91be3f` |
| `backend/src/main/java/com/o2o/payment/domain/MockOutcome.java` | `95d8041d8651714b` |
| `backend/src/main/java/com/o2o/payment/domain/MockPaymentEvent.java` | `13c00e1554930628` |
| `backend/src/main/java/com/o2o/payment/domain/MockPaymentEventRepository.java` | `f9263d1fbc373a7c` |
| `backend/src/main/java/com/o2o/payment/domain/NoApprovedAttemptException.java` | `684a81c5b9a7016c` |
| `backend/src/main/java/com/o2o/payment/domain/Payment.java` | `47907dd46ea3e610` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentApproved.java` | `485c7276e31f8b92` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentAttempt.java` | `ffddff505a1f857a` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentAttemptId.java` | `3b720c82c02c7982` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentAttemptStatus.java` | `a7d74b89162e1ef8` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentFailed.java` | `1c0f9928640dd2b9` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentId.java` | `0d1e501d040b4e8b` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentRefunded.java` | `f8b3fd0cc69ee3db` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentRepository.java` | `7d6517182ab5e443` |
| `backend/src/main/java/com/o2o/payment/domain/PaymentRequested.java` | `2da1bceabf95524b` |
| `backend/src/main/java/com/o2o/payment/domain/PgTransactionMismatchException.java` | `7908c65ad94194b5` |
| `backend/src/main/java/com/o2o/payment/domain/RefundReason.java` | `cca4a5006f383449` |
| `backend/src/main/java/com/o2o/payment/domain/UnknownAttemptException.java` | `5fe7cae0d5cb9205` |
| `backend/src/main/java/com/o2o/payment/domain/package-info.java` | `13096494dd3e11ca` |
| `backend/src/main/java/com/o2o/payment/infrastructure/InProcessMockPaymentGateway.java` | `9cf8feb1d46b2796` |
| `backend/src/main/java/com/o2o/payment/infrastructure/JpaMockPaymentEventRepository.java` | `efb2cde46f85a7ac` |
| `backend/src/main/java/com/o2o/payment/infrastructure/JpaPaymentRepository.java` | `75c26de276f29ad3` |
| `backend/src/main/java/com/o2o/payment/infrastructure/MockAutoResultAdapter.java` | `07e7585a7dc2b0af` |
| `backend/src/main/java/com/o2o/payment/infrastructure/MockAutoResultResumeRunner.java` | `262fda55fa54258d` |
| `backend/src/main/java/com/o2o/payment/infrastructure/MockPaymentEventJpaRepository.java` | `0b8eb81aa8e66687` |
| `backend/src/main/java/com/o2o/payment/infrastructure/PaymentJpaRepository.java` | `a7f7baecb9015628` |
| `backend/src/main/java/com/o2o/payment/infrastructure/package-info.java` | `0bc5ae1a1226afb7` |
| `backend/src/main/java/com/o2o/shared/ActorResolver.java` | `c40ff8e6d5a4547b` |
| `backend/src/main/resources/application.properties` | `a53c90a2ad96f493` |
| `backend/src/test/resources/application.properties` | `7a925f9016cc6666` |

### 테스트 파일

| 파일 | sha256 앞 16자리 |
|---|---|
| `backend/src/test/java/com/o2o/payment/CommittedPaymentEvents.java` | `49824d40d2284240` |
| `backend/src/test/java/com/o2o/payment/api/DevProfileBoundaryTest.java` | `e0b93bae5f524e4e` |
| `backend/src/test/java/com/o2o/payment/api/MockPaymentEventApiTest.java` | `1cb5ae1f18772c96` |
| `backend/src/test/java/com/o2o/payment/application/PaymentApplicationServiceTest.java` | `1191ff3790bdfc8f` |
| `backend/src/test/java/com/o2o/payment/application/PaymentAutoResultTest.java` | `9efe7c5a7ecf9ad3` |
| `backend/src/test/java/com/o2o/payment/application/PaymentEventTest.java` | `22da6313a4125edb` |
| `backend/src/test/java/com/o2o/payment/domain/PaymentTest.java` | `c363455340cd0f2b` |

### 생성자 전달 실행 결과

| 파일 | sha256 앞 16자리 |
|---|---|
| `harness/out/task-S9-payment-R1/step4/test-summary.txt` | `221c6dad3029d3ba` |
| `harness/out/task-S9-payment-R1/step5/test-summary.txt` | `848f5d32214ea67f` |
| `harness/out/task-S9-payment-R1/step6/test-summary.txt` | `9203c5ded9207af1` |
| `harness/out/task-S9-payment-R1/step6/http-calls.txt` | `0e2987971b66ea2c` |
| `harness/out/task-S9-payment-R1/step9/test-summary.txt` | `3e90a3d5cdf08077` |
| `harness/out/task-S9-booking-lifecycle-R1/step9/test-summary.txt` | `70607c73cd1a146e` |
| `harness/out/task-S9-payment-R1/step5/TEST-com.o2o.payment.application.PaymentApplicationServiceTest.xml` | `690fb2a203900bb4` |
| `harness/out/task-S9-payment-R1/step5/TEST-com.o2o.payment.application.PaymentAutoResultTest.xml` | `f7e8ca6c97199bd5` |
| `harness/out/task-S9-payment-R1/step5/TEST-com.o2o.payment.application.PaymentEventTest.xml` | `65a7cd5d8d0b000d` |
| `harness/out/task-S9-payment-R1/step5/TEST-com.o2o.payment.domain.PaymentTest.xml` | `6e4f775d6b448eef` |
| `harness/out/task-S9-payment-R1/step6/TEST-com.o2o.payment.api.DevProfileBoundaryTest.xml` | `919a3a6a2f79df30` |
| `harness/out/task-S9-payment-R1/step6/TEST-com.o2o.payment.api.MockPaymentEventApiTest.xml` | `ddef9278d3e085a7` |
| `harness/out/task-S9-payment-R1/step6/TEST-com.o2o.payment.application.PaymentApplicationServiceTest.xml` | `65ecc21fdb79c6bc` |
| `harness/out/task-S9-payment-R1/step6/TEST-com.o2o.payment.application.PaymentAutoResultTest.xml` | `c85bdfbd26fc9c8d` |
| `harness/out/task-S9-payment-R1/step6/TEST-com.o2o.payment.application.PaymentEventTest.xml` | `29a488a2a738eab4` |
| `harness/out/task-S9-payment-R1/step6/TEST-com.o2o.payment.domain.PaymentTest.xml` | `5e58f1f0990d3724` |
| `harness/out/task-S9-payment-R1/step9/TEST-com.o2o.payment.api.DevProfileBoundaryTest.xml` | `765ed0c56c878fa0` |
| `harness/out/task-S9-payment-R1/step9/TEST-com.o2o.payment.api.MockPaymentEventApiTest.xml` | `30cd2f192fc0bb47` |
| `harness/out/task-S9-payment-R1/step9/TEST-com.o2o.payment.application.PaymentApplicationServiceTest.xml` | `b7e1fa7d6515f366` |
| `harness/out/task-S9-payment-R1/step9/TEST-com.o2o.payment.application.PaymentAutoResultTest.xml` | `e3844ac89a7a6111` |
| `harness/out/task-S9-payment-R1/step9/TEST-com.o2o.payment.application.PaymentEventTest.xml` | `db150b8f971f8434` |
| `harness/out/task-S9-payment-R1/step9/TEST-com.o2o.payment.domain.PaymentTest.xml` | `c7294664781c6a3b` |
| `harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.api.DevProfileBoundaryTest.xml` | `85e512343a4854f3` |
| `harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.api.MockPaymentEventApiTest.xml` | `9f3eb94ddf53bbb3` |
| `harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.application.PaymentApplicationServiceTest.xml` | `ce7ceea5e95e2598` |
| `harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.application.PaymentAutoResultTest.xml` | `4b3e29c25e97fb5a` |
| `harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.application.PaymentEventTest.xml` | `fd777c3e8b47348d` |
| `harness/out/task-S9-booking-lifecycle-R1/step9/TEST-com.o2o.payment.domain.PaymentTest.xml` | `c34af0fbad273880` |

### 직접 실행 결제 JUnit XML

| 파일 | sha256 앞 16자리 |
|---|---|
| `backend/build/test-results/test/TEST-com.o2o.payment.api.DevProfileBoundaryTest.xml` | `9dacf048174242ea` |
| `backend/build/test-results/test/TEST-com.o2o.payment.api.MockPaymentEventApiTest.xml` | `720b73873d0f14de` |
| `backend/build/test-results/test/TEST-com.o2o.payment.application.PaymentApplicationServiceTest.xml` | `fefc3287f5476b57` |
| `backend/build/test-results/test/TEST-com.o2o.payment.application.PaymentAutoResultTest.xml` | `6d1978d05824b6cf` |
| `backend/build/test-results/test/TEST-com.o2o.payment.application.PaymentEventTest.xml` | `fb09a0ab7de2edd3` |
| `backend/build/test-results/test/TEST-com.o2o.payment.domain.PaymentTest.xml` | `0fcff5549d7581e8` |

직접 전체 집계에는 `backend/build/test-results/test/TEST-*.xml` 50개를 읽었다. 판정 근거로 쓴 결제 XML 6개는 위에 개별 SHA256을 남겼고, 나머지 44개는 전체 443건과 기준선 차이만 확인하는 데 사용했다. 범위 밖 테스트의 내용은 읽지 않았다.
