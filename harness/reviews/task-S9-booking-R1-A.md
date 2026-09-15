# task-S9-booking과 task-S9-booking-lifecycle R1 평가자 A 리포트

## 판정 요약

치명 0 / 보통 3 / 확인필요 1

평가자 A 담당 축만 판정했다. 요구사항 역추적표와 평가자 B의 공통 축은 판정하지 않았다.

## 상세

| 심각도 | 위치(파일:행) | 위반 축 | 문제 | 근거(코드 또는 실행 결과) | 수정 제안 |
|---|---|---|---|---|---|
| 보통 | backend/src/main/java/com/o2o/booking/application/IdempotentRequestExecutor.java:52 | 멱등 규칙, 계약 하강 | 기존 기록이 처리 중이면 body 비교 전에 곧바로 `REQUEST_IN_PROGRESS`를 반환한다. 같은 범위와 키에 다른 body가 들어와도 `IDEMPOTENCY_KEY_REUSED`가 아니라 `REQUEST_IN_PROGRESS`가 된다. | 11 공통 멱등 규칙 2는 같은 범위와 키의 다른 body를 409 `IDEMPOTENCY_KEY_REUSED`로 정한다. 실행기는 52행부터 56행에서 처리 중 여부를 먼저 판정하고 body 비교는 57행부터 수행한다. K17, L14, L20의 처리 중 테스트는 모두 같은 body만 재전송하고, 완료 기록의 다른 body는 K16만 확인한다. | 기존 기록을 받으면 완료 여부와 무관하게 body 해시를 먼저 비교한다. 처리 중 같은 body만 `REQUEST_IN_PROGRESS`로 보낸다. 세 멱등 API에 처리 중 다른 body 반례를 추가한다. |
| 보통 | backend/src/main/java/com/o2o/booking/api/BookingController.java:192 | API 계약 준수, 계약 하강 | BOOK-02가 명세에 없는 query parameter를 거절하지 않는다. `status`, `page`, `size`만 인자로 선언했지만 Spring MVC는 기본적으로 다른 query parameter를 무시하므로 `guestId`, `hostId`, 임의 필드를 붙인 요청이 정상 조회된다. 소유권은 서버 행위자로 유지되지만 미지원 필드를 400으로 거절한다는 API 계약은 지켜지지 않는다. | 11 공통 헤더 뒤 규칙은 API에 표시하지 않은 Query 필드를 지원하지 않는다고 정하고, BOOK-02 처리 규칙은 `guestId`와 `hostId` query를 받지 않는다고 정한다. BookingQueryApiTest의 K24와 K25에는 미정의 query 반례가 없다. | 허용 query 이름을 명시적으로 검사해 나머지를 400 `INVALID_REQUEST`로 거절하고 `guestId`, `hostId`, 임의 query의 API 테스트를 추가한다. |
| 보통 | backend/src/main/java/com/o2o/booking/api/PageResponse.java:9 | 경계 위반 | 공통 Page 응답 모델의 두 번째 사본을 booking api에 다시 정의했다. 파일 자체도 catalog 사본과 같은 모양이며 두 번째 사본이라 shared 후보라고 적고 있다. | backend 층 규칙 3-2는 두 번째 컨텍스트가 쓰기 시작하면 shared로 올리도록 정한다. 현재 `PageResult`는 shared에 있지만 같은 공통 응답 모델은 컨텍스트별로 중복됐다. | 공통 Page 응답 모델을 shared의 API 중립 위치로 올리고 catalog와 booking이 함께 사용하게 한다. 컨텍스트별 응답 타입을 유지하려면 층 규칙의 예외 근거를 계약에 명시한다. |
| 확인필요 | 직접 실행 `gradlew test` | 테스트 격리와 재현성 | 현재 작업공간에서 419건의 독립 재실행 성공을 확인하지 못했다. Docker Desktop 엔진이 없어 DB 컨테이너 기동이 실패했고, 전체 테스트는 419건 중 296건이 Spring 컨텍스트의 DB 연결 단계에서 실패했다. | 직접 실행은 `419 tests completed, 296 failed`와 `BUILD FAILED`였다. 반면 생성자 전달 `step9/test-summary.txt`와 XML은 419건, 실패 0, 오류 0이고 대상 테스트 22파일의 G1은 모두 PASS다. 전달 결과와 현재 직접 실행 결과가 다르므로 현재 환경에서 기능 실패인지 재현할 수 없다. | Docker Desktop과 `o2o_catalog_test`를 정상 기동한 뒤 같은 승인 명령을 다시 실행하고, 새 XML에서 419건, 실패 0, 오류 0을 확인한다. |

