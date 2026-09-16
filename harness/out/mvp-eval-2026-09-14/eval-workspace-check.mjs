// 평가 작업 공간 검사의 공통 몸통 (mvp-eval-2026-09-14 라운드. 쌍 다섯이 같이 쓴다)
//
// 왜 필요한가: 요청문 A와 B는 기준 커밋 1bdadfe와 목록 파일의 표를 가리킨다. 그런데 Codex가
// 실제로 여는 폴더가 그 커밋을 담고 있고 그 표의 파일이 그 해시 그대로 있는지는 아무도 보지
// 않는다. 2026-09-10에 어긋났다(재고와 요금 R1의 verify-eval-workspace.mjs 머리 주석). 그 스크립트를
// 본보기로 하되, 대상을 커밋 범위가 아니라 목록 파일의 표에서 읽는다. 이 라운드는 묶음 넷이 main에
// 섞여 들어 커밋 범위가 겹치기 때문이다(README 3절).
//
// LLM을 부르지 않는다. 로컬 검사만 한다. 어떤 파일도 고치지 않는다.
//
// 쌍마다의 스크립트(verify-eval-workspace.mjs)가 상수 넷을 주고 run을 부른다.
//   base       기준 커밋
//   listPath   목록 파일. 표의 첫 칸이 backend/로 시작하고 둘째 칸에 sha256이 있는 행이 대상이다
//   requests   요청문 A와 B. 둘째 칸이 경로이고 셋째 칸에 sha256이 있는 행을 대조한다
//   expect     목록 문서가 적은 수. 프로덕션, 테스트, 설정
//   blind      평가자가 읽으면 안 되는 파일과 폴더. 있으면 알림만 낸다
//
// 실행 예시
//   node harness/out/task-S9-catalog-R1/verify-eval-workspace.mjs
//   node harness/out/task-S9-catalog-R1/verify-eval-workspace.mjs C:/Dev/potenup/99_projects/o2o-dev
//
// 통과 출력 예시
//   PASS eval-workspace catalog C:/Dev/potenup/99_projects/o2o-dev
//     검사 9건 통과
//       ws.git ws.commit ws.agents ws.list ws.target-count ws.target-exists
//       ws.target-hash ws.target-drift ws.clean ws.doc-hash
//     알림 1건
//       ws.blind  평가자가 읽으면 안 되는 파일이 이 폴더에 있다: harness/out/task-S9-catalog-R1/step9-verification.md 외 3
//
// 실패 출력 예시 (종료 코드 1)
//   FAIL eval-workspace catalog C:/Dev/potenup/99_projects/o2o
//     검사 3건 통과, 3건 실패
//       ws.git ws.agents ws.list
//       ws.commit  기준 커밋 1bdadfe이 HEAD의 조상이 아니다. HEAD는 chore/context-budget 16bb437
//       ws.target-exists  평가 대상 58개 중 52개가 없다. 예: backend/src/main/java/com/o2o/catalog/api/CatalogController.java
//       ws.doc-hash  해시가 다른 자료 1건. harness/tasks/task-S9-catalog.md 기록 7d6cc521 실제 0a1b2c3d
//     범위 밖 3건
//       ws.target-hash  대상 파일이 없어서 해시를 대조할 것이 없다
//       ws.target-drift  대상 파일이 없어서 내용을 대조할 것이 없다
//       ws.clean  대상 파일이 없어서 볼 변경이 없다
//   범위 밖은 통과로 세지 않는다. 파일이 없으면 이 셋은 검사한 것이 아니라 안 돌린 것이다
//
// 통과가 뜻하는 것: 그 폴더를 Codex에 열면 요청문과 목록이 가리키는 파일이 적힌 해시 그대로 보인다.
// 통과가 뜻하지 않는 것: 평가자가 금지 파일을 안 읽는다는 보장. 그것은 사람이 지키는 규칙이라
// 스크립트가 확인할 수 없다. 그래서 알림으로만 낸다.

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFileSync } from 'node:child_process';

