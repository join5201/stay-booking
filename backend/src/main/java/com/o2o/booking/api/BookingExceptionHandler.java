package com.o2o.booking.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.o2o.booking.domain.BookingNotFoundException;
import com.o2o.booking.domain.IdempotencyKeyReusedException;
import com.o2o.booking.domain.InvalidBookingPeriodException;
import com.o2o.booking.domain.InvalidIdempotencyKeyException;
import com.o2o.booking.domain.InvalidPriceSnapshotException;
import com.o2o.booking.domain.OccupancyExceededException;
import com.o2o.booking.domain.PriceChangedException;
import com.o2o.booking.domain.RateNotConfiguredException;
import com.o2o.booking.domain.RequestInProgressException;
import com.o2o.inventory.domain.InventoryNotOpenedException;
import com.o2o.inventory.domain.InventoryShortageException;
import com.o2o.shared.ErrorResponse;

/**
 * 예약 예외를 11 에러 응답 표의 코드로 바꾼다. 설계 근거: 11 BOOK-01 에러 표, 에러 응답 표
 * (112행부터), 공통 헤더 표의 Retry-After. 재고 컨텍스트의 예외 둘(가용 부족, 미개설)이 예약
 * 경로에서만 HTTP로 나가므로 여기서 받는다. 없는 객실 타입(RoomTypeNotFound)은 카탈로그의
 * 처리기가 이미 404로 낸다. 처리기는 전역이라 컨트롤러가 어느 컨텍스트든 걸린다.
 */
@RestControllerAdvice
public class BookingExceptionHandler {

    /** 400 IDEMPOTENCY_KEY_REQUIRED. 누락과 형식 오류를 한 코드로 묶는다(에러 표 18행) */
    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyKey(InvalidIdempotencyKeyException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("IDEMPOTENCY_KEY_REQUIRED",
                        "Idempotency-Key 헤더가 없거나 형식이 올바르지 않습니다."));
    }

    /**
     * 400 INVALID_DATE_RANGE. 날짜 형식은 이 층, 순서와 30박과 과거는 StayPeriod다. 11 에러
     * 응답 표가 셋을 한 줄로 묶는다. 재고의 같은 자리와 같은 처리다.
     */
    @ExceptionHandler({InvalidDateFormatException.class, InvalidBookingPeriodException.class})
    public ResponseEntity<ErrorResponse> handleDateRange(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_DATE_RANGE", "날짜 값이 올바르지 않습니다."));
    }

    /** 409 IDEMPOTENCY_KEY_REUSED. 같은 키에 다른 body(멱등 규칙 2, T11) */
    @ExceptionHandler(IdempotencyKeyReusedException.class)
    public ResponseEntity<ErrorResponse> handleReused(IdempotencyKeyReusedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("IDEMPOTENCY_KEY_REUSED",
                        "같은 멱등키로 다른 요청을 보낼 수 없습니다."));
    }

    /** 409 REQUEST_IN_PROGRESS와 Retry-After: 1(멱등 규칙 4, 공통 헤더 72행) */
    @ExceptionHandler(RequestInProgressException.class)
    public ResponseEntity<ErrorResponse> handleInProgress(RequestInProgressException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header("Retry-After", "1")
                .body(ErrorResponse.of("REQUEST_IN_PROGRESS", "같은 요청이 처리 중입니다."));
    }

    /** 409 INVENTORY_UNAVAILABLE. 하나 이상의 숙박 날짜에 선점 가능한 재고 없음(T08, T09) */
    @ExceptionHandler(InventoryShortageException.class)
    public ResponseEntity<ErrorResponse> handleShortage(InventoryShortageException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("INVENTORY_UNAVAILABLE",
                        "선점 가능한 재고가 없는 날짜가 있습니다."));
    }

    /** 409 INVENTORY_NOT_CONFIGURED. 하나 이상의 숙박 날짜에 재고 레코드 없음(T07) */
    @ExceptionHandler(InventoryNotOpenedException.class)
    public ResponseEntity<ErrorResponse> handleNotOpened(InventoryNotOpenedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("INVENTORY_NOT_CONFIGURED",
                        "재고가 등록되지 않은 날짜가 있습니다."));
    }

    /** 409 RATE_NOT_CONFIGURED. 하나 이상의 숙박 날짜에 요금 없음(T07, A6) */
    @ExceptionHandler(RateNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleRateNotConfigured(RateNotConfiguredException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("RATE_NOT_CONFIGURED", "요금이 등록되지 않은 날짜가 있습니다."));
    }

    /** 409 OCCUPANCY_EXCEEDED. 객실 최대 인원 초과(A5) */
    @ExceptionHandler(OccupancyExceededException.class)
    public ResponseEntity<ErrorResponse> handleOccupancy(OccupancyExceededException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("OCCUPANCY_EXCEEDED", "객실 최대 인원을 넘었습니다."));
    }

    /** 409 PRICE_CHANGED. 예상 총액과 서버 금액 불일치(T12, P06) */
    @ExceptionHandler(PriceChangedException.class)
    public ResponseEntity<ErrorResponse> handlePriceChanged(PriceChangedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("PRICE_CHANGED",
                        "금액이 바뀌었습니다. 다시 조회한 금액으로 요청하세요."));
    }

    /** 404 RESOURCE_NOT_FOUND. 없는 예약과 남의 예약이 같은 응답이다(T02) */
    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(BookingNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("RESOURCE_NOT_FOUND", "자원을 찾을 수 없습니다."));
    }

    /**
     * 500 INTERNAL_ERROR. 스냅샷 위반은 가격 어댑터의 결함이라 요청자의 잘못이 아니다.
     * 4xx로 내면 클라이언트가 고칠 것이 있는 것처럼 보인다. 11 에러 응답 표의 500 행이다.
     */
    @ExceptionHandler(InvalidPriceSnapshotException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSnapshot(InvalidPriceSnapshotException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "가격 계산 결과가 올바르지 않습니다."));
    }
}
