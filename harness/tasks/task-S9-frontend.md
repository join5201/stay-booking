# 작업 계약 task-S9-frontend (프론트 화면 전체)

최초 작성: 2026-09-15
최종 갱신: 2026-09-16 (평가 라운드 준비. 5절 해시 둘 갱신(README 한 줄 수정 뒤 재생성한 평가 대상 목록과 프론트 축 여섯이 더해진 기준), 4절 기준 해시, 10절 미해결. 이슈 186. 같은 날 앞서 PR 스택 병합 뒤 10절 미해결 갱신. 같은 날 앞서 T10 마무리 끝. 5절 평가 대상 코드 행 기입, 9절 T10 실행 결과 행, 10절. main 병합 뒤 4절 해시 둘 갱신(02 v9와 백엔드 README). 같은 날 앞서 T9 게스트 3 끝. 9절 T9 실행 결과 행, 10절. 그 앞서 T8 게스트 2 끝. 9절 T8 실행 결과 행, 10절. 그 앞서 T7 게스트 1 끝. 9절 T7 실행 결과 행, 10절. 그 앞서 T6 운영자 끝. 9절 T6 실행 결과 행, 10절. 그 앞서 T5 호스트 2 끝. 9절 T5 실행 결과 행, 10절. 4절 백엔드 README 해시 갱신(재고 R2 보류 PR 163 뒤. 1절부터 4절은 그대로). 그 전날 T4 호스트 1 끝. 9절 T4 실행 결과 행, 10절. 그 전 같은 날 T3 API 층 끝. 9절 T3 실행 결과 행, 10절. 4절 해시 둘 갱신(예약 R1 반영 뒤 예약 컨트롤러와 백엔드 README). 그 전 같은 날 T2 공통 UI 끝. 6절 스택 행에 Tailwind 4 토큰 자리와 proxy 이름, 9절 실행 환경과 T2 실행 결과 행, 10절. 그 전 같은 날 T1 기반 끝. 9절 실행 환경 행에 package-lock.json 판 기입과 T1 실행 결과 행, 10절 마지막 성공 단계와 실제 사용 시간. 그 전 같은 날 계약 승인. D-1부터 D-7 추천대로 확정. 작업 표 이름 확정, 6절 지역 코드 행, 7절 결정 줄 일곱, 10절. 그 전 같은 날 초안. 인계 문서 HANDOFF.md를 받아 화면과 API 대응표를 옮기고 백엔드 실물과의 차이 여섯을 2-1절에 고정)
양식: harness/prompts/task-contract.md v6

이 계약은 프론트 Task 계열의 첫 계약이다. 백엔드 40단위가 main에 다 올라간 뒤(2026-09-13 PR 138, 커밋 1bdadfe) 사용자가 2026-09-14 Claude Design에 화면 설계를 시켰고(harness/out/claude-design-handoff-2026-09-14/prompt.md와 context.md), 그 결과의 요약본 HANDOFF.md가 2026-09-15 04:54에 왔다. 원문은 같은 폴더 received/ 아래에 받은 그대로 두었다. 이 계약은 그 요약본을 구현 입력으로 옮기되 백엔드 실물과 어긋난 여섯 곳을 명세 값으로 바로잡는다(2-1절). 사용자 결정 셋(2026-09-15 05:0x)이 전제다. 첫째, 데스크톱 전용과 브랜드 색은 사용자가 디자인 세션에서 정한 것이다. 둘째, 화면 설계 파일 다섯(01부터 05 .dc.html)은 없고 요약대로 간다. 셋째, 계약 초안을 진행한다.

쉽게 말하면 이렇다. 백엔드는 주문을 받는 주방이고 다 지어졌다. 이제 손님이 주문을 넣는 창구(화면)를 짓는다. 디자인이 창구 도면을 그려 줬는데 도면에 적힌 주방 규칙 여섯 줄이 실제 주방과 달라서, 도면은 그대로 두고 이 계약이 그 여섯 줄을 고쳐 읽는다. 창구는 주방을 고치지 않는다. 주방이 바뀌어야 할 것 같으면 이슈로만 적는다.

## 작업

| 항목 | 내용 |
|---|---|
| Task ID와 Step | task-S9-frontend. Step 9 구현의 프론트 몫. 10-6 E절(프론트 개발)과 F절(연결 테스트) 전부. 이름과 단위는 7절 D-1로 확정(계약 하나, 2026-09-15) |
| 작업 유형 | 코드 |
| 목표 | Next.js 프론트를 frontend/ 아래에 만들어 화면 열여섯(G1부터 G7, H1부터 H5, O1과 O2, C1과 C2)이 백엔드 API 32개를 실제로 부르고, 인계 문서의 상태 전이와 오류 표시 세 자리를 지키며, E2E 여섯이 백엔드와 DB 상태까지 맞는 것을 확인한다 |
| 대상 API ID | CAT-01부터 09, INV-01부터 05, RATE-01부터 04, PROMO-01부터 05, SEARCH-01부터 03, BOOK-01부터 04, PAY-01과 02. INTERNAL-01은 화면이 부르지 않고 E2E의 DEFER 시험(8-1 E03)에서만 시험 코드가 부른다 |
| 선행 작업 | 백엔드 여섯 묶음 전부 main(1bdadfe). 숙소 R1 반영(PR 145, 지역 코드 열일곱)까지 main(af34431). 남은 블라인드 평가 네 쌍(재고, 프로모션 검색, 예약, 결제)은 이 Task와 독립이고(인계 폴더 README 3절) 그 반영이 백엔드를 바꾸면 2-1절을 갱신한다 |
| 완료 기준 | 넷을 모두 만족해야 한다. 첫째, 8절 단계 T1부터 T10이 전부 끝난다. 둘째, 8-1절 테스트 ID(W01부터 W15, E01부터 E06)가 전부 통과이거나 미실행 사유와 함께 기록된다. 셋째, npm run build와 typecheck와 lint가 실패 0이고 결과 사본이 harness/out/task-S9-frontend-R1/ 아래에 있다. 넷째, 모든 단계의 실제 시간이 harness/state/progress.md에 분 단위로 기입돼 있다 |
| 변경 허용 파일과 범위 | frontend/ 전체(새로 만든다). harness/out/task-S9-frontend-R1/ 아래 실행 결과. harness/state/progress.md 행 추가. 이 계약 파일. 저장소 루트 .gitignore에 frontend 산출물 줄(node_modules, .next, 테스트 리포트)을 더하는 것은 diff 요약 뒤 허용. backend/, document/, harness/의 나머지는 읽기만 한다. 백엔드가 바뀌어야 하면 이슈만 연다 |
| 범위 밖과 유지할 전제 | 아래 3절 |
| 기준 버전 | 문서는 4절 입력 표의 sha256. 백엔드는 origin/main af34431(2026-09-14 22:02 PR 146 병합). 프론트 코드는 작업 브랜치의 커밋 해시 |
| 후보 작업 공간 | frontend/. 작업 브랜치는 T1에서 feat/task-s9-frontend로 딴다. 후보와 정본의 구분은 git 브랜치가 맡는다 |
| 결과 기록 경로 | harness/state/progress.md |

## 1. 이 Task가 앞 묶음과 다른 점

| 항목 | 백엔드 묶음 여섯 | 이 Task |
|---|---|---|
| 입력의 정본 | document/11 API 명세와 06 설계 문서 | 인계 문서(received/HANDOFF.md)와 그것이 요약한 context.md. 백엔드 사실이 어긋나면 명세와 코드가 맞다(2-1절) |
| 만드는 것 | 애그리거트, 앱 서비스, HTTP 입구, 테스트 | 라우트 열여섯, 공통 컴포넌트 스물여섯, 훅 서른셋, fetch 래퍼, 오류 배치, E2E 여섯 |
| 검증의 뜻 | 테스트 xml의 failures와 errors 합 0 | typecheck와 lint와 build 실패 0, 컴포넌트와 훅 테스트 통과, E2E 여섯이 백엔드와 DB 상태와 맞음 |
| 백엔드 접근 | 코드 안 | HTTP만. next.config rewrites로 /api/v1을 백엔드로 넘긴다(교차 출처 설정이 백엔드에 없다. context.md 2절) |
| 행위자 | 테스트가 X-Dev-Actor-Id를 직접 넣음 | 개발용 바(C1)가 쿠키 dev_actor에 담고 fetch 래퍼가 헤더로 바꾼다. 백엔드는 쿠키를 읽지 않는다 |
| 시간 | 고정 Clock | 브라우저 시계를 쓰지 않는다. 남은 시간은 응답의 expiresAt과 serverNow 차이다 |
| 평가 기준 | eval-criteria-code.md 축 그대로 | 같은 파일에 프론트 축이 0개다. 7절 D-2 |
| 진행 단위 | 기능 묶음 하나가 검증까지(N9) | 8절 단계 하나가 자기 검증까지. 한 턴에 한 단계 |

## 2. 대상과 설계 근거

화면 ID와 라우트와 API 대응은 인계 문서 13행부터 32행(라우트)과 105행부터 116행(화면별 핵심 규칙)과 75행부터 80행(상태 전이)을 옮긴 것이다. 실패 코드별 반응은 81행부터 86행(오류 표시 세 자리)을 따르되 2-1절의 여섯 곳은 이 표의 값이 우선한다.

