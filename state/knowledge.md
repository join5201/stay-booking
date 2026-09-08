# 누적 지식 (state/knowledge.md)

최초 작성: 2026-09-07
최종 갱신: 2026-09-07 (H2 후속. 3행 추가)

왜 이 파일이 필요한가: 지식의 부채를 갚는 자리다. 두 번 걸린 것, 문서 어디에도 없어서 매번 다시 알아내는 것을 여기 모은다. 이 파일이 없으면 같은 확인 작업을 매 세션 반복한다.

무엇을 적는가 세 가지.
1. state/troubleshooting.md에서 두 번째로 나온 실패 원인. 승격된 것.
2. 공식 문서로 확인한 설정 키, 규약, 명령. 확인한 URL을 같이 적는다(CLAUDE.md N6).
3. 확인하지 못한 것. [추측] 태그와 검증 방법을 같이 적는다.

무엇을 적지 않는가: 설계 결정은 decisions/에, 단계 진행은 state/progress.md에, 실패 하나하나는 state/troubleshooting.md에 적는다. 여기는 재사용되는 지식만 남긴다.

행 형식: 임시다. 날짜, 항목, 알게 된 것, 출처, 승격 위치 다섯 칸이다. 정식 형식은 H5에서 확정한다. 승격 위치는 이 지식이 최종적으로 어느 문서 조항이 되었는지다. 비어 있으면 아직 문서에 반영되지 않았다는 뜻이다.

규칙: 추가 전용. 틀린 것으로 밝혀지면 지우지 말고 새 행에 정정을 적고 원래 행의 승격 위치에 폐기를 적는다.

| 날짜 | 항목 | 알게 된 것 | 출처 | 승격 위치 |
|---|---|---|---|---|
| 2026-09-07 | 권한 규칙 평가 순서 | deny, ask, allow 순으로 평가한다. 넓은 deny는 더 좁은 allow를 덮는다. allow는 폴더 신뢰 뒤에 적용되고 deny와 ask는 즉시 적용된다 | https://code.claude.com/docs/en/permissions | CLAUDE.md 4절 |
| 2026-09-07 | PowerShell 규칙과 별칭 | PowerShell 규칙은 Bash와 같은 모양이다. 별칭이 정규화되므로 PowerShell(Remove-Item *)가 rm과 del에도 걸린다 | 같은 문서 | .claude/settings.json deny |
| 2026-09-07 | Bash 규칙 접두 비교 | Bash(cmd:*)는 Bash(cmd *)와 같다. 첫 별표 앞은 문자열 그대로 비교한다 | 같은 문서 | CLAUDE.md 4절. 경로 구분자를 슬래시로 고정하는 근거 |
| 2026-09-07 | Claude Code는 AGENTS.md를 읽지 않는다 | CLAUDE.md만 읽는다. 같이 읽히게 하려면 @AGENTS.md 임포트를 써야 한다 | https://code.claude.com/docs/en/memory | AGENTS.md 머리말. 이 하네스는 일부러 잇지 않는다 |
| 2026-09-07 | Codex의 AGENTS.md 탐색 | Codex 홈을 먼저 보고 Git 루트에서 현재 디렉터리까지 내려오며 모은다. 가까운 파일이 뒤에 와서 우선한다. 디렉터리당 한 파일, 기본 상한 32 KiB | https://learn.chatgpt.com/docs/agent-configuration/agents-md | AGENTS.md 0절 운용 조건 |
| 2026-09-07 | 이 폴더는 git 추적 밖이다 | 루트 .gitignore의 /99_projects/가 제외한다. o2o 자체 저장소는 없다. git status와 git diff로 변경을 볼 수 없다 | C:/Dev/potenup/.gitignore 확인 | CLAUDE.md 1절 |
| 2026-09-07 | 루트 .gitignore가 out/를 무시한다 | 하네스 디렉터리 이름 out/이 루트 .gitignore의 빌드 산출물 규칙과 겹친다. 지금은 /99_projects/가 폴더 전체를 제외해서 무해하다. o2o를 자체 저장소로 만드는 순간 out/이 통째로 무시된다. 검증 방법은 저장소 편입 시 git check-ignore out/로 확인하고 !out/ 예외를 넣는 것 | C:/Dev/potenup/.gitignore 확인 | 없음. 저장소 편입 결정 시 처리 |
| 2026-09-07 | 세 디렉터리 성격 분리 | document는 평가 대상, harness-prompts는 실행 중 읽는 양식, harness-docs는 참고용 이력이다. 이름으로 갈라야 평가 입력을 고를 때마다 성격을 다시 판단하지 않는다 | harness-prompts/eval-criteria-ddd.md 8절 첫 항목 | CLAUDE.md 3절, harness-docs/README.md |
| 2026-09-07 | 멱등키는 발급하지 말고 유도한다 | 새 UUID를 발급하면 롤백 뒤 재시도에서 다른 키가 나와 멱등성이 깨진다. 기존 식별자(attemptId, pgTransactionId)에서 결정론적으로 유도하면 롤백해도 같은 키가 나온다 | decisions-08-3.md 9-4 | 08-3 결정 4번 |
| 2026-09-07 | REQUIRES_NEW 안에서 예외를 삼키면 커밋된다 | Spring 프록시가 정상 종료로 판단한다. 예외는 트랜잭션 밖 어댑터에서 잡아야 한다. 안에서 삼키면 부분 커밋이 생긴다 | decisions-08-3.md 9-6 | 08-3 결정 6번 |
| 2026-09-07 | 상태 플래그는 갱신 지점을 열거하지 말고 의미를 정의한다 | 열거 방식은 분기가 늘 때마다 누락이 생긴다. 08-3 검증에서 실제로 났다. 의미를 정의하고 갱신 규칙을 도출하면 새 분기도 규칙에 걸린다 | decisions-08-3.md 9-1 | 08-3 결정 1번 |
| 2026-09-07 | 긴 문서는 heredoc 대신 Write [추측] | 6KB 이하 heredoc 3건은 성공, 9KB 1건은 실패했다. 원인 미확인. 검증 방법은 같은 내용을 절반으로 잘라 두 번 append해 경계를 찾는 것 | 이번 세션 관측 | 없음 |
| 2026-09-07 | 확정 전제는 복사하지 않고 가리킨다 | 07 v2가 Context에 확정 전제 표를 복사해 두었더니 01이 v9에서 v19로 가는 동안 갱신되지 않았다. v3는 01 v20을 가리키고 자주 쓰는 4행만 남겼다 | project-sync/07-o2o-ptcf-prompt.md 4절 세 번째 주의사항 | 07 v3 Context |
| 2026-09-07 | settings.json 빈 hooks 객체 [추측] | "hooks": {}가 유효한지 확인하지 못했다. 키를 두지 않는 방식으로 우회했다. 검증 방법은 훅 단계에서 빈 객체를 넣고 세션 시작 시 잘못된 설정 경고가 뜨는지 보는 것 | 미확인 | 없음 |
