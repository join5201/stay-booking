# task-S9-inventory-rate R1 평가자 A 리포트

평가일: 2026-09-10

## 평가 경계

- 역할: 평가자 A
- 담당 축: 경계 위반, Repository 단위, 계약 하강, 레이어 역전, API 계약 준수, 멱등 규칙, 소유권과 접근, 동시성과 트랜잭션, 테스트 격리와 재현성
- 평가 대상: 계약에 지정된 프로덕션 코드 43개와 테스트 코드 7개
- 제외: `harness/docs/`, `harness/state/`, 이전 평가 리포트, 사용자 결정표, 생성 대화, `step9-verification.md`
- 생성자가 전달한 step5, step6, step6-2의 JUnit XML과 `http-calls.txt`를 읽었다.
- 직접 실행 결과, 생성자가 전달한 결과, 실행하지 못한 항목을 구분했다.
- 저장소의 소스와 테스트는 수정하지 않았다.

## 판정 요약

치명 1 / 보통 2 / 확인필요 1

## 상세

| ID | 심각도 | 위치(파일:행) | 위반 축 | 문제 | 근거(코드 또는 실행 결과) | 수정 제안 |
|---|---|---|---|---|---|---|
| S9-R1-A-01 | 치명 | `backend/src/main/java/com/o2o/inventory/application/InventoryApplicationService.java:265`, `backend/src/main/java/com/o2o/shared/ClockConfiguration.java:23`, `backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java:63` | API 계약 준수, 테스트 격리와 재현성 | 명세는 숙박 날짜를 Asia/Seoul 기준으로 정했지만 구현과 테스트는 UTC 날짜를 사용한다. 한국 시각 00:00부터 08:59에는 서울 기준으로 이미 지난 날짜를 등록하거나 수정할 수 있다. | `document/11-o2o-api-spec.md` 35행은 숙박 날짜의 Asia/Seoul 기준을 명시한다. 서비스 266행은 `ZoneOffset.UTC`, Clock 설정 25행은 `Clock.systemUTC()`, API 테스트 64행은 `LocalDate.now(ZoneOffset.UTC)`를 사용한다. 구현과 테스트가 같은 잘못된 기준을 공유하므로 테스트가 오류를 잡지 못한다. | Instant용 Clock은 유지하되 오늘 계산에 `ZoneId.of("Asia/Seoul")`을 사용한다. UTC와 서울 날짜가 갈리는 고정 시각 테스트를 추가한다. |
| S9-R1-A-02 | 보통 | `backend/src/main/java/com/o2o/inventory/infrastructure/DailyInventoryJpaRepository.java:30`, `backend/src/main/java/com/o2o/inventory/infrastructure/DailyRateJpaRepository.java:25`, `backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java:288` | 계약 하강, 동시성과 트랜잭션 | 비관적 락 구현은 있지만 실제 MySQL에서 두 트랜잭션이 경합하는 검증이 없다. 현재 테스트는 지난 version을 순차 호출로만 검사한다. | 재고와 요금 저장소에는 각각 `PESSIMISTIC_WRITE`가 있다. 그러나 대상 테스트 63건에는 동시 요청, 락 대기, 잠금 후 상태 재검사 시나리오가 없다. 계약 D-2가 선택한 비관적 락 검증이 테스트까지 내려가지 않았다. | 재고와 요금 수정 각각에 두 트랜잭션을 동시에 실행하는 MySQL 통합 테스트를 추가한다. 두 번째 요청이 잠금 이후 최신 상태를 읽고 VERSION_CONFLICT 또는 불변식 오류로 끝나는지 확인한다. |
| S9-R1-A-03 | 보통 | `backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java:243`, `backend/src/test/java/com/o2o/inventory/api/InventoryQueryApiTest.java:245` | 계약 하강 | V9가 재고 등록과 수정, 요금 등록, 재고 조회만 검사한다. RATE-02의 과거 날짜 수정 거부와 RATE-03, RATE-04의 과거 날짜 조회 허용은 테스트가 없다. | `InventoryApiTest`의 V9는 재고 등록, 재고 수정, 요금 등록까지만 호출한다. `InventoryQueryApiTest`의 과거 조회는 재고 기간과 재고 단건만 호출한다. | 과거 날짜 RATE-02가 400 INVALID_DATE_RANGE인지, 과거 RATE-03과 RATE-04가 정상 조회되는지 실제 포트 테스트를 추가한다. |
| S9-R1-A-04 | 확인필요 | `backend/src/main/java/com/o2o/inventory/api/InventoryController.java:50`, `backend/src/main/java/com/o2o/inventory/api/RegisterInventoryRequest.java:19`, `backend/src/main/java/com/o2o/inventory/api/RegisterRateRequest.java:20` | API 계약 준수 | 명세는 정의되지 않은 JSON, Path, Query 필드를 400으로 거부한다. 허용 입력에서는 알 수 없는 JSON 필드와 추가 Query 파라미터를 거부하는 전역 설정이나 테스트가 확인되지 않는다. | 대상 DTO와 컨트롤러에는 개별 필드 검증만 있고, 전달된 HTTP 기록에도 추가 필드 요청이 없다. 전역 Jackson 및 MVC 설정은 허용 입력 밖이라 읽지 않았다. | 등록 요청에 알 수 없는 JSON 필드, 조회 요청에 알 수 없는 Query 필드를 넣어 400 INVALID_REQUEST를 확인하는 실제 포트 테스트를 추가한다. |

