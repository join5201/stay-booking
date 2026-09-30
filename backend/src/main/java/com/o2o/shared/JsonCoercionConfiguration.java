package com.o2o.shared;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * 설계 근거: 11 공통 요청과 응답 규칙 40행(잘못된 타입은 400), 이슈 217.
 *
 * 문자열 칸에 숫자와 불리언이 오면 거절한다. jackson-databind 3.1.5 기본값은 {"name":123}을
 * "123"으로 받는다. application.properties의 accept-float-as-int와 allow-coercion-of-scalars는
 * 숫자와 불리언 칸 쪽만 막고 이 방향은 못 막는다(2026-09-29 standalone 실측). 이 방향은 설정 키가
 * 없어서 빌더의 강제 변환 설정으로 끈다. 거절은 HttpMessageNotReadableException이 되어
 * SharedExceptionHandler가 400 INVALID_REQUEST로 낸다.
 */
@Configuration
public class JsonCoercionConfiguration {

    @Bean
    public JsonMapperBuilderCustomizer rejectScalarToString() {
        return (builder) -> builder.withCoercionConfig(LogicalType.Textual, (config) -> config
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
