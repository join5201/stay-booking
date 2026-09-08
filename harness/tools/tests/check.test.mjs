// 검사 스크립트의 골든 파일 테스트
//
// 왜 필요한가: 검사 스크립트가 조용히 통과시키면 G1과 G2가 있으나 마나다. 그 고장은
// 산출물이 아니라 검사기에 있어서 리포트만 보면 드러나지 않는다.
//
// 구조: fixtures/의 통과 파일이 골든이다. 실패 fixture는 골든에서 딱 한 군데를
// 바꿔 만든다. 그래야 그 검사가 그 차이 하나 때문에 걸렸다는 것이 증명된다.
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
import { fill, g1, g2 } from '../check.mjs';

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

test('fill 실패. 절대경로가 아니다', () => {
  const f = prep('fill-pass.md', (t) => t.replace(ROOT + '/document/', 'document/'));
  const r = run(() => fill(f));
  assert.equal(r.code, 1);
  assert.match(r.out, /fill\.path-absolute/);
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
  ['링크 대상 없음', (t) => t.replace('eval-criteria-ddd.md', 'eval-criteria-없음.md'), /link\.exists/],
  ['옛 상대경로 링크', (t) => t.replace('본문이다.', '본문이다. [옛 링크](claude/06-4.md)'), /link\.stale/],
];

for (const [name, mutate, want] of docCases) {
  test(`g1 doc 실패. ${name}`, () => {
    const r = run(() => g1(prep('g1-doc-pass.md', mutate), { type: 'doc' }));
    assert.equal(r.code, 1);
    assert.match(r.out, want);
  });
}

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

test('g1 code 통과. 결과 파일이 있다', () => {
  const a = path.join(TMP, 'build.log');
  fs.writeFileSync(a, 'BUILD SUCCESSFUL\n');
  const r = run(() => g1(prep('g1-api-pass.md'), { type: 'code', artifacts: [a] }));
  assert.equal(r.code, 0);
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

// ---------- g2 ----------

test('g2 통과. HR4 항목 전부', () => {
  const r = run(() => g2(prep('g2-pass.md')));
  assert.equal(r.code, 0);
});

const g2Cases = [
  ['버전 해시 불일치', (t) => t.replace(/sha256:[0-9a-f]{64}/, 'sha256:' + '0'.repeat(64)), /g2\.version-match/],
  ['원본에 있는 지적이 결정표에 없다', (t) => t.replace(/^\| A \| 2 \| S9-R1-A-02 \|.*$\n/m, ''), /g2\.id-missing/],
  ['결정표에만 있는 지적', (t) => t.replace('S9-R1-A-02', 'S9-R1-A-09'), /g2\.id-extra/],
  ['선언한 행 수가 A와 B의 합과 다르다', (t) => t.replace('| A 원본 지적 수 | 2 |', '| A 원본 지적 수 | 3 |'), /g2\.row-count-declared/],
  ['빈 결정', (t) => t.replace('| 수용 | 반영한다 |', '|  | 반영한다 |'), /g2\.decision-empty/],
  ['거부에 이유가 없다', (t) => t.replace('| 거부 | 오판 기록 MJ-01 |', '| 거부 |  |'), /g2\.reject-reason/],
  ['반박을 확인필요가 아닌 등급에 썼다', (t) => t.replace('| S9-R1-A-02 | 확인필요 |', '| S9-R1-A-02 | 보통 |'), /g2\.rebut-severity/],
  ['치명 거부의 오판 기록에 근거가 없다', (t) => t.replace(/^\| 근거 \|.*$/m, '| 근거 | {{필수}} |'), /g2\.fatal-reject-record/],
  ['남은 실제 치명이 있다', (t) => t.replace('| 남은 실제 치명 지적 | 없음 |', '| 남은 실제 치명 지적 | S9-R1-B-01 |'), /g2\.fatal-remaining/],
];

for (const [name, mutate, want] of g2Cases) {
  test(`g2 실패. ${name}`, () => {
    const r = run(() => g2(prep('g2-pass.md', mutate)));
    assert.equal(r.code, 1);
    assert.match(r.out, want);
  });
}