Repository는 DailyInventory와 DailyRate 애그리거트 루트에만 정의됐다. domain에서 Spring, infrastructure, api, application으로 향하는 import도 없었다. 이 Task의 9개 API에는 Idempotency-Key가 적용되지 않으므로 멱등 축은 해당 없음이다.

## 실행 기록

| 명령 | 목적 | 종료 상태 | 결과 요약 | 직접 실행 여부 |
|---|---|---|---|---|
| `node harness/out/task-S9-inventory-rate-R1/verify-eval-workspace.mjs` | 작업공간 검증 | 최초 실패 | 샌드박스가 내부 git 실행을 EPERM으로 막았다. | 직접 실행 |
| 같은 작업공간 검증 명령 재실행 | 실제 작업공간 검증 | PASS | 권한 확장 재실행에서 `ws.git`, `ws.commit`, `ws.agents`, `ws.target-count`, `ws.target-exists`, `ws.target-drift`, `ws.clean`, `ws.doc-hash` 8건이 모두 PASS였다. 금지된 `step9-verification.md` 존재 알림을 확인하고 해당 파일은 읽지 않았다. | 직접 실행 |
| `docker compose -f backend/docker-compose.yml up -d` | 테스트 DB 실행 | 최초 실패 | 샌드박스에서 Docker Desktop 파이프 접근이 거부됐다. | 직접 실행 |
| 같은 Docker 명령 재실행 | 테스트 DB 실행 | 성공 | `o2o-catalog-mysql` 컨테이너가 Running 상태였다. | 직접 실행 |
| `JAVA_HOME=<JDK 21> ./backend/gradlew.bat -p backend test`에 해당하는 `./backend/gradlew.bat -p backend test` | 전체 테스트 | 최초 실패 | 사용자 Gradle 캐시의 잠금 파일 접근이 거부됐다. | 직접 실행 |
| 같은 Gradle 명령 재실행 | 전체 테스트 | BUILD SUCCESSFUL | `test UP-TO-DATE`였다. 이번 명령에서 테스트 프로세스는 새로 돌지 않았다. | 직접 실행 |
| `node harness/tools/check.mjs g1 backend/src/main/java/com/o2o/inventory --type code --artifact backend/build/test-results/test/TEST-com.o2o.inventory.api.InventoryApiTest.xml` | 게이트 | 실패 | 단일 파일을 받는 소스 인자에 디렉터리를 넣어 EISDIR로 실패했다. | 직접 실행 |
| `node harness/tools/check.mjs g1 backend/src/main/java/com/o2o/inventory/application/InventoryApplicationService.java --type code --artifact backend/build/test-results/test/TEST-com.o2o.inventory.api.InventoryApiTest.xml` | 게이트 | PASS | 게이트 6개 검사 통과. JUnit XML의 테스트 22건, 실패 0, 오류 0, 건너뜀 0을 확인했다. | 직접 실행 |
| step5 JUnit XML | 5단계 결과 확인 | 통과 주장 확인 | 79건, 실패 0, 오류 0이었다. | 생성자 전달 |
| step6 JUnit XML과 `http-calls.txt` | 등록과 수정 결과 확인 | 통과 주장 확인 | 106건, 실패 0, 오류 0이었다. 실제 서버 응답으로 등록과 수정의 주요 성공과 오류 경로를 확인했다. | 생성자 전달 |
| step6-2 JUnit XML과 `http-calls.txt` | 조회 결과 확인 | 통과 주장 확인 | 118건, 실패 0, 오류 0이었다. 조회 4개와 missingDates 응답을 확인했다. | 생성자 전달 |
| 동시 요청, 추가 JSON 및 Query 필드 호출 | 누락 경계 확인 | 미실행 | 승인 명령에 서버 기동과 임의 HTTP 호출이 없어 실행하지 않았다. | 미실행 |

