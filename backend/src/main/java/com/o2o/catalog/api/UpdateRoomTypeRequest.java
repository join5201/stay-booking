package com.o2o.catalog.api;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * CAT-07 요청 body. 설계 근거: 11 객실 타입 CAT-07의 필드표.
 *
 * UpdatePropertyRequest와 같은 규칙이다. version만 필수이고 하나 이상은 바꿔야 한다. 생략은
 * 유지, 명시적 null은 400, name은 strip한 뒤 1~100자(R1 평가 A-03, B-03).
 * propertyId가 없는 것이 의도다. 같은 절의 처리 규칙이 propertyId를 바꾸지 않는다고 적는다.
 */
public class UpdateRoomTypeRequest {

    @NotNull
    @Min(0)
    private Long version;

    @Size(min = 1, max = 100)
    private String name;

    @Min(1)
    @Max(100)
    private Integer maxOccupancy;

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
    public void setMaxOccupancy(Integer maxOccupancy) {
        this.maxOccupancy = maxOccupancy;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setDescription(String description) {
        this.description = description;
    }

    @AssertTrue(message = "version 외에 바꿀 필드가 한 개 이상 필요하다")
    public boolean isAnyFieldPresent() {
        return name != null || maxOccupancy != null || description != null;
    }

    public Long version() {
        return version;
    }

    public String name() {
        return name;
    }

    public Integer maxOccupancy() {
        return maxOccupancy;
    }

    public String description() {
        return description;
    }
}
