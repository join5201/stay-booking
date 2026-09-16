# task-S9-payment R1 평가자 A 요청문

최초 작성: 2026-09-14
최종 갱신: 2026-09-14

이 파일은 사용자가 Codex의 새 작업에 그대로 붙여 넣는 요청문이다. 라운드 mvp-eval-2026-09-14(harness/out/mvp-eval-2026-09-14/README.md)의 쌍 5이고 근거는 오늘의 전제 결정 3과 10-4 1절의 전달은 사람이 한다는 규칙이다. A와 B는 각각 다른 새 작업에서 돈다. 한 작업에서 둘 다 하지 않는다. 

작업 디렉터리는 C:/Dev/potenup/99_projects/o2o-dev로 연다. 그래야 AGENTS.md와 backend/AGENTS.md가 읽힌다. 브랜치는 eval/mvp-2026-09-14이고 기준 커밋 1bdadfe이 그 조상이다. 상위 폴더 o2o는 세션 여럿이 브랜치를 바꿔 가며 쓰는 곳이라 열지 않는다.

열기 전에 `node harness/out/task-S9-payment-R1/verify-eval-workspace.mjs`를 그 폴더에서 돌려 PASS를 확인한다. FAIL이면 붙여 넣지 않는다. 평가자 B의 요청문은 같은 폴더의 eval-request-B.md다.

리포트는 `harness/reviews/task-S9-payment-R1-A.md`에 저장된다. 그 파일이 생겼는지 붙여 넣은 뒤에 본다.

아래 구분선부터가 붙여 넣을 내용이다.

---

너는 평가자 A다. 설계자도 구현자도 아니다. 이 코드가 만들어진 논의를 보지 못했고 볼 필요도 없다. 주어진 파일만으로 판정한다.

## 1. 대상

| 항목 | 값 |
|---|---|
| Task | task-S9-payment. 결제 컨텍스트 묶음 |
| 라운드 | R1 |
| 브랜치 | eval/mvp-2026-09-14. origin/main에서 딴 평가용 브랜치라 코드는 main과 같다. 이 묶음의 코드는 PR 133(feat/task-s9-payment, 병합 뒤 삭제)으로 들어왔고 그 뒤 예약 2차가 테스트 설정 파일에 스케줄러 줄 하나를 더했다. 코드는 같다 |
| 기준 커밋 | 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1 |

평가 대상 파일 목록은 `harness/out/task-S9-payment-R1/eval-target-files.md`에 있다. 그 파일의 5절 main 기준 목록이 이번 대상이다. 프로덕션 50개(payment 49, shared 1)와 설정 파일 2와 테스트 7파일 55건(지원 파일 1 포함)이다. 1절부터 4절은 브랜치 시점의 기록이다. 목록이 평가 대상이 아니라고 적은 것은 읽지 않는다.

## 2. 읽어도 되는 것

| 자료 | 경로 | 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 코드 목록 | harness/out/task-S9-payment-R1/eval-target-files.md | sha256:4afdd900b3dc8f02 | 전문 |
| 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 | 축, 심각도, 출력 스키마 |
| 작업 계약 | harness/tasks/task-S9-payment.md | 자기 해시 없음 | 전문 |
| 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절, PAY-01과 PAY-02, 내부 처리와 Mock 이벤트 절, 응답 모델, 검증 기준 |
| 06-2 애그리거트 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 1절, 3-1, 3-4, 4절, 5절, 6절 결제, 7절 |
| 06-4 계약 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 0절, 1-1, 1-2 결제, 1-3, 1-4 |
| 06-1 컨텍스트 맵 | document/06-1-o2o-context-map.md | sha256:95bc2b1079d3b739 | 2절 R6과 R7 |
| 08-3 결정 | harness/decisions/decisions-08-3.md | sha256:1b8580aa86c18fd8 | 1, 2, 3, 4, 6, 9, 10, 11과 표 아래의 11 v2와 부딪히는 번호 문장. 코드가 11 명세 대신 이 결정을 따른 자리를 위반으로 적지 않게 한다. harness/decisions/ 아래에서 읽어도 되는 것은 이 파일 하나다 |
| 백엔드 층 규칙 | backend/.claude/rules/layers.md | sha256:cb9e4f68bdc4c43c | 3-2 shared 기준, 3-3 이벤트 규칙 E1과 E2 |
| 전역 역직렬화 설정 | backend/src/main/resources/application.properties | sha256:a53c90a2ad96f493 | 7행 fail-on-unknown-properties와 프로파일 줄과 격리 수준 줄. 목록의 설정 파일 표에도 있다 |
| 생성자가 낸 실행 결과 | harness/out/task-S9-payment-R1/step4, step5, step6, step9와 harness/out/task-S9-booking-lifecycle-R1/step9의 JUnit XML | 없음 | JUnit XML과 http-calls.txt와 요약. 단계 사본은 브랜치 시점 코드의 결과이고 기준 커밋 코드로 돈 결과는 booking-lifecycle-R1/step9의 XML 48개다. step6의 http-calls.txt는 살아 있는 서버에 INTERNAL-01을 친 기록이다 |

기록된 해시와 실제 파일의 해시가 다르면 그 사실을 리포트에 적고 실제 파일로 진행한다.

## 3. 읽지 않는 것

- 01 계획서 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, `harness/docs/` 전체, `harness/state/` 전체, 생성 대화
- `harness/reviews/` 전체
- `harness/out/` 아래에서 1절과 2절이 지정한 파일과 폴더 밖의 전부. 특히 `harness/out/mvp-eval-2026-09-14/README.md`(라운드 계획), 다른 묶음의 요청문, 각 묶음의 `step9-verification.md`
- `harness/decisions/` 아래에서 `decisions-08-3.md` 밖의 전부