직접 Gradle 명령은 UP-TO-DATE였으므로 새 테스트 실행으로 보지 않았다. 통과 근거는 생성자가 전달한 JUnit, 현재 JUnit 산출물, 코드 대조이며 직접 실행 결과와 구분했다.

## 검증 ID 결과

| ID | 통과 / 실패 / 미실행 | 근거 |
|---|---|---|
| T03 | 통과 | 전달된 `InventoryApiTest`의 V5가 기존 날짜 포함 시 409와 신규 날짜 미저장을 확인했다. `openInventories`도 한 트랜잭션에서 사전 중복 검사 후 `saveAll`을 수행한다. 직접 Gradle 명령에서는 test가 UP-TO-DATE여서 새로 실행되지는 않았다. |
| T04 | 통과 | 전달된 API 및 MySQL 통합 테스트가 soldCount보다 낮은 totalCount 수정의 409와 기존 수량 유지를 확인했다. 애그리거트는 `totalCount < soldCount + heldCount`를 검사한다. |
| T05 | 통과 | 전달된 실제 포트 테스트가 재고와 요금 모두 지난 version에 409 VERSION_CONFLICT가 나고 최근 변경이 유지되는 것을 확인했다. 비관적 락 자체의 경합 검증 누락은 S9-R1-A-02로 분리했다. |
| T06 | 미실행 | 체크아웃 날짜 제외를 예약까지 연결하는 경로가 이번 Task 범위 밖이다. 날짜 범위의 to 제외 구현은 확인했지만 예약 가능 여부는 실행하지 않았다. |
| T07 | 미실행 | 재고와 요금의 누락 날짜 구분과 missingDates 응답은 확인했다. Booking과 Hold가 생성되지 않는지까지 확인하는 예약 경로는 범위 밖이라 실행하지 않았다. |

## 실제로 읽은 파일과 SHA256

### 평가 지침과 허용 문서

| 파일 | SHA256 |
|---|---|
| `AGENTS.md` | `6cf0a192aab3bba8170f5a487197412793247864de77b99719fcdab8c8562670` |
| `backend/AGENTS.md` | `132a0c23c5f109045f6f8793a3a00b83137f14ea3e2611eff5a063637abd0b9b` |
| `harness/out/task-S9-inventory-rate-R1/eval-request-A.md` | `83e86cba2db4335768caf06f048b5f726d6cf2034c4d5da107472803e0acb4b8` |
| `harness/out/task-S9-inventory-rate-R1/eval-target-files.md` | `807b6239259b410748c92c239725bc040eb8b18fd4d8a1407ec916d0864b7822` |
| `harness/project-sync/o2o-review-input-pack.md` | `1608942c0815f9113ef022e74131743bdca3cb30cfaebb277cb8e038cf0dc645` |
| `harness/tasks/task-S9-inventory-rate.md` | `896a9868f73b3eed79edfbdff0d398e6c7dc7c18400577447a8862c5dff11f16` |
| `harness/prompts/eval-criteria-code.md` | `c5ed835951fe09a5c015d50abbbacf9755af6ba8be807e1be1b9c0ba2cb1553d` |
| `document/11-o2o-api-spec.md` | `3f2613a77b6499039b6d8b2310504d7c37047005ae028f6568455472c1ea105b` |
| `document/06-2-o2o-aggregates.md` | `113c6734b5525e1d5a5b2116de1250232707bc0518b3470d133ec0b7c993cec9` |
| `document/06-4-o2o-contracts.md` | `edacbe47d6d63e3f2b72849234a0a9dbd4dbd297a8eee7b1ea19d70ab5e3c3e3` |

