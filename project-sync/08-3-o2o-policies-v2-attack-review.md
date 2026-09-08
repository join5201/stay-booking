# O2O 정책 해결안 v2 블라인드 검증 리포트 (Step 8, 3차)

최초 작성: 2026-09-06 (예약 작업 `O2O 정책 식별` 실행 3회차 세션에서 작성)
대상: claude/08-2-o2o-policies-resolution-review.md 6절의 해결안 v2(D0~D12, 정책 목록, 페이로드 표, 스케줄러 명세)를 스펙 S-0~S-12로 옮긴 것. 바탕은 04-5 v1, 06-2 v4, 06-4 v4
방식: ddd-blind-review 스킬. 대화 이력이 없는 읽기 전용 에이전트 셋에게 병렬 위임.
- G: 공격자. 스펙 v2가 적용된 시스템을 상대로 7가지 수단(인터리빙, 유실, 중복, 순서 역전, 외부 호출 실패, 시간 위상, 상태 조합 전수)으로 R1~R6과 불변식을 깨는 시나리오 탐색
- H: 감사자. 정책과 Saga 축 5개, 공통 축 3개, 이벤트 스토밍 델타 전수 추출
- I: 구현자 워크스루. 독자 프로필(주니어 백엔드)로 여섯 경로의 의사코드를 써 보며 문서만으로 정할 수 없는 지점을 표시
수정은 하지 않았다. 02, 03, 04, 04-5, 05-3, 06-1, 06-2, 06-4, 입력 팩 전부 그대로다. 수용 여부는 사용자가 정한다.

## 0. 이 리포트의 전제

### 0-1. 실행 맥락. 08-2 8절 4항을 넘어선 이유

08-2(09-06 03:50)와 01 v18(09-06 04:22)은 "사용자 판정이 나타나기 전까지 판정 유무만 확인하고 조용히 종료한다"고 적었다. 이번 실행(09-06)에서 확인한 결과 08-2 이후 프로젝트 문서 변경은 없고 사용자 판정도 없다.

그럼에도 이번 실행이 검증을 돌린 이유는 하나다. 08-2 6절의 해결안 v2는 08-2 세션이 E, F의 공격 결과를 보고 직접 쓴 것이라 검증을 거친 적이 없다. 08-2 8절의 결정 10개는 전부 v2를 전제로 하는데, v2에 치명 결함이 있으면 사용자는 결함 있는 안에 대해 판정하게 된다. 08-1 7절의 해결안(v1)을 08-2가 스펙으로 옮겨 공격했을 때 치명 6건이 나왔다는 사실이, v2도 같은 검사를 받아야 한다는 근거다. 스킬 규칙의 "재검증 최대 1회"는 반영 후의 재검증을 말하고, v2는 아직 어느 문서에도 반영되지 않았으므로 이 규칙에 걸리지 않는다. "해결안을 또 공격하면 v3가 나올 뿐"이라는 08-2의 우려는 맞았다. 실제로 이번에도 치명이 나왔고, 그래서 6절에 v2.1이 있다. 다만 이번 치명은 v2가 새로 도입한 기계(정산 표식, 고아 승인, 동기 환불)의 내부 모순이라, 사용자가 판정하기 전에 알아야 하는 종류다.

이 실행 이후의 반복 규칙은 8절에 다시 적었다.

### 0-2. 에이전트에게 넘긴 것과 넘기지 않은 것

넘긴 것 (격리 폴더에 사본으로): 입력 팩(08-2와 같은 가공. R6 추가, 확정 전제 세 줄 추가), 02, 03, 04, 04-5, 06-2 (1~7절), 06-4 (0~2절 + 3절의 역추적표와 [가설] 설정값만). 그리고 스펙 v2(S-0~S-12).
넘기지 않은 것: 08-1, 08-2 전체(공격 결과, 추천 근거, 대안 비교), 06-4 3절의 이월 목록, 01, 04-6, 06-3, 06-5, 06-6, 05-3, 06-1, 대화 이력.

사본에서 가공한 것: 06-2 4절 "취소의 RefundPayment(P6)는 Step 8"을 "비동기 (06-4 2-2)"로 바꿔 적었다(08-2와 동일). 06-4 3절은 역추적표(R1~R5)와 [가설] 설정값(ttl 10분)만 남기고 이월 목록을 뺐다. 08-2 F 확인필요 1(TTL 값 부재)이 사본 가공에서 나온 지적이었으므로 이번엔 ttl 값을 남겼다.

### 0-3. 스펙 v2가 무엇인가

08-2 6절 D0~D12의 추천안을 근거, 대안, "추천" 표현을 빼고 스펙 문장으로만 옮긴 것이다. D0의 "리스너 예외는 로그 후 삼킨다"를 S-0에 그대로 두었고, D8 스케줄러 표를 S-8에 그대로 옮겼다. 에이전트 셋 모두 이 스펙을 봤다(08-2의 D처럼 스펙을 보지 않는 재도출 에이전트는 이번에 두지 않았다. 08-2 D가 이미 뿌리 결정 넷을 재도출했고 이번 대상은 그 위의 기계 부품이라 재도출 대상이 아니다).

| 번호 | 내용 (요약) | 08-2 출처 |
|---|---|---|
| S-0 | AFTER_COMMIT 동기 리스너 + REQUIRES_NEW, 하위 호출 REQUIRED 합류, 최대 1회, 예외는 로그 후 삼킴, PG 응답 항상 2xx | D0 |
| S-1 | Payment에 settledAt, settledBy. SettlePayment(bookingId, settledBy) 커맨드. 찍는 곳 4곳 열거. T2 후보는 settledAt null | D1 |
| S-2 | attemptId(UUID), U5. 종착 시도에 온 승인은 거부 대신 OrphanApproval 기록 + 같은 트랜잭션 환불. I7에서 고아 제외 | D2 |
| S-3 | 확정 우선. T1이 Payment를 조회해 미정산 APPROVED면 확정. confirm에 expiresAt 검사 없음. RequestPayment 중계에 잠금, 임계 거부 없음 | D3 |
| S-4 | 취소 환불 동기, RefundPayment(bookingId). 이탈 선언 확장. 전역 잠금 순서 Booking → 재고 → Payment | D4 |
| S-5 | 분기 주체 앱 서비스. confirm 예외 단순화. expire의 CONFIRMED/CANCELED 스킵 반환. 반환 2값 | D5 |
| S-6 | T2 조회에 3회 실패 조건. R4 문언 | D6 |
| S-7 | 타임아웃 미도입, 수동 개입 문구 교체 | D7 |
| S-8 | 스케줄러 표 (T1 30초, T2 60초, 지연 임계 30초, 배치 100) | D8 |
| S-9 | n = 1 | D9 |
| S-10 | 종착 멱등키 재요청은 기존 예약 반환 | D10 |
| S-11 | A4 유지 + 문언, 06-2 4절 감수 문장 삭제 | D11 |
| S-12 | U4 유지 | D12 |

### 0-4. 서로 모르고 같은 지점을 짚은 것

| 지점 | G (공격) | H (감사) | I (구현자) |
|---|---|---|---|
| 정산이 끝난 Payment에 붙은 고아 승인(REFUND_PENDING)이 T2 조회의 settledAt null 조건에 걸려 영구 제외. 실청구가 환불되지 않음 | 치명 1 | 치명 1 | [32] |
| T2 조회 조건 "APPROVED 시도 존재"가 REFUNDED를 포함하는지 미정. 포함하면 환불 완료 건이 영구 재후보 | 치명 3, 확인필요 1 | 치명 2 | [17] 치명 |
| T2와 P1의 환불 분기, expire 스킵 분기가 정산 표식을 찍지 않아 settledAt null인 건이 매 주기 재후보. 배치 100을 채우면 T2 보정 전면 정지 | 치명 2, 치명 3 | 보통 1 | [3] [9] [14] [22] |
| 전역 잠금 순서(Booking → 재고 → Payment)와 S-8 T1 건별 처리(Booking → Payment → 재고)의 모순 | 보통 1 | 보통 2 | [13] 치명 |
| T2 건별 처리에 Payment 잠금과 조건 재검사가 없음 | 보통 5 | 보통 3, 확인필요 2 | [21] 치명 |
| S-4 동기 환불 이후 CANCELED + APPROVED 분기는 도달 불가 | 보통 3 | 확인필요 4 | - |
| 고아 환불 실패 시 REFUND_PENDING으로 커밋되는지 롤백되는지 미정 | - | 확인필요 3 | [30] 치명 |
| 환불 요청에 멱등 식별자가 없어 결과 미상 시 PG 이중 환불 | 치명 4 | - | [19] |
| 잠금(재고 행, Payment)을 쥔 채 외부 PG 호출 | 보통 2 | - | [26] |
| R4 "먼저 오는 조건"이 S-3 확정 우선으로 깨지는데 S-6 문언과 02 3-3이 그대로 | - | 보통 4 | - |
| 리스너의 예외 삼킴 위치가 REQUIRES_NEW 롤백을 무효화할 수 있음 | - | - | [1] 치명 |
| recordApproval 재서술에서 I7 검사(승인 이력 없음)와 AlreadyApproved가 빠짐 | - | 보통 10 | [28] 치명 |
| expire 스킵이 금지 전이 감지를 소실시킴. 종착 무해와 금지 전이 스킵을 같은 반환값으로 합침 | 보통 6 | 보통 11 | - |

## 1. 판정 요약

| 에이전트 | 치명 | 보통 | 확인필요 |
|---|---|---|---|
| G (공격) | 4 | 6 | 4 |
| H (감사) | 2 | 14 | 7 |
| I (구현자) | 6 | 15 | 11 |

결론 한 줄: v2의 방향(정산 표식으로 T2 후보를 유한하게, 고아 승인으로 기록 없는 승인을 없애고, 확정 우선으로 T1과 승인의 경합을 결정적으로)은 셋 다 성립했다. 그러나 정산 표식의 의미와 찍는 위치가 S-1과 S-8 사이에서 어긋나 v1의 굶주림(08-2 치명 1)이 형태만 바꿔 다시 열렸고, 고아 승인 환불의 실패 처리와 T2 조회 조건이 이어져 있지 않아 실청구 미환불 경로가 새로 생겼다. 6절 v2.1은 정산 표식의 정의를 하나로 고정하고(예약 컨텍스트의 후속이 끝났다는 표식. 모든 종결 분기가 찍는다), 고아 미결을 settledAt과 독립된 조회 조건으로 분리하고, 잠금 순서를 Booking → Payment → 재고로 바꾸는 것으로 이 셋을 닫는다.

G가 판정한 08-2 해결안의 기존 결함 폐쇄 여부는 3절 "막혔다고 확인한 공격" 목록에 있다. 08-2 치명 1(굶주림)은 정상 확정분에 대해서는 닫혔고 스킵/환불 분기에서 재발했다. 08-2 치명 2, 3(기록 없는 승인, PG 이중 승인)은 고아 승인 경로로 닫혔다. 08-2 치명 4(T1이 HELD 중 승인을 만료로 뒤집음)는 확정 우선으로 닫혔다.

## 2. 에이전트 G 상세 (공격 시나리오, 원문 그대로)

판정 요약: 치명 4 / 보통 6 / 확인필요 4

