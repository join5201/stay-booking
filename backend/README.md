# backend

O2O 숙박 예약 백엔드. 단일 Spring Boot 모듈에 컨텍스트별 패키지를 나눈다(입력 팩 1절, 01 23행).

최초 작성: 2026-09-08
최종 갱신: 2026-09-13 (6절 결제와 확정 행. 결제 묶음 사용자 최종 승인, PR 133 병합 지시. B2. 그 전 같은 날 6절 결제와 확정 행과 접근 범위 행. 결제 묶음 9단계 검증 표, PR 133. B2. 그 전 2026-09-12 6절을 묶음 7행 진척 표로, 이슈 129)

이 파일은 사람이 읽는 설명서다. 에이전트용 규칙은 따로 있다.

| 파일 | 독자 | 무엇 |
|---|---|---|
| CLAUDE.md | Claude Code와 생성 에이전트 | 읽는 순서, 층 규칙, 테스트 규칙, 금지 |
| AGENTS.md | Codex와 평가 에이전트 | 허용 입력, 허용 명령, 코드 평가 제약 |

작업 계약: `../harness/tasks/` 아래. 진행 중인 것이 가장 최근 파일이다.
계약별 상태는 6절 표에 있다.

## 1. 확정된 판 (2026-09-08 실측)

| 항목 | 값 | 어떻게 확인했나 |
|---|---|---|
| Spring Boot | 4.1.1 | start.spring.io metadata의 기본값 |
| Gradle | 9.7.1 | 뼈대에 들어 있는 wrapper. gradlew --version |
| Java 툴체인 | 21 | build.gradle. 로컬 JDK는 Microsoft 21.0.11 |
| MySQL 이미지 | mysql:9.7.2 | Docker Hub에서 mysql:lts가 가리키던 판 |

Java 17도 로컬에 있으나(Temurin 17.0.18) 툴체인은 21이다. PATH의 java가 17이므로 Gradle을 부를 때 JAVA_HOME을 21로 준다.

## 2. 처음 한 번 하는 준비

backend/env.example을 backend/.env로 복사하고 값을 채운다. .env는 추적하지 않는다. 값은 로컬 테스트용이고 저장소에 올리지 않는다.

```
cp backend/env.example backend/.env
```

| 이름 | 무엇 |
|---|---|
| O2O_MYSQL_ROOT_PASSWORD | 컨테이너 root 비밀번호 |
| O2O_MYSQL_USER | 애플리케이션이 붙을 계정 이름 |
| O2O_MYSQL_PASSWORD | 그 계정의 비밀번호 |

본보기 파일 이름에 앞 점이 없는 이유가 있다. .claude/settings.json의 deny가 Edit(**/.env.*)로 .env로 시작하는 모든 파일을 막는다. 그 규칙을 좁혀 예외를 만들면 나중에 생기는 새 접미사가 안 막힌다. 규칙은 넓게 두고 본보기 파일만 그 패턴 밖으로 뺐다. 이슈 39.

## 3. 실행

```
docker compose -f backend/docker-compose.yml up -d
cd backend && JAVA_HOME=<JDK 21 경로> ./gradlew test
```

실패 시 출력 예시.

```
error during connect: ... dockerDesktopLinuxEngine: The system cannot find the file specified.
```
Docker Desktop이 꺼져 있다. 켜고 다시 실행한다.

```
Communications link failure ... Connection refused: connect
```
컨테이너가 아직 안 떴거나 포트가 다르다. docker compose ps로 상태를 본다.

```
Access denied for user
```
backend/.env의 계정 이름이나 비밀번호가 컨테이너를 처음 만들 때 쓴 값과 다르다. 컨테이너를 내렸다 다시 올린다.

## 4. 포트

| 대상 | 포트 | 이유 |
|---|---|---|
| 테스트 MySQL 컨테이너 | 127.0.0.1:3307 | 3306은 이 기계의 다른 MySQL이 이미 쓰고 있다(2026-09-08 확인) |

## 5. 패키지 구조

com.o2o 아래에 컨텍스트를 나눈다. 만든 묶음의 컨텍스트만 둔다. 아직 안 한 묶음은 빈 폴더도 두지 않는다.

| 패키지 | 근거 | 언제 생겼나 |
|---|---|---|
| catalog | 05-2 1절 숙소 카탈로그 | task-S9-catalog |
| inventory | 05-2 재고와 요금 | task-S9-inventory-rate |
| shared | 06-1 4절. 단일 모듈이라 공유 커널을 shared 패키지로 둔다 | task-S9-catalog |

컨텍스트 안의 층은 domain, application, api, infrastructure 넷이다. 근거는 06-4 1-4 검증 책임 위치 표다. 형식 검증은 컨트롤러, 규칙 검증은 애그리거트와 VO, 컨텍스트를 넘는 선행조건은 앱 서비스, 유일성과 무결성은 DB다.

