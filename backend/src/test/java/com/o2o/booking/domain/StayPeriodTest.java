package com.o2o.booking.domain;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * K1. 설계 근거: task-S9-booking 8-1절, 06-2 3-1 I3(checkIn < checkOut), 11 BOOK-01 요청 표
 * (최대 30박, 서버의 오늘 이상, 끝 날짜 제외), T06(체크아웃 날짜는 숙박 대상이 아니다).
 */
class StayPeriodTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-10-02");
    private static final LocalDate CHECK_IN = LocalDate.parse("2026-10-10");

    @Test
    void K1_체크인이_체크아웃과_같거나_뒤면_거절한다() {
        // I3. 같은 날은 0박이라 기간이 아니다
        assertThrows(InvalidBookingPeriodException.class,
                () -> new StayPeriod(CHECK_IN, CHECK_IN));
        assertThrows(InvalidBookingPeriodException.class,
                () -> new StayPeriod(CHECK_IN, CHECK_IN.minusDays(1)));
    }

    @Test
    void K1_1박은_통과하고_박수와_날짜_목록이_오름차순이다() {
        // 거절의 짝. 끝 날짜는 목록에 없다(T06)
        StayPeriod period = new StayPeriod(CHECK_IN, CHECK_IN.plusDays(3));

        assertEquals(3, period.nights());
        assertEquals(List.of(CHECK_IN, CHECK_IN.plusDays(1), CHECK_IN.plusDays(2)), period.dates());
        assertEquals(1, new StayPeriod(CHECK_IN, CHECK_IN.plusDays(1)).nights());
    }

    @Test
    void K1_30박은_통과하고_31박은_거절한다() {
        // 경계는 양쪽을 본다(testing.md T2). 11 BOOK-01 요청 표의 최대 30박
        assertEquals(30, new StayPeriod(CHECK_IN, CHECK_IN.plusDays(30)).nights());
        assertThrows(InvalidBookingPeriodException.class,
                () -> new StayPeriod(CHECK_IN, CHECK_IN.plusDays(31)));
    }

    @Test
    void K1_새_예약의_체크인은_오늘_이상이어야_한다() {
        // 오늘은 통과, 어제는 거절. 저장된 예약을 다시 읽는 생성자는 이 검사를 하지 않는다
        assertEquals(TODAY, StayPeriod.of(TODAY, TODAY.plusDays(1), TODAY).checkIn());
        assertThrows(InvalidBookingPeriodException.class,
                () -> StayPeriod.of(TODAY.minusDays(1), TODAY.plusDays(1), TODAY));
        assertEquals(TODAY.minusDays(1),
                new StayPeriod(TODAY.minusDays(1), TODAY.plusDays(1)).checkIn());
    }
}
