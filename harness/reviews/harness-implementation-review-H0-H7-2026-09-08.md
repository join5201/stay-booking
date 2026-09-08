# 하네스 구현 리뷰 H0~H7

최초 작성: 2026-09-08
최종 갱신: 2026-09-08
대상 프로젝트: C:/Dev/potenup/99_projects/o2o
성격: 직전 대화에서 수행한 하네스 구현 리뷰의 저장본

이 문서는 평가 절차를 하네스가 보장하는지 검토한 결과다. O2O 프로젝트의 설계 결론이나 합격 여부를 판정한 A/B 리포트가 아니다. 아래 SHA256과 실행 결과는 직전 리뷰에서 확인한 값이다. 파일 저장을 위해 구현 검토나 테스트를 다시 수행하지 않았다.

검토는 요구사항 확인, 허용 자료 열람 또는 명령 실행, 관찰 기록, 판정 순서로 진행했다. 발견 사항의 HRV ID는 하네스 구현 리뷰용이며, 설계 평가의 S{Step}-R{라운드}-{A/B}-{번호}와 구분한다.

## 1. 전체 판단

현재 G1/G2 통과만으로 평가의 신뢰성이 보장된다고 보기 어렵다. 원본 지적이 누락되거나 심각도가 바뀌어도 통과할 수 있고, 실제 치명을 수정하려는 정상 흐름이 G2에서 막힌다. 보호 경로 검사 우회와 부적절한 코드 검증 자료의 통과는 직접 재현했다.

마스터 PTCF의 H0~H7을 기준으로 설정, 검사기, 테스트, 양식, 진행 기록과 새로 확인된 task-S8 계약 초안을 검토했다. H2의 도메인 설계 결론은 평가하지 않았다. 마지막 조회에서 H2~H5와 task-S8 계약은 승인 대기 상태였다. H7은 선택표를 검토했으며 실제 A/B 평가 실행은 확인하지 못했다.

| 요구사항 | 현재 구현과 통제 수준 |
|---|---|
| HR1 입력 분리 | 프롬프트와 파일 복사 규칙이다. 생성 입력과 평가 입력 목록이 충돌한다 |
| HR2 버전 기록 | fill의 일부 표에서 해시를 계산한다. 기존 값 재검증과 결정표 연결에 누락이 있다 |
| HR3 실패 목록 보존 | 검사기가 실패 목록을 출력한다. 재요청에 그대로 전달하는 것은 운영 규칙이다 |
| HR4 결정표 검사 | 코드가 있으나 원본 스키마, 심각도, 실행 식별과 기록 연결을 충분히 검사하지 않는다 |
| HR5 사용자 완료 승인 | 문서에 명시돼 있다. 승인 준수를 기계적으로 입증하는 실행 기록은 확인하지 못했다 |
| HR6 실패 복구 | 진행 기록과 선택표에 의존한다. 중단 및 재실행 조건에 공백이 있다 |

A/B 작업 분리, 승인 명령 제한, 소스와 테스트 수정 금지는 현재 운영 지시다. [실행 방식 결정 78행](C:/Dev/potenup/99_projects/o2o/harness/docs/10-4-o2o-harness-workflow-decisions.md:78)도 평가 폴더가 강제 접근 차단을 보장하지 않는다고 명시한다. 자동 실행기나 hooks가 없다는 사실 자체는 이번 범위의 결함으로 처리하지 않았다.

리뷰 당시 파일 생성과 수정은 하지 않았다. 이후 사용자의 요청으로 이 리포트 파일만 생성했다. 이번 리뷰에서 읽은 운영 문서와 상태 기록은 하네스 리뷰 근거이며, 실제 A/B 평가자에게 전달할 허용 자료를 뜻하지 않는다.

## 2. 발견 사항

영향이 큰 순서로 정리했다. 실행으로 확인한 항목과 코드 경로로 판정한 항목을 구분했다.

### HRV-01. 결정표의 심각도를 바꾸면 치명 보호 검사를 우회한다

구분: 요구 위반

