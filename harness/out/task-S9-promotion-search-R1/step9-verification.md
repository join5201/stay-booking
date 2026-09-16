# task-S9-promotion-search 9단계 백엔드 검증 표와 회고 표

최초 작성: 2026-09-12
최종 갱신: 2026-09-12

대상: 작업 계약 task-S9-promotion-search 8절 9단계.
계약 경로: harness/tasks/task-S9-promotion-search.md

이 문서가 이 바퀴의 두 번째 산출물이다. 첫째는 코드이고 둘째가 이 표다. 앞 두 묶음의 step9-verification.md와 같은 골격을 쓴다.

이번 바퀴의 전제 셋(2026-09-11 사용자 지시, 계약 8절과 다른 곳은 이쪽이 우선). 정지점은 9단계 하나이고 2단계부터 6-2단계까지 사용자 확인 없이 이었다. N9는 세션 단위로 읽어 예약과 결제 세션이 나란히 돈다. 블라인드 평가는 MVP 코드가 다 나올 때까지 미룬다(D-4 보류).

## 1. 검증 ID 대조표

계약 9절이 지정한 계약 테스트 ID는 T27과 T28 전부, T12와 T13 부분이다. 근거는 10-6 기능별 API와 검증 연결 표의 프로모션과 검색 행이다.

### 1-1. T27

11 검증 기준의 문장은 이렇다. 가용성은 있지만 인원 초과 또는 요금 누락. 기대는 availability=false와 reasons, 검색에서 제외다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| 인원 초과가 200과 available=false와 reasons OCCUPANCY_EXCEEDED | 통과 | SearchApiTest 인원 초과 건. 재고 3에 요금이 있는 객실을 3명으로 조회하면 availableCount 3인 채 available=false |
| 요금 누락이 missingRateDates와 reasons RATE_NOT_CONFIGURED | 통과 | V14. 3박 중 둘째와 셋째 날 요금이 없으면 두 날짜가 missingRateDates에 든다 |
| 재고 미등록이 days의 null과 missingInventoryDates와 INVENTORY_NOT_CONFIGURED | 통과 | V14. 둘째 날 재고 레코드가 없으면 days[1].availableCount가 null이고 최상위 availableCount는 0 |
| 재고 0이 INVENTORY_UNAVAILABLE | 통과 | V13. 둘째 날 재고 0이면 available=false이고 reasons 하나 |
| 가용성 부족은 400이 아니라 200 | 통과 | V13과 V14 전부 200. 400은 형식 오류와 날짜 순서와 30박 초과와 과거 체크인뿐(INVALID_DATE_RANGE) |
| 검색에서 제외 | 통과 | V15. 인원 초과와 재고 0과 요금 누락과 재고 누락 객실 넷이 전부 빠지고 조건을 다 갖춘 객실 하나만 남는다 |
| 그런 객실만 있는 숙소는 뺀다 | 통과 | 같은 테스트. 제외 객실만 가진 숙소와 객실이 없는 숙소가 items에 없다. totalElements 1 |
| reasons의 순서 | 통과 | V14에서 INVENTORY_NOT_CONFIGURED 다음 RATE_NOT_CONFIGURED. 11 Availability 표의 순서다 |
| 실제 HTTP에서 같은 결과 | 통과 | step6-2/http-calls.txt의 SEARCH-02 셋과 SEARCH-01 둘. 재고 0인 객실이 검색에서 빠지고 3명 조회는 빈 items |

T27 판정: 통과.

가용성 부족을 오류로 만들지 않은 것이 이 항목의 요점이다. 검색과 가용성 조회는 예약 가능 여부를 묻는 질문이고 아니오도 답이다. 그래서 200과 available=false다. 400은 질문 자체가 잘못됐을 때만이다.

### 1-2. T28

