package com.o2o.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.o2o.booking.application.BookingApplicationService;
import com.o2o.booking.application.RequestBookingCommand;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.booking.domain.IdempotencyKey;
import com.o2o.booking.domain.UserId;
import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.RoomType;
import com.o2o.inventory.application.InventoryApplicationService;
import com.o2o.inventory.domain.DailyInventory;
import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.Payment;
import com.o2o.payment.domain.PaymentAttempt;
import com.o2o.payment.domain.PaymentRepository;
import com.o2o.shared.HostId;
import com.o2o.shared.Money;
import com.o2o.shared.RoomTypeId;

/**
 * 2차 테스트의 공통 준비. 1차 테스트가 클래스마다 되풀이하던 숙소와 객실 타입과 재고와 요금과
 * HELD 예약 만들기를 한곳에 둔다. 테스트 클래스가 @TestConfiguration의 @Bean으로 등록한다.
 *
 * 데이터를 되돌리지 않는다(1차 BookingApplicationServiceTest와 같은 이유. 커밋이 실제로 일어나야
 * AFTER_COMMIT 구독자와 REQUIRES_NEW가 돈다). 그래서 테스트마다 객실 타입을 새로 만들고 지역
 * 코드도 새로 뽑아 프로모션이 섞이지 않게 한다. 시각은 테스트가 돌리는 시계에서 읽는다.
 */
public class BookingFixtures {

    public static final HostId HOST = HostId.of("host_001");
    public static final UserId GUEST = UserId.of("guest_001");
    public static final UserId OTHER_GUEST = UserId.of("guest_002");
    public static final LocalDate CHECK_IN = LocalDate.parse("2026-10-10");
    public static final long RATE = 100_000L;

    private final CatalogApplicationService catalogService;
    private final InventoryApplicationService inventoryService;
    private final BookingApplicationService bookingService;
    private final BookingRepository bookingRepository;
    private final DailyInventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionTemplate transaction;
    private final MutableClock clock;

    public BookingFixtures(CatalogApplicationService catalogService,
                           InventoryApplicationService inventoryService,
                           BookingApplicationService bookingService,
                           BookingRepository bookingRepository,
                           DailyInventoryRepository inventoryRepository,
                           PaymentRepository paymentRepository,
                           PlatformTransactionManager transactionManager, MutableClock clock) {
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.inventoryRepository = inventoryRepository;
        this.paymentRepository = paymentRepository;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /** 지역 코드를 새로 뽑아 어느 프로모션에도 걸리지 않는 객실 타입. 최대 인원 2 */
    public RoomTypeId roomType() {
        return roomType(newRegion());
    }

    public RoomTypeId roomType(String regionCode) {
        Property property = catalogService.registerProperty(
                HOST, "2차 테스트 스테이", regionCode, "서울특별시 중구 예시로 1", "");
        RoomType roomType = catalogService.registerRoomType(HOST, property.id(), "스탠다드", 2, "");
        return roomType.id();
    }

    public static String newRegion() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** from부터 nights 밤을 재고 totalCount와 요금 RATE로 연다. 체크아웃 날짜는 열지 않는다 */
    public void open(RoomTypeId roomTypeId, LocalDate from, int nights, int totalCount) {
        for (int i = 0; i < nights; i++) {
            inventoryService.openInventory(HOST, roomTypeId, from.plusDays(i), totalCount);
            inventoryService.registerRate(HOST, roomTypeId, from.plusDays(i), RATE);
        }
    }

    /** 재고 2, 요금 RATE로 연 객실 타입에 CHECK_IN부터 nights 밤의 HELD 예약 */
    public Booking held(RoomTypeId roomTypeId, int nights) {
        open(roomTypeId, CHECK_IN, nights, 2);
        return bookingService.requestBooking(request(roomTypeId, CHECK_IN, nights, RATE * nights));
    }

    public Booking held(int nights) {
        return held(roomType(), nights);
    }

    public static RequestBookingCommand request(RoomTypeId roomTypeId, LocalDate checkIn, int nights,
                                                long expectedTotal) {
        return new RequestBookingCommand(GUEST, roomTypeId, checkIn, checkIn.plusDays(nights), 1,
                expectedTotal, "KRW", IdempotencyKey.of("key-" + UUID.randomUUID()));
    }

    public Booking reload(BookingId bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow();
    }

    public int heldCount(RoomTypeId roomTypeId, LocalDate stayDate) {
        return inventory(roomTypeId, stayDate).heldCount();
    }

    public int soldCount(RoomTypeId roomTypeId, LocalDate stayDate) {
        return inventory(roomTypeId, stayDate).soldCount();
    }

    public DailyInventory inventory(RoomTypeId roomTypeId, LocalDate stayDate) {
        return inventoryRepository.findByRoomTypeIdAndStayDate(roomTypeId, stayDate).orElseThrow();
    }

    public Money charge(Booking booking) {
        return new Money(booking.priceSnapshot().totalAmount(), booking.priceSnapshot().currency());
    }

    /**
     * T6. 결제 서비스를 거치지 않고 결제 집합체를 심는다. 이벤트가 발행되지 않으므로 예약의 구독자는
     * 모른다. P1이 유실된 상태(HELD인데 승인 시도 있음)와 실패 셋이 쌓였는데 P3가 닿지 않은 상태를
     * 만드는 자리다. 거래 번호는 Mock PG 대신 손으로 붙인다(결제 Y13 방식).
     */
    public PaymentAttempt seedApproved(Booking booking) {
        return transaction.execute((status) -> {
            Instant now = clock.instant();
            Payment payment = Payment.open(booking.id().value(), charge(booking), now);
            PaymentAttempt attempt = payment.openAttempt(charge(booking), MockMode.DEFER, now);
            String pgTransactionId = "mock_tx_seed_" + attempt.id().value();
            payment.attachPgTransaction(attempt.id(), pgTransactionId);
            payment.recordApproval(attempt.id(), pgTransactionId, now);
            paymentRepository.save(payment);
            return attempt;
        });
    }

    public void seedFailed(Booking booking, int count) {
        transaction.executeWithoutResult((status) -> {
            Instant now = clock.instant();
            Payment payment = Payment.open(booking.id().value(), charge(booking), now);
            for (int i = 0; i < count; i++) {
                PaymentAttempt attempt = payment.openAttempt(charge(booking), MockMode.DECLINE, now);
                String pgTransactionId = "mock_tx_seed_" + attempt.id().value();
                payment.attachPgTransaction(attempt.id(), pgTransactionId);
                payment.recordFailure(attempt.id(), pgTransactionId, PaymentAttempt.MOCK_DECLINED, now);
            }
            paymentRepository.save(payment);
        });
    }
}
