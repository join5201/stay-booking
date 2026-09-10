# backend

O2O 숙박 예약 백엔드. 단일 Spring Boot 모듈에 컨텍스트별 패키지를 나눈다(입력 팩 1절, 01 23행).

최초 작성: 2026-09-08
최종 갱신: 2026-09-10 (5절에 inventory 추가. 6절 신설)

이 파일은 사람이 읽는 설명서다. 에이전트용 규칙은 따로 있다.

| 파일 | 독자 | 무엇 |
|---|---|---|
| CLAUDE.md | Claude Code와 생성 에이전트 | 읽는 순서, 층 규칙, 테스트 규칙, 금지 |
| AGENTS.md | Codex와 평가 에이전트 | 허용 입력, 허용 명령, 코드 평가 제약 |

작업 계약: `../harness/tasks/` 아래. 진행 중인 것이 가장 최근 파일이다.
지금까지의 계약: task-S9-catalog.md(완료), task-S9-inventory-rate.md(진행 중)

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

## 6. 지금 어디까지 왔나

구현 묶음의 정본은 `../harness/docs/10-6` 3절의 기능별 API와 검증 연결 표다. 한 묶음이 백엔드와 테스트와 검증까지 내려간 다음에 다음 묶음으로 간다(루트 CLAUDE.md N9).

단계별 진행 상태는 이 파일에 적지 않는다. 적으면 낡는다. 두 곳을 본다.

| 무엇 | 어디 |
|---|---|
| 이 묶음의 단계 표와 완료 조건 | 진행 중인 계약의 8절 |
| 어디까지 했나 | `../harness/state/progress.md`의 그 Task 마지막 행 |