| 심각도 | 시나리오 이름 | 깨지는 요구사항 또는 불변식 | 인터리빙 또는 실행 순서 | 결과 상태 | 문서 내 근거 (막는 규칙이 없다는 근거) | 수정 제안 |
|---|---|---|---|---|---|---|
| 치명 | 정산 완료된 Payment에 붙은 고아 승인이 순찰 대상에서 영구 제외 | R6(중복 콜백이 환불을 만들지 않음의 반대편. 실청구가 환불되지 않음), S-2 "고아 승인 전이: REFUND_PENDING → REFUNDED" | t1 attempt3 REQUESTED(pgTx 미정) / t2 실패 콜백(pgTx=X) → recordFailure → attempt3 FAILED, PaymentFailed(count=3) / t3 P3 리스너: Booking HELD → expire(PAYMENT_FAILED) + releaseHeld + SettlePayment(FAILURE_HANDLER) 커밋 → settledAt=t3 / t4 PG가 뒤늦게 같은 attemptId=3, 새 pgTx=Y로 승인 콜백(순서 역전) / t5 recordApproval: attempt3은 종착(FAILED) → OrphanApproval(REFUND_PENDING) 기록 + Mock PG 환불 호출이 타임아웃 → REFUND_PENDING 유지한 채 커밋 / t6 이후 T2 매 주기 | Booking EXPIRED, Payment settledAt=t3, OrphanApproval이 REFUND_PENDING으로 영구 고착. 게스트는 실제 청구당한 뒤 환불받지 못함 | spec-v2 S-8 T2 후보 조회 칸: settledAt is null and (… 또는 OrphanApproval REFUND_PENDING 존재). settledAt 조건이 OR 그룹 전체와 AND로 묶여 있다. S-1 마지막 줄이 이를 못박는다: "T2의 후보 집합은 settledAt이 null인 Payment로 한정된다". 결과 정책 목록 8개 중 OrphanApproval을 다루는 것은 T2뿐이고, S-0은 재전달, DLQ, 재시도를 모두 없앴다 | 조회를 settledAt is null OR OrphanApproval REFUND_PENDING 존재로 바꾸거나, 고아 미결을 settledAt과 독립된 별도 표식(예: pendingOrphanRefundCount > 0)으로 두고 그 표식을 T2 조회의 독립 OR 항으로 올린다 |
| 치명 | 전이 없음(스킵) 경로가 정산 표식을 남기지 않아 T2가 무한 재후보 → 배치 기아 | R4(S-6 문언 "리스너 유실 시에도 늦어도 T2 주기 안에 발동"), A4 | t1 Booking HELD, attempt1, 2 FAILED, attempt3 REQUESTED / t2 TTL 경과 → T1: Booking 잠금 → Payment 잠금 조회 → 미정산 APPROVED 없음 → expire(TTL_EXPIRED) + releaseHeld 커밋 (T1의 expire 분기는 SettlePayment를 부르지 않는다) → settledAt=null / t3 attempt3 실패 콜백 → recordFailure → PaymentFailed(count=3) / t4 P3 리스너: Booking 잠금 → EXPIRED → expire는 전이 없음 반환(S-5) → 재고 연산 생략. SettlePayment(FAILURE_HANDLER)를 부르는지 문서가 정하지 않음 → 안 부르는 구현이면 settledAt=null / t5 T2 조회: "settledAt is null and FAILED 3개 and 승인 이력 없음 and 마지막 failedAt <= now-30초" 전부 충족 → 후보 / t6 Booking 잠금 → 상태 EXPIRED + 승인 없음 → S-8 T2 분기 목록 어디에도 없음 → 스킵 / t7 settledAt 여전히 null → t5로 복귀, 영구 반복 | 좀비 후보 1건 생성. S-8 T2 후보 조회는 "가장 오래된 시각 오름차순, 배치 100"이므로 좀비가 100건 누적되면 신규 미정산 건이 배치에 영영 들어오지 못한다. 그 순간 P1, P3 유실 보정(S-6, S-11)이 전면 정지 | S-5는 "CONFIRMED와 CANCELED는 전이 없음 반환(스킵)"만 규정하고, 스킵 시 같은 정책 행동의 나머지(SettlePayment)를 실행하는지 침묵한다. S-8 T2 "전이 불가 상태: 스킵" 칸도 같다. 반면 S-1 (라)는 "T2가 처리하는 건별 트랜잭션"에서 정산 표식을 찍는다고 하여, S-8 분기표와 정면 충돌한다. S-8 "한 건 실패: settledAt이 남지 않으므로 자동 재후보"는 성공/스킵에도 그대로 적용되어 재후보를 만든다 | T2 건별 트랜잭션은 결과와 무관하게(전이 없음, 스킵 포함) 반드시 SettlePayment(T2)로 종결한다는 규칙을 S-8에 명시한다. 또는 좀비 방지용으로 후보 조회에 lastPatrolledAt 기반 백오프를 넣는다. 어느 쪽이든 배치 100 + 오래된 순 정렬은 기아를 구조적으로 허용하므로 스킵 건을 후보에서 빼는 수단이 필요하다 |
| 치명 | T2의 환불 분기가 정산 표식을 남기지 않아 같은 건을 매 주기 재처리 | R4, A4 (보정 경로 기아). "APPROVED 시도 존재"의 정의에 따라 갈림 | t1 Booking HELD + attempt1 APPROVED, PaymentApproved 발행 / t2 T1이 TTL로 먼저 만료(A4 정의상 TTL 승) → Booking EXPIRED / t3 P1 리스너가 EXPIRED 분기 실행 중 예외 → 로그 후 삼킴(S-0), settledAt=null / t4 T2: settledAt null + APPROVED + approvedAt <= now-30초 → 후보 → Booking 잠금 → EXPIRED+APPROVED → refund(bookingId)만 수행. attempt는 REFUNDED, settledAt은 여전히 null / t5 다음 주기: 조회 조건 "APPROVED 시도 존재"를 I7의 "승인 이력(APPROVED 또는 REFUNDED)"으로 읽으면 다시 후보 → refund 재호출(REFUNDED 무해) → 영구 반복 | 좀비 후보. 치명 2와 합쳐져 배치 100을 채우면 T2 보정 전면 정지. 좁게(status==APPROVED만) 읽으면 좀비는 안 되지만 settledAt이 영구 null로 남아, S-1이 세운 "미정산 = 아직 처리 안 됨"이라는 전제가 무너지고 치명 1의 반대 케이스 판별이 불가능해진다 | S-8 T2 건별 처리 칸: "EXPIRED 또는 CANCELED + APPROVED → refund(bookingId)". SettlePayment가 없다. 같은 칸의 다른 세 분기(HELD+APPROVED, HELD+3실패, CONFIRMED+APPROVED)에는 명시적으로 SettlePayment(T2)가 붙어 있으므로 누락이 의도적 대비로 보인다. 결과 정책 목록 P1의 "EXPIRED: refund(bookingId)"도 동일하게 정산 표식이 없다 | 환불 분기에도 SettlePayment(T2)를 붙인다. 동시에 "APPROVED 시도 존재"가 REFUNDED를 포함하는지 조회 조건 문언을 확정한다 |
| 치명 | 환불 외부 호출의 결과 불명 + 멱등키 부재 → PG 이중 환불 | R6("예약당 승인된 결제는 하나"의 금액 정합), 06-4 refund 계약의 "REFUNDED 재호출 무해" 전제 | t1 게스트 CancelBooking → 앱 서비스가 Booking 잠금 → CONFIRMED 확인 → cancel() → releaseSold ×N → refund(bookingId) → Payment 잠금 → Mock PG 환불 호출 / t2 PG는 환불을 실제로 처리했으나 응답이 타임아웃 또는 커넥션 리셋 / t3 S-4 "환불 호출이 실패하면 트랜잭션 롤백, 취소 자체가 실패한다" → 전체 롤백. Booking CONFIRMED, soldCount 그대로, attempt APPROVED, settledAt 그대로 / t4 게스트가 다시 CancelBooking → refund Pre "APPROVED 존재" 충족 → PG에 환불 재요청 | PG 원장 기준 환불 2회, 우리 원장 기준 CANCELED + REFUNDED 1회. 금액 불일치. T2는 settledAt이 non-null이라 이 Payment를 후보로 잡지 않으므로 탐지도 안 된다 | S-4는 "실패하면 롤백"만 규정하고, 실패가 "미도달"인지 "결과 미상"인지 구분하지 않는다. 06-4의 REFUNDED 재호출 무해는 로컬 상태 기준이지 PG 호출 기준이 아니다. 이벤트 페이로드표의 PaymentRefunded에는 pgTransactionId만 있고 우리가 발급한 환불 요청 식별자가 없다. S-2가 승인 쪽에는 attemptId를 도입했지만 환불 쪽에는 대응 식별자를 도입하지 않았다. 같은 구멍이 S-8 T2의 "OrphanApproval REFUND_PENDING → 환불 재시도"에도 그대로 있다 | 승인의 attemptId와 대칭으로 refundId(UUID)를 발급해 PG 환불 요청에 실어 보내고, Mock PG 콜백 계약에 "같은 refundId 재요청은 같은 결과를 돌려준다"를 넣는다. 그리고 결과 미상일 때는 롤백이 아니라 REFUND_PENDING으로 커밋 후 T2가 재시도하도록 분리한다 |
| 보통 | 전역 잠금 순서 선언과 T1 건별 처리 순서의 모순 | S-4 "전역 잠금 순서: Booking → 재고 N행(날짜 오름차순) → Payment" | S-8 T1 건별 처리: Booking 잠금 → 재검사 → Payment 잠금 후 조회 → 확정 경로(confirm + commit ×N + SettlePayment(T1)). 실제 순서는 Booking → Payment → 재고 | 현재 명세만으로는 실제 교착을 구성하지 못했다. 재고를 쥔 채 특정 Payment를 기다리는 트랜잭션이 필요한데, U3(bookingId당 Payment 1개)와 모든 결제 경로의 Booking 선행 잠금 때문에 사이클이 닫히지 않는다. 다만 독자가 T2나 P1을 T1과 같은 형태(Payment 먼저 조회)로 짜는 순간 P1(Booking→재고→Payment)과 사이에 즉시 사이클이 생긴다 | S-4가 전역 순서를 한 줄로 선언했는데 S-8 T1 칸이 그 순서를 어긴다. 어느 쪽이 우선인지 스펙 안에 판정 규칙이 없고, 06-2 5절 동시성 규칙도 Booking과 Payment 사이 상대 순서를 정하지 않았다 | 전역 순서를 Booking → Payment → 재고로 바꾸거나(모든 경로가 이 순서를 따를 수 있다), T1이 Payment를 잠금 없이 조회하고 확정 경로 진입 후 재고 → Payment 순으로 잠그도록 S-8을 고친다 |
| 보통 | 재고 행 락을 쥔 채 외부 PG 동기 호출 | 없음(요구사항 직접 위반 아님). R2의 처리량에 영향 | cancel() 트랜잭션이 Booking + 재고 N행 + Payment 락을 모두 쥔 상태에서 Mock PG 환불을 동기 호출(S-4). 재고 행은 (roomTypeId, stayDate) 단위라 그 객실 타입의 모든 게스트가 공유한다 | PG 응답이 t초 지연되면 그 날짜의 hold/commit/release가 전부 t초 차단. 동시 취소 k건이면 직렬 k×t. 같은 문제가 P1의 EXPIRED 분기(S-4 "리스너 트랜잭션 안에서 동기")에도 있고, 이 리스너는 S-0에 따라 PG 콜백 요청 스레드에서 돈다 | S-4는 "같은 트랜잭션"만 규정하고 호출 타임아웃 상한이나 락 획득 시점을 규정하지 않는다. 06-4 0절 락 선언에도 "락 보유 중 외부 호출 금지" 같은 제약이 없다. S-2의 고아 환불(Payment 락 보유 중 PG 호출)도 마찬가지다 | PG 호출에 짧은 타임아웃 상한을 [가설]로 못박고, 취소 경로에서 재고 반환과 환불의 락 구간을 분리하거나(REFUND_PENDING 커밋 후 T2 재시도), 최소한 "재고 락 획득 전에 환불을 끝낸다"를 명시한다 |
| 보통 | 도달 불가능한 CANCELED + APPROVED 분기 | 없음(죽은 분기). S-4와 정책 목록의 모순 | S-4가 cancel()의 refund를 같은 트랜잭션으로 옮기고 실패 시 롤백시켰으므로, Booking이 CANCELED가 된 시점에 그 Payment의 시도는 항상 REFUNDED다. APPROVED로 남는 경로가 없다 | 결과 정책 목록 P1의 "CANCELED: 미정산 APPROVED가 있으면 refund"와 S-8 T2의 "EXPIRED 또는 CANCELED + APPROVED → refund"의 CANCELED 부분이 실행되지 않는 코드가 된다 | 스펙 안에서 S-4(동기 환불)와 P1/T2 분기표(CANCELED 잔여 승인 상정)가 서로를 참조하지 않는다. 06-4 2-2의 "취소 시 환불 = 결과적(비동기)"을 전제로 쓰인 분기가 S-4의 동기화 이후에도 남아 있다 | CANCELED 분기를 삭제하거나, 남긴다면 "S-4 이전 데이터 또는 수동 개입 흔적에 대한 방어"라고 근거를 적는다 |
| 보통 | 같은 멱등키 동시 진입 시 응답 미규정 | R3(사용자 관점의 중복 예약 방지) | t1, t2 두 요청이 같은 idempotencyKey로 동시 진입 / t3 둘 다 "기존 예약 없음" 조회(잠금 없음) / t4 둘 다 hold ×N (재고 락으로 직렬화, heldCount 2 증가) / t5 늦은 쪽 Booking insert에서 U1 유니크 위반 → 전체 롤백(hold도 롤백) | 재고는 안전하고 Booking도 하나만 남으므로 R3의 문자적 보장은 유지된다. 그러나 늦은 쪽 클라이언트가 받는 응답이 오류인지 멱등 반환인지 규정이 없다. 오류를 받은 클라이언트가 새 키로 재시도하면 실질적 중복 예약이 생긴다 | S-10은 "재요청"만 다루고 동시 요청을 다루지 않는다. 06-4 RequestBooking 계약의 예외 칸에 OccupancyExceeded, RateNotFound, InventoryShortage만 있고 U1 위반 예외가 없다. 06-4 1-1 U1의 강제 수단은 "DB 유니크 + 선행조건"인데, 선행조건 검사와 insert 사이의 경합을 다루는 문장이 없다 | RequestBooking에 "U1 유니크 위반을 잡으면 해당 키로 재조회해 S-10의 멱등 반환 경로로 합류한다"를 Post에 명시한다 |
| 보통 | T2 건별 처리의 Payment 잠금 비대칭 | 없음(실피해 구성 실패). 명세 누락 | S-8 T1 칸은 "Payment 잠금 후 조회"를 명시하는데, T2 칸은 "Booking 잠금 → 상태 분기"만 있고 Payment를 언제 어떻게 잠그는지 없다. T2의 상태 분기는 Payment 조건(APPROVED 유무, FAILED 3개)에 의존한다 | 잠금 없이 읽은 스냅샷으로 분기하게 된다. 다만 I6(시도 상한 3)과 I9(진행 유일)가 조회 시점 이후의 시도 증가를 막고, 종착 시도에 오는 승인은 고아 경로로 빠져 PaymentApproved를 발행하지 않으므로, 현재 명세로는 실제 오분기 시나리오를 구성하지 못했다 | 06-2 5절은 Payment에 대해 "시도 추가와 콜백 기록은 루트를 잠근 뒤"라고만 하여 순찰 조회를 커버하지 않는다. S-8 T1과 T2의 서술 형식이 다른 것 외에 판정 근거가 없다 | T2 칸에도 "Booking 잠금 → Payment 잠금 → 상태 분기"를 명시해 T1과 형식을 맞춘다 |
| 보통 | expire()의 금지 전이 감지가 소실 | I5 전이 폐쇄(위반 자체는 없으나 감지가 사라짐) | S-5: "expire(reason): CONFIRMED와 CANCELED는 예외가 아니라 전이 없음 반환(스킵)". 06-4 1-2 원본은 InvalidStateTransition | 상태는 바뀌지 않으므로 I5는 유지된다. 그러나 06-4 1-3이 "CONFIRMED에서 EXPIRED로"를 금지 전이로 못박은 이상 그런 호출이 들어온 것 자체가 버그 신호인데, 이제 조용히 삼켜진다. 게다가 이 스킵 경로가 치명 2의 발생 지점이다 | S-5는 "종착 무해"(같은 종착 상태로의 재호출)와 "금지 전이 요청"을 같은 반환값(전이 없음)으로 합쳤고, 둘을 구분하는 수단을 두지 않았다. 06-4 0절의 종착 무해 정의는 "이미 그 종착 상태면"으로 한정돼 있어 CONFIRMED에 대한 expire를 포함하지 않는다 | 반환 규약을 전이 발생 / 종착 무해 / 금지 전이 스킵 셋으로 나누고, 세 번째는 경보 로그 대상으로 지정한다 |
| 확인필요 | T2 조회 조건 "APPROVED 시도 존재"의 정의 | R4, A4 (치명 3의 분기점) | - | I7은 "승인 이력(APPROVED 또는 REFUNDED)"이라고 REFUNDED를 포함시켰는데, S-8 T2 조회 조건은 "APPROVED 시도 존재"라고만 쓴다. 넓게 읽으면 환불 완료 건이 영구 재후보가 되고(치명 3), 좁게 읽으면 settledAt이 영구 null로 남는다 | S-8, S-1, 06-4 1-1 I7 세 곳의 용어가 서로 다르고, 어느 것이 조회 조건의 정의인지 스펙이 지정하지 않는다 | 조회 조건을 "status = APPROVED인 시도가 존재하고"로 확정하고, 환불 완료 건은 SettlePayment로 후보에서 뺀다 |
| 확인필요 | openAttempt와 PG 요청의 트랜잭션 경계 | - | S-2: "openAttempt Post에 attemptId 발급과 PG 요청 시 전달이 추가된다" | 트랜잭션 안이라면 Booking 락(S-3 RequestPayment 중계 Pre) + Payment 락을 쥔 채 외부 호출이다. 트랜잭션 밖이라면 커밋과 PG 호출 사이 프로세스 종료 시 attempt가 REQUESTED로 영구 갇히고 I9가 재시도를 막는다 | S-2는 "Post에 추가"라고만 쓰고 커밋 전후를 정하지 않는다. S-7은 "콜백을 영영 못 받는 갇힘"만 감수 대상으로 선언했고, "요청 자체가 나가지 않은 갇힘"은 언급하지 않는다 | openAttempt 커밋 후 PG 호출인지, 같은 트랜잭션인지 한 줄로 정한다. 후자라면 S-4의 외부 호출 제약과 함께 다뤄야 한다 |
| 확인필요 | recordApproval의 UnknownAttempt 예외 시 PG 응답 | R6 (PG 재전송 유발 가능성) | S-2: "attemptId에 해당하는 시도가 없으면 UnknownAttempt 예외" | S-0은 "PG 콜백에 대한 HTTP 응답은 리스너 결과와 무관하게 2xx"라고만 규정한다. recordApproval 본체가 던진 예외의 HTTP 응답은 규정되지 않았다. 4xx/5xx로 나가면 PG가 재전송할 수 있고, 재전송의 대상 attempt는 여전히 없다 | S-0의 2xx 규정 범위가 리스너로 한정돼 있고, ACL(04 발견 2)의 오류 처리 규정이 어느 문서에도 없다 | recordApproval 본체 실패 시의 HTTP 응답과 재전송 정책을 S-0에 추가한다 |
| 확인필요 | settledBy 열거의 T1과 T1 expire 분기의 불일치 | - | S-1은 settledBy에 T1을 넣었으나, S-8 T1의 expire(TTL_EXPIRED) 분기에는 SettlePayment가 없다. T1이 정산자로 등장하는 경우는 확정 경로 하나뿐이다 | TTL로 만료된 건의 Payment는 settledAt이 영구 null로 남는다. 지금은 T2 조회 조건 셋 어디에도 안 걸려 무해하지만, 치명 1의 고아 승인이 이런 Payment에 붙으면 잡히고 정산된 Payment에 붙으면 안 잡히는 비일관이 생긴다 | S-1 (가)~(라)가 정산 표식을 찍는 네 지점을 열거했는데 T1의 expire 분기가 빠져 있고, 그것이 의도인지 누락인지 판정할 근거가 스펙 안에 없다 | TTL 만료 건도 SettlePayment(T1)로 종결할지 결정한다. "미정산 = 아직 후속 처리가 필요한 상태"라는 정의를 명문화하면 자동으로 정해진다 |

#### 막혔다고 확인한 공격 (요약 목록)

