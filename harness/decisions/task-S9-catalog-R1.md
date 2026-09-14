# 사용자 결정표 task-S9-catalog R1

양식: harness/prompts/decision-table.md v4 (2026-09-08)
최초 작성: 2026-09-14
최종 갱신: 2026-09-14 (반영 뒤 인계 표 기입. 수용 8건 반영, 반박 2건 무변경, 남은 치명 없음. 그 전 같은 날 결정 10건 확정. 사용자 발언은 초안대로 10건 확정해라. 반영 진행해라)

왜 이 파일이 필요한가: 리포트를 눈으로 읽고 반영하면 자기 결정에 불리한 지적이 조용히 빠진다(HR4). 원본 행을 하나씩 옮겨 두면 g2가 ID 집합과 심각도를 원본과 대조할 수 있다.

이 표의 행은 원본 리포트의 상세 표와 보조 표에서 셀 단위로 스크립트가 뽑아 만들었다(원문 지적, 근거, 수정 제안은 글자 그대로). 결정과 이유 열은 개발 세션이 초안을 적었고 사용자가 2026-09-14 초안대로 확정했다. 그 근거는 지적마다 실제 파일을 열어 대조한 것이다. 위치를 이유에 적었다. 라운드 mvp-eval-2026-09-14의 쌍 1이다(harness/out/mvp-eval-2026-09-14/README.md).

## 대상과 원본

| 항목 | 기록 |
|---|---|
| Step | 9 |
| 작업 유형 | 코드 |
| 평가 라운드 | R1 |
| 평가 대상 절대경로와 파일 목록 | harness/out/task-S9-catalog-R1/eval-target-files.md |
| 평가 대상 버전 또는 해시 | sha256:c019bc515f6c68e2. 목록이 가리키는 프로덕션 52개와 테스트 6개의 기준 커밋은 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1(PR 138 병합, main)이고 파일별 sha256은 A와 B 리포트의 읽은 파일 표에 있다. 평가 브랜치 eval/mvp-2026-09-14 |
| 승인된 작업 계약 절대경로와 버전 | harness/tasks/task-S9-catalog.md sha256:b35eca9a0afe6e15. 2026-09-14 개정 4(R1 반영을 받은 판) 뒤의 해시다. 평가와 결정 시점의 판은 앞 16자리가 7d6cc521e6df67e6이고 그 판과의 차이는 개정 4 한 번뿐이다(계약 0-5절) |
| A 원본 리포트 절대경로와 버전 또는 해시 | harness/reviews/task-S9-catalog-R1-A.md sha256:b14f84a47634d661 |
| B 원본 리포트 절대경로와 버전 또는 해시 | harness/reviews/task-S9-catalog-R1-B.md sha256:aaf8326e6c52ca71 |
| A 원본 지적 수 | 4 |
| B 원본 지적 수 | 6 |
| 결정표 전체 행 수 | 10 |
| 판단한 사용자 | join5201. 2026-09-14 초안 10건을 그대로 확정했다. 사용자 발언은 초안대로 10건 확정해라. 반영 진행해라. 요청문은 eval-request-A.md sha256:534e2ecc433a07eb와 eval-request-B.md sha256:3474906a80a69ee2 |
| 결정 날짜 | 2026-09-14 |

A는 치명 1, 보통 2, 확인필요 1이다. B는 치명 1, 보통 3, 확인필요 2다. 합쳐서 치명 2, 보통 5, 확인필요 3이다. 결정은 수용 8, 반박 2, 거부 0이다. 2026-09-14 사용자가 초안대로 확정했다.

치명 둘은 같은 문제다. A-01과 B-01 모두 version 대조가 메모리에서만 일어나 같은 version의 동시 수정을 막지 못한다는 지적이다. 원본 행은 둘 다 남긴다. A-02와 B-02(등록 지역 코드 확인 없음), A-03과 B-03(문자열 공백 정규화와 null)도 같은 자리를 짚었다.

### 평가 절차 기록 (2026-09-14)

라운드 계획의 D-2 가는 쌍마다 A와 B를 새 작업(세션)으로 여는 것이었다. 사용자는 Codex 한 세션에 붙여 넣기 순서 표와 harness/out/task-S9-catalog-R1/ 경로를 주고 각각 따로 피드백해 저장하라고 했다(사용자 보고 2026-09-14 14:3x). 그래서 A와 B는 같은 세션의 산출물이고 서로 독립이 아니다. 파일 시각은 B가 14:45, A가 14:50이다. A 리포트 제목이 재평가라고 적혔지만 이 라운드는 R1이고 지적 ID도 R1이다. 결정표에서 이 사정을 다음처럼 다뤘다.

