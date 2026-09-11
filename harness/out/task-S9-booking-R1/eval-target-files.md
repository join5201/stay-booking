# task-S9-booking 평가 대상 코드 목록

최초 작성: 2026-09-12
최종 갱신: 2026-09-12

계약 5절 평가 대상 코드 행이 가리키는 파일이다. 계약 표 한 칸에 예순 줄을 넣을 수 없어 목록을 여기 둔다. 오늘의 전제 결정 3에 따라 평가는 MVP 코드가 다 붙은 뒤 한 번이고 이 문서는 그 라운드에 이 묶음 몫으로 넘길 목록이다.

## 1. 버전

| 항목 | 값 |
|---|---|
| 브랜치 | feat/task-s9-booking |
| 기준 커밋 | d111e66. 마지막 코드 커밋. 뒤 커밋은 기록과 문서다 |
| 앞 묶음과 갈라진 지점 | 42cf9b3 (origin/main. 2026-09-11) |

이 Task의 backend 커밋은 여섯이다.

| 해시 | 무엇 | 단계 |
|---|---|---|
| 55d0896 | 재고 hold와 commit과 releaseHeld와 releaseSold, findRangeForUpdate, InventoryAllocationService, 테스트 10건 | 4와 5 |
| 5ca3464 | 예약 도메인과 앱 서비스와 저장소, 테스트 38건 | 4와 5 |
| 3d84e83 | BOOK-01부터 BOOK-03 API와 guest_002, 테스트 4건 | 4와 5 |
| 8dbcc2a | 멱등 기록의 response_body 열을 text로 | 6 |
| f2f4da8 | BOOK-01 API 테스트 17건 | 6 |
| d111e66 | BOOK-02와 BOOK-03 API 테스트 8건 | 6-2 |

## 2. 프로덕션 코드 58개

경로는 backend/src/main/java/com/o2o/ 아래다.

### 2-1. booking/domain 24개

AppliedPromotion.java, Booking.java, BookingCreated.java, BookingId.java, BookingNotFoundException.java, BookingRepository.java, BookingStatus.java, DailyPrice.java, IdempotencyKey.java, IdempotencyKeyReusedException.java, IdempotencyRecord.java, IdempotencyRecordRepository.java, IdempotencyScope.java, InvalidBookingPeriodException.java, InvalidIdempotencyKeyException.java, InvalidPriceSnapshotException.java, OccupancyExceededException.java, PriceChangedException.java, PriceSnapshot.java, RateNotConfiguredException.java, RequestInProgressException.java, StayPeriod.java, UserId.java, package-info.java

### 2-2. booking/application 8개

BookingApplicationService.java, IdempotencyRecordService.java, IdempotentRequestExecutor.java, IdempotentResult.java, PriceQuotePort.java, RequestBookingCommand.java, StoredResponse.java, package-info.java

### 2-3. booking/infrastructure 7개

BookingJpaRepository.java, IdempotencyRecordJpaRepository.java, InventoryAllocationConfiguration.java, JpaBookingRepository.java, JpaIdempotencyRecordRepository.java, RateOnlyPriceQuoteAdapter.java, package-info.java

### 2-4. booking/api 8개

ApiFormat.java, BookingController.java, BookingExceptionHandler.java, BookingResponse.java, InvalidDateFormatException.java, PageResponse.java, RequestBookingRequest.java, package-info.java

### 2-5. inventory/domain 새 파일 6개와 고친 파일 2개

새 파일: InsufficientHoldException.java, InsufficientSoldException.java, InventoryAllocationService.java, InventoryHeld.java, InventoryNotOpenedException.java, InventoryShortageException.java

고친 파일: DailyInventory.java(hold, commit, releaseHeld, releaseSold), DailyInventoryRepository.java(findRangeForUpdate)

### 2-6. inventory/infrastructure 고친 파일 2개

DailyInventoryJpaRepository.java와 JpaDailyInventoryRepository.java. findRangeForUpdate 한 질의씩이다.

### 2-7. shared 고친 파일 1개

ActorRegistry.java. guest_002 한 줄과 주석이다. 근거는 계약 6절 P07 행.

## 3. 테스트 코드 10개

경로는 backend/src/test/java/com/o2o/ 아래다.

| 파일 | 건수 | 성격 | 8-1절 |
|---|---|---|---|
| booking/domain/StayPeriodTest.java | 4 | 단위 | K1 |
| booking/domain/PriceSnapshotTest.java | 9 | 단위 | K2 |
| booking/domain/BookingTest.java | 5 | 단위 | K3 |
| booking/domain/IdempotencyRecordTest.java | 6 | 단위 | K6 |
| inventory/domain/DailyInventoryAllocationTest.java | 10 | 단위 | K4, K5 |
| booking/api/RequestBookingRequestTest.java | 4 | 단위 | K6 |
| booking/application/BookingApplicationServiceTest.java | 8 | MySQL 통합. 트랜잭션을 감싸지 않는다 | K7부터 K11 |
| booking/application/BookingEventTest.java | 2 | MySQL 통합. 테스트 전용 구독자 | K12 |
| booking/api/BookingApiTest.java | 17 | 실제 포트. 잠금 경합 둘 | K13부터 K23 |
| booking/api/BookingQueryApiTest.java | 8 | 실제 포트 | K24, K25 |

합계 73건이다. 저장소 전체는 196건이고 나머지 123건은 앞 묶음 것이다.

## 4. 평가 대상이 아닌 것

| 무엇 | 왜 |
|---|---|
| catalog 패키지 전부, inventory의 api와 application, shared의 나머지 | 앞 묶음에서 평가받는 대상이다 |
| promotion, search, payment 패키지 | 병렬 세션 P와 Y의 것이다. 이 브랜치에 없고 1차는 import하지 않는다 |
| build.gradle, docker-compose.yml, 설정 파일 | 이 Task가 바꾸지 않았다. hold-ttl 키는 코드 기본값이다(계약 7절 D-3) |
| backend/CLAUDE.md와 backend/.claude/rules/ | 코드가 아니다 |

## 5. 실행 결과 파일

| 경로 | 무엇 |
|---|---|
| harness/out/task-S9-booking-R1/step5/ | 5단계 junit 21파일. 171건 실패 0 |
| harness/out/task-S9-booking-R1/step6/ | 6단계 junit 23파일과 http-calls.txt 18회 |
| harness/out/task-S9-booking-R1/step6-2/ | 6-2단계 junit 23파일과 http-calls.txt 12회. 196건 실패 0 |
| harness/out/task-S9-booking-R1/step9-verification.md | 검증 표와 회고 표 |
