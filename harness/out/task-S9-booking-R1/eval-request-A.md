# task-S9-booking과 task-S9-booking-lifecycle R1 평가자 A 요청문

최초 작성: 2026-09-14
최종 갱신: 2026-09-14

이 파일은 사용자가 Codex의 새 작업에 그대로 붙여 넣는 요청문이다. 라운드 mvp-eval-2026-09-14(harness/out/mvp-eval-2026-09-14/README.md)의 쌍 4이고 근거는 오늘의 전제 결정 3과 10-4 1절의 전달은 사람이 한다는 규칙이다. A와 B는 각각 다른 새 작업에서 돈다. 한 작업에서 둘 다 하지 않는다. 라운드 README 5절 D-4 가에 따라 1차와 2차를 한 쌍이 본다. 결정표는 harness/decisions/task-S9-booking-R1.md 하나이고 계약 둘과 해시 둘을 적는다. 이 쌍의 A는 숙소 쌍의 A와 같은 테스트 DB(o2o_catalog_test)를 쓰므로 숙소 A가 끝난 뒤에 연다.

작업 디렉터리는 C:/Dev/potenup/99_projects/o2o-dev로 연다. 그래야 AGENTS.md와 backend/AGENTS.md가 읽힌다. 브랜치는 eval/mvp-2026-09-14이고 기준 커밋 1bdadfe이 그 조상이다. 상위 폴더 o2o는 세션 여럿이 브랜치를 바꿔 가며 쓰는 곳이라 열지 않는다.

열기 전에 `node harness/out/task-S9-booking-R1/verify-eval-workspace.mjs`를 그 폴더에서 돌려 PASS를 확인한다. FAIL이면 붙여 넣지 않는다. 평가자 B의 요청문은 같은 폴더의 eval-request-B.md다.

리포트는 `harness/reviews/task-S9-booking-R1-A.md`에 저장된다. 그 파일이 생겼는지 붙여 넣은 뒤에 본다.

아래 구분선부터가 붙여 넣을 내용이다.

---

너는 평가자 A다. 설계자도 구현자도 아니다. 이 코드가 만들어진 논의를 보지 못했고 볼 필요도 없다. 주어진 파일만으로 판정한다.

## 1. 대상

| 항목 | 값 |
|---|---|
| Task | task-S9-booking과 task-S9-booking-lifecycle. 예약과 선점 묶음. 1차(선점과 조회)와 2차(결제 중계와 확정과 만료와 취소)를 한 쌍이 본다 |
| 라운드 | R1 |
| 브랜치 | eval/mvp-2026-09-14. origin/main에서 딴 평가용 브랜치라 코드는 main과 같다. 1차는 PR 127(feat/task-s9-booking), 2차는 PR 138(feat/task-s9-booking-lifecycle)로 들어왔고 둘 다 병합 뒤 삭제됐다. 2차가 1차 파일 11개를 고쳤다 |
| 기준 커밋 | 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1 |

평가 대상 파일 목록은 `harness/out/mvp-eval-2026-09-14/eval-target-files-booking.md`에 있다. 프로덕션 87개(booking 74, inventory 12, shared 1)와 테스트 29파일 155건(지원 파일 7 포함)과 설정 파일 1이다. 두 계약이 각각 가리키는 옛 목록 둘은 브랜치 시점 기록이고 이번 대상은 이 합본이다. 합본이 평가 대상이 아니라고 적은 것은 읽지 않는다.

## 2. 읽어도 되는 것