문장은 이렇다. 할인 소수점과 동률 후보. 기대는 날짜별 버림, 날짜 합계 일치, 선택 결과 재현이다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| 할인 총액이 내림이다 | 통과 | V6. 33333, 33333, 33334에 10퍼센트면 총액 100000의 10퍼센트인 10000 |
| 날짜별 배분이 비례 내림이고 단수는 마지막 날짜 | 통과 | V6. 3333, 3333, 3334. 셋째 날에 단수 1이 붙는다. 06-4 118행 |
| 큰 수에서도 넘치지 않는다 | 통과 | PriceCalculationTest. 30박 10억원 99퍼센트를 BigInteger로 배분하고 합이 맞는다 |
| 배분 합이 할인 총액과 같다(I10) | 통과 | V6과 V7. 단위 테스트가 합을 센다 |
| 행 수가 박수와 같다(I11) | 통과 | V7. 3박이면 days 셋 |
| 총액이 날짜별 (단가 - 배분액)의 합(I15) | 통과 | V7 단위, V16 HTTP. SEARCH-03의 finalAmount 합이 totalAmount이고 PROMO-05의 discountAmount가 같은 계산 |
| 동률 후보는 ID 오름차순 하나 | 통과 | V8. 같은 할인율 둘이면 id가 앞선 것이 selected이고 나머지는 후보로 남는다 |
| 선택 결과가 재현된다 | 통과 | V8. 같은 입력으로 두 번 계산해 같은 선택 |
| 검색과 견적과 PROMO-05가 같은 계산을 쓴다 | 통과 | SearchApiTest의 lowestTotalAmount 건(10퍼센트 반영 144000). step6-2에서 같은 객실의 SEARCH-01 totalAmount와 SEARCH-03 totalAmount가 180000으로 같다. 셋 다 PricingService 하나다(D-3 가) |
| 실제 HTTP에서 같은 결과 | 통과 | step6-2/http-calls.txt의 V16 호출. baseTotalAmount 200000, discountTotalAmount 20000, totalAmount 180000 |

T28 판정: 통과.

식이 둘이라는 것을 적어 둔다. 11 명세는 날짜별로 내림해 더하는 식이고 06-4 118행은 총액을 먼저 내리고 비례 배분하는 식이다. 두 식은 총액이 다를 수 있다. 계약 6절이 06-4를 골랐고 코드가 그쪽이며 PriceCalculation 주석에 두 식이 다름을 적었다. 이 표의 통과는 06-4 식 기준이다.

### 1-3. T12

문장은 이렇다. 검색 이후 가격 변경. 기대는 PRICE_CHANGED와 새 가격 동의 전 예약 생성 없음이다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| 검색 시점의 가격이 견적으로 나온다 | 통과 | SEARCH-03. estimatedAt과 함께 날짜별 단가와 할인과 총액이 나온다. 예약 요청이 보낼 예상 총액이 이 값이다 |
| 견적과 예약이 같은 계산기를 쓸 수 있다 | 통과 | PricingService.quote(roomTypeId, checkIn, checkOut)가 도메인 서비스 빈이고 인터페이스가 계약 표에 고정돼 있다. 예약 묶음이 같은 빈을 부른다 |
| 요금 변경이 견적에 바로 반영된다 | 통과 | 견적은 저장하지 않고 매번 계산한다(SearchApplicationService). RATE-02로 요금을 바꾸면 다음 견적이 새 값이다 |
| PRICE_CHANGED 판정 | 미완 | 예약 요청의 예상 총액과 서버 금액을 비교하는 곳이 BOOK-01이다. 범위 밖 |
| 동의 전 예약 생성 없음 | 미완 | 범위 밖 |

T12 판정: 부분. 계약이 처음부터 부분으로 적었다(V17 이월). 이번 묶음이 기여한 것은 비교의 한쪽 값과 비교에 쓸 계산기다.

### 1-4. T13

문장은 이렇다. 예약 생성 이후 요금과 프로모션 변경. 기대는 기존 스냅샷과 실제 청구액 유지다.

| 판정 항목 | 결과 | 무엇이 확인했나 |
|---|---|---|
| 스냅샷이 값이다 | 통과 | PriceSnapshot과 PriceDay와 AppliedPromotion이 레코드다. Promotion 엔티티를 참조하지 않고 id와 name과 discountRate를 복사한다. 프로모션이 나중에 바뀌어도 만들어진 스냅샷은 바뀔 길이 없다 |
| 프로모션 변경이 다른 것을 건드리지 않는다 | 통과 | V9와 V10. PROMO-02는 version을 대조해 자기 행만 바꾸고 이벤트 PromotionUpdated를 낸다. 06-4 프로모션 계약표 update 행의 Post 절반 |
| 지난 version은 거절되고 최근 변경이 남는다 | 통과 | V9. 409 뒤 다시 읽으면 성공했던 값 |
| 기존 스냅샷 무영향(I4) | 미완 | 저장된 스냅샷이 아직 없다. Booking이 스냅샷을 담는 것은 예약 묶음이다 |
| 실제 청구액 유지 | 미완 | 결제 묶음 |

