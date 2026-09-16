# backend 규칙: 명령

최초 작성: 2026-09-10 (backend/CLAUDE.md의 절로)
최종 갱신: 2026-09-11 (backend/.claude/rules/로 옮김. 이슈 116. 절 번호는 인용이 깨지지 않게 그대로 둔다)

경로는 저장소 루트 기준이다. 루트 CLAUDE.md와 backend/CLAUDE.md가 먼저 적용되고 이 파일은 그 위에 얹힌다.

## 6. 명령

`JAVA_HOME`을 21로 준다. PATH의 java는 17이다.

| 하는 일 | 명령 |
|---|---|
| DB 올리기 | `docker compose -f backend/docker-compose.yml up -d` |
| 테스트 | `JAVA_HOME=<JDK 21> ./backend/gradlew.bat -p backend test` |
| 컴파일만 | `JAVA_HOME=<JDK 21> ./backend/gradlew.bat -p backend compileJava` |
| 서버 띄우기 | `JAVA_HOME=<JDK 21> ./backend/gradlew.bat -p backend bootRun` |
| 게이트 | `node harness/tools/check.mjs g1 <소스> --type code --artifact <junit.xml>` |

PowerShell에서는 `&&`가 안 된다. `;` 로 잇거나 `; if ($?) { }` 를 쓴다.

Docker Desktop이 꺼져 있으면 `docker desktop start`로 켠다. 엔진이 올라오는 데 몇 분 걸린다. 나머지 실패 예시는 README.md 3절에 있다.

테스트 결과 사본은 `harness/out/task-S9-{기능키}-R1/step{단계}/`에 둔다. 실제 서버에 친 기록은 같은 폴더의 `http-calls.txt`다.

