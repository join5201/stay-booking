# CodeRabbit 적용

최초 작성: 2026-09-10
최종 갱신: 2026-09-10 (4절. 제외를 폴더 단위에서 파일 단위로. 이슈 100)

왜 이 문서가 필요한가: 설정 파일만 두면 왜 그 값인지가 남지 않는다. 왜 markdownlint를 껐는지, 왜 harness/state/를 리뷰에서 뺐는지, 그리고 모델이 못 하고 사람이 해야 하는 한 단계가 무엇인지가 여기 있다.

## 1. 무엇이 없었나

PR에 자동 리뷰가 붙지 않았다. 저장소에 .coderabbit.yaml도 .github/workflows/도 없었고 리뷰 봇도 설치되지 않았다.

하네스의 평가 축은 Codex A와 B 둘인데 둘 다 사람이 손으로 돌린다. 설계 문서 한 바퀴에는 그 두 축이 붙지만, PR 단위로는 아무것도 안 붙었다. 이슈 60이 그것이다.

## 2. CodeRabbit이 N1에 걸리지 않는 이유

먼저 짚을 것이 있다. CLAUDE.md 5절의 N1은 별도 LLM API 호출과 앱 간 자동 연결을 금한다. 근거는 10-4 1절의 비용 조건과 실행 방식이다.

| 항목 | N1이 금한 것 | CodeRabbit |
|---|---|---|
| 어디서 도나 | 하네스 안. 생성과 평가 사이 | GitHub. PR이 열린 뒤 |
| 무엇을 부르나 | 하네스가 LLM API를 직접 | 하네스가 아무것도 부르지 않는다 |
| 앱을 잇나 | Claude Code와 Codex를 자동으로 잇는 것 | 두 앱을 잇지 않는다 |

CodeRabbit은 하네스 바깥이다. 생성은 여전히 Claude Code, 평가는 여전히 Codex의 새 작업, 전달은 여전히 사람이다. PR 리뷰는 그 한 바퀴가 끝난 뒤에 붙는 별개의 그물이다.

다만 비용은 걸린다. 6절이 그 이야기다.

## 3. 기본값과 다르게 정한 것

설정 파일에는 기본값과 같은 항목을 적지 않았다. 기본값을 그대로 다시 적으면 무엇이 판단이고 무엇이 기본인지 구별할 수 없다. 아래가 판단한 항목 전부다.

| 키 | 기본값 | 이 저장소 | 왜 |
|---|---|---|---|
| language | en-US | ko-KR | 리뷰 코멘트를 한국어로 낸다(W1) |
| tone_instructions | 빈 문자열 | 문체 규칙 요약 103자 | 봇의 코멘트도 W2와 W3을 지키게 한다 |
| reviews.high_level_summary_in_walkthrough | false | true | false면 봇이 PR 본문을 고쳐 요약을 넣는다. PR 본문은 템플릿의 자리다 |
| reviews.suggested_reviewers | true | false | 1인 저장소다. 제안할 리뷰어가 없다 |
| reviews.in_progress_fortune | true | false | 운세 문구는 장식이다(W6) |
| reviews.path_filters | 빈 배열 | 제외 10개 | 4절 |
| reviews.labeling_instructions | 빈 배열 | 라벨 6개 | 비워 두면 봇이 과거 PR만 보고 짐작한다 |
| reviews.path_instructions | 빈 배열 | 경로 5개 | 5절 |
| reviews.tools.markdownlint | 켬 | 끔 | 서식 권위가 둘이면 어느 쪽을 어겼는지 못 가린다. 문서 서식은 CLAUDE.md 2절과 check.mjs g1이 잰다 |
| reviews.tools.languagetool | 켬 | 끔 | 한국어 본문에 영어 문법 검사를 걸면 소음만 나온다 |
| chat.art | 켬 | 끔 | 아스키 아트는 장식이다 |
| knowledge_base.learnings.scope | auto | local | auto는 공개에서 local, 비공개에서 global로 풀린다. 공개 여부가 바뀌어도 학습이 조직 전체로 새지 않게 못박는다 |
| knowledge_base.code_guidelines.filePatterns | 기본 목록 | 5개 지정 | 기본 목록도 CLAUDE.md와 AGENTS.md를 읽지만 하네스의 평가 기준 파일은 안 읽는다 |

profile은 기본값 chill 그대로지만 파일에 적었다. 소음이 많으면 quiet로, 놓치는 것이 많으면 assertive로 바꾸는 손잡이라 눈에 보이는 자리에 두는 편이 낫다.

