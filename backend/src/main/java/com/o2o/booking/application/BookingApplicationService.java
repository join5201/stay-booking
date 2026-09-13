package com.o2o.booking.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingCreated;
import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.BookingNotFoundException;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.booking.domain.CancellationNotAllowedException;
import com.o2o.booking.domain.InvalidStateTransitionException;
import com.o2o.booking.domain.OccupancyExceededException;
import com.o2o.booking.domain.PriceChangedException;
import com.o2o.booking.domain.PriceSnapshot;
import com.o2o.booking.domain.StayPeriod;
import com.o2o.booking.domain.UserId;
import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeNotFoundException;
import com.o2o.catalog.domain.RoomTypeRepository;
import com.o2o.inventory.domain.DailyInventory;
import com.o2o.inventory.domain.InventoryAllocationService;
import com.o2o.inventory.domain.InventoryHeld;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.domain.PaymentAttemptId;
import com.o2o.payment.domain.RefundReason;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;
import com.o2o.shared.SeoulDate;

/**
 * 설계 근거: 06-2 6절 예약 CRC의 BookingApplicationService 행. RequestBooking은 인원을 검증하고
 * (A5), 가격 계산을 맡긴 뒤, 재고 선점과 예약 생성을 한 트랜잭션으로 묶는다. 06-2 4절 생성
 * 경계가 hold ×N과 Booking 생성을 한 트랜잭션으로 적고 커밋 뒤 BookingCreated와 InventoryHeld ×N을
 * 적는다. 멱등 기록의 완료도 같은 트랜잭션이다(11 멱등 규칙 6). 그 기록은 이 서비스가 아니라
 * IdempotentRequestExecutor가 바깥에서 감싼다. 이 서비스는 멱등을 모른다.
 *
 * 검사 순서는 계약 2절 표 6부터 14다. roomType 존재(404), 인원(A5), 재고 N행 잠금과 미개설,
 * 요금 존재(A6, 포트가 확인), 가격 대조(PRICE_CHANGED), hold ×N, Booking 생성, 커밋.
 * 잠금이 가격 대조보다 앞인 이유는 재고 행 읽기가 곧 미개설 검사이고 잠근 채 두 번 읽지 않기
 * 위해서다. 거절은 전부 예외라 트랜잭션이 통째로 되돌아가고 재고도 예약도 남지 않는다(A1).
 *
 * 잠금 순서는 08-3 결정 3이다. 생성 경로는 만들 Booking이 아직 없고 Payment도 없어 재고 N행만
 * 날짜 오름차순으로 잠근다.
 *
 * TTL은 08-3 결정 11의 11-3으로 10분이다. 설정 키 o2o.booking.hold-ttl로 받되 기본값을 여기
 * 두고 application.properties는 손대지 않는다(계약 7절 D-3 가). 기준 시각은 shared의 UTC Clock이다.
 * 만료 시각과 생성 시각과 serverNow가 같은 시계에서 나온다.
 *
 * 이벤트는 트랜잭션 안에서 발행하고 커밋 뒤 전달은 구독자가 지킨다(layers.md 3-3).
 *
 * CancelBooking(BOOK-04)은 2차에서 붙었다. 06-2 6절 CRC의 CancelBooking 행(잠그고 cancel한 뒤
 * 같은 트랜잭션에서 재고 반환)과 06-4 1-2 예약 표의 RefundPayment 중계 행이 근거다. 환불이 재고
 * 반환보다 앞인 것은 잠금 순서 Booking, Payment, 재고 N행 때문이다(08-3 결정 3과 10). 환불과
 * 취소와 반환이 한 트랜잭션인 것은 08-3 결정 11의 11-2(취소 환불 동기)다. 전이와 재고와 이벤트는
 * BookingLifecycle이 한 자리에서 한다.
 */
@Service
@Transactional
public class BookingApplicationService {

    // 08-3 결정 11의 11-5. 예약 한 건에 객실 하나
    private static final int ROOMS_PER_BOOKING = 1;

    private final BookingRepository bookingRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final InventoryAllocationService inventoryAllocationService;
    private final PriceQuotePort priceQuotePort;
    private final PaymentApplicationService paymentService;
    private final BookingLifecycle lifecycle;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final Duration holdTtl;

    public BookingApplicationService(BookingRepository bookingRepository,
                                     RoomTypeRepository roomTypeRepository,
                                     InventoryAllocationService inventoryAllocationService,
                                     PriceQuotePort priceQuotePort,
                                     PaymentApplicationService paymentService,
                                     BookingLifecycle lifecycle,
                                     ApplicationEventPublisher eventPublisher,
                                     Clock clock,
                                     @Value("${o2o.booking.hold-ttl:PT10M}") Duration holdTtl) {
        this.bookingRepository = bookingRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.inventoryAllocationService = inventoryAllocationService;
        this.priceQuotePort = priceQuotePort;
        this.paymentService = paymentService;
        this.lifecycle = lifecycle;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.holdTtl = holdTtl;
    }

