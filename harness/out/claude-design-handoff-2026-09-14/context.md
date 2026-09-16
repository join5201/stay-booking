# 클로드 디자인 컨텍스트: O2O 숙박 예약 MVP 프론트

최초 작성: 2026-09-14
최종 갱신: 2026-09-14

이 문서는 같은 폴더의 prompt.md와 함께 Claude Design에 넘기는 사실 묶음이다. 출처는 document/02-o2o-feature-list.md(기능 목록), document/11-o2o-api-spec.md(API 명세), document/06-6-o2o-design-digest.md(설계 핵심 요약)이고, 개발용 행위자 ID는 backend/src/main/java/com/o2o/shared/ActorRegistry.java에서 확인했다. 요약이 출처와 다르면 출처가 맞다. 새 결정은 담지 않았다. 백엔드는 저장소 main(커밋 1bdadfe, 2026-09-13 PR 138 병합)이고 확인 날짜는 전부 2026-09-14다.

## 1. 서비스 한 줄

호스트가 숙소와 객실, 날짜별 재고와 요금을 올린다. 게스트가 지역과 날짜와 인원으로 검색해 예약하고 Mock 결제로 확정한다. 운영자가 자동으로 적용되는 정률 할인(프로모션)을 만든다. 로컬에서 돌리는 MVP이고 배포와 운영은 범위 밖이다.

시스템이 지키는 보장 다섯. 화면은 이 보장을 깨는 동작을 유도하면 안 된다.

| 번호 | 보장 | 화면에 미치는 뜻 |
|---|---|---|
| R1 | 연박은 전 날짜가 선점되거나 전무다 | 날짜 일부만 예약되는 화면은 없다 |
| R2 | 동시 요청에도 초과 예약이 없다 | 예약 요청이 409로 거절될 수 있음을 화면이 전제한다 |
| R3 | 같은 요청이 반복돼도 예약은 하나다 | 재시도는 같은 멱등키로 보낸다 |
| R4 | 선점으로 끝난 예약의 재고는 돌아온다 | 만료된 예약은 그냥 만료 상태로 보여 주면 된다. 화면이 되돌릴 일이 없다 |
| R5 | 확정된 예약의 금액은 이후 요금이나 할인이 바뀌어도 변하지 않는다 | 예약 상세는 예약 안의 금액 스냅샷을 보여 준다. 다시 계산하지 않는다 |

## 2. 기술 구성과 범위

| 항목 | 값 |
|---|---|
| 프론트 | Next.js. 아직 코드가 없다. 이 설계가 그 입력이다 |
| 백엔드 | Spring Boot, Java 21, MySQL. 완성. 로컬에서 http://localhost:8080 (포트 설정이 없어 Spring 기본값) |
| API 기본 경로 | /api/v1. 요청과 응답은 JSON, 필드 이름은 camelCase |
| 인증 | 없다. 개발용 행위자 헤더(3절) |
| 배포 | 없다. 로컬 개발과 검증까지 |
| 브라우저 교차 출처 | 백엔드에 교차 출처 허용 설정이 없다. 프론트를 같은 출처로 보이게 하는 방법은 프론트 구현 Task에서 정한다. 화면 설계에는 영향이 없다 |
| 화면 문구 | 한국어 |

## 3. 행위자와 개발용 행위자

| 역할 | 하는 일 | 개발용 ID |
|---|---|---|
| 공개 (헤더 없음) | 숙소와 객실 조회, 검색, 가용성, 예상 금액, 적용 가능 프로모션 조회 | 없음 |
| HOST | 본인 숙소와 객실 등록과 수정, 날짜별 재고와 요금 관리 | host_001, host_002 |
| OPERATOR | 프로모션 등록, 수정, 관리 목록 조회 | operator_001 |
| GUEST | 본인 예약 생성, 조회, 결제 요청, 취소 | guest_001, guest_002 |
| MOCK_SYSTEM | Mock 결제 결과 전달. 서비스 API에서 쓰지 않고 프론트도 쓰지 않는다 | mock_001 |

규칙