T13 판정: 부분. 계약이 처음부터 부분으로 적었다(V18 이월). 이번 묶음이 기여한 것은 스냅샷을 값으로 만든 것이다. 참조로 만들었다면 예약 묶음이 동결을 코드로 따로 만들어야 했다.

### 1-5. 8-1절 항목별 대조

| ID | 확인할 것 | 결과 | 테스트 |
|---|---|---|---|
| V1 | 캠페인 시작일이 종료일보다 늦으면 거부, 같거나 앞서면 통과 | 통과 | ConditionTest 2건 |
| V2 | discountRate 0과 100 거부, 1과 99 통과 | 통과 | PromotionTest 2건, PromotionApiTest 1건 |
| V3 | regionCodes 중복 거부, 빈 배열은 전체 지역 | 통과 | ConditionTest 3건(100개 통과와 101개 거부 포함) |
| V4 | 숙박 기간 한쪽만 오면 거부, 둘 다 없으면 통과 | 통과 | ConditionTest 3건, PromotionApiTest 2건 |
| V5 | isApplicable이 지역과 박수와 캠페인 기간을 다 본다 | 통과 | ConditionTest 4건(캠페인 양끝 포함), PromotionApplicationServiceTest의 DB 몫 |
| V6 | 비례 내림 뒤 마지막 날짜 가산, 합이 할인 총액 | 통과 | PriceCalculationTest 3건 |
| V7 | 행 수가 박수, 총액이 날짜별 합 | 통과 | PriceCalculationTest 2건 |
| V8 | 동률은 ID 오름차순 하나, 재현 | 통과 | PriceCalculationTest 2건 |
| V9 | 지난 version 409와 최근 변경 유지 | 통과 | PromotionTest 1건, PromotionApplicationServiceTest 1건, PromotionApiTest 1건 |
| V10 | 생략 필드 유지, 합친 상태에서 날짜 순서 | 통과 | PromotionTest 4건, PromotionApplicationServiceTest 1건, PromotionApiTest 3건(유지와 순서, 해제와 한쪽만, 명시적 null과 version만) |
| V11 | PROMO-05 요금 없는 날짜 409 RATE_NOT_CONFIGURED | 통과 | PromotionApplicationServiceTest 1건, PromotionApiTest 1건 |
| V12 | PROMO-05 인원 초과 409, 후보 없음은 빈 배열과 null | 통과 | PromotionApplicationServiceTest 2건, PromotionApiTest 1건 |
| V13 | SEARCH-02 가용성 부족 200과 available=false, 형식 오류만 400 | 통과 | SearchApiTest 3건 |
| V14 | missingInventoryDates와 missingRateDates | 통과 | SearchApiTest 1건. days의 null도 같은 건 |
| V15 | SEARCH-01 제외 규칙과 숙소 제외 | 통과 | SearchApiTest 2건(제외 넷, 빈 items) |
| V16 | SEARCH-03 finalAmount 합이 totalAmount | 통과 | SearchApiTest 1건 |
| V17 | 검색 이후 가격 변경 PRICE_CHANGED | 이월 | 예약 묶음 |
| V18 | 예약 생성 이후 프로모션 변경 무영향 | 이월 | 예약 묶음 |
| V19 | 종료된 프로모션 재개 불가 | 이월 | D-2 다. CLOSED 상태 자체를 이번에 안 만들었다 |

열여섯이 통과이고 셋이 이월이다. 이월 셋은 계약 8-1절에 사유와 함께 적혀 있다.

### 1-6. 계약표 후행조건 대조

06-4 1-2 프로모션 계약표의 Post 열이다.

