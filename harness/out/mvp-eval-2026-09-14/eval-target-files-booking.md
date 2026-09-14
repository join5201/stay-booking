# task-S9-booking과 task-S9-booking-lifecycle 평가 대상 코드 합본 목록

최초 작성: 2026-09-14
최종 갱신: 2026-09-14

라운드 mvp-eval-2026-09-14의 쌍 4(예약과 선점 1차와 2차) 몫이다. 라운드 README 5절 D-4 가에 따라 1차와 2차를 한 쌍이 보고, 두 계약의 5절이 각각 가리키던 목록 둘(1차 harness/out/task-S9-booking-R1/eval-target-files.md 기준 d111e66, 2차 harness/out/task-S9-booking-lifecycle-R1/eval-target-files.md 기준 892223f)을 main 기준으로 다시 뽑아 하나로 합쳤다. 2차 계약 5절이 이 합본을 가리키고 1차 계약은 최종 완료라 고치지 않는다. 원래 목록 둘은 그때의 브랜치 시점 기록으로 남고 이번 라운드의 대상은 이 파일이다.

## 1. 버전

| 항목 | 값 |
|---|---|
| 브랜치 | main |
| 기준 커밋 | 1bdadfe5a78722f1e708d96bafc0f728eddfc4a1 (PR 138 병합, 2026-09-13 23:53) |
| 이 묶음의 backend 커밋 | 1차는 PR 127(feat/task-s9-booking) 여섯. 2차는 PR 138(feat/task-s9-booking-lifecycle) 스물셋. 커밋 표는 원래 목록 둘의 1절 |
| 뽑은 방식 | 패키지 단위. booking 패키지 전체, inventory와 shared 중 예약 1차나 2차 커밋이 만들거나 고친 파일, 테스트는 booking 전체와 inventory 중 예약 묶음이 만든 둘, 설정은 2차가 한 줄을 더한 테스트 설정. 표는 harness/out/mvp-eval-2026-09-14/build-target-lists.mjs booking 이 기준 커밋에서 낸 것이다 |

## 2. 파일 표

경로는 저장소 루트 기준이다. sha256은 앞 16자리이고 기준 커밋의 작업 트리 값이다. 만든 묶음은 그 파일을 처음 더한 커밋의 PR, 고친 묶음은 그 뒤 그 파일을 고친 다른 PR이다. 테스트 표의 건수는 main 전체 실행 사본(harness/out/task-S9-booking-lifecycle-R1/step9의 JUnit XML)에서 읽었다.

1차와 2차 사이. booking의 1차 파일 11개를 2차가 고쳤다(고친 묶음 칸이 예약 2차인 행). 2차가 1차 위에 얹힌 것이라 평가자는 지금 모양을 한 번에 판정하고, 지적이 1차 근거에 걸리면 1차 계약을, 2차 근거에 걸리면 2차 계약을 인용한다.

다른 묶음과의 관계. inventory의 DailyInventory와 그 리포지토리 인터페이스와 JPA 구현 둘은 재고와 요금 묶음이 만들고 예약 1차가 hold 계열을 더했다. 이 넷과 예약 묶음이 더한 inventory 파일 여덟과 테스트 둘은 재고와 요금 쌍(R2)도 본다. shared의 ActorRegistry는 숙소 묶음이 만들고 예약 1차가 GUEST 행위자를 더했다. 테스트 설정 파일은 결제가 프로파일 줄과 격리 수준 줄을, 예약 2차가 스케줄러 줄을 더했다.

