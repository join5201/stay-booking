# O2O 하네스 결정 문서 PDF 대조 (10-7)

최초 작성: 2026-09-07
최종 갱신: 2026-09-07 v1
성격: 10-2-o2o-harness-decisions v1을 강의 PDF 8종과 대조한 결과. 10-2 v2 갱신의 입력으로 쓴다. 6절이 v2 반영안이다.
입력: 10-2 v1, PDF 01(컨텍스트 앵커링), 02(컨텍스트 엔지니어링), 03(CLAUDE.md), 04(SDD와 Plan Mode), 07(스킬), 08(훅), 09(서브에이전트와 eval), 10(headless와 거버넌스). 05, 06은 첨부에 없음.
검증 방법: PDF 01은 직접 읽음. 02, 03, 04, 07, 08, 09, 10은 PDF별 독립 에이전트가 전수 읽고 쪽 번호와 원문 인용을 붙여 추출. 그중 결정 문서에 영향이 큰 PDF 10의 13~19쪽과 PDF 09의 5~10쪽은 직접 재확인. SDK와 API 사실은 공식 소스로 확인(4절).

번호 정정: 처음 10-3으로 저장했으나 로컬 폴더의 10-3(입력 팩), 10-4(실행 방식 결정), 10-5(인계), 10-6(구현 계획)과 충돌해 10-7로 옮겼다. 본문에서 10-3이라고 자기 참조한 곳은 이 문서를 뜻한다.
적용 범위 정정: 로컬 10-4 v4가 실행 방식을 Claude Code 생성 + Codex 평가 + 사용자 전달로 바꾸고 API 기반 Java 하네스를 보류했다. 따라서 이 문서 2-2의 프롬프트 캐싱, usage 기록, 누적 토큰 상한, 종료 코드, ApiPort 관련 항목과 4절(SDK 확인 사실)은 보류 하네스의 부록이다. 나머지(실패 사유 되먹임, 진행 기록 형식, 승인 원문 보존, 지적 ID, 종료 신호 문장, G2 기계 검사, 평가 기준 파일, 07 v3 구조)는 수동 실행에서도 그대로 유효하며 10-8에 정리했다.

## 0. 판정 요약

Thought: 10-2의 확정 27행과 미확정 15행을, PDF가 말하는 하네스 원리(결정론 게이트, 격리 평가, 상태 외부화, 비용 통제, 인계 문서)와 대조하고, PDF의 Claude Code 전용 기능은 이식 불가로 걸러낸다.
Action: PDF별 추출을 결정 문서 행 단위로 병합하고, 미확정 항목 중 PDF가 답을 못 주는 SDK 사실은 GitHub 소스와 Maven Central과 공식 문서로 대신 확인했다.
Observation: 확정 사항과 정면 충돌하는 PDF 권고는 없다. 대신 10-2에 아예 없는 항목이 다섯 묶음 나왔다(halted 뒤 작업 트리 처리, 실패 사유 되먹임, 토큰 사용량 기록과 비용 상한, 프롬프트 캐싱, 승인 원문 보존). 미확정 15행 중 3행(SDK 시스템 프롬프트 메서드, stopReason 비교, SDK 버전)은 이번 확인으로 확정 가능하다.
Answer: 10-2 v1은 게이트와 격리 평가 설계에서 PDF 원리와 잘 맞는다. 빠진 것은 실패 이후 경로와 비용 관측이다. 1절의 전제 문제 두 건을 먼저 정리한 뒤, 6절 반영안대로 v2를 만들면 된다.

## 1. 답변 전에 짚을 전제 문제

### 1-1. 10-2가 입력으로 적은 두 문서가 프로젝트에 없다

10-2는 입력을 10-o2o-harness-plan v2와 10-1-o2o-harness-troubleshooting v1이라고 적었지만 프로젝트 docs에는 둘 다 없다(project_search 결과). 그래서 10-2의 위치 열(계획 N절)이 가리키는 원문은 확인하지 못했고, 이 대조는 10-2 본문만을 대상으로 한다. 아래 보완 후보 중 일부는 계획 v2에 이미 있을 수 있다. 그런 항목은 [추측]을 붙였다. 계획 v2와 10-1을 프로젝트에 올린 뒤 이 문서를 다시 대조해야 한다.

### 1-2. check의 dirty 정지가 정상 흐름을 막을 수 있다 [추측]

10-2 확정 표에 "check가 .git 없음과 dirty를 나눠 정지"와 "커밋 시점은 Step 종료 시" 둘 다 있다. step 세션이 초안을 쓰고 review 세션이 reviews/를 쓰는 동안 트리는 미커밋 상태이므로, check가 트리 전체를 dirty로 잡으면 step 다음의 review와 apply 자체가 정지된다. halted 뒤에는 더 확실히 막힌다. 계획 v2에 dirty 판정 범위(하네스 관리 경로 제외, 또는 명령별 check 범위)가 있으면 해소되지만 10-2에는 없다. 이건 PDF와 무관한 10-2 내부 결함 후보라 가장 먼저 확인해야 한다. 해결 방향은 2-1 첫 행.

### 1-3. 결정 주체 "하네스"가 두 뜻을 겸한다

10-2 2절의 결정 주체 "하네스"는 (가) 하네스를 만드는 개발 세션(사람이 승인) (나) 하네스 안에서 도는 LLM 두 뜻으로 읽힌다. 07 v3 작성 같은 항목이 (나)라면 PDF 03 16쪽 "헌법 수정은 당신이"와 충돌한다. (가)로 통일하고 표에 한 줄 정의를 두는 편이 맞다.

