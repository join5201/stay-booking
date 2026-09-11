# 작업 계약 task-S9-booking (예약과 선점 묶음. 1차)

최초 작성: 2026-09-11
최종 갱신: 2026-09-11
양식: harness/prompts/task-contract.md v6

이 계약은 세 번째 구현 묶음이다. 첫 묶음 task-S9-catalog(2026-09-09 완료)와 둘째 묶음 task-S9-inventory-rate(2026-09-11 R1 반영 완료. PR 105, 115, 117이 main에 들어갔다) 위에 얹는다. 2026-09-11 사용자 결정 셋(harness/out/mvp-parallel-2026-09-11/README.md 1절)에 따라 정지점은 1단계 계약 승인과 9단계 검증 표 둘뿐이고, 같은 시각에 프로모션과 검색 세션(P)과 결제 세션(Y)이 다른 워크트리에서 돈다. 이 계약은 그 셋 중 예약 세션(B)의 1차다.

쉽게 말하면 이번 묶음은 손님이 방을 고르고 예약 버튼을 눌렀을 때 서버가 하는 일이다. 묵는 날마다 방 하나를 붙잡아 두고(선점), 그날 값을 그대로 얼려 두고(스냅샷), 같은 버튼을 두 번 눌러도 예약이 둘이 되지 않게 한다(멱등). 결제와 확정과 만료와 취소는 2차다.

## 작업

| 항목 | 내용 |
|---|---|
| Task ID와 Step | task-S9-booking. Step 9 구현. 기능 묶음은 예약과 선점. 이 계약은 1차다. 2차(결제 중계, 확정, 만료, 취소)는 7절 D-5가 정하는 형태로 받는다 |
| 작업 유형 | 코드 |
| 목표 | BOOK-01, BOOK-02, BOOK-03 세 API와 내부 처리 HoldInventory를 백엔드와 테스트와 백엔드 검증까지 내린다. 예약 하나가 연박 전 날짜를 한 번에 선점하거나 아무것도 선점하지 않고(R1, R2), 같은 키의 재전송이 예약을 두 번 만들지 않는다(R3). 프론트는 이 Task에 없다 |
| 대상 API ID | BOOK-01, BOOK-02, BOOK-03. 내부 처리 HoldInventory |
| 선행 작업 | task-S9-catalog. 완료됨. RoomType과 ActorResolver와 PageQuery를 그대로 쓴다. task-S9-inventory-rate. R1 반영 완료. DailyInventory와 DailyRate와 리포지토리 둘과 SeoulDate를 그대로 쓴다. 병렬로 도는 task-S9-promotion-search(세션 P)와 task-S9-payment(세션 Y)는 선행이 아니다. 1차는 그 두 패키지의 파일을 import하지 않는다 |
| 완료 기준 | 넷을 모두 만족해야 한다. 첫째, 9절 계약 테스트 ID가 전부 통과이거나 미실행 사유와 함께 기록된다. 둘째, 8절 단계가 전부 끝난다. 셋째, 빌드 도구가 낸 기계 판독 결과 파일에서 실행 수가 0이 아니고 실패 수가 0이며 앞 묶음까지의 123건이 그대로 통과한다. 넷째, 모든 단계의 실제 시간이 harness/state/progress.md에 분 단위로 기입돼 있다 |
| 변경 허용 파일과 범위 | backend/src/main/java/com/o2o/booking/ 전체와 그 테스트. backend/src/main/java/com/o2o/inventory/domain/의 DailyInventory와 DailyInventoryRepository와 새 예외와 새 이벤트와 새 도메인 서비스, backend/src/main/java/com/o2o/inventory/infrastructure/의 리포지토리 구현 둘. backend/src/main/java/com/o2o/shared/ActorRegistry.java 한 줄(6절 P07 행). harness/out/task-S9-booking-R1/ 아래 실행 결과. harness/state/progress.md 행 추가. 이 계약 파일. inventory의 api와 application 패키지, shared의 나머지 기존 파일, promotion과 search와 payment 패키지, document/, harness/의 나머지는 읽기만 한다 |
| 범위 밖과 유지할 전제 | 아래 3절 |
| 기준 버전 | 문서는 4절 입력 표의 sha256. 코드는 작업 브랜치 feat/task-s9-booking의 커밋 해시와 5절 평가 대상 행의 파일 목록 |
| 후보 작업 공간 | backend/. 워크트리 C:/Dev/potenup/99_projects/o2o-dev, 브랜치 feat/task-s9-booking. 후보와 정본의 구분은 git 브랜치가 맡는다. 테스트 DB는 기본값 o2o_catalog_test이고 다른 두 세션이 o2o_promo_test와 o2o_payment_test를 쓴다 |
| 결과 기록 경로 | harness/state/progress.md |

## 1. 이 Task가 앞 묶음과 다른 점

| 항목 | task-S9-inventory-rate | 이 Task |
|---|---|---|
| 애그리거트 | DailyInventory, DailyRate | Booking. StayPeriod와 PriceSnapshot 값 객체를 품는다. 06-2 1절 |
| 식별자 | 대리키 + 유니크(roomTypeId, stayDate) | BookingId + 유니크(userId, idempotencyKey). 06-2 3-4절 U1을 11 명세 멱등 규칙 1의 범위로 읽는다. 6절 |
| 잠금 | 행 하나를 잠근 뒤 version 대조 | 숙박 날짜 N행을 날짜 오름차순으로 잠근다. 08-3 결정 3. 7절 D-2 |
| 한 트랜잭션 한 애그리거트 | INV-02만 예외 | 재고 N행 hold와 Booking 생성과 멱등 기록이 한 트랜잭션. 06-2 4절이 이것을 의도적 예외로 적는다(A1). 11 명세 멱등 규칙 6 |
| 원자성 | 한 날짜라도 겹치면 전부 실패 | 한 날짜라도 부족하면 전부 롤백. 거절된 요청은 재고도 예약도 멱등 기록도 남기지 않는다 |
| 멱등 | 없었다 | Idempotency-Key 필수. 11 명세 멱등 처리 절의 아홉 규칙 전부. 7절 D-1 |
| 컨텍스트를 넘는 선행조건 | roomType 존재 하나 | roomType 존재와 maxOccupancy(A5, 06-1 R2), 요금 존재(A6), 재고 가용(06-1 R4). 셋 |
| 시간 | 서울 날짜만 | 서울 날짜(checkIn은 서울 오늘 이상)와 UTC 시각(expiresAt, createdAt, serverNow) 둘 다. 7절 D-3 |
| 이벤트 | 발행 넷. 구독자 없음 | BookingCreated 하나와 InventoryHeld N개. 구독자는 아직 없으나 롤백된 요청의 이벤트가 커밋 뒤 구독자에 닿지 않는 것을 8-1 K12로 본다. backend/.claude/rules/layers.md 3-3 E1과 E2 |
| 다른 컨텍스트 호출 | 없음 | 예약 앱 서비스가 재고 컨텍스트의 도메인 서비스 InventoryAllocationService를 부른다. 06-2 6절이 그 서비스를 재고와 요금 컨텍스트에 둔다 |

