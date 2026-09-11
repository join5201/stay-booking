# harness/tools/

최초 작성: 2026-09-08
최종 갱신: 2026-09-10 (날짜 줄 신설)

무엇이 들어가는가: 검사 스크립트다. LLM을 부르지 않는 로컬 검사 도구.

왜 필요한가: 양식의 버전 칸을 사람이 손으로 적으면 틀린다(HR2). G1 실패 목록도 사람 기억이 아니라 스크립트 출력이 원천이다(HR3). G2의 일곱 항목은 눈으로 대조하면 반드시 새는데, 새는 쪽이 늘 자기 결정에 불리한 지적이다(HR4).

| 파일 | 상태 |
|---|---|
| check.mjs | fill, g1, g2, answer, sweep, numbers, state, settings 여덟 명령. 2026-09-08 H3에서 재작성, 같은 날 하네스 리뷰 12건 반영. 2026-09-10 sweep과 state와 settings 추가 (10-14 8-4절과 9-4절, 10-19 4절), 같은 날 numbers 추가 (이슈 75) |
| numbers-gate.mjs | PostToolUse 훅용 다리. 방금 건드린 파일이 harness/docs 아래면 numbers를 부른다 (2026-09-10 추가) |
| build-v2.mjs | tmp/api-spec-v2/build-v2.mjs 사본. API 명세 조립용. 미검증 |
| tests/ | 골든 fixture 6개와 테스트 129건 (2026-09-10 실측). fixture는 harness/prompts/의 양식에서 만든다. numbers의 대조 테스트는 임시 git 저장소를 만들어 refs/remotes/origin/main을 직접 박는다 |

원본은 tmp/ 아래에 그대로 둔다. 삭제하지 않는다.

## 명령

| 명령 | 검사 |
|---|---|
| node harness/tools/check.mjs fill <양식> [--dry] | 빈칸 잔존 0, 경로가 절대경로이고 존재, 버전 칸에 sha256 기입 |
| node harness/tools/check.mjs g1 <후보> --type doc\|api\|code | doc은 날짜 2종, 금지 기호 3종, 표 칸 수, 내부 링크, 종료 문장. api는 요청과 응답과 오류 절과 표 칸 수. code는 빌드와 테스트 결과 파일 존재 |
| node harness/tools/check.mjs answer <답변파일> [--grade A|B|C] | 첫 줄이 결론인지, 마지막 절이 결과인지, 결과 절 다섯 행이 다 있는지. 등급은 안 주면 기계가 정한다 (harness/prompts/answer-format.md 4절) |
| node harness/tools/check.mjs sweep <디렉터리> --type doc | 디렉터리 아래 마크다운을 전부 g1으로 돌고 한 줄로 요약한다. 통과한 파일은 안 찍고 실패만 상세를 낸다. 하나라도 실패하면 종료 코드 1 |
| node harness/tools/check.mjs numbers <디렉터리> [--fetch] | 파일명의 10-N 접두를 모아 한 번호를 문서 둘이 쓰는지 본다. 로컬 트리와 origin/main 트리의 합집합으로 센다. 번호를 주장하는 것은 본문 md 하나이고 다른 확장자는 그 본문 이름으로 시작하는 첨부다. 겹치면 종료 코드 1 |
| node harness/tools/check.mjs state <진행 기록> [--task <이름>] | 안 끝난 Task의 마지막 행과 막힌 채로 남은 행을 고정 형식으로 낸다. 게이트가 아니라 보고라 실패해도 종료 코드가 0이다 |
| node harness/tools/check.mjs settings <권한 설정> | Bash와 PowerShell 규칙의 짝, deny와 allow 겹침. 한쪽만 고쳐 규칙이 조용히 사라지는 것을 잡는다 |
| node harness/tools/check.mjs g2 <결정표> --mode pre 또는 final | 버전 일치, 원본 리포트 스키마, 지적 ID 집합, 행 수, 심각도 보존, 빈 결정, 거부 이유, 반박 등급, 치명 거부 필수 필드. 남은 치명과 반영본은 final에서만 |
| node --test harness/tools/tests/check.test.mjs | 골든 파일 테스트 |

종료 코드는 0 통과, 1 검사 실패, 2 사용법 오류다.

## 쓰기 정책

g1과 g2와 sweep과 numbers와 state와 settings는 저장소 파일을 쓰지 않는다. 읽기만 한다.

numbers는 --fetch를 줬을 때만 git fetch origin을 부른다. 그때 .git의 원격 추적 ref가 갱신된다. 기본값은 부르지 않는다. 검사기가 매번 네트워크를 부르면 오프라인에서 못 돌기 때문이다. 대신 어느 커밋과 대조했는지를 출력에 적어서 낡은 ref로 대조하고도 최신인 줄 아는 일을 막는다.

fill만 쓴다. 대상은 인자로 받은 그 파일 하나뿐이고 document/, harness/project-sync/, harness/docs/는 거부한다. 이전 판이 검사 도중 document/o2o-*.md를 덮어써서 생긴 규칙이다. 미리 보려면 --dry를 쓴다.

## 옵션

