# 사용자 결정표 task-S9-payment R1

양식: harness/prompts/decision-table.md v4 (2026-09-08)
최초 작성: 2026-09-16
최종 갱신: 2026-09-16 (초안. 결정 8건은 개발 세션이 대조해 적었고 사용자 확정 대기)

왜 이 파일이 필요한가: 리포트를 눈으로 읽고 반영하면 자기 결정에 불리한 지적이 조용히 빠진다(HR4). 원본 행을 하나씩 옮겨 두면 g2가 ID 집합과 심각도를 원본과 대조할 수 있다.

이 표의 행은 원본 리포트의 상세 표와 보조 표에서 셀 단위로 스크립트가 뽑아 만들었다(원문 지적, 근거, 수정 제안은 글자 그대로). 결정과 이유 열은 개발 세션이 초안을 적었고 그 근거는 지적마다 실제 파일을 열어 대조한 것이다. 위치를 이유에 적었다. 라운드 mvp-eval-2026-09-14의 쌍 5다(harness/out/mvp-eval-2026-09-14/README.md). 이 쌍을 맡았던 결제 세션이 비활동이라 개발 세션이 이어받아 썼다.

## 대상과 원본

| 항목 | 기록 |
|---|---|
| Step | 9 |
| 작업 유형 | 코드 |
| 평가 라운드 | R1 |
| 평가 대상 절대경로와 파일 목록 | harness/out/task-S9-payment-R1/eval-target-files.md |
| 평가 대상 버전 또는 해시 | sha256:4afdd900b3dc8f02. 목록 5절(main 기준)이 가리키는 프로덕션 50개와 테스트 7파일 55건과 설정 2개의 기준 커밋은 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1(PR 138 병합, main)이고 파일별 sha256은 목록 5절과 A와 B 리포트의 읽은 파일 표에 있다. 그 59개는 현재 origin/main(635071e)에서도 같다 |
| 승인된 작업 계약 절대경로와 버전 | harness/tasks/task-S9-payment.md sha256:71a624284e313402 |
| A 원본 리포트 절대경로와 버전 또는 해시 | harness/reviews/task-S9-payment-R1-A.md sha256:8c70c7fe3c2674de |
| B 원본 리포트 절대경로와 버전 또는 해시 | harness/reviews/task-S9-payment-R1-B.md sha256:16a5e1e58177fe4a |
| A 원본 지적 수 | 5 |
| B 원본 지적 수 | 3 |
| 결정표 전체 행 수 | 8 |
| 판단한 사용자 | 초안. join5201 확정 대기. 요청문은 eval-request-A.md sha256:dd55c06ce877934e와 eval-request-B.md sha256:f513785b4c81de9c |
| 결정 날짜 | 2026-09-16 (초안) |

A는 치명 1, 보통 3, 확인필요 1이다. B는 치명 0, 보통 2, 확인필요 1이다. 합쳐서 치명 1, 보통 5, 확인필요 2다. 초안 결정은 수용 5, 거부 1, 반박 2다.

치명 하나(A-01)는 서로 다른 Payment의 시도 둘에 같은 eventId가 같은 순간 올 때 INTERNAL-01 규칙 2가 깨지는 것이다. Payment 잠금이 두 루트를 줄 세우지 못해 한쪽이 500이 되거나 먼저 저장된 기록이 덮인다. 수용 다섯 중 코드가 바뀌는 것은 A-01 하나이고 A-02, A-03, B-01, B-02는 테스트만 더한다. 거부 하나(A-04)는 계약이 정한 자리를 그대로 두는 것이고, 반박 둘(A-05, B-03)은 근거 파일이 저장소 안에 있다(숙소 R1 반영 기록, shared Money와 그 테스트).

### 평가 절차 기록 (2026-09-16)

라운드 계획의 D-2 가는 쌍마다 A와 B를 새 작업으로 여는 것이다. 이 쌍은 그렇게 돌았다. 사용자가 2026-09-14 22:09에 Codex 새 작업 둘을 열었고(A 22:09:42, B 22:09:47. 작업 폴더는 요청문대로 o2o-dev 작업 트리) B가 22:18:54에, A가 22:26:50에 리포트를 끝냈다(Codex 작업 기록의 task_complete). 리포트 둘은 o2o-dev에 미커밋으로 남았고 이 쌍을 맡은 결제 세션이 비활동이라 개발 세션이 2026-09-16 이어받아(사용자 발언 현재 결제 세션 비활동 상태다. 이어받아라. 착지하고 결정표 초안 만들어라) 검사 넷을 돌린 뒤 origin/main(635071e)에서 딴 브랜치 docs/task-s9-payment-r1-decision에 바이트 그대로 착지했다(커밋 b0466ce). 결제 세션이 o2o-dev에 남긴 결정표 초안(미커밋. 결정 열이 빈 8행, g2 pre 28건 중 24건 실패)은 쓰지 않고 이 표를 스크립트로 새로 만들었다.

