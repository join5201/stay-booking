package com.o2o.promotion.domain;

import java.time.LocalDate;
import java.util.List;

import com.o2o.shared.RoomTypeId;

/**
 * 숙박 날짜 중 요금이 없는 날이 있다. 설계 근거: 06-4 1-2 예약 계약표 PricingService 행의
 * Pre(숙박 기간 전 날짜에 요금 존재, A6)와 위반 시 예외 RateNotFound. 11 PROMO-05와 SEARCH-03의
 * 409 RATE_NOT_CONFIGURED. 계약 8-1절 V11.
 *
 * 06-4의 이름은 RateNotFound인데 여기서는 명세의 오류 코드를 따라 RateNotConfigured로 적는다.
 * 재고 묶음에 이미 RateNotFoundException이 있고 그것은 RATE-04의 404다. 같은 이름을 두
 * 컨텍스트에 두면 무엇이 409이고 무엇이 404인지 헷갈린다.
 *
 * 없는 날짜 목록을 갖는다. 계약의 PricingService 접점이 그렇게 정했고, 11 에러 응답의
 * details가 어느 날짜인지를 실을 자리다.
 */
public class RateNotConfiguredException extends RuntimeException {

    private final List<LocalDate> missingDates;

    public RateNotConfiguredException(RoomTypeId roomTypeId, List<LocalDate> missingDates) {
        super("요금이 없는 숙박 날짜가 있다: " + roomTypeId.value() + " " + missingDates);
        this.missingDates = List.copyOf(missingDates);
    }

    public List<LocalDate> missingDates() {
        return missingDates;
    }
}
