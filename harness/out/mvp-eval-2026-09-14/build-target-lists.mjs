// 평가 대상 목록 표 생성기 (mvp-eval-2026-09-14 라운드)
//
// 왜 필요한가: 이 라운드는 묶음 넷이 main에 섞여 들어 커밋 범위로 대상을 뽑을 수 없다
// (README 3절). 대신 패키지 단위로 뽑고, 다른 묶음이 고친 파일은 그 사실을 표에 적는다.
// 사람이 손으로 표를 만들면 파일 수와 해시가 틀리고, 틀린 표는 검사 스크립트가
// 작업 트리와 대조할 때 그대로 실패로 나온다. 그래서 표는 이 스크립트가 낸다.
//
// LLM을 부르지 않는다. git ls-files, git log, sha256만 쓴다. 어떤 파일도 고치지 않는다.
// 표의 sha256은 작업 트리 파일의 값이다. 기준 커밋 1bdadfe에서 돌려야 목록의 값과 같다.
//
// 실행 예시
//   node harness/out/mvp-eval-2026-09-14/build-target-lists.mjs catalog
//   node harness/out/mvp-eval-2026-09-14/build-target-lists.mjs booking > tmp/booking-tables.md
//   쌍 키는 catalog, inventory-rate, promotion-search, booking, payment 다섯이다
//
// 통과 출력 예시 (표준 출력에 마크다운 표. 마지막 줄이 요약)
//   ### backend/src/main/java/com/o2o/catalog/domain 14개
//
//   | 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
//   |---|---|---|---|---|
//   | backend/src/main/java/com/o2o/catalog/domain/Property.java | sha256:0123456789abcdef | 숙소 | 없음 | 9e51a29 |
//   ...
//   요약 catalog 프로덕션 52 테스트 6 설정 0 (기준 1bdadfe)
//   테스트 표에는 건수 칸이 하나 더 있다. 값은 JUNIT_DIR의 XML에서 읽고, XML이 없는 파일은 테스트 지원 파일이다
//
// 실패 출력 예시 (종료 코드 1)
//   FAIL build-target-lists 쌍 키를 모른다: foo. catalog, inventory-rate, promotion-search, booking, payment 중 하나
//   FAIL build-target-lists 기준 커밋 1bdadfe이 HEAD의 조상이 아니다
//   FAIL build-target-lists 대상 파일이 작업 트리에 없다: backend/src/main/java/com/o2o/catalog/domain/Property.java

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { execFileSync } from 'node:child_process';

const BASE = '1bdadfe'; // README 1절. PR 138 병합 커밋. 백엔드 40단위 전부
const SRC = 'backend/src/main/java/com/o2o';
const TEST = 'backend/src/test/java/com/o2o';
const MAIN_PROPS = 'backend/src/main/resources/application.properties';
const TEST_PROPS = 'backend/src/test/resources/application.properties';
const JUNIT_DIR = 'harness/out/task-S9-booking-lifecycle-R1/step9'; // main 1bdadfe의 전체 실행 사본

// PR 번호를 묶음 이름으로. 첫 부모 병합 이력에서 뽑았다(2026-09-14)
const PR_BUNDLE = {
  33: '숙소', 35: '숙소',
  54: '재고와 요금', 115: '재고와 요금', // 115는 R1 반영 브랜치. 같은 묶음으로 센다(R2 블라인드)
  98: '프로모션과 검색',
  127: '예약 1차',
  133: '결제',
  138: '예약 2차',
};

const root = path.resolve(process.cwd());
const pairKey = process.argv[2];

