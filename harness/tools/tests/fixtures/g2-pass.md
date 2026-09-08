# g2 통과 fixture

이 파일은 harness/prompts/decision-table.md v4의 표 구조를 그대로 따른다. 검사기에 맞춰 만들지 않는다. 그렇게 만들면 테스트가 코드의 거울이 되어 코드의 오해까지 통과시킨다. 2026-09-08 하네스 리뷰 HRV-07이 그 사고였다.

루트 토큰과 해시 토큰은 테스트가 실행 시점에 치환한다. 설명줄에 토큰 자체를 적으면 치환기가 그것도 경로로 읽는다.

## 대상과 원본

| 항목 | 기록 |
|---|---|
| Step | 9 |
| 작업 유형 | 설계 문서 |
| 평가 라운드 | R1 |
| 평가 대상 절대경로와 파일 목록 | __ROOT__/harness/tools/tests/fixtures/g1-doc-pass.md, __ROOT__/harness/tools/tests/fixtures/g1-api-pass.md |
| 평가 대상 버전 또는 해시 | sha256:__HASH:harness/tools/tests/fixtures/g1-doc-pass.md__, sha256:__HASH:harness/tools/tests/fixtures/g1-api-pass.md__ |
| 승인된 작업 계약 절대경로와 버전 | __ROOT__/harness/tools/tests/fixtures/fill-pass.md sha256:__HASH:harness/tools/tests/fixtures/fill-pass.md__ |
| A 원본 리포트 절대경로와 버전 또는 해시 | __ROOT__/harness/tools/tests/fixtures/g2-report-A.md sha256:__HASH:harness/tools/tests/fixtures/g2-report-A.md__ |
| B 원본 리포트 절대경로와 버전 또는 해시 | __ROOT__/harness/tools/tests/fixtures/g2-report-B.md sha256:__HASH:harness/tools/tests/fixtures/g2-report-B.md__ |
| A 원본 지적 수 | 2 |
| B 원본 지적 수 | 1 |
| 결정표 전체 행 수 | 3 |
| 판단한 사용자 | join5201 |
| 결정 날짜 | 2026-09-08 |

## 지적별 결정

| 리포트 A/B | 원본 번호 | 지적 ID | 심각도 | 대상 위치 | 위반 기준 | 원문 지적 | 원문 근거 | 결정 | 이유 |
|---|---|---|---|---|---|---|---|---|---|
| A | 1 | S9-R1-A-01 | 보통 | 3절 | F5 | 날짜 표기가 없다 | 3절 머리 | 수용 | 반영한다 |
| A | 2 | S9-R1-A-02 | 확인필요 | 4절 | 규칙 준수 | 배경 없이는 판단 불가 | 4절 표 | 반박 | 06-4 1-2에 근거가 있다 |
| B | 1 | S9-R1-B-01 | 치명 | 2절 | 추적성 | 불변식이 깨진다 | 2절 표 | 거부 | 오판 기록 MJ-01 |

## 사용자 추가 지적

| 추가 ID | 심각도 | 대상 위치 | 지적 | 근거 | 처리 |
|---|---|---|---|---|---|

## 치명 지적의 오판 판단 기록

| 항목 | 사용자 기록 |
|---|---|
| 판단 기록 ID | MJ-01 |
| Step, 라운드, A/B, 지적 ID | S9-R1-B-01 |
| 평가 대상 버전 또는 해시 | sha256:__HASH:harness/tools/tests/fixtures/g1-doc-pass.md__ |
| 근거 | 2절 표가 참조하는 계약이 06-4 1-2에 있고 거기서 불변식이 유지된다 |
| 대안 | 계약을 고치는 안을 검토했고 채택하지 않았다 |
| 안티패턴 | 심각도를 낮춰 검사를 통과시키는 것 |
| 사용자 결정 | 오판으로 정정 |
| 결정 날짜 | 2026-09-08 |

## 반영 전 G2 확인

| 확인 항목 | 기록 |
|---|---|
| 확인자와 날짜 | join5201, 2026-09-08 |
| G2 결과 | 통과 |
| 미완료 사유 | 해당 없음 |

## 반영과 최종 확인 인계

| 확인 대상 | 기록 |
|---|---|
| 반영본 절대경로와 버전 또는 해시 | __ROOT__/harness/tools/tests/fixtures/g1-doc-pass.md sha256:__HASH:harness/tools/tests/fixtures/g1-doc-pass.md__ |
| 남은 실제 치명 지적 | 없음 |
| 검증 미완료 사항 | 없음 |
