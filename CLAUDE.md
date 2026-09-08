# CLAUDE.md (O2O 하네스 프로젝트 규칙)

최초 작성: 2026-09-07
최종 갱신: 2026-09-07 (H1 파일 레이아웃. 3절 전면 개편, harness-docs/ 분리)

왜 이 파일이 필요한가: 세션마다 문체 규칙과 금지 사항을 다시 붙여넣으면 규칙이 세션마다 흔들리고, G1 실패 사유를 조항 번호로 인용할 수 없다. 이 파일이 그 흔들림을 막는다.

확인한 출처 (전부 2026-09-07 확인)

| 내용 | URL |
|---|---|
| CLAUDE.md 로드 위치와 순서, 200줄 권고 | https://code.claude.com/docs/en/memory |
| settings.json 위치와 우선순위 | https://code.claude.com/docs/en/settings |
| 권한 규칙 문법 (Bash, PowerShell, Read, Edit) | https://code.claude.com/docs/en/permissions |

이 파일은 규칙이지 강제 장치가 아니다. 공식 문서는 CLAUDE.md를 context, not enforced configuration이라고 적는다(위 memory 문서). 강제가 필요한 것은 .claude/settings.json의 deny와 검사 스크립트로 내린다.

## 1. 프로젝트와 기술 구성

| 항목 | 값 |
|---|---|
| 대상 | O2O 숙박 예약 서비스. DDD 설계 문서와 구현 |
| 루트 | C:/Dev/potenup/99_projects/o2o |
| 백엔드 | Spring 기반 Java, MySQL |
| 프론트 | Next.js |
| 범위 | 로컬 개발과 검증까지. 배포와 운영 제외 |
| 검사 스크립트 언어 | Node. 기존 tmp/document-review/check.mjs, tmp/api-spec-v2/build-v2.mjs와 통일 |

하네스는 모델을 뺀 전부다. 파일 규약, 검사 스크립트, 기록 규칙, 프롬프트 양식이 하네스이고 LLM 호출은 사람이 두 앱에서 한다. 생성과 수정은 Claude Code, 평가는 Codex의 새 작업(A와 B 분리), 전달은 사용자다(harness-docs/10-4 1절).

git 상태 주의: 이 폴더는 C:/Dev/potenup 저장소 안에 있으나 루트 .gitignore의 /99_projects/ 규칙으로 추적에서 빠져 있고, o2o 자체 저장소는 없다. 따라서 git status와 git diff로는 이 폴더의 변경을 볼 수 없다. 버전 근거는 검사 스크립트가 계산하는 sha256이다(HR2).

## 2. 작성 표준 (조항 번호. G1 실패 목록과 결정표에서 이 번호로 인용한다)

출처는 project-sync/07-o2o-ptcf-prompt.md(07 v2)의 Format 절이다. 번호는 이 파일에서 처음 붙인다. 07 v3(H2 산출물)이 나오면 번호의 정본을 07 v3로 옮기고 이 절은 참조만 남긴다.

문체

| 번호 | 규칙 |
|---|---|
| W1 | 한국어로 쓴다 |
| W2 | 제목 외 볼드체를 쓰지 않는다 |
| W3 | 줄표와 가운뎃점을 쓰지 않는다 |
| W4 | 인용부호는 최소화한다 |
| W5 | 코드에는 핵심만 짧게 주석을 단다. 코드 문법과 데이터에는 문서 서식 규칙을 적용하지 않는다(harness-docs/10-4 3절) |
| W6 | 불필요한 반복 설명을 생략한다 |

산출물

| 번호 | 규칙 |
|---|---|
| D1 | 구조는 도식으로 그릴 수 있는 형태로 낸다 |
| D2 | 서술은 마크다운 문서로 낸다 |
| D3 | 표로 정리 가능한 것은 산문 대신 표로 낸다 |
| D4 | 문서에 최초 작성일과 최종 갱신일을 적고, 바뀐 절 제목 옆에 반영 날짜를 적는다 |
| D5 | 스크립트 파일 머리 주석에 실행 예시와 실패 시 출력 예시를 넣는다 |

D5는 07 v2에 없다. 이번 하네스에서 추가한 조항이며 H2의 07 v3에 반영한다.

## 3. 파일 규약 (2026-09-07 H1 개편)

세 디렉터리의 성격을 이름으로 가른다. 섞어 두면 평가에 넘길 파일을 고를 때마다 성격을 다시 판단해야 하고, 계획 문서가 평가 입력에 섞이는 사고가 난다.

| 경로 | 성격 | 평가 입력이 될 수 있나 |
|---|---|---|
| document/ | O2O 서비스 베이스 설계 문서 | 된다 |
| harness-prompts/ | 실행 중 읽는 양식과 평가 기준 | 기준 파일만 |
| harness-docs/ | 하네스 설계 근거와 결정 이력 | 안 된다 |

전체 배치

