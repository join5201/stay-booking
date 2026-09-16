package com.o2o.booking.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingStatus;

import jakarta.persistence.LockModeType;

/**
 * 설계 근거: 06-4 1-4의 유일성과 무결성은 DB. 재고와 카탈로그의 같은 자리와 성격이 같다.
 * 정렬은 Pageable이 준다(JpaBookingRepository). status가 null이면 전체다.
 */
public interface BookingJpaRepository extends JpaRepository<Booking, String> {

    @Query("select b from Booking b where b.userId = :userId "
            + "and (:status is null or b.status = :status)")
    Page<Booking> findAllByUserId(@Param("userId") String userId,
                                  @Param("status") BookingStatus status, Pageable pageable);

    // 행 잠금을 건다. 08-3 결정 3의 잠금 순서에서 Booking이 첫 자리다. 방법은 재고의 findRangeForUpdate와 같다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") String id);

    // T1 스캔. 잠그지 않는다. 정렬은 11 P01의 due 조건에 오래된 것부터, 동률이면 id로 결정적이게
    @Query("select b.id from Booking b where b.status = :status and b.expiresAt <= :now "
            + "order by b.expiresAt asc, b.id asc")
    List<String> findDueIds(@Param("status") BookingStatus status, @Param("now") Instant now,
                            Pageable pageable);
}
