package com.o2o.promotion.domain;

/**
 * PROMO-02가 숙박 기간에 요구하는 세 가지 뜻. 설계 근거: 11 PROMO-02 필드표의 stayStartDate 행과
 * 처리 규칙 첫 줄. 생략하면 유지, 두 날짜를 null로 보내면 해제, 두 날짜를 보내면 변경이다.
 *
 * null 하나로는 유지와 해제를 못 가른다. 그래서 값이 세 갈래인 타입을 둔다. 다른 필드는
 * null 해제가 없어서 null이 곧 유지이고 이 타입이 필요 없다.
 */
public sealed interface StayWindowChange {

    /** 생략. 기존 값을 유지한다 */
    record Keep() implements StayWindowChange {
    }

    /** 두 날짜를 null로 보냄. 숙박 기간 제한을 없앤다 */
    record Clear() implements StayWindowChange {
    }

    /** 두 날짜를 보냄. 그 기간으로 바꾼다 */
    record Replace(StayWindow window) implements StayWindowChange {
    }

    static StayWindowChange keep() {
        return new Keep();
    }

    static StayWindowChange clear() {
        return new Clear();
    }

    static StayWindowChange replace(StayWindow window) {
        return new Replace(window);
    }
}