- 근거: HR4와 [결정표 38행](C:/Dev/potenup/99_projects/o2o/harness/prompts/decision-table.md:38)은 원본 심각도 보존을 요구한다. [검사기 303행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:303)은 원본에서 ID만 추출하고, [334행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:334)의 반박 및 치명 거부 검사는 결정표의 심각도를 사용한다.
- 발생 조건과 관찰: 원본 치명 지적을 결정표에서 보통으로 바꾸면 치명 거부 기록 검사가 생략된다. 확인필요로 바꾸면 반박 조건도 충족한다. ID와 행 수는 그대로여서 이 변경을 검출하지 못한다. 정적 분석 결과다.
- 영향: 심각도 하향과 부적절한 반박이 기계 검사를 통과할 수 있다. 사람의 원문 대조는 별도 통제로 남는다.
- 최소 수정 방향: 원본 상세 행과 보조 ID를 연결하고, 원본 심각도로 결정 규칙을 검사한다.
- 수정 후 확인: 원본 치명을 유지한 채 결정표만 보통 또는 확인필요로 바꾼 두 사례가 모두 실패해야 한다.

### HRV-02. 불완전 리포트를 0건으로 처리하고 다른 실행의 결과도 받아들일 수 있다

구분: 요구 위반

- 근거: [결정표 79행부터의 확인 항목](C:/Dev/potenup/99_projects/o2o/harness/prompts/decision-table.md:79)은 원본 행 보존과 미완료의 0건 처리 금지를 요구한다. [검사기 297행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:297)은 A/B의 ID 문자열을 하나의 집합으로 합치고, [315행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:315)은 결정표에 선언된 수만 비교한다.
- 발생 조건과 관찰: 상세 지적은 있지만 보조 ID 표 전에 응답이 잘리면 해당 지적을 추출하지 못한다. 그 평가자의 수를 0으로 적으면 나머지 조건에 따라 통과할 수 있다. A/B에 같은 파일을 지정하거나 결정표의 Step과 라운드를 바꾸는 것도 검사하지 않는다. 정적 분석 결과다.
- 영향: 누락, 파싱 실패, 중복 결과와 다른 실행의 결과가 정상 자료처럼 대조될 수 있다.
- 최소 수정 방향: 원본별 스키마, 상세 행 수, ID 연결, 역할, Step과 라운드를 검증한 뒤 집합을 비교한다. 파싱 실패와 정상 0건을 구분한다.
- 수정 후 확인: 잘린 리포트, 잘못된 ID, 동일 파일 A/B 지정, 다른 Step 및 라운드는 실패하고 정상 완료된 0건 리포트는 통과해야 한다.

원래 평가 스키마의 정상 완료 여부를 기계 G2가 증명한다고 설명해서는 안 된다. 실제 운영에서 사람이 이 문제를 차단했는지는 확인하지 못했다.

### HRV-03. 생성용 입력 목록을 평가용으로 복사하는 경로가 남아 있다

구분: 요구 위반. 실제 혼입 발생 여부는 확인필요

- 근거: HR1은 이전 리포트와 결정표 등을 평가 입력에서 제외한다. 하지만 [계약 양식 25행](C:/Dev/potenup/99_projects/o2o/harness/prompts/task-contract.md:25)은 유일한 입력 목록에 01 전체를 요구한다. 현재 [task-S8 계약 75행부터의 입력 목록](C:/Dev/potenup/99_projects/o2o/harness/tasks/task-S8.md:75)에는 과거 감사 문서와 사용자 결정표가 들어 있다. [H7 선택표 300행](C:/Dev/potenup/99_projects/o2o/harness/prompts/harness-ptcf-prompt.md:300)은 계약의 HR1 목록을 평가 폴더에 복사하도록 한다.
- 발생 조건과 관찰: 계약의 입력 목록을 그대로 복사하면 금지 자료가 평가 폴더에 들어간다. 이를 빼면 어떤 목록과 일치시켜야 하는지 불명확하다. 실제 평가 폴더는 확인되지 않았다.
- 영향: 입력 준비자가 상충하는 규칙을 해석해야 하므로 블라인드 입력 구성을 재현하기 어렵다.
- 최소 수정 방향: 계약에서 생성용 입력과 A/B 평가 허용 입력을 구분하고, 평가 준비는 후자만 참조하도록 맞춘다.
- 수정 후 확인: 생성 입력에 감사 문서와 결정표가 있어도 평가 파일 목록과 요청 본문에는 혼입되지 않는지 대조한다.

