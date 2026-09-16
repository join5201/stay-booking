# AGENTS.md (frontend 평가자 규칙)

최초 작성: 2026-09-16
최종 갱신: 2026-09-16 (신설. 이슈 186)
독자: frontend/ 아래 코드를 평가하는 코딩 에이전트. 실제 운용에서는 Codex의 새 작업이다.

왜 이 파일이 필요한가: backend/AGENTS.md는 Spring 코드 기준이다. JUnit과 테스트 DB와 bootRun을 전제한다. 화면 코드는 읽을 근거 문서(인계 문서 둘)와 돌릴 명령(npm 스크립트)과 못 돌리는 것(E2E)이 다르다. 그 차이를 여기에 적는다.

Codex는 Git 루트에서 현재 디렉터리까지 내려오며 AGENTS.md를 모은다. 아래 파일이 뒤에 와서 우선한다. 그러므로 이 파일은 루트 AGENTS.md를 덮지 않고 더한다. E1부터 E8은 그대로 걸린다. backend/AGENTS.md는 이 폴더에 걸리지 않는다.

Claude Code는 이 파일을 읽지 않는다. 생성자 규칙은 루트 CLAUDE.md다. 이 파일은 평가 대상이 아니다. 평가 대상 목록(harness/out/task-S9-frontend-R1/eval-target-files.md)에 이 파일이 없는 것은 그래서다.

## 1. 읽는 것과 읽지 않는 것

허용 입력은 그 라운드 작업 계약의 허용 입력 칸(task-S9-frontend 5절)이 정한다. 목록에 없으면 읽지 않는다(E6).

| 읽는다 | 읽지 않는다 |
|---|---|
| 계약 5절이 적은 평가 대상 코드. 목록 `../harness/out/task-S9-frontend-R1/eval-target-files.md`의 148개 | `../harness/docs/` 전체 |
| `../document/11-o2o-api-spec.md`의 공통 절과 API 절과 응답 모델 | `../harness/state/` 아래 기록 |
| 인계 문서 둘. `../harness/out/claude-design-handoff-2026-09-14/received/HANDOFF.md` 전문과 같은 폴더 `context.md` 4절부터 13절 | 같은 인계 폴더의 `prompt.md`와 `README.md`(계획 문서) |
| `../backend/src/main/java/com/o2o/shared/`의 ErrorResponse.java 전문, RegionRegistry.java 24행부터 27행, ActorRegistry.java 22행부터 32행 | 그 밖의 backend/ 코드 |
| 계약이 적은 결과 파일. `../harness/out/task-S9-frontend-R1/t10/`의 로그 넷과 E2E 결과(e2e.log, e2e-results.json, playwright-report.html)와 error-mapping-check.txt | 같은 폴더의 `verification.md`(생성자의 검증 표와 회고), `t1/`부터 `t9/`(단계 시점 기록), 이전 리포트, 결정표, 생성 대화 |

`harness/state/progress.md`를 읽지 않는 이유는 그 파일에 생성자의 판단과 다음 계획이 적혀 있기 때문이다. 그것을 보면 블라인드가 아니다. `t10/verification.md`도 같다. 생성자가 스스로 매긴 판정이다.

인계 문서와 계약이 다르면 계약이 우선한다. 계약 2-1절이 그 여섯 곳을 적는다.

## 2. 화면 코드 평가에서만 걸리는 제약

