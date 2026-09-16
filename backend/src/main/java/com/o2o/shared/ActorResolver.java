package com.o2o.shared;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 헤더의 행위자 ID를 Actor로 바꾸고 역할을 확인한다. 설계 근거: 11 인증과 접근 제어, P07.
 *
 * 컨트롤러마다 같은 세 줄을 쓰지 않으려고 한 곳에 모았다. 규칙이 한 곳에 있어야 8-1절
 * C10이 그 한 곳을 검사한다.
 *
 * 2026-09-13 T30. 11 인증과 접근 제어 셋째 문단이 개발 프로파일 밖에서는 이 어댑터를
 * 비활성화하라고 적는다. dev 프로파일이 아니면 헤더가 있어도 401이다. 대체 인증이 없는 v1에서
 * 인증 필요 API가 전부 401인 것이 명세가 말한 비활성 그 자체이고 공개 API는 그대로 열린다
 * (task-S9-payment 7절 D-3). ActorRegistry 머리 주석의 프로파일 유예는 이것으로 끝났다.
 */
@Component
public class ActorResolver {

    static final String DEV_PROFILE = "dev";

    private final ActorRegistry registry;
    private final Environment environment;

    public ActorResolver(ActorRegistry registry, Environment environment) {
        this.registry = registry;
        this.environment = environment;
    }

    /**
     * 헤더 없음과 미등록 ID는 401, 역할 불일치는 403이다. 순서가 중요하다. 누구인지 모르는
     * 상태에서 역할을 따질 수 없으므로 401을 먼저 본다. dev 프로파일 밖은 그보다 앞이다.
     */
    public Actor require(String actorId, ActorRole role) {
        if (!environment.matchesProfiles(DEV_PROFILE)) {
            throw new ActorRequiredException();
        }
        Actor actor = registry.find(actorId).orElseThrow(ActorRequiredException::new);
        if (actor.role() != role) {
            throw new ActorAccessDeniedException(role, actor.role());
        }
        return actor;
    }
}
