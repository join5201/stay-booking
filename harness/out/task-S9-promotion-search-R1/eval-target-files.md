# task-S9-promotion-search 평가 대상 코드 목록

최초 작성: 2026-09-14
최종 갱신: 2026-09-14

계약 5절 평가 대상 코드 행이 가리키는 파일이다. 계약 표 한 칸에 예순 줄을 넣을 수 없어 목록을 여기 둔다. 라운드 mvp-eval-2026-09-14의 쌍 3(프로모션과 검색) 몫이고 라운드의 조건은 harness/out/mvp-eval-2026-09-14/README.md에 있다.

## 1. 버전

| 항목 | 값 |
|---|---|
| 브랜치 | main |
| 기준 커밋 | 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1 (PR 138 병합, 2026-09-13 23:53) |
| 이 묶음의 backend 커밋 | PR 98(feat/task-s9-promotion-search)로 들어온 것. 도메인 39dcee7부터 검색 API까지. 이 묶음이 catalog 리포지토리 여섯에 읽기 메서드 둘을 더했고(888b278) 그 여섯은 숙소 쌍이 본다 |
| 뽑은 방식 | 패키지 단위. promotion과 search 패키지 전체, shared 중 git 이력의 첫 커밋이 이 묶음 PR 안인 파일 여섯. 표는 harness/out/mvp-eval-2026-09-14/build-target-lists.mjs promotion-search 가 기준 커밋에서 낸 것이다 |

## 2. 파일 표

경로는 저장소 루트 기준이다. sha256은 앞 16자리이고 기준 커밋의 작업 트리 값이다. 만든 묶음은 그 파일을 처음 더한 커밋의 PR, 고친 묶음은 그 뒤 그 파일을 고친 다른 PR이다. 이 쌍의 파일은 다른 묶음이 고친 것이 없다. 테스트 표의 건수는 main 전체 실행 사본(harness/out/task-S9-booking-lifecycle-R1/step9의 JUnit XML)에서 읽었다.

### backend/src/main/java/com/o2o/promotion/api 8개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/promotion/api/ApplicablePromotionsResponse.java | sha256:ccf69c0dc03778d4 | 프로모션과 검색 | 없음 | be5e510 |
| backend/src/main/java/com/o2o/promotion/api/CreatePromotionRequest.java | sha256:05aae4a8238c011a | 프로모션과 검색 | 없음 | be5e510 |
| backend/src/main/java/com/o2o/promotion/api/PriceSnapshotResponse.java | sha256:5d851438eaa72d66 | 프로모션과 검색 | 없음 | 468b3cf |
| backend/src/main/java/com/o2o/promotion/api/PromotionController.java | sha256:f4d426e04ee2a1bd | 프로모션과 검색 | 없음 | be5e510 |
| backend/src/main/java/com/o2o/promotion/api/PromotionExceptionHandler.java | sha256:3a87938934baded0 | 프로모션과 검색 | 없음 | be5e510 |
| backend/src/main/java/com/o2o/promotion/api/PromotionResponse.java | sha256:0ea6908d89483a1a | 프로모션과 검색 | 없음 | be5e510 |
| backend/src/main/java/com/o2o/promotion/api/UpdatePromotionRequest.java | sha256:bb1f924838a699d4 | 프로모션과 검색 | 없음 | be5e510 |
| backend/src/main/java/com/o2o/promotion/api/package-info.java | sha256:1fcfa89dc6dfb921 | 프로모션과 검색 | 없음 | be5e510 |

### backend/src/main/java/com/o2o/promotion/application 4개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/promotion/application/ApplicablePromotionsResult.java | sha256:071b584e04496c21 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/application/PromotionApplicationService.java | sha256:16e91dcc29572b8e | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/application/PromotionDraft.java | sha256:85771231e775b0d1 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/application/package-info.java | sha256:b599c4532a3f27e1 | 프로모션과 검색 | 없음 | 39dcee7 |

