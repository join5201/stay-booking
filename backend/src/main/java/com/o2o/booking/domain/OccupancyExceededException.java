package com.o2o.booking.domain;

/**
 * A5 위반. 설계 근거: 06-2 3-2 A5(userCount <= maxOccupancy. 신청 시점 검증, 예약 앱 서비스
 * 선행조건), 06-1 R2(카탈로그가 예약에 maxOccupancy를 준다). 11 BOOK-01의 409 OCCUPANCY_EXCEEDED.
 */
public class OccupancyExceededException extends RuntimeException {

    public OccupancyExceededException(int userCount, int maxOccupancy) {
        super("인원 " + userCount + "이 객실 최대 인원 " + maxOccupancy + "을 넘는다");
    }
}
