package com.o2o.booking.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.booking.domain.IdempotencyKey;
import com.o2o.booking.domain.OccupancyExceededException;
import com.o2o.booking.domain.PriceChangedException;
import com.o2o.booking.domain.RateNotConfiguredException;
import com.o2o.booking.domain.UserId;
import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.catalog.domain.Property;
import com.o2o.catalog.domain.RoomType;
import com.o2o.catalog.domain.RoomTypeNotFoundException;
import com.o2o.inventory.application.InventoryApplicationService;
import com.o2o.inventory.domain.DailyInventory;
import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.inventory.domain.DailyRate;
import com.o2o.inventory.domain.InventoryNotOpenedException;
import com.o2o.inventory.domain.InventoryShortageException;
import com.o2o.shared.HostId;
import com.o2o.shared.PageQuery;
import com.o2o.shared.RoomTypeId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K7부터 K11. 설계 근거: task-S9-booking 8-1절, 06-4 1-2 RequestBooking(Pre A5, A6, Post HELD와
 * 스냅샷 동결), 06-2 4절 생성 경계(hold ×N과 Booking 생성이 한 트랜잭션), T06, T07, T09, T12, T13.
 *
 * 실제 MySQL에 붙고 트랜잭션을 테스트가 감싸지 않는다. 앞 묶음의 앱 서비스 테스트와 다른 점이고
 * 이유는 K7이다. 거절된 요청의 롤백이 앞 날짜의 선점 증가를 지웠는지를 보려면 앱 서비스의
 * 트랜잭션이 실제로 끝나야 한다. 테스트가 트랜잭션을 감싸면 앱 서비스가 거기 합류해 롤백이
 * 테스트 끝까지 미뤄지고 앞 날짜의 증가가 그대로 보인다(testing.md T3).
 *
 * 그래서 데이터를 되돌리지 않는다. 테스트마다 숙소와 객실 타입을 새로 만들고 멱등키를 새로 뽑아
 * 서로 간섭하지 않는다. 테스트 DB는 create-drop이라 컨텍스트가 닫힐 때 비워진다.
 *
 * 시각은 앞 묶음과 같은 UTC 10월 1일 20시로 고정한다. 서울의 오늘은 10월 2일이다. 숙박 날짜
 * 10월 10일부터가 그 오늘 뒤라 재고와 요금 등록과 예약 요청이 모두 미래를 향한다(T5).
 */
