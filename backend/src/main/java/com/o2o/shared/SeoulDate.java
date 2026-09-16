package com.o2o.shared;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 숙박과 캠페인 날짜의 오늘. 설계 근거: 11 공통 요청과 응답 규칙 35행.
 *
 * 그 줄이 날짜와 시각을 가른다. 날짜(숙박, 캠페인)는 Asia/Seoul 기준이고 시각은 UTC다.
 * 시각이 UTC라고 해서 날짜까지 UTC로 자르면 한국 새벽 아홉 시간 동안 서울 기준 어제가
 * 오늘로 통한다. R1 평가 A-01과 B-01이 짚은 자리다. Instant는 그대로 UTC Clock에서 받고
 * 날짜로 자를 때만 이 지역을 쓴다.
 *
 * shared에 두는 근거는 06-1 4절과 backend/CLAUDE.md 3-2다. 재고와 요금이 첫 사용처이고
 * 프로모션 캠페인 기간이 두 번째다. 오늘을 계산하는 자리를 여기 하나로 두어야 컨텍스트마다
 * 다른 지역을 쓰는 사고가 안 난다.
 */
public final class SeoulDate {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private SeoulDate() {
    }

    /** Clock이 주는 지금 이 순간을 서울 날짜로 자른다. Clock의 zone은 쓰지 않는다 */
    public static LocalDate today(Clock clock) {
        return LocalDate.ofInstant(Instant.now(clock), ZONE);
    }
}
