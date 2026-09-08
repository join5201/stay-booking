# O2O 숙박 예약 DDD 설계 진행 계획

최초 작성: 2026-08-27
최종 갱신: 2026-09-07 v9 (API 계약과 구현 범위 연결, 로컬 경로와 보드 영역 정정)
입력 문서: [기능 목록](C:/Dev/potenup/99_projects/o2o/02-o2o-feature-list.md), [이벤트 스토밍](C:/Dev/potenup/99_projects/o2o/03-o2o-event-storming.md)
FigJam 보드: https://www.figma.com/board/ffhYVMh8awMBfqLinXFMDY

문서 규칙: 새로 추가되거나 내용이 바뀐 절에는 제목 옆에 반영 날짜를 표기한다.

이 문서는 로컬 v8을 수정한 v9다. 다른 계획에서 참조한 v19를 확보하거나 대체한 것이 아니다. 아래 완료 표시는 당시 설계 기록이며 서비스 구현과 테스트 완료를 뜻하지 않는다.

## 0. 현재 적용 범위 (2026-09-07)

서비스는 Next.js 프론트, Spring 기반 Java 백엔드, MySQL을 사용해 로컬에서 개발하고 검증한다. 배포와 운영 준비는 제외한다. 생성과 수정은 Claude Code, 평가는 Codex의 별도 작업에서 수행하고 사용자가 결과를 전달한다. 별도 LLM API를 호출하지 않는다.

[API 명세 v2](C:/Dev/potenup/99_projects/o2o/11-o2o-api-spec.md)는 요청과 응답, 상태 처리, 멱등 규칙과 검증 시나리오를 정의한 초안이다. P01~P11은 제안값이다. TTL 10분, 수수료 0, 할인 선택 방식과 단일 트랜잭션 후보를 이 문서의 확정 전제로 자동 편입하지 않는다.

## 1. 확정된 전제

| 항목 | 결정 | 날짜 |
|---|---|---|
| 도메인 모델과 JPA 엔티티 | 분리하지 않는다 | 08-27 |
| 프로젝트 구조 | 단일 Spring Boot 모듈 + 컨텍스트별 패키지 분리 | 08-27 |
| 진행 순서 | 설계 문서를 먼저 확정하고 코드로 내려간다 | 08-27 |
| 결제 | Mock. 단 Hold TTL 설계는 실제 PG 지연을 전제로 한다 | 08-27 |
| 쿠폰 | v1 제외 (학습 범위 결정) | 09-01 |
| 프로모션 | 자동 적용형만, 수량 제한 없음 | 09-01 |
| 재고 점유 | 예약이 재고에 커맨드를 보낸다. 직접 만지지 않는다 | 09-01 |
| 산출물 배치 | 구조(컨텍스트, 애그리거트, 정책)는 FigJam, 서술(용어, 근거, 계약)은 문서 | 09-01 v6 |
| Hold | Booking에 흡수. 별도 애그리거트가 아니다 | 09-01 v7 |
| 결제 요청 경로 | 게스트 → 예약 컨텍스트(총액 첨부) → 결제 컨텍스트. 결제는 예약을 읽지 않는다 | 09-01 v8 |
| 금액 스냅샷 시점 | 예약 생성 시 | 09-01 v8 |
| HELD 종착 | EXPIRED 하나. TTL 만료와 결제 3회 실패를 가르지 않는다 | 09-01 v7 |

## 2. 9단계 계획

### Step 1-2. Domain Events 탐색 + 타임라인 정렬 (완료)

자료 02, 03. 산출물 [이벤트 스토밍](C:/Dev/potenup/99_projects/o2o/03-o2o-event-storming.md), FigJam 영역 1. 이벤트 22개 (v7: Hold 흡수로 24에서 22).

### Step 2.5. 엔티티별 그룹핑 (완료)

산출물 FigJam 영역 2. 애그리거트 후보 7개 (v7: Hold 흡수로 8에서 7).

### Step 3. Commands + Actors 추가 (완료)