export function run({ pair, base, listPath, requests, expect, blind }) {
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
  const reach = git(['merge-base', '--is-ancestor', base, 'HEAD']);
  check('ws.commit', reach.ok, `기준 커밋 ${base.slice(0, 7)}이 HEAD의 조상이 아니다. HEAD는 ${branch} ${head}`);

  // 3. 평가자가 자동으로 읽는 규칙 파일
  const agentsMissing = ['AGENTS.md', 'backend/AGENTS.md'].filter((p) => !fs.existsSync(path.join(root, p)));
  check('ws.agents', agentsMissing.length === 0, `규칙 파일이 없다: ${agentsMissing.join(', ')}`);

  // 4. 목록 파일과 그 표. 첫 칸이 backend/로 시작하고 둘째 칸에 sha256이 있는 행
  const listAbs = path.join(root, listPath);
  const targets = []; // { rel, hash, kind }
  if (!fs.existsSync(listAbs)) {
    check('ws.list', false, `목록 파일이 없다: ${listPath}`);
  } else {
    for (const line of fs.readFileSync(listAbs, 'utf8').split('\n')) {
      if (!line.startsWith('|')) continue;
      const c = cells(line);
      const rel = c[1];
      const hash = (String(c[2] || '').match(/sha256:([0-9a-f]{8,64})/) || [])[1];
      if (!rel || !rel.startsWith('backend/') || !hash) continue;
      const kind = rel.startsWith('backend/src/test/java/') ? 'test' : rel.endsWith('.properties') ? 'config' : 'prod';
      targets.push({ rel, hash, kind });
    }
    check('ws.list', targets.length > 0, `목록 표에서 대상 행을 못 찾았다: ${listPath}`);
  }

  // 5. 표의 행 수가 목록 문서가 적은 수와 같은가
  const count = { prod: 0, test: 0, config: 0 };
  for (const t of targets) count[t.kind] += 1;
  check('ws.target-count',
    count.prod === expect.prod && count.test === expect.test && count.config === expect.config,
    `문서는 프로덕션 ${expect.prod} 테스트 ${expect.test} 설정 ${expect.config}인데 표는 ${count.prod}과 ${count.test}과 ${count.config}이다`);

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
      const then = git(['rev-parse', `${base}:${t.rel}`]);
      if (!now.ok || !then.ok || now.out !== then.out) drift.push(t.rel);
    }
    check('ws.target-drift', drift.length === 0,
      `기준 커밋 이후 내용이 바뀐 대상 파일 ${drift.length}개. 예: ${drift.slice(0, 3).join(', ')}`);

    // 9. 대상 파일에 미커밋 변경이 없는가
    const dirty = git(['status', '--porcelain', '--', ...targets.map((t) => t.rel)]);
    check('ws.clean', dirty.ok && dirty.out === '', `대상 파일에 미커밋 변경이 있다\n${dirty.out}`);
  }

  // 10. 요청문 A와 B가 적은 해시가 실제 파일과 같은가. HR2와 같은 이유다
  const bad = [];
  let seen = 0;
  const missingReq = [];
  for (const req of requests) {
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
      if (!rec || !target) continue;
      seen += 1;
      const t = path.join(root, target);
      if (!fs.existsSync(t)) bad.push(`${target} 없음`);
      else if (!sha256(t).startsWith(rec)) bad.push(`${target} 기록 ${rec.slice(0, 8)} 실제 ${sha256(t).slice(0, 8)}`);
    }
  }
  check('ws.doc-hash', missingReq.length === 0 && seen > 0 && bad.length === 0,
    missingReq.length ? `요청문이 없다: ${missingReq.join(', ')}`
      : seen === 0 ? '요청문에서 해시 행을 못 찾았다'
        : `해시가 다른 자료 ${bad.length}건. ${bad.join(' / ')}`);

  // 알림. 검사가 아니다. 사람이 지키는 규칙이라 여기서 막을 수 없다
  const present_blind = blind.filter((p) => fs.existsSync(path.join(root, p)));
  if (present_blind.length) {
    const more = present_blind.length > 1 ? ` 외 ${present_blind.length - 1}` : '';
    notes.push({ name: 'ws.blind', detail: `평가자가 읽으면 안 되는 파일이 이 폴더에 있다: ${present_blind[0]}${more}` });
  }

  report();
  process.exit(failed.length ? 1 : 0);

  function report() {
    const verdict = failed.length ? 'FAIL' : 'PASS';
    console.log(`${verdict} eval-workspace ${pair} ${root.replace(/\\/g, '/')}`);
    console.log(failed.length ? `  검사 ${passed.length}건 통과, ${failed.length}건 실패` : `  검사 ${passed.length}건 통과`);
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
}
