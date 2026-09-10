package com.o2o.inventory.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.o2o.inventory.domain.DuplicateInventoryException;
import com.o2o.inventory.domain.DuplicateRateException;
import com.o2o.inventory.domain.InvalidRateException;
import com.o2o.inventory.domain.InvalidStayPeriodException;
import com.o2o.inventory.domain.InventoryBelowOccupiedException;
import com.o2o.inventory.domain.InventoryCountBelowZeroException;
import com.o2o.inventory.domain.InventoryNotFoundException;
import com.o2o.inventory.domain.PastStayDateException;
import com.o2o.inventory.domain.RateNotFoundException;
import com.o2o.shared.ErrorResponse;

/**
 * 재고와 요금 고유 예외를 11 에러 응답 표의 코드로 바꾼다.
 *
 * catalog/api의 핸들러와 갈라 둔 이유가 같다. shared가 두 컨텍스트의 예외를 알면 공유 커널이
 * 컨텍스트에 묶인다. 컨텍스트마다 자기 핸들러를 갖고 shared는 공통 규칙만 갖는다.
 *
 * VersionConflictException은 여기 없다. 11 공통 요청과 응답 규칙이 정한 것이라 shared의
 * 핸들러가 이미 409 VERSION_CONFLICT로 낸다. 같은 코드를 두 번 적으면 둘이 갈라진다.
 * RoomTypeNotFoundException도 여기 없다. catalog의 예외라 그쪽 핸들러가 404를 낸다.
 * 재고 API의 남의 자원 404가 그 경로로 나간다.
 */
@RestControllerAdvice
public class InventoryExceptionHandler {

    /**
     * 409 RESOURCE_ALREADY_EXISTS. 11 INV-01과 INV-02와 RATE-01의 오류 표가 재고와 요금
     * 중복 등록을 한 코드로 묶는다. 그래서 예외 둘이 한 응답으로 모인다.
     */
    @ExceptionHandler({DuplicateInventoryException.class, DuplicateRateException.class})
    public ResponseEntity<ErrorResponse> handleDuplicate(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("RESOURCE_ALREADY_EXISTS",
                        "같은 객실 타입과 날짜에 이미 등록돼 있습니다."));
    }

    /**
     * 409 INVENTORY_BELOW_COMMITTED. 11 INV-03의 오류 표다. 불변식 I1 위반이고 검증 항목은 T04다.
     * 400이 아니라 409인 것이 요점이다. 요청 값 자체는 올바르고 지금 재고 상태와 충돌한 것이다.
     */
    @ExceptionHandler(InventoryBelowOccupiedException.class)
    public ResponseEntity<ErrorResponse> handleBelowOccupied(InventoryBelowOccupiedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("INVENTORY_BELOW_COMMITTED",
                        "선점과 판매 수의 합보다 적게 수정할 수 없습니다."));
    }

    /**
     * 404 RESOURCE_NOT_FOUND. 없는 날짜를 수정할 때다. 11 인증과 접근 제어의 자원 없음이다.
     */
    @ExceptionHandler({InventoryNotFoundException.class, RateNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("RESOURCE_NOT_FOUND", "자원을 찾을 수 없습니다."));
    }

    /**
     * 400 INVALID_DATE_RANGE. 11 에러 응답 표가 날짜 형식과 순서와 박수 제한을 한 줄로 묶는다.
     * 형식은 이 층의 InvalidDateFormatException, 순서와 366일 상한은 InvalidStayPeriod,
     * 과거 날짜는 PastStayDate다. 셋이 같은 코드로 나가는 것이 그 줄을 따른 것이다.
     */
    @ExceptionHandler({InvalidDateFormatException.class, InvalidStayPeriodException.class,
            PastStayDateException.class})
    public ResponseEntity<ErrorResponse> handleDateRange(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_DATE_RANGE", "날짜 값이 올바르지 않습니다."));
    }

    /**
     * 400 INVALID_REQUEST. 불변식 I2와 I1a 위반이 여기까지 오면 이 층의 검증이 샌 것이다.
     * 11 에러 응답 표에 요금 하한과 수량 하한의 전용 코드가 없어 범위 오류와 같은 코드로 낸다.
     */
    @ExceptionHandler({InvalidRateException.class, InventoryCountBelowZeroException.class})
    public ResponseEntity<ErrorResponse> handleInvalidValue(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_REQUEST", "요청 값이 올바르지 않습니다."));
    }
}
