package com.o2o.catalog.domain;

/**
 * I14 위반. 설계 근거: 06-4 1-2 카탈로그 계약표 registerRoomType의 위반 시 예외 InvalidOccupancy.
 */
public class InvalidOccupancyException extends RuntimeException {

    public InvalidOccupancyException(int maxOccupancy) {
        super("최대 인원은 0보다 커야 한다: " + maxOccupancy);
    }
}
