package com.o2o.catalog.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * CAT-02 요청 body. 설계 근거: 11 숙소 CAT-02의 필드표.
 *
 * version만 필수이고 나머지는 선택이다. 생략하면 기존 값을 유지한다고 그 표가 적는다.
 * 그래서 여기서는 NotBlank를 쓰지 않는다. null과 빈 문자열이 다른 뜻이다.
 *
 * 그 표가 version 외 변경 필드가 한 개 이상 필요하다고도 적는다. 그것을 아래 검사가 본다.
 * 형식 검증이므로 컨트롤러 층에 있는 것이 맞다(06-4 1-4). 8-2절 C5가 이 검사를 검사한다.
 */
public record UpdatePropertyRequest(
        @NotNull @Min(0) Long version,
        @Size(min = 1, max = 100) String name,
        @Size(min = 1, max = 32) String regionCode,
        @Size(min = 1, max = 300) String address,
        @Size(max = 2000) String description) {

    @AssertTrue(message = "version 외에 바꿀 필드가 한 개 이상 필요하다")
    public boolean isAnyFieldPresent() {
        return name != null || regionCode != null || address != null || description != null;
    }
}