## 실행 기록

| 명령 | 목적 | 종료 상태 | 결과 요약 | 직접 실행 여부 |
|---|---|---|---|---|
| `git branch --show-current; git rev-parse HEAD` | 작업공간 기준 확인 | 성공 | 브랜치명 출력 없음, detached HEAD. HEAD `ed40c3281671a43cf4f6a1f4a16007f65706caf3` | 직접 실행 |
| `node harness/out/task-S9-booking-R1/verify-eval-workspace.mjs` | 평가 작업공간 10항 검증 | 첫 실행 환경 실패, 동일 명령 재실행 성공 | 첫 실행은 내부 git `EPERM`. 샌드박스 밖 동일 명령은 `ws.git`, `ws.commit`, `ws.agents`, `ws.list`, `ws.target-count`, `ws.target-exists`, `ws.target-hash`, `ws.target-drift`, `ws.clean`, `ws.doc-hash` 10건 PASS. 금지 파일 존재 알림만 1건 | 직접 실행 |
| `docker compose -f backend/docker-compose.yml up -d` | 테스트 DB 기동 | 실패 | Docker Desktop Linux 엔진 pipe가 없어 `mysql:9.7.2` 컨테이너를 기동하지 못함 | 직접 실행 |
| `$env:JAVA_HOME='C:/Users/user/.jdks/ms-21.0.11'; .\backend\gradlew.bat -p backend test` | 전체 테스트 | 첫 실행 환경 실패, 동일 명령 재실행 실패 | 첫 실행은 Gradle 캐시 잠금 파일 접근 거부. 샌드박스 밖 동일 명령은 419건 실행, 296건 DB 연결 단계 실패, BUILD FAILED | 직접 실행 |
| `node harness/tools/check.mjs g1 <소스> --type code --artifact <step9 JUnit XML>` 22회 | 대상 테스트 소스와 기준 커밋 XML 대조 | 전부 성공 | 대상 테스트 22파일, 합계 155건, 실패 0, 오류 0, 건너뜀 0. 각 파일의 `code.tests-run`과 `code.tests-passed` 포함 6항 PASS | 직접 실행. artifact는 생성자 전달 |
| 생성자 전달 `harness/out/task-S9-booking-lifecycle-R1/step9/test-summary.txt`와 XML 48개 | 기준 커밋 전체 실행 결과 확인 | 전달 결과 성공 | 419건, 실패 0, 오류 0, 건너뜀 0. `--rerun` 실행 사본이라고 기록됨 | 생성자 전달, 직접 실행 아님 |
| 생성자 전달 step6과 step6-2의 `http-calls.txt` | 실제 서버 API 응답 확인 | 전달 결과 성공 | BOOK-01, BOOK-02, BOOK-03, PAY-01, PAY-02, BOOK-04와 주요 오류, T22, T29 응답 기록 확인 | 생성자 전달, 직접 실행 아님 |

## 검증 ID 결과

아래 통과는 기준 커밋의 생성자 전달 XML과 HTTP 기록, 현재 코드 정적 대조를 근거로 한다. 현재 작업공간의 전체 테스트 직접 재실행은 DB 환경 실패로 성공을 확인하지 못했다.