자료 04. 산출물 [커맨드와 액터](C:/Dev/potenup/99_projects/o2o/04-o2o-commands-actors.md), FigJam 영역 3. 커맨드 22개 (v7).
액터 유형이 넷이다. 사람, 외부 시스템, 다른 컨텍스트, 정책과 스케줄러.

### Step 4. 바운디드 컨텍스트 + 용어 사전 (완료, 2026-09-01 v6: 산출물 재배치)

자료 05.
- 컨텍스트 명세와 의존 관계: FigJam 영역 4
- 용어 사전: [용어 사전](C:/Dev/potenup/99_projects/o2o/05-o2o-glossary.md)

컨텍스트 5개(숙소 카탈로그, 재고와 요금, 프로모션, 예약, 결제)와 읽기 모델 1개(검색).

### Step 5. 컨텍스트 맵 (외교 관계도)

자료 99. 산출물 FigJam 영역 4에 화살표와 관계 유형 추가.

커맨드를 보내는 쪽이 Downstream, 받는 쪽이 Upstream이다. 이벤트 구독은 별도로 표기한다.

| 관계 패턴 | 기술 구현 | 결합도 |
|---|---|---|
| Customer-Supplier | 직접 API 호출 (동기) | 높음 |
| Conformist + ACL | 번역기를 거친 호출 | 중간 |
| Publisher-Subscriber | 메시지 큐 (비동기) | 매우 낮음 |
| Shared Kernel | 공유 라이브러리 | 매우 높음 |

공유 커널 후보는 Money, StayPeriod, Typed ID.

### Step 6. 애그리거트 도출 + CRC 카드 (2026-09-07)

자료 06. 예정 산출물은 FigJam 영역 6의 애그리거트 경계와 o2o-aggregates.md의 CRC 카드다. 해당 CRC 파일은 아직 확인하지 못했다. 영역 5는 컨텍스트 맵이다.

아래 4절의 불변식 후보를 입력으로 COT 절차를 적용한다.

1. 규칙 식별
2. 함께 변경 묶음
3. 책임자 지정
4. 경계 밖은 이벤트
5. 작게 유지

여기서 결론낼 것. 3절의 컨텍스트 간 원자성, ReleaseInventory를 원인별로 나눌지(핫스팟 2). Hold 흡수와 만료/해제 통합은 재검증 결함 때문에 v7에서 앞당겨 결정했다.

### Step 7. DbC 계약 명세

자료 07. 산출물 aggregates 문서에 통합.

| 요구사항 | DbC 분류 | 책임 컨텍스트 |
|---|---|---|
| 연박 부분 부족 시 전체 실패 | 선행조건 + 불변식 | 재고(원자성) + 예약(조율) |
| 초과 예약 0 | 불변식 | 재고 |
| 멱등 재시도 | 선행조건 + 후행조건 | 예약 |
| HELD 종료 시 재고 반환 | 정책 + 후행조건 | 예약(정책) + 재고(실행) |
| 확정 금액 불변 | 시간적 불변식 (생성 시점부터) | 예약 |
| 예약당 승인 결제 하나 | 선행조건 + 불변식 | 결제 (v8 추가) |

1번과 4번이 두 컨텍스트에 걸친다. 3절의 컨텍스트 간 원자성 문제와 직결된다.

산출물 셋. 불변식 카탈로그, 행동별 Pre-Invariant-Post 계약표, 상태전이와 가드 표.

### Step 8. 프로세스 정책 (Policy / Saga, 2026-09-07)

자료 08. 예정 산출물은 FigJam 영역 7의 정책 지도와 o2o-policies.md의 정책 카드다. 해당 정책 카드 파일은 아직 확인하지 못했다.

Step 3 이후 정한 Hold 종료 정책은 자료 03 기준으로 조기 도입이다. 여기서 재판정한다.

Saga 판정 기준 (v7 추가): 자료 08 기준으로 정책이 중간 상태를 보관해야 하면 Saga, 이벤트 하나 받아 커맨드를 쏘고 잊으면 Stateless다. 아래 표에서 커맨드가 둘 이상인 행은 전부 같은 기준으로 다시 판정한다.
이 판정은 3-2와 묶여 있다 (v8). 3-2에서 "동기 + 같은 트랜잭션"을 택하면 판정 필요 세 행은 자동으로 Stateless가 된다. 중간 상태를 보관할 틈이 없기 때문이다. Step 6에서 3-2를 정하면 이 표는 따라온다. 따로 판정하지 않는다.