@SpringBootTest
class BookingApplicationServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-01T20:00:00Z");
    private static final HostId HOST = HostId.of("host_001");
    private static final UserId GUEST = UserId.of("guest_001");
    private static final LocalDate CHECK_IN = LocalDate.parse("2026-10-10");
    private static final long RATE = 100_000L;

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private BookingApplicationService bookingApplicationService;

    @Autowired
    private CatalogApplicationService catalogApplicationService;

    @Autowired
    private InventoryApplicationService inventoryApplicationService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private DailyInventoryRepository inventoryRepository;

    @Test
    void 요청하면_HELD_예약이_저장되고_숙박_날짜마다_선점이_1_오른다() {
        // 거절 케이스들의 짝. 체크아웃 날짜는 숙박 대상이 아니라 재고도 요금도 없어도 된다(T06)
        RoomTypeId roomTypeId = 객실_타입();
        재고와_요금(roomTypeId, CHECK_IN, 2, 2);

        Booking booking = bookingApplicationService.requestBooking(
                명령(roomTypeId, CHECK_IN, 2, 2, RATE * 2));

        Booking saved = bookingRepository.findById(booking.id()).orElseThrow();
        assertEquals(BookingStatus.HELD, saved.status());
        assertEquals(FIXED_NOW.plus(Duration.ofMinutes(10)), saved.expiresAt());
        assertEquals(RATE * 2, saved.priceSnapshot().totalAmount());
        assertEquals(2, saved.priceSnapshot().days().size());
        assertEquals(1, 선점수(roomTypeId, CHECK_IN));
        assertEquals(1, 선점수(roomTypeId, CHECK_IN.plusDays(1)));
        assertTrue(inventoryRepository.findByRoomTypeIdAndStayDate(roomTypeId, CHECK_IN.plusDays(2))
                .isEmpty());
    }

    @Test
    void K7_3박_중_가운데_날_재고가_0이면_어느_날짜도_선점이_안_오르고_예약이_없다() {
        // T09와 A1. 첫날 hold가 지나간 뒤 둘째 날에서 깨진다. 롤백이 첫날의 증가를 지워야 한다
        RoomTypeId roomTypeId = 객실_타입();
        inventoryApplicationService.openInventory(HOST, roomTypeId, CHECK_IN, 2);
        inventoryApplicationService.openInventory(HOST, roomTypeId, CHECK_IN.plusDays(1), 0);
        inventoryApplicationService.openInventory(HOST, roomTypeId, CHECK_IN.plusDays(2), 2);
        for (int i = 0; i < 3; i++) {
            inventoryApplicationService.registerRate(HOST, roomTypeId, CHECK_IN.plusDays(i), RATE);
        }

        assertThrows(InventoryShortageException.class, () -> bookingApplicationService.requestBooking(
                명령(roomTypeId, CHECK_IN, 3, 2, RATE * 3)));

        for (int i = 0; i < 3; i++) {
            assertEquals(0, 선점수(roomTypeId, CHECK_IN.plusDays(i)), "day " + i);
        }
        assertEquals(0, 예약수(roomTypeId));
    }

    @Test
    void K8_숙박_날짜_중_하루에_요금이_없으면_거절하고_선점도_예약도_없다() {
        // A6. 재고는 2박 다 있고 요금은 둘째 날이 없다
        RoomTypeId roomTypeId = 객실_타입();
        inventoryApplicationService.openInventory(HOST, roomTypeId, CHECK_IN, 2);
        inventoryApplicationService.openInventory(HOST, roomTypeId, CHECK_IN.plusDays(1), 2);
        inventoryApplicationService.registerRate(HOST, roomTypeId, CHECK_IN, RATE);

        assertThrows(RateNotConfiguredException.class, () -> bookingApplicationService.requestBooking(
                명령(roomTypeId, CHECK_IN, 2, 2, RATE * 2)));

        assertEquals(0, 선점수(roomTypeId, CHECK_IN));
        assertEquals(0, 예약수(roomTypeId));
    }

    @Test
    void K8_숙박_날짜_중_하루에_재고_행이_없으면_거절하고_선점도_예약도_없다() {
        // T07. 06-4 1-2 hold의 InventoryNotOpened. 요금은 2박 다 있고 재고는 둘째 날이 없다
        RoomTypeId roomTypeId = 객실_타입();
        inventoryApplicationService.openInventory(HOST, roomTypeId, CHECK_IN, 2);
        inventoryApplicationService.registerRate(HOST, roomTypeId, CHECK_IN, RATE);
        inventoryApplicationService.registerRate(HOST, roomTypeId, CHECK_IN.plusDays(1), RATE);

        assertThrows(InventoryNotOpenedException.class, () -> bookingApplicationService.requestBooking(
                명령(roomTypeId, CHECK_IN, 2, 2, RATE * 2)));

        assertEquals(0, 선점수(roomTypeId, CHECK_IN));
        assertEquals(0, 예약수(roomTypeId));
    }

    @Test
    void K9_인원이_최대_인원을_넘으면_거절한다() {
        // A5. 객실 타입의 maxOccupancy가 2다. 2는 위의 통과 테스트가 짝이다
        RoomTypeId roomTypeId = 객실_타입();
        재고와_요금(roomTypeId, CHECK_IN, 1, 2);

        assertThrows(OccupancyExceededException.class, () -> bookingApplicationService.requestBooking(
                명령(roomTypeId, CHECK_IN, 1, 3, RATE)));

        assertEquals(0, 선점수(roomTypeId, CHECK_IN));
    }

    @Test
    void K9_없는_객실_타입은_없음_예외다() {
        // 06-1 R2. 카탈로그가 존재를 준다. 예약 컨텍스트가 따로 판단하지 않는다
        assertThrows(RoomTypeNotFoundException.class, () -> bookingApplicationService.requestBooking(
                명령(RoomTypeId.of("room_없는것"), CHECK_IN, 1, 1, RATE)));
    }

    @Test
    void K10_예상_총액이_요금_합과_다르면_거절하고_선점도_예약도_없다() {
        // T12의 요금 절반과 P06. 요청이 든 총액이 서버가 계산한 총액과 다르면 PRICE_CHANGED다
        RoomTypeId roomTypeId = 객실_타입();
        재고와_요금(roomTypeId, CHECK_IN, 2, 2);

        assertThrows(PriceChangedException.class, () -> bookingApplicationService.requestBooking(
                명령(roomTypeId, CHECK_IN, 2, 2, RATE * 2 - 1)));

        assertEquals(0, 선점수(roomTypeId, CHECK_IN));
        assertEquals(0, 선점수(roomTypeId, CHECK_IN.plusDays(1)));
        assertEquals(0, 예약수(roomTypeId));
    }

    @Test
    void K11_예약_뒤_요금을_바꿔도_스냅샷이_그대로다() {
        // T13의 요금 절반과 I4. RATE-02로 첫날 요금을 올려도 이미 만든 예약의 총액은 안 변한다
        RoomTypeId roomTypeId = 객실_타입();
        재고와_요금(roomTypeId, CHECK_IN, 2, 2);
        Booking booking = bookingApplicationService.requestBooking(
                명령(roomTypeId, CHECK_IN, 2, 1, RATE * 2));

        DailyRate first = inventoryApplicationService.getRate(HOST, roomTypeId, CHECK_IN);
        inventoryApplicationService.adjustRate(HOST, roomTypeId, CHECK_IN, first.version(), RATE * 5);

        Booking reloaded = bookingRepository.findById(booking.id()).orElseThrow();
        assertEquals(RATE * 2, reloaded.priceSnapshot().totalAmount());
        assertEquals(RATE, reloaded.priceSnapshot().days().get(0).baseAmount());
        // 새 요청은 새 요금으로 계산된다. 스냅샷이 동결된 것이지 요금이 안 바뀐 것이 아니다
        assertThrows(PriceChangedException.class, () -> bookingApplicationService.requestBooking(
                명령(roomTypeId, CHECK_IN, 2, 1, RATE * 2)));
    }

    private RoomTypeId 객실_타입() {
        Property property = catalogApplicationService.registerProperty(
                HOST, "예약 테스트 스테이", "SEOUL", "서울특별시 중구 예시로 1", "");
        RoomType roomType = catalogApplicationService.registerRoomType(
                HOST, property.id(), "스탠다드", 2, "");
        return roomType.id();
    }

    /** from부터 nights 밤을 재고 totalCount와 요금 RATE로 연다. 체크아웃 날짜는 열지 않는다 */
    private void 재고와_요금(RoomTypeId roomTypeId, LocalDate from, int nights, int totalCount) {
        for (int i = 0; i < nights; i++) {
            inventoryApplicationService.openInventory(HOST, roomTypeId, from.plusDays(i), totalCount);
            inventoryApplicationService.registerRate(HOST, roomTypeId, from.plusDays(i), RATE);
        }
    }

    private static RequestBookingCommand 명령(RoomTypeId roomTypeId, LocalDate checkIn, int nights,
                                            int userCount, long expectedTotalAmount) {
        return new RequestBookingCommand(GUEST, roomTypeId, checkIn, checkIn.plusDays(nights),
                userCount, expectedTotalAmount, "KRW",
                IdempotencyKey.of("key-" + UUID.randomUUID()));
    }

    private int 선점수(RoomTypeId roomTypeId, LocalDate stayDate) {
        DailyInventory inventory = inventoryRepository.findByRoomTypeIdAndStayDate(roomTypeId, stayDate)
                .orElseThrow();
        return inventory.heldCount();
    }

    /** 이 객실 타입에 걸린 손님의 예약 수. 데이터를 되돌리지 않으므로 객실 타입으로 거른다 */
    private long 예약수(RoomTypeId roomTypeId) {
        return bookingRepository.findByUserId(GUEST, null, PageQuery.of(0, 100)).items().stream()
                .filter((b) -> b.roomTypeId().equals(roomTypeId))
                .count();
    }
}