### 1-4. PDF는 전부 Claude Code 교재라 설정 키와 명령은 하나도 이식되지 않는다

하네스는 Anthropic Java SDK로 Messages API를 직접 호출하고 도구 루프가 없다. 그래서 PDF의 settings.json, hooks, .claude/agents, 슬래시 명령, CLI 플래그, 단축키는 전부 해당 없음이다. 이식되는 것은 원리뿐이다. 이 대조에서 "이식 구분" 열의 뜻은 다음과 같다. 원리: 하네스 코드나 프롬프트 설계로 옮길 수 있음. 기능: Claude Code 런타임 기능이라 그대로는 불가하고 대체 구현이 필요함.

## 2. 보완 후보

우선순위: P1은 Phase 0 전에 결정해야 하는 것(결정 문서 결함 또는 이후 설계의 전제), P2는 Phase 1~2, P3은 Phase 3~4.

### 2-1. P1: 실패 이후 경로와 인계 기록

| 대상 행 | 보완 내용 | 근거 | 이식 구분 |
|---|---|---|---|
| 확정 git, 커밋 시점, G1 실패 처리 | halted 시 작업 트리 처리 규칙이 없다. PDF 10은 검증 통과 시에만 커밋하고 실패 시 git restore로 안정 상태로 되돌린다. 선택지 셋. (a) 산출물 restore (b) step N round R halted 별도 커밋 (c) 실패 초안을 failed/ 같은 디렉토리로 옮기고 나머지 restore. 설계 문서 하네스는 실패 초안이 10-1 트러블슈팅의 진단 자료이므로 (b) 또는 (c)가 PDF의 "안정 상태 복귀 + 실패 기록 보존" 원리에 맞다. 1-2의 dirty 범위 결정과 함께 정한다 | PDF 10 15, 18, 19쪽 "git restore로 코드를 되돌리고 progress.txt에 실패 원인만 적은 뒤" | 원리 |
| 확정 상태 파일 | halted 뒤 재실행 시 어느 status에서 재개하는지 없다. drafted면 review부터, reviewed면 apply부터처럼 status별 재개 지점을 표로 둔다. PDF 10의 부분 성공 보존 원리 | PDF 10 15쪽 "4번째 작업부터 바로 재개" | 원리 |
| 확정 G1 실패 처리, 재평가 | 재요청과 재평가 프롬프트에 직전 실패 사유를 넣는 규칙이 없다. PDF 02, 04, 08, 10이 모두 같은 말을 한다. 교훈 없는 재시도는 같은 실패를 반복한다. G1 재요청에는 어느 검사가 어느 위치에서 걸렸는지, 재평가에는 결정표의 거부 이유와 반박을 넣는다. 재요청 1회 상한은 PDF 08의 stop_hook_active(1회만 차단)와 같은 원리라 유지 | PDF 10 13, 17, 18쪽, PDF 08 2, 17, 24쪽, PDF 04 9, 13쪽 "헌법 조항 번호를 인용해 지적", PDF 02 10쪽 | 원리 |
| 확정 상태 파일, 3절 6번 progress.md | progress.md 행 형식이 없다. PDF 02의 HANDOFF.md 3요소(진행 상황, 성공과 실패 내역, 다음 할 일)와 PDF 10의 progress.txt 4요소(attempt 번호, 작업 ID, 실패 원인, 교훈)를 합쳐 한 행의 필수 필드로 정한다. 여기에 아래 2-2의 usage 기록을 같이 넣는다. 첫 항목(3절 6번)을 쓰기 전에 형식부터 정해야 한다 | PDF 02 6, 13쪽, PDF 10 17, 22쪽 | 원리 |
| 확정 계약 확인 | y 승인한 Task 블록이 파일로 남지 않는다. PDF 04와 09의 Sprint Contract는 계약을 파일로 남기고 평가자가 그 파일만 기준으로 본다. 승인한 Task 블록 원문을 tasks/step-N-R.md 또는 progress.md에 저장하고, 평가 세션 허용 목록에 넣을지 정한다. 하네스가 Task 블록에 자동 주입하는 것(날짜, 전제 표, inputs 목록)도 승인 화면에 그대로 보이게 한다 | PDF 04 3, 10, 12쪽, PDF 09 7쪽 "이 계약서를 기준으로만 냉정하게 평가", PDF 08 7쪽 | 원리 |
| 확정 트러블슈팅 | 10-1 행 형식에 PDF 10의 4요소가 들어 있는지 확인. 10-1(사람용)과 progress.md(하네스용) 중 어느 쪽이 재요청 프롬프트 주입 원천인지 정한다. 같은 원인이 두 번 나오면 07 DO NOT 블록에 한 줄 추가를 표준 반영 위치로 둔다 | PDF 10 17쪽, PDF 03 11, 16쪽 "같은 실수를 두 번 하면 그건 헌법의 부재 탓" | 원리 |

### 2-2. P2: 비용 관측, 프롬프트 캐싱, 호출 계약

