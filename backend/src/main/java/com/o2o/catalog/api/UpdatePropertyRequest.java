package com.o2o.catalog.api;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * CAT-02 요청 body. 설계 근거: 11 숙소 CAT-02의 필드표.
 *
 * version만 필수이고 나머지는 선택이다. 생략하면 기존 값을 유지한다고 그 표가 적는다.
 * 그래서 여기서는 NotBlank를 쓰지 않는다. 생략(null)과 빈 문자열이 다른 뜻이다.
 *
 * 생략과 명시적 null은 다르다(R1 평가 A-03). 생략은 유지이고 명시적 null은 허용하지 않은 null이라
 * 400이다(11 공통 요청과 응답 규칙). 그 둘을 가르려고 record 대신 setter를 가진 클래스다.
 * 이유는 RegisterPropertyRequest 머리와 같다. 문자열 셋은 공백만 있는 값도 400이다. name은
 * strip한 뒤 1~100자를 보고 strip한 값을 저장한다(B-03). regionCode와 address는 strip 규칙이
 * 필드표에 없어 공백만 있는지만 본다.
 *
 * 그 표가 version 외 변경 필드가 한 개 이상 필요하다고도 적는다. 그것을 아래 검사가 본다.
 * 형식 검증이므로 컨트롤러 층에 있는 것이 맞다(06-4 1-4). 8-2절 C5가 이 검사를 검사한다.
 */
public class UpdatePropertyRequest {

    private static final String NOT_BLANK = "(?s).*\\S.*";

    @NotNull
    @Min(0)
    private Long version;

    @Size(min = 1, max = 100)
    private String name;

    @Size(min = 1, max = 32)
    @Pattern(regexp = NOT_BLANK, message = "공백만 있는 값은 받지 않는다")
    private String regionCode;

    @Size(min = 1, max = 300)
    @Pattern(regexp = NOT_BLANK, message = "공백만 있는 값은 받지 않는다")
    private String address;

    @Size(max = 2000)
    private String description;

    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setName(String name) {
        this.name = RegisterPropertyRequest.strip(name);
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setAddress(String address) {
        this.address = address;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setDescription(String description) {
        this.description = description;
    }

    @AssertTrue(message = "version 외에 바꿀 필드가 한 개 이상 필요하다")
    public boolean isAnyFieldPresent() {
        return name != null || regionCode != null || address != null || description != null;
    }

    public Long version() {
        return version;
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

    public String description() {
        return description;
    }
}