연결된 확인필요가 있다. [진행 기록 24행](C:/Dev/potenup/99_projects/o2o/harness/state/progress.md:24)은 실패 원인을 재평가 프롬프트에도 넣도록 한다. R1 평가 관련 기록까지 R2에 전달할 수 있는 지시다. G1 형식 보정용 재요청과 블라인드 재평가를 구분해야 한다.

### HRV-04. 반영 전 G2가 치명 수정 완료를 요구해 정상 진행을 막는다

구분: 요구 위반

- 근거: [H7 선택표 302행과 다음 행](C:/Dev/potenup/99_projects/o2o/harness/prompts/harness-ptcf-prompt.md:302)은 G2 통과 후 수용 항목을 반영한다. 그런데 [검사기 347행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:347)은 남은 실제 치명이 없음이어야 G2를 통과시킨다.
- 발생 조건과 관찰: 사용자가 실제 치명을 수용하고 수정하려는 경우, 반영 전이므로 해당 치명이 남아 있다고 적으면 G2가 실패한다. 정적 분석 결과다.
- 영향: 수정하려면 G2를 통과해야 하지만, 수정 전에는 G2를 통과할 수 없다.
- 최소 수정 방향: 반영 준비 검사와 최종 완료 검사를 구분한다. 실제 치명 잔존은 최종 확인에서 판정한다.
- 수정 후 확인: 치명 수용 상태의 반영 전 G2는 통과하고, 수정되지 않은 치명은 최종 완료를 막아야 한다.

### HRV-05. 경로 대소문자를 바꾸면 fill의 보호 검사를 우회한다

구분: 요구 위반

- 근거: [검사기 32행부터의 쓰기 정책](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:32)은 보호 경로 쓰기 금지를 명시한다. [64행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:64)은 경로를 대소문자를 구분하는 문자열로 비교한다.
- 발생 조건과 관찰: 같은 문서에 harness/docs/ 경로를 사용하면 종료 코드 1과 fill.protected가 나왔다. HARNESS/docs/ 경로로 바꾸면 종료 코드 0과 PASS가 나왔다. --dry로 직접 재현했다.
- 영향: 보호 경로 안에 해시를 채울 표가 있고 일반 fill을 실행하면 [169행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:169)의 쓰기까지 도달할 수 있다. 실제 덮어쓰기는 실행하지 않았다.
- 최소 수정 방향: 실제 파일시스템의 경로 동일성을 반영해 보호 경로를 비교한다.
- 수정 후 확인: 동일 파일을 대소문자가 다른 경로로 지정해도 모두 차단되고 파일 해시가 유지돼야 한다.

### HRV-06. 기존 해시가 있으면 입력 변경을 재검증하지 않는다

구분: 요구 위반

- 근거: HR2는 모든 입력과 대상의 버전을 스크립트가 계산하도록 요구한다. [검사기 158행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:158)은 해시 칸에 빈칸 표시가 아닌 값이 있으면 계산을 건너뛴다.
- 발생 조건과 관찰: 최초 fill 후 입력 파일을 변경해도 재실행에서 기존 값을 비교하지 않는다. v3 같은 임의 문자열도 같은 조건으로 건너뛴다. G2도 계약 자체의 해시를 확인할 뿐 계약 안의 입력 목록을 재검증하지 않는다. 정적 분석 결과다.
- 영향: 기록된 버전과 실제 평가 입력이 달라도 발견하지 못할 수 있다. 평가 기준 파일도 같은 경로로 변경되면 이 문제에 해당한다.
- 최소 수정 방향: 기존 해시도 실제 바이트와 비교하고 불일치를 실패로 처리한다. 평가에 전달한 사본과 기록된 해시도 연결한다.
- 수정 후 확인: 승인된 입력이나 평가 기준을 변경한 후 검사하면 버전 불일치로 실패해야 한다.