| ID | 통과 / 실패 / 미실행 | 근거 |
|---|---|---|
| T01 | 통과 | BOOK-01 생성과 응답 구간의 BookingApiTest, 전달 XML PASS. 결제 응답 구간의 BookingPaymentApiTest, BookingCancelApiTest, 전달 XML PASS |
| T02 | 통과 | BookingQueryApiTest의 소유권 404, BookingPaymentApiTest와 BookingCancelApiTest의 남의 예약 404, 전달 XML PASS |
| T04 | 통과 | BookingApiTest K22와 1차 실제 HTTP의 `INVENTORY_BELOW_COMMITTED`, heldCount 유지 확인 |
| T06 | 통과 | BookingApiTest K23 체크아웃 제외, 전달 XML PASS |
| T07 | 통과 | BookingApplicationServiceTest K8과 BookingApiTest K23의 재고와 요금 누락 구분, 전달 XML PASS |
| T08 | 통과 | BookingApiTest K19 동시 예약, 전달 XML PASS |
| T09 | 통과 | BookingApplicationServiceTest K7 연박 원자성, 전달 XML PASS |
| T10 | 통과 | BookingApiTest K15 재전송, 전달 XML PASS |
| T11 | 통과 | BookingApiTest K16 완료 성공 키의 다른 body, 전달 XML PASS. 처리 중 다른 body는 S9-R1-A-01로 별도 지적 |
| T12 | 통과 | BookingApplicationServiceTest K10과 PricingPriceQuoteAdapterTest L5, 전달 XML PASS |
| T13 | 통과 | BookingApplicationServiceTest K11, PricingPriceQuoteAdapterTest L5, BookingPaymentApiTest L16, 전달 XML PASS |
| T14 | 통과 | BookingPaymentApiTest L14, 전달 XML PASS |
| T15 | 통과 | BookingPaymentServiceTest L6과 BookingPaymentApiTest L15, 전달 XML PASS |
| T16 | 통과 | PaymentOutcomeServiceTest L8과 BookingPaymentApiTest L17, 전달 XML PASS |
| T17 | 통과 | PaymentOutcomeServiceTest L8과 BookingPaymentApiTest L17, 전달 XML PASS |
| T18 | 통과 | PaymentOutcomeServiceTest L7의 expiresAt 동시각, 전달 XML PASS |
| T19 | 통과 | BookingLockContentionTest L23 2건, 전달 XML PASS |
| T20 | 통과 | PaymentOutcomeServiceTest L7 지연 승인 환불, 전달 XML PASS |
| T22 | 통과 | BookingCancelApiTest L22와 2차 실제 HTTP의 DUPLICATE 및 취소 유지, 전달 XML PASS |
| T23 | 통과 | ApprovalLossRecoveryTest L13의 부분 결과 롤백과 T1 치유. 08-3 결정에 따른 재전달 DUPLICATE 해석 적용. 전달 XML PASS |
| T24 | 통과 | CancelBookingTest L10과 BookingCancelApiTest L19, 전달 XML PASS |
| T25 | 통과 | CancelBookingTest L10과 BookingCancelApiTest L20, 전달 XML PASS |
| T29 | 통과 | BookingCancelApiTest L22와 2차 실제 HTTP의 최초 HELD 응답 및 별도 GET EXPIRED, 전달 XML PASS |

## 보조 표

| 원본 번호 | 지적 ID | 심각도 | 위반 기준 |
|---|---|---|---|
| 01 | S9-R1-A-01 | 보통 | 11 공통 멱등 규칙 2와 4, 평가 기준의 멱등 규칙과 계약 하강 |
| 02 | S9-R1-A-02 | 보통 | 11 공통의 미표시 Query 금지와 BOOK-02 처리 규칙, 평가 기준의 API 계약 준수와 계약 하강 |
| 03 | S9-R1-A-03 | 보통 | backend 층 규칙 3-2 shared 기준, 평가 기준의 경계 위반 |
| 04 | S9-R1-A-04 | 확인필요 | 평가 기준의 테스트 격리와 재현성, 직접 실행과 전달 결과 구분 |

보조 표 합계는 치명 0 / 보통 3 / 확인필요 1로 판정 요약과 같다.

## 담당 축 자체 점검

| 항목 | 결과 |
|---|---|
| 평가자 A 구조 축 | 경계 위반, Repository 단위, 계약 하강, 레이어 역전을 확인했다. Repository 단위와 레이어 역전은 별도 지적 없음 |
| 평가자 A 코드 축 | API 계약, 멱등, 소유권과 접근, 동시성과 트랜잭션, 테스트 격리와 재현성을 확인했다 |
| 평가자 B 축 | 요구사항 역추적표를 만들지 않았고 추적성 양방향, 결정 근거의 자립성, 요구사항 역추적 판정을 대신하지 않았다 |
| 금지 입력 | 기존 리포트, 같은 쌍의 B 리포트, 사용자 결정표 중 허용되지 않은 파일, `step9-verification.md`, `harness/state/`, `harness/docs/`를 읽지 않았다 |
| 파일 변경 | 이 리포트 외 소스, 테스트, 설정 파일을 만들거나 고치지 않았다. 승인된 테스트가 만든 build 산출물만 변경됐다 |

## 읽은 파일과 SHA256

작업공간 검증기의 `ws.target-hash`, `ws.target-drift`, `ws.doc-hash`가 PASS했다. 대상 코드와 테스트의 전체 해시는 `eval-target-files-booking.md`의 각 행과 일치한다. 아래는 판정에 직접 인용한 입력과 핵심 코드 및 테스트의 실제 SHA256이다.

