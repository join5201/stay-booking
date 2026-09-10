// 하네스 검사 스크립트
//
// 왜 필요한가: 양식의 버전 칸을 사람이 손으로 적으면 틀린다(HR2). G1 실패 목록도
// 사람 기억이 아니라 이 스크립트의 출력이 원천이다(HR3). G2의 일곱 항목은 눈으로
// 대조하면 반드시 새는데, 새는 쪽이 늘 자기 결정에 불리한 지적이다(HR4).
//
// LLM을 부르지 않는다. 로컬 검사만 한다.
//
// 명령 셋
//   node harness/tools/check.mjs fill <양식파일> [--dry]
//   node harness/tools/check.mjs g1 <후보파일> --type doc|api|code [--end "<문장>"] [--require "a,b"] [--artifact <경로>]
//   node harness/tools/check.mjs g2 <결정표파일> [--mode pre|final]
//   node harness/tools/check.mjs answer <답변파일> [--grade A|B|C]
//   node --test harness/tools/tests/check.test.mjs
//
// 실행 예시
//   node harness/tools/check.mjs fill harness/tasks/task-S8.md
//   node harness/tools/check.mjs g1 harness/out/task-S8-R1/candidate.md --type doc
//   node harness/tools/check.mjs g2 harness/decisions/task-S8-R1.md --mode pre
//   node harness/tools/check.mjs answer /tmp/answer.md
//   node harness/tools/check.mjs sweep harness/docs --type doc
//
// 통과 출력 예시
//   PASS g1 harness/out/task-S8-R1/candidate.md
//     검사 8건 통과
//       doc.date-created doc.date-updated doc.no-emdash doc.no-middot doc.no-bold
//       doc.end-sentence link.exists link.stale
//   이름을 다 적는 이유는 그 명령이 무엇을 보는지 실행 한 번으로 알기 위해서다 (10-14 2-3절 A2).
//
// 범위 밖 출력 예시
//   PASS g1 harness/docs/10-9-o2o-harness-answer-format-plan.md
//     검사 7건 통과
//       doc.date-created doc.date-updated doc.no-emdash doc.no-middot doc.no-bold link.exists link.stale
//     범위 밖 1건
//       doc.end-sentence  하네스 문서는 Step 산출물이 아니라 고정 종료 문장이 없다 (10-14 3절)
//   범위 밖은 통과로 세지 않는다. 검사를 안 돌린 것과 돌려서 통과한 것을 가른다 (10-14 2-3절 A4).
//
// 쓸기 출력 예시 (종료 코드 1)
//   FAIL g1 harness/docs/README.md
//     [doc.date-created] 1  최초 작성 줄이 없다 (F5)
//     [doc.date-updated] 1  최종 갱신 줄이 없다 (F5)
//     검사 7건 중 2건 실패
//   FAIL sweep harness/docs --type doc
//     문서 13개 중 12개 통과
//     실패 1개
//       harness/docs/README.md
//   통과한 파일은 이름도 안 찍는다. 스물일곱 개를 돌려도 읽히는 출력이어야 한다 (10-14 8-4절 D1).
//
// 실패 출력 예시 (종료 코드 1)
//   FAIL g2 harness/decisions/task-S8-R1.md
//     [g2.severity-preserved]  61  S9-R1-B-01 심각도가 원본과 다르다. 원본 치명, 결정표 보통
//     [g2.report-truncated]     1  A 리포트의 요약 합계 3과 보조 표 행 2가 다르다
//     검사 24건 중 2건 실패
//   이 줄들을 그대로 재요청 프롬프트에 붙인다 (HR3).
//
// g1 code 실행 예시 (2026-09-09 D-1 다)
//   node harness/tools/check.mjs g1 backend/src/test/java/com/o2o/catalog/domain/RoomTypeTest.java \
//     --type code --artifact backend/build/test-results/test/TEST-com.o2o.catalog.domain.RoomTypeTest.xml
//
// g1 code 통과 출력 예시
//   PASS g1 backend/.../RoomTypeTest.java
//     검사 6건 통과
//       code.artifact-given code.artifact-exists code.artifact-not-source
//       code.artifact-machine-readable code.tests-run code.tests-passed
//     테스트 4건, 실패 0, 오류 0, 건너뜀 0
//
// g1 code 실패 출력 예시 (종료 코드 1)
//   FAIL g1 backend/.../RoomTypeTest.java
//     [code.tests-passed]  0  실패 1건 오류 0건
//     검사 6건 중 1건 실패
//     테스트 4건, 실패 1, 오류 0, 건너뜀 0
//   결과 파일이 JUnit XML이 아니면 code.artifact-machine-readable이 먼저 걸린다
//
// answer 실패 출력 예시 (종료 코드 1)
//   FAIL answer /tmp/answer.md
//     [answer.result-section]  40  마지막 절 제목이 결과가 아니다 (R4). 실제: 다음 단계
//     검사 6건 중 1건 실패
//     등급 A (자동)
//   등급 줄이 뒤에 오는 것은 그것이 판정 결과지 실패가 아니기 때문이다
//
// 쓰기 정책
//   g1과 g2는 아무 파일도 쓰지 않는다. 읽기만 한다.
//   fill만 쓴다. 대상은 인자로 받은 그 파일 하나뿐이다. 보호 경로는 거부한다.
//   보호 경로 비교는 대소문자를 무시한다. Windows에서 HARNESS/docs로 우회되던 구멍이다(HRV-05).
//
// 2026-09-08 하네스 구현 리뷰(HRV-01부터 12) 반영
//   g2가 원본 리포트의 심각도와 스키마를 직접 읽는다. 결정표 값을 믿지 않는다(HRV-01, 02).
//   반영 전 검사와 최종 완료 검사를 --mode로 가른다(HRV-04).
//   fill이 기존 해시도 실제 바이트와 대조한다(HRV-06).
//   양식의 경로 행과 해시 행을 짝으로 읽고 파일 목록 전체를 대조한다(HRV-07).
//   오판 기록은 지정된 식별 필드에서만 ID를 읽는다(HRV-08).
//   g1 doc이 필수 항목과 상대 링크와 앵커까지 본다(HRV-09 문서분).
//
// 종료 코드: 0 통과, 1 검사 실패, 2 사용법 오류

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

// 저장소 루트를 스크립트 위치에서 유도한다. 절대경로를 박아 두면 디렉터리를 옮길 때마다 깨진다
const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, '..', '..');

