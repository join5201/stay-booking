# Codex 평가 요청 task-S8 R1 A

양식: harness/prompts/evaluate.md v4 (2026-09-08)
최초 작성: 2026-09-09
최종 갱신: 2026-09-09

왜 이 파일이 필요한가: 평가자가 무엇을 읽고 무엇을 안 읽는지가 정해지지 않으면 블라인드 평가가 아니라 답 맞추기가 된다. 이 파일이 그 목록을 고정한다.

너는 이 문서들이 만들어진 논의를 보지 못했고 볼 필요도 없다. 문서만으로 판단한다.

## 1. 이번 평가

| 항목 | 입력 |
|---|---|
| Task ID와 Step | task-S8, Step 8 |
| 작업 유형 | 설계 문서 |
| 라운드 | R1 |
| 구분 | A |
| 대상 버전 | 아래 2절의 sha256 아홉 건 |
| 리포트 출력 절대경로 | C:/Dev/potenup/99_projects/o2o/harness/reviews/task-S8-R1-A.md |
| 담당 | 규칙 준수. Step별 판정 축 |

## 2. 허용 입력

이 표에 없는 파일은 읽지 않는다. 목록에 없는 참조 파일을 자동으로 따라가지 않는다.

| 자료 | 절대경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 06-4 v5 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/06-4-o2o-contracts.v5.md | sha256:c32cc10985cb86c2 | 전문 |
| 평가 대상 06-2 v5 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/06-2-o2o-aggregates.v5.md | sha256:d260e2d76196eb0f | 전문 |
| 평가 대상 04-5 v2 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/04-5-o2o-policies-draft.v2.md | sha256:7435d1e4f714070c | 전문 |
| 평가 대상 04 v10 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/04-o2o-commands-actors.v10.md | sha256:fa0463666b950db6 | 전문 |
| 평가 대상 03 v13 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/03-o2o-event-storming.v13.md | sha256:36dd7d4dd5e6a04f | 전문 |
| 평가 대상 02 v9 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/02-o2o-feature-list.v9.md | sha256:ee9bcf474a8f9f8a | 전문 |
| 평가 대상 06-1 v2 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/06-1-o2o-context-map.v2.md | sha256:90c016f3ec3e7c9a | 전문 |
| 평가 대상 05-3 v11 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/05-3-o2o-glossary.v11.md | sha256:276f6cf7cc25805b | 전문 |
| 평가 대상 입력 팩 v3 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/o2o-review-input-pack.v3.md | sha256:41d7582034ad30da | 전문 |
| 입력 팩 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 승인된 Task 계약 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/task-S8.md | sha256:0acfdb9c844398c2 | 전문 |
| 실제 평가 기준 | C:/Dev/potenup/99_projects/o2o/harness/out/task-S8-R1/eval-input/eval-criteria-ddd.md | sha256:0ec4e54137d66085 | 3절 축, 4절 출력 스키마, 5절 심각도 |

버전 또는 해시 칸은 사람이 손으로 적지 않는다. node harness/tools/check.mjs fill이 채운다(HR2).

읽지 않는 것: 01 전체, 08-1과 08-2와 08-3 원문, harness/decisions/ 전체, harness/docs/ 전체, harness/state/ 전체, 이전 평가 리포트, 생성 대화, 다른 평가자의 리포트.

08 계열 셋을 빼는 이유는 그것이 과거 감사 문서이기 때문이다. 무엇이 어디로 갔어야 하는지는 계약의 반영 대상 표가 알려 준다. 그 표와 대상 문서를 대조하지 원본 감사 문서를 대조하지 않는다.

입력 팩이 두 줄인 것은 오기가 아니다. 배경으로 읽는 것은 project-sync의 현재 판이고, 평가 대상 입력 팩 v3은 이번에 만든 후보다.

## 3. 적용할 축

eval-criteria-ddd.md 3절은 문서 종류를 먼저 판별하고 해당 축만 적용하라고 한다. 축을 전부 섞어 던지지 않는다.

| 대상 | 적용할 축 묶음 | 왜 그 묶음인가 |
|---|---|---|
| 06-4 v5 | DbC 계약 6축 | 루트 메서드의 선행조건과 후행조건과 예외를 적는 계약 문서다 |
| 06-2 v5 | 애그리거트와 CRC 6축 | 애그리거트 경계와 불변식 문서다 |
| 06-1 v2 | 컨텍스트 맵 6축 | 컨텍스트 간 관계 문서다 |
| 04-5 v2 | 정책과 Saga 5축 | 정책 목록 문서다 |
| 04 v10 | 애그리거트와 CRC 중 커맨드 고아 하나 | 커맨드와 루트 매핑이 이 문서의 정면이다 |
| 03 v13 | 정책과 Saga 중 이벤트 전수 커버리지와 경합 조건 둘 | 이번 판의 변경이 종료 조건과 경합 처리다 |
| 05-3 v11 | 컨텍스트 맵 중 언어 충돌 하나 | 용어 사전이다 |
| 02 v9 | 없음. 준비 미완료 | 기능 목록 문서를 위한 축 묶음이 3절에 없다 |
| 입력 팩 v3 | 없음. 준비 미완료 | 같음 |

02 v9와 입력 팩 v3은 evaluate.md 절차 1에 따라 준비 미완료로 기록한다. 없는 축을 만들어 적용하지 않는다. 그 둘에 대한 A의 판정은 준비 미완료 한 줄이고 지적 0건과 구분해서 적는다. 두 문서의 공통 축 판정은 B가 한다.

각 묶음에서 이 문서에 대상이 없는 축은 해당 없음으로 남긴다. 억지로 채우지 않는다.

## 4. 이번 판이 무엇을 바꿨는가

계약의 반영 대상 표가 문서별 완료 조건을 적는다. 그 표와 대상 문서를 대조한다. 조건을 채웠는지, 채우면서 다른 것을 깨뜨렸는지 둘 다 본다.

## 5. 출력

eval-criteria-ddd.md 4절 스키마를 따른다. 판정 요약, 상세, 요구사항 역추적표 셋이다.

그 뒤에 보조 표를 붙인다. 열은 원본 번호, 지적 ID, 심각도, 위반 기준이다. 판정 요약의 등급별 수와 이 표의 등급별 수가 같아야 한다. G2가 이 표에서 심각도를 읽어 결정표와 대조한다. 표가 없거나 수가 어긋나면 리포트가 잘린 것으로 처리한다.

지적 ID는 S8-R1-{A 또는 B}-{원본 번호 2자리}다. 예로 S8-R1-A-01이다.

보조 표 뒤에 다음을 적는다. 실제 읽은 파일과 버전, 적용한 축, 시작과 종료 시각과 실제 사용 시간 또는 미측정, 미확인 범위와 추가로 필요한 입력.

심각도는 5절 정의를 쓴다. 확인필요 등급을 반드시 유지한다. 문맥 부족으로 판단이 안 되는 것을 치명으로 올리지 않는다.

지적 수 0과 검증 완료를 구분한다. 리포트가 불완전하거나 필수 확인을 못 했으면 통과로 기록하지 않는다.

대상 문서를 고치지 않는다. 사용자 대신 완료를 승인하지 않는다. 별도 LLM API를 호출하지 않는다.

## 6. 코드 실행

해당 없음. 설계 문서 평가다.
