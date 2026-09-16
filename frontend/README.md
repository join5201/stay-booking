# frontend

최초 작성: 2026-09-15
최종 갱신: 2026-09-16 (test:e2e 줄을 T10 뒤 실제대로. 그 전 2026-09-15 T2 공통 UI와 T1 기반)

O2O 숙박 예약 MVP의 화면이다. 로컬 개발과 검증까지만 다룬다. 작업 계약은 harness/tasks/task-S9-frontend.md이고 화면과 API 대응표는 그 2절이다.

## 1. 구성

| 항목 | 값 |
|---|---|
| 틀 | Next.js App Router, TypeScript, Tailwind 4(토큰은 app/globals.css의 @theme), TanStack Query |
| 역할 경로 | proxy.ts(Next 16의 middleware 이름)가 쿠키 dev_actor의 역할과 /host, /operator, /bookings 앞부분이 어긋나면 역할 첫 화면으로 보낸다 |
| 개발용 쿠키 | dev_actor와 dev_mock_mode. layout이 요청 쿠키를 읽어 첫 그림부터 맞고, 개발용 바가 바꾸면 쿠키를 다시 쓴다 |
| 백엔드 연결 | next.config.ts의 rewrites가 /api/v1 아래 요청을 BACKEND_URL로 넘긴다. 백엔드에 CORS 설정이 없어 같은 출처로 부른다 |
| 환경변수 | BACKEND_URL 하나. env.example을 .env.local로 복사한다. 비어 있으면 http://localhost:8080 |
| 테스트 | 컴포넌트와 훅은 Vitest와 MSW, E2E는 Playwright(백엔드와 DB가 떠 있을 때만) |

## 2. 실행

| 명령 | 하는 일 |
|---|---|
| npm install | 의존성 설치. 판은 package-lock.json이 고정한다 |
| npm run dev | 개발 서버. 백엔드는 backend/README.md 3절대로 먼저 띄운다 |
| npm run typecheck | 라우트 타입 생성 뒤 tsc. 오류 0이어야 한다 |
| npm run lint | eslint. 오류 0이어야 한다 |
| npm run build | 프로덕션 빌드. 실패 0이어야 한다 |
| npm run test | Vitest(jsdom) 단위와 컴포넌트와 훅 테스트. 설정은 vitest.config.mts |
| npm run test:e2e | Playwright E2E 여섯(계약 8-1절 E01부터 E06). 백엔드가 o2o_web_test DB와 hold-ttl 20초로, 프론트가 next start 3000으로 떠 있어야 한다. 주소는 E2E_BASE_URL(기본 3000)과 E2E_BACKEND_URL(기본 8080). 시험 코드는 rewrites를 지나 백엔드를 부르고 INTERNAL-01만 백엔드 주소로 직접 부른다(e2e/support.ts 머리) |

## 3. 폴더

| 경로 | 무엇 |
|---|---|
| app/ | 라우트와 layout. providers.tsx가 QueryClient를 주고 layout이 AppShell을 감싼다 |
| components/ | 공통 컴포넌트. AppShell(개발용 바 36, 역할 탭 48, 띠 자리, 1200 폭 본문)과 인계 문서 103행의 목록 |
| lib/ | regions.ts(지역 코드 열일곱), dev-actor.ts, cookies.ts, role-route.ts, dates.ts, seoul-time.ts, api/types.ts. api/client.ts와 errors.ts와 api/hooks/는 T3 |
| proxy.ts | 역할 경로 지킴이 |