| 파일 | SHA256 |
|---|---|
| harness/out/task-S9-booking-R1/eval-request-A.md | `13c4460379f85759ef4c902374adeb2de509ef8aa76b5cfb9d73ea52fc9f0179` |
| harness/out/mvp-eval-2026-09-14/eval-target-files-booking.md | `79859e3f5bb5bddd15d85caea04901282c3270fc3de8eabf87fb22daaeb78f67` |
| harness/project-sync/o2o-review-input-pack.md | `1608942c0815f9113ef022e74131743bdca3cb30cfaebb277cb8e038cf0dc645` |
| harness/prompts/eval-criteria-code.md | `c5ed835951fe09a5c015d50abbbacf9755af6ba8be807e1be1b9c0ba2cb1553d` |
| harness/tasks/task-S9-booking.md | `0876574192269065f65e72be10239bb577f67ccdb918e74cea9898b8a7652165` |
| harness/tasks/task-S9-booking-lifecycle.md | `af5cba4e4077a2f0b1075aca765aa274d679daf516f48bb961622ba435be7caa` |
| document/11-o2o-api-spec.md | `3f2613a77b6499039b6d8b2310504d7c37047005ae028f6568455472c1ea105b` |
| document/06-2-o2o-aggregates.md | `113c6734b5525e1d5a5b2116de1250232707bc0518b3470d133ec0b7c993cec9` |
| document/06-4-o2o-contracts.md | `edacbe47d6d63e3f2b72849234a0a9dbd4dbd297a8eee7b1ea19d70ab5e3c3e3` |
| document/06-1-o2o-context-map.md | `95bc2b1079d3b739bae022e85c5a468e4dbdc36e5b3b27a8b92702afce664b8a` |
| harness/decisions/decisions-08-3.md | `1b8580aa86c18fd880ab14593bebedd0d83559a9a163981c5abbb6286c8c6ae9` |
| backend/.claude/rules/layers.md | `cb9e4f68bdc4c43c3ab6080cbcb808c485532d147fd6d46e3e4c5ebcda7c2a7c` |
| backend/src/main/resources/application.properties | `a53c90a2ad96f4930db7d9fe2ddb5f1572c9b2128932605621b21a1f290ea8ee` |
| backend/src/test/resources/application.properties | `7a925f9016cc66669ef9d3f29f395fb08a3efc85801ed5288e6f0b8a8d2e5830` |
| backend/src/main/java/com/o2o/booking/api/BookingController.java | `33de44b118ea1e93f251b8a4188b8d295ca523fa805e44083500ed7df22d3a94` |
| backend/src/main/java/com/o2o/booking/api/PageResponse.java | `9f6bc7d9706d44b2a3405f1f69ca29b33ff04b1804d28504a57fcff518251481` |
| backend/src/main/java/com/o2o/booking/application/IdempotentRequestExecutor.java | `1a2c6324b2461d2fc3c2bf4351bc6dd7523441135e0d09b5f8e2b90252bc0f8d` |
| backend/src/main/java/com/o2o/booking/application/IdempotencyRecordService.java | `bf18fd1c7fce7ca3ff71faafd2ea639f20858e7e49ecec5b663a8b0cdf84f5b8` |
| backend/src/main/java/com/o2o/booking/domain/IdempotencyRecord.java | `70fc5c4270cd2cb23cf027740c0f2778c6a580eb6d5b61475be4eadf918647a4` |
| backend/src/test/java/com/o2o/booking/api/BookingApiTest.java | `bdb1cf2a930d132806fbde5756b0ffcd140924aec7695aed5c2e000819891e21` |
| backend/src/test/java/com/o2o/booking/api/BookingPaymentApiTest.java | `699cb424b52918c9bb11ad8da64b78036e8781df028da2be2e0b916fc58a1ab3` |
| backend/src/test/java/com/o2o/booking/api/BookingCancelApiTest.java | `5fd4c92e2792d3dce14dc6fabf8dec1bd6a6a1280844ed22c6ac817563bd044e` |
| backend/src/test/java/com/o2o/booking/api/BookingQueryApiTest.java | `09921de0811c7f5dcee3981058bd020cb69b2d9d2f602842f9cb8bc7467bd3e1` |
| harness/out/task-S9-booking-lifecycle-R1/step9/test-summary.txt | `70607c73cd1a146e231325b9559aeed610e3c4cbc5984a6bc3ee9748d2ffa98c` |

G1이 읽은 나머지 대상 테스트 18파일과 정적 대조에서 읽은 나머지 대상 코드는 `eval-target-files-booking.md`의 경로와 SHA256 앞 16자리로 확인했고, 작업공간 검증기의 전체 대상 해시와 drift 검사를 통과했다.