멱등과 N행 잠금과 세 선행조건이 이번 묶음의 난이도다. 앞 묶음의 원자성은 유니크 제약 하나로 DB가 지켰지만 이번은 코드가 잠금 순서와 롤백을 직접 다룬다.

## 2. 대상 API 셋과 내부 처리 하나와 설계 근거

| API ID | 엔드포인트 | 인증 | 계약표 행 | 불변식 |
|---|---|---|---|---|
| BOOK-01 | POST /api/v1/bookings | GUEST | RequestBooking(앱 서비스), Booking 생성자, hold(1) ×N | I3, I4, I5(HELD 생성만), I10, I11, I12, I15, U1, A1, A5, A6 |
| BOOK-02 | GET /api/v1/bookings | GUEST | 없음. 조회 | 없음 |
| BOOK-03 | GET /api/v1/bookings/{bookingId} | GUEST 소유자 | 없음. 조회 | 없음 |
| HoldInventory | 내부. BOOK-01 트랜잭션 안 | 없음 | hold(1) ×N. InventoryAllocationService | I1, I1a, A1 |

묶음의 근거는 10-6 기능별 API와 검증 연결 표의 예약과 선점 행이다. 그 행이 BOOK-01부터 BOOK-03과 HoldInventory를 한 묶음으로 묶는다. 11 명세 2149행이 Hold를 별도 자원이 아니라 Booking의 HELD 상태로 적으므로 /holds 같은 경로는 없다.

셋 다 인증이 GUEST다. HOST 경로가 없다. BOOK-03은 소유자만 200이고 남의 예약은 자원 정보 없이 404 RESOURCE_NOT_FOUND다(11 명세 T02, 에러 표 404 행).

BOOK-01의 검사 순서. 앞 검사에서 거절되면 뒤 검사는 하지 않는다. 층은 06-4 1-4 검증 책임 위치 표를 따른다.

| 순서 | 무엇 | 실패 시 응답 | 층 |
|---|---|---|---|
| 1 | X-Dev-Actor-Id가 등록된 GUEST | 401 ACTOR_REQUIRED, 403 | api (ActorResolver). 멱등 규칙 9가 인증을 재전송에도 적용하라고 적으므로 멱등 조회보다 앞이다 |
| 2 | Idempotency-Key 존재와 형식. 8자 이상 128자 이하의 영문, 숫자, 하이픈, 밑줄 | 400 IDEMPOTENCY_KEY_REQUIRED | api |
| 3 | body 형식. 필수, 타입, 범위, 미정의 필드, 날짜 형식과 순서와 30박 상한과 서울 오늘 이상 | 400 INVALID_REQUEST, 400 INVALID_DATE_RANGE | api |
| 4 | 멱등 기록 조회. 범위는 행위자 ID + POST + /api/v1/bookings + 키 | 완료 기록이고 body 같음이면 최초 응답 재전송과 Idempotency-Replayed: true. body 다름이면 409 IDEMPOTENCY_KEY_REUSED. 진행 중이면 409 REQUEST_IN_PROGRESS와 Retry-After: 1 | application |
| 5 | 진행 중 기록 삽입 | 해당 없음. 같은 순간 둘이 들어오면 유니크가 하나를 4의 진행 중으로 보낸다 | application, DB |
| 6 | roomType 존재 | 404 RESOURCE_NOT_FOUND | application |
| 7 | userCount <= maxOccupancy (A5) | 409 OCCUPANCY_EXCEEDED | application |
| 8 | 재고 N행을 날짜 오름차순으로 잠그며 읽는다. 행 수가 박수보다 적으면 미개설 | 409 INVENTORY_NOT_CONFIGURED | InventoryAllocationService, infrastructure |
| 9 | 요금 N행 존재 (A6). 가격 포트가 확인한다 | 409 RATE_NOT_CONFIGURED | application (PriceQuotePort 어댑터) |
| 10 | 가격 계산과 expectedTotalAmount 대조 | 409 PRICE_CHANGED. 예약과 Hold 없음 | application |
| 11 | hold(1) ×N. 한 행이라도 가용 0이면 전부 롤백 (A1) | 409 INVENTORY_UNAVAILABLE | domain (DailyInventory), InventoryAllocationService |
| 12 | Booking 생성. HELD, expiresAt = 지금 + TTL, 스냅샷 동결 | 스냅샷이 I10, I11, I12, I15를 어기면 예외. 정상 입력에서는 나지 않는다 | domain (Booking, PriceSnapshot) |
| 13 | 멱등 기록을 완료로 바꾸고 최초 응답을 저장. 예약과 재고와 같은 트랜잭션 (규칙 6) | 해당 없음 | application |
| 14 | 커밋. 그 뒤 BookingCreated 1건과 InventoryHeld N건이 구독자에 닿는다 | 해당 없음 | application |

4단계에서 7단계와 8단계 사이가 거절되면 5의 진행 중 기록을 지운다(규칙 7. 4xx는 완료로 캐시하지 않는다). 8의 잠금이 10의 가격 대조보다 앞인 이유는 재고 행 읽기가 곧 미개설 검사이고 잠근 채 두 번 읽지 않기 위해서다. 잠금 보유 시간은 요금 조회와 덧셈만큼 늘어나며 로컬 v1에서 문제가 되지 않는다.

응답 모델 Booking의 21개 필드는 11 명세 2557행 표 그대로다. 1차에서 값이 고정인 것: status HELD, expirationReason null, payment는 attemptCount 0과 approvedAttemptId null과 attempts 빈 배열과 refund null, cancellationReason null, confirmedAt과 canceledAt과 expiredAt null, version 0. priceSnapshot의 appliedPromotion은 1차 어댑터가 프로모션을 모르므로 null이고 discountAmount는 0이다(7절 D-4). 2차가 payment를 결제 세션의 attemptsOf로 채우고 appliedPromotion을 PricingService 어댑터로 채운다.

### 2-1. 불변식과 선행조건

| 번호 | 문장 | 어디서 지키나 |
|---|---|---|
| I3 | checkIn < checkOut | StayPeriod VO. 최대 30박은 11 명세 1638행이라 api 형식 검사에도 있다 |
| I4 | 생성 이후 PriceSnapshot 불변 | Booking. 스냅샷을 바꾸는 메서드가 없고 days 목록은 방어 복사한다 |
| I5 | 전이는 HELD → CONFIRMED, HELD → EXPIRED, CONFIRMED → CANCELED 셋뿐 | Booking. 1차는 HELD 생성만 만든다. 전이 메서드 셋은 2차 |
| I10 | 할인 배분액 합 == 할인 총액 | PriceSnapshot VO 생성자 |
| I11 | 스냅샷 행 수 == 박수 | PriceSnapshot VO 생성자. 박수는 StayPeriod가 준다 |
| I12 | 할인 배분액 > 0이면 promotionId 보유 | PriceSnapshot VO 생성자 |
| I15 | totalAmount == 날짜별 (단가 - 할인 배분액)의 합 | PriceSnapshot VO 생성자 |
| I1 | totalCount >= soldCount + heldCount | DailyInventory.hold가 가용 수량으로 검사한다. 앞 묶음 코드 그대로 |
| I1a | soldCount >= 0, heldCount >= 0 | DailyInventory의 commit과 releaseHeld와 releaseSold. 앞 묶음 8-1 V2 이월분 |
| U1 | 같은 idempotencyKey의 예약은 하나 | DB 유니크(userId, idempotencyKey)와 멱등 기록의 유니크(행위자, 메서드, 경로, 키). 둘이 겹치는 것은 의도다. 기록 논리에 구멍이 나도 Booking이 둘이 되지 않는다 |
| A1 | 연박 N일 전부 선점 또는 전무 | InventoryAllocationService. 한 트랜잭션 |
| A5 | userCount <= maxOccupancy | 예약 앱 서비스 선행조건 |
| A6 | 숙박 기간 모든 날짜에 요금 존재 | 예약 앱 서비스 선행조건. 가격 포트 어댑터가 확인한다 |

