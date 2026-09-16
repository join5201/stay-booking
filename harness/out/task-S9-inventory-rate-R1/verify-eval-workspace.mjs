// 평가 작업 공간 검사 스크립트 (task-S9-inventory-rate R1)
//
// 왜 필요한가: 평가 요청문 A와 B는 기준 커밋 280a691과 평가 대상 파일 50개를 적는다.
// 그런데 Codex가 실제로 여는 폴더가 그 커밋을 담고 있는지는 아무도 보지 않는다.
// 2026-09-10에 어긋났다. C:/Dev/potenup/99_projects/o2o가 다른 세션의 브랜치에
// 체크아웃되어 있어서 inventory 파일이 0개였다. 그 상태로 요청문을 붙여 넣었으면
// 평가자가 파일을 못 찾고 멈추거나, 더 나쁘게는 없는 것을 미구현으로 판정한다.
//
// LLM을 부르지 않는다. 로컬 검사만 한다. 어떤 파일도 고치지 않는다.
//
// 대상 목록을 문서에서 베끼지 않고 git 커밋 범위에서 뽑는다. 문서가 적은 43과 7이
// 맞는지를 이 스크립트가 대조한다. 문서를 믿으면 문서의 오기를 못 잡는다.
//
// 실행 예시
//   node harness/out/task-S9-inventory-rate-R1/verify-eval-workspace.mjs
//   node harness/out/task-S9-inventory-rate-R1/verify-eval-workspace.mjs C:/Dev/potenup/99_projects/o2o
//
// 통과 출력 예시
//   PASS eval-workspace C:/Dev/potenup/99_projects/o2o-dev
//     검사 8건 통과
//       ws.git ws.commit ws.agents ws.target-count ws.target-exists
//       ws.target-drift ws.clean ws.doc-hash
//     알림 1건
//       ws.blind  이 폴더에 평가자가 읽으면 안 되는 파일이 있다
//
// 실패 출력 예시 (종료 코드 1). 2026-09-10에 실제로 나온 출력이다
//   FAIL eval-workspace C:/Dev/potenup/99_projects/o2o
//     검사 2건 통과, 4건 실패
//       ws.git ws.target-count
//       ws.commit  기준 커밋 280a691이 HEAD의 조상이 아니다. HEAD는 chore/context-budget 16bb437
//       ws.agents  규칙 파일이 없다: backend/AGENTS.md
//       ws.target-exists  평가 대상 50개 중 50개가 없다
//       ws.doc-hash  요청문이 없다: harness/out/task-S9-inventory-rate-R1/eval-request-A.md
//     범위 밖 2건
//       ws.target-drift  대상 파일이 없어서 내용을 대조할 것이 없다
//       ws.clean  대상 파일이 없어서 볼 변경이 없다
//   범위 밖은 통과로 세지 않는다. 파일이 없으면 이 둘은 검사한 것이 아니라 안 돌린 것이다
//
// 통과가 뜻하는 것: 그 폴더를 Codex에 열면 요청문이 가리키는 파일이 요청문이 적은
// 버전 그대로 보인다. 통과가 뜻하지 않는 것: 평가자가 금지 파일을 안 읽는다는 보장.
// 그것은 사람이 지키는 규칙이라 스크립트가 확인할 수 없다. 그래서 알림으로만 낸다.

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFileSync } from 'node:child_process';

const BASE = '280a69163a6a64225bb414969a134af22d5cefbe'; // 요청문 1절의 기준 커밋
const SPLIT = '8e40435'; // 앞 묶음과 갈라진 지점. eval-target-files.md 1절
const OUT_DIR = 'harness/out/task-S9-inventory-rate-R1';
const REQUEST_A = `${OUT_DIR}/eval-request-A.md`;
const BLIND = `${OUT_DIR}/step9-verification.md`;
const EXPECT_PROD = 43;
const EXPECT_TEST = 7;

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
    return {
      ok: true,
      out: execFileSync('git', args, { cwd: root, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim(),
    };
  } catch (e) {
    return { ok: false, out: String((e.stderr || e.message || '')).trim() };
  }
}

