# task-S9-frontend R1 평가자 A 요청문

최초 작성: 2026-09-16
최종 갱신: 2026-09-16

이 파일은 사용자가 Codex의 새 작업에 그대로 붙여 넣는 요청문이다. 프론트 평가 라운드(계약 5절. 2026-09-12 결정 프론트 평가는 한다, 이슈 186)의 평가자 A 몫이고 근거는 10-4 1절의 전달은 사람이 한다는 규칙이다. A와 B는 각각 다른 새 작업에서 돈다. 한 작업에서 둘 다 하지 않는다.

작업 디렉터리는 C:/Dev/potenup/99_projects/o2o-dev로 연다. 그래야 루트 AGENTS.md가 읽힌다. frontend/AGENTS.md는 frontend/ 아래를 만질 때 걸리는 규칙이고 이 요청문이 그 핵심(읽는 것, 명령, 제약)을 그대로 담고 있다. 브랜치는 main이고 이슈 186의 PR이 병합된 뒤 git pull한 상태다. 기준 커밋 8530a82가 그 조상이다. 상위 폴더 o2o는 세션 여럿이 브랜치를 바꿔 가며 쓰는 곳이라 열지 않는다.

열기 전에 `node harness/out/task-S9-frontend-R1/verify-eval-workspace.mjs C:/Dev/potenup/99_projects/o2o-dev`를 돌려 PASS를 확인한다. FAIL이면 붙여 넣지 않는다. 평가자 B의 요청문은 같은 폴더의 eval-request-B.md다.

리포트는 `harness/reviews/task-S9-frontend-R1-A.md`에 저장된다. 그 파일이 생겼는지 붙여 넣은 뒤에 본다.

아래 구분선부터가 붙여 넣을 내용이다.

---

너는 평가자 A다. 설계자도 구현자도 아니다. 이 코드가 만들어진 논의를 보지 못했고 볼 필요도 없다. 주어진 파일만으로 판정한다.

## 1. 대상

| 항목 | 값 |
|---|---|
| Task | task-S9-frontend. 프론트 화면 전체(게스트 일곱, 호스트 다섯, 운영자 둘, 공통 둘) |
| 라운드 | R1 |
| 브랜치 | main. 이 코드는 T1부터 T10의 PR 열 개(152, 154, 159, 161, 164, 168, 171, 173, 176, 183)로 들어왔고 그 뒤 frontend/README.md의 test:e2e 줄 하나가 바뀌었다(이슈 186). 그 밖의 frontend/ 파일은 T10의 기준 커밋 0a1f109와 같다 |
| 기준 커밋 | 8530a828ec1af1e6dab03fc870661129c5e3e2b6 |

평가 대상 파일 목록은 `harness/out/task-S9-frontend-R1/eval-target-files.md`에 있다. frontend/ 추적 파일 148개 전부다. 화면(라우트) 19, 컴포넌트 55, API 층 10, 도우미 11, 테스트 30, 테스트 목 4, E2E 8, 미들웨어 1, 설정 10이다. package-lock.json은 판 대조만 한다. frontend/AGENTS.md는 평가자 규칙이라 목록에 없고 대상이 아니다.

## 2. 읽어도 되는 것

| 자료 | 경로 | 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 코드 목록 | harness/out/task-S9-frontend-R1/eval-target-files.md | sha256:693273e0f2bb30d1 | 전문. 파일마다의 sha256이 안에 있다 |
| 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:bf11b34c286c52f2 | 축, 심각도, 출력 스키마. 2절의 프론트 전용 축 여섯이 이번에 처음 쓰인다 |
| 작업 계약 | harness/tasks/task-S9-frontend.md | 자기 해시 없음 | 전문. 2절 대응표와 2-1절 여섯 행과 6절과 8-1절이 판정의 근거다 |
| 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절, API 32개 절, 응답 모델 |
| 인계 문서 | harness/out/claude-design-handoff-2026-09-14/received/HANDOFF.md | sha256:76d665f6ac36a9db | 전문. 화면 요구의 원천. 계약 2-1절 여섯 곳은 계약이 우선한다 |
| 인계 컨텍스트 | harness/out/claude-design-handoff-2026-09-14/context.md | sha256:0919a1aad3b72d9e | 1절 표(보장 다섯과 화면에 미치는 뜻)와 4절부터 13절 |
| 오류 본문 | backend/src/main/java/com/o2o/shared/ErrorResponse.java | sha256:c62440297db7ef67 | 전문 |
| 지역 코드 | backend/src/main/java/com/o2o/shared/RegionRegistry.java | sha256:79160f582d45d513 | 24행부터 27행 |
| 행위자 | backend/src/main/java/com/o2o/shared/ActorRegistry.java | sha256:9b0e000f80967372 | 22행부터 32행 |
| 생성자가 낸 실행 결과 | harness/out/task-S9-frontend-R1/t10/의 typecheck.log, lint.log, test.log, build.log, e2e.log, e2e-results.json, playwright-report.html, error-mapping-check.txt | 없음 | 전문. 기준 커밋 0a1f109에서 돈 결과다. 기준 커밋 8530a82와의 차이는 frontend/README.md 한 줄이라 실행 결과에 영향이 없다 |

