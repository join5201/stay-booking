package com.o2o.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 주소 값 객체. 설계 근거: 06-2 1절 Property 내부 요소, 06-2 6절 Property CRC의 협력자.
 *
 * 형식 검증을 여기서 하지 않는다. 06-4 1-2 registerProperty의 Pre가 없음이고 괄호에
 * 이름과 주소와 지역의 형식 완비는 컨트롤러 검증이라고 적는다. 06-4 1-4 검증 책임 위치
 * 표도 형식 검증을 컨트롤러에 둔다. 여기서 또 검사하면 책임이 두 곳으로 갈라진다.
 *
 * record가 아닌 이유는 JPA 임베더블이 인자 없는 생성자를 요구하기 때문이다.
 */
@Embeddable
public class Address {

    @Column(name = "address", nullable = false, length = 300)
    private String value;

    protected Address() {
    }

    private Address(String value) {
        this.value = value;
    }

    public static Address of(String value) {
        return new Address(value);
    }

    public String value() {
        return value;
    }
}