- 헤더 이름은 X-Dev-Actor-Id이고 값은 위 ID다. 실제 인증이 아니고 개발 프로파일에서만 산다.
- 필요한 헤더가 없거나 미등록 ID면 401 ACTOR_REQUIRED, 역할이 다르면 403 ACCESS_DENIED, 남의 자원이면 404 RESOURCE_NOT_FOUND다. 남의 자원은 있는지 없는지도 알려 주지 않는다.
- 화면에는 행위자 전환 컨트롤 하나면 된다. 역할이 바뀌면 그 역할의 첫 화면으로 간다. 로그인, 회원가입, 비밀번호 화면은 없다.
- 호스트 둘과 게스트 둘이 있는 이유는 남의 자원이 404가 되는 것을 눈으로 확인하기 위해서다.

## 4. 화면 흐름 후보

후보다. Claude Design이 고쳐 낼 수 있다. 화면 ID는 이 문서 안에서만 쓰는 임시 이름이다.

게스트

| ID | 화면 후보 | 무엇을 보고 하나 | 부르는 API |
|---|---|---|---|
| G1 | 검색 | 지역 코드, 체크인, 체크아웃, 인원 넷을 입력한다(전부 필수). 결과는 숙소 카드 목록이고 카드마다 최저 총액과 예약 가능한 객실 타입(이름, 최대 인원, 가용 수, 총액) | SEARCH-01 |
| G2 | 숙소 상세 | 숙소 정보와 객실 타입 목록 | CAT-03, CAT-09 |
| G3 | 객실 상세 | 객실 정보, 날짜별 가용 수, 예상 금액(날짜별 기본가와 할인액, 적용된 프로모션 하나), 적용 가능 프로모션 목록(선택된 것 표시) | CAT-08, SEARCH-02, SEARCH-03, PROMO-05 |
| G4 | 예약 요청 | 확인 화면. 방금 본 예상 총액을 expectedTotalAmount로 함께 보낸다. 성공하면 HELD 예약과 만료 시각을 받는다 | BOOK-01 |
| G5 | 결제 | 남은 시간, 금액, 시도 횟수(최대 3), Mock 결제 요청 버튼. 결과는 확정, 실패(남은 횟수 안에서 재시도), 처리 중 셋 | PAY-01, PAY-02, BOOK-03 |
| G6 | 예약 목록 | 상태 필터(HELD, CONFIRMED, CANCELED, EXPIRED)와 페이지 | BOOK-02 |
| G7 | 예약 상세 | 상태, 금액 스냅샷, 결제 시도와 환불, 취소 버튼(CONFIRMED이고 체크인 전일 때만) | BOOK-03, BOOK-04 |

호스트

| ID | 화면 후보 | 무엇을 보고 하나 | 부르는 API |
|---|---|---|---|
| H1 | 내 숙소 목록 | 본인 숙소만, 페이지 | CAT-05 |
| H2 | 숙소 등록과 수정 | 이름, 지역 코드, 주소, 설명. 수정은 version을 함께 보낸다 | CAT-01, CAT-02, CAT-03 |
| H3 | 객실 타입 목록과 등록과 수정 | 이름, 최대 인원, 설명. 수정은 version | CAT-06, CAT-07, CAT-08, CAT-09 |
| H4 | 재고 달력 | 기간 조회(최대 366일). 날짜마다 총 재고, 선점, 판매, 가용. 레코드 없는 날짜는 누락으로 표시. 날짜별 등록과 수정, 기간 일괄 등록 | INV-01, INV-02, INV-03, INV-04, INV-05 |
| H5 | 요금 달력 | 기간 조회. 날짜마다 금액. 누락 날짜 표시. 날짜별 등록과 수정. 일괄 등록 API는 없다 | RATE-01, RATE-02, RATE-03, RATE-04 |

운영자

| ID | 화면 후보 | 무엇을 보고 하나 | 부르는 API |
|---|---|---|---|
| O1 | 프로모션 목록 | 사용 여부 필터, 페이지 | PROMO-04 |
| O2 | 프로모션 등록과 수정 | 이름, 할인율(1~99), 캠페인 기간, 숙박 기간(선택. 둘 다 있거나 둘 다 없음), 최소 박수(1~30), 지역 코드 목록(빈 목록은 전체 지역), 사용 여부. 수정은 version. 종료는 사용 여부를 끄는 것이다 | PROMO-01, PROMO-02, PROMO-03 |

공통

| ID | 화면 후보 | 무엇을 보고 하나 |
|---|---|---|
| C1 | 행위자 전환 | 위쪽 고정. 현재 ID와 역할 표시, 다섯 ID 중 선택, 공개(헤더 없음) 선택 |
| C2 | 오류 표시 | 8절의 코드별 반응을 한 자리에서 처리하는 공통 요소 |

