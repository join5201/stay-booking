package com.o2o.promotion.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.o2o.promotion.domain.IncompleteStayWindowException;
import com.o2o.promotion.domain.InvalidDiscountRateException;
import com.o2o.promotion.domain.InvalidPeriodException;
import com.o2o.promotion.domain.InvalidRegionCodesException;
import com.o2o.promotion.domain.InvalidStayRangeException;
import com.o2o.promotion.domain.OccupancyExceededException;
import com.o2o.promotion.domain.PromotionNotFoundException;
import com.o2o.promotion.domain.RateNotConfiguredException;
import com.o2o.shared.ApiDate;
import com.o2o.shared.ErrorResponse;
import com.o2o.shared.InvalidDateFormatException;

/**
 * 프로모션 고유 예외를 11 에러 응답 표의 코드로 바꾼다. 컨텍스트마다 자기 핸들러를 갖는
 * 이유는 InventoryExceptionHandler와 같다.
 *
 * VersionConflictException과 IllegalArgumentException(PageQuery, GuestCount, RoomTypeId 범위)은
 * shared의 핸들러가 낸다. RoomTypeNotFoundException은 catalog의 핸들러가 404를 낸다.
 *
 * shared의 InvalidDateFormatException을 여기서 받는 이유는 SharedExceptionHandler가 이번 세션에
 * 동결이기 때문이다. 검색 컨텍스트의 날짜 형식 오류도 이 핸들러가 받는다. 어드바이스는 전역이다.
 * 다음 묶음에서 shared 핸들러가 열리면 그쪽으로 옮긴다.
 */
@RestControllerAdvice
public class PromotionExceptionHandler {

    /**
     * 400 INVALID_DATE_RANGE. 11 에러 응답 표가 날짜 형식과 순서와 박수 제한을 한 줄로 묶는다.
     * 형식은 InvalidDateFormat, 캠페인과 숙박 기간 순서는 InvalidPeriod, 조회 구간의 순서와
     * 30박 상한과 오늘 이상은 InvalidStayRange다.
     */
    @ExceptionHandler({InvalidDateFormatException.class, InvalidPeriodException.class,
            InvalidStayRangeException.class})
    public ResponseEntity<ErrorResponse> handleDateRange(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_DATE_RANGE", "날짜 값이 올바르지 않습니다."));
    }

    /**
     * 400 INVALID_REQUEST. 11 에러 응답 표의 범위 오류. 할인율 범위와 지역 코드 중복과 상한,
     * 숙박 기간 두 날짜 중 하나만 온 것이 여기까지 오면 이 층의 검증이 샌 것이다.
     */
    @ExceptionHandler({InvalidDiscountRateException.class, InvalidRegionCodesException.class,
            IncompleteStayWindowException.class})
    public ResponseEntity<ErrorResponse> handleInvalidValue(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_REQUEST", "요청 값이 올바르지 않습니다."));
    }

    /** 404 RESOURCE_NOT_FOUND. 11 PROMO-02와 03의 없는 ID */
    @ExceptionHandler(PromotionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PromotionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("RESOURCE_NOT_FOUND", "자원을 찾을 수 없습니다."));
    }

    /**
     * 409 RATE_NOT_CONFIGURED. 11 PROMO-05와 SEARCH-03의 오류 표. 빠진 날짜를 details에 싣는다.
     * 11 에러 응답 절의 예시가 details의 field에 checkIn을, reason에 날짜와 사유를 적는다.
     */
    @ExceptionHandler(RateNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleRateNotConfigured(RateNotConfiguredException e) {
        List<ErrorResponse.ErrorDetail> details = e.missingDates().stream()
                .map(date -> new ErrorResponse.ErrorDetail("checkIn", ApiDate.format(date) + " 요금 없음"))
                .toList();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("RATE_NOT_CONFIGURED",
                        "하나 이상의 숙박 날짜에 요금이 없습니다.", details));
    }

    /** 409 OCCUPANCY_EXCEEDED. 11 PROMO-05와 SEARCH-03의 오류 표 */
    @ExceptionHandler(OccupancyExceededException.class)
    public ResponseEntity<ErrorResponse> handleOccupancyExceeded(OccupancyExceededException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("OCCUPANCY_EXCEEDED", "객실 최대 인원을 넘었습니다."));
    }
}
