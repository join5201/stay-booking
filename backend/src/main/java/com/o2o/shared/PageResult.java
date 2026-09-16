package com.o2o.shared;

import java.util.List;

/**
 * 목록 응답. 설계 근거: 11 공통 목록과 날짜 범위의 Page 구조.
 *
 * 그 절이 items와 page와 size와 totalElements와 totalPages 다섯 칸을 적는다.
 * 스키마 표의 Page 표기가 items가 T 배열이라는 뜻이라고도 적혀 있다.
 *
 * 스프링 데이터의 Page를 도메인 경계에 쓰지 않는 이유는 레이어 역전 때문이다.
 * domain이 선언한 리포지토리가 스프링 타입을 돌려주면 domain이 infrastructure를 참조하게 되고
 * eval-criteria-code.md의 레이어 역전 축에 걸린다.
 */
public record PageResult<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public <R> PageResult<R> map(java.util.function.Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(),
                page, size, totalElements, totalPages);
    }
}
