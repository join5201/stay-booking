package com.o2o.catalog.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.o2o.shared.PageQuery;
import com.o2o.shared.PageResult;

/**
 * 도메인의 쪽 나눔 타입과 스프링 데이터의 것을 오간다.
 * 설계 근거: 11 공통 목록과 날짜 범위.
 *
 * 그 절이 기본 정렬을 id 오름차순으로 적어서 정렬을 여기 한 곳에 고정한다. 어댑터마다
 * 적으면 한 곳을 고칠 때 나머지가 남는다.
 *
 * 이 변환이 infrastructure에 있는 이유는 스프링 데이터 타입이 여기서만 보여야 하기 때문이다.
 * domain은 shared의 PageQuery와 PageResult만 안다.
 */
final class SpringPage {

    private SpringPage() {
    }

    static Pageable toPageable(PageQuery query) {
        return PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.ASC, "id"));
    }

    static <T> PageResult<T> toResult(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