| 대상 행 | 보완 내용 | 근거 | 이식 구분 |
|---|---|---|---|
| 확정 API 호출 형태, 미확정 maxTokens | 호출마다 usage(input_tokens, output_tokens, cache_creation_input_tokens, cache_read_input_tokens)와 stop_reason, 모델, 소요 시간을 기록하는 규칙이 없다. PDF 02, 04, 07, 10이 /cost를 시점별로 재라고 하고 PDF 08은 Stop 훅에서 토큰과 비용을 기록한다. maxTokens 실측(Phase 2)과 2분할 비교(Phase 4)는 이 기록이 있어야 가능하다. USD는 API가 주지 않으므로 하네스가 단가표로 계산하거나 토큰만 기록한다 | PDF 10 3, 11, 24쪽, PDF 08 7쪽, PDF 02 5, 13쪽, PDF 04 8, 14쪽 | 원리. /cost 자체는 기능 |
| 확정 API 호출 형태, 미확정 maxTokens | 비용 상한이 없다. PDF 10의 max-budget-usd와 max-turns에 해당하는 것을 하네스가 직접 둬야 한다. 요청당 maxTokens 외에 Step당 또는 실행당 누적 토큰 상한을 두고 초과 시 halted. 수치 근거는 PDF에 없다(0.5 USD는 Node 예시) | PDF 10 1, 2, 11쪽 | 원리. 플래그는 기능 |
| 신설 | 프롬프트 캐싱(Prompt Caching) 적용 여부가 없다. 평가 A와 B는 같은 대상 문서와 입력 팩을 공유하고 순차 실행이므로 공통 접두부(system + Context 공통부)를 캐시하면 두 번째 호출 입력 비용이 준다. 기본 TTL 5분 안에 A 다음 B가 돌면 적중한다. 조건과 단가는 4-4 참조. 주의: 최소 캐시 토큰이 모델별로 512~4096으로 달라서 모델 문자열 판정(2절 첫 행)의 입력이 된다 | PDF 01 3, 13쪽 "캐시 골든 타임 (5분)", PDF 02 1, 3, 13쪽 | 원리. API 설정은 4-4에서 확인 |
| 확정 G1 실패 처리, 명령 | halted 시 프로세스 종료 코드를 0이 아닌 값으로 명시. PDF 10은 종료 코드를 유일한 성공 기준으로 삼는다. 나중에 셸이나 CI로 감쌀 때 필요 | PDF 10 11, 12, 19쪽 "Exit Code 0 (성공)만이 유일한" | 원리 |
| 확정 명령 check, 프로파일 | check가 live 프로파일에서 ANTHROPIC_API_KEY 존재를 확인하고 없으면 정지. PDF 08 SessionStart 용도와 PDF 10 인증 주의와 일치. fake 프로파일에서는 검사하지 않는다 | PDF 08 6쪽, PDF 10 2쪽 | 원리 |
| 확정 Phase 순서 1 뼈대 | Phase 1에 게이트 단독 테스트를 넣는다. PDF 08은 빈 JSON을 stdin에 넣어 훅을 단독 실행한다. G1과 G2를 순수 함수로 두고 빈 응답, 잘린 응답(stop_reason max_tokens), 날짜 누락, 볼드 포함 응답을 fake로 넣어 판정을 확인 | PDF 08 29쪽 | 원리 |
| 확정 게이트 | 게이트 표에 실행 시점과 적용 세션을 추가. 차단 목록 검사와 y 승인은 호출 전(PreToolUse 대응), G1은 응답 직후(PostToolUse 대응), G2는 apply의 done 전환 직전(Stop 대응). 모든 세션에 모든 게이트를 걸지 않는다. 검사 순서는 싼 것(코드) 먼저, 비싼 것(LLM, 사람) 나중 | PDF 08 1, 5, 8, 9쪽, PDF 02 4쪽 | 원리 |
| 확정 게이트, 미확정 stopReason | 게이트 입력을 객체 하나로 정의(세션 종류, Step, round, 대상 경로, 응답 본문, stop_reason, usage). G1 잘림 검사는 stop_reason을 읽고, 보조로 Format 블록의 종료 신호 문장 존재를 본다(아래 07 v3 행) | PDF 08 2쪽, PDF 07 15, 17, 19, 21쪽 "반드시 이 문장으로 끝낼 것" | 원리 |
| 확정 평가 입력, 미확정 차단 목록, 3절 4번 | 차단 목록의 매칭 규칙(경로 정규화, 대소문자, 절대경로 변환)과 예외 메시지 형식(걸린 파일명 + 상수 항목명)을 상수 옆에 적는다. 허용 목록이 1차 방어, 차단 목록은 허용 목록에 잘못 들어온 파일을 잡는 2차 방어라는 역할 구분을 적으면 전수 작성 범위가 정해진다. 차단 목록을 코드 상수로 둔 결정은 맞다. PDF 08 16쪽 스크린샷에서 에이전트는 차단당하자 훅 비활성화를 대안으로 제안했다 | PDF 08 13, 14, 16쪽, PDF 07 3, 4쪽 화이트리스트 정의 | 원리 |
| 미확정 .gitignore | 원칙: 공유 설정은 커밋, 개인 설정과 비밀은 제외. 후보: .env 계열, API 키가 들어갈 수 있는 파일, application-live 로컬 오버라이드, build/, .gradle/, IDE 파일. Java용 목록은 PDF에 없다 | PDF 01 5~8쪽 deny Read(.env), PDF 03 16쪽, PDF 08 10쪽, PDF 10 5쪽 | 원리 |
| 확정 프롬프트, 3절 3번 07 v3 | 07 v3 구조 원칙 넷. (1) 규칙부(07)와 자료부(인라인 문서)를 프롬프트 안에서 구분 표기. (2) 규칙은 코드로 검증 가능한 1줄 규칙만. "간결하게" 같은 모호한 지침은 삭제. (3) 규칙에 조항 번호를 붙여 G1 재요청과 결정표에서 인용 가능하게. (4) DDD 일반론은 빼고 O2O 고유 전제와 파일 구조만 남김. 크기 상한 수치(PDF 200줄, 500줄)는 경험칙이라 그대로 쓰지 않고 실측 | PDF 03 6, 9, 10, 12쪽, PDF 07 20, 21쪽 "검증 가능 1줄 규칙만", PDF 04 8, 9쪽 | 원리 |
| 확정 프롬프트 | Format 블록 끝에 고정 종료 문장을 강제(PDF 07의 5개 스킬 전부). Task 블록에 부정형 제약(건드리지 말 절, 쓰지 말 문서)을 쓰는 규칙. 평가 Task 첫 줄에 "대상 문서를 읽고 1줄 요약을 먼저 출력"을 넣어 입력을 실제로 읽었는지 G1 전에 확인 | PDF 07 14, 15, 16쪽, PDF 02 7, 10쪽, PDF 04 10쪽 | 원리 |
| 확정 프롬프트 | 07 공통부(Format)와 Task 블록이 충돌할 때 우선순위가 없다. Format은 Task가 덮어쓸 수 없고 Task는 Step 국소 지시라고 명시 | PDF 03 2, 5쪽 (PDF 자체가 2쪽과 5쪽에서 상충하므로 원칙만 빌림) | 원리 |
| 확정 프롬프트, 평가 입력 | 프롬프트 배치 순서. 평가 대상과 판정 기준을 앞과 끝에, 참고용 입력 팩을 중간에. PDF는 Lost in the Middle을 근거로 들지만 출처가 없고 최신 모델에서의 재현 여부는 미확인 [추측]. 순서 규칙을 정하는 것 자체는 비용이 없으니 넣어 두고 Phase 4에서 검증 | PDF 02 3, 4쪽 | 원리 |
| 미확정 Java SDK 시스템 프롬프트 메서드, stopReason, SDK 버전 | 이번에 확인됨. 4-1~4-3 참조. 세 행은 확정으로 옮길 수 있다 | 공식 소스 | 해당 |