| 정책 이름 | 트리거 | 가드 | 실행 커맨드 | Saga 여부 |
|---|---|---|---|---|
| 결제 승인 시 예약 확정 | PaymentApproved | status == HELD, now < expiresAt | ConfirmBooking, CommitInventory × N | 3-2에 종속 |
| 승인 지연 시 자동 환불 | PaymentApproved | status == EXPIRED | RefundPayment | Stateless |
| 결제 실패 시 만료 | PaymentFailed | attemptCount >= 3 | ExpireBooking(PAYMENT_FAILED) | Stateless |
| TTL 만료 | 스케줄러 (시간 정책) | status == HELD, expiresAt <= now | ExpireBooking(TTL_EXPIRED) | Stateless |
| 만료 시 재고 반환 | BookingExpired | 없음 | ReleaseInventory × N | 3-2에 종속 |
| 예약 취소 | BookingCanceled | 없음 | RefundPayment, ReleaseInventory × N | 3-2에 종속 |

v7의 "예약 생성 시 결제 요청" 행은 v8에서 삭제했다. RequestPayment는 게스트 커맨드다. 스케줄러 행은 v8에서 추가했다. 시간을 트리거로 쓰는 정책으로 본다.

승인 지연 가드가 == EXPIRED인 이유 (v7): != HELD로 두면 중복 도착한 승인 콜백이 CONFIRMED 예약을 환불한다. 중복 승인은 결제 컨텍스트가 pgTransactionId로 걸러 이벤트를 다시 내지 않는다.
ExpireBooking의 선행조건 status == HELD가 확정 예약을 보호한다.

API 초안과의 시간 경계는 다음과 같다. 승인 처리 시 이미 expiresAt에 도달한 HELD는 먼저 만료시키고 지연 승인 환불로 처리한다. 스케줄러 실행이 늦었다는 이유로 만료된 예약을 확정하지 않는다. TTL 수치와 트랜잭션 구현 선택은 별도 미결 사항이다.

### Step 9. 구현 매핑과 검증 (2026-09-07)

패키지 구조, Repository, Entity/VO, 구조 검사와 계약표의 테스트 변환을 다룬다. API 명세의 상태와 응답 모델이 JPA 모델의 구조를 그대로 결정하지는 않는다.

| 작업 | 산출물 | 완료 조건 |
|---|---|---|
| 입력 고정 | 사용할 설계 버전, API ID, 채택한 정책 목록 | 미결 정책과 확정 정책 구분 |
| 구현 매핑 | 패키지, 저장 모델, 호출과 트랜잭션 경계 | 설계 계약의 책임과 코드 위치 연결 |
| 테스트 변환 | 단위, MySQL 통합, API와 동시성 테스트 계획 | API 명세의 T01~T30 중 해당 항목 연결 |
| 기능별 구현 계획 | 백엔드, 프론트, 연결 테스트 순서 | 기능 하나마다 검증 가능한 작업 크기로 분할 |

Step 9 이후 실제 작업은 초기 세팅, 백엔드와 테스트 코드, 프론트, 연결 테스트, 최종 검증을 기능 단위로 반복한다. API 명세 작성만으로 Step 5~8의 미결 설계나 Step 9 실행이 완료되지는 않는다.

## 3. 최대 쟁점: 원자성 두 층

### 3-1. 재고 컨텍스트 안: DailyInventory N개를 어떻게 원자적으로 잡나

| 안 | 내용 | 자료 규칙과의 충돌 |
|---|---|---|
| A | DailyInventory를 (roomTypeId, stayDate) 단위 애그리거트로 | 규칙 3 위반 (한 트랜잭션에 N개 수정) |
| B | RoomTypeInventory 루트 + 날짜 컬렉션 | 규칙 4 위반 (거대 애그리거트, 락 경쟁) |
| C | A + 도메인 서비스가 조율, 강한 일관성 정책으로 문서화 | 규칙 3을 의도적 예외로 선언 |

