# task-S9-catalog R1 반영 기록

양식: harness/prompts/apply.md v3 (2026-09-08)
최초 작성: 2026-09-14
최종 갱신: 2026-09-14

왜 이 파일이 필요한가: 반영본만 내면 어느 지적이 어디로 갔는지 사람이 다시 대조해야 한다. 지적 ID와 변경 위치를 짝지어 두면 최종 확인에서 이 표만 보면 된다. 코드 반영이라 문서 반영과 달리 diff와 테스트 숫자와 변이 검사가 근거다. 라운드 mvp-eval-2026-09-14 쌍 1의 반영이고 앞선 같은 모양은 harness/out/task-S9-inventory-rate-R1/applied/apply-report.md다.

## 1. 이번 반영

| 항목 | 내용 |
|---|---|
| Task ID와 Step | task-S9-catalog, Step 9 |
| 작업 유형 | 코드 |
| 평가 라운드 | R1. 라운드 mvp-eval-2026-09-14 쌍 1 |
| 평가받은 대상 버전 | 기준 커밋 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1(PR 138 병합, main). 파일 목록 harness/out/task-S9-catalog-R1/eval-target-files.md sha256:c019bc515f6c68e2. 평가 브랜치 eval/mvp-2026-09-14 |
| 반영 범위 | 결정표의 수용 8건. 조치는 5건이다. A-01과 B-01, A-02와 B-02, A-03과 B-03이 각각 같은 자리라 하나로 닫힌다. 반박 2건(A-04, B-06)은 코드 변경 없음이고 계약 5절에 R2부터 읽는 허용 입력 둘을 열었다 |
| 수정 후보 위치 | 브랜치 fix/task-s9-catalog-r1-apply. 평가 브랜치 끝 e1a9392(결정 확정 커밋)에서 땄다. 코드 변경은 e26f1c0부터 eb1304a까지 열 커밋이고 이유 하나에 커밋 하나다 |
| 결과 기록 경로 | harness/out/task-S9-catalog-R1/applied/apply-report.md. diff 전문은 같은 폴더 apply.diff, 테스트 결과 사본은 junit/ 아래 50개 |
| 사용자 결정 | 결정 10건 초안대로 확정. join5201, 2026-09-14. 발언은 초안대로 10건 확정해라. 반영 진행해라 |
| 작업 공간 | 워크트리 C:/Dev/potenup/99_projects/o2o-payment. o2o-dev는 남은 네 쌍의 평가가 도는 동안 평가 브랜치를 그대로 둬야 해서 손대지 않았다. 테스트 DB는 컨테이너 o2o-catalog-mysql에 새로 만든 o2o_fix_test다. 평가자 A 세션에 배정된 DB 넷(o2o_catalog_test, o2o_promo_test, o2o_booking_test, o2o_payment_test)을 건드리지 않기 위해서다 |

## 2. 반영본

브랜치 시작 e1a9392에서 코드 마지막 커밋 eb1304a까지 backend/ 아래 바뀐 파일은 아래 26개뿐이다. 기준 커밋 1bdadfe와 e1a9392 사이 backend/ 차이는 backend/README.md 6절 진척 표 한 곳(PR 139)이고 평가 대상 파일은 없다. 다른 세션의 변경이 섞이지 않았다. diff 전문은 같은 폴더의 apply.diff다.

프로덕션 15개.