filePatterns에 주의할 점이 하나 있다. 배열을 적으면 기본 목록을 더하는 것이 아니라 대체한다. 그래서 기본 목록에 이미 있는 CLAUDE.md와 AGENTS.md를 설정 파일에 다시 적었다. 빼면 그 둘을 안 읽는다.

## 4. 리뷰에서 뺀 자리 (2026-09-10 폴더 단위에서 파일 단위로. 이슈 100)

path_filters의 느낌표는 제외다. 뺀 이유는 넷으로 갈린다.

| 무엇 | 왜 뺐나 |
|---|---|
| harness/project-sync/ 전체 | 원본 사본이다. 모델도 못 고치는 자리라 지적해도 반영할 데가 없다(CLAUDE.md 3-1) |
| harness/reviews/의 task와 harness로 시작하는 md | 평가자 리포트 원문이다. 원문 그대로 두는 것이 규칙이다 |
| harness/state/의 기록 파일 셋 | 추가 전용 기록이다. 같은 형식 지적이 매 PR마다 되풀이된다 |
| harness/out/ 하위 폴더 안쪽 | 생성 후보와 형식 보정 이력이다. 반영판은 document/로 가므로 거기서 본다 |
| backend/gradle/wrapper/, gradlew, gradlew.bat | 빌드 도구가 만든 파일이다 |
| pdf, png, jpg | 바이너리다 |

처음에는 셋을 폴더 통째로 막았다. 그러면 그 안의 README.md까지 같이 빠진다. 이 저장소는 README를 어디에 있든 색인으로 대우하기로 했고 검사기에 index 성격도 넣었는데(이슈 74) 리뷰 설정만 그 결정을 안 따르고 있었다. 2026-09-10 PR 77에서 봇이 그것을 지적했다. 색인 셋을 못 봐서 이슈 74의 완료 여부를 확인할 수 없다는 것이었다.

고칠 때 지킨 원칙이 하나 있다. 규칙을 전부 느낌표로만 쓴다.

이 패턴은 봇이 저장소를 받아올 때도 쓰인다. 제외한 자리는 리뷰에서 빠지는 것이 아니라 아예 받지 않는다. 느낌표 없는 포함 패턴을 하나라도 섞으면 그것만 받고 나머지를 다 버릴 위험이 있다. 그러면 봇이 절반만 보고 통과를 낸다. 실패가 아니라 조용한 오답이다.

그래서 제외한 뒤 README만 다시 포함하는 방법을 쓰지 않았다. 대신 제외 패턴 자체를 좁혀 README에 안 걸리게 했다. 규칙이 전부 느낌표면 순서가 결과를 바꾸지 않으므로 어느 파일이 막히는지가 패턴 하나하나로 결정된다.

문서가 minimatch를 쓴다고 적어서 같은 라이브러리로 실측했다. 저장소 파일 362개 전체에 옛 규칙과 새 규칙을 각각 걸어 차이를 봤다.

| 항목 | 값 |
|---|---|
| 옛 규칙이 막던 파일 | 124개 |
| 새 규칙이 막는 파일 | 121개 |
| 풀린 파일 | 3개. harness/out과 harness/reviews와 harness/state의 README.md |
| 새로 막힌 파일 | 0개 |

재현 방법은 6-1절에 있다.

## 5. 경로별로 무엇을 보게 했나

path_instructions는 경로와 지시문의 짝이다. 다섯 개를 넣었다.

| 경로 | 무엇을 보나 |
|---|---|
| 모든 md | W1, W2, W3, W4, W6, D3, D4. 지적할 때 조항 번호를 함께 적게 했다 |
| harness의 md | 파일명 규약과 지적 ID 규약. 문서 번호 충돌. done을 아무나 쓰는 것 |
| harness/tools의 mjs | D5 머리 주석. 절대경로 사용. 판단 못 하는 입력에 통과를 내는 것 |
| backend 본 코드 | 검증 책임 위치. domain의 참조 금지. 명세에 없는 경로. 근거 없는 규칙 |
| backend 테스트 | 계약의 테스트 ID 연결. 빠진 실패 경로 |

md 지시문에는 영어로 고치라는 지적과 마크다운 서식 취향 지적을 하지 말라는 줄을 넣었다. 이것을 안 적으면 한국어 본문 자체를 문제로 삼는 코멘트가 나온다.

## 6. 검증

설정 파일이 공식 스키마를 통과하는지 재는 방법이다. 저장소에 검사기를 새로 넣지 않았다. 이 파일 하나뿐이라 검사기 하나를 유지할 값이 없다.

준비. 저장소 밖 임시 폴더에서 한다.

    npm init -y
    npm i --no-save js-yaml ajv ajv-formats
    curl -sSL -o schema.json https://storage.googleapis.com/coderabbit_public_assets/schema.v2.json

