# frontend

최초 작성: 2026-09-15
최종 갱신: 2026-09-15 (T1 기반. 뼈대와 스크립트)

O2O 숙박 예약 MVP의 화면이다. 로컬 개발과 검증까지만 다룬다. 작업 계약은 harness/tasks/task-S9-frontend.md이고 화면과 API 대응표는 그 2절이다.

## 1. 구성

| 항목 | 값 |
|---|---|
| 틀 | Next.js App Router, TypeScript, Tailwind, TanStack Query |
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
| npm run test | Vitest 단위와 훅 테스트 |
| npm run test:e2e | Playwright E2E. 설정과 시험은 T10에서 붙는다 |

## 3. 폴더

| 경로 | 무엇 |
|---|---|
| app/ | 라우트와 layout. providers.tsx가 QueryClient를 준다 |
| lib/ | T3부터. api/client.ts, errors.ts, api/hooks/, regions.ts |
| components/ | T2부터. 공통 컴포넌트 |
