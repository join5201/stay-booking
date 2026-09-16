package com.o2o.booking.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.booking.domain.UserId;
import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;

/**
 * domain이 선언한 BookingRepository의 구현. 설계 근거: 06-2 6절 CRC 협력자, 06-4 1-4.
 * 재고의 JpaDailyInventoryRepository와 같은 구조다.
 */
@Repository
public class JpaBookingRepository implements BookingRepository {

    // 11 목록 규칙 89행. 예약 목록은 createdAt 내림차순, 동률이면 id 내림차순이다
    private static final Sort BOOKING_ORDER = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final BookingJpaRepository jpaRepository;

    public JpaBookingRepository(BookingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Booking save(Booking booking) {
        return jpaRepository.save(booking);
    }

    @Override
    public Optional<Booking> findById(BookingId bookingId) {
        return jpaRepository.findById(bookingId.value());
    }

    @Override
    public Optional<Booking> findByIdForUpdate(BookingId bookingId) {
        return jpaRepository.findByIdForUpdate(bookingId.value());
    }

    @Override
    public List<BookingId> findDueIds(Instant now, int limit) {
        return jpaRepository.findDueIds(BookingStatus.HELD, now, PageRequest.of(0, limit))
                .stream().map(BookingId::of).toList();
    }

    @Override
    public PageResult<Booking> findByUserId(UserId userId, BookingStatus status,
                                            PageQuery pageQuery) {
        Page<Booking> page = jpaRepository.findAllByUserId(userId.value(), status,
                PageRequest.of(pageQuery.page(), pageQuery.size(), BOOKING_ORDER));
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