| 파일 | 무엇이 바뀌었나 | 해시 |
|---|---|---|
| backend/src/main/java/com/o2o/catalog/domain/Property.java | version에 @Version. update의 손 증가 줄을 뺐다 | sha256:f85ce76665a89688 |
| backend/src/main/java/com/o2o/catalog/domain/RoomType.java | 같음 | sha256:ab34009e82e878ce |
| backend/src/main/java/com/o2o/catalog/infrastructure/JpaPropertyRepository.java | save가 saveAndFlush. 대조가 저장 시점에 일어나고 응답과 이벤트가 새 version을 든다 | sha256:f7f4c71ed3a6e453 |
| backend/src/main/java/com/o2o/catalog/infrastructure/JpaRoomTypeRepository.java | 같음 | sha256:35b56e5230f66be1 |
| backend/src/main/java/com/o2o/shared/SharedExceptionHandler.java | 핸들러 넷 추가. OptimisticLockingFailureException 409, 파라미터 타입 오류 400, 필수 파라미터 누락 400, UnregisteredRegionException 400 | sha256:3296140deac47518 |
| backend/src/main/java/com/o2o/shared/UnexpectedExceptionResolver.java | 신설. 순번이 가장 늦은 HandlerExceptionResolver. 예상하지 못한 예외를 500 INTERNAL_ERROR 공통 형식으로. 본문에 코드와 traceId만, 예외 내용은 로그에 | sha256:8eca9c03d84a67d6 |
| backend/src/main/java/com/o2o/shared/RegionRegistry.java | 신설. 등록 지역 코드 목록. 초기값 열일곱은 제안이고 사용자가 정한다. register는 초기 세팅과 테스트 fixture용 | sha256:58bc982c0fcb3d90 |
| backend/src/main/java/com/o2o/shared/UnregisteredRegionException.java | 신설 | sha256:f874c0e334adeca1 |
| backend/src/main/java/com/o2o/catalog/application/CatalogApplicationService.java | RegionRegistry 주입. 등록과 수정(regionCode를 준 경우)과 목록에서 등록 여부 확인 | sha256:91f1087ce7d4069d |
| backend/src/main/java/com/o2o/catalog/api/PropertyController.java | CAT-04의 regionCode 1자 이상 32자 이하 검사. 검색 SEARCH-01과 같은 모양 | sha256:648b74c01d10e720 |
| backend/src/main/java/com/o2o/catalog/domain/Region.java | 머리 주석만. 두 번째 바퀴 이월 문장을 지금 자리로 | sha256:5e679316572fa8b3 |
| backend/src/main/java/com/o2o/catalog/api/RegisterPropertyRequest.java | record에서 setter 클래스로. name strip, description의 명시적 null 400 | sha256:e11b18024d0bb8b9 |
| backend/src/main/java/com/o2o/catalog/api/RegisterRoomTypeRequest.java | 같음 | sha256:bc67d4ee14de4da9 |
| backend/src/main/java/com/o2o/catalog/api/UpdatePropertyRequest.java | record에서 setter 클래스로. name strip 뒤 1~100자, regionCode와 address의 공백만 400, 문자열 넷의 명시적 null 400 | sha256:7e08dc2b74f5c000 |
| backend/src/main/java/com/o2o/catalog/api/UpdateRoomTypeRequest.java | 같음. maxOccupancy의 명시적 null도 400 | sha256:321ef25f62cbee8b |

테스트 11개.

| 파일 | 무엇이 바뀌었나 | 해시 |
|---|---|---|
| backend/src/test/java/com/o2o/catalog/api/CatalogConcurrentUpdateApiTest.java | 신설. C11 둘 | sha256:2ca6e6251fd37ded |
| backend/src/test/java/com/o2o/shared/SharedErrorResponseApiTest.java | 신설. C15 넷 | sha256:aaa725dc9e0b339a |
| backend/src/test/java/com/o2o/catalog/api/CatalogUpdateAndListApiTest.java | C12 여섯, C13 넷, C14 하나. 목록 검사 셋은 새_지역 도우미로 고유 코드를 fixture에 넣는다 | sha256:405d8eb3ec4296c2 |
| backend/src/test/java/com/o2o/catalog/api/CatalogApiTest.java | C13 넷. 32자 경계는 등록된 32자 코드로 | sha256:0aed87e4d222d043 |
| backend/src/test/java/com/o2o/catalog/domain/RoomTypeTest.java | C14 셋. update의 0과 음수 거절, 양수 통과 | sha256:d3c28a63b8915584 |
| backend/src/test/java/com/o2o/booking/BookingFixtures.java | 새로 뽑는 지역 코드를 fixture에 등록. 검사 내용 불변 | sha256:dfe471005a357e09 |
| backend/src/test/java/com/o2o/booking/BookingLifecycleTestConfiguration.java | 같음 | sha256:5a49ed9c47cc6bc7 |
| backend/src/test/java/com/o2o/promotion/api/PromotionApiTest.java | 같음 | sha256:35d7c95f1b7ade5b |
| backend/src/test/java/com/o2o/promotion/application/PromotionApplicationServiceTest.java | 같음 | sha256:080c803227137b75 |
| backend/src/test/java/com/o2o/search/api/SearchApiTest.java | 같음 | sha256:27d858634c5b6d20 |
| backend/src/test/java/com/o2o/inventory/domain/InventoryAllocationServiceTest.java | 같음 | sha256:77f75c62cf7f1138 |