| 확인 | 결과 |
|---|---|
| 파일 | 요청문이 정한 경로에 A와 B 하나씩. 두 작업의 파일 쓰기는 각자의 리포트 하나뿐이다(작업 기록의 apply_patch 대상. A는 새 파일 1과 갱신 2, B는 새 파일 1과 갱신 1). o2o-dev의 git status에 그 둘 외에 두 작업이 만든 파일은 없다. A의 전체 테스트가 남긴 backend/build는 추적 밖이다 |
| 보조 표 | A 5행, B 3행. 판정 요약의 등급별 수와 같다(A 1, 3, 1. B 0, 2, 1). g2 report-schema 통과 |
| 읽은 파일 | A 106개, B 72개. 전부 요청문의 허용 입력과 대상 목록 59개와 전달 실행 결과 사본 안이다. 금지 목록(harness/reviews/, step9-verification.md, 라운드 README, 다른 쌍의 결정표, harness/state/, harness/docs/)은 어느 표에도 없다. 해시는 B 72개 전부와 A 77개가 현재 파일과 같다. A의 나머지 29개 중 23개는 전달 실행 결과 사본(단계 XML과 http-calls.txt)으로 o2o-dev 작업 사본의 CRLF 판 해시이고 git 저장 내용(blob)은 같다. 6개는 A가 직접 돌린 backend/build의 XML이라 여기 없다 |
| 서로의 인용 | A 리포트에 B의 지적 ID와 경로 0건, B 리포트에 A의 지적 ID 0건. B의 블라인드 경계 확인 절은 같은 쌍의 다른 리포트를 읽지 않았다고 적는다. A가 끝난 22:26에 B의 파일이 이미 있었으나 A의 읽은 파일 표에 그 경로가 없다 |
| A의 실행 | Docker와 Gradle이 샌드박스에서 한 번씩 막혔고 제한 밖 재실행으로 verify 10건 PASS, 전체 테스트 443건 실패 0, g1 code 셋 PASS. 443건과 요청문 419건의 차이는 A-05 행이다(숙소 R1 반영 24건. o2o-dev가 그때 af34431 기반 브랜치였다). 결제 대상 59개는 기준 커밋과 그 트리와 현재 origin/main에서 같다. 개발 세션도 origin/main 635071e에서 verify 10건 PASS를 다시 확인했다(2026-09-16). 이 초안은 Docker Desktop 엔진이 꺼진 상태에서 정적 대조로 썼다 |
| B의 실행 | 요청문 4절대로 테스트 미실행. verify 10건 PASS. 정적 대조만이고 요구사항 역추적표 25행 중 미커버 1(U5. B-02)과 확인필요 1(KRW. B-03) |

재평가(R2)는 반영 뒤 사용자 판단이다. 반박 둘(A-05, B-03)의 근거 파일은 저장소 안에 있어 R2 없이 닫히고, 다음 라운드 요청문에는 평가 시점의 전체 건수와 shared Money 둘을 넣는다.

## 지적별 결정

지적 ID 규약: S{Step}-R{라운드}-{A 또는 B}-{원본 번호 2자리}.