| 행동 | Post | 결과 |
|---|---|---|
| create | 생성, PromotionCreated 발행 | 통과. PromotionEventTest. enabled 값과 함께 발행 |
| update | PromotionUpdated 발행, 기존 스냅샷 무영향 | 발행은 통과. 무영향은 1-4의 미완과 같다 |
| close | CLOSED, PromotionClosed 발행 | 해당 없음. D-2 다로 이월 |
| isApplicable 조회 | 지역과 최소 숙박일과 캠페인 기간 안일 때만 참 | 통과. V5. 다만 양끝 포함은 계약 6절이 11 명세의 끝 날짜 제외로 바꿨다 |
| 거절된 요청 | 발행 없음 | 통과. PromotionEventTest |

받는 곳은 아직 없다. 06-4 2-1 트리거 맵핑에서 PromotionUpdated를 받는 것은 검색 프로젝션이고 D-1 나로 이번에 없다. 그래서 발행 누락이 다른 테스트로는 안 잡힌다. 앞 묶음과 같은 이유로 이벤트 테스트를 따로 둔다.

### 1-7. 불변식 대조

| 번호 | 문장 | 어디서 지키나 | 확인 |
|---|---|---|---|
| I8 | 캠페인 시작일은 종료일보다 늦지 않다 | Condition.of | 통과. V1. 같은 날은 VO를 지나고 Promotion.create가 11 명세의 빈 기간 금지로 거절한다 |
| I10 | 배분 합이 할인 총액 | PriceCalculation.allocate | 통과. V6 |
| I11 | 행 수가 박수 | PriceCalculation.calculate | 통과. V7 |
| I13 | 종료 불가역 | 없음 | 이월. D-2 다 |
| I15 | 총액이 날짜별 합 | PriceSnapshot.of | 통과. V7과 V16 |

I10과 I11과 I15의 검사가 PriceSnapshot 생성자가 아니라 만드는 쪽과 테스트에 있는 것이 계약 D-3의 경계다. 06-4는 셋을 PriceSnapshot VO에 둔다. 예약 묶음이 Booking에 스냅샷을 담을 때 생성자 검증을 넣으면 이 테스트가 그 검증의 통과 짝이 된다. 5단계 진행 행에 적어 두었다.

### 1-8. 실행 결과

| 항목 | 값 |
|---|---|
| 테스트 | 209건. 실패 0, 오류 0, 건너뜀 0 |
| 그중 이 묶음이 더한 것 | 86건. 도메인 31, 앱 16, API 39 |
| g1 code | 5단계 10건, 6단계 5건, 6-2단계 2건 전부 PASS |
| 살아 있는 서버 HTTP 호출 | step6 19회, step6-2 14회. 전부 명세의 상태 코드 |
| 서버 포트와 DB | 8085, o2o_promo_test. 8080과 o2o_catalog_test는 나란히 도는 세션이 쓴다 |
| 결과 파일 | harness/out/task-S9-promotion-search-R1/ 아래 step5, step6, step6-2 |

### 1-9. 기존 파일 변경

계약이 만들지 않는 것으로 적은 기존 공유 파일 수정은 없다. 카탈로그 기존 파일 여섯에 읽기 메서드 둘을 더했고 기존 메서드와 애그리거트는 바꾸지 않았다. 사용자 인터페이스 표의 허용 범위(리포지토리 조회 메서드 추가)다.

| 파일 | 추가 |
|---|---|
| catalog/domain/PropertyRepository.java | findAllByRegionCode(String) |
| catalog/domain/RoomTypeRepository.java | findAllByPropertyId(PropertyId) |
| catalog/infrastructure/PropertyJpaRepository.java | 같은 이름의 JPQL 질의. id 오름차순 |
| catalog/infrastructure/RoomTypeJpaRepository.java | 같은 이름의 JPQL 질의. id 오름차순 |
| catalog/infrastructure/JpaPropertyRepository.java | 위임 한 줄 |
| catalog/infrastructure/JpaRoomTypeRepository.java | 위임 한 줄 |

여섯 파일 합계 43줄 추가, 삭제 0줄. 커밋 888b278.

## 2. 회고 표

단계 번호는 계약 8절 기준이다.

