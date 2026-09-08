# 공간과 숙박 O2O 서비스 형태 정리

작성일: 2026-08-25

문서 구분: 공간과 숙박 서비스의 유형을 비교하는 학습 참고 자료다. 아래 SQL, 상태 모델과 TTL 수치를 현재 프로젝트의 확정 설계로 간주하지 않는다.

이번 프로젝트는 박 단위 숙박과 객실 타입별 날짜 재고를 다룬다. 시간제 대실, 다중 판매 채널, 자동 가격 조정과 실제 PG 연동은 이번 API 범위에 없다. Hold는 Booking의 HELD 상태이며, 결제 응답을 기다리는 동안 DB 트랜잭션과 락을 계속 유지하지 않는다.

## 1. 한 줄 정리

공간과 숙박 O2O는 이동이 불가능한 고정 자원(객실, 룸, 자리, 주차면)을 시간 단위로 쪼개 파는 소멸성 재고 판매업이며, 배달형이 주문 상태 머신 싸움이라면 이쪽은 재고 캘린더와 채널 동기화 싸움이다.

## 2. 배달형과 결정적으로 다른 네 가지

| 항목 | 배달형 | 공간과 숙박형 |
|---|---|---|
| 자원 | 이동한다 (라이더, 상품) | 고정되어 있다 (건물, 방) |
| 재고 축 | 스칼라 (남은 수량) | 자원 x 시간 (2차원 캘린더) |
| 시간 제약 | 지금부터 30분 | 미래 특정 구간을 선점 |
| 판매 채널 | 대체로 단일 앱 | 야놀자 + 여기어때 + 네이버 + 자사몰 동시 판매 |

여기서 나오는 결론이 이 도메인의 전부다.

- 배차와 물류가 없다. 대신 위치가 고정이라 지역 검색, 지도, 반경 필터가 코어다.
- 재고가 소멸한다. 오늘 안 팔린 오늘 방은 영원히 못 판다. 그래서 다이나믹 프라이싱과 막판 할인이 구조적으로 필연이다.
- 예약이 미래를 점유한다. 취소, 노쇼, 환불 정책이 도메인 로직의 큰 덩어리를 차지한다.
- 멀티채널 판매가 기본이라 오버부킹이 이 업계의 상시 장애다.

## 3. 형태를 가르는 4개 축

### 축 1. 시간 단위 (재고 모델을 통째로 결정하는 축)

| 단위 | 대표 서비스 | 재고 모델 | 백엔드 난이도 |
|---|---|---|---|
| 분 단위 | 모두의주차장, 아이파킹, 카카오T 주차 | 실시간 가용 면 수 (센서/차단기 연동) | IoT 연동, 정산 |
| 시간 단위 | 스페이스클라우드, 아워플레이스, 스터디룸, 연습실, 회의실 | 하루를 시간 슬롯으로 분할 | 구간 겹침 검사 |
| 대실 + 숙박 | 모텔 (야놀자, 여기어때) | 같은 객실에 대실 2~3회와 숙박 1회가 하루 안에 공존 | 이중 재고, 시간 충돌 |
| 박 단위 | 호텔, 펜션, 리조트, 에어비앤비 | 날짜 범위(date range) 재고 | 다일자 원자적 점유 |
| 주/월 단위 | 패스트파이브, 스파크플러스, 셀프스토리지, 단기임대 | 계약/구독형 | 계약 갱신, 청구 |

같은 숙박이라도 대실은 시간 축, 호텔은 날짜 축이라 테이블 설계가 다르다. 도메인이 아니라 이 축으로 묶어야 한다.

### 축 2. 예약 확정 방식

| 방식 | 흐름 | 예시 | 설계 영향 |
|---|---|---|---|
| 즉시 확정 | 결제 완료 = 확정 | 모텔 대실, 주차, 스터디룸 | 재고 락이 결제 트랜잭션에 걸림 |
| 요청 후 승인 | 요청 → 호스트 수락 → 결제 | 에어비앤비 일부, 아워플레이스 촬영 | 임시 홀드 + TTL 만료 필요 |
| 견적형 | 문의 → 협의 → 계약 | 웨딩홀, 대형 행사장, 공유오피스 | 재고보다 리드 관리가 코어 |