권고는 C다. 자료 07의 정책 분류에서 내부 정책은 동기 처리와 강한 일관성을 인정한다.

### 3-2. 컨텍스트 사이: 예약이 재고에 보내는 커맨드가 같은 트랜잭션인가 (2026-09-01 v7: 범위 확장)

RequestBooking이 성공하려면 InventoryHeld × N과 BookingCreated가 전부 성공해야 한다.
같은 모양의 문제가 세 곳에 있다. 한 곳만 다루면 안 된다.

| 경로 | 예약 쪽 | 재고 쪽 |
|---|---|---|
| 생성 | BookingCreated | HoldInventory × N |
| 확정 | ConfirmBooking | CommitInventory × N |
| 만료, 취소 | BookingExpired, BookingCanceled | ReleaseInventory × N |

셋에 같은 답을 적용한다. 생성만 동기 트랜잭션이고 나머지는 이벤트 기반이면 정합성 모델이 경로마다 달라져 Step 7 계약이 세 벌이 된다.

| 안 | 내용 | 대가 |
|---|---|---|
| 동기 + 같은 트랜잭션 | 예약 서비스가 재고 서비스를 직접 호출, 하나의 DB 트랜잭션 | 컨텍스트 경계가 트랜잭션 경계와 일치하지 않음 |
| 동기 + 별도 트랜잭션 | 재고 점유 먼저 커밋, 예약 생성 실패 시 보상 커맨드 | 중간 상태 노출. 보상 실패 시 재고 누수 |
| Saga | 예약이 프로세스 상태를 들고 조율 | 복잡도 상승 |

단일 모듈 전제이므로 첫 번째가 가능하고 가장 단순하다. 컨텍스트 경계를 트랜잭션이 넘는 구조임을 명시해야 하고, MSA로 쪼개면 이 지점이 Saga로 바뀐다. Step 6에서 확정한다.

첫 번째 안을 택하면 자료 07의 분류에서 벗어난다 (v7 추가). 자료 07은 컨텍스트를 넘는 외부 정책을 비동기와 결과적 일관성으로 분류한다. 이 프로젝트는 단일 모듈이라는 이유로 예약과 재고 사이를 동기 강한 일관성으로 묶는 것이고, 이것이 자료의 일반 규칙에서 의도적으로 벗어나는 지점임을 Step 7 계약표 머리에 적는다.

## 4. Step 6 입력: 규칙 후보 (2026-09-01 v7: 불변식과 선행조건으로 분류, 3개 추가)

자료 06의 분류를 따른다. 불변식은 애그리거트가 어느 시점에나 지키는 것이고, 선행조건은 특정 커맨드를 받을 때만 검사하는 것이다. 첫 판에서 둘을 섞어 두어 Step 6 경계 판단 근거로 쓸 수 없다는 지적이 있었다.

### 4-1. 불변식 (항상 참)

| 불변식 | 종류 | 컨텍스트 | 애그리거트 후보 |
|---|---|---|---|
| totalCount >= soldCount + heldCount | 내부 | 재고 | DailyInventory |
| Rate > 0 | 내부 | 재고 | DailyRate |
| checkIn < checkOut | 내부 | 예약 | Booking |
| BookingCreated 이후 PriceSnapshot 불변 | 시간적 | 예약 | Booking (v8: 시점을 확정에서 생성으로) |
| CONFIRMED는 EXPIRED로 전이하지 않는다 | 시간적 | 예약 | Booking (v7 추가) |
| attemptCount <= 3 | 내부 | 결제 | Payment |
| 예약당 APPROVED 결제는 하나 | 내부 | 결제 | Payment (v7 추가) |
| campaignPeriod.start < campaignPeriod.end | 내부 | 프로모션 | Promotion |

### 4-2. 선행조건 (커맨드 시점 검사)

