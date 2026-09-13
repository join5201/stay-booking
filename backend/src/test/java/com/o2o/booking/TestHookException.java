package com.o2o.booking;

/** 테스트 전용 훅이 내는 예외. 운영 예외와 섞이지 않게 따로 둔다(L11, L13) */
public class TestHookException extends RuntimeException {

    public TestHookException(String message) {
        super(message);
    }
}
