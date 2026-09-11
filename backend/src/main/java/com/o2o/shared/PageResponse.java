package com.o2o.shared;

import java.util.List;
import java.util.function.Function;

/**
 * 목록 응답. 설계 근거: 11 공통 목록과 날짜 범위의 Page 구조 다섯 칸.
 *
 * catalog/api의 PageResponse와 같은 모양이다. 프로모션 목록이 두 번째 사용처라 shared에
 * 둔다(backend/CLAUDE.md 3-2). PageResult와 따로 두는 이유는 그쪽 주석과 같다. 11이 응답
 * 모양을 정하고 PageResult는 도메인 경계의 값이다.
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements,
                              int totalPages) {

    public static <D, R> PageResponse<R> from(PageResult<D> result, Function<D, R> mapper) {
        return new PageResponse<>(result.items().stream().map(mapper).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
