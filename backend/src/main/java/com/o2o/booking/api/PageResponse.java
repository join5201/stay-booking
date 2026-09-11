package com.o2o.booking.api;

import java.util.List;
import java.util.function.Function;

import com.o2o.shared.PageResult;

/**
 * 응답 모델 Page(T). 11 공통 목록 규칙의 items, page, size, totalElements, totalPages.
 * 카탈로그의 PageResponse와 같은 모양이다. 컨텍스트 사이 api 층이 서로를 가리키지 않아
 * 여기 한 번 더 있다. 두 번째 사본이라 layers.md 3-2의 shared 후보이고 옮기는 일은 카탈로그
 * api를 같이 고쳐야 해서 이 묶음 밖이다(계약 3절).
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements,
                              int totalPages) {

    public static <D, R> PageResponse<R> from(PageResult<D> result, Function<D, R> mapper) {
        return new PageResponse<>(result.items().stream().map(mapper).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