문서 쪽 반영은 계약이다.

| 파일 | 무엇이 바뀌었나 | 해시 |
|---|---|---|
| harness/tasks/task-S9-catalog.md | 개정 4. 0-5절 신설, 5절 R2 허용 입력 둘, 8-1의 C11부터 C15, 10절 개정 4 행과 마지막 성공 단계 행. fill 120건 중 12건 실패이고 열두 건 전부 2026-09-09부터 있던 것(2절 API 경로 행 아홉, 4절 절대경로 해시 둘, 자기 해시 없음 하나). 새 행은 통과 | sha256:b35eca9a0afe6e15 |

## 3. 지적 ID별 변경 위치

| 지적 ID | 심각도 | 결정 | 변경 위치 | 무엇을 넣었나 |
|---|---|---|---|---|
| S9-R1-A-01 | 치명 | 수용 | Property.java와 RoomType.java의 version에 @Version, JPA 어댑터 둘의 saveAndFlush, SharedExceptionHandler의 OptimisticLockingFailureException 409, CatalogConcurrentUpdateApiTest C11 둘 | 대조가 읽은 시점이 아니라 저장 시점에 일어난다. 증가는 flush 때 프레임워크가 한다. update의 메모리 대조는 순차적인 낡은 요청의 즉시 409로 남겼다. 같은 version을 든 두 트랜잭션이 MySQL에서 경합하면 뒤에 저장하는 쪽이 409이고 앞쪽 값이 남는다 |
| S9-R1-B-01 | 치명 | 수용 | 같음 | A-01과 같은 조치가 닫는다. B가 더 짚은 RoomType의 두 자리도 같은 모양으로 고쳤다 |
| S9-R1-A-02 | 보통 | 수용 | shared/RegionRegistry.java와 UnregisteredRegionException.java 신설, CatalogApplicationService의 등록과 수정과 목록, SharedExceptionHandler 400, CatalogUpdateAndListApiTest C12 여섯 | 미등록 코드는 CAT-01 등록과 CAT-02 수정과 CAT-04 목록에서 400 INVALID_REQUEST이고 details가 regionCode를 가리킨다. fixture 값 열일곱은 제안이고 사용자가 정한다. 검색과 프로모션의 같은 확인은 그 쌍의 결정표에서 가른다 |
| S9-R1-B-02 | 보통 | 수용 | 같음. PropertyController의 CAT-04 regionCode 길이 검사, CatalogApiTest 32자 경계 테스트의 fixture 등록 | CAT-04의 regionCode가 공백만이거나 33자면 400. B가 짚은 임의 문자열 등록 성공 테스트 둘은 등록된 코드로 바꿨고 미등록 값의 400은 C12가 본다 |
| S9-R1-A-03 | 보통 | 수용 | 요청 DTO 넷(RegisterPropertyRequest, RegisterRoomTypeRequest, UpdatePropertyRequest, UpdateRoomTypeRequest), CatalogApiTest와 CatalogUpdateAndListApiTest의 C13 여덟 | name은 요청 경계에서 strip한 뒤 검사하고 strip한 값을 저장한다. 수정 DTO의 문자열 셋은 공백만 있는 값을 400으로. 선택 필드의 명시적 null은 생략과 갈라 400으로. 결정표가 방법은 Jackson의 null 처리 설정을 확인한 뒤 정한다고 적었고 그 확인 결과가 4절의 Jackson 항목이다 |
| S9-R1-B-03 | 보통 | 수용 | 같음 | 앞뒤 공백을 빼면 100자인 이름이 통과한다(C13). 도메인은 건드리지 않았다. Property.update와 RoomType.update는 전달된 이름을 그대로 대입하고 정규화는 요청 경계 한 곳이다 |
| S9-R1-A-04 | 확인필요 | 반박 | 코드 변경 없음. 계약 5절 R2 행(application.properties) | 반박 근거인 fail-on-unknown-properties를 R2 평가자가 읽을 수 있게 했다 |
| S9-R1-B-04 | 확인필요 | 수용 | shared/UnexpectedExceptionResolver.java 신설, SharedExceptionHandler의 파라미터 타입 오류와 필수 파라미터 누락 400, SharedErrorResponseApiTest C15 넷 | 결정표는 SharedExceptionHandler에 둘을 더한다고 적었는데 500 쪽은 어드바이스가 아니라 HandlerExceptionResolver로 갔다. 이유는 4절. 400 둘은 결정표대로 SharedExceptionHandler에 있다. 다섯 쌍 공통 반영이라 이 라운드에서 한 번만 고친다 |
| S9-R1-B-05 | 보통 | 수용 | RoomTypeTest 셋, CatalogUpdateAndListApiTest C14 하나 | 코드는 맞고 테스트가 비어 있었다. update의 0과 음수 거절(값 그대로)과 양수 통과, CAT-07의 0과 101이 400이고 값과 version이 그대로 |
| S9-R1-B-06 | 확인필요 | 반박 | 코드 변경 없음. 계약 5절 R2 행(task-S9-promotion-search.md 7절 D-1) | 반박 근거인 직접 조회 결정을 R2 평가자가 읽을 수 있게 했다 |

