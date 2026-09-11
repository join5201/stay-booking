# 누적 지식 (harness/state/knowledge.md)

최초 작성: 2026-09-07
최종 갱신: 2026-09-08 (하네스 리뷰 조치. 3행 추가)

왜 이 파일이 필요한가: 지식의 부채를 갚는 자리다. 두 번 걸린 것, 문서 어디에도 없어서 매번 다시 알아내는 것을 여기 모은다. 이 파일이 없으면 같은 확인 작업을 매 세션 반복한다.

무엇을 적는가 세 가지.
1. harness/state/troubleshooting.md에서 두 번째로 나온 실패 원인. 승격된 것.
2. 공식 문서로 확인한 설정 키, 규약, 명령. 확인한 URL을 같이 적는다(CLAUDE.md N6).
3. 확인하지 못한 것. [추측] 태그와 검증 방법을 같이 적는다.

무엇을 적지 않는가: 설계 결정은 harness/decisions/에, 단계 진행은 harness/state/progress.md에, 실패 하나하나는 harness/state/troubleshooting.md에 적는다. 여기는 재사용되는 지식만 남긴다.

행 형식: 임시다. 날짜, 항목, 알게 된 것, 출처, 승격 위치 다섯 칸이다. 정식 형식은 H5에서 확정한다. 승격 위치는 이 지식이 최종적으로 어느 문서 조항이 되었는지다. 비어 있으면 아직 문서에 반영되지 않았다는 뜻이다.

규칙: 추가 전용. 틀린 것으로 밝혀지면 지우지 말고 새 행에 정정을 적고 원래 행의 승격 위치에 폐기를 적는다.

