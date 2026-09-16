# task-S9-payment R1 반영 기록

양식: harness/prompts/apply.md v3 (2026-09-08)
최초 작성: 2026-09-16
최종 갱신: 2026-09-16

왜 이 파일이 필요한가: 반영본만 내면 어느 지적이 어디로 갔는지 사람이 다시 대조해야 한다. 지적 ID와 변경 위치를 짝지어 두면 최종 확인에서 이 표만 보면 된다. 코드 반영이라 문서 반영과 달리 diff와 테스트 숫자와 변이 검사가 근거다. 라운드 mvp-eval-2026-09-14 쌍 5의 반영이고 앞선 같은 모양은 harness/out/task-S9-catalog-R1/applied/apply-report.md와 harness/out/task-S9-booking-R1/applied/apply-report.md다.

## 1. 이번 반영

| 항목 | 내용 |
|---|---|
| Task ID와 Step | task-S9-payment, Step 9 |
| 작업 유형 | 코드 |
| 평가 라운드 | R1. 라운드 mvp-eval-2026-09-14 쌍 5 |
| 평가받은 대상 버전 | 기준 커밋 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1(PR 138 병합, main). 파일 목록 harness/out/task-S9-payment-R1/eval-target-files.md sha256:4afdd900b3dc8f02(5절 main 기준 목록 59개). 그 59개는 현재 origin/main(635071e)에서도 같다 |
| 반영 범위 | 결정표의 수용 5건. 코드 조치는 1건(A-01)이고 나머지 넷(A-02, A-03, B-01, B-02)은 테스트만이다. 거부 1건(A-04)과 반박 2건(A-05, B-03)은 코드 변경 없음이고 B-03의 근거 파일 둘은 계약 5절에 R2 행으로 열었다 |
| 수정 후보 위치 | 브랜치 fix/task-s9-payment-r1-apply. 결정표 브랜치 끝 13ff6c0(결정 행 커밋)에서 땄다. 코드와 테스트 변경은 7ab7b02부터 107eae6까지 다섯 커밋이고 이유 하나에 커밋 하나다 |
| 결과 기록 경로 | harness/out/task-S9-payment-R1/applied/apply-report.md. diff 전문은 같은 폴더 apply.diff, 테스트 결과 사본은 junit/ 아래 51개 |
| 사용자 결정 | 결정 8건 초안대로 확정. join5201, 2026-09-16. 발언은 초안대로 확정해라. 반영 진행해라 |
| 작업 공간 | 워크트리 C:/Dev/potenup/99_projects/o2o-payment. 테스트 DB는 컨테이너 o2o-catalog-mysql의 o2o_fix_test다(숙소와 예약 R1 반영과 같은 자리). 평가자 세션에 배정된 DB 넷은 건드리지 않았다 |
| 작업 경과 | 같은 대화의 두 실행분에 걸쳤다. 14:50의 반영 지시를 받은 실행분이 15:03에 A-01 코드 커밋(7ab7b02)과 Y24와 Y26 초안을 남기고 15:06에 멈췄고, 15:07에 다시 열린 실행분이 그 초안을 검사한 뒤 이어받아 16:55부터 나머지를 했다. 다른 세션은 없었다 |

## 입력