    /**
     * BOOK-01. 설계 근거: 11 BOOK-01 처리 규칙, 06-4 1-2 RequestBooking, T06부터 T13.
     */
    public Booking requestBooking(RequestBookingCommand command) {
        Instant now = Instant.now(clock);
        StayPeriod period = StayPeriod.of(command.checkIn(), command.checkOut(),
                SeoulDate.today(clock));

        // 06-1 R2. 카탈로그가 존재와 maxOccupancy를 준다. 없으면 404다
        RoomType roomType = roomTypeRepository.findById(command.roomTypeId())
                .orElseThrow(() -> new RoomTypeNotFoundException(command.roomTypeId()));
        if (command.userCount() > roomType.maxOccupancy()) {
            throw new OccupancyExceededException(command.userCount(), roomType.maxOccupancy());
        }

        // 08-3 결정 3. 재고 N행을 날짜 오름차순으로 잠근다. 빠진 날짜는 INVENTORY_NOT_CONFIGURED
        List<DailyInventory> locked = inventoryAllocationService.lock(
                command.roomTypeId(), period.checkIn(), period.checkOut());

        // A6와 가격. 요금 없는 날짜는 RATE_NOT_CONFIGURED, 총액이 다르면 PRICE_CHANGED
        PriceSnapshot snapshot = priceQuotePort.quote(command.roomTypeId(), period);
        if (snapshot.totalAmount() != command.expectedTotalAmount()
                || !snapshot.currency().equals(command.currency())) {
            throw new PriceChangedException(command.expectedTotalAmount(), snapshot.totalAmount());
        }

        // A1. 한 행이라도 부족하면 예외가 트랜잭션을 깨고 앞 날짜의 증가도 남지 않는다
        List<DailyInventory> held = inventoryAllocationService.hold(locked, ROOMS_PER_BOOKING, now);

        Booking booking = bookingRepository.save(Booking.request(
                command.userId(), roomType.propertyId(), command.roomTypeId(), period,
                command.userCount(), snapshot, command.idempotencyKey(), now, holdTtl));

        eventPublisher.publishEvent(BookingCreated.of(booking));
        for (DailyInventory inventory : held) {
            eventPublisher.publishEvent(InventoryHeld.of(inventory));
        }
        return booking;
    }

    /**
     * BOOK-03. 설계 근거: 11 BOOK-03(GUEST, 대상 예약의 소유자), T02. 없는 예약과 남의 예약이
     * 같은 예외다. 남의 자원을 자원 정보 없이 404로 주라는 것이 11 인증과 접근 제어다.
     */
    @Transactional(readOnly = true)
    public Booking getBooking(UserId userId, BookingId bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.userId().equals(userId)) {
            throw new BookingNotFoundException(bookingId);
        }
        return booking;
    }

    /** BOOK-02. 설계 근거: 11 BOOK-02 처리 규칙. 본인 예약만, status 생략이면 전체 */
    @Transactional(readOnly = true)
    public PageResult<Booking> findBookings(UserId userId, BookingStatus status,
                                            PageQuery pageQuery) {
        return bookingRepository.findByUserId(userId, status, pageQuery);
    }

    /**
     * BOOK-04. 설계 근거: 11 BOOK-04 처리 규칙, 정책 P05, T24와 T25, 2차 계약 2절 BOOK-04 표
     * 7부터 12. 검사 순서는 소유, 잠금, 상태 CONFIRMED, 서울 오늘이 checkIn 전, 환불, cancel과
     * 판매분 반환이다. 거절은 전부 예외라 환불도 취소도 반환도 남지 않는다(11 BOOK-04의 부분 결과
     * 없음). 사람 경로라 종착 상태도 409다(08-3 결정 5).
     */
    public Booking cancelBooking(CancelBookingCommand command) {
        BookingId bookingId = command.bookingId();
        getBooking(command.userId(), bookingId);

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (booking.status() != BookingStatus.CONFIRMED) {
            throw new InvalidStateTransitionException(bookingId, booking.status(),
                    BookingStatus.CANCELED);
        }

        // P05. 체크인 전날까지다. 날짜의 기준은 서울이고 시각의 기준은 UTC Clock이다(11 공통 35행)
        LocalDate today = SeoulDate.today(clock);
        if (!today.isBefore(booking.period().checkIn())) {
            throw new CancellationNotAllowedException(bookingId, booking.period().checkIn(), today);
        }

        // 08-3 결정 10. 환불(Payment 잠금)이 재고 N행 잠금보다 앞이다. CONFIRMED는 승인 시도가
        // 있어야 하므로 없으면 불변 위반이고 500이다(2차 계약 2절 BOOK-04 표 10)
        String approvedAttemptId = paymentService.attemptsOf(bookingId.value()).approvedAttemptId();
        if (approvedAttemptId == null) {
            throw new IllegalStateException(
                    "CONFIRMED인데 승인 시도가 없다. 불변 위반이다: " + bookingId.value());
        }
        paymentService.refund(PaymentAttemptId.of(approvedAttemptId), RefundReason.BOOKING_CANCELED);

        lifecycle.cancel(booking, command.reason(), Instant.now(clock));
        return booking;
    }
}
