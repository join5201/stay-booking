package com.o2o.promotion.domain;

/**
 * 지역 코드 목록 위반. 설계 근거: 11 PROMO-01 필드표 regionCodes 행의 최대 100개와 중복 금지.
 * 계약 8-1절 V3.
 *
 * 등록된 지역 코드인지는 여기서 보지 않는다. 카탈로그의 Region이 같은 이유로 그 확인을
 * 두지 않았다. 지역 fixture가 아직 없다.
 */
public class InvalidRegionCodesException extends RuntimeException {

    public InvalidRegionCodesException(String reason) {
        super(reason);
    }
}
