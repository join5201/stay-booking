package com.o2o.booking.application;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingCanceled;
import com.o2o.booking.domain.BookingConfirmed;
import com.o2o.booking.domain.BookingExpired;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.inventory.domain.DailyInventory;
import com.o2o.inventory.domain.InventoryAllocationService;
import com.o2o.inventory.domain.InventoryCommitted;
import com.o2o.inventory.domain.InventoryReleased;

/**
 * 전이 하나와 그 재고 연산과 이벤트를 한 자리에서 정확히 한 번 한다. 설계 근거: 06-2 6절 예약
 * CRC의 ConfirmBooking(잠그고 confirm한 뒤 같은 트랜잭션에서 재고 확정, A3)과 ExpireBooking과
 * CancelBooking(전이시킨 뒤 같은 트랜잭션에서 재고 반환, A2), 2차 계약 2-1절 R4와 판매분 반환
 * 한 번과 종착 무해 행.
 *
 * 같은 전이를 경로 여럿이 부른다. 확정은 P1과 T1의 확정 우선이, TTL 만료는 T1과 P1의 지연
 * 승인과 P3의 TTL 우선과 PAY-01의 선만료가, 취소는 BOOK-04가 부른다. 전이 발생 여부는
 * Booking이 돌려주고(종착 재호출은 false) 재고 연산과 이벤트는 그 값이 true일 때만 한다. 그래서
 * 경로가 몇 번 겹쳐도 재고는 한 번 움직인다(R4).
 *
 * 트랜잭션은 열지 않는다. 호출자가 Booking을 잠근 트랜잭션 안에서 부른다(06-4 1-2 Pre의 잠금).
 * 잠금 순서에서 재고 N행은 마지막이다(08-3 결정 3). Payment를 잠그는 환불은 호출자가 이 메서드보다
 * 먼저 부른다. 이벤트는 트랜잭션 안에서 발행하고 커밋 뒤 전달은 구독자가 지킨다(layers.md 3-3).
 * 재고 이벤트는 InventoryHeld처럼 행마다 하나씩이다(계약 개정 2).
 */
@Component
public class BookingLifecycle {

    // 08-3 결정 11의 11-5. 예약 한 건에 객실 하나. 1차의 hold와 같은 수다
    private static final int ROOMS_PER_BOOKING = 1;

    private final BookingRepository bookingRepository;
    private final InventoryAllocationService inventoryAllocationService;
    private final ApplicationEventPublisher eventPublisher;

    public BookingLifecycle(BookingRepository bookingRepository,
                            InventoryAllocationService inventoryAllocationService,
                            ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.inventoryAllocationService = inventoryAllocationService;
        this.eventPublisher = eventPublisher;
    }

    /** ConfirmBooking. HELD에서 CONFIRMED, 선점을 판매로(A3). 이미 CONFIRMED면 아무것도 안 한다 */
    public boolean confirm(Booking booking, Instant now) {
        if (!booking.confirm(now)) {
            return false;
        }
        bookingRepository.save(booking);
        List<DailyInventory> committed = inventoryAllocationService.commit(booking.roomTypeId(),
                booking.period().checkIn(), booking.period().checkOut(), ROOMS_PER_BOOKING, now);
        eventPublisher.publishEvent(BookingConfirmed.of(booking));
        for (DailyInventory inventory : committed) {
            eventPublisher.publishEvent(InventoryCommitted.of(inventory));
        }
        return true;
    }

    /** ExpireBooking(TTL_EXPIRED). HELD에서 EXPIRED, 선점 반환(A2). 이미 EXPIRED면 아무것도 안 한다 */
    public boolean expireByTtl(Booking booking, Instant now) {
        if (!booking.expireByTtl(now)) {
            return false;
        }
        releaseHeldAndPublish(booking, now);
        return true;
    }

    /** ExpireBooking(PAYMENT_FAILED). 시도 수 3 이상. 이미 EXPIRED면 아무것도 안 한다 */
    public boolean expireByPaymentFailure(Booking booking, int attemptCount, Instant now) {
        if (!booking.expireByPaymentFailure(attemptCount, now)) {
            return false;
        }
        releaseHeldAndPublish(booking, now);
        return true;
    }

    /** CancelBooking. CONFIRMED에서 CANCELED, 판매분 반환. 이미 CANCELED면 아무것도 안 한다 */
    public boolean cancel(Booking booking, String reason, Instant now) {
        if (!booking.cancel(reason, now)) {
            return false;
        }
        bookingRepository.save(booking);
        List<DailyInventory> released = inventoryAllocationService.releaseSold(booking.roomTypeId(),
                booking.period().checkIn(), booking.period().checkOut(), ROOMS_PER_BOOKING, now);
        eventPublisher.publishEvent(BookingCanceled.of(booking));
        for (DailyInventory inventory : released) {
            eventPublisher.publishEvent(InventoryReleased.of(inventory, InventoryReleased.Source.SOLD));
        }
        return true;
    }

    private void releaseHeldAndPublish(Booking booking, Instant now) {
        bookingRepository.save(booking);
        List<DailyInventory> released = inventoryAllocationService.releaseHeld(booking.roomTypeId(),
                booking.period().checkIn(), booking.period().checkOut(), ROOMS_PER_BOOKING, now);
        eventPublisher.publishEvent(BookingExpired.of(booking));
        for (DailyInventory inventory : released) {
            eventPublisher.publishEvent(InventoryReleased.of(inventory, InventoryReleased.Source.HELD));
        }
    }
}
