package com.o2o.shared;

/**
 * 401 ACTOR_REQUIRED. 설계 근거: 11 인증과 접근 제어와 에러 응답 표.
 * 헤더가 없거나 미등록 ID일 때다. 둘을 가르지 않는 것도 그 표가 한 줄로 적기 때문이다.
 */
public class ActorRequiredException extends RuntimeException {

    public ActorRequiredException() {
        super("필요한 개발 행위자가 없거나 미등록이다");
    }
}
