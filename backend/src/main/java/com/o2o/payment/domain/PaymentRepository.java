package com.o2o.payment.domain;

import java.util.List;
import java.util.Optional;

/**
 * 설계 근거: 06-2 6절 결제 CRC, 06-2 1절이 Payment를 애그리거트 루트로 적는다. 시도는 루트를
 * 통해서만 접근되므로 시도에는 리포지토리를 두지 않는다.
 *
 * ForUpdate가 붙은 조회가 따로 있는 근거는 06-2 5절 Payment 행(시도 추가와 콜백 기록은 루트를
 * 잠근 뒤 수행)과 06-4 0절의 비관적 락 확정이다. 재고 리포지토리와 같은 이름 규칙이다. 잠금이
 * 붙는 조회는 부르는 쪽이 알고 골라야 하므로 이름을 가른다.
 */
public interface PaymentRepository {

    Payment save(Payment payment);

    /** attemptsOf 조회. 잠그지 않는다 */
    Optional<Payment> findByBookingId(String bookingId);

    /** openAttempt. 06-4 1-2 openAttempt Pre의 잠금. 없으면 첫 요청이라 앱 서비스가 만든다(U3) */
    Optional<Payment> findByBookingIdForUpdate(String bookingId);

    /**
     * recordApproval과 recordFailure와 refund. 시도 ID로 그 시도를 가진 루트를 잠그고 읽는다.
     * 계약 2절 INTERNAL-01 표 3행과 4행이다. 없으면 UnknownAttempt(11 규칙 1의 404)다.
     */
    Optional<Payment> findByAttemptIdForUpdate(PaymentAttemptId attemptId);

    /**
     * 재시작 재개(T26). REQUESTED이고 mockMode가 자동인 NORMAL 시도의 ID. 11 결제 접수와 환불
     * 절이 저장된 REQUESTED와 mockMode로 재시작 후에도 재개한다고 적는다. 잠그지 않는다.
     * 잠금은 건별 처리가 다시 건다.
     */
    List<PaymentAttemptId> findAutoAttemptIdsToResume();
}