즉시 확정만 있다고 가정하고 설계하면, 나중에 요청형을 붙일 때 재고 테이블을 갈아엎어야 한다. 처음부터 확정 재고와 홀드 재고를 분리해 두는 게 안전하다.

### 축 3. 재고 소유 구조

- 순수 중개(OTA): 야놀자, 여기어때, 스페이스클라우드. 재고는 호스트 것이고 플랫폼은 노출과 결제만 담당한다. 진입은 쉽지만 재고 정확도를 통제할 수 없어 오버부킹 책임 문제가 생긴다.
- 직영/위탁 운영: 호텔 브랜드, 운영 대행. 재고를 직접 통제하니 정확하지만 자본이 든다.
- 솔루션형(B2B SaaS): PMS와 CMS를 파는 형태. 야놀자가 실제로 이동한 방향이다. 예약 앱보다 진입장벽이 높고 락인이 강하다.

### 축 4. 수익 모델

- 예약 수수료: 거래액의 일정 비율
- 광고/노출: 숙박 O2O는 수수료보다 상단 노출 광고 경쟁이 더 치열하다
- B2B SaaS 구독: PMS/CMS 월 이용료 + 예약 건당 수수료
- 직영 마진, 부가 상품(레저 티켓, 렌터카, 항공 묶음)

## 4. 국내 카테고리 지도

| 카테고리 | 대표 | 시간 단위 | 확정 방식 | 주 수익 |
|---|---|---|---|---|
| 종합 숙박 | 야놀자, 여기어때 | 대실 + 박 | 즉시 확정 | 수수료 + 광고 |
| 글로벌 OTA | 아고다, 부킹닷컴, 에어비앤비 | 박 | 즉시/승인 혼합 | 수수료 |
| 생활 공간 대여 | 스페이스클라우드 | 시간 | 즉시 확정 | 수수료 |
| 촬영 공간 | 아워플레이스 | 시간 | 요청 후 승인 | 수수료 |
| 공유 오피스 | 패스트파이브, 스파크플러스 | 월 | 견적/계약 | 임대 마진 |
| 주차 | 모두의주차장, 아이파킹, 카카오T 주차 | 분 | 즉시 확정 | 주차료 + 솔루션 |
| 숙박 솔루션 | 야놀자 클라우드, 산하정보기술, 각종 CMS | - | - | SaaS 구독 |

## 5. 백엔드 코어: 이 도메인의 진짜 난이도 4개

### 5-1. 날짜 범위 재고 (가장 중요)

음식 주문은 재고가 스칼라지만, 숙박은 날짜 축이 붙는다. 3박 예약은 3개의 독립된 일자 재고를 동시에 점유해야 하고, 하루라도 실패하면 전체가 실패해야 한다.

정석은 일자별로 재고를 미리 펼쳐 두는 것이다.

```java
// (room_type_id, stay_date) 유니크. 재고를 날짜 단위로 펼쳐서 관리한다
@Entity
@Table(
    name = "room_inventory",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_type_id", "stay_date"})
)
public class RoomInventory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roomTypeId;
    private LocalDate stayDate;
    private int totalCount;
    private int soldCount;

    public void reserve(int count) {
        if (soldCount + count > totalCount) {
            throw new SoldOutException(roomTypeId, stayDate);
        }
        this.soldCount += count;
    }

    public void release(int count) {
        this.soldCount = Math.max(0, this.soldCount - count);
    }
}
```

```java
public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {

    // 체크인 당일부터 체크아웃 전날까지가 점유 대상 (체크아웃 날은 팔지 않는다)
    // order by stay_date 고정: 여러 예약이 겹칠 때 락 획득 순서를 통일해 데드락을 막는다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select i from RoomInventory i
        where i.roomTypeId = :roomTypeId
          and i.stayDate >= :checkIn
          and i.stayDate < :checkOut
        order by i.stayDate asc
        """)
    List<RoomInventory> findForUpdate(@Param("roomTypeId") Long roomTypeId,
                                      @Param("checkIn") LocalDate checkIn,
                                      @Param("checkOut") LocalDate checkOut);
}
```

