# O2O 숙박 예약 MVP — 프론트 구현 인계 (Claude Code용)

작성 2026-09-14. 화면 설계 원본: `01 화면 목록과 흐름.dc.html` ~ `05 정리.dc.html`. 백엔드: join5201/stay-booking main, API 명세 `document/11-o2o-api-spec.md`.

## 확정 스택
- Next.js App Router, TypeScript, 전부 `'use client'` 페이지 (서버 컴포넌트는 layout 껍데기만)
- TanStack Query v5, Tailwind (토큰은 tailwind.config theme.extend)
- 백엔드 호출: `next.config` rewrites `/api/v1/:path*` → `${BACKEND_URL}/api/v1/:path*`. 프록시 핸들러 없음. 헤더는 클라이언트 fetch 래퍼가 붙임
- 개발용 행위자: 쿠키 `dev_actor` ('public' | guest_001 | guest_002 | host_001 | host_002 | operator_001), `dev_mock_mode` (APPROVE | DECLINE | DEFER). Path=/, SameSite=Lax, httpOnly 아님, 30일
- 지역 코드 입력은 대문자 강제 변환, 최근값 5개 localStorage `recent_region_codes`
- 데스크톱 1200px 기준. 모바일 대응 없음

## 라우트
```
/                                             G1 검색        ?regionCode&checkIn&checkOut&guestCount&page
/properties/[propertyId]                      G2 숙소 상세    (검색 쿼리 유지)
/room-types/[roomTypeId]                      G3 객실 상세    (검색 쿼리 유지)
/room-types/[roomTypeId]/book                 G4 예약 확인    (쿼리 없으면 G3로 replace)
/bookings                                     G6 예약 목록    ?status&page
/bookings/[bookingId]                         G7 예약 상세
/bookings/[bookingId]/pay                     G5 결제        (HELD 아니면 G7로 replace)
/host/properties                              H1
/host/properties/new                          H2 등록
/host/properties/[propertyId]/edit            H2 수정
/host/properties/[propertyId]/room-types      H3             ?edit=roomTypeId | ?new=1
/host/room-types/[roomTypeId]/inventories     H4             ?from&to
/host/room-types/[roomTypeId]/rates           H5             ?from&to
/operator/promotions                          O1             ?enabled&page
/operator/promotions/new                      O2 등록
/operator/promotions/[promotionId]/edit       O2 수정
```
middleware: 쿠키 role과 경로 prefix(/host, /operator, /bookings) 불일치 → 역할 첫 화면(G1/H1/O1)으로 redirect.

## fetch 래퍼 (lib/api/client.ts)
- 매 요청 `dev_actor` 쿠키 읽어 `X-Dev-Actor-Id` 부착. 'public'이면 생략
- 쓰기 중 멱등 대상(BOOK-01, BOOK-04, PAY-01)은 `Idempotency-Key` 필수 인자
- 응답 헤더 `Idempotency-Replayed: true` → result.replayed = true (성공으로 처리, 이어서 BOOK-03 재조회)
- 오류 → `ApiError { status, code, message, fieldErrors?: {field,message}[], retryAfterSec?, traceId? }`
- 응답 `Date` 헤더로 serverNow 오프셋 저장 (Countdown용). body에 serverNow가 있으면 그것 우선

## 멱등키 규칙
- 화면 상태(useRef)에만 보관. URL·저장소 금지. `crypto.randomUUID()`
- G4 BOOK-01: 마운트 시 1개. 409 PRICE_CHANGED 뒤 "다시 확인"은 **새 키**(body 지문이 바뀜). 네트워크 오류·503·409 REQUEST_IN_PROGRESS 재시도만 **같은 키**
- G5 PAY-01: 버튼 클릭마다 새 키. 재시도 규칙 동일. body에 mockMode(쿠키값; APPROVE면 생략 가능)
- G7 BOOK-04: 취소 시트 open 시 1개, 닫으면 폐기
- 모든 쓰기 성공(또는 replayed) 뒤 BOOK-03 재조회로 현재 상태 확인. 재전송 응답은 최초 스냅샷이라 그대로 믿지 않는다

