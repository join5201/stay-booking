package com.o2o.booking.infrastructure;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.o2o.booking.BookingFixtures;
import com.o2o.booking.BookingLifecycleTestConfiguration;
import com.o2o.booking.MutableClock;
import com.o2o.booking.application.BookingApplicationService;
import com.o2o.booking.application.PriceQuotePort;
import com.o2o.booking.domain.AppliedPromotion;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.DailyPrice;
import com.o2o.booking.domain.PriceChangedException;
import com.o2o.booking.domain.PriceSnapshot;
import com.o2o.booking.domain.RateNotConfiguredException;
import com.o2o.booking.domain.StayPeriod;
import com.o2o.promotion.application.PromotionApplicationService;
import com.o2o.promotion.application.PromotionDraft;
import com.o2o.promotion.domain.Promotion;
import com.o2o.promotion.domain.PromotionChanges;
import com.o2o.promotion.domain.StayWindowChange;
import com.o2o.shared.RoomTypeId;
import com.o2o.shared.SeoulDate;

import static com.o2o.booking.BookingFixtures.CHECK_IN;
import static com.o2o.booking.BookingFixtures.RATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L5. 설계 근거: task-S9-booking-lifecycle 8-1절, 11 가격과 프로모션 절(예약 가격은 프로모션
 * 계산기가 낸 스냅샷), I4(스냅샷 불변)와 R5(확정 청구액은 요금과 프로모션 변경에 흔들리지 않는다),
 * T12와 T13의 프로모션 절반, 프로모션 계약 V17과 V18, 1차 K8 이월(요금 없는 날짜).
 *
 * 1차의 요금 합산 어댑터를 프로모션 세션의 계산기로 바꾼 자리(4-1단계, 7절 D-4 가)를 본다. 포트를
 * 직접 부르는 테스트와 예약 요청을 지나는 테스트를 같이 둔다. 지역 코드를 테스트마다 새로 뽑아
 * 다른 테스트의 프로모션이 섞이지 않게 한다. 시각은 공유 시계의 원점(서울 10월 2일)이다.
 */
@SpringBootTest
@Import(BookingLifecycleTestConfiguration.class)
class PricingPriceQuoteAdapterTest {

    @Autowired
    private PriceQuotePort priceQuotePort;

    @Autowired
    private BookingApplicationService bookingApplicationService;

    @Autowired
    private PromotionApplicationService promotionApplicationService;

    @Autowired
    private BookingFixtures fixtures;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void L5_프로모션이_있으면_스냅샷에_적용_프로모션과_날짜별_할인이_들고_총액이_날짜_합이다() {
        String region = BookingFixtures.newRegion();
        RoomTypeId roomTypeId = fixtures.roomType(region);
        fixtures.open(roomTypeId, CHECK_IN, 2, 2);
        Promotion promotion = 프로모션(region, 10, 1);

        PriceSnapshot snapshot = priceQuotePort.quote(roomTypeId, StayPeriod.of(CHECK_IN,
                CHECK_IN.plusDays(2), SeoulDate.today(clock)));

        AppliedPromotion applied = snapshot.appliedPromotion().orElseThrow();
        assertEquals(promotion.id().value(), applied.id());
        assertEquals(promotion.name(), applied.name());
        assertEquals(10, applied.discountRate());
        assertEquals("KRW", snapshot.currency());
        assertEquals(RATE * 2, snapshot.baseTotalAmount());
        assertEquals(RATE * 2 / 10, snapshot.discountTotalAmount());
        assertEquals(RATE * 2 - RATE * 2 / 10, snapshot.totalAmount());
        assertEquals(List.of(CHECK_IN, CHECK_IN.plusDays(1)),
                snapshot.days().stream().map(DailyPrice::date).toList());
        assertTrue(snapshot.days().stream().allMatch((day) -> day.baseAmount() == RATE
                && day.discountAmount() == RATE / 10));
        long sumOfDays = snapshot.days().stream()
                .mapToLong((day) -> day.baseAmount() - day.discountAmount()).sum();
        assertEquals(sumOfDays, snapshot.totalAmount());

        // 그 총액으로 예약하면 통과하고 저장된 스냅샷이 같다
        Booking booking = bookingApplicationService.requestBooking(
                BookingFixtures.request(roomTypeId, CHECK_IN, 2, snapshot.totalAmount()));
        Booking saved = fixtures.reload(booking.id());
        assertEquals(snapshot.totalAmount(), saved.priceSnapshot().totalAmount());
        assertEquals(applied.id(), saved.priceSnapshot().appliedPromotion().orElseThrow().id());
    }