### backend/src/main/java/com/o2o/booking/api 13개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/booking/api/ApiFormat.java | sha256:de2a2a9b5b3ed2b8 | 예약 1차 | 없음 | 3d84e83 |
| backend/src/main/java/com/o2o/booking/api/BodyFingerprint.java | sha256:20ea05b507694c52 | 예약 2차 | 없음 | 0153a07 |
| backend/src/main/java/com/o2o/booking/api/BookingController.java | sha256:33de44b118ea1e93 | 예약 1차 | 예약 2차 | 7c120ef |
| backend/src/main/java/com/o2o/booking/api/BookingExceptionHandler.java | sha256:7d8ee112c516c8bd | 예약 1차 | 예약 2차 | 529f571 |
| backend/src/main/java/com/o2o/booking/api/BookingResponse.java | sha256:88c05e5c9b59a622 | 예약 1차 | 예약 2차 | 5872340 |
| backend/src/main/java/com/o2o/booking/api/CancelBookingRequest.java | sha256:75297307682ddfaa | 예약 2차 | 없음 | 7c120ef |
| backend/src/main/java/com/o2o/booking/api/InvalidDateFormatException.java | sha256:8695a23087a982bd | 예약 1차 | 없음 | 3d84e83 |
| backend/src/main/java/com/o2o/booking/api/PageResponse.java | sha256:9f6bc7d9706d44b2 | 예약 1차 | 없음 | 3d84e83 |
| backend/src/main/java/com/o2o/booking/api/PaymentAttemptListResponse.java | sha256:6c839d3b768a58b8 | 예약 2차 | 없음 | 0153a07 |
| backend/src/main/java/com/o2o/booking/api/PaymentAttemptResponse.java | sha256:8f0e51dd40a3d6b6 | 예약 2차 | 없음 | 5872340 |
| backend/src/main/java/com/o2o/booking/api/RequestBookingRequest.java | sha256:6f32bc6549d52e39 | 예약 1차 | 없음 | 3d84e83 |
| backend/src/main/java/com/o2o/booking/api/RequestPaymentRequest.java | sha256:b3472e9b7dbf44dc | 예약 2차 | 없음 | 0153a07 |
| backend/src/main/java/com/o2o/booking/api/package-info.java | sha256:46bd18a6c6fbdbb8 | 예약 1차 | 예약 2차 | 7c120ef |

### backend/src/main/java/com/o2o/booking/application 15개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/booking/application/BookingApplicationService.java | sha256:d0e71e27a80d056a | 예약 1차 | 예약 2차 | ac7b225 |
| backend/src/main/java/com/o2o/booking/application/BookingExpirationService.java | sha256:410f54e2d20f69a1 | 예약 2차 | 없음 | ac7b225 |
| backend/src/main/java/com/o2o/booking/application/BookingLifecycle.java | sha256:f3356d010ca578b3 | 예약 2차 | 없음 | ac7b225 |
| backend/src/main/java/com/o2o/booking/application/BookingPaymentService.java | sha256:b54e888b24a7ffad | 예약 2차 | 없음 | ac7b225 |
| backend/src/main/java/com/o2o/booking/application/CancelBookingCommand.java | sha256:04516fd10756f860 | 예약 2차 | 없음 | ac7b225 |
| backend/src/main/java/com/o2o/booking/application/ExpireDueBookings.java | sha256:4791bb0518eb28bc | 예약 2차 | 없음 | ac7b225 |
| backend/src/main/java/com/o2o/booking/application/IdempotencyRecordService.java | sha256:bf18fd1c7fce7ca3 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/application/IdempotentRequestExecutor.java | sha256:1a2c6324b2461d2f | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/application/IdempotentResult.java | sha256:b143e7a1e65c8ec0 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/application/PaymentOutcomeService.java | sha256:16ab8a1390db47b1 | 예약 2차 | 없음 | ac7b225 |
| backend/src/main/java/com/o2o/booking/application/PriceQuotePort.java | sha256:46d573aaf6122ba9 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/application/RequestBookingCommand.java | sha256:b20967c29ed5eefd | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/application/RequestPaymentCommand.java | sha256:662e93c2360b7726 | 예약 2차 | 없음 | ac7b225 |
| backend/src/main/java/com/o2o/booking/application/StoredResponse.java | sha256:a315b7bb07843761 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/application/package-info.java | sha256:a7f0ed0c45dfb2cf | 예약 1차 | 없음 | 5ca3464 |

