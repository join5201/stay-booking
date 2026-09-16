# task-S9-inventory-rate 평가 대상 코드 목록

최초 작성: 2026-09-10
최종 갱신: 2026-09-14 (6절 main 기준 목록 신설. 라운드 mvp-eval-2026-09-14 R2)

계약 5절 평가 대상 코드 행이 가리키는 파일이다. 계약 표 한 칸에 쉰 줄을 넣을 수 없어 목록을 여기 둔다.

## 1. 버전

| 항목 | 값 |
|---|---|
| 브랜치 | feat/task-s9-inventory-rate |
| 기준 커밋 | 280a69163a6a64225bb414969a134af22d5cefbe |
| 앞 묶음과 갈라진 지점 | 8e40435 |

이 Task의 backend 커밋은 일곱이다.

| 해시 | 무엇 | 단계 |
|---|---|---|
| 5db25b2 | Clock 빈을 catalog에서 shared로 | 4 |
| 75e850c | 재고와 요금 도메인 코드 | 4 |
| 18b65eb | 불변식과 유일성 테스트 24건 | 5 |
| 3871bdc | Clock 빈의 package 선언 누락분 | 6 앞 |
| bba1a22 | 등록과 수정 API 다섯과 테스트 27건 | 6 |
| fa87349 | 조회 API 넷과 테스트 12건 | 6-2 |
| 3ed5aee | backend의 CLAUDE.md와 AGENTS.md | Task 밖 |

마지막 하나는 평가 대상이 아니다. 코드가 아니라 에이전트 규칙 문서다.

## 2. 프로덕션 코드 43개

경로는 backend/src/main/java/com/o2o/ 아래다.

### 2-1. inventory/domain 18개

DailyInventory.java, DailyInventoryRepository.java, DailyRate.java, DailyRateRepository.java, DuplicateInventoryException.java, DuplicateRateException.java, InvalidRateException.java, InvalidStayPeriodException.java, InventoryAdjusted.java, InventoryBelowOccupiedException.java, InventoryCountBelowZeroException.java, InventoryNotFoundException.java, InventoryOpened.java, PastStayDateException.java, RateAdjusted.java, RateNotFoundException.java, RateRegistered.java, package-info.java

### 2-2. inventory/application 3개

InventoryApplicationService.java, RangeResult.java, package-info.java

### 2-3. inventory/api 15개

AdjustInventoryRequest.java, AdjustRateRequest.java, ApiDate.java, BulkInventoryRequest.java, DailyInventoryResponse.java, DailyRateResponse.java, InvalidDateFormatException.java, InventoryController.java, InventoryExceptionHandler.java, InventoryRangeResponse.java, RateController.java, RateRangeResponse.java, RegisterInventoryRequest.java, RegisterRateRequest.java, package-info.java

### 2-4. inventory/infrastructure 5개

DailyInventoryJpaRepository.java, DailyRateJpaRepository.java, JpaDailyInventoryRepository.java, JpaDailyRateRepository.java, package-info.java

### 2-5. shared 2개

Money.java는 이 Task가 만들었다. ClockConfiguration.java는 catalog/infrastructure/CatalogClockConfiguration.java에서 옮겨 왔고 그 근거는 06-1 4절이다.

## 3. 테스트 코드 7개

경로는 backend/src/test/java/com/o2o/ 아래다.

| 파일 | 건수 | 성격 |
|---|---|---|
| inventory/domain/DailyInventoryTest.java | 7 | 단위 |
| inventory/domain/DailyRateTest.java | 6 | 단위 |
| shared/MoneyTest.java | 4 | 단위 |
| inventory/application/InventoryApplicationServiceTest.java | 7 | MySQL 통합 |
| inventory/application/InventoryEventTest.java | 5 | MySQL 통합 |
| inventory/api/InventoryApiTest.java | 22 | 실제 포트 |
| inventory/api/InventoryQueryApiTest.java | 12 | 실제 포트 |

합계 63건이다. 저장소 전체는 118건이고 나머지 55건은 앞 묶음 것이다.

## 4. 평가 대상이 아닌 것

| 무엇 | 왜 |
|---|---|
| catalog 패키지 전부 | 앞 묶음 task-S9-catalog에서 평가받을 대상이다 |
| shared의 Actor 계열, PageQuery, PageResult, ErrorResponse, 식별자 셋 | 같음 |
| backend/CLAUDE.md와 backend/AGENTS.md | 코드가 아니다 |
| build.gradle, docker-compose.yml, 설정 파일 | 이 Task가 바꾸지 않았다 |