### backend/src/main/java/com/o2o/promotion/domain 25개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/promotion/domain/AppliedPromotion.java | sha256:e666da5169a71c6c | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/Condition.java | sha256:83dc2085ea4f154c | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/IncompleteStayWindowException.java | sha256:41990dd02230213c | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/InvalidDiscountRateException.java | sha256:f59128c22c629b83 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/InvalidPeriodException.java | sha256:bbb556f8023aa7cb | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/InvalidRegionCodesException.java | sha256:5422839eb59ce211 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/InvalidStayRangeException.java | sha256:f85bb0e1b000259f | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/OccupancyExceededException.java | sha256:9fd70e49dd827b10 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/PriceCalculation.java | sha256:dcd4a5707fd6a838 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/PriceDay.java | sha256:b02d75960395e3eb | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/PriceSnapshot.java | sha256:12bf50dcb0554010 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/PricingResult.java | sha256:c61c66afd274646a | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/PricingService.java | sha256:79d53b1a99e45185 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/Promotion.java | sha256:ac0de9317329e505 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/PromotionCandidate.java | sha256:de9951f3f0c07e6f | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/PromotionChanges.java | sha256:8bf0091b6527b19c | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/PromotionCreated.java | sha256:995ddf629e049904 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/PromotionNotFoundException.java | sha256:9d440f77155242cd | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/PromotionRepository.java | sha256:b6e8dbcc55f72f71 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/PromotionUpdated.java | sha256:d7358bb4628850f9 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/RateNotConfiguredException.java | sha256:6650c7b7f7f500ce | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/StayRange.java | sha256:24f310b17ea612cb | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/StayWindow.java | sha256:4a182ba597a5d195 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/StayWindowChange.java | sha256:199546372cb32a20 | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/domain/package-info.java | sha256:36e6f80672f87df1 | 프로모션과 검색 | 없음 | 39dcee7 |

### backend/src/main/java/com/o2o/promotion/infrastructure 4개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/promotion/infrastructure/JpaPromotionRepository.java | sha256:b5dcbf0b2c48613d | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/infrastructure/PricingServiceConfiguration.java | sha256:5dd9510a88f8cc9c | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/infrastructure/PromotionJpaRepository.java | sha256:f5b1d74d7e4052cc | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/promotion/infrastructure/package-info.java | sha256:a8dadfc3f8d692dd | 프로모션과 검색 | 없음 | 39dcee7 |

### backend/src/main/java/com/o2o/promotion 1개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/promotion/package-info.java | sha256:1577dc9a65f3e102 | 프로모션과 검색 | 없음 | 39dcee7 |

### backend/src/main/java/com/o2o/search/api 5개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/search/api/AvailabilityResponse.java | sha256:44b76df2a81816b9 | 프로모션과 검색 | 없음 | 468b3cf |
| backend/src/main/java/com/o2o/search/api/PriceQuoteResponse.java | sha256:5f6d7961c398b97e | 프로모션과 검색 | 없음 | 468b3cf |
| backend/src/main/java/com/o2o/search/api/PropertySearchResultResponse.java | sha256:2f8c6e9ec4aed2e7 | 프로모션과 검색 | 없음 | 468b3cf |
| backend/src/main/java/com/o2o/search/api/SearchController.java | sha256:49ab572fdac85b2c | 프로모션과 검색 | 없음 | 468b3cf |
| backend/src/main/java/com/o2o/search/api/package-info.java | sha256:8fd7f18c70b3da11 | 프로모션과 검색 | 없음 | 468b3cf |

### backend/src/main/java/com/o2o/search/application 6개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/search/application/AvailabilityResult.java | sha256:e94d72676ef62080 | 프로모션과 검색 | 없음 | 468b3cf |
| backend/src/main/java/com/o2o/search/application/PriceQuoteResult.java | sha256:a92b997e33a35233 | 프로모션과 검색 | 없음 | 468b3cf |
| backend/src/main/java/com/o2o/search/application/PropertySearchResult.java | sha256:967698f178012d27 | 프로모션과 검색 | 없음 | 468b3cf |
| backend/src/main/java/com/o2o/search/application/RoomSearchResult.java | sha256:6e834743331ada0c | 프로모션과 검색 | 없음 | 468b3cf |
| backend/src/main/java/com/o2o/search/application/SearchApplicationService.java | sha256:ad8e097c180cdde7 | 프로모션과 검색 | 없음 | 468b3cf |
| backend/src/main/java/com/o2o/search/application/package-info.java | sha256:3482e761d7a27bde | 프로모션과 검색 | 없음 | 468b3cf |

### backend/src/main/java/com/o2o/search 1개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/search/package-info.java | sha256:d9c6a28c34894471 | 프로모션과 검색 | 없음 | 468b3cf |