| 확인 | 결과 |
|---|---|
| 파일 | 요청문이 정한 두 경로에 저장됐고 다른 파일은 만들지 않았다(git status 신규 둘) |
| 보조 표 | A 4행, B 6행. 판정 요약의 등급별 수와 같다. g2 report-schema 통과 |
| 읽은 파일 | 둘 다 요청문 2절의 허용 입력 여덟과 대상 58개와 실행 결과 XML만 적었다. step9-verification.md, 라운드 README, 다른 쌍 요청문, 지난 리포트와 결정표는 목록에 없고 리포트도 읽지 않았다고 적었다. A는 backend/build/test-results의 XML도 읽었는데 그것은 5절 명령의 산출물이다 |
| B의 독립 | B는 A의 지적 ID나 문장을 인용하지 않았고 먼저 작성됐다. 다만 같은 세션이라 A-01부터 03과 B-01부터 03이 같은 자리인 것을 독립 확인 둘로 세지 않는다. 개발 세션이 열 건 전부 코드를 직접 대조했다 |
| A의 실행 | Gradle이 UP-TO-DATE라 테스트 본문이 새로 돌지 않았다고 A가 스스로 적었다. 검증 ID 결과는 기준 커밋의 XML(예약 2차 9단계, 48파일 419건)과 코드 대조에 기댄다. 코드가 기준 커밋과 같으므로(verify-eval-workspace.mjs의 ws.target-drift PASS, 14:23) 그 XML은 지금 코드의 결과다 |

재평가(R2)를 쓰지 않는다. 열 건이 전부 코드로 확인되거나 반박되어 리포트의 쓸모가 세션 구성으로 줄지 않았다. 남은 네 쌍은 D-2 가대로 새 작업 여덟으로 돈다.

## 지적별 결정

지적 ID 규약: S{Step}-R{라운드}-{A 또는 B}-{원본 번호 2자리}.