### HRV-07. 결정표 양식과 fill 및 G2의 입력 구조가 맞지 않는다

구분: 요구 위반

- 근거: [결정표 14행과 다음 행](C:/Dev/potenup/99_projects/o2o/harness/prompts/decision-table.md:14)은 경로와 해시를 서로 다른 행에 둔다. [검사기 288행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:288)은 해시 행 안에서 경로까지 찾는다. [150행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:150)의 fill은 가로 열 제목이 있는 표만 해시 기입 대상으로 삼는다.
- 발생 조건과 관찰: 결정표 양식대로 경로와 해시를 나누면 G2가 경로 없음으로 실패한다. 항목/기록 형태인 결정표에는 fill의 해시 자동 기입도 적용되지 않는다. [통과 fixture 12행](C:/Dev/potenup/99_projects/o2o/harness/tools/tests/fixtures/g2-pass.md:12)은 양식과 달리 경로와 해시를 합쳐 이 문제를 피한다.
- 영향: 실제 양식을 사용하는 흐름과 골든 테스트가 검증하는 흐름이 다르다. 여러 대상 파일을 적어도 G2는 해당 셀의 첫 경로와 해시만 읽는다.
- 최소 수정 방향: 양식과 검사기의 입력 구조를 일치시키고 파일 목록 전체를 처리한다.
- 수정 후 확인: 실제 결정표 양식을 채운 사례로 fill에서 g2까지의 연결을 확인하고, 두 번째 대상 파일의 변경도 실패로 잡아야 한다.

### HRV-08. 다른 지적을 언급한 표가 해당 지적의 오판 기록으로 인정된다

구분: 요구 위반

- 근거: [결정표 64행](C:/Dev/potenup/99_projects/o2o/harness/prompts/decision-table.md:64)과 [84행](C:/Dev/potenup/99_projects/o2o/harness/prompts/decision-table.md:84)은 오판 기록을 해당 지적에 연결하도록 한다. [검사기 391행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:391)은 어떤 셀에서든 ID 문자열을 찾으면 그 표를 반환한다.
- 발생 조건과 관찰: B-01 오판 기록의 근거에 B-02를 언급하면, B-02의 별도 기록이 없어도 B-01 표의 필수 필드로 검사를 충족할 수 있다. 정적 분석 결과다.
- 영향: 지적별 사용자 판단이 없는 치명 거부가 기록 요건을 갖춘 것으로 처리될 수 있다.
- 최소 수정 방향: 지정된 식별 필드에서 정확한 ID를 읽고 연결의 누락과 중복을 검사한다.
- 수정 후 확인: 다른 기록의 근거나 대안에 ID를 언급하는 것만으로는 해당 지적의 기록 검사가 통과하지 않아야 한다.

### HRV-09. G1이 H3에서 요구한 검사 범위를 충족하지 않는다

구분: 요구 위반

- 근거: [H3 완료 조건 232행](C:/Dev/potenup/99_projects/o2o/harness/prompts/harness-ptcf-prompt.md:232)은 승인 양식의 필수 항목, 내부 링크, 빌드와 테스트 결과 파일 확인을 요구한다. 실제 구현은 [doc 검사 239행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:239), [링크 검사 205행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:205), [code 검사 256행](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs:256)에 있다.
- 발생 조건과 관찰: doc 검사는 승인 양식의 필수 항목을 읽지 않는다. 링크 검사는 일부 절대경로와 특정 옛 상대경로만 처리한다. 루트 치환 전 fixture의 __ROOT__/ 링크도 PASS였다. code 검사는 비어 있지 않은 파일 하나면 충족하며, 검사기 소스를 결과 파일로 줘도 PASS였다.
- 영향: 필수 항목 누락과 잘못된 링크, 빌드 또는 테스트 자료 누락이 G1에서 검출되지 않을 수 있다.
- 최소 수정 방향: 승인 양식의 필수 항목과 링크 대상 검사 범위를 연결하고, 계약에서 요구한 빌드 및 테스트 자료를 각각 확인한다.
- 수정 후 확인: 필수 항목 누락, 깨진 상대 링크와 앵커, 빌드만 있는 경우, 소스를 결과로 지정한 경우가 실패해야 한다.

