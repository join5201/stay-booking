# task-S9-frontend 평가 대상 파일 목록

최초 작성: 2026-09-16
최종 갱신: 2026-09-16 (평가 라운드 준비에서 재생성. 기준 커밋을 8530a82로 옮기고 frontend/README.md 한 행이 바뀜. 그 앞서 같은 날 T10 마무리에서 기입)

계약 5절 평가 대상 코드 행이 가리키는 목록이다. 기준 커밋은 8530a82(8530a828ec1af1e6dab03fc870661129c5e3e2b6. 브랜치 docs/eval-criteria-frontend-axis-186의 frontend/README.md test:e2e 줄 수정 커밋. T10의 기준 커밋 0a1f109와 견줘 frontend/ 아래 차이는 그 한 파일뿐이다). 파일 단위 버전은 sha256 앞 16자리다(CLAUDE.md 1절. 커밋 해시는 파일 단위 버전이 아니다). git ls-files frontend 결과 중 평가자 규칙 파일 frontend/AGENTS.md(2026-09-16 신설. 평가 대상이 아니다)를 뺀 148개 전부이고 추적 제외 파일(.next, node_modules, playwright-report, test-results, .env)은 없다.

| 성격 | 개수 | 읽을 범위 |
|---|---|---|
| 화면(라우트) | 19 | 전문 |
| 컴포넌트 | 55 | 전문 |
| API 층 | 10 | 전문 |
| 도우미 | 11 | 전문 |
| 테스트 | 30 | 전문 |
| 테스트 목 | 4 | 전문 |
| E2E | 8 | 전문 |
| 미들웨어 | 1 | 전문 |
| 설정 | 10 | 전문. package-lock.json은 판 대조만 |