| 화면 | 라우트와 쿼리 | 부르는 API와 시점 | 성공 시 화면 변화 | 실패 코드별 반응 |
|---|---|---|---|---|
| G1 검색 | / 와 regionCode, checkIn, checkOut, guestCount, page | SEARCH-01. 쿼리 넷이 다 있을 때만 | 숙소 카드 목록. 카드 머리는 G2, 카드 안 객실 줄은 G3(쿼리 전달) | 날짜 순서와 30박과 인원과 지역 코드는 화면 검사가 먼저. 400은 필드 아래. 빈 결과는 빈 상태 |
| G2 숙소 상세 | /properties/[propertyId] (검색 쿼리 유지) | CAT-03, CAT-09 진입 시 | 숙소 정보와 객실 타입 목록. 객실 줄은 G3 | 404 전면 Notice |
| G3 객실 상세 | /room-types/[roomTypeId] (검색 쿼리 유지) | CAT-08 진입 시. 조건 넷이 있으면 SEARCH-02, SEARCH-03, PROMO-05 동시 | 날짜별 가용 수, 예상 금액(날짜별 표와 합계, 적용 프로모션), 적용 가능 프로모션. 예상 금액이며 확정 금액이 아님 고정 문구 | 셋 중 하나 실패, available이 false, public 행위자 중 하나면 예약 버튼 비활성과 이유. days의 null은 판매 안 함. INVENTORY_UNAVAILABLE과 INVENTORY_NOT_CONFIGURED와 RATE_NOT_CONFIGURED와 OCCUPANCY_EXCEEDED는 날짜별 사유 |
| G4 예약 확인 | /room-types/[roomTypeId]/book (쿼리 없으면 G3로 replace) | 진입 시 SEARCH-03을 다시 받아 그 값을 expectedTotalAmount로. 확인 버튼이 BOOK-01(마운트 시 키 하나) | 201 HELD면 G5로. 요청 중 두 버튼 비활성 | 409 PRICE_CHANGED는 SEARCH-03 재조회 뒤 새 금액으로 다시 확인(새 키). INVENTORY_UNAVAILABLE 계열은 그사이 객실이 마감 Notice. REQUEST_IN_PROGRESS와 5xx와 네트워크는 같은 키로 다시 시도 Banner. IDEMPOTENCY_KEY_REQUIRED와 REUSED는 화면 결함이라 일반 오류와 새 키 |
| G5 결제 | /bookings/[bookingId]/pay (HELD 아니면 G7로 replace) | BOOK-03 진입 시. 결제 버튼이 PAY-01(클릭마다 새 키, body의 mockMode는 쿠키 dev_mock_mode 값이고 APPROVE면 생략 가능). 성공이나 replayed 뒤 BOOK-03 재조회 | CONFIRMED면 확정 카드와 G7. HELD이고 FAILED가 3 미만이면 재시도. HELD이고 REQUESTED(DEFER)면 처리 중과 수동 새로 고침, 결제 버튼 비활성. EXPIRED(TTL_EXPIRED 또는 PAYMENT_FAILED)면 만료 카드와 같은 조건으로 새 예약(G1 쿼리). 남은 시간은 expiresAt과 serverNow 차이, 시도 n/3 | BOOKING_EXPIRED, BOOKING_STATE_CONFLICT, PAYMENT_IN_PROGRESS, PAYMENT_ATTEMPTS_EXHAUSTED는 BOOK-03 재조회 뒤 상태 화면과 짧은 띠. REQUEST_IN_PROGRESS와 네트워크는 같은 키 |
| G6 예약 목록 | /bookings 와 status, page | BOOK-02. 줄마다 CAT-03과 CAT-08로 이름(인계 문서 Q1) | 목록. 필터는 전체, HELD, CONFIRMED, EXPIRED, CANCELED 고정 | 401 ACTOR_REQUIRED는 개발용 바 안내. status는 화면이 네 값 밖을 만들지 않는다 |
| G7 예약 상세 | /bookings/[bookingId] | BOOK-03 진입 시, Countdown 0 도달 시 1회, 새로 고침 버튼. 취소 시트가 열릴 때 키 하나, 닫으면 폐기. 확인이 BOOK-04(reason 300자 이하). 성공이나 replayed 뒤 BOOK-03 재조회 | HELD면 Countdown과 결제하기(G5). CONFIRMED이고 체크인 서울 날짜가 serverNow 서울 날짜보다 뒤면 예약 취소 활성. CANCELED면 환불 줄. EXPIRED면 읽기만. 주기 폴링 없음 | CANCELLATION_NOT_ALLOWED와 BOOKING_STATE_CONFLICT는 재조회 뒤 상태와 띠. 404 전면 |
| H1 내 숙소 | /host/properties | CAT-05 | 목록과 페이지 | 401과 403은 Notice |
| H2 숙소 등록과 수정 | /host/properties/new, /host/properties/[propertyId]/edit | 수정은 CAT-03 진입 시. 저장이 CAT-01(등록) 또는 CAT-02(수정. version 숨김 보관, 바뀐 필드만) | 저장 뒤 H1. 지역 코드는 열일곱 고정 선택(2-1절 6행) | 400 INVALID_REQUEST는 details의 field를 필드 아래에. 409 VERSION_CONFLICT는 새로 읽기(CAT-03). 404 전면 |
| H3 객실 타입 | /host/properties/[propertyId]/room-types 와 edit 또는 new=1 | CAT-09 진입 시. 편집 패널이 열릴 때 CAT-08. 저장이 CAT-06 또는 CAT-07. 저장 뒤 CAT-09 재조회 | 옆 패널로 등록과 수정 | VERSION_CONFLICT는 새로 읽기(CAT-08). 400은 필드 아래 |
| H4 재고 달력 | /host/room-types/[roomTypeId]/inventories 와 from, to | 머리는 CAT-08(탭 전환 시 재호출 없음, from과 to를 H5와 공유). INV-04 기간(최대 366일). 셀이 누락이면 INV-01 폼, 있으면 INV-05(version)를 받은 뒤 INV-03. 일괄은 INV-02 | 달력. 등록 폼은 직전 날짜 값을 미리 채움 | INV-02의 409 RESOURCE_ALREADY_EXISTS는 전부 실패다. 겹치는 날짜를 보내기 전에 막고 409면 INV-04로 되돌린다(2-1절 2행). 단건 등록의 RESOURCE_ALREADY_EXISTS는 수정 폼으로 자동 전환과 INV-05. INVENTORY_BELOW_COMMITTED는 필드 아래이고 숫자는 INV-05의 heldCount와 soldCount로 채운다(2-1절 5행). VERSION_CONFLICT는 새로 읽기 |
| H5 요금 달력 | /host/room-types/[roomTypeId]/rates 와 from, to | 머리와 PeriodPicker와 CalendarGrid를 H4와 공유. RATE-03 기간. 셀이 누락이면 RATE-01(date, amount, currency KRW), 있으면 RATE-04 뒤 RATE-02(version, amount만. 2-1절 3행) | 달력. 일괄 등록 없음이라 날짜별 등록 안내(인계 문서 Q3) | RESOURCE_ALREADY_EXISTS는 수정 폼 전환. VERSION_CONFLICT는 새로 읽기 |
| O1 프로모션 목록 | /operator/promotions 와 enabled, page | PROMO-04 | 목록. 켜기와 끄기 토글 없음 | 401과 403 |
| O2 프로모션 등록과 수정 | /operator/promotions/new, /operator/promotions/[promotionId]/edit | 수정은 PROMO-03 진입 시. 저장이 PROMO-01 또는 PROMO-02. 머리의 사용 끄기와 사용 켜기는 확인 시트 뒤 PROMO-02에 enabled와 version만 | 저장 뒤 O1 | 400은 필드 아래. 숙박 기간 둘 다 또는 둘 다 아님과 할인율 1부터 99와 최소 박수 1부터 30은 화면 검사가 먼저. VERSION_CONFLICT는 새로 읽기 |
| C1 개발용 바 | 전 화면 위 36px | API 없음. 쿠키 dev_actor(public, guest_001, guest_002, host_001, host_002, operator_001)와 dev_mock_mode(APPROVE, DECLINE, DEFER) | 역할 변경은 queryClient.clear()와 역할 첫 화면 replace. 같은 역할의 ID 교체는 invalidateQueries. public이면 내 예약 탭 비활성과 Mock 선택 숨김. mockMode가 APPROVE가 아니면 G5 버튼 옆 개발용 표시 | 401 ACTOR_REQUIRED Notice가 이 바를 가리킨다 |
| C2 오류 표시 | 공통 | API 없음 | 세 자리. 필드 아래, 화면 안 Notice, 화면 위 Banner(성공 알림도 Banner, 5초) | 인계 문서 81행부터 86행. 오류에 빨간색을 쓰지 않는다 |

middleware는 쿠키의 역할과 경로 앞부분(/host, /operator, /bookings)이 어긋나면 그 역할의 첫 화면(G1, H1, O1)으로 보낸다(인계 문서 32행).

훅 서른셋은 인계 문서 48행부터 73행 표 그대로다. 그중 셋만 2-1절이 고친다. useCreateRate는 currency를 보내고 useUpdateRate는 보내지 않는다(3행). useBulkCreateInventory는 건너뜀 결과가 없고 409면 전부 실패다(2행). fetch 래퍼의 ApiError는 details와 reason이다(1행).

### 2-1. 인계 문서와 백엔드 실물의 차이 여섯 (이 표가 인계 문서보다 우선한다)

| 번호 | 인계 문서 | 백엔드 실제 | 화면이 할 일 | 근거 |
|---|---|---|---|---|
| 1 | 38행 ApiError의 fieldErrors 배열, 원소는 field와 message | 오류 본문은 code, message, traceId, details 넷이고 details 원소는 field와 reason. 필드가 특정되지 않으면 field는 빈 문자열 | ApiError를 status, code, message, traceId, details로 두고 필드 오류는 details에서 field로 찾는다. 인계 문서 Q5는 이름만 달랐고 details의 field가 본문 키인 것은 맞다 | document/11 123행. backend/src/main/java/com/o2o/shared/ErrorResponse.java 16행. backend/src/main/java/com/o2o/shared/SharedExceptionHandler.java 49행부터 50행 |
| 2 | 113행 일괄 등록은 있는 날짜를 건너뛰고 created와 skipped 띠 | 한 날짜라도 이미 있으면 전부 실패하고 409 RESOURCE_ALREADY_EXISTS. 성공 응답은 InventoryRange이고 건수 칸이 없다 | 일괄 등록 폼은 INV-04의 items에 있는 날짜와 겹치면 보내기 전에 막는다. 그래도 409면 INV-04를 다시 받아 겹친 날짜를 표시한다. created와 skipped 띠는 만들지 않는다 | document/11 737행. backend/src/main/java/com/o2o/inventory/api/InventoryExceptionHandler.java 40행 |
| 3 | 61행 RATE-01과 RATE-02에 currency KRW 고정 전송 | 요금 수정 본문은 version과 amount 둘뿐. 정의되지 않은 필드는 전역 설정이 400으로 막는다 | currency는 RATE-01에만 보낸다 | document/11 74행과 982행부터 983행. backend/src/main/java/com/o2o/inventory/api/AdjustRateRequest.java 13행부터 15행. backend/src/main/resources/application.properties 7행 |
| 4 | 84행 500과 503과 네트워크 오류에 Retry-After 초 카운트 | Retry-After는 409 REQUEST_IN_PROGRESS에만 1초. 503 TEMPORARY_FAILURE를 내는 코드가 백엔드에 없다 | 초 카운트는 REQUEST_IN_PROGRESS에만. 5xx와 네트워크는 일시적인 오류입니다 문구와 다시 시도 버튼(쓰기는 같은 키). 503 분기는 두되 헤더를 기대하지 않는다 | document/11 72행과 101행. backend/src/main/java/com/o2o/booking/api/BookingExceptionHandler.java 70행. 백엔드에서 TEMPORARY_FAILURE 검색 0건(2026-09-15) |
| 5 | 82행 선점 h 더하기 판매 s는 n 아래로 줄일 수 없다는 숫자 문구 | 오류 본문은 message 한 줄이고 숫자가 없다 | 숫자는 셀을 열 때 받은 INV-05의 heldCount와 soldCount로 화면이 채운다. 최소 재고 검사(선점 더하기 판매)는 화면 검사가 먼저 | backend/src/main/java/com/o2o/inventory/api/InventoryExceptionHandler.java 51행부터 52행 |
| 6 | 10행 지역 코드는 자유 입력, 대문자 강제 변환, 최근값 5개 localStorage | 2026-09-14 21:39 반영(PR 145) 뒤 등록 지역 코드 열일곱이 고정이고 숙소 등록과 수정과 목록에서 그 밖은 400 INVALID_REQUEST. 명세도 프론트 선택 목록을 초기 세팅에서 준비하라고 적는다. 검색과 프로모션이 목록을 확인할지는 남은 평가 쌍의 결정표 몫 | RegionCodeInput을 열일곱 고정 선택 상자로 바꾼다. 값은 lib/regions.ts 상수 하나이고 RegionRegistry와 같다(7절 D-6). 대문자 변환과 최근값 저장은 만들지 않는다. G1과 O2도 같은 상자를 쓴다 | backend/src/main/java/com/o2o/shared/RegionRegistry.java 24행부터 27행. backend/src/main/java/com/o2o/shared/SharedExceptionHandler.java 117행부터 124행. document/11 209행. harness/decisions/task-S9-catalog-R1.md A-02 행 |