### 2-3. P3: 평가 설계와 Phase 4 판정 자료

| 대상 행 | 보완 내용 | 근거 | 이식 구분 |
|---|---|---|---|
| 확정 상태 파일 reviews/ | 평가 결과 옆에 기계 판독 행(Step, round, 평가자, 치명 수, 보통 수, 확인필요 수, 사용자 수용 수)을 CSV로 append. Phase 4의 2분할 판정과 G1 산문 가드 판정의 비교 데이터. PDF 09는 "정확하게 나오는 것이 중요"라고 하므로 검출 수가 아니라 사용자가 수용한 행 수를 기준으로 삼는다 | PDF 09 9, 15, 20, 23쪽 | 원리 |
| 확정 결정표 | 결정표에 사용자 추가 항목 절을 둔다. PDF 04의 review.md는 사람이 승인 외에 추가 Criteria 3개 이상을 직접 쓴다. 현재 결정표는 평가자 행에 답만 하고 사용자 자신의 지적을 넣는 자리가 없다 | PDF 04 12, 14쪽 | 원리 |
| 확정 재평가 | review_round 상한 도달 시 status가 halted인지 reviewed인지 없다. PDF 09의 maxTurns는 한계 도달 시 종료다. 명시 필요 | PDF 09 3, 20쪽 | 원리 |
| 확정 평가 세션 수와 실행 | A와 B가 독립이면 PDF 09 분할 축 ④에 따라 병렬 가능하다. 순차를 택한 이유(비용, 결과 병합 순서, 캐시 적중)를 확정 표에 적는다. 캐시를 쓰면 순차가 오히려 유리하다 | PDF 09 5쪽 | 원리 |
| 미확정 평가자 2분할 유지 여부 | 판정 기준으로 PDF 09 분할 축 5개를 쓴다. ①A와 B가 다른 입력을 필요로 하는가 ②다른 권한이 필요한가 ③다른 편향을 잡는가 ④병렬 가능한가 ⑤다른 모델이 적합한가. 다섯 다 아니오면 합친다. 데이터는 같은 문서 N개를 분할과 통합 평가자에 각각 돌려 사용자 수용 행 수를 비교 | PDF 09 5, 10, 23쪽 | 원리 |
| 미확정 평가자 few-shot | PDF에 few-shot 근거가 없다. 가장 가까운 것은 스킬 디렉토리의 examples/ 패턴(형식 예시를 별도 파일로 두고 본문에서 링크). few-shot을 프롬프트 본문에 박지 않고 examples/에 두고 하네스가 읽어 Format 뒤에 인라인하는 구조만 채택. 어느 행을 쓸지는 여전히 미정 | PDF 07 4, 22쪽 | 원리 |
| 확정 평가 통과선 | 치명 판정 기준을 PDF 09 20쪽처럼 번호 매긴 하드 임계값 목록으로 쓰고, 각 항목이 검증 가능한 문장인지 점검. "설계가 좋은가" 같은 문항은 PDF가 나쁜 예로 든 형태 | PDF 09 4, 9, 20쪽 | 원리 |
| 미확정 모델 문자열 | PDF 09의 sonnet, haiku, opus, inherit은 Claude Code 프론트매터 별칭이라 API에는 못 쓴다. 전체 모델 ID가 필요하다. PDF는 생성과 평가를 다르게 하라고 권하지 않으며 평가자를 낮추지 말라는 원칙만 준다(논리 결함 탐지). 설정 키를 harness.model 하나가 아니라 세션 종류별(step, review)로 둘지 함께 판정. 모델 변경 시 07 규칙 재검토 절차도 추가 | PDF 09 3, 19, 23쪽, PDF 07 3쪽 model 필드, PDF 03 12쪽 | 원리. 별칭은 기능 |
| 미확정 반영 세션의 미수용 항목 보호 | PDF 04 보안 게이트 원리(모델 순응에 의존하지 말고 하네스가 기계적으로 막음)에 따르면 절 단위 치환이 우선이고 사람 diff는 병행이다. 택일이 아니다. 반영 1회가 치환하는 절 수 상한을 두고 초과면 분할 반영(PDF 02 파일 3개 룰의 원리, 수치는 실측) | PDF 04 3, 13쪽, PDF 02 3, 4쪽 | 원리 |
| 미확정 G1 산문 가드 검사 | PDF 08의 핸들러 구분(command는 결정론, prompt는 모델 yes/no)을 판정 기준으로. 정규식으로 잡히는 항목만 G1에 두고, 안 잡히는 판단은 별도 이름의 게이트로 분리해 단일 호출 yes/no를 받는다. 둘을 섞지 않는다. PDF 03의 "모호한 지침 금지"는 표 형식만 잡는다는 가설을 지지 | PDF 08 3, 4쪽, PDF 03 9쪽 | 원리 |
| 3절 신설 | 베이스라인 측정 방법(대상 문서, 회수, 기록 위치). PDF 09는 기준점 없이는 개선을 증명할 수 없다고 본다. 다만 DDD 설계 문서는 정답이 없어 골든 시나리오 10개를 만들기 어렵다. 06-5, 08-1, 08-3의 기존 블라인드 검증 결과를 기준점으로 재활용하는 방안이 현실적 | PDF 09 9, 10쪽 | 원리 |
| 미확정 v2 코드 구현 하네스 기반 | 재판정 항목에 추가. (a) 플래그 세트 -p, --output-format json, --max-turns, --max-budget-usd. (b) --dangerously-skip-permissions 사용 조건은 격리 컨테이너 또는 롤백 보장. v2가 git restore 자동화로 이를 충족하는지가 판정 항목. --allowedTools 같은 세밀한 권한 통제는 PDF에 없어 별도 확인. (c) 검증은 gradle build와 test 종료 코드. 에이전트의 완료 선언 불신. (d) 실패 시 restore와 오답 노트. (e) 호출 단위는 단일 컨텍스트에서 끝나는 최소 작업. (f) Java 하네스가 ProcessBuilder로 claude -p를 부르면 ralph.sh와 같은 오케스트레이터 구조 | PDF 10 1, 2, 11, 13, 14, 17, 18, 19, 22쪽 | 기능. 구조는 원리 |