검사기. 같은 폴더에 validate.mjs로 둔다.

    import fs from "node:fs";
    import { createRequire } from "node:module";
    const require = createRequire(import.meta.url);
    const yaml = require("js-yaml");
    const Ajv = require("ajv/dist/2020");   // 스키마가 draft 2020-12다. 기본 빌드는 못 읽는다
    const addFormats = require("ajv-formats");

    const target = process.argv[2];
    const schema = JSON.parse(fs.readFileSync("./schema.json", "utf8"));
    const data = yaml.load(fs.readFileSync(target, "utf8"));
    const ajv = new Ajv({ allErrors: true, strict: false });
    addFormats(ajv);
    if (ajv.validate(schema, data)) { console.log("PASS", target); process.exit(0); }
    for (const e of ajv.errors) console.log("FAIL", e.instancePath || "/", e.message, JSON.stringify(e.params));
    process.exit(1);

실행 예시

    node validate.mjs <저장소>/.coderabbit.yaml

통과 시 출력 예시

    PASS C:/.../.coderabbit.yaml

실패 시 출력 예시. profile을 strict로 바꿔 실제로 낸 출력이다.

    FAIL /reviews/profile must be equal to one of the allowed values {"allowedValues":["quiet","chill","assertive"]}

2026-09-10 실측 결과는 PASS다. 스키마 최상위가 additionalProperties false라 키 이름을 틀리면 여기서 걸린다.

### 6-1. 제외 패턴이 무엇을 막는지 재는 법 (2026-09-10 신설)

스키마 검증은 키 이름만 본다. 제외 패턴이 실제로 어느 파일을 막는지는 안 본다. 그것은 따로 잰다. 공식 문서가 path_filters에 minimatch를 쓴다고 적으므로 같은 라이브러리로 돌린다.

준비. 저장소 밖 임시 폴더에서 한다.

    npm init -y
    npm i --no-save minimatch
    git ls-tree -r --name-only origin/main > files.txt