## 5. API 33개

서비스 API 32개와 로컬 Mock API 1개. 경로는 /api/v1 아래다(INTERNAL-01만 /internal). 행위자 열의 소유자는 그 역할이면서 자원의 주인이어야 한다는 뜻이다.

| ID | 메서드와 경로 | 행위자 | 하는 일 | 화면 후보 |
|---|---|---|---|---|
| CAT-01 | POST /properties | HOST | 숙소 등록. 201과 Location | H2 |
| CAT-02 | PATCH /properties/{propertyId} | HOST 소유자 | 숙소 수정. version 필수, 바꿀 필드 1개 이상 | H2 |
| CAT-03 | GET /properties/{propertyId} | 공개 | 숙소 상세 | G2, H2 |
| CAT-04 | GET /properties | 공개 | 숙소 목록. regionCode 필터 선택 | 필요하면 G1의 대안 |
| CAT-05 | GET /host/properties | HOST | 본인 숙소 목록 | H1 |
| CAT-06 | POST /properties/{propertyId}/room-types | HOST 소유자 | 객실 타입 등록 | H3 |
| CAT-07 | PATCH /room-types/{roomTypeId} | HOST 소유자 | 객실 타입 수정. version 필수 | H3 |
| CAT-08 | GET /room-types/{roomTypeId} | 공개 | 객실 타입 상세 | G3, H3 |
| CAT-09 | GET /properties/{propertyId}/room-types | 공개 | 숙소의 객실 타입 목록 | G2, H3 |
| INV-01 | POST /room-types/{roomTypeId}/inventories | HOST 소유자 | 날짜별 재고 등록 (date, totalCount) | H4 |
| INV-02 | POST /room-types/{roomTypeId}/inventories/bulk | HOST 소유자 | 기간 재고 일괄 등록 (from, to, totalCount. 최대 366일) | H4 |
| INV-03 | PATCH /room-types/{roomTypeId}/inventories/{date} | HOST 소유자 | 날짜별 재고 수정 (totalCount, version) | H4 |
| INV-04 | GET /room-types/{roomTypeId}/inventories | HOST 소유자 | 기간 재고 조회. items와 missingDates | H4 |
| INV-05 | GET /room-types/{roomTypeId}/inventories/{date} | HOST 소유자 | 날짜별 재고 조회 | H4 |
| RATE-01 | POST /room-types/{roomTypeId}/rates | HOST 소유자 | 날짜별 요금 등록 (date, amount, currency) | H5 |
| RATE-02 | PATCH /room-types/{roomTypeId}/rates/{date} | HOST 소유자 | 날짜별 요금 수정 (amount, version) | H5 |
| RATE-03 | GET /room-types/{roomTypeId}/rates | HOST 소유자 | 기간 요금 조회. items와 missingDates | H5 |
| RATE-04 | GET /room-types/{roomTypeId}/rates/{date} | HOST 소유자 | 날짜별 요금 조회 | H5 |
| PROMO-01 | POST /promotions | OPERATOR | 프로모션 등록 | O2 |
| PROMO-02 | PATCH /promotions/{promotionId} | OPERATOR | 프로모션 수정. version 필수. enabled=false가 종료 | O2 |
| PROMO-03 | GET /promotions/{promotionId} | OPERATOR | 프로모션 상세 | O2 |
| PROMO-04 | GET /promotions | OPERATOR | 프로모션 관리 목록. enabled 필터 선택 | O1 |
| PROMO-05 | GET /room-types/{roomTypeId}/applicable-promotions | 공개 | 그 객실과 날짜와 인원에 적용 가능한 프로모션 목록과 선택 결과 | G3 |
| SEARCH-01 | GET /search/properties | 공개 | 숙소 검색. regionCode, checkIn, checkOut, guestCount 전부 필수 | G1 |
| SEARCH-02 | GET /room-types/{roomTypeId}/availability | 공개 | 연박 가용성. 날짜별 가용 수, 누락 날짜, 불가 사유 | G3 |
| SEARCH-03 | GET /room-types/{roomTypeId}/price-quote | 공개 | 예상 숙박 금액. 확정 금액이 아니다 | G3, G4 |
| BOOK-01 | POST /bookings | GUEST | 예약 요청. 멱등키 필수. 성공하면 HELD | G4 |
| BOOK-02 | GET /bookings | GUEST | 본인 예약 목록. status 필터 선택 | G6 |
| BOOK-03 | GET /bookings/{bookingId} | GUEST 소유자 | 예약 상세. 결제 요약과 serverNow 포함 | G5, G7 |
| PAY-01 | POST /bookings/{bookingId}/payment-attempts | GUEST 소유자 | Mock 결제 요청. 멱등키 필수. mockMode 선택(APPROVE, DECLINE, DEFER. 생략하면 APPROVE) | G5 |
| PAY-02 | GET /bookings/{bookingId}/payment-attempts | GUEST 소유자 | 결제 시도 목록 | G5, G7 |
| BOOK-04 | POST /bookings/{bookingId}/cancellations | GUEST 소유자 | 예약 취소. 멱등키 필수. reason 선택(최대 300자) | G7 |
| INTERNAL-01 | POST /internal/mock-payments/events | MOCK_SYSTEM | Mock 결제 결과 전달. 프론트는 부르지 않는다 | 없음 |

