# harness/tools/

무엇이 들어가는가: 검사 스크립트다. LLM을 부르지 않는 로컬 검사 도구.

왜 필요한가: 양식의 버전 칸을 사람이 손으로 적으면 틀린다(HR2). G1 실패 목록도 사람 기억이 아니라 스크립트 출력이 원천이다(HR3). G2의 일곱 항목은 눈으로 대조하면 반드시 새는데, 새는 쪽이 늘 자기 결정에 불리한 지적이다(HR4).

| 파일 | 상태 |
|---|---|
| check.mjs | fill, g1, g2 세 명령. 2026-09-08 H3에서 재작성 |
| build-v2.mjs | tmp/api-spec-v2/build-v2.mjs 사본. API 명세 조립용. 미검증 |
| tests/ | 골든 fixture 6개와 테스트 29건 |

원본은 tmp/ 아래에 그대로 둔다. 삭제하지 않는다.

## 명령

| 명령 | 검사 |
|---|---|
| node harness/tools/check.mjs fill <양식> [--dry] | 빈칸 잔존 0, 경로가 절대경로이고 존재, 버전 칸에 sha256 기입 |
| node harness/tools/check.mjs g1 <후보> --type doc\|api\|code | doc은 날짜 2종, 금지 기호 3종, 내부 링크, 종료 문장. api는 요청과 응답과 오류 절. code는 빌드와 테스트 결과 파일 존재 |
| node harness/tools/check.mjs g2 <결정표> | 버전 일치, 지적 ID 집합, 행 수, 빈 결정, 거부 이유, 반박 등급, 치명 거부 필수 필드, 남은 치명 |
| node --test harness/tools/tests/check.test.mjs | 골든 파일 테스트 |

종료 코드는 0 통과, 1 검사 실패, 2 사용법 오류다.

## 쓰기 정책

g1과 g2는 아무 파일도 쓰지 않는다. 읽기만 한다.

fill만 쓴다. 대상은 인자로 받은 그 파일 하나뿐이고 document/, harness/project-sync/, harness/docs/는 거부한다. 이전 판이 검사 도중 document/o2o-*.md를 덮어써서 생긴 규칙이다. 미리 보려면 --dry를 쓴다.

## 옵션

| 옵션 | 쓰임 |
|---|---|
| --type doc\|api\|code | g1 필수 |
| --end "<문장>" | g1 doc의 종료 문장을 바꾼다. 기본값은 07 v3 F16의 문장이다 |
| --artifact <경로> | g1 code에서 빌드나 테스트 결과 파일을 지정한다. 여러 번 쓸 수 있다 |
| --dry | fill이 쓰지 않고 결과만 보여 준다 |

## 경로 규칙

check.mjs는 저장소 루트를 스크립트 위치에서 유도한다(../..). 절대경로를 상수로 박지 않는다. 이전 판이 그렇게 해서 디렉터리 개편 두 번에 두 번 다 깨졌다.

명령을 적을 때 경로 구분자는 슬래시로 고정한다. .claude/settings.json의 allow 규칙이 명령 문자열 앞부분을 그대로 비교한다.
