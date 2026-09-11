package com.o2o.promotion.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 조회가 묻는 숙박 구간. checkIn 포함, checkOut 제외. 설계 근거: 11 PROMO-05와 SEARCH-01부터
 * 03의 Query 표(checkOut은 checkIn보다 뒤, 최대 30박, 끝 날짜 제외), 05-3 반열림 규칙,
 * 계약 6절 숙박 기간 상한 30박 행.
 *
 * 예약의 StayPeriod VO(06-2 1절)와 다른 것이다. 그쪽은 예약이 소유하고 I3을 지킨다. 이것은
 * 가격을 묻는 쪽이 들고 오는 질문의 범위다. 이름을 가른 이유가 그것이다.
 *
 * 서버의 오늘 이상 조건은 생성자가 보지 않는다. 오늘은 Clock에서 나오고 값 객체는 Clock을
 * 갖지 않는다. 앱 서비스가 requireNotBefore로 본다. 재고 묶음의 PastStayDateException과
 * 같은 배치다.
 */
public record StayRange(LocalDate checkIn, LocalDate checkOut) {

    public static final int MAX_NIGHTS = 30;

    public StayRange {
        Objects.requireNonNull(checkIn, "checkIn은 null일 수 없다");
        Objects.requireNonNull(checkOut, "checkOut은 null일 수 없다");
        if (!checkIn.isBefore(checkOut)) {
            throw new InvalidStayRangeException("checkOut은 checkIn보다 뒤여야 한다", checkIn, checkOut);
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_NIGHTS) {
            throw new InvalidStayRangeException(
                    "숙박은 " + MAX_NIGHTS + "박을 넘을 수 없다", checkIn, checkOut);
        }
    }

    public static StayRange of(LocalDate checkIn, LocalDate checkOut) {
        return new StayRange(checkIn, checkOut);
    }

    /** 11 응답 모델 PriceQuote의 nights. checkOut - checkIn */
    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** 숙박 날짜 전부. 날짜 오름차순이고 checkOut은 없다 */
    public List<LocalDate> dates() {
        List<LocalDate> dates = new ArrayList<>(nights());
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            dates.add(d);
        }
        return dates;
    }

    /** 11 Query 표의 checkIn은 서버의 오늘 이상. 오늘은 부르는 쪽이 준다 */
    public void requireNotBefore(LocalDate today) {
        if (checkIn.isBefore(today)) {
            throw new InvalidStayRangeException("checkIn은 오늘 이상이어야 한다. 오늘 " + today,
                    checkIn, checkOut);
        }
    }
}