기록된 해시와 실제 파일의 해시가 다르면 그 사실을 리포트에 적고 실제 파일로 진행한다.

## 3. 읽지 않는 것

- 01 계획서 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, `harness/docs/` 전체, `harness/state/` 전체, `harness/decisions/` 전체, 생성 대화
- `harness/reviews/` 전체
- `harness/out/` 아래에서 1절과 2절이 지정한 파일 밖의 전부. 특히 `harness/out/task-S9-frontend-R1/t10/verification.md`(생성자의 검증 표와 회고), `t1/`부터 `t9/`(단계 시점 기록), 인계 폴더의 `prompt.md`와 `README.md`(계획 문서), `harness/out/mvp-eval-2026-09-14/`와 다른 묶음의 폴더
- `backend/` 아래에서 2절의 shared 파일 셋 밖의 전부. 화면은 백엔드 구현이 아니라 명세와 인계 문서를 보고 만들어졌다. 백엔드 코드로 화면을 판정하지 않는다

t10/verification.md는 생성자가 스스로 매긴 판정이다. 그것을 먼저 보면 블라인드가 아니다.

## 4. 네가 볼 축

`harness/prompts/eval-criteria-code.md` 2절이 정본이다. 아래는 이 대상에 맞춘 안내다.

### 4-1. 구조 축과 코드 전용 축

구조 축 넷 중 경계 위반과 Repository 단위와 레이어 역전은 Spring 층 구조의 말이라 이 대상에 해당 없음으로 적는다. 계약 하강은 본다. 계약 2절 대응표의 행과 8-1절의 W 항목 중 테스트로 내려가지 않은 것을 찾는다.

코드 전용 축 다섯 중 동시성과 트랜잭션은 해당 없음이다. 나머지 넷은 이렇게 읽는다.

| 축 | 이 대상에서의 판정 질문 | 근거 |
|---|---|---|
| API 계약 준수 | 요청 본문의 키 집합과 쿼리와 헤더가 명세와 같은가. 명세에 없는 필드나 경로를 만들었나. 응답 필드를 명세와 다른 이름으로 읽나 | 11 해당 API 절, 계약 2-1절 1행과 3행 |
| 멱등 규칙 | Idempotency-Key를 붙이는 쓰기 API가 명세 공통 멱등 처리와 같은가. Idempotency-Replayed 응답을 성공으로 다루나. REQUEST_IN_PROGRESS면 같은 키로 재시도하나 | 11 공통 멱등 처리, 계약 6절 멱등키 행 |
| 소유권과 접근 | 행위자가 쿠키 dev_actor에서 헤더 X-Dev-Actor-Id로만 가고 본문에 guestId나 hostId를 넣어 권한을 정하지 않나. public이면 헤더를 생략하나. middleware가 역할과 경로 앞부분이 어긋나면 역할 첫 화면으로 보내나 | 11 인증과 접근 제어, 계약 6절 P07, 계약 2절 middleware 문단 |
| 테스트 격리와 재현성 | 시간을 제어하나(브라우저 시계에 기대는 테스트가 있나). MSW 목이 실물과 다른 순서로 응답하는 곳을 테스트가 실물인 양 다루나. 테스트 사이에 목과 쿠키를 정리하나 | 10-6 D-3, 계약 8-1절 W06 |

### 4-2. 프론트 전용 축 여섯

| 축 | 판정 질문 | 근거 |
|---|---|---|
| 화면과 API 대응 준수 | 화면마다 부르는 API와 시점이 계약 2절 대응표 그대로인가. 대응표에 없는 호출, 계약 2-2절이 만들지 않기로 한 것(주기 폴링, 서버 컴포넌트 조회, 프록시 핸들러)이 있나 | 계약 2절과 2-1절과 2-2절, context.md 5절과 6절 |
| 상태 전이 준수 | 화면의 분기가 서버가 준 status와 expirationReason과 결제 시도 상태로만 갈리나. 화면이 스스로 상태를 바꾸는 곳이 있나. 쓰기 성공과 replayed 뒤 BOOK-03 재조회가 있나 | 계약 2절 G4와 G5와 G7 행, HANDOFF.md 75행부터 80행, context.md 7절과 13절 |
| 멱등키 규칙 | G4 마운트 시 하나, PRICE_CHANGED 뒤 새 키, 네트워크와 5xx와 REQUEST_IN_PROGRESS 재시도만 같은 키, G5 클릭마다 새 키, G7 시트 열 때 하나 닫으면 폐기. 키가 useRef 밖(URL, 저장소)에 있나 | 계약 6절 멱등키 행, HANDOFF.md 41행부터 46행 |
| 오류 표시 자리 | context.md 8절의 코드 스물셋마다 자리 셋 중 하나가 정해져 있고 그 자리가 HANDOFF.md 81행부터 86행과 계약 2-1절 1행과 4행과 5행대로인가. Retry-After를 REQUEST_IN_PROGRESS 밖에서 기대하나. 화면 검사가 서버보다 먼저인가. 오류에 빨간색을 썼나 | 계약 2절 C2 행과 6절 오류 표시 행, context.md 8절과 9절 |
| 시간 규칙 | 남은 시간을 expiresAt과 serverNow 차이로 세고 브라우저 시계를 쓰지 않나. serverNow는 본문 우선이고 없으면 Date 헤더인가. 0 도달 시 BOOK-03 1회이고 주기 폴링이 없나. 시각은 UTC로 받아 서울로 표시하고 숙박 날짜는 서울 날짜 문자열 그대로인가. 취소 가능 판단이 서울 날짜 비교인가 | 계약 6절 P01과 P03과 P05와 serverNow 행, HANDOFF.md 39행, context.md 7절 |
| 접근성 최소 | 기준 파일 2절의 최소 다섯 줄만 본다(label 연결, button과 a, 비활성 이유 문구, 오류와 시트의 role, 색만으로 상태 구분 안 함). 상위 문서에 접근성 규칙이 없으므로 위반의 심각도 상한은 보통이고 다섯 줄 밖의 접근성 지적은 확인필요다 | 기준 파일 2절 |