| 리포트 A/B | 원본 번호 | 지적 ID | 심각도 | 대상 위치 | 위반 축 | 위반 기준 | 원문 지적 | 원문 근거 | 수정 제안 | 결정 | 이유 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| A | 01 | S9-R1-A-01 | 치명 | `backend/src/main/java/com/o2o/catalog/domain/Property.java:56` | 동시성과 트랜잭션, 계약 하강 | 동시성과 트랜잭션, 계약 하강 | `version`이 일반 컬럼이고 수정 시 조회한 엔티티의 숫자만 비교한다. 동시 요청 둘이 같은 version을 읽으면 둘 다 통과하고 뒤의 저장이 앞의 값을 덮을 수 있다. CAT-02와 CAT-07의 낙관적 잠금과 409 계약이 실제 경합에서 보장되지 않는다. | `Property.update`와 `RoomType.update`는 메모리 값 비교 뒤 version을 직접 올리지만 `@Version`이나 version 조건부 UPDATE가 없다. C4 테스트 `CatalogUpdateAndListApiTest:92`는 요청 version 99인 순차 실패만 확인한다. | JPA `@Version` 또는 `where id = ? and version = ?` 조건부 갱신으로 DB 커밋 시점 충돌을 검출하고 409로 번역한다. Property와 RoomType 각각 같은 version의 동시 PATCH 테스트를 실제 MySQL에 추가한다. | 수용 | 대조 결과 사실이다. Property.java 56행의 version은 @Column뿐이고 update(100행부터)는 메모리의 this.version과 요청 값을 대조한 뒤 1을 더한다. catalog 아래에 잠금이나 @Version이나 조건부 갱신이 없다(Lock, @Version, PESSIMISTIC 검색 0건). 두 트랜잭션이 같은 version을 읽으면 둘 다 통과하고 뒤 저장이 앞을 덮는다. 계약 2-2절과 7절이 정한 낙관적 잠금은 저장할 때 대조한다고 적었는데 구현은 읽은 시점의 값을 대조한다. 반영은 둘이다. version에 JPA @Version을 달아 손으로 더하던 증가를 프레임워크 증가로 바꾸고 프레임워크의 낙관적 잠금 예외를 409 VERSION_CONFLICT로 옮긴다. 그리고 같은 version으로 동시에 PATCH하는 MySQL 테스트를 Property와 RoomType 각각 하나씩 둔다. 재고 묶음 R1 반영의 V14와 같은 모양이다. 반영 주체는 숙소 묶음 |
| A | 02 | S9-R1-A-02 | 보통 | `backend/src/main/java/com/o2o/catalog/domain/Region.java:27` | API 계약 준수, 계약 하강 | API 계약 준수, 계약 하강 | 등록된 지역 코드만 저장해야 하지만 임의 문자열을 그대로 받는다. CAT-01과 CAT-02의 지역 존재 확인이 없고 CAT-04도 임의 코드를 조회 조건으로 받는다. | `Region.of`는 확인 없이 객체를 만든다. 같은 파일 9행부터 12행도 지역 fixture가 없어 확인하지 않는다고 명시한다. 11 CAT-01은 등록 지역 코드 확인을 요구한다. `CatalogUpdateAndListApiTest:197`은 임의 `REGION_` 값을 정상 등록한다. | 승인된 지역 fixture 조회 포트를 두고 등록과 수정 전에 존재를 확인한다. CAT-04에도 길이와 등록 코드 검증을 적용하고 미등록 코드의 400 테스트를 추가한다. | 수용 | 사실이다. Region.java 27행의 of는 확인 없이 만든다. 같은 파일 9행부터 12행 주석이 지역 fixture가 없어 두 번째 바퀴로 미룬다고 적었지만 계약 어디에도 그 이월이 없다(계약에서 지역과 fixture 검색 0건). 11 CAT-01 처리 규칙 49행은 등록된 지역 코드를 확인하고 저장한다이고 CAT-02와 CAT-04 필드표도 등록된 지역 코드다. 코드 주석에만 남긴 이월은 이월이 아니다. 반영은 지역 fixture(등록 목록)를 두고 CAT-01과 CAT-02가 존재를 확인하며 CAT-04는 길이와 등록 여부를 검사해 400 INVALID_REQUEST로 거절하는 것이고 테스트는 미등록 코드의 등록과 수정과 목록 셋이다. fixture의 값 목록은 사용자가 정한다. 명세 예시는 SEOUL 하나뿐이다. 검색 SEARCH-01과 프로모션 regionCodes도 같은 문구를 쓰므로 fixture는 shared에 두고 그 둘의 확인은 프로모션과 검색 쌍 결정표에서 가른다. 반영 주체는 숙소 묶음 |
| A | 03 | S9-R1-A-03 | 보통 | `backend/src/main/java/com/o2o/catalog/api/UpdatePropertyRequest.java:17` | API 계약 준수, 계약 하강 | API 계약 준수, 계약 하강 | PATCH 문자열 필드가 공백 제거 후 최소 길이와 허용하지 않은 null 규칙을 온전히 지키지 않는다. 공백만 있는 name, regionCode, address를 `@Size`가 허용하고 POST의 description 명시적 null도 생략과 같이 빈 문자열로 바꾼다. | `UpdatePropertyRequest`와 `UpdateRoomTypeRequest` 문자열은 `@Size`뿐이다. 두 등록 DTO의 `descriptionOrEmpty`는 null을 빈 문자열로 바꾼다. 11 공통 규칙은 허용하지 않은 null을 400으로 하고 name 등은 공백 제거 후 최소 길이를 요구한다. 대상 API 테스트에는 이 경계가 없다. | 생략과 명시적 null을 구분하고 명세가 null을 허용하지 않는 필드는 400으로 거절한다. 문자열은 trim한 값으로 길이를 검사하고 등록과 수정 양쪽에 공백 및 null 테스트를 추가한다. | 수용 | 사실이다. UpdatePropertyRequest 17행부터와 UpdateRoomTypeRequest 17행은 @Size뿐이라 공백만 있는 name과 regionCode와 address가 통과해 그대로 저장된다. 등록 DTO 둘은 @NotBlank로 공백만 있는 값은 막지만 앞뒤 공백을 떼지 않아 원문 길이로 세고, descriptionOrEmpty가 명시적 null을 생략과 같이 빈 문자열로 바꾼다. 11 공통 규칙 38행은 허용하지 않은 null을 400으로, CAT-01과 CAT-02 필드표는 name을 공백 제거 후 1~100자로 적는다. 반영은 셋이다. name은 요청 경계에서 strip한 뒤 1~100자를 검사하고 strip한 값을 저장한다(등록과 수정, 숙소와 객실 넷). 수정 DTO의 문자열 셋은 공백만 있는 값을 400으로 거절한다. 명시적 null은 생략과 갈라 400으로 거절하되 방법은 Jackson의 null 처리 설정을 확인한 뒤 정한다. 테스트는 CAT-01, 02, 06, 07 각각에 공백만 있는 값과 앞뒤 공백 경계와 명시적 null이다. 반영 주체는 숙소 묶음 |
| A | 04 | S9-R1-A-04 | 확인필요 | `backend/src/main/java/com/o2o/catalog/api/PropertyController.java:94` | API 계약 준수 | API 계약 준수 | 명세에 없는 Query 필드를 거절하는 장치가 허용 입력 안에서 확인되지 않는다. Spring 기본 동작이면 `?unknown=x`를 무시하고 200을 반환한다. 목록 밖 전역 필터가 막는지는 허용 입력만으로 확인할 수 없다. | 11 공통 헤더 74행은 명세에 표시하지 않은 Query 필드를 지원하지 않는다고 한다. 세 목록 컨트롤러와 대상 테스트에는 미정의 Query 검증이 없다. 승인 명령에는 이를 직접 호출할 명령이 없다. | 전역 처리 유무를 확인하고 없다면 허용 Query 이름을 검사해 400 `INVALID_REQUEST`로 통일한다. CAT-04, CAT-05, CAT-09 테스트를 추가한다. | 반박 | 확인했다. 11 공통 헤더 74행은 표시하지 않은 Path와 Query와 Body 필드를 지원하지 않는다고만 적고 상태 코드를 정하지 않는다. 400을 요구하는 것은 38행이고 그 대상은 요청의 정의되지 않은 필드, 곧 JSON body다. JSON 쪽은 backend/src/main/resources/application.properties 7행의 fail-on-unknown-properties=true가 전역으로 켜져 있고 CatalogApiTest 186행 C9가 정의되지 않은 body 필드의 400을 실제 포트로 고정한다(A 자신의 검증 ID 결과 C9 통과와 같은 자리). Query 필드를 무시하고 200을 주는 것은 명세 위반이 아니다. 평가자가 못 본 이유는 설정 파일이 허용 입력 밖이어서이고 그 말은 맞다. 재고 묶음 R1의 A-04와 B-04도 같은 자리에서 같은 결론이었다. 반영 없음 |
| B | 01 | S9-R1-B-01 | 치명 | `backend/src/main/java/com/o2o/catalog/domain/Property.java:56`, `backend/src/main/java/com/o2o/catalog/domain/Property.java:102`, `backend/src/main/java/com/o2o/catalog/domain/RoomType.java:47`, `backend/src/main/java/com/o2o/catalog/domain/RoomType.java:94` | 추적성 양방향, 결정 근거의 자립성, 요구사항 역추적 | 추적성 양방향, 결정 근거의 자립성, 요구사항 역추적 | 채택된 낙관적 잠금이 원자적으로 강제되지 않는다. 두 트랜잭션이 같은 version을 읽으면 둘 다 메모리 대조를 통과하고 저장할 수 있어 마지막 저장이 앞선 수정을 덮는다. 두 요청 모두 200으로 끝날 수 있으므로 11 명세의 `version` 불일치 409와 계약 2-2절의 동시 수정 검출이 깨진다. 현재 테스트는 숙소의 순차적인 잘못된 version만 확인해 이 경합을 잡지 못한다. | `version` 필드는 `@Column`일 뿐이고, 애그리거트가 읽어 온 값과 요청값을 비교한 뒤 직접 1을 더한다. `PropertyJpaRepository`와 `RoomTypeJpaRepository`에도 version 조건부 갱신이나 잠금이 없다. `CatalogUpdateAndListApiTest:92-106`은 이미 저장된 뒤 잘못된 version을 보내는 순차 경로만 검사한다. | JPA `@Version`을 사용하거나 `where id = ? and version = ?` 조건부 갱신의 영향 행 수로 충돌을 판정한다. 프레임워크의 낙관적 잠금 예외도 409 `VERSION_CONFLICT`로 변환한다. Property와 RoomType 각각 같은 version으로 동시에 수정하는 테스트를 추가한다. | 수용 | A-01과 같은 문제다. 대조와 반영 방법은 A-01 행에 적었다. A와 B를 같은 세션이 썼으므로(아래 평가 절차 기록) 두 리포트가 같은 줄을 짚은 것을 독립 확인 둘로 세지 않는다. 대신 개발 세션이 코드를 직접 대조했고 결과는 같다. B가 더 짚은 RoomType.java 47행과 94행도 같은 모양이라 둘 다 고친다. PropertyJpaRepository와 RoomTypeJpaRepository에 조건부 갱신이 없다는 근거도 맞다 |
| B | 02 | S9-R1-B-02 | 보통 | `backend/src/main/java/com/o2o/catalog/domain/Region.java:27`, `backend/src/main/java/com/o2o/catalog/application/CatalogApplicationService.java:67`, `backend/src/main/java/com/o2o/catalog/api/PropertyController.java:96` | 추적성 양방향, 요구사항 역추적 | 추적성 양방향, 요구사항 역추적 | CAT-01과 CAT-02가 요구하는 등록된 지역 코드 확인이 없다. CAT-04의 `regionCode`는 1자 이상 32자 이하 제약도 적용되지 않는다. 임의 문자열이 숙소에 저장되고 목록 필터로 전달된다. | `Region.of`는 문자열을 그대로 감싸고 앱 서비스도 별도 조회 없이 생성한다. CAT-04 컨트롤러는 검증 애노테이션 없는 `String`을 받는다. `CatalogApiTest:350-356`은 등록 여부와 무관한 32자 문자열의 등록 성공을 기대하고, `CatalogUpdateAndListApiTest:195-205`는 매번 만든 임의 지역 코드로 등록과 조회를 성공시킨다. 이는 11 CAT-01 처리 규칙과 CAT-01, CAT-02, CAT-04 필드 제약을 반대로 고정한다. | 지역 fixture 또는 지역 조회 포트를 두고 등록과 수정 전에 존재를 확인한다. CAT-04에도 1자 이상 32자 이하 검증을 적용한다. 미등록 지역의 등록, 수정, 목록 조회가 400 `INVALID_REQUEST`인지 확인하는 테스트를 추가하고 임의 지역 성공 테스트는 등록된 fixture를 사용한다. | 수용 | A-02와 같은 자리다. 대조와 반영은 A-02 행. B가 더한 것 둘도 맞다. CAT-04는 PropertyController 96행이 검증 없는 String을 받아 1자 이상 32자 이하 제약이 없고, CatalogApiTest 350행과 CatalogUpdateAndListApiTest 195행이 임의 문자열의 등록 성공을 고정한다. 반영 때 그 두 테스트는 등록된 fixture 값으로 바꾸고 미등록 값은 400을 기대하게 한다 |
| B | 03 | S9-R1-B-03 | 보통 | `backend/src/main/java/com/o2o/catalog/api/UpdatePropertyRequest.java:19`, `backend/src/main/java/com/o2o/catalog/api/UpdateRoomTypeRequest.java:17`, `backend/src/main/java/com/o2o/catalog/domain/Property.java:105`, `backend/src/main/java/com/o2o/catalog/domain/RoomType.java:103` | 추적성 양방향, 요구사항 역추적 | 추적성 양방향, 요구사항 역추적 | 이름의 공백 제거 후 1자 이상 100자 규칙이 구현되지 않았다. 특히 PATCH는 공백만 있는 이름이 `@Size(min = 1)`을 통과하고 그대로 저장된다. 앞뒤 공백을 제외하면 100자 이하인 입력도 원문 길이로 검사되어 거절될 수 있다. | 등록 DTO의 `@NotBlank`는 공백만 있는 값은 막지만 정규화하지 않는다. 수정 DTO는 `@Size`만 사용한다. 애그리거트는 전달된 이름을 그대로 대입하고 대상 테스트에는 공백 정규화 경계가 없다. | 요청 경계에서 이름을 먼저 `strip`한 뒤 1자 이상 100자를 검사하고 정규화된 값을 저장한다. CAT-01, CAT-02, CAT-06, CAT-07 각각에 공백만 있는 값과 앞뒤 공백이 있는 경계값 테스트를 추가한다. | 수용 | A-03과 겹치는 자리이고 그중 name의 공백 정규화 몫이다. 대조와 반영은 A-03 행. B가 짚은 대로 앞뒤 공백을 빼면 100자 이하인 입력이 원문 길이로 거절되는 것도 같은 반영(strip 뒤 검사)으로 닫힌다. Property.java 105행과 RoomType.java 103행은 전달된 이름을 그대로 대입하므로 정규화는 요청 경계 한 곳에서 하고 도메인은 건드리지 않는다 |
| B | 04 | S9-R1-B-04 | 확인필요 | `backend/src/main/java/com/o2o/shared/SharedExceptionHandler.java:24` | 추적성 양방향, 요구사항 역추적 | 추적성 양방향, 요구사항 역추적 | 허용된 대상 코드만 보면 예상하지 못한 예외를 500 `INTERNAL_ERROR`의 공통 Error 모델로 바꾸는 경로가 없다. 타입 변환 등 일부 400 경로도 동일 형식을 보장하는지 확인할 수 없다. 다만 허용 범위 밖의 다른 전역 Advice가 처리할 가능성을 배제할 수 없어 확인필요다. | 대상의 전역 Advice는 행위자, Bean Validation, 읽을 수 없는 body, version 충돌, `IllegalArgumentException`만 처리한다. 11 에러 응답 절은 모든 API에 공통 형식과 500 `INTERNAL_ERROR`를 적용한다고 정한다. 허용 범위에는 나머지 전역 예외 처리 코드가 포함되지 않았다. | 전체 전역 Advice를 허용 입력에 추가해 최종 매핑을 확인한다. 없다면 예상하지 못한 예외와 파라미터 타입 오류를 공통 Error 모델로 변환하고 응답에 내부 예외 내용을 넣지 않는 테스트를 추가한다. | 수용 | 확인했더니 사실이다. 허용 범위 밖까지 봤다. 백엔드 전체에 @RestControllerAdvice는 여섯(shared 하나, 컨텍스트마다 하나)이고 예상하지 못한 예외를 500 INTERNAL_ERROR의 공통 Error 모델로 바꾸는 핸들러는 어디에도 없다. BookingExceptionHandler 166행의 INTERNAL_ERROR는 가격 스냅샷 위반 한 경우뿐이다. 파라미터 타입 오류(page=abc 같은 것)와 필수 파라미터 누락을 잡는 핸들러도 없어 스프링 기본 오류 본문으로 나간다. 프로모션과 검색 계약 10절도 필수 쿼리 파라미터 누락의 스프링 기본 400 본문을 공유 핸들러 동결 때문에 남겼다고 적었다. 반영은 SharedExceptionHandler에 둘을 더하는 것이다. 예상하지 못한 예외는 500 INTERNAL_ERROR로 내부 내용 없이, 파라미터 타입 오류와 필수 파라미터 누락은 400 INVALID_REQUEST로. 테스트는 shared 자리에 하나씩. 다섯 쌍 모두에 걸리는 공통 반영이라 라운드에서 한 번만 고치고 다른 쌍 결정표는 이 행을 가리킨다. 반영 주체는 숙소 묶음(shared) |
| B | 05 | S9-R1-B-05 | 보통 | `backend/src/test/java/com/o2o/catalog/domain/RoomTypeTest.java:27`, `backend/src/test/java/com/o2o/catalog/api/CatalogUpdateAndListApiTest.java:154` | 요구사항 역추적 | 요구사항 역추적 | I14는 등록과 수정 모두 RoomType이 지켜야 하지만 테스트는 등록 실패만 검사한다. CAT-07 테스트는 유효한 하향 수정과 소유권만 확인해 `RoomType.update`에서 I14 검사를 없애거나 PATCH DTO의 하한을 제거해도 대상 테스트가 잡지 못한다. | `RoomTypeTest` 네 건은 전부 `RoomType.register`만 호출한다. CAT-07 두 건은 `maxOccupancy:2` 성공과 다른 소유자 404다. 06-4의 updateRoomType 계약은 인원 양수 I14를 명시한다. | `RoomType.update`에 0과 음수를 넣는 도메인 실패 테스트와 양수 성공 테스트를 짝으로 추가한다. CAT-07에도 `maxOccupancy` 0과 101이 400이고 기존 값과 version이 유지되는 API 테스트를 추가한다. | 수용 | 사실이다. RoomTypeTest 27행부터의 네 건은 전부 register이고 CatalogUpdateAndListApiTest 154행의 CAT-07 두 건은 maxOccupancy 2의 성공과 남의 소유자 404다. RoomType.update 98행의 I14 검사와 UpdateRoomTypeRequest 18행의 @Min(1) @Max(100)은 있지만 어느 테스트도 수정 경로에 0과 101을 보내지 않는다. 코드는 맞고 테스트가 빈다. 반영은 테스트만이다. 도메인에 update의 0과 음수 실패와 양수 성공, API에 CAT-07의 0과 101이 400이고 값과 version이 그대로인 것. 반영 주체는 숙소 묶음 |
| B | 06 | S9-R1-B-06 | 확인필요 | `backend/src/main/java/com/o2o/catalog/domain/PropertyRepository.java:45`, `backend/src/main/java/com/o2o/catalog/domain/RoomTypeRepository.java:26` | 추적성 양방향, 결정 근거의 자립성 | 추적성 양방향, 결정 근거의 자립성 | 검색용 무페이지 조회 메서드 둘은 이 작업 계약의 API 9개와 허용된 11, 06-2, 06-4 문서에 근거가 없다. 대상 목록은 다른 프로모션과 검색 묶음이 추가했다고 설명하지만 그 묶음의 계약과 06-1은 허용 입력이 아니어서 추가 동작의 정당성을 독립 판정할 수 없다. | `findAllByRegionCode`와 `findAllByPropertyId` 및 JPA 구현은 SEARCH-01, 06-1, 다른 작업 계약을 주석 근거로 든다. 현재 허용 문서에는 해당 계약이 없다. | 이 두 메서드를 현재 평가 대상에서 제외하거나, 검색 묶음 계약과 필요한 06-1 절을 허용 입력에 추가해 별도 묶음에서 양방향 추적을 확인한다. | 반박 | 확인했다. 두 메서드는 프로모션과 검색 묶음이 더한 것이다(커밋 888b278. PropertyRepository 46행부터의 주석이 06-1 R8과 계약 task-S9-promotion-search 7절 D-1 나를 근거로 적는다). 그 계약 144행의 D-1은 기존 테이블 직접 조회로 2026-09-10 확정됐고, SearchApplicationService 89행과 91행이 이 둘을 부르며 SearchApiTest가 그 경로를 실제 포트로 지난다. 대상 목록 2절 23행이 이 여덟 파일의 사정을 미리 적었고 평가자도 그것을 읽었다. 평가자가 독립 판정할 수 없다고 한 것은 허용 입력의 범위 때문이고 그 말은 맞다. 이 두 메서드의 양방향 추적은 프로모션과 검색 쌍의 결정표에서 가른다. 반영 없음 |

