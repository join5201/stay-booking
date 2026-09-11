package com.o2o.promotion.domain;

/**
 * 기간 순서 위반. 설계 근거: 06-4 1-2 프로모션 계약표 create의 위반 시 예외 InvalidPeriod,
 * 06-4 1-1 I8 캠페인 순서, 11 PROMO-01 필드표의 campaignEndDate와 stayEndDate 행.
 *
 * 한 예외가 셋을 덮는다. I8(캠페인 시작일이 종료일보다 늦다), 숙박 기간의 같은 순서 위반,
 * 그리고 11 명세의 끝 날짜가 시작보다 뒤여야 한다는 제약이다. 11 에러 응답 표가 날짜 순서
 * 오류를 400 INVALID_DATE_RANGE 한 줄로 묶으므로 예외도 하나면 된다. 어느 규칙인지는
 * 메시지가 말한다.
 */
public class InvalidPeriodException extends RuntimeException {

    public InvalidPeriodException(String reason) {
        super(reason);
    }
}
