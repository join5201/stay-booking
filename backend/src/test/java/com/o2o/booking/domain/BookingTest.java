package com.o2o.booking.domain;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.o2o.shared.PropertyId;
import com.o2o.shared.RoomTypeId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K3. 설계 근거: task-S9-booking 8-1절, 06-4 1-2 RequestBooking의 Post(HELD 생성, expiresAt =
 * 생성 시각 + ttl, 스냅샷 동결), 06-2 3-1 I4(스냅샷 불변)와 I5(1차는 전이 없음), 08-3 결정 11-3
 * (TTL 10분), 11 응답 모델 Booking의 version.
 */
class BookingTest {

    private static final Instant NOW = Instant.parse("2026-10-01T20:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final LocalDate CHECK_IN = LocalDate.parse("2026-10-10");
    private static final StayPeriod TWO_NIGHTS = new StayPeriod(CHECK_IN, CHECK_IN.plusDays(2));
    private static final UserId GUEST = UserId.of("guest_001");
    private static final PropertyId PROPERTY_ID = PropertyId.of("prop_test");
    private static final RoomTypeId ROOM_TYPE_ID = RoomTypeId.of("room_test");
    private static final IdempotencyKey KEY = IdempotencyKey.of("booking-key-0001");

    private static PriceSnapshot snapshotFor(StayPeriod period) {
        List<DailyPrice> days = period.dates().stream()
                .map((date) -> new DailyPrice(date, 100_000, 0))
                .toList();
        long total = 100_000L * period.nights();
        return PriceSnapshot.of(period, "KRW", days, null, total, 0, total);
    }

    @Test
    void K3_생성하면_HELD이고_만료가_생성_시각_더하기_TTL이다() {
        Booking booking = Booking.request(GUEST, PROPERTY_ID, ROOM_TYPE_ID, TWO_NIGHTS, 2,
                snapshotFor(TWO_NIGHTS), KEY, NOW, TTL);

        assertEquals(BookingStatus.HELD, booking.status());
        assertEquals(NOW.plus(TTL), booking.expiresAt());
        assertEquals(NOW, booking.createdAt());
        assertEquals(NOW, booking.updatedAt());
        assertEquals(0L, booking.version());
        assertEquals(GUEST, booking.userId());
        assertEquals(PROPERTY_ID, booking.propertyId());
        assertEquals(ROOM_TYPE_ID, booking.roomTypeId());
        assertEquals(2, booking.userCount());
        assertEquals(KEY, booking.idempotencyKey());
        assertTrue(booking.id().value().startsWith("booking_"));
        assertEquals(200_000L, booking.priceSnapshot().totalAmount());
    }

    @Test
    void K3_인원_0과_TTL_0은_거절한다() {
        // 1은 통과의 짝이다. 위 테스트가 2로 통과했고 여기서 1의 경계를 본다(T2)
        assertEquals(1, Booking.request(GUEST, PROPERTY_ID, ROOM_TYPE_ID, TWO_NIGHTS, 1,
                snapshotFor(TWO_NIGHTS), KEY, NOW, TTL).userCount());
        assertThrows(IllegalArgumentException.class, () -> Booking.request(
                GUEST, PROPERTY_ID, ROOM_TYPE_ID, TWO_NIGHTS, 0, snapshotFor(TWO_NIGHTS), KEY, NOW, TTL));
        assertThrows(IllegalArgumentException.class, () -> Booking.request(
                GUEST, PROPERTY_ID, ROOM_TYPE_ID, TWO_NIGHTS, 1, snapshotFor(TWO_NIGHTS), KEY, NOW,
                Duration.ZERO));
    }

    @Test
    void K3_다른_기간의_스냅샷은_거절한다() {
        // I11을 값 객체 둘을 함께 아는 자리에서 다시 본다. 1박 스냅샷을 2박 예약에 붙일 수 없다
        StayPeriod oneNight = new StayPeriod(CHECK_IN, CHECK_IN.plusDays(1));

        assertThrows(InvalidPriceSnapshotException.class, () -> Booking.request(
                GUEST, PROPERTY_ID, ROOM_TYPE_ID, TWO_NIGHTS, 1, snapshotFor(oneNight), KEY, NOW, TTL));
    }

    @Test
    void K3_상태와_스냅샷을_바꾸는_공개_메서드가_없다() {
        // I4와 I5. 1차는 인자 있는 공개 인스턴스 메서드가 없었다. 2차가 전이 넷(confirm, expireByTtl,
        // expireByPaymentFailure, cancel)과 만료 판정 조회 isDue를 더했고 그 다섯이 전부다.
        // 스냅샷과 expiresAt을 바꾸는 메서드는 여전히 없다. 이 테스트가 없으면 누가 setter를
        // 붙여도 아무 테스트도 붉어지지 않는다. 전이 넷의 동작은 L1부터 L3이 본다
        List<String> mutators = Arrays.stream(Booking.class.getDeclaredMethods())
                .filter((method) -> Modifier.isPublic(method.getModifiers()))
                .filter((method) -> !Modifier.isStatic(method.getModifiers()))
                .filter((method) -> method.getParameterCount() > 0)
                .map(Method::getName)
                .sorted()
                .toList();

        assertEquals(List.of("cancel", "confirm", "expireByPaymentFailure", "expireByTtl", "isDue"),
                mutators);
        assertEquals(List.of("request"), Arrays.stream(Booking.class.getDeclaredMethods())
                .filter((method) -> Modifier.isPublic(method.getModifiers()))
                .filter((method) -> Modifier.isStatic(method.getModifiers()))
                .map(Method::getName)
                .toList());
    }

    @Test
    void K3_예약이_든_스냅샷의_days도_바깥에서_바꿀_수_없다() {
        Booking booking = Booking.request(GUEST, PROPERTY_ID, ROOM_TYPE_ID, TWO_NIGHTS, 1,
                snapshotFor(TWO_NIGHTS), KEY, NOW, TTL);

        assertThrows(UnsupportedOperationException.class,
                () -> booking.priceSnapshot().days().clear());
        assertEquals(2, booking.priceSnapshot().days().size());
    }
}