| 자료 | 경로 | 버전 또는 해시 |
|---|---|---|
| 승인된 Task 계약과 실행 결정 | harness/tasks/task-S9-payment.md | 반영 전 sha256:71a624284e313402, 개정 6 뒤 sha256:5ebdede6f8b5c8a8 |
| 01 전체 | 넣지 않는다(N3). 입력 팩만 | harness/project-sync/o2o-review-input-pack.md sha256:1608942c0815f911 |
| 적용 양식과 코드 규칙 | harness/prompts/apply.md, backend/.claude/rules/testing.md, backend/.claude/rules/layers.md | sha256:011cba475d127c70, sha256:0639a76427f449a3, sha256:cb9e4f68bdc4c43c |
| API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 |
| 평가받은 대상과 필요한 의존 파일 | harness/out/task-S9-payment-R1/eval-target-files.md 5절의 59개 | sha256:4afdd900b3dc8f02 |
| 평가 A 원문 | harness/reviews/task-S9-payment-R1-A.md | sha256:8c70c7fe3c2674de |
| 평가 B 원문 | harness/reviews/task-S9-payment-R1-B.md | sha256:16a5e1e58177fe4a |
| 사용자 결정표 | harness/decisions/task-S9-payment-R1.md | 반영 전 sha256:7835f60aae336194. 인계 표를 채운 뒤 해시는 결정표 자신의 커밋에 |
| 오판 기록 | 없음. 치명 1건(A-01)은 수용이라 오판 판단 기록이 없다 | 없음 |
| 08-3 결정 | harness/decisions/decisions-08-3.md | sha256:1b8580aa86c18fd8 |

## 2. 반영본

브랜치 시작 13ff6c0에서 코드 마지막 커밋 107eae6까지 backend/ 아래 바뀐 파일은 아래 6개뿐이다. 기준 커밋 1bdadfe와 13ff6c0 사이 backend/ 차이 42개 파일은 숙소 R1 반영(PR 145)과 예약 R1 반영(PR 156)과 프론트 계약 병합이고 결제 대상 59개는 그 안에 없다(git diff --name-only 1bdadfe 13ff6c0 로 결제 패키지와 ActorResolver와 설정 둘을 걸면 0건). 다른 세션의 변경이 섞이지 않았다. diff 전문은 같은 폴더의 apply.diff다.

프로덕션 2개.

| 파일 | 무엇이 바뀌었나 | 해시 |
|---|---|---|
| backend/src/main/java/com/o2o/payment/infrastructure/JpaMockPaymentEventRepository.java | save가 Spring Data의 save(merge)에서 EntityManager의 persist와 flush로. 배정 기본키에 @Version이 없어 merge가 그 키의 행을 다시 읽어 있으면 UPDATE로 덮던 것을 순수 INSERT로 바꾸고 기본키 충돌을 저장 시점에 드러낸다 | sha256:22de67f0d589f09f |
| backend/src/main/java/com/o2o/payment/application/PaymentApplicationService.java | 이벤트 기록 저장 둘을 recordEvent로 모으고 DataIntegrityViolationException을 잡아 MockEventConflictException(409 MOCK_EVENT_CONFLICT)으로. 트랜잭션은 예외로 롤백되어 진 쪽의 시도 전이가 남지 않는다 | sha256:e780cfce78e327b3 |

테스트 4개.

| 파일 | 무엇이 바뀌었나 | 해시 |
|---|---|---|
| backend/src/test/java/com/o2o/payment/api/MockPaymentEventApiTest.java | Y24 하나. 테스트 전용 데코레이터 RacingEventRepository(무장하면 findByEventId가 없음을 돌려준 뒤 문에서 두 요청을 멈춘다)를 @Primary 빈으로 | sha256:07c804c08c95e721 |
| backend/src/test/java/com/o2o/payment/application/PaymentAutoResultTest.java | Y25 하나. 떠 있는 컨텍스트에 ApplicationReadyEvent를 다시 낸다 | sha256:5e9d9884bf9ce48b |
| backend/src/test/java/com/o2o/payment/application/PaymentApplicationServiceTest.java | Y3 셋째 사례와 Y9 U5 사례 둘. 기존 시도 행을 본떠 같은 Payment에 시도 행을 직접 넣는 도우미(JdbcTemplate) 하나. CommittedPaymentEvents 빈 추가 | sha256:c26c6964cf7c6abc |
| backend/src/test/java/com/o2o/payment/infrastructure/InProcessMockPaymentGatewayTest.java | 신설. Y26 둘 | sha256:99e541419999d843 |

