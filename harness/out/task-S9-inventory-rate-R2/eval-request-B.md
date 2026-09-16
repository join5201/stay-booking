# task-S9-inventory-rate R2 평가자 B 요청문

최초 작성: 2026-09-14
최종 갱신: 2026-09-14

이 파일은 사용자가 Codex의 새 작업에 그대로 붙여 넣는 요청문이다. 라운드 mvp-eval-2026-09-14(harness/out/mvp-eval-2026-09-14/README.md)의 쌍 2이고 근거는 오늘의 전제 결정 3과 10-4 1절의 전달은 사람이 한다는 규칙이다. A와 B는 각각 다른 새 작업에서 돈다. 한 작업에서 둘 다 하지 않는다. 평가자 분할의 근거는 eval-criteria-ddd.md 6절이다. R2다. 4-4절의 확인 항목 여섯은 R1 결정표(harness/decisions/task-S9-inventory-rate-R1.md)의 반영 인계 표에서 같은 자리를 짚은 둘씩을 합쳐 중립 문장으로 옮긴 것이다. 옮긴 사람은 나이고 사용자가 원문과 대조할 수 있게 출처를 여기 적는다. 계약 10절 개정 칸과 5절 두 문장이 R1 지적 번호를 인용하지만 지적 본문은 없다. 계약은 허용 입력이라 그대로 간다.

작업 디렉터리는 C:/Dev/potenup/99_projects/o2o-dev로 연다. 그래야 AGENTS.md와 backend/AGENTS.md가 읽힌다. 브랜치는 eval/mvp-2026-09-14이고 기준 커밋 1bdadfe이 그 조상이다. 상위 폴더 o2o는 세션 여럿이 브랜치를 바꿔 가며 쓰는 곳이라 열지 않는다.

열기 전에 `node harness/out/task-S9-inventory-rate-R2/verify-eval-workspace.mjs`를 그 폴더에서 돌려 PASS를 확인한다. FAIL이면 붙여 넣지 않는다. 평가자 A의 요청문은 같은 폴더의 eval-request-A.md다.

리포트는 `harness/reviews/task-S9-inventory-rate-R2-B.md`에 저장된다. 그 파일이 생겼는지 붙여 넣은 뒤에 본다.

아래 구분선부터가 붙여 넣을 내용이다.

---

너는 평가자 B다. 설계자도 구현자도 아니다. 이 코드가 만들어진 논의를 보지 못했고 볼 필요도 없다. 주어진 파일만으로 판정한다.

## 1. 대상

| 항목 | 값 |
|---|---|
| Task | task-S9-inventory-rate. 재고와 요금 묶음 |
| 라운드 | R2 |
| 브랜치 | eval/mvp-2026-09-14. origin/main에서 딴 평가용 브랜치라 코드는 main과 같다. 이 묶음의 코드는 PR 54와 115로 들어왔고 그 뒤 예약 묶음이 inventory 파일 넷을 고치고 여덟을 더했다. 어느 파일인지는 목록의 표에 있다 |
| 기준 커밋 | 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1 |

평가 대상 파일 목록은 `harness/out/task-S9-inventory-rate-R1/eval-target-files.md`에 있다. 그 파일의 6절 main 기준 목록이 이번 대상이다. 프로덕션 52개(inventory 49, shared 3)와 테스트 9파일 83건이다. 1절부터 5절은 지난 라운드 시점의 기록이라 대상이 아니다. 목록이 평가 대상이 아니라고 적은 것은 읽지 않는다.

## 2. 읽어도 되는 것

평가자 A와 같다.

| 자료 | 경로 | 해시 |
|---|---|---|
| 평가 대상 코드 목록 | harness/out/task-S9-inventory-rate-R1/eval-target-files.md | sha256:f01d6c51b6bcc3e7 |
| 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 |
| 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 |
| 작업 계약 | harness/tasks/task-S9-inventory-rate.md | 자기 해시 없음 |
| 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 |
| 06-2 애그리거트 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d |
| 06-4 계약 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f |
| 전역 역직렬화 설정 | backend/src/main/resources/application.properties | sha256:a53c90a2ad96f493 |
| 앞 묶음의 미정의 필드 거절 테스트 | backend/src/test/java/com/o2o/catalog/api/CatalogApiTest.java | sha256:700d593eed9f3741 |

실행 결과 사본(harness/out/task-S9-inventory-rate-R1/step5, step6, step6-2와 harness/out/task-S9-booking-lifecycle-R1/step9의 JUnit XML)도 읽어도 된다. 기록된 해시와 실제 파일의 해시가 다르면 그 사실을 리포트에 적고 실제 파일로 진행한다.

