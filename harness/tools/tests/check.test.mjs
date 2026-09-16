// 검사 스크립트의 골든 파일 테스트
//
// 왜 필요한가: 검사 스크립트가 조용히 통과시키면 G1과 G2가 있으나 마나다. 그 고장은
// 산출물이 아니라 검사기에 있어서 리포트만 보면 드러나지 않는다.
//
// 구조: fixtures/의 통과 파일이 골든이다. 실패 fixture는 골든에서 딱 한 군데를
// 바꿔 만든다. 그래야 그 검사가 그 차이 하나 때문에 걸렸다는 것이 증명된다.
//
// 골든 파일을 만드는 규칙 (2026-09-08 HRV-07): fixture는 harness/prompts/의 양식에서
// 만든다. 검사기에 맞춰 만들지 않는다. 그렇게 만들면 테스트가 코드의 거울이 되어
// 코드가 양식을 오해한 것까지 통과시킨다. 실제로 그 사고가 났다.
//
// 실행
//   node --test harness/tools/tests/check.test.mjs
//   디렉터리를 넘기면 Node 24가 모듈로 해석해서 실패한다. 파일을 직접 넘긴다.
//
// 실패 시 출력 예시
//   not ok 5 - g1 doc 볼드
//     error: 'Expected values to be strictly equal: 0 !== 1'

import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFileSync, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { fill, g1, g2, answer, sweep, numbers, state, stateRows, supersededSet, settings, ruleBody, scopeKind, skipReason, union, unionFiles, unionMarks } from '../check.mjs';
import { touchedNames } from '../numbers-gate.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const FIX = path.join(HERE, 'fixtures');
const ROOT = path.resolve(HERE, '..', '..', '..').split(path.sep).join('/');
const TMP = fs.mkdtempSync(path.join(os.tmpdir(), 'harness-check-'));
let seq = 0;

// 골든을 실행 환경에 맞게 치환하고 필요하면 한 군데를 바꾼 사본을 만든다
function prep(name, mutate) {
  let text = fs.readFileSync(path.join(FIX, name), 'utf8');
  text = text.replace(/__HASH:(.+?)__/g, (_, rp) =>
    crypto.createHash('sha256').update(fs.readFileSync(path.join(ROOT, rp))).digest('hex'));
  text = text.split('__ROOT__').join(ROOT);
  if (mutate) text = mutate(text);
  const dst = path.join(TMP, `${seq++}-${name}`);
  fs.writeFileSync(dst, text, 'utf8');
  return dst;
}

function run(fn) {
  const out = [];
  const log = console.log;
  const err = console.error;
  console.log = (...a) => out.push(a.join(' '));
  console.error = (...a) => out.push(a.join(' '));
  try {
    return { code: fn(), out: out.join('\n') };
  } finally {
    console.log = log;
    console.error = err;
  }
}

// ---------- fill ----------

test('fill 통과. sha256 두 건 기입', () => {
  const r = run(() => fill(prep('fill-pass.md')));
  assert.equal(r.code, 0);
  assert.match(r.out, /sha256 기입 2건/);
});

test('fill 실패. 빈칸 잔존', () => {
  const f = prep('fill-pass.md', (t) => t + '\n\n| 작업 유형 | {{설계 문서}} |\n');
  const r = run(() => fill(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.placeholder/);
});

// 이슈 26. 경로 칸은 저장소 상대경로다. 절대경로는 받아 주되 경고한다.
// 통과 케이스와 실패 케이스와 경고 케이스를 짝으로 붙인다. 금지만 테스트하면 가드가 허용 값까지 막는다
test('fill 통과. 저장소 상대경로', () => {
  const r = run(() => fill(prep('fill-pass.md'), { dry: true }));
  assert.equal(r.code, 0);
  assert.doesNotMatch(r.out, /절대경로 표기/);
});

test('fill 통과. 절대경로도 받지만 경고한다', () => {
  const f = prep('fill-pass.md', (t) => t.replace(/\| (document|harness)\//g, `| ${ROOT}/$1/`));
  const r = run(() => fill(f, { dry: true }));
  assert.equal(r.code, 0);
  assert.match(r.out, /절대경로 표기 2줄/);
});

test('fill 실패. 저장소 밖을 가리키는 상대경로', () => {
  const f = prep('fill-pass.md', (t) => t.replace('document/01-o2o-ddd-plan.md', '../document/01-o2o-ddd-plan.md'));
  const r = run(() => fill(f, { dry: true }));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.path-shape/);
});

test('fill 실패. 경로로 읽을 값이 없다', () => {
  const f = prep('fill-pass.md', (t) => t.replace('document/01-o2o-ddd-plan.md', '어디에도없음'));
  const r = run(() => fill(f, { dry: true }));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.path-shape/);
});

test('fill 실패. 파일이 없다', () => {
  const f = prep('fill-pass.md', (t) => t.replace('01-o2o-ddd-plan.md', '01-없는-파일.md'));
  const r = run(() => fill(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.path-exists/);
});

test('fill 실패. 보호 경로는 쓰지 않는다', () => {
  const r = run(() => fill(path.join(ROOT, 'document', 'README.md')));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.protected/);
});

// HRV-05
test('fill 실패. 보호 경로를 대소문자로 우회할 수 없다', () => {
  const r = run(() => fill(path.join(ROOT, 'DOCUMENT', 'README.md')));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.protected/);
});

// HRV-06
test('fill 실패. 기록된 해시가 실제와 다르면 잡는다', () => {
  const f = prep('fill-pass.md', (t) => t.replace('{{필수}}', 'sha256:0000000000000000'));
  const r = run(() => fill(f, { dry: true }));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.hash-stale/);
});

test('fill 실패. 버전 칸에 sha256이 아닌 값이 있으면 잡는다', () => {
  const f = prep('fill-pass.md', (t) => t.replace('{{필수}}', 'v3'));
  const r = run(() => fill(f, { dry: true }));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.hash-recorded/);
});

// 폴더 경로에서 크래시하던 것을 검사 실패로 바꿨다
test('fill 실패. 폴더 경로는 해시를 계산할 수 없다', () => {
  const f = prep('fill-pass.md', (t) => t.replace('document/01-o2o-ddd-plan.md', 'harness/prompts'));
  const r = run(() => fill(f, { dry: true }));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.path-is-file/);
});

test('fill 통과. 생성 후 기입은 빈칸이 아니라 유예다', () => {
  const f = prep('fill-pass.md', (t) =>
    t.replace('| 01 전체 | document/01-o2o-ddd-plan.md | {{필수}} | 전문 |',
      '| 평가 대상 | 생성 후 기입 | 생성 후 기입 | 전체 |'));
  const r = run(() => fill(f, { dry: true }));
  assert.equal(r.code, 0);
  assert.match(r.out, /생성 후 기입 1건/);
});

// 자기 해시는 fixture로 만들 수 없다. 파일이 자기 경로를 알아야 하는데
// 그 경로가 실행 시점에 정해지기 때문이다. 그래서 이 케이스만 테스트에서 조립한다.
test('fill 실패. 계약이 자기 해시를 적으려 하면 거부한다', () => {
  const dst = path.join(TMP, `${seq++}-self-hash.md`);
  const asPosix = dst.split(path.sep).join('/');
  fs.writeFileSync(dst, [
    '# 자기 해시 재현', '',
    '| 자료 | 절대경로 | 버전 또는 해시 | 읽을 범위 |',
    '|---|---|---|---|',
    `| 이 작업 계약 | ${asPosix} | {{fill}} | 전문 |`,
    '',
  ].join('\n'), 'utf8');
  const r = run(() => fill(dst, { dry: true }));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.self-hash/);
});

test('fill 통과. 자기 해시 없음은 허용 값이다', () => {
  const dst = path.join(TMP, `${seq++}-self-ok.md`);
  const asPosix = dst.split(path.sep).join('/');
  fs.writeFileSync(dst, [
    '# 자기 해시 허용 값', '',
    '| 자료 | 절대경로 | 버전 또는 해시 | 읽을 범위 |',
    '|---|---|---|---|',
    `| 이 작업 계약 | ${asPosix} | 자기 해시 없음 | 전문 |`,
    '',
  ].join('\n'), 'utf8');
  const r = run(() => fill(dst, { dry: true }));
  assert.equal(r.code, 0);
});