### 2-4. 정합 확인 (변경 없음, 근거만 붙일 것)

| 결정 | PDF 근거 |
|---|---|
| 세션 하나 = 요청 하나, 이력 없음 | PDF 10 14, 19쪽 Fresh Context per Iteration, PDF 09 2쪽 "0토큰의 깨끗한 상태", PDF 02 1, 4쪽 30턴 오염. v2에서 이력을 붙이자는 유혹에 대한 반박 자료로 한 줄 적어 둔다 |
| status는 하네스만 씀 | PDF 10 19쪽 결정론적 검증, PDF 04 8쪽 5조 passes 토글 자율 금지. PDF 10 16쪽은 에이전트가 직접 바꾼다고 써서 PDF 내부가 상충하며 10-2 쪽이 맞다 |
| G1을 프롬프트가 아닌 코드로 검사 | PDF 09 3쪽 "프롬프트에 적어두어도 AI는 종종 이를 어기고", PDF 08 1쪽 조언 대 강제 |
| 계획자 없음, Task 블록은 사용자가 씀 | PDF 03 16쪽 "헌법 수정은 당신이". PDF 04의 Research와 Design 단계가 통째로 사람에게 온 구조. 문구는 "LLM 계획자 없음. 절차는 스킬 축 파일과 Task 블록에 사람이 미리 정의"로 정밀화 |
| 도구 루프 없음 | PDF 04 1~3쪽 Plan Mode의 권한 박탈(Permission Stripping)이 구조적으로 달성된 상태. 생성과 평가 세션 전체가 읽기 전용이고 파일 쓰기는 하네스와 사용자만 한다는 한 줄 명시 |
| 07 v3 재검증 블록 폐기, 스킬 축 교체 | PDF 07 20, 21쪽 "Rules ≠ Skills", "CLAUDE.md에 절차가 들어가지 않았는가". 07에는 정적 규칙만, 절차는 스킬 파일. 스킬 파일의 Claude Code 전용 프론트매터(allowed-tools, disable-model-invocation 등)는 하네스가 무시한다고 적는다 |
| 전제 표 01 1절에서만 파싱 | PDF 02 6쪽 Context Routing(진실의 원천 문서 지정, 복사본 없음) |
| 차단 목록 코드 상수 | PDF 08 16쪽 (2-2 참조) |
| Phase 0 git init 우선 | PDF 10 20쪽 "Git 초기화가 가장 중요합니다" |

## 3. 충돌과 긴장