### 프로덕션 코드 43개

| 파일 | SHA256 |
|---|---|
| `backend/src/main/java/com/o2o/inventory/api/AdjustInventoryRequest.java` | `8f0eda564fc2d3cc073317761ca9461d4854767c348b4350e6973fe441f6f90a` |
| `backend/src/main/java/com/o2o/inventory/api/AdjustRateRequest.java` | `c2dbdf5f93824c4708bd55405f588228a13950eaee9b625631af6dfca2f04821` |
| `backend/src/main/java/com/o2o/inventory/api/ApiDate.java` | `da20472be8f2f6715f46f30f0587411fb91616761e2b8e83c659c970cf7ee802` |
| `backend/src/main/java/com/o2o/inventory/api/BulkInventoryRequest.java` | `d055a9179796b4a5013cafa217e5079910afef795609676c5e370ac8837e8c8c` |
| `backend/src/main/java/com/o2o/inventory/api/DailyInventoryResponse.java` | `3df7642751873191fbd2da7fb713972265e8139e28284973ff9da61d68f998eb` |
| `backend/src/main/java/com/o2o/inventory/api/DailyRateResponse.java` | `96d5bfb4cb351f0f395c7305ad2ffc0a3efb201bd82ac5cfe77f7cd78679733e` |
| `backend/src/main/java/com/o2o/inventory/api/InvalidDateFormatException.java` | `c69036956c51688cecdd7ff241080cc314491a65de7bc162f7d3f876cb2ab47a` |
| `backend/src/main/java/com/o2o/inventory/api/InventoryController.java` | `f0a08366cec320ea26c086a4b8e90e2cd866b8aeee0254d729fa7c4e775ae013` |
| `backend/src/main/java/com/o2o/inventory/api/InventoryExceptionHandler.java` | `78347a957a155424b811cfb7c0b2b18aec3ce67455b0121f8a5ba900317e6fff` |
| `backend/src/main/java/com/o2o/inventory/api/InventoryRangeResponse.java` | `96828441054299d8ead7412acf900107c5b998b5f44886b8236053f03d9511c4` |
| `backend/src/main/java/com/o2o/inventory/api/package-info.java` | `7a9f269b39dbfe23615d7f927961cc573ec6770487171d2275727581888785f3` |
| `backend/src/main/java/com/o2o/inventory/api/RateController.java` | `224f206649642d99cb3b86978a0dedb1a517c0b51017b379914dbb3de3877aa8` |
| `backend/src/main/java/com/o2o/inventory/api/RateRangeResponse.java` | `56ce5218de831b8ce0b3f6c49f5c87fa121aa186b0e706c8bb4ef80913930844` |
| `backend/src/main/java/com/o2o/inventory/api/RegisterInventoryRequest.java` | `c787c852cd89bb45b6914bd0517028732030d6300f512cfc7ace661d5e4a09a7` |
| `backend/src/main/java/com/o2o/inventory/api/RegisterRateRequest.java` | `db7a133d655bd37ae3b6254eb878d4026e4d4ce5d8240b91691b7783c3f52561` |
| `backend/src/main/java/com/o2o/inventory/application/InventoryApplicationService.java` | `2521519434ff1d9145855d785091dc02a5a1fa2a1bf34c93065fffa5457a0892` |
| `backend/src/main/java/com/o2o/inventory/application/package-info.java` | `15492150e9db1dcd6bf63e22034ab9cbc7b0e7b6ff5df3382bc951973cb3e98a` |
| `backend/src/main/java/com/o2o/inventory/application/RangeResult.java` | `2207e7f5fdc14d5ede83e7390e43ba85ffe7446ea9296638bd7e25266cf11c67` |
| `backend/src/main/java/com/o2o/inventory/domain/DailyInventory.java` | `8cae2bc36ba373461a283e7c91d23df2a54bf5b6a548103ded71620b77b39d96` |
| `backend/src/main/java/com/o2o/inventory/domain/DailyInventoryRepository.java` | `36cc75997b2a49a344a06dbe99ac76c5cdc9137a6e89b6e32895dfc9b46e85c7` |
| `backend/src/main/java/com/o2o/inventory/domain/DailyRate.java` | `8b9c33c0946e7954bc42be28aa34935ea0ccc647f0ad90cb877a973fd70bad08` |
| `backend/src/main/java/com/o2o/inventory/domain/DailyRateRepository.java` | `68af292d2bb7ccab19ac24392a0e5f708d8c2890ab03cf897b75e7ae369768d0` |
| `backend/src/main/java/com/o2o/inventory/domain/DuplicateInventoryException.java` | `b7fa9beb00e5d3ccf20032e48d2bc2f6a46882657d16e75a58d9d8b136c0a2b8` |
| `backend/src/main/java/com/o2o/inventory/domain/DuplicateRateException.java` | `dc805da046ea00d8358d873458b7ba90d25fe0ec7dc6f30880cfb9ebbff9db6f` |
| `backend/src/main/java/com/o2o/inventory/domain/InvalidRateException.java` | `a6f366072a2cadfcb7e2f703caa2baf980025512043749ca551d52149855a4c2` |
| `backend/src/main/java/com/o2o/inventory/domain/InvalidStayPeriodException.java` | `0f0bcc58eca8fc2ae12ab7e16da0d6318b182c9afbcf3895524dd7d75491ca72` |
| `backend/src/main/java/com/o2o/inventory/domain/InventoryAdjusted.java` | `96d7cd154045b4cb14bcd57ceae572766a1a35f2f7f140be8301f6a37fcbcc06` |
| `backend/src/main/java/com/o2o/inventory/domain/InventoryBelowOccupiedException.java` | `e577ed2e89d3f113ab13c81a004ea6e9754aa5c350c0dc1c5d113781cbe46203` |
| `backend/src/main/java/com/o2o/inventory/domain/InventoryCountBelowZeroException.java` | `cb74c51dd45eabb51d770742efdcc0671fcf90836a050f5ca813fa7c62e52057` |
| `backend/src/main/java/com/o2o/inventory/domain/InventoryNotFoundException.java` | `963598dabcc6f7a658f9823dcb41c5ee3ec96f58c55403f29a228fee7efefb13` |
| `backend/src/main/java/com/o2o/inventory/domain/InventoryOpened.java` | `a909d55175984da823ca24ba85f00e63e03af77334ac3361c91396306188945e` |
| `backend/src/main/java/com/o2o/inventory/domain/package-info.java` | `43834f2fbfda2597c854f1dfd4b3aff9d21d830c691caca220bc0a6847d78e70` |
| `backend/src/main/java/com/o2o/inventory/domain/PastStayDateException.java` | `1330088ad6245cac1325c4ffb321f1cadd7261171be7d243e82b7ce5297a2b97` |
| `backend/src/main/java/com/o2o/inventory/domain/RateAdjusted.java` | `1be142a22db6a199116afccacb49d92b2e7db85a950a7c4e62beb2b50d7b84bb` |
| `backend/src/main/java/com/o2o/inventory/domain/RateNotFoundException.java` | `1492c5dbe2ddfd79c6662a3c5c3c5f3c494876f1d521d4071aa492585146575b` |
| `backend/src/main/java/com/o2o/inventory/domain/RateRegistered.java` | `75a02bce7edd10d226312b58a33ddd2ef157286647da55388814fddb195f4b5d` |
| `backend/src/main/java/com/o2o/inventory/infrastructure/DailyInventoryJpaRepository.java` | `d3b0600e1a57059e701ce1373fd2f6b4a8ad68b84ad6a1e116abb9a180bb5306` |
| `backend/src/main/java/com/o2o/inventory/infrastructure/DailyRateJpaRepository.java` | `ebcdfe8e8bb0c483a9820a5132e2b19203652acb7c141070c1884327f94b5fa6` |
| `backend/src/main/java/com/o2o/inventory/infrastructure/JpaDailyInventoryRepository.java` | `f6fa5af20cf1a36bdcb39648c96f3241f21e49a7dd49d039a621009208931ecb` |
| `backend/src/main/java/com/o2o/inventory/infrastructure/JpaDailyRateRepository.java` | `da9cf722f53cba91037447627e6d76c2ab1a7aba7d965aa7016a312a169f551a` |
| `backend/src/main/java/com/o2o/inventory/infrastructure/package-info.java` | `326f0999de203671c7b4c8b5819443cd209657ec23717cfe3b256196f4cda4a7` |
| `backend/src/main/java/com/o2o/shared/ClockConfiguration.java` | `9578695bbe136265534e9ec0673e1ec76ae9ba1739a35f508a845ef992d2ac05` |
| `backend/src/main/java/com/o2o/shared/Money.java` | `be20a03c3ac01c87cdf6cc57f96045863d94f793d50586be4ef1f7eb7efafc02` |

