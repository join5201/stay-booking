// 평가 작업 공간 검사 (숙소와 객실 R1. 라운드 mvp-eval-2026-09-14 쌍)
//
// 몸통은 harness/out/mvp-eval-2026-09-14/eval-workspace-check.mjs에 있다. 이 파일은 그 쌍의 상수만 준다.
// 검사 열 가지와 출력 모양과 실패 출력 예시는 그 파일 머리 주석에 있다.
//
// 실행 예시
//   node harness/out/task-S9-catalog-R1/verify-eval-workspace.mjs
//   node harness/out/task-S9-catalog-R1/verify-eval-workspace.mjs C:/Dev/potenup/99_projects/o2o-dev
//
// 통과 출력 예시
//   PASS eval-workspace catalog C:/Dev/potenup/99_projects/o2o-dev
//     검사 10건 통과
//       ws.git ws.commit ws.agents ws.list ws.target-count ws.target-exists ws.target-hash ws.target-drift ws.clean ws.doc-hash
//     알림 1건
//       ws.blind  평가자가 읽으면 안 되는 파일이 이 폴더에 있다: harness/out/task-S9-catalog-R1/step9-verification.md 외 3
//
// 실패 출력 예시 (종료 코드 1)
//   FAIL eval-workspace catalog C:/Dev/potenup/99_projects/o2o
//     검사 3건 통과, 2건 실패
//       ws.git ws.agents ws.list
//       ws.commit  기준 커밋 1bdadfe이 HEAD의 조상이 아니다. HEAD는 chore/context-budget 16bb437
//       ws.target-exists  평가 대상 58개 중 58개가 없다. 예: ...
//     범위 밖 3건
//       ws.target-hash ws.target-drift ws.clean (대상 파일이 없어서 안 돌렸다)

import { run } from '../mvp-eval-2026-09-14/eval-workspace-check.mjs';

run({
  pair: 'catalog',
  base: '1bdadfe5a78722f1e708d96bafc0f728eddfc4a1', // 요청문 1절의 기준 커밋. PR 138 병합
  listPath: 'harness/out/task-S9-catalog-R1/eval-target-files.md',
  requests: ['harness/out/task-S9-catalog-R1/eval-request-A.md', 'harness/out/task-S9-catalog-R1/eval-request-B.md'],
  expect: { prod: 52, test: 6, config: 0 }, // 목록 문서가 적은 수
  blind: [
    'harness/out/task-S9-catalog-R1/step9-verification.md',
    'harness/out/mvp-eval-2026-09-14/README.md',
    'harness/reviews/task-S9-inventory-rate-R1-A.md',
    'harness/decisions/task-S9-inventory-rate-R1.md',
  ],
});