문서 쪽 반영은 계약이다.

| 파일 | 무엇이 바뀌었나 | 해시 |
|---|---|---|
| harness/tasks/task-S9-payment.md | 개정 6. 5절 R2 허용 입력 둘(Money.java, MoneyTest.java), 8-1의 Y3과 Y9 문장과 Y24부터 Y26 세 행과 마지막 문단, 10절 개정 6 행과 마지막 성공 단계 행과 미해결 행. fill 186건 중 1건 실패이고 그 한 건은 164행 예약 계약 해시가 2026-09-15 예약 개정 4로 낡은 것(반영 전부터, 범위 밖 N7). 새 행은 통과 | sha256:5ebdede6f8b5c8a8 |

## 3. 지적 ID별 변경 위치

| 지적 ID | 심각도 | 결정 | 변경 위치 | 무엇을 넣었나 |
|---|---|---|---|---|
| S9-R1-A-01 | 치명 | 수용 | JpaMockPaymentEventRepository.save의 persist와 flush, PaymentApplicationService.recordEvent의 409 매핑, MockPaymentEventApiTest Y24 | 서로 다른 Payment의 시도 둘에 같은 eventId가 같은 순간 오면 하나는 200 PROCESSED, 다른 하나는 409 MOCK_EVENT_CONFLICT다. 기록은 하나이고 패자의 시도는 REQUESTED 그대로, PaymentApproved는 한 번. 고치기 전에는 패자가 500 INTERNAL_ERROR였다(변이 검사 첫 행) |
| S9-R1-A-02 | 보통 | 수용 | PaymentAutoResultTest Y25 | REQUESTED이고 APPROVE인 시도를 심고 기동 완료 신호를 다시 내면 러너의 구독이 APPROVED로 만들고 auto_ 기록이 PROCESSED 하나, PaymentApproved 한 번. 구독 한 줄을 떼면 이 테스트만 빨갛다(변이 검사 둘째 행) |
| S9-R1-A-03 | 보통 | 수용 | PaymentApplicationServiceTest Y3 셋째 사례 | 승인 시도가 있는 Payment의 다른 REQUESTED 시도(DB에 직접 심음)에 승인이 오면 AlreadyApproved이고 두 시도 상태와 이벤트 기록과 발행이 그대로다. recordApproval의 그 분기를 떼면 PaymentTest 21건은 그대로 초록이고 이 사례만 빨갛다(변이 검사 셋째 행) |
| S9-R1-A-04 | 보통 | 거부 | 코드 변경 없음 | MockPaymentEvent와 domain 리포지토리의 자리는 계약 8절 4단계가 정했다. 결정표 이유 그대로 |
| S9-R1-A-05 | 확인필요 | 반박 | 코드 변경 없음 | 443건은 숙소 R1 반영 PR 145의 24건이 더해진 main의 정상 값이었다. 이번 반영 뒤 전체는 460건(origin/main 454건에 6이 늘었다). 다음 라운드 요청문은 평가 시점 origin/main의 전체 건수를 적는다(계약 10절 미해결 행) |
| S9-R1-B-01 | 보통 | 수용 | InProcessMockPaymentGatewayTest Y26 둘 | 같은 attemptId의 request는 같은 거래 번호(mock_tx_ 뒤 32자), 다른 attemptId는 다른 번호. 같은 attemptId의 refund 둘은 예외 없이 hasRefunded 참. 매 호출 새 번호를 주게 바꾸면 첫 테스트가 빨갛다(변이 검사 넷째 행) |
| S9-R1-B-02 | 보통 | 수용 | PaymentApplicationServiceTest Y9 U5 사례 | 같은 id의 둘째 payment_attempt 행을 기본키가 거부한다(DuplicateKeyException, 메시지에 PRIMARY). 거래 번호는 달리 해 U4가 아니라 기본키가 막는 것을 본다. 다른 id는 들어간다 |
| S9-R1-B-03 | 확인필요 | 반박 | 코드 변경 없음. 계약 5절 R2 행 둘(Money.java, MoneyTest.java) | 반박 근거인 KRW 생성자와 그 테스트를 R2 평가자가 읽을 수 있게 했다 |

