package com.o2o.catalog.api;

import java.util.List;

import com.o2o.shared.PageResult;

/**
 * 목록 응답. 설계 근거: 11 공통 목록과 날짜 범위의 구조.
 *
 * 다섯 칸이 그 절의 예시 JSON 그대로다. items와 page와 size와 totalElements와 totalPages다.
 * shared의 PageResult와 모양이 같은데도 따로 두는 이유는 PropertyResponse를 따로 둔 것과 같다.
 * 11이 응답 모양을 정하고 shared는 도메인 경계의 값을 정한다. 둘을 한 타입으로 묶으면
 * 한쪽이 바뀔 때 다른 쪽이 끌려간다.
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <D, R> PageResponse<R> from(PageResult<D> result,
                                              java.util.function.Function<D, R> mapper) {
        return new PageResponse<>(result.items().stream().map(mapper).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