| 리포트 A/B | 원본 번호 | 지적 ID | 심각도 | 대상 위치 | 위반 축 | 위반 기준 | 원문 지적 | 원문 근거 | 수정 제안 | 결정 | 이유 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| A | 01 | S9-R1-A-01 | 치명 | `backend/src/main/java/com/o2o/payment/application/PaymentApplicationService.java:202`, `:213`, `:246`; `backend/src/main/java/com/o2o/payment/domain/MockPaymentEvent.java:30`; `backend/src/test/java/com/o2o/payment/api/MockPaymentEventApiTest.java:543`, `:560`, `:588` | 멱등 규칙, 동시성과 트랜잭션, 계약 하강 | INTERNAL-01 규칙 2, eventId 유일, 동시성과 트랜잭션, 계약 하강 | 서로 다른 Payment의 시도에 같은 eventId가 동시에 들어오면 INTERNAL-01 규칙 2의 결과를 보장하지 못한다. Payment 루트 잠금은 서로 다른 두 루트를 직렬화하지 않는다. 두 트랜잭션이 모두 eventId 미존재를 본 뒤 같은 기본키를 저장하면 한쪽은 커밋 시 유니크 충돌로 500이 될 수 있다. 계약상 다른 body면 409 MOCK_EVENT_CONFLICT여야 한다. 현재 Y22는 같은 Payment 한 건에 대해서만 경합한다. | `process`는 시도 소유 Payment를 먼저 잠그고 eventId를 일반 조회한 뒤 마지막에 저장한다. `event_id`는 기본키지만 전역 eventId 경쟁을 선점하거나 유니크 충돌 뒤 기존 body를 다시 비교하는 처리가 없다. 직접 MySQL 테스트 55건은 통과했으나 서로 다른 Payment를 사용하는 같은 eventId 경합 테스트가 없다. | eventId를 원자적으로 선점하거나 유니크 충돌 시 기존 기록을 다시 읽어 bodyHash에 따라 DUPLICATE 또는 MOCK_EVENT_CONFLICT로 끝내라. 서로 다른 bookingId와 Payment 두 개에 같은 eventId를 동시에 보내는 MySQL 경합 테스트를 추가하라. | 수용 | 사실이다. PaymentApplicationService의 process는 202행에서 시도를 가진 Payment 하나를 잠그고 213행에서 eventId 기록을 잠금 없이 조회한 뒤 233행이나 246행에서 저장한다. 같은 eventId가 서로 다른 Payment의 시도 둘로 같은 순간 오면 둘은 각자의 루트를 잠그므로 줄을 서지 않고, 둘 다 기록 없음을 본 뒤 같은 기본키를 저장한다. 결과는 둘 중 하나다. 첫째의 커밋 전에 둘째의 저장이 실행되면 둘째가 커밋에서 기본키 충돌로 롤백되고, PaymentExceptionHandler와 SharedExceptionHandler에 DataIntegrityViolation 매핑이 없어 UnexpectedExceptionResolver의 500이 나간다(계약 2절 표 7행은 409 MOCK_EVENT_CONFLICT). 첫째가 먼저 커밋한 뒤 둘째의 저장이 실행되면 더 나쁘다. JpaMockPaymentEventRepository.save(24행)는 Spring Data의 save이고 MockPaymentEvent는 배정 기본키에 @Version이 없어 새 객체가 merge로 저장되는데, merge는 그 키의 행을 다시 읽어 있으면 UPDATE한다. 첫째의 기록이 둘째의 시도 ID와 body 해시로 덮이고 둘 다 200 PROCESSED가 된다. 11 INTERNAL-01 규칙 2(eventId를 유일하게 저장한다. 다른 body면 MOCK_EVENT_CONFLICT)가 깨지므로 치명이 맞다. Y22 둘(MockPaymentEventApiTest 543행, 560행)은 같은 Payment 한 건만 경합시켜 이 길을 지나지 않는다. 반영은 수정 제안의 둘째 길이다. 기록 저장을 persist 의미로 바꿔 덮어쓰기를 막고, 기본키 충돌로 롤백된 요청은 트랜잭션 밖에서 기록을 다시 읽어 body가 같으면 DUPLICATE, 다르면 409 MOCK_EVENT_CONFLICT로 답한다(예약 묶음 IdempotentRequestExecutor 51행부터의 DataAccessException 재조회와 같은 모양). 서로 다른 Payment의 body는 paymentAttemptId가 달라 언제나 다르므로 경합의 패자는 409다. 테스트는 서로 다른 bookingId의 Payment 둘에 같은 eventId를 동시에 보내는 MySQL 경합 한 건(하나는 200 PROCESSED, 하나는 409 MOCK_EVENT_CONFLICT, 기록은 승자의 시도 하나, 패자의 시도는 REQUESTED 그대로, PaymentApproved 한 번)을 Y22 옆에 둔다. 계약 8-1절에 Y24로 더한다 |
| A | 02 | S9-R1-A-02 | 보통 | `backend/src/test/java/com/o2o/payment/application/PaymentAutoResultTest.java:187`; `backend/src/main/java/com/o2o/payment/infrastructure/MockAutoResultResumeRunner.java:36` | 계약 하강, 테스트 격리와 재현성 | T26, 계약 하강, 테스트 격리와 재현성 | T26의 앱 재시작 뒤 재개가 직접 검증되지 않는다. Y13은 이미 뜬 컨텍스트에서 REQUESTED 행을 심고 `resumeAutoResults()`를 직접 호출한다. `ApplicationReadyEvent` 연결과 기존 DB 행을 가진 실제 재기동 경로는 실행하지 않는다. | 직접 실행에서 Y13은 통과했다. 그러나 테스트 192행부터 208행까지는 수동 시드와 메서드 직접 호출뿐이다. T26은 접수 후 앱 재시작과 자동 재개를 요구한다. | 기존 REQUESTED 행을 보존한 테스트 DB로 컨텍스트를 다시 띄우거나 별도 프로세스를 재기동해 `ApplicationReadyEvent`가 자동 재개하는지 확인하라. | 수용 | 사실이다. Y13(PaymentAutoResultTest 187행부터)은 계약 8-1절 Y13 행(360행)이 정한 그대로 REQUESTED 시도를 심고 resumeAutoResults()를 직접 부른다. 그래서 재개 논리는 검증됐지만 계약 7절 D-1(236행부터)의 결정 가(243행)가 적은 ApplicationReadyEvent 러너, 곧 MockAutoResultResumeRunner 36행의 @EventListener(ApplicationReadyEvent.class)와 onApplicationReady는 어느 테스트도 지나지 않는다. 그 한 줄이 빠져도 55건이 전부 통과하므로 T26(자동 Mock 모드에서 접수 후 앱 재시작. 저장된 시도로 결과 처리를 재개)의 재시작 몫은 테스트로 내려가지 않은 계약 항목이다. 반영은 테스트 하나다. REQUESTED이고 mockMode APPROVE인 시도를 심은 뒤 테스트 컨텍스트에 ApplicationReadyEvent를 다시 발행해(SpringApplication 인스턴스와 이 컨텍스트로 만든 이벤트) 시도가 APPROVED가 되고 auto_ 뒤 attemptId의 기록이 PROCESSED 하나이며 PaymentApproved가 한 번 나는지 본다. 앱 안의 ApplicationReadyEvent 구독자는 이 러너 하나라 다른 부작용이 없다. 두 번째 애플리케이션 컨텍스트를 띄우는 방법은 테스트 프로파일이 create-drop이라 심은 행을 지우므로 쓰지 않고, 별도 프로세스 재기동은 테스트 밖이라 하지 않는다. 계약 8-1절에 Y25로 더한다 |
| A | 03 | S9-R1-A-03 | 보통 | `backend/src/main/java/com/o2o/payment/domain/Payment.java:188`; `backend/src/test/java/com/o2o/payment/domain/PaymentTest.java:119`, `:130` | 계약 하강 | I7, 06-4 1-2 recordApproval Pre, 계약 하강 | `recordApproval`의 I7 방어선인 기존 승인 이력 검사 분기가 테스트로 내려가지 않았다. 현재 Y3은 승인 또는 환불 뒤 `openAttempt` 거절만 확인한다. 다른 시도의 승인 이력이 이미 있는 상태에서 `recordApproval`이 둘째 승인을 거절하는 계약은 직접 확인하지 않는다. | 06-4 1-2의 recordApproval Pre는 승인 이력 없음을 요구한다. 구현은 188행부터 190행에서 `AlreadyApprovedException`을 던지지만 허용된 테스트 55건에는 이 분기를 직접 실행하는 사례가 없다. | DB fixture로 기존 승인 이력과 별도 REQUESTED 시도를 준비하고 서비스의 승인 처리로 들어가 `AlreadyApprovedException`, 상태 무변경, 이벤트 미발행을 확인하라. | 수용 | 사실이다. Payment.recordApproval(178행부터)은 188행부터 190행에서 다른 NORMAL 시도에 승인 이력이 있으면 AlreadyApprovedException을 던진다. 06-4 1-2 recordApproval Pre의 승인 이력 없음이고 I7의 둘째 방어선이다. PaymentTest의 Y3 둘(119행, 130행)은 openAttempt의 거절만 보고 recordApproval의 이 분기를 지나는 테스트는 55건에 없다. 이 상태(승인 시도 하나와 REQUESTED 시도 하나)는 openAttempt가 I7과 I9로 막아 서비스 길로는 만들 수 없으므로 분기는 방어선이지 도달 가능한 업무 흐름은 아니다. 그래도 계약 항목이 테스트로 내려가지 않은 것은 사실이라 수용한다. 반영은 테스트 하나다. 수정 제안대로 MySQL fixture로 APPROVED 시도와 별도 REQUESTED 시도를 한 Payment에 직접 심고(T6. 둘째 시도는 JdbcTemplate 행) 앱 서비스 handleMockEvent로 REQUESTED 시도의 승인을 넣어 AlreadyApprovedException, 두 시도 상태 무변경, 이벤트 기록 없음, PaymentApproved 미발행을 확인한다. API 층은 이 예외를 매핑하지 않아 500이 되고 계약 2절 표에도 그 행이 없으므로 테스트는 앱 서비스 수준에 둔다. 계약 8-1절 Y3에 셋째 사례로 더한다 |
| A | 04 | S9-R1-A-04 | 보통 | `backend/src/main/java/com/o2o/payment/domain/MockPaymentEventRepository.java:9`; `backend/src/main/java/com/o2o/payment/infrastructure/MockPaymentEventJpaRepository.java:11`; `document/06-2-o2o-aggregates.md:19` | Repository 단위 | Repository 단위, 06-2 1절 애그리거트 목록 | 애그리거트 목록에는 Payment만 결제 루트로 정의돼 있는데 별도 MockPaymentEvent에 domain Repository와 Spring Data Repository가 정의돼 있다. eventId 처리 원장은 필요하지만 현재 구조는 애그리거트 루트가 아닌 기술 기록을 도메인 Repository 단위로 노출한다. | 06-2 1절 결제 행은 Payment를 루트로 두고 PaymentAttempt만 내부 요소로 둔다. 코드에는 PaymentRepository 외에 MockPaymentEventRepository가 추가돼 있다. 작업 계약은 리포지토리 인터페이스 둘을 요구하지만 이 기록을 새 애그리거트 루트나 Repository 예외로 선언하지 않는다. | 기술 멱등 원장 포트를 application 또는 infrastructure 경계로 옮기거나, MockPaymentEvent를 독립 애그리거트 루트로 볼 근거와 경계를 계약에 명시하라. | 거부 | 거부한다. 자리는 계약이 정했다. 계약 8절 4단계(330행)는 payment/domain에 MockPaymentEvent와 리포지토리 인터페이스 둘을 두라고 적고, 6절 이벤트 기록의 저장 범위 행(225행)과 2-1절 이벤트 유일 행(113행)이 그 기록의 역할과 유일성 자리를 정한다. 코드는 그대로다. 수정 제안의 첫째 길(포트를 application이나 infrastructure 경계로)은 backend 층 규칙 3절(infrastructure는 domain이 선언한 인터페이스를 구현한다. 의존은 한 방향)에 어긋나 열려 있지 않다. 06-2 1절의 애그리거트 목록은 2절 커맨드 22개의 루트 목록이고 MockPaymentEvent는 어느 커맨드의 대상도 아닌 재전달 판별 기록이다. 같은 모양이 이미 있다. 예약 묶음의 IdempotencyRecord와 booking/domain/IdempotencyRecordRepository.java도 06-2 목록 밖의 기록에 domain Repository를 두었고(예약 계약 8절 4단계 300행의 리포지토리 인터페이스 둘) 예약 R1 평가 A와 B 어느 쪽도 짚지 않았다. 왜 Payment 밖의 별도 기록인지는 MockPaymentEvent 22행부터 24행과 MockPaymentEventRepository 6행부터 7행의 주석이 적는다. 둘째 길(계약에 근거 명시)은 문서 한 행이라 사용자가 원하면 반영 때 계약 6절에 더할 수 있으나 코드 결함이 아니므로 결정은 거부다 |
| A | 05 | S9-R1-A-05 | 확인필요 | `harness/out/task-S9-payment-R1/eval-request-A.md:89`; `backend/build/test-results/test/` | 테스트 격리와 재현성 | 전체 테스트 기준선, 테스트 격리와 재현성 | 요청문 기준선은 419건인데 직접 실행은 443건이다. 결제 대상 55건은 일치하고 모두 통과했으므로 결제 코드 실패로 판정하지 않았다. 허용 입력 밖의 추가 테스트 파일을 열 수 없어 24건 증가가 평가 브랜치의 정상 갱신인지 작업공간 혼입인지 판단할 수 없다. | 작업공간 검증 10건은 PASS했고 대상 해시는 모두 일치했다. 직접 XML 집계는 50파일 443건이며 생성자 전달 main 사본은 48파일 419건이다. | 현재 평가 브랜치 기준 전체 테스트 수를 요청문에 갱신하거나, 24건을 추가한 테스트 파일 목록과 기준 커밋을 허용 입력으로 제공해 기준선 차이를 닫아라. | 반박 | 반박한다. 24건은 작업공간 혼입이 아니라 main의 정상 갱신이다. 숙소 R1 반영 PR 145가 2026-09-14 21:50에 main에 병합됐고(10dff9a) 테스트 24건을 더했다(harness/out/task-S9-catalog-R1/applied/apply-report.md 6절. 50클래스 443건, 평가 전 419건. backend/README.md 6절 숙소 행). 기록 PR 146(af34431)이 22:02에 뒤따랐고 o2o-dev 작업 트리는 22:03:26에 af34431에서 딴 chore/answer-format-s-grade로 옮겨 갔다(git reflog. 그 브랜치의 유일한 커밋 434ab39는 22:06:44, CLAUDE.md와 답변 양식 둘만 바꿈). A의 Codex 작업은 22:09:42에 열려 22:26:50에 끝났으므로(작업 기록 rollout) 그 트리에서 돌았고 전체 테스트가 443건인 것이 맞다. 1bdadfe와 434ab39 사이의 backend/src/test 변경은 11개 파일이고 결제 패키지는 없다. 결제 대상 59개는 1bdadfe와 434ab39와 현재 origin/main(635071e) 사이에 한 바이트도 다르지 않다(git diff --stat이 비어 있음). 그래서 A가 적은 대상 해시 일치와 verify 10건 PASS(ws.commit은 기준 커밋이 HEAD의 조상인지 본다)가 나왔고 결제 판정은 그대로 선다. 요청문 89행의 419건은 요청문을 만든 2026-09-14 13:53 시점의 main 값이라 틀린 값이 아니고 시점이 다른 것이다. 요청문은 라운드 입력이라 고치지 않고 이 결정표의 평가 절차 기록이 차이를 닫는다. 근거 파일은 harness/out/task-S9-catalog-R1/applied/apply-report.md와 backend/README.md 6절이고, 다음 라운드 요청문은 평가 시점 origin/main의 전체 건수를 적는다 |
| B | 01 | S9-R1-B-01 | 보통 | `backend/src/main/java/com/o2o/payment/infrastructure/InProcessMockPaymentGateway.java:35`, `backend/src/test/java/com/o2o/payment/application/PaymentApplicationServiceTest.java:186` | 요구사항 역추적 | 요구사항 역추적 | 계약은 Mock PG 요청과 환불 모두 attemptId를 멱등키로 사용하고 같은 키의 재요청은 같은 결과라고 확정했지만, 어댑터 자체에 같은 attemptId를 두 번 전달하는 직접 테스트가 없다. 서비스 테스트는 둘째 환불에서 어댑터 호출 자체를 생략하므로 어댑터의 멱등 동작을 검증하지 않는다. | `request`는 `computeIfAbsent`로 같은 거래 번호를 반환하고 `refund`는 Set에 attemptId를 넣는다. Y7 앱 서비스 테스트는 첫 환불 뒤 `hasRefunded`가 참이고 둘째 서비스 호출의 반환 뷰가 같은지만 확인한다. 요청 어댑터의 동일 키 재호출과 환불 어댑터의 동일 키 재호출 결과를 직접 단정하는 테스트는 대상 7개 파일에서 찾지 못했다. | `InProcessMockPaymentGateway`에 같은 attemptId로 `request`를 두 번 호출했을 때 거래 번호가 같고, `refund`를 두 번 호출해도 업무 효과가 하나임을 확인하는 테스트를 추가한다. | 수용 | 사실이다. InProcessMockPaymentGateway의 request(35행)는 computeIfAbsent로 같은 attemptId에 같은 거래 번호를 돌려주고 refund(41행)는 Set에 넣어 둘째 호출이 무해하지만, 이 어댑터를 직접 두 번 부르는 테스트는 대상 7개 파일에 없다. PaymentApplicationServiceTest의 Y7(186행부터)은 둘째 refund에서 앱 서비스가 PG 호출을 건너뛰는 것을 보므로 어댑터 몫은 지나지 않는다. 계약 6절 08-3 결정 4 행(211행)은 Mock PG 어댑터의 refund가 그 키를 받고 같은 키 재요청은 같은 결과라고 적고 2절 openAttempt 표 8행(90행)은 attemptId가 멱등키라고 적으므로 어댑터의 멱등성은 계약 항목이다. 반영은 DB 없는 단위 테스트 하나(InProcessMockPaymentGatewayTest)다. 같은 attemptId로 request 둘은 같은 거래 번호이고 다른 attemptId는 다른 번호이며 형식은 mock_tx_ 뒤 32자, refund 둘 뒤 hasRefunded가 참이고 예외가 없다. 계약 8-1절에 Y26으로 더한다 |
| B | 02 | S9-R1-B-02 | 보통 | `backend/src/main/java/com/o2o/payment/domain/PaymentAttempt.java:41`, `backend/src/test/java/com/o2o/payment/application/PaymentApplicationServiceTest.java:102` | 요구사항 역추적 | 요구사항 역추적 | 계약 2-1절의 U5인 같은 attemptId의 시도 하나를 직접 확인하는 테스트가 없다. U3과 U4는 실제 MySQL 유일성 테스트가 있지만 U5는 `@Id` 선언만 있고 대응 테스트를 짚을 수 없다. | `PaymentAttempt.id`는 JPA 기본키다. `PaymentApplicationServiceTest`의 Y9 두 건은 `booking_id`와 `pg_transaction_id` 유일성만 검증한다. 대상 테스트 7개에서 중복 attemptId 저장 거부를 확인하는 테스트는 찾지 못했다. | 실제 MySQL에서 같은 attemptId를 가진 둘째 시도 저장이 기본키 제약으로 거부되는 테스트를 추가하고 U5에 연결한다. | 수용 | 사실이다. PaymentAttempt 41행의 @Id가 U5의 자리이고 계약 2-1절 U5 행(109행)은 DB 기본키라고 적지만, 8-1절 Y9(356행)는 booking_id와 pg_transaction_id 유니크 둘만 정했고 PaymentApplicationServiceTest의 Y9 둘(102행부터)도 그 둘만 본다. 시도 ID는 attempt_ 뒤 UUID를 도메인이 발급해 서비스 길로는 중복이 생기지 않지만, U5가 계약 항목인데 대응 테스트가 없는 것은 사실이라 수용한다. 반영은 Y9에 사례 하나다. 같은 id의 payment_attempt 둘째 행 저장이 기본키 제약으로 거절되는 것을 실제 MySQL에서 본다(JdbcTemplate로 둘째 행을 넣어 DuplicateKeyException). 계약 8-1절 Y9 문장에 U5를 더한다 |
| B | 03 | S9-R1-B-03 | 확인필요 | `backend/src/main/java/com/o2o/payment/application/PaymentApplicationService.java:92`, `backend/src/main/java/com/o2o/payment/api/MockPaymentEventRequest.java:28` | 추적성 양방향, 요구사항 역추적 | 추적성 양방향, 요구사항 역추적 | 계약 2-1절의 KRW 불변식은 `shared Money`가 지킨다고 적지만 `Money` 구현은 이번 허용 입력에 없다. 대상 코드의 `openAttempt`는 양수만 직접 검사하고, 테스트는 `Money.krw` 성공값만 사용한다. 따라서 비 KRW `Money`가 생성되거나 전달될 수 있는지와 결제 입구에서 거부되는지를 허용 입력만으로 판정할 수 없다. | INTERNAL-01의 currency는 형식 단계에서 KRW로 제한하지 않고 대상 시도 통화와 다르면 409로 처리하도록 계약이 정했으므로 그 부분은 위반이 아니다. 확인이 필요한 부분은 앱 서비스 공개 메서드 `openAttempt`가 의존하는 `Money`의 생성 불변식이다. | 다음 평가 입력에 `Money` 구현과 통화 생성 테스트를 포함하거나, 결제 대상 테스트에 비 KRW 생성 또는 전달이 불가능하다는 직접 증거를 추가한다. | 반박 | 반박한다. KRW 불변식은 shared Money가 지키고 테스트도 있다. backend/src/main/java/com/o2o/shared/Money.java 27행부터의 압축 생성자가 29행부터 31행에서 currency가 KRW가 아니면 IllegalArgumentException을 던지고 32행부터 상한도 본다. 생성 길은 그 생성자 하나(krw 정적 메서드도 같은 생성자를 지난다)라 비 KRW Money는 만들어질 수 없고, openAttempt(PaymentApplicationService 92행부터)가 받는 Money도 예외가 아니다. backend/src/test/java/com/o2o/shared/MoneyTest.java 18행(통화가 KRW가 아니면 거절한다)과 23행(KRW면 만든다)이 그 불변식의 테스트다. B가 판정하지 못한 것은 두 파일이 대상 목록 59개에 없어서다. 재고와 요금 묶음의 파일이라 목록을 만들 때 결제 묶음 밖으로 뒀다. 근거 파일 둘을 다음 라운드 요청문의 허용 입력에 넣는다(계약 5절 평가 대상 행에 적을 것). INTERNAL-01의 currency 처리(형식 단계에서 제한하지 않고 시도와 다르면 409)는 B 자신이 위반이 아니라고 적었고 Y17이 그것을 본다 |

