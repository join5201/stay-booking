# harness/out/mvp-parallel-2026-09-11/

최초 작성: 2026-09-11
최종 갱신: 2026-09-11
용도: 백엔드 MVP를 오늘 안에 코드와 테스트와 검증 표까지 내리기 위해 세션 셋을 병렬로 돌릴 때 각 세션의 첫 프롬프트

왜 이 폴더가 필요한가: 세션마다 범위와 파일 소유와 테스트 DB를 말로 설명하면 세션마다 흔들리고, 두 세션이 같은 파일을 고쳐 병합에서 깨진다. 프롬프트 셋이 그 경계를 같은 문장으로 고정한다. 양식은 harness/prompts/dev-ptcf-prompt.v3.md 2절의 PTCF 골격이다.

## 1. 전제. 2026-09-11 사용자 결정 셋

| 번호 | 지금 규칙 | 오늘 | 근거 |
|---|---|---|---|
| 결정 1 | 한 턴에 한 단계. 묶음당 정지 아홉 번 | 묶음당 정지 둘. 계약 승인(1단계)과 검증 표(9단계). 계약이 이미 승인된 묶음은 9단계 하나 | progress.md 실측. 코딩은 묶음당 72분과 88분, 대기가 이틀 |
| 결정 2 | N9. 한 묶음이 검증까지 내려가기 전에 다음 묶음 금지 | 세션 단위로 읽는다. 세션 하나는 자기 묶음 안에서 세로로 간다 | 06-1 관계표의 의존 방향. 루트 CLAUDE.md 문구 수정은 하네스 몫이라 이슈로 연다 |
| 결정 3 | 묶음마다 Codex A와 B 라운드 | 오늘은 코드와 테스트와 검증 표까지. Codex는 MVP 코드가 다 붙은 뒤 한 번, 재고와 요금 R2를 같이 | 왕복이 하루다 |

프롬프트를 새 세션에 붙여 넣는 행위가 이 셋의 승인이다. 셋 중 하나라도 다르면 붙이기 전에 프롬프트의 오늘의 전제 절을 고친다.

## 2. 세션 셋

| 세션 | 프롬프트 파일 | 묶음 | 워크트리 | 브랜치 | 테스트 DB |
|---|---|---|---|---|---|
| P | prompt-P-promotion-search.md | 프로모션과 검색. PROMO-01~05, SEARCH-01~03 | 새 폴더 | feat/task-s9-promotion-search (PR 98, 계약 승인됨) | o2o_promo_test |
| Y | prompt-Y-payment.md | 결제 컨텍스트. PaymentAttempt, Mock 모드 셋, INTERNAL-01, 환불 기록, T30 | 새 폴더 | feat/task-s9-payment (새로) | o2o_payment_test |
| B | prompt-B-booking.md | 예약과 선점. BOOK-01~03, HoldInventory. 1차 셋이 main에 들어간 뒤 2차로 결제 중계와 확정과 만료와 취소 | o2o-dev (지금 세션) | feat/task-s9-booking (새로) | o2o_catalog_test (기본값 그대로) |

o2o_booking_test는 예비다. 네 번째 세션이 생기면 쓴다.

## 3. 시작 순서 (사용자)

| 순서 | 하는 일 | 왜 |
|---|---|---|
| 1 | PR 105, 115, 117을 순서대로 병합한다 | 세 세션이 같은 main(SeoulDate, V13, V14, backend/.claude/rules)에서 출발해야 한다 |
| 2 | 워크트리 둘을 만든다. 아래 명령 | 세션 하나에 작업 트리 하나. 브랜치가 섞이지 않는다 |
| 3 | 각 워크트리의 backend/에 backend/.env를 복사한다 | DB 계정. 모델은 이 파일을 읽지 않는다 |
| 4 | 새 세션 둘을 각 워크트리에서 열고 프롬프트 파일의 코드 블록을 통째로 붙여 넣는다 | 첫 턴이 상태 확인부터 한다 |
| 5 | 이 세션(o2o-dev)에 prompt-B-booking.md의 블록을 붙여 넣거나 시작하라고 말한다 | 같다 |

