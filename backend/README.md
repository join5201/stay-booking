# backend

O2O 숙박 예약 백엔드. 단일 Spring Boot 모듈에 컨텍스트별 패키지를 나눈다(입력 팩 1절, 01 23행).

최초 작성: 2026-09-08
최종 갱신: 2026-09-08

작업 계약: ../harness/tasks/task-S9-catalog.md
3단계 계획: ../harness/out/task-S9-catalog-R1/plan-step3.md

## 1. 확정된 판 (2026-09-08 실측)

| 항목 | 값 | 어떻게 확인했나 |
|---|---|---|
| Spring Boot | 4.1.1 | start.spring.io metadata의 기본값 |
| Gradle | 9.7.1 | 뼈대에 들어 있는 wrapper. gradlew --version |
| Java 툴체인 | 21 | build.gradle. 로컬 JDK는 Microsoft 21.0.11 |
| MySQL 이미지 | mysql:9.7.2 | Docker Hub에서 mysql:lts가 가리키던 판 |

Java 17도 로컬에 있으나(Temurin 17.0.18) 툴체인은 21이다. PATH의 java가 17이므로 Gradle을 부를 때 JAVA_HOME을 21로 준다.

## 2. 처음 한 번 하는 준비

환경변수 셋을 backend/.env에 적는다. 이 파일은 추적하지 않는다. 값은 로컬 테스트용이고 저장소에 올리지 않는다.

| 이름 | 무엇 |
|---|---|
| O2O_MYSQL_ROOT_PASSWORD | 컨테이너 root 비밀번호 |
| O2O_MYSQL_USER | 애플리케이션이 붙을 계정 이름 |
| O2O_MYSQL_PASSWORD | 그 계정의 비밀번호 |

파일 형식은 한 줄에 이름=값이다.

이 저장소에 본보기 파일을 두지 않는 이유가 있다. .claude/settings.json의 deny 규칙이 Edit(**/.env.*)로 .env.example까지 막는다. 비밀 값이 섞여 나가는 것을 막는 규칙이라 우회하지 않고 이름만 여기 적는다.

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

com.o2o 아래에 컨텍스트를 나눈다. 이번 Task는 catalog와 shared 둘만 만든다. 나머지 넷은 범위 밖이라 빈 폴더도 두지 않는다.

| 패키지 | 근거 |
|---|---|
| catalog | 05-2 1절 숙소 카탈로그 |
| shared | 06-1 4절. 단일 모듈이라 공유 커널을 shared 패키지로 둔다 |

컨텍스트 안의 층은 domain, application, api, infrastructure 넷이다. 근거는 06-4 1-4 검증 책임 위치 표다. 형식 검증은 컨트롤러, 규칙 검증은 애그리거트와 VO, 컨텍스트를 넘는 선행조건은 앱 서비스, 유일성과 무결성은 DB다.