## 4. 결정표와 다르게 간 것과 실측

| 항목 | 결정표 | 실제 | 왜 |
|---|---|---|---|
| B-04의 500 자리 | SharedExceptionHandler에 더한다 | UnexpectedExceptionResolver(HandlerExceptionResolver, 순번 가장 늦음) | 스프링 MVC는 어드바이스를 빈 순서로 물어 맞는 핸들러가 하나라도 있는 첫 어드바이스가 이긴다. 공유 어드바이스에 Exception 핸들러를 두면 순서에 따라 컨텍스트 어드바이스의 404와 409를 삼킬 수 있다. 어드바이스 여섯과 스프링 기본 처리기가 전부 지나간 뒤에만 불리는 자리가 resolver다. 검사 C15의 404 테스트가 그 삼킴이 없는 것을 고정한다 |
| A-03의 명시적 null 방법 | Jackson의 null 처리 설정을 확인한 뒤 정한다 | record를 setter 클래스로 바꾸고 선택 필드 setter에 @JsonSetter(nulls = FAIL) | 2026-09-14 실측. Jackson 3.1.5는 record의 생성자 속성에 생략도 null로 넘겨서 FAIL이 생략까지 거절한다. 클래스의 setter는 그 필드가 body에 있을 때만 불리므로 생략(유지 또는 빈 문자열)과 명시적 null(400)을 가를 수 있다. 접근자 이름은 record와 같게 두어 컨트롤러는 그대로다 |
| A-02의 fixture 값 | 사용자가 정한다 | RegionRegistry에 광역자치단체 17곳의 영문 코드를 제안 초기값으로 넣었다 | 값이 하나도 없으면 등록 API가 전부 400이라 기동 확인이 안 된다. 명세 예시는 SEOUL 하나뿐이다. 값 목록은 사용자 결정으로 남아 있고 바꾸면 그 파일의 목록만 바꾸면 된다 |

## 5. 미수용 항목 무변경 확인

반박 둘이 짚은 자리 중 A-04의 목록 컨트롤러 셋 가운데 HostPropertyController.java와 RoomTypeController.java, B-06의 PropertyRepository.java와 RoomTypeRepository.java는 기준 커밋과 같다. git diff --stat 1bdadfe eb1304a 로 그 넷이 나오지 않는다. PropertyController.java는 바뀌었지만 그 변경은 B-02(수용)의 CAT-04 regionCode 길이 검사이고 A-04가 요구한 미정의 Query 필드 거절은 넣지 않았다. B-06의 검색용 조회 메서드 둘은 그대로다.