## 5. 실행 결과 파일

| 단계 | 경로 |
|---|---|
| 5 | harness/out/task-S9-inventory-rate-R1/step5/ |
| 6 | harness/out/task-S9-inventory-rate-R1/step6/ |
| 6-2 | harness/out/task-S9-inventory-rate-R1/step6-2/ |

각 폴더에 JUnit XML 사본이 있고 step6과 step6-2에는 살아 있는 서버에 친 http-calls.txt가 있다.

## 6. main 기준 목록 (2026-09-14 신설. 라운드 mvp-eval-2026-09-14의 R2 몫)

1절부터 5절은 R1 시점(280a691)의 기록이다. 이번 라운드의 대상은 이 절이다. 라운드 README 5절 D-3 가에 따라 R2는 축 전부를 다시 보고, 계약 5절 평가 대상 행의 해시는 이 절을 더한 뒤 값이다.

| 항목 | 값 |
|---|---|
| 브랜치 | main |
| 기준 커밋 | 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1 (PR 138 병합, 2026-09-13 23:53) |
| R1 뒤 이 묶음 파일을 고친 커밋 | 이 묶음의 PR 115, 예약 1차의 PR 127, 예약 2차의 PR 138. 파일 표의 고친 묶음 칸이 예약 몫을 가른다 |
| 뽑은 방식 | 패키지 단위. inventory 패키지 전체(예약 묶음이 더한 여덟 포함), shared 중 R1 목록에 있던 Money와 ClockConfiguration과 R1 뒤 이 묶음이 더한 SeoulDate. 테스트는 inventory 전체와 shared/MoneyTest. 표는 harness/out/mvp-eval-2026-09-14/build-target-lists.mjs inventory-rate 가 기준 커밋에서 낸 것이다 |

다른 묶음과의 관계. 예약 1차가 DailyInventory와 DailyInventoryRepository와 그 JPA 구현 둘에 hold 계열을 더하고 domain에 여섯(InventoryAllocationService, InventoryHeld, 예외 넷)을 더했다. 예약 2차가 InventoryCommitted와 InventoryReleased를 더하고 InventoryAllocationService를 고쳤다. 이 열둘과 테스트 둘(DailyInventoryAllocationTest, InventoryAllocationServiceTest)은 예약 쌍도 본다. ClockConfiguration은 숙소 쌍도 본다. 평가자는 지금 모양을 판정하고, 지적이 예약 몫에 떨어지면 그대로 적는다. 어느 묶음이 반영할지는 결정표가 가른다.

경로는 저장소 루트 기준이다. sha256은 앞 16자리이고 기준 커밋의 작업 트리 값이다. 테스트 표의 건수는 main 전체 실행 사본(harness/out/task-S9-booking-lifecycle-R1/step9의 JUnit XML)에서 읽었다.

### backend/src/main/java/com/o2o/inventory/api 15개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/inventory/api/AdjustInventoryRequest.java | sha256:8f0eda564fc2d3cc | 재고와 요금 | 없음 | bba1a22 |
| backend/src/main/java/com/o2o/inventory/api/AdjustRateRequest.java | sha256:c2dbdf5f93824c47 | 재고와 요금 | 없음 | bba1a22 |
| backend/src/main/java/com/o2o/inventory/api/ApiDate.java | sha256:da20472be8f2f671 | 재고와 요금 | 없음 | bba1a22 |
| backend/src/main/java/com/o2o/inventory/api/BulkInventoryRequest.java | sha256:d055a9179796b4a5 | 재고와 요금 | 없음 | bba1a22 |
| backend/src/main/java/com/o2o/inventory/api/DailyInventoryResponse.java | sha256:3df7642751873191 | 재고와 요금 | 없음 | bba1a22 |
| backend/src/main/java/com/o2o/inventory/api/DailyRateResponse.java | sha256:96d5bfb4cb351f0f | 재고와 요금 | 없음 | bba1a22 |
| backend/src/main/java/com/o2o/inventory/api/InvalidDateFormatException.java | sha256:c69036956c51688c | 재고와 요금 | 없음 | bba1a22 |
| backend/src/main/java/com/o2o/inventory/api/InventoryController.java | sha256:f0a08366cec320ea | 재고와 요금 | 없음 | fa87349 |
| backend/src/main/java/com/o2o/inventory/api/InventoryExceptionHandler.java | sha256:78347a957a155424 | 재고와 요금 | 없음 | bba1a22 |
| backend/src/main/java/com/o2o/inventory/api/InventoryRangeResponse.java | sha256:96828441054299d8 | 재고와 요금 | 없음 | bba1a22 |
| backend/src/main/java/com/o2o/inventory/api/RateController.java | sha256:224f206649642d99 | 재고와 요금 | 없음 | fa87349 |
| backend/src/main/java/com/o2o/inventory/api/RateRangeResponse.java | sha256:56ce5218de831b8c | 재고와 요금 | 없음 | fa87349 |
| backend/src/main/java/com/o2o/inventory/api/RegisterInventoryRequest.java | sha256:c787c852cd89bb45 | 재고와 요금 | 없음 | bba1a22 |
| backend/src/main/java/com/o2o/inventory/api/RegisterRateRequest.java | sha256:db7a133d655bd37a | 재고와 요금 | 없음 | bba1a22 |
| backend/src/main/java/com/o2o/inventory/api/package-info.java | sha256:7a9f269b39dbfe23 | 재고와 요금 | 없음 | bba1a22 |