프론트가 갖지 않는 것: 결제 확정, 선점 해제, 환불, Mock 결과 전달. 전부 백엔드 안에서 일어나고 화면은 예약 상세를 다시 읽어 결과를 본다.

## 6. 응답 모델 핵심 필드

표시에 필요한 것만 추렸다. 전체는 document/11 응답 모델 절.

| 모델 | 핵심 필드 | 화면 메모 |
|---|---|---|
| Property | id, hostId, name(1~100자), regionCode(1~32자), address(1~300자), description(최대 2,000자), version, createdAt, updatedAt | 사진과 좌표는 없다 |
| RoomType | id, propertyId, name, maxOccupancy(1~100), description, version | |
| DailyInventory | roomTypeId, date, totalCount(0~100,000), heldCount, soldCount, availableCount(total - held - sold), version | 달력 셀에 넷 다 보이면 좋다 |
| DailyRate | roomTypeId, date, amount(1~1,000,000,000), currency KRW, version | |
| InventoryRange, RateRange | roomTypeId, from, to, items(date 오름차순, 있는 날짜만), missingDates | 누락 날짜는 빈 셀이 아니라 누락으로 표시 |
| Promotion | id, name, discountRate(1~99), campaignStartDate, campaignEndDate(제외), stayStartDate와 stayEndDate(둘 다 또는 둘 다 null), minNights(1~30), regionCodes(최대 100, 빈 배열은 전체), enabled, version | |
| PropertySearchResult | property, lowestTotalAmount, currency, availableRoomTypes[] | 검색 결과 카드 |
| RoomSearchResult | roomTypeId, name, maxOccupancy, availableCount(전 날짜 최소), totalAmount(할인 후) | 카드 안의 객실 줄 |
| Availability | roomTypeId, checkIn, checkOut, guestCount, available, availableCount, days[](date, availableCount 또는 null), missingInventoryDates, missingRateDates, reasons[] | reasons는 OCCUPANCY_EXCEEDED, INVENTORY_NOT_CONFIGURED, INVENTORY_UNAVAILABLE, RATE_NOT_CONFIGURED 중 해당하는 것만 |
| PriceQuote | roomTypeId, checkIn, checkOut, guestCount, nights(1~30), estimatedAt, price(PriceSnapshot) | 조회 시점 계산이고 확정 금액이 아니라고 화면에 적는다 |
| PriceSnapshot | currency, baseTotalAmount, discountTotalAmount, totalAmount, appliedPromotion(id, name, discountRate 또는 null), days[](date, baseAmount, discountAmount, finalAmount) | 날짜별 표와 합계 |
| ApplicablePromotions | roomTypeId, checkIn, checkOut, guestCount, evaluatedAt, items[](id, name, discountRate, discountAmount, selected), selectedPromotionId | 할인액 내림차순. 선택은 서버가 한다. 게스트가 고르지 않는다 |
| Booking | id, guestId, propertyId, roomTypeId, checkIn, checkOut, guestCount, status, expiresAt, expirationReason(TTL_EXPIRED, PAYMENT_FAILED 또는 null), priceSnapshot, payment(PaymentSummary), cancellationReason, createdAt, updatedAt, confirmedAt, canceledAt, expiredAt, serverNow, version | 상태 넷과 시각들이 화면의 뼈대 |
| PaymentSummary | attemptCount(0~3), approvedAttemptId, attempts[], refund(또는 null) | |
| PaymentAttempt | id, bookingId, attemptNumber(1~3), status(REQUESTED, APPROVED, FAILED), amount, currency, pgTransactionId, mockMode, requestedAt, completedAt(또는 null), failureCode(FAILED이면 MOCK_DECLINED) | |
| Refund | id, paymentAttemptId, amount(승인 금액 전액), currency, status REFUNDED, reason(BOOKING_CANCELED, LATE_APPROVAL), refundedAt | |
| Page | 목록 응답은 페이지 껍데기 안에 온다. page는 0부터, size 기본 20, 최대 100 | 페이지 이동 컨트롤 |

