package com.o2o.booking;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.transaction.event.TransactionalEventListener;

import com.o2o.booking.domain.BookingCanceled;
import com.o2o.booking.domain.BookingConfirmed;
import com.o2o.booking.domain.BookingExpired;
import com.o2o.booking.domain.BookingId;
import com.o2o.inventory.domain.InventoryCommitted;
import com.o2o.inventory.domain.InventoryReleased;
import com.o2o.shared.RoomTypeId;

/**
 * 커밋된 사실만 받는 테스트 전용 구독자. 1차 K12의 CommittedEventRecorder와 결제의
 * CommittedPaymentEvents와 같은 방식(@TransactionalEventListener, 기본 phase AFTER_COMMIT)으로
 * 걸어 운영 구독자와 같은 조건에서 받는다(layers.md 3-3 E1). 2차의 전이 이벤트 셋과 재고 이벤트
 * 둘(계약 개정 2)을 센다. 롤백된 경로의 이벤트는 여기 닿지 않아야 한다(E2, L12).
 */
public class CommittedBookingEvents {

    public final List<BookingConfirmed> confirmed = new CopyOnWriteArrayList<>();
    public final List<BookingExpired> expired = new CopyOnWriteArrayList<>();
    public final List<BookingCanceled> canceled = new CopyOnWriteArrayList<>();
    public final List<InventoryCommitted> inventoryCommitted = new CopyOnWriteArrayList<>();
    public final List<InventoryReleased> inventoryReleased = new CopyOnWriteArrayList<>();

    @TransactionalEventListener
    public void on(BookingConfirmed event) {
        confirmed.add(event);
    }

    @TransactionalEventListener
    public void on(BookingExpired event) {
        expired.add(event);
    }

    @TransactionalEventListener
    public void on(BookingCanceled event) {
        canceled.add(event);
    }

    @TransactionalEventListener
    public void on(InventoryCommitted event) {
        inventoryCommitted.add(event);
    }

    @TransactionalEventListener
    public void on(InventoryReleased event) {
        inventoryReleased.add(event);
    }

    public void clear() {
        confirmed.clear();
        expired.clear();
        canceled.clear();
        inventoryCommitted.clear();
        inventoryReleased.clear();
    }

    public long confirmedOf(BookingId bookingId) {
        return confirmed.stream().filter((e) -> e.bookingId().equals(bookingId)).count();
    }

    public long expiredOf(BookingId bookingId) {
        return expired.stream().filter((e) -> e.bookingId().equals(bookingId)).count();
    }

    public long canceledOf(BookingId bookingId) {
        return canceled.stream().filter((e) -> e.bookingId().equals(bookingId)).count();
    }

    public List<InventoryCommitted> committedOf(RoomTypeId roomTypeId) {
        return inventoryCommitted.stream().filter((e) -> e.roomTypeId().equals(roomTypeId)).toList();
    }

    public List<InventoryReleased> releasedOf(RoomTypeId roomTypeId, InventoryReleased.Source source) {
        return inventoryReleased.stream()
                .filter((e) -> e.roomTypeId().equals(roomTypeId) && e.source() == source)
                .toList();
    }
}