### 테스트 코드 7개

| 파일 | SHA256 |
|---|---|
| `backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java` | `fefa52e9af563ac4e400b90374c62ea0972dec08cf62ad6c6b1603442c1189c5` |
| `backend/src/test/java/com/o2o/inventory/api/InventoryQueryApiTest.java` | `79a47aa1998dadbff1029bc23dd350314d94456495563b0c1d799c41c446c6e1` |
| `backend/src/test/java/com/o2o/inventory/application/InventoryApplicationServiceTest.java` | `05fc74e65926fb234dd228f3b0023706aa43c941a66b5193b6c2e1c27b1fc494` |
| `backend/src/test/java/com/o2o/inventory/application/InventoryEventTest.java` | `f3e8a109cf17606854ec406bedcd598eed91abd7876cb861943c7018a7bed76f` |
| `backend/src/test/java/com/o2o/inventory/domain/DailyInventoryTest.java` | `4445791f13e54a7ed9e1c5434f2d19ecd67bb35e4800b6e91792b509b6ef9008` |
| `backend/src/test/java/com/o2o/inventory/domain/DailyRateTest.java` | `7bf71388ed435eb3f1c86fde6b4590ed0ebee8d8e77668619f628aff5c7e9eaf` |
| `backend/src/test/java/com/o2o/shared/MoneyTest.java` | `d99981f767523ad2ab327687f83e498a6e96711d23d0f02b2615813ce89f999b` |