### backend/src/main/java/com/o2o/booking/domain 35개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/booking/domain/AppliedPromotion.java | sha256:2efdfd4bd38cc9c5 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/Booking.java | sha256:9274fc81a0fd1e9e | 예약 1차 | 예약 2차 | 8b8df23 |
| backend/src/main/java/com/o2o/booking/domain/BookingCanceled.java | sha256:9fc24dfcb9531206 | 예약 2차 | 없음 | 8b8df23 |
| backend/src/main/java/com/o2o/booking/domain/BookingConfirmed.java | sha256:27e902ffa95a2365 | 예약 2차 | 없음 | 8b8df23 |
| backend/src/main/java/com/o2o/booking/domain/BookingCreated.java | sha256:87f7af0801b0ae3c | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/BookingExpired.java | sha256:ed92c6e24b8995a0 | 예약 2차 | 없음 | 8b8df23 |
| backend/src/main/java/com/o2o/booking/domain/BookingExpiredException.java | sha256:37b98dd00bc322a5 | 예약 2차 | 없음 | 2a06060 |
| backend/src/main/java/com/o2o/booking/domain/BookingId.java | sha256:0ba802ffa196527e | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/BookingNotFoundException.java | sha256:23174de6bbd2719a | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/BookingRepository.java | sha256:06c08ef038325630 | 예약 1차 | 예약 2차 | a388eab |
| backend/src/main/java/com/o2o/booking/domain/BookingStatus.java | sha256:254975141df3a18f | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/CancellationNotAllowedException.java | sha256:91f33240885544b6 | 예약 2차 | 없음 | 2a06060 |
| backend/src/main/java/com/o2o/booking/domain/DailyPrice.java | sha256:03cd133708ec2100 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/ExpirationNotDueException.java | sha256:ec63acbee84e7c3f | 예약 2차 | 없음 | 8b8df23 |
| backend/src/main/java/com/o2o/booking/domain/ExpirationReason.java | sha256:845931f18eb266f2 | 예약 2차 | 없음 | 8b8df23 |
| backend/src/main/java/com/o2o/booking/domain/FailedAttemptsBelowLimitException.java | sha256:eda4f4b6d27200c1 | 예약 2차 | 없음 | 8b8df23 |
| backend/src/main/java/com/o2o/booking/domain/IdempotencyKey.java | sha256:c09f2e72fc07c737 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/IdempotencyKeyReusedException.java | sha256:d5b5380f8acb2ce8 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/IdempotencyRecord.java | sha256:70fc5c4270cd2cb2 | 예약 1차 | 없음 | 8dbcc2a |
| backend/src/main/java/com/o2o/booking/domain/IdempotencyRecordRepository.java | sha256:2c09d51d1b0eef5a | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/IdempotencyScope.java | sha256:e343075ec5ef02dd | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/InvalidBookingPeriodException.java | sha256:d36cc4ddc11a2bbc | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/InvalidIdempotencyKeyException.java | sha256:4af33e86c0bec60f | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/InvalidPriceSnapshotException.java | sha256:543a4ad3f0fc067f | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/InvalidStateTransitionException.java | sha256:650fca87b5892375 | 예약 2차 | 없음 | 2a06060 |
| backend/src/main/java/com/o2o/booking/domain/OccupancyExceededException.java | sha256:2f5635883e6d9018 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/PaymentAttemptsExhaustedException.java | sha256:c68b6eda4e90a24c | 예약 2차 | 없음 | 2a06060 |
| backend/src/main/java/com/o2o/booking/domain/PaymentInProgressException.java | sha256:fe133d0a60ee7011 | 예약 2차 | 없음 | 2a06060 |
| backend/src/main/java/com/o2o/booking/domain/PriceChangedException.java | sha256:19deab9f26ec6f24 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/PriceSnapshot.java | sha256:1938dbe211a03c8f | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/RateNotConfiguredException.java | sha256:3370ad9e8121594d | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/RequestInProgressException.java | sha256:54ee882ec39a65c8 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/StayPeriod.java | sha256:701d1c2de5b87716 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/UserId.java | sha256:406ff81b5fa520f0 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/domain/package-info.java | sha256:b8f15af066c24a9a | 예약 1차 | 없음 | 5ca3464 |