```java
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final RoomInventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Long reserve(ReserveCommand cmd) {
        long nights = ChronoUnit.DAYS.between(cmd.checkIn(), cmd.checkOut());
        if (nights <= 0) {
            throw new InvalidStayPeriodException(cmd.checkIn(), cmd.checkOut());
        }

        List<RoomInventory> inventories =
            inventoryRepository.findForUpdate(cmd.roomTypeId(), cmd.checkIn(), cmd.checkOut());

        // 재고 row가 없는 날 = 판매 오픈 기간 밖. 부분 예약이 성립하면 안 되므로 개수부터 검증
        if (inventories.size() != nights) {
            throw new NotOnSaleException(cmd.roomTypeId(), cmd.checkIn(), cmd.checkOut());
        }

        // 하루라도 SoldOut이면 예외가 터지고 트랜잭션 전체가 롤백된다
        inventories.forEach(inv -> inv.reserve(cmd.roomCount()));

        Reservation reservation = Reservation.pending(cmd);
        return reservationRepository.save(reservation).getId();
    }
}
```

주의할 점 세 가지.

- 체크아웃 날짜는 점유하지 않는다. 3/1 입실 3/4 퇴실이면 3/1, 3/2, 3/3 세 날짜만 잡는다. 이 off-by-one이 이 도메인에서 가장 흔한 버그다.
- 락 획득 순서를 날짜 오름차순으로 고정한다. A가 3/1→3/3 순으로, B가 3/3→3/1 순으로 잡으면 데드락이 난다.
- 결제가 외부 PG를 타면 트랜잭션이 길어진다. 재고를 확정 차감하지 말고 홀드 상태로 잡은 뒤 TTL(보통 10~15분) 안에 결제 완료되면 확정, 아니면 스케줄러가 반납하는 구조로 가야 한다.

### 5-2. 오버부킹과 채널 동기화

숙박업의 실제 구조는 3층이다.

```
OTA (야놀자 / 여기어때 / 네이버 / 아고다 / 자사몰)
        ↕  재고와 요금을 양방향 동기화
CMS (채널매니저) ─ 여러 채널의 재고를 한 화면에서 통합 관리
        ↕
PMS (숙소 내부 시스템) ─ 객실 배정, 체크인/아웃, 청소 스케줄, 정산
```

CMS 없이 수동으로 운영하면, 야놀자에 예약이 들어온 뒤 사장님이 여기어때와 네이버에 로그인해 마감 처리하기까지 7~11분의 공백이 생기고, 그 사이 같은 객실이 중복 판매된다. 이게 오버부킹의 실제 발생 경로다.

설계 원칙.

- 재고의 단일 진실 원천(SSOT)을 하나로 정한다. 보통 CMS 또는 PMS다. OTA는 그 사본을 들고 있을 뿐이다.
- 예약 발생을 이벤트로 잡아 모든 채널에 재고를 push한다. push 실패는 재시도 큐로 보낸다.
- 그래도 네트워크 지연은 남으니 완전 방지는 불가능하다. 채널별 판매 가능 수를 실제 재고보다 낮게 잡는 버퍼, 그리고 오버부킹 발생 시 업그레이드나 대체 숙소 보상 정책을 도메인에 넣어 둔다.
- OTA 쪽 재고 갱신은 멱등해야 한다. 절대 수량(남은 방 3개)을 보내지, 증감(-1)을 보내면 안 된다. 증감은 재시도 시 어긋난다.

### 5-3. 대실과 숙박의 이중 재고 (국내 특유)

같은 객실 하나가 하루 안에 대실 4시간 두 번 + 숙박 한 번을 받을 수 있다. 일자 단위 재고만으로는 표현이 안 되고, 시간 구간 겹침 검사가 필요하다.

```java
// MySQL에는 range 타입이 없으므로 시작/종료 컬럼 + 겹침 조건으로 처리한다
// 겹침 조건: 기존.start < 신규.end AND 기존.end > 신규.start
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    select r from RoomOccupancy r
    where r.roomId = :roomId
      and r.status <> 'CANCELED'
      and r.startAt < :endAt
      and r.endAt   > :startAt
    """)
List<RoomOccupancy> findOverlapping(@Param("roomId") Long roomId,
                                    @Param("startAt") LocalDateTime startAt,
                                    @Param("endAt") LocalDateTime endAt);
```

