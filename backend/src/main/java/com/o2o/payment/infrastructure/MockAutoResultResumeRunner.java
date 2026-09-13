package com.o2o.payment.infrastructure;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.domain.PaymentAttemptId;

/**
 * 재시작 재개 러너. 설계 근거: 11 결제 접수와 환불 절(자동 결과는 저장된 REQUESTED와 mockMode로
 * 재시작 후에도 재개한다), T26, 계약 7절 D-1(ApplicationReadyEvent 러너).
 *
 * 앱이 뜬 뒤 REQUESTED이고 mockMode가 자동인 시도를 찾아 건마다 deliverAutoResult를 부른다.
 * 자동 결과 어댑터와 같은 메서드라 처리 길이 하나다. 어댑터와 같은 시도를 두 번 잡아도 Payment
 * 잠금 아래에서 규칙 2와 3이 DUPLICATE로 막는다. 두 번째 실행은 후보가 없어 아무것도 바꾸지
 * 않는다(Y13). 건마다 try와 catch인 이유는 한 건의 실패가 나머지를 막지 않게 하려는 것이다.
 *
 * ApplicationReadyEvent는 spring-boot-4.1.1.jar에서 확인했다(계약 7절 D-3의 확인 목록).
 */
@Component
public class MockAutoResultResumeRunner {

    private static final Logger log = LoggerFactory.getLogger(MockAutoResultResumeRunner.class);

    private final PaymentApplicationService paymentService;

    public MockAutoResultResumeRunner(PaymentApplicationService paymentService) {
        this.paymentService = paymentService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        int delivered = resumeAutoResults();
        if (delivered > 0) {
            log.info("재시작 뒤 자동 결과 재개. {}건", delivered);
        }
    }

    /** 재개한 건수를 돌려준다. 테스트가 직접 부른다(Y13) */
    public int resumeAutoResults() {
        List<PaymentAttemptId> candidates = paymentService.findAutoAttemptsToResume();
        int delivered = 0;
        for (PaymentAttemptId attemptId : candidates) {
            try {
                if (paymentService.deliverAutoResult(attemptId).isPresent()) {
                    delivered++;
                }
            } catch (RuntimeException e) {
                log.error("자동 결과 재개 실패. attemptId={}", attemptId.value(), e);
            }
        }
        return delivered;
    }
}