## 7. 예약의 상태와 시간

상태는 넷이고 이동은 셋뿐이다.

| 지금 | 무엇이 일어나면 | 다음 | 화면에서 |
|---|---|---|---|
| 없음 | 예약 요청 성공 | HELD | 전 날짜 선점, 금액 고정. 만료 시각을 받는다 |
| HELD | 만료 전 결제 승인 | CONFIRMED | 확정 안내. 취소 버튼이 생긴다(체크인 전) |
| HELD | 결제 1회나 2회 실패, 만료 전 | HELD | 남은 횟수 안내와 재시도 |
| HELD | 결제 3회 실패 | EXPIRED (PAYMENT_FAILED) | 재시도 없음. 새 예약으로 안내 |
| HELD | 만료 시각 도달 | EXPIRED (TTL_EXPIRED) | 같음 |
| CONFIRMED | 본인 취소, 체크인 전 | CANCELED | 전액 Mock 환불 표시 |
| CONFIRMED | 만료 시각 도달 | 그대로 CONFIRMED | 확정은 만료되지 않는다. 카운트다운을 그리지 않는다 |

시간 규칙

- 남은 시간은 expiresAt과 serverNow의 차이로 센다. 둘 다 예약 응답에 있다. 브라우저 시계를 쓰지 않는다.
- 남은 시간이 0이 되면 화면이 알아서 만료로 바꾸지 않는다. 예약 상세를 다시 읽어 서버 상태를 따른다.
- 결제 보류(DEFER)는 시도가 REQUESTED로 남는다. 완료 신호를 프론트가 받을 길이 없으므로 처리 중 상태를 보여 주고 예약 상세를 다시 읽는다. 처리 중에는 새 결제 요청이 409 PAYMENT_IN_PROGRESS다.
- 시각 응답은 전부 UTC다. 화면은 서울 시각으로 바꿔 보여 준다. 숙박 날짜(checkIn, checkOut)는 시각이 아니라 날짜이고 서울 날짜다.
- 체크아웃 날은 숙박에 포함되지 않는다. 9월 1일 체크인 9월 3일 체크아웃은 2박이고 날짜별 표는 1일과 2일 두 줄이다.

결제 규칙

- Mock 결제다. 카드 번호나 결제수단 입력이 없다.
- 시도는 예약당 최대 3번. 승인 하나면 끝. 실패면 남은 횟수 안에서 재시도.
- mockMode는 결과를 시뮬레이션하는 개발용 입력이다. 게스트 화면에 노출할지, 개발용 패널에 둘지는 정해지지 않았다(prompt.md 질문 목록 후보).
- 늦게 승인이 온 만료 예약(LATE_APPROVAL)은 환불로 기록된다. 예약 상세의 환불 칸에 나온다.

## 8. 오류 코드와 화면 반응