## 훅 33개 (lib/api/hooks)
| API | 훅 | 비고 |
|---|---|---|
| CAT-01/02 | useCreateProperty, useUpdateProperty | invalidate ['host','properties'], ['property',id] |
| CAT-03 | useProperty(id) | staleTime 60s. G6/G7 이름 표시에도 사용 |
| CAT-04 | usePublicProperties | 훅만, 화면 미사용 |
| CAT-05 | useMyProperties(page) | |
| CAT-06/07 | useCreateRoomType, useUpdateRoomType | invalidate ['roomTypes',propertyId], ['roomType',id] |
| CAT-08 | useRoomType(id) | staleTime 60s |
| CAT-09 | useRoomTypes(propertyId, page) | Page 응답 |
| INV-01/02/03 | useCreateInventory, useBulkCreateInventory, useUpdateInventory | invalidate ['inventories',roomTypeId] |
| INV-04 | useInventories(roomTypeId, from, to) | 최대 366일 |
| INV-05 | useInventory(roomTypeId, date) | enabled=셀 선택 시, staleTime 0 (version 확보) |
| RATE-01/02 | useCreateRate, useUpdateRate | currency 'KRW' 고정 전송 |
| RATE-03/04 | useRates(...), useRate(...) | INV와 동일 패턴 |
| PROMO-01/02 | useCreatePromotion, useUpdatePromotion | 사용 끄기 = PATCH {enabled:false, version} |
| PROMO-03/04 | usePromotion(id), usePromotions(enabled, page) | |
| PROMO-05 | useApplicablePromotions(params) | enabled=조건 완성 |
| SEARCH-01 | useSearch(params) | enabled=쿼리 넷 완성, staleTime 30s, Page 응답 |
| SEARCH-02/03 | useAvailability(params), useQuote(params) | staleTime 0. G4는 refetchOnMount 'always' → expectedTotalAmount |
| BOOK-01 | useRequestBooking(idemKey) | body 6필드 전부 필수: roomTypeId, checkIn, checkOut, guestCount, expectedTotalAmount, currency:'KRW' |
| BOOK-02 | useBookings(status, page) | status 4값 외 400 |
| BOOK-03 | useBooking(id) | staleTime 0. refetch를 Countdown.onZero·PAY-01/BOOK-04 onSettled·"새로 고침"에 연결 |
| BOOK-04 | useCancelBooking(idemKey) | reason ≤300자 |
| PAY-01 | useRequestPayment() | 202 + PaymentAttempt(REQUESTED) 하나. 예약 상태는 BOOK-03으로 |
| PAY-02, INTERNAL-01 | usePaymentAttempts(훅만), 없음 | BOOK-03 payment.attempts로 충분 |

## 상태 전이 (화면 기준)
- G4 BOOK-01 성공 → HELD → G5
- G5: PAY-01 → BOOK-03. CONFIRMED → 확정 카드 → G7. HELD+실패 n<3 → 재시도. HELD+REQUESTED(DEFER) → 수동 새로 고침, 결제 버튼 비활성. EXPIRED(TTL_EXPIRED | PAYMENT_FAILED) → 만료 카드 + "같은 조건으로 새 예약"(G1 쿼리)
- G7: HELD → Countdown + "결제하기"(G5). CONFIRMED & 체크인(서울 날짜) > serverNow(서울 날짜) → "예약 취소" 활성. CANCELED → 환불 줄. EXPIRED → 읽기만
- Countdown 0 도달 시 BOOK-03 자동 1회. 주기 폴링 없음

## 오류 표시 3자리 (lib/errors.ts → placement)
- ① 필드 아래: 400 INVALID_REQUEST(fieldErrors.field 매핑), INVALID_DATE_RANGE, 409 INVENTORY_BELOW_COMMITTED("선점 h + 판매 s = n 아래로 줄일 수 없습니다"), OCCUPANCY_EXCEEDED
- ② 화면 안 Notice: 401 ACTOR_REQUIRED("{역할} 행위자가 필요합니다" + 행위자 선택), 403 ACCESS_DENIED, 404 RESOURCE_NOT_FOUND(전면, 남의 것인지 말하지 않음), 409 VERSION_CONFLICT("다른 곳에서 먼저 수정되어 저장하지 못했습니다" + 새로 읽기=단건 GET), RESOURCE_ALREADY_EXISTS(등록→수정 폼 자동 전환 + 단건 GET), PRICE_CHANGED(SEARCH-03 재조회 → "{b}원으로 다시 확인" 새 키), INVENTORY_UNAVAILABLE/INVENTORY_NOT_CONFIGURED/RATE_NOT_CONFIGURED(G3 날짜별 사유, G4 "그사이 객실이 마감"), BOOKING_STATE_CONFLICT/BOOKING_EXPIRED/CANCELLATION_NOT_ALLOWED/PAYMENT_ATTEMPTS_EXHAUSTED/PAYMENT_IN_PROGRESS(BOOK-03 재조회 후 상태 화면 + 짧은 띠)
- ③ 화면 위 Banner: 500/503/네트워크("일시적인 오류입니다" + 다시 시도, Retry-After 초 카운트, 쓰기는 같은 키), REQUEST_IN_PROGRESS(같은 키), IDEMPOTENCY_KEY_REQUIRED/REUSED("요청 처리에 문제" — 화면 버그, 새 키). 성공 알림도 Banner(5초 자동 닫힘)
- 화면 검사가 서버보다 먼저 (날짜 순서, 범위, 최소 재고 = 선점+판매, 숙박 기간 둘 다/둘 다 아님, 지역 코드 1~32자)
- 오류에 빨간색 안 씀. Notice: 진한 회색 테두리 흰 배경. Banner: #2b7de9 배경