| 10-2 확정 | PDF 권고 | 판정 |
|---|---|---|
| 계약 확인 y 승인, 결정표 사용자 편집 | PDF 10 1~3쪽 headless는 승인 없이 끝까지 자율 실행. PDF 09 6쪽 "인간의 개입 없이 스스로 평가하고 재작업" | 방향이 반대다. v1 범위가 설계 판단이고 사용자가 결정 주체이므로 의도된 선택. 10-2에 "v1은 반자동(semi-automatic)이며 headless 아님"을 명시하고, CI용 비대화 플래그를 미확정 항목으로 추가하면 충돌이 문서화된다 |
| 평가 3등급(치명, 보통, 확인필요) | PDF 09 9쪽 "'애매한 3점' 같은 점수는 없습니다", Pass/Fail 이진 | 통과선(치명 0)은 이진이므로 실질은 기계 이진 + 사람 3등급. 충돌 아님. 이 구조를 한 줄 적어 두면 된다 |
| 평가 세션 A, B (LLM 블라인드 평가) | PDF 04 12쪽 "검증은 사람이 합니다. 에이전트에게 위임하지 마세요." | 부분 긴장. 하네스는 1차 검증을 LLM에 맡기고 최종 결정을 결정표로 사람이 한다. PDF 문맥은 1인 실습의 Evaluator 역할이라 하네스 구조가 취지를 지킨다고 볼 수 있으나 사용자 판단 사안 |
| G1에 문체 규칙(볼드, 줄표, 가운뎃점 금지) 포함 | PDF 08 1쪽 비교표는 문체를 조언(CLAUDE.md) 영역으로 분류 | 반박: 같은 표가 조언은 "모델이 무시할 수 있음"이라 한다. 하네스의 형식 규칙은 다음 단계(결정표 생성, 절 파싱)의 입력 형식이라 무시되면 안 되므로 강제가 맞다. 다만 후속 파싱에 영향 없는 순수 문체 항목은 재요청 사유가 아닌 경고로 낮추는 선택지가 있다. 어느 항목이 그런지는 10-2에 없다 |
| 세션 하나 = 요청 하나 | PDF 04 2, 11쪽 Design은 토론 단계. "의문점이 있으면 나에게 질문한 뒤 답을 받고 진행하라" | 원리 수준 긴장. 하네스는 단발 요청이라 LLM이 질문하고 답을 받는 왕복이 없다. review와 apply 루프가 일부 대체한다. 사람과 LLM의 설계 토론이 구조적으로 없다는 점을 인지하고 있는지만 확인 |
| 07 v3 결정 주체 "하네스" | PDF 03 16쪽 "헌법 수정은 당신이" | 1-3 참조. 주체 정의 분리로 해소 |

## 4. 공식 소스로 확인한 사실 (10-2 미확정 해소분)

### 4-1. Java SDK 시스템 프롬프트 설정 메서드: 확정 가능

MessageCreateParams.Builder에 다음이 있다. putAdditionalBodyProperty는 필요 없다.

```kotlin
fun system(system: System)                                   // System prompt.
fun system(string: String)                                   // System.ofString(string) 별칭
fun systemOfTextBlockParams(textBlockParams: List<TextBlockParam>)  // System.ofTextBlockParams 별칭
```

캐시 브레이크포인트를 시스템 프롬프트에 두려면 systemOfTextBlockParams에 TextBlockParam.builder().text(...).cacheControl(CacheControlEphemeral) 블록을 넣는다. 최상위 MessageCreateParams.Builder.cacheControl(CacheControlEphemeral?)도 있으며 KDoc은 "마지막 캐시 가능 블록에 cache_control 표식을 자동으로 붙인다"이다.
출처: github.com/anthropics/anthropic-sdk-java main 브랜치 anthropic-java-core/src/main/kotlin/com/anthropic/models/messages/MessageCreateParams.kt, TextBlockParam.kt (2026-09-07 확인)

### 4-2. stopReason 비교 방식: 확정 가능

```kotlin
// Message.kt
fun stopReason(): Optional<StopReason>
// StopReason.kt
@JvmField val MAX_TOKENS = of("max_tokens")   // END_TURN, STOP_SEQUENCE, TOOL_USE, PAUSE_TURN, REFUSAL도 있음
enum class Value { END_TURN, MAX_TOKENS, STOP_SEQUENCE, TOOL_USE, PAUSE_TURN, REFUSAL, _UNKNOWN }
fun value(): Value
fun known(): Known   // 알 수 없는 값이면 예외
fun asString(): String
```

판정 코드는 known() 대신 value()를 쓴다. 미래에 새 stop_reason이 오면 _UNKNOWN으로 떨어져 예외 없이 G1이 잘림 아님으로 판정한다.

```java
boolean truncated = message.stopReason()
        .map(r -> r.value() == StopReason.Value.MAX_TOKENS)
        .orElse(false);
```

Message.kt KDoc: max_tokens는 "we exceeded the requested max_tokens or the model's maximum". 출처: 같은 저장소 Message.kt, StopReason.kt

### 4-3. Java SDK 버전: 2.61.0으로 고정

Maven Central maven-metadata.xml: latest 2.61.0, release 2.61.0, lastUpdated 20260904223122. 공식 문서 페이지의 2.60.0은 문서 갱신 지연이고, GitHub README는 2.59.0으로 더 오래됐다. 10-2의 "문서 2.60.0, GitHub 2.61.0" 중 2.61.0이 맞다.
출처: repo1.maven.org/maven2/com/anthropic/anthropic-java/maven-metadata.xml

