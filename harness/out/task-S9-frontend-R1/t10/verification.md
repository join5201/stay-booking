# task-S9-frontend T10 검증 표와 회고

최초 작성: 2026-09-16
최종 갱신: 2026-09-16 (T10 마무리에서 작성)

계약 9절의 검증 표다. 8-1절 W01부터 W15와 E01부터 E06, 그리고 10-6 F절 항목을 어디서 어떻게 확인했는지 적는다. 코드 기준은 커밋 0a1f109(harness/out/task-S9-frontend-R1/eval-target-files.md).

## 1. 검사 넷 (T10 시점)

| 검사 | 결과 | 사본 |
|---|---|---|
| npm run typecheck | 종료 코드 0 | t10/typecheck.log |
| npm run lint | 종료 코드 0. Playwright 결과물(playwright-report, test-results)을 eslint 제외에 넣은 뒤 | t10/lint.log |
| npm test | 30파일 230건 통과(T10 몫 26건. Countdown 2건, errors.test.ts 전수 점검 24건) | t10/test.log |
| npm run build | 종료 코드 0. 라우트 17 | t10/build.log |
| npm run test:e2e | 6건 통과(두 번 연속. 32.9초와 31.7초) | t10/e2e.log, t10/e2e-results.json(주석에 E04의 요청 기록), t10/playwright-report.html |

## 2. W01부터 W15 (컴포넌트와 훅 테스트, MSW 목)

| ID | 단계 | 테스트 파일 | 결과 |
|---|---|---|---|
| W01 | T3 | lib/api/client.test.ts | 통과. 쿠키 dev_actor가 X-Dev-Actor-Id, public이면 생략 |
| W02 | T3 | lib/api/client.test.ts | 통과. Idempotency-Replayed true가 result.replayed이고 200과 201과 202 성공 |
| W03 | T3 | lib/api/client.test.ts, lib/errors.test.ts | 통과. ApiError(status, code, message, traceId, details)와 details의 field로 필드 오류 |
| W04 | T3 | lib/api/client.test.ts | 통과. Retry-After는 409 REQUEST_IN_PROGRESS에서만 retryAfterSec |
| W05 | T3 | lib/api/client.test.ts | 통과. serverNow는 본문 우선, 없으면 Date 헤더 |
| W06 | T2 | components/Countdown.test.tsx | 통과. 차이로 세고 0에서 onZero 1회. T10 추가: 받았을 때부터 0인 응답은 1초 뒤 1회, 그 안에 새 응답이 오면 안 부름 |
| W07 | T2 | lib/regions.test.ts | 통과. 열일곱이 RegionRegistry.java와 같음 |
| W08 | T4, T6, T7 | components/host/PropertyForm.test.tsx, components/operator/PromotionForm.test.tsx, lib/stay.test.ts, app/page.test.tsx, components/host/InventoryCalendar.test.tsx | 통과. 화면 검사가 서버보다 먼저이고 요청이 안 나감 |
| W09 | T3, T8, T9 | lib/api/hooks/booking.test.tsx, components/guest/BookingConfirm.test.tsx, components/guest/PaymentScreen.test.tsx, components/guest/BookingDetail.test.tsx | 통과. G4 마운트 시 하나, PRICE_CHANGED 뒤 새 키, 네트워크와 5xx와 REQUEST_IN_PROGRESS는 같은 키, G5 클릭마다 새 키, G7 시트 열 때 하나 |
| W10 | T2 | lib/role-route.test.ts, proxy.test.ts | 통과. 어긋난 여섯 조합이 역할 첫 화면 |
| W11 | T5 | components/host/InventoryCalendar.test.tsx | 통과. 겹침을 보내기 전에 막고 409면 INV-04 재조회와 겹친 날짜, BELOW_COMMITTED 숫자가 INV-05 값 |
| W12 | T3 | lib/api/hooks/inventory.test.tsx | 통과. useUpdateRate는 currency 없음, useCreateRate는 있음 |
| W13 | T7 | components/guest/RoomTypeDetail.test.tsx | 통과. 비활성 조건 넷과 이유 문구 |
| W14 | T8 | components/guest/PaymentScreen.test.tsx | 통과. 상태 분기 넷, 다섯 응답. TTL 시험은 T10에서 첫 응답을 남은 시간 1초로 바꿔 0 도달 경로를 탄다 |
| W15 | T9 | components/guest/BookingDetail.test.tsx | 통과. 서울 09-14 23:59:59와 09-15 00:00:00 경계 |

## 3. E01부터 E06 (Playwright, 백엔드 실물과 o2o_web_test)