현재 H3 요구는 코드 결과 파일의 존재 확인까지다. 결과 파일이 있다는 사실을 실제 테스트 성공으로 해석해서는 안 된다.

### HRV-10. H7 선택표에 완료 및 실패 복구 조건이 빠져 있다

구분: 요구 위반. 실제 운영 실패는 미재현

- 근거: [선택표 296행](C:/Dev/potenup/99_projects/o2o/harness/prompts/harness-ptcf-prompt.md:296)은 위에서 처음 맞는 행을 선택한다. [299행부터 306행의 조건](C:/Dev/potenup/99_projects/o2o/harness/prompts/harness-ptcf-prompt.md:299)은 아래 경우를 처리하지 못한다.
- 발생 조건과 관찰: 최종 확인을 마친 뒤 사용자가 완료를 승인해도 그 앞의 최종 확인 행 조건이 계속 참이다. R1 폴더 생성 후 후보 작성이나 G1 도중 중단되면 폴더 없음 조건으로 돌아갈 수 없다. G2 실패 후 결정표를 수정한 경우도 기존 실행과 새 버전의 미실행 상태를 구분하지 않는다.
- 영향: 최종 확인을 반복하거나, 중단 후 어느 단계에서 재개할지 임의 판단하게 된다.
- 최소 수정 방향: 현재 단계, 산출물 버전과 해당 검사의 성공 여부를 선택 조건에 반영한다.
- 수정 후 확인: 폴더만 생성된 중단, G2 실패 후 수정, R2 완료, 재평가 생략 후 최종 승인 사례를 선택표에 순서대로 대입한다.

자동 실행기 추가를 요구하는 지적은 아니다. 현재 수동 선택표의 조건을 보완하는 문제다.

### HRV-11. H0의 비밀 파일 편집 금지가 설정에 일부만 반영됐다

구분: 요구 위반. 실제 권한 적용 효과는 확인필요

- 근거: [H0 완료 조건 183행](C:/Dev/potenup/99_projects/o2o/harness/prompts/harness-ptcf-prompt.md:183)은 비밀 파일의 읽기와 편집 금지를 요구한다. [설정 9행부터 19행](C:/Dev/potenup/99_projects/o2o/.claude/settings.json:9)은 secrets, 키 파일, SSH와 AWS에 읽기 금지만 두고 편집 금지는 일부 경로에만 둔다.
- 발생 조건과 관찰: 해당 비밀 파일의 편집은 현재 파일에 열거된 Edit deny에 포함되지 않는다. 상위 설정과 실제 앱 권한 동작은 검증하지 않았다.
- 영향: H0가 요구한 명시적 편집 차단 범위를 이 설정만으로 보장할 수 없다.
- 최소 수정 방향: 요구한 읽기 및 편집 금지 대상의 대응을 맞춘다.
- 수정 후 확인: 비밀 값이 없는 시험 대상과 실제 도구로 각 제한을 확인한다.

이 설정은 Claude 생성자 설정이다. Codex 평가자의 쓰기 제한을 보장하는 근거로 사용할 수 없다.

### HRV-12. task-S8 초안의 하네스 실패 예외가 중단 규칙과 충돌할 수 있다

구분: 확인필요

