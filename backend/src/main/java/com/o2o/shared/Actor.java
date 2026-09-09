package com.o2o.shared;

/**
 * 요청을 보낸 개발 행위자. 설계 근거: 11 인증과 접근 제어, P07.
 *
 * 서버가 X-Dev-Actor-Id를 역할에 매핑한다. 요청 body의 guestId나 hostId나 role로
 * 권한을 정하지 않는다는 같은 절의 규칙이 이 타입의 존재 이유다.
 */
public record Actor(String id, ActorRole role) {

    public HostId asHostId() {
        return HostId.of(id);
    }
}