// ---------- 양식 대조 (이슈 29) ----------
// 양식이 오르면 이미 승인된 계약은 그 자리에 멈춘다. 승인은 그 시점 양식 기준이라
// 나중 절이 자동으로 붙지 않는다. task-S8이 v2에 멈춘 채 이틀 막혔다

test('fill 통과. 양식의 절을 다 갖추고 판도 같다', () => {
  const r = run(() => fill(prep('form-pass.md'), { dry: true }));
  assert.equal(r.code, 0);
  assert.doesNotMatch(r.out, /양식 판이 다르다/);
});

test('fill 실패. 양식의 절이 계약에 없다', () => {
  const f = prep('form-pass.md', (t) => t.replace('## A와 B 평가 허용 입력 (HR1)', '## 딴 절'));
  const r = run(() => fill(f, { dry: true }));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.form-sections/);
});

test('fill 통과하되 경고. 선언한 양식 판이 낡았다', () => {
  const f = prep('form-pass.md', (t) => t.replace('task-contract.md v6', 'task-contract.md v3'));
  const r = run(() => fill(f, { dry: true }));
  assert.equal(r.code, 0);
  assert.match(r.out, /양식 판이 다르다\. 선언 v3, 현재 v6/);
});

test('fill 실패. 양식 줄이 없는 파일을 가리킨다', () => {
  const f = prep('form-pass.md', (t) => t.replace('harness/prompts/task-contract.md', 'harness/prompts/없는양식.md'));
  const r = run(() => fill(f, { dry: true }));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.form-path/);
});

// ---------- g1 doc ----------

test('g1 doc 통과. 펜스 안 기호는 제외된다', () => {
  const r = run(() => g1(prep('g1-doc-pass.md'), { type: 'doc' }));
  assert.equal(r.code, 0);
});

const docCases = [
  ['최초 작성 줄 없음', (t) => t.replace(/^최초 작성:.*$/m, ''), /doc\.date-created/],
  ['최종 갱신 줄 없음', (t) => t.replace(/^최종 갱신:.*$/m, ''), /doc\.date-updated/],
  ['제목 밖 볼드', (t) => t.replace('본문이다.', '**본문이다.**'), /doc\.no-bold/],
  ['긴 줄표', (t) => t.replace('본문이다.', '본문이다 — 그렇다.'), /doc\.no-emdash/],
  ['가운뎃점', (t) => t.replace('본문이다.', '본문 · 이다.'), /doc\.no-middot/],
  ['종료 문장 없음', (t) => t.replace('Step 9 산출물 제출. 다음 지시를 기다린다.', '끝.'), /doc\.end-sentence/],
  ['절대 링크 대상 없음', (t) => t.replace('eval-criteria-ddd.md', 'eval-criteria-없음.md'), /link\.exists/],
  ['옛 상대경로 링크', (t) => t.replace('본문이다.', '본문이다. [옛 링크](claude/06-4.md)'), /link\.stale/],
  // HRV-09
  ['깨진 상대 링크', (t) => t.replace('본문이다.', '본문이다. [상대](./없는파일.md)'), /link\.exists/],
  // 이슈 93. 원인 셋을 각각 건다. 칸 모자람, 칸 넘침, 이스케이프 안 한 파이프
  ['표 칸이 모자람', (t) => t.replace('| --dry | 쓰지 않고 결과만 낸다 |', '| --dry 쓰지 않고 결과만 낸다 |'), /doc\.table-cells/],
  ['표 칸이 넘침', (t) => t.replace('| --dry | 쓰지 않고 결과만 낸다 |', '| --dry | 쓰지 않고 | 결과만 낸다 |'), /doc\.table-cells/],
  ['이스케이프 안 한 파이프', (t) => t.replace('doc\\|api\\|code', 'doc|api|code'), /doc\.table-cells/],
];

for (const [name, mutate, want] of docCases) {
  test(`g1 doc 실패. ${name}`, () => {
    const r = run(() => g1(prep('g1-doc-pass.md', mutate), { type: 'doc' }));
    assert.equal(r.code, 1);
    assert.match(r.out, want);
  });
}

// HRV-09
test('g1 doc 실패. 승인 양식의 필수 항목 누락', () => {
  const r = run(() => g1(prep('g1-doc-pass.md'), { type: 'doc', require: ['반영하지 않은 것'] }));
  assert.equal(r.code, 1);
  assert.match(r.out, /doc\.required-item/);
});

test('g1 doc 통과. 필수 항목이 있으면 통과', () => {
  const r = run(() => g1(prep('g1-doc-pass.md'), { type: 'doc', require: ['본문이다'] }));
  assert.equal(r.code, 0);
});

// ---------- g1 api, code ----------

test('g1 api 통과', () => {
  const r = run(() => g1(prep('g1-api-pass.md'), { type: 'api' }));
  assert.equal(r.code, 0);
});

test('g1 api 실패. 응답 절 없음', () => {
  const f = prep('g1-api-pass.md', (t) => t.replace('## 응답', '## 결과'));
  const r = run(() => g1(f, { type: 'api' }));
  assert.equal(r.code, 1);
  assert.match(r.out, /api\.section/);
});

// D-1 다 (2026-09-09). 존재 확인이 아니라 결과 파일의 숫자를 읽는다
const junitXml = (name, { tests, failures = 0, errors = 0, skipped = 0 }) =>
  `<?xml version="1.0" encoding="UTF-8"?>\n<testsuite name="${name}" tests="${tests}" `
  + `skipped="${skipped}" failures="${failures}" errors="${errors}"></testsuite>\n`;

test('g1 code 통과. 결과 파일의 실패 수가 0이다', () => {
  const a = path.join(TMP, 'TEST-pass.xml');
  fs.writeFileSync(a, junitXml('com.o2o.PassTest', { tests: 4 }));
  const r = run(() => g1(prep('g1-api-pass.md'), { type: 'code', artifacts: [a] }));
  assert.equal(r.code, 0);
  assert.match(r.out, /테스트 4건, 실패 0, 오류 0, 건너뜀 0/);
});

test('g1 code 실패. 결과 파일에 실패가 있다', () => {
  const a = path.join(TMP, 'TEST-fail.xml');
  fs.writeFileSync(a, junitXml('com.o2o.FailTest', { tests: 2, failures: 1 }));
  const r = run(() => g1(prep('g1-api-pass.md'), { type: 'code', artifacts: [a] }));
  assert.equal(r.code, 1);
  assert.match(r.out, /code\.tests-passed/);
});

test('g1 code 실패. 결과 파일에 오류가 있다', () => {
  const a = path.join(TMP, 'TEST-error.xml');
  fs.writeFileSync(a, junitXml('com.o2o.ErrorTest', { tests: 2, errors: 1 }));
  const r = run(() => g1(prep('g1-api-pass.md'), { type: 'code', artifacts: [a] }));
  assert.equal(r.code, 1);
  assert.match(r.out, /code\.tests-passed/);
});

test('g1 code 실패. 실행된 테스트가 0건이다', () => {
  const a = path.join(TMP, 'TEST-empty.xml');
  fs.writeFileSync(a, junitXml('com.o2o.EmptyTest', { tests: 0 }));
  const r = run(() => g1(prep('g1-api-pass.md'), { type: 'code', artifacts: [a] }));
  assert.equal(r.code, 1);
  assert.match(r.out, /code\.tests-run/);
});

test('g1 code 실패. 로그만 있고 기계 판독 결과가 없다', () => {
  const a = path.join(TMP, 'build.log');
  fs.writeFileSync(a, 'BUILD SUCCESSFUL\n');
  const r = run(() => g1(prep('g1-api-pass.md'), { type: 'code', artifacts: [a] }));
  assert.equal(r.code, 1);
  assert.match(r.out, /code\.artifact-machine-readable/);
});

test('g1 code 통과. 결과 파일 여럿을 합산한다', () => {
  const a1 = path.join(TMP, 'TEST-sum1.xml');
  const a2 = path.join(TMP, 'TEST-sum2.xml');
  fs.writeFileSync(a1, junitXml('com.o2o.OneTest', { tests: 3 }));
  fs.writeFileSync(a2, junitXml('com.o2o.TwoTest', { tests: 5, skipped: 1 }));
  const r = run(() => g1(prep('g1-api-pass.md'), { type: 'code', artifacts: [a1, a2] }));
  assert.equal(r.code, 0);
  assert.match(r.out, /테스트 8건, 실패 0, 오류 0, 건너뜀 1/);
});