| 경로 | 성격 | 줄 수 | sha256 |
|---|---|---|---|
| frontend/.gitignore | 설정 | 46 | sha256:2dc9e1d911366890 |
| frontend/README.md | 설정 | 39 | sha256:3ca86064f7a93ca6 |
| frontend/app/bookings/[bookingId]/page.tsx | 화면(라우트) | 12 | sha256:b75ed2ed8babd0da |
| frontend/app/bookings/[bookingId]/pay/page.tsx | 화면(라우트) | 15 | sha256:a4e6e38c942852c8 |
| frontend/app/bookings/page.tsx | 화면(라우트) | 24 | sha256:b33e8968135fa685 |
| frontend/app/globals.css | 화면(라우트) | 74 | sha256:fe9d105b6770d7f8 |
| frontend/app/host/properties/[propertyId]/edit/page.tsx | 화면(라우트) | 76 | sha256:4b83c1ed2c085c9b |
| frontend/app/host/properties/[propertyId]/room-types/page.tsx | 화면(라우트) | 91 | sha256:845f1ccff76b39ab |
| frontend/app/host/properties/new/page.tsx | 화면(라우트) | 47 | sha256:14d484adcc7af921 |
| frontend/app/host/properties/page.tsx | 화면(라우트) | 73 | sha256:f8ddf81bcaa61c07 |
| frontend/app/host/room-types/[roomTypeId]/inventories/page.tsx | 화면(라우트) | 18 | sha256:9275701328d75c46 |
| frontend/app/host/room-types/[roomTypeId]/rates/page.tsx | 화면(라우트) | 18 | sha256:26bdbef487e443dd |
| frontend/app/layout.tsx | 화면(라우트) | 30 | sha256:4650017c2869aee6 |
| frontend/app/operator/promotions/[promotionId]/edit/page.tsx | 화면(라우트) | 12 | sha256:0b1469809a09d580 |
| frontend/app/operator/promotions/new/page.tsx | 화면(라우트) | 45 | sha256:c069512fc8a0969d |
| frontend/app/operator/promotions/page.test.tsx | 테스트 | 64 | sha256:b2e1cbfdc051f30a |
| frontend/app/operator/promotions/page.tsx | 화면(라우트) | 101 | sha256:d6bfbe37eb273da8 |
| frontend/app/page.test.tsx | 테스트 | 105 | sha256:8c02b226f6a2670a |
| frontend/app/page.tsx | 화면(라우트) | 67 | sha256:772fea02a825608a |
| frontend/app/properties/[propertyId]/page.test.tsx | 테스트 | 55 | sha256:df6a3ffa9c96f4d3 |
| frontend/app/properties/[propertyId]/page.tsx | 화면(라우트) | 87 | sha256:e6d886beb8f3b436 |
| frontend/app/providers.test.ts | 테스트 | 15 | sha256:4607d10bd41828ec |
| frontend/app/providers.tsx | 화면(라우트) | 18 | sha256:a8d463cef673e284 |
| frontend/app/room-types/[roomTypeId]/book/page.tsx | 화면(라우트) | 25 | sha256:dbf705cfa8e0baea |
| frontend/app/room-types/[roomTypeId]/page.tsx | 화면(라우트) | 14 | sha256:0cb7123a2bc6e1e1 |
| frontend/components/AppShell.tsx | 컴포넌트 | 28 | sha256:344860488389abc7 |
| frontend/components/Banner.tsx | 컴포넌트 | 92 | sha256:5e8c062ab71e8215 |
| frontend/components/Button.tsx | 컴포넌트 | 40 | sha256:f3555159358a6192 |
| frontend/components/CalendarGrid.tsx | 컴포넌트 | 78 | sha256:483ffea2a7080bcd |
| frontend/components/ChipsInput.tsx | 컴포넌트 | 42 | sha256:c608154b3015bb3c |
| frontend/components/ConfirmSheet.tsx | 컴포넌트 | 45 | sha256:7abf87bf1f776b61 |
| frontend/components/Countdown.test.tsx | 테스트 | 89 | sha256:b9955ce4e2d5c415 |
| frontend/components/Countdown.tsx | 컴포넌트 | 61 | sha256:bf2d63f30bd66c75 |
| frontend/components/DataTable.tsx | 컴포넌트 | 58 | sha256:9a553740c8153fdf |
| frontend/components/DateInput.tsx | 컴포넌트 | 32 | sha256:43ca7df7045c49f5 |
| frontend/components/DateText.tsx | 컴포넌트 | 20 | sha256:d23d51a1107d4af9 |
| frontend/components/DevActorBar.tsx | 컴포넌트 | 60 | sha256:2416e5c095720ad9 |
| frontend/components/DevActorProvider.tsx | 컴포넌트 | 45 | sha256:55a1ef403606bf0d |
| frontend/components/EmptyState.tsx | 컴포넌트 | 19 | sha256:94def934c9ae8e5a |
| frontend/components/Field.tsx | 컴포넌트 | 37 | sha256:6e40c5791c9ef7c1 |
| frontend/components/Money.tsx | 컴포넌트 | 16 | sha256:7d338d7406f23a39 |
| frontend/components/Notice.tsx | 컴포넌트 | 23 | sha256:651f7fe815cc78c7 |
| frontend/components/NumberStepper.tsx | 컴포넌트 | 54 | sha256:f491856056d7cd88 |
| frontend/components/Pagination.tsx | 컴포넌트 | 29 | sha256:34e0f52ed2452d5b |
| frontend/components/PaymentAttemptsList.tsx | 컴포넌트 | 55 | sha256:d72d8769e88e9b93 |
| frontend/components/PeriodPicker.tsx | 컴포넌트 | 54 | sha256:d85126fffe8a2ec4 |
| frontend/components/PriceBreakdownTable.tsx | 컴포넌트 | 55 | sha256:db020ff72a20b801 |
| frontend/components/QueryErrorNotice.tsx | 컴포넌트 | 39 | sha256:4468360316f1470d |
| frontend/components/RegionCodeInput.tsx | 컴포넌트 | 39 | sha256:b64471b3ef21119b |
| frontend/components/RoleTabs.tsx | 컴포넌트 | 60 | sha256:60161424c2341dd6 |
| frontend/components/SearchForm.tsx | 컴포넌트 | 70 | sha256:dd2d57441aeb5850 |
| frontend/components/SegmentedFilter.tsx | 컴포넌트 | 36 | sha256:5dcac090fb8b6768 |
| frontend/components/SidePanel.tsx | 컴포넌트 | 42 | sha256:a2cfc2b49eee836f |
| frontend/components/Skeleton.tsx | 컴포넌트 | 16 | sha256:cac5baa2c911cff4 |
| frontend/components/StatusBadge.tsx | 컴포넌트 | 45 | sha256:357810bc573e6142 |
| frontend/components/TextInput.tsx | 컴포넌트 | 39 | sha256:1b93a99c143e16c9 |
| frontend/components/Toggle.tsx | 컴포넌트 | 30 | sha256:bd8d4a61857c0754 |
| frontend/components/VersionConflictNotice.tsx | 컴포넌트 | 27 | sha256:7cefeb7421156f1e |
| frontend/components/guest/ApplicablePromotionList.tsx | 컴포넌트 | 31 | sha256:50a13db752c8b1a4 |
| frontend/components/guest/AvailabilityTable.tsx | 컴포넌트 | 49 | sha256:cb9bcb81a764cbde |
| frontend/components/guest/BookingConfirm.test.tsx | 테스트 | 140 | sha256:57ed70a60248ceea |
| frontend/components/guest/BookingConfirm.tsx | 컴포넌트 | 122 | sha256:4018e3b3746d8315 |
| frontend/components/guest/BookingDetail.test.tsx | 테스트 | 212 | sha256:a4057b4df0de9055 |
| frontend/components/guest/BookingDetail.tsx | 컴포넌트 | 232 | sha256:954666ce2941b234 |
| frontend/components/guest/BookingList.test.tsx | 테스트 | 95 | sha256:f76062cae45919ae |
| frontend/components/guest/BookingList.tsx | 컴포넌트 | 94 | sha256:022f07d23b96cafb |
| frontend/components/guest/PaymentScreen.test.tsx | 테스트 | 215 | sha256:54042ffa138bc6f1 |
| frontend/components/guest/PaymentScreen.tsx | 컴포넌트 | 162 | sha256:f75d006c646320e8 |
| frontend/components/guest/PropertyCard.tsx | 컴포넌트 | 48 | sha256:e3815e216f0d7e20 |
| frontend/components/guest/RoomTypeDetail.test.tsx | 테스트 | 133 | sha256:a5b179f916c8887a |
| frontend/components/guest/RoomTypeDetail.tsx | 컴포넌트 | 138 | sha256:674c48e99a677cc6 |
| frontend/components/guest/StayConditions.tsx | 컴포넌트 | 33 | sha256:c14047bff761aa25 |
| frontend/components/guest/booking-text.ts | 컴포넌트 | 33 | sha256:6a022e4741ccf7c2 |
| frontend/components/host/InventoryBulkPanel.tsx | 컴포넌트 | 149 | sha256:755f289bf6b5a056 |
| frontend/components/host/InventoryCalendar.test.tsx | 테스트 | 180 | sha256:430c313cce3ff742 |
| frontend/components/host/InventoryCalendar.tsx | 컴포넌트 | 78 | sha256:32faad9efce18bfb |
| frontend/components/host/InventoryCellPanel.tsx | 컴포넌트 | 273 | sha256:88fc7cae2f00d8f8 |
| frontend/components/host/PropertyForm.test.tsx | 테스트 | 114 | sha256:74256bb828ec8a65 |
| frontend/components/host/PropertyForm.tsx | 컴포넌트 | 136 | sha256:15bceb80bd8c720e |
| frontend/components/host/RateCalendar.test.tsx | 테스트 | 141 | sha256:3ca7b790e6beda8b |
| frontend/components/host/RateCalendar.tsx | 컴포넌트 | 50 | sha256:d07a6b0a70962ec1 |
| frontend/components/host/RateCellPanel.tsx | 컴포넌트 | 229 | sha256:db167465713e598c |
| frontend/components/host/RoomTypeCalendarShell.tsx | 컴포넌트 | 86 | sha256:0e5b0ce7bdbb35f0 |
| frontend/components/host/RoomTypeForm.tsx | 컴포넌트 | 128 | sha256:d99a1acca1adbe35 |
| frontend/components/host/RoomTypePanel.test.tsx | 테스트 | 102 | sha256:9616e8fdc41725c6 |
| frontend/components/host/RoomTypePanel.tsx | 컴포넌트 | 100 | sha256:0c1f13612b1e3724 |
| frontend/components/host/useCalendarPeriod.ts | 컴포넌트 | 25 | sha256:e4aec0ec7db6faf5 |
| frontend/components/operator/PromotionEditor.test.tsx | 테스트 | 112 | sha256:9581197c6bf163ad |
| frontend/components/operator/PromotionEditor.tsx | 컴포넌트 | 115 | sha256:f678a5ab67d6b0d8 |
| frontend/components/operator/PromotionForm.test.tsx | 테스트 | 131 | sha256:9756716e65aed2b7 |
| frontend/components/operator/PromotionForm.tsx | 컴포넌트 | 247 | sha256:2fa04823eef4e753 |
| frontend/components/useWriteError.ts | 컴포넌트 | 55 | sha256:74c487557f58495d |
| frontend/e2e/e01-booking-success.spec.ts | E2E | 105 | sha256:6307e2aa9d31f0c2 |
| frontend/e2e/e02-decline-expired.spec.ts | E2E | 36 | sha256:ee8b205709b778ed |
| frontend/e2e/e03-defer-internal-event.spec.ts | E2E | 43 | sha256:7cd876bf29cdf310 |
| frontend/e2e/e04-ttl-expired.spec.ts | E2E | 50 | sha256:fef9bfbb28640f64 |
| frontend/e2e/e05-cancel-refund.spec.ts | E2E | 51 | sha256:ef9a1331c9bb8b13 |
| frontend/e2e/e06-version-conflict.spec.ts | E2E | 60 | sha256:dabb5a82d80296bd |
| frontend/e2e/support.ts | E2E | 177 | sha256:603727d111f0f531 |
| frontend/env.example | 설정 | 13 | sha256:8e41de9a622e17a9 |
| frontend/eslint.config.mjs | 설정 | 22 | sha256:eb23b4194d91fc7f |
| frontend/lib/api/client.test.ts | 테스트 | 136 | sha256:cd2cdc584591f58c |
| frontend/lib/api/client.ts | API 층 | 155 | sha256:b479596394235378 |
| frontend/lib/api/hooks/booking.test.tsx | 테스트 | 96 | sha256:333a3a2c0c74c74d |
| frontend/lib/api/hooks/booking.ts | API 층 | 79 | sha256:5ef931795f4814e3 |
| frontend/lib/api/hooks/catalog.test.tsx | 테스트 | 97 | sha256:78eb39b939f0aad8 |
| frontend/lib/api/hooks/catalog.ts | API 층 | 104 | sha256:257d6ea9fc1b359c |
| frontend/lib/api/hooks/index.ts | API 층 | 7 | sha256:b0dae209aee4bb68 |
| frontend/lib/api/hooks/inventory.test.tsx | 테스트 | 65 | sha256:996666316d7513d8 |
| frontend/lib/api/hooks/inventory.ts | API 층 | 102 | sha256:08d9a8de50f04ad9 |
| frontend/lib/api/hooks/promotion.ts | API 층 | 64 | sha256:1a6be55fcb69b2e5 |
| frontend/lib/api/hooks/search.ts | API 층 | 51 | sha256:7c11c277197496b4 |
| frontend/lib/api/keys.ts | API 층 | 38 | sha256:9cc84d40cae55d17 |
| frontend/lib/api/mocks/fixtures.ts | 테스트 목 | 178 | sha256:03fb05a245202422 |
| frontend/lib/api/mocks/handlers.ts | 테스트 목 | 100 | sha256:89ca5c9678243fc2 |
| frontend/lib/api/mocks/server.ts | 테스트 목 | 6 | sha256:ca8f02e3399133af |
| frontend/lib/api/mocks/test-utils.tsx | 테스트 목 | 44 | sha256:533bf1444f631995 |
| frontend/lib/api/server-clock.ts | API 층 | 20 | sha256:dab566b0c092ca7e |
| frontend/lib/api/types.ts | API 층 | 352 | sha256:5df5a201f124567a |
| frontend/lib/calendar.test.ts | 테스트 | 36 | sha256:fb9dd89242e2f529 |
| frontend/lib/calendar.ts | 도우미 | 53 | sha256:d49bc999b02de50c |
| frontend/lib/cookies.test.ts | 테스트 | 31 | sha256:03a43095f5d61ce3 |
| frontend/lib/cookies.ts | 도우미 | 26 | sha256:92903cc6e62a1dac |
| frontend/lib/dates.ts | 도우미 | 46 | sha256:385353c6f2c50b81 |
| frontend/lib/dev-actor.ts | 도우미 | 42 | sha256:27f892bcf78ecea5 |
| frontend/lib/error-view.ts | 도우미 | 23 | sha256:852645c365386427 |
| frontend/lib/errors.test.ts | 테스트 | 126 | sha256:8b8aa8e8ee772eeb |
| frontend/lib/errors.ts | 도우미 | 96 | sha256:db9a7f4af65716fd |
| frontend/lib/forms.test.ts | 테스트 | 49 | sha256:b883dcf393a3dbb5 |
| frontend/lib/forms.ts | 도우미 | 43 | sha256:6830eed3ea3f573a |
| frontend/lib/regions.test.ts | 테스트 | 33 | sha256:9a63ae630551f0d0 |
| frontend/lib/regions.ts | 도우미 | 25 | sha256:7707c486ba5b107e |
| frontend/lib/role-route.test.ts | 테스트 | 40 | sha256:7581561696f8e726 |
| frontend/lib/role-route.ts | 도우미 | 26 | sha256:937ad674eb414e38 |
| frontend/lib/seoul-time.test.ts | 테스트 | 38 | sha256:7d0a6c60ba6dca73 |
| frontend/lib/seoul-time.ts | 도우미 | 47 | sha256:edbb38b400059970 |
| frontend/lib/stay.test.ts | 테스트 | 54 | sha256:79ad66debb332075 |
| frontend/lib/stay.ts | 도우미 | 89 | sha256:0b2d41f930f0fe1f |
| frontend/next.config.test.ts | 테스트 | 30 | sha256:d297455ada7604e5 |
| frontend/next.config.ts | 설정 | 25 | sha256:dd52b734ff4b28d1 |
| frontend/package-lock.json | 설정 | 9193 | sha256:c6e6f75f69f4b34a |
| frontend/package.json | 설정 | 37 | sha256:5fdacb2a9249e50e |
| frontend/playwright.config.ts | E2E | 28 | sha256:7f5659e6d854bcf9 |
| frontend/postcss.config.mjs | 설정 | 8 | sha256:dfac7ac2d86d326a |
| frontend/proxy.test.ts | 테스트 | 36 | sha256:34503ed9f9e55b1c |
| frontend/proxy.ts | 미들웨어 | 16 | sha256:acc10a06ea53462a |
| frontend/tsconfig.json | 설정 | 35 | sha256:be18523b23b78b6e |
| frontend/vitest.config.mts | 설정 | 14 | sha256:d0197bc3656f5b62 |
