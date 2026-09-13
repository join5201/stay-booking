package com.o2o.booking.application;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.booking.domain.BookingStatus;
import com.o2o.payment.application.PaymentApplicationService;

/**
 * ExpireBooking의 건별 처리. T1 TTL 만료의 한 건과 PAY-01의 선만료가 같은 메서드다. 설계 근거:
 * 06-2 6절 예약 CRC의 ExpireBooking 행(잠그고 전이시킨 뒤 같은 트랜잭션에서 재고 반환), 2차
 * 계약 2절 T1 표(잠금 뒤 재확인 넷)와 7절 D-2 가(잠금 전 별도 트랜잭션의 선만료), 08-3 결정 8과
 * 11의 11-1(확정 우선).
 *
 * REQUIRES_NEW인 이유 둘. T1은 건마다 독립 트랜잭션이라 한 건의 실패가 배치를 되돌리지 않는다.
 * PAY-01은 409를 내려고 예외를 던지면 자기 트랜잭션이 되돌아가는데 11 명세는 만료와 재고 반환을
 * 먼저 저장하라 적으므로 그 저장이 바깥과 무관하게 커밋돼야 한다.
 *
 * 확정 우선. 잠근 뒤 결제에 승인 시도가 있으면 만료 대신 확정한다. P1이 유실된 예약(HELD인데
 * 승인 이력 있음)이 여기서 닫힌다(7절 D-1 나의 치유 자리). 잠금 순서는 Booking, Payment(조회만),
 * 재고 N행이다. 처리 시각은 여기서 읽는다(11 시간 경계의 전이 검사 시각).
 */
@Service
public class BookingExpirationService {

    private static final Logger log = LoggerFactory.getLogger(BookingExpirationService.class);

    /** 건별 처리의 결과. 호출자가 응답이나 로그로 바꾼다 */
    public enum Outcome {
        /** HELD가 아니거나 아직 만료 시각 전이라 아무것도 하지 않았다 */
        SKIPPED,
        /** TTL_EXPIRED로 만료시키고 선점을 반환했다 */
        EXPIRED,
        /** 승인 시도가 있어 만료 대신 확정했다(확정 우선) */
        CONFIRMED
    }

    private final BookingRepository bookingRepository;
    private final BookingLifecycle lifecycle;
    private final PaymentApplicationService paymentService;
    private final Clock clock;

    public BookingExpirationService(BookingRepository bookingRepository, BookingLifecycle lifecycle,
                                    PaymentApplicationService paymentService, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.lifecycle = lifecycle;
        this.paymentService = paymentService;
        this.clock = clock;
    }

    /**
     * 잠근 뒤 재확인하고 만료 시각이 지난 HELD만 끝낸다. 없는 예약은 SKIPPED다. 스캔 목록을 읽은
     * 뒤 다른 경로가 먼저 전이했거나 예약이 사라진 경우라 오류가 아니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome expireIfDue(BookingId bookingId) {
        Instant now = Instant.now(clock);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
        if (booking == null || booking.status() != BookingStatus.HELD || !booking.isDue(now)) {
            return Outcome.SKIPPED;
        }
        String approvedAttemptId = paymentService.attemptsOf(bookingId.value()).approvedAttemptId();
        if (approvedAttemptId != null) {
            log.info("만료 대신 확정. 승인 시도가 있다. booking={} attempt={}", bookingId.value(),
                    approvedAttemptId);
            lifecycle.confirm(booking, now);
            return Outcome.CONFIRMED;
        }
        lifecycle.expireByTtl(booking, now);
        return Outcome.EXPIRED;
    }
}