// fill이 절대 쓰지 않는 곳. 원본과 이력이다
const PROTECTED = ['document', path.join('harness', 'project-sync'), path.join('harness', 'docs')];

// 경로 규약 (이슈 26). 양식의 경로 칸은 저장소 상대경로다. 절대경로도 읽지만 경고한다.
// 절대경로를 쓰면 그 값이 특정 폴더를 가리켜 worktree마다 다른 파일을 해시한다.
// 조용히 남의 파일을 해시하는 쪽이 못 찾는 쪽보다 나쁘다
const ABS_RE = /^[A-Za-z]:\//;

// 경로 칸의 값 하나를 실제 경로로 푼다. 두 번째 원소는 절대경로 표기 여부다
function resolvePath(v) {
  if (ABS_RE.test(v)) return [v, true];
  return [path.join(ROOT, v), false];
}

// 경로 칸에서 경로로 보이는 토큰을 뽑는다. 절대경로가 있으면 그것만, 없으면 상대경로를 본다
function pathTokens(v) {
  const abs = [...v.matchAll(/[A-Za-z]:\/[^\s,)]+/g)].map((m) => m[0]);
  if (abs.length) return abs;
  return [...v.matchAll(/(?:^|[\s,])((?:[\w.-]+\/)+[\w.-]+)/g)].map((m) => m[1]);
}

