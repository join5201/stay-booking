package com.o2o.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 지역 값 객체. 설계 근거: 06-2 1절 Property 내부 요소, 06-2 6절 Property CRC의 협력자.
 *
 * 등록된 지역 코드인지 확인하는 책임은 여기 없다. 11 CAT-01 처리 규칙이 그 확인을 적지만
 * 그것은 형식이 아니라 존재 확인이고, 지역 fixture는 초기 세팅에서 준비한다고 같은 줄이
 * 적는다. 이번 바퀴에 지역 fixture가 없으므로 그 확인은 아직 어느 층에도 두지 않는다.
 * 두 번째 바퀴의 선행 항목이다.
 */
@Embeddable
public class Region {

    @Column(name = "region_code", nullable = false, length = 32)
    private String code;

    protected Region() {
    }

    private Region(String code) {
        this.code = code;
    }

    public static Region of(String code) {
        return new Region(code);
    }

    public String code() {
        return code;
    }
}