### 4-3. 검증 ID 판정

계약 8-1절의 W01부터 W15와 E01부터 E06 각각에 통과, 실패, 미실행 셋 중 하나를 적는다. W는 네가 5절의 `npm test`를 돌려 나온 결과와 테스트 코드를 대조해 판정한다. 테스트가 초록이라는 것과 그 테스트가 8-1절의 통과 조건을 실제로 확인한다는 것은 다르다. 둘 다 적는다. E는 백엔드와 DB 실물이 있어야 돌므로 돌리지 않는다. 전달받은 e2e.log와 e2e-results.json으로 판정하고 전달받은 결과라고 적는다. 미실행으로 적지 않는다. 실행하지 않은 것을 통과로 적지 않는다.

## 5. 돌려도 되는 명령

전부 `frontend/` 안에서 돌린다. PowerShell이면 `Set-Location frontend`를 먼저 한다.

| 명령 | 무엇 | 기준선 |
|---|---|---|
| `npm ci` | package-lock.json대로 의존성 설치. 처음 한 번. node_modules가 없는 상태에서 시작한다 | 오류 없이 끝남 |
| `npm run typecheck` | next typegen 뒤 tsc | 오류 0 |
| `npm run lint` | eslint | 오류 0 |
| `npm test` | Vitest(jsdom). e2e/는 제외돼 있다 | 30파일 230건 통과 |
| `npm run build` | 프로덕션 빌드 | 실패 0, 라우트 17 |

바꿔도 되는 것은 `frontend/.next`와 `frontend/node_modules`와 로그뿐이다. 소스와 테스트와 설정은 어떤 파일도 고치지 않는다. `npm run dev`와 `npm run start`와 `npm run test:e2e`는 돌리지 않는다. 백엔드와 DB 컨테이너는 건드리지 않는다. 다른 세션이 쓴다.

생성자가 전달한 결과와 네가 직접 돌린 결과를 리포트에서 구분해 적는다. 돌리지 못했으면 돌리지 못했다고 적고 통과로 바꾸지 않는다.

## 6. 출력

`harness/prompts/eval-criteria-code.md` 5절의 스키마를 그대로 쓴다. 판정 요약, 상세, 실행 기록, 검증 ID 결과다. 요구사항 역추적표는 평가자 B의 몫이라 이 리포트에 넣지 않는다.

그 뒤에 보조 표를 붙인다. 열은 원본 번호, 지적 ID, 심각도, 위반 기준 넷이다. 상세 표의 행마다 한 행이고, 판정 요약의 치명과 보통과 확인필요 수가 이 표의 등급별 수와 같아야 한다. 이 표가 없거나 수가 어긋나면 리포트가 잘린 것으로 처리된다.

지적 ID는 `S9-R1-A-{원본 번호 2자리}`로 붙인다. 위치는 파일과 행 번호로 적고 층은 frontend/AGENTS.md 4절의 이름(화면, 컴포넌트, API 층, 도우미, 미들웨어, 테스트, E2E)으로 적는다.

리포트에 실제로 읽은 파일과 그 sha256을 남긴다.

리포트를 `harness/reviews/task-S9-frontend-R1-A.md`에 저장한다. 이 파일 하나만 만든다.

## 7. 하지 말 것

- 리포트 파일 말고 파일을 만들거나 고치기
- 문체나 표현 지적하기
- 코드에 없는 내용을 추측으로 채우기
- 확인필요 등급을 없애고 통과와 실패 둘로만 판정하기
- 5절에 없는 명령 실행하기
- 인계 문서와 계약이 다른 자리를 계약 2-1절을 안 보고 위반으로 적기

판단이 서지 않으면 확인필요로 남긴다.