환경: 프론트 next build 뒤 next start 3000(rewrites 기본 8080). 백엔드는 o2o-dev 작업 트리의 gradlew bootRun을 SPRING_DATASOURCE_URL(o2o_web_test)과 O2O_BOOKING_HOLD_TTL(PT20S)로 띄움(계약 7절 D-4, D-7). DB는 o2o-catalog-mysql 컨테이너(3307)에 o2o_web_test를 새로 만들고 앱 사용자에게 권한을 줌(자격 증명은 컨테이너 안에서만 풀림). Playwright chromium 1.63.0.

| ID | 시험 파일 | 통과 조건(계약) | 결과와 근거 |
|---|---|---|---|
| E01 | e2e/e01-booking-success.spec.ts | 화면 CONFIRMED, BOOK-03 CONFIRMED, DB의 soldCount 증가 | 통과 3.7초. 호스트가 H2와 H3와 H4와 H5를 화면으로(CAT-01 201, CAT-06 201, INV-02 201, RATE-01 201 둘), 게스트가 G1 검색 카드에서 G3, G4, G5 승인. 확정 카드와 1/3. BOOK-03 CONFIRMED와 attemptCount 1. INV-05 두 밤 다 soldCount 0에서 1, availableCount 3에서 2 |
| E02 | e2e/e02-decline-expired.spec.ts | 시도 3, 만료 카드, 재고 반환 | 통과 1.1초. 선점 직후 heldCount 1. DECLINE 셋 뒤 만료 카드와 PAYMENT_FAILED 문구, 3/3, 결제 버튼 없음. BOOK-03 EXPIRED PAYMENT_FAILED와 FAILED 셋. INV-05 두 밤 heldCount 0, availableCount 3 |
| E03 | e2e/e03-defer-internal-event.spec.ts | 처리 중 뒤 확정 | 통과 0.9초. DEFER 뒤 처리 중 카드와 잠긴 다시 결제하기. BOOK-03의 시도가 REQUESTED. INTERNAL-01을 mock_001로 8080에 직접(200). 새로 고침 뒤 확정 카드. BOOK-03 CONFIRMED와 시도 APPROVED, soldCount 1 |
| E04 | e2e/e04-ttl-expired.spec.ts | 화면이 스스로 만료로 바꾸지 않았음을 요청 기록으로 확인 | 통과 21.7초. BOOK-03 요청 기록(진입 뒤 밀리초): HELD 0, HELD 20023, EXPIRED 21047. 0 도달 전 읽기 없음, 0 도달의 읽기가 아직 HELD(서버 만료 스캔 주기), 1초 뒤 한 번 더가 EXPIRED, 그 뒤에 만료 카드(TTL_EXPIRED 문구, 새 예약 버튼). BOOK-03 EXPIRED TTL_EXPIRED와 attemptCount 0. INV-05 heldCount 0 |
| E05 | e2e/e05-cancel-refund.spec.ts | DB의 soldCount 감소와 refund 행 | 통과 1.2초. 승인 뒤 soldCount 1. G7 취소 시트에 사유 9자, BOOK-04 200(요청 헤더에 Idempotency-Key, 본문 reason). 띠와 취소 카드와 사유와 환불 줄, 취소 버튼 없음. BOOK-03 CANCELED와 refund.amount 200,000. INV-05 두 밤 soldCount 0 |
| E06 | e2e/e06-version-conflict.spec.ts | 두 번째 저장의 version이 오른 값 | 통과 0.6초. H2 수정을 연 채 CAT-02로 주소를 먼저 고침(version 0에서 1). 이름을 바꿔 저장이 409 VERSION_CONFLICT와 저장하지 못했습니다 Notice, 저장 버튼 잠김. 새로 읽기(CAT-03 200) 뒤 저장이 200이고 본문 version 1, 목록에 새 이름과 띠. CAT-03 version 2 |

## 4. 10-6 F절 항목 대조

