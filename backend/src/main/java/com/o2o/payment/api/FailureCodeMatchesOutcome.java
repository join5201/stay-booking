package com.o2o.payment.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * INTERNAL-01 요청 표의 failureCode 행. outcome이 FAILED면 필수이고 MOCK_DECLINED만이며 APPROVED에서는
 * 보내지 않는다. 필드 하나의 제약이 아니라 두 필드의 조합이라 타입 수준 제약으로 둔다. 위반은
 * details의 field를 failureCode로 낸다(11 에러 응답 절의 ErrorDetail).
 */
@Documented
@Constraint(validatedBy = FailureCodeMatchesOutcomeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FailureCodeMatchesOutcome {

    String message() default "outcome이 FAILED면 MOCK_DECLINED가 필수이고 APPROVED면 보내지 않습니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
