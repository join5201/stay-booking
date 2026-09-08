## 무엇을 바꿨나

한 줄. 무엇을 했는지가 아니라 무엇이 달라졌는지 쓴다.

Closes #

## 문제와 원인과 해결

| 항목 | 내용 |
|---|---|
| 문제 | 무엇이 깨져 있었나 |
| 원인 | 어느 파일 어느 줄이 그렇게 만들었나 |
| 해결 | 무엇으로 막았나 |

## 검사

| 명령 | 결과 |
|---|---|
| `node --test harness/tools/tests/check.test.mjs` | |
| `node harness/tools/check.mjs g1 <대상> --type doc` | |
| `node harness/tools/check.mjs g2 <결정표> --mode pre` | |

돌리지 않은 검사는 돌리지 않았다고 적는다. 결과 파일이 있다는 사실을 성공으로 적지 않는다.

## 반영하지 않은 것

범위 밖으로 둔 것과 그 이유. 없으면 없다고 쓴다.

## 기록

| 파일 | 무엇을 남겼나 |
|---|---|
| harness/state/progress.md | |
| harness/state/troubleshooting.md | |
| harness/state/knowledge.md | |

## 확인

- [ ] 커밋을 최소 단위로 나눴다
- [ ] 문체 규칙을 지켰다. 제목 밖 볼드, 긴 줄표, 가운뎃점 없음
- [ ] 승인하지 않은 파일을 건드리지 않았다
- [ ] done은 사용자 승인 행에서만 썼다
