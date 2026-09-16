package com.o2o.booking.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.event.TransactionalEventListener;

import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingCreated;
import com.o2o.booking.domain.IdempotencyKey;
import com.o2o.booking.domain.UserId;
import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.RoomType;
import com.o2o.inventory.application.InventoryApplicationService;
import com.o2o.inventory.domain.InventoryHeld;
import com.o2o.inventory.domain.InventoryShortageException;
import com.o2o.shared.HostId;
import com.o2o.shared.RoomTypeId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K12. 설계 근거: task-S9-booking 8-1절, 06-2 4절 생성 경계(커밋 후 BookingCreated와 InventoryHeld
 * ×N), 03 이벤트 행, backend/.claude/rules/layers.md 3-3 E1과 E2.
 *
 * 앞 묶음의 이벤트 테스트는 발행 횟수를 셌다. 여기는 한 걸음 더 가서 구독자에 닿는 시점을 본다.
 * E1이 구독자에게 커밋 뒤를 지키라고 하므로 테스트 전용 구독자를 E1대로 걸고, 성공한 요청의
 * 이벤트는 닿고 롤백된 요청의 이벤트는 닿지 않는 것을 확인한다(E2). 프로덕션 구독자는 아직 없고
 * 이 구독자는 테스트 컨텍스트에만 있다.
 *
 * 트랜잭션을 테스트가 감싸지 않는 이유는 BookingApplicationServiceTest와 같다. 커밋이 실제로
 * 일어나야 AFTER_COMMIT이 돈다.
 */
@SpringBootTest
class BookingEventTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-01T20:00:00Z");
    private static final HostId HOST = HostId.of("host_001");
    private static final UserId GUEST = UserId.of("guest_001");
    private static final LocalDate CHECK_IN = LocalDate.parse("2026-10-10");
    private static final long RATE = 100_000L;

    /** E1대로 건 구독자. 기본 phase가 AFTER_COMMIT이라 커밋된 사실만 받는다 */
    static class CommittedEventRecorder {

        final List<BookingCreated> bookingCreated = new CopyOnWriteArrayList<>();
        final List<InventoryHeld> inventoryHeld = new CopyOnWriteArrayList<>();

        @TransactionalEventListener
        void on(BookingCreated event) {
            bookingCreated.add(event);
        }

        @TransactionalEventListener
        void on(InventoryHeld event) {
            inventoryHeld.add(event);
        }

        void clear() {
            bookingCreated.clear();
            inventoryHeld.clear();
        }
    }

    @TestConfiguration
    static class RecorderConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }

        @Bean
        CommittedEventRecorder committedEventRecorder() {
            return new CommittedEventRecorder();
        }
    }

    @Autowired
    private BookingApplicationService bookingApplicationService;

    @Autowired
    private CatalogApplicationService catalogApplicationService;

    @Autowired
    private InventoryApplicationService inventoryApplicationService;

    @Autowired
    private CommittedEventRecorder recorder;

    @BeforeEach
    void clearRecorder() {
        recorder.clear();
    }

    @Test
    void K12_성공하면_BookingCreated_1건과_InventoryHeld_N건이_커밋_뒤_구독자에_닿는다() {
        RoomTypeId roomTypeId = 객실_타입();
        for (int i = 0; i < 2; i++) {
            inventoryApplicationService.openInventory(HOST, roomTypeId, CHECK_IN.plusDays(i), 2);
            inventoryApplicationService.registerRate(HOST, roomTypeId, CHECK_IN.plusDays(i), RATE);
        }

        Booking booking = bookingApplicationService.requestBooking(명령(roomTypeId, 2, RATE * 2));

        assertEquals(1, recorder.bookingCreated.size());
        BookingCreated created = recorder.bookingCreated.get(0);
        assertEquals(booking.id(), created.bookingId());
        assertEquals(GUEST, created.userId());
        assertEquals(RATE * 2, created.totalAmount());
        assertEquals(booking.expiresAt(), created.expiresAt());

        assertEquals(2, recorder.inventoryHeld.size());
        assertEquals(List.of(CHECK_IN, CHECK_IN.plusDays(1)),
                recorder.inventoryHeld.stream().map(InventoryHeld::stayDate).toList());
        assertTrue(recorder.inventoryHeld.stream().allMatch((held) -> held.heldCount() == 1));
    }

    @Test
    void K12_롤백된_요청의_이벤트는_구독자에_닿지_않는다() {
        // E2. K7과 같은 상황이다. 둘째 날 재고 0으로 첫날 InventoryHeld가 발행된 뒤 깨진다.
        // 구독자를 @EventListener로 걸면 그 첫날 이벤트가 닿아 이 테스트가 붉어진다
        RoomTypeId roomTypeId = 객실_타입();
        inventoryApplicationService.openInventory(HOST, roomTypeId, CHECK_IN, 2);
        inventoryApplicationService.openInventory(HOST, roomTypeId, CHECK_IN.plusDays(1), 0);
        for (int i = 0; i < 2; i++) {
            inventoryApplicationService.registerRate(HOST, roomTypeId, CHECK_IN.plusDays(i), RATE);
        }

        assertThrows(InventoryShortageException.class,
                () -> bookingApplicationService.requestBooking(명령(roomTypeId, 2, RATE * 2)));

        assertEquals(0, recorder.bookingCreated.size());
        assertEquals(0, recorder.inventoryHeld.size());
    }

    private RoomTypeId 객실_타입() {
        Property property = catalogApplicationService.registerProperty(
                HOST, "이벤트 테스트 스테이", "SEOUL", "서울특별시 중구 예시로 1", "");
        RoomType roomType = catalogApplicationService.registerRoomType(
                HOST, property.id(), "스탠다드", 2, "");
        return roomType.id();
    }

    private static RequestBookingCommand 명령(RoomTypeId roomTypeId, int nights, long total) {
        return new RequestBookingCommand(GUEST, roomTypeId, CHECK_IN, CHECK_IN.plusDays(nights),
                1, total, "KRW", IdempotencyKey.of("key-" + UUID.randomUUID()));
    }
}
