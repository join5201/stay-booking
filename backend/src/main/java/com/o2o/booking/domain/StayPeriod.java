package com.o2o.booking.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 숙박 기간 값 객체. 설계 근거: 06-2 1절 Booking의 StayPeriod VO, 05-3 숙박 기간 행.
 * checkIn 이상 checkOut 미만이다. 끝 날짜를 빼는 이유는 체크아웃 날에는 묵지 않아서다(T06).
 *
 * I3(checkIn < checkOut)은 생성자가 지킨다. 최대 30박은 11 BOOK-01 요청 표(P02)다. 서버의 오늘
 * 이상은 of가 지킨다. 저장된 예약을 다시 읽을 때는 그 검사를 하지 않아야 하므로 생성자에
 * 두지 않는다. 오늘은 서울 날짜다(P03, SeoulDate).
 */
@Embeddable
public record StayPeriod(@Column(name = "check_in", nullable = false) LocalDate checkIn,
                         @Column(name = "check_out", nullable = false) LocalDate checkOut) {

    public static final int MAX_NIGHTS = 30;

    public StayPeriod {
        Objects.requireNonNull(checkIn, "checkIn은 null일 수 없다");
        Objects.requireNonNull(checkOut, "checkOut은 null일 수 없다");
        if (!checkIn.isBefore(checkOut)) {
            throw new InvalidBookingPeriodException("checkOut은 checkIn보다 뒤여야 한다",
                    checkIn, checkOut);
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_NIGHTS) {
            throw new InvalidBookingPeriodException("최대 " + MAX_NIGHTS + "박이다", checkIn, checkOut);
        }
    }

    /** 새 예약의 기간. 과거 checkIn을 막는다. 11 BOOK-01 요청 표의 서버의 오늘 이상 */
    public static StayPeriod of(LocalDate checkIn, LocalDate checkOut, LocalDate today) {
        StayPeriod period = new StayPeriod(checkIn, checkOut);
        if (checkIn.isBefore(today)) {
            throw new InvalidBookingPeriodException("checkIn은 오늘 " + today + " 이상이어야 한다",
                    checkIn, checkOut);
        }
        return period;
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** 숙박 날짜를 오름차순으로. checkOut은 없다 */
    public List<LocalDate> dates() {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            dates.add(d);
        }
        return List.copyOf(dates);
    }
}
