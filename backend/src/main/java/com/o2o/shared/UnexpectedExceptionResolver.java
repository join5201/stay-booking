package com.o2o.shared;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * 아무도 잡지 않은 예외의 마지막 자리. 11 에러 응답 표의 500 INTERNAL_ERROR다.
 * 설계 근거: 11 에러 응답 표(예상하지 못한 오류. 내부 예외 내용은 응답에 넣지 않음). R1 평가 B-04.
 *
 * 어드바이스의 Exception 핸들러가 아니라 순번이 가장 늦은 HandlerExceptionResolver로 둔 이유.
 * 어드바이스 여섯(shared 하나, 컨텍스트마다 하나)은 등록 순서대로 물어보고 먼저 맞는 핸들러가
 * 이긴다. shared 어드바이스에 Exception 핸들러를 두면 그 어드바이스가 앞에 설 때 컨텍스트
 * 핸들러의 404와 409를 전부 500으로 삼킨다. 지금은 패키지 이름 순으로 shared가 맨 뒤라 우연히
 * 안전할 뿐이다. 이 클래스는 어드바이스와 스프링 기본 처리기가 모두 지나간 뒤에만 불리므로
 * 순서를 이름에 기대지 않는다. 스프링 기본 처리기가 맡는 404와 405와 415는 여기 오지 않는다.
 *
 * 본문에 예외의 클래스나 메시지를 넣지 않는다. 로그에 traceId와 함께 남기고 응답에는 traceId만
 * 준다. 사람이 로그와 응답을 그 값으로 잇는다.
 */
@Component
public class UnexpectedExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger log = LoggerFactory.getLogger(UnexpectedExceptionResolver.class);

    private final JsonMapper jsonMapper;

    public UnexpectedExceptionResolver(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                         Object handler, Exception ex) {
        ErrorResponse body = ErrorResponse.of("INTERNAL_ERROR", "서버 내부 오류입니다.");
        log.error("예상하지 못한 예외. traceId={} {} {}", body.traceId(), request.getMethod(),
                request.getRequestURI(), ex);
        if (response.isCommitted()) {
            return null;
        }
        try {
            response.resetBuffer();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(jsonMapper.writeValueAsString(body));
        } catch (IOException e) {
            return null;
        }
        // 빈 ModelAndView는 처리 완료의 표시다. 뷰 없이 응답이 이미 써졌다
        return new ModelAndView();
    }
}
