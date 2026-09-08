// Stop 훅용 답변 게이트
//
// 왜 필요한가: check.mjs answer는 파일을 받는데 훅은 답변을 stdin JSON으로 준다.
// 이 파일이 그 사이를 잇는다. 그리고 문서에 무한 반복 방지 필드가 없어서
// 한 세션에서 막는 횟수를 여기서 직접 제한한다. 막힌 답변이 또 막히면
// 세션이 빠져나오지 못한다.
//
// 확인한 것 (https://code.claude.com/docs/en/hooks, 2026-09-08)
//   Stop 이벤트 입력에 session_id, prompt_id, last_assistant_message가 있다
//   종료 코드 2가 정지를 막고 stderr가 그 사유가 된다
//   종료 코드 0과 그 밖의 값은 막지 않는다
//
// 설정 예시 (.claude/settings.local.json)
//   {
//     "hooks": {
//       "Stop": [
//         { "matcher": "*",
//           "hooks": [ { "type": "command",
//                        "command": "node \"${CLAUDE_PROJECT_DIR}/harness/tools/answer-gate.mjs\"",
//                        "timeout": 30 } ] }
//       ]
//     }
//   }
//
// 손으로 돌려 보는 예시
//   echo '{"session_id":"t1","last_assistant_message":"짧은 확인입니다."}' | node harness/tools/answer-gate.mjs
//   echo $?
//
// 통과 출력 예시 (종료 코드 0. 아무것도 출력하지 않는다)
//
// 막을 때 출력 예시 (종료 코드 2. stderr)
//   답변 형식 검사 실패. 아래 항목을 고쳐 다시 답한다 (CLAUDE.md 2-1, harness/prompts/answer-format.md)
//     [answer.result-section]  40  마지막 절 제목이 결과가 아니다 (R4). 실제: 다음 단계
//   이 세션에서 남은 차단 횟수 0
//
// 안전 규칙 셋
//   1. 한 세션에서 막는 횟수는 ANSWER_GATE_MAX_BLOCKS번까지다. 기본 1
//   2. 스크립트 자체가 어떤 이유로든 터지면 막지 않는다. 게이트 고장이 세션을 잠그면 안 된다
//   3. 답변이 비었으면 막지 않는다. 도구만 돌린 턴이 있다

import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { answer } from './check.mjs';

const MAX_BLOCKS = Number(process.env.ANSWER_GATE_MAX_BLOCKS || 1);
const STATE = path.join(os.tmpdir(), 'harness-answer-gate.json');

function readStdin() {
  try { return fs.readFileSync(0, 'utf8'); } catch { return ''; }
}

function loadState() {
  try { return JSON.parse(fs.readFileSync(STATE, 'utf8')); } catch { return {}; }
}

function saveState(s) {
  try { fs.writeFileSync(STATE, JSON.stringify(s), 'utf8'); } catch { /* 기록 실패는 무시한다 */ }
}

// check.mjs answer는 console.log로 낸다. 훅은 그 텍스트가 필요하므로 가로챈다
function runAnswer(file) {
  const out = [];
  const log = console.log;
  console.log = (...a) => out.push(a.join(' '));
  try {
    return { code: answer(file), out: out.join('\n') };
  } finally {
    console.log = log;
  }
}

function main() {
  const raw = readStdin();
  if (!raw.trim()) return 0;

  let input;
  try { input = JSON.parse(raw); } catch { return 0; }

  const text = input.last_assistant_message;
  if (typeof text !== 'string' || text.trim() === '') return 0;

  const tmp = path.join(os.tmpdir(), `answer-${process.pid}.md`);
  fs.writeFileSync(tmp, text, 'utf8');

  let res;
  try { res = runAnswer(tmp); } finally { try { fs.unlinkSync(tmp); } catch { /* 남아도 무해하다 */ } }
  if (res.code === 0) return 0;

  // 실패다. 이 세션에서 이미 막은 횟수를 본다
  const key = input.session_id || 'unknown';
  const state = loadState();
  const used = state[key] || 0;
  if (used >= MAX_BLOCKS) {
    console.error(`답변 형식 검사가 또 실패했지만 이 세션의 차단 한도 ${MAX_BLOCKS}을 이미 썼다. 막지 않는다`);
    return 0;
  }
  state[key] = used + 1;
  saveState(state);

  const detail = res.out.split('\n').filter((l) => l.includes('[answer.')).join('\n');
  console.error('답변 형식 검사 실패. 아래 항목을 고쳐 다시 답한다 (CLAUDE.md 2-1, harness/prompts/answer-format.md)');
  console.error(detail || res.out);
  console.error(`이 세션에서 남은 차단 횟수 ${MAX_BLOCKS - state[key]}`);
  return 2;
}

let code = 0;
try {
  code = main();
} catch (e) {
  // 게이트가 고장 나도 세션을 잠그지 않는다
  console.error(`answer-gate 내부 오류. 막지 않는다: ${e && e.message}`);
  code = 0;
}
process.exit(code);