## 4. 결정표와 다르게 간 것과 실측

| 항목 | 결정표 | 실제 | 왜 |
|---|---|---|---|
| A-01의 충돌 뒤 처리 | 기본키 충돌로 롤백된 요청은 트랜잭션 밖에서 기록을 다시 읽어 body가 같으면 DUPLICATE, 다르면 409 | 다시 읽지 않고 충돌을 바로 409로 답한다 | 이 저장에서 충돌이 나는 길은 서로 다른 Payment가 같은 eventId를 같은 순간 넣은 것뿐이다. 같은 Payment의 같은 eventId는 루트 잠금이 규칙 2 조회에서 먼저 걸러(Y22) 저장에 닿지 않는다. 서로 다른 Payment면 paymentAttemptId가 달라 body가 언제나 다르므로 다시 읽어도 결과는 409다. 결정표 이유의 마지막 문장(경합의 패자는 409다)과 같은 결과이고 재조회 한 번이 준다 |
| A-01의 저장 방식 | persist 의미로 바꾼다 | EntityManager의 persist 뒤 flush | flush가 없으면 충돌이 커밋 시점에 나서 앱 서비스의 catch 밖으로 빠진다. flush로 저장 시점에 드러내 recordEvent가 잡는다 |
| B-02의 예외 형 | DuplicateKeyException | 그대로 DuplicateKeyException | 2026-09-16 실측. Spring 7.0.9의 JdbcTemplate이 MySQL 1062를 DuplicateKeyException으로 옮긴다. 메시지는 Duplicate entry 뒤 key 'payment_attempt.PRIMARY' |
| Y25의 신호 만들기 | SpringApplication 인스턴스와 이 컨텍스트로 만든 이벤트 | new ApplicationReadyEvent(new SpringApplication(BackendApplication.class), new String[0], context, Duration.ZERO) | 결정표대로다. 생성자 네 인자는 spring-boot 4.1.1에서 확인했다 |

## 5. 미수용 항목 무변경 확인

A-04가 짚은 MockPaymentEvent.java와 MockPaymentEventRepository.java, A-05가 짚은 요청문 eval-request-A.md, B-03이 짚은 PaymentApplicationService.openAttempt 경로와 shared Money.java는 기준 커밋과 같다. git diff --stat 1bdadfe 107eae6 에 그 파일들이 나오지 않는다(PaymentApplicationService.java는 나오지만 그 변경은 A-01의 recordEvent뿐이고 openAttempt는 그대로다. apply.diff로 확인). 요청문은 라운드 입력이라 고치지 않았다.

2절의 6개 밖은 backend/ 아래 어느 파일도 바뀌지 않았다. 동결 대상(backend/build.gradle, backend/CLAUDE.md, harness/prompts, harness/tools, document)은 손대지 않았다.

## 6. 검사