### 생성자가 전달한 실행 결과

#### step5

| 파일 | SHA256 |
|---|---|
| `step5/TEST-com.o2o.BackendApplicationTests.xml` | `262eeea55d622ed66e7df4672c96944dec0f1225f5e05b860a69ed9035b63b80` |
| `step5/TEST-com.o2o.catalog.api.CatalogApiTest.xml` | `42f732ac8e70ac781a2ee69e8efd326a4a6e9afde1846af5504709e614a131e5` |
| `step5/TEST-com.o2o.catalog.api.CatalogUpdateAndListApiTest.xml` | `edfc11b56e3d808a70b39246500008b8616034477677a7aac50d88496af98c89` |
| `step5/TEST-com.o2o.catalog.application.CatalogApplicationServiceTest.xml` | `e1bcd02cd56a2de456f966cf4863ee1c784e982a2eea674f15d26b240568db0f` |
| `step5/TEST-com.o2o.catalog.domain.RoomTypeTest.xml` | `7e0c83f29e7849b5585ea862335b4816adc1a14c32d919ba503f52db1608a40c` |
| `step5/TEST-com.o2o.DatabaseConnectionTest.xml` | `c62d7b7442fc341a50f00ccfc562c22a6acb613cafbf525d9aa65f58cd113efa` |
| `step5/TEST-com.o2o.inventory.application.InventoryApplicationServiceTest.xml` | `9c0d7a35ef806a0ec570e4d85ec8231ab7eddcf0d6c46e19ee38991e45bcff83` |
| `step5/TEST-com.o2o.inventory.domain.DailyInventoryTest.xml` | `fa8c09e615fc7f845a1e6f6eddce5e861f931ea23fa08aa7e5b09d99530ffd78` |
| `step5/TEST-com.o2o.inventory.domain.DailyRateTest.xml` | `b12fa6200567339c6c805e8677399020d56c0c934f502e669142be5fba19140d` |
| `step5/TEST-com.o2o.shared.MoneyTest.xml` | `41266953e2f31830fd70e27778ad61b72fac7890b404252f0f06d7c1865009a2` |