function git(args) {
  return execFileSync('git', args, { cwd: root, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
}
function fail(msg) {
  console.error(`FAIL build-target-lists ${msg}`);
  process.exit(1);
}
function sha16(rel) {
  const abs = path.join(root, rel);
  if (!fs.existsSync(abs)) fail(`대상 파일이 작업 트리에 없다: ${rel}`);
  return crypto.createHash('sha256').update(fs.readFileSync(abs)).digest('hex').slice(0, 16);
}
function lsFiles(prefix) {
  const out = git(['ls-files', '--', prefix]);
  return out ? out.split('\n').map((s) => s.trim()).filter(Boolean) : [];
}

// 커밋을 묶음으로. 첫 부모 병합마다 그 브랜치의 커밋 집합을 구한다
function buildCommitMap() {
  const map = new Map();
  const merges = git(['log', '--first-parent', '--merges', '--format=%h %P %s', BASE]).split('\n');
  for (const line of merges) {
    const m = line.match(/^(\w+) (\w+) (\w+) Merge pull request #(\d+)/);
    if (!m) continue;
    const pr = Number(m[4]);
    const label = PR_BUNDLE[pr] || `기타(PR ${pr})`;
    const commits = git(['rev-list', '--abbrev-commit', `${m[2]}..${m[3]}`]).split('\n').filter(Boolean);
    for (const c of commits) if (!map.has(c)) map.set(c, label);
    map.set(m[1], label);
  }
  return map;
}
const commitMap = buildCommitMap();
function bundleOf(hash) {
  return commitMap.get(hash) || '하네스 초기';
}

// 파일 하나의 이력. 만든 커밋, 고친 묶음, 마지막 커밋
const histCache = new Map();
function history(rel) {
  if (histCache.has(rel)) return histCache.get(rel);
  const lines = git(['log', '--format=%h', BASE, '--', rel]).split('\n').filter(Boolean);
  if (lines.length === 0) fail(`기준 커밋에 이력이 없다: ${rel}`);
  const last = lines[0];
  const first = lines[lines.length - 1];
  const made = bundleOf(first);
  const touched = [];
  for (const h of lines.slice(0, -1).reverse()) {
    const b = bundleOf(h);
    if (b !== made && !touched.includes(b)) touched.push(b);
  }
  const h = { made, touched: touched.length ? touched.join(', ') : '없음', last, first, commits: lines };
  histCache.set(rel, h);
  return h;
}

function touchedBy(rel, labels) {
  return history(rel).commits.some((c) => labels.includes(bundleOf(c)));
}
function madeBy(rel, labels) {
  return labels.includes(history(rel).made);
}

const PAIRS = {
  'catalog': {
    prod: () => [
      ...lsFiles(`${SRC}/catalog/`),
      ...lsFiles(`${SRC}/shared/`).filter((f) => madeBy(f, ['숙소'])),
      `${SRC}/BackendApplication.java`,
    ],
    test: () => [
      ...lsFiles(`${TEST}/catalog/`),
      `${TEST}/BackendApplicationTests.java`,
      `${TEST}/DatabaseConnectionTest.java`,
    ],
    config: () => [],
  },
  'inventory-rate': {
    prod: () => [
      ...lsFiles(`${SRC}/inventory/`),
      `${SRC}/shared/Money.java`,
      `${SRC}/shared/SeoulDate.java`,
      `${SRC}/shared/ClockConfiguration.java`,
    ],
    test: () => [...lsFiles(`${TEST}/inventory/`), `${TEST}/shared/MoneyTest.java`],
    config: () => [],
  },
  'promotion-search': {
    prod: () => [
      ...lsFiles(`${SRC}/promotion/`),
      ...lsFiles(`${SRC}/search/`),
      ...lsFiles(`${SRC}/shared/`).filter((f) => madeBy(f, ['프로모션과 검색'])),
    ],
    test: () => [...lsFiles(`${TEST}/promotion/`), ...lsFiles(`${TEST}/search/`)],
    config: () => [],
  },
  'booking': {
    prod: () => [
      ...lsFiles(`${SRC}/booking/`),
      ...lsFiles(`${SRC}/inventory/`).filter((f) => touchedBy(f, ['예약 1차', '예약 2차'])),
      ...lsFiles(`${SRC}/shared/`).filter((f) => touchedBy(f, ['예약 1차', '예약 2차'])),
    ],
    test: () => [
      ...lsFiles(`${TEST}/booking/`),
      ...lsFiles(`${TEST}/inventory/`).filter((f) => madeBy(f, ['예약 1차', '예약 2차'])),
    ],
    config: () => [TEST_PROPS],
  },
  'payment': {
    prod: () => [
      ...lsFiles(`${SRC}/payment/`),
      ...lsFiles(`${SRC}/shared/`).filter((f) => touchedBy(f, ['결제'])),
    ],
    test: () => lsFiles(`${TEST}/payment/`),
    config: () => [MAIN_PROPS, TEST_PROPS],
  },
};

if (!PAIRS[pairKey]) fail(`쌍 키를 모른다: ${pairKey}. ${Object.keys(PAIRS).join(', ')} 중 하나`);
try {
  git(['merge-base', '--is-ancestor', BASE, 'HEAD']);
} catch {
  fail(`기준 커밋 ${BASE}이 HEAD의 조상이 아니다`);
}

function groupBy(files) {
  const groups = new Map();
  for (const f of files) {
    const dir = path.posix.dirname(f);
    if (!groups.has(dir)) groups.set(dir, []);
    groups.get(dir).push(f);
  }
  return groups;
}

// 테스트 건수는 main 1bdadfe의 마지막 전체 실행 사본(JUnit XML 48개, 419건)에서 읽는다
function junitCounts() {
  const counts = new Map();
  if (!fs.existsSync(path.join(root, JUNIT_DIR))) return counts;
  for (const name of fs.readdirSync(path.join(root, JUNIT_DIR))) {
    const m = name.match(/^TEST-(.+)\.xml$/);
    if (!m) continue;
    const xml = fs.readFileSync(path.join(root, JUNIT_DIR, name), 'utf8');
    const t = xml.match(/<testsuite[^>]*\btests="(\d+)"/);
    if (t) counts.set(m[1], Number(t[1]));
  }
  return counts;
}
const junit = junitCounts();
function testCount(rel) {
  const cls = rel.replace(/^backend\/src\/test\/java\//, '').replace(/\.java$/, '').replace(/\//g, '.');
  return junit.has(cls) ? String(junit.get(cls)) : '0 (테스트 지원 파일)';
}

function table(files, withCount) {
  const out = [];
  out.push(withCount
    ? '| 경로 | sha256 | 건수 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |'
    : '| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |');
  out.push(withCount ? '|---|---|---|---|---|---|' : '|---|---|---|---|---|');
  for (const f of files) {
    const h = history(f);
    const count = withCount ? ` ${testCount(f)} |` : '';
    out.push(`| ${f} | sha256:${sha16(f)} |${count} ${h.made} | ${h.touched} | ${h.last} |`);
  }
  return out.join('\n');
}

const pair = PAIRS[pairKey];
const prod = [...new Set(pair.prod())].sort();
const test = [...new Set(pair.test())].sort();
const config = [...new Set(pair.config())].sort();
const lines = [];

for (const [dir, files] of groupBy(prod)) {
  lines.push(`### ${dir} ${files.length}개`, '', table(files, false), '');
}
if (test.length) {
  lines.push(`### 테스트 ${test.length}개`, '', table(test, true), '');
}
if (config.length) {
  lines.push(`### 설정 파일 ${config.length}개`, '', table(config, false), '');
}
lines.push(`요약 ${pairKey} 프로덕션 ${prod.length} 테스트 ${test.length} 설정 ${config.length} (기준 ${BASE})`);
console.log(lines.join('\n'));