인증과 스트리밍은 공식 문서 페이지에서 확인했다. AnthropicOkHttpClient.fromEnv()는 ANTHROPIC_API_KEY 환경변수를 읽는다. createStreaming과 MessageAccumulator.create(), accumulate, message() 패턴은 문서의 예시와 10-2 결정이 일치한다.
출처: platform.claude.com/docs/en/api/sdks/java

### 4-4. 프롬프트 캐싱(Prompt Caching) 조건과 단가

| 항목 | 값 |
|---|---|
| 기본 TTL | 5분. 1시간 옵션은 cache_control에 ttl: "1h" |
| 캐시 쓰기 | 기본 입력 단가의 1.25배(5분), 2배(1시간) |
| 캐시 읽기 | 0.1배. 예외: Claude Fable 5.1과 Claude Mythos 5.1은 0.025배 |
| 최소 캐시 토큰 | 모델별 512(Fable 5.1, Mythos 5.1, Opus 5, Fable 5, Mythos 5), 1024(Opus 4.8, Sonnet 5, Sonnet 4.6, Sonnet 4.5, Opus 4.1, Opus 4, Sonnet 4), 2048(Mythos Preview, Opus 4.7, Haiku 3.5), 4096(Opus 4.6, Opus 4.5, Haiku 4.5). 미달이면 오류 없이 캐시 없이 처리 |
| 브레이크포인트 | 요청당 4개 |
| usage 필드 | cache_creation_input_tokens, cache_read_input_tokens, input_tokens(마지막 브레이크포인트 이후 토큰). 합이 총 입력 |

하네스 적용: system과 Context 공통부(07 규칙부 + 입력 팩)를 한 블록으로 묶어 브레이크포인트 하나. Task와 대상 문서는 그 뒤에. A 평가 직후 B 평가가 5분 안에 돌면 읽기 요금으로 떨어진다. 공통 접두부가 최소 캐시 토큰(모델별)보다 작으면 효과가 없으므로 모델 판정 때 같이 본다.
PDF 01, 02의 "TTL 5분, 90% 절감"은 기본 TTL과 0.1배 읽기 단가와 맞는다. 다만 PDF는 쓰기 추가 요금(1.25배)을 말하지 않는다. 한 번만 부르는 프롬프트에 캐시를 걸면 손해다.
출처: platform.claude.com/docs/en/build-with-claude/prompt-caching (2026-09-07 확인)

Java 접근자 [추측]: Usage.kt의 cacheReadInputTokens()와 cacheCreationInputTokens() 반환형이 Optional<Long>인지 Long인지 이번에 확인 못 했다(요청 한도). Phase 1에서 Usage.kt로 확인.

### 4-5. 확인하지 못한 것

Spring Boot 마이너 버전, maxTokens 기본값은 PDF와 무관하고 이번 확인 범위 밖이다. 모델 문자열은 사용자 결정이라 후보를 고르지 않았다. 모델 페이지에서 컨텍스트 윈도우, 최대 출력 토큰, 최소 캐시 토큰 셋을 함께 보고 정하면 된다.

## 5. PDF 주장 중 하네스에 영향 있는 재확인 항목

| PDF 주장 | 쪽 | 상태 |
|---|---|---|
| 컨텍스트 윈도우 약 200,000 토큰 | 01 3쪽, 02 1쪽 | 모델별로 다르다. 모델 확정 후 해당 모델 문서로 확인 |
| 캐시 TTL 5분, 90% 절감 | 01 3쪽, 02 1, 3, 13쪽 | 확인됨(4-4). 쓰기 요금 누락 |
| 30턴 이상이면 오염 | 02 1, 4쪽 | 출처 없음. 경험칙 |
| Lost in the Middle | 02 3, 4쪽 | 출처 없음. 최신 모델 재현 여부 [추측] |
| CLAUDE.md 200줄 25KB, SKILL.md 500줄 | 03 6쪽, 07 22쪽 | 출처 없음. 경험칙. 07 v3 크기는 실측으로 |
| "10개 이상의 파일, 3개 이상의 독립 작업"이 Anthropic 공식 가이드라인 | 09 1쪽 | 출처 미확인 |
| Anthropic "Effective Harnesses for Long-Running Agents" 2025와 feature_list.json 구조 | 04 5, 6쪽 | 원문 대조 안 함. 하네스 step_list.json은 이미 같은 원리 |
| --max-budget-usd 플래그와 JSON 출력 필드 .cost.totalCostUSD | 10 1, 2, 11쪽 | v2 재판정 때 CLI 문서로 확인. 필드명은 [추측] 다를 수 있음 |
| PDF 내부 불일치 | 04 6쪽 대 8쪽(passes 토글 주체), 08 17쪽 대 21쪽(PostToolUse 차단 여부), 09 9쪽 대 15쪽(CSV 헤더), 10 16쪽 대 23쪽(prd.json 스키마), 10 12쪽(pre-commit 대 pre-push) | 인용 시 주의 |

## 6. 10-2 v2 반영안

### 6-1. 2절에서 1절로 옮길 행

| 항목 | 값 | 근거 |
|---|---|---|
| Java SDK 시스템 프롬프트 설정 | system(String) 또는 systemOfTextBlockParams(List<TextBlockParam>). 캐시 블록은 후자 | 4-1 |
| stopReason 비교 | stopReason().map(r -> r.value() == StopReason.Value.MAX_TOKENS).orElse(false). known() 미사용 | 4-2 |
| Java SDK 버전 | 2.61.0 (Maven Central 2026-09-04) | 4-3 |