    @Test
    void L5_프로모션_조건_밖이면_할인_0이고_적용_프로모션이_없다() {
        // minNights 3인데 2박이다. 조건 판정은 프로모션 몫이고 여기는 그 결과가 스냅샷에 실리는지를 본다
        String region = BookingFixtures.newRegion();
        RoomTypeId roomTypeId = fixtures.roomType(region);
        fixtures.open(roomTypeId, CHECK_IN, 2, 2);
        프로모션(region, 10, 3);

        PriceSnapshot snapshot = priceQuotePort.quote(roomTypeId, StayPeriod.of(CHECK_IN,
                CHECK_IN.plusDays(2), SeoulDate.today(clock)));

        assertTrue(snapshot.appliedPromotion().isEmpty());
        assertEquals(0L, snapshot.discountTotalAmount());
        assertEquals(RATE * 2, snapshot.totalAmount());
        assertTrue(snapshot.days().stream().allMatch((day) -> day.discountAmount() == 0));
    }

    @Test
    void L5_예상_총액이_할인_전_금액이면_PRICE_CHANGED다() {
        // T12의 프로모션 절반. 손님 화면이 할인을 못 본 채 요금 합을 보냈다
        String region = BookingFixtures.newRegion();
        RoomTypeId roomTypeId = fixtures.roomType(region);
        fixtures.open(roomTypeId, CHECK_IN, 2, 2);
        프로모션(region, 10, 1);

        assertThrows(PriceChangedException.class, () -> bookingApplicationService.requestBooking(
                BookingFixtures.request(roomTypeId, CHECK_IN, 2, RATE * 2)));

        assertEquals(0, fixtures.heldCount(roomTypeId, CHECK_IN));
        // 짝. 할인된 총액이면 통과한다
        Booking booking = bookingApplicationService.requestBooking(
                BookingFixtures.request(roomTypeId, CHECK_IN, 2, RATE * 2 - RATE * 2 / 10));
        assertEquals(1, fixtures.heldCount(roomTypeId, CHECK_IN));
        assertEquals(RATE * 2 - RATE * 2 / 10, booking.priceSnapshot().totalAmount());
    }

    @Test
    void L5_예약_뒤_프로모션을_바꿔도_스냅샷과_총액이_그대로다() {
        // T13의 프로모션 절반과 R5. 할인율을 10에서 50으로 올린다. 새 견적은 바뀌고 예약은 안 바뀐다
        String region = BookingFixtures.newRegion();
        RoomTypeId roomTypeId = fixtures.roomType(region);
        fixtures.open(roomTypeId, CHECK_IN, 2, 2);
        Promotion promotion = 프로모션(region, 10, 1);
        long discountedTotal = RATE * 2 - RATE * 2 / 10;
        Booking booking = bookingApplicationService.requestBooking(
                BookingFixtures.request(roomTypeId, CHECK_IN, 2, discountedTotal));

        promotionApplicationService.update(promotion.id(), promotion.version(),
                new PromotionChanges(null, 50, null, null, StayWindowChange.keep(), null, null, null));

        Booking reloaded = fixtures.reload(booking.id());
        assertEquals(discountedTotal, reloaded.priceSnapshot().totalAmount());
        assertEquals(10, reloaded.priceSnapshot().appliedPromotion().orElseThrow().discountRate());
        assertEquals(RATE / 10, reloaded.priceSnapshot().days().get(0).discountAmount());
        PriceSnapshot fresh = priceQuotePort.quote(roomTypeId, StayPeriod.of(CHECK_IN,
                CHECK_IN.plusDays(2), SeoulDate.today(clock)));
        assertEquals(RATE * 2 / 2, fresh.totalAmount());
        assertThrows(PriceChangedException.class, () -> bookingApplicationService.requestBooking(
                BookingFixtures.request(roomTypeId, CHECK_IN, 2, discountedTotal)));
    }

    @Test
    void L5_요금_없는_날짜가_있으면_예약_컨텍스트의_RateNotConfigured다() {
        // 1차 K8 이월. 프로모션의 같은 이름 예외를 어댑터가 예약 도메인의 예외로 바꾼다. 재고는 있다
        RoomTypeId roomTypeId = fixtures.roomType();
        fixtures.open(roomTypeId, CHECK_IN, 1, 2);
        LocalDate secondNight = CHECK_IN.plusDays(1);
        assertTrue(fixtures.inventory(roomTypeId, CHECK_IN).availableCount() > 0);

        RateNotConfiguredException e = assertThrows(RateNotConfiguredException.class,
                () -> priceQuotePort.quote(roomTypeId, StayPeriod.of(CHECK_IN, CHECK_IN.plusDays(2),
                        SeoulDate.today(clock))));

        assertEquals(List.of(secondNight), e.missingDates());
    }

    private Promotion 프로모션(String region, int discountRate, int minNights) {
        LocalDate today = SeoulDate.today(clock);
        return promotionApplicationService.create(new PromotionDraft(
                region + " " + discountRate, discountRate, today, today.plusDays(30),
                null, null, minNights, List.of(region), true));
    }
}