| 번호 | 규칙 |
|---|---|
| FE1 | 결과 파일을 그대로 믿지 않는다. 3절의 명령을 직접 돌려 대조한다. 돌리지 못했으면 돌리지 못했다고 적는다 |
| FE2 | E2E(E01부터 E06)는 백엔드와 DB 실물이 있어야 돌므로 평가자가 돌리지 않는다. 전달받은 결과(e2e.log와 e2e-results.json)로 판정하고 리포트에 전달받은 결과라고 적는다 |
| FE3 | 테스트가 통과했다는 사실과 그 테스트가 무엇을 지키는지는 다르다. MSW 목이 돌려주는 응답은 시험이 정한 것이라 실물의 순서와 다를 수 있다. 둘을 따로 적는다 |
| FE4 | 명세와 코드가 다르면 명세가 정본이고, 인계 문서와 코드가 다르면 계약 2-1절을 먼저 본다. 어디에도 없으면 추측하지 말고 확인필요로 남긴다 |
| FE5 | 화면이 서버 상태를 스스로 바꾸는 자리를 찾는다. 만료를 브라우저 시계로 판정하거나 재전송 응답을 현재 상태로 믿는 곳이다. 상태의 정본은 BOOK-03 응답이다 |
| FE6 | 커버리지 숫자를 등급 근거로 쓰지 않는다. 계약 8-1절의 W01부터 W15마다 대응 테스트가 있는지를 본다 |
| FE7 | 접근성은 평가 기준 파일이 정한 최소 다섯 줄만 본다. 그 밖의 접근성 지적은 확인필요로 적고 치명이나 보통을 주지 않는다 |

## 3. 돌려도 되는 명령

계약 9절이 승인 명령으로 적은 것만 돌린다(E2). 전부 `frontend/` 안에서 돌린다.

| 명령 | 무엇 |
|---|---|
| `npm ci` | package-lock.json대로 의존성 설치. 처음 한 번. 소스를 바꾸지 않는다 |
| `npm run typecheck` | next typegen 뒤 tsc. 오류 0이 기준선 |
| `npm run lint` | eslint. 오류 0이 기준선 |
| `npm test` | Vitest(jsdom). 30파일 230건 통과가 기준선. e2e/는 제외돼 있다 |
| `npm run build` | 프로덕션 빌드. 라우트 17이 기준선 |

바꿔도 되는 것은 빌드 산출물(.next)과 node_modules와 로그뿐이다(E3). `npm run dev`와 `npm run start`와 `npm run test:e2e`는 목록에 없다. 서버를 띄우고 E2E를 돌리는 것은 생성자 몫이고 그 결과는 t10/으로 온다.

## 4. 층과 근거 문서

지적을 적을 때 어느 층의 문제인지와 어느 절을 어겼는지를 같이 적는다.

| 층 | 경로 | 근거 절 |
|---|---|---|
| 화면(라우트) | app/ | 계약 2절 대응표의 그 화면 행, 인계 문서 화면별 핵심 규칙 |
| 컴포넌트 | components/ | 인계 문서 공통 컴포넌트 절과 토큰 절, 계약 6절 폭과 색 행 |
| API 층 | lib/api/ (client.ts, keys.ts, server-clock.ts, types.ts, hooks/) | 11 공통 절과 해당 API 절, 계약 2-1절, 계약 6절 멱등키와 serverNow 행 |
| 도우미 | lib/ 바로 아래(errors.ts, error-view.ts, dates.ts, seoul-time.ts, regions.ts, cookies.ts, dev-actor.ts, role-route.ts, calendar.ts, forms.ts, stay.ts) | 계약 6절 오류 표시와 지역 코드와 P03 행, context.md 8절과 9절 |
| 미들웨어 | proxy.ts | 계약 2절 middleware 문단, 8-1절 W10 |
| 테스트 | *.test.ts와 *.test.tsx, 목은 lib/api/mocks/ | 계약 8-1절 W01부터 W15 |
| E2E | e2e/ | 계약 8-1절 E01부터 E06, 7절 D-4와 D-7 |

## 5. 미결

코드 평가 기준(`../harness/prompts/eval-criteria-code.md`)의 판이 확정이 아니다. 프론트 전용 축 여섯은 2026-09-16에 처음 붙었고 이 라운드가 첫 검증이다. 축과 등급은 그 파일을 따르고 이 파일에 옮겨 적지 않는다. 두 곳에 두면 갈라진다.
