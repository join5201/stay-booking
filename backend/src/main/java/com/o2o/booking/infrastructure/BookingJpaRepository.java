package com.o2o.booking.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingStatus;

/**
 * 설계 근거: 06-4 1-4의 유일성과 무결성은 DB. 재고와 카탈로그의 같은 자리와 성격이 같다.
 * 정렬은 Pageable이 준다(JpaBookingRepository). status가 null이면 전체다.
 */
public interface BookingJpaRepository extends JpaRepository<Booking, String> {

    @Query("select b from Booking b where b.userId = :userId "
            + "and (:status is null or b.status = :status)")
    Page<Booking> findAllByUserId(@Param("userId") String userId,
                                  @Param("status") BookingStatus status, Pageable pageable);
}