### backend/src/main/java/com/o2o/inventory/application 3개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/inventory/application/InventoryApplicationService.java | sha256:e61d32aa9cf68272 | 재고와 요금 | 없음 | 99aa357 |
| backend/src/main/java/com/o2o/inventory/application/RangeResult.java | sha256:2207e7f5fdc14d5e | 재고와 요금 | 없음 | fa87349 |
| backend/src/main/java/com/o2o/inventory/application/package-info.java | sha256:15492150e9db1dcd | 재고와 요금 | 없음 | 75e850c |

### backend/src/main/java/com/o2o/inventory/domain 26개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/inventory/domain/DailyInventory.java | sha256:cd47187239009880 | 재고와 요금 | 예약 1차 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/DailyInventoryRepository.java | sha256:4e0cb3bd075fdb15 | 재고와 요금 | 예약 1차 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/DailyRate.java | sha256:8b9c33c0946e7954 | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/DailyRateRepository.java | sha256:68af292d2bb7ccab | 재고와 요금 | 없음 | fa87349 |
| backend/src/main/java/com/o2o/inventory/domain/DuplicateInventoryException.java | sha256:b7fa9beb00e5d3cc | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/DuplicateRateException.java | sha256:dc805da046ea00d8 | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/InsufficientHoldException.java | sha256:f082179e36064148 | 예약 1차 | 없음 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/InsufficientSoldException.java | sha256:6b20043712999e8f | 예약 1차 | 없음 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/InvalidRateException.java | sha256:a6f366072a2cadfc | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/InvalidStayPeriodException.java | sha256:0f0bcc58eca8fc2a | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/InventoryAdjusted.java | sha256:96d7cd154045b4cb | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/InventoryAllocationService.java | sha256:3cfccfd6ce098a56 | 예약 1차 | 예약 2차 | fce4c6c |
| backend/src/main/java/com/o2o/inventory/domain/InventoryBelowOccupiedException.java | sha256:e577ed2e89d3f113 | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/InventoryCommitted.java | sha256:789ed670f7767d05 | 예약 2차 | 없음 | 84a2d77 |
| backend/src/main/java/com/o2o/inventory/domain/InventoryCountBelowZeroException.java | sha256:cb74c51dd45eabb5 | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/InventoryHeld.java | sha256:221b05a1259facb0 | 예약 1차 | 없음 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/InventoryNotFoundException.java | sha256:963598dabcc6f7a6 | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/InventoryNotOpenedException.java | sha256:3bb8c9a548663130 | 예약 1차 | 없음 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/InventoryOpened.java | sha256:a909d55175984da8 | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/InventoryReleased.java | sha256:ffb690195749de63 | 예약 2차 | 없음 | 84a2d77 |
| backend/src/main/java/com/o2o/inventory/domain/InventoryShortageException.java | sha256:76943b6474caa275 | 예약 1차 | 없음 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/PastStayDateException.java | sha256:1330088ad6245cac | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/RateAdjusted.java | sha256:1be142a22db6a199 | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/RateNotFoundException.java | sha256:1492c5dbe2ddfd79 | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/RateRegistered.java | sha256:75a02bce7edd10d2 | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/inventory/domain/package-info.java | sha256:43834f2fbfda2597 | 재고와 요금 | 없음 | 75e850c |