// 양식 대조 (이슈 29). 계약 머리의 양식 줄이 어느 양식의 어느 판을 따르는지 밝힌다.
// 양식이 오르면 이미 승인된 계약은 그 자리에 멈춘다. 승인은 그 시점 양식 기준이라
// 나중 절이 자동으로 붙지 않는다. task-S8이 v2에 멈춘 채 이틀 막혔다
const FORM_LINE = /^양식:\s*(\S+)\s+v(\d+)/m;
const FORM_VER = /^버전:\s*\S+\s+v(\d+)/m;
// 절 제목에서 번호와 괄호 주석을 떼고 뼈대만 남긴다. 계약은 번호를 붙이고 양식은 안 붙인다
function sectionKey(line) {
  return line.replace(/^#+\s*/, '').replace(/^[\d-]+\.\s*/, '').replace(/\s*\(.*$/, '').trim();
}
function sectionKeys(text) {
  return text.split('\n').filter((l) => /^## /.test(l)).map(sectionKey);
}

const EM_DASH = '—';
const MIDDLE_DOT = '·';
const DEFAULT_END = 'Step {N} 산출물 제출. 다음 지시를 기다린다.';
const ID_RE = /^S(\d+)-R(\d+)-([AB])-(\d{2})$/;
const SEVERITIES = ['치명', '보통', '확인필요'];
// 계약 시점에 아직 없는 대상을 가리키는 허용 값. 빈칸이 아니라 의도된 유예다
const DEFERRED = '생성 후 기입';
// 계약이 자기 해시를 적으면 항상 틀린다. 이 값 하나만 허용한다
const NO_SELF_HASH = '자기 해시 없음';

function rel(p) {
  return path.relative(ROOT, path.resolve(p)).split(path.sep).join('/');
}

function sha256(p) {
  return crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
}

// 폴더에 sha256을 부르면 EISDIR로 죽는다. existsSync는 폴더에도 참을 돌려준다
function isFile(p) {
  try { return fs.statSync(p).isFile(); } catch { return false; }
}

// 대소문자를 무시한다. Windows 파일시스템이 구분하지 않기 때문이다 (HRV-05)
function isProtected(p) {
  const r = path.relative(ROOT, path.resolve(p)).toLowerCase();
  return PROTECTED.some((d) => {
    const dl = d.toLowerCase();
    return r === dl || r.startsWith(dl + path.sep);
  });
}

// 코드 펜스를 지운다. 문체 규칙은 코드와 데이터에 적용하지 않는다 (F11)
function stripFences(text) {
  const out = [];
  let inFence = false;
  for (const line of text.split('\n')) {
    if (/^\s*```/.test(line)) { inFence = !inFence; out.push(''); continue; }
    out.push(inFence ? '' : line);
  }
  return out;
}

// 마크다운 표를 셀 배열로. 구분선은 버린다
function parseTables(text) {
  const tables = [];
  let cur = null;
  text.split('\n').forEach((line, i) => {
    if (/^\s*\|/.test(line)) {
      if (!cur) { cur = { rows: [] }; tables.push(cur); }
      if (/^\s*\|[\s|:-]+\|\s*$/.test(line)) return;
      cur.rows.push({
        line: i + 1,
        cells: line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map((c) => c.trim()),
      });
    } else if (line.trim() !== '') {
      cur = null;
    }
  });
  return tables;
}

// 두 칸짜리 항목표에서 값을 찾는다
function lookup(text, label) {
  for (const t of parseTables(text)) {
    for (const r of t.rows) {
      if (r.cells.length >= 2 && r.cells[0] === label) return { value: r.cells[1], line: r.line };
    }
  }
  return null;
}

// 헤더 이름으로 열을 찾은 표를 돌려준다
function findTableByHeaders(text, needed) {
  for (const t of parseTables(text)) {
    const h = t.rows[0];
    if (!h) continue;
    const idx = {};
    let ok = true;
    for (const [key, name] of Object.entries(needed)) {
      const i = h.cells.indexOf(name);
      if (i < 0) { ok = false; break; }
      idx[key] = i;
    }
    if (ok) return { table: t, idx };
  }
  return null;
}

// 적용 범위 선언 (10-14 2-3절 A4). promptfoo는 설정 파일이 어느 테스트에 어느 assert를
// 거는지 적어서 범위가 데이터다. 우리는 --type을 사람이 고르므로 경로로 성격을 못 박는다.
// 이것이 없으면 harness/docs/ 문서에 g1 doc을 돌렸을 때 종료 문장 검사가 전원 실패한다.
const SCOPE = [
  ['document/', 'step'],
  ['harness/out/', 'step'],
  ['harness/docs/', 'harness-doc'],
  ['harness/prompts/', 'harness-doc'],
];

// 성격별로 적용하지 않는 검사와 그 이유. 범위 밖은 통과가 아니라 따로 센다.
const OUT_OF_SCOPE = {
  'harness-doc': {
    'doc.end-sentence': '하네스 문서는 Step 산출물이 아니라 고정 종료 문장이 없다 (10-14 3절)',
  },
  'index': {
    'doc.end-sentence': 'README는 디렉터리 색인이지 Step 산출물이 아니다',
  },
};

// 저장소 밖 파일은 성격이 없다. 테스트가 임시 폴더에 쓰므로 그 경우 기존 동작을 그대로 둔다.
export function scopeKind(file) {
  const p = rel(file);
  // 저장소 밖은 성격이 없다. rel이 ../로 시작하거나 다른 드라이브면 절대경로를 돌려준다
  if (p.startsWith('..') || path.isAbsolute(p)) return null;
  // 이름이 앞자리보다 세다. README는 어디에 있든 색인이라 위치가 성격을 바꾸지 않는다
  if (p === 'README.md' || p.endsWith('/README.md')) return 'index';
  for (const [prefix, kind] of SCOPE) if (p.startsWith(prefix)) return kind;
  return null;
}

export function skipReason(file, id) {
  const kind = scopeKind(file);
  return (kind && OUT_OF_SCOPE[kind] && OUT_OF_SCOPE[kind][id]) || null;
}

// 목록이 길어지면 줄을 접는다. g2는 검사가 27건이라 한 줄에 다 넣으면 못 읽는다
function wrapNames(items, width = 96, indent = '    ') {
  const lines = [];
  let cur = '';
  for (const it of items) {
    if (cur && cur.length + 1 + it.length > width) { lines.push(cur); cur = it; }
    else cur = cur ? `${cur} ${it}` : it;
  }
  if (cur) lines.push(cur);
  return lines.map((l) => indent + l);
}

class Report {
  constructor(cmd, file) {
    this.cmd = cmd; this.file = file; this.fails = []; this.count = 0;
    this.names = []; this.skips = [];
  }
  check(name, line, ok, msg) {
    const why = skipReason(this.file, name);
    if (why) {
      if (!this.skips.some((s) => s.name === name)) this.skips.push({ name, why });
      return;
    }
    this.count++;
    if (!this.names.includes(name)) this.names.push(name);
    if (!ok) this.fails.push({ name, line, msg });
  }
  print(quiet = false) {
    const f = rel(this.file);
    if (this.fails.length === 0) {
      if (quiet) return 0;
      console.log(`PASS ${this.cmd} ${f}`);
      console.log(`  검사 ${this.count}건 통과`);
      for (const l of wrapNames(this.names)) console.log(l);
      this.printSkips();
      return 0;
    }
    console.log(`FAIL ${this.cmd} ${f}`);
    const w = Math.max(...this.fails.map((x) => x.name.length));
    for (const x of this.fails) {
      console.log(`  [${x.name}]${' '.repeat(w - x.name.length)} ${x.line}  ${x.msg}`);
    }
    console.log(`  검사 ${this.count}건 중 ${this.fails.length}건 실패`);
    this.printSkips();
    return 1;
  }

  printSkips() {
    if (this.skips.length === 0) return;
    console.log(`  범위 밖 ${this.skips.length}건`);
    for (const s of this.skips) console.log(`    ${s.name}  ${s.why}`);
  }
}

// ---------- fill ----------
// 빈칸 잔존 0, 경로가 절대경로이고 존재, 버전 또는 해시 칸에 sha256 기입과 대조

export function fill(file, { dry = false } = {}) {
  const r = new Report('fill', file);
  if (isProtected(file)) {
    console.log(`FAIL fill ${rel(file)}`);
    console.log('  [fill.protected] 0  보호 경로다. document/, harness/project-sync/, harness/docs/에는 쓰지 않는다');
    return 1;
  }
  let text = fs.readFileSync(file, 'utf8');

  // 1. 버전 또는 해시 칸. 비어 있으면 채우고, 값이 있으면 실제 바이트와 대조한다 (HRV-06)
  const filled = [];
  const deferred = [];
  const absPaths = new Set(); // 절대경로 표기를 쓴 줄. 실패가 아니라 경고다 (이슈 26)
  const lines = text.split('\n');
  for (const t of parseTables(text)) {
    const header = t.rows[0];
    if (!header) continue;
    const pathCol = header.cells.findIndex((c) => c.includes('경로'));
    const hashCol = header.cells.findIndex((c) => c.includes('버전 또는 해시'));
    if (pathCol < 0 || hashCol < 0) continue;
    for (const row of t.rows.slice(1)) {
      const target = row.cells[pathCol];
      const hash = row.cells[hashCol];
      if (!target || target === DEFERRED || /^\{\{/.test(target) || target === '해당 없음') continue;
      const [abs, wasAbs] = resolvePath(target);
      if (wasAbs) absPaths.add(row.line);
      if (!isFile(abs)) continue; // 없는 경로와 폴더는 아래 경로 검사가 보고한다
      // 자기 자신의 해시는 적는 순간 틀린다. 적으면 파일이 바뀌고 파일이 바뀌면 해시가 바뀐다.
      // 허용 값은 NO_SELF_HASH 하나다
      if (path.resolve(abs) === path.resolve(file)) {
        r.check('fill.self-hash', row.line, hash === NO_SELF_HASH,
          `이 파일이 자기 해시를 적으려 한다. 버전 칸을 ${NO_SELF_HASH}으로 두고 승인 커밋으로 가리킨다`);
        continue;
      }
      const digest = sha256(abs);
      const isBlank = !hash || /^\{\{.*\}\}$/.test(hash);
      if (!isBlank) {
        // 기록된 값을 건너뛰지 않는다. 입력이 바뀌면 실패시킨다
        const rec = (hash.match(/sha256:([0-9a-f]{8,64})/) || [])[1];
        r.check('fill.hash-recorded', row.line, !!rec, `버전 칸에 sha256이 없다: ${hash}`);
        if (rec) {
          r.check('fill.hash-stale', row.line, digest.startsWith(rec),
            `기록된 해시가 실제와 다르다: ${rel(abs)}. 기록 ${rec}, 실제 ${digest.slice(0, rec.length)}`);
        }
        continue;
      }
      const idx = row.line - 1;
      const cells = lines[idx].trim().replace(/^\|/, '').replace(/\|$/, '').split('|');
      cells[hashCol] = ` sha256:${digest.slice(0, 16)} `;
      lines[idx] = '|' + cells.join('|') + '|';
      filled.push({ line: row.line, target: rel(abs), digest: digest.slice(0, 16) });
    }
  }
  if (filled.length) {
    text = lines.join('\n');
    if (!dry) fs.writeFileSync(file, text, 'utf8');
  }

  // 2. 빈칸 잔존
  let placeholderOk = true;
  text.split('\n').forEach((line, i) => {
    const m = line.match(/\{\{[^}]*\}\}/g);
    if (m) { placeholderOk = false; r.check('fill.placeholder', i + 1, false, `빈칸 ${m.join(', ')} 남음`); }
  });
  if (placeholderOk) r.check('fill.placeholder', 0, true, '');

  // 3. 경로 칸의 절대경로 형식과 존재
  for (const t of parseTables(text)) {
    const header = t.rows[0];
    if (!header) continue;
    const pathCol = header.cells.findIndex((c) => c.includes('경로'));
    if (pathCol < 0) continue;
    for (const row of t.rows.slice(1)) {
      const v = row.cells[pathCol];
      if (!v || /^\{\{/.test(v) || v === '해당 없음') continue;
      if (v === DEFERRED) { deferred.push(row.line); continue; }
      const toks = pathTokens(v);
      r.check('fill.path-shape', row.line, toks.length > 0,
        `경로로 읽을 값이 없다. 저장소 상대경로를 적는다: ${v}`);
      for (const tok of toks) {
        if (/^[/\\]/.test(tok) || tok.split('/').includes('..')) {
          r.check('fill.path-shape', row.line, false, `저장소 상대경로가 아니다: ${tok}`);
          continue;
        }
        const [abs, wasAbs] = resolvePath(tok);
        if (wasAbs) absPaths.add(row.line);
        const exists = fs.existsSync(abs);
        r.check('fill.path-exists', row.line, exists, `파일이 없다: ${tok}`);
        if (exists) {
          r.check('fill.path-is-file', row.line, isFile(abs),
            `폴더는 해시를 계산할 수 없다. 파일을 적거나 생성 후 기입으로 두라: ${tok}`);
        }
      }
    }
  }

  // 4. 양식 대조 (이슈 29). 양식 줄이 있는 문서만 본다
  const formDrift = [];
  const fm = text.match(FORM_LINE);
  if (fm) {
    const formPath = path.join(ROOT, fm[1]);
    if (!isFile(formPath)) {
      r.check('fill.form-path', 1, false, `양식 줄이 가리키는 파일이 없다: ${fm[1]}`);
    } else {
      const formText = fs.readFileSync(formPath, 'utf8');
      const cur = (formText.match(FORM_VER) || [])[1];
      if (cur && cur !== fm[2]) formDrift.push(`선언 v${fm[2]}, 현재 v${cur}`);
      // 절 검사는 작업 계약에만 건다. 다른 양식은 인스턴스가 절을 접어 쓰는 경우가 있다
      if (/task-contract\.md$/.test(fm[1])) {
        const want = sectionKeys(formText);
        const have = sectionKeys(text);
        for (const w of want) {
          const ok = have.some((h) => h.includes(w) || w.includes(h));
          r.check('fill.form-sections', 1, ok,
            `양식 ${fm[1]}의 절이 이 문서에 없다: ${w}. 양식이 오른 뒤 계약을 안 따라 올린 것이다`);
        }
      }
    }
  }

  const code = r.print();
  if (formDrift.length) {
    console.log(`  경고. 양식 판이 다르다. ${formDrift.join(', ')}`);
    console.log('  승인은 그 시점 양식 기준이다. 새 절이 자동으로 붙지 않으니 훑어보라 (이슈 29)');
  }
  if (deferred.length) {
    console.log(`  생성 후 기입 ${deferred.length}건. 줄 ${deferred.join(', ')}`);
    console.log('  이 행이 남아 있는 동안에는 평가 요청 단계로 가지 않는다');
  }
  if (absPaths.size) {
    console.log(`  경고. 절대경로 표기 ${absPaths.size}줄. 줄 ${[...absPaths].sort((a, b) => a - b).join(', ')}`);
    console.log('  절대경로는 worktree마다 다른 파일을 가리킨다. 저장소 상대경로로 바꾸라 (이슈 26)');
  }
  if (filled.length) {
    console.log(`  sha256 기입 ${filled.length}건${dry ? ' (dry, 저장 안 함)' : ''}`);
    for (const f of filled) console.log(`    ${f.line}  ${f.target} -> ${f.digest}`);
  }
  return code;
}

// ---------- g1 ----------

// 링크 검사. 절대경로, 상대경로, 앵커까지 본다 (HRV-09 문서분)
function checkLinks(r, text, file) {
  const dir = path.dirname(path.resolve(file));
  let existOk = true;
  let staleOk = true;
  text.split('\n').forEach((line, i) => {
    for (const m of line.matchAll(/\]\(([^)]+)\)/g)) {
      const raw = m[1].trim();
      if (/^(https?:|mailto:)/.test(raw)) continue;
      if (raw.startsWith('#')) continue;
      const target = raw.replace(/#.*$/, '').replace(/:\d+$/, '');
      if (!target) continue;
      if (/\]\(claude\//.test(m[0])) { staleOk = false; r.check('link.stale', i + 1, false, '프로젝트 계열 상대경로 링크가 남아 있다 (claude/)'); continue; }
      const abs = /^[A-Za-z]:\//.test(target) ? target : path.resolve(dir, target);
      if (!fs.existsSync(abs)) { existOk = false; r.check('link.exists', i + 1, false, `링크 대상이 없다: ${target}`); }
    }
  });
  if (existOk) r.check('link.exists', 0, true, '');
  if (staleOk) r.check('link.stale', 0, true, '');
}

// prefix는 검사 ID의 앞부분이다. 문서는 doc, 답변은 answer를 쓴다.
// 같은 금지 기호를 두 벌 구현하면 한쪽만 고치는 사고가 난다
function checkStyle(r, text, prefix = 'doc') {
  const lines = stripFences(text);
  const seen = { emdash: true, middot: true, bold: true };
  lines.forEach((line, i) => {
    if (line.includes(EM_DASH)) { seen.emdash = false; r.check(`${prefix}.no-emdash`, i + 1, false, '긴 줄표 사용 (F9)'); }
    if (line.includes(MIDDLE_DOT)) { seen.middot = false; r.check(`${prefix}.no-middot`, i + 1, false, '가운뎃점 사용 (F9)'); }
    if (!/^\s*#/.test(line)) {
      const b = line.match(/\*\*[^*\n]+\*\*/);
      if (b) { seen.bold = false; r.check(`${prefix}.no-bold`, i + 1, false, `제목 밖 볼드 ${b[0]} (F8)`); }
    }
  });
  if (seen.emdash) r.check(`${prefix}.no-emdash`, 0, true, '');
  if (seen.middot) r.check(`${prefix}.no-middot`, 0, true, '');
  if (seen.bold) r.check(`${prefix}.no-bold`, 0, true, '');
}

// 결과 파일에서 실행 수와 실패 수와 오류 수와 건너뛴 수 넷을 읽는다 (D-1 다).
// JUnit XML만 읽는다. 다른 형식을 추측으로 읽으면 그 형식이 바뀌었을 때 조용히 통과시킨다.
// testsuite가 여럿이면 합산한다. Gradle이 테스트 클래스마다 파일 하나를 낸다.
function readTestCounts(file) {
  let text;
  try { text = fs.readFileSync(file, 'utf8'); } catch { return null; }
  const tags = text.match(/<testsuite\b[^>]*>/g);
  if (!tags) return null;
  const sum = { tests: 0, failures: 0, errors: 0, skipped: 0 };
  for (const tag of tags) {
    for (const k of Object.keys(sum)) {
      const m = tag.match(new RegExp(`\\b${k}="(\\d+)"`));
      if (m) sum[k] += Number(m[1]);
    }
  }
  return sum;
}

export function g1(file, { type, end, require: required = [], artifacts = [], quiet = false } = {}) {
  const r = new Report('g1', file);
  const text = fs.readFileSync(file, 'utf8');
  let countSummary = null;

  if (type === 'doc') {
    r.check('doc.date-created', 1, /^최초 작성:/m.test(text), '최초 작성 줄이 없다 (F5)');
    r.check('doc.date-updated', 1, /^최종 갱신:/m.test(text), '최종 갱신 줄이 없다 (F5)');
    // 승인 양식의 필수 항목 (HRV-09). 계약이 지정한 항목명을 --require로 받는다
    for (const item of required) {
      r.check('doc.required-item', 1, text.includes(item), `승인 양식의 필수 항목이 없다: ${item}`);
    }
    checkStyle(r, text);
    checkLinks(r, text, file);
    const nonEmpty = text.split('\n').filter((l) => l.trim() !== '');
    const last = (nonEmpty[nonEmpty.length - 1] || '').trim();
    const want = end || DEFAULT_END;
    const re = new RegExp('^' + want.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace('\\{N\\}', '\\d+') + '$');
    r.check('doc.end-sentence', text.split('\n').length, re.test(last),
      `마지막 줄이 고정 종료 문장이 아니다 (F16). 기대: ${want}`);
  } else if (type === 'api') {
    for (const sec of ['요청', '응답', '오류']) {
      r.check('api.section', 1, new RegExp('^#{1,6}\\s.*' + sec, 'm').test(text), `${sec} 절이 없다`);
    }
    checkStyle(r, text);
    checkLinks(r, text, file);
  } else if (type === 'code') {
    // 2026-09-09 task-S9-catalog 결정 D-1 다로 존재 확인에서 숫자 판정으로 올렸다.
    // 파일이 있다는 사실을 성공으로 읽지 않는다. 실패한 빌드 로그도 파일이기 때문이다.
    // 판정 범위는 계약 0-2절이 넷으로 한정했다. 실행 수, 실패 수, 오류 수, 건너뛴 수.
    r.check('code.artifact-given', 0, artifacts.length > 0, '--artifact로 빌드와 테스트 결과 파일을 지정해야 한다');
    let parsed = 0;
    const total = { tests: 0, failures: 0, errors: 0, skipped: 0 };
    for (const a of artifacts) {
      const ok = fs.existsSync(a) && fs.statSync(a).size > 0;
      r.check('code.artifact-exists', 0, ok, `결과 파일이 없거나 비어 있다: ${a}`);
      if (ok) {
        const insideRepo = !path.relative(ROOT, path.resolve(a)).startsWith('..');
        const isSource = /\.(mjs|js|ts|java|tsx|jsx)$/.test(a);
        r.check('code.artifact-not-source', 0, !(insideRepo && isSource),
          `소스 파일을 결과 파일로 지정했다: ${a}`);
        const counts = readTestCounts(a);
        if (counts) {
          parsed++;
          for (const k of Object.keys(total)) total[k] += counts[k];
        }
      }
    }
    // 로그 파일만 붙이고 기계 판독 결과를 빼면 숫자를 못 읽는다. 그 상태를 통과로 두면
    // D-1 다가 없던 일이 된다
    r.check('code.artifact-machine-readable', 0, parsed > 0,
      '기계 판독 결과 파일이 하나도 없다. JUnit XML을 --artifact로 지정한다 (D-1 다)');
    if (parsed > 0) {
      // 실행 수 0을 통과로 두면 테스트를 다 지워도 초록이 뜬다
      r.check('code.tests-run', 0, total.tests > 0, '실행된 테스트가 0건이다');
      r.check('code.tests-passed', 0, total.failures + total.errors === 0,
        `실패 ${total.failures}건 오류 ${total.errors}건`);
      countSummary = `  테스트 ${total.tests}건, 실패 ${total.failures}, 오류 ${total.errors}, 건너뜀 ${total.skipped}`;
    }
  } else {
    console.error('사용법 오류: --type은 doc, api, code 중 하나');
    return 2;
  }
  const rc = r.print(quiet);
  if (countSummary && !quiet) console.log(countSummary);
  return rc;
}

// ---------- 원본 리포트 파서 (HRV-01, 02) ----------
// 평가 리포트에서 지적 ID와 심각도를 직접 읽는다. 결정표의 값을 믿지 않는다.
// 스키마는 eval-criteria-ddd.md 4절이고 보조 표는 evaluate.md 5항이 요구한다.

// 디렉터리 하나를 g1으로 쓸고 한 줄로 요약한다 (10-14 8-4절 D1).
// 게이트를 부를 자리가 파일 하나씩이면 훅이나 CI가 부를 것이 없다.
export function sweep(dir, { type = 'doc', end, require: required = [] } = {}) {
  const files = fs.readdirSync(dir)
    .filter((n) => n.endsWith('.md'))
    .sort()
    .map((n) => path.join(dir, n));
  const fails = [];
  for (const p of files) {
    const code = g1(p, { type, end, require: required, quiet: true });
    if (code !== 0) fails.push(rel(p));
  }
  const pass = files.length - fails.length;
  console.log(`${fails.length ? 'FAIL' : 'PASS'} sweep ${rel(dir)} --type ${type}`);
  console.log(`  문서 ${files.length}개 중 ${pass}개 통과`);
  if (fails.length) {
    console.log(`  실패 ${fails.length}개`);
    for (const f of fails) console.log(`    ${f}`);
  }
  return fails.length ? 1 : 0;
}

export function parseReport(file) {
  const out = { file, ok: true, errors: [], rows: [], summary: null, detailCount: 0 };
  const text = fs.readFileSync(file, 'utf8');

  const m = text.match(/치명\s*(\d+)\s*\/\s*보통\s*(\d+)\s*\/\s*확인필요\s*(\d+)/);
  if (!m) {
    out.ok = false;
    out.errors.push('판정 요약 줄이 없다. 리포트가 잘렸거나 스키마를 따르지 않았다');
    return out;
  }
  out.summary = { 치명: Number(m[1]), 보통: Number(m[2]), 확인필요: Number(m[3]) };
  const total = out.summary.치명 + out.summary.보통 + out.summary.확인필요;

  const detail = findTableByHeaders(text, { sev: '심각도', loc: '위치' });
  out.detailCount = detail ? detail.table.rows.length - 1 : 0;

  const aux = findTableByHeaders(text, { num: '원본 번호', id: '지적 ID', sev: '심각도' });
  if (!aux) {
    if (total === 0) return out; // 정상 완료된 0건 리포트
    out.ok = false;
    out.errors.push('보조 표(원본 번호, 지적 ID, 심각도)가 없다. 지적을 ID로 추적할 수 없다');
    return out;
  }
  for (const row of aux.table.rows.slice(1)) {
    const id = row.cells[aux.idx.id];
    const sev = row.cells[aux.idx.sev];
    const num = row.cells[aux.idx.num];
    if (!ID_RE.test(id)) { out.ok = false; out.errors.push(`지적 ID 형식이 아니다: ${id}`); continue; }
    if (!SEVERITIES.includes(sev)) { out.ok = false; out.errors.push(`${id} 심각도 값이 셋 중 하나가 아니다: ${sev}`); continue; }
    out.rows.push({ id, severity: sev, num });
  }

  if (out.rows.length !== total) {
    out.ok = false;
    out.errors.push(`요약 합계 ${total}과 보조 표 행 ${out.rows.length}이 다르다. 잘렸거나 누락됐다`);
  }
  if (detail && out.detailCount !== total) {
    out.ok = false;
    out.errors.push(`요약 합계 ${total}과 상세 표 행 ${out.detailCount}이 다르다`);
  }
  const dist = { 치명: 0, 보통: 0, 확인필요: 0 };
  for (const x of out.rows) dist[x.severity] += 1;
  for (const s of SEVERITIES) {
    if (dist[s] !== out.summary[s]) {
      out.ok = false;
      out.errors.push(`${s} 수가 요약 ${out.summary[s]}과 보조 표 ${dist[s]}로 다르다`);
    }
  }
  return out;
}

// ---------- g2 ----------
// HR4의 항목을 기계로 검사한다. 사람 서명은 결정표의 G2 확인 절에 남긴다.
// mode가 pre면 반영 전 검사, final이면 최종 완료 검사다 (HRV-04)

const VERSION_SPECS = [
  { name: '평가 대상', pathLabel: '평가 대상 절대경로와 파일 목록', hashLabel: '평가 대상 버전 또는 해시' },
  { name: '작업 계약', hashLabel: '승인된 작업 계약 절대경로와 버전' },
  { name: 'A 리포트', hashLabel: 'A 원본 리포트 절대경로와 버전 또는 해시', role: 'A' },
  { name: 'B 리포트', hashLabel: 'B 원본 리포트 절대경로와 버전 또는 해시', role: 'B' },
];

function readSpec(text, spec) {
  const hashRow = lookup(text, spec.hashLabel);
  if (!hashRow) return null;
  const pathRow = spec.pathLabel ? lookup(text, spec.pathLabel) : hashRow;
  if (!pathRow) return null;
  const paths = pathTokens(pathRow.value);
  const hashes = [...hashRow.value.matchAll(/sha256:([0-9a-f]{8,64})/g)].map((x) => x[1]);
  return { paths, hashes, line: hashRow.line, pathLine: pathRow.line };
}

export function g2(file, { mode = 'pre' } = {}) {
  const r = new Report('g2', file);
  const text = fs.readFileSync(file, 'utf8');

  const declaredStep = (lookup(text, 'Step') || { value: '' }).value.match(/\d+/);
  const declaredRound = (lookup(text, '평가 라운드') || { value: '' }).value.match(/R(\d+)/);

  // 1. 버전 일치. 경로 행과 해시 행을 짝으로 읽고 파일 목록 전체를 대조한다 (HRV-07)
  const specs = {};
  for (const spec of VERSION_SPECS) {
    const got = readSpec(text, spec);
    r.check('g2.version-row', got ? got.line : 1, !!got, `${spec.hashLabel} 행이 없다`);
    if (!got) continue;
    specs[spec.name] = got;
    r.check('g2.version-count', got.pathLine, got.paths.length > 0 && got.paths.length === got.hashes.length,
      `${spec.name}의 경로 ${got.paths.length}개와 해시 ${got.hashes.length}개가 짝이 맞지 않는다`);
    got.paths.forEach((p, i) => {
      if (!fs.existsSync(p)) { r.check('g2.version-path', got.pathLine, false, `파일이 없다: ${p}`); return; }
      if (!isFile(p)) { r.check('g2.version-is-file', got.pathLine, false, `폴더는 해시를 계산할 수 없다: ${p}`); return; }
      const h = got.hashes[i];
      if (!h) { r.check('g2.version-hash', got.line, false, `${rel(p)}의 sha256이 없다 (HR2)`); return; }
      r.check('g2.version-match', got.line, sha256(p).startsWith(h), `해시 불일치: ${rel(p)}`);
    });
  }

  // 2. A와 B가 같은 파일이면 안 된다 (HRV-02)
  const aPath = specs['A 리포트'] && specs['A 리포트'].paths[0];
  const bPath = specs['B 리포트'] && specs['B 리포트'].paths[0];
  r.check('g2.ab-distinct', 1, !aPath || !bPath || path.resolve(aPath) !== path.resolve(bPath),
    'A와 B 리포트가 같은 파일이다. 두 평가자는 각각 새 작업에서 돈다');

  // 3. 원본 리포트를 스키마째 읽는다 (HRV-01, 02)
  const origin = new Map();
  for (const spec of VERSION_SPECS.filter((s) => s.role)) {
    const got = specs[spec.name];
    const p = got && got.paths[0];
    if (!p || !isFile(p)) { r.check('g2.report-readable', 1, false, `${spec.name}를 읽을 수 없다. 없는 경로이거나 폴더다`); continue; }
    const rep = parseReport(p);
    r.check('g2.report-schema', 1, rep.ok, `${spec.name} 스키마 위반: ${rep.errors.join(' / ')}`);
    for (const row of rep.rows) {
      const m = row.id.match(ID_RE);
      r.check('g2.report-role', 1, m[3] === spec.role, `${row.id}가 ${spec.name}에 있는데 역할 문자가 ${m[3]}다`);
      if (declaredStep) r.check('g2.report-step', 1, m[1] === declaredStep[0], `${row.id}의 Step이 결정표 선언 ${declaredStep[0]}과 다르다`);
      if (declaredRound) r.check('g2.report-round', 1, m[2] === declaredRound[1], `${row.id}의 라운드가 결정표 선언 R${declaredRound[1]}과 다르다`);
      origin.set(row.id, row);
    }
    const declared = num(lookup(text, `${spec.role} 원본 지적 수`));
    r.check('g2.report-count', 1, declared === rep.rows.length,
      `${spec.name}에 선언한 지적 수 ${declared}가 실제 ${rep.rows.length}과 다르다`);
  }

  // 4. 지적 ID 집합과 행 수
  const decisions = readDecisionRows(text);
  const tableIds = new Set(decisions.map((d) => d.id));
  const missing = [...origin.keys()].filter((x) => !tableIds.has(x));
  const extra = [...tableIds].filter((x) => !origin.has(x));
  r.check('g2.id-missing', 1, missing.length === 0, `원본에 있는데 결정표에 없다: ${missing.join(', ')}`);
  r.check('g2.id-extra', 1, extra.length === 0, `결정표에만 있다: ${extra.join(', ')}`);
  r.check('g2.id-duplicate', 1, tableIds.size === decisions.length, '결정표에 중복 지적 ID가 있다');

  const rowCount = num(lookup(text, '결정표 전체 행 수'));
  r.check('g2.row-count-declared', 1, rowCount === origin.size,
    `선언한 행 수 ${rowCount}가 원본 지적 합계 ${origin.size}과 다르다`);
  r.check('g2.row-count-actual', 1, decisions.length === rowCount,
    `실제 행 ${decisions.length}이 선언한 ${rowCount}과 다르다`);

  // 5. 결정 값과 이유. 심각도는 원본에서 읽는다 (HRV-01)
  const VALID = ['수용', '거부', '반박'];
  for (const d of decisions) {
    const src = origin.get(d.id);
    r.check('g2.severity-preserved', d.line, !!src && src.severity === d.severity,
      `${d.id} 심각도가 원본과 다르다. 원본 ${src ? src.severity : '없음'}, 결정표 ${d.severity}`);
    const sev = src ? src.severity : d.severity;
    r.check('g2.decision-empty', d.line, d.decision !== '', `${d.id} 결정이 비어 있다`);
    if (d.decision !== '') {
      r.check('g2.decision-value', d.line, VALID.includes(d.decision),
        `${d.id} 결정 값이 수용, 거부, 반박이 아니다: ${d.decision}`);
    }
    if (d.decision === '거부') {
      r.check('g2.reject-reason', d.line, d.reason !== '', `${d.id} 거부에 이유가 없다`);
    }
    if (d.decision === '반박') {
      r.check('g2.rebut-severity', d.line, sev === '확인필요',
        `${d.id} 반박은 확인필요 등급에만 쓴다. 원본 심각도는 ${sev}다`);
    }
    if (d.decision === '거부' && sev === '치명') {
      const rec = findMisjudgeRecord(text, d.id);
      const need = ['근거', '대안', '안티패턴', '사용자 결정', '결정 날짜'];
      const lack = need.filter((k) => !rec || !rec[k] || /^\{\{/.test(rec[k]));
      r.check('g2.fatal-reject-record', d.line, lack.length === 0,
        `${d.id} 치명 거부의 오판 기록에 빠진 필드: ${lack.join(', ') || '기록 자체가 없다'}`);
    }
  }

  // 6. 남은 실제 치명. 최종 완료 검사에서만 본다 (HRV-04)
  //    반영 전에는 수용한 치명이 아직 남아 있는 것이 정상이다
  if (mode === 'final') {
    const remaining = lookup(text, '남은 실제 치명 지적');
    r.check('g2.fatal-remaining', remaining ? remaining.line : 1,
      !!remaining && /^(없음|해당 없음)$/.test(remaining.value.trim()),
      `남은 실제 치명이 있다: ${remaining ? remaining.value : '행 자체가 없다'}`);
    const applied = lookup(text, '반영본 절대경로와 버전 또는 해시');
    r.check('g2.applied-recorded', applied ? applied.line : 1,
      !!applied && !/^\{\{/.test(applied.value.trim()) && applied.value.trim() !== '',
      '반영본 경로와 버전이 비어 있다');
  }

  return r.print();
}

function num(row) {
  if (!row) return NaN;
  const m = row.value.match(/\d+/);
  return m ? Number(m[0]) : NaN;
}

// 지적별 결정 표를 읽는다. 열 위치는 헤더 이름으로 찾는다
function readDecisionRows(text) {
  const out = [];
  const found = findTableByHeaders(text, { id: '지적 ID', dec: '결정' });
  if (!found) return out;
  const h = found.table.rows[0];
  const sevCol = h.cells.indexOf('심각도');
  const whyCol = h.cells.indexOf('이유');
  for (const row of found.table.rows.slice(1)) {
    const id = (row.cells[found.idx.id] || '').trim();
    if (!ID_RE.test(id)) continue;
    out.push({
      line: row.line,
      id,
      severity: sevCol >= 0 ? row.cells[sevCol] || '' : '',
      decision: row.cells[found.idx.dec] || '',
      reason: whyCol >= 0 ? row.cells[whyCol] || '' : '',
    });
  }
  return out;
}

// 치명 거부의 오판 판단 기록.
// 지정된 식별 필드에서만 ID를 읽는다. 근거나 대안에 남의 ID를 적어도 인정하지 않는다 (HRV-08)
const MISJUDGE_KEY = 'Step, 라운드, A/B, 지적 ID';

function findMisjudgeRecord(text, id) {
  for (const t of parseTables(text)) {
    const map = {};
    for (const row of t.rows) {
      if (row.cells.length < 2) continue;
      map[row.cells[0]] = row.cells[1];
    }
    const key = map[MISJUDGE_KEY];
    if (!key) continue;
    const ids = key.split(/[\s,]+/).filter(Boolean);
    if (ids.includes(id)) return map;
  }
  return null;
}

// ---------- answer ----------
// 답변 형식 R1부터 R4. 규격은 harness/prompts/answer-format.md, 근거는 harness/docs/10-9
//
// 왜 필요한가: 규칙만 있고 재는 장치가 없으면 두 답변 연속으로 샌다. 실제로 샜다.
// 다만 R1 무게중심과 R3 쉬운 말은 기계가 못 잰다. 여기서 재는 것은 R4 결과 절과,
// 결과 절의 행을 고정해 잴 수 있게 바꾼 R2뿐이다. 통과가 곧 품질은 아니다

const RESULT_ROWS = ['문제', '원인', '해결책', '파일 변경', '다음에 필요한 것'];
const OPTIONAL_RESULT_ROWS = ['문제', '원인', '해결책']; // 해당 없음을 쓸 수 있는 행
const NOT_APPLICABLE = '해당 없음';
const GRADE_C_LIMIT = 1200; // 실측 표본이 없어 정한 첫 판. 오탐이 나오면 조정한다

// 파일 경로를 언급했는가. URL은 먼저 지운다. 지우지 않으면 문서 링크가 경로로 잡힌다
function mentionsPath(text) {
  const t = text.replace(/https?:\/\/\S+/g, ' ');
  if (/[A-Za-z]:\/[\w./-]+/.test(t)) return true;
  if (/(?:^|[\s(`|,])[\w.-]+\/[\w./-]*\.\w{1,5}\b/.test(t)) return true;
  return /(?:^|[\s(`|,])[\w.-]+\/[\w.-]+\/[\w.-]*/.test(t);
}

function autoGrade(text) {
  if (mentionsPath(text)) return 'A';
  if (/^\s*#{0,6}\s*(Thought|Observation)\b/m.test(text)) return 'B';
  if (text.trim().length < GRADE_C_LIMIT) return 'C';
  return 'B';
}

// 마지막 절 제목. 코드 울타리 안의 제목은 세지 않는다
function lastHeading(text) {
  const lines = stripFences(text);
  for (let i = lines.length - 1; i >= 0; i--) {
    const m = lines[i].match(/^#{1,6}\s+(.+?)\s*$/);
    if (m) return { line: i + 1, title: m[1].trim() };
  }
  return null;
}

export function answer(file, { grade } = {}) {
  const r = new Report('answer', file);
  const text = fs.readFileSync(file, 'utf8');

  const g = grade || autoGrade(text);
  r.check('answer.grade', 1, ['A', 'B', 'C'].includes(g), `등급이 A, B, C 중 하나가 아니다: ${g}`);
  // 등급은 판정 결과지 실패가 아니다. 그래서 PASS 또는 FAIL 줄 뒤에 붙인다
  const finish = () => { const code = r.print(); console.log(`  등급 ${g}${grade ? ' (지정)' : ' (자동)'}`); return code; };

  checkStyle(r, text, 'answer');

  if (g === 'C') return finish(); // C 등급은 결과 절이 면제다. R3만 사람이 본다

  // R1. 첫 줄은 결론 한 문장이다. 제목이나 표나 목록으로 시작하면 과정부터 나열한 것이다
  const lines = stripFences(text);
  const firstIdx = lines.findIndex((l) => l.trim() !== '');
  const first = firstIdx < 0 ? '' : lines[firstIdx].trim();
  r.check('answer.lead', firstIdx + 1, first !== '' && !/^[#|>]/.test(first) && !/^[-*+]\s/.test(first),
    '첫 줄이 결론 문장이 아니다. 제목과 표와 목록은 실패 (R1)');

  // R4. 마지막 절 제목이 결과다
  const head = lastHeading(text);
  r.check('answer.result-section', head ? head.line : 1, !!head && head.title === '결과',
    `마지막 절 제목이 결과가 아니다 (R4). 실제: ${head ? head.title : '절 제목 없음'}`);
  if (!head || head.title !== '결과') return finish();

  // R2. 결과 절의 다섯 행. 자유 서술이면 못 재지만 행 이름이 고정이면 잰다
  const body = text.split('\n').slice(head.line).join('\n');
  const found = new Map();
  for (const t of parseTables(body)) {
    for (const row of t.rows) {
      if (row.cells.length >= 2) found.set(row.cells[0], { value: row.cells[1], line: head.line + row.line });
    }
  }
  for (const name of RESULT_ROWS) {
    r.check('answer.result-rows', head.line, found.has(name), `결과 절에 ${name} 행이 없다 (R2)`);
  }

  // 해당 없음은 쓸 수 있지만 왜 해당 없는지를 같은 칸에 적는다. 안 그러면 빈칸 채우기가 된다
  for (const name of OPTIONAL_RESULT_ROWS) {
    const cell = found.get(name);
    if (!cell) continue;
    const v = cell.value.trim();
    if (!v.startsWith(NOT_APPLICABLE)) continue;
    r.check('answer.result-empty', cell.line, v.length > NOT_APPLICABLE.length + 2,
      `${name} 행이 해당 없음뿐이다. 왜 해당 없는지를 같은 칸에 적는다 (R2)`);
  }

  return finish();
}

// ---------- CLI ----------

export function main(argv) {
  const [cmd, file, ...rest] = argv;
  if (!cmd || !file) {
    console.error('사용법: node harness/tools/check.mjs <fill|g1|g2|answer|sweep> <파일 또는 디렉터리> [옵션]');
    return 2;
  }
  if (!fs.existsSync(file)) { console.error(`파일이 없다: ${file}`); return 2; }
  const opt = { artifacts: [], require: [] };
  for (let i = 0; i < rest.length; i++) {
    if (rest[i] === '--type') opt.type = rest[++i];
    else if (rest[i] === '--end') opt.end = rest[++i];
    else if (rest[i] === '--require') opt.require = rest[++i].split(',').map((s) => s.trim()).filter(Boolean);
    else if (rest[i] === '--artifact') opt.artifacts.push(rest[++i]);
    else if (rest[i] === '--mode') opt.mode = rest[++i];
    else if (rest[i] === '--dry') opt.dry = true;
    else if (rest[i] === '--grade') opt.grade = rest[++i];
  }
  if (cmd === 'fill') return fill(file, opt);
  if (cmd === 'g1') return g1(file, opt);
  if (cmd === 'g2') return g2(file, opt);
  if (cmd === 'answer') return answer(file, opt);
  if (cmd === 'sweep') return sweep(file, opt);
  console.error(`알 수 없는 명령: ${cmd}`);
  return 2;
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.resolve(fileURLToPath(import.meta.url))) {
  process.exit(main(process.argv.slice(2)));
}
