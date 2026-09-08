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
//   node harness/tools/check.mjs g1 <후보파일> --type doc|api|code [--end "<종료문장>"] [--artifact <경로>]
//   node harness/tools/check.mjs g2 <결정표파일>
//
// 실행 예시
//   node harness/tools/check.mjs fill harness/tasks/task-S9.md
//   node harness/tools/check.mjs g1 harness/out/task-S9-R1/candidate.md --type doc
//   node harness/tools/check.mjs g2 harness/decisions/task-S9-R1.md
//   node --test harness/tools/tests/check.test.mjs        골든 파일 테스트 29건
//
// 통과 출력 예시
//   PASS g1 harness/out/task-S9-R1/candidate.md
//     검사 8건 통과
//
// 실패 출력 예시 (종료 코드 1)
//   FAIL g1 harness/out/task-S9-R1/candidate.md
//     [doc.date-updated]  1  최종 갱신 줄이 없다 (F5)
//     [doc.no-bold]      42  제목 밖 볼드 **비관적 락** (F8)
//     [doc.end-sentence] 310  마지막 줄이 고정 종료 문장이 아니다 (F16)
//     검사 8건 중 3건 실패
//   이 세 줄을 그대로 재요청 프롬프트에 붙인다 (HR3).
//
// 쓰기 정책
//   g1과 g2는 아무 파일도 쓰지 않는다. 읽기만 한다.
//   fill만 쓴다. 대상은 인자로 받은 그 파일 하나뿐이다. 보호 경로는 거부한다.
//   이전 판이 검사 도중 document/o2o-*.md를 덮어써서 생긴 규칙이다.
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

const EM_DASH = '—';
const MIDDLE_DOT = '·';
const DEFAULT_END = 'Step {N} 산출물 제출. 다음 지시를 기다린다.';
const ID_RE = /S\d+-R\d+-[AB]-\d{2}/;

function rel(p) {
  return path.relative(ROOT, path.resolve(p)).split(path.sep).join('/');
}

function sha256(p) {
  return crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
}