### backend/src/main/java/com/o2o/inventory/infrastructure 5개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/inventory/infrastructure/DailyInventoryJpaRepository.java | sha256:c455d74d17a29d46 | 재고와 요금 | 예약 1차 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/infrastructure/DailyRateJpaRepository.java | sha256:ebcdfe8e8bb0c483 | 재고와 요금 | 없음 | fa87349 |
| backend/src/main/java/com/o2o/inventory/infrastructure/JpaDailyInventoryRepository.java | sha256:f3adcc84bae5bc3e | 재고와 요금 | 예약 1차 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/infrastructure/JpaDailyRateRepository.java | sha256:da9cf722f53cba91 | 재고와 요금 | 없음 | fa87349 |
| backend/src/main/java/com/o2o/inventory/infrastructure/package-info.java | sha256:326f0999de203671 | 재고와 요금 | 없음 | 75e850c |

### backend/src/main/java/com/o2o/shared 3개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/shared/ClockConfiguration.java | sha256:1d55f5e872302c21 | 재고와 요금 | 없음 | 99aa357 |
| backend/src/main/java/com/o2o/shared/Money.java | sha256:be20a03c3ac01c87 | 재고와 요금 | 없음 | 75e850c |
| backend/src/main/java/com/o2o/shared/SeoulDate.java | sha256:6950bcc6a2b09ead | 재고와 요금 | 없음 | 99aa357 |

### 테스트 9개

| 경로 | sha256 | 건수 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|---|
| backend/src/test/java/com/o2o/inventory/api/InventoryApiTest.java | sha256:df6b9c10b619bbae | 24 | 재고와 요금 | 없음 | 70e8394 |
| backend/src/test/java/com/o2o/inventory/api/InventoryQueryApiTest.java | sha256:5c5b47dd333ca6d7 | 13 | 재고와 요금 | 없음 | 53ef63c |
| backend/src/test/java/com/o2o/inventory/application/InventoryApplicationServiceTest.java | sha256:a5a53be71918e23f | 9 | 재고와 요금 | 없음 | 99aa357 |
| backend/src/test/java/com/o2o/inventory/application/InventoryEventTest.java | sha256:f3e8a109cf176068 | 5 | 재고와 요금 | 없음 | bba1a22 |
| backend/src/test/java/com/o2o/inventory/domain/DailyInventoryAllocationTest.java | sha256:6223da00ca7f9bfb | 10 | 예약 1차 | 없음 | 55d0896 |
| backend/src/test/java/com/o2o/inventory/domain/DailyInventoryTest.java | sha256:4445791f13e54a7e | 7 | 재고와 요금 | 없음 | 18b65eb |
| backend/src/test/java/com/o2o/inventory/domain/DailyRateTest.java | sha256:7bf71388ed435eb3 | 6 | 재고와 요금 | 없음 | 18b65eb |
| backend/src/test/java/com/o2o/inventory/domain/InventoryAllocationServiceTest.java | sha256:0f4232c145a47ca5 | 5 | 예약 2차 | 없음 | dcbd8e1 |
| backend/src/test/java/com/o2o/shared/MoneyTest.java | sha256:d99981f767523ad2 | 4 | 재고와 요금 | 없음 | 18b65eb |

### 6-1. 합계

프로덕션 52개다. inventory 49(api 15, application 3, domain 26, infrastructure 5), shared 3. 테스트 9파일 83건이다. 이 묶음이 만든 7파일 68건(R1 시점 63건에서 R1 뒤 5건 늘었다)과 예약 묶음이 만든 2파일 15건. 저장소 전체는 56파일 419건이다.

### 6-2. 이번 라운드에서 평가 대상이 아닌 것

| 무엇 | 왜 |
|---|---|
| booking 패키지와 결제와 프로모션과 검색과 catalog 패키지 | 다른 쌍의 몫이다. 라운드 README 3절 |
| harness/out/task-S9-inventory-rate-R1/applied/ | R1 반영 기록이다. R2 평가자는 이전 리포트와 결정표와 함께 읽지 않는다 |
| harness/out/task-S9-inventory-rate-R1/step9-verification.md | 생성자의 자기 판정이라 읽지 않는다 |
| 4절의 표 | R1 시점 그대로다 |

### 6-3. 실행 결과 파일

5절의 단계 사본 셋은 R1 시점의 코드로 돈 결과다. 기준 커밋의 코드로 돈 것은 harness/out/task-S9-booking-lifecycle-R1/step9/의 JUnit XML 48개(419건)이고 이 묶음 몫은 inventory 여덟과 shared 하나다.
