// task-S9-frontend R1 평가 작업 트리 검사
//
// 왜 이 파일이 필요한가: 평가자가 여는 폴더가 요청문이 가리키는 기준 커밋을 담고 있고 148개 대상 파일이
// 목록의 해시 그대로인지 아무도 확인하지 않으면 다른 코드를 평가한 리포트를 받는다(이슈 87).
// mvp-eval-2026-09-14/eval-workspace-check.mjs는 backend/ 행과 backend/AGENTS.md만 알아서 프론트 판을 따로 둔다.
// 표의 열도 다르다. 프론트 목록은 경로, 성격, 줄 수, sha256 넷이라 해시가 넷째 칸이다.
//
// 실행 예시 (인자는 평가자가 열 폴더. 없으면 현재 폴더)
//   node harness/out/task-S9-frontend-R1/verify-eval-workspace.mjs C:/Dev/potenup/99_projects/o2o-dev
//
// 통과 출력 예시
//   PASS eval-workspace frontend C:/Dev/potenup/99_projects/o2o-dev
//     검사 10건 통과
//       ws.git ws.commit ws.agents ws.list ws.target-count ws.target-exists
//       ws.target-hash ws.target-drift ws.clean ws.doc-hash
//     알림 2건
//       ws.blind  평가자가 읽으면 안 되는 파일이 이 폴더에 있다: harness/out/task-S9-frontend-R1/t10/verification.md 외 9
//       ws.deps  frontend/node_modules가 없다. 평가자가 frontend/에서 npm ci를 먼저 돌린다
//
// 실패 출력 예시 (종료 코드 1)
//   FAIL eval-workspace frontend C:/Dev/potenup/99_projects/o2o
//     검사 3건 통과, 3건 실패
//       ws.git ws.agents ws.list
//       ws.commit  기준 커밋 8530a82가 HEAD의 조상이 아니다. HEAD는 main e165aeb Merge pull request #182
//       ws.target-exists  평가 대상 148개 중 12개가 없다. 예: frontend/e2e/support.ts, frontend/playwright.config.ts
//       ws.doc-hash  해시가 다른 자료 1건. harness/prompts/eval-criteria-code.md 기록 bf11b34c 실제 c5ed8359
//     범위 밖 3건
//       ws.target-hash  대상 파일이 없어서 해시를 대조할 것이 없다
//       ws.target-drift  대상 파일이 없어서 내용을 대조할 것이 없다
//       ws.clean  대상 파일이 없어서 볼 변경이 없다
//   범위 밖은 통과로 세지 않는다. 파일이 없으면 이 셋은 검사한 것이 아니라 안 돌린 것이다
//
// 통과가 뜻하는 것: 그 폴더를 Codex에 열면 요청문과 목록이 가리키는 파일이 적힌 해시 그대로 보인다.
// 통과가 뜻하지 않는 것: 평가자가 금지 파일을 안 읽는다는 보장. 그것은 사람이 지키는 규칙이라
// 스크립트가 확인할 수 없다. 그래서 알림으로만 낸다. node_modules 유무도 같다. 없으면 npm ci가 첫 명령이다.

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFileSync } from 'node:child_process';

const PAIR = 'frontend';
const BASE = '8530a828ec1af1e6dab03fc870661129c5e3e2b6';
const LIST = 'harness/out/task-S9-frontend-R1/eval-target-files.md';
const REQUESTS = ['harness/out/task-S9-frontend-R1/eval-request-A.md', 'harness/out/task-S9-frontend-R1/eval-request-B.md'];
const AGENTS = ['AGENTS.md', 'frontend/AGENTS.md'];
const EXPECT_TOTAL = 148;
// 평가자가 읽으면 안 되는 것. 있어도 실패가 아니라 알림이다
const BLIND = [
  'harness/out/task-S9-frontend-R1/t10/verification.md',
  ...Array.from({ length: 9 }, (_, i) => `harness/out/task-S9-frontend-R1/t${i + 1}`),
  'harness/reviews/task-S9-frontend-R1-A.md',
  'harness/reviews/task-S9-frontend-R1-B.md',
  'harness/decisions/task-S9-frontend-R1.md',
];

const root = path.resolve(process.argv[2] || process.cwd());
const passed = [];
const failed = [];
const skipped = [];
const notes = [];