step9-verification.md는 생성자가 스스로 매긴 판정이다. 그것을 먼저 보면 블라인드가 아니다.

## 4. 네가 볼 축

### 4-1. 구조 축

| 축 | 판정 질문 |
|---|---|
| 경계 위반 | 금지되어야 할 컨텍스트 간 의존이 규칙으로 막히나 |
| Repository 단위 | 애그리거트 루트가 아닌 것에 Repository가 정의됐나 |
| 계약 하강 | 계약표 항목 중 테스트 케이스로 내려가지 않은 것 |
| 레이어 역전 | domain이 infrastructure를 참조하나 |

### 4-2. 코드 전용 축

| 축 | 판정 질문 | 근거 |
|---|---|---|
| API 계약 준수 | 요청과 응답 필드, 상태 코드, 오류 code가 명세와 같은가. 명세에 없는 필드나 경로를 만들었나. 명세 대신 08-3 결정을 따른 자리는 위반이 아니다 | 11 내부 처리와 Mock 이벤트 절, 08-3 결정 |
| 멱등 규칙 | INTERNAL-01의 같은 거래 재전달과 중복 콜백 처리가 명세와 계약 2절 검사 순서와 같은가 | 11 공통 멱등 처리, 계약 2절 |
| 소유권과 접근 | MOCK_SYSTEM 행위자와 dev 프로파일 밖 비활성이 명세와 계약과 같은가. 행위자가 헤더에서만 오나 | 11 인증과 접근 제어, 계약 7절 D-3 |
| 동시성과 트랜잭션 | Payment 잠금 아래의 I6과 I7과 I9 검사, 격리 수준, 커밋 뒤 이벤트와 자동 결과 어댑터, 재시작 뒤 재개가 설계 계약과 같은가. 락 없는 읽기로 분기하는 곳이 있나 | 06-4 0절, 계약 6절, 08-3 결정 3 |
| 테스트 격리와 재현성 | 시간과 Mock 결과를 제어하나. 데이터를 정리하나. MySQL에서 봐야 할 것(잠금 경합, 유니크, 롤백)을 메모리 저장소로 대신하지 않았나 | 11 검증 기준 |

### 4-3. 검증 ID 판정

계약 9절이 지정한 것은 T21과 T26과 T30 전부, INTERNAL-01 오류 넷(400 INVALID_REQUEST, 404 RESOURCE_NOT_FOUND, 409 MOCK_EVENT_CONFLICT, 409 PAYMENT_AMOUNT_MISMATCH), T15와 T16과 T17과 T20과 T22와 T23의 결제 몫이다(예약 몫은 예약 쌍이 본다). 계약 8-1절의 Y1부터 Y23이 근거 테스트다. 각각에 통과, 실패, 미실행 셋 중 하나를 적는다. 실행하지 않은 것을 통과로 적지 않는다.

## 5. 돌려도 되는 명령

| 명령 | 무엇 |
|---|---|
| `docker compose -f backend/docker-compose.yml up -d` | 테스트 DB 컨테이너. 127.0.0.1:3307 |
| `SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3307/o2o_payment_test JAVA_HOME=C:/Users/user/.jdks/ms-21.0.11 ./backend/gradlew.bat -p backend test` | 테스트 전부. 저장소 루트에서. 419건이 기준선이다 |
| `node harness/tools/check.mjs g1 <소스> --type code --artifact <junit.xml>` | 게이트 |

테스트 DB는 `o2o_payment_test`다. 설정 파일 기본값은 다른 DB라 명령 앞에 환경변수를 붙인다. PowerShell이면 `$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3307/o2o_payment_test'`와 `$env:JAVA_HOME='C:/Users/user/.jdks/ms-21.0.11'`를 먼저 주고 `.\backend\gradlew.bat -p backend test`를 돌린다. 설정 파일은 고치지 않는다.

바꿔도 되는 것은 빌드 산출물과 로그와 테스트 DB `o2o_payment_test` 안의 표뿐이다. 소스와 테스트와 설정은 어떤 파일도 고치지 않는다. 컨테이너를 내리지 않는다. 다른 세션이 같은 컨테이너의 다른 DB를 쓴다.

생성자가 전달한 결과와 네가 직접 돌린 결과를 리포트에서 구분해 적는다. 돌리지 못했으면 돌리지 못했다고 적고 통과로 바꾸지 않는다.

## 6. 출력

`harness/prompts/eval-criteria-code.md` 5절의 스키마를 그대로 쓴다. 판정 요약, 상세, 실행 기록, 검증 ID 결과다. 요구사항 역추적표는 평가자 B의 몫이라 이 리포트에 넣지 않는다.

그 뒤에 보조 표를 붙인다. 열은 원본 번호, 지적 ID, 심각도, 위반 기준 넷이다. 상세 표의 행마다 한 행이고, 판정 요약의 치명과 보통과 확인필요 수가 이 표의 등급별 수와 같아야 한다. 이 표가 없거나 수가 어긋나면 리포트가 잘린 것으로 처리된다.

지적 ID는 `S9-R1-A-{원본 번호 2자리}`로 붙인다. 위치는 파일과 행 번호로 적는다.

리포트에 실제로 읽은 파일과 그 sha256을 남긴다.

리포트를 `harness/reviews/task-S9-payment-R1-A.md`에 저장한다. 이 파일 하나만 만든다.

## 7. 하지 말 것

- 리포트 파일 말고 파일을 만들거나 고치기
- 문체나 표현 지적하기
- 코드에 없는 내용을 추측으로 채우기
- 확인필요 등급을 없애고 통과와 실패 둘로만 판정하기
- 5절에 없는 명령 실행하기

판단이 서지 않으면 확인필요로 남긴다.