워크트리 명령. PowerShell에서 한 줄씩. main은 다른 워크트리에 체크아웃돼 있어서 로컬 main 대신 origin/main에서 딴다.

```powershell
git -C C:\Dev\potenup\99_projects\o2o fetch origin
```

```powershell
git -C C:\Dev\potenup\99_projects\o2o worktree add C:\Dev\potenup\99_projects\o2o-promo feat/task-s9-promotion-search
```

```powershell
git -C C:\Dev\potenup\99_projects\o2o worktree add -b feat/task-s9-payment C:\Dev\potenup\99_projects\o2o-payment origin/main
```

테스트 DB 셋은 2026-09-11 16시에 컨테이너 o2o-catalog-mysql 안에 만들어 앱 계정에 권한을 줬다. 컨테이너를 내리면 사라지므로 다시 올린 뒤에는 같은 문장을 다시 돌린다. 비밀번호는 컨테이너 환경변수로 넘겨서 셸에 안 남는다.

```bash
docker exec o2o-catalog-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS o2o_promo_test; CREATE DATABASE IF NOT EXISTS o2o_booking_test; CREATE DATABASE IF NOT EXISTS o2o_payment_test; GRANT ALL PRIVILEGES ON o2o_promo_test.* TO \"$MYSQL_USER\"@\"%\"; GRANT ALL PRIVILEGES ON o2o_booking_test.* TO \"$MYSQL_USER\"@\"%\"; GRANT ALL PRIVILEGES ON o2o_payment_test.* TO \"$MYSQL_USER\"@\"%\"; FLUSH PRIVILEGES;"'
```

DB를 세션마다 나누는 근거. 테스트 설정이 create-drop이라 한 세션의 테스트가 다른 세션의 표를 지운다. 셸 환경변수 SPRING_DATASOURCE_URL이 테스트 앱의 spring.datasource.url을 덮는 것은 2026-09-11에 실측했다. 없는 DB 이름을 주자 실패 메시지가 그 이름을 그대로 냈고, o2o_booking_test로 DatabaseConnectionTest가 1건 통과했다. build.gradle의 dotenv가 .env의 키를 상속 환경 위에 얹으므로 .env를 고칠 필요가 없다. API 테스트는 RANDOM_PORT라 포트는 안 부딪힌다.

## 4. 병합 순서와 접점

| 순서 | 무엇 | 왜 |
|---|---|---|
| 1 | P와 Y와 B 1차의 PR 셋. 순서는 끝나는 대로 | 서로 다른 패키지라 순서가 없다. progress.md는 PR 전에 각자 origin/main을 병합해 union으로 합친다 |
| 2 | B 2차 | Booking 상태 기계에 결제 이벤트 구독과 만료와 취소를 얹는다. P의 PricingService에 어댑터를 붙인다. Y의 openAttempt와 refund를 부른다 |
| 3 | Codex 한 번 | MVP 코드 전체. 재고와 요금 R2 포함 |

세션 사이 접점은 셋뿐이다. 각 프롬프트의 접점 표가 같은 문장을 갖는다.

| 접점 | 만드는 쪽 | 쓰는 쪽 | 모양 |
|---|---|---|---|
| PricingService.quote | P | B 2차 | PriceSnapshot quote(RoomTypeId roomTypeId, LocalDate checkIn, LocalDate checkOut). 인원 검사는 호출자 몫 |
| PaymentApplicationService의 openAttempt, refund, attemptsOf | Y | B 2차 | 예약 ID는 문자열로 받는다. 결제는 예약을 모른다(06-1 R6) |
| 도메인 이벤트 PaymentApproved, PaymentFailed | Y | B 2차 | bookingId, paymentAttemptId, amount, 그 예약의 완료된 시도 수를 싣는다 |
