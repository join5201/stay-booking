package com.o2o.payment.api;

import com.o2o.payment.domain.PaymentAttempt;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** FailureCodeMatchesOutcome의 판정. outcome이 APPROVED도 FAILED도 아니면 그 필드의 제약이 따로 잡는다 */
public class FailureCodeMatchesOutcomeValidator
        implements ConstraintValidator<FailureCodeMatchesOutcome, MockPaymentEventRequest> {

    @Override
    public boolean isValid(MockPaymentEventRequest request, ConstraintValidatorContext context) {
        if (request == null || request.outcome() == null) {
            return true;
        }
        boolean valid = switch (request.outcome()) {
            case "FAILED" -> PaymentAttempt.MOCK_DECLINED.equals(request.failureCode());
            case "APPROVED" -> request.failureCode() == null;
            default -> true;
        };
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("failureCode")
                    .addConstraintViolation();
        }
        return valid;
    }
}