- 같은 pgTransactionId 승인 콜백 재전송 → S-2 recordApproval Pre "같은 pgTransactionId가 이미 기록돼 있으면 무해 무시(U4)", S-12.
- CONFIRMED 예약에 승인 콜백 재도착으로 정상 결제 환불 유도 → 결과 정책 P1의 CONFIRMED 분기가 SettlePayment(무해)만 수행하고 환불하지 않는다. 04 발견 3의 "가드를 == EXPIRED로 좁힌 이유"가 그대로 유지된다.
- 이중 확정(같은 Booking에 confirm 두 번) → confirm의 CONFIRMED 재호출 무해 + 전이 없음 반환이 commit ×N까지 생략시킨다(06-4 0절, S-5).
- 이중 재고 반환으로 heldCount 음수 또는 초과 예약 유도 → expire/cancel의 전이 없음 반환이 후속 재고 연산을 생략시키고, I1a가 하한을 지킨다.
- T1과 P1이 같은 Booking을 동시에 처리 → 양쪽 모두 Booking 비관적 락 후 상태 재검사(S-8, S-5). 늦은 쪽은 스킵.
- T1과 T2가 같은 건을 같은 주기에 집는 경우 → Booking 락으로 직렬화되고, 늦은 쪽은 CONFIRMED/EXPIRED 재검사에서 스킵하거나 SettlePayment 무해로 끝난다.
- 승인 커밋 직후 프로세스 종료로 P1 리스너 유실 → settledAt이 null로 남아 T2 조회 첫 번째 조건(APPROVED + approvedAt <= now-30초)에 걸린다(S-11, S-6).
- P3 리스너 유실로 3회 실패 건이 HELD에 방치 → T2 조회 두 번째 조건(FAILED 3개 + 승인 이력 없음)이 잡고 expire(PAYMENT_FAILED) + 선점 반환(S-6). 단 Booking이 아직 HELD일 때만.
- 실패 통보 뒤 같은 attempt에 승인 도착(순서 역전) → S-2 고아 승인 경로. PaymentApproved를 발행하지 않으므로 이중 확정 없고, I7 셈에서 제외되므로 승인 유일도 유지된다.
- 실패 콜백과 승인 콜백 동시 도착 → Payment 락으로 직렬화. 어느 쪽이 이기든 늦은 쪽은 종착 판정으로 무해 무시 또는 고아 경로.
- 4번째 결제 시도 개설 → openAttempt Pre의 I6(시도 3 미만), I9(진행 유일).
- 승인 이력이 있는 상태에서 새 시도 개설 → openAttempt Pre의 I7.
- 스케줄러 다중 인스턴스가 같은 HELD 건을 동시 만료 → Booking 비관적 락 + 잠금 후 상태 재검사(S-8 다중 인스턴스 칸).
- 확정된 예약의 재고를 만료로 반환 → I5 전이 폐쇄. CONFIRMED → EXPIRED 전이가 없다.
- 확정 후 요금/프로모션 변경으로 청구액 변동 유도 → I4 스냅샷 동결, adjustRate와 promotion update/close의 "기존 스냅샷 무영향"(06-4 1-2). R5 유지.
- 연박 3박 중 1박만 선점된 상태 유도 → A1 + hold(n)의 전체 롤백(06-4 1-2). S-9로 n=1 고정이라 부분 성공 여지가 더 줄었다.
- TTL 경과 후 도착한 승인이 재고를 이중 점유 → T1의 확정 우선(S-3)이 Booking 락 안에서 판정하므로, 확정이든 만료든 한 번만 일어난다.
- expiresAt 경과 후 게스트가 새 결제를 열어 확정시키는 경로 → S-3이 "confirm()에 expiresAt 검사는 없다", "잔여 TTL에 따른 거부는 없다"로 명시적으로 허용한 동작이다. 결함이 아니라 결정.

#### 요구사항 역추적표 (G)

| 요구사항 | 스펙 적용 후 대응 항목 | 커버 여부 |
|---|---|---|
| R1 연박 전체 선점 실패 | A1 연박 원자성, hold(n) 전체 롤백, S-9(n=1 고정) | 커버 |
| R2 초과 예약 0 | I1, I1a, 06-4 0절 비관적 락 선언, S-4 전역 잠금 순서, 전이 없음 반환에 의한 재고 연산 생략 | 커버 (보통 5의 순서 모순은 현재 명세로 교착까지 가지 않음) |
| R3 중복 예약 방지 | U1 + S-10(어느 상태든 기존 예약 반환) | 부분. 동시 진입 시 U1 유니크 위반의 응답이 규정되지 않음(보통 8) |
| R4 HELD 종료 시 재고 반환 | A2, expire(reason), P3, T1, S-6(T2 보정)과 R4 재문언 | 부분. 보정 주체가 T2 하나뿐인데 치명 2, 3의 좀비 후보가 배치 100을 채우면 보정이 정지한다 |
| R5 확정 금액 불변 | I4, I15, adjustRate/promotion의 스냅샷 무영향, openAttempt의 총액 대조 | 커버 |
| R6 승인 결제 유일, 중복 콜백이 환불과 이중 확정을 만들지 않음 | S-2(attemptId, 고아 승인, U4 무해 무시), I7(고아 제외), P1의 CONFIRMED 분기 | 부분. 이중 확정은 막힌다. 그러나 치명 1(실청구가 영구 미환불)과 치명 4(PG 이중 환불)로 금액 정합이 깨진다. 덧붙여 06-4 3절 역추적표에 R6 행 자체가 없고 spec-v2도 추가하지 않았다 |
| A4 승인 후속 (파생) | S-11 재문언, P1, S-3 T1 확정 우선, T2 | 부분. "결과적. 리스너 유실 시 T2 주기 안"의 상한이 T2 배치 기아(치명 2, 3)로 보장되지 않는다 |

## 3. 에이전트 H 상세 (규칙 준수, 추적성, 델타 감사. 원문 그대로)

판정 요약: 치명 2 / 보통 14 / 확인필요 7

#### 델타 표

스펙 v2가 강제하는 기반 문서 변경의 전수 추출이다. "스펙이 스스로 적었나" 칸은 스펙이 대상 문서와 절을 명시했는가를 뜻한다.

| 대상 문서와 절 | 추가/삭제/변경 | 내용 | 스펙이 스스로 적었나 |
|---|---|---|---|
| 06-2 1절 (애그리거트 표) | 추가 | Payment 내부 요소에 settledAt, settledBy | 아니오 (S-1은 필드만 선언) |
| 06-2 1절, 6절 CRC | 추가 | PaymentAttempt에 attemptId(UUID), approvedAt, failedAt | 아니오 |
| 06-2 1절, 6절 CRC | 추가 | OrphanApproval 구성요소와 상태(REFUND_PENDING, REFUNDED) | 아니오 (전이만 선언) |
| 06-2 2절 (커맨드→루트 22개) | 추가 | SettlePayment → Payment. 22 → 23 | 수량만. 표 미지정 |
| 06-2 3-4, 06-4 1-1 U 표 | 추가 | U5 attemptId 유일 | 규칙만. 절 미지정 |
| 06-2 4절 | 삭제 | "PaymentApproved 유실 시 ... 학습 범위로 감수" | 예 (S-11) |
| 06-2 4절 | 변경 | "취소의 RefundPayment(P6)는 비동기 (06-4 2-2)" → 동기 같은 트랜잭션 | 아니오 |
| 06-2 5절 (동시성 규칙) | 변경 | 전역 잠금 순서 Booking → 재고 N행 → Payment 명문화, T1/T2의 Payment 잠금 | 순서만 (S-4). 절 미지정 |
| 06-2 6절 CRC 예약 | 추가 | BookingApplicationService의 협력자에 결제 조회 API, T2 스케줄러 | 아니오 |
| 06-2 6절 CRC 예약 | 변경 | Booking 행 "전이표 밖 요청은 거부한다(I5)" → expire의 CONFIRMED/CANCELED는 스킵 | 아니오 |
| 06-2 7절 시그니처 메모 | 추가/변경 | SettlePayment(bookingId, settledBy), RefundPayment(bookingId), attemptId | 부분 (S-1, S-4) |
| 06-4 0절 이탈 선언 | 변경 | 재고 셋 + 결제 셋 | 예 (S-4) |
| 06-4 0절 종착 무해 | 변경 | 적용 6곳 → 7곳 (settle 추가) | 예 ("7곳째", S-1) |
| 06-4 0절 멱등 반환 | 변경 | 대상이 종착이어도 그대로 반환 | 아니오 (S-10이 절 미지정) |
| 06-4 1-1 A4 | 변경 | 문언 교체 | 예 (S-11) |
| 06-4 1-2 대응표 | 추가/변경 | SettlePayment → settle, RefundPayment → refund(bookingId) | 아니오 |
| 06-4 1-2 confirm 행 | 변경 | EXPIRED 거부 → InvalidStateTransition 명시 | 규칙만 (S-5) |
| 06-4 1-2 expire 행 | 삭제/변경 | Pre의 "시도 수 3 이상 재확인" 삭제, CONFIRMED/CANCELED 예외 → 스킵 | Pre 삭제는 예. 예외 칸은 절 미지정 |
| 06-4 1-2 cancel 행 | 추가 | Post에 refund(bookingId), 실패 시 롤백 | 아니오 |
| 06-4 1-2 RequestPayment 중계 | 추가 | Pre에 Booking 잠금 | 아니오 |
| 06-4 1-2 recordApproval / recordFailure | 전면 교체 | attemptId 기준 분기, 고아 승인, UnknownAttempt | 규칙만. 절 미지정 |
| 06-4 1-3 예약 전이표 | 변경 | TTL 만료 행 Guard에 "미정산 승인 없음" 추가 | 아니오 |
| 06-4 1-3 결제 전이표 | 추가 | OrphanApproval REFUND_PENDING → REFUNDED | 규칙만 |
| 06-4 1-3 "알려진 갇힘" | 변경 | "수동 개입 대상" → v1 감수 | 예 (S-7) |
| 06-4 2-1 트리거 맵핑표 | 변경 | BookingCanceled 행 무효(내부 호출화), 시간 트리거에 T2 추가 | 아니오 |
| 06-4 2-2 정책 카드 | 전면 교체 | 8장 → 스펙 "결과 정책 목록" | 아니오 (대체 선언 없음) |
| 06-4 2-3 분류표 | 변경 | 취소 시 환불을 이탈 선언 군으로 이동, T2 행 추가 | 아니오 |
| 06-4 3절 역추적 | 변경/추가 | R4 문언 갱신, R6 행 신설 | R4 문언만 (S-6). R6 미해결 |
| 04-5 2절 정책표 | 전면 교체 | P1+P2 통합, P6 트리거 변경, 번호 재배정 | 아니오 |
| 04-5 3절 시간 트리거표 | 추가 | T2 행 | 아니오 |
| 04-5 4-1 | 변경 | 구독자 없는 이벤트 17개 → 20개 | 아니오 |
| 04-5 4-2 발신자 표 | 추가/변경 | SettlePayment 추가, 정책 번호 재배정 | 아니오 |
| 04-5 4-3 유형2 | 변경 | "거부가 오류인지 무시인지는 미정" → 무해 무시로 확정 | 아니오 |
| 04-5 6절 Step 8 이월 4항목 | 삭제 | 전부 스펙에서 종결 | 아니오 |
| 04 1절, 4-2 | 변경 | 커맨드 22 → 23 | 수량만 |
| 04 발견 3 체인표 | 변경 | "승인 지연" 행과 "확정 취소" 행 무효 | 아니오 |
| 03 1절 이벤트 의미 | 변경 | PaymentRefunded에 kind | 페이로드 표만. 03 미지정 |
| 03 5-3 | 변경 | "status == EXPIRED이면 자동 환불" → 4분기 통합 정책 | 아니오 |
| 03 6절 엔티티별 그룹 | 추가 | OrphanApproval 미반영 | 아니오 |
| 02 3-3 | 삭제 | "3회에 도달하기 전에 TTL이 먼저 만료되면 TTL이 이긴다" | 아니오 |
| 02 2절 R4 문언 | 변경 | S-6 문언 | 아니오 |

스펙이 손대지 않았지만 스펙 적용으로 무효가 되는 문장 (발췌).
1. 06-2 4절 "취소의 RefundPayment(P6)는 비동기 (06-4 2-2)"
2. 06-2 6절 CRC Booking "전이표 밖 요청은 거부한다(I5)"
3. 06-4 2-2 "취소 시 환불 / BookingCanceled / 결과적 (비동기)"
4. 06-4 2-3 "내부 정책(같은 컨텍스트 트리거) TTL 만료", "정책의 소속(구독자)은 전부 예약 컨텍스트다"
5. 04-5 1절 "이벤트 22개 중 시스템의 자동 반응이 필요한 것은 5개다"
6. 04-5 4-3 유형3 "T1이 이긴 경우의 잔여 승인은 P2가 환불로 흡수한다" (S-3에서 T1이 애초에 지지 않음)
7. 02 3-3 "3회에 도달하기 전에 TTL이 먼저 만료되면 TTL이 이긴다"
8. 03 2절 타임라인 "PaymentApproved, EXPIRED -> PaymentRefunded (TTL이 먼저 이긴 경우)"가 유일 경로처럼 읽힘

#### 상세