I3부터 I15까지 일곱이 06-2 1절 예약 행의 불변식 전부다. 그중 I5는 1차에서 생성만 있어 전이 폐쇄를 검사할 전이가 없다. 상태를 바꾸는 공개 메서드를 만들지 않는 것이 1차의 I5다.

### 2-2. 이번 묶음이 만들지 않는 것

| 항목 | 누가 | 왜 |
|---|---|---|
| Booking의 confirm, expire, cancel과 그 이벤트 | 2차 | 06-4 1-3 전이표는 결제 승인과 TTL과 취소가 트리거다. 결제 컨텍스트와 스케줄러가 있어야 닿는 경로다 |
| CommitInventory, 선점 반환, 판매분 반환의 호출 경로 | 2차 | 위와 같다. DailyInventory의 commit, releaseHeld, releaseSold 메서드 자체는 1차가 만들고 단위 테스트한다(K5). 호출자는 2차다 |
| PAY-01, PAY-02, BOOK-04와 그 멱등 적용 | 2차 | 결제 세션의 openAttempt와 refund가 main에 들어간 뒤 |
| TTL 스케줄러 | 2차 | ExpireBooking이 있어야 돌 것이 있다 |
| PaymentAttempt, 환불, INTERNAL-01 | 세션 Y | 결제 컨텍스트 |
| PricingService, 프로모션, 검색 | 세션 P | 프로모션과 검색 컨텍스트. 1차는 포트 뒤에서 요금만 합산한다(7절 D-4) |
| InventoryHeld 구독자 | 없음 | 구독할 쪽이 아직 없다. 1차의 K12는 테스트 전용 구독자로 커밋 뒤 전달을 본다 |

## 3. 범위 밖과 유지할 전제

| 항목 | 왜 밖인가 |
|---|---|
| 프론트와 연결 테스트 | 별도 Task. 사용자 결정이고 앞 두 묶음과 같은 처리다 |
| 2차 항목 전부. 결제 중계, 확정, 만료, 취소, 환불, 스케줄러 | 2-2절. 두 세션의 PR이 main에 들어간 뒤 7절 D-5의 형태로 받는다 |
| promotion, search, payment 패키지 | 병렬 세션의 몫. 읽지도 import하지도 않는다. 병합 충돌의 원천이다 |
| inventory의 api와 application 패키지와 기존 메서드의 동작 | 세션 P가 그 파일을 읽기만 하고 이 세션이 domain과 infrastructure만 더한다. 기존 123건이 그대로 통과해야 한다 |
| shared의 기존 파일 | ActorRegistry 한 줄(6절 P07 행)만 예외다. 그 외는 고치지 않는다 |
| 설계 문서 수정 | 틀렸으면 멈추고 보고한다. document/는 동결 |
| 하네스 파일 수정 | harness/prompts, harness/tools, harness/docs, harness/project-sync, CLAUDE.md, AGENTS.md, .claude, backend/.claude, backend/build.gradle, 설정 파일 둘은 동결. 예외는 harness/state 기록 파일의 행 추가와 이 계약 파일 |
| Codex 평가 | 오늘의 전제 결정 3. MVP 코드가 다 붙은 뒤 한 번, 재고와 요금 R2를 같이 |
| 패키지 재배치와 식별자 타입 변경 | 사용자가 미뤘다. 2026-09-09 |
| 다중 객실 | 08-3 결정 11의 11-5. n = 1 |

유지할 전제. shared 패키지(Actor, ActorResolver, ActorRegistry, ActorRole, PageQuery, PageResult, ErrorResponse, VersionConflictException, Money, RoomTypeId, PropertyId, HostId, SeoulDate, ClockConfiguration의 UTC Clock)를 그대로 쓴다. 층 이름 넷 domain, application, api, infrastructure를 그대로 쓴다. 리포지토리는 인터페이스와 어댑터 두 파일이다(layers.md 3-1). 앱 서비스는 트랜잭션 안에서 publishEvent를 부르고 구독자가 AFTER_COMMIT을 지킨다(layers.md 3-3). 새 값 객체는 두 번째 컨텍스트가 쓰기 전까지 booking 안에 둔다(layers.md 3-2). 도메인은 스프링 웹과 스프링 데이터를 모른다.

## 4. 입력과 적용 규칙

| 자료 | 경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 01 전체 | document/01-o2o-ddd-plan.md | sha256:17569703c90a18df | 전문 |
| 대상 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절 전부(특히 멱등 처리 94행부터, 목록 규칙 76행부터, 에러 표 112행부터), 예약과 결제 절의 BOOK-01부터 BOOK-03(1610행부터 1894행), 내부 처리의 Hold와 재고와 상태 전이와 가격과 프로모션(2147행부터 2196행), 응답 모델 PriceDay와 PriceSnapshot과 AppliedPromotion과 PaymentSummary와 Booking과 Page Booking, 검증 기준 T01, T02, T04, T06부터 T13, T29, 검토할 정책 P01부터 P11 |
| 대상 06-2 애그리거트 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 1절 예약 행, 2절, 3-1 I3과 I4와 I5와 I10부터 I15, 3-4 유일성과 멱등, 4절 트랜잭션 경계, 5절 동시성 규칙, 6절 예약 CRC와 InventoryAllocationService, 9절 |
| 대상 06-4 계약 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 0절 잠금과 멱등 반환 선언, 1-2 재고 hold와 commit과 releaseHeld와 releaseSold 행, 1-2 예약의 RequestBooking과 Booking 생성자 행, 1-3 상태전이표, 1-4 검증 책임 위치 |
| 대상 06-1 컨텍스트 맵 | document/06-1-o2o-context-map.md | sha256:95bc2b1079d3b739 | 2절 R2, R4, R5, R6. 4절 shared 기준 |
| 대상 05-3 용어 | document/05-3-o2o-glossary.md | sha256:62a87a62baf5846e | 예약 용어 행. userId와 userCount(v8 개명), StayPeriod, PriceSnapshot, DailyPrice, HELD, TTL |
| 대상 02 기능 목록 | document/02-o2o-feature-list.md | sha256:2a3d3ca2d8b63809 | 예약 기능 행 |
| 대상 03 이벤트 스토밍 | document/03-o2o-event-storming.md | sha256:3334e5a73cb8f896 | 예약 커맨드와 이벤트 행. BookingCreated, InventoryHeld |
| 확정 전제 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 1절 확정 전제와 2절 요구사항 R1부터 R5 |
| 정책 결정 08-3 | harness/decisions/decisions-08-3.md | sha256:1b8580aa86c18fd8 | 표 11행 전부와 표 아래 문장. 특히 3, 11의 11-3과 11-5와 11-6 |
| 구현 계획 10-6 | harness/docs/10-6-o2o-harness-implementation-plan.md | sha256:eec97503b53ffff0 | 3절 실행 순서와 기능별 API와 검증 연결 표 |
| 앞 묶음의 계약 하나 | harness/tasks/task-S9-catalog.md | sha256:bd62431b07c6d3f9 | 3절 범위 밖, 6절 정책 적용, 8절 단계표 |
| 앞 묶음의 계약 둘 | harness/tasks/task-S9-inventory-rate.md | sha256:4eb9f268cf82319a | 7절 D-2 잠금, 8-1 V2 이월과 V14 방식, 9절 실행 환경 |
| 오늘의 전제 | harness/out/mvp-parallel-2026-09-11/README.md | sha256:7cbeaa454ed78eec | 1절 결정 셋, 2절 세션 셋, 4절 병합 순서와 접점 |
| 이 세션의 프롬프트 | harness/out/mvp-parallel-2026-09-11/prompt-B-booking.md | sha256:6aa29f27984d2c16 | Task 절의 만든다와 만들지 않는다 표, 접점 표, 결정 후보, 검증 ID 표 |
| 적용할 코드 양식 | harness/prompts/dev-ptcf-prompt.v3.md | sha256:4fd959abf491efe0 | Format 절 |
| 실제 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 | 축, 심각도, 출력 스키마 |
| 백엔드 층 규칙 | backend/.claude/rules/layers.md | sha256:cb9e4f68bdc4c43c | 3절 전부. 특히 3-2와 3-3 |

