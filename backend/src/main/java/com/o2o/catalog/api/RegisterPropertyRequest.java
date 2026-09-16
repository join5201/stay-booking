package com.o2o.catalog.api;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CAT-01 요청 body. 설계 근거: 11 숙소 CAT-01의 필드표.
 *
 * 제약값이 전부 그 표에서 왔다. 이름은 공백 제거 후 1~100, 지역 코드 1~32, 주소 1~300, 설명 최대
 * 2000. 검증이 여기 있는 근거는 06-4 1-4다. 형식 검증은 컨트롤러가 한다.
 *
 * hostId가 없는 것이 핵심이다. 11 인증과 접근 제어가 body의 hostId로 소유자를 정하지
 * 말라고 적는다. 받을 칸이 없으면 실수로 쓸 수도 없다. 8-1절 C6이 이것을 검사한다.
 *
 * record가 아니라 setter를 가진 클래스인 이유(R1 평가 A-03). 11 공통 규칙은 생략과 명시적 null을
 * 가른다. description을 생략하면 빈 문자열이고 null을 보내면 허용하지 않은 null이라 400이다.
 * Jackson 3.1.5는 record의 생성자 속성에 생략도 null로 넘겨서 JsonSetter의 nulls = FAIL이 생략까지
 * 거절한다(2026-09-14 실측). setter는 그 필드가 body에 있을 때만 불리므로 둘을 가를 수 있다.
 *
 * name은 setter에서 strip한다. 필드표가 공백 제거 후 1~100자라 검증도 저장도 strip한 값이다(B-03).
 * 접근자 이름을 record와 같은 name() 꼴로 둔 것은 부르는 쪽을 바꾸지 않기 위해서다.
 */
public class RegisterPropertyRequest {

    @NotBlank
    @Size(min = 1, max = 100)
    private String name;

    @NotBlank
    @Size(min = 1, max = 32)
    private String regionCode;

    @NotBlank
    @Size(min = 1, max = 300)
    private String address;

    @Size(max = 2000)
    private String description;

    public void setName(String name) {
        this.name = strip(name);
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /** 생략은 빈 문자열, 명시적 null은 400. 11 공통 요청과 응답 규칙의 허용하지 않은 null */
    @JsonSetter(nulls = Nulls.FAIL)
    public void setDescription(String description) {
        this.description = description;
    }

    public String name() {
        return name;
    }

    public String regionCode() {
        return regionCode;
    }

    public String address() {
        return address;
    }

    /** 11 필드표가 description 생략 시 빈 문자열로 적는다. null을 도메인에 넘기지 않는다 */
    public String descriptionOrEmpty() {
        return description == null ? "" : description;
    }

    static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
