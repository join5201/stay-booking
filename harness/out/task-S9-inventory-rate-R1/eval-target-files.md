# task-S9-inventory-rate 평가 대상 코드 목록

최초 작성: 2026-09-10
최종 갱신: 2026-09-10

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