10-6과 앞 묶음의 계약과 오늘의 전제와 프롬프트는 이 표에만 있고 5절에는 없다. harness/docs/와 harness/out/의 계획 문서는 평가 입력이 될 수 없고(CLAUDE.md 3절), 앞 묶음의 계약은 이 묶음 코드의 근거가 아니다.

## 5. A와 B 평가 허용 입력 (HR1)

오늘의 전제 결정 3에 따라 평가는 MVP 코드가 다 붙은 뒤 한 번이다. 이 표는 그 라운드에서 이 묶음 몫으로 넘길 것이다.

| 자료 | 경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 코드 | 생성 후 기입 | 생성 후 기입 | 파일 목록과 커밋 해시. harness/out/task-S9-booking-R1/eval-target-files.md에 만든다 |
| 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 이 작업 계약 | harness/tasks/task-S9-booking.md | 자기 해시 없음 | 전문 |
| 실제 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:c5ed835951fe09a5 | 축, 심각도, 출력 스키마 |
| 대상이 참조하는 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절, 예약과 결제 절의 BOOK-01부터 BOOK-03, 내부 처리 절, 응답 모델, 검증 기준 |
| 대상이 참조하는 06-2 | document/06-2-o2o-aggregates.md | sha256:113c6734b5525e1d | 1절, 3-1, 3-4, 4절, 5절, 6절 예약 |
| 대상이 참조하는 06-4 | document/06-4-o2o-contracts.md | sha256:edacbe47d6d63e3f | 0절, 1-2 재고와 예약, 1-3, 1-4 |
| 대상이 참조하는 08-3 결정 | harness/decisions/decisions-08-3.md | sha256:1b8580aa86c18fd8 | 3, 11의 11-3과 11-5와 11-6, 표 아래의 11 v2와 부딪히는 번호 문장. 코드가 11 명세 대신 이 결정을 따른 자리를 평가자가 위반으로 적지 않게 한다 |
| 전역 역직렬화 설정 | backend/src/main/resources/application.properties | sha256:f091e83f098bd0d1 | 7행 fail-on-unknown-properties |
| 백엔드 층 규칙 | backend/.claude/rules/layers.md | sha256:cb9e4f68bdc4c43c | 3-2 shared 기준, 3-3 이벤트 규칙 E1과 E2 |

이 표에 넣지 않는 것: 01 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, harness/docs/ 전체, harness/out/의 계획 문서, harness/state/ 전체, 생성 대화.

요구사항 역추적 축에 대한 지시. 입력 팩 2절의 요구사항 R1부터 R5 중 이번 묶음에 걸리는 것은 R1(연박 전체 선점 실패), R2(초과 예약 0), R3(재시도 중복 Booking 방지) 셋이다. R5(확정 Booking 금액 불변)는 부분이다. 스냅샷 동결과 요금 변경 뒤 유지는 1차가 닫고 확정은 2차다. R4(HELD 종료 시 재고 반환)는 2차라 해당 없음으로 적고 미커버로 적지 않는다. 이 Task의 역추적 대상은 2절의 API 셋과 HoldInventory와 2-1절의 불변식 중 I3, I4, I10, I11, I12, I15, I1, I1a, U1, A1, A5, A6이다. I5는 1차에 전이가 없어 대상에서 뺀다.

## 6. 정책 적용

