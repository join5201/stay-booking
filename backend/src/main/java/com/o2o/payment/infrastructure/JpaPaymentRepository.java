package com.o2o.payment.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.o2o.payment.domain.AttemptKind;
import com.o2o.payment.domain.MockMode;
import com.o2o.payment.domain.Payment;
import com.o2o.payment.domain.PaymentAttemptId;
import com.o2o.payment.domain.PaymentAttemptStatus;
import com.o2o.payment.domain.PaymentRepository;

/**
 * domain이 선언한 PaymentRepository의 구현. 설계 근거: 06-2 6절 결제 CRC, 06-4 1-4.
 *
 * 재고의 JpaDailyInventoryRepository와 같은 구조다. domain은 Spring Data를 모르고 이 클래스가
 * 그 사이를 잇는다(layers.md 3-1). 평가 축의 레이어 역전이 그것을 요구한다.
 */
@Repository
public class JpaPaymentRepository implements PaymentRepository {

    private static final List<MockMode> AUTOMATIC_MODES = List.of(MockMode.APPROVE, MockMode.DECLINE);

    private final PaymentJpaRepository jpaRepository;

    public JpaPaymentRepository(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        return jpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findByBookingId(String bookingId) {
        return jpaRepository.findOneByBookingId(bookingId);
    }

    /** 06-4 1-2 openAttempt Pre의 잠금. 방법은 PESSIMISTIC_WRITE다 */
    @Override
    public Optional<Payment> findByBookingIdForUpdate(String bookingId) {
        return jpaRepository.findOneByBookingIdForUpdate(bookingId);
    }

    /** 06-4 1-2 recordApproval과 recordFailure와 refund의 잠금. 시도 ID로 루트를 잠근다 */
    @Override
    public Optional<Payment> findByAttemptIdForUpdate(PaymentAttemptId attemptId) {
        return jpaRepository.findOneByAttemptIdForUpdate(attemptId.value());
    }

    /** T26. REQUESTED이고 mockMode가 APPROVE나 DECLINE인 NORMAL 시도 */
    @Override
    public List<PaymentAttemptId> findAutoAttemptIdsToResume() {
        return jpaRepository.findAttemptIds(AttemptKind.NORMAL, PaymentAttemptStatus.REQUESTED,
                        AUTOMATIC_MODES).stream()
                .map(PaymentAttemptId::of)
                .toList();
    }
}