function isProtected(p) {
  const r = path.relative(ROOT, path.resolve(p));
  return PROTECTED.some((d) => r === d || r.startsWith(d + path.sep));
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

class Report {
  constructor(cmd, file) { this.cmd = cmd; this.file = file; this.fails = []; this.count = 0; }
  check(name, line, ok, msg) {
    this.count++;
    if (!ok) this.fails.push({ name, line, msg });
  }
  print() {
    const f = rel(this.file);
    if (this.fails.length === 0) {
      console.log(`PASS ${this.cmd} ${f}`);
      console.log(`  검사 ${this.count}건 통과`);
      return 0;
    }
    console.log(`FAIL ${this.cmd} ${f}`);
    const w = Math.max(...this.fails.map((x) => x.name.length));
    for (const x of this.fails) {
      console.log(`  [${x.name}]${' '.repeat(w - x.name.length)} ${x.line}  ${x.msg}`);
    }
    console.log(`  검사 ${this.count}건 중 ${this.fails.length}건 실패`);
    return 1;
  }
}

// ---------- fill ----------
// 빈칸 잔존 0, 경로가 절대경로이고 존재, 버전 또는 해시 칸에 sha256 기입

export function fill(file, { dry = false } = {}) {
  const r = new Report('fill', file);
  if (isProtected(file)) {
    console.log(`FAIL fill ${rel(file)}`);
    console.log('  [fill.protected] 0  보호 경로다. document/, harness/project-sync/, harness/docs/에는 쓰지 않는다');
    return 1;
  }
  let text = fs.readFileSync(file, 'utf8');

  // 1. 버전 또는 해시 칸을 먼저 채운다. 같은 행의 절대경로에서 sha256을 계산한다
  const filled = [];
  const lines = text.split('\n');
  for (const t of parseTables(text)) {
    const header = t.rows[0];
    if (!header) continue;
    const pathCol = header.cells.findIndex((c) => c.includes('절대경로'));
    const hashCol = header.cells.findIndex((c) => c.includes('버전 또는 해시'));
    if (pathCol < 0 || hashCol < 0) continue;
    for (const row of t.rows.slice(1)) {
      const target = row.cells[pathCol];
      const hash = row.cells[hashCol];
      if (!target || !/^[A-Za-z]:\//.test(target)) continue;
      if (!fs.existsSync(target)) continue;
      if (hash && !/^\{\{.*\}\}$/.test(hash)) continue; // 이미 채워져 있으면 두지 않는다
      const digest = sha256(target);
      const idx = row.line - 1;
      const cells = lines[idx].trim().replace(/^\|/, '').replace(/\|$/, '').split('|');
      cells[hashCol] = ` sha256:${digest.slice(0, 16)} `;
      lines[idx] = '|' + cells.join('|') + '|';
      filled.push({ line: row.line, target: rel(target), digest: digest.slice(0, 16) });
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
    const pathCol = header.cells.findIndex((c) => c.includes('절대경로'));
    if (pathCol < 0) continue;
    for (const row of t.rows.slice(1)) {
      const v = row.cells[pathCol];
      if (!v || /^\{\{/.test(v) || v === '해당 없음') continue;
      const abs = /^[A-Za-z]:\//.test(v);
      r.check('fill.path-absolute', row.line, abs, `절대경로가 아니다: ${v}`);
      if (abs) r.check('fill.path-exists', row.line, fs.existsSync(v), `파일이 없다: ${v}`);
    }
  }

  const code = r.print();
  if (filled.length) {
    console.log(`  sha256 기입 ${filled.length}건${dry ? ' (dry, 저장 안 함)' : ''}`);
    for (const f of filled) console.log(`    ${f.line}  ${f.target} -> ${f.digest}`);
  }
  return code;
}

// ---------- g1 ----------

function checkLinks(r, text) {
  let staleOk = true;
  let existOk = true;
  text.split('\n').forEach((line, i) => {
    for (const m of line.matchAll(/\]\(([A-Za-z]:\/[^)]+)\)/g)) {
      const target = m[1].replace(/#.*$/, '');
      if (!fs.existsSync(target)) { existOk = false; r.check('link.exists', i + 1, false, `링크 대상이 없다: ${target}`); }
    }
    if (/\]\(claude\//.test(line)) { staleOk = false; r.check('link.stale', i + 1, false, '프로젝트 계열 상대경로 링크가 남아 있다 (claude/)'); }
  });
  if (existOk) r.check('link.exists', 0, true, '');
  if (staleOk) r.check('link.stale', 0, true, '');
}

function checkStyle(r, text) {
  const lines = stripFences(text);
  const seen = { emdash: true, middot: true, bold: true };
  lines.forEach((line, i) => {
    if (line.includes(EM_DASH)) { seen.emdash = false; r.check('doc.no-emdash', i + 1, false, '긴 줄표 사용 (F9)'); }
    if (line.includes(MIDDLE_DOT)) { seen.middot = false; r.check('doc.no-middot', i + 1, false, '가운뎃점 사용 (F9)'); }
    if (!/^\s*#/.test(line)) {
      const b = line.match(/\*\*[^*\n]+\*\*/);
      if (b) { seen.bold = false; r.check('doc.no-bold', i + 1, false, `제목 밖 볼드 ${b[0]} (F8)`); }
    }
  });
  if (seen.emdash) r.check('doc.no-emdash', 0, true, '');
  if (seen.middot) r.check('doc.no-middot', 0, true, '');
  if (seen.bold) r.check('doc.no-bold', 0, true, '');
}

export function g1(file, { type, end, artifacts = [] } = {}) {
  const r = new Report('g1', file);
  const text = fs.readFileSync(file, 'utf8');

  if (type === 'doc') {
    r.check('doc.date-created', 1, /^최초 작성:/m.test(text), '최초 작성 줄이 없다 (F5)');
    r.check('doc.date-updated', 1, /^최종 갱신:/m.test(text), '최종 갱신 줄이 없다 (F5)');
    checkStyle(r, text);
    checkLinks(r, text);
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
    checkLinks(r, text);
  } else if (type === 'code') {
    r.check('code.artifact-given', 0, artifacts.length > 0, '--artifact로 빌드와 테스트 결과 파일을 지정해야 한다');
    for (const a of artifacts) {
      const ok = fs.existsSync(a) && fs.statSync(a).size > 0;
      r.check('code.artifact-exists', 0, ok, `결과 파일이 없거나 비어 있다: ${a}`);
    }
  } else {
    console.error('사용법 오류: --type은 doc, api, code 중 하나');
    return 2;
  }
  return r.print();
}

// ---------- g2 ----------
// HR4의 항목을 기계로 검사한다. 사람 서명은 결정표의 G2 확인 절에 남긴다

const VERSION_LABELS = [
  '평가 대상 버전 또는 해시',
  '승인된 작업 계약 절대경로와 버전',
  'A 원본 리포트 절대경로와 버전 또는 해시',
  'B 원본 리포트 절대경로와 버전 또는 해시',
];

export function g2(file) {
  const r = new Report('g2', file);
  const text = fs.readFileSync(file, 'utf8');

  // 1. 버전 일치
  for (const label of VERSION_LABELS) {
    const row = lookup(text, label);
    r.check('g2.version-row', row ? row.line : 1, !!row, `${label} 행이 없다`);
    if (!row) continue;
    const p = (row.value.match(/[A-Za-z]:\/[^\s,)]+/) || [])[0];
    const h = (row.value.match(/sha256:([0-9a-f]{8,64})/) || [])[1];
    if (!p) { r.check('g2.version-path', row.line, false, `${label}에 절대경로가 없다`); continue; }
    if (!fs.existsSync(p)) { r.check('g2.version-path', row.line, false, `파일이 없다: ${p}`); continue; }
    if (!h) { r.check('g2.version-hash', row.line, false, `${label}에 sha256이 없다 (HR2)`); continue; }
    r.check('g2.version-match', row.line, sha256(p).startsWith(h), `해시 불일치: ${p}`);
  }

  // 2. 지적 ID 집합과 행 수
  const reportIds = new Set();
  for (const label of ['A 원본 리포트 절대경로와 버전 또는 해시', 'B 원본 리포트 절대경로와 버전 또는 해시']) {
    const row = lookup(text, label);
    if (!row) continue;
    const p = (row.value.match(/[A-Za-z]:\/[^\s,)]+/) || [])[0];
    if (p && fs.existsSync(p)) {
      for (const m of fs.readFileSync(p, 'utf8').matchAll(/S\d+-R\d+-[AB]-\d{2}/g)) reportIds.add(m[0]);
    }
  }

  const decisions = readDecisionRows(text);
  const tableIds = new Set(decisions.map((d) => d.id));
  const missing = [...reportIds].filter((x) => !tableIds.has(x));
  const extra = [...tableIds].filter((x) => !reportIds.has(x));
  r.check('g2.id-missing', 1, missing.length === 0, `원본에 있는데 결정표에 없다: ${missing.join(', ')}`);
  r.check('g2.id-extra', 1, extra.length === 0, `결정표에만 있다: ${extra.join(', ')}`);
  r.check('g2.id-duplicate', 1, tableIds.size === decisions.length, '결정표에 중복 지적 ID가 있다');

  const aCount = num(lookup(text, 'A 원본 지적 수'));
  const bCount = num(lookup(text, 'B 원본 지적 수'));
  const rowCount = num(lookup(text, '결정표 전체 행 수'));
  r.check('g2.row-count-declared', 1, rowCount === aCount + bCount,
    `선언한 행 수 ${rowCount}가 A ${aCount} 더하기 B ${bCount}와 다르다`);
  r.check('g2.row-count-actual', 1, decisions.length === rowCount,
    `실제 행 ${decisions.length}이 선언한 ${rowCount}과 다르다`);

  // 3. 결정 값과 이유
  const VALID = ['수용', '거부', '반박'];
  for (const d of decisions) {
    r.check('g2.decision-empty', d.line, d.decision !== '', `${d.id} 결정이 비어 있다`);
    if (d.decision !== '') {
      r.check('g2.decision-value', d.line, VALID.includes(d.decision),
        `${d.id} 결정 값이 수용, 거부, 반박이 아니다: ${d.decision}`);
    }
    if (d.decision === '거부') {
      r.check('g2.reject-reason', d.line, d.reason !== '', `${d.id} 거부에 이유가 없다`);
    }
    if (d.decision === '반박') {
      r.check('g2.rebut-severity', d.line, d.severity === '확인필요',
        `${d.id} 반박은 확인필요 등급에만 쓴다 (현재 ${d.severity})`);
    }
    if (d.decision === '거부' && d.severity === '치명') {
      const rec = findMisjudgeRecord(text, d.id);
      const need = ['근거', '대안', '안티패턴', '사용자 결정', '결정 날짜'];
      const lack = need.filter((k) => !rec || !rec[k] || /^\{\{/.test(rec[k]));
      r.check('g2.fatal-reject-record', d.line, lack.length === 0,
        `${d.id} 치명 거부의 오판 기록에 빠진 필드: ${lack.join(', ') || '기록 자체가 없다'}`);
    }
  }

  // 4. 남은 실제 치명
  const remaining = lookup(text, '남은 실제 치명 지적');
  r.check('g2.fatal-remaining', remaining ? remaining.line : 1,
    !!remaining && /^(없음|해당 없음)$/.test(remaining.value.trim()),
    `남은 실제 치명이 있다: ${remaining ? remaining.value : '행 자체가 없다'}`);

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
  for (const t of parseTables(text)) {
    const h = t.rows[0];
    if (!h) continue;
    const c = {
      id: h.cells.indexOf('지적 ID'),
      sev: h.cells.indexOf('심각도'),
      dec: h.cells.indexOf('결정'),
      why: h.cells.indexOf('이유'),
    };
    if (c.id < 0 || c.dec < 0) continue;
    for (const row of t.rows.slice(1)) {
      const id = row.cells[c.id] || '';
      if (!ID_RE.test(id)) continue;
      out.push({
        line: row.line,
        id,
        severity: c.sev >= 0 ? row.cells[c.sev] || '' : '',
        decision: c.dec >= 0 ? row.cells[c.dec] || '' : '',
        reason: c.why >= 0 ? row.cells[c.why] || '' : '',
      });
    }
  }
  return out;
}

// 치명 거부의 오판 판단 기록. 지적 ID가 들어 있는 두 칸 표를 찾는다
function findMisjudgeRecord(text, id) {
  for (const t of parseTables(text)) {
    const map = {};
    let hit = false;
    for (const row of t.rows) {
      if (row.cells.length < 2) continue;
      map[row.cells[0]] = row.cells[1];
      if (row.cells[1] && row.cells[1].includes(id)) hit = true;
    }
    if (hit) return map;
  }
  return null;
}

// ---------- CLI ----------

export function main(argv) {
  const [cmd, file, ...rest] = argv;
  if (!cmd || !file) {
    console.error('사용법: node harness/tools/check.mjs <fill|g1|g2> <파일> [옵션]');
    return 2;
  }
  if (!fs.existsSync(file)) { console.error(`파일이 없다: ${file}`); return 2; }
  const opt = { artifacts: [] };
  for (let i = 0; i < rest.length; i++) {
    if (rest[i] === '--type') opt.type = rest[++i];
    else if (rest[i] === '--end') opt.end = rest[++i];
    else if (rest[i] === '--artifact') opt.artifacts.push(rest[++i]);
    else if (rest[i] === '--dry') opt.dry = true;
  }
  if (cmd === 'fill') return fill(file, opt);
  if (cmd === 'g1') return g1(file, opt);
  if (cmd === 'g2') return g2(file);
  console.error(`알 수 없는 명령: ${cmd}`);
  return 2;
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.resolve(fileURLToPath(import.meta.url))) {
  process.exit(main(process.argv.slice(2)));
}