| 심각도 | 위치 | 위반 축 | 문제 | 문서 내 근거 | 수정 제안 |
|---|---|---|---|---|---|
| 치명 | spec-v2 S-1 ↔ S-8 (T2 후보 조회) | 실패 경로 / 멱등 | T2 후보 집합이 settledAt is null로 한정되는데, 고아 승인 재시도 조건 (c)는 정산이 끝난 Payment에서 발생한다. 정상 확정으로 settledAt이 찍힌 뒤 지연 콜백이 와서 OrphanApproval이 REFUND_PENDING으로 남으면 T2가 그 건을 영원히 후보로 잡지 못한다. 환불이 영구 미완이 되고 게스트 돈이 PG에 묶인다 | S-1 "T2의 후보 집합은 settledAt이 null인 Payment로 한정된다" vs S-8 "settledAt is null and (... 또는 OrphanApproval REFUND_PENDING 존재)". S-2 "환불 호출이 실패하면 REFUND_PENDING으로 남는다" | 후보 조회를 settledAt is null AND (a or b) OR OrphanApproval REFUND_PENDING 존재로 분리하거나, S-1의 한정 문장을 "(a)(b) 조건에 한한다"로 좁힌다 |
| 치명 | spec-v2 S-8 T2 조회 조건 (a) | 실패 경로 / 경합 | (a)가 "APPROVED 시도 존재"라 쓰였는데 I7은 "승인 이력(APPROVED 또는 REFUNDED)"을 같은 뜻으로 쓴다. 같은 셀 안에서 (b)는 "승인 이력 없음"이라는 다른 용어를 쓴다. 느슨한 해석(REFUNDED 포함)이면 EXPIRED + 환불 완료 건이 settledAt이 영원히 null인 채 매 주기 재후보가 되어, 배치 100 슬롯을 오래된 완료 건이 영구 점유한다. 오래된 시각 오름차순 정렬이므로 신규 미정산 건이 T2에 영영 도달하지 못하고 보정 기능 자체가 마비된다 | 06-4 1-1 I7 "승인 이력(APPROVED 또는 REFUNDED) 시도는 하나를 넘지 않는다". S-8 "APPROVED 시도 존재 and approvedAt <= now - 30초" / "승인 이력 없음". 정렬과 배치는 S-8 "가장 오래된 시각 오름차순, 배치 100" | (a)를 "REFUNDED가 아닌 APPROVED 상태 시도"로 명문화하고, 문서 전체에서 "APPROVED 시도"와 "승인 이력"을 구분 정의한다 |
| 보통 | spec-v2 S-1 ↔ S-8 T2 건별 처리 | 실패 경로 / 표기 일치 | S-1은 정산 표식을 찍는 곳으로 "(라) T2가 처리하는 건별 트랜잭션"을 들었는데, S-8 T2 표의 세 분기(EXPIRED/CANCELED + APPROVED → refund, OrphanApproval 재시도)에는 SettlePayment가 없다. P1의 EXPIRED 분기와 CANCELED 분기도 마찬가지다. 어느 경로가 표식을 찍는지 문서가 두 곳에서 다르게 말한다 | S-1 (가)~(라). 결과 정책 목록 P1 행 "EXPIRED: refund(bookingId)" (SettlePayment 없음). S-8 T2 건별 처리 칸 | 표식을 찍는 분기를 S-8 표에 전수로 명기하고, 환불로 종결된 건의 settledAt 처리 방침을 정한다 |
| 보통 | spec-v2 S-4 ↔ S-8 T1 건별 처리 | 경합 조건 | S-4가 전역 잠금 순서를 "Booking → 재고 N행 → Payment"로 선언했는데, S-8 T1 건별 처리는 "Booking 잠금 → 재검사 → Payment 잠금 후 조회 → 확정 경로(confirm + commit ×N + SettlePayment)"로 Payment를 재고보다 먼저 잠근다. 같은 확정 경로가 P1에서는 재고 → Payment 순, T1에서는 Payment → 재고 순이 된다. 코드로 내려갈 때 어느 쪽을 따를지 문서만으로 정해지지 않는다 | S-4 "전역 잠금 순서: Booking → 재고 N행(날짜 오름차순) → Payment". S-8 T1 건별 처리 칸 | T1의 Payment 조회를 잠금 없는 선판정으로 바꾸고 잠금은 확정 경로 말미의 SettlePayment에서만 잡거나, 전역 순서를 Booking → Payment → 재고로 통일한다 |
| 보통 | spec-v2 S-8 T2 건별 처리 | 멱등 전략 / 경합 조건 | T1 열은 "Payment 잠금 후 조회"를 명시했으나 T2 열은 "Booking 잠금 → 상태 분기"만 적고 Payment 잠금을 적지 않았다. T2 후보 조회는 "잠금 없음"이므로 조회 시점과 처리 시점 사이의 Payment 상태 변화에 대한 방어가 문서에 없다. 다중 인스턴스 방어도 "Booking 잠금 + 종착 무해"인데 OrphanApproval에는 종착 무해 규약이 선언돼 있지 않다 | S-8 "후보 조회 (잠금 없음)", T1 열 "Payment 잠금 후 조회", T2 열 "Booking 잠금 → 상태 분기", "다중 인스턴스: 동일" | T2 건별 처리에도 Payment 잠금과 조건 재검사를 명기하고, OrphanApproval 재시도의 REFUNDED 재호출 무해를 종착 무해 목록에 추가한다 |
| 보통 | spec-v2 S-6 R4 문언 ↔ S-3 | 추적성 / 요구사항 역추적 | S-6은 R4를 "먼저 오는 조건이 발동하며"로 유지했으나, S-3은 TTL이 먼저 와도 미정산 승인이 있으면 만료 대신 확정으로 간다. 즉 "먼저 오는 조건"이 발동하지 않는 경우가 스펙 자신에 의해 생긴다. 02 3-3의 "3회에 도달하기 전에 TTL이 먼저 만료되면 TTL이 이긴다"도 함께 무효인데 스펙이 손대지 않았다 | S-6 "먼저 오는 조건이 발동하며". S-3 "미정산 APPROVED가 있으면 만료 대신 확정 경로로 간다". 02 3-3 | R4 문언에 "단 미정산 승인이 있으면 확정이 우선한다"를 넣고 02 3-3 문장 삭제를 지시한다 |
| 보통 | 04-5 4-1, 04-5 1절 | 이벤트 전수 커버리지 | S-0이 "비동기 리스너는 둘뿐"이라 선언하고 S-4가 취소 환불을 내부 호출로 옮기면서 BookingCanceled의 자동 후속 판정이 "있음"에서 "없음"으로 바뀌었다. 그런데 04-5 4-1의 "구독자 없는 이벤트 17개" 표와 1절의 "자동 반응이 필요한 것은 5개"가 갱신되지 않았다. 스펙 적용 후 실제 수치는 구독자 없는 이벤트 20개, 반응 필요 2개다 | 04-5 1절, 4-1. S-0 "이벤트를 구독하는 비동기 리스너는 둘뿐이다". 스펙 페이로드 표 "BookingConfirmed, BookingCanceled ... 읽는 곳: 없음 (기록)" | 04-5 4-1 표에 BookingConfirmed, BookingExpired, BookingCanceled 세 행을 "불필요(내부 호출)"로 추가하고 수치를 20으로 고친다 |
| 보통 | spec-v2 결과 정책 목록 ↔ 04-5 2절, 4-2 | 추적성 양방향 | 스펙이 정책 번호를 재배정했다. 스펙 P2는 "확정 시 재고 확정"인데 04-5 P2는 "승인 지연 시 자동 환불", 스펙 P3는 "결제 실패 시 만료"인데 04-5 P3는 "확정 시 재고 확정"이다. 04-5 4-2의 "ConfirmBooking(P1), CommitInventory(P3), ExpireBooking(P4, T1), ReleaseInventory(P5, P7), RefundPayment(P2, P6)"가 스펙 체계와 정면으로 어긋난다. 같은 기호가 두 문서에서 다른 정책을 가리킨다 | 04-5 2절 P1~P7, 4-2 발신자 표. 스펙 결과 정책 목록 P1~P6, T1, T2 | 스펙에 번호 재배정표(구 P1~P7 → 신 P1~P6)를 넣고 04-5 4-2 갱신을 명시한다 |
| 보통 | spec-v2 결과 정책 목록 T1/T2 일관성 칸 | 표기 일치 | T1과 T2를 "내부 동기"로 표기했으나 S-3에서 T1은 결제 컨텍스트 조회 API를 부르고 SettlePayment를 보내며, T2의 후보 조회는 Payment의 settledAt, approvedAt, failedAt, OrphanApproval 상태를 직접 읽는다. 컨텍스트를 넘는 정책이 "내부"로 표기됐다. 06-4 2-3의 "내부 정책(같은 컨텍스트 트리거) TTL 만료"도 함께 부정확해진다 | 스펙 T1/T2 일관성 칸 "내부 동기". S-3 "결제 컨텍스트를 조회해". S-8 T2 후보 조회 칸. 06-4 2-3 | T1/T2의 일관성 표기를 "내부 트리거 + 컨텍스트 넘는 동기 호출"로 고치고 06-4 2-3 갱신을 지시한다 |
| 보통 | spec-v2 S-3, S-8 | 결정 근거 자립성 / 추적성 | 예약 → 결제 조회 API의 계약이 문서에 없다. S-3는 "그 bookingId의 Payment에 미정산 APPROVED 시도가 있는지"만 정의했는데, S-8의 T2 후보 조회는 approvedAt, 마지막 failedAt, FAILED 시도 개수, OrphanApproval 상태까지 요구한다. 06-2 CRC의 협력자 목록에도 이 API가 없다 | S-3 "예약 → 결제 방향의 조회 API. 조회 대상: ..." vs S-8 T2 후보 조회 칸. 06-2 6절 BookingApplicationService 행 | 조회 API의 반환 계약을 T2가 필요한 항목 전부로 확장해 명시하고 CRC 협력자에 추가한다 |
| 보통 | spec-v2 S-1 | 추적성 양방향 | SettlePayment는 커맨드로 추가됐으나 대응 이벤트가 없다. 04 1절은 커맨드마다 이벤트를 매핑하는 표 구조이고 스펙은 "이벤트 22" 유지를 선언했다. 이벤트 없는 커맨드가 처음 생기는데 그 근거가 문서에 없다 | 04 1절 커맨드-액터-이벤트 매핑표. 스펙 "커맨드 23 ... 이벤트 22" | SettlePayment가 이벤트를 내지 않는 이유(내부 표식이라 도메인 사건이 아님 등)를 명시하고 04 1절 표의 이벤트 칸 처리를 정한다 |
| 보통 | spec-v2 S-2 recordApproval | 추적성 양방향 | 기존 recordApproval Pre의 "승인 이력 없음"과 예외 칸의 AlreadyApproved, NoRequestedAttempt를 유지하는지 삭제하는지 스펙이 적지 않았다. S-2는 attemptId 기준 세 분기만 서술하고 "AttemptAlreadyClosed 같은 거부 예외는 두지 않는다"고만 했다. 06-4 1-2의 예외 칸이 어떻게 되는지 문서만으로 정해지지 않는다 | 06-4 1-2 recordApproval 행 "잠금. 같은 거래번호는 무해 무시(U4). REQUESTED 존재, 승인 이력 없음 / 예외 NoRequestedAttempt, AlreadyApproved". S-2 | recordApproval, recordFailure 행을 예외 칸까지 포함해 통째로 재작성한 표를 스펙에 싣는다 |
| 보통 | spec-v2 S-5 | 결정 근거 자립성 | 같은 애그리거트의 두 전이 메서드가 비대칭 규약을 갖는다. confirm은 EXPIRED/CANCELED에 InvalidStateTransition을 던지고, expire는 CONFIRMED/CANCELED에 예외 없이 스킵한다. "분기 주체는 앱 서비스"라는 원칙을 세웠으면서 confirm에만 방어적 예외를 남긴 이유가 문서에 없다 | S-5 "confirm() 예외 칸: EXPIRED와 CANCELED는 InvalidStateTransition" / "expire(reason): ... CONFIRMED와 CANCELED는 예외가 아니라 전이 없음 반환(스킵)" | 비대칭의 근거(스케줄러가 부르는 expire는 경합이 정상, 확정은 호출자 버그)를 한 줄로 적거나 규약을 통일한다 |
| 보통 | 06-2 4절 | 델타 미기재 | S-4가 취소 환불을 같은 트랜잭션 동기로 바꿨는데, 06-2 4절의 "취소의 RefundPayment(P6)는 비동기 (06-4 2-2)" 문장에 대한 삭제 지시가 없다. S-11은 같은 절의 다른 문장만 삭제하라고 지시해, 삭제 대상을 골라서 적었으면서 이 문장을 빠뜨린 것으로 읽힌다 | 06-2 4절. S-4 "cancel() Post: 같은 트랜잭션에서 ... refund(bookingId)". S-11 "06-2 4절의 ... 문장은 삭제한다" | S-4에 06-2 4절 해당 문장의 교체를 명시한다 |
| 보통 | 06-4 3절 | 요구사항 역추적 | 역추적표에 R1~R5만 있고 R6 행이 없다. 스펙은 S-2에서 R6의 핵심(중복 콜백이 환불이나 이중 확정을 만들지 않음)을 크게 바꾸면서도 R6 행을 추가하지 않았다. 문서만 보고 R6가 어느 불변식, 정책으로 내려갔는지 짚을 수 없다 | 06-4 3절 표 (R1~R5). 00-input-pack 2절 R6. S-2 "I7(승인 유일)의 셈에서 OrphanApproval은 제외한다" | 06-4 3절에 "R6 → I7, U4, U5, S-2 고아 승인 경로, P1 가드 분기" 행을 추가한다 |
| 보통 | spec-v2 전반 | 추적성 양방향 | 새 개념 6종(settledAt/settledBy, attemptId, OrphanApproval, U5, SettlePayment, T2)이 어느 기반 문서 어느 절을 바꾸는지 스펙이 대부분 적지 않았다. 절을 명시한 곳은 S-4(06-4 0절), S-7(06-4 1-3), S-11(06-2 4절) 셋뿐이다. 나머지는 위 델타 표의 40여 항목을 독자가 스스로 추론해야 한다 | 델타 표의 "스펙이 스스로 적었나" 칸 | 스펙 말미에 문서별 반영 지점 목록을 붙인다 |
| 보통 | 04-5 6절, 4-3 | 추적성 양방향 | "Step 8에 넘긴 것" 4항목(가드 확정, Commit/Release 중복 방지, 결제 시도 타임아웃, ExpireBooking 중복 거부 의미론)이 스펙에서 전부 종결됐는데 종결 표시가 없다. 4-3 유형2의 "거부가 오류인지 무시인지는 미정"도 S-5가 무해 무시로 확정했으나 미갱신 | 04-5 6절, 4-3 유형2. S-5, S-7 | 04-5 6절 이월 목록에 종결 표시와 근거 조항(S-5, S-7)을 적는다 |
| 확인필요 | spec-v2 S-1 ↔ S-3 | 표기 일치 | settledAt은 Payment 단위 필드인데 S-3와 S-8은 "미정산 APPROVED 시도"라고 시도 단위로 쓴다. U3(bookingId당 Payment 하나)과 I7(승인 시도 하나) 때문에 실질 동치로 보이나, OrphanApproval이 생긴 뒤에도 동치인지는 문서만으로 판단이 안 선다 | S-1 "Payment에 settledAt ... 필드를 둔다". S-3 "미정산 APPROVED 시도가 있는지" | 정산 표식의 단위를 Payment로 못박고 "미정산 APPROVED 시도"를 "settledAt이 null인 Payment의 APPROVED 시도"로 표현 통일 |
| 확인필요 | spec-v2 S-8 T2 건별 처리 | 실패 경로 | T1 열은 "HELD와 expiresAt 재검사"를 명시했으나 T2 열은 조회 조건의 재검사 유무를 적지 않았다. 후보 조회가 잠금 없이 이뤄지므로 (b)의 "FAILED 시도 3개 and 승인 이력 없음"을 처리 시점에 다시 보는지 알 수 없다 | S-8 T1 열 vs T2 열 | T2 건별 처리에 조회 조건 재검사를 명시하거나 재검사가 불필요한 이유를 적는다 |
| 확인필요 | spec-v2 S-2 | 실패 경로 | 고아 승인의 환불이 recordApproval과 같은 트랜잭션인데, 환불 호출 실패 시 OrphanApproval 기록이 롤백되는지 REFUND_PENDING으로 커밋되는지 문서가 정하지 않았다. 롤백되면 (c) 후보에 잡히지 않아 영구 미환불이고, 남으면 U4의 pgTransactionId 기록도 함께 남는다 | S-2 "같은 트랜잭션에서 Mock PG 환불을 호출해 REFUNDED로 닫는다. 환불 호출이 실패하면 REFUND_PENDING으로 남는다" | 환불 실패를 잡아 REFUND_PENDING 상태로 커밋시킨다는 것을 명시한다 |
| 확인필요 | spec-v2 S-8 T2 "CANCELED + APPROVED → refund" | 실패 경로 | S-4에서 cancel이 같은 트랜잭션에서 refund를 하고 실패 시 취소 자체가 롤백되므로, CANCELED이면서 APPROVED(미환불) 시도가 남는 조합이 어떻게 생기는지 문서만으로는 알 수 없다. 도달 불가 분기일 가능성이 있다 | S-4 "환불 호출이 실패하면 트랜잭션 롤백, 취소 자체가 실패한다". S-8 T2 "EXPIRED 또는 CANCELED + APPROVED → refund(bookingId)" | 이 분기의 도달 시나리오를 적거나 EXPIRED만 남긴다 |
| 확인필요 | spec-v2 S-1 | 결정 근거 자립성 | settledBy를 4값(APPROVAL_HANDLER, FAILURE_HANDLER, T1, T2)으로 두는 이유와 이 값을 읽는 곳이 문서에 없다. 분기에 쓰이는지 관측용인지 불명이다 | S-1 "settledBy(APPROVAL_HANDLER, FAILURE_HANDLER, T1, T2 중 하나) 필드를 둔다" | 용도(운영 관측, 보정 비율 계측 등)를 한 줄 적는다 |
| 확인필요 | spec-v2 S-8 값 순서 제약 | 결정 근거 자립성 | "T2 지연 임계 30초 < T2 주기 60초 < TTL 10분" 제약에 T1 주기 30초가 들어 있지 않다. T1 주기와 T2 지연 임계가 같은 30초인 것이 의도인지, 두 값의 관계에 제약이 필요한지 근거가 없다 | S-8 주기 [가설] 칸, 값 순서 제약 문장 | T1 주기를 제약식에 포함시키고 각 부등호의 이유를 적는다 |
| 확인필요 | spec-v2 S-9 ↔ 이벤트 페이로드 표 | 추적성 양방향 | S-9가 객실 수를 1 고정으로 하고 roomCount 필드를 두지 않는데, 페이로드 표는 BookingConfirmed/Expired/Canceled에 "날짜별 수량"을 필수로 요구한다. 항상 1인 값을 왜 페이로드에 싣는지, 이 수량이 객실 수인지 다른 것인지 불명이다 | S-9. 이벤트 페이로드 표 "bookingId, 날짜별 수량" | 수량의 의미를 정의하거나 v1에서 생략한다 |

#### 요구사항 역추적표 (H)

| 요구사항 | 문서에서 대응되는 항목 | 커버 여부 |
|---|---|---|
| R1 | A1 연박 원자성(06-4 1-1, 06-2 3-3), hold(n) 계약의 전체 롤백(06-4 1-2), RequestBooking Invariant, InventoryAllocationService(06-2 6절). 스펙 무영향 | 커버 |
| R2 | I1, I1a(06-4 1-1), 06-4 0절 락 선언, hold/commit의 잠금 후 검사, 06-2 5절 오름차순 잠금, S-4 전역 잠금 순서 | 커버. 단 S-3/S-8 T1 절차가 전역 순서와 어긋남(상세 4행) |
| R3 | U1 멱등키 유일(06-4 1-1, 06-2 3-4), RequestBooking 멱등 반환(06-4 0절, 1-2), S-10 종착 대상 확장 | 커버 |
| R4 | A2 종료 반환, expire(reason) 계약, 06-4 1-3 예약 전이표, P3와 T1, S-6 T2 보정 | 부분 커버. "먼저 오는 조건"이 S-3 확정 우선으로 깨지는데 S-6 문언이 이를 반영하지 않음(상세 6행). 재고 반환 자체는 보장됨 |
| R5 | I4 스냅샷 동결, I15 총액 합치, adjustRate와 프로모션 update/close의 스냅샷 무영향, openAttempt의 총액 대조(06-4 3절). 스펙 무영향 | 커버 |
| R6 | I7 승인 유일, U4 콜백 멱등, U5 attemptId 유일(S-2), S-2 고아 승인 경로, P1의 4분기 가드 | 미커버. 06-4 3절 역추적표에 R6 행 자체가 없고 스펙도 추가하지 않았다. 나아가 치명 1행에 의해 고아 승인 환불이 영구 미완이 될 수 있어 "중복 콜백이 환불을 만들지 않음"의 뒤처리가 완결되지 않는다 |

## 4. 에이전트 I 상세 (구현자 워크스루, 원문 그대로)

판정 요약: 치명 6 / 보통 15 / 확인필요 11
- 치명: [1] 리스너 catch 위치, [13] 잠금 순서 역전, [17] REFUNDED 포함 여부, [21] T2의 Payment 잠금 부재, [28] I7 검사 소실, [30] 고아 환불 실패 시 트랜잭션
- 보통: [3] [4] [6] [9] [10] [11] [14] [16] [19] [20] [22] [23] [24] [26] [31]
- 확인필요: [2] [5] [7] [8] [12] [15] [18] [25] [27] [29] [32]

#### 의사코드

경로 1. PaymentApproved 리스너 (P1)