넷(1, 2, 3, 4)은 context.md에 없던 사실이라 디자인이 짐작으로 채운 것이고, 6은 context.md를 쓴 뒤 백엔드가 바뀐 것이고, 5는 숫자가 오류 응답에 없어 화면이 스스로 채워야 하는 것이다. 디자인 판단은 하나도 건드리지 않는다.

인계 문서가 맞는 것(대조 2026-09-15). 헤더 셋(X-Dev-Actor-Id, Idempotency-Key, Idempotency-Replayed). PAY-01은 202와 REQUESTED 하나이고 APPROVE와 DECLINE의 결과는 커밋 직후 같은 스레드에서 전달돼 202 응답 전에 끝난다(backend/src/main/java/com/o2o/payment/infrastructure/MockAutoResultAdapter.java 35행부터 41행. 비동기 실행 없음). 그래서 G5의 PAY-01 뒤 BOOK-03 재조회가 바로 CONFIRMED나 FAILED를 본다. DEFER만 REQUESTED로 남는다. BOOK-02의 status 네 값 밖은 400. Booking 응답에 serverNow. SEARCH-01과 CAT-09는 Page 응답. BOOK-01 본문 여섯 필드 전부 필수. 개발용 행위자 다섯 ID. 백엔드는 쿠키를 읽지 않는다(Q4 해소). 교차 출처 설정 없음이라 rewrites가 맞다. 취소는 CONFIRMED이고 체크인 날짜 전만.

### 2-2. 이번 Task가 만들지 않는 것

| 항목 | 이유 |
|---|---|
| 로그인, 회원, 결제수단 입력, 쿠폰, 리뷰, 알림, 지도, 사진, 호스트의 예약 관리 | context.md 11절 v1 밖. 나중을 위한 빈 자리도 두지 않는다 |
| 모바일 대응 | 데스크톱 1200px 기준. 사용자 결정 2026-09-15(인계 문서 11행) |
| 주기 폴링 | Countdown 0 도달 시 BOOK-03 1회뿐. DEFER는 수동 새로 고침(인계 문서 Q6, 79행) |
| 프론트 서버의 프록시 핸들러 | next.config rewrites만. 헤더는 클라이언트 fetch 래퍼가 붙인다(인계 문서 8행) |
| 백엔드 코드 변경 | 없다. Q1(예약 응답에 숙소와 객실 이름)은 이슈 후보로만 적는다(7절 D-5) |
| 지역 코드 목록 API | 없다. 프론트 상수 열일곱(2-1절 6행) |
| 요금 일괄 등록 | API가 없다. 날짜별 등록 안내(인계 문서 Q3) |
| 서버 컴포넌트 데이터 조회 | 전부 use client 페이지. 서버 컴포넌트는 layout 껍데기만(인계 문서 6행) |

## 3. 범위 밖과 유지할 전제

| 항목 | 내용 |
|---|---|
| 백엔드 | 읽기만. 고치지 않는다. 화면이 백엔드의 변경을 요구하면 이슈를 열고 화면은 현재 백엔드에 맞춘다 |
| document/ | 고치지 않는다. 명세와 인계 문서가 다르면 명세가 맞다 |
| 하네스 파일 | 이 계약, harness/state/progress.md 행 추가, harness/out/task-S9-frontend-R1/ 아래만. 나머지는 읽기만. 프론트 평가 축(7절 D-2)은 별도 이슈의 하네스 세션 몫이라 이 Task가 eval-criteria-code.md를 고치지 않는다 |
| 인계 문서 원문 | harness/out/claude-design-handoff-2026-09-14/received/HANDOFF.md는 받은 그대로 둔다. 고칠 것은 이 계약 2-1절에 적는다 |
| 설계 파일 다섯 | 없다. 공통 컴포넌트의 props는 인계 문서 102행부터 103행의 이름 목록만으로 정한다. 사용자 결정 2026-09-15 |
| 디자인 결정 | 데스크톱 1200px 전용과 브랜드 색(트립닷컴 계열 파랑과 지브리 색 원칙, 인계 문서 88행부터 100행)은 사용자 결정 2026-09-15. 디자인 프롬프트 3절의 모바일 폭 먼저와 4절의 중립 색은 이 결정으로 대체됐다 |
| 남은 평가 네 쌍 | 이 Task와 독립. 그 반영이 백엔드를 바꾸면 2-1절과 4절 해시를 갱신한다. 검색과 프로모션의 지역 코드 확인 여부가 특히 그렇다 |
| 프론트 평가 | 한다. 2026-09-12 사용자 결정. 기준은 7절 D-2 |
| 비용 | 별도 LLM API 호출 없음(N1). 생성은 Claude Code, 평가는 Codex 새 작업, 전달은 사용자 |
| 진행 방식 | 승인 뒤 한 턴에 한 단계만 하고 멈춘다. 커밋은 이유 하나씩 가른다. 8절 단계마다 progress.md에 실제 시간 행 |
| v1 밖 | context.md 11절과 2-2절. 2026-09-12 결정(프로모션 CLOSED와 검색 프로젝션은 v1 밖)도 그대로 |

## 4. 입력과 적용 규칙 (2026-09-16 라운드 준비에서 평가 기준 해시 갱신. 같은 날 앞서 T10 뒤 해시 둘 갱신. 그 앞서 T5와 T3에서 갱신)

| 자료 | 경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 01 전체 | document/01-o2o-ddd-plan.md | sha256:17569703c90a18df | 전문 |
| 인계 문서 원문 | harness/out/claude-design-handoff-2026-09-14/received/HANDOFF.md | sha256:76d665f6ac36a9db | 전문. 2-1절 여섯 곳은 이 계약이 우선 |
| 인계 컨텍스트 | harness/out/claude-design-handoff-2026-09-14/context.md | sha256:0919a1aad3b72d9e | 전문. 특히 5절 API 33개, 6절 응답 모델, 7절 상태와 시간, 8절 오류 코드, 9절 입력 규칙, 10절 정책값, 13절 상태 이름 |
| 인계 프롬프트 | harness/out/claude-design-handoff-2026-09-14/prompt.md | sha256:73395fa086e85053 | 3절 반드시 지킬 것. 폭과 색 행은 3절 디자인 결정 행으로 대체 |
| 인계 폴더 안내 | harness/out/claude-design-handoff-2026-09-14/README.md | sha256:36aef71e2e530ced | 3절 받은 뒤 |
| 대상 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절 전부(28행부터 156행), API 32개 절 각각의 요청 표와 응답과 오류 표와 처리 규칙, 응답 모델 절(2284행부터), 검토할 정책 P01부터 P11 |
| 대상 02 기능 목록 | document/02-o2o-feature-list.md | sha256:89c36d3211281b02 | 전문. 화면 문구의 용어. 2026-09-16 T10 뒤 origin/main 병합으로 v9(08-3 결정 반영 착지. 2절 R4 문언과 R6, 3-3 TTL 경합 문장과 CANCELED 분기). 화면 문구는 v8로 만들었고 v9와의 문구 대조는 안 했다(10절 미해결) |
| 대상 06-6 설계 핵심 요약 | document/06-6-o2o-design-digest.md | sha256:5d5a4758305d9625 | 전문. 상태 전이와 보장 다섯 |
| 오류 본문 | backend/src/main/java/com/o2o/shared/ErrorResponse.java | sha256:c62440297db7ef67 | 전문. 2-1절 1행 |
| 공통 오류 처리 | backend/src/main/java/com/o2o/shared/SharedExceptionHandler.java | sha256:3296140deac47518 | 49행부터 50행 details 매핑, 117행부터 124행 미등록 지역 코드 |
| 지역 코드 | backend/src/main/java/com/o2o/shared/RegionRegistry.java | sha256:79160f582d45d513 | 24행부터 27행 열일곱 값. 프론트 상수의 출처 |
| 개발용 행위자 | backend/src/main/java/com/o2o/shared/ActorRegistry.java | sha256:9b0e000f80967372 | 22행부터 32행 ID 여섯 |
| 행위자 판정 | backend/src/main/java/com/o2o/shared/ActorResolver.java | sha256:c40ff8e6d5a4547b | dev 프로파일 밖은 401 |
| 재고 오류 | backend/src/main/java/com/o2o/inventory/api/InventoryExceptionHandler.java | sha256:78347a957a155424 | 40행 RESOURCE_ALREADY_EXISTS, 51행부터 52행 INVENTORY_BELOW_COMMITTED |
| 요금 수정 본문 | backend/src/main/java/com/o2o/inventory/api/AdjustRateRequest.java | sha256:c2dbdf5f93824c47 | 전문. 2-1절 3행 |
| 예약 오류 | backend/src/main/java/com/o2o/booking/api/BookingExceptionHandler.java | sha256:7d8ee112c516c8bd | 66행부터 70행 Retry-After |
| 예약 컨트롤러 | backend/src/main/java/com/o2o/booking/api/BookingController.java | sha256:cb78f0179d648c9e | 87행부터 211행. Idempotency-Replayed, PAY-01 202, BOOK-02 status. 2026-09-15 예약 R1 반영(PR 156)으로 BOOK-02가 shared의 PageResponse를 쓴다. 응답 모양은 같다 |
| 자동 결과 어댑터 | backend/src/main/java/com/o2o/payment/infrastructure/MockAutoResultAdapter.java | sha256:07e7585a7dc2b0af | 전문. APPROVE와 DECLINE 결과가 응답 전에 끝나는 근거 |
| 예약 앱 서비스 | backend/src/main/java/com/o2o/booking/application/BookingApplicationService.java | sha256:d0e71e27a80d056a | 92행 o2o.booking.hold-ttl 키. E2E의 TTL 만료 시험(7절 D-4) |
| 만료 스케줄러 | backend/src/main/java/com/o2o/booking/infrastructure/BookingExpireScheduler.java | sha256:0cd50fa7fa7e6c7c | 31행 o2o.booking.expire-scan-interval 키 |
| 백엔드 설정 | backend/src/main/resources/application.properties | sha256:a53c90a2ad96f493 | 7행 fail-on-unknown-properties, 22행 datasource url, 35행 dev 프로파일. 포트 설정이 없어 8080 |
| 백엔드 실행 안내 | backend/README.md | sha256:65ea1e5153d4dd4b | 1절부터 4절. 판, 준비, 실행, 포트. 2026-09-15 6절 상태 행만 바뀜(PR 157). 2026-09-16 6절 재고 행에 R2 보류만 바뀜(PR 163). 같은 날 T10 뒤 6절 결제와 확정 행에 결제 R1 반영(PR 175)만 바뀜 |
| 숙소 결정표 | harness/decisions/task-S9-catalog-R1.md | sha256:97d3e84f210bf0ac | A-02 행. 지역 코드 열일곱 확정 |
| 구현 계획 10-6 | harness/docs/10-6-o2o-harness-implementation-plan.md | sha256:eec97503b53ffff0 | E절 프론트 개발, F절 연결 테스트, 3절 기능별 API와 검증 연결 표 |
| 적용할 코드 양식 | harness/prompts/dev-ptcf-prompt.v3.md | sha256:4fd959abf491efe0 | 문체, 코드, 금지 절 |
| 실제 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:bf11b34c286c52f2 | 축, 심각도, 출력 스키마. 프론트 축 여섯이 더해진 판(7절 D-2, 이슈 186. 2026-09-16 라운드 준비에서 갱신) |
| 확정 전제 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 1절 확정 전제, 2절 요구사항 R1부터 R5 |

