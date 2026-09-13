package com.o2o.booking;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.booking.application.BookingLifecycle;
import com.o2o.booking.application.PaymentOutcomeService;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.domain.PaymentApproved;

/**
 * L13의 테스트 전용 훅. 무장하면 다음 P1 처리 한 번이 본체를 다 돌린 뒤 커밋 직전에 예외로 나간다.
 * 확정과 재고 이동이 같은 REQUIRES_NEW 트랜잭션에 있으니 둘 다 되돌아가고, 결제의 승인은 그 앞
 * 트랜잭션에서 이미 커밋돼 그대로다. 이것이 T23의 부분 결과 롤백이다. 구독자 어댑터가 예외를
 * 잡아 로그하고 T1의 확정 우선이 나중에 닫는다(7절 D-1 나).
 *
 * 재정의 메서드에 REQUIRES_NEW를 다시 붙이는 이유는 트랜잭션 속성이 재정의 메서드에서 상속되지
 * 않기 때문이다. 없으면 통과 경로까지 트랜잭션 밖에서 돌게 된다. @Primary로 등록해 운영 빈 대신
 * 어댑터에 주입된다.
 */
public class FailingOncePaymentOutcomeService extends PaymentOutcomeService {

    private final AtomicBoolean failNextApproval = new AtomicBoolean(false);

    public FailingOncePaymentOutcomeService(BookingRepository bookingRepository,
                                            BookingLifecycle lifecycle,
                                            PaymentApplicationService paymentService, Clock clock) {
        super(bookingRepository, lifecycle, paymentService, clock);
    }

    public void failNextApproval() {
        failNextApproval.set(true);
    }

    public void disarm() {
        failNextApproval.set(false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApproved(PaymentApproved event) {
        super.onApproved(event);
        if (failNextApproval.getAndSet(false)) {
            throw new TestHookException("테스트 훅. 승인 처리를 커밋 직전에 강제 실패");
        }
    }
}