function sha256(abs) {
  return crypto.createHash('sha256').update(fs.readFileSync(abs)).digest('hex');
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

// 2. 기준 커밋이 HEAD에서 도달 가능한가. 이것이 이 스크립트의 핵심 검사다
const reach = git(['merge-base', '--is-ancestor', BASE, 'HEAD']);
check('ws.commit', reach.ok,
  `기준 커밋 ${BASE.slice(0, 7)}이 HEAD의 조상이 아니다. HEAD는 ${branch} ${head}`);

// 3. 평가자가 자동으로 읽는 규칙 파일
const agentsMissing = ['AGENTS.md', 'backend/AGENTS.md'].filter((p) => !fs.existsSync(path.join(root, p)));
check('ws.agents', agentsMissing.length === 0, `규칙 파일이 없다: ${agentsMissing.join(', ')}`);

// 4. 커밋 범위가 내는 대상 파일 수가 문서가 적은 43과 7인가
function rangeFiles(sub) {
  const r = git(['diff', '--name-only', '--diff-filter=ACMR', SPLIT, BASE, '--', sub]);
  return r.ok && r.out ? r.out.split('\n').map((s) => s.trim()).filter(Boolean) : [];
}
const prod = rangeFiles('backend/src/main/java');
const test = rangeFiles('backend/src/test/java');
const targets = [...prod, ...test];
check('ws.target-count', prod.length === EXPECT_PROD && test.length === EXPECT_TEST,
  `문서는 프로덕션 ${EXPECT_PROD} 테스트 ${EXPECT_TEST}인데 커밋 범위는 ${prod.length}과 ${test.length}이다`);

// 5. 그 파일들이 작업 트리에 실제로 있는가
const missing = targets.filter((p) => !fs.existsSync(path.join(root, p)));
const targetsPresent = check('ws.target-exists', targets.length > 0 && missing.length === 0,
  targets.length === 0
    ? '커밋 범위에서 대상 파일이 하나도 안 나왔다'
    : `평가 대상 ${targets.length}개 중 ${missing.length}개가 없다. 예: ${missing.slice(0, 3).join(', ')}`);

// 6과 7은 파일이 있어야 볼 수 있다. 없는 파일에 대고 돌리면 둘 다 조용히 통과한다
if (!targetsPresent) {
  skip('ws.target-drift', '대상 파일이 없어서 내용을 대조할 것이 없다');
  skip('ws.clean', '대상 파일이 없어서 볼 변경이 없다');
} else {
  // 6. 작업 트리의 내용이 기준 커밋 때와 같은가. 평가자는 커밋이 아니라 파일을 읽는다
  const drift = [];
  for (const p of targets) {
    const now = git(['hash-object', path.join(root, p)]);
    const then = git(['rev-parse', `${BASE}:${p}`]);
    if (now.ok && then.ok && now.out !== then.out) drift.push(p);
  }
  check('ws.target-drift', drift.length === 0,
    `기준 커밋 이후 내용이 바뀐 대상 파일 ${drift.length}개. 예: ${drift.slice(0, 3).join(', ')}`);

  // 7. 대상 파일에 미커밋 변경이 없는가
  const dirty = git(['status', '--porcelain', '--', ...targets]);
  check('ws.clean', dirty.ok && dirty.out === '',
    `대상 파일에 미커밋 변경이 있다\n${dirty.out}`);
}

// 8. 요청문이 적은 해시가 실제 파일과 같은가. HR2와 같은 이유다
const reqAbs = path.join(root, REQUEST_A);
if (!fs.existsSync(reqAbs)) {
  check('ws.doc-hash', false, `요청문이 없다: ${REQUEST_A}`);
} else {
  const rows = fs.readFileSync(reqAbs, 'utf8').split('\n');
  const bad = [];
  let seen = 0;
  for (const line of rows) {
    if (!line.trim().startsWith('|')) continue;
    const cells = line.split('|').map((c) => c.trim());
    const target = cells[2];
    const rec = (String(cells[3] || '').match(/sha256:([0-9a-f]{8,64})/) || [])[1];
    if (!rec || !target) continue;
    seen += 1;
    const abs = path.join(root, target);
    if (!fs.existsSync(abs)) bad.push(`${target} 없음`);
    else if (!sha256(abs).startsWith(rec)) bad.push(`${target} 기록 ${rec} 실제 ${sha256(abs).slice(0, rec.length)}`);
  }
  check('ws.doc-hash', seen > 0 && bad.length === 0,
    seen === 0 ? '요청문에서 해시 행을 못 찾았다' : `해시가 다른 자료 ${bad.length}건. ${bad.join(' / ')}`);
}

// 알림. 검사가 아니다. 사람이 지키는 규칙이라 여기서 막을 수 없다
if (fs.existsSync(path.join(root, BLIND))) {
  notes.push({ name: 'ws.blind', detail: `평가자가 읽으면 안 되는 파일이 이 폴더에 있다: ${BLIND}` });
}

report();
process.exit(failed.length ? 1 : 0);

function report() {
  const verdict = failed.length ? 'FAIL' : 'PASS';
  console.log(`${verdict} eval-workspace ${root.replace(/\\/g, '/')}`);
  const line = failed.length
    ? `  검사 ${passed.length}건 통과, ${failed.length}건 실패`
    : `  검사 ${passed.length}건 통과`;
  console.log(line);
  if (passed.length) console.log(`    ${passed.join(' ')}`);
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