| 자료 | 경로 | 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 코드 목록 | harness/out/mvp-eval-2026-09-14/eval-target-files-booking.md | sha256:79859e3f5bb5bddd | 전문 |
| 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 | 축, 심각도, 출력 스키마 |
| 작업 계약 1차 | harness/tasks/task-S9-booking.md | sha256:0876574192269065 | 전문. 최종 완료된 계약이라 이번 라운드에서 고치지 않았다. 5절의 application.properties 해시는 결제 묶음이 그 파일을 고치기 전 값이라 아래 행의 값이 지금 값이다 |
| 작업 계약 2차 | harness/tasks/task-S9-booking-lifecycle.md | 자기 해시 없음 | 전문. 5절 라운드 합본 행이 이 쌍의 대상이다 |
| 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절, 예약과 결제 절의 BOOK-01부터 BOOK-04와 PAY-01과 PAY-02, 내부 처리와 Mock 이벤트 절, 응답 모델, 검증 기준, 정책 P01과 P05 |
| 06-2 애그리거트 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 1절, 3-1, 3-4, 4절, 5절, 6절 예약, 7절 |
| 06-4 계약 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 0절, 1-2 재고와 예약과 결제, 1-3, 1-4, 2-1, 2-2, 2-3 |
| 06-1 컨텍스트 맵 | document/06-1-o2o-context-map.md | sha256:95bc2b1079d3b739 | 2절 R5와 R6 |
| 08-3 결정 | harness/decisions/decisions-08-3.md | sha256:1b8580aa86c18fd8 | 3, 5, 6, 7, 8, 10, 11과 표 아래의 11 v2와 부딪히는 번호 문장. 코드가 11 명세 대신 이 결정을 따른 자리를 위반으로 적지 않게 한다. harness/decisions/ 아래에서 읽어도 되는 것은 이 파일 하나다 |
| 전역 역직렬화 설정 | backend/src/main/resources/application.properties | sha256:a53c90a2ad96f493 | 7행 fail-on-unknown-properties |
| 백엔드 층 규칙 | backend/.claude/rules/layers.md | sha256:cb9e4f68bdc4c43c | 3-2 shared 기준, 3-3 이벤트 규칙 E1과 E2 |
| 생성자가 낸 실행 결과 | harness/out/task-S9-booking-R1/step5, step6, step6-2와 harness/out/task-S9-booking-lifecycle-R1/step4-1부터 step4-4, step5, step6, step6-2, step9 | 없음 | JUnit XML과 http-calls.txt와 요약. 1차 단계 사본은 JUnit XML과 http-calls.txt다. 2차 단계 사본은 test-summary.txt 요약과 http-calls.txt와 bootrun-excerpt.txt이고 XML은 step9에만 있다. 기준 커밋 코드로 돈 결과는 step9의 XML 48개(419건)다 |

기록된 해시와 실제 파일의 해시가 다르면 그 사실을 리포트에 적고 실제 파일로 진행한다.

## 3. 읽지 않는 것

- 01 계획서 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, `harness/docs/` 전체, `harness/state/` 전체, 생성 대화
- `harness/reviews/` 전체
- `harness/out/` 아래에서 1절과 2절이 지정한 파일과 폴더 밖의 전부. 특히 `harness/out/mvp-eval-2026-09-14/README.md`(라운드 계획), 다른 묶음의 요청문, 각 묶음의 `step9-verification.md`
- `harness/out/task-S9-booking-R1/step9-verification.md`와 `harness/out/task-S9-booking-lifecycle-R1/step9-verification.md` 둘
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
| API 계약 준수 | 요청과 응답 필드, 상태 코드, 오류 code가 명세와 같은가. 명세에 없는 필드나 경로를 만들었나. 명세 대신 08-3 결정을 따른 자리는 위반이 아니다 | 11 예약과 결제 절, 08-3 결정 |
| 멱등 규칙 | Idempotency-Key 범위, body 비교, 재전송 응답, REQUEST_IN_PROGRESS 처리가 명세 멱등 절 1부터 9항과 같은가. BOOK-01, PAY-01, BOOK-04 셋 다 본다 | 11 공통 멱등 처리 |
| 소유권과 접근 | 행위자가 헤더에서만 오나. body의 guestId로 권한을 정하지 않나. 남의 예약이 404인가 | 11 인증과 접근 제어 |
| 동시성과 트랜잭션 | 잠금 순서(Booking, Payment, 재고 N행 날짜 오름차순), 잠금 후 상태 재검사, 전체 성공 또는 전체 실패, 만료와 승인의 경합, 커밋 뒤 이벤트가 설계 계약과 같은가. 락 없는 읽기로 분기하는 곳이 있나 | 06-4 0절과 2절, 08-3 결정 3 |
| 테스트 격리와 재현성 | 시간과 Mock 결제 결과를 제어하나. 데이터를 정리하나. MySQL에서 봐야 할 것(잠금 경합, 유니크, 롤백)을 메모리 저장소로 대신하지 않았나. 스케줄러를 테스트에서 어떻게 다루나 | 11 검증 기준 |

