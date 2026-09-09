package com.o2o.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CAT-01 요청 body. 설계 근거: 11 숙소 CAT-01의 필드표.
 *
 * 제약값이 전부 그 표에서 왔다. 이름 1~100, 지역 코드 1~32, 주소 1~300, 설명 최대 2000.
 * 검증이 여기 있는 근거는 06-4 1-4다. 형식 검증은 컨트롤러가 한다.
 *
 * hostId가 없는 것이 핵심이다. 11 인증과 접근 제어가 body의 hostId로 소유자를 정하지
 * 말라고 적는다. 받을 칸이 없으면 실수로 쓸 수도 없다. 8-1절 C6이 이것을 검사한다.
 */
public record RegisterPropertyRequest(
        @NotBlank @Size(min = 1, max = 100) String name,
        @NotBlank @Size(min = 1, max = 32) String regionCode,
        @NotBlank @Size(min = 1, max = 300) String address,
        @Size(max = 2000) String description) {

    /** 11 필드표가 description 생략 시 빈 문자열로 적는다. null을 도메인에 넘기지 않는다 */
    public String descriptionOrEmpty() {
        return description == null ? "" : description;
    }
}
