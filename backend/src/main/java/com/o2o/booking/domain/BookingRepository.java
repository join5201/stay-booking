package com.o2o.booking.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;

/**
 * 설계 근거: 06-2 6절 예약 CRC의 협력자 리포지토리, 06-2 1절이 Booking을 애그리거트 루트로
 * 적는다. 루트가 아닌 것(StayPeriod, PriceSnapshot, DailyPrice)에는 리포지토리를 두지 않는다.
 *
 * 잠금 조회는 2차가 더했다. 08-3 결정 3의 잠금 순서(Booking, Payment, 재고)에서 Booking을
 * 잠그는 경로는 확정과 만료와 취소와 결제 중계다. 생성 경로는 만들 Booking이 아직 없어 잠글
 * 것이 없다(1차 계약 6절 잠금 순서 행). due 조회는 T1 스케줄러의 스캔 몫이고 잠그지 않는다.
 * 건별 처리가 다시 잠그고 재확인한다(2차 계약 2절 T1 표).
 */
public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(BookingId bookingId);

    /**
     * 행 잠금을 걸고 읽는다. 잠금 순서의 첫 자리다(08-3 결정 3). 잠근 채 상태를 재확인하고
     * 전이시키는 호출자만 쓴다. 방법(PESSIMISTIC_WRITE)은 인프라가 안다.
     */
    Optional<Booking> findByIdForUpdate(BookingId bookingId);

    /**
     * T1 만료 대상. status가 HELD이고 expiresAt이 now 이하인 예약의 ID를 expiresAt 오름차순,
     * 동률이면 id 오름차순으로 최대 limit개. 잠그지 않는다(11 P01의 due 조건).
     */
    List<BookingId> findDueIds(Instant now, int limit);

    /**
     * BOOK-02. 본인 예약만. status가 null이면 전체다. 정렬은 createdAt 내림차순, 동률이면 id
     * 내림차순이다(11 목록 규칙 89행). 정렬을 질의가 하는 이유는 재고의 findRange와 같다.
     */
    PageResult<Booking> findByUserId(UserId userId, BookingStatus status, PageQuery pageQuery);
}