test('g1 code 실패. 결과 파일이 없다', () => {
  const r = run(() => g1(prep('g1-api-pass.md'), { type: 'code', artifacts: [path.join(TMP, '없음.log')] }));
  assert.equal(r.code, 1);
  assert.match(r.out, /code\.artifact-exists/);
});

test('g1 code 실패. 결과 파일을 지정하지 않았다', () => {
  const r = run(() => g1(prep('g1-api-pass.md'), { type: 'code' }));
  assert.equal(r.code, 1);
  assert.match(r.out, /code\.artifact-given/);
});

// HRV-09
test('g1 code 실패. 소스 파일을 결과 파일로 지정', () => {
  const r = run(() => g1(prep('g1-api-pass.md'), { type: 'code', artifacts: [path.join(ROOT, 'harness/tools/check.mjs')] }));
  assert.equal(r.code, 1);
  assert.match(r.out, /code\.artifact-not-source/);
});

// ---------- g2 ----------

test('g2 통과. 실제 양식 구조로 HR4 항목 전부', () => {
  const r = run(() => g2(prep('g2-pass.md')));
  assert.equal(r.code, 0);
});

const g2Cases = [
  ['버전 해시 불일치', (t) => t.replace(/sha256:[0-9a-f]{64}/, 'sha256:' + '0'.repeat(64)), /g2\.version-match/],
  ['원본에 있는 지적이 결정표에 없다', (t) => t.replace(/^\| A \| 2 \| S9-R1-A-02 \|.*$\n/m, ''), /g2\.id-missing/],
  ['결정표에만 있는 지적', (t) => t.replace('| A | 2 | S9-R1-A-02 |', '| A | 2 | S9-R1-A-09 |'), /g2\.id-extra/],
  ['선언한 행 수가 원본 합계와 다르다', (t) => t.replace('| A 원본 지적 수 | 2 |', '| A 원본 지적 수 | 3 |'), /g2\.report-count/],
  ['빈 결정', (t) => t.replace('| 수용 | 반영한다 |', '|  | 반영한다 |'), /g2\.decision-empty/],
  ['거부에 이유가 없다', (t) => t.replace('| 거부 | 오판 기록 MJ-01 |', '| 거부 |  |'), /g2\.reject-reason/],
  ['치명 거부의 오판 기록에 근거가 없다', (t) => t.replace(/^\| 근거 \|.*$/m, '| 근거 | {{필수}} |'), /g2\.fatal-reject-record/],
  // HRV-01. 결정표의 심각도만 바꿔도 원본과 대조해 잡는다
  ['결정표 심각도 하향', (t) => t.replace('| S9-R1-B-01 | 치명 |', '| S9-R1-B-01 | 보통 |'), /g2\.severity-preserved/],
  ['치명에 반박', (t) => t.replace('| 거부 | 오판 기록 MJ-01 |', '| 반박 | 확인했다 |'), /g2\.rebut-severity/],
  // HRV-02
  ['A와 B가 같은 파일', (t) => t.replace('g2-report-B.md', 'g2-report-A.md'), /g2\.ab-distinct/],
  ['결정표 Step이 원본과 다르다', (t) => t.replace('| Step | 9 |', '| Step | 8 |'), /g2\.report-step/],
  ['결정표 라운드가 원본과 다르다', (t) => t.replace('| 평가 라운드 | R1 |', '| 평가 라운드 | R2 |'), /g2\.report-round/],
  // HRV-08
  ['남의 근거에 ID를 적어도 오판 기록으로 인정하지 않는다',
    (t) => t.replace('| Step, 라운드, A/B, 지적 ID | S9-R1-B-01 |', '| Step, 라운드, A/B, 지적 ID | S9-R1-A-01 |')
      .replace(/^\| 근거 \|(.*)$/m, '| 근거 | S9-R1-B-01 의 근거다$1 |'), /g2\.fatal-reject-record/],
];

for (const [name, mutate, want] of g2Cases) {
  test(`g2 실패. ${name}`, () => {
    const r = run(() => g2(prep('g2-pass.md', mutate)));
    assert.equal(r.code, 1);
    assert.match(r.out, want);
  });
}

