# task-S9-frontend R1 평가자 B 요청문

최초 작성: 2026-09-16
최종 갱신: 2026-09-16

이 파일은 사용자가 Codex의 새 작업에 그대로 붙여 넣는 요청문이다. 프론트 평가 라운드(계약 5절, 이슈 186)의 평가자 B 몫이다. A와 B는 각각 다른 새 작업에서 돈다. 한 작업에서 둘 다 하지 않는다.

작업 디렉터리는 C:/Dev/potenup/99_projects/o2o-dev로 연다. 그래야 루트 AGENTS.md가 읽힌다. frontend/AGENTS.md는 frontend/ 아래를 만질 때 걸리는 규칙이고 이 요청문이 그 핵심을 그대로 담고 있다. 브랜치는 main이고 이슈 186의 PR이 병합된 뒤 git pull한 상태다. 기준 커밋 8530a82가 그 조상이다. 상위 폴더 o2o는 열지 않는다.

열기 전에 `node harness/out/task-S9-frontend-R1/verify-eval-workspace.mjs C:/Dev/potenup/99_projects/o2o-dev`를 돌려 PASS를 확인한다. FAIL이면 붙여 넣지 않는다. 평가자 A의 요청문은 같은 폴더의 eval-request-A.md다.

리포트는 `harness/reviews/task-S9-frontend-R1-B.md`에 저장된다. 그 파일이 생겼는지 붙여 넣은 뒤에 본다.

아래 구분선부터가 붙여 넣을 내용이다.

---

너는 평가자 B다. 설계자도 구현자도 아니다. 이 코드가 만들어진 논의를 보지 못했고 볼 필요도 없다. 주어진 파일만으로 판정한다.

## 1. 대상

| 항목 | 값 |
|---|---|
| Task | task-S9-frontend. 프론트 화면 전체(게스트 일곱, 호스트 다섯, 운영자 둘, 공통 둘) |
| 라운드 | R1 |
| 브랜치 | main. 이 코드는 T1부터 T10의 PR 열 개(152, 154, 159, 161, 164, 168, 171, 173, 176, 183)로 들어왔고 그 뒤 frontend/README.md의 test:e2e 줄 하나가 바뀌었다(이슈 186). 그 밖의 frontend/ 파일은 T10의 기준 커밋 0a1f109와 같다 |
| 기준 커밋 | 8530a828ec1af1e6dab03fc870661129c5e3e2b6 |

평가 대상 파일 목록은 `harness/out/task-S9-frontend-R1/eval-target-files.md`에 있다. frontend/ 추적 파일 148개 전부다. package-lock.json은 판 대조만 한다. frontend/AGENTS.md는 평가자 규칙이라 목록에 없고 대상이 아니다.

## 2. 읽어도 되는 것

평가자 A와 같다.

| 자료 | 경로 | 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 코드 목록 | harness/out/task-S9-frontend-R1/eval-target-files.md | sha256:693273e0f2bb30d1 | 전문 |
| 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:bf11b34c286c52f2 | 3절, 4절, 5절 |
| 작업 계약 | harness/tasks/task-S9-frontend.md | 자기 해시 없음 | 전문. 2절 대응표와 2-1절과 5절 마지막 문단과 6절과 8-1절 |
| 11 API 명세 | document/11-o2o-api-spec.md | sha256:9822c14edc59223f | 공통 절, API 32개 절, 응답 모델 |
| 인계 문서 | harness/out/claude-design-handoff-2026-09-14/received/HANDOFF.md | sha256:76d665f6ac36a9db | 전문. 계약 2-1절 여섯 곳은 계약이 우선한다 |
| 인계 컨텍스트 | harness/out/claude-design-handoff-2026-09-14/context.md | sha256:0919a1aad3b72d9e | 1절 표(보장 다섯과 화면에 미치는 뜻)와 4절부터 13절 |
| 오류 본문 | backend/src/main/java/com/o2o/shared/ErrorResponse.java | sha256:c62440297db7ef67 | 전문 |
| 지역 코드 | backend/src/main/java/com/o2o/shared/RegionRegistry.java | sha256:79160f582d45d513 | 24행부터 27행 |
| 행위자 | backend/src/main/java/com/o2o/shared/ActorRegistry.java | sha256:9b0e000f80967372 | 22행부터 32행 |

실행 결과 사본(harness/out/task-S9-frontend-R1/t10/의 typecheck.log, lint.log, test.log, build.log, e2e.log, e2e-results.json, playwright-report.html, error-mapping-check.txt)도 읽어도 된다. 기록된 해시와 실제 파일의 해시가 다르면 그 사실을 리포트에 적고 실제 파일로 진행한다.

읽지 않는 것도 A와 같다.