### 6-2. 1절에 추가할 행 (근거 문장만 붙이는 정합 항목은 2-4 표를 위치 열에 인용)

| 항목 | 값 |
|---|---|
| 자동화 수준 | v1은 반자동. y 승인과 결정표 편집이 있어 headless 아님 |
| 평가 등급 구조 | 기계 이진(치명 0) + 사람 3등급(보통, 확인필요는 사용자 결정) |
| 결정 주체 정의 | "하네스" = 하네스 개발 세션(사람 승인). 하네스 안 LLM은 결정 주체가 아님 |
| 게이트 시점 | 차단 목록과 y 승인은 호출 전, G1은 응답 직후, G2는 done 전환 직전 |
| 종료 코드 | halted와 예외는 0이 아닌 값 |
| check 추가 검사 | live 프로파일에서 ANTHROPIC_API_KEY 존재 |

### 6-3. 2절에 추가할 행

| 항목 | 상태 | 결정 시점 | 결정 주체 |
|---|---|---|---|
| check dirty 판정 범위 | 미정. 하네스 관리 경로 제외 또는 명령별 범위 | Phase 0 (1-2가 결함이면 즉시) | 사용자 |
| halted 시 작업 트리 처리 | 미정. restore, halted 커밋, failed/ 이동 중 택일 | Phase 0 | 사용자 |
| status별 재개 지점 | 미정 | Phase 1 | 하네스 |
| 재요청과 재평가 프롬프트의 실패 사유 주입 | 미정. 주입 원천(progress.md 또는 10-1)과 형식 | Phase 1 | 하네스 |
| progress.md 행 형식 | 미정. HANDOFF 3요소 + attempt 4요소 + usage | Phase 0 (3절 6번 전) | 하네스 |
| 승인한 Task 블록 저장 | 미정. 저장 위치와 평가 입력 포함 여부 | Phase 1 | 하네스 |
| usage와 stop_reason 기록 | 미정. 기록 위치와 필드 | Phase 1 | 하네스 |
| 누적 토큰 상한 | 미정. Step당 또는 실행당. 초과 시 halted | Phase 2 실측 | 하네스 |
| 프롬프트 캐싱 적용 | 미정. 브레이크포인트 위치와 공통 접두부 크기. 모델의 최소 캐시 토큰 확인 필요 | Phase 2 | 하네스 |
| review_round 상한 도달 시 status | 미정. halted 또는 reviewed | Phase 1 | 하네스 |
| 결정표 사용자 추가 항목 절 | 미정 | Phase 3 | 사용자 |
| 평가 결과 기계 판독 행 | 미정. CSV 열 | Phase 3 | 하네스 |
| G1 문체 항목의 경고 강등 | 미정. 후속 파싱 무관 항목만 | Phase 4 | 하네스 |
| 07 v3 조항 번호와 종료 신호 문장 | 미정 | Phase 0 (3절 3번) | 하네스 |
| 베이스라인 측정 방법 | 미정. 06-5, 08-1, 08-3 재활용 여부 | Phase 3 | 사용자 |
| CI용 비대화 실행 플래그 | 미정. v1 범위 밖 | v2 | 사용자 |
| 모델 설정 키 분리 | 미정. harness.model 하나 대 step, review 별도 | 모델 판정과 동시 | 사용자 |

### 6-4. 3절 순서 조정

1. 1-2 dirty 범위와 halted 처리를 먼저 결정한다. 이게 정해져야 check와 상태 전이가 확정된다.
2. 모델 문자열 판정에 최소 캐시 토큰과 최대 출력 토큰을 판정 입력으로 추가한다.
3. 2번 첫 커밋은 베이스라인 커밋(claude/ 원본)으로 이름 붙이고, Phase 0 산출물(07 v3, step_list, progress)은 두 번째 커밋으로 둔다. PDF 04와 10의 "산출물 생성 뒤 초기 커밋" 순서와 맞추기 위함.
4. progress.md 행 형식 결정을 6번 앞에 넣는다.
5. 07 v3 작성(3번)에 조항 번호와 종료 신호 문장, 규칙부와 자료부 구분 표기를 포함한다.
6. 완료 조건에 "2절 Phase 0 항목이 남아 있지 않다"는 그대로 두되, 6-3에서 Phase 0으로 표시한 행이 추가된다.

### 6-5. 4절 Task 블록 갱신

```
# 이번 턴 Task: 하네스 Phase 0 준비
입력: 10-2-o2o-harness-decisions v1, 10-3-o2o-harness-pdf-crosscheck v1, 07-o2o-ptcf-prompt v2
목표: 10-3 1-2와 6-4의 순서대로 진행한다. 한 항목이 끝날 때마다 멈추고 확인을 받는다.
결정 대상 쟁점: check dirty 범위와 halted 처리, 모델 문자열(생성과 평가 동일 여부, 최소 캐시 토큰 포함)
출력 형식:
1. 입력 요약 (3줄 이내)
2. 이번 항목의 산출물 본문
3. 이번 항목에서 확정한 것 / 남긴 미확정 / 다음 항목에 미치는 영향
```

## 7. 함께 볼 키워드

프롬프트 캐싱 브레이크포인트 설계(공통 접두부를 앞에 모으는 프롬프트 배치), LLM-as-a-Judge의 긍정 편향과 위치 편향, 종료 코드 규약(Exit Code Convention)을 가진 CLI 상태 머신 설계.