### backend/src/main/java/com/o2o/booking/infrastructure 11개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/booking/infrastructure/BookingExpireScheduler.java | sha256:0cd50fa7fa7e6c7c | 예약 2차 | 없음 | 26ac525 |
| backend/src/main/java/com/o2o/booking/infrastructure/BookingExpireSchedulerConfiguration.java | sha256:69a6af89136193f4 | 예약 2차 | 없음 | 26ac525 |
| backend/src/main/java/com/o2o/booking/infrastructure/BookingJpaRepository.java | sha256:00e22fb02b138889 | 예약 1차 | 예약 2차 | a388eab |
| backend/src/main/java/com/o2o/booking/infrastructure/IdempotencyRecordJpaRepository.java | sha256:2a556497380f7689 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/infrastructure/InventoryAllocationConfiguration.java | sha256:5e2e64dcc02e0322 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/infrastructure/JpaBookingRepository.java | sha256:f2ecebc4af2ca5ad | 예약 1차 | 예약 2차 | a388eab |
| backend/src/main/java/com/o2o/booking/infrastructure/JpaIdempotencyRecordRepository.java | sha256:804650906c5d46b4 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/main/java/com/o2o/booking/infrastructure/PaymentApprovedAdapter.java | sha256:243b743d785a26e0 | 예약 2차 | 없음 | 82623de |
| backend/src/main/java/com/o2o/booking/infrastructure/PaymentFailedAdapter.java | sha256:5c6debaa88106a3a | 예약 2차 | 없음 | 82623de |
| backend/src/main/java/com/o2o/booking/infrastructure/PricingPriceQuoteAdapter.java | sha256:0072b1cde0b1f7c3 | 예약 2차 | 없음 | 6b9c4e6 |
| backend/src/main/java/com/o2o/booking/infrastructure/package-info.java | sha256:fa1b19d03de7e72d | 예약 1차 | 예약 2차 | 26ac525 |

### backend/src/main/java/com/o2o/inventory/domain 10개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/inventory/domain/DailyInventory.java | sha256:cd47187239009880 | 재고와 요금 | 예약 1차 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/DailyInventoryRepository.java | sha256:4e0cb3bd075fdb15 | 재고와 요금 | 예약 1차 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/InsufficientHoldException.java | sha256:f082179e36064148 | 예약 1차 | 없음 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/InsufficientSoldException.java | sha256:6b20043712999e8f | 예약 1차 | 없음 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/InventoryAllocationService.java | sha256:3cfccfd6ce098a56 | 예약 1차 | 예약 2차 | fce4c6c |
| backend/src/main/java/com/o2o/inventory/domain/InventoryCommitted.java | sha256:789ed670f7767d05 | 예약 2차 | 없음 | 84a2d77 |
| backend/src/main/java/com/o2o/inventory/domain/InventoryHeld.java | sha256:221b05a1259facb0 | 예약 1차 | 없음 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/InventoryNotOpenedException.java | sha256:3bb8c9a548663130 | 예약 1차 | 없음 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/domain/InventoryReleased.java | sha256:ffb690195749de63 | 예약 2차 | 없음 | 84a2d77 |
| backend/src/main/java/com/o2o/inventory/domain/InventoryShortageException.java | sha256:76943b6474caa275 | 예약 1차 | 없음 | 55d0896 |

### backend/src/main/java/com/o2o/inventory/infrastructure 2개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/inventory/infrastructure/DailyInventoryJpaRepository.java | sha256:c455d74d17a29d46 | 재고와 요금 | 예약 1차 | 55d0896 |
| backend/src/main/java/com/o2o/inventory/infrastructure/JpaDailyInventoryRepository.java | sha256:f3adcc84bae5bc3e | 재고와 요금 | 예약 1차 | 55d0896 |

### backend/src/main/java/com/o2o/shared 1개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/main/java/com/o2o/shared/ActorRegistry.java | sha256:9b0e000f80967372 | 숙소 | 예약 1차 | 3d84e83 |

### 테스트 29개