```java
@Component
class PaymentApprovedListener {

  @TransactionalEventListener(phase = AFTER_COMMIT)      // S-0
  @Transactional(propagation = REQUIRES_NEW)             // S-0
  public void on(PaymentApproved e) {
    try {                                                // ??? [1] try가 트랜잭션 안이면 롤백이 안 걸린다
      Booking b = bookingRepo.lockById(e.bookingId());   // 전역 순서 1) Booking (S-4)
      // ??? [2] Booking이 없으면(조회 실패) 무엇을 하는가

      switch (b.status()) {
        case HELD -> {
          b.confirm();                                   // S-5: 앱 서비스가 분기, HELD일 때만 호출
          inventoryAlloc.commitAll(b.roomTypeId(), b.dates(), 1);  // 2) 재고 N행 오름차순, S-9로 n=1
          paymentPort.settle(b.id(), APPROVAL_HANDLER);  // 3) Payment (S-1 (가))
        }
        case EXPIRED -> {
          paymentPort.refund(b.id());                    // ??? [3] 이 분기에 SettlePayment를 찍는가
          // ??? [4] refund가 예외를 던지면 롤백인가 삼킴인가 (S-4는 취소 경로만 규정)
        }
        case CONFIRMED -> {
          if (paymentPort.isUnsettled(b.id()))           // ??? [5] 이 조회에 Payment 잠금이 필요한가
            paymentPort.settle(b.id(), /* ??? [6] */ APPROVAL_HANDLER);
        }
        case CANCELED -> {
          if (paymentPort.hasUnsettledApproved(b.id()))  // ??? [7] "미정산 APPROVED"가 Payment 단위인가 시도 단위인가
            paymentPort.refund(b.id());
        }
      }
    } catch (Exception ex) {
      log.error("P1 실패 bookingId={}", e.bookingId(), ex);   // S-0: 로그 후 삼킨다
    }
    // ??? [8] 이벤트의 attemptId, pgTransactionId, attemptCount를 P1은 전혀 쓰지 않는다
  }
}
```

경로 2. PaymentFailed 리스너 (P3)

```java
@Component
class PaymentFailedListener {

  @TransactionalEventListener(phase = AFTER_COMMIT)
  @Transactional(propagation = REQUIRES_NEW)
  public void on(PaymentFailed e) {
    try {
      if (e.attemptCount() < 3) return;                  // 가드: 이벤트 탑재값 (스펙 정책표 P3)

      Booking b = bookingRepo.lockById(e.bookingId());   // 1) Booking
      Transition t = b.expire(PAYMENT_FAILED);           // S-5: HELD면 전이, EXPIRED면 무해,
                                                         //      CONFIRMED/CANCELED는 전이 없음 반환
      if (t == TRANSITIONED) {
        inventoryAlloc.releaseHeldAll(b.roomTypeId(), b.dates(), 1);  // 2) 재고
        paymentPort.settle(b.id(), FAILURE_HANDLER);     // 3) Payment (S-1 (나))
      } else {
        // ??? [9] 전이가 없었을 때(이미 EXPIRED / CONFIRMED / CANCELED) SettlePayment를 찍는가
        //         안 찍으면 settledAt이 null로 남아 T2 후보에 영구히 재등장한다
      }
    } catch (Exception ex) {
      log.error("P3 실패", ex);                          // ??? [1]과 같은 문제
    }
    // ??? [10] 앱 서비스가 분기 주체(S-5)라면 expire()를 부르기 전에 상태를 읽어 분기해야 하는데,
    //          S-5는 expire의 반환 규약만 주고 "expire는 HELD일 때만 부른다"고는 하지 않는다
    // ??? [11] 가드를 이벤트 탑재값으로만 볼지, Payment를 다시 읽어 확인할지
    //          (S-5는 애그리거트 Pre의 재확인을 삭제하고 "재확인은 정책 가드에서만"이라고 한다)
    // ??? [12] attemptCount가 3인데 승인 이력이 있는 Payment(불가능해야 하지만)에서의 동작
  }
}
```

경로 3. T1 스케줄러 건별 처리

```java
@Scheduled(fixedDelay = 30_000)                          // S-8 [가설]
public void runT1() {
  List<BookingId> ids = bookingRepo.findExpiryCandidates(now(), PageRequest.of(0, 100));
  for (BookingId id : ids) {
    try { t1Handler.handleOne(id); }
    catch (Exception ex) { log.warn("T1 건 실패 {}", id, ex); }   // 로그 후 다음 건
  }
}

@Transactional(propagation = REQUIRES_NEW)               // 건별 트랜잭션
void handleOne(BookingId id) {
  Booking b = bookingRepo.lockById(id);                  // 1) Booking
  if (b.status() != HELD || b.expiresAt().isAfter(now())) return;   // 재검사

  // ??? [13] S-8은 여기서 "Payment 잠금 후 조회"라고 하는데, 확정 경로로 가면 그 뒤에 재고를 잠근다.
  //          S-4의 전역 순서는 Booking → 재고 → Payment 이므로 순서가 뒤집힌다.
  boolean hasUnsettledApproved = paymentPort.lockAndHasUnsettledApproved(b.id());

  if (hasUnsettledApproved) {
    b.confirm();
    inventoryAlloc.commitAll(b.roomTypeId(), b.dates(), 1);
    paymentPort.settle(b.id(), T1);                      // S-1 (다)
  } else {
    b.expire(TTL_EXPIRED);                               // Pre에 expiresAt 경과 재검사 유지
    inventoryAlloc.releaseHeldAll(b.roomTypeId(), b.dates(), 1);
    // ??? [14] 이 만료 경로에는 SettlePayment가 없다. FAILED 1~2개만 있는 Payment는
    //          settledAt이 영원히 null이지만 T2 조회 조건 셋 중 어디에도 안 걸린다.
    //          그것이 의도인지, 아니면 여기서도 표식을 찍어야 하는지
  }
  // ??? [15] Payment 자체가 없는 경우(결제 요청 전 TTL 만료) 조회 API의 응답 규약
  // ??? [16] T1이 확정 경로로 갈 때 P1과 같은 코드를 쓴다면(S-3), P1의 EXPIRED/CONFIRMED/CANCELED
  //          분기와 settledBy 값(APPROVAL_HANDLER vs T1)을 어떻게 공유하는가
}
```

경로 4. T2 후보 조회와 건별 처리

```java
// 후보 조회 (잠금 없음)
@Query("""
  SELECT p FROM Payment p
   WHERE p.settledAt IS NULL
     AND ( EXISTS (SELECT 1 FROM PaymentAttempt a WHERE a.payment = p
                     AND a.status = 'APPROVED'                       /* ??? [17] REFUNDED 포함? */
                     AND a.approvedAt <= :threshold)
        OR ( (SELECT COUNT(f) FROM PaymentAttempt f
               WHERE f.payment = p AND f.status = 'FAILED') = 3      /* ??? [18] =3 인가 >=3 인가 */
             AND NOT EXISTS (SELECT 1 FROM PaymentAttempt g WHERE g.payment = p
                               AND g.status IN ('APPROVED','REFUNDED'))
             AND (SELECT MAX(f2.failedAt) FROM PaymentAttempt f2
                   WHERE f2.payment = p AND f2.status='FAILED') <= :threshold )
        OR EXISTS (SELECT 1 FROM OrphanApproval o WHERE o.payment = p
                     AND o.status = 'REFUND_PENDING') )              /* ??? [19] 고아엔 지연 임계 없음 */
  ORDER BY /* ??? [20] "가장 오래된 시각"을 셋 중 무엇으로 만드나 */
""")

@Transactional(propagation = REQUIRES_NEW)
void handleOne(PaymentId pid) {
  BookingId bid = paymentPort.bookingIdOf(pid);
  Booking b = bookingRepo.lockById(bid);                 // 1) Booking. ??? [13]과 같은 순서 문제
  PaymentView pv = paymentPort.lockAndLoad(pid);         // ??? [21] T2 표엔 Payment 잠금 언급이 없다

  if (b.status()==HELD && pv.hasApproved()) {
    b.confirm(); inventoryAlloc.commitAll(...); paymentPort.settle(bid, T2);
  } else if (b.status()==HELD && pv.failedCount()>=3) {
    b.expire(PAYMENT_FAILED); inventoryAlloc.releaseHeldAll(...); paymentPort.settle(bid, T2);
  } else if ((b.status()==EXPIRED || b.status()==CANCELED) && pv.hasApproved()) {
    paymentPort.refund(bid);
    // ??? [22] settle이 없다. S-1 (라)는 "T2가 처리하는 건별 트랜잭션"에 표식을 찍는다고 했는데
    //          이 분기와 아래 고아 분기에는 SettlePayment가 없어 매 주기 재후보가 된다
  } else if (b.status()==CONFIRMED && pv.hasApproved()) {
    paymentPort.settle(bid, T2);
  }
  if (pv.hasRefundPending()) paymentPort.retryOrphanRefund(pid);   // 재시도 상한도 침묵
}
```

경로 5. CancelBooking 앱 서비스

```java
@Transactional
public void cancelBooking(CancelBooking cmd) {
  Booking b = bookingRepo.lockById(cmd.bookingId());     // 1) Booking (S-4 전역 순서)

  // S-5: 분기 주체는 앱 서비스
  if (b.status() == CANCELED) return;                    // 종착 무해
  if (b.status() != CONFIRMED) {
    throw new /* ??? [23] */ InvalidStateTransition();   // S-5는 confirm/expire의 예외 칸만
                                                          // 재정의하고 cancel은 침묵.
                                                          // 06-4는 애그리거트가 던진다고 한다
  }

  Transition t = b.cancel();
  if (t == NO_TRANSITION) return;

  inventoryAlloc.releaseSoldAll(b.roomTypeId(), b.dates(), 1);   // 2) 재고 (P6)
  paymentPort.refund(b.id());                            // 3) Payment (P5, S-4: 같은 트랜잭션)
  // ??? [24] refund Pre는 "APPROVED 존재", 없으면 NoApprovedAttempt.
  //          예외가 나면 S-4에 따라 취소 전체가 실패한다. 그것이 의도인가
  // ??? [25] Mock PG 환불은 성공했는데 뒤에서 DB가 롤백되는 경우의 보정 경로가 없다
  //          (T2 후보 조건에도 "CANCELED인데 환불 안 됨"은 settledAt이 null일 때만 잡힌다)

  // 취소 경로에는 SettlePayment가 없다 (S-1 열거 4곳에 없음)
  eventPublisher.publish(new BookingCanceled(b.id(), b.dailyCounts()));  // 커밋 후 발행
}
```

경로 6. Payment.recordApproval

```java
// 시그니처는 애그리거트 메서드인데 Mock PG 환불 호출이 필요하다
// ??? [26] 이 메서드가 애그리거트에 있는가(S-2 문면), 앱 서비스가 분기하는가(S-5 "분기 주체는 앱 서비스")
public Transition recordApproval(UUID attemptId, String pgTxId, Money amount, Instant receivedAt) {
  // Pre: 잠금은 호출자가 이미 잡았다고 가정
  if (attempts.anyWithPgTxId(pgTxId) || orphans.anyWithPgTxId(pgTxId))
    return NO_TRANSITION;                                // U4 무해 무시
  // ??? [27] 두 테이블에 걸친 pgTransactionId 유일성을 DB 유니크로 어떻게 강제하는가

  PaymentAttempt a = attempts.byAttemptId(attemptId)
      .orElseThrow(UnknownAttempt::new);                 // S-2

  if (a.status() == REQUESTED) {
    // ??? [28] 다른 시도에 이미 APPROVED/REFUNDED가 있으면? 06-4의 "승인 이력 없음" Pre와
    //          AlreadyApproved 예외가 S-2에서 재서술될 때 빠졌다. I7을 여기서 지키는가
    // ??? [29] amount와 this.amount 불일치 검사가 recordApproval 계약에 없다(openAttempt에만 있음)
    a.approve(pgTxId, receivedAt);
    register(new PaymentApproved(bookingId, id, attemptId,
                                 attempts.size(), pgTxId, a.approvedAt()));  // 커밋 후 발행
    return TRANSITIONED;
  }

  // 종착(FAILED, APPROVED, REFUNDED) → 고아 승인
  OrphanApproval o = new OrphanApproval(attemptId, pgTxId, amount, receivedAt, REFUND_PENDING);
  orphans.add(o);
  try {
    pgClient.refund(pgTxId, amount);                     // 같은 트랜잭션 안에서 동기 호출
    o.markRefunded();
    register(new PaymentRefunded(bookingId, id, ORPHAN, pgTxId));
  } catch (Exception ex) {
    log.warn("고아 환불 실패, REFUND_PENDING 유지", ex);
    // ??? [30] 여기서 예외를 삼켜야 OrphanApproval이 커밋된다. 던지면 롤백되어
    //          고아 기록 자체가 사라지고 같은 콜백이 다시 오면 U4도 못 막는다. 문서는 침묵
  }
  return /* ??? [31] */ NO_TRANSITION;                   // S-5의 두 값 중 고아 경로는 어느 쪽인가
  // ??? [32] 고아 승인은 PaymentApproved를 안 내므로 이 Payment의 settledAt은 그대로다.
  //          settledAt이 이미 찍힌 Payment에 고아가 생기면 T2 후보에서 빠져 REFUND_PENDING이 방치된다
}
```

#### 임의 결정 지점 표