| 정책 ID 또는 쟁점 | 적용할 값 또는 판단 | 상태와 사용자 확인 |
|---|---|---|
| P01 Hold TTL | 채택. 생성 시점부터 10분. 08-3 결정 11의 11-3이 TTL 10분을 v1의 최종 안전망으로 확정했다. 05-3의 [가설] 표시는 그 수용으로 풀린 것으로 본다. 값의 자리와 기준 시각은 7절 D-3 | 확정. 08-3 수용. join5201, 2026-09-07. 자리는 계약 승인. join5201, 2026-09-11 |
| P02 기간 제한 | 채택. 예약은 최대 30박. 11 명세 1638행 | 확정. 계약 승인. join5201, 2026-09-11 |
| P03 통화와 달력 | 채택. currency는 KRW 고정이고 amount는 정수 원이다. checkIn은 서울 오늘 이상이고 SeoulDate.today(clock)로 판정한다. expiresAt과 createdAt과 updatedAt과 serverNow는 UTC 시각이다 | 확정. 앞 묶음 R1 반영. join5201, 2026-09-11 |
| P06 검색과 예약 금액 차이 | 채택. expectedTotalAmount와 서버 계산 총액이 다르면 409 PRICE_CHANGED. 예약과 Hold를 만들지 않는다. 같은 총액에서 할인 구성만 다른 경우는 1차에 할인이 없어 생기지 않는다 | 확정. 계약 승인. join5201, 2026-09-11 |
| P07 로컬 행위자 | 채택. X-Dev-Actor-Id 헤더로 행위자를 받고 body로 이용자를 정하지 않는다(11 명세 BOOK-01 처리 규칙). 등록된 GUEST가 guest_001 하나뿐이라 T02의 남의 예약 조회와 T08의 두 손님 경합을 만들 수 없다. ActorRegistry에 guest_002를 GUEST로 한 줄 더한다. host_002를 더한 것과 같은 이유다. shared 기존 파일 수정이라 계약 승인이 그 한 줄의 승인이다. 다른 두 세션은 shared 기존 파일을 고치지 않으므로 병합 충돌은 없다 | 확정. 계약 승인. join5201, 2026-09-11 |
| P08 원자성 구현 후보 | 채택. 단일 MySQL 트랜잭션으로 재고 N행 hold와 Booking 생성과 멱등 기록을 묶는다. 락 방식은 7절 D-2 | 확정. 계약 승인. join5201, 2026-09-11 |
| P09 가격 계산 | 채택. 1차는 할인이 0이라 버림이 나오지 않는다. 날짜별 finalAmount의 합이 totalAmount다 | 확정. 계산 자체는 세션 P |
| P04, P05, P10, P11 | 이번 작업과 무관. 프로모션과 취소와 판매 제약 계열이다 | 확인 대기 |
| 08-3 결정 3 잠금 순서 | 채택. Booking, Payment, 재고 N행 날짜 오름차순. 생성 경로는 만들 Booking이 아직 없고 Payment도 없어 재고 N행만 잠근다. 오름차순은 리포지토리 질의가 보장한다(7절 D-2) | 확정. 08-3 수용. join5201, 2026-09-07 |
| 08-3 결정 11의 11-5 n = 1 | 채택. hold(1). 객실 수는 입력받지 않는다 | 확정. 08-3 수용 |
| 08-3 결정 11의 11-6 종착 멱등 반환 | 채택. 같은 범위와 키의 재전송은 기존 Booking을 가리키고 새 선점을 하지 않는다. 응답 body는 11 명세 멱등 규칙 3대로 최초 저장한 응답이다. 1차는 HELD뿐이라 둘이 같은 뜻이고, 2차의 T29에서 예약이 EXPIRED가 된 뒤에도 최초 HELD 응답을 재전송하며 GET은 현재 상태를 낸다 | 확정. 08-3 수용 |
| 08-3 결정 1, 2, 4부터 10 | 이번 1차와 무관. 결제와 환불과 리스너 계열이다. 2차와 세션 Y 몫 | 확정. 08-3 수용 |
| 11 명세와 08-3의 충돌 | 예약 생성 경로에는 없다. decisions-08-3.md 표 아래 문장이 부딪힌다고 적는 번호는 1, 2, 3(P08), 8과 11의 확정 우선(T18, T19)이고 전부 결제와 확정 경로라 2차에서 다룬다 | 확정. 계약 승인 |
| U1의 범위 | 06-2 3-4절은 유니크 idempotencyKey라 적고 11 명세 멱등 규칙 1은 범위를 행위자와 메서드와 경로와 키로 적는다. Booking의 유니크는 (userId, idempotencyKey)로 잡아 둘을 같이 만족시킨다. 서로 다른 손님이 우연히 같은 키 문자열을 써도 각자의 예약이 된다 | 확정. 계약 승인. join5201, 2026-09-11 |
| 도메인 이름과 API 이름 | 도메인은 05-3 v8대로 userId와 userCount다. API JSON은 11 명세대로 guestId와 guestCount다. 변환은 api 층의 요청과 응답 DTO에서만 한다 | 확정. 05-3 v8 |
| 응답의 propertyId | Booking에 PropertyId를 같이 저장한다. 06-2 1절 필드 목록에는 없으나 11 응답 모델 Booking이 요구하고, RoomType의 propertyId는 등록 뒤 바뀌지 않는다(RoomType.update가 그 필드를 받지 않는다). 목록 조회에서 RoomType을 다시 읽지 않기 위해서다 | 확정. 계약 승인. join5201, 2026-09-11 |
| 멱등 기록의 body 비교 | 11 명세 멱등 규칙 2대로 JSON 키 순서와 무관하다. 키를 정렬한 정규 JSON의 sha256을 저장하고 비교한다 | 확정. 계약 승인. join5201, 2026-09-11 |
| 멱등 기록의 만료 | 없음. 11 명세 멱등 규칙 8. 로컬 v1은 완료 기록을 자동 만료시키지 않는다 | 확정. 명세 |
| 이벤트 발행 자리 | 앱 서비스가 트랜잭션 안에서 publishEvent를 부른다. layers.md 3-3. 롤백된 요청의 이벤트가 커밋 뒤 구독자에 닿지 않는 것은 K12가 본다 | 확정. layers.md |
| 인원 상한 검사의 자리 | A5는 예약 앱 서비스가 RoomType.maxOccupancy로 한다. 세션 P의 PricingService는 인원을 보지 않는다(접점 표) | 확정. 06-4 1-4 |

확인 대기인 P04와 P05와 P10과 P11은 이번 1차 코드가 의존하지 않는다. 미결 정책에 의존하는 구현을 확정하지 않는다(N4). 08-3 결정 11건은 2026-09-07 전부 수용됐으므로 확정값으로 쓴다.

## 7. 결정 5건의 안과 추천

### D-1. 멱등 기록을 어디에 두나

멱등 기록은 예약 생성 요청 하나마다 남는 작은 표다. 범위(행위자, 메서드, 경로, 키), body 해시, 상태(진행 중 또는 완료), 저장한 응답(상태 코드, Location, body)을 갖는다. 진행 중 기록은 본 트랜잭션과 별도로 먼저 커밋해(REQUIRES_NEW) 같은 순간 들어온 같은 키가 그것을 보게 하고, 완료 전환은 Booking과 재고와 같은 트랜잭션에서 한다(규칙 6). 본 트랜잭션이 거절되면 진행 중 기록을 별도 트랜잭션으로 지운다(규칙 7). 저장하는 응답은 api 층이 만든 JSON 문자열이고 앱 서비스는 그것을 공급자로 받아 저장만 한다. 그래서 application이 api를 가리키지 않는다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | booking 패키지 안에 둔다. 엔티티와 리포지토리 인터페이스는 booking/domain, 구현은 booking/infrastructure, 진행 중 삽입과 완료와 해제는 booking/application. 2차의 PAY-01과 BOOK-04도 예약 컨텍스트의 경로라 같은 기록을 그대로 쓴다. BookingId, IdempotencyKey, UserId 값 객체도 booking/domain에 둔다 | 결제 세션이 나중에 자기 멱등이 필요하면 옮겨야 한다. 지금은 INTERNAL-01이 멱등키 대신 pgTransactionId로 중복을 가리므로 그럴 일이 없다 |
| 나 | shared에 둔다. 엔티티와 리포지토리와 서비스를 shared 아래에 만든다 | layers.md 3-2에 어긋난다. 두 번째 컨텍스트가 쓰기 전에 올리는 것이다. shared에 JPA 엔티티와 트랜잭션 서비스가 처음 생겨 층이 하나 더 는다. 프롬프트의 shared 새 파일 문구가 이 안을 가정했으나 규칙이 우선이다 |

추천은 가다. 쓰는 쪽이 예약 컨텍스트뿐이고 규칙이 그렇게 적는다. Clock이 두 번째 컨텍스트가 생겼을 때 옮겨진 것처럼 필요할 때 옮긴다.

결정: 가. join5201, 2026-09-11. 멱등 기록과 값 객체는 booking 안에 둔다.

### D-2. 재고 N행을 어떻게 잠그나