| 옵션 | 쓰임 |
|---|---|
| --type doc\|api\|code | g1 필수 |
| --end "<문장>" | g1 doc의 종료 문장을 바꾼다. 기본값은 07 v3 F16의 문장이다 |
| --artifact <경로> | g1 code에서 빌드나 테스트 결과 파일을 지정한다. 여러 번 쓸 수 있다 |
| --dry | fill이 쓰지 않고 결과만 보여 준다 |
| --ref <ref> | numbers가 대조할 ref. 기본값은 origin/main |
| --fetch | numbers가 대조 전에 git fetch origin을 부른다 |
| --local-only | numbers가 대조를 끄고 로컬 트리만 본다. 남이 먼저 가져간 번호를 못 본다 |

경로 칸의 허용 값 넷이다. 절대경로, 해당 없음, 생성 후 기입, 그리고 아직 안 채운 빈칸 표시다. 앞의 셋은 통과하고 빈칸 표시는 실패한다. 생성 후 기입은 계약 시점에 아직 없는 대상을 가리키는 유예이며 fill이 건수를 출력한다.

폴더 경로는 실패한다. 해시를 계산할 수 없기 때문이다. 이전 판은 이 자리에서 EISDIR로 죽었다.

검사 대상 파일이 자기 자신을 가리키는 행도 실패한다. 해시를 적는 순간 파일이 바뀌기 때문이다.

줄바꿈은 저장소 루트의 .gitattributes가 LF로 고정한다. core.autocrlf가 true인 채로 두면 커밋 전에 계산한 해시가 체크아웃 뒤 어긋난다.
| --require "a,b" | g1 doc이 확인할 승인 양식 필수 항목 |
| --mode pre 또는 final | g2의 반영 전 검사와 최종 완료 검사를 가른다 |
| --task <이름> | state가 그 Task의 행만 본다 |

## 경로 규칙

check.mjs는 저장소 루트를 스크립트 위치에서 유도한다(../..). 절대경로를 상수로 박지 않는다. 이전 판이 그렇게 해서 디렉터리 개편 두 번에 두 번 다 깨졌다.

명령을 적을 때 경로 구분자는 슬래시로 고정한다. .claude/settings.json의 allow 규칙이 명령 문자열 앞부분을 그대로 비교한다.

## 표 칸 수 (2026-09-10 신설. 이슈 93)

doc.table-cells는 표 행이 자기 표 머리글과 같은 칸 수인지 본다. 칸이 넘치는 것과 모자란 것을 한 검사로 잡는다.

넘치는 쪽은 대개 이스케이프하지 않은 파이프 기호다. 값에 파이프가 들어가면 그 자리에서 칸이 갈리고, 마크다운은 머리글보다 넘치는 칸을 버린다. 그래서 그 칸의 글이 화면에서 사라진다. 표 안에 파이프를 적을 때는 --type doc\|api\|code처럼 이스케이프한다.

두 가지를 마크다운과 같게 읽는다. 빈 줄은 표를 끊는다. 이스케이프한 파이프는 칸을 가르지 않는다. 펜스 안은 데이터라 세지 않는다(F11).

harness/state/progress.md만 이 검사의 범위 밖이다. 머리글이 12칸으로 자랐는데 그 전에 쓴 행은 아홉 칸이고, 추가 전용이라 기존 행을 고칠 수 없다(CLAUDE.md 4-1). 고치면 merge=union이 옛 행과 새 행을 둘 다 남긴다. troubleshooting.md에서 실제로 그렇게 됐다.

## 훅 (2026-09-10 추가)

검사가 있다는 것과 검사가 무언가를 막는다는 것은 다르다. 사람이 손으로 쳐야 도는 검사는 바쁠 때 안 돌고, 안 돈 것과 돌아서 통과한 것을 구별할 수 없다(harness/docs/10-14 8-2절).

numbers를 PostToolUse 훅에 걸었다. 배선은 .claude/settings.json이고 이 파일은 추적한다. answer 게이트는 배선이 .claude/settings.local.json에 있어 저장소를 받은 사람에게는 없는 것과 같다. 그 자리를 반복하지 않으려고 추적하는 쪽에 넣었다.

| 항목 | 값 |
|---|---|
| 이벤트 | PostToolUse |
| matcher | Write와 Bash |
| 부르는 것 | node harness/tools/numbers-gate.mjs |
| 보는 자리 | harness/docs 하나 |
| 막는 조건 | 방금 건드린 파일이 겹침에 이름을 올렸을 때 |

Bash를 넣은 것은 이름을 바꾸는 일이 git mv로 일어나기 때문이다. 커밋 16bb437이 그 모양이었고 Write만 보면 그 변경을 놓친다. 대신 Bash 호출마다 게이트가 한 번 뜬다. 실측 75밀리초이고 그중 60밀리초는 node 기동이라 더 줄일 자리가 없다. 느리면 matcher에서 Bash를 빼면 되고 그때 잃는 것은 이름 변경 감지다.

document/는 보지 않는다. 06-2-o2o-aggregates.md와 06-2-o2o-aggregates-explained.md처럼 한 번호에 본문과 해설을 짝으로 두는 자리라 걸면 정상 배치가 매번 걸린다. 늘리려면 그 자리의 규약을 먼저 정한다.

게이트가 고장 나도 세션을 잠그지 않는다. 안전 규칙 넷은 numbers-gate.mjs 머리 주석에 있다. 남이 만든 겹침은 알리기만 하고 막지 않는다. 이미 겹친 저장소에서 모든 쓰기가 막히면 세션이 못 나간다.

훅은 이 저장소를 여는 세션에서만 돈다. PR과 다른 사람의 클론까지 덮으려면 CI가 따로 필요하다. 지금 .github/workflows는 없다.
