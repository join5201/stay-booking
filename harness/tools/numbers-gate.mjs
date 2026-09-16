// PostToolUse 훅용 문서 번호 게이트
//
// 왜 필요한가: check.mjs numbers는 디렉터리를 받는데 훅은 방금 무엇을 건드렸는지를
// stdin JSON으로 준다. 이 파일이 그 사이를 잇는다. 번호가 겹치는 것은 파일을 만드는
// 순간에 잡아야 싸다. 커밋하고 병합한 뒤에 잡으면 이름을 바꾸는 커밋이 따로 필요하고
// 색인과 본문의 인용까지 같이 고쳐야 한다(16bb437이 그 모양이었다).
//
// 확인한 것 (https://code.claude.com/docs/en/hooks, 2026-09-10)
//   PostToolUse 입력에 session_id, tool_name, tool_input이 있다
//   종료 코드 2는 stderr를 모델에게 준다. 도구는 이미 돌았으므로 되돌리지는 못한다
//   종료 코드 0과 그 밖의 값은 모델에게 주지 않는다
//
// 설정 (.claude/settings.json. 추적하는 파일이다)
//   "PostToolUse": [
//     { "matcher": "Write|Edit|Bash",
//       "hooks": [ { "type": "command",
//                    "command": "node \"${CLAUDE_PROJECT_DIR}/harness/tools/numbers-gate.mjs\"",
//                    "timeout": 20 } ] }
//   ]
//   answer 게이트는 배선이 .claude/settings.local.json에 있어 추적 밖이다. 저장소를
//   받은 사람에게는 없는 것과 같다(10-14 8-2절). 이 게이트는 그 자리를 반복하지 않는다.
//
// 손으로 돌려 보는 예시
//   echo '{"tool_name":"Write","tool_input":{"file_path":"harness/docs/10-19-a.md"}}' | node harness/tools/numbers-gate.mjs
//   echo $?
//
// 통과 출력 예시 (종료 코드 0. 아무것도 출력하지 않는다)
//
// 막을 때 출력 예시 (종료 코드 2. stderr)
//   문서 번호가 겹친다. 방금 만든 파일의 번호를 비어 있는 다음 번호로 바꾼다
//     [numbers.duplicate] 0  번호 10-18. 문서 2개가 같은 번호를 쓴다: 10-18-a.md(로컬만), 10-18-o2o-harness-crosscheck-rollup.md(origin/main만). 나중 것이 다음 번호로 내려간다 (16bb437)
//   나중 것이 양보한다. 색인(harness/docs/README.md)의 행도 같이 옮긴다
//
// 보는 자리는 harness/docs 하나다
//   document/는 06-2-o2o-aggregates.md와 06-2-o2o-aggregates-explained.md처럼 한 번호에
//   본문과 해설을 짝으로 두는 자리다. 여기 걸면 정상 배치가 매번 걸린다. 늘리려면
//   그 자리의 규약을 먼저 정한다.
//
// 안전 규칙 넷
//   1. 방금 건드린 파일이 harness/docs 아래가 아니면 아무것도 하지 않는다
//   2. 실패가 방금 건드린 파일을 가리킬 때만 막는다. 남이 만든 겹침은 한 줄만 알린다.
//      그러지 않으면 이미 겹친 저장소에서 모든 쓰기가 막혀 세션이 못 나간다
//   3. 한 세션에서 막는 횟수는 NUMBERS_GATE_MAX_BLOCKS번까지다. 기본 5
//   4. 스크립트 자체가 어떤 이유로든 터지면 막지 않는다. 게이트 고장이 세션을 잠그면 안 된다
//
// 네트워크를 부르지 않는다. numbers의 --fetch는 주지 않는다. 훅이 파일 쓸 때마다
// git fetch를 부르면 오프라인에서 세션이 멈춘다. 대조하는 ref는 마지막 fetch 시점이다.

import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { numbers } from './check.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, '..', '..');
// 보는 디렉터리. NUMBERS_GATE_DIR은 테스트가 임시 저장소를 가리키게 하는 용도다.
// 이 문이 없으면 게이트를 끝까지 도는 시험을 하려고 harness/docs에 임시 파일을
// 만들어야 하고, 그 폴더는 세션 여럿이 공유한다
const WATCHED = process.env.NUMBERS_GATE_DIR
  ? path.resolve(process.env.NUMBERS_GATE_DIR)
  : path.join(ROOT, 'harness', 'docs');
const WATCHED_REL = 'harness/docs';
const MAX_BLOCKS = Number(process.env.NUMBERS_GATE_MAX_BLOCKS || 5);
const STATE = path.join(os.tmpdir(), 'harness-numbers-gate.json');

function readStdin() {
  try { return fs.readFileSync(0, 'utf8'); } catch { return ''; }
}