| HTTP | code | 언제 | 화면 반응 후보 |
|---|---|---|---|
| 400 | INVALID_REQUEST | 타입, 필수값, 범위, 모르는 필드 | 폼 필드 오류. 서버에 보내기 전에 9절 규칙으로 막는 것이 먼저 |
| 400 | INVALID_DATE_RANGE | 날짜 형식, 순서, 박수 제한 | 날짜 선택기 오류 |
| 400 | IDEMPOTENCY_KEY_REQUIRED | 멱등키 누락이나 형식 | 프론트 결함. 사용자에게는 일반 오류 |
| 401 | ACTOR_REQUIRED | 행위자 헤더 없음 또는 미등록 | 행위자 전환 컨트롤로 안내 |
| 403 | ACCESS_DENIED | 역할 불일치 | 이 역할로는 할 수 없다고 안내 |
| 404 | RESOURCE_NOT_FOUND | 없음, 부모 불일치, 남의 자원 | 없는 페이지. 남의 것인지는 말하지 않는다 |
| 409 | VERSION_CONFLICT | 수정 버전 불일치 | 다른 곳에서 바뀌었으니 새로 읽어 오라고 안내. 새로 읽기 버튼 |
| 409 | RESOURCE_ALREADY_EXISTS | 같은 날짜의 재고나 요금 중복 등록 | 이미 있으니 수정으로 가라고 안내 |
| 409 | INVENTORY_BELOW_COMMITTED | 총 재고를 선점과 판매 합 아래로 수정 | 최소 가능 값을 보여 준다 |
| 409 | INVENTORY_UNAVAILABLE | 어느 날짜에 선점 가능한 재고 없음 | 예약 불가. 날짜 바꾸기 안내 |
| 409 | INVENTORY_NOT_CONFIGURED | 어느 날짜에 재고 레코드 없음 | 예약 불가. 게스트에게는 판매하지 않는 날짜로 표시 |
| 409 | RATE_NOT_CONFIGURED | 어느 날짜에 요금 없음 | 같음 |
| 409 | OCCUPANCY_EXCEEDED | 객실 최대 인원 초과 | 인원 줄이기 안내 |
| 409 | PRICE_CHANGED | 예상 총액과 서버 금액 불일치 | 예약이 만들어지지 않았다. 금액을 다시 조회해 보여 주고 다시 확인 |
| 409 | IDEMPOTENCY_KEY_REUSED | 같은 키에 다른 내용 | 프론트 결함. 일반 오류 |
| 409 | REQUEST_IN_PROGRESS | 같은 요청 처리 중 | 잠시 뒤 다시. Retry-After 헤더가 있으면 그 초 뒤 |
| 409 | BOOKING_STATE_CONFLICT | 현재 상태에서 안 되는 동작 | 예약 상세를 다시 읽어 상태를 보여 준다 |
| 409 | BOOKING_EXPIRED | 결제 요청 시 만료 | 만료 안내. 새 예약으로 |
| 409 | PAYMENT_IN_PROGRESS | 완료되지 않은 시도 존재 | 처리 중 표시 유지 |
| 409 | PAYMENT_ATTEMPTS_EXHAUSTED | 시도 3회 소진 | 재시도 버튼 없음 |
| 409 | CANCELLATION_NOT_ALLOWED | 취소 가능 날짜 지남 | 취소 버튼 비활성과 이유 |
| 503 | TEMPORARY_FAILURE | 일시 실패 | 잠시 뒤 다시 |
| 500 | INTERNAL_ERROR | 예상 못 한 오류 | 일반 오류. 내부 내용은 응답에 없다 |

응답 헤더 둘

| 헤더 | 언제 | 화면에서 |
|---|---|---|
| Idempotency-Replayed | 같은 멱등키의 재전송을 서버가 앞 결과로 답할 때 | 성공으로 취급한다. 중복 예약이 아니다 |
| Retry-After | 409 REQUEST_IN_PROGRESS 등 | 그 초만큼 기다렸다가 다시 |

## 9. 입력 규칙

| 항목 | 규칙 |
|---|---|
| 날짜 | YYYY-MM-DD, 서울 날짜. checkIn은 서버의 오늘 이상. checkOut은 checkIn보다 뒤. 끝 날짜 제외 |
| 박수 | 예약과 검색은 최대 30박. 재고와 요금 기간 조회와 일괄 등록은 최대 366일 |
| 인원 | guestCount 1~100. 객실 maxOccupancy 이하 |
| 돈 | KRW 정수. 소수점 없음. 1박 요금 1~1,000,000,000. 예약 총액 1~30,000,000,000. 천 단위 구분과 원 |
| 재고 | totalCount 0~100,000. 증감량이 아니라 총 수량 |
| 할인율 | 1~99 백분율. 프로모션은 하나만 적용된다. 날짜별 할인액은 원 단위 버림 |
| 글자 수 | 이름 1~100, 주소 1~300, 설명 최대 2,000, 취소 사유 최대 300, 지역 코드 1~32, ID 최대 64 |
| 지역 코드 | 문자열. 등록 목록을 주는 API가 없다. 화면은 입력 칸이거나 예시 목록(SEOUL 등)으로 둔다. 예시 값은 명세의 예시일 뿐이다 |
| 페이지 | page 0부터, size 기본 20, 최대 100 |
| 수정 요청 | version 필수, 바꿀 필드 1개 이상. 응답의 version을 폼이 들고 있다가 그대로 보낸다 |
| 멱등키 | 예약 요청, 결제 요청, 취소에 필수. 8~128자. 같은 동작의 재시도는 같은 키, 새 동작은 새 키. 화면에서 다시 시도와 새로 만들기를 구분한다 |
| 모르는 필드 | 요청에 정의되지 않은 필드가 있으면 400. 폼이 여분 필드를 보내지 않는다 |

