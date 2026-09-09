package com.o2o.shared;

import org.springframework.stereotype.Component;

/**
 * 헤더의 행위자 ID를 Actor로 바꾸고 역할을 확인한다. 설계 근거: 11 인증과 접근 제어, P07.
 *
 * 컨트롤러마다 같은 세 줄을 쓰지 않으려고 한 곳에 모았다. 규칙이 한 곳에 있어야 8-1절
 * C10이 그 한 곳을 검사한다.
 */
@Component
public class ActorResolver {

    private final ActorRegistry registry;

    public ActorResolver(ActorRegistry registry) {
        this.registry = registry;
    }

    /**
     * 헤더 없음과 미등록 ID는 401, 역할 불일치는 403이다. 순서가 중요하다. 누구인지 모르는
     * 상태에서 역할을 따질 수 없으므로 401을 먼저 본다.
     */
    public Actor require(String actorId, ActorRole role) {
        Actor actor = registry.find(actorId).orElseThrow(ActorRequiredException::new);
        if (actor.role() != role) {
            throw new ActorAccessDeniedException(role, actor.role());
        }
        return actor;
    }
}