## 토큰 (tailwind.config) — 트립닷컴 계열 파랑 + 지브리 색 원칙
색 원칙: 순검정·순회색 없음(잉크는 따뜻한 청회색 #2f3e4e) · 바탕은 따뜻한 회백(#f7f8f6) · 강조는 파랑 #2b7de9 한 가지 · 상태색은 자연물 톤 저채도(하늘·풀·흙) · 오류도 빨강 대신 따뜻한 주황 테두리(#d9895a) · 그림자 최소
```
colors: ink {DEFAULT:#2f3e4e, 2:#4a5a6a, 3:#6f7f8d, 4:#9aa7b3}
        line {DEFAULT:#e4e9ee, strong:#cdd6df, soft:#eef1f4}
        surface {DEFAULT:#fff, 2:#fbfbf9, page:#f7f8f6, missing:#f1f3f1}
        devbar {DEFAULT:#2b7de9, 2:#5a9bf0}
        badge: held #dcecfd/#1f5fb8 · confirmed(승인·사용 중) #dfeadb/#3f6b3a · expired(처리 중·환불·꺼짐) #e9edef/#4a5a6a · canceled(실패) #f1e1dc/#8a4a3c
font: Apple SD Gothic Neo, Noto Sans KR, Malgun Gothic, system-ui · mono ui-monospace
text: 11 12 13 14(본문) 15 16 18 22(제목) 24 26 28 34 · weight 400/600 · 금액·시간 tabular-nums
radius: 3 배지 · 4 입력/버튼 · 6 카드. 카드 그림자 없음. 드롭다운 0 6px 20px rgba(0,0,0,.12)
높이: 개발용 바 36 · 역할 탭 48 · 입력 38 · 버튼 38/42. 폼 폭 680–720. 화면 좌우 24
```

## 공통 컴포넌트
DevActorBar, RoleTabs, Banner, Notice, Field/TextInput/NumberStepper/DateInput/RegionCodeInput/ChipsInput/Toggle, Button(primary|outline|ghost, md|lg, loading), SegmentedFilter, Pagination, DataTable, Skeleton, EmptyState, StatusBadge, Money, DateText(Asia/Seoul), Countdown(expiresAt, serverNow, onZero), SidePanel, ConfirmSheet, SearchForm, PriceBreakdownTable, PaymentAttemptsList, CalendarGrid(renderCell), PeriodPicker(max 366), VersionConflictNotice. props는 `05 정리.dc.html` 2절.

## 화면별 핵심 규칙 (요약)
- G1: 쿼리 넷 완성 시에만 SEARCH-01. 카드 머리 → G2, 카드 안 객실 줄 → G3(쿼리 전달)
- G3: CAT-08 + (조건 있으면) SEARCH-02/03 + PROMO-05 동시. 셋 중 하나 실패 또는 available=false 또는 public 행위자면 예약 버튼 비활성 + 이유. days의 null = "판매 안 함". "예상 금액이며 확정 금액이 아님" 고정
- G4: 진입 시 SEARCH-03 다시 받아 그 값을 expectedTotalAmount로. 요청 중 두 버튼 비활성
- G5: 남은 시간 = expiresAt − serverNow. 시도 n/3. mockMode가 APPROVE 아니면 버튼 옆 "개발용: DECLINE" 표시
- G6: 필터 고정 선택지(전체/HELD/CONFIRMED/EXPIRED/CANCELED). 줄마다 useProperty/useRoomType로 이름 (Q1)
- H2/H3/O2: version 숨김 보관, 바뀐 필드만 PATCH
- H3: 등록·수정은 옆 패널(?edit/?new). 저장 뒤 CAT-09 재조회
- H4/H5: 같은 머리·PeriodPicker·CalendarGrid, 탭 전환 시 CAT-08 재호출 없음, from/to 공유. 셀 누락→POST, 있음→GET 단건(version)→PATCH. 등록 폼은 직전 날짜 값 미리 채움. H4만 일괄 등록(INV-02, 있는 날짜는 건너뜀 → created/skipped 띠)
- O1: 켜기/끄기 토글 없음. O2 머리에 "사용 끄기"/"사용 켜기" 확인 시트
- C1: 역할 변경 → queryClient.clear() + 역할 첫 화면 replace. 같은 역할 ID 교체 → invalidateQueries(). public이면 "내 예약" 탭 비활성, Mock 선택 숨김

## Task 순서
T1 기반 → T2 공통 UI + layout + middleware → T3 훅 33개(+MSW 목) → T4 H1–H3 → T5 H4–H5 → T6 O1–O2 → T7 G1–G3 → T8 G4–G5 → T9 G6–G7 → T10 오류 매핑 + E2E 6개(예약 성공 / 거절 3회 만료 / DEFER / TTL 만료 / 취소+환불 / version 충돌). T4·T6·T7은 T3 뒤 병렬 가능.

## 미해결 (가정으로 진행)
- Q1 G6/G7 이름 추가 조회 허용 (백엔드가 Booking 응답에 이름 넣어주면 제거)
- Q2 serverNow 출처: body 없으면 Date 헤더
- Q3 요금 일괄 API 없음 → 날짜별 등록 안내
- Q4 rewrites로 쿠키가 백엔드에 넘어감. 백엔드는 헤더만 읽는다고 가정
- Q5 fieldErrors.field == body 키
- Q6 DEFER 자동 폴링 없음
- Q7 최신 Chrome, Playwright, Vercel/Node