## 10. 정책값

명세의 제안값이고 백엔드가 그대로 구현했다.

| ID | 항목 | 값 |
|---|---|---|
| P01 | 선점 유지 시간 | 예약 생성부터 10분. 서버 설정. expiresAt으로 온다 |
| P02 | 기간 제한 | 예약과 검색 최대 30박. 재고 일괄 등록과 기간 조회 최대 366일 |
| P03 | 통화와 달력 | KRW 정수. 숙박과 캠페인 날짜는 서울, 시각은 UTC |
| P04 | 프로모션 | 정률 할인, 중복 없음. 실제 할인액이 가장 큰 하나. 동률이면 ID 오름차순 |
| P05 | 취소 | 체크인 날짜 전까지 전체 취소와 전액 Mock 환불. 수수료 0. 부분 취소와 예약 변경은 없다 |
| P06 | 금액 차이 | 예상 총액과 서버 재계산이 다르면 409 PRICE_CHANGED. 예약과 선점을 만들지 않는다 |
| P07 | 로컬 행위자 | 회원 API 대신 개발용 행위자와 X-Dev-Actor-Id |
| P09 | 가격 계산 | 날짜별 할인액을 원 단위로 버린 뒤 합산. 세금과 수수료 없음 |
| P10 | 판매 제약 | 재고 0은 판매 불가. 별도 마감 필드 없음 |
| P11 | 프로모션 종료 | 수동 종료는 enabled=false. 자동 종료 없음 |

## 11. v1 밖 (그리지 않는다)

| 항목 | 이유 |
|---|---|
| 회원가입, 로그인, 프로필 | 회원 기능이 없다. guestId만 있다 |
| 쿠폰 입력, 프로모션 선택 | 프로모션은 자동 적용이고 서버가 고른다 |
| 호실(개별 방) 배정 | 재고는 객실 타입 단위 수량이다 |
| 알림, 정산, 리뷰 | 없다 |
| 호스트나 운영자의 예약 취소 | 게스트 본인 취소만 있다 |
| 부분 취소, 예약 변경 | 전체 취소만 있다 |
| 프로모션 자동 종료, 종료 상태 | enabled 켜고 끄기만 있다 |
| 사진, 지도, 좌표 | 숙소 모델에 없다 |
| 결제수단 입력 | Mock 결제다 |
| 요금 일괄 등록 | API가 없다. 재고만 일괄 등록이 있다 |

## 12. 백엔드가 주지 않는 것과 화면의 대처

| 없는 것 | 화면의 대처 |
|---|---|
| 결제 완료 알림(푸시, 웹소켓) | 예약 상세를 다시 읽는다 |
| 지역 코드 목록 API | 입력 칸 또는 예시 목록 |
| 숙소 검색 정렬 옵션 | 서버가 준 순서 그대로 |
| 게스트용 숙소 이미지 | 글자 카드 |
| 호스트용 예약 목록 | 호스트 화면에 예약이 보이지 않는다. 재고 달력의 선점과 판매 수로만 간접 확인 |
| 운영자용 통계 | 없다 |

## 13. 상태 이름과 배지 색 (제안)

문서 용어를 따른다. 색은 중립 토큰으로 두고 이름만 정한다.

| 값 | 한국어 표시 | 뜻 |
|---|---|---|
| HELD | 선점 | 자리를 잡아 두었고 결제를 기다린다 |
| CONFIRMED | 확정 | 결제가 승인됐다 |
| EXPIRED | 만료 | 시간이 지났거나 결제가 3회 실패했다 |
| CANCELED | 취소 | 게스트가 취소했고 전액 환불됐다 |
| REQUESTED | 처리 중 | 결제 시도가 아직 끝나지 않았다 |
| APPROVED | 승인 | |
| FAILED | 실패 | |
| REFUNDED | 환불 | |