원문 지적과 근거와 수정 제안은 상세 표의 문장을 스크립트로 그대로 옮겼다. 요약하거나 자르지 않았다. 위반 기준 열은 보조 표의 값이다.

## 사용자 추가 지적

| 추가 ID | 심각도 | 대상 위치 | 지적 | 근거 | 처리 |
|---|---|---|---|---|---|

개발 세션이 대조하면서 평가자 둘이 못 본 코드 결함을 더 찾지 못했다. 이 절은 비운다. 라운드 차원의 하네스 후보(지적 ID에 Task 키가 없어 다섯 쌍이 같은 ID를 쓴다)는 라운드 README 8절에 있고 여기 넣지 않는다.

## 치명 지적의 오판 판단 기록

치명 둘을 모두 수용했다. 오판 정정을 주장하는 행이 없어 이 절은 비운다.

## 반영 전 G2 확인

- [x] 평가 대상, 계약, A/B 리포트의 버전이 결정표와 일치한다.
- [x] A와 B 원본의 지적 ID 집합과 결정표의 지적 ID 집합이 같다. 누락, 추가, 중복이 없다.
- [x] 원본의 각 행과 결정표를 대조했고, 결정과 이유 외의 정보가 동일하다. 스크립트가 셀을 그대로 옮겼다.
- [x] 전체 행 수가 A와 B 원본 지적 수의 합과 같다. 지적 수가 0이면 리포트가 실제로 정상 완료되었는지 확인했고 파싱 실패나 평가 미완료를 0건으로 취급하지 않았다.
- [x] 모든 행의 결정이 수용, 거부, 반박 중 하나다. 빈 결정이 없다.
- [x] 모든 거부에 이유가 있고, 반박은 원본 심각도가 확인필요인 행에만 있다. 거부는 없다.
- [x] 치명 오판 정정을 주장한 행마다 근거, 대안, 안티패턴, 사용자 결정, 날짜가 원본 지적에 연결되어 있다. 해당 행 없음.