인계 프롬프트와 폴더 안내와 10-6과 결정표는 이 표에만 있고 5절에는 없다. harness/docs/의 계획 문서는 평가 입력이 될 수 없다(CLAUDE.md 3절). 인계 문서 원문과 컨텍스트는 화면 요구의 원천이라 5절에 넣는다(7절 D-3).

## 5. A와 B 평가 허용 입력 (HR1) (2026-09-16 라운드 준비에서 해시 둘 갱신. 같은 날 앞서 T10 평가 대상 코드 행 기입)

프론트 평가는 한다(2026-09-12 결정). 라운드는 T10이 끝난 뒤 한 번이다. 이 표는 그때 넘길 것이다.

| 자료 | 경로 | 버전 또는 해시 | 읽을 범위 |
|---|---|---|---|
| 평가 대상 코드 | harness/out/task-S9-frontend-R1/eval-target-files.md | sha256:693273e0f2bb30d1 | 목록의 148개 파일 전문(frontend/ 추적 파일 전부. 기준 커밋 8530a82. package-lock.json은 판 대조만. frontend/AGENTS.md는 평가자 규칙이라 대상이 아니다). 버전 칸은 그 목록 파일의 해시이고 파일마다의 sha256은 목록 안에 있다(2026-09-16 T10 기입. 같은 날 라운드 준비에서 README 한 줄 수정 뒤 재생성) |
| 입력 팩 | harness/project-sync/o2o-review-input-pack.md | sha256:1608942c0815f911 | 전문 |
| 이 작업 계약 | harness/tasks/task-S9-frontend.md | 자기 해시 없음 | 전문 |
| 실제 평가 기준 | harness/prompts/eval-criteria-code.md | sha256:bf11b34c286c52f2 | 축, 심각도, 출력 스키마. 프론트 축 여섯이 더해진 판(7절 D-2, 이슈 186. 2026-09-16 라운드 준비에서 갱신) |
| 대상이 참조하는 11 API 명세 | document/11-o2o-api-spec.md | sha256:3f2613a77b649903 | 공통 절, API 32개 절, 응답 모델 |
| 대상이 참조하는 인계 문서 | harness/out/claude-design-handoff-2026-09-14/received/HANDOFF.md | sha256:76d665f6ac36a9db | 전문. 화면 요구의 원천. 2-1절 여섯 곳은 계약이 우선 |
| 대상이 참조하는 인계 컨텍스트 | harness/out/claude-design-handoff-2026-09-14/context.md | sha256:0919a1aad3b72d9e | 4절부터 13절 |
| 대상이 참조하는 오류 본문 | backend/src/main/java/com/o2o/shared/ErrorResponse.java | sha256:c62440297db7ef67 | 전문 |
| 대상이 참조하는 지역 코드 | backend/src/main/java/com/o2o/shared/RegionRegistry.java | sha256:79160f582d45d513 | 24행부터 27행 |
| 대상이 참조하는 행위자 | backend/src/main/java/com/o2o/shared/ActorRegistry.java | sha256:9b0e000f80967372 | 22행부터 32행 |

이 표에 넣지 않는 것: 01 전체, 과거 감사 문서, 이전 평가 리포트, 사용자 결정표, harness/docs/ 전체, harness/out/의 계획 문서(인계 폴더의 prompt.md와 README.md 포함), harness/state/ 전체, 생성 대화, 설계 파일 다섯(없다).

요구사항 역추적 축에 대한 지시. 입력 팩 2절의 R1부터 R5는 백엔드가 지키는 보장이고 화면은 그 보장을 깨는 동작을 유도하지 않는 쪽이다(context.md 1절 표의 셋째 칸). 이 Task의 역추적 대상은 2절 대응표의 화면 열여섯 행과 2-1절 여섯 행과 8-1절 E01부터 E06이다. 백엔드 보장 자체는 해당 없음으로 적고 미커버로 적지 않는다.

## 6. 정책 적용 (2026-09-15 T2 스택 행 실측 기입)

| 정책 ID 또는 쟁점 | 적용할 값 또는 판단 | 상태와 사용자 확인 |
|---|---|---|
| P01 선점 유지 시간 | 채택. 값은 서버가 expiresAt으로 준다. 화면은 expiresAt과 serverNow 차이로 세고 0이면 BOOK-03을 1회 다시 읽는다. 브라우저 시계는 쓰지 않는다 | 확정. 백엔드 구현값 10분 |
| P02 기간 제한 | 채택. 검색과 예약 30박, 재고와 요금 366일을 화면 검사가 서버보다 먼저 막는다 | 확정. 백엔드와 같다 |
| P03 통화와 달력 | 채택. KRW 정수, 천 단위 구분과 원(Money). 숙박 날짜는 서울 날짜 문자열 그대로. 시각은 UTC로 받아 서울로 표시(DateText). 금액과 시간은 tabular-nums | 확정 |
| P04 프로모션 선택 | 표시만. 서버가 하나를 고르고 화면은 selected를 보여 준다. 게스트 선택 UI 없음 | 확정 |
| P05 취소 | 채택. CONFIRMED이고 체크인 서울 날짜가 serverNow 서울 날짜보다 뒤일 때만 취소 버튼 활성. 전액 환불 줄 표시 | 확정 |
| P06 금액 차이 | 채택. G4의 expectedTotalAmount는 진입 시 SEARCH-03을 다시 받은 값. PRICE_CHANGED면 재조회한 금액으로 다시 확인이고 새 키 | 확정 |
| P07 로컬 행위자 | 채택. 쿠키 dev_actor를 fetch 래퍼가 X-Dev-Actor-Id로 바꾼다. public이면 헤더 생략. 로그인 화면 없음. 개발용이라는 것이 화면에서 드러난다 | 확정 |
| P09 가격 계산과 P10 판매 제약 | 표시만. 날짜별 표는 서버 값 그대로. 재고 0과 누락은 판매 안 함 | 확정 |
| P11 프로모션 종료 | 채택. 사용 끄기는 PROMO-02에 enabled false와 version. 상태 값 없음 | 확정 |
| 스택 | Next.js App Router, TypeScript, 전부 use client 페이지, TanStack Query v5, Tailwind(토큰은 tailwind.config theme.extend), next.config rewrites, MSW(훅 테스트 목), Playwright(E2E). 판은 T1에서 설치된 실제 판을 9절에 적는다. 기억으로 적지 않는다(N6). T2 실측: 설치된 Tailwind가 4판이라 토큰은 tailwind.config가 아니라 app/globals.css의 @theme에 있고 tailwind.config 파일은 없다(빌드 CSS에 토큰 유틸리티 생성 확인. harness/out/task-S9-frontend-R1/t2/css-tokens-check.txt). middleware는 Next 16에서 proxy로 이름이 바뀌어 파일이 frontend/proxy.ts이고 함수 이름도 proxy다(node_modules/next/dist/lib/constants.js의 PROXY_FILENAME과 server/web/types.d.ts의 NextMiddleware deprecated 표시). 인계 문서의 두 이름은 그대로 두고 여기서 고쳐 읽는다 | 인계 문서 5행부터 11행. 사용자 확정 2026-09-15. Tailwind 4와 proxy는 2026-09-15 T2 실측 |
| 개발용 쿠키 | dev_actor와 dev_mock_mode. Path는 /, SameSite는 Lax, httpOnly 아님, 30일 | 인계 문서 9행. 확정 |
| mockMode의 자리 | 게스트 화면이 아니라 개발용 바. 디자인 프롬프트 질문 목록의 첫 항목이 이렇게 닫혔다 | 인계 문서 9행과 109행. 확정 |
| 멱등키 | crypto.randomUUID(). 화면 상태(useRef)에만 보관하고 URL과 저장소에 두지 않는다. G4는 마운트 시 하나, PRICE_CHANGED 뒤 다시 확인은 새 키, 네트워크와 5xx와 REQUEST_IN_PROGRESS 재시도만 같은 키. G5는 클릭마다 새 키. G7 취소는 시트가 열릴 때 하나. 쓰기 성공이나 replayed 뒤 BOOK-03 재조회 | 인계 문서 41행부터 46행. 명세 멱등 규칙 5와 6과 일치 |
| serverNow | Booking 응답의 serverNow가 우선. 없는 응답은 Date 헤더로 오프셋 | 인계 문서 39행과 Q2. 카운트다운이 있는 화면(G5, G7)은 전부 BOOK-03을 읽으므로 Date 헤더 경로는 보조 |
| 오류 표시 세 자리 | 필드 아래, 화면 안 Notice, 화면 위 Banner. 오류에 빨간색 없음. 2-1절 1행과 4행과 5행이 세부를 고친다 | 인계 문서 81행부터 86행. 확정 |
| 폭과 색 | 데스크톱 1200px 전용. 토큰은 인계 문서 88행부터 100행 | 사용자 결정 2026-09-15 |
| 지역 코드 | 프론트 상수 열일곱(RegionRegistry와 같은 값). 고정 선택 상자. G1, H2, O2가 같은 상자 | 2-1절 6행. 확정. join5201, 2026-09-15(계약 승인, D-6 가) |
| 이름 표시(Q1) | G6과 G7이 줄마다 CAT-03과 CAT-08을 부른다(staleTime 60초) | 인계 문서 Q1. 7절 D-5 |
| 08-3 결정 11건 | 백엔드 몫. 화면은 결과만 본다 | 이번 Task와 무관 |

미결에 의존하는 구현은 없다. 검색과 프로모션이 지역 코드를 확인할지(2-1절 6행)는 화면이 어느 쪽이든 같은 상자를 쓰므로 결과에 영향이 없다.

## 7. 결정 7건의 안과 추천 (2026-09-15 전부 확정. 추천대로 가)

### D-1. 계약 단위와 이름 (2026-09-15 결정 가)

| 안 | 내용 | 장단 |
|---|---|---|
| 가 | 계약 하나(task-S9-frontend). T1부터 T10을 8절 단계로 둔다 | T1부터 T3이 전 화면 공통이라 기능키로 나눌 수 없다. 화면은 한 디자인이다. 단점은 계약이 길다 |
| 나 | 기능키별 계약(task-S9-catalog-web 같은 이름) 여섯에 공통 계약 하나 | 백엔드 묶음과 1대1이라 N9 표현이 쉽다. 단점은 공통 계약이 끝나기 전에는 아무것도 못 하고 계약 일곱을 유지한다 |

추천은 가다. N9의 뜻(하나가 검증까지 내려간 뒤 다음)은 8절의 단계 단위로 지킨다. tasks README의 기능키 여섯은 백엔드 묶음 이름이라 프론트에는 그대로 맞지 않는다.