| F절 항목 | 어디서 |
|---|---|
| Next.js에서 Spring API를 호출하고 MySQL에 저장된 결과까지 | E01부터 E06 전부. DB 값은 INV-05와 BOOK-03으로 읽었다(DB 직접 접속 없이. D-7의 초기화 범위 안) |
| API 주소, 프록시, 식별 정보 전달 | next.config.ts rewrites(T1 실증)와 E2E의 rewrites 경유 호출. 쿠키 dev_actor가 헤더로(W01, E01의 호스트와 게스트 전환) |
| 필드, 오류 코드, 날짜, 시간대, 금액이 계약과 일치 | 오류 코드 전수 점검(t10/error-mapping-check.txt). 서울 날짜 경계(W15). 금액(E05 환불 200,000, E01 요금 100,000 둘) |
| 검색, 객실 선택, 예약, Mock 결제, 확정, 예약 조회를 브라우저에서 연속 실행 | E01(검색부터 확정), E05(확정 뒤 예약 상세) |
| 예약 취소, Mock 환불, 재고 반환을 브라우저와 DB에서 함께 | E05 |
| 결제 실패와 재시도, TTL 만료, 지연 승인 시 화면과 서버 상태 일치 | E02(실패 셋), E04(TTL), E03(지연 승인). 각각 화면 카드 뒤 BOOK-03으로 대조 |
| 타임아웃, 요청 재전송, 중복 클릭과 새로고침 뒤 중복 없음 | 단위 테스트 몫(W09의 같은 키 재시도, BookingConfirm의 요청 중 버튼 비활성, PaymentScreen의 클릭마다 새 키와 잠금). E2E에서는 E03의 잠긴 버튼과 E05의 키 헤더까지. 타임아웃 실물은 안 만들었다(아래 남긴 것) |
| 핵심 흐름의 브라우저 자동 테스트 | 이 여섯. npm run test:e2e |
| 테스트용 데이터와 Mock 실패 조건 공유 | e2e/support.ts의 seedStay와 setActor(mockMode APPROVE, DECLINE, DEFER). 씨앗 이름에 실행 시각 꼬리를 붙여 같은 DB에 되풀이 실행해도 화면의 이름 찾기가 하나만 잡는다 |

## 5. E2E가 잡은 것

| 무엇 | 원인 | 고침 |
|---|---|---|
| 남은 시간 0 도달 뒤 BOOK-03이 1초에 열아홉 번(첫 실행의 E04) | Countdown이 새 응답마다 통째로 다시 만들어지는데, 0 도달의 재조회 응답이 아직 HELD이고 이미 0이면 곧바로 onZero가 다시 불려 되풀이됐다. 서버의 만료 처리는 스캔 주기가 있어 0 직후 응답이 HELD일 수 있다 | components/Countdown.tsx. 받았을 때부터 0인 응답만 ZERO_RETRY_MS(1초) 뒤에 한 번 부르고 그 안에 새 응답이 오면 취소. 단위 테스트 둘 추가. 수정 뒤 기록은 HELD 0, HELD 20023, EXPIRED 21047 |
| 이름 찾기가 둘을 잡음(E06 두 번째 실행) | 씨앗 이름이 실행마다 같아 앞 실행이 남긴 숙소와 겹쳤다 | seedStay 이름에 실행 시각 꼬리 |
| lint 3054건 | Playwright HTML 리포트의 번들 스크립트가 eslint 대상에 들어갔다 | eslint 제외 둘 |

## 6. 남긴 것

- 타임아웃 실물(10-6 F절의 타임아웃)은 E2E로 만들지 않았다. 네트워크 오류의 같은 키 재시도는 단위 테스트(W09)로 갈음.
- G1 검색 결과의 페이지 이동은 E2E에 없다. E01은 첫 쪽의 카드를 찾는다. o2o_web_test에 BUSAN 숙소가 한 쪽을 넘게 쌓이면 E01의 카드 찾기가 실패할 수 있다. 그때는 DB를 비우거나(D-7의 초기화 허용 범위) 시험에 페이지 이동을 더한다.
- E2E는 백엔드와 DB가 떠 있을 때만 돈다. CI 없음(계약 3절 범위 밖).
- 프론트 평가 축 이슈(계약 7절 D-2)는 하네스 세션 몫이고 아직 없다.

## 7. 회고

| 무엇 | 배운 것 |
|---|---|
| 단위 테스트가 다 통과해도 되풀이 요청은 못 봤다 | 목 응답은 시험이 정한 순서로 오지만 실물은 스캔 주기 때문에 0 직후에 HELD를 준다. 시간이 끼는 동작은 실물에서 요청 기록을 세야 한다. E04의 요청 기록 주석을 리포트에 남긴 이유다 |
| 화면 문구는 낱말이 겹친다 | 만료 카드 제목과 사유 문구가 같은 낱말을 품어 getByText가 둘을 잡았다. 제목은 exact, 사유는 전문으로 찾는다 |
| 씨앗은 되풀이 실행을 전제해야 한다 | 같은 DB에 두 번째 돌리면 앞 실행의 자료가 남는다. 이름에 실행 꼬리를 붙이고 판정은 이름이 아니라 id로 한 API 값으로 한다 |
| 결과물 폴더는 도구마다 제외 목록에 넣어야 한다 | .gitignore에만 넣고 eslint에는 안 넣어 3054건이 났다 |