검사기. 같은 폴더에 blocked.mjs로 둔다.

    import fs from "node:fs";
    import { minimatch } from "minimatch";
    const files = fs.readFileSync("./files.txt", "utf8").trim().split("
");
    const pats = process.argv.slice(2);   // 느낌표를 뗀 패턴을 넘긴다
    const hit = files.filter(f => pats.some(p => minimatch(f, p)));
    console.log("막히는 파일", hit.length, "개");
    for (const f of hit) console.log("   ", f);

실행 예시

    node blocked.mjs 'harness/out/*/**' 'harness/state/progress.md'

실패 시 출력 예시. 막으려던 파일이 목록에 없거나 살리려던 파일이 목록에 있으면 그것이 실패다.

    막히는 파일 113 개
        harness/out/README.md      <- 살리려던 파일이 막혔다

2026-09-10 실측이다. 옛 규칙 셋과 새 규칙 여섯을 저장소 파일 362개에 각각 걸었다.

| 항목 | 값 |
|---|---|
| 옛 규칙이 막던 파일 | 124개 |
| 새 규칙이 막는 파일 | 121개 |
| 풀린 파일 | README.md 셋 |
| 새로 막힌 파일 | 0개 |

이 검사는 패턴이 전부 느낌표일 때만 뜻이 있다. 포함 패턴을 섞으면 어느 것이 이기는지가 봇의 구현에 달렸고 그것은 문서에 없다. 규칙을 전부 느낌표로 두는 이유가 그것이다.

## 7. 사람이 해야 하는 것

설정 파일만으로는 아무 일도 안 일어난다. GitHub App 설치가 남는다. 이것은 GitHub 권한 승인이라 모델이 대신 못 한다.

| 순서 | 무엇 | 어디서 |
|---|---|---|
| 1 | CodeRabbit에 GitHub 계정으로 로그인한다 | https://app.coderabbit.ai/login |
| 2 | 조직 또는 개인 계정을 고른다 | 같은 화면 |
| 3 | 저장소 접근 범위를 고른다. join5201/stay-booking 하나만 고르면 된다 | GitHub 설치 화면 |
| 4 | 권한을 확인하고 승인한다 | 같은 화면 |

CodeRabbit이 요구하는 권한이다.

| 범위 | 무엇 |
|---|---|
| 읽기 | actions, discussions, members, metadata, 머지 큐 |
| 읽기 쓰기 | checks, code, commit statuses, issues, pull requests |

checks에 쓰기가 필요한 이유는 리뷰 상태를 PR의 체크로 올리기 때문이다. code에 쓰기가 필요한 이유는 봇이 제안한 수정을 커밋으로 만들 수 있기 때문이다. 이 저장소는 그 기능을 켜지 않았지만 권한 자체는 설치할 때 함께 준다.

## 8. 요금제가 가져오는 제약 (2026-09-10 공개 전환 반영)

이 저장소는 2026-09-10 공개로 바뀌었다. 그것이 무엇을 받을 수 있는지를 가른다.

| 요금제 | PR에 붙는 것 |
|---|---|
| Free | PR 요약만이다. 공개 저장소에서도 같다. 줄 단위 리뷰는 VS Code 확장과 CLI로만 쓴다 |
| OSS | 공개 저장소가 받는 자리다. 유료 구독 없이 Team 기능을 준다 |
| 체험 | 새 조직에 14일 Advanced 체험이 붙는다 |
| Essentials | 개발자당 월 24달러부터 |
| Team | 위에 더해 분류와 계획과 테스트 생성 |
| Advanced | 위에 더해 상시 보안 리뷰 |

주의할 것이 하나 있다. 공개로 바꾼다고 Free 요금제가 좋아지는 것이 아니다. Free는 공개든 비공개든 요약만 준다. 줄 단위 리뷰를 공짜로 받는 자리는 Free가 아니라 OSS다. 이름이 비슷해 헷갈리는 지점이다.

OSS의 한도는 고정값이 아니라 저장소의 별 개수와 커뮤니티 규모를 따라간다.

| 항목 | 범위 | 이 저장소의 현재 자리 |
|---|---|---|
| PR 리뷰 | 시간당 1건에서 10건 | 별 0개다. 아래쪽 끝인 시간당 1건으로 본다 |
| 리뷰당 파일 수 | 100개에서 300개 | 같은 이유로 100개로 본다 |
| 챗 메시지 | 시간당 25건 | 같음 |
| 확장과 CLI 리뷰 | 각각 시간당 1건 | 같음 |

시간당 1건이 이 저장소에 실제로 걸린다. 세션이 여럿이라 열린 PR이 2026-09-10 기준 여섯이다. 여섯을 한 번에 열면 리뷰가 여섯 시간에 걸쳐 하나씩 온다. 급한 PR이 있으면 그것부터 열고 나머지를 미루는 편이 낫다.

공식 문서가 적지 않은 것이 하나 있다. OSS 자격이 공개 저장소에 자동으로 붙는지 신청이 필요한지다. 요금 안내 쪽은 공개 저장소에 설치하면 리뷰가 무료라고만 적는다. 설치한 뒤 실제로 무엇이 오는지로 확인한다.

설정 파일은 어느 쪽이든 그대로 맞는다. 요금제가 바뀐다고 고칠 항목이 없다. learnings.scope도 마찬가지다. 기본값 auto가 공개 저장소에서 local로 풀리므로 우리가 적어 둔 local과 결과가 같다.

## 9. 이 설정이 못 하는 것

| 못 하는 것 | 왜 |
|---|---|
| Codex A와 B를 대체 | 평가 축과 출력 스키마가 다르다. eval-criteria 두 파일이 정본이고 봇은 그것을 읽는 참고자일 뿐이다 |
| check.mjs 게이트를 대체 | 봇의 지적은 코멘트다. 통과와 실패를 내는 기계가 아니다 |
| 설계 결정의 옳고 그름 판정 | 봇은 설계 문서를 읽지만 미결 11개가 무엇인지 모른다(N4) |
| 문서 서식 판정 | markdownlint를 껐다. 서식은 check.mjs g1이 잰다 |

봇의 지적을 결정표에 넣지 않는다. 결정표의 입력은 Codex 리포트다. 봇의 지적은 PR 코멘트로 끝나고, 고칠 값이 있으면 이슈를 새로 연다.

## 확인한 출처

전부 2026-09-10 확인이다.

| 내용 | URL |
|---|---|
| 설정 스키마 v2 | https://coderabbit.ai/integrations/schema.v2.json |
| 설정 참고 문서 | https://docs.coderabbit.ai/reference/yaml-template |
| GitHub 설치 절차와 권한 | https://docs.coderabbit.ai/platforms/github-com |
| 요금제별 포함 범위 | https://docs.coderabbit.ai/management/plans |

참고 문서가 스키마 주소로 적는 https://coderabbit.ai/integrations/schema.json은 404다. 실제로 응답하는 주소는 위 표의 v2다. 원본 스키마는 https://storage.googleapis.com/coderabbit_public_assets/schema.v2.json 에도 같은 내용으로 올라와 있고 6절의 검사기는 그쪽을 받는다.
