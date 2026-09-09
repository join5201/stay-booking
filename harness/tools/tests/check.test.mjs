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
import { fileURLToPath } from 'node:url';
import { fill, g1, g2, answer } from '../check.mjs';

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