08-3 결정 3이 순서를 날짜 오름차순으로 정했다. 남은 것은 방식이다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | DailyInventoryRepository에 findRangeForUpdate(roomTypeId, from, toExclusive)를 더한다. 질의 하나가 from 이상 to 미만을 stayDate 오름차순으로 정렬해 PESSIMISTIC_WRITE로 읽는다. 돌아온 행 수가 박수보다 적으면 미개설이다. InventoryAllocationService가 이 질의 하나로 잠금과 존재 검사를 같이 한다 | 새 질의 메서드 하나와 어댑터 구현. 정렬을 질의가 하므로 잠금 순서가 코드 순서에 안 기댄다. 앞 묶음 findRange와 같은 규칙이라 이해 비용이 낮다 |
| 나 | 기존 findForUpdate를 날짜마다 오름차순으로 부른다 | 박수만큼 왕복한다. 순서가 호출 루프에 기대므로 루프를 고치면 순서가 깨진다. 없는 날짜를 만나면 그 앞 날짜의 잠금을 쥔 채 실패한다 |

추천은 가다. 한 질의가 순서와 존재 검사를 같이 주고, 두 트랜잭션 경합 테스트(K19, K17)가 잠그는 자리를 하나로 본다. InventoryAllocationService는 06-2 6절대로 재고와 요금 컨텍스트의 도메인 서비스이고 리포지토리 인터페이스만 안다. 예약 앱 서비스가 그것을 부른다.

결정: 가. join5201, 2026-09-11. findRangeForUpdate 질의 하나로 잠금과 존재 검사를 같이 한다.

### D-3. TTL 값의 자리와 expiresAt의 기준 시각

11 명세 2178행이 DB 시각 또는 주입 Clock 중 하나로 통일하라고 적고 테스트가 Clock을 제어하라고 적는다. P01은 TTL을 서버 설정으로 관리하라고 적는다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 기준 시각은 shared ClockConfiguration의 UTC Clock. TTL은 설정 키 o2o.booking.hold-ttl로 받되 기본값 PT10M을 코드의 @Value 기본값으로 둔다. application.properties는 손대지 않는다 | 설정 키 하나가 생긴다. 이 계약이 그 키를 정의하므로 N6의 검증 안 된 키가 아니다. 테스트는 Clock을 고정하고 TTL 기본값으로 expiresAt을 검산한다 |
| 나 | 기준 시각은 같고 TTL은 도메인 상수 10분 | P01의 서버 설정 문구와 어긋난다. 2차 스케줄러 테스트가 짧은 TTL을 원할 때 코드를 고쳐야 한다 |
| 다 | DB 시각 NOW(6)로 판정한다 | 테스트가 시각을 제어할 수 없다. 11 명세 2178행의 테스트 조건과 어긋난다 |

추천은 가다. Clock 하나로 createdAt과 expiresAt과 serverNow가 같은 시계에서 나오고, 2차의 스케줄러도 같은 Clock으로 due를 판정한다.

결정: 가. join5201, 2026-09-11. UTC Clock 주입과 설정 키 o2o.booking.hold-ttl 기본값 PT10M.

### D-4. 가격 포트의 1차 어댑터를 어디에 두나

PricingService는 세션 P가 만들고 1차는 그것을 import하지 않는다. 그래도 6단계의 실제 서버가 가격을 내야 하므로 포트 뒤에 무언가가 있어야 한다.

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | booking/application에 PriceQuotePort 인터페이스를 두고 booking/infrastructure에 RateOnlyPriceQuoteAdapter를 프로덕션 빈으로 둔다. DailyRateRepository의 findRange로 요금 N행을 읽어 할인 0과 appliedPromotion null인 PriceSnapshot을 만든다. 요금이 빠진 날짜가 있으면 그 날짜 목록을 실은 예외를 던진다. 클래스 머리 주석에 2차에서 PricingService 어댑터로 교체한다고 적는다 | 2차에서 지우거나 바꿔야 하는 빈이 하나 생긴다. 2차는 이 클래스를 tmp/_moved/로 옮기고 PricingServiceAdapter를 같은 자리에 둔다 |
| 나 | 어댑터를 테스트 전용으로 두고 프로덕션에는 빈이 없다 | 6단계 bootRun이 포트 구현이 없어 뜨지 않거나 예약을 못 만든다. 게이트를 못 넘는다 |

추천은 가다. 포트 시그니처는 PriceSnapshot quote(RoomTypeId roomTypeId, StayPeriod period)로 두어 2차 어댑터가 세션 P의 quote(RoomTypeId, LocalDate, LocalDate)를 한 줄로 감싸게 한다. 인원 검사는 포트 밖이다.

결정: 가. join5201, 2026-09-11. 요금만 합산하는 프로덕션 어댑터. 2차에서 교체.

### D-5. 2차를 어떤 계약으로 받나

| 안 | 내용 | 대가 |
|---|---|---|
| 가 | 이 계약의 개정 1로 받는다. 2절과 8절과 8-1에 2차 행을 더한다 | 승인된 범위를 넓히는 개정이라 다시 승인해야 한다. 평가 대상 목록과 역추적 대상이 한 계약 안에서 두 번 바뀐다. 10-6 표의 세 행이 계약 하나에 겹친다 |
| 나 | 새 계약 harness/tasks/task-S9-booking-lifecycle.md로 받는다. 대상은 PAY-01과 PAY-02의 중계, ConfirmBooking과 CommitInventory, ExpireBooking 둘과 스케줄러, BOOK-04와 RefundPayment 중계. 10-6 표의 결제와 확정 행의 예약 몫과 취소와 만료 행이다 | 계약 하나를 더 쓴다. 이슈와 PR도 하나 더다. 대신 1차의 승인과 평가 대상이 그대로 남는다 |

추천은 나다. 2차는 Booking 상태 기계의 전이 셋과 두 세션의 접점 셋을 붙이는 일이라 1차와 성격이 다르고, 계약 파일명 규약이 기능키 단위다. 결제와 확정과 취소와 만료를 한 계약으로 받는 이유는 셋이 같은 상태 기계(I5)를 나눠 가져서 둘로 가르면 전이표가 반으로 쪼개지기 때문이다.

결정: 나. join5201, 2026-09-11. 2차는 새 계약 task-S9-booking-lifecycle.md로 받는다.

## 8. 세로 진행 단계와 각 단계의 완료 조건

오늘의 전제 결정 1에 따라 정지점은 1단계와 9단계 둘이다. 2단계부터 6-2단계까지는 사용자 확인 없이 잇되 단계마다 progress.md 행과 실제 시간을 남긴다. 번호는 dev-ptcf-prompt.v3.md 3절과 같다.