#### step6

| 파일 | SHA256 |
|---|---|
| `step6/http-calls.txt` | `5c1d6b30a06d2803503b3cb897080664753e0bee223a2f6aa41be688eebf01ee` |
| `step6/TEST-com.o2o.BackendApplicationTests.xml` | `a395a04648c8e4fc98220a7bcaf29aa268f0c297ce69c348e97f5afbebff7c42` |
| `step6/TEST-com.o2o.catalog.api.CatalogApiTest.xml` | `b82a68a06c09c454ceaa7be0c8312e2b1f913ce598847759e021058cfba4713f` |
| `step6/TEST-com.o2o.catalog.api.CatalogUpdateAndListApiTest.xml` | `27b7c4946c6890ff823f376aff685466fb5e1051e390532f139c7abeca4cb764` |
| `step6/TEST-com.o2o.catalog.application.CatalogApplicationServiceTest.xml` | `59e2463221ccd65f19d9cf893e4c99ddbde33bc9a68d1cd2f9a498669472d58c` |
| `step6/TEST-com.o2o.catalog.domain.RoomTypeTest.xml` | `521ad8677c6cc737f78728ae5221608a5862a28ee9308094e85464c7c1d97c21` |
| `step6/TEST-com.o2o.DatabaseConnectionTest.xml` | `70908292019246de606f85912bbc97d03e1d59a9c34d1e7985af39e777637da4` |
| `step6/TEST-com.o2o.inventory.api.InventoryApiTest.xml` | `09a892f78e0df77ebaaa5350e946eb5cbcb4a8e085049a438ab312b03136e672` |
| `step6/TEST-com.o2o.inventory.application.InventoryApplicationServiceTest.xml` | `1141209ef51aa716acb53078f756d34d8d26fd79c6e668f93d2bd8f06b2c5d52` |
| `step6/TEST-com.o2o.inventory.application.InventoryEventTest.xml` | `1e005722b43cf7b7284285ec4a7dbbc588aceee5bc5e861a530599f2fa8a4c87` |
| `step6/TEST-com.o2o.inventory.domain.DailyInventoryTest.xml` | `b4cebf70ae146e7b64d9be24ce96dfa0e8251e8fbbeebd106fecb53ac16a5a4e` |
| `step6/TEST-com.o2o.inventory.domain.DailyRateTest.xml` | `be286e21479a0d2dca00f342a29d7336f3fbf6df4eba5d146bfe27501c0f3ecf` |
| `step6/TEST-com.o2o.shared.MoneyTest.xml` | `94254134c07948766cb7732c85e42571d408fcda0a45ec80137028a7a976c6f6` |

#### step6-2