여기에 청소 시간(turnover buffer)을 더해야 한다. 11시 퇴실 후 곧바로 11시 입실은 불가능하므로, 겹침 검사 시 종료 시각에 30분~1시간을 더해서 비교한다. 이걸 빼먹으면 운영에서 바로 사고가 난다.

시간 단위 공간 대여(스터디룸, 파티룸, 촬영 스튜디오)도 정확히 같은 모델을 쓴다. 대실 로직을 잘 만들어 두면 공간 대여로 그대로 확장된다.

### 5-4. 다이나믹 프라이싱

가격이 상품에 붙어 있지 않고 날짜에 붙는다. 요일, 시즌, 공휴일, 잔여율, 리드타임에 따라 달라지므로 재고 테이블과 같은 축(자원 x 날짜)으로 요금 테이블을 만든다.

```
room_rate (room_type_id, stay_date, base_price, min_los, closed)
```

min_los(최소 숙박일)와 closed(판매 중지) 같은 판매 제약도 같은 row에 두면, 검색 쿼리 한 번으로 판매 가능 여부와 가격을 함께 계산할 수 있다. 3박 예약의 총액은 세 날짜 요금의 합이지 1박 요금 x 3이 아니다.

## 6. 최소 도메인 모델 스케치

```
Property (숙소/공간)     1 --- N  RoomType (객실 타입/룸)
RoomType                 1 --- N  Room (실제 호실)
RoomType                 1 --- N  RoomInventory (room_type_id, stay_date, total, sold)
RoomType                 1 --- N  RoomRate      (room_type_id, stay_date, price, min_los, closed)
Reservation              N --- 1  RoomType   (status, check_in, check_out, guest, amount)
RoomOccupancy            N --- 1  Room       (start_at, end_at, type: STAY/DAY_USE)
ChannelMapping           N --- 1  RoomType   (channel, external_room_id, last_synced_at)
```

핵심은 RoomType(파는 단위)과 Room(실제 호실)을 분리하는 것이다. 고객은 디럭스 더블을 사지 305호를 사지 않는다. 호실 배정은 체크인 시점에 PMS가 한다. 처음부터 호실 단위로 팔면 재고 활용률이 떨어지고 배정 유연성이 사라진다.

## 7. 2025~2026 흐름

- 국내는 야놀자와 여기어때 양강 구도이고, 여기어때가 앱 설치와 사용 시간 지표에서 격차를 좁혀 왔다.
- 야놀자는 예약 플랫폼을 넘어 솔루션 기업으로 이동 중이다. 클라우드 PMS와 CMS를 글로벌에 팔고, 인도 호스피탈리티 솔루션 기업 인키를 인수해 대형 호텔 영역까지 확장했다. 예약 앱보다 진입장벽이 높고 락인이 강하다는 판단이다.
- 여기어때는 플랫폼 확장 노선이다. 일본 고급 숙박 플랫폼 리럭스를 인수해 하이엔드 상품군과 인바운드 수요를 노린다.
- 숙박 O2O의 경쟁축은 수수료보다 상단 노출 광고에 가깝다. 공급자는 광고비로, 플랫폼은 노출 지면으로 수익을 낸다.
- 공간 대여는 파티룸, 연습실, 스터디룸, 촬영 스튜디오로 세분화되어 시간제 대여가 독립 카테고리로 정착했다.
- 주차는 카카오, 티맵 등 모빌리티 대기업과 아이파킹 같은 전문 사업자가 각축 중이며, 차단기와 번호판 인식 하드웨어 연동이 진입장벽 역할을 한다.

## 8. 이어서 볼 키워드

- 소멸성 재고와 Revenue Management: ADR, RevPAR, Occupancy. 이 도메인의 KPI 언어다.
- 구간 겹침(interval overlap) 쿼리와 배타 제약: PostgreSQL이면 tstzrange + EXCLUDE 제약으로 DB가 직접 막아준다. MySQL이면 애플리케이션 락으로 풀어야 한다.
- 홀드와 TTL: 결제 대기 동안의 재고 선점. Redis 분산 락 또는 상태 컬럼 + 만료 스케줄러.
