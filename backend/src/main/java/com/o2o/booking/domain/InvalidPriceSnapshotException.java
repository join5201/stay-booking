package com.o2o.booking.domain;

/**
 * 스냅샷 위반. 설계 근거: 06-4 1-2 Booking 생성자의 위반 시 예외 InvalidSnapshot. I10, I11,
 * I12, I15와 날짜별 가격 행의 값 범위가 여기로 온다. 가격 포트 어댑터가 맞게 만들면 나지
 * 않는 예외라 HTTP 코드 표에 없다. 나면 어댑터의 결함이다.
 */
public class InvalidPriceSnapshotException extends RuntimeException {

    public InvalidPriceSnapshotException(String reason) {
        super("가격 스냅샷이 틀리다. " + reason);
    }
}