| 단계 | 내용 | 완료 조건 | 정지 |
|---|---|---|---|
| 1 | 이 계약과 결정 5건 승인 | 10절 승인 칸과 7절 결정 5건이 값을 갖는다 | 승인 대기 |
| 2 | 이슈와 브랜치 | 이슈 하나(제목에 task-S9-booking), 브랜치 feat/task-s9-booking은 이미 있다. PR을 초안으로 열고 본문에 Refs로 잇는다. CLAUDE.md 4-1 순서 | 잇는다 |
| 3 | 해당 없음 | 백엔드 틀과 MySQL은 앞 묶음이 세웠다 | 건너뛴다 |
| 4 | 도메인 코드. booking/domain의 Booking, BookingId, UserId, StayPeriod, PriceSnapshot, DailyPrice, BookingStatus, IdempotencyKey, IdempotencyRecord, 리포지토리 인터페이스 둘, 이벤트 BookingCreated, 예외들. inventory/domain의 DailyInventory에 hold와 commit과 releaseHeld와 releaseSold, 예외 둘, 이벤트 InventoryHeld, InventoryAllocationService, 리포지토리의 findRangeForUpdate. booking/application의 앱 서비스와 PriceQuotePort와 멱등 서비스. infrastructure의 JPA 구현과 1차 가격 어댑터 | 06-2 6절 예약 CRC의 책임 행마다 대응 코드가 있고 주석에 절 번호가 있다. 2-1절의 불변식이 애그리거트와 값 객체 안에 있다. 기존 123건이 그대로 통과한다 | 잇는다 |
| 5 | 테스트. 도메인 단위와 앱 서비스 통합 | 8-1절의 4단계와 5단계 항목 K1부터 K12에 테스트가 있고 실패 케이스와 통과 케이스가 짝이다 | 잇는다 |
| 6 | 예약 요청 API. BOOK-01 | 경로가 명세의 요청과 응답과 상태 코드 그대로다. 8-1절의 6단계 항목 K13부터 K23이 붙는다. bootRun으로 띄운 실제 서버의 HTTP 호출 결과가 http-calls.txt로 남는다 | 잇는다 |
| 6-2 | 조회 API 둘. BOOK-02, BOOK-03 | 두 경로가 명세 그대로이고 8-1절의 6-2단계 항목 K24와 K25가 붙는다. 실제 서버 호출 기록을 같은 파일에 잇는다 | 잇는다 |
| 7 | 해당 없음 | 프론트는 별도 Task | 고르지 않는다 |
| 8 | 해당 없음 | 프론트는 별도 Task | 고르지 않는다 |
| 9 | 백엔드 검증 표 | T06, T07, T08, T09, T10, T11의 대조표가 닫히고 T12와 T13은 요금 절반, T01과 T02는 예약 조회 구간, T04는 heldCount 몫까지 닫힌 것으로 적는다. 회고 표가 채워진다. PR 전에 origin/main을 병합한다 | 사용자 완료 판단 |

모든 단계에 공통으로 걸리는 완료 조건 둘. 첫째, harness/state/progress.md에 그 단계의 행이 추가되고 실제 시간 칸이 분 단위로 채워진다. 둘째, 하네스 파일을 고치지 않는다.

3단계를 건너뛰는 이유. 앞 묶음이 backend/와 MySQL 컨테이너와 결과 파일 경로를 확정했고 9절이 그 값을 그대로 쓴다.

### 8-1. 단계별 테스트 목록

불변식 하나에 테스트 하나가 최소다. 이번 묶음은 불변식 일곱에 원자성과 잠금과 멱등이 더 붙는다. ID의 K는 예약(booking)의 K다. 앞 묶음의 V와 첫 묶음의 C와 겹치지 않게 골랐다.

| ID | 단계 | 무엇을 확인하나 | 근거 |
|---|---|---|---|
| K1 | 5 | StayPeriod. checkIn >= checkOut이 거부되고 박수와 날짜 목록이 오름차순으로 나온다 | I3, 06-2 1절 |
| K2 | 5 | PriceSnapshot 생성자가 I10, I11, I12, I15를 각각 거부하고 맞는 입력은 통과한다. 넷 각각 실패와 통과가 짝이다 | I10, I11, I12, I15 |
| K3 | 5 | Booking 생성이 HELD이고 expiresAt이 지금 + TTL이며 스냅샷을 바꿀 길이 없다. days 목록을 바깥에서 바꿔도 안 변한다 | I4, I5, 06-4 1-2 Booking 생성자와 RequestBooking Post |
| K4 | 5 | DailyInventory.hold(1)이 가용 0에서 거부되고 가용 있으면 heldCount가 1 오른다. totalCount와 soldCount는 안 변한다 | I1, 06-4 1-2 hold |
| K5 | 5 | commit(1)은 heldCount 0에서, releaseHeld(1)은 heldCount 0에서, releaseSold(1)은 soldCount 0에서 거부된다. 맞는 상태에서는 선점이 판매로, 선점이 반환으로, 판매가 반환으로 간다 | I1a. 앞 묶음 8-1 V2 이월. 06-4 1-2 commit과 releaseHeld와 releaseSold |
| K6 | 5 | 멱등 범위가 행위자와 메서드와 경로와 키 넷으로 갈리고 body 해시가 JSON 키 순서와 무관하다 | 11 멱등 규칙 1과 2 |
| K7 | 5 | 3박 중 가운데 날 재고가 0이면 예외가 나고 세 날짜 어느 것도 heldCount가 안 오르며 Booking이 없다. MySQL 통합 | T09, A1, R1 |
| K8 | 5 | 숙박 날짜 중 하루에 요금이 없으면 요금 없음 예외, 재고 행이 없으면 재고 미개설 예외. 둘 다 Booking과 Hold가 없다. 체크아웃 날짜에는 재고와 요금이 없어도 성공한다 | T07, T06, A6, 06-4 hold의 InventoryNotOpened |
| K9 | 5 | userCount가 maxOccupancy를 넘으면 거부. 없는 roomType은 없음 예외 | A5, 06-1 R2 |
| K10 | 5 | expectedTotalAmount가 요금 합과 다르면 거부되고 Booking과 Hold가 없다 | T12 요금 절반, P06 |
| K11 | 5 | 예약 뒤 RATE-02로 요금을 바꿔도 예약의 스냅샷과 totalAmount가 그대로다 | T13 요금 절반, I4, R5 부분 |
| K12 | 5 | 성공하면 BookingCreated 1건과 InventoryHeld N건이 커밋 뒤 테스트 전용 구독자에 닿고, K7처럼 롤백된 요청에서는 어느 것도 닿지 않는다 | 06-2 4절, 03 이벤트, layers.md 3-3 E1과 E2 |
| K13 | 6 | BOOK-01이 201과 Location과 Booking 21개 필드 전부를 낸다. status HELD, payment는 0과 null과 빈 배열과 null, appliedPromotion null, days N행 날짜 오름차순, version 0, serverNow 있음 | 11 명세 BOOK-01 응답과 응답 모델 Booking |
| K14 | 6 | Idempotency-Key가 없거나 7자이거나 129자이거나 허용 밖 문자를 담으면 400 IDEMPOTENCY_KEY_REQUIRED | 11 멱등 처리 절 머리, BOOK-01 에러 표 |
| K15 | 6 | 같은 키 같은 body 재전송이 같은 id와 같은 body와 201과 Idempotency-Replayed: true를 내고 heldCount가 더 오르지 않는다 | T10, R3, 멱등 규칙 3 |
| K16 | 6 | 같은 키 다른 body는 409 IDEMPOTENCY_KEY_REUSED. 키 순서만 바꾼 body는 같은 body로 판정돼 재전송이다 | T11, 멱등 규칙 2 |
| K17 | 6 | 첫 요청이 잠긴 재고 행에 붙잡혀 있는 동안 같은 키가 오면 409 REQUEST_IN_PROGRESS와 Retry-After: 1. 잠금이 풀리면 첫 요청은 201. 앞 묶음 V14의 두 트랜잭션 방식 | 멱등 규칙 4 |
| K18 | 6 | 거절된 요청(PRICE_CHANGED)은 완료로 캐시되지 않는다. 같은 키로 고친 body를 보내면 201이고 진행 중 기록이 남아 있지 않다 | 멱등 규칙 7 |
| K19 | 6 | 마지막 객실 하나에 두 손님이 동시에 요청하면 하나만 201이고 다른 하나는 409 INVENTORY_UNAVAILABLE이며 heldCount는 1이고 Booking은 하나다. 잠긴 행을 두고 두 요청이 실제 MySQL에서 경합한다 | T08, R2, 08-3 결정 3 |
| K20 | 6 | guestCount 0과 101, currency USD, expectedTotalAmount 0, 미정의 필드는 400 INVALID_REQUEST. checkOut이 checkIn 이하, 31박, 서울 어제, 잘못된 날짜 형식은 400 INVALID_DATE_RANGE. 서울 오늘은 통과 | 11 명세 BOOK-01 요청 표, 공통 절 38행, P03 |
| K21 | 6 | 행위자 없음은 401 ACTOR_REQUIRED. HOST가 예약을 요청하면 403 | 11 인증과 접근 제어, P07 |
| K22 | 6 | 예약으로 heldCount가 1인 날짜의 totalCount를 soldCount + heldCount 아래로 INV-03으로 내리면 409 INVENTORY_BELOW_COMMITTED이고 수량이 유지된다 | T04의 heldCount 몫. 앞 묶음 V1 보강 |
| K23 | 6 | 없는 roomType은 404 RESOURCE_NOT_FOUND. maxOccupancy 초과는 409 OCCUPANCY_EXCEEDED. 요금 없는 날짜는 409 RATE_NOT_CONFIGURED. 재고 행 없는 날짜는 409 INVENTORY_NOT_CONFIGURED. 넷 다 Booking과 Hold가 없다 | T07, BOOK-01 에러 표 |
| K24 | 6-2 | BOOK-02가 본인 예약만 내고 createdAt 내림차순 동률이면 id 내림차순이다. status 필터가 걸리고 page와 size 기본값과 범위 밖 400과 범위 넘는 page의 빈 items가 맞다 | T01의 예약 조회 구간, T02, 11 목록 규칙 |
| K25 | 6-2 | BOOK-03이 본인 예약을 200과 serverNow와 함께 내고 남의 예약과 없는 id는 자원 정보 없이 404 RESOURCE_NOT_FOUND다 | T02, BOOK-03 처리 규칙 |