| 번호 | 경로 | 지점 | 침묵 / 어긋남 | 어긋나는 경우 인용 | 임의로 고르면 무엇이 달라지나 |
|---|---|---|---|---|---|
| 1 | 1, 2 | 리스너의 try/catch 위치와 REQUIRES_NEW 롤백의 관계 | 어긋남 (같은 절 두 문장) | S-0 "리스너는 자기 트랜잭션을 REQUIRES_NEW로 연다." / S-0 "리스너 예외는 로그 후 삼킨다." | catch를 트랜잭션 안(메서드 안)에 두면 confirm은 됐는데 commit ×N이 실패해도 커밋된다 → 예약 CONFIRMED + 재고 heldCount 잔존. A3와 R2가 깨진다. 밖(래퍼)에 두면 전체 롤백 후 T2 보정 |
| 2 | 1 | e.bookingId()로 Booking을 못 찾을 때 | 침묵 | - | 예외로 던져 삼키면 T2가 잡고, 무시하면 아무 데도 안 잡힌다 |
| 3 | 1 | P1 EXPIRED 분기에 SettlePayment를 찍는가 | 어긋남 | S-1 "정산 표식은 다음 트랜잭션 안에서 같은 트랜잭션으로 찍는다. (가) 승인 처리 정책이 HELD 예약을 확정하는 트랜잭션." / 결과 정책 표 P1 "EXPIRED: refund(bookingId)." (표식 없음) | 안 찍으면 settledAt이 null로 남아 매 60초마다 T2가 같은 건을 재환불 시도(무해하지만 무한 폴링). 찍으면 S-1 열거 4곳을 벗어난다 |
| 4 | 1 | P1 EXPIRED 분기의 refund 실패 처리 | 침묵 | - | 던지면 롤백 후 T2 재시도, 삼키면 그 자리에서 끝. S-4는 취소 경로의 롤백만 규정한다 |
| 5 | 1 | "미정산인지" 조회에 Payment 잠금이 필요한가 | 침묵 | - | 잠금 없이 읽고 settle을 부르면 T2와 이중 settle. settle이 무해라 결과는 같지만 settledBy가 경쟁으로 뒤집힌다 |
| 6 | 1 | CONFIRMED 분기의 settledBy 값 | 어긋남 | S-1 "settledBy(APPROVAL_HANDLER, FAILURE_HANDLER, T1, T2 중 하나)" / S-1 "(가) 승인 처리 정책이 HELD 예약을 확정하는 트랜잭션" | CONFIRMED 분기는 열거에 없어 APPROVAL_HANDLER를 쓸 근거가 없다. 값 선택에 따라 운영 조회의 원인 추적이 달라진다 |
| 7 | 1, 3, 4 | "미정산 APPROVED"의 단위 | 침묵 | - | settledAt은 Payment 필드인데 문장은 "미정산 APPROVED 시도"다. 시도 단위로 읽으면 시도마다 settledAt이 필요해 스키마가 달라진다 |
| 8 | 1 | PaymentApproved의 attemptId/pgTransactionId를 P1이 쓰는가 | 침묵 | - | 안 쓰면 페이로드가 잉여. 쓴다면 refund가 그 시도를 지정해야 하는데 refund 시그니처는 bookingId뿐(S-4) |
| 9 | 2 | expire가 전이 없음을 반환했을 때 SettlePayment 여부 | 침묵 | - | 안 찍으면 CONFIRMED/CANCELED로 이미 넘어간 3회 실패 건이 settledAt null로 남는다. 다만 T2의 두 번째 조건이 "승인 이력 없음"이라 실제로 걸릴지는 문서로 확정 불가 |
| 10 | 2 | expire를 부르기 전 앱 서비스가 상태로 분기하는가 | 어긋남 | S-5 "분기 주체는 앱 서비스다. 앱 서비스가 Booking을 잠근 뒤 상태를 읽어 분기하고, confirm()은 HELD일 때만 부른다." / S-5 "expire(reason): HELD면 전이. EXPIRED 재호출 무해. CONFIRMED와 CANCELED는 예외가 아니라 전이 없음 반환(스킵)." | confirm만 앱 서비스 분기, expire는 애그리거트 자체 분기로 읽힌다. 어느 쪽이든 결과는 같지만 SettlePayment를 어디서 부를지가 갈린다 |
| 11 | 2 | 3회 가드를 이벤트값으로만 볼지 재조회할지 | 어긋남 | S-5 "Pre의 '원인이 결제 실패면 이벤트 탑재 시도 수 3 이상 재확인'은 삭제한다(재확인은 정책 가드에서만)." / 결과 정책 표 P3 "시도 수 3 이상 (이벤트 탑재값)" | "정책 가드에서만 재확인"은 정책이 Payment를 다시 읽으라는 뜻으로도, 이벤트값을 쓰라는 뜻으로도 읽힌다. 재조회하면 결제→예약 역방향 조회가 하나 더 생긴다 |
| 12 | 2 | attemptCount == 3인데 승인 이력이 있는 경우 | 침묵 | - | 발생 불가라고 보고 방어를 빼면, 고아 승인 경로로 상태가 꼬였을 때 정상 결제 건이 만료된다 |
| 13 | 3, 4 | 잠금 순서: Payment가 재고보다 먼저 | 어긋남 (치명) | S-4 "전역 잠금 순서: Booking → 재고 N행(날짜 오름차순) → Payment." / S-8 T1 건별 처리 "Booking 잠금 → HELD와 expiresAt 재검사 → Payment 잠금 후 조회 → 미정산 APPROVED면 확정 경로(confirm + commit ×N + SettlePayment(T1))" | S-8대로 짜면 T1은 Booking→Payment→재고, P1은 Booking→재고→Payment가 되어 두 트랜잭션이 교차 대기한다. Booking 잠금이 앞서므로 같은 예약끼리는 직렬화되지만, 재고 행을 공유하는 다른 예약의 P1과 이 T1 사이에서 데드락이 성립한다. 어느 쪽을 따르느냐로 R2/R4의 보장 여부가 갈린다 |
| 14 | 3 | T1 만료 경로에 SettlePayment가 없다 | 어긋남 | S-1 "정산 표식은 ... (다) T1이 확정 경로로 가는 트랜잭션." / 결과 정책 표 T1 "expire(TTL_EXPIRED) + 선점 반환. 미정산 승인이 있으면 P1 확정 경로" | 만료 경로에서 안 찍으면 그 Payment는 영구 미정산이다. T2 조회 셋 중 어디에도 안 걸린다고 읽으면 무해하지만, 문서에 "미정산으로 남겨도 된다"는 문장이 없어 판단 근거가 없다 |
| 15 | 3 | 조회 대상 Payment가 없을 때 조회 API 규약 | 침묵 | - | 예외로 처리하면 T1이 결제 전 예약을 만료시키지 못해 R4가 깨진다. false로 처리하면 정상 |
| 16 | 3 | T1 확정 경로와 P1의 코드 공유 방식 | 침묵 | - | S-3은 "승인 처리 정책과 같은 코드"라고 하는데, P1은 4분기 switch고 T1은 HELD 확정만이다. 통째로 재사용하면 T1이 EXPIRED 분기의 환불까지 하게 된다 |
| 17 | 4 | T2 조회의 "APPROVED 시도 존재"에 REFUNDED가 포함되는가 | 어긋남 (치명) | S-8 T2 후보 조회 "APPROVED 시도 존재 and approvedAt <= now - 30초" / 06-4 1-1 I7 "승인 이력(APPROVED 또는 REFUNDED) 시도는 하나를 넘지 않는다" | 포함하면 P1 EXPIRED 분기가 환불을 마친 건이 영원히 T2 후보로 남아(표식 없음, [3]) 매 주기 재환불 시도가 돈다. 제외하면 반대로 "환불이 실패해 APPROVED로 남은 건"만 잡히므로 정상이지만, 문서에 REFUNDED 취급을 정한 문장이 없다 |
| 18 | 4 | FAILED 3개가 정확히 3인가 3 이상인가 | 침묵 | - | I6이 상한 3이라 실무상 같지만, 데이터가 깨졌을 때 잡히느냐가 갈린다 |
| 19 | 4 | OrphanApproval REFUND_PENDING에 지연 임계가 없다 | 침묵 | - | recordApproval 트랜잭션이 방금 커밋한 건을 T2가 즉시 다시 환불 호출한다. Mock PG 이중 환불 호출이 되는지 여부가 문서로 확정 불가 |
| 20 | 4 | ORDER BY의 정렬 키 | 침묵 | - | "가장 오래된 시각"이 approvedAt / MAX(failedAt) / 고아 receivedAt 중 무엇의 최소값인지 없다. 배치 100 경계에서 어떤 건이 먼저 처리되는지가 달라진다 |
| 21 | 4 | T2 건별 처리에 Payment 잠금 언급이 없다 | 어긋남 | S-8 T1 건별 처리 "Booking 잠금 → ... → Payment 잠금 후 조회" / S-8 T2 건별 처리 "Booking 잠금 → 상태 분기: HELD + APPROVED → confirm + commit ×N + SettlePayment(T2)." | T2가 Payment를 안 잠그면 P1 리스너와 동시에 읽고 이중 confirm 시도. confirm 재호출 무해가 막지만 commit ×N이 두 번 나갈 수 있다(전이 없음 반환으로 막힌다고 읽어야 안전). R2 성립 여부가 여기 달렸다 |
| 22 | 4 | T2의 EXPIRED/CANCELED 환불 분기와 고아 재시도 분기에 SettlePayment가 없다 | 어긋남 | S-1 "정산 표식은 ... (라) T2가 처리하는 건별 트랜잭션." / S-8 T2 건별 처리 "EXPIRED 또는 CANCELED + APPROVED → refund(bookingId)" | S-1을 따라 모든 T2 분기에 찍으면 고아 REFUND_PENDING이 남은 채로 settledAt이 찍혀 후보에서 영원히 빠진다([32]와 충돌). S-8을 따르면 무한 재후보다. 둘 다 문제이므로 임의 선택이 강제된다 |
| 23 | 5 | cancel의 비CONFIRMED 상태 예외 주체와 이름 | 어긋남 | 06-4 1-2 예약 "cancel() ... HELD와 EXPIRED는 InvalidStateTransition" / S-5 "분기 주체는 앱 서비스다. 앱 서비스가 Booking을 잠근 뒤 상태를 읽어 분기하고" | 애그리거트에 두면 S-5의 분기 주체 선언과 어긋나고, 앱 서비스에 두면 06-4의 예외 칸이 죽은 코드가 된다. HTTP 상태 코드 매핑 위치가 갈린다 |
| 24 | 5 | refund가 NoApprovedAttempt를 던질 때 취소 전체 실패 여부 | 어긋남 | S-4 "환불 호출이 실패하면 트랜잭션 롤백, 취소 자체가 실패한다(CancelBooking이 오류로 끝난다)." / 06-4 1-2 결제 "refund: 잠금. APPROVED 존재. REFUNDED 재호출 무해 / NoApprovedAttempt" | "환불 호출 실패"가 PG 통신 실패만인지 선행조건 위반까지인지 불명. 후자로 읽으면 승인 이력 없는(그러나 CONFIRMED인) 예약을 영원히 취소할 수 없다 |
| 25 | 5 | PG 환불 성공 후 DB 롤백된 경우의 보정 | 침묵 | - | 보정 경로가 없다. T2는 settledAt이 null인 Payment만 보는데 취소 경로는 표식을 안 찍으므로 조건 셋 중 무엇에 걸릴지 문서로 결정 불가 |
| 26 | 6 | recordApproval이 애그리거트인가 앱 서비스인가 | 어긋남 | S-2 "OrphanApproval(...)을 Payment 안에 기록하고, 같은 트랜잭션에서 Mock PG 환불을 호출해 REFUNDED로 닫는다." / S-5 "분기 주체는 앱 서비스다." (및 06-2 6절 CRC Payment "승인과 실패를 기록한다 / PaymentAttempt") | 애그리거트에 두면 도메인이 PG 클라이언트를 의존하고, 앱 서비스로 빼면 요청된 시그니처 Payment.recordApproval(...)이 성립하지 않는다 |
| 27 | 6 | PaymentAttempt와 OrphanApproval에 걸친 pgTransactionId 유일성 | 침묵 | - | U4를 "선행조건 + DB 유니크"로 강제한다는데(06-2 3-4) 두 테이블에 걸친 유니크는 단일 제약으로 못 만든다. 애플리케이션 검사만 두면 동시 중복 콜백에서 R6이 깨진다 |
| 28 | 6 | REQUESTED 시도를 승인할 때 I7(승인 유일) 검사 | 어긋남 (치명) | 06-4 1-2 결제 "recordApproval: 잠금. 같은 거래번호는 무해 무시(U4). REQUESTED 존재, 승인 이력 없음 / 승인 유일 / NoRequestedAttempt, AlreadyApproved" / S-2 "attemptId로 특정한 시도가 REQUESTED이면: APPROVED로 전이, pgTransactionId와 approvedAt 저장, PaymentApproved 발행." | S-2가 Pre를 다시 쓰면서 "승인 이력 없음"과 AlreadyApproved를 언급하지 않는다. 빼면 다른 시도가 APPROVED인 상태에서 또 승인이 들어와 I7과 R6이 깨진다. 남기면 S-2의 "거부 예외는 두지 않는다"와 마찰 |
| 29 | 6 | 승인 금액과 청구액 대조 | 침묵 | - | openAttempt에만 AmountMismatch가 있다. 대조를 안 하면 금액이 다른 승인을 그대로 받아 R5의 의미가 흔들린다 |
| 30 | 6 | 고아 환불 실패 시 트랜잭션을 커밋하는가 | 침묵 (치명) | - | 예외를 던지면 OrphanApproval 기록까지 롤백되어 REFUND_PENDING이 남지 않고 T2도 못 잡는다. 그러면 승인 금액이 영구 미환불로 남아 A4/R6이 깨진다. "REFUND_PENDING으로 남는다"는 문장은 있으나 트랜잭션 처리는 없다 |
| 31 | 6 | 고아 경로의 반환값 | 어긋남 | S-5 "애그리거트 메서드 반환 규약은 전이 발생 / 전이 없음 둘이다." / S-2 "OrphanApproval(..., 상태 REFUND_PENDING)을 Payment 안에 기록하고, 같은 트랜잭션에서 Mock PG 환불을 호출해 REFUNDED로 닫는다." | 상태가 새로 생겼으니 전이 발생인지, 예약 상태는 안 변했으니 전이 없음인지 불명. 호출자가 후속 동작을 이 값으로 결정하므로 갈린다 |
| 32 | 6, 4 | 이미 정산된 Payment에 고아가 생기면 T2 후보에서 빠진다 | 어긋남 | S-1 "T2의 후보 집합은 settledAt이 null인 Payment로 한정된다." / S-8 T2 후보 조회 "... 또는 OrphanApproval REFUND_PENDING 존재" | settledAt이 이미 찍힌 뒤(정상 확정 후) 도착한 늦은 승인이 고아가 되고 환불에 실패하면, REFUND_PENDING이 영원히 방치된다. 이 경우가 실제로 발생 가능한지도 문서만으로는 판단 불가 |

부수 지적 (I 원문). 문서 사이 번호 체계가 어긋나 참조가 위험하다. 04-5 2절 "P3 확정 시 재고 확정"과 스펙 결과 정책 표 "결제 실패 시 만료 (P3)"가 같은 기호로 다른 정책을 가리키고, 06-2 4절 "P3, P5, P7은 동기 호출로 명세 (Stateless 확정). 취소의 RefundPayment(P6)는 비동기"는 스펙의 P 번호로 읽으면 뜻이 완전히 뒤집힌다. 또 하나. T2는 Payment를 후보로 조회하면서 Booking 상태로 분기하는데, 입력 팩 1절 "결제는 예약을 모른다"와 06-4 2-3 "정책의 소속(구독자)은 전부 예약 컨텍스트다"를 합치면 T2가 예약 컨텍스트에서 Payment 테이블을 직접 쿼리해야 한다. 어느 패키지에 두는지 문서에 없다.

문서만 보고 짜기 어려웠던 것 (I 원문). 정산 표식(settledAt)이 설계의 중심축인데, 표식을 찍는 곳을 S-1이 4곳으로 열거하고 S-8 표와 결과 정책 표가 그 열거 밖의 분기를 여럿 만들어서, 분기마다 "여기서 찍나"를 내가 정해야 했다. 잠금 순서는 S-4가 한 줄로 못 박았는데 S-8의 T1/T2 서술이 그 순서를 뒤집어 놓아서 둘 중 하나를 버려야 했다. recordApproval은 애그리거트 메서드 시그니처로 주어졌는데 안에서 PG를 호출하고 실패를 삼켜야 해서, 트랜잭션 경계와 계층 소속을 문서에서 못 찾았다. T2 조회 조건의 "APPROVED 시도"가 REFUNDED를 포함하는지 한 줄만 있었으면 [3] [17] [22]가 한꺼번에 풀렸을 것 같다.

## 5. 확인필요 항목에 대한 세션 대조

스킬 규칙대로 치명과 보통은 반박하지 않는다. 확인필요만 08-2 원문(에이전트가 보지 못한 근거)과 대조했다.

| 항목 | 대조 결과 | 판정 |
|---|---|---|
| G 확인필요 1, I [17]. T2 조회 "APPROVED 시도 존재"에 REFUNDED 포함 여부 | 08-2 D8 원문도 "APPROVED 시도 존재"라고만 썼다. 의도는 status = APPROVED(미환불)였으나 적히지 않았다 | 미정 확인. 6절 C-1에서 확정 |
| G 확인필요 2, I [26]. openAttempt와 PG 요청의 트랜잭션 경계 | 08-1, 08-2 어디에도 없다 | 미정 확인. 6절 C-10. 사용자 결정 |
| G 확인필요 3. UnknownAttempt 예외 시 PG 응답 | 없다 | 미정 확인. 6절 C-3에서 2xx + 로그로 확정 제안 |
| G 확인필요 4, I [14]. T1 만료 분기에 SettlePayment가 없는 것이 의도인가 | 08-2 D8 원문도 T1 만료 분기에 SettlePayment가 없다. D1은 "확정 트랜잭션과 결제 실패 만료 트랜잭션"에서만 찍는다고 했다. 즉 08-2는 정산 표식을 "승인 또는 3회 실패라는 결제 사실을 소비했다"는 뜻으로 썼고, 그래서 T1 만료(결제 사실 없음)에는 안 찍었다. 이 정의가 G 치명 2(스킵 경로 좀비)와 H 치명 1(정산된 Payment의 고아)을 낳았다 | 의도였으나 정의가 틀렸다. 6절 C-1에서 정의를 교체 |
| H 확인필요 1, I [7]. 정산 표식의 단위 | Payment 단위가 의도다(08-2 D1 "Payment에 정산 표식") | 확인. C-1에 명시 |
| H 확인필요 2. T2 건별 처리의 조건 재검사 | 08-2 D8 원문도 없다 | 미정 확인. C-9 |
| H 확인필요 3, I [30]. 고아 환불 실패 시 커밋인가 롤백인가 | 08-2 D2 "환불 실패면 REFUND_PENDING으로 남고 T2가 순찰한다"는 커밋 의도다. 트랜잭션 처리 문장이 빠졌다 | 의도 확인. C-4에서 명시 |
| H 확인필요 4, G 보통 3. CANCELED + APPROVED 분기 도달 불가 | 08-2 정책 목록이 "정상 경로에선 없음"이라 적고 방어용으로 남겼다. D4가 취소 환불을 동기로 바꾼 뒤에도 남긴 것은 08-2의 일관성 누락이다 | 지적 수용. C-6에서 삭제 |
| H 확인필요 5. settledBy의 용도 | 08-2 D1 "F 확인필요 5(관측 수단)를 겸한다". 관측용이다 | 확인. C-11에 명시 |
| H 확인필요 6. 값 순서 제약에 T1 주기가 없는 이유 | 08-2 D3 확정 우선으로 T1과 승인의 결과가 폴링 위상에 좌우되지 않게 됐으므로 T1 주기와 T2 임계 사이에 정확성 제약이 없다. 08-2 E 치명 4(임계 정렬 실패)가 D3로 닫힌 결과다 | 반박. 제약 불필요. 다만 그 근거를 S-8에 적어야 한다(C-11) |
| H 확인필요 7. 페이로드 "날짜별 수량"과 n = 1의 관계 | 08-2 페이로드 표가 D9(n = 1)보다 먼저 쓰인 흔적이다 | 반박 아님. C-11에서 "날짜 목록"으로 축소 |
| I [2]. Booking 조회 실패 | 없다. PaymentApproved의 bookingId는 openAttempt 때 예약이 넘긴 값이라 없을 수 없다 | 불변 위반이므로 예외(로그, T2 대상 아님). C-3 |
| I [5], [21]. P1, T2의 Payment 잠금 | 08-2 D8 T2 행에 Payment 잠금이 없다 | 누락 확인. C-2, C-9 |
| I [8]. P1이 attemptId, pgTransactionId를 쓰는가 | 안 쓴다. 로그와 추적용이다. refund(bookingId)는 U3와 I7로 대상이 유일하다 | 확인. 페이로드 유지, 용도를 C-11에 적는다 |
| I [12]. 3회 실패 + 승인 이력 동시 존재 | I6, I7, I9로 불가능하다(승인 뒤에는 새 시도를 열 수 없다). 방어 분기 불필요 | 반박. 단 C-4의 고아 규칙으로 "REQUESTED 시도 승인 시 이미 NORMAL 승인 이력이 있으면 고아"를 두므로 이 조합은 구조적으로 생기지 않는다 |
| I [15]. Payment가 없는 Booking에 대한 조회 응답 | 없다 | 미정 확인. C-9에서 "후속 없음"으로 응답 |
| I [18]. FAILED 3개가 = 3인가 >= 3인가 | I6으로 3 초과는 불가능 | >= 3으로 적는다(C-1). 데이터 손상 시에도 잡히도록 |
| I [25]. PG 환불 성공 후 DB 롤백 | 08-2에 없다. G 치명 4와 같은 뿌리 | C-5 |
| I [27]. 두 테이블에 걸친 pgTransactionId 유니크 | 08-2 D2가 OrphanApproval을 별도 요소로 둔 결과다 | 지적 수용. C-4에서 고아를 같은 시도 테이블의 kind = ORPHAN 행으로 바꿔 단일 유니크로 해소 |
| I [29]. 승인 금액 대조 | 08-2에 없다. 08-2 F 확인필요 4(approvedAmount 사용처 없음)에서 페이로드에서 뺐던 것 | C-4에서 recordApproval Pre에 금액 대조를 넣고 불일치는 고아 취급 |
| I [32] | G 치명 1, H 치명 1과 동일 | C-1 |

