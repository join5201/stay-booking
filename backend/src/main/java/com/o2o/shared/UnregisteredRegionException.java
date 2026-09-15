package com.o2o.shared;

/**
 * 등록되지 않은 지역 코드. 400 INVALID_REQUEST. 설계 근거: 11 CAT-01 처리 규칙, CAT-01과
 * CAT-02와 CAT-04 필드표의 등록된 지역 코드. R1 평가 A-02와 B-02.
 *
 * 형식(1자 이상 32자 이하)이 맞아도 RegionRegistry에 없으면 여기로 온다. shared에 두는 이유는
 * RegionRegistry와 같다.
 */
public class UnregisteredRegionException extends RuntimeException {

    private final String code;

    public UnregisteredRegionException(String code) {
        super("등록되지 않은 지역 코드다: " + code);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