// HRV-02. 원본 리포트가 잘리면 잡는다
test('g2 실패. 원본 리포트가 잘려 보조 표 행이 모자란다', () => {
  const cut = fs.readFileSync(path.join(FIX, 'g2-report-A.md'), 'utf8')
    .replace(/^\| 2 \| S9-R1-A-02 \|.*$\n/m, '');
  const cutPath = path.join(TMP, `${seq++}-cut-report-A.md`);
  fs.writeFileSync(cutPath, cut, 'utf8');
  const f = prep('g2-pass.md', (t) => t
    .replace(new RegExp(ROOT + '/harness/tools/tests/fixtures/g2-report-A\\.md', 'g'), cutPath.split(path.sep).join('/'))
    .replace(/A 원본 리포트 절대경로와 버전 또는 해시 \| ([^|]*)\|/, (mm, v) =>
      `A 원본 리포트 절대경로와 버전 또는 해시 | ${v.replace(/sha256:[0-9a-f]+/, 'sha256:' + crypto.createHash('sha256').update(cut).digest('hex'))}|`));
  const r = run(() => g2(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /g2\.report-schema/);
});

test('g2 실패. 리포트 경로가 폴더면 크래시하지 않고 잡는다', () => {
  const f = prep('g2-pass.md', (t) => t.replace(ROOT + '/harness/tools/tests/fixtures/g2-report-B.md', ROOT + '/harness/tools/tests/fixtures'));
  const r = run(() => g2(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /g2\.version-is-file/);
});

// HRV-04. 반영 전에는 치명이 남아 있어도 통과하고 최종에서만 막는다
test('g2 pre 통과. 수용한 치명이 남아 있어도 반영 전 검사는 통과', () => {
  const f = prep('g2-pass.md', (t) => t.replace('| 남은 실제 치명 지적 | 없음 |', '| 남은 실제 치명 지적 | S9-R1-B-01 |'));
  const r = run(() => g2(f, { mode: 'pre' }));
  assert.equal(r.code, 0);
});

test('g2 final 실패. 치명이 남으면 최종 완료 검사가 막는다', () => {
  const f = prep('g2-pass.md', (t) => t.replace('| 남은 실제 치명 지적 | 없음 |', '| 남은 실제 치명 지적 | S9-R1-B-01 |'));
  const r = run(() => g2(f, { mode: 'final' }));
  assert.equal(r.code, 1);
  assert.match(r.out, /g2\.fatal-remaining/);
});

test('g2 final 통과. 치명 0이고 반영본이 기록돼 있으면 통과', () => {
  const r = run(() => g2(prep('g2-pass.md'), { mode: 'final' }));
  assert.equal(r.code, 0);
});

// ---------- answer ----------
// 골든은 harness/prompts/answer-format.md 3절 골격에서 만든다. 검사기에 맞춰 만들지 않는다.
// 각 검사에 실패 케이스와 통과 케이스를 함께 붙인다. 금지만 테스트하면 가드가 허용 값까지 막는다

test('answer A 통과. 파일 경로를 쓴 답변은 자동으로 A', () => {
  const r = run(() => answer(prep('answer-a-pass.md')));
  assert.equal(r.code, 0);
  assert.match(r.out, /등급 A \(자동\)/);
});

test('answer B 통과. Thought와 Observation이 있으면 자동으로 B', () => {
  const r = run(() => answer(prep('answer-b-pass.md')));
  assert.equal(r.code, 0);
  assert.match(r.out, /등급 B \(자동\)/);
});

test('answer C 통과. 짧은 단답은 결과 절이 면제다', () => {
  const r = run(() => answer(prep('answer-c-pass.md')));
  assert.equal(r.code, 0);
  assert.match(r.out, /등급 C \(자동\)/);
});

test('answer 실패. 마지막 절 제목이 결과가 아니다', () => {
  const f = prep('answer-a-pass.md', (t) => t.replace('## 결과', '## 다음 단계'));
  const r = run(() => answer(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /answer\.result-section/);
});

test('answer 실패. 결과 절에 원인 행이 없다', () => {
  const f = prep('answer-a-pass.md', (t) => t.replace(/^\| 원인 \|.*$/m, ''));
  const r = run(() => answer(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /answer\.result-rows/);
});

test('answer 실패. 해당 없음만 적고 이유를 안 적었다', () => {
  const f = prep('answer-a-pass.md', (t) => t.replace(/^\| 문제 \|.*$/m, '| 문제 | 해당 없음 |'));
  const r = run(() => answer(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /answer\.result-empty/);
});

test('answer 통과. 해당 없음에 이유가 붙으면 통과한다', () => {
  const f = prep('answer-a-pass.md', (t) =>
    t.replace(/^\| 문제 \|.*$/m, '| 문제 | 해당 없음. 이번 턴은 상태 확인만 했고 고친 것이 없다 |'));
  const r = run(() => answer(f));
  assert.equal(r.code, 0);
});

test('answer 실패. 첫 줄이 결론 문장이 아니라 제목이다', () => {
  const f = prep('answer-a-pass.md', (t) => '# 작업 보고\n\n' + t);
  const r = run(() => answer(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /answer\.lead/);
});

test('answer 실패. 제목 밖 볼드', () => {
  const f = prep('answer-a-pass.md', (t) => t.replace('## 결과', '**중요**\n\n## 결과'));
  const r = run(() => answer(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /answer\.no-bold/);
});

test('answer 지정 등급이 자동 판정을 이긴다. C로 지정하면 결과 절을 안 본다', () => {
  const f = prep('answer-a-pass.md', (t) => t.replace('## 결과', '## 다음 단계'));
  const r = run(() => answer(f, { grade: 'C' }));
  assert.equal(r.code, 0);
  assert.match(r.out, /등급 C \(지정\)/);
});

// 자동 등급이 올라가는 경로. 짧다는 이유만으로 C가 되지 않는다는 것을 잰다
test('answer 실패. 짧아도 경로를 쓰면 A로 올라가 결과 절을 요구한다', () => {
  const f = prep('answer-c-pass.md', (t) => t + '\n\n값은 harness/prompts/answer-format.md에 있습니다.\n');
  const r = run(() => answer(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /등급 A \(자동\)/);
  assert.match(r.out, /answer\.result-section/);
});

// 울타리 안의 제목을 세면 결과 절이 없는 답변이 통과한다
test('answer 실패. 코드 울타리 안의 결과 제목은 절로 세지 않는다', () => {
  const f = prep('answer-a-pass.md', (t) => t.replace('## 결과', '## 다음 단계') + '\n```\n## 결과\n```\n');
  const r = run(() => answer(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /answer\.result-section/);
});

// ---------- 적용 범위 (10-14 2-3절 A4) ----------
// 왜 필요한가: 이 선언이 없으면 harness/docs/ 문서에 g1 doc을 돌렸을 때 종료 문장
// 검사가 전원 실패한다. 검사기의 범위 문제를 산출물의 결함으로 읽게 만드는 고장이다.
// 범위 밖은 통과와 다르게 센다. 검사를 안 돌린 것과 돌려서 통과한 것을 가르기 위해서다.

test('scopeKind. 경로 앞자리가 파일 성격을 정한다', () => {
  assert.equal(scopeKind(path.join(ROOT, 'document/01-x.md')), 'step');
  assert.equal(scopeKind(path.join(ROOT, 'harness/out/task-S8-R1/candidate.md')), 'step');
  assert.equal(scopeKind(path.join(ROOT, 'harness/docs/10-9-x.md')), 'harness-doc');
  assert.equal(scopeKind(path.join(ROOT, 'harness/prompts/generate.md')), 'harness-doc');
});

// 테스트는 임시 폴더에 fixture를 쓴다. 그 파일이 성격을 얻으면 기존 73건이 달라진다
test('scopeKind. 저장소 밖 파일은 성격이 없다', () => {
  assert.equal(scopeKind(path.join(TMP, 'x.md')), null);
});

test('skipReason. 하네스 문서의 종료 문장만 범위 밖이다', () => {
  assert.ok(skipReason(path.join(ROOT, 'harness/docs/10-9-x.md'), 'doc.end-sentence'));
  assert.equal(skipReason(path.join(ROOT, 'harness/out/r/candidate.md'), 'doc.end-sentence'), null);
  assert.equal(skipReason(path.join(ROOT, 'harness/docs/10-9-x.md'), 'doc.date-created'), null);
});

// 이슈 93. progress.md만 머리글이 자라서 옛 행의 칸이 모자란다. 다른 기록 파일은 그대로 본다
test('skipReason. 표 칸 수는 progress.md에서만 범위 밖이다', () => {
  assert.ok(skipReason(path.join(ROOT, 'harness/state/progress.md'), 'doc.table-cells'));
  assert.equal(skipReason(path.join(ROOT, 'harness/state/troubleshooting.md'), 'doc.table-cells'), null);
  assert.equal(skipReason(path.join(ROOT, 'harness/state/knowledge.md'), 'doc.table-cells'), null);
  assert.equal(skipReason(path.join(ROOT, 'harness/state/progress.md'), 'doc.date-created'), null);
});

test('g1 doc. 하네스 문서는 종료 문장을 범위 밖으로 센다', () => {
  const f = path.join(ROOT, 'harness/docs/10-9-o2o-harness-answer-format-plan.md');
  const r = run(() => g1(f, { type: 'doc' }));
  assert.match(r.out, /범위 밖 1건/);
  assert.doesNotMatch(r.out, /\[doc\.end-sentence\]/);
});

test('g1 doc. Step 산출물 자리에서는 종료 문장 검사가 살아 있다', () => {
  const f = path.join(ROOT, 'harness/out');
  assert.equal(skipReason(path.join(f, 'x/candidate.md'), 'doc.end-sentence'), null);
});

test('g1 doc. 통과 출력이 검사 이름을 다 적는다', () => {
  const r = run(() => g1(prep('g1-doc-pass.md'), { type: 'doc' }));
  assert.equal(r.code, 0);
  assert.match(r.out, /doc\.date-created/);
  assert.match(r.out, /doc\.end-sentence/);
  assert.doesNotMatch(r.out, /범위 밖/);
});

// ---------- 쓸기 진입점 (10-14 8-4절 D1) ----------
// 왜 필요한가: 게이트를 부를 자리가 파일 하나씩이면 훅도 CI도 부를 것이 없다.
// 쓸기는 한 번 돌려 한 줄로 답한다. 통과 파일의 상세는 안 찍고 실패만 찍는다.
// 그래야 스물일곱 개를 돌려도 읽을 수 있는 출력이 된다.

test('sweep. 디렉터리를 돌고 통과 수를 센다', () => {
  const dir = fs.mkdtempSync(path.join(TMP, 'sweep-pass-'));
  fs.copyFileSync(prep('g1-doc-pass.md'), path.join(dir, 'a.md'));
  fs.copyFileSync(prep('g1-doc-pass.md'), path.join(dir, 'b.md'));
  const r = run(() => sweep(dir, { type: 'doc' }));
  assert.equal(r.code, 0);
  assert.match(r.out, /문서 2개 중 2개 통과/);
});

test('sweep. 통과한 파일의 상세는 찍지 않는다', () => {
  const dir = fs.mkdtempSync(path.join(TMP, 'sweep-quiet-'));
  fs.copyFileSync(prep('g1-doc-pass.md'), path.join(dir, 'a.md'));
  const r = run(() => sweep(dir, { type: 'doc' }));
  assert.doesNotMatch(r.out, /검사 \d+건 통과/);
  assert.doesNotMatch(r.out, /doc\.date-created/);
});

test('sweep. 실패한 파일은 이름과 상세를 같이 찍고 종료 코드가 1이다', () => {
  const dir = fs.mkdtempSync(path.join(TMP, 'sweep-fail-'));
  fs.copyFileSync(prep('g1-doc-pass.md'), path.join(dir, 'a.md'));
  fs.copyFileSync(prep('g1-doc-pass.md', (t) => t.replace(/^최초 작성:.*$/m, '작성:')), path.join(dir, 'b.md'));
  const r = run(() => sweep(dir, { type: 'doc' }));
  assert.equal(r.code, 1);
  assert.match(r.out, /문서 2개 중 1개 통과/);
  assert.match(r.out, /실패 1개/);
  assert.match(r.out, /\[doc\.date-created\]/);
});

test('sweep. 마크다운이 아닌 파일은 세지 않는다', () => {
  const dir = fs.mkdtempSync(path.join(TMP, 'sweep-ext-'));
  fs.copyFileSync(prep('g1-doc-pass.md'), path.join(dir, 'a.md'));
  fs.writeFileSync(path.join(dir, 'b.txt'), '문서가 아니다');
  const r = run(() => sweep(dir, { type: 'doc' }));
  assert.match(r.out, /문서 1개 중 1개 통과/);
});

// ---------- 문서 번호 겹침 (이슈 75) ----------
// 왜 필요한가: 번호는 우리 파일명 규약이지 git이 아는 규칙이 아니다. 경로가 다르면
// git은 충돌 없이 둘 다 병합하므로 사람이나 검사기가 봐야 한다.
//
// 통과 케이스를 실패 케이스와 같이 붙인다. 10-17은 본문 하나에 그림 셋이 붙어
// 파일 넷이다. 파일 수로 세는 가드는 그 정상 배치를 겹침으로 막는다.
// 금지만 테스트하고 허용을 테스트하지 않아 가드가 허용 값까지 막은 적이 있다
// (progress.md 2026-09-08 16:17).

function numdir(tag, names) {
  const dir = fs.mkdtempSync(path.join(TMP, `numbers-${tag}-`));
  for (const n of names) fs.writeFileSync(path.join(dir, n), '');
  return dir;
}

test('numbers 통과. 번호가 다 다르다', () => {
  const r = run(() => numbers(numdir('pass', ['10-4-a.md', '10-5-b.md', '10-6-c.md'])));
  assert.equal(r.code, 0);
  assert.match(r.out, /번호 3개, 파일 3개/);
});

test('numbers 통과. 본문 하나에 그림 셋이 붙은 배치 (10-17)', () => {
  const r = run(() => numbers(numdir('fig', [
    '10-17-o2o-harness-overview.md',
    '10-17-o2o-harness-overview-fig1.svg',
    '10-17-o2o-harness-overview-fig2.svg',
    '10-17-o2o-harness-overview-fig3.svg',
  ])));
  assert.equal(r.code, 0);
  assert.match(r.out, /번호 1개, 파일 4개/);
});

test('numbers 통과. 번호가 없는 파일은 번호를 주장하지 않는다', () => {
  const r = run(() => numbers(numdir('readme', ['README.md', '10-4-a.md'])));
  assert.equal(r.code, 0);
  assert.match(r.out, /번호 1개, 파일 1개/);
});

// 10-4와 10-14는 다른 번호다. 접두를 자리수로 자르면 둘이 한 번호가 된다
test('numbers 통과. 10-4와 10-14를 가른다', () => {
  const r = run(() => numbers(numdir('prefix', ['10-4-a.md', '10-14-b.md'])));
  assert.equal(r.code, 0);
  assert.match(r.out, /번호 2개, 파일 2개/);
});

test('numbers 실패. 같은 번호를 문서 둘이 쓴다', () => {
  const r = run(() => numbers(numdir('dup', ['10-15-ondemand.md', '10-15-fix-plan.md'])));
  assert.equal(r.code, 1);
  assert.match(r.out, /\[numbers\.duplicate\]/);
  assert.match(r.out, /10-15-fix-plan\.md, 10-15-ondemand\.md/);
});

test('numbers 실패. 붙을 본문이 없는 첨부가 번호를 차지한다', () => {
  const r = run(() => numbers(numdir('orphan', ['10-18-b-fig1.svg'])));
  assert.equal(r.code, 1);
  assert.match(r.out, /\[numbers\.attachment\]/);
});

// 그림이 본문과 같은 번호를 쓰면서 이름은 다른 본문에서 딴 경우다. 번호만 맞추면 붙는 게 아니다
test('numbers 실패. 첨부 이름이 본문에서 시작하지 않는다', () => {
  const r = run(() => numbers(numdir('mismatch', ['10-17-overview.md', '10-17-recheck-fig1.svg'])));
  assert.equal(r.code, 1);
  assert.match(r.out, /\[numbers\.attachment\]/);
});

test('numbers. 디렉터리가 아니면 사용법 오류다', () => {
  const dir = numdir('notdir', ['10-4-a.md']);
  const r = run(() => numbers(path.join(dir, '10-4-a.md')));
  assert.equal(r.code, 2);
});

// 로컬 트리 회귀 가드. 이 테스트가 깨지면 산출물이 진짜로 겹친 것이다
test('numbers. 지금의 harness/docs가 통과한다', () => {
  const r = run(() => numbers(path.join(ROOT, 'harness', 'docs')));
  assert.equal(r.code, 0);
});

// ---------- 문서 번호 origin/main 대조 (이슈 75) ----------
// 왜 필요한가: 로컬 트리만 보면 남이 먼저 가져간 번호가 초록으로 나온다. 그 초록이
// 이 결함의 원래 모양이다. 실제로 이 작업 중에 다른 세션이 10-18을 origin/main에
// 올렸고 이 브랜치에는 그 파일이 없었다.
//
// 원격 없이 refs/remotes/origin/main을 직접 박아 기본 ref 경로를 그대로 시험한다.
// --ref로 바꿔 시험하면 기본값이 도는지를 못 본다.

function gitrepo(tag, committed, working) {
  const root = fs.mkdtempSync(path.join(TMP, `numbers-git-${tag}-`));
  const docs = path.join(root, 'docs');
  fs.mkdirSync(docs);
  const g = (...a) => execFileSync('git', a, { cwd: root, stdio: 'ignore' });
  g('init', '-q');
  g('config', 'user.email', 'test@example.com');
  g('config', 'user.name', 'test');
  g('config', 'commit.gpgsign', 'false');
  for (const n of committed) fs.writeFileSync(path.join(docs, n), '');
  // 경로를 명시한다. 임시 저장소라도 add -A를 쓰지 않는다 (CLAUDE.md 4-1)
  g('add', ...committed.map((n) => `docs/${n}`));
  g('commit', '-q', '-m', 'seed');
  const sha = execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim();
  g('update-ref', 'refs/remotes/origin/main', sha);
  for (const n of committed) if (!working.includes(n)) fs.unlinkSync(path.join(docs, n));
  for (const n of working) fs.writeFileSync(path.join(docs, n), '');
  return docs;
}

test('numbers 실패. origin/main이 먼저 가져간 번호가 겹친다', () => {
  const r = run(() => numbers(gitrepo('taken', ['10-15-ondemand.md'], ['10-15-fix-plan.md'])));
  assert.equal(r.code, 1);
  assert.match(r.out, /\[numbers\.duplicate\]/);
  // 어느 쪽에만 있는지를 적어야 누가 양보하는지가 보인다
  assert.match(r.out, /10-15-fix-plan\.md\(로컬만\)/);
  assert.match(r.out, /10-15-ondemand\.md\(origin\/main만\)/);
});

test('numbers 통과. 양쪽에 있는 같은 파일은 겹침이 아니다', () => {
  const r = run(() => numbers(gitrepo('same', ['10-15-a.md'], ['10-15-a.md'])));
  assert.equal(r.code, 0);
  assert.match(r.out, /대조 origin\/main [0-9a-f]+/);
  assert.match(r.out, /로컬만 0개, origin\/main만 0개/);
});

// 남이 먼저 가져간 번호가 내게 없는 것은 겹침이 아니라 내가 뒤처진 것이다.
// 실패로 두면 브랜치를 팔 때마다 빨간불이 뜬다
test('numbers 통과. origin/main에만 있는 번호는 겹침이 아니다', () => {
  const r = run(() => numbers(gitrepo('behind', ['10-16-a.md'], ['10-15-b.md'])));
  assert.equal(r.code, 0);
  assert.match(r.out, /로컬만 1개, origin\/main만 1개/);
});

test('numbers 통과. 그림 첨부는 양쪽에 갈려 있어도 본문에 붙는다', () => {
  const r = run(() => numbers(gitrepo('figsplit',
    ['10-17-overview.md'], ['10-17-overview-fig1.svg'])));
  assert.equal(r.code, 0);
});

test('numbers 실패. ref를 읽을 수 없으면 통과로 두지 않는다', () => {
  const docs = gitrepo('noref', ['10-15-a.md'], ['10-15-a.md']);
  const r = run(() => numbers(docs, { ref: 'origin/nope' }));
  assert.equal(r.code, 1);
  assert.match(r.out, /\[numbers\.ref\]/);
  assert.match(r.out, /--local-only/);
});

// --local-only는 대조를 끄는 값이다. 끄면 위의 taken 케이스가 초록으로 나온다.
// 그 초록이 이 결함의 원래 모양이라 한계를 출력에 적는다
test('numbers. --local-only는 대조를 끄고 한계를 적는다', () => {
  const r = run(() => numbers(gitrepo('localonly', ['10-15-ondemand.md'], ['10-15-fix-plan.md']),
    { localOnly: true }));
  assert.equal(r.code, 0);
  assert.match(r.out, /로컬 트리만 봤다/);
  assert.doesNotMatch(r.out, /numbers\.ref/);
});

// git 저장소가 아니면 대조할 ref 자체가 없다. 실패가 아니라 범위 밖이다.
// 범위 밖은 통과로 세지 않으므로 안 돌린 것과 돌려서 통과한 것이 갈린다
test('numbers. git 저장소가 아니면 대조를 범위 밖으로 센다', () => {
  const r = run(() => numbers(numdir('norepo', ['10-4-a.md'])));
  assert.equal(r.code, 0);
  assert.match(r.out, /범위 밖 1건/);
  assert.match(r.out, /numbers\.ref/);
});

// ---------- 문서 번호 게이트 (PostToolUse 훅) ----------
// 왜 필요한가: 검사가 있다는 것과 검사가 무언가를 막는다는 것은 다르다. 사람이 손으로
// 쳐야 도는 검사는 바쁠 때 안 돌고, 안 돈 것과 돌아서 통과한 것을 구별할 수 없다
// (10-14 8-2절). 이 게이트가 파일을 만드는 순간에 부른다.
//
// 게이트가 고장 나면 세션이 잠긴다. 그래서 막는 경우보다 안 막는 경우를 더 많이 본다.

const GATE = path.join(HERE, '..', 'numbers-gate.mjs');

// 차단 횟수 기록은 os.tmpdir()에 남아 실행 사이에 살아남는다. 세션 id를 고정하면
// 두 번째 실행부터 한도를 이미 쓴 상태로 시작해 테스트가 들쭉날쭉해진다.
// 실제 세션도 매번 다른 id를 받으므로 이쪽이 현실에 가깝다
const sid = (tag) => `${tag}-${Date.now()}-${seq++}`;

// 훅은 stdin JSON을 주고 종료 코드로 답한다. 자식 프로세스로 돌려야 그 계약을 시험한다.
// spawnSync를 쓴다. execFileSync는 성공한 실행의 stderr를 돌려주지 않는데 이 게이트는
// 안 막을 때도 stderr로 알린다. 그 줄을 놓치면 알림이 없는 것과 구별이 안 된다
function gate(input, env = {}) {
  const r = spawnSync('node', [GATE], {
    input: typeof input === 'string' ? input : JSON.stringify(input),
    env: { ...process.env, ...env },
    encoding: 'utf8',
  });
  return { code: r.status, out: String(r.stdout || '') + String(r.stderr || '') };
}

// 겹침이 있는 임시 저장소. 로컬에 10-18-mine.md, origin/main에 10-18-rollup.md
function gaterepo(tag) {
  const root = fs.mkdtempSync(path.join(TMP, `gate-${tag}-`));
  const docs = path.join(root, 'docs');
  fs.mkdirSync(docs);
  const g = (...a) => execFileSync('git', a, { cwd: root, stdio: 'ignore' });
  g('init', '-q');
  g('config', 'user.email', 'test@example.com');
  g('config', 'user.name', 'test');
  g('config', 'commit.gpgsign', 'false');
  fs.writeFileSync(path.join(docs, '10-18-rollup.md'), '');
  g('add', 'docs/10-18-rollup.md');
  g('commit', '-q', '-m', 'seed');
  const sha = execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim();
  g('update-ref', 'refs/remotes/origin/main', sha);
  fs.unlinkSync(path.join(docs, '10-18-rollup.md'));
  fs.writeFileSync(path.join(docs, '10-18-mine.md'), '');
  return docs;
}

test('게이트. harness/docs 아래 쓰기에서 파일 이름을 뽑는다', () => {
  assert.deepEqual(
    touchedNames({ tool_input: { file_path: 'harness/docs/10-19-a.md' } }), ['10-19-a.md']);
});

test('게이트. 다른 경로의 쓰기는 뽑지 않는다', () => {
  assert.deepEqual(touchedNames({ tool_input: { file_path: 'backend/src/Main.java' } }), []);
  assert.deepEqual(touchedNames({ tool_input: { file_path: 'document/06-2-o2o-aggregates.md' } }), []);
});

// 이름을 바꾸는 일은 git mv로 한다. Write만 보면 16bb437 같은 변경을 놓친다
test('게이트. Bash 명령에서도 뽑는다', () => {
  const got = touchedNames({
    tool_input: { command: 'git mv harness/docs/10-15-a.md harness/docs/10-16-b.md' } });
  assert.deepEqual(got.sort(), ['10-15-a.md', '10-16-b.md']);
});

test('게이트. 하위 디렉터리는 뽑지 않는다', () => {
  assert.deepEqual(touchedNames({ tool_input: { file_path: 'harness/docs/sub/10-19-a.md' } }), []);
});

test('게이트. 방금 만든 파일이 겹치면 종료 코드 2로 막는다', () => {
  const docs = gaterepo('block');
  const r = gate({ session_id: sid('block'), tool_name: 'Write',
    tool_input: { file_path: path.join(docs, '10-18-mine.md') } }, { NUMBERS_GATE_DIR: docs });
  assert.equal(r.code, 2);
  assert.match(r.out, /문서 번호가 겹친다/);
  assert.match(r.out, /10-18-mine\.md\(로컬만\)/);
});

// 이미 겹친 저장소에서 모든 쓰기가 막히면 세션이 못 나간다. 알리되 막지 않는다
test('게이트. 남이 만든 겹침은 알리되 막지 않는다', () => {
  const docs = gaterepo('other');
  const r = gate({ session_id: sid('other'), tool_name: 'Write',
    tool_input: { file_path: path.join(docs, '10-99-other.md') } }, { NUMBERS_GATE_DIR: docs });
  assert.equal(r.code, 0);
  assert.match(r.out, /방금 만든 파일과는 무관하다/);
});

test('게이트. 차단 한도를 넘으면 막지 않는다', () => {
  const docs = gaterepo('limit');
  const arg = { session_id: sid('limit'), tool_name: 'Write',
    tool_input: { file_path: path.join(docs, '10-18-mine.md') } };
  const env = { NUMBERS_GATE_DIR: docs, NUMBERS_GATE_MAX_BLOCKS: '1' };
  assert.equal(gate(arg, env).code, 2);
  const second = gate(arg, env);
  assert.equal(second.code, 0);
  assert.match(second.out, /차단 한도/);
});

// 게이트 고장이 세션을 잠그면 안 된다. 입력이 무엇이든 막지 않는 쪽으로 넘어진다
test('게이트. 빈 입력과 깨진 JSON은 막지 않는다', () => {
  assert.equal(gate('').code, 0);
  assert.equal(gate('{ 이건 JSON이 아니다').code, 0);
  assert.equal(gate({ tool_name: 'Write' }).code, 0);
});

// ---------- 재개 브리핑 (10-14 9-4절 E1) ----------
// 왜 필요한가: 재개할 때 42,900바이트를 통째로 읽으면 무엇이 안 끝났는지가 안 보인다.
// 그리고 merge=union은 추가된 줄의 순서를 보장하지 않아서 파일의 마지막 행이 시간의
// 마지막 행이 아니다. 이 둘이 이 명령이 있는 이유이고 아래 두 번째 테스트가 그것을 잰다.

const ROWS = [
  '| 날짜시각 | Task | 라운드 | 단계 | 결과 | 실패 원인 | 교훈 | 다음 작업 | 실제 시간 |',
  '|---|---|---|---|---|---|---|---|---|',
  '| 2026-09-08 10:00 | H1 | 해당 없음 | 준비 | drafted. 초안 | 없음 | 없음 | H1 승인 | 미측정 |',
  '| 2026-09-08 12:00 | H1 | 해당 없음 | 준비 | done. 승인됨 | 없음 | 없음 | H2 | 미측정 |',
  '| 2026-09-08 11:00 | H2 | 해당 없음 | 준비 | halted. 막혔다 | 권한 규칙이 막는다 | 없음 | 사용자 판단 | 미측정 |',
].join('\n');

function stateFile(body = ROWS) {
  const p = path.join(TMP, `state-${seq++}.md`);
  fs.writeFileSync(p, `# 진행 기록\n\n| 칸 | 무엇 |\n|---|---|\n| 날짜시각 | YYYY-MM-DD HH:MM |\n\n${body}\n`);
  return p;
}

test('stateRows. 진행 기록 행만 뽑고 다른 표는 무시한다', () => {
  const rows = stateRows(fs.readFileSync(stateFile(), 'utf8'));
  assert.equal(rows.length, 3);
  assert.deepEqual(rows.map((r) => r.task), ['H1', 'H2', 'H1']);
});

test('stateRows. 파일 순서가 아니라 날짜시각으로 읽는다', () => {
  const rows = stateRows(fs.readFileSync(stateFile(), 'utf8'));
  assert.deepEqual(rows.map((r) => r.when), ['2026-09-08 10:00', '2026-09-08 11:00', '2026-09-08 12:00']);
});

test('state. 끝난 Task와 안 끝난 Task를 가른다', () => {
  const r = run(() => state(stateFile()));
  assert.equal(r.code, 0);
  assert.match(r.out, /끝난 Task 1종/);
  assert.match(r.out, /안 끝난 Task 1종/);
});

test('state. 막힌 행을 실패 원인과 같이 낸다', () => {
  const r = run(() => state(stateFile()));
  assert.match(r.out, /막힌 채로 남은 행 1건/);
  assert.match(r.out, /권한 규칙이 막는다/);
});

test('state. --task는 그 Task만 본다', () => {
  const r = run(() => state(stateFile(), { task: 'H2' }));
  assert.match(r.out, /행 1개/);
  assert.doesNotMatch(r.out, /H1/);
});

test('state. 행 형식에 맞는 행이 없으면 종료 코드 2', () => {
  const p = path.join(TMP, `state-empty-${seq++}.md`);
  fs.writeFileSync(p, '# 진행 기록\n\n표가 없다\n');
  const r = run(() => state(p));
  assert.equal(r.code, 2);
});

// ---------- 열두 칸 (2026-09-10. 10-14 9-3절 E2 E3 E4) ----------
// 왜 필요한가: 아홉 칸에는 결과 칸의 주장을 받치는 것이 없었고, 떠올렸다가 안 해 본
// 방법을 적을 데가 없었고, 앞 행이 틀렸을 때 그것을 가리킬 데가 없었다. 추가 전용
// 파일이라 고칠 수가 없으니 뒤 행이 앞 행을 대체한다고 적는 칸이 필요하다.
// 옛 행은 그대로 둔다. merge=union이 걸린 파일을 아흔 행째 다시 쓰면 다른 세션의
// 추가와 만나 파일이 두 벌이 된다. 그래서 읽는 쪽이 두 형태를 다 받는다.

const ROWS12 = [
  '| 날짜시각 | Task | 라운드 | 단계 | 결과 | 실패 원인 | 교훈 | 다음 작업 | 실제 시간 | 증거 | 안 해 본 것 | 대체 |',
  '|---|---|---|---|---|---|---|---|---|---|---|---|',
  '| 2026-09-10 09:00 | H7 | 해당 없음 | 준비 | halted. 막혔다 | 경로를 잘못 봤다 | 없음 | 사용자 판단 | 미측정 | 없음 | 없음 | 없음 |',
  '| 2026-09-10 11:00 | H7 | 해당 없음 | 준비 | drafted. 초안 | 없음 | 없음 | H8 | 20분 | node --test 97건 통과 | 양식을 두 벌로 나눠 보기 | 2026-09-10 09:00 |',
  '| 2026-09-10 12:00 | H8 | 해당 없음 | 준비 | drafted. 초안 | 없음 | 없음 | H9 | 미측정 | 없음 | 없음 | 없음 |',
].join('\n');

test('stateRows. 열두 칸 행의 뒤 세 칸을 읽는다', () => {
  const rows = stateRows(fs.readFileSync(stateFile(ROWS12), 'utf8'));
  const r = rows.find((x) => x.when === '2026-09-10 11:00');
  assert.equal(r.evidence, 'node --test 97건 통과');
  assert.equal(r.untried, '양식을 두 벌로 나눠 보기');
  assert.equal(r.supersedes, '2026-09-10 09:00');
});

test('stateRows. 아홉 칸 행의 뒤 세 칸은 빈 문자열이다', () => {
  const rows = stateRows(fs.readFileSync(stateFile(), 'utf8'));
  assert.equal(rows[0].cols, 9);
  assert.equal(rows[0].evidence, '');
  assert.equal(rows[0].untried, '');
  assert.equal(rows[0].supersedes, '');
});

test('stateRows. 두 형태가 한 파일에 섞여도 둘 다 뽑는다', () => {
  const rows = stateRows(fs.readFileSync(stateFile(`${ROWS}\n${ROWS12}`), 'utf8'));
  assert.equal(rows.length, 6);
  assert.deepEqual(rows.map((r) => r.cols), [9, 9, 9, 12, 12, 12]);
});

test('supersededSet. 뒤 행이 가리킨 앞 행만 모은다', () => {
  const rows = stateRows(fs.readFileSync(stateFile(ROWS12), 'utf8'));
  assert.deepEqual([...supersededSet(rows)], ['2026-09-10 09:00']);
});

test('state. 대체된 앞 행은 셈에서 빠지고 막힌 행에도 안 나온다', () => {
  const r = run(() => state(stateFile(ROWS12)));
  assert.equal(r.code, 0);
  assert.match(r.out, /행 2개/);
  assert.match(r.out, /뒤 행이 대체한 행 1건은 셈에서 뺐다/);
  assert.match(r.out, /막힌 채로 남은 행 0건/);
  assert.doesNotMatch(r.out, /경로를 잘못 봤다/);
});

test('state. 안 해 본 것이 있으면 안 끝난 Task에 같이 낸다', () => {
  const r = run(() => state(stateFile(ROWS12)));
  assert.match(r.out, /안 해 본 것: 양식을 두 벌로 나눠 보기/);
});

test('state. 증거가 없는 열두 칸 행은 지적한다', () => {
  const r = run(() => state(stateFile(ROWS12)));
  assert.match(r.out, /증거 없음\. 결과 칸은 주장이지 기록이 아니다/);
});

test('state. 아홉 칸 행에는 증거를 안 따진다', () => {
  const r = run(() => state(stateFile()));
  assert.doesNotMatch(r.out, /증거 없음/);
  assert.doesNotMatch(r.out, /안 해 본 것:/);
});

// ---------- 권한 설정 검사 (10-19 4절 G5) ----------
// 왜 필요한가: 같은 규칙을 Bash와 PowerShell 두 벌로 쓰는데 한쪽만 고치면 실패가 나지
// 않고 그 규칙이 다른 쪽 경로에서 조용히 사라진다. 안 걸리는 것은 통과처럼 보인다.

function settingsFile(perms) {
  const p = path.join(TMP, `settings-${seq++}.json`);
  fs.writeFileSync(p, JSON.stringify({ permissions: perms }, null, 2));
  return p;
}

test('ruleBody. 두 문법의 같은 규칙이 같은 본체가 된다', () => {
  assert.deepEqual(ruleBody('Bash(git status:*)'), { tool: 'Bash', body: 'git status' });
  assert.deepEqual(ruleBody('PowerShell(git status *)'), { tool: 'PowerShell', body: 'git status' });
});

test('ruleBody. 명령 이름이 다른 짝은 선언으로 맞춘다', () => {
  assert.equal(ruleBody('Bash(rm *)').body, 'Remove-Item');
  assert.equal(ruleBody('PowerShell(Remove-Item *)').body, 'Remove-Item');
});

test('ruleBody. 셸 규칙이 아니면 본체가 없다', () => {
  assert.equal(ruleBody('Read(./.env)'), null);
  assert.equal(ruleBody('Edit(**/*.pem)'), null);
});

test('settings. 짝이 맞으면 통과한다', () => {
  const f = settingsFile({ deny: ['Bash(rm *)', 'PowerShell(Remove-Item *)', 'Read(./.env)'], allow: [] });
  const r = run(() => settings(f));
  assert.equal(r.code, 0);
});

test('settings. 한쪽만 있는 규칙을 잡는다', () => {
  const f = settingsFile({ deny: [], allow: ['Bash(git push:*)', 'PowerShell(git status *)'] });
  const r = run(() => settings(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /짝이 되는 PowerShell 규칙이 없다: git push/);
  assert.match(r.out, /짝이 되는 Bash 규칙이 없다: git status/);
});

test('settings. 같은 규칙이 deny와 allow 양쪽에 있으면 잡는다', () => {
  const f = settingsFile({ deny: ['Read(./.env)'], allow: ['Read(./.env)'] });
  const r = run(() => settings(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /deny와 allow 양쪽에 있다/);
});

test('settings. permissions 키가 없으면 잡는다', () => {
  const p = path.join(TMP, `settings-nokey-${seq++}.json`);
  fs.writeFileSync(p, '{}');
  const r = run(() => settings(p));
  assert.equal(r.code, 1);
  assert.match(r.out, /permissions 키가 없다/);
});

test('settings. JSON이 깨졌으면 종료 코드 2', () => {
  const p = path.join(TMP, `settings-bad-${seq++}.json`);
  fs.writeFileSync(p, '{ 깨진 ');
  const r = run(() => settings(p));
  assert.equal(r.code, 2);
});

// ---------- union 병합 흔적 (2026-09-11. 이슈 119) ----------
// 왜 필요한가: merge=union은 양쪽이 같은 구간을 건드리면 두 판을 다 남긴다. 그 흔적을
// 네 번 사람이 눈으로 찾았다. 행은 harness/state/README.md의 열두 칸 형식에서 만든다.

const U_HEAD = [
  '| 날짜시각 | Task | 라운드 | 단계 | 결과 | 실패 원인 | 교훈 | 다음 작업 | 실제 시간 | 증거 | 안 해 본 것 | 대체 |',
  '|---|---|---|---|---|---|---|---|---|---|---|---|',
];
const U_ROW_A = '| 2026-09-11 10:00 | H1 | 해당 없음 | 준비 | applied. 초안 | 없음 | 없음 | H1 승인 | 미측정 | 없음 | 없음 | 없음 |';
const U_ROW_B = '| 2026-09-11 10:00 | H2 | 해당 없음 | 준비 | drafted. 같은 분에 다른 세션 | 없음 | 없음 | H2 승인 | 미측정 | 없음 | 없음 | 없음 |';
const U_NINE = '| 2026-09-11 09:00 | H0 | 해당 없음 | 준비 | done. 아홉 칸 행 | 없음 | 없음 | H1 | 미측정 |';
const U_GROWN = U_NINE + ' 없음 | 없음 | 없음 |';

// 임시 저장소 모양을 만든다. .gitattributes와 그것이 가리키는 기록 파일 하나
function unionRepo(rows, attr = 'state/log.md merge=union') {
  const dir = fs.mkdtempSync(path.join(TMP, 'union-'));
  fs.mkdirSync(path.join(dir, 'state'));
  fs.writeFileSync(path.join(dir, '.gitattributes'), `# 줄바꿈\n* text=auto eol=lf\n${attr}\n`);
  fs.writeFileSync(path.join(dir, 'state', 'log.md'), `# 기록\n\n${[...U_HEAD, ...rows].join('\n')}\n`);
  return path.join(dir, '.gitattributes');
}

test('unionFiles. 주석과 다른 속성은 빼고 merge=union만 뽑는다', () => {
  const files = unionFiles('# 설명\n* text=auto eol=lf\n*.png binary\na/b.md merge=union\nc.md merge=union # 꼬리 주석\n');
  assert.deepEqual(files, ['a/b.md', 'c.md']);
});

test('union 통과. 날짜시각이 같고 내용이 다른 행 둘은 정상이다', () => {
  const r = run(() => union(unionRepo([U_ROW_A, U_ROW_B])));
  assert.equal(r.code, 0, r.out);
  assert.match(r.out, /union 파일 1개/);
});

// 기록 파일은 제목 줄과 빈 줄 뒤에 머리글이 3행, 구분선이 4행이라 첫 행이 5행이다
test('union 실패. 같은 행이 두 번', () => {
  const r = run(() => union(unionRepo([U_ROW_A, U_ROW_B, U_ROW_A])));
  assert.equal(r.code, 1);
  assert.match(r.out, /\[union\.dup-row\] +7 +state\/log\.md 7행이 5행과 같다/);
});

test('union 실패. 칸을 붙여 고친 행의 옛 판이 접두로 남았다', () => {
  const r = run(() => union(unionRepo([U_NINE, U_ROW_A, U_GROWN])));
  assert.equal(r.code, 1);
  assert.match(r.out, /\[union\.prefix-row\] +5 +state\/log\.md 5행이 7행의 접두다/);
});

test('union 실패. 옛 판의 칸을 고쳐서 접두가 아니면 칸 수로 잡는다', () => {
  const edited = U_NINE.replace('done. 아홉 칸 행', 'done. 고친 칸');
  const r = run(() => union(unionRepo([edited, U_GROWN])));
  assert.equal(r.code, 1);
  assert.doesNotMatch(r.out, /union\.prefix-row\] +\d/);
  assert.match(r.out, /\[union\.table-cells\] +5 +state\/log\.md 5행이 9칸이다. 머리글 3행은 12칸/);
});

test('union 통과. 펜스 안의 표 흉내는 세지 않는다', () => {
  const r = run(() => union(unionRepo([U_ROW_A, '', '```', U_ROW_A, U_ROW_A, '```'])));
  assert.equal(r.code, 0, r.out);
});

test('union 실패. 선언한 파일이 없다', () => {
  const r = run(() => union(unionRepo([U_ROW_A], 'state/log.md merge=union\nstate/없음.md merge=union')));
  assert.equal(r.code, 1);
  assert.match(r.out, /\[union\.exists\] +0 +state\/없음\.md 파일이 없다/);
});

test('union 통과. 글롭 패턴과 앞 슬래시도 읽는다', () => {
  const r = run(() => union(unionRepo([U_ROW_A, U_ROW_B], '/state/*.md merge=union')));
  assert.equal(r.code, 0, r.out);
  assert.match(r.out, /union 파일 1개/);
});

test('union 실패. 글롭 패턴에 맞는 파일이 없다', () => {
  const r = run(() => union(unionRepo([U_ROW_A], 'state/log.md merge=union\nstate/없음-*.md merge=union')));
  assert.equal(r.code, 1);
  assert.match(r.out, /\[union\.exists\] +0 +state\/없음-\*\.md 에 맞는 파일이 없다/);
});

test('union. merge=union 선언이 없으면 사용법 오류다', () => {
  const r = run(() => union(unionRepo([U_ROW_A], '*.png binary')));
  assert.equal(r.code, 2);
});

test('unionMarks. 같은 행과 접두 행을 줄 번호 순으로 낸다', () => {
  const text = [...U_HEAD, U_NINE, U_ROW_A, U_ROW_A, U_GROWN].join('\n');
  assert.deepEqual(unionMarks(text), [
    { kind: 'prefix', line: 3, other: 6 },
    { kind: 'dup', line: 5, other: 4 },
  ]);
});

test('union. 지금의 .gitattributes가 통과한다. progress.md의 칸 수만 범위 밖이다', () => {
  const r = run(() => union(path.join(ROOT, '.gitattributes')));
  assert.equal(r.code, 0, r.out);
  assert.match(r.out, /union 파일 3개/);
  assert.match(r.out, /범위 밖 1건/);
  assert.match(r.out, /union\.table-cells +harness\/state\/progress\.md/);
});