- 근거: [계약 118행](C:/Dev/potenup/99_projects/o2o/harness/tasks/task-S8.md:118)은 필수 검증 미실행 시 중단을 요구한다. 바로 뒤 [120행](C:/Dev/potenup/99_projects/o2o/harness/tasks/task-S8.md:120)은 하네스 결함이면 기록하고 계속 진행하며 재시도 횟수에서도 제외하도록 한다.
- 발생 조건과 관찰: G1/G2 오류를 하네스 결함으로 분류했을 때, 검사가 복구될 때까지 기다려야 하는지 다음 단계로 넘어가도 되는지 정해져 있지 않다. 계약은 미승인 초안이며 실제 우회 실행은 확인하지 못했다.
- 영향: 게이트 실패를 예외로 처리하거나 재시도 상한을 적용하지 않는 해석이 가능하다.
- 최소 수정 방향: 계속할 수 있는 독립 작업과, 게이트 복구 및 재검증이 필요한 후속 단계를 구분한다.
- 수정 후 확인: 검사기 오류가 나더라도 검증 성공 전에는 평가 전달이나 반영 단계로 진행하지 않는지 확인한다.

## 3. 검증 내역

### 3.1 직접 실행한 결과

작업 디렉터리는 C:/Dev/potenup/99_projects/o2o다. 아래는 당시 실행한 명령 문자열이다. 파일을 쓰지 않는 경로로 실행했다.

| 명령 | 결과 |
|---|---|
| `node harness/tools/check.mjs g1 harness/tools/tests/fixtures/g1-doc-pass.md --type doc` | 종료 0. PASS, 검사 8건 |
| `node harness/tools/check.mjs g1 harness/tools/tests/fixtures/g1-api-pass.md --type code --artifact harness/tools/check.mjs` | 종료 0. PASS, 검사 2건. HRV-09 재현 |
| `node harness/tools/check.mjs fill harness/prompts/decision-table.md --dry` | 종료 1. 미작성 양식의 빈칸 35건 검출 |
| `node harness/tools/check.mjs fill harness/docs/10-4-o2o-harness-workflow-decisions.md --dry` | 종료 1. 보호 경로 거부 |
| `node harness/tools/check.mjs fill HARNESS/docs/10-4-o2o-harness-workflow-decisions.md --dry` | 종료 0. 보호 검사 우회. HRV-05 재현 |

파일 탐색과 열람, SHA256 계산도 직접 수행했다. 미작성 결정표의 실패는 예상된 결과이며, 양식과 G2의 정상 연결을 검증한 결과는 아니다.

### 3.2 생성자가 기록한 결과

[진행 기록 59행](C:/Dev/potenup/99_projects/o2o/harness/state/progress.md:59)에 테스트 29건 통과와 연기 테스트 3건이 적혀 있다. [62행](C:/Dev/potenup/99_projects/o2o/harness/state/progress.md:62)에는 계약 fill 검사 37건과 해시 18건 기입이 기록돼 있다.

이것은 생성자의 기록이다. 해당 실행 원본 로그를 대조하거나 이번 리뷰에서 재현한 결과가 아니다.

### 3.3 실행하지 못한 항목과 한계

- 전체 node --test: [테스트 29행](C:/Dev/potenup/99_projects/o2o/harness/tools/tests/check.test.mjs:29)과 [40행](C:/Dev/potenup/99_projects/o2o/harness/tools/tests/check.test.mjs:40)이 임시 디렉터리와 파일을 생성하므로 당시 쓰기 금지 범위에서 실행하지 않았다.
- 변형 리포트를 사용하는 G2 재현과 실제 보호 파일 덮어쓰기: 실행하지 않았다.
- 실제 A/B 작업 분리, 평가 전후 소스 무변경, 전달 입력과 읽은 파일의 일치: 실행 자료가 없어 확인하지 못했다.
- 중복 실행, 시간 초과, 사용자 승인 후 반영 및 완료: 선택표만 검토했다.
- C:/Dev/potenup/99_projects/o2o/harness/tools/build-v2.mjs의 동작과 쓰기 범위: 검증하지 않았다.
- O2O 도메인 설계의 적정성과 합격 여부: 이번 리뷰 대상이 아니다.

### 3.4 읽은 파일과 SHA256

주 검토와 병렬 검토에서 전문 또는 필요한 구간을 읽은 파일이다. 적용 기준은 마스터 PTCF v2, 실행 방식 결정 v5, 평가 요청 및 결정표 v3, 설계 평가 기준 v1, 코드 평가 기준 v0다.