## 6. 해결안 v2.1 (v2에서 바뀐 것만. 사용자 판정용)

Thought: 치명 지적은 전부 v2가 새로 만든 부품 셋(정산 표식, 고아 승인, 동기 환불)의 정의가 덜 닫힌 데서 나왔다. 뿌리 결정(전달 보장 감수 + T2 보정, 승인 핸들러 통합, 확정 우선, attemptId)은 셋 다 성립을 확인했다.
Action: 정산 표식의 정의를 "결제 사실을 소비했다"에서 "예약 컨텍스트가 이 Payment에 대해 할 후속이 없다"로 바꾸고, 그 정의에서 찍는 규칙과 되돌리는 규칙을 기계적으로 유도했다. 고아는 별도 요소가 아니라 시도 테이블의 다른 종류 행으로 바꿔 유니크와 셈 규칙을 한 곳에 뒀다. 환불 멱등키는 발급하지 않고 유도했다.
Observation: 아래 C-1~C-12. 08-2 D0~D12 중 D5(expire 스킵 반환)만 뒤집었고 나머지는 보강이다.

### C-1. 정산 표식의 정의와 규칙 (S-1, S-8 교체. G 치명 1, 2, 3, H 치명 1, 2, I [3] [9] [14] [17] [22] [32])

정의. settledAt이 있는 Payment는 "예약 컨텍스트가 이 Payment를 보고 할 후속(확정, 만료, 환불)이 현재 없다"는 뜻이다. 단위는 Payment다. settledBy(APPROVAL_HANDLER, FAILURE_HANDLER, T1, T2, CANCEL)는 관측용이다(보정 비율 계측). 분기에 쓰지 않는다.

찍는 규칙 (예외 없음). 예약 컨텍스트가 Booking 잠금 아래에서 Payment 상태를 읽고 분기를 끝낸 트랜잭션은 그 결과가 전이든 무해든 스킵이든 환불이든 SettlePayment로 끝난다. 해당 트랜잭션: P1 전 분기, P3 전 분기(전이, 무해, 스킵), T1 전 분기(확정, 만료), T2 전 분기, 취소 트랜잭션(환불 뒤).

되돌리는 규칙. 결제 컨텍스트가 예약 후속을 요구하는 새 사실을 기록하면 settledAt을 null로 되돌린다. 해당 사실: recordApproval이 REQUESTED 시도를 APPROVED로 바꿀 때, recordFailure가 3회째 실패를 기록할 때. (고아 승인은 예약 후속이 아니라 결제 내부 후속이므로 되돌리지 않는다. 아래 조회 조건 (c)가 settledAt과 독립인 이유다.)

T2 후보 조회 (잠금 없음, 배치 [가설 100], candidateSince 오름차순).
- (a) settledAt is null and status = APPROVED(REFUNDED 아님)인 NORMAL 시도 존재 and approvedAt <= now - 임계. candidateSince = approvedAt
- (b) settledAt is null and NORMAL FAILED 시도 수 >= 3 and 승인 이력(APPROVED 또는 REFUNDED) NORMAL 시도 없음 and 마지막 failedAt <= now - 임계. candidateSince = 마지막 failedAt
- (c) kind = ORPHAN이고 status = REFUND_PENDING인 행 존재 and receivedAt <= now - 임계. settledAt 무관. candidateSince = receivedAt
(a) or (b) or (c). 임계 [가설 30초].

T2 건별 처리 (건별 트랜잭션). Booking 잠금 → Payment 잠금 → 조건 재검사 → 분기.
- HELD + (a): confirm + commit ×N + settle(T2)
- HELD + (b): expire(PAYMENT_FAILED) + releaseHeld ×N + settle(T2)
- EXPIRED + (a): refund + settle(T2)
- CONFIRMED + (a): settle(T2) (P1 유실의 자가 치유)
- CONFIRMED, EXPIRED, CANCELED + (b): settle(T2) (후속 없음)
- (c): 고아 환불 재시도. 성공이면 REFUNDED. 실패면 REFUND_PENDING 유지, 다음 주기. settle 무관
- Payment에 대응하는 Booking이 없으면: 불변 위반(U3, openAttempt는 예약이 연다). 로그, 스킵. settle 안 함(운영자가 볼 수 있게 남긴다)

좀비 판정. (a)와 (b)는 처리 뒤 반드시 settle되므로 후보에서 빠진다. (c)는 실제 미환불이 있는 동안만 후보다. 정상 확정분은 P1이 settle하므로 후보가 아니다. 후보 집합은 "후속이 남은 건 + 미환불 고아"로 유한하다.

### C-2. 전역 잠금 순서 (S-4 교체. G 보통 1, H 보통 2, I [13])

Booking → Payment → 재고 N행(날짜 오름차순). 모든 경로 대조.

| 경로 | 잠금 순서 |
|---|---|
| RequestBooking | 재고 N행만 (Booking은 insert) |
| RequestPayment 중계 | Booking → Payment(openAttempt) |
| recordApproval, recordFailure | Payment만 |
| P1 (승인 처리) | Booking → Payment(상태 읽기, 뒤에 settle 또는 refund) → 재고 N행(commit) |
| P3 (실패 시 만료) | Booking → Payment(settle) → 재고 N행(releaseHeld). Payment를 먼저 잠근 뒤 재고 |
| T1 | Booking → Payment(조회 + settle) → 재고 N행 |
| T2 | Booking → Payment → 재고 N행 |
| CancelBooking | Booking → Payment(refund) → 재고 N행(releaseSold) |

06-2 5절과 06-4 0절 락 선언에 이 표를 싣는다. 08-2 E가 "잠금 순서 교착 막힘"이라 판정한 근거(Booking → 재고 또는 Payment)는 T1이 Payment를 먼저 잠그면서 무효가 됐고, 이 표가 그것을 대체한다.

### C-3. 리스너 예외 처리 위치와 콜백 응답 (S-0 보강. I [1], G 확인필요 3, I [2])

- 리스너 본체(REQUIRES_NEW)는 예외를 던진다. 그래야 부분 커밋(confirm은 됐는데 commit ×N 실패)이 롤백된다. 예외를 잡아 로그하는 것은 트랜잭션 밖의 얇은 어댑터다. 구현 형태: @TransactionalEventListener가 붙은 어댑터가 try/catch로 @Transactional(REQUIRES_NEW) 서비스 메서드를 호출한다. 같은 클래스 안의 self-invocation은 프록시를 타지 않으므로 두 빈으로 나눈다.
- PG 콜백 HTTP 응답은 recordApproval/recordFailure 본체의 결과와도 무관하게 2xx다. UnknownAttempt는 로그로 남긴다. PG 재전송으로 복구되는 경우가 없으므로(U4, 유도 복구는 T2) 5xx는 무의미하다.
- 리스너에서 Booking을 못 찾는 것은 불변 위반(openAttempt는 예약 컨텍스트가 존재하는 Booking에 대해서만 연다). 예외로 던지고 로그. T2 대상이 아니다.

### C-4. recordApproval, recordFailure 계약 재작성과 고아의 모델 (S-2 교체. I [26] [27] [28] [29] [30] [31], H 보통 10, H 확인필요 3)

고아의 모델. OrphanApproval을 별도 요소로 두지 않는다. PaymentAttempt 행에 kind(NORMAL, ORPHAN)를 둔다. ORPHAN 행은 sourceAttemptId(원 시도의 attemptId), pgTransactionId, amount, receivedAt, status(REFUND_PENDING, REFUNDED)를 갖고 순번이 없다. I6(시도 상한), I7(승인 유일), I9(진행 유일)는 NORMAL 행만 센다. U4의 DB 유니크는 payment_attempt.pg_transaction_id 단일 컬럼이다(I [27] 해소). U5는 NORMAL 행의 attemptId 유니크.

계층. Payment 애그리거트는 PG를 호출하지 않는다. recordApproval의 고아 분기는 두 단계다. 애그리거트가 ORPHAN 행을 REFUND_PENDING으로 추가하고 "고아 환불 필요"를 반환값으로 알린다. 결제 앱 서비스가 같은 트랜잭션에서 PG 환불을 호출하고, 성공이면 애그리거트의 markOrphanRefunded를 부른다. PG 호출 예외는 앱 서비스가 잡아 로그하고 트랜잭션을 커밋한다(REFUND_PENDING이 남아야 T2 (c)가 잡는다). 이것이 I [30]의 답이다.

| 행동 | Pre(선행) | Invariant | Post(후행) | 위반 시 예외 |
|---|---|---|---|---|
| recordApproval(attemptId, pgTransactionId, amount) | Payment 잠금. 같은 pgTransactionId가 어느 행에든 있으면 무해 무시(U4, 전이 없음 반환). attemptId의 NORMAL 시도 존재 | 승인 유일(I7, NORMAL만) | (1) 시도가 REQUESTED이고 NORMAL 승인 이력이 없고 amount가 청구액과 같으면: APPROVED, pgTransactionId와 approvedAt 저장, settledAt = null, PaymentApproved 발행, 전이 발생 반환. (2) 그 외(시도가 종착이거나, 이미 NORMAL 승인 이력이 있거나, 금액 불일치)이면: ORPHAN 행 REFUND_PENDING 추가, PaymentApproved 발행 없음, "고아 환불 필요" 반환. 앱 서비스가 같은 트랜잭션에서 PG 환불(멱등키 C-5) 호출, 성공 시 REFUNDED + PaymentRefunded(kind ORPHAN) 발행, 실패 시 로그 후 REFUND_PENDING으로 커밋 | UnknownAttempt (attemptId 없음). AlreadyApproved, NoRequestedAttempt, AttemptAlreadyClosed는 폐기 |
| recordFailure(attemptId, pgTransactionId) | Payment 잠금. 같은 pgTransactionId 무해 무시. attemptId의 NORMAL 시도 존재 | 시도 상한(I6, NORMAL만) | 시도가 REQUESTED면 FAILED, pgTransactionId와 failedAt 저장, NORMAL FAILED 수가 3이 되면 settledAt = null, PaymentFailed 발행(시도 수 탑재), 전이 발생 반환. 종착이면 무해 무시(전이 없음 반환) | UnknownAttempt |
| settle(settledBy) | Payment 잠금 | 없음 | settledAt이 null이면 기록. 있으면 무해(전이 없음 반환). 종착 무해 7곳째 | 없음 |
| refund(bookingId) | Payment 잠금. NORMAL APPROVED 존재. REFUNDED 재호출 무해 | 승인 유일 | APPROVED → REFUNDED, PaymentRefunded(kind NORMAL) 발행. PG 호출은 앱 서비스가 멱등키(C-5)로 수행 | NoApprovedAttempt |

반환 규약(S-5)은 유지한다. 전이 발생 / 전이 없음. 고아 분기의 "고아 환불 필요"는 전이 발생의 한 종류로 값을 붙인다(전이 발생: APPROVED, 전이 발생: ORPHAN_PENDING). 값이 셋으로 늘지만 규약의 종류는 둘이다.

### C-5. 환불 멱등키는 발급하지 않고 유도한다 (G 치명 4, I [19] [25])

- NORMAL 환불의 PG 멱등키 = 그 시도의 attemptId. I7로 Payment당 승인 시도가 하나이므로 환불도 하나다.
- ORPHAN 환불의 PG 멱등키 = 그 고아의 pgTransactionId.
- Mock PG 계약에 추가: 같은 멱등키의 환불 재요청은 처음과 같은 결과를 돌려준다.
- 유도 키라서 트랜잭션이 롤백되어도(S-4 취소 경로) 재시도가 같은 키를 쓴다. 저장할 필드가 없다. 결과 미상(타임아웃)이면 취소 경로는 S-4대로 롤백하고 이용자가 재시도한다. 이중 환불이 되지 않는다.
- 고아 환불의 T2 재시도(C-1 (c))도 같은 키라 이중 환불이 없다. 재시도 상한은 두지 않는다(Mock). 실 PG 전환 시 상한과 운영 알림을 이월.

### C-6. CANCELED 분기 삭제 (G 보통 3, H 확인필요 4)

S-4로 취소 환불이 취소 트랜잭션 안에 있으므로 CANCELED이면서 미환불 APPROVED인 상태는 생기지 않는다. P1과 T2의 CANCELED 분기를 지운다. P1의 CANCELED는 CONFIRMED와 같이 "settle 무해"로 끝난다. 03 5-3, 02 3-3, 04 발견 3의 "== EXPIRED로 좁힌 이유" 문단은 08-2 7절의 수정안 대신 "CONFIRMED와 CANCELED에 도착한 승인은 정산 표식만 찍는다"로 고친다(08-2 F 보통 5와 같은 자리).

### C-7. R4 문언과 02 3-3 (H 보통 4)

R4 문언(입력 팩 2절, 02 2절, 06-4 3절): "HELD 종료 시 재고 반환 보장. 종료 조건은 TTL 만료와 결제 3회 실패 중 먼저 커밋되는 것. 단 만료 커밋 전에 승인이 기록됐으면 확정이 우선한다. 리스너 유실 시에도 늦어도 T2 주기 안에 종료된다."
02 3-3 "3회에 도달하기 전에 TTL이 먼저 만료되면 TTL이 이긴다"는 "만료 커밋이 승인 기록 커밋보다 먼저면 TTL이 이긴다"로. 04-5 4-3 유형 3의 "T1이 이긴 경우의 잔여 승인은 P2가 환불로 흡수" 문장은 "만료 뒤 기록된 승인은 P1의 EXPIRED 분기가 환불"로.

### C-8. expire의 스킵 반환 폐기, 분기 주체 앱 서비스로 통일 (08-2 D5 뒤집음. G 보통 6, H 보통 11, I [10] [23])

- confirm, expire, cancel 세 메서드의 계약은 06-4 v4 그대로다. 허용 상태 밖 호출은 InvalidStateTransition, 같은 종착 재호출은 무해(전이 없음 반환).
- 분기 주체는 앱 서비스다. 앱 서비스가 Booking을 잠근 뒤 상태를 읽고, 그 상태에서 허용되는 메서드만 부른다. T1과 P3가 CONFIRMED 또는 CANCELED를 만나면 expire를 부르지 않고 WARN 로그 후 settle로 끝낸다(금지 전이 요청이 들어온 것은 경합의 정상 결과이므로 오류가 아니라 관측 대상이다). 이용자의 CancelBooking이 HELD나 EXPIRED를 만나면 앱 서비스가 InvalidStateTransition을 던진다(사람 경로는 오류).
- 04-5 6절 미결 "ExpireBooking 중복 거부의 의미론(오류인가 무시인가)"의 답: 애그리거트 계약은 오류, 정책 경로의 앱 서비스는 무시(로그), 사람 경로의 앱 서비스는 오류.
- 이 결과 S-5의 confirm/expire 비대칭(H 보통 11)이 없어지고, 06-4 1-2 expire 행의 예외 칸은 v4 그대로 유지된다. 08-2 7절의 "expire(): 예외 칸을 스킵 반환으로" 항목은 삭제.