| 경로 | 역할 |
|---|---|
| document/01~06, 11, 12, o2o-*.md | 설계 문서, API 명세, 검토 기록. README.md 있음 |
| harness-docs/10-4~10-8 | 실행 방식, 인계, 구현 계획, 대조 기록 2종. README.md 있음 |
| harness-prompts/ | 양식 5종과 평가 기준 2종 |
| harness-prompts/tools/ | 검사 스크립트. check.mjs, build-v2.mjs |
| project-sync/ | 프로젝트 계열 원본 사본. 수정 금지 |
| tasks/ | 작업 계약. task-S{Step}.md |
| out/ | 생성 후보와 형식 보정 이력. task-S{Step}-R{라운드}/ |
| reviews/ | Codex 리포트 원문. task-S{Step}-R{라운드}-{A 또는 B}.md |
| decisions/ | 결정의 정본. decisions-08-3.md와 task-S{Step}-R{라운드}.md |
| state/ | progress.md, troubleshooting.md, knowledge.md. 전부 추가 전용 |
| tmp/ | 과거 산출물과 스크립트. 원본 보관. 삭제하지 않는다 |
| Claude outputs/, _to_delete/, output/, .tmp_hla_check/ | 미분류. 처리 미결 |

디렉터리 7곳에 README.md가 있다. document, harness-docs, harness-prompts/tools, tasks, out, reviews, state, decisions.

파일명에 Task, 라운드, A와 B가 들어가야 G2가 파일명만으로 버전 일치를 1차 검사할 수 있다(harness-docs/10-8 5-2).

지적 ID 규약: S{Step}-R{라운드}-{A 또는 B}-{원본 번호 2자리}

번호 인용 해석: 01~06, 11, 12는 document/ 아래, 10-4~10-8은 harness-docs/ 아래를 가리킨다.

state/에 파일 수정 이력은 두지 않는다. 2026-09-07 사용자 결정이다. 이 폴더가 git 추적 밖이므로 파일 이동과 개명은 어디에도 남지 않는다. 2026-09-07 개편 두 건(문서 26개를 document/로, 10-4~10-8을 harness-docs/로)은 이 절의 제목 날짜와 harness-docs/README.md에만 남아 있다.

## 4. 실행 명령

아직 존재하지 않는다. H3에서 만든다. 이름과 인자를 미리 고정하는 이유는 양식(H4)의 입력 칸이 이 명령 이름을 인용하기 때문이다.

| 명령 | 하는 일 |
|---|---|
| node harness-prompts/tools/check.mjs fill <양식> | 빈칸 잔존 0 확인, 경로 존재 확인, 버전 칸에 sha256 기입 |
| node harness-prompts/tools/check.mjs g1 <후보> --type doc 또는 api 또는 code | 양식 필수 항목, 날짜, 금지 기호, 종료 문장 검사 |
| node harness-prompts/tools/check.mjs g2 <결정표> | ID 집합 일치, 행 수, 빈 결정, 거부 이유, 반박 등급, 치명 거부 필수 필드 |

명령을 적을 때 경로 구분자는 슬래시로 고정한다. settings.json의 allow 규칙은 명령 문자열 앞부분을 그대로 비교하므로 역슬래시로 적으면 규칙에 걸리지 않고 매번 승인 프롬프트가 뜬다.

settings.json은 JSON이라 주석을 달 수 없다. deny와 allow를 그렇게 정한 이유는 여기에 적는다.

| 규칙 | 이유 |
|---|---|
| deny에 .env 계열과 키 파일 | 비밀 값이 프롬프트에 섞여 앱 밖으로 나가는 것을 막는다. deny는 allow보다 먼저 평가되고 폴더 신뢰 절차 없이 즉시 적용된다 |
| deny에 삭제 명령 | N2를 규칙이 아니라 게이트로 만든다. 삭제는 되돌릴 수 없다 |
| deny에 project-sync 편집 | 원본 사본이 병합 도중 오염되면 H2의 차이표가 근거를 잃는다 |
| allow에 검사 스크립트와 git 조회 | 검사를 돌릴 때마다 승인 프롬프트가 뜨면 G1을 건너뛰게 된다 |

allow 규칙은 폴더를 신뢰한 뒤에 적용된다. deny와 ask는 즉시 적용된다(permissions 문서).

## 5. DO NOT

| 번호 | 금지 | 어겼을 때 깨지는 것 |
|---|---|---|
| N1 | 별도 LLM API 호출과 앱 간 자동 연결 | 10-4 1절의 비용 조건과 실행 방식 |
| N2 | 파일 삭제. 치울 것은 tmp/_moved/로 옮긴다 | 되돌릴 수 없다 |
| N3 | 01 전체를 평가 입력에 넣기. 입력 팩만 넘긴다 | 블라인드 평가가 무력화된다(harness-docs/10-8 1-2) |
| N4 | 08-3 결정 11개가 비어 있는 동안 그 값을 확정값으로 쓰기 | 미결이 조용히 확정으로 굳는다 |
| N5 | done을 스크립트나 모델이 쓰기 | HR5. done은 사용자 승인 행에서만 바뀐다 |
| N6 | 검증하지 않은 설정 키, 플래그, API를 기억으로 쓰기 | 07 v2 Format 금지 조항 |
| N7 | 이번 단계 범위를 넘는 파일을 만들거나 고치기 | 승인하지 않은 변경이 섞인다 |
| N8 | 기존 파일을 diff 요약과 승인 없이 고치기 | 같음 |
| N9 | 기능 하나가 백엔드와 테스트, 프론트, 연결 테스트, 검증까지 내려가기 전에 다음 기능으로 넘어가기 | harness-docs/10-6 3절. 백엔드 전체를 끝낸 뒤 프론트 전체를 시작하는 순서가 아니다. 확인 위치는 작업 계약의 선행 작업 칸(H6) |