| 경로 | sha256 | 건수 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|---|
| backend/src/test/java/com/o2o/booking/BookingFixtures.java | sha256:7ed5e373cbaf3357 | 0 (테스트 지원 파일) | 예약 2차 | 없음 | 72b14dc |
| backend/src/test/java/com/o2o/booking/BookingLifecycleTestConfiguration.java | sha256:8dbe8fd3977f1463 | 0 (테스트 지원 파일) | 예약 2차 | 없음 | 72b14dc |
| backend/src/test/java/com/o2o/booking/CommittedBookingEvents.java | sha256:7457df97616afdc5 | 0 (테스트 지원 파일) | 예약 2차 | 없음 | 72b14dc |
| backend/src/test/java/com/o2o/booking/FailingOnceInventoryAllocationService.java | sha256:2926d4d62b16db2f | 0 (테스트 지원 파일) | 예약 2차 | 없음 | 72b14dc |
| backend/src/test/java/com/o2o/booking/FailingOncePaymentOutcomeService.java | sha256:65f25d83d453ce0f | 0 (테스트 지원 파일) | 예약 2차 | 없음 | 72b14dc |
| backend/src/test/java/com/o2o/booking/MutableClock.java | sha256:73783ff3d28753d2 | 0 (테스트 지원 파일) | 예약 2차 | 없음 | 72b14dc |
| backend/src/test/java/com/o2o/booking/TestHookException.java | sha256:7ebc574f196a1caa | 0 (테스트 지원 파일) | 예약 2차 | 없음 | 72b14dc |
| backend/src/test/java/com/o2o/booking/api/BookingApiTest.java | sha256:bdb1cf2a930d1328 | 17 | 예약 1차 | 없음 | f2f4da8 |
| backend/src/test/java/com/o2o/booking/api/BookingCancelApiTest.java | sha256:5fd4c92e2792d3dc | 9 | 예약 2차 | 없음 | ee5611c |
| backend/src/test/java/com/o2o/booking/api/BookingPaymentApiTest.java | sha256:699cb424b52918c9 | 14 | 예약 2차 | 없음 | c00bd0f |
| backend/src/test/java/com/o2o/booking/api/BookingQueryApiTest.java | sha256:09921de0811c7f5d | 8 | 예약 1차 | 예약 2차 | 03ba667 |
| backend/src/test/java/com/o2o/booking/api/RequestBookingRequestTest.java | sha256:0791e0c3522a1c31 | 4 | 예약 1차 | 없음 | 3d84e83 |
| backend/src/test/java/com/o2o/booking/application/ApprovalLossRecoveryTest.java | sha256:0b2b3795748e5f23 | 2 | 예약 2차 | 없음 | c8dc438 |
| backend/src/test/java/com/o2o/booking/application/BookingApplicationServiceTest.java | sha256:e0df9e17f5f23dea | 8 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/test/java/com/o2o/booking/application/BookingEventTest.java | sha256:38c74fdb92ca6728 | 2 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/test/java/com/o2o/booking/application/BookingLifecycleEventTest.java | sha256:6addce89c523df61 | 4 | 예약 2차 | 없음 | c8dc438 |
| backend/src/test/java/com/o2o/booking/application/BookingLockContentionTest.java | sha256:01d80ef2776e7aaa | 2 | 예약 2차 | 없음 | 892223f |
| backend/src/test/java/com/o2o/booking/application/BookingPaymentServiceTest.java | sha256:467bdabbe02defd3 | 9 | 예약 2차 | 없음 | c8dc438 |
| backend/src/test/java/com/o2o/booking/application/CancelBookingTest.java | sha256:615ee5d297943efe | 6 | 예약 2차 | 없음 | c8dc438 |
| backend/src/test/java/com/o2o/booking/application/ExpireDueBookingsTest.java | sha256:2558163ad91e6aee | 4 | 예약 2차 | 없음 | c8dc438 |
| backend/src/test/java/com/o2o/booking/application/PaymentOutcomeServiceTest.java | sha256:05c6d775879dc231 | 10 | 예약 2차 | 없음 | c8dc438 |
| backend/src/test/java/com/o2o/booking/domain/BookingTest.java | sha256:082cc50067dc8218 | 5 | 예약 1차 | 예약 2차 | 8b8df23 |
| backend/src/test/java/com/o2o/booking/domain/BookingTransitionTest.java | sha256:2b18688ca7113d05 | 12 | 예약 2차 | 없음 | bcdc493 |
| backend/src/test/java/com/o2o/booking/domain/IdempotencyRecordTest.java | sha256:1013fd0ccc7a5920 | 6 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/test/java/com/o2o/booking/domain/PriceSnapshotTest.java | sha256:02e9fb88faf50178 | 9 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/test/java/com/o2o/booking/domain/StayPeriodTest.java | sha256:8265012793ae76d4 | 4 | 예약 1차 | 없음 | 5ca3464 |
| backend/src/test/java/com/o2o/booking/infrastructure/PricingPriceQuoteAdapterTest.java | sha256:7cbdb4205c61f457 | 5 | 예약 2차 | 없음 | bcdc493 |
| backend/src/test/java/com/o2o/inventory/domain/DailyInventoryAllocationTest.java | sha256:6223da00ca7f9bfb | 10 | 예약 1차 | 없음 | 55d0896 |
| backend/src/test/java/com/o2o/inventory/domain/InventoryAllocationServiceTest.java | sha256:0f4232c145a47ca5 | 5 | 예약 2차 | 없음 | dcbd8e1 |

