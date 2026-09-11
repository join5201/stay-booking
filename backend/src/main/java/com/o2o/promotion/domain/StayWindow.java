package com.o2o.promotion.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 할인 대상 숙박 기간. start 포함, end 제외. 설계 근거: 11 PROMO-01 필드표의 stayStartDate와
 * stayEndDate 행, 11 내부 처리 가격과 프로모션 절의 설정한 숙박 기간 안에 전체 숙박 구간이
 * 들어와야 한다는 규칙, 05-3 4절 캠페인 기간과 숙박 기간을 갈라 쓴다는 문장.
 *
 * Condition VO의 일부다. 06-2 6절 CRC의 Condition 책임 행은 최소 숙박일과 지역과 캠페인 기간
 * 셋만 적고 숙박 기간을 적지 않는다. 11 명세가 더한 조건이라 그 사실을 여기 적는다.
 * 없을 수 있다. 두 필드가 모두 null이면 숙박 기간 제한이 없다.
 *
 * 순서 검사는 캠페인 기간의 I8과 같은 모양이다. 시작이 종료보다 늦으면 거부한다.
 * 끝 날짜가 시작보다 뒤여야 한다는 11의 제약은 Promotion이 requireNonEmptyPeriods로 본다.
 */
public record StayWindow(LocalDate start, LocalDate end) {

    public StayWindow {
        Objects.requireNonNull(start, "숙박 기간 시작일은 null일 수 없다");
        Objects.requireNonNull(end, "숙박 기간 종료일은 null일 수 없다");
        if (start.isAfter(end)) {
            throw new InvalidPeriodException("숙박 기간 시작일이 종료일보다 늦다: " + start + " > " + end);
        }
    }

    public static StayWindow of(LocalDate start, LocalDate end) {
        return new StayWindow(start, end);
    }

    /** 전체 숙박 구간이 이 기간 안에 든다. 둘 다 반열림이라 checkOut이 end와 같아도 든다 */
    public boolean covers(StayRange stay) {
        return !stay.checkIn().isBefore(start) && !stay.checkOut().isAfter(end);
    }

    boolean isEmpty() {
        return start.equals(end);
    }
}