| 명령 | 결과 |
|---|---|
| JAVA_HOME=JDK 21, SPRING_DATASOURCE_URL=o2o_fix_test, ./backend/gradlew.bat -p backend test --rerun-tasks | BUILD SUCCESSFUL(1분 54초, 17:08부터 17:10). 51클래스 460건, 실패 0, 오류 0, 건너뜀 0. origin/main 454건에 6이 늘었다(Y24 하나, Y25 하나, Y26 둘, Y3 셋째 사례 하나, Y9 U5 하나). 평가 시점 443건과의 차이 11은 예약 R1 반영(PR 156)의 몫이다 |
| node harness/tools/check.mjs g1 backend/src/main/java/com/o2o/payment/application/PaymentApplicationService.java --type code --artifact applied/junit/*.xml 51개 | 106건 통과. 테스트 460건 |
| 같은 명령을 JpaMockPaymentEventRepository.java에 | 106건 통과 |
| node harness/tools/check.mjs fill harness/tasks/task-S9-payment.md | 186건 중 1건 실패. 164행 예약 계약 해시가 낡은 것이고 반영 전부터 있던 것. 새 행은 통과. 2절 문서 표 참조 |
| node harness/tools/check.mjs g2 harness/decisions/task-S9-payment-R1.md --mode final | 결정표 인계 표를 채운 뒤 실행. 결과는 결정표 인계 표에 |

테스트 결과 사본은 같은 폴더의 junit/ 아래 51개다. 로그의 HHH000478(drop foreign key 실패)은 JVM 종료 때 캐시된 테스트 컨텍스트 여럿이 같은 스키마를 차례로 지우며 나는 것이고 컨텍스트 하나만 도는 실행에는 없다. 테스트 결과와 무관하다.

새 테스트가 실제로 결함을 잡는지 변이 검사로 확인했다. 고친 코드를 잠깐 되돌리거나 지키는 줄을 떼고 테스트가 빨개지는지 본 것이다. 파일의 존재나 초록만 보지 않는다(루트 CLAUDE.md 5-1절).

| 되돌린 것 | 결과 | 뜻 |
|---|---|---|
| JpaMockPaymentEventRepository.save를 Spring Data의 save로 | Y24 실패. 한쪽 200 PROCESSED, 다른 쪽 500 INTERNAL_ERROR | A의 첫째 결과(기본키 충돌의 500)가 그대로 재현됐다. Y24만 그 결함을 잡는다 |
| MockAutoResultResumeRunner의 @EventListener(ApplicationReadyEvent.class) 한 줄 삭제 | PaymentAutoResultTest 6건 중 Y25만 실패(APPROVED 기대, REQUESTED). Y13 포함 5건 통과 | A-02의 지적 그대로. 그 한 줄은 Y25만 본다 |
| Payment.recordApproval의 다른 시도 승인 이력 검사(AlreadyApproved) 삭제 | PaymentApplicationServiceTest 9건 중 Y3 셋째 사례만 실패(예외 없음). PaymentTest 21건 전부 통과 | A-03의 지적 그대로. 기존 Y3 둘은 openAttempt만 봤다 |
| InProcessMockPaymentGateway.request가 매 호출 새 번호를 주게 | Y26 둘 중 request 테스트 실패(번호 불일치). refund 테스트 통과 | B-01의 멱등성은 Y26 첫 테스트가 잡는다 |

되돌린 코드는 검사 뒤 git checkout으로 복구했고 2절의 해시가 복구된 상태다.

## 7. 남은 문제

| 항목 | 상태 | 왜 |
|---|---|---|
| 병합 | 사용자 몫 | PR을 올리고 멈춘다. 병합은 사용자가 한다 |
| 다음 라운드 요청문 | 이월 | 반박 둘의 근거를 넣는다. B-03은 계약 5절 R2 행 둘, A-05는 평가 시점 origin/main의 전체 건수 |
| 계약 fill의 한 건 | 그대로 | 164행 예약 계약 해시. 2026-09-15 예약 개정 4로 낡았고 이번 반영 범위 밖이다(N7) |
| 이전 실행분의 커밋 서명 | 그대로 | 7ab7b02의 Co-Authored-By가 Claude Opus 4.8이다. 내용은 검사했고 이후 커밋은 Opus 5다. 이력을 다시 쓰지 않는다 |

## 8. 재평가

재평가는 최대 1회이고 아직 쓰지 않았다. 결정표가 R2를 쓰지 않는다고 적었다(여덟 건이 전부 코드로 확인되거나 반박됐다). 사용자가 R2를 원하면 기준 커밋은 107eae6이고 허용 입력은 계약 5절의 R2 행 둘이 늘었다.