2절의 26개 밖은 backend/ 아래 어느 파일도 바뀌지 않았다. 동결 대상(backend/build.gradle, backend/CLAUDE.md, harness/prompts, harness/tools, document)은 손대지 않았다.

## 6. 검사

| 명령 | 결과 |
|---|---|
| JAVA_HOME=JDK 21, SPRING_DATASOURCE_URL=o2o_fix_test, ./backend/gradlew.bat -p backend test --rerun-tasks | BUILD SUCCESSFUL. 50클래스 443건, 실패 0, 오류 0, 건너뜀 0. 평가 전 419건에 24가 늘었다(C11 둘, C12 여섯, C13 여덟, C14 넷, C15 넷) |
| node harness/tools/check.mjs g1 backend/src/main/java/com/o2o/shared/RegionRegistry.java --type code --artifact applied/junit/*.xml 50개 | 104건 통과. 테스트 443건 |
| 같은 명령을 UnexpectedExceptionResolver.java에 | 104건 통과 |
| node harness/tools/check.mjs fill harness/tasks/task-S9-catalog.md | 120건 중 12건 실패. 열두 건 전부 개정 전부터 있던 것이고 새 행 열은 통과. 2절 문서 표 참조 |
| node harness/tools/check.mjs g2 harness/decisions/task-S9-catalog-R1.md --mode final | 결정표 인계 표를 채운 뒤 실행. 결과는 결정표 인계 표에 |

테스트 결과 사본은 같은 폴더의 junit/ 아래 50개다.

새 테스트가 실제로 결함을 잡는지 변이 검사로 확인했다. 고친 코드를 잠깐 되돌리고 테스트가 빨개지는지 본 것이다. 파일의 존재나 초록만 보지 않는다(루트 CLAUDE.md 5-1절).

| 되돌린 것 | 결과 | 뜻 |
|---|---|---|
| Property와 RoomType의 @Version을 떼고 손 증가로 | C11 둘 다 실패(둘 다 200으로 끝나 마지막 저장이 앞을 덮는다). 순차 실패 C4와 나머지 16건은 통과 | C11만 A-01의 결함을 잡는다. 순차 호출 테스트는 잠금이 없어도 초록이라는 지적이 숫자로 보였다 |
| UnexpectedExceptionResolver의 @Component를 뗌 | C15 넷 중 500 테스트만 실패(스프링 기본 본문). 셋은 통과 | 500의 공통 형식은 그 자리 하나가 만든다 |
| 요청 DTO 넷을 record로 되돌림 | C13 여덟 중 일곱 실패, 나머지 48건 통과. 공백만 있는 등록 이름의 400 하나는 @NotBlank가 이미 잡던 것이라 되돌려도 통과 | C13이 A-03과 B-03의 결함을 잡는다 |

되돌린 코드는 검사 뒤 원래대로 복구했고 2절의 해시가 복구된 상태다.

## 7. 남은 문제

| 항목 | 상태 | 왜 |
|---|---|---|
| 지역 fixture 값 | 사용자 결정 대기 | RegionRegistry의 열일곱 값은 제안이다. 결정표 A-02 행이 값은 사용자가 정한다고 적었다 |
| 검색과 프로모션의 등록 지역 코드 확인 | 이월 | SEARCH-01과 프로모션 regionCodes도 같은 문구를 쓴다. 그 확인을 넣을지는 프로모션과 검색 쌍의 결정표에서 가른다 |
| 다른 네 쌍의 B-04 자리 | 이 반영을 가리킨다 | 공통 오류 처리는 shared 한 곳이라 다른 쌍 결정표는 이 결정표의 B-04 행을 가리키면 된다 |
| 병합 | 사용자 몫 | PR을 올리고 멈춘다. 병합은 사용자가 한다 |
| 계약 fill의 열두 건 | 그대로 | 2026-09-09부터 있던 실패이고 이번 반영 범위 밖이다(N7) |

## 8. 재평가

재평가는 최대 1회이고 아직 쓰지 않았다. 결정표가 R2를 쓰지 않는다고 적었다(열 건이 전부 코드로 확인되거나 반박됐다). 사용자가 R2를 원하면 기준 커밋은 eb1304a이고 허용 입력은 계약 5절의 R2 행 둘이 늘었다.