| 단계 | 실제 시간 | 막힌 것 | 하네스가 도움이 됐나 | 하네스가 방해했나 |
|---|---|---|---|---|
| 1 계약과 결정 승인 | 29분 | 없음 | 결정 넷을 1단계에 모아 이후 개정이 0회였다 | 없음 |
| 2 이슈와 브랜치 | 4분 | 없음 | 앞 묶음의 교훈대로 단계 커밋을 Refs로 걸어 이슈가 조기에 닫히지 않았다 | 없음 |
| 3 백엔드 틀 | 건너뜀 | 없음 | 앞 묶음이 세운 것을 그대로 썼다 | 없음 |
| 4 도메인 코드 | 55분 | 11 명세와 06-4의 할인 식이 달랐다 | 계약 6절이 미리 골라 두어 코드에서 다시 결정하지 않았다 | 명령 한 번에 큰 파일 여럿을 쓰면 셸이 깨진다. 하네스 문제는 아니고 도구 한계 |
| 5 테스트 | 8분 | 없음 | 8-1절의 V 목록이 테스트 파일의 목차가 됐다 | 없음 |
| 6 API 다섯 | 15분 | PATCH의 생략과 명시적 null 구분. jackson 3의 레코드 처리 | F15가 기억 대신 javap으로 확인하게 했고 그 결과가 원인을 찾았다 | 없음 |
| 6-2 API 셋 | 16분 | 검색 결과 순서를 등록 순으로 가정한 테스트 1건 | 6단계가 세운 ApiDate와 PageResponse와 핸들러를 그대로 썼다 | 없음 |
| 7과 8 프론트 | 해당 없음 | 없음 | 계약이 처음부터 뺐다 | 없음 |
| 9 검증 | 이 문서. 분은 progress.md 행 | 없음 | 검증 ID와 테스트를 잇는 표가 I10과 I11과 I15의 위치가 06-4와 다름을 다시 드러냈다 | 없음 |

측정된 개발 시간의 합은 94분이다. 4단계 55분, 5단계 8분, 6단계 15분, 6-2단계 16분이다. 1단계와 2단계는 승인과 브랜치라 개발 시간이 아니다. 세션이 중간에 두 번 끊겨 6단계가 두 구간으로 나뉘었고 합산은 progress.md 행에 있다.

## 3. 회고 질문 넷

### 3-1. 한 바퀴에 실제로 몇 분이 들었나

개발 94분이다. 앞 묶음의 55분보다 길다. API는 여덟로 하나 적은데 시간이 늘었다.

늘어난 자리가 4단계 하나다. 55분 중 대부분이 도메인 코드이고 이유가 셋이다.

| 이유 | 크기 |
|---|---|
| 도메인 클래스 수가 앞 묶음의 두 배다. 예외 여덟, 값 객체 여섯, 계산기와 서비스 | 37 클래스 |
| 계산 규칙이 있다. 앞 두 묶음은 저장과 조회였고 이번은 배분과 선택이 있다 | PriceCalculation과 그 테스트 |
| 큰 파일을 도구로 못 써서 나눠 썼다 | 셸 한계로 세 번 다시 썼다 |

5단계와 6-2단계는 앞 묶음과 비슷하거나 짧다. 계약이 골라 둔 것과 앞 묶음의 골격이 그대로 들었다.

### 3-2. 하네스 때문에 늘어난 시간이 얼마인가

| 늘린 것 | 값어치 |
|---|---|
| 계약 8-1절의 테스트 목록 | V8의 재현 확인과 V14의 두 목록 분리가 여기 없었으면 빠졌다 |
| 결과 파일 사본과 g1 | 미실행을 통과로 못 바꾼다 |
| F15 검증 규칙 | 6단계 PATCH 실패의 원인을 기억이 아니라 javap으로 잡았다. 이 규칙이 없었으면 애너테이션을 바꿔 가며 시도했을 것이다 |
| progress.md 행마다 실제 시간 | 시각을 먼저 쓰고 시계를 나중에 본 것이 두 번 있었고 둘 다 커밋 전에 고쳤다 |

이번에 눈에 띈 것은 하네스가 잡지 못한 자리가 아니라 하네스 밖의 것이 잡은 자리다. 실행 환경이 두 번 바뀌어 세션이 끊겼고 그때마다 브랜치와 미커밋 파일을 git status로 다시 확인했다. 그 확인 절차가 CLAUDE.md 5-1의 행에 있어서 빠뜨리지 않았다.

### 3-3. 빌드와 테스트가 잡은 결함과 사람이 잡은 결함의 비율