결정: 가. join5201, 2026-09-15. 계약 하나, 이름 task-S9-frontend 그대로. T1부터 T10이 단계.

### D-2. 프론트 평가 기준 (2026-09-15 결정 가)

| 안 | 내용 | 장단 |
|---|---|---|
| 가 | 하네스 세션이 별도 이슈로 eval-criteria-code.md에 프론트 축(화면과 API 대응 준수, 상태 전이 준수, 멱등키 규칙, 오류 표시 자리, 시간 규칙, 접근성 최소)을 더한다. T10 전까지 | 2026-09-12 결정(프론트 평가는 한다, 기준을 먼저 만든다)과 맞는다. 이 Task 범위 밖 파일이라 별도 이슈 |
| 나 | 기존 축만으로 평가한다 | 축이 백엔드 구조 위주라 화면 문제를 못 잡는다 |
| 다 | 프론트 평가를 생략한다 | 2026-09-12 결정에 어긋난다 |

추천은 가다. 조건은 평가 요청 전까지이고 이 Task의 T1부터 T9는 그것을 기다리지 않는다.

결정: 가. join5201, 2026-09-15. 프론트 축은 별도 하네스 이슈로 더한다. 평가 요청 전까지.

### D-3. 평가 입력에 인계 문서를 넣는가 (2026-09-15 결정 가)

| 안 | 내용 | 장단 |
|---|---|---|
| 가 | received/HANDOFF.md와 context.md를 5절에 넣는다 | 화면이 요구대로인지 평가자가 판정할 원천이 있다. harness/out/의 계획 문서 금지(CLAUDE.md 3절)는 생성 후보와 계획을 막는 것이고 이 둘은 요구 문서다 |
| 나 | 넣지 않는다 | 평가자가 화면 요구를 명세에서 유추해야 한다. 화면 ID와 라우트가 명세에 없다 |

추천은 가다. prompt.md와 README.md는 넣지 않는다.

결정: 가. join5201, 2026-09-15. received/HANDOFF.md와 context.md를 5절에 둔다.

### D-4. E2E의 TTL 만료 시험 방법 (2026-09-15 결정 가)

| 안 | 내용 | 장단 |
|---|---|---|
| 가 | E2E용 백엔드를 o2o.booking.hold-ttl을 짧게(예: PT20S) 주고 띄운다. 키는 확인됐다(4절 예약 앱 서비스 행). 만료 스캔 주기는 기본 PT1S | 시험이 30초 안에 끝난다. 설정은 E2E 실행 환경에만 준다 |
| 나 | 기본 10분을 기다린다 | E2E 한 건이 10분이라 실용적이지 않다 |

추천은 가다. 조건은 그 설정 값을 9절 실행 환경에 적고 프로덕션 설정 파일은 고치지 않는 것이다.

결정: 가. join5201, 2026-09-15. E2E용 백엔드만 짧은 hold-ttl. 설정 파일은 그대로.

### D-5. 예약 응답의 이름 필드(Q1) (2026-09-15 결정 가)

| 안 | 내용 | 장단 |
|---|---|---|
| 가 | 이번은 인계 문서 Q1대로 G6과 G7이 줄마다 CAT-03과 CAT-08을 부른다. 백엔드에 propertyName과 roomTypeName을 더하는 것은 이슈 후보로만 적는다 | 백엔드를 안 고친다(3절). 목록 20줄에 조회 40회가 붙지만 staleTime 60초와 캐시로 같은 숙소는 한 번이다 |
| 나 | 백엔드 Booking 응답에 이름 둘을 더한 뒤 화면을 만든다 | 화면이 단순해지지만 백엔드 명세와 코드와 평가 대상이 바뀐다 |

추천은 가다. 이슈를 열지는 사용자가 정한다.

결정: 가. join5201, 2026-09-15. 이번은 추가 조회. 백엔드 이름 필드 이슈는 열지 않았다.

### D-6. 지역 코드 목록의 프론트 위치 (2026-09-15 결정 가)

| 안 | 내용 | 장단 |
|---|---|---|
| 가 | frontend/lib/regions.ts 상수 하나. 값은 RegionRegistry의 열일곱과 같고 주석에 출처를 적는다 | API가 없으니 상수뿐이다. 두 곳이 갈리면 H2 등록이 400을 맞으므로 8-1 W07이 값 일치를 시험한다 |
| 나 | 화면마다 목록을 둔다 | 세 화면이 갈린다 |

추천은 가다.

결정: 가. join5201, 2026-09-15. lib/regions.ts 상수 하나. W07이 값 일치를 지킨다.

### D-7. E2E의 테스트 DB (2026-09-15 결정 가)

| 안 | 내용 | 장단 |
|---|---|---|
| 가 | 같은 컨테이너(127.0.0.1:3307)에 o2o_web_test를 만들고 E2E용 백엔드를 SPRING_DATASOURCE_URL로 거기 붙인다. 숙소 R1 반영 때의 o2o_fix_test와 같은 방식 | 평가자 DB 넷과 기존 테스트 DB를 안 건드린다 |
| 나 | 기존 o2o_catalog_test를 쓴다 | 남은 평가 쌍이 그 DB를 본다. 섞인다 |

추천은 가다. 초기화 허용 범위는 그 DB뿐이다.

결정: 가. join5201, 2026-09-15. o2o_web_test. 초기화는 그 DB만.

## 8. 세로 진행 단계와 각 단계의 완료 조건

인계 문서 117행부터 118행의 순서 그대로다. T4와 T6과 T7은 T3 뒤 병렬이 가능하다고 적혀 있지만 이 계약은 한 턴에 한 단계 규칙대로 차례로 간다. 병렬 세션은 사용자가 열 때만이다. 단계마다 끝나면 멈추고 progress.md에 실제 시간 행을 더한다.

| 단계 | 하는 일 | 완료 조건 |
|---|---|---|
| T1 기반 | frontend/ 생성(Next.js App Router, TypeScript, Tailwind, TanStack Query v5), next.config rewrites(/api/v1을 BACKEND_URL로), 환경변수 이름 BACKEND_URL, lint와 typecheck와 build와 test 스크립트, .gitignore 줄. 설치된 판을 9절 실행 환경 표에 기입 | npm run build 실패 0. 9절 판 표가 lockfile 값으로 채워짐. 브랜치 feat/task-s9-frontend |
| T2 공통 UI | 토큰(tailwind.config), 컴포넌트 스물여섯(인계 문서 103행), layout(개발용 바 36px, 역할 탭 48px), middleware(역할과 경로 앞부분), 쿠키 읽기와 쓰기 | 8-1 W06, W07, W10 통과. typecheck 실패 0 |
| T3 API 층 | lib/api/client.ts(헤더, 멱등키 인자, Idempotency-Replayed, ApiError details와 reason, serverNow 오프셋), lib/errors.ts(placement 셋), 훅 서른셋, MSW 핸들러 | 8-1 W01부터 W05, W09, W12 통과 |
| T4 호스트 1 | H1, H2, H3 | 화면 셋이 백엔드 실물에서 동작. W08의 H2 몫 |
| T5 호스트 2 | H4, H5 | 8-1 W11 통과. INV-02 전부 실패와 BELOW_COMMITTED 숫자가 2-1절대로 |
| T6 운영자 | O1, O2 | 화면 둘이 백엔드 실물에서 동작. W08의 O2 몫 |
| T7 게스트 1 | G1, G2, G3 | 8-1 W13 통과. W08의 G1 몫 |
| T8 게스트 2 | G4, G5 | 8-1 W09(화면 몫), W14 통과 |
| T9 게스트 3 | G6, G7 | 8-1 W15 통과 |
| T10 마무리 | 오류 매핑 전수 점검(context.md 8절 코드 25개가 세 자리 중 하나에 있음), E2E 여섯(E01부터 E06), 검증 표와 회고, 평가 대상 목록 기입(5절) | E01부터 E06 통과. 결과 사본이 harness/out/task-S9-frontend-R1/에 있음. 5절 평가 대상 행 기입 뒤 fill 통과 |

### 8-1. 테스트 목록

W는 컴포넌트와 훅 테스트(MSW 목), E는 Playwright E2E(백엔드와 DB 실물)다.

| ID | 단계 | 무엇 | 통과 조건 |
|---|---|---|---|
| W01 | T3 | fetch 래퍼가 쿠키 dev_actor를 X-Dev-Actor-Id로 붙이고 public이면 생략 | 요청 헤더로 확인 |
| W02 | T3 | 응답 헤더 Idempotency-Replayed가 true면 result.replayed가 true이고 성공으로 처리 | 200과 201과 202 셋 다 |
| W03 | T3 | 오류 본문을 ApiError(status, code, message, traceId, details)로 바꾸고 details의 field로 필드 오류를 찾는다 | fieldErrors 이름을 쓰지 않는다(2-1절 1행) |
| W04 | T3 | Retry-After는 409 REQUEST_IN_PROGRESS에서만 retryAfterSec으로 읽고 5xx에는 기대하지 않는다 | 헤더 없는 503에서 초 카운트가 없음 |
| W05 | T3 | serverNow는 본문이 우선이고 없으면 Date 헤더 | 둘 다 있는 응답에서 본문 값 |
| W06 | T2 | Countdown이 expiresAt과 serverNow 차이로 세고 0에서 onZero를 1회만 부른다 | 브라우저 시계를 바꿔도 값이 같다 |
| W07 | T2 | 지역 코드 상자의 열일곱이 RegionRegistry.java의 값과 같다 | 테스트가 백엔드 파일을 읽어 대조(D-6) |
| W08 | T4, T6, T7 | 화면 검사가 서버보다 먼저. 날짜 순서, 30박, 366일, 인원 1부터 100, 최소 재고(선점 더하기 판매), 숙박 기간 둘 다 또는 둘 다 아님, 할인율 1부터 99, 최소 박수 1부터 30 | 요청이 나가지 않고 필드 아래 문구 |
| W09 | T3, T8 | 멱등키. G4 마운트 시 하나, PRICE_CHANGED 뒤 다시 확인은 새 키, 네트워크와 5xx와 REQUEST_IN_PROGRESS 재시도는 같은 키, G5 클릭마다 새 키, G7 시트 열 때 하나 | 요청 헤더의 키 값 비교 |
| W10 | T2 | middleware가 쿠키 역할과 경로 앞부분이 어긋나면 역할 첫 화면으로 보낸다 | 여섯 조합 |
| W11 | T5 | INV-02 겹침을 보내기 전에 막고 409면 INV-04 재조회와 겹친 날짜 표시. BELOW_COMMITTED 문구의 숫자가 INV-05 값 | 2-1절 2행과 5행 |
| W12 | T3 | useUpdateRate가 currency를 보내지 않고 useCreateRate는 보낸다 | 요청 본문 키 집합 |
| W13 | T7 | G3 예약 버튼 비활성 조건 넷(셋 중 하나 실패, available false, public, 조건 미완성)과 이유 문구 | 넷 각각 |
| W14 | T8 | G5 상태 분기 넷(CONFIRMED, HELD와 FAILED 3 미만, HELD와 REQUESTED, EXPIRED 두 사유) | 다섯 응답 |
| W15 | T9 | G7 취소 버튼이 CONFIRMED이고 체크인 서울 날짜가 serverNow 서울 날짜보다 뒤일 때만 활성. UTC 자정 경계 | 경계 날짜 둘 |
| E01 | T10 | 예약 성공. 호스트가 숙소와 객실과 재고와 요금을 만들고 게스트가 검색부터 G5 APPROVE로 CONFIRMED까지 | 화면 CONFIRMED, BOOK-03 CONFIRMED, DB의 soldCount 증가 |
| E02 | T10 | DECLINE 세 번으로 EXPIRED(PAYMENT_FAILED) | 시도 3, 만료 카드, 재고 반환 |
| E03 | T10 | DEFER로 처리 중 표시, 시험 코드가 INTERNAL-01을 mock_001로 불러 승인, 새로 고침으로 CONFIRMED | 처리 중 뒤 확정 |
| E04 | T10 | TTL 만료. 짧은 hold-ttl(D-4)로 HELD가 EXPIRED(TTL_EXPIRED)가 되고 Countdown 0에서 BOOK-03 1회로 화면이 바뀜 | 화면이 스스로 만료로 바꾸지 않았음을 요청 기록으로 확인 |
| E05 | T10 | 취소와 환불. CONFIRMED에서 취소, 환불 줄, 재고 반환 | DB의 soldCount 감소와 refund 행 |
| E06 | T10 | version 충돌. H2 폼을 연 뒤 시험 코드가 CAT-02로 먼저 수정, 저장 시 VERSION_CONFLICT Notice와 새로 읽기 뒤 저장 성공 | 두 번째 저장의 version이 오른 값 |