### 4-3. 검증 ID 판정

두 계약의 9절을 합치면 T06부터 T20과 T22부터 T25와 T29 전부, T12와 T13의 예약 몫(요금 절반은 1차, 프로모션 절반은 2차), T01과 T02의 예약 조회 구간과 결제 구간, T04의 heldCount 몫이다. T21과 T26과 T30은 결제 쌍, T27과 T28은 프로모션 쌍이 본다. 근거 테스트는 1차 8-1절 K1부터 K25와 2차 8-1절 L1부터 L23이다. 각각에 통과, 실패, 미실행 셋 중 하나를 적는다. 실행하지 않은 것을 통과로 적지 않는다.

## 5. 돌려도 되는 명령

| 명령 | 무엇 |
|---|---|
| `docker compose -f backend/docker-compose.yml up -d` | 테스트 DB 컨테이너. 127.0.0.1:3307 |
| `JAVA_HOME=C:/Users/user/.jdks/ms-21.0.11 ./backend/gradlew.bat -p backend test` | 테스트 전부. 저장소 루트에서. 419건이 기준선이다 |
| `node harness/tools/check.mjs g1 <소스> --type code --artifact <junit.xml>` | 게이트 |

테스트 DB는 `o2o_catalog_test`다. 설정 파일 기본값이라 환경변수 없이 돈다. PowerShell이면 `$env:JAVA_HOME='C:/Users/user/.jdks/ms-21.0.11'`를 먼저 주고 `.\backend\gradlew.bat -p backend test`를 돌린다.

바꿔도 되는 것은 빌드 산출물과 로그와 테스트 DB `o2o_catalog_test` 안의 표뿐이다. 소스와 테스트와 설정은 어떤 파일도 고치지 않는다. 컨테이너를 내리지 않는다. 다른 세션이 같은 컨테이너의 다른 DB를 쓴다.

생성자가 전달한 결과와 네가 직접 돌린 결과를 리포트에서 구분해 적는다. 돌리지 못했으면 돌리지 못했다고 적고 통과로 바꾸지 않는다.

## 6. 출력

`harness/prompts/eval-criteria-code.md` 5절의 스키마를 그대로 쓴다. 판정 요약, 상세, 실행 기록, 검증 ID 결과다. 요구사항 역추적표는 평가자 B의 몫이라 이 리포트에 넣지 않는다.

그 뒤에 보조 표를 붙인다. 열은 원본 번호, 지적 ID, 심각도, 위반 기준 넷이다. 상세 표의 행마다 한 행이고, 판정 요약의 치명과 보통과 확인필요 수가 이 표의 등급별 수와 같아야 한다. 이 표가 없거나 수가 어긋나면 리포트가 잘린 것으로 처리된다.

지적 ID는 `S9-R1-A-{원본 번호 2자리}`로 붙인다. 위치는 파일과 행 번호로 적는다.

리포트에 실제로 읽은 파일과 그 sha256을 남긴다.

리포트를 `harness/reviews/task-S9-booking-R1-A.md`에 저장한다. 이 파일 하나만 만든다.

## 7. 하지 말 것

- 리포트 파일 말고 파일을 만들거나 고치기
- 문체나 표현 지적하기
- 코드에 없는 내용을 추측으로 채우기
- 확인필요 등급을 없애고 통과와 실패 둘로만 판정하기
- 5절에 없는 명령 실행하기

판단이 서지 않으면 확인필요로 남긴다.