| 파일 | SHA256 |
|---|---|
| `step6-2/http-calls.txt` | `b1656fc8d0d00047ec6ece73599254350421fbae75053687c5c68654a131146c` |
| `step6-2/TEST-com.o2o.BackendApplicationTests.xml` | `885bfbc5f37aea4f6e139dab376945eb1befbe874bf125c1df2abc3d08d42ad6` |
| `step6-2/TEST-com.o2o.catalog.api.CatalogApiTest.xml` | `8553a621571cd9fe1f6c1bee42bd40052b4749bfd9e8b0c3492b65d8ad606937` |
| `step6-2/TEST-com.o2o.catalog.api.CatalogUpdateAndListApiTest.xml` | `02487852dc904ecfb1f494177da4d7755804369515f0ca84eea03b244683513a` |
| `step6-2/TEST-com.o2o.catalog.application.CatalogApplicationServiceTest.xml` | `dae6bf42252d867549f89e124331465c9375dd9df6c8f794d9cd9f0cd83c63f0` |
| `step6-2/TEST-com.o2o.catalog.domain.RoomTypeTest.xml` | `9ee8ebd302b7d80735a8610b52eeb8eb8a583560556091ac8a1768206f16f222` |
| `step6-2/TEST-com.o2o.DatabaseConnectionTest.xml` | `53990d008932e4e0e8431ab8c860cf2b2d9bd39ef2a3238a75f7fe5560296783` |
| `step6-2/TEST-com.o2o.inventory.api.InventoryApiTest.xml` | `d69acf3b0fd62a94eed892b0401f3db9b423d63437ccdbd7485122686e02888a` |
| `step6-2/TEST-com.o2o.inventory.api.InventoryQueryApiTest.xml` | `aab53f028b469275fcb90dd32b662072f4790e8555b7c96d35d0717ec37dcad6` |
| `step6-2/TEST-com.o2o.inventory.application.InventoryApplicationServiceTest.xml` | `5c98a7ff451ea172239c30d2b32f1bad290e2f79301c2fd0974beca620846cff` |
| `step6-2/TEST-com.o2o.inventory.application.InventoryEventTest.xml` | `0b8ee042ea089839398d35d44414abce33e6357ff4ea4f1c987bfb65b0254f50` |
| `step6-2/TEST-com.o2o.inventory.domain.DailyInventoryTest.xml` | `2947c4d04b4ff042b97f8a23fc705e62100e7efadf739aa4196feb808a71b23f` |
| `step6-2/TEST-com.o2o.inventory.domain.DailyRateTest.xml` | `0e1b8a94ac0061261a3e0a0c4548444fb7d0d6bf796693a9454de21bb157fd69` |
| `step6-2/TEST-com.o2o.shared.MoneyTest.xml` | `e0add7a57750acbfbfdbdd559777c9823650bec9419300a937475c5b401398d3` |

### 직접 실행 시 확인한 현재 JUnit 산출물

| 파일 | SHA256 |
|---|---|
| `backend/build/test-results/test/TEST-com.o2o.inventory.api.InventoryApiTest.xml` | `6a41b5a69cab50af56f6890d4929c89114e83b5afdf8c59d2d90086ddcfd427a` |
| `backend/build/test-results/test/TEST-com.o2o.inventory.api.InventoryQueryApiTest.xml` | `95527a06b86290c5b730e3bbdf598c7ad996ea581abc0b9a79a54a1fc9364470` |
| `backend/build/test-results/test/TEST-com.o2o.inventory.application.InventoryApplicationServiceTest.xml` | `807460900bf95680aac5a5f2667dc0815bc4420990651da54474de1634db7a7b` |
| `backend/build/test-results/test/TEST-com.o2o.inventory.application.InventoryEventTest.xml` | `1785b308021a4aca37aedfa27ef197605d0c883a0a9cf6107ed87fc0085e22bd` |
| `backend/build/test-results/test/TEST-com.o2o.inventory.domain.DailyInventoryTest.xml` | `1516cda34fbf1460f833e0584556f01ac5029b8e0ce60ffd6d7fec2cb61b8b50` |
| `backend/build/test-results/test/TEST-com.o2o.inventory.domain.DailyRateTest.xml` | `b0789046a7f308a66ed769289bb0af0dd4283697246355521b804ce647fe8d58` |
| `backend/build/test-results/test/TEST-com.o2o.shared.MoneyTest.xml` | `d752b97e1fd4d30ba7329c9e631c29ef89527498429a4132fe99b8e33be5a101` |