K7과 K12와 K17과 K19는 MySQL 통합 테스트다. 메모리 저장소로 대신하지 않는다. 롤백과 유니크와 잠금과 커밋 뒤 전달은 DB와 트랜잭션이 하는 일이라 메모리로 바꾸면 확인하려던 것이 사라진다. K17과 K19는 앞 묶음 InventoryApiTest의 V14처럼 TransactionTemplate으로 재고 행을 먼저 잠근 뒤 요청을 보내고 잠금을 풀어 순서를 만든다.

K5로 앞 묶음 8-1 V2가 닫힌다. 앞 묶음이 I1a 검사 코드를 DailyInventory에 넣어 두었고 이번에 그 검사에 닿는 메서드 넷이 생긴다.

## 9. 실행과 검증

| 항목 | 내용 |
|---|---|
| 작업 디렉터리 | backend/ |
| 실행 환경 | Spring Boot 4.1.1, Gradle 9.7.1 wrapper, Java 툴체인 21(JAVA_HOME은 C:/Users/user/.jdks/ms-21.0.11), MySQL 9.7.2 컨테이너 o2o-catalog-mysql. 환경변수 이름은 O2O_MYSQL_ROOT_PASSWORD, O2O_MYSQL_USER, O2O_MYSQL_PASSWORD. 값은 backend/.env이고 저장소에 없으며 모델은 그 파일을 읽지 않는다 |
| 실행할 명령 | docker compose -f backend/docker-compose.yml up -d로 DB를 올린다. cd backend && ./gradlew test로 테스트를 돌린다. 예상 결과는 BUILD SUCCESSFUL과 실패 0이고 실행 수는 123 + 이번 묶음 테스트 수다. 6단계와 6-2단계는 ./gradlew bootRun으로 띄우고 curl로 친다 |
| 테스트 DB | 127.0.0.1:3307, o2o_catalog_test. 설정 파일 기본값 그대로. 다른 두 세션은 o2o_promo_test와 o2o_payment_test라 부딪히지 않는다 |
| 데이터 초기화 허용 범위 | 테스트 프로파일의 ddl-auto가 create-drop이라 테스트 DB 스키마 전체다. 개발 DB o2o_catalog는 ddl-auto update라 표가 생기기만 한다. 운영 DB는 없다 |
| 빌드 출력과 로그 경로 | backend/build/test-results/test/*.xml. 사본을 harness/out/task-S9-booking-R1/step{단계}/에 남긴다. 실제 서버 기록은 harness/out/task-S9-booking-R1/step6/http-calls.txt와 step6-2/http-calls.txt |
| 계약 테스트 ID | 8-1절의 K1부터 K25. 검증 ID는 T06, T07, T08, T09, T10, T11 전부와 T12, T13의 요금 절반과 T01, T02의 예약 조회 구간과 T04의 heldCount 몫 |
| A와 B 평가 범위 | eval-criteria-code.md의 축 전부. 오늘의 전제 결정 3에 따라 MVP 코드가 다 붙은 뒤 한 번이며 이 묶음 몫은 5절 표다 |
| 필수 검증을 실행하지 못했을 때 | progress.md의 실패 원인 칸에 명령과 출력을 적고 결과를 halted로 남긴다. 미실행을 통과로 적지 않는다 |

T12와 T13이 절반인 이유. 둘 다 요금과 프로모션 두 갈래가 있다. 1차 어댑터는 요금만 합산하므로 요금 변경으로 인한 PRICE_CHANGED와 요금 변경 뒤 스냅샷 유지까지만 닫는다. 프로모션 갈래는 2차가 PricingService 어댑터를 붙인 뒤 닫는다. T29는 만료가 있어야 하므로 2차다.

## 10. 승인과 진행

| 항목 | 기록 |
|---|---|
| 작업 계약 승인 | 승인. join5201, 2026-09-11. 결정 5건은 D-1부터 D-4 가, D-5 나 |
| 개정 | 없음 |
| 마지막 성공 단계 | 1단계 완료(2026-09-11). 계약과 결정 5건 승인 |
| 실제 사용 시간 (1단계) | 20분 |
| 실제 사용 시간 (2단계) | 미착수 |
| 실제 사용 시간 (3단계) | 건너뜀. 앞 묶음이 세웠다 |
| 실제 사용 시간 (4단계) | 미착수 |
| 실제 사용 시간 (5단계) | 미착수 |
| 실제 사용 시간 (6단계) | 미착수 |
| 실제 사용 시간 (6-2단계) | 미착수 |
| 미해결 사항과 다음 작업 | 2단계 이슈와 초안 PR부터 6-2단계까지 정지 없이 간다. 다음 정지점은 9단계 검증 표 |
| 최종 산출물과 버전 | 작업 후 기록 |
| 실제 사용 시간 | 미측정. 단계별 실측 합을 9단계에서 적는다 |
| 최종 완료 판단 | 대기 |