### 설정 파일 1개

| 경로 | sha256 | 만든 묶음 | 고친 묶음 | 마지막 커밋 |
|---|---|---|---|---|
| backend/src/test/resources/application.properties | sha256:7a925f9016cc6666 | 숙소 | 결제, 예약 2차 | c4c38dd |

## 3. 합계

프로덕션 87개다. booking 74(api 13, application 15, domain 35, infrastructure 11), inventory 12(domain 10, infrastructure 2), shared 1. 테스트 29파일 155건이다. booking 27파일 140건(그중 지원 파일 7은 0건), inventory 2파일 15건. 설정 1. 저장소 전체는 56파일 419건이고 inventory 둘의 15건은 재고와 요금 쌍의 합계에도 들어 있다.

## 4. 원래 목록 둘과의 차이

| 목록 | 시점 | 수 | 합본에서 |
|---|---|---|---|
| 1차 task-S9-booking-R1/eval-target-files.md | d111e66 | 프로덕션 58(booking 47, inventory 10, shared 1), 테스트 10파일 73건 | 전부 들어 있다. 그중 11개는 2차가 고친 뒤 모양이다 |
| 2차 task-S9-booking-lifecycle-R1/eval-target-files.md | 892223f | 프로덕션 41(booking 새 30과 고침 11, inventory 3), 설정 1, 테스트 12파일 82건과 지원 7과 고친 1차 2 | 전부 들어 있다. 892223f 뒤 backend 코드 커밋이 없어(backend/README.md 기록 둘뿐) 내용이 같다 |

## 5. 평가 대상이 아닌 것

| 무엇 | 왜 |
|---|---|
| catalog, promotion, search, payment 패키지 | 다른 쌍의 몫이다. 라운드 README 3절. 결제의 openAttempt와 refund와 attemptsOf는 결제 쌍이 본다 |
| inventory 중 예약 묶음이 만들거나 고치지 않은 파일 | 재고와 요금 쌍이 본다 |
| shared 중 ActorRegistry 밖 | 만든 묶음의 쌍이 본다 |
| backend/src/main/resources/application.properties | 2차 계약 5절의 허용 입력이다(7행 fail-on-unknown-properties). 대상 코드가 아니라 읽는 자료다 |
| harness/out/task-S9-booking-R1과 task-S9-booking-lifecycle-R1 아래 결과 사본과 step9-verification.md 둘 | 산출물의 증거이지 평가 대상 코드가 아니다. step9-verification.md 둘은 생성자의 자기 판정이라 읽지 않는다 |
| 계약 파일 둘 | 5절 표의 별도 행이다 |

## 6. 실행 결과 파일

| 차수 | 단계 | 경로 | 무엇 |
|---|---|---|---|
| 1차 | 5 | harness/out/task-S9-booking-R1/step5/ | 재고 hold 계열과 예약 도메인과 앱 서비스 |
| 1차 | 6 | harness/out/task-S9-booking-R1/step6/ | BOOK-01. http-calls.txt |
| 1차 | 6-2 | harness/out/task-S9-booking-R1/step6-2/ | BOOK-02와 BOOK-03. http-calls.txt |
| 2차 | 4-1부터 4-4 | harness/out/task-S9-booking-lifecycle-R1/step4-1/부터 step4-4/ | 단계별 요약 test-summary.txt. 4-4에 bootrun-excerpt.txt |
| 2차 | 5 | harness/out/task-S9-booking-lifecycle-R1/step5/ | 요약 test-summary.txt |
| 2차 | 6 | harness/out/task-S9-booking-lifecycle-R1/step6/ | PAY-01과 PAY-02와 결제 결과 처리. http-calls.txt와 bootrun-excerpt.txt와 요약 |
| 2차 | 6-2 | harness/out/task-S9-booking-lifecycle-R1/step6-2/ | BOOK-04와 만료. http-calls.txt와 bootrun-excerpt.txt와 XML 둘 |
| main 전체 | 9 | harness/out/task-S9-booking-lifecycle-R1/step9/ | JUnit XML 48개, 419건. 기준 커밋의 코드로 돈 결과다. 2차의 XML 전체는 여기뿐이다 |