## 9. 실행과 검증 (2026-09-15 T1 기입)

| 항목 | 내용 |
|---|---|
| 작업 디렉터리 | frontend/ |
| 실행 환경 | Node 24.14.1, npm 11.11.0(2026-09-15 실측. pnpm 없음). package-lock.json 값(2026-09-15 T1 설치): next 16.3.5, react와 react-dom 19.2.8, @tanstack/react-query 5.102.8, tailwindcss와 @tailwindcss/postcss 4.3.3, msw 2.15.0, @playwright/test 1.63.0(브라우저 내려받기는 T10), vitest 5.0.1(vite 8.3.0), typescript 5.9.3, eslint 9.39.5, eslint-config-next 16.3.5, @types/node 24.13.4(create-next-app의 ^20을 실행 Node 24와 vitest 5의 peer 조건에 맞춰 올림). T2 추가: @testing-library/react 16.3.3, @testing-library/dom 10.4.2, jsdom 29.1.1. Vitest 설정은 vitest.config.mts(환경 jsdom, @ 별칭. proxy 테스트만 파일 머리 주석으로 node 환경). Tailwind는 4판이라 설정이 CSS 우선(globals.css의 @import "tailwindcss")이고 6절 스택 행의 tailwind.config theme.extend는 T2에서 @config 지시어로 잇거나 @theme으로 옮긴다. 어느 쪽인지는 T2에서 실측하고 6절에 적는다. 백엔드는 backend/README.md 3절대로 컨테이너(127.0.0.1:3307)와 JDK 21로 띄우고 dev 프로파일(기본값)과 포트 8080(설정 없음). 환경변수 이름은 BACKEND_URL(프론트), SPRING_DATASOURCE_URL과 O2O_BOOKING_HOLD_TTL(E2E용 백엔드, D-4와 D-7). 값은 여기 적지 않는다 |
| 실행할 명령 | npm run typecheck(타입 오류 0), npm run lint(오류 0), npm run build(빌드 실패 0), npm run test(W01부터 W15 통과), npm run test:e2e(E01부터 E06 통과. 백엔드와 DB가 떠 있을 때만). 스크립트 이름은 T1에서 package.json에 이 이름으로 만든다. T1 반영: typecheck는 next typegen && tsc --noEmit(라우트 타입 파일 .next/types가 추적 제외라 먼저 생성한다), lint는 eslint, test는 vitest run, test:e2e는 playwright test(설정 파일과 시험은 T10. 지금 돌리면 시험 없음으로 실패) |
| 테스트 DB | 127.0.0.1:3307 o2o_web_test(D-7). 컴포넌트와 훅 테스트는 DB 사용 없음 |
| 데이터 초기화 허용 범위 | o2o_web_test만. 다른 DB는 읽지도 않는다 |
| 빌드 출력과 로그 경로 | harness/out/task-S9-frontend-R1/ 아래. build 로그, 테스트 결과, Playwright 리포트 사본, 검증 표 |
| 계약 테스트 ID | 8-1절 W01부터 W15, E01부터 E06. 10-6 F절의 항목(연속 실행, 취소와 환불과 재고 반환, 실패와 재시도와 TTL 만료와 지연 승인, 중복 클릭과 새로고침) |
| A와 B 평가 범위 | harness/prompts/eval-criteria-code.md의 축에 D-2의 프론트 축을 더한 것 |
| 필수 검증을 실행하지 못했을 때 | progress.md에 halted 행과 미실행 사유. 5절 평가 대상 행을 채우지 않고 평가 요청으로 가지 않는다 |
| T5 실행 결과 (2026-09-16 통과) | npm run typecheck와 lint와 build와 test 넷 다 종료 코드 0. test는 19파일 135건(T5 몫 20건. InventoryCalendar 8건에 W11(일괄 겹침을 보내기 전에 막음, 409면 INV-04 재조회와 겹친 날짜 표시), 최소 재고 화면 검사와 서버 INVENTORY_BELOW_COMMITTED 뒤 다시 읽은 INV-05 숫자, INV-01 직전 날짜 값, RESOURCE_ALREADY_EXISTS 수정 전환, 일괄 검사 순서 다섯. RateCalendar 6건에 RATE-01 currency와 RATE-02 본문, VERSION_CONFLICT 새로 읽기 뒤 오른 version, 전환, 요금 범위 문구. calendar 5건, useRoomType keep 1건). build 라우트에 /host/room-types/[roomTypeId]/inventories와 rates. 실증: 백엔드 실물(작업 트리 434ab39의 bootRun, inventory와 catalog와 shared는 main과 같음)과 next start로 H4 일괄 등록 201, 겹침 차단(요청 없음), curl로 먼저 등록한 뒤 일괄 409와 재조회와 겹친 날짜, 단건 등록의 직전 값, H5 등록 201 둘과 curl로 version을 올린 뒤 409와 새로 읽기와 200, BOOK-01 둘로 선점을 만들어 최소 재고 화면 검사와 서버 409 BELOW_COMMITTED 뒤 다시 읽은 숫자(선점 2), PeriodPicker 366일 차단과 기간 적용, 탭 전환에 CAT-08 없음. 사본은 harness/out/task-S9-frontend-R1/t5/(typecheck.log, lint.log, test.log, build.log, backend-check.txt). 결정 셋. 첫째, 백엔드 INV-04와 RATE-03과 INV-02는 to를 제외하고(document/11 688행, 821행, 1032행) 화면의 PeriodPicker와 CalendarGrid는 양끝 포함이라 화면과 URL은 양끝 포함으로 두고 API에 보낼 때만 to에 하루를 더한다(lib/calendar.ts). 기간 fixture의 to도 그렇게 고쳤다. 둘째, 일괄 등록 범위는 달력에 보이는 기간 안으로 제한해 INV-04의 items와의 겹침 검사가 완전하다. 셋째, 실측에서 staleTime 60초로는 1분 뒤 탭 전환에 CAT-08이 다시 나가 useRoomType에 keep 옵션(staleTime 없음)을 더했다. 기본 기간은 서울 오늘부터 60일이고 오늘은 서버 시각 오프셋이 있으면 그것, 없으면 브라우저 시계다(달력 기본값에만 쓰고 판정에는 안 쓴다) |
| T6 실행 결과 (2026-09-16 통과) | npm run typecheck와 lint와 build와 test 넷 다 종료 코드 0. test는 22파일 154건(T6 몫 19건. PromotionForm 12건에 W08의 O2 몫(필수 문구, 할인율 1부터 99, 최소 박수 1부터 30, 캠페인 끝은 시작보다 뒤, 숙박 기간 둘 다 또는 둘 다 아님과 순서, 화면에서 숙박 시작만 넣은 제출 차단), 등록 본문(숙박 기간 없으면 두 필드 생략, enabled 생략, 지역은 선택 상자로 더하고 칩으로 뺌), 수정 본문(version과 바뀐 필드만, 안 바뀌면 안내, 숙박 기간은 둘 함께 또는 둘 다 null, 지역 코드는 순서 무시), 새로 읽기 합치기, VERSION_CONFLICT 저장 비활성. PromotionEditor 4건에 PROMO-03 진입과 PROMO-02 본문, 머리의 사용 끄기 확인 시트 뒤 enabled와 version만(P11)과 내 입력값 유지와 오른 version으로 다음 저장, 409 새로 읽기 뒤 오른 version, 404 전면 Notice. O1 page 3건에 PROMO-04 쿼리(enabled 없음과 enabled=false)와 필터 버튼의 URL과 403 Notice. next/navigation을 흉내 내는 첫 페이지 테스트다). build 라우트에 /operator/promotions와 /operator/promotions/new와 /operator/promotions/[promotionId]/edit. 실증: 백엔드 실물(T5와 같은 bootRun)과 next start로 O1 빈 목록, O2 빈 제출과 숙박 시작만 넣은 제출 차단(요청 없음), PROMO-01 201(부산과 제주, version 0), 필터 꺼짐과 사용 중의 PROMO-04 쿼리, 수정 진입 PROMO-03, 안 바꾼 저장 안내, 이름만 저장 PROMO-02 200 version 1, 머리의 사용 끄기 시트 뒤 PROMO-02 200 enabled false version 2에 이름은 서버 값 그대로(enabled와 version만 갔다)와 내 입력값 유지, curl로 version을 올린 뒤 409와 새로 읽기(최소 박수만 새 값, 이름은 내 값)와 200 version 4, 없는 id 404 전면 Notice, guest의 PROMO-04 403. 사본은 harness/out/task-S9-frontend-R1/t6/(typecheck.log, lint.log, test.log, build.log, backend-check.txt). 결정 셋. 첫째, 머리의 사용 끄기와 켜기가 훅의 setQueryData로 폼의 기준(version 포함)을 바꿔도 내 입력값은 남긴다(mergePromotionValues. 지역 코드는 값으로 비교). 그래서 끈 뒤의 저장이 오른 version으로 나간다. 둘째, 숙박 기간은 UpdatePromotionRequest의 쌍 규칙대로 바뀌면 둘을 함께 보내고 해제는 둘 다 null이다. 셋째, O1의 401과 403은 미들웨어가 다른 역할을 첫 화면으로 돌려 실물에서는 못 보고 단위 테스트로만 봤다. 브라우저 도구가 요청 본문을 안 보여 줘 PATCH 본문은 응답의 필드와 version으로 읽었고 본문 구성은 단위 테스트가 본다 |
| T7 실행 결과 (2026-09-16 통과) | npm run typecheck와 lint와 build와 test 넷 다 종료 코드 0. test는 26파일 177건(T7 몫 23건. stay 7건에 URL 쿼리 넷 읽기와 쿼리 문자열, W08의 G1 몫(날짜 순서, 30박, 인원 1부터 100, 지역). G1 page 8건에 쿼리 없으면 요청 없음, 화면 검사 뒤 URL도 요청도 없음, 검색 버튼은 URL만, 넷이 있으면 SEARCH-01과 카드 링크의 쿼리 전달, 빈 상태, 400의 field는 필드 아래와 field 없으면 Notice, 페이지. G2 page 2건에 CAT-03과 CAT-09와 객실 링크 쿼리, 404 전면. RoomTypeDetail 6건에 W13(조건 미완성, 셋 중 하나 실패, available false와 날짜별 사유, public)과 넷 동시 호출과 예상 금액 고정 문구와 이유 순서). build 라우트에 /, /properties/[propertyId], /room-types/[roomTypeId]. 실증: 백엔드 실물(T5와 같은 bootRun)과 next start로 열두 단계. 화면 검사 셋(요청 없음), SEARCH-01 200과 카드와 링크 쿼리, G2의 CAT-03과 CAT-09, G3의 CAT-08과 SEARCH-02와 SEARCH-03과 PROMO-05 연달아 넷과 오늘부터 적용되는 프로모션 10%의 할인 표와 적용 배지, public 비활성, 요금 없는 날짜의 사유와 409 둘, 인원 초과 사유와 409 둘, 지난 날짜 400(details 비어 Notice), 빈 결과, 404 전면, 쿼리 없는 G3은 CAT-08만, 예약하기가 /room-types/{id}/book에 쿼리 넷. 사본은 harness/out/task-S9-frontend-R1/t7/(typecheck.log, lint.log, test.log, build.log, backend-check.txt). 결정 넷. 첫째, 검색 버튼은 URL만 바꾸고 요청은 URL에서 나온다. 뒤로 가기와 새로 고침이 같은 결과를 내고 G2와 G3과 G4가 같은 쿼리 넷을 이어받는다(lib/stay.ts). 둘째, G3의 세 API는 checkIn과 checkOut과 guestCount만 받으므로 그 셋이 있으면 부르고 regionCode는 검색으로 돌아갈 때만 쓴다. 셋째, 지난 날짜는 W08 목록에 없어 화면 검사에 넣지 않았고 서버 400의 details가 비어 있어 화면 안 Notice(날짜를 확인하세요)로 낸다. 넷째, T2의 SearchForm에 noValidate와 서버 필드 문구 자리와 undefined 값 키를 오류로 세지 않는 제출 검사를 더했다. 실물에서 브라우저 기본 검사 풍선이 제출을 막아 문구가 안 떴고, 검사 결과의 undefined 키 때문에 정상 제출도 막혔다 |
| T8 실행 결과 (2026-09-16 통과) | npm run typecheck와 lint와 build와 test 넷 다 종료 코드 0. test는 28파일 189건(T8 몫 12건. BookingConfirm 4건에 진입 시 SEARCH-03 재조회와 그 값이 expectedTotalAmount, 요청 중 두 버튼 비활성, 201이면 onHeld, W09의 네트워크 같은 키와 REQUEST_IN_PROGRESS의 Retry-After 뒤 같은 키와 PRICE_CHANGED 뒤 새 키와 새 금액과 INVENTORY_UNAVAILABLE Notice와 IDEMPOTENCY_KEY_REUSED 새 키. PaymentScreen 8건에 paymentPhaseOf 여섯 국면, W14 다섯 응답(CONFIRMED 카드, DECLINE 셋이 클릭마다 새 키와 mockMode 본문이고 셋째 뒤 EXPIRED PAYMENT_FAILED 카드와 같은 조건으로 새 예약의 G1 쿼리, DEFER의 처리 중 카드와 버튼 잠금과 새로 고침, 남은 시간 0에서 BOOK-03 한 번과 EXPIRED TTL_EXPIRED 카드, 409 PAYMENT_ATTEMPTS_EXHAUSTED의 띠와 다 썼습니다 카드), 네트워크 같은 키, HELD 아니면 replace). build 라우트에 /room-types/[roomTypeId]/book과 /bookings/[bookingId]/pay. 실증: 백엔드 실물(T5와 같은 bootRun)과 next start로 열세 단계. G3 예약하기에서 G4 진입과 SEARCH-03 재조회, 확인 전 curl로 요금을 바꿔 BOOK-01 409 PRICE_CHANGED와 재조회와 새 금액으로 다시 확인 버튼과 원래 버튼 비활성, 다시 확인이 201과 G5, G5의 BOOK-03과 남은 시간과 0/3, APPROVE의 PAY-01 202와 CONFIRMED 카드, 예약 상세로가 /bookings/{id}(G7은 T9 몫이라 404 페이지)와 CONFIRMED 예약의 /pay 직접 열기가 replace, DECLINE 셋의 1/3과 2/3과 EXPIRED PAYMENT_FAILED 카드, 같은 조건으로 새 예약이 G1 쿼리 넷(지역은 CAT-03), DEFER의 처리 중 카드와 잠금과 새로 고침, curl로 PAYMENT_IN_PROGRESS와 BOOKING_EXPIRED와 IDEMPOTENCY_KEY_REQUIRED, 쿼리 모자란 G4의 G3 replace, 남은 시간 0 도달의 실물. 사본은 harness/out/task-S9-frontend-R1/t8/(typecheck.log, lint.log, test.log, build.log, backend-check.txt). 결정 넷. 첫째, G5의 G7 replace는 첫 BOOK-03 응답의 상태로만 판단한다. 결제 뒤의 CONFIRMED와 EXPIRED는 이 화면의 카드이고 replace하지 않는다. 둘째, 만료 카드의 같은 조건으로 새 예약은 예약 응답에 없는 regionCode를 CAT-03에서 받아 G1 쿼리 넷을 채운다. 셋째, G4의 멱등키는 마운트 시 하나를 ref에 두고 PRICE_CHANGED와 새 키 정책의 Banner 재시도 앞에서만 바꾼다. G5는 클릭마다 새 키이고 APPROVE면 본문을 비운다. 넷째, PAY-01 409 넷은 훅의 onSettled가 BOOK-03을 다시 읽으므로 화면은 짧은 띠만 띄운다 |
| T9 실행 결과 (2026-09-16 통과) | npm run typecheck와 lint와 build와 test 넷 다 종료 코드 0. test는 30파일 204건(T9 몫 15건. BookingList 6건에 필터 값 읽기(네 값 밖은 전체), 전체는 BOOK-02에 page와 size만과 줄마다 CAT-08과 CAT-03 이름과 숙박과 금액과 배지와 상세와 줄 클릭과 필터 onChange, 필터와 페이지가 status와 page, 빈 상태, 401 ACTOR_REQUIRED Notice. BookingDetail 9건에 W15 경계 날짜(서울 09-14 23:59:59와 09-15 00:00:00, UTC로는 같은 날), HELD 진입과 남은 시간과 결제하기와 새로 고침, 남은 시간 0에서 BOOK-03 한 번과 EXPIRED 읽기만, CONFIRMED의 취소 시트(사유 300자 초과 잠금, BOOK-04 키와 본문, CANCELED 카드와 환불 줄), 빈 사유는 빈 본문, 체크인이 오늘이면 비활성과 이유, 409 CANCELLATION_NOT_ALLOWED의 띠와 재조회와 다시 열면 새 키, 네트워크 같은 키, 404 전면). build 라우트에 /bookings와 /bookings/[bookingId]. 실증: 백엔드 실물(T5와 같은 bootRun)과 next start로 열두 단계. 목록 10건과 이름 조회 넷(캐시), 필터 확정과 선점(빈 상태), 네 값 밖과 page의 URL 읽기, 체크인이 오늘인 CONFIRMED의 취소 비활성(W15 실물), curl로 CANCELLATION_NOT_ALLOWED와 BOOKING_STATE_CONFLICT와 reason 301자 400(details field reason), 새 예약의 G7 HELD(남은 시간, 결제하기가 G5, 새로 고침)와 결제 뒤 G7 CONFIRMED의 취소 활성, 시트에서 사유 입력과 BOOK-04 200과 CANCELED 카드와 환불 줄과 띠, 404 전면, EXPIRED 읽기만, 옛 취소 예약의 사유와 환불, guest_002로 바꾸면 남의 예약 404와 자기 목록, public은 미들웨어가 /로, curl 401. 사본은 harness/out/task-S9-frontend-R1/t9/(typecheck.log, lint.log, test.log, build.log, backend-check.txt). 결정 셋. 첫째, 취소 가능 판단은 체크인 날짜 문자열과 serverNow의 서울 날짜 문자열을 비교한다(lib/seoul-time.ts seoulDateOf). 브라우저 시계를 쓰지 않는다. 둘째, 목록의 이름은 줄 셀 컴포넌트가 CAT-03과 CAT-08 훅을 부르고 staleTime 60초라 같은 숙소와 객실은 한 번이다(7절 D-5). 셋째, 취소 시트의 키는 여는 순간 만들고 닫으면 버린다. 409 notice 계열은 시트를 닫고 훅의 onSettled 재조회 뒤 띠만, 네트워크와 REQUEST_IN_PROGRESS는 시트를 둔 채 같은 키로 다시 시도 |
| T10 실행 결과 (2026-09-16 통과) | npm run typecheck와 lint와 build와 test 넷 다 종료 코드 0. test는 30파일 230건(T10 몫 26건. Countdown 2건에 받았을 때부터 0인 응답은 1초 뒤 onZero 1회와 그 안에 새 응답이 오면 안 부름, errors.test.ts 24건에 context.md 8절 코드 23개가 KNOWN_CODES와 집합으로 같고 코드마다 자리와 다시 시도 규칙이 셋 중 하나이고 문구가 일반 문구나 서버 문구가 아님). build 라우트 17. npm run test:e2e는 E01부터 E06 여섯 통과(두 번 연속, 32.9초와 31.7초. Playwright chromium 1.63.0, next start 3000, 백엔드는 o2o-dev 작업 트리의 bootRun을 SPRING_DATASOURCE_URL(o2o_web_test)과 O2O_BOOKING_HOLD_TTL(PT20S)로. DB o2o_web_test는 이 단계에서 새로 만듦). E01 호스트 화면 넷(CAT-01, CAT-06, INV-02, RATE-01 둘)과 게스트 G1부터 G5 승인, INV-05 soldCount 0에서 1. E02 DECLINE 셋 뒤 EXPIRED PAYMENT_FAILED와 시도 3과 heldCount 0. E03 DEFER 뒤 처리 중, INTERNAL-01(mock_001, 8080 직접) 200, 새로 고침 뒤 CONFIRMED. E04 BOOK-03 요청 기록 HELD 0ms, HELD 20023ms, EXPIRED 21047ms 뒤에 만료 카드(화면이 스스로 바꾸지 않음). E05 BOOK-04 200(키 헤더와 reason 본문)과 환불 200,000과 soldCount 1에서 0. E06 CAT-02 먼저 고침 뒤 저장 409 VERSION_CONFLICT, 새로 읽기 뒤 저장 200이고 version 0에서 2. 오류 매핑 전수 점검: context.md 8절 표의 코드 행은 23개(표의 25행은 코드 23에 응답 헤더 둘)이고 23개 전부 규칙이 있음(field 4, notice 14, banner 5). E2E가 잡은 결함 하나를 고침: Countdown이 0 도달의 재조회 응답이 아직 HELD이고 이미 0이면 곧바로 다시 읽어 되풀이됐다(첫 실행에서 1초에 열아홉 번). 받았을 때부터 0인 응답만 1초 뒤에 한 번 더 읽는다. 그 밖에 eslint가 Playwright 결과물을 제외하고, E2E 씨앗 이름에 실행 꼬리를 붙였다. 사본은 harness/out/task-S9-frontend-R1/t10/(typecheck.log, lint.log, test.log, build.log, e2e.log, e2e-results.json, playwright-report.html, error-mapping-check.txt, verification.md)과 harness/out/task-S9-frontend-R1/eval-target-files.md(148개 파일의 sha256, 기준 커밋 0a1f109). 남긴 것: 타임아웃 실물과 G1 페이지 이동 E2E 없음, E2E는 백엔드와 DB가 떠 있을 때만, 프론트 평가 축 이슈(D-2)는 아직 없음 |
| T4 실행 결과 (2026-09-15 통과) | npm run typecheck와 lint와 build와 test 넷 다 종료 코드 0. test는 16파일 115건(T4 몫 19건. PropertyForm 10건에 W08의 H2 몫(빈 폼과 글자 수와 요청 없음), 수정 본문 version과 바뀐 필드만, 새로 읽기 합치기, 서버 field 문구, VERSION_CONFLICT 자리. RoomTypePanel 3건에 MSW로 CAT-06과 CAT-08과 CAT-07과 409 뒤 새로 읽기 뒤 오른 version으로 저장. forms와 error-view 5건, 조회 재시도 규칙 1건). build 라우트에 /host/properties와 new와 [propertyId]/edit와 [propertyId]/room-types 넷. 실증: 백엔드 실물(작업 트리 434ab39의 bootRun, catalog와 shared는 main과 같음)과 next start로 H1 목록, H2 등록 201과 화면 검사, H2 수정에서 curl로 먼저 고친 뒤 409와 새로 읽기와 version 2 저장, H3 등록 201과 수정 200과 CAT-09 재조회, 없는 id의 404 전면. 사본은 harness/out/task-S9-frontend-R1/t4/(typecheck.log, lint.log, test.log, build.log, backend-check.txt). 실측에서 고친 것 하나: QueryClient 기본 재시도 셋이 404 전면을 7초 늦춰 app/providers.tsx에 4xx 무재시도 규칙(5xx와 네트워크는 한 번). 읽기 실패는 화면 안 Notice에 다시 시도 버튼이고 화면 위 띠는 쓰기 실패에 쓴다. 새로 읽기는 내가 안 건드린 필드만 새 기준을 따라 남의 수정을 되돌리지 않는다 |
| T3 실행 결과 (2026-09-15 통과) | npm run typecheck와 lint와 build와 test 넷 다 종료 코드 0. test는 12파일 96건 통과(T3 몫 58건. client.test.ts에 W01 4건, W02 200과 201과 202 셋과 헤더 없음 4건, W03 3건, W04 3건, W05 3건, 쿼리 문자열 1건. errors.test.ts에 placement 셋 28건. hooks/booking.test.tsx에 W09의 T3 몫 4건, hooks/inventory.test.tsx에 W12 2건과 재고 본문 2건, hooks/catalog.test.tsx에 조회와 캐시 4건. 나머지 38건은 T1과 T2). 훅은 32개다. 계약의 서른셋은 인계 문서 표의 INTERNAL-01 행까지 센 수이고 그 행은 훅이 없다. MSW 핸들러 32개(msw/node setupServer, 경로 */api/v1, 요청 기록으로 헤더와 본문 대조), 오류 규칙 23코드에 네트워크와 알 수 없음. 로그와 대조 사본은 harness/out/task-S9-frontend-R1/t3/(typecheck.log, lint.log, test.log, build.log, w-check.txt). 실제 백엔드는 안 띄웠다. 붙는 것은 T4부터 |
| T2 실행 결과 (2026-09-15 통과) | npm run typecheck와 lint와 build와 test 넷 다 종료 코드 0. test는 7파일 38건 통과(W06 Countdown 4건, W07 지역 코드 대조 3건, W10 여섯 조합과 통과 여섯과 proxy 배선 4건, 쿠키와 서울 시각과 날짜 도우미, T1의 rewrites 5건). build는 라우트 둘이 동적(layout이 요청 쿠키를 읽는다)이고 Proxy 줄이 있다. 실증: next start 3000에 curl로 어긋난 조합 넷이 307과 역할 첫 화면, 맞는 조합 둘이 통과(화면이 없어 404), host_001 쿠키로 / 를 받으면 행위자 선택이 host_001이고 탭이 내 숙소와 Mock 선택이 DEFER, 쿠키 없음이면 public과 내 예약 비활성. 로그와 실증 사본은 harness/out/task-S9-frontend-R1/t2/(typecheck.log, lint.log, test.log, build.log, proxy-check.txt, css-tokens-check.txt). 컴포넌트는 인계 문서 103행의 이름 스물아홉 전부(계약 8절의 스물여섯은 그 줄의 세는 방식 차이이고 목록은 같다)와 TextArea, AppShell, DevActorProvider 셋을 더했다 |
| T1 실행 결과 (2026-09-15 통과) | npm run typecheck와 lint와 build와 test 넷 다 종료 코드 0. build는 라우트 둘(/, /_not-found) 정적 생성. test는 next.config.test.ts 1파일 5건 통과(BACKEND_URL 기본값과 빈 값과 끝 슬래시, rewrites 규칙). rewrites 실증: next start 3000과 8080의 가짜 백엔드(경로와 헤더를 돌려주는 Node 서버)로 GET /api/v1/properties?regionCode=SEOUL이 200과 같은 경로와 X-Dev-Actor-Id 값으로 돌아옴. 로그와 실증 사본은 harness/out/task-S9-frontend-R1/t1/(typecheck.log, lint.log, test.log, build.log, rewrites-check.txt). 실제 백엔드는 안 띄웠다 |