- 01 계획서 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, `harness/docs/` 전체, `harness/state/` 전체, `harness/decisions/` 전체, 생성 대화
- `harness/reviews/` 전체
- `harness/out/` 아래에서 1절과 2절이 지정한 파일 밖의 전부. 특히 `harness/out/task-S9-frontend-R1/t10/verification.md`, `t1/`부터 `t9/`, 인계 폴더의 `prompt.md`와 `README.md`, `harness/out/mvp-eval-2026-09-14/`와 다른 묶음의 폴더
- `backend/` 아래에서 2절의 shared 파일 셋 밖의 전부

t10/verification.md는 생성자가 스스로 매긴 판정이라 먼저 보면 블라인드가 아니다.

## 3. 네가 볼 축 셋

| 축 | 판정 질문 |
|---|---|
| 추적성 양방향 | 계약과 명세와 인계 문서의 항목이 코드에 전부 반영됐나. 코드에 근거 없이 새로 등장한 동작이 있나 |
| 결정 근거의 자립성 | 미결 정책(P01부터 P11 중 미채택)이 코드에 확정값으로 들어갔나. 들어갔다면 작업 계약 6절이 채택했나 |
| 요구사항 역추적 | 역추적 대상 각각이 어느 테스트로 확인되는지 코드와 테스트만 보고 짚을 수 있나 |

첫째 축의 뒤쪽 절반이 중요하다. 코드에 있으나 명세와 계약과 인계 문서 어디에도 근거가 없는 동작을 찾는 일이다. 앞쪽 절반만 보면 빠진 것은 잡히고 더해진 것은 안 잡힌다. 인계 문서와 계약이 다르면 계약이 우선한다. 그 여섯 곳은 계약 2-1절에 있다. 인계 문서대로가 아니라고 위반으로 적기 전에 그 절을 본다.

### 3-1. 요구사항 개수와 역추적 대상에 대한 지시

입력 팩 2절의 요구사항은 R1부터 R5까지 다섯이다. 기준 파일 3절도 다섯이다(2026-09-16 고침). 다섯으로 센다.

계약 5절 마지막 문단은 이렇게 적는다. R1부터 R5는 백엔드가 지키는 보장이고 화면은 그 보장을 깨는 동작을 유도하지 않는 쪽이다(context.md 1절 표의 셋째 칸). 이 Task의 역추적 대상은 2절 대응표의 화면 열여섯 행과 2-1절 여섯 행과 8-1절 E01부터 E06이다. 백엔드 보장 자체는 해당 없음으로 적고 미커버로 적지 않는다.

계약이 그렇게 적었다는 사실 자체가 타당한지는 네가 판단한다. 계약의 주장을 그대로 받아쓰지 말고 입력 팩의 요구사항 문장과 화면 코드를 직접 대조해 화면이 그 보장을 깨는 동작을 유도하는 자리가 있는지 확인한다. 있으면 그것이 지적이다.

역추적표는 대응표 열여섯 행(G1부터 G7, H1부터 H5, O1과 O2, C1과 C2)과 2-1절 여섯 행마다 한 행이고, 각각이 어느 테스트(계약 8-1절의 W나 E, 또는 그 밖의 테스트 파일과 케이스 이름)로 확인되는지 짚는다. E01부터 E06은 어느 행을 덮는지를 적는다.

## 4. 실행

평가자 B는 명령을 돌리지 않아도 된다. 축 셋이 전부 문서와 코드의 대조라서다. 돌린다면 평가자 A의 요청문 5절과 같은 명령만 `frontend/` 안에서 돌린다. E2E는 돌리지 않는다.

소스와 테스트와 설정은 어떤 파일도 고치지 않는다.

## 5. 출력

`harness/prompts/eval-criteria-code.md` 5절의 스키마를 쓴다. 판정 요약, 상세, 요구사항 역추적표다. 검증 ID 결과표는 평가자 A의 몫이라 이 리포트에 넣지 않는다.

그 뒤에 보조 표를 붙인다. 열은 원본 번호, 지적 ID, 심각도, 위반 기준 넷이다. 상세 표의 행마다 한 행이고, 판정 요약의 치명과 보통과 확인필요 수가 이 표의 등급별 수와 같아야 한다. 이 표가 없거나 수가 어긋나면 리포트가 잘린 것으로 처리된다.

지적 ID는 `S9-R1-B-{원본 번호 2자리}`로 붙인다. 위치는 파일과 행 번호로 적는다.

리포트에 실제로 읽은 파일과 그 sha256을 남긴다.

리포트를 `harness/reviews/task-S9-frontend-R1-B.md`에 저장한다. 이 파일 하나만 만든다.

## 6. 하지 말 것

- 리포트 파일 말고 파일을 만들거나 고치기
- 문체나 표현 지적하기
- 코드에 없는 내용을 추측으로 채우기
- 확인필요 등급을 없애고 통과와 실패 둘로만 판정하기
- 작업 계약이 적은 주장을 검증 없이 근거로 삼기
- 인계 문서와 계약이 다른 자리를 계약 2-1절을 안 보고 위반으로 적기

판단이 서지 않으면 확인필요로 남긴다.
