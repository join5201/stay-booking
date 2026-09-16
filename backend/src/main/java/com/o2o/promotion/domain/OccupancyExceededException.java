package com.o2o.promotion.domain;

/**
 * 요청 인원이 객실 최대 인원을 넘는다. 설계 근거: 06-4 1-2 예약 계약표 RequestBooking의
 * 인원 검증(A5)과 위반 시 예외 OccupancyExceeded. 11 PROMO-05와 SEARCH-03의 409
 * OCCUPANCY_EXCEEDED. 계약 8-1절 V12.
 *
 * 이 패키지에 있는 이유. 검사 자체는 PricingService가 하지 않는다. 계약의 접점이 인원 검사를
 * 호출자 몫으로 뒀다. 그래도 예외의 집이 여기인 것은 PROMO-05와 SEARCH-03이 같은 예외를
 * 던지고 그것을 프로모션 api의 핸들러 하나가 받기 때문이다. 검색은 컨텍스트가 아니라
 * 읽기 모델이라 자기 예외를 갖지 않는다(05-2).
 */
public class OccupancyExceededException extends RuntimeException {

    public OccupancyExceededException(int maxOccupancy, int guestCount) {
        super("객실 최대 인원을 넘었다. 최대 " + maxOccupancy + ", 요청 " + guestCount);
    }
}
