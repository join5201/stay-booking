package com.o2o.catalog.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * CAT-06 요청 body. 설계 근거: 11 객실 타입 CAT-06의 필드표.
 *
 * maxOccupancy가 Integer인 이유는 누락과 0을 갈라야 하기 때문이다. int면 누락이 0으로
 * 들어와 필수 누락이 범위 위반으로 둔갑한다.
 *
 * 상한 100은 11 공통 요청과 응답 규칙에서 왔다. 하한 1은 같은 줄이자 I14와도 겹치는데,
 * 여기 검증은 형식이고 I14는 도메인이 따로 지킨다. 06-4 1-4가 그 둘을 갈라 둔다.
 *
 * propertyId가 없는 것도 의도다. 11 CAT-06 처리 규칙이 propertyId는 경로에서 가져온다고 적는다.
 */
public record RegisterRoomTypeRequest(
        @NotBlank @Size(min = 1, max = 100) String name,
        @NotNull @Min(1) @Max(100) Integer maxOccupancy,
        @Size(max = 2000) String description) {

    public String descriptionOrEmpty() {
        return description == null ? "" : description;
    }
}