function check(name, ok, detail) {
  if (ok) passed.push(name);
  else failed.push({ name, detail });
  return ok;
}
// 검사를 안 돌린 것과 돌려서 통과한 것을 가른다 (10-14 2-3절 A4)
function skip(name, why) {
  skipped.push({ name, why });
}
// git이 죽어도 스크립트는 계속 간다. 죽은 이유가 곧 검사 결과다
function git(args) {
  try {
    return { ok: true, out: execFileSync('git', args, { cwd: root, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim() };
  } catch (e) {
    return { ok: false, out: String(e.stderr || e.message || '').trim() };
  }
}
function sha256(abs) {
  return crypto.createHash('sha256').update(fs.readFileSync(abs)).digest('hex');
}
function cells(line) {
  return line.split('|').map((c) => c.trim());
}

// 1. 폴더가 이 저장소의 작업 트리인가
const remote = git(['remote', 'get-url', 'origin']);
const isRepo = remote.ok && remote.out.includes('stay-booking');
check('ws.git', isRepo, isRepo ? '' : `stay-booking 작업 트리가 아니다: ${remote.out || '원격 없음'}`);
if (!isRepo) {
  report();
  process.exit(1);
}

const head = git(['log', '--oneline', '-1']).out;
const branch = git(['rev-parse', '--abbrev-ref', 'HEAD']).out;

// 2. 기준 커밋이 HEAD에서 도달 가능한가. 이것이 핵심 검사다
const reach = git(['merge-base', '--is-ancestor', BASE, 'HEAD']);
check('ws.commit', reach.ok, `기준 커밋 ${BASE.slice(0, 7)}가 HEAD의 조상이 아니다. HEAD는 ${branch} ${head}`);

// 3. 평가자가 자동으로 읽는 규칙 파일
const agentsMissing = AGENTS.filter((p) => !fs.existsSync(path.join(root, p)));
check('ws.agents', agentsMissing.length === 0, `규칙 파일이 없다: ${agentsMissing.join(', ')}`);

// 4. 목록 파일과 그 표. 첫 칸이 frontend/로 시작하고 넷째 칸에 sha256이 있는 행. 성격 표(첫 표)의 개수도 읽는다
const listAbs = path.join(root, LIST);
const targets = []; // { rel, kind, hash }
const expectByKind = {};
if (!fs.existsSync(listAbs)) {
  check('ws.list', false, `목록 파일이 없다: ${LIST}`);
} else {
  for (const line of fs.readFileSync(listAbs, 'utf8').split('\n')) {
    if (!line.startsWith('|')) continue;
    const c = cells(line);
    if (c[1] && c[1].startsWith('frontend/')) {
      const hash = (String(c[4] || '').match(/sha256:([0-9a-f]{8,64})/) || [])[1];
      if (hash) targets.push({ rel: c[1], kind: c[2], hash });
    } else if (c[1] && /^\d+$/.test(c[2] || '')) {
      expectByKind[c[1]] = Number(c[2]);
    }
  }
  check('ws.list', targets.length > 0, `목록 표에서 대상 행을 못 찾았다: ${LIST}`);
}

// 5. 표의 행 수가 목록 문서가 적은 수와 같은가. 총수 148과 성격별 개수 둘 다
const countByKind = {};
for (const t of targets) countByKind[t.kind] = (countByKind[t.kind] || 0) + 1;
const kindDiff = Object.keys({ ...expectByKind, ...countByKind }).filter((k) => (expectByKind[k] || 0) !== (countByKind[k] || 0));
check('ws.target-count', targets.length === EXPECT_TOTAL && kindDiff.length === 0,
  `문서는 ${EXPECT_TOTAL}개인데 표는 ${targets.length}개다` + (kindDiff.length ? `. 성격별 불일치: ${kindDiff.map((k) => `${k} 문서 ${expectByKind[k] || 0} 표 ${countByKind[k] || 0}`).join(', ')}` : ''));

// 6. 그 파일들이 작업 트리에 실제로 있는가
const missing = targets.filter((t) => !fs.existsSync(path.join(root, t.rel)));
const present = check('ws.target-exists', targets.length > 0 && missing.length === 0,
  targets.length === 0
    ? '대상 파일이 하나도 없다'
    : `평가 대상 ${targets.length}개 중 ${missing.length}개가 없다. 예: ${missing.slice(0, 3).map((t) => t.rel).join(', ')}`);

// 7부터 9는 파일이 있어야 볼 수 있다. 없는 파일에 대고 돌리면 조용히 통과한다
if (!present) {
  skip('ws.target-hash', '대상 파일이 없어서 해시를 대조할 것이 없다');
  skip('ws.target-drift', '대상 파일이 없어서 내용을 대조할 것이 없다');
  skip('ws.clean', '대상 파일이 없어서 볼 변경이 없다');
} else {
  // 7. 작업 트리 파일의 sha256이 목록 표의 값과 같은가. 평가자는 커밋이 아니라 파일을 읽는다
  const badHash = targets.filter((t) => !sha256(path.join(root, t.rel)).startsWith(t.hash));
  check('ws.target-hash', badHash.length === 0,
    `목록의 해시와 다른 대상 파일 ${badHash.length}개. 예: ${badHash.slice(0, 3).map((t) => t.rel).join(', ')}`);

  // 8. 작업 트리의 내용이 기준 커밋 때와 같은가
  const drift = [];
  for (const t of targets) {
    const now = git(['hash-object', path.join(root, t.rel)]);
    const then = git(['rev-parse', `${BASE}:${t.rel}`]);
    if (!now.ok || !then.ok || now.out !== then.out) drift.push(t.rel);
  }
  check('ws.target-drift', drift.length === 0,
    `기준 커밋 이후 내용이 바뀐 대상 파일 ${drift.length}개. 예: ${drift.slice(0, 3).join(', ')}`);

  // 9. 대상 파일에 미커밋 변경이 없는가
  const dirty = git(['status', '--porcelain', '--', ...targets.map((t) => t.rel)]);
  check('ws.clean', dirty.ok && dirty.out === '', `대상 파일에 미커밋 변경이 있다: ${dirty.out}`);
}

// 10. 요청문 A와 B가 적은 해시가 실제 파일과 같은가. HR2와 같은 이유다. 표의 셋째 칸이 해시인 행만 본다
const bad = [];
let seen = 0;
const missingReq = [];
for (const req of REQUESTS) {
  const abs = path.join(root, req);
  if (!fs.existsSync(abs)) {
    missingReq.push(req);
    continue;
  }
  for (const line of fs.readFileSync(abs, 'utf8').split('\n')) {
    if (!line.trim().startsWith('|')) continue;
    const c = cells(line);
    const target = c[2];
    const rec = (String(c[3] || '').match(/sha256:([0-9a-f]{8,64})/) || [])[1];
    if (!target || !rec) continue;
    seen += 1;
    const tAbs = path.join(root, target);
    if (!fs.existsSync(tAbs)) {
      bad.push(`${target} 파일 없음(기록 ${rec.slice(0, 8)})`);
      continue;
    }
    const actual = sha256(tAbs);
    if (!actual.startsWith(rec)) bad.push(`${target} 기록 ${rec.slice(0, 8)} 실제 ${actual.slice(0, 8)}`);
  }
}
check('ws.doc-hash', missingReq.length === 0 && seen > 0 && bad.length === 0,
  missingReq.length ? `요청문이 없다: ${missingReq.join(', ')}` : seen === 0 ? '요청문에서 해시 행을 못 찾았다' : `해시가 다른 자료 ${bad.length}건. ${bad.join('; ')}`);

// 알림. 실패가 아니다
const blindHere = BLIND.filter((p) => fs.existsSync(path.join(root, p)));
if (blindHere.length) notes.push({ name: 'ws.blind', detail: `평가자가 읽으면 안 되는 파일이 이 폴더에 있다: ${blindHere[0]}${blindHere.length > 1 ? ` 외 ${blindHere.length - 1}` : ''}` });
if (!fs.existsSync(path.join(root, 'frontend/node_modules'))) notes.push({ name: 'ws.deps', detail: 'frontend/node_modules가 없다. 평가자가 frontend/에서 npm ci를 먼저 돌린다' });

report();
process.exit(failed.length ? 1 : 0);

function report() {
  const ok = failed.length === 0;
  console.log(`${ok ? 'PASS' : 'FAIL'} eval-workspace ${PAIR} ${root.split(path.sep).join('/')}`);
  console.log(`  검사 ${passed.length}건 통과${failed.length ? `, ${failed.length}건 실패` : ''}`);
  for (let i = 0; i < passed.length; i += 6) console.log(`    ${passed.slice(i, i + 6).join(' ')}`);
  for (const f of failed) console.log(`    ${f.name}  ${f.detail}`);
  if (skipped.length) {
    console.log(`  범위 밖 ${skipped.length}건`);
    for (const s of skipped) console.log(`    ${s.name}  ${s.why}`);
  }
  if (notes.length) {
    console.log(`  알림 ${notes.length}건`);
    for (const n of notes) console.log(`    ${n.name}  ${n.detail}`);
  }
}