원문 지적과 근거와 수정 제안은 상세 표의 문장을 스크립트로 그대로 옮겼다. 요약하거나 자르지 않았다. 위반 기준 열은 보조 표의 값이다.

## 사용자 추가 지적

| 추가 ID | 심각도 | 대상 위치 | 지적 | 근거 | 처리 |
|---|---|---|---|---|---|

개발 세션이 대조하면서 평가자 둘이 못 본 코드 결함을 더 찾지 못했다. A-01을 대조하며 본 merge 저장의 덮어쓰기 경로는 같은 지적의 다른 결과라 A-01 이유에 적었다. 이 절은 비운다.

## 치명 지적의 오판 판단 기록

치명 하나(A-01)는 수용이다. 오판 정정을 주장하지 않는다. 판단 근거는 A-01 행의 이유다. 리포트가 적은 500 경로에 더해 merge 저장이 먼저 커밋된 기록을 덮는 경로가 있어 규칙 2가 깨지는 것이 맞다.

## 반영 전 G2 확인

- [x] 평가 대상, 계약, A/B 리포트의 버전이 결정표와 일치한다.
- [x] A와 B 원본의 지적 ID 집합과 결정표의 지적 ID 집합이 같다. 누락, 추가, 중복이 없다.
- [x] 원본의 각 행과 결정표를 대조했고, 결정과 이유 외의 정보가 동일하다. 스크립트가 셀을 그대로 옮겼다.
- [x] 전체 행 수가 A와 B 원본 지적 수의 합과 같다. 지적 수가 0이면 리포트가 실제로 정상 완료되었는지 확인했고 파싱 실패나 평가 미완료를 0건으로 취급하지 않았다.
- [x] 모든 행의 결정이 수용, 거부, 반박 중 하나다. 빈 결정이 없다.
- [x] 모든 거부에 이유가 있고, 반박은 원본 심각도가 확인필요인 행에만 있다. 반박 둘은 A-05와 B-03이고 둘 다 확인필요다.
- [x] 치명 오판 정정을 주장한 행마다 근거, 대안, 안티패턴, 사용자 결정, 날짜가 원본 지적에 연결되어 있다. 해당 행 없음.