### C-9. 예약 → 결제 조회 API 계약과 T2의 잠금 (H 보통 3, 9, 확인필요 2, I [5] [15] [16] [21])

- 조회 API 하나: paymentFollowUp(bookingId) → PaymentFollowUpView { exists, settledAt, approvedAttempt(attemptId, approvedAt) 또는 없음, normalFailedCount, lastFailedAt, orphanPendingCount }. Payment가 없으면 exists = false, 나머지 비어 있음(T1은 "후속 없음"으로 읽고 만료 경로). 잠금 옵션: T1, T2, P1은 Payment 잠금을 함께 요청한다(C-2 순서).
- T2 후보 조회는 결제 컨텍스트의 findFollowUpCandidates(threshold, limit) → PaymentId와 bookingId 목록. 이 두 조회는 결제 컨텍스트가 제공하고 예약 컨텍스트(정책 소속)가 호출한다. 06-1 R6 행 "흐르는 것"에 추가(01 v18이 이미 적은 항목).
- T1과 P1의 확정 경로 코드 공유(I [16]): 공통 메서드는 confirmHeld(bookingId, settledBy) = confirm + commit ×N + settle. P1의 4분기 switch와 T1의 2분기는 각자 두고 확정 분기에서만 이 메서드를 부른다. settledBy는 호출자가 넘긴다.
- T2 건별 처리는 Booking 잠금 → Payment 잠금 → C-1 조건 재검사 → 분기. 다중 인스턴스는 이 잠금과 settle 무해, REFUNDED 무해로 안전하다.

### C-10. openAttempt와 PG 요청의 경계 (G 확인필요 2, I [26]. 사용자 결정)

| 안 | 내용 | 평가 |
|---|---|---|
| 가. 같은 트랜잭션 안에서 PG 요청 | Booking 잠금 + Payment 잠금을 쥔 채 PG 호출. Mock은 프로세스 내 호출이라 지연이 없다 | 기계가 없다. 실 PG 전환 시 되돌릴 항목에 추가 |
| 나. REQUESTED 커밋 후 PG 요청, 전송 실패 시 즉시 recordFailure(로컬 실패 사유) | 잠금 밖 호출. 커밋과 전송 사이 프로세스 종료 시 REQUESTED 갇힘(S-7 감수 범위) | 로컬 실패에 pgTransactionId가 없어 U4 유니크와 충돌. 별도 사유 필드가 필요 |

추천: 가. S-7 감수와 일관되고, 실 PG 전환 목록(취소 환불 비동기, 타임아웃 스케줄러)에 "PG 요청 커밋 후 호출"을 함께 올린다.

### C-11. 표기와 추적성 (H 보통 5, 6, 7, 8, 12~14, 확인필요 5~7, I [6] [8] [20], 부수 지적)

- 정책 번호 재배정표를 04-5 v2 머리에 싣는다. 구 P1 + P2 → 신 P1 결제 승인 처리. 구 P3 → 신 P2. 구 P4 → 신 P3. 구 P5 → 신 P4. 구 P6 → 신 P5. 구 P7 → 신 P6. T1 유지, T2 신설. 구 번호를 참조하는 06-2 4절, 04-5 4-2, 4-3 문장을 전부 신 번호로.
- 04-5 4-1: 구독자 없는 이벤트 20개(BookingConfirmed, BookingExpired, BookingCanceled 추가. 이유: 후속이 같은 트랜잭션 내부 호출). 1절 "자동 반응이 필요한 것은 2개(PaymentApproved, PaymentFailed) + 시간 2".
- T1, T2의 일관성 표기: "내부 트리거. 결제 컨텍스트 조회와 SettlePayment는 동기 같은 트랜잭션(이탈 선언)". 06-4 2-3 분류표에 T2 행, 이탈 선언 군에 T1, T2의 결제 호출.
- SettlePayment는 이벤트를 내지 않는다. 근거: 예약 컨텍스트의 처리 완료 표식이지 도메인 사건이 아니다. 04 1절 표의 이벤트 칸은 "없음(표식)". 이벤트 없는 커맨드가 처음 생긴다는 사실을 04에 적는다.
- 06-4 3절 역추적표에 R6 행: I7(NORMAL), U4, U5, C-4 고아 규칙, P1의 CONFIRMED/CANCELED 무해 분기, C-5 환불 멱등키.
- 페이로드 표의 "날짜별 수량"을 "날짜 목록"으로(n = 1). PaymentApproved의 attemptId, pgTransactionId, attemptCount는 P1이 분기에 쓰지 않는다. 로그와 추적용이라고 적는다.
- settledBy는 관측용. 값에 CANCEL 추가.
- S-8 값 제약: "T2 임계 < T2 주기 < TTL. T1 주기는 정확성과 무관(확정 우선으로 T1과 승인의 결과가 폴링 위상에 좌우되지 않음). 정렬 키 candidateSince."
- 스펙 말미의 문서별 반영 지점 목록은 08-2 7절을 7절에서 갱신한 것으로 대신한다.

### C-12. 잠금 보유 중 외부 호출 (G 보통 2, I [26])

취소 트랜잭션(Booking + Payment + 재고 N행 잠금)과 P1 EXPIRED 분기, 고아 환불이 잠금을 쥔 채 Mock PG를 호출한다. v1 감수. 06-4 0절 이탈 선언에 "PG 호출은 프로세스 내 Mock. 호출 타임아웃 [가설 3초]. 실 PG 전환 시 환불을 REFUND_PENDING 커밋 + T2 재시도로 바꿔 잠금 밖으로 낸다"를 적는다. C-2의 순서에서 재고 N행이 마지막이므로, 취소 경로에서 refund를 releaseSold보다 먼저 두면 재고 행 잠금 보유 중 외부 호출은 없어진다. 그렇게 적는다(G 보통 2의 최소 제안 수용).

### v2.1 결과 정책 목록 (8개. v2에서 바뀐 칸만 굵게 대신 별표)

| 정책 | 트리거 | 가드 | 행동 | 일관성 | 실패 시 |
|---|---|---|---|---|---|
| 결제 승인 처리 (P1) | PaymentApproved | Booking 잠금 → Payment 잠금 → 상태 | HELD: confirmHeld(APPROVAL_HANDLER). EXPIRED: refund + settle*. CONFIRMED, CANCELED: settle(무해)* | 결과적 (리스너 REQUIRES_NEW. 결제 호출은 같은 트랜잭션) | 예외는 트랜잭션 밖에서 로그*. 유실과 예외는 T2 |
| 확정 시 재고 확정 (P2) | 확정 트랜잭션 내부 호출 | 없음 | commit ×N | 강한 | 해당 없음 |
| 결제 실패 시 만료 (P3) | PaymentFailed | 시도 수 3 이상(이벤트 탑재값). Booking 잠금 → Payment 잠금 → 상태 | HELD: expire(PAYMENT_FAILED) + releaseHeld ×N + settle. EXPIRED: settle(무해). CONFIRMED, CANCELED: WARN 로그 + settle* | 결과적 | 동일 |
| 만료 시 재고 반환 (P4) | 만료 트랜잭션 내부 호출 | 없음 | releaseHeld ×N | 강한 | 해당 없음 |
| 취소 시 환불 (P5) | 취소 트랜잭션 내부 호출 | 없음 | refund(bookingId, 멱등키 attemptId)* + settle(CANCEL)*. 재고 반환보다 먼저* | 강한 (이탈 선언) | 롤백, 취소 실패. 재시도는 같은 멱등키 |
| 취소 시 재고 반환 (P6) | 취소 트랜잭션 내부 호출 | 없음 | releaseSold ×N | 강한 | 해당 없음 |
| TTL 만료 (T1) | 시간 | HELD, 기한 경과. Booking 잠금 → Payment 잠금 → paymentFollowUp | 미정산 APPROVED 있음: confirmHeld(T1). 없음: expire(TTL_EXPIRED) + releaseHeld ×N + settle(T1)* | 내부 트리거, 결제 호출 동기* | 다음 주기 재후보 |
| 후속 미완 결제 순찰 (T2) | 시간 | C-1 조회 (a)(b)(c) | C-1 건별 처리. 모든 분기 settle* | 내부 트리거, 결제 호출 동기* | 다음 주기 재후보 |

수량: 정책 8, 커맨드 23(SettlePayment), 이벤트 22(PaymentRefunded에 kind). 새 필드: Payment.settledAt, settledBy. PaymentAttempt.kind, attemptId, approvedAt, failedAt, sourceAttemptId(ORPHAN만), receivedAt(ORPHAN만).

## 7. 08-2 7절 반영 목록에 대한 갱신 (수용 시. 08-2 7절에 더하거나 바꾸는 것만)

### 06-4 → v5
- 0절: 잠금 순서를 C-2 표로 (Booking → Payment → 재고). 이탈 선언에 C-12 문장. 리스너 예외 위치 C-3. "종착 무해 7곳째"는 settle.
- 1-1: I6, I7, I9 문장에 "(NORMAL 시도만)". U4 강제 수단 "payment_attempt.pg_transaction_id 유니크". U5 attemptId 유일(NORMAL). R6 행. A4 문장 유지(S-11).
- 1-2 예약: confirm, expire, cancel 행은 v4 그대로(C-8. 08-2의 expire 스킵 반환 항목 삭제). RequestPayment 중계 Pre에 Booking 잠금. hold(n) n = 1. RequestBooking 예외 칸에 U1 위반 시 재조회 멱등 반환(G 보통 8, 08-1 B 보통 11).
- 1-2 결제: C-4 표로 교체. openAttempt Post에 attemptId 발급, PG 전달(C-10 채택안).
- 1-3 결제 시도 상태표에 kind 열과 ORPHAN 행(REFUND_PENDING → REFUNDED). "알려진 갇힘" 문구 S-7.
- 2-1: BookingCanceled 행 "환불과 판매분 반환은 취소 트랜잭션 내부 호출". 시간 트리거 행에 T2.
- 2-2: 6절 v2.1 정책 목록으로 교체. 멱등 칸 3분할(발행 중복 U4 / 배달 중복 종착 무해 / 유실 T2).
- 2-3: 이탈 선언 군에 T1, T2의 결제 호출 포함. 반응 불필요 이벤트 20개.
- 2-4 페이로드 표(날짜 목록), 2-5 스케줄러 명세(C-1 조회, C-9 API), 2-6 조회 API 계약(PaymentFollowUpView, findFollowUpCandidates) 신설.
- 3절: R4 문언 C-7. 04-5 6절 이월 4건 종결 근거(가드 = 2-2 확정, Commit/Release 중복 = 내부 호출, 타임아웃 = S-7, ExpireBooking 중복 = C-8).

### 06-2 → v5
- 1절 Payment 내부 요소: settledAt, settledBy, PaymentAttempt(kind, attemptId, 순번, 상태, pgTransactionId, requestedAt, approvedAt, failedAt, sourceAttemptId, receivedAt).
- 4절: 취소 행 "같은 트랜잭션에서 refund(재고 반환보다 먼저) + settle". "PaymentApproved 유실 감수" 삭제. "P3, P5, P7 동기, P6 비동기" 문장을 신 번호와 C-6 결과로 재작성. T1, T2 행.
- 5절: C-2 표.
- 6절 CRC: Payment "정산 표식을 찍고 되돌린다", "고아 승인을 ORPHAN 행으로 기록한다", "PG를 호출하지 않는다". PaymentApplicationService 행 신설(PG 호출, 고아 환불, 멱등키 유도). BookingApplicationService에 T1, T2, paymentFollowUp 협력자.
- 7절: SettlePayment(bookingId, settledBy), RefundPayment(bookingId), confirmHeld(bookingId, settledBy) 내부 메서드, hold(1).

### 04-5 → v2, 04 → v9, 03 → v11, 02 → v8, 06-1 → v2, 입력 팩 → v3, 05-3 → v11
- 04-5: C-11 번호 재배정표를 머리에. 4-1 20개. 1절 수치. 4-2에 SettlePayment. 4-3 유형 1 PaymentStatus 행 S-7 종결, 유형 2 "거부는 무해 무시(C-4)", 유형 3 C-7 문장. 6절 이월 종결.
- 04: 커맨드 23. SettlePayment(이벤트 없음, 근거 C-11). RefundPayment(bookingId). 발견 3 체인표 신 정책으로. ConfirmBooking 발신 정책에 T1, T2.
- 03: 결제 이벤트 페이로드. 5-3 문단 C-6, C-7. 6절 엔티티별 그룹의 Payment 행에 "시도 행 kind NORMAL, ORPHAN".
- 02: 2절 R4 문언 C-7. 3-3 문장 C-7.
- 06-1: 01 v18 항목(R6 행 흐르는 것에 SettlePayment, paymentFollowUp, findFollowUpCandidates. 구독자 없는 이벤트 20개).
- 입력 팩: R6, 확정 전제 세 줄, 3절에 정산 표식, 고아 승인, 확정 우선 정의. R4 문언 C-7.
- 05-3: 정산 표식(Settlement Mark), 고아 승인(Orphan Approval, ORPHAN 시도 행), 순찰 정책(Patrol Policy), 시도 식별자(attemptId), 확정 우선(Confirm-First), 환불 멱등키(유도).

## 8. 사용자 결정 (08-2 8절을 대체한다. 이 표 하나에만 답하면 된다)

| 번호 | 결정 | 추천 | 08-2 대비 |
|---|---|---|---|
| 1 | C-1 정산 표식 정의(후속 없음 표식), 찍는 규칙(예외 없음), 되돌리는 규칙, T2 조회 (a)(b)(c) | 가 | 08-2 결정 1의 정의 교체 |
| 2 | C-4 고아 승인을 ORPHAN kind 시도 행으로, 애그리거트는 PG를 호출하지 않고 앱 서비스가 같은 트랜잭션에서 환불, 실패는 REFUND_PENDING 커밋. 금액 불일치와 승인 이력 중복도 고아 취급 | 가 | 08-2 결정 2의 모델 변경 |
| 3 | C-2 잠금 순서 Booking → Payment → 재고 | 가 | 신규 |
| 4 | C-5 환불 멱등키 유도(attemptId, pgTransactionId), Mock PG 계약 추가 | 가 | 신규 |
| 5 | C-8 expire 스킵 반환 폐기. 세 메서드 계약 v4 유지, 분기는 앱 서비스, 정책 경로는 로그 후 무시 | 가 | 08-2 결정 5 뒤집음 |
| 6 | C-3 리스너 예외는 트랜잭션 밖에서 로그. 콜백 응답 항상 2xx | 가 | 08-2 결정 10(D0) 보강 |
| 7 | C-6 CANCELED 분기 삭제, 03/02/04 문단을 "CONFIRMED와 CANCELED 도착 승인은 표식만" | 가 | 08-2 7절 03 v11 항목 교체 |
| 8 | C-7 R4 문언과 02 3-3 | 가 | 신규 |
| 9 | C-10 openAttempt의 PG 요청을 같은 트랜잭션 안에서 | 가 | 신규 |
| 10 | C-12 잠금 보유 중 PG 호출 v1 감수, 취소 경로는 refund를 재고 반환보다 먼저 | 가 | 신규 |
| 11 | 08-2 결정 3(확정 우선, R-9 삭제), 4(취소 환불 동기), 6(타임아웃 미도입), 7(가설값), 8(n = 1), 9(종착 멱등키) | 그대로 | 이번 검증에서 뒤집을 근거 없음 |

"전부 추천대로"라고 답하면 7절 목록대로 반영한다. 하나라도 다르면 그 번호만 적어 주면 된다.

다음 행동.
1. 위 표에 답하면 메인 세션이 7절과 08-2 7절대로 06-4 v5, 06-2 v5, 04-5 v2, 04 v9, 03 v11, 02 v8, 06-1 v2, 입력 팩 v3, 05-3 v11에 반영한다.
2. 반영 후 정책 축 재검증 1회(스킬 규칙). 이번 실행은 그 1회가 아니다.
3. 재검증 통과 후 01 Step 8 완료, Step 9 진입.
4. 이 예약 작업의 이후 실행 규칙. v2.1은 다시 공격하지 않는다. 이번 치명이 v2의 새 부품 정의에서 나온 내부 모순이었고 v2.1은 그 정의를 닫은 것이라, 다음 라운드의 지적은 반영 후 재검증(2항)이 잡는 쪽이 비용이 낮다. 사용자 판정이 문서에 나타나기 전까지는 판정 유무만 확인하고 조용히 종료한다. 이 규칙을 08-2가 적었는데 이번 실행이 한 번 넘어선 이유는 0-1에 있고, v2.1에는 그 이유가 적용되지 않는다(v2는 미검증 신규안이었고 v2.1은 검증된 안의 정의 정정이다).
