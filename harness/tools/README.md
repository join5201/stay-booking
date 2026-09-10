# harness/tools/

무엇이 들어가는가: 검사 스크립트다. LLM을 부르지 않는 로컬 검사 도구.

왜 필요한가: 양식의 버전 칸을 사람이 손으로 적으면 틀린다(HR2). G1 실패 목록도 사람 기억이 아니라 스크립트 출력이 원천이다(HR3). G2의 일곱 항목은 눈으로 대조하면 반드시 새는데, 새는 쪽이 늘 자기 결정에 불리한 지적이다(HR4).

| 파일 | 상태 |
|---|---|
| check.mjs | fill, g1, g2, answer, sweep, state, settings 일곱 명령. 2026-09-08 H3에서 재작성, 같은 날 하네스 리뷰 12건 반영. 2026-09-10 sweep과 state와 settings 추가 (10-14 8-4절과 9-4절, 10-19 4절) |
| build-v2.mjs | tmp/api-spec-v2/build-v2.mjs 사본. API 명세 조립용. 미검증 |
| tests/ | 골든 fixture 6개와 테스트 97건 (2026-09-10 실측). fixture는 harness/prompts/의 양식에서 만든다 |

원본은 tmp/ 아래에 그대로 둔다. 삭제하지 않는다.

## 명령

| 명령 | 검사 |
|---|---|
| node harness/tools/check.mjs fill <양식> [--dry] | 빈칸 잔존 0, 경로가 절대경로이고 존재, 버전 칸에 sha256 기입 |
| node harness/tools/check.mjs g1 <후보> --type doc\|api\|code | doc은 날짜 2종, 금지 기호 3종, 내부 링크, 종료 문장. api는 요청과 응답과 오류 절. code는 빌드와 테스트 결과 파일 존재 |
| node harness/tools/check.mjs answer <답변파일> [--grade A|B|C] | 첫 줄이 결론인지, 마지막 절이 결과인지, 결과 절 다섯 행이 다 있는지. 등급은 안 주면 기계가 정한다 (harness/prompts/answer-format.md 4절) |
| node harness/tools/check.mjs sweep <디렉터리> --type doc | 디렉터리 아래 마크다운을 전부 g1으로 돌고 한 줄로 요약한다. 통과한 파일은 안 찍고 실패만 상세를 낸다. 하나라도 실패하면 종료 코드 1 |
| node harness/tools/check.mjs state <진행 기록> [--task <이름>] | 안 끝난 Task의 마지막 행과 막힌 채로 남은 행을 고정 형식으로 낸다. 게이트가 아니라 보고라 실패해도 종료 코드가 0이다 |
| node harness/tools/check.mjs settings <권한 설정> | Bash와 PowerShell 규칙의 짝, deny와 allow 겹침. 한쪽만 고쳐 규칙이 조용히 사라지는 것을 잡는다 |
| node harness/tools/check.mjs g2 <결정표> --mode pre 또는 final | 버전 일치, 원본 리포트 스키마, 지적 ID 집합, 행 수, 심각도 보존, 빈 결정, 거부 이유, 반박 등급, 치명 거부 필수 필드. 남은 치명과 반영본은 final에서만 |
| node --test harness/tools/tests/check.test.mjs | 골든 파일 테스트 |

종료 코드는 0 통과, 1 검사 실패, 2 사용법 오류다.

## 쓰기 정책

g1과 g2와 sweep과 state와 settings는 아무 파일도 쓰지 않는다. 읽기만 한다.

fill만 쓴다. 대상은 인자로 받은 그 파일 하나뿐이고 document/, harness/project-sync/, harness/docs/는 거부한다. 이전 판이 검사 도중 document/o2o-*.md를 덮어써서 생긴 규칙이다. 미리 보려면 --dry를 쓴다.

## 옵션

| 옵션 | 쓰임 |
|---|---|
| --type doc\|api\|code | g1 필수 |
| --end "<문장>" | g1 doc의 종료 문장을 바꾼다. 기본값은 07 v3 F16의 문장이다 |
| --artifact <경로> | g1 code에서 빌드나 테스트 결과 파일을 지정한다. 여러 번 쓸 수 있다 |
| --dry | fill이 쓰지 않고 결과만 보여 준다 |

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