| 선행조건 | 커맨드 | 컨텍스트 | 참조하는 것 |
|---|---|---|---|
| totalCount를 soldCount + heldCount 아래로 내릴 수 없다 | AdjustInventory | 재고 | 자기 상태 |
| guestCount <= maxOccupancy | RequestBooking | 예약 | 카탈로그 조회 (연관) |
| 숙박 기간의 모든 날짜에 요금이 존재해야 한다 | RequestBooking | 예약 | 재고 조회 (연관) |
| status == HELD | ConfirmBooking, ExpireBooking | 예약 | 자기 상태 |
| status == CONFIRMED | CancelBooking | 예약 | 자기 상태 |
| PriceSnapshot 총액 == 청구 Amount | RequestPayment | 결제 | 예약이 실어 보낸 값 (v7 추가) |
| 같은 pgTransactionId의 승인은 무시한다 | RecordPaymentApproval | 결제 | 자기 상태 (v7 추가) |
| booking status == HELD, attemptCount < 3, REQUESTED 상태의 시도가 없을 것 | RequestPayment | 결제 (예약이 검사해 넘김) | 예약 상태 + 자기 상태 (v8 추가) |

RequestPayment의 세 번째 조건이 없으면 2회째 시도가 PG에서 지연되는 동안 3회째가 나가 둘 다 승인될 수 있다. 거래 번호가 다르므로 pgTransactionId 멱등으로는 못 막고, "예약당 APPROVED 하나" 불변식이 깨진다. 진행 중인 시도가 있으면 새 시도를 받지 않는 것으로 막는다.

### 4-3. 불변식이 아닌 것 (v8 추가)

"같은 멱등키로 예약은 하나만"은 인스턴스 사이의 유일성이라 애그리거트 내부 불변식이 아니다. DB 유니크 제약과 RequestBooking 선행조건(같은 키의 예약이 없을 것)으로 옮긴다.

연관 참조가 있는 둘(인원, 요금 존재)은 다른 컨텍스트를 읽는다. Step 5에서 카탈로그와 재고가 예약의 상류라는 근거가 된다.

## 5. 나머지 쟁점

### 쟁점 2. Hold의 소속 (2026-09-01 v7: 해소)
Booking에 흡수했다. RequestBooking이 직접 건드리는 애그리거트가 하나가 됐고, HoldExpired와 HoldReleased가 BookingExpired 하나로 합쳐졌다. 근거는 event-storming 핫스팟 3.

### 쟁점 3. 가격 계산의 소속
도메인 서비스로 두고 결과를 VO로 반환한다. 검색과 예약이 같은 계산기를 호출한다.

### 쟁점 4. 검색은 도메인 모델을 쓰지 않는다
조회 전용 쿼리와 DTO. 도메인 계산은 각 컨텍스트의 서비스를 호출하고 검색은 조합만 한다.

### 쟁점 5. Payment의 식별 단위 (2026-09-01 v8 추가, Step 6에서 결정)
attemptCount <= 3과 "예약당 APPROVED 하나"를 Payment 내부 불변식으로 두려면 Payment가 예약당 하나이고 시도 목록을 안에 들어야 한다.

| 안 | 내용 | 대가 |
|---|---|---|
| 예약당 하나 + 시도 목록 | Payment(bookingId) 안에 PaymentAttempt N개. 불변식 둘을 안에서 지킨다 | 시도 상태 FAILED 이후 새 시도를 여는 전이가 필요. 자료 07 기준 역행이 아니라 새 하위 엔티티 생성으로 모델링해야 한다 |
| 시도당 하나 | Payment 하나가 시도 하나. 단순 | attemptCount와 "예약당 하나"가 애그리거트 밖(도메인 서비스 또는 DB 제약)으로 나간다 |

지금 4-1이 둘을 Payment 불변식으로 분류했으므로 첫째 안을 전제로 쓰고 있다. Step 6에서 확정한다.

## 6. JPA 엔티티를 도메인 모델로 쓸 때의 규칙

- 애그리거트 경계를 넘는 연관관계에 @ManyToOne을 쓰지 않는다. ID 타입 VO로 참조한다
- public setter를 만들지 않는다
- VO는 @Embeddable + protected 기본 생성자 + 값 기반 equals/hashCode
- 애그리거트 내부 컬렉션만 cascade + orphanRemoval
- 검증 책임을 레이어로 분리한다
