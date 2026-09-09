package com.o2o.shared;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * 개발 행위자 fixture. 설계 근거: 11 인증과 접근 제어.
 *
 * 예시 fixture를 host_001, operator_001, guest_001, mock_001로 적고 서버가 ID를 역할에
 * 매핑한다고 한 그 매핑이다. 값을 코드에 두는 이유는 이것이 비밀이 아니라 명세에 적힌
 * 공개 값이기 때문이다.
 *
 * 개발 프로파일 밖에서는 이 어댑터를 비활성화하라고 같은 절이 적는다. 이번 바퀴는 로컬
 * 개발과 검증까지가 범위라 프로파일 분리를 하지 않는다. 배포가 범위에 들어올 때 건다.
 */
@Component
public class ActorRegistry {

    private static final Map<String, ActorRole> FIXTURE = Map.of(
            "host_001", ActorRole.HOST,
            "operator_001", ActorRole.OPERATOR,
            "guest_001", ActorRole.GUEST,
            "mock_001", ActorRole.MOCK_SYSTEM);

    public Optional<Actor> find(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            return Optional.empty();
        }
        ActorRole role = FIXTURE.get(actorId);
        return role == null ? Optional.empty() : Optional.of(new Actor(actorId, role));
    }
}