function loadState() {
  try { return JSON.parse(fs.readFileSync(STATE, 'utf8')); } catch { return {}; }
}

function saveState(s) {
  try { fs.writeFileSync(STATE, JSON.stringify(s), 'utf8'); } catch { /* 기록 실패는 무시한다 */ }
}

// 방금 건드린 파일 중 harness/docs 바로 아래 것의 파일 이름만 뽑는다.
// Write와 Edit는 file_path를 주고 Bash는 명령 문자열을 준다. 이름을 바꾸는 일은
// git mv로 하므로 Bash도 봐야 한다. 16bb437이 바로 그 모양이었다
export function touchedNames(input) {
  const raw = [];
  const ti = input.tool_input || {};
  if (typeof ti.file_path === 'string') raw.push(ti.file_path);
  if (typeof ti.command === 'string') {
    for (const m of ti.command.matchAll(/harness\/docs\/[^\s"'`;|&)]+/g)) raw.push(m[0]);
  }
  // 문자열 앞머리를 비교하지 않고 실제 경로로 풀어 부모를 본다. 훅 입력이 절대경로로
  // 올지 상대경로로 올지, 구분자가 무엇일지가 자리마다 다르다.
  // 대소문자를 무시하는 것은 Windows 파일시스템이 구분하지 않기 때문이다 (HRV-05)
  const parent = path.resolve(WATCHED).toLowerCase();
  const names = new Set();
  for (const one of raw) {
    const abs = path.resolve(ROOT, one);
    if (path.dirname(abs).toLowerCase() !== parent) continue; // 하위 디렉터리도 여기서 걸린다
    names.add(path.basename(abs));
  }
  return [...names];
}

// check.mjs numbers는 console.log로 낸다. 훅은 그 텍스트가 필요하므로 가로챈다
function runNumbers(dir) {
  const out = [];
  const log = console.log;
  const err = console.error;
  console.log = (...a) => out.push(a.join(' '));
  console.error = (...a) => out.push(a.join(' '));
  try {
    return { code: numbers(dir), out: out.join('\n') };
  } finally {
    console.log = log;
    console.error = err;
  }
}

// 실패 줄 중에서 방금 건드린 파일을 가리키는 것만 고른다
export function minedFailures(text, names) {
  const fails = text.split('\n').filter((l) => l.includes('[numbers.'));
  return {
    all: fails,
    mine: fails.filter((l) => names.some((n) => l.includes(n))),
  };
}

function main() {
  const raw = readStdin();
  if (!raw.trim()) return 0;

  let input;
  try { input = JSON.parse(raw); } catch { return 0; }

  const names = touchedNames(input);
  if (names.length === 0) return 0; // 규칙 1

  if (!fs.existsSync(WATCHED)) return 0;
  const res = runNumbers(WATCHED);
  if (res.code === 0) return 0;

  const { all, mine } = minedFailures(res.out, names);
  if (mine.length === 0) {
    // 규칙 2. 남이 만든 겹침이다. 알리되 막지 않는다
    console.error(`문서 번호 겹침이 이미 ${all.length}건 있다. 방금 만든 파일과는 무관하다. node harness/tools/check.mjs numbers ${WATCHED_REL}`);
    return 0;
  }

  // 규칙 3
  const key = input.session_id || 'unknown';
  const state = loadState();
  const used = state[key] || 0;
  if (used >= MAX_BLOCKS) {
    console.error(`문서 번호가 또 겹쳤지만 이 세션의 차단 한도 ${MAX_BLOCKS}을 이미 썼다. 막지 않는다`);
    return 0;
  }
  state[key] = used + 1;
  const keys = Object.keys(state);
  if (keys.length > 200) for (const k of keys.slice(0, keys.length - 200)) delete state[k];
  saveState(state);

  console.error('문서 번호가 겹친다. 방금 만든 파일의 번호를 비어 있는 다음 번호로 바꾼다');
  for (const l of mine) console.error(l);
  console.error('나중 것이 양보한다. 색인(harness/docs/README.md)의 행도 같이 옮긴다 (16bb437)');
  return 2;
}

// 이 파일이 직접 실행될 때만 돈다. 가드가 없으면 테스트가 import하는 순간
// main이 돌면서 stdin을 기다려 멈춘다. check.mjs와 같은 꼴이다
if (process.argv[1] && path.resolve(process.argv[1]) === path.resolve(fileURLToPath(import.meta.url))) {
  let code = 0;
  try {
    code = main();
  } catch (e) {
    // 게이트가 고장 나도 세션을 잠그지 않는다
    console.error(`numbers-gate 내부 오류. 막지 않는다: ${e && e.message}`);
    code = 0;
  }
  process.exit(code);
}
