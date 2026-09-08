# harness-prompts/tools/

무엇이 들어가는가: 검사 스크립트다. LLM을 부르지 않는 로컬 검사 도구.

왜 필요한가: 양식의 버전 칸을 사람이 손으로 적으면 틀린다. 스크립트가 sha256으로 채운다(HR2). G1 실패 목록도 사람 기억이 아니라 스크립트 출력이 원천이다(HR3).

| 파일 | 상태 |
|---|---|
| check.mjs | tmp/document-review/check.mjs 사본. 아직 확장 전이다. H3에서 fill, g1, g2 세 명령으로 확장한다 |
| build-v2.mjs | tmp/api-spec-v2/build-v2.mjs 사본. API 명세 조립용 |

원본은 tmp/ 아래에 그대로 둔다. 삭제하지 않는다.

H3에서 확정할 명령

| 명령 | 검사 |
|---|---|
| node harness-prompts/tools/check.mjs fill <양식> | 빈칸 잔존 0, 경로 존재, 버전 칸 sha256 기입 |
| node harness-prompts/tools/check.mjs g1 <후보> --type doc 또는 api 또는 code | 양식 필수 항목, 날짜, 금지 기호, 종료 문장 |
| node harness-prompts/tools/check.mjs g2 <결정표> | ID 집합 일치, 행 수, 빈 결정, 거부 이유, 반박 등급, 치명 거부 필수 필드 |

경로 구분자는 슬래시로 고정한다. .claude/settings.json의 allow 규칙이 명령 문자열 앞부분을 그대로 비교한다.
