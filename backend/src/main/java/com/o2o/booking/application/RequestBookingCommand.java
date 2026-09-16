package com.o2o.booking.application;

import java.time.LocalDate;
import java.util.Objects;

import com.o2o.booking.domain.IdempotencyKey;
import com.o2o.booking.domain.UserId;
import com.o2o.shared.RoomTypeId;

/**
 * RequestBooking 커맨드. 설계 근거: 03 예약 커맨드 행, 11 BOOK-01 요청 본문. 이용자는 body가
 * 아니라 행위자에서 온다(BOOK-01 처리 규칙). 객실 수는 1이고 받지 않는다(08-3 결정 11의 11-5).
 * 도메인 이름은 userId와 userCount다(05-3 v8). API의 guestId와 guestCount 변환은 api 층이 한다.
 * 날짜를 StayPeriod가 아니라 LocalDate 둘로 받는 이유는 과거 검사에 서버의 오늘이 필요하고
 * 그 오늘은 Clock을 가진 앱 서비스가 알기 때문이다.
 */
public record RequestBookingCommand(UserId userId, RoomTypeId roomTypeId, LocalDate checkIn,
                                    LocalDate checkOut, int userCount, long expectedTotalAmount,
                                    String currency, IdempotencyKey idempotencyKey) {

    public RequestBookingCommand {
        Objects.requireNonNull(userId, "userId는 null일 수 없다");
        Objects.requireNonNull(roomTypeId, "roomTypeId는 null일 수 없다");
        Objects.requireNonNull(checkIn, "checkIn은 null일 수 없다");
        Objects.requireNonNull(checkOut, "checkOut은 null일 수 없다");
        Objects.requireNonNull(currency, "currency는 null일 수 없다");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey는 null일 수 없다");
    }
}
