package com.o2o.booking.infrastructure;

import org.junit.jupiter.api.Test;

import com.o2o.booking.application.ExpireDueBookings;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 관측 3-2(이슈 228). 만료 스캔 한 바퀴가 예외로 끝나면 실패 지표가 오른다. 장애 실험 C1에서 예약
 * 테이블 쓰기를 막으면 스캔도 같이 막힌다. 그 구간을 이 지표로 본다.
 *
 * 테스트 설정은 스케줄러를 끄므로(o2o.booking.expire-scheduler.enabled=false) 컨텍스트 없이 직접 만든다.
 */
class BookingExpireSchedulerMetricsTest {

    @Test
    void 스캔이_예외로_끝나면_실패_지표가_하나_오르고_예외는_밖으로_나가지_않는다() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BookingMetrics metrics = new BookingMetrics(registry);
        ExpireDueBookings failing = new ExpireDueBookings(null, null, null, 1) {
            @Override
            public Summary runOnce() {
                throw new IllegalStateException("DB 권한 회수를 흉내 낸다");
            }
        };

        new BookingExpireScheduler(failing, metrics).scan();

        assertEquals(1.0, registry.get("o2o.booking.expire.scan.failures").counter().count());
    }
}