| 확인 항목 | 기록 |
|---|---|
| 확인자와 날짜 | g2 실행은 2026-09-16 Claude Code(개발 세션). 사람 서명 대기 |
| G2 결과 | PASS. --mode pre 73건 통과(2026-09-16 14:45). version-match는 대상 목록과 계약과 리포트 둘의 네 해시 전부 일치 |
| 미완료 사유 | 사용자 확정 전이라 결정 열은 초안이다. 기계 검사는 위 결과 |

## 반영과 최종 확인 인계

| 확인 대상 | 기록 |
|---|---|
| 반영본 절대경로와 버전 또는 해시 | 반영 후 작성 |
| 수용 항목 반영 확인 | 반영 후 작성. 수용 5건의 반영 위치는 이유 열에 적었다. 반영 순서 제안은 A-01(경합. 코드와 테스트), A-03(승인 이력 방어선 테스트), B-02(U5 기본키 테스트), B-01(어댑터 단위 테스트), A-02(ApplicationReadyEvent 재발행 테스트) |
| 미수용 항목 무변경 확인 | 반영 후 작성. 거부 하나(A-04)가 짚은 MockPaymentEventRepository와 MockPaymentEventJpaRepository의 자리는 바꾸지 않는다. 반박 둘(A-05, B-03)은 코드 변경이 없다 |
| 남은 실제 치명 지적 | 반영 후 작성. 치명 하나(A-01)가 수용이라 반영과 경합 테스트 통과 뒤에 없음으로 적는다 |
| 오판 정정 완료 항목 | 없음 |
| 마지막 반영본에 필요한 검증과 결과 | 반영 후 작성. Docker Desktop을 켠 뒤 전체 테스트(기준선은 origin/main의 454건), g1 code, 계약 fill, 서로 다른 Payment의 같은 eventId 경합 테스트, 승인 이력 방어선 테스트, U5 기본키 테스트, 어댑터 멱등 테스트, ApplicationReadyEvent 재발행 테스트 |
| 코드 작업의 직접 실행 근거 | 반영 후 작성 |
| 평가 대상 소스와 테스트의 무변경 확인 | 평가 전후 대조. 대상 59개의 sha256이 목록 5절의 앞 16자리와 같고 기준 커밋 1bdadfe와 평가 시각의 트리 434ab39와 현재 origin/main 635071e에서 같다(git diff --stat이 비어 있음). verify-eval-workspace.mjs가 o2o-dev와 o2o-payment에서 10건 PASS였고 평가 뒤 git status에 리포트 말고 바뀐 파일이 없다 |
| 검증 미완료 사항 | 사용자 확정. A-01의 경합 재현은 이 초안에서 하지 않았다(Docker Desktop 엔진이 꺼져 있었고 정적 대조로 판단했다). 반영 라운드의 경합 테스트가 그것을 한다 |
| 사용자 최종 완료 판단과 날짜 | 미완료. 초안이고 반영 전이다 |
| 실제 판단 및 확인에 사용한 시간 | 초안 작성 약 55분(리포트 읽기와 검사 넷과 Codex 작업 기록 대조 약 20분, 지적 8건 코드 대조 약 25분, 표 생성과 g2 약 10분). 사용자 판단 시간은 미측정 |