검토 중 계약과 진행 기록이 변경됐다. task-S8.md와 progress.md는 마지막에 동일한 메모리 바이트에서 내용과 해시를 함께 확인했다. 검사기와 테스트 파일의 해시는 재조회에서도 같았다. 아래 값은 리뷰 당시 마지막 확인값이며 저장 시점의 재검증 결과가 아니다.

| 파일 | SHA256 |
|---|---|
| [AGENTS.md](C:/Dev/potenup/99_projects/o2o/AGENTS.md) | `6cf0a192aab3bba8170f5a487197412793247864de77b99719fcdab8c8562670` |
| [CLAUDE.md](C:/Dev/potenup/99_projects/o2o/CLAUDE.md) | `fefad7cf7df4da59f1ffe8e49c4bd7714cf849431bc6347ac335513e62dc3cde` |
| [settings.json](C:/Dev/potenup/99_projects/o2o/.claude/settings.json) | `67b0c19a74443fd5ee460a6b1588387e9eb15f79dfa8198aa92de95e1c4c27d7` |
| [마스터 PTCF](C:/Dev/potenup/99_projects/o2o/harness/prompts/harness-ptcf-prompt.md) | `d0343b1925ed83a49e4e235b03f8a8b6d2494f9bd8ca4dff684e0b40d861ffaa` |
| [task-contract.md](C:/Dev/potenup/99_projects/o2o/harness/prompts/task-contract.md) | `78ceb0e06c30c279d917e63ded50720fbc2d686a9a6c2fc9b69e62b8d34ba253` |
| [generate.md](C:/Dev/potenup/99_projects/o2o/harness/prompts/generate.md) | `9c59f5caff9f50e1a31bebe5374e02dbedb0e1d74db70e9e7a696256f24fd788` |
| [evaluate.md](C:/Dev/potenup/99_projects/o2o/harness/prompts/evaluate.md) | `c94a0673afa0bf0f8ad5994a8bf68705f3cba3fa78b9899b29c9c87cd8335cc1` |
| [decision-table.md](C:/Dev/potenup/99_projects/o2o/harness/prompts/decision-table.md) | `210047f5f8f801d13542781693c2cf57f481bad63b512a161bcfe0485b5f4bb2` |
| [apply.md](C:/Dev/potenup/99_projects/o2o/harness/prompts/apply.md) | `21c71be5128cac155e94db3069e71a57cb9ceb51f3e6185bcd614b81975bfed6` |
| [eval-criteria-ddd.md](C:/Dev/potenup/99_projects/o2o/harness/prompts/eval-criteria-ddd.md) | `0ec4e54137d66085f5d88db6eaeb7126334c4f7d3358dc85d752c53fc3dfae8b` |
| [eval-criteria-code.md](C:/Dev/potenup/99_projects/o2o/harness/prompts/eval-criteria-code.md) | `c5ed835951fe09a5c015d50abbbacf9755af6ba8be807e1be1b9c0ba2cb1553d` |
| [task-S8.md](C:/Dev/potenup/99_projects/o2o/harness/tasks/task-S8.md) | `18958ee8d2c36a0170d781bf508821119b13aa1dbbbac826b087c2095f363bb5` |
| [tasks/README.md](C:/Dev/potenup/99_projects/o2o/harness/tasks/README.md) | `c23b5cf814b70280637bf48cc9c9b6e1a334641052d74c513f9338e90986ae6e` |
| [reviews/README.md](C:/Dev/potenup/99_projects/o2o/harness/reviews/README.md) | `96d3c39dc9d199ce15397c5e621eb2e847e73bc8458b2adeb6c879ed6bc032b0` |
| [progress.md](C:/Dev/potenup/99_projects/o2o/harness/state/progress.md) | `985f74c54c0741948789df422894666da254246a88519c2fa5b223cad94c52a9` |
| [10-4 실행 방식 결정](C:/Dev/potenup/99_projects/o2o/harness/docs/10-4-o2o-harness-workflow-decisions.md) | `fbd3c734d7088abba371d8959dbbd8a1ca665af96a895d3b1d6ae22dd622280b` |
| [10-5 인계](C:/Dev/potenup/99_projects/o2o/harness/docs/10-5-o2o-harness-handoff.md) | `b98a9ad3c464f1df5ee21974fa6467efc7a5f35353866d9b322f28cc30c3db81` |
| [10-6 구현 계획](C:/Dev/potenup/99_projects/o2o/harness/docs/10-6-o2o-harness-implementation-plan.md) | `6289683579908585632ef85b1f1d65de1f4add18e271f8d90aa9d691466d9b63` |
| [10-8 재점검](C:/Dev/potenup/99_projects/o2o/harness/docs/10-8-o2o-harness-recheck.md) | `80a6299cb3b6dfbe03f7732928df90cee5f98831e527ed97f2d84d8b6411d5f5` |
| [check.mjs](C:/Dev/potenup/99_projects/o2o/harness/tools/check.mjs) | `60807aa1df641bb8b47a9c144203cd3609f9df0f262552b2a5e549b557b2b698` |
| [tools/README.md](C:/Dev/potenup/99_projects/o2o/harness/tools/README.md) | `1ac8a7dfe51dd8d4b06dcef58ed4a4b49b4596cee40f646eb9bdf97d31bfdb70` |
| [check.test.mjs](C:/Dev/potenup/99_projects/o2o/harness/tools/tests/check.test.mjs) | `c2a7f0da6d019e83643e0621ed13f9520a868ab5c9809379b90c7a861e298843` |
| [fill-pass.md](C:/Dev/potenup/99_projects/o2o/harness/tools/tests/fixtures/fill-pass.md) | `536ad0b0c426b6005ba2d07b86c0a9b96e35e07da7c9eeb0a84bff8df99ad092` |
| [g1-doc-pass.md](C:/Dev/potenup/99_projects/o2o/harness/tools/tests/fixtures/g1-doc-pass.md) | `459ac502d736a718f4cd301d440e4356fbd5c46e49428dce67f4d3f3b68df458` |
| [g1-api-pass.md](C:/Dev/potenup/99_projects/o2o/harness/tools/tests/fixtures/g1-api-pass.md) | `d50a0fc10637bff44246c6433d812b80f481381d3529bad3753f436f86bbda7f` |
| [g2-pass.md](C:/Dev/potenup/99_projects/o2o/harness/tools/tests/fixtures/g2-pass.md) | `406c01e074914991c3ad3bd4cf0dc0076b35d734546bb73c0a852190f9315ec3` |
| [g2-report-A.md](C:/Dev/potenup/99_projects/o2o/harness/tools/tests/fixtures/g2-report-A.md) | `2600a00e416b90c7557cd0df94386a4c74da62b45fdc865067f0e51c8186b4a5` |
| [g2-report-B.md](C:/Dev/potenup/99_projects/o2o/harness/tools/tests/fixtures/g2-report-B.md) | `6a03d5597ee9f8bef2d25adc354e1f4b657275c493f83ba2bc186eeec84083d0` |

## 4. 다음 작업

1. HRV-01, 02, 04, 08부터 처리한다. 원본 지적 보존과 정상적인 치명 수정 흐름이 우선이다.
2. HRV-03, 06, 07을 함께 처리한다. 역할별 입력 목록, 실제 파일 버전, 양식과 검사기의 연결을 맞춘다.
3. HRV-05, 09, 11의 보호 및 검사 누락을 보완한다.
4. HRV-10을 반영한 선택표로 중단과 재개 사례를 확인한다. 이후 실제 A/B 첫 실행에서 운영 규칙 준수 여부를 확인한다.

사용자 결정이 필요한 것은 HRV-12의 실패 예외 범위다. 검사기 결함으로 재시도하는 것과 검증되지 않은 결과를 다음 단계로 넘기는 것을 구분해 계약에 확정할 필요가 있다.

선택적 구조 개선 제안은 추가하지 않았다. 위 항목은 명시된 요구의 누락, 현재 흐름의 충돌과 확인하지 못한 범위다.