층마다 `package-info.java`가 있고 거기에 그 층이 하는 일과 근거 절이 적혀 있다. 파일을 새로 만들기 전에 그 층의 `package-info.java`를 읽는다.

## 6. 지금 어디까지 왔나 (2026-09-11 묶음 표 신설. 2026-09-12 main 기준으로 갱신. 2026-09-13 결제 행과 접근 범위 행)

구현 묶음의 정본은 `../harness/docs/10-6` 3절의 기능별 API와 검증 연결 표다. 한 묶음이 백엔드와 테스트와 검증까지 내려간 다음에 다음 묶음으로 간다(루트 CLAUDE.md N9).

묶음 단위로만 적는다. 단위 40은 API 33, 내부 처리 4, 접근 범위 3이고 검증 시나리오는 T01부터 T30이다. 끝은 코드와 테스트가 main에 있는 단위 수다.

| 묶음 | 단위 | 끝 | 남음 | 검증 시나리오 | 상태 | 근거 |
|---|---|---|---|---|---|---|
| 숙소와 객실 | CAT-01~09 (9) | 9 | 0 | T01 부분(재고와 요금 구간 남음), T02 통과 | 최종 승인 | progress.md 2026-09-09 16:55 |
| 재고와 요금 | INV-01~05, RATE-01~04 (9) | 9 | 0 | T03, T04, T05, T06, T07 통과. T04와 T05의 잠금 몫과 T06, T07의 예약 몫은 예약 묶음이 닫음 | main 병합. Codex 2라운드는 MVP 뒤 | PR 115. progress.md 2026-09-11 16:33 |
| 프로모션과 검색 | PROMO-01~05, SEARCH-01~03 (8) | 8 | 0 | T27, T28 통과. T12, T13 부분(계약이 처음부터 부분) | 최종 승인 | PR 98. progress.md 2026-09-12 04:41 |
| 예약과 선점 | BOOK-01~03, HoldInventory (4) | 4 | 0 | T06~T11 통과. T12, T13 부분(요금 절반만 이 묶음 몫) | main 병합. 1차 done 행. 2차(결제 중계, 확정, 만료, 취소)는 결제 PR 뒤 | PR 127. progress.md 2026-09-12 04:37 |
| 결제와 확정 | PAY-01~02, INTERNAL-01, ConfirmBooking (4) | 0 | 4 | T21, T26 통과(브랜치). T15~T17, T20, T22, T23 부분(결제 몫만 이 묶음). T14, T18, T19는 예약 2차 | 사용자 최종 승인(2026-09-13 병합해라). 개정 다섯 확정. INTERNAL-01과 내부 처리는 브랜치에 있고 PR 133 병합은 이 행 뒤. PAY-01~02와 ConfirmBooking은 예약 2차 | PR 133. 그 브랜치의 progress.md 2026-09-13 20:16 |
| 취소와 만료 | BOOK-04, ExpireBooking, RefundPayment (3) | 0 | 3 | T17~T25 | 미착수. 예약 2차 몫. 결제 PR 병합 뒤 | 없음 |
| 접근 범위 | 로컬 행위자, 소유권, Mock 경로 (3) | 2 | 1 | T02 통과, T30 통과(브랜치) | 행위자와 소유권은 숙소 묶음에서 끝. Mock 경로는 결제 PR 133의 9단계 검증 표. main 병합 뒤 끝 3 | shared의 Actor 여섯 파일, catalog의 hostId 대조. T30은 PR 133 |
| 합계 | 40 | 32 | 8 | 30 중 통과 12, 부분 3, 미착수 15 | 전체 대비 남음 20% | main a22041c 기준 |

기능 수로는 40 중 32이지만 남은 여덟이 결제와 만료다. 동시 확정, 멱등 결제, 만료 경합 같은 어려운 시나리오(T14부터 T26)가 거기 몰려 있어 남은 일은 20%보다 많다.

이 표는 묶음 상태를 바꾸는 사건 넷에서만 고친다. 계약 승인, 코드의 main 병합, 9단계 검증 표, 사용자 최종 승인이다. 그 사건의 행을 `../harness/state/progress.md`에 더하는 커밋이 같은 커밋에서 이 표의 그 묶음 행을 고치고 근거 칸에 그 행의 날짜시각이나 PR 번호를 적는다(`.claude/rules/record.md` B2). 따로 고치면 낡는다. 이 파일은 union이 아니라 두 세션이 같은 표를 고치면 충돌이 나고 5단계에서 손으로 합친다.

단계별 상태는 여기 적지 않는다. 두 곳을 본다.

| 무엇 | 어디 |
|---|---|
| 이 묶음의 단계 표와 완료 조건 | 진행 중인 계약의 8절 |
| 어디까지 했나 | `../harness/state/progress.md`의 그 Task 마지막 행 |
