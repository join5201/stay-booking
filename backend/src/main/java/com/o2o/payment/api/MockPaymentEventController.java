package com.o2o.payment.api;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.shared.ActorResolver;
import com.o2o.shared.ActorRole;

import jakarta.validation.Valid;

/**
 * INTERNAL-01 Mock 결제 결과 전달. 설계 근거: 11 내부 처리와 Mock 이벤트 절의 INTERNAL-01(요청 표,
 * 200 MockEventResult, 처리 규칙 첫 줄의 개발 프로파일), 11 인증과 접근 제어의 MOCK_SYSTEM 행,
 * 계약 2절 검사 순서 1행과 2행, 7절 D-3.
 *
 * 검사 순서. body 형식은 프레임워크가 인자를 해석하며 먼저 보고, 그다음 행위자, 그다음 앱
 * 서비스다. 예약과 재고의 컨트롤러와 같다. 3행부터 13행은 앱 서비스와 도메인의 몫이고 그 예외는
 * PaymentExceptionHandler가 11의 코드로 바꾼다.
 *
 * dev 프로파일에서만 뜬다(T30). 밖에서는 이 경로가 없어 404다. 11 인증과 접근 제어 셋째 문단이
 * 개발 프로파일 밖에서 /internal 경로를 비활성화하라고 적는다. 경로에 /api/v1이 없는 것도 명세
 * 그대로다. 내부 경로라 서비스 API의 기본 경로를 따르지 않는다.
 */
@RestController
@Profile("dev")
public class MockPaymentEventController {

    static final String EVENTS_PATH = "/internal/mock-payments/events";

    private final PaymentApplicationService paymentService;
    private final ActorResolver actorResolver;

    public MockPaymentEventController(PaymentApplicationService paymentService,
                                      ActorResolver actorResolver) {
        this.paymentService = paymentService;
        this.actorResolver = actorResolver;
    }

    /** 200 MockEventResult. PROCESSED와 DUPLICATE 둘 다 200이다(규칙 2와 3) */
    @PostMapping(EVENTS_PATH)
    public MockEventResultResponse deliver(
            @RequestHeader(value = "X-Dev-Actor-Id", required = false) String actorId,
            @Valid @RequestBody MockPaymentEventRequest request) {
        actorResolver.require(actorId, ActorRole.MOCK_SYSTEM);
        return MockEventResultResponse.from(paymentService.handleMockEvent(request.toCommand()));
    }
}