## 10. 승인과 진행 (2026-09-16 T10 기입. 같은 날 T9와 T8과 T7과 T6과 T5 기입. 그 전날 T4와 T3과 T2와 T1 기입과 승인 기입)

| 항목 | 기록 |
|---|---|
| 작업 계약 승인 | 승인. join5201, 2026-09-15. 발언은 병합해라. 계약 승인하고 D-1부터 D-7 추천대로 확정해라(05:3x). 결정 7건은 전부 가. PR 149 병합은 이 기입 뒤 |
| 마지막 성공 단계 | T10 마무리(2026-09-16). 같은 날 T9 게스트 3과 T8 게스트 2와 T7 게스트 1과 T6 운영자와 T5 호스트 2. 그 전날 T4 호스트 1과 T3 API 층과 T2 공통 UI와 T1 기반과 계약 승인과 계약 초안 |
| 미해결 사항과 다음 작업 | T1부터 T10이 끝났고 평가 라운드 준비도 끝났다(2026-09-16, 이슈 186). 다음은 라운드 실행과 사용자의 최종 완료 판단. 실행 순서: 사용자가 o2o-dev에서 main을 당기고 node harness/out/task-S9-frontend-R1/verify-eval-workspace.mjs C:/Dev/potenup/99_projects/o2o-dev 가 PASS인 것을 본 뒤 Codex 새 작업 둘에 eval-request-A.md와 eval-request-B.md의 구분선 아래를 붙여 넣는다. 리포트는 harness/reviews/task-S9-frontend-R1-A.md와 -B.md로 오고 그 뒤 결정표(harness/decisions/task-S9-frontend-R1.md). 프론트 평가 축 여섯은 기준 파일 2절에 붙었고 frontend/AGENTS.md가 평가자 규칙이다. frontend/README.md의 test:e2e 줄을 고쳐 목록을 재생성했고 기준 커밋이 0a1f109에서 8530a82로 옮겨졌다(차이는 그 한 파일). E2E 환경은 껐고 평소 백엔드(o2o_catalog_test)로 되돌렸다(19:1x). 그 앞의 경과: PR 스택(T5 PR 164, T6 168, T7 171, T8 173, T9 176, T10 183)은 2026-09-16 18:0x부터 19:0x에 앞에서부터 차례로 병합됐다(164부터 176은 사용자, 183은 사용자 지시로 이 세션이 이력 확인 뒤). 병합될 때마다 다음 PR의 base를 main으로 바꿨고 병합 행은 4-1절 7단계대로 다음 브랜치에 얹었다(173과 176의 행은 183에). 결제 R1 반영(PR 175)과 08-3 결정 착지(02 v9)가 T10 뒤 main 병합으로 들어와 4절 해시 둘을 갱신했다. 결제 반영은 PaymentApplicationService와 모의 이벤트 저장소이고 API 형태는 그대로라 2-1절은 안 바뀐다. 02 v9와의 화면 문구 대조는 안 했다. 남은 평가 세 쌍의 반영이 백엔드를 바꾸면 2-1절과 4절 해시 갱신 |
| 최종 산출물과 버전 | frontend/ 추적 파일 148개. 파일마다의 sha256과 기준 커밋 0a1f109는 harness/out/task-S9-frontend-R1/eval-target-files.md. 결과 사본은 harness/out/task-S9-frontend-R1/t1부터 t10. 검증 표와 회고는 t10/verification.md. 최종 완료 판단은 아래 행(대기) |
| 실제 사용 시간 (계약 초안) | 약 30분. 2026-09-15 04:54 파일 수신부터 05:22 기록 행까지. 대조, 원문 보관, 계약 작성, fill과 g1, 이슈와 PR. 승인 기입 턴은 따로다 |
| 실제 사용 시간 | T1 기반 약 12분(2026-09-15 18:15 뼈대 생성부터 18:27 기록 행까지. PR 150 병합 10:51은 계약 몫이라 안 센다). T2 공통 UI 약 20분(18:36 PR 152 병합 직후부터 18:56 기록 행까지). T3 API 층 약 23분(19:01 PR 154 병합 직후부터 19:24 기록 행까지). T4 호스트 1 약 22분(19:50 PR 159 병합 직후부터 20:12 기록 행까지. 백엔드 기동 포함). T5 호스트 2 약 31분(2026-09-16 14:04 PR 161 병합 직후부터 14:35 기록 행까지. Docker Desktop과 백엔드 기동, 실물 대조 열두 단계 포함). T6 운영자 약 22분(14:35 T5 기록 행 직후부터 14:57 기록 행까지. 실물 대조 열두 단계 포함). T7 게스트 1 약 22분(14:57 T6 기록 행 직후부터 15:19 기록 행까지. 실물 대조 열두 단계 포함). T8 게스트 2 약 106분(15:19 T7 기록 행 직후부터 17:05 기록 행까지. 세션 문맥 초기화와 재시도 대기 포함. 실물 대조 열세 단계 포함). T9 게스트 3 약 14분(17:05 T8 기록 행 직후부터 17:19 기록 행까지. 실물 대조 열두 단계 포함). T10 마무리 약 46분(17:19 T9 기록 행 직후부터 18:05 기록 행까지. o2o_web_test 생성과 E2E 백엔드 기동, Playwright 설치, E2E 세 번 실행, 결함 수정과 재빌드 포함). 합계 318분 |
| 최종 완료 판단 | 대기. 사용자 |