읽지 않는 것도 A와 같다.

- 01 계획서 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, `harness/docs/` 전체, `harness/state/` 전체, 생성 대화
- `harness/reviews/` 전체
- `harness/decisions/` 전체
- `harness/out/` 아래에서 1절과 2절이 지정한 파일과 폴더 밖의 전부. 특히 `harness/out/mvp-eval-2026-09-14/README.md`(라운드 계획), 다른 묶음의 요청문, 각 묶음의 `step9-verification.md`
- `harness/out/task-S9-inventory-rate-R1/applied/` 전체
- `harness/reviews/task-S9-inventory-rate-R1-A.md`와 `-B.md`
- `harness/decisions/task-S9-inventory-rate-R1.md`

step9-verification.md는 생성자가 스스로 매긴 판정이라 먼저 보면 블라인드가 아니다.

## 3. 네가 볼 축 셋

| 축 | 판정 질문 |
|---|---|
| 추적성 양방향 | 계약과 명세의 항목이 코드에 전부 반영됐나. 코드에 근거 없이 새로 등장한 동작이 있나 |
| 결정 근거의 자립성 | 미결 정책(P01부터 P11 중 미채택)이 코드에 확정값으로 들어갔나. 들어갔다면 작업 계약이 채택했나 |
| 요구사항 역추적 | 요구사항 각각이 어느 테스트로 확인되는지 코드와 테스트만 보고 짚을 수 있나 |

첫째 축의 뒤쪽 절반이 중요하다. 코드에 있으나 명세와 계약 어디에도 근거가 없는 동작을 찾는 일이다. 앞쪽 절반만 보면 빠진 것은 잡히고 더해진 것은 안 잡힌다.

### 3-1. 요구사항 개수에 대한 지시

입력 팩 2절의 요구사항은 R1부터 R5까지 다섯이다. `eval-criteria-code.md` 3절이 R1부터 R6이라 적으나 그 파일의 오기다. 정본은 입력 팩이다. 다섯으로 센다.

이번 묶음에 걸리는 요구사항은 없다고 계약 5절이 적는다. 걸리지 않는 것은 해당 없음으로 적고 미커버로 적지 않는다. 미커버는 덮어야 하는데 안 덮은 것이고 해당 없음은 이 묶음의 일이 아닌 것이다. 둘은 다르다.

계약이 그렇게 적었다는 사실 자체가 타당한지는 네가 판단한다. 계약의 주장을 그대로 받아쓰지 말고 입력 팩의 요구사항 문장과 이 묶음의 코드를 직접 대조해 확인한다.

이 Task의 역추적 대상은 계약 2절의 API 아홉과 2-1절의 I1과 I2다. I1a는 계약 5절이 예약 묶음 이월로 대상에서 뺐다. 그 이월이 지금 코드와 맞는지는 네가 본다.

## 4. 실행

평가자 B는 명령을 돌리지 않아도 된다. 축 셋이 전부 문서와 코드의 대조라서다. 돌린다면 평가자 A의 요청문 5절과 같은 명령만 같은 테스트 DB(`o2o_booking_test`)에서 돌린다.

소스와 테스트와 설정은 어떤 파일도 고치지 않는다.

## 5. 출력

`harness/prompts/eval-criteria-code.md` 5절의 스키마를 쓴다. 판정 요약, 상세, 요구사항 역추적표다. 검증 ID 결과표는 평가자 A의 몫이라 이 리포트에 넣지 않는다.

그 뒤에 보조 표를 붙인다. 열은 원본 번호, 지적 ID, 심각도, 위반 기준 넷이다. 상세 표의 행마다 한 행이고, 판정 요약의 치명과 보통과 확인필요 수가 이 표의 등급별 수와 같아야 한다. 이 표가 없거나 수가 어긋나면 리포트가 잘린 것으로 처리된다.

지적 ID는 `S9-R2-B-{원본 번호 2자리}`로 붙인다. 위치는 파일과 행 번호로 적는다.

리포트에 실제로 읽은 파일과 그 sha256을 남긴다.

리포트를 `harness/reviews/task-S9-inventory-rate-R2-B.md`에 저장한다. 이 파일 하나만 만든다.

## 6. 하지 말 것

- 리포트 파일 말고 파일을 만들거나 고치기
- 문체나 표현 지적하기
- 코드에 없는 내용을 추측으로 채우기
- 확인필요 등급을 없애고 통과와 실패 둘로만 판정하기
- 작업 계약이 적은 주장을 검증 없이 근거로 삼기

판단이 서지 않으면 확인필요로 남긴다.
