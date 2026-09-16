# task-S9-inventory-rate R1 평가자 B 리포트

평가일: 2026-09-10

## 평가 경계

- 역할: 평가자 B
- 담당 축: 추적성 양방향, 결정 근거의 자립성, 요구사항 역추적
- 평가 대상: 계약에 지정된 프로덕션 코드 43개와 테스트 코드 7개
- 제외: `harness/docs/`, `harness/state/`, 이전 평가 리포트, 사용자 결정표, 생성 대화, `step9-verification.md`
- 실행 결과 파일은 읽지 않았다.
- 저장소의 소스와 테스트는 수정하지 않았다.

## 판정 요약

치명 1 / 보통 1 / 확인필요 2

## 상세

| ID | 심각도 | 위치(파일:행) | 위반 축 | 문제 | 근거(코드 또는 실행 결과) | 수정 제안 |
|---|---|---|---|---|---|---|
| S9-R1-B-01 | 치명 | `backend/src/main/java/com/o2o/inventory/application/InventoryApplicationService.java:265`, `backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java:63` | 추적성 양방향, 결정 근거의 자립성 | 명세는 숙박 날짜를 Asia/Seoul 기준으로 정했지만 구현과 테스트는 UTC 날짜를 사용한다. 한국 시각 00:00부터 08:59에는 서울 기준으로 이미 지난 날짜를 등록하거나 수정할 수 있다. | `document/11-o2o-api-spec.md` 35행과 P03은 숙박 날짜의 Asia/Seoul 기준을 명시한다. 서비스 266행은 `ZoneOffset.UTC`, API 테스트 64행은 `LocalDate.now(ZoneOffset.UTC)`를 사용한다. | 오늘 계산을 `ZoneId.of("Asia/Seoul")` 기준으로 바꾸고 UTC 날짜와 서울 날짜가 갈리는 시각의 경계 테스트를 추가한다. |
| S9-R1-B-02 | 보통 | `backend/src/main/java/com/o2o/inventory/domain/DailyInventory.java:123`, `backend/src/test/java/com/o2o/inventory/domain/DailyInventoryTest.java:33` | 요구사항 역추적 | I1a 검사는 코드에 있지만 `soldCount < 0`과 `heldCount < 0`을 직접 확인하는 테스트가 없다. 계약은 V2를 다음 묶음으로 이월했지만 이번 평가 요청은 불변식 셋 모두를 어느 테스트가 확인하는지 짚도록 요구한다. | `DailyInventory.validateCounts` 124행에 가드가 있다. `DailyInventoryTest`의 7개 테스트는 totalCount와 version만 바꾸며 soldCount와 heldCount의 음수 경계를 만들지 않는다. | I1a를 이번 묶음의 역추적 대상에서 제외한다고 계약과 평가 요청을 맞추거나, 실제 수량 변경 경로가 생기는 묶음에서 대응 테스트를 필수로 연결한다. |
| S9-R1-B-03 | 확인필요 | `backend/src/main/java/com/o2o/inventory/application/InventoryApplicationService.java:102` | 추적성 양방향 | 06-2는 이벤트를 커밋 후 발행한다고 명시하지만 대상 코드는 트랜잭션 메서드 안에서 `publishEvent`를 호출한다. 허용 입력에는 구독자와 전달 설정이 없어 실제 처리가 커밋 후로 지연되는지 판정할 수 없다. | `document/06-2-o2o-aggregates.md` 85행부터 94행은 커밋 후 발행을 명시한다. 서비스는 102, 138, 159, 178, 195행에서 직접 발행한다. 대상 코드에는 커밋 후 전달을 보장하는 장치가 없다. | 구독자와 이벤트 전달 설정을 평가 입력에 포함하고, 롤백 시 외부 효과가 발생하지 않는 테스트를 추가한다. |
| S9-R1-B-04 | 확인필요 | `backend/src/main/java/com/o2o/inventory/api/RegisterInventoryRequest.java`, `backend/src/main/java/com/o2o/inventory/api/RegisterRateRequest.java` | 추적성 양방향 | 명세는 정의되지 않은 JSON 필드를 400으로 거절한다. 대상 DTO와 테스트에서는 이를 확인할 수 없고 공통 Jackson 설정은 허용 입력 밖이다. | `document/11-o2o-api-spec.md` 38행과 74행은 미정의 필드 거절을 요구한다. 대상 API 테스트에는 미정의 필드 요청이 없다. | 공통 역직렬화 설정을 다음 평가 입력에 포함하거나 미정의 필드가 400인지 실제 포트 테스트로 고정한다. |

