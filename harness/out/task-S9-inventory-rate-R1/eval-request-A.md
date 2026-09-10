# task-S9-inventory-rate R1 평가자 A 요청문

최초 작성: 2026-09-10
최종 갱신: 2026-09-10 (작업 디렉터리를 o2o-dev로. 이슈 87)

이 파일은 사용자가 Codex의 새 작업에 그대로 붙여 넣는 요청문이다. 근거는 계약 7절 D-3이 가로 확정된 것과 10-4 1절의 전달은 사람이 한다는 규칙이다. A와 B는 각각 다른 새 작업에서 돈다. 한 작업에서 둘 다 하지 않는다.

작업 디렉터리는 C:/Dev/potenup/99_projects/o2o-dev로 연다. 그래야 AGENTS.md와 backend/AGENTS.md가 읽힌다. 상위 폴더 o2o는 세션 여럿이 브랜치를 바꿔 가며 쓰는 곳이라 평가 대상 파일이 없을 수 있다.

열기 전에 `node harness/out/task-S9-inventory-rate-R1/verify-eval-workspace.mjs`를 그 폴더에서 돌려 PASS를 확인한다. FAIL이면 붙여 넣지 않는다.

아래 구분선부터가 붙여 넣을 내용이다.

---

너는 평가자 A다. 설계자도 구현자도 아니다. 이 코드가 만들어진 논의를 보지 못했고 볼 필요도 없다. 주어진 파일만으로 판정한다.

## 1. 대상

| 항목 | 값 |
|---|---|
| Task | task-S9-inventory-rate. 재고와 요금 묶음 |
| 라운드 | R1 |
| 브랜치 | feat/task-s9-inventory-rate |
| 기준 커밋 | 280a69163a6a64225bb414969a134af22d5cefbe |

평가 대상 파일 목록은 `harness/out/task-S9-inventory-rate-R1/eval-target-files.md`에 있다. 프로덕션 43개와 테스트 7개다. 그 파일이 평가 대상이 아니라고 적은 것은 읽지 않는다.

## 2. 읽어도 되는 것

| 자료 | 경로 | 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 코드 목록 | harness/out/task-S9-inventory-rate-R1/eval-target-files.md | sha256:807b6239259b4107 | 전문 |
| 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 작업 계약 | harness/tasks/task-S9-inventory-rate.md | 자기 해시 없음 | 전문 |
| 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 | 축, 심각도, 출력 스키마 |
| 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절, 재고 절, 요금 절, 응답 모델, 검증 기준 T03부터 T07 |
| 06-2 애그리거트 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 1절, 3-1, 5절, 6절 재고와 요금 |
| 06-4 계약 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 0절, 1-1, 1-2 재고와 요금, 1-4 |
| 생성자가 낸 실행 결과 | harness/out/task-S9-inventory-rate-R1/step5, step6, step6-2 | 없음 | JUnit XML과 http-calls.txt |

기록된 해시와 실제 파일의 해시가 다르면 그 사실을 리포트에 적고 실제 파일로 진행한다.

## 3. 읽지 않는 것

01 계획서 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, `harness/docs/` 전체, `harness/state/` 전체, 생성 대화.

`harness/out/task-S9-inventory-rate-R1/step9-verification.md`도 읽지 않는다. 그것은 생성자가 스스로 매긴 판정이다. 그것을 먼저 보면 블라인드가 아니다.

## 4. 네가 볼 축

### 4-1. 구조 축

| 축 | 판정 질문 |
|---|---|
| 경계 위반 | 금지되어야 할 컨텍스트 간 의존이 규칙으로 막히나 |
| Repository 단위 | 애그리거트 루트가 아닌 것에 Repository가 정의됐나 |
| 계약 하강 | 계약표 항목 중 테스트 케이스로 내려가지 않은 것 |
| 레이어 역전 | domain이 infrastructure를 참조하나 |

### 4-2. 코드 전용 축

| 축 | 판정 질문 | 근거 |
|---|---|---|
| API 계약 준수 | 요청과 응답 필드, 상태 코드, 오류 code가 명세와 같은가. 명세에 없는 필드나 경로를 만들었나 | 11 재고 절과 요금 절 |
| 멱등 규칙 | 명세 멱등 절 1부터 9항과 같은가 | 11 공통 멱등 처리 |
| 소유권과 접근 | 행위자가 헤더에서만 오나. body의 hostId로 권한을 정하지 않나. 남의 자원이 404인가 | 11 인증과 접근 제어 |
| 동시성과 트랜잭션 | 잠금 순서, 잠금 후 상태 재검사, 전체 성공 또는 전체 실패가 설계 계약과 같은가. 락 없는 읽기로 분기하는 곳이 있나 | 06-4 0절 |
| 테스트 격리와 재현성 | 시간을 제어하나. 데이터를 정리하나. MySQL에서 봐야 할 것을 메모리 저장소로 대신하지 않았나 | 11 검증 기준 |

### 4-3. 검증 ID 판정

계약 9절이 지정한 것은 T03과 T04와 T05 전부, T06과 T07 부분이다. 각각에 통과, 실패, 미실행 셋 중 하나를 적는다. 실행하지 않은 것을 통과로 적지 않는다.

## 5. 돌려도 되는 명령

| 명령 | 무엇 |
|---|---|
| `docker compose -f backend/docker-compose.yml up -d` | 테스트 DB. 127.0.0.1:3307 |
| `JAVA_HOME=<JDK 21 경로> ./backend/gradlew.bat -p backend test` | 테스트 |
| `node harness/tools/check.mjs g1 <소스> --type code --artifact <junit.xml>` | 게이트 |

바꿔도 되는 것은 빌드 산출물과 로그와 테스트 DB(`o2o_catalog_test`)뿐이다. 소스와 테스트는 어떤 파일도 고치지 않는다.

생성자가 전달한 결과와 네가 직접 돌린 결과를 리포트에서 구분해 적는다. 돌리지 못했으면 돌리지 못했다고 적고 통과로 바꾸지 않는다.

## 6. 출력

`harness/prompts/eval-criteria-code.md` 5절의 스키마를 그대로 쓴다. 판정 요약, 상세, 실행 기록, 검증 ID 결과다. 요구사항 역추적표는 평가자 B의 몫이라 이 리포트에 넣지 않는다.

지적 ID는 `S9-R1-A-{원본 번호 2자리}`로 붙인다. 위치는 파일과 행 번호로 적는다.

리포트에 실제로 읽은 파일과 그 sha256을 남긴다.

## 7. 하지 말 것

- 파일을 만들거나 고치기
- 문체나 표현 지적하기
- 코드에 없는 내용을 추측으로 채우기
- 확인필요 등급을 없애고 통과와 실패 둘로만 판정하기
- 5절에 없는 명령 실행하기

판단이 서지 않으면 확인필요로 남긴다.