| 잡은 주체 | 무엇 | 건수 |
|---|---|---|
| 빌드와 테스트 | PATCH 레코드의 생략 처리 실패 4건(원인 하나), 검색 순서 가정 1건 | 2 |
| 검사기 | 없음 | 0 |
| 나 | 11 명세와 06-4의 할인 식 차이, I10과 I11과 I15의 위치 차이, 캠페인 양끝 차이, 필수 쿼리 누락의 응답 모양 | 4 |
| 사용자 | 이번 바퀴에는 없음. 정지점이 하나라 중간 확인이 없었다 | 0 |

사용자가 잡은 것이 0인 것은 좋은 신호가 아니다. 정지점을 하나로 줄인 대가다. 그 대신 이 표가 넷을 드러내고 아래 5절이 그것을 다음으로 넘긴다.

### 3-4. 동결 중 관찰이 몇 건 쌓였고 그중 실제로 필요한 것은 몇 건인가

이번 바퀴에 진행 행의 안 해 본 것 칸에 쌓인 것은 다섯이다.

| 무엇 | 고쳐야 하나 |
|---|---|
| 필수 쿼리 파라미터 누락이 스프링 기본 400 본문이다. ErrorResponse 모양이 아니다 | 그렇다. 공유 핸들러에 MissingServletRequestParameterException 처리가 필요하다. 공유 파일이 동결이라 이번에 안 했다 |
| shared의 InvalidDateFormatException 핸들러가 promotion/api에 있다 | 그렇다. 공유 핸들러 동결이 풀리면 그쪽으로 옮긴다 |
| 마지막 날짜 가산이 그 날짜 단가를 넘는 극단 | 아니다. 단가 1원 옆에 10억원 29박 같은 값이고 계약 정책 그대로 둔다. 알아 둘 것이다 |
| create-drop 종료 때 promotion_region_code FK drop 실패 로그 | 아니다. 한 DB에 컨텍스트 여럿이 뜨는 테스트 구성의 부산물이고 결과에 영향이 없다 |
| 검색 결과 배열의 정렬 기준이 명세에 없다 | 판단 대기. id 오름차순으로 두었다. 프로젝션이 생기면 그쪽 정렬이 정본이 된다 |

앞 둘이 공유 핸들러 하나에 모인다. 다음 묶음이 공유 파일을 열 때 같이 처리하면 된다.

## 4. 이 Task의 완료 조건 대조

계약 작업 표의 완료 기준이다.

| 조건 | 결과 |
|---|---|
| 계약 테스트 ID가 전부 통과이거나 미실행 사유와 함께 기록 | 충족. T27과 T28은 통과, T12와 T13은 계약이 처음부터 부분으로 적었고 그 구간을 1-3과 1-4에 적었다 |
| 8절 단계가 전부 끝난다 | 충족. 3단계는 건너뜀, 7과 8단계는 해당 없음, 나머지 전부 applied |
| 기계 판독 결과 파일에서 실행 수가 0이 아니고 실패 수가 0 | 충족. 209건, 실패 0 |
| 모든 단계의 실제 시간이 분 단위로 기입 | 충족. 1, 2, 4, 5, 6, 6-2, 9단계 전부 progress.md에 분 단위 |

## 5. 남은 것

| 무엇 | 어디로 |
|---|---|
| V17과 T12의 PRICE_CHANGED 몫 | 예약 묶음. BOOK-01이 예상 총액과 PricingService.quote를 비교할 때 |
| V18과 T13의 스냅샷 동결 몫 | 예약 묶음. Booking이 PriceSnapshot을 담을 때 I10과 I11과 I15의 생성자 검증도 그때 |
| V19와 I13과 CLOSED 상태 | D-2 다로 이월. 시점은 사용자가 정한다 |
| 검색 프로젝션과 이벤트 구독 | D-1 나로 이월. 예약 묶음 뒤 |
| 공유 핸들러의 필수 쿼리 누락 처리와 InvalidDateFormatException 핸들러 이동 | 공유 파일 동결이 풀리는 묶음 |
| PriceSnapshotResponse의 위치 | 지금은 promotion/api다. 예약 묶음이 쓰면 shared 규칙 3-2로 옮긴다 |
| 블라인드 평가 A와 B | D-4 보류. MVP 코드가 다 나온 뒤 사용자가 시점을 정한다 |
| 프론트 화면과 연결 테스트 | 별도 Task |
