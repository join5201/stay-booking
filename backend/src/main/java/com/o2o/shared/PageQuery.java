package com.o2o.shared;

/**
 * 목록 요청의 쪽 번호와 크기. 설계 근거: 11 공통 목록과 날짜 범위.
 *
 * 그 절이 page 기본 0, size 기본 20과 최대 100을 적고 잘못된 범위는 400이라고 적는다.
 * 값 검사가 여기 있는 이유는 이 타입이 도메인 경계로 넘어가는 값이기 때문이다. 컨트롤러의
 * 형식 검증(06-4 1-4)이 먼저 걸러 400을 내고, 여기 검사는 그 뒤에 남은 마지막 가드다.
 *
 * shared에 두는 이유는 이 규칙이 카탈로그만의 것이 아니기 때문이다. 11 공통 절이 정하고
 * 재고와 프로모션과 예약의 목록 API가 같은 규칙을 쓴다.
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 한다: " + page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size는 1 이상 " + MAX_SIZE + " 이하여야 한다: " + size);
        }
    }

    /** 생략된 값을 기본값으로 채운다. 11 공통 절의 기본 0과 20이다 */
    public static PageQuery of(Integer page, Integer size) {
        return new PageQuery(page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size);
    }
}