| 날짜 | 항목 | 알게 된 것 | 출처 | 승격 위치 |
|---|---|---|---|---|
| 2026-09-07 | 권한 규칙 평가 순서 | deny, ask, allow 순으로 평가한다. 넓은 deny는 더 좁은 allow를 덮는다. allow는 폴더 신뢰 뒤에 적용되고 deny와 ask는 즉시 적용된다 | https://code.claude.com/docs/en/permissions | CLAUDE.md 4절 |
| 2026-09-07 | PowerShell 규칙과 별칭 | PowerShell 규칙은 Bash와 같은 모양이다. 별칭이 정규화되므로 PowerShell(Remove-Item *)가 rm과 del에도 걸린다 | 같은 문서 | .claude/settings.json deny |
| 2026-09-07 | Bash 규칙 접두 비교 | Bash(cmd:*)는 Bash(cmd *)와 같다. 첫 별표 앞은 문자열 그대로 비교한다 | 같은 문서 | CLAUDE.md 4절. 경로 구분자를 슬래시로 고정하는 근거 |
| 2026-09-07 | Claude Code는 AGENTS.md를 읽지 않는다 | CLAUDE.md만 읽는다. 같이 읽히게 하려면 @AGENTS.md 임포트를 써야 한다 | https://code.claude.com/docs/en/memory | AGENTS.md 머리말. 이 하네스는 일부러 잇지 않는다 |
| 2026-09-07 | Codex의 AGENTS.md 탐색 | Codex 홈을 먼저 보고 Git 루트에서 현재 디렉터리까지 내려오며 모은다. 가까운 파일이 뒤에 와서 우선한다. 디렉터리당 한 파일, 기본 상한 32 KiB | https://learn.chatgpt.com/docs/agent-configuration/agents-md | AGENTS.md 0절 운용 조건 |
| 2026-09-07 | 이 폴더는 git 추적 밖이었다 | 루트 .gitignore의 /99_projects/가 제외했다. 2026-09-08 자체 저장소 편입으로 해소. 폐기 | C:/Dev/potenup/.gitignore 확인 | 폐기 |
| 2026-09-08 | fixture는 양식에서 만든다 | 골든 fixture를 검사기에 맞춰 만들었더니 테스트 29건이 전부 통과하는데 실제 양식을 넣으면 g2가 죽었다. 테스트가 코드의 거울이면 코드가 양식을 오해한 것까지 통과시킨다 | 하네스 리뷰 HRV-07 | harness/tools/tests/check.test.mjs 머리 주석 |
| 2026-09-08 | 게이트는 원본을 봐야 게이트다 | g2가 원본 리포트에서 ID만 읽고 심각도는 결정표 값을 믿었다. 그러면 심각도를 낮추는 것으로 치명 보호를 통째로 우회할 수 있다. 대조 대상이 되는 값은 전부 원본에서 읽는다 | 하네스 리뷰 HRV-01 | check.mjs parseReport |
| 2026-09-08 | 검사기를 고치면 이미 통과한 산출물도 다시 검사한다 | 강화한 g1을 후보 9개에 다시 돌리자 입력 팩 후보의 절 누락이 바로 걸렸다. 이전 검사기로 받은 통과는 그 검사기의 통과이지 규칙의 통과가 아니다 | 이번 세션 실측 | harness/reviews/...-fix.md 5절 |
| 2026-09-08 | 후보 문서에 공통 종료 문장을 둔다 | 설계 문서는 대화 응답이 아니라 종료 문장이 붙을 자리가 없다. 대신 후보임을 밝히는 한 문장을 마지막 줄로 고정하면 g1 --end 하나로 전수 검사가 되고, 후보가 확정본으로 오인되는 것도 막는다 | task-S8 R1 실측. 9건 전부 통과 | harness/out/task-S8-R1/ 전체 |
| 2026-09-08 | 반영 범위는 계약이 정한 것만 | 02와 04에 게스트와 guestId 표기가 남아 있고 액터 개명은 이미 확정된 결정이지만, 작업 계약의 반영 대상 표에 없어서 고치지 않고 반영하지 않은 것 절에 적었다. 계약 밖의 옳은 수정도 승인하지 않은 변경이다 | task-S8 R1 | 02 v9 7절, 04 v10 6절 |
| 2026-09-08 | 승인 필요는 대안이 설 때만 붙인다 | 계약에 승인 필요를 두 행 붙였는데 둘 다 사실에서 한 갈래로만 나오는 항목이었다. 대안이 실제로 둘 이상 서고 고르는 근거가 프로젝트 밖에 있을 때만 붙인다. 판단할 것이 없는데 물으면 사용자가 턴을 쓰고 계약은 그만큼 늦어진다 | 이번 세션. 사용자가 개별 판단이 필요한지 되물음 | harness/tasks/task-S8.md 정책 적용 표 |
| 2026-09-08 | 판 번호는 반영 완료의 증거가 아니다 | 로컬 02, 03, 04가 08-3 7절의 목표 판 번호 v8, v11, v9를 이미 달고 있는데 08-3 내용은 0건이었다. 판 번호는 그 파일의 이력이고 다른 계열의 목표 번호와 우연히 같을 수 있다. 반영 여부는 표지 문자열로 확인한다 | task-S8 계약 작성 중 실측. SettlePayment, settledAt, ORPHAN, REFUND_PENDING 전부 0건 | harness/tasks/task-S8.md 이 계약이 먼저 정하는 것 1항 |
| 2026-09-08 | 기록 형식은 첫 행 전에 정한다 | progress.md를 임시 4칸으로 열 턴 돌렸더니 H5에서 재작성할 행이 열한 개가 됐다. 형식 확정을 뒤로 미루는 비용은 미룬 턴 수에 비례한다 | 이번 세션 실측 | harness/state/progress.md 재작성 주석 |
| 2026-09-08 | 결정 문서끼리도 대조가 필요하다 | 10-4 v4가 01 전체 제공을 결정했는데 그것이 평가 기준 스킬 원본이 지목한 무력화 행위 그 자체였다. 두 결정 문서를 대조하지 않으면 상위 규칙을 어기는 결정이 확정으로 남는다 | 10-8 1-2, H4에서 10-4 v5로 번복 | harness/docs/10-4 1절과 3절 |
| 2026-09-08 | node --test에 디렉터리를 넘기면 실패한다 | Node 24.14.1에서 node --test <디렉터리>가 그 경로를 모듈로 해석해 MODULE_NOT_FOUND로 죽는다. 파일 경로를 직접 넘겨야 한다 | 이번 세션 실측 | harness/tools/tests/check.test.mjs 머리 주석 |
| 2026-09-08 | 골든 fixture에 치환 토큰 설명을 적으면 치환기가 문다 | 설명줄에 __HASH:경로__를 예시로 적었더니 치환 정규식이 그것도 실제 경로로 읽어 ENOENT가 났다. fixture 안에서는 토큰 자체를 쓰지 않고 말로 설명한다 | 이번 세션 실측 | fixtures/g2-pass.md, fill-pass.md |
| 2026-09-08 | 검사는 읽고 채움은 쓴다 | 이전 check.mjs가 검사 도중 document/o2o-*.md를 덮어썼다. 새 판은 g1과 g2가 아무것도 쓰지 않고 fill만 인자로 받은 파일 하나를 쓰며 보호 경로는 거부한다 | 이번 세션. troubleshooting 2026-09-08 행 | harness/tools/check.mjs 머리 주석 |
| 2026-09-08 | 프로젝트 .claude/는 자동 삭제되지 않는다 | 정리 sweep 대상은 전부 ~/.claude/ 아래다. 프로젝트 .claude/는 표에 없다. 다만 rules/, skills/, commands/, agents/, workflows/, output-styles/, agent-memory/가 예약 이름이고 .claude/rules/ 아래 .md는 매 세션 컨텍스트에 올라간다 | https://code.claude.com/docs/en/claude-directory | .claude/에 하네스를 넣지 않기로 한 근거. 삭제 위험이 아니라 소유권과 예약 이름이 이유다 |
| 2026-09-08 | 프롬프트에 구체 경로를 박지 않는다 | 마스터 프롬프트가 progress.md와 템플릿 파일의 절대경로를 적고 있어서 디렉터리를 옮길 때마다 프롬프트가 깨졌다. CLAUDE.md 3절 한 곳만 가리키게 바꿨다 | 이번 개편에서 실제 발생 | harness/prompts/harness-ptcf-prompt.md 1절 |
| 2026-09-08 | 중첩 저장소는 상위 gitignore와 간섭하지 않는다 | 상위 저장소가 /99_projects/를 무시하고 있어도 그 안에서 git init을 하면 새 저장소가 독립적으로 동작한다. 상위의 harness/out/ 무시 규칙도 새 저장소에 상속되지 않는다 | 이번 세션 실측 | .gitignore, CLAUDE.md 1절 |
| 2026-09-07 | 상위 .gitignore가 harness/out/를 무시한다 | 하네스 디렉터리 이름 harness/out/이 루트 .gitignore의 빌드 산출물 규칙과 겹친다. 지금은 /99_projects/가 폴더 전체를 제외해서 무해하다. o2o를 자체 저장소로 만드는 순간 harness/out/이 통째로 무시된다. 검증 방법은 저장소 편입 시 git check-ignore harness/out/로 확인하고 !harness/out/ 예외를 넣는 것 | C:/Dev/potenup/.gitignore 확인 | 없음. 저장소 편입 결정 시 처리 |
| 2026-09-07 | 세 디렉터리 성격 분리 | document는 평가 대상, harness-prompts는 실행 중 읽는 양식, harness-docs는 참고용 이력이다. 이름으로 갈라야 평가 입력을 고를 때마다 성격을 다시 판단하지 않는다 | harness/prompts/eval-criteria-ddd.md 8절 첫 항목 | CLAUDE.md 3절, harness/docs/README.md |
| 2026-09-07 | 멱등키는 발급하지 말고 유도한다 | 새 UUID를 발급하면 롤백 뒤 재시도에서 다른 키가 나와 멱등성이 깨진다. 기존 식별자(attemptId, pgTransactionId)에서 결정론적으로 유도하면 롤백해도 같은 키가 나온다 | decisions-08-3.md 9-4 | 08-3 결정 4번 |
| 2026-09-07 | REQUIRES_NEW 안에서 예외를 삼키면 커밋된다 | Spring 프록시가 정상 종료로 판단한다. 예외는 트랜잭션 밖 어댑터에서 잡아야 한다. 안에서 삼키면 부분 커밋이 생긴다 | decisions-08-3.md 9-6 | 08-3 결정 6번 |
| 2026-09-07 | 상태 플래그는 갱신 지점을 열거하지 말고 의미를 정의한다 | 열거 방식은 분기가 늘 때마다 누락이 생긴다. 08-3 검증에서 실제로 났다. 의미를 정의하고 갱신 규칙을 도출하면 새 분기도 규칙에 걸린다 | decisions-08-3.md 9-1 | 08-3 결정 1번 |
| 2026-09-07 | 긴 문서는 heredoc 대신 Write [추측] | 6KB 이하 heredoc 3건은 성공, 9KB 1건은 실패했다. 원인 미확인. 검증 방법은 같은 내용을 절반으로 잘라 두 번 append해 경계를 찾는 것 | 이번 세션 관측 | 없음 |
| 2026-09-07 | 확정 전제는 복사하지 않고 가리킨다 | 07 v2가 Context에 확정 전제 표를 복사해 두었더니 01이 v9에서 v19로 가는 동안 갱신되지 않았다. v3는 01 v20을 가리키고 자주 쓰는 4행만 남겼다 | harness/project-sync/07-o2o-ptcf-prompt.md 4절 세 번째 주의사항 | 07 v3 Context |
| 2026-09-07 | settings.json 빈 hooks 객체 [추측] | "hooks": {}가 유효한지 확인하지 못했다. 키를 두지 않는 방식으로 우회했다. 검증 방법은 훅 단계에서 빈 객체를 넣고 세션 시작 시 잘못된 설정 경고가 뜨는지 보는 것 | 미확인 | 없음 |
| 2026-09-08 | Stop 훅이 받는 입력 | Stop 이벤트의 JSON 입력에 last_assistant_message가 있다. 답변 텍스트를 훅이 그대로 받는다. exit 2면 정지를 막고 대화를 계속시키며 additionalContext로 텍스트를 되먹인다 | https://code.claude.com/docs/en/hooks (2026-09-08 확인) | harness/docs/10-9 7절 |
| 2026-09-08 | 훅 exit 2 뒤의 동작 | [추측] 정지를 막았을 때 모델이 답변을 다시 쓰는지 뒤에 이어 붙이는지 문서 문장으로는 확정 못 한다. 문서는 계속한다고만 적는다. 검증 방법은 훅을 임시로 걸고 일부러 결과 절 없는 짧은 답변을 한 번 내 보는 것 | 같은 문서. 해당 문장 없음 | 미반영. 10-9 12절 미결 |
| 2026-09-08 | 규칙과 규격을 가르는 기준 | CLAUDE.md는 세션마다 자동으로 읽히고 harness/prompts/ 아래 파일은 누가 열어야 읽힌다. 그래서 짧고 매번 보여야 하는 규칙은 CLAUDE.md에 남기고 긴 규격만 옮긴다. 길이가 아니라 자동 로드 여부가 기준이다 | https://code.claude.com/docs/en/memory (2026-09-08 확인) | harness/docs/10-9 2-1절 |
| 2026-09-08 | 훅 exit 2 뒤의 동작. 실측 | 이어 붙인다. 다시 쓰지 않는다. 막힌 답변은 그대로 사용자에게 남고, stderr가 새 입력으로 들어와 모델이 다음 메시지를 이어서 낸다. 정정본은 별도 메시지가 된다. 위 2026-09-08 [추측] 행을 닫는다 | 직접 실측. probe.md를 일부러 결과 절 없이 내보내 차단을 받았다 | harness/docs/10-9 12절 미결 하나 해소 |
| 2026-09-08 | 차단이 대화에 남기는 자국 | 한 메시지 안의 중복이 아니라 대화 수준의 중복이다. 결과 절이 아예 없어 막힌 경우 정정본이 빠진 절을 채우므로 대화 전체로는 결과 절이 하나다. 결과 절이 있는데 행이 모자라 막힌 경우에만 결과 절이 두 번 보인다 | 같은 실측에서 유도 | 미반영. 10-9 7절 판단 근거 |
| 2026-09-08 | 차단 한도의 실제 동작 | answer-gate.mjs의 session_id별 1회 제한이 실측에서 작동했다. 차단 메시지가 남은 차단 횟수 0을 알렸고 그 뒤 같은 세션의 실패는 막히지 않는다. 훅으로 세션이 잠기는 사고는 이 장치가 막는다 | 직접 실측과 손 시험 5건 | harness/tools/answer-gate.mjs 안전 규칙 1 |
| 2026-09-08 | 훅 exit 2 뒤의 동작 (정정) | 실측으로 닫았다. 이어 붙이기다. 정지를 막으면 앞 답변이 그대로 남고 모델이 그 뒤에 이어 쓴다. 그래서 훅 문구를 다시 답하라가 아니라 빠진 것만 이어서 내라로 적어야 답변이 두 번 나오지 않는다 | 이 저장소에서 직접 실측. Stop 훅에 answer-gate.mjs를 걸고 결과 절 없는 답변을 냈다 | harness/docs/10-9 7절. 위 미확인 행을 폐기한다 |
| 2026-09-08 | 훅 차단 한도의 기준 | prompt_id로 센다. session_id로 세면 세션당 한 번밖에 못 고친다. 무한 반복은 한 턴을 두 번 막을 때만 나므로 턴 단위로 막으면 안전하면서 매 턴 고칠 수 있다 | 같은 실측 | harness/tools/answer-gate.mjs |
| 2026-09-08 | 한 작업 트리를 여러 세션이 쓰면 브랜치가 발밑에서 바뀐다 | 세션 시작 때 확인한 브랜치를 그 세션 내내 믿을 수 없다. 2026-09-08에 세 번 겪었다. README 행 중복, 테스트 이름 중복, 그리고 backend 파일 셋을 덮어쓴 것이다. 마지막 건은 상대가 이미 커밋한 파일을 내가 작업 트리에서 덮어써서 git status에 수정으로 떴다. 정석은 worktree인데 계약의 절대경로 265곳이 막고 있다. 그전까지의 대응은 셋이다. 파일을 쓰기 직전마다 git status를 보고, 되돌리기 전에 내 판을 딴 데 보관하고, 커밋할 때 경로를 명시한다 | 이번 세션 실측. troubleshooting 2026-09-08 19:45행 | CLAUDE.md 4-1, harness/state/README.md 여러 세션 절 |
| 2026-09-09 | 사용자에게 줄 명령은 PowerShell 5.1 문법으로 쓴다 | 이 기계의 사용자 터미널은 Windows PowerShell 5.1이다. 그 판에 없는 것 넷을 쓰면 실행 전에 파서 오류로 죽는다. 체인 연산자 &&와 \|\|, 삼항 연산자, null 병합 연산자, bash 명령 이름(cp, rm, touch, head). 대신 쓰는 것은 블록당 명령 하나, 조건 실행이 필요하면 A; if ($?) { B }, 복사는 Copy-Item이다. 내가 검사 스크립트를 돌릴 때 쓰는 셸과 사용자가 쓰는 셸이 다르다는 것이 이 실수의 뿌리다 | 2026-09-08 17:05 bash cp 실패, 2026-09-09 13:52 && 파서 오류. 두 번 실측 | harness/prompts/dev-ptcf-prompt.v3.md 다음 판의 Format 절 후보. 동결 중이라 지금 넣지 않는다 |
| 2026-09-09 | 검사기가 읽는 칸의 작성 규칙 | check.mjs가 값으로 읽는 칸에는 값만 적는다. 설명을 덧붙이면 문장 전체가 값이 된다. 2026-09-09 하루에 두 번 걸렸다. 하나는 결정표의 남은 실제 치명 지적 칸에 이유를 붙인 것이고 다른 하나는 계약 경로 뒤에 마침표를 붙인 것이다. 설명은 표 밖 문단에 둔다 | 실측. g2.fatal-remaining과 g2.version-path 실패 | harness/decisions/task-S2-R1.md 인계 표 아래 문단 |
| 2026-09-09 | 한글은 프로세스 경계에서 깨진다 | 세 번 걸렸다. Spring Initializr 요청 파라미터의 한글이 400, HTTP 헤더 값의 한글을 JDK 클라이언트가 거부, curl -d의 인라인 한글 본문이 400. 파일로 넘기거나 ASCII로 바꾸면 전부 해결된다. 프로세스와 프로토콜 경계를 넘는 값은 ASCII로 두거나 UTF-8 파일로 넘긴다 | troubleshooting 2026-09-09 세 행에서 승격 | 아직 없음 |
| 2026-09-09 | 한 행동이 두 표에 걸쳐 있으면 둘 다 대조한다 | 4단계 완료 조건을 06-2 6절 CRC 한 표로 잡았더니 06-4 1-2 계약표 Post 열의 이벤트 발행을 통째로 놓쳤다. CRC는 책임을 적고 계약표는 후행조건을 적어서 같은 행동이 두 표에 나뉘어 있다 | 이번 세션. 사용자가 테스트를 의심해서 대조하다 발견 | troubleshooting 2026-09-09 계약 하강 행 |
| 2026-09-09 | 테스트가 다 통과해도 운영 설정 구멍은 안 보인다 | 접속 정보를 src/test/resources에만 넣고 src/main/resources에 안 넣었다. 테스트는 자기 설정 파일을 따로 갖기 때문에 28건이 다 통과했는데 gradlew bootRun은 DataSource를 못 찾고 죽었다. 단계 완료 조건에 실제 기동을 넣는다 | 이번 세션 실측 | 아직 없음 |
| 2026-09-09 | 미결을 쪼갤 때 종속 관계를 먼저 본다 | 대조 방식과 필드 존재와 필드 시점 셋을 나란히 놓고 따로 물었는데 실제로는 첫째가 정해지면 나머지가 따라오는 구조였다. 사용자가 세 번 답할 뻔했다 | 이번 세션. 사용자 결정 한 번으로 셋이 닫힘 | task-S9-catalog 계약 2-2절 |
| 2026-09-09 | 세션의 창 크기는 세션에서 읽는다 | 문서의 모델별 기본 임계 문장을 보고 Opus 5를 20만으로 단정했는데 실제 세션은 100만이었다. 그 오판으로 사용자에게 40퍼센트가 불가능하다고 잘못 답했다. /context가 창 크기와 항목별 사용량을 다 보여 준다 | 이번 세션 실측 | 아직 없음 |
| 2026-09-09 | 압축 기준보다 시작 비용이 크다 | 대화 없이도 20만 2천이 차 있다. 그중 MCP 도구 지연분이 13만 4천으로 3분의 2다. 압축은 메시지만 줄이고 시작 비용은 압축 뒤에도 다시 올라온다. 안 쓰는 커넥터를 끄는 것이 압축 기준을 낮추는 것보다 효과가 크다 | 이번 세션 /context 실측 | .claude/settings.local.json |
| 2026-09-09 | 권한 규칙에 부정 문법이 없다 | Read와 Edit은 gitignore 패턴을 쓰지만 규칙 목록에 예외를 적을 자리가 없고 넓은 deny가 좁은 allow를 덮는다. 넓은 deny에 구멍을 내려면 접미사를 열거해야 하는데 그러면 새 접미사가 안 막힌다. 규칙을 좁히는 대신 대상 파일 이름을 패턴 밖으로 빼는 편이 안전을 안 깎는다 | https://code.claude.com/docs/en/permissions. 이슈 39 | backend/env.example |
| 2026-09-09 | 표 머리글 선택자와 하위 호환 | check.mjs가 경로 칸을 머리글의 낱말 포함으로 고른다. 절대경로를 경로로 바꾸면 includes 특성상 옛 머리글도 그대로 잡혀 하위 호환이 공짜다. 선택자를 좁은 낱말로 두면 이 이득이 사라진다 | 실측. 이슈 26 작업 | harness/tools/check.mjs 233행과 286행 |
| 2026-09-09 | 양식의 절 제목이 곧 필수 목록이다 | 이슈 29를 열 때는 양식에 기계가 읽을 필수 절 목록을 따로 둬야 한다고 적었다. 실제로는 양식의 ## 제목이 이미 그 목록이다. 계약은 번호를 붙이고 양식은 안 붙이므로 번호와 괄호 주석만 떼면 대조된다 | 실측. 이슈 29 작업 | harness/tools/check.mjs sectionKeys |
| 2026-09-09 | 09-1 전사본은 정본과 같다 | FigJam 보드 렌더 이미지와 harness/project-sync/09-1-o2o-board-v2.md를 11개 절로 대조해 전 항목 일치를 확인했다. 커맨드 27과 이벤트 30, 점선 테두리 8곳, 정책 카드 8장이 다 맞는다. 유일한 차이는 정책 카드 나열 순서이고 보드는 2차원 표는 1차원이라 생기는 것이다 | 실측. 2026-09-09 사용자 제공 이미지 | harness/docs/10-11 |
| 2026-09-10 | CodeRabbit 스키마 주소 | 공식 참고 문서가 적는 https://coderabbit.ai/integrations/schema.json은 404다. 응답하는 주소는 schema.v2.json이고 원본은 storage.googleapis.com/coderabbit_public_assets/schema.v2.json이다. 스키마가 draft 2020-12라 ajv 기본 빌드로는 못 읽고 ajv/dist/2020을 써야 한다. 최상위가 additionalProperties false라 키 이름 오타는 검증에서 걸린다 | https://docs.coderabbit.ai/reference/yaml-template 와 실측 | harness/docs/10-16 6절 |
| 2026-09-10 | CodeRabbit 비공개 저장소의 Free 요금제 | Free는 비공개 저장소에서 PR 요약만 준다. 줄 단위 PR 리뷰는 Essentials부터다. 줄 단위 리뷰를 Free로 쓰려면 VS Code 확장이나 CLI로만 가능하다. 새 조직에 14일 Advanced 체험이 붙는다 | https://docs.coderabbit.ai/management/plans | harness/docs/10-16 8절 |
| 2026-09-10 | code_guidelines.filePatterns는 대체다 | 배열을 적으면 기본 목록에 더해지는 것이 아니라 대체한다. 기본 목록에 CLAUDE.md와 AGENTS.md가 이미 있으므로 직접 적을 때 그 둘을 다시 적지 않으면 봇이 저장소 규칙 파일을 안 읽는다 | 스키마 knowledge_base.code_guidelines.filePatterns 설명 | harness/docs/10-16 3절 |
| 2026-09-10 | CodeRabbit OSS 자리는 Free와 다르다 | 공개로 바꾼다고 Free 요금제가 좋아지는 것이 아니다. Free는 공개든 비공개든 PR 요약만 준다. 줄 단위 리뷰를 공짜로 받는 자리는 OSS이고 유료 구독 없이 Team 기능을 준다. 한도가 별 개수를 따라가서 별 0개면 시간당 PR 리뷰 1건과 리뷰당 파일 100개다. 자격이 자동인지 신청인지는 공식 문서에 없다 | https://docs.coderabbit.ai/management/plans 와 https://www.coderabbit.ai/pricing | harness/docs/10-16 8절 |
| 2026-09-11 | CodeRabbit path_filters는 minimatch다 | 공식 문서가 path_filters에 minimatch를 쓴다고 적는다. 그래서 어느 파일이 막히는지를 봇에 물어보지 않고 같은 라이브러리로 로컬에서 잴 수 있다. 저장소 파일 목록에 패턴을 걸어 막히는 것과 남는 것을 세면 된다. 포함 패턴과 제외 패턴이 섞였을 때 어느 것이 이기는지는 문서에 없다. 제외만 쓰면 순서와 무관하다 | https://docs.coderabbit.ai/guides/review-instructions 와 실측 | harness/docs/10-16 6-1절 |