## 요구사항 역추적표

| 요구사항 | 대응 테스트 또는 코드 | 커버 여부 |
|---|---|---|
| R1 연박 전체 선점 실패 | 예약과 선점 경로가 이번 Task 범위 밖 | 해당 없음 |
| R2 동시 요청 초과 예약 0 | 예약과 선점 경로가 이번 Task 범위 밖 | 해당 없음 |
| R3 동일 요청 중복 Booking 방지 | Booking과 멱등 예약 경로가 이번 Task 범위 밖 | 해당 없음 |
| R4 HELD 종료 시 재고 반환 | HELD, 만료, 반환 경로가 이번 Task 범위 밖 | 해당 없음 |
| R5 확정 금액 불변 | Booking 가격 스냅샷이 이번 Task 범위 밖 | 해당 없음 |
| INV-01 | `InventoryApiTest` 134, 155, 166행 | 커버 |
| INV-02 | `InventoryApiTest` 178, 195, 218, 233행 | 커버 |
| INV-03 | `InventoryApiTest` 273, 288, 306, 323행 | 커버 |
| INV-04 | `InventoryQueryApiTest` 123, 146, 183, 246, 266, 283행 | 커버 |
| INV-05 | `InventoryQueryApiTest` 198, 215, 246행 | 커버 |
| RATE-01 | `InventoryApiTest` 352, 369, 379행 | 커버 |
| RATE-02 | `InventoryApiTest` 391, 406행 | 커버 |
| RATE-03 | `InventoryQueryApiTest` 166행 | 부분 커버 |
| RATE-04 | `InventoryQueryApiTest` 227행 | 커버 |
| I1 | `DailyInventoryTest` 34, 84, 94행과 `InventoryApplicationServiceTest` 80, 99행 | 커버 |
| I1a | `DailyInventory.validateCounts`에 검사만 존재 | 미커버 |
| I2 | `DailyRateTest` 29, 35, 41, 61행 | 커버 |

## 실행 기록

| 명령 | 목적 | 종료 상태 | 결과 요약 | 직접 실행 여부 |
|---|---|---|---|---|
| `node harness/out/task-S9-inventory-rate-R1/verify-eval-workspace.mjs` | 작업공간 검증 | 최초 실패, 재실행 성공 | 최초 실행은 sandbox의 `git EPERM`으로 실패했다. 권한 확장 재실행에서 `ws.git`, `ws.commit`, `ws.agents`, `ws.target-count`, `ws.target-exists`, `ws.target-drift`, `ws.clean`, `ws.doc-hash` 8건이 모두 PASS였다. | 직접 실행 |
| `docker compose -f backend/docker-compose.yml up -d` | 테스트 DB 실행 | 미실행 | 평가자 B는 문서와 코드의 정적 대조만 수행하도록 요청됐다. | 미실행 |
| `JAVA_HOME=<JDK 21> ./backend/gradlew.bat -p backend test` | 테스트 실행 | 미실행 | 평가자 B는 문서와 코드의 정적 대조만 수행하도록 요청됐다. | 미실행 |
| 전달받은 실행 결과 | 기존 결과 대조 | 사용하지 않음 | 실행 결과 파일과 `step9-verification.md`를 읽지 않았다. | 전달 결과 없음 |

평가자 B 요청에 따라 검증 ID 결과표는 넣지 않았다.

## 실제로 읽은 파일과 SHA256

### 평가 지침과 허용 문서

| 파일 | SHA256 |
|---|---|
| `AGENTS.md` | `6cf0a192aab3bba8170f5a487197412793247864de77b99719fcdab8c8562670` |
| `backend/AGENTS.md` | `132a0c23c5f109045f6f8793a3a00b83137f14ea3e2611eff5a063637abd0b9b` |
| `harness/out/task-S9-inventory-rate-R1/eval-request-B.md` | `5b3c1dd25759bd46cb4018fbbb590e4c75e32419da57f093ef003e775903b46a` |
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
