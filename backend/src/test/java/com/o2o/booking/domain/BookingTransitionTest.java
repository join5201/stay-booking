package com.o2o.booking.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.o2o.shared.PropertyId;
import com.o2o.shared.RoomTypeId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L1부터 L3. 설계 근거: task-S9-booking-lifecycle 8-1절, 06-4 1-2 confirm과 expire와 cancel의
 * Pre와 Post, 06-4 1-3 예약 상태 표(허용 전이 셋, I5), 06-4 0절 종착 재호출 무해, 11 상태 전이와
 * 시간 경계(정확히 expiresAt이면 만료), 계약 6절 version 행(HELD 0, CONFIRMED 1, CANCELED 2,
 * EXPIRED 1)과 cancellationReason 행(null은 빈 문자열, 최대 300자).
 *
 * 1차 BookingTest는 생성만 봤다. 여기는 전이 넷을 본다. 재호출 무해(false)와 금지 전이(예외)를
 * 나눠 보는 것이 08-3 결정 5의 두 얼굴이다. 통과와 거절을 짝으로 두고 경계는 양쪽을 본다(T1, T2).
 */
class BookingTransitionTest {

    private static final Instant NOW = Instant.parse("2026-10-01T20:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Instant EXPIRES_AT = NOW.plus(TTL);
    private static final Instant LATER = NOW.plusSeconds(30);
    private static final LocalDate CHECK_IN = LocalDate.parse("2026-10-10");
    private static final StayPeriod TWO_NIGHTS = new StayPeriod(CHECK_IN, CHECK_IN.plusDays(2));

    private static Booking held() {
        List<DailyPrice> days = TWO_NIGHTS.dates().stream()
                .map((date) -> new DailyPrice(date, 100_000, 0))
                .toList();
        PriceSnapshot snapshot = PriceSnapshot.of(TWO_NIGHTS, "KRW", days, null, 200_000, 0, 200_000);
        return Booking.request(UserId.of("guest_001"), PropertyId.of("prop_test"),
                RoomTypeId.of("room_test"), TWO_NIGHTS, 2, snapshot,
                IdempotencyKey.of("booking-key-0001"), NOW, TTL);
    }

    private static Booking confirmed() {
        Booking booking = held();
        booking.confirm(LATER);
        return booking;
    }

    private static Booking expired() {
        Booking booking = held();
        booking.expireByTtl(EXPIRES_AT);
        return booking;
    }

    private static Booking canceled() {
        Booking booking = confirmed();
        booking.cancel("사정", LATER.plusSeconds(1));
        return booking;
    }

    @Test
    void L1_HELD를_확정하면_CONFIRMED이고_confirmedAt과_version_1이며_전이가_일어났다() {
        Booking booking = held();

        assertTrue(booking.confirm(LATER));

        assertEquals(BookingStatus.CONFIRMED, booking.status());
        assertEquals(LATER, booking.confirmedAt());
        assertEquals(LATER, booking.updatedAt());
        assertEquals(1L, booking.version());
        assertNull(booking.expiredAt());
        assertNull(booking.canceledAt());
        assertNull(booking.expirationReason());
        // 스냅샷과 만료 시각은 전이가 건드리지 않는다(I4). expiresAt은 확정 뒤에도 기록으로 남는다
        assertEquals(EXPIRES_AT, booking.expiresAt());
        assertEquals(200_000L, booking.priceSnapshot().totalAmount());
    }

    @Test
    void L1_CONFIRMED에_재확정은_변화_없이_false다() {
        Booking booking = confirmed();

        assertFalse(booking.confirm(LATER.plusSeconds(5)));

        assertEquals(LATER, booking.confirmedAt());
        assertEquals(1L, booking.version());
    }

    @Test
    void L1_EXPIRED와_CANCELED는_확정할_수_없다() {
        InvalidStateTransitionException fromExpired = assertThrows(
                InvalidStateTransitionException.class, () -> expired().confirm(LATER));
        assertEquals(BookingStatus.EXPIRED, fromExpired.from());
        assertEquals(BookingStatus.CONFIRMED, fromExpired.to());

        assertThrows(InvalidStateTransitionException.class, () -> canceled().confirm(LATER));
    }

    @Test
    void L2_만료_시각이_되면_TTL_EXPIRED로_만료되고_expiredAt과_version_1이다() {
        // 경계의 통과 쪽. 정확히 expiresAt이면 만료다(11 시간 경계)
        Booking booking = held();

        assertTrue(booking.expireByTtl(EXPIRES_AT));

        assertEquals(BookingStatus.EXPIRED, booking.status());
        assertEquals(ExpirationReason.TTL_EXPIRED, booking.expirationReason());
        assertEquals(EXPIRES_AT, booking.expiredAt());
        assertEquals(EXPIRES_AT, booking.updatedAt());
        assertEquals(1L, booking.version());
        assertNull(booking.confirmedAt());
    }

    @Test
    void L2_만료_시각_1초_전에는_TTL_만료를_거부하고_상태가_그대로다() {
        // 경계의 거절 쪽. isDue도 같은 경계를 본다
        Booking booking = held();

        assertFalse(booking.isDue(EXPIRES_AT.minusSeconds(1)));
        assertTrue(booking.isDue(EXPIRES_AT));
        assertThrows(ExpirationNotDueException.class,
                () -> booking.expireByTtl(EXPIRES_AT.minusSeconds(1)));

        assertEquals(BookingStatus.HELD, booking.status());
        assertEquals(0L, booking.version());
        assertNull(booking.expirationReason());
    }

    @Test
    void L2_결제_실패_셋이면_PAYMENT_FAILED로_만료되고_둘이면_거부한다() {
        Booking three = held();
        assertTrue(three.expireByPaymentFailure(3, LATER));
        assertEquals(BookingStatus.EXPIRED, three.status());
        assertEquals(ExpirationReason.PAYMENT_FAILED, three.expirationReason());
        assertEquals(LATER, three.expiredAt());
        assertEquals(1L, three.version());

        Booking two = held();
        assertThrows(FailedAttemptsBelowLimitException.class, () -> two.expireByPaymentFailure(2, LATER));
        assertEquals(BookingStatus.HELD, two.status());
        // 만료 시각 전이라도 실패 셋이면 만료다. TTL과 다른 조건이다
        assertTrue(LATER.isBefore(two.expiresAt()));
    }

    @Test
    void L2_EXPIRED에_재만료는_두_방법_다_변화_없이_false다() {
        Booking booking = expired();

        assertFalse(booking.expireByTtl(EXPIRES_AT.plusSeconds(60)));
        assertFalse(booking.expireByPaymentFailure(3, EXPIRES_AT.plusSeconds(60)));

        assertEquals(ExpirationReason.TTL_EXPIRED, booking.expirationReason());
        assertEquals(EXPIRES_AT, booking.expiredAt());
        assertEquals(1L, booking.version());
    }

    @Test
    void L2_CONFIRMED와_CANCELED는_만료할_수_없다() {
        assertThrows(InvalidStateTransitionException.class, () -> confirmed().expireByTtl(EXPIRES_AT));
        assertThrows(InvalidStateTransitionException.class,
                () -> confirmed().expireByPaymentFailure(3, EXPIRES_AT));
        assertThrows(InvalidStateTransitionException.class, () -> canceled().expireByTtl(EXPIRES_AT));
        assertThrows(InvalidStateTransitionException.class,
                () -> canceled().expireByPaymentFailure(3, EXPIRES_AT));
    }

    @Test
    void L3_CONFIRMED를_취소하면_CANCELED이고_사유와_canceledAt과_version_2다() {
        Booking booking = confirmed();
        Instant canceledAt = LATER.plusSeconds(10);

        assertTrue(booking.cancel("일정 변경", canceledAt));

        assertEquals(BookingStatus.CANCELED, booking.status());
        assertEquals("일정 변경", booking.cancellationReason());
        assertEquals(canceledAt, booking.canceledAt());
        assertEquals(canceledAt, booking.updatedAt());
        assertEquals(2L, booking.version());
        // 확정 기록은 취소 뒤에도 남는다(11 BOOK-04 응답 예시의 confirmedAt 유지)
        assertEquals(LATER, booking.confirmedAt());
    }

    @Test
    void L3_사유_null은_빈_문자열이고_300자는_통과하며_301자는_거절한다() {
        Booking noReason = confirmed();
        assertTrue(noReason.cancel(null, LATER));
        assertEquals("", noReason.cancellationReason());

        Booking maxReason = confirmed();
        assertTrue(maxReason.cancel("가".repeat(Booking.CANCELLATION_REASON_MAX_LENGTH), LATER));
        assertEquals(300, maxReason.cancellationReason().length());

        Booking tooLong = confirmed();
        assertThrows(IllegalArgumentException.class,
                () -> tooLong.cancel("가".repeat(Booking.CANCELLATION_REASON_MAX_LENGTH + 1), LATER));
        assertEquals(BookingStatus.CONFIRMED, tooLong.status());
        assertEquals(1L, tooLong.version());
    }

    @Test
    void L3_CANCELED에_재취소는_변화_없이_false다() {
        Booking booking = canceled();
        Instant firstCanceledAt = booking.canceledAt();

        assertFalse(booking.cancel("다른 사유", LATER.plusSeconds(99)));

        assertEquals("사정", booking.cancellationReason());
        assertEquals(firstCanceledAt, booking.canceledAt());
        assertEquals(2L, booking.version());
    }

    @Test
    void L3_HELD와_EXPIRED는_취소할_수_없다() {
        // HELD 이탈은 TTL이 한다(11 BOOK-04 규칙). 손님이 HELD를 무를 길은 없다
        InvalidStateTransitionException fromHeld = assertThrows(
                InvalidStateTransitionException.class, () -> held().cancel("사정", LATER));
        assertEquals(BookingStatus.HELD, fromHeld.from());
        assertEquals(BookingStatus.CANCELED, fromHeld.to());

        assertThrows(InvalidStateTransitionException.class, () -> expired().cancel("사정", LATER));
    }
}