### backend/src/main/java/com/o2o/shared 6개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/shared/ApiDate.java | sha256:c02e5edd77c654b4 | 프로모션과 검색 | 없음 | be5e510 |
| backend/src/main/java/com/o2o/shared/ApiTime.java | sha256:b77817bfff261710 | 프로모션과 검색 | 없음 | be5e510 |
| backend/src/main/java/com/o2o/shared/GuestCount.java | sha256:57f7541878ef64da | 프로모션과 검색 | 없음 | 39dcee7 |
| backend/src/main/java/com/o2o/shared/InvalidDateFormatException.java | sha256:a36a39c42dcd08c3 | 프로모션과 검색 | 없음 | be5e510 |
| backend/src/main/java/com/o2o/shared/PageResponse.java | sha256:aa2feb59977384de | 프로모션과 검색 | 없음 | be5e510 |
| backend/src/main/java/com/o2o/shared/PromotionId.java | sha256:b161606d54664972 | 프로모션과 검색 | 없음 | 39dcee7 |

### 테스트 7개

| 경로 | sha256 | 건수 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|---|
| backend/src/test/java/com/o2o/promotion/api/PromotionApiTest.java | sha256:d9a1303ef2bf0d71 | 18 | 프로모션과 검색 | 없음 | be5e510 |
| backend/src/test/java/com/o2o/promotion/application/PromotionApplicationServiceTest.java | sha256:3d58620f75330132 | 13 | 프로모션과 검색 | 없음 | e069071 |
| backend/src/test/java/com/o2o/promotion/application/PromotionEventTest.java | sha256:bf2a8a911e3966dd | 3 | 프로모션과 검색 | 없음 | e069071 |
| backend/src/test/java/com/o2o/promotion/domain/ConditionTest.java | sha256:a2db685cdc64ebed | 13 | 프로모션과 검색 | 없음 | e069071 |
| backend/src/test/java/com/o2o/promotion/domain/PriceCalculationTest.java | sha256:7e20538b0a7c1e98 | 7 | 프로모션과 검색 | 없음 | e069071 |
| backend/src/test/java/com/o2o/promotion/domain/PromotionTest.java | sha256:e0932645a6c1c34f | 11 | 프로모션과 검색 | 없음 | e069071 |
| backend/src/test/java/com/o2o/search/api/SearchApiTest.java | sha256:02d6487e5fa0b79b | 21 | 프로모션과 검색 | 없음 | 468b3cf |

## 3. 합계

프로덕션 60개다. promotion 42(api 8, application 4, domain 25, infrastructure 4, package-info 1), search 12(api 5, application 6, package-info 1), shared 6. 테스트 7파일 86건이다. promotion 6파일 65건, search 1파일 21건. 저장소 전체는 56파일 419건이다.

## 4. 평가 대상이 아닌 것

| 무엇 | 왜 |
|---|---|
| catalog, inventory, booking, payment 패키지 | 다른 쌍의 몫이다. 라운드 README 3절. 이 묶음이 고친 catalog 리포지토리 여섯도 숙소 쌍이 본다 |
| shared 중 다른 묶음이 만든 열여덟 | 그 묶음의 쌍이 본다 |
| 예약의 가격 포트 어댑터(booking/infrastructure의 PricingPriceQuoteAdapter) | 이 묶음의 PricingService를 부르는 쪽이고 예약 쌍이 본다 |
| harness/out/task-S9-promotion-search-R1/ 아래 결과 사본과 step9-verification.md | 산출물의 증거이지 평가 대상 코드가 아니다. step9-verification.md는 생성자의 자기 판정이라 읽지 않는다 |
| 설정 파일 둘 | 이 묶음이 고치지 않았다. 결제 쌍이 본다 |
| 이 계약 파일 harness/tasks/task-S9-promotion-search.md | 5절 표의 별도 행이다 |

## 5. 실행 결과 파일

| 단계 | 경로 | 무엇 |
|---|---|---|
| 5 | harness/out/task-S9-promotion-search-R1/step5/ | 도메인과 앱 서비스 |
| 6 | harness/out/task-S9-promotion-search-R1/step6/ | 프로모션 API. http-calls.txt |
| 6-2 | harness/out/task-S9-promotion-search-R1/step6-2/ | 검색 API. http-calls.txt |
| main 전체 | harness/out/task-S9-booking-lifecycle-R1/step9/ | JUnit XML 48개, 419건. 이 묶음 몫은 promotion 여섯과 search 하나 |

단계 사본은 그 단계 시점의 코드로 돈 결과다. 기준 커밋의 코드로 돈 것은 마지막 행이다.
