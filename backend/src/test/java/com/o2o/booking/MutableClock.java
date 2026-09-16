package com.o2o.booking;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 테스트가 돌리는 시계. 설계 근거: testing.md T5(시각이 결과를 바꾸는 테스트는 Clock을 고정해
 * 끼운다), 11 상태 전이와 시간 경계(만료 판정은 공통 서버 Clock).
 *
 * 앞 묶음의 고정 시계(Clock.fixed)로는 만료 시각을 지난 뒤를 볼 수 없다. 2차의 만료와 취소 날짜
 * 조건은 같은 예약을 두 시각에서 봐야 한다. 그래서 값을 바꿀 수 있는 시계를 @Primary Clock으로
 * 끼운다. 앱은 UTC 시계 하나를 공유하므로(shared ClockConfiguration) 여기서 바꾸면 예약과 결제와
 * 재고가 같은 지금을 본다. 테스트마다 reset으로 되돌린다.
 */
public class MutableClock extends Clock {

    private final Instant origin;
    private final AtomicReference<Instant> now;

    public MutableClock(Instant origin) {
        this.origin = origin;
        this.now = new AtomicReference<>(origin);
    }

    public void set(Instant instant) {
        now.set(instant);
    }

    public void advance(Duration duration) {
        now.updateAndGet((current) -> current.plus(duration));
    }

    public void reset() {
        now.set(origin);
    }

    public Instant origin() {
        return origin;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now.get();
    }
}