| 확인 항목 | 기록 |
|---|---|
| 확인자와 날짜 | g2 실행은 2026-09-14 Claude Code(개발 세션). 사람 서명 대기 |
| G2 결과 | 통과. node harness/tools/check.mjs g2 harness/decisions/task-S9-catalog-R1.md --mode pre 검사 84건 통과(2026-09-14 결정 전). 같은 명령 --mode final 검사 86건 통과(2026-09-14 반영 뒤 인계 표 기입 후) |
| 미완료 사유 | 없음. 사용자가 2026-09-14 결정 10건을 확정했고 기계 검사는 위 결과 |

## 반영과 최종 확인 인계

| 확인 대상 | 기록 |
|---|---|
| 반영본 절대경로와 버전 또는 해시 | 브랜치 fix/task-s9-catalog-r1-apply. 코드 마지막 커밋 eb1304a(브랜치 시작은 이 결정표의 확정 커밋 e1a9392). backend/ 아래 26개 파일의 sha256은 harness/out/task-S9-catalog-R1/applied/apply-report.md 2절(sha256:f69ad34b8377d2f8). diff 전문은 같은 폴더 apply.diff. 계약은 harness/tasks/task-S9-catalog.md 개정 4 sha256:b35eca9a0afe6e15 |
| 수용 항목 반영 확인 | 8건 전부 반영했다. 위치는 apply-report 3절. A-01과 B-01은 Property와 RoomType의 @Version과 JPA 어댑터의 saveAndFlush와 공유 핸들러의 409와 C11 둘. A-02와 B-02는 shared/RegionRegistry와 앱 서비스의 등록 여부 확인 셋과 CAT-04 길이 검사와 C12 여섯. A-03과 B-03은 요청 DTO 넷의 setter 클래스(strip, 공백만 400, 명시적 null 400)와 C13 여덟. B-04는 UnexpectedExceptionResolver와 공유 핸들러의 400 둘과 C15 넷. B-05는 RoomTypeTest 셋과 C14 하나. 결정표와 다르게 간 것 셋(500의 자리가 어드바이스가 아니라 resolver, 명시적 null의 방법은 Jackson 3.1.5 실측으로 정함, fixture 열일곱 값은 제안)은 apply-report 4절 |
| 미수용 항목 무변경 확인 | 반박 둘(A-04, B-06)이 짚은 HostPropertyController와 RoomTypeController와 PropertyRepository와 RoomTypeRepository는 기준 커밋 1bdadfe와 같다. PropertyController는 B-02(수용)의 CAT-04 길이 검사로만 바뀌었고 A-04가 요구한 미정의 Query 필드 거절은 넣지 않았다. 반박 근거 둘은 계약 5절의 R2 허용 입력 행으로 열었다. apply-report 5절 |
| 남은 실제 치명 지적 | 없음 |
| 치명 둘이 닫힌 근거 | A-01과 B-01은 @Version과 saveAndFlush로 저장 시점 대조가 됐고 C11 둘이 잡는다. @Version을 떼면 둘 다 실패한다(apply-report 6절 변이 검사) |
| 오판 정정 완료 항목 | 없음 |
| 마지막 반영본에 필요한 검증과 결과 | 전체 테스트 --rerun-tasks 50클래스 443건 실패 0 오류 0 건너뜀 0(평가 전 419건에 24 추가). g1 code 104건 통과(RegionRegistry.java와 UnexpectedExceptionResolver.java, --artifact junit 50개). 계약 fill 120건 중 12건 실패이고 열두 건 전부 2026-09-09부터 있던 것(새 행은 통과). 같은 version 동시 PATCH 테스트 둘 통과. 변이 검사 셋은 apply-report 6절. g2 --mode final은 아래 G2 결과 행 |
| 코드 작업의 직접 실행 근거 | harness/out/task-S9-catalog-R1/applied/junit/ 아래 JUnit XML 50개(2026-09-14 21:25 실행, DB o2o_fix_test). 명령과 숫자는 apply-report 6절 |
| 평가 대상 소스와 테스트의 무변경 확인 | 평가 전후 대조. A와 B가 적은 58개 파일의 sha256이 서로 같고 대상 목록의 앞 16자리와 같다. verify-eval-workspace.mjs의 ws.target-drift가 2026-09-14 14:23에 PASS였고 평가 뒤 git status에 리포트 둘 말고 바뀐 파일이 없다 |
| 검증 미완료 사항 | 사용자 확정. A의 Gradle 실행이 UP-TO-DATE였던 것은 반영 라운드의 전체 테스트 실행이 대신한다 |
| 사용자 최종 완료 판단과 날짜 | 미완료. 반영은 2026-09-14 끝났다. 남은 사용자 몫은 PR 병합과 RegionRegistry 값 목록 결정이다 |
| 실제 판단 및 확인에 사용한 시간 | 초안 작성 약 40분. 사용자 판단 시간은 미측정. 반영은 코드와 테스트 약 57분(확정 커밋 20:26부터 코드 마지막 커밋 21:22), 기록 약 15분 |
