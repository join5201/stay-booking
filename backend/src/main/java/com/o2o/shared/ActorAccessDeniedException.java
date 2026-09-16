package com.o2o.shared;

/**
 * 403 ACCESS_DENIED. 설계 근거: 11 에러 응답 표의 역할 불일치 행.
 *
 * 이름에 Actor를 붙인 이유는 스프링 시큐리티의 같은 이름 예외와 헷갈리지 않기 위해서다.
 * 이번 바퀴에 시큐리티는 의존성에 없다.
 */
public class ActorAccessDeniedException extends RuntimeException {

    public ActorAccessDeniedException(ActorRole required, ActorRole actual) {
        super("역할이 다르다. 필요 " + required + ", 실제 " + actual);
    }
}
