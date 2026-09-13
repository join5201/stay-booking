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
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.domain.PaymentApproved;
import com.o2o.payment.domain.PaymentFailed;
import com.o2o.payment.domain.RefundReason;

/**
 * 결제 결과에 대한 예약 정책 둘. P1 결제 승인 시 예약 확정(지연 승인이면 환불)과 P3 결제 실패 시
 * 만료. 설계 근거: 06-4 2-2 정책 카드(결제 승인 시 예약 확정, 승인 지연 시 자동 환불, 결제 실패 시
 * 만료), 06-4 v5 2-3 정책 소속(예약 앱 서비스), 11 INTERNAL-01 규칙 5와 6, 11 상태 전이와 시간
 * 경계, 2차 계약 2절 P1 표와 P3 문단.
 *
 * 부르는 쪽은 booking/infrastructure의 구독자 어댑터다. 결제가 자기 트랜잭션을 커밋한 뒤
 * AFTER_COMMIT으로 받아 try와 catch로 이 REQUIRES_NEW 메서드를 부른다(08-3 결정 6, 06-4 v5 0-3).
 * 그래서 여기의 실패는 결제 몫을 되돌리지 않고 콜백 응답에도 나타나지 않는다. 같은 이벤트의
 * 재전달은 결제가 DUPLICATE로 막아 다시 오지 않으므로 유실은 T1의 확정 우선이 닫는다(D-1 나).
 *
 * 분기 입력은 이벤트가 아니라 잠근 뒤의 Booking 상태다(06-4 v5 2-4). CONFIRMED와 CANCELED는
 * 로그 후 무시다. 정책 경로에서 종착 상태는 오류가 아니다(08-3 결정 5). 잠금 순서는 Booking,
 * Payment(환불), 재고 N행이다(08-3 결정 3). 처리 시각은 여기서 읽는다(11 시간 경계. 정확히
 * expiresAt이면 만료).
 */
@Service
public class PaymentOutcomeService {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutcomeService.class);

    private final BookingRepository bookingRepository;
    private final BookingLifecycle lifecycle;
    private final PaymentApplicationService paymentService;
    private final Clock clock;

    public PaymentOutcomeService(BookingRepository bookingRepository, BookingLifecycle lifecycle,
                                 PaymentApplicationService paymentService, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.lifecycle = lifecycle;
        this.paymentService = paymentService;
        this.clock = clock;
    }

    /**
     * P1. HELD이고 만료 시각 전이면 확정(A3). HELD인데 만료 시각 이상이거나 이미 EXPIRED면 지연
     * 승인이라 전액 환불이고 HELD였으면 TTL_EXPIRED로 끝낸다(T18, T20). 환불이 재고보다 먼저인
     * 이유는 잠금 순서다. 없는 예약은 불변 위반이다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApproved(PaymentApproved event) {
        Instant now = Instant.now(clock);
        BookingId bookingId = BookingId.of(event.bookingId());
        Booking booking = lockOrFail(bookingId);
        switch (booking.status()) {
            case HELD -> {
                if (!booking.isDue(now)) {
                    lifecycle.confirm(booking, now);
                } else {
                    log.info("지연 승인. 만료 시각을 지나 환불하고 만료시킨다. booking={} attempt={}",
                            bookingId.value(), event.paymentAttemptId().value());
                    paymentService.refund(event.paymentAttemptId(), RefundReason.LATE_APPROVAL);
                    lifecycle.expireByTtl(booking, now);
                }
            }
            case EXPIRED -> {
                log.info("지연 승인. 이미 만료된 예약이라 환불만 한다. booking={} attempt={}",
                        bookingId.value(), event.paymentAttemptId().value());
                paymentService.refund(event.paymentAttemptId(), RefundReason.LATE_APPROVAL);
            }
            case CONFIRMED, CANCELED -> log.info("승인 처리 무시. 종착 상태다. booking={} status={}",
                    bookingId.value(), booking.status());
        }
    }

    /**
     * P3. 시도 수가 3 미만이면 아무것도 하지 않는다(T16). 3 이상이고 HELD면 만료하고 선점을
     * 반환한다(T17). 처리 시각에 TTL이 이미 지났으면 TTL_EXPIRED가 우선한다(11 시간 경계).
     * EXPIRED면 무해, CONFIRMED와 CANCELED면 로그 후 무시다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onFailed(PaymentFailed event) {
        if (event.attemptCount() < Booking.PAYMENT_FAILURE_LIMIT) {
            return;
        }
        Instant now = Instant.now(clock);
        BookingId bookingId = BookingId.of(event.bookingId());
        Booking booking = lockOrFail(bookingId);
        switch (booking.status()) {
            case HELD -> {
                if (booking.isDue(now)) {
                    lifecycle.expireByTtl(booking, now);
                } else {
                    lifecycle.expireByPaymentFailure(booking, event.attemptCount(), now);
                }
            }
            case EXPIRED -> log.info("실패 처리 무해. 이미 만료다. booking={}", bookingId.value());
            case CONFIRMED, CANCELED -> log.info("실패 처리 무시. 종착 상태다. booking={} status={}",
                    bookingId.value(), booking.status());
        }
    }

    private Booking lockOrFail(BookingId bookingId) {
        return bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalStateException(
                        "결제 결과의 예약이 없다. 불변 위반이다: " + bookingId.value()));
    }
}
