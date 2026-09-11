package com.o2o.booking.domain;

import java.util.Optional;

import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;

/**
 * 설계 근거: 06-2 6절 예약 CRC의 협력자 리포지토리, 06-2 1절이 Booking을 애그리거트 루트로
 * 적는다. 루트가 아닌 것(StayPeriod, PriceSnapshot, DailyPrice)에는 리포지토리를 두지 않는다.
 *
 * 1차에는 잠금 조회가 없다. 08-3 결정 3의 잠금 순서(Booking, Payment, 재고)에서 Booking을
 * 잠그는 경로는 확정과 만료와 취소라 2차가 findForUpdate를 더한다. 생성 경로는 만들 Booking이
 * 아직 없어 잠글 것이 없다(계약 6절 잠금 순서 행).
 */
public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(BookingId bookingId);

    /**
     * BOOK-02. 본인 예약만. status가 null이면 전체다. 정렬은 createdAt 내림차순, 동률이면 id
     * 내림차순이다(11 목록 규칙 89행). 정렬을 질의가 하는 이유는 재고의 findRange와 같다.
     */
    PageResult<Booking> findByUserId(UserId userId, BookingStatus status, PageQuery pageQuery);
}
