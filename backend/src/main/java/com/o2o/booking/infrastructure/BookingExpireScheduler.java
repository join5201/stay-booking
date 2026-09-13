package com.o2o.booking.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.o2o.booking.application.ExpireDueBookings;

/**
 * T1 TTL 만료 스케줄러. 설계 근거: 11 P01(TTL 만료는 서버가 주기적으로 스캔), 08-3 결정 11의
 * 11-4(주기와 배치 크기는 설정값), 2차 계약 7절 D-3 가(키 셋, 켜고 끄기), 2절 T1 표.
 *
 * 주기마다 ExpireDueBookings.runOnce를 한 번 부른다. 판단과 트랜잭션은 전부 앱 서비스에 있고
 * 여기는 시계 노릇만 한다. fixedDelay라 앞 바퀴가 끝난 뒤 주기를 센다. 한 바퀴가 길어져도 겹치지
 * 않는다. 주기 기본 PT1S는 키 o2o.booking.expire-scan-interval로 바꾼다(ISO 8601 기간 표기).
 *
 * 빈 등록과 켜고 끄기는 BookingExpireSchedulerConfiguration이 한다. 이 클래스에 @Component가 없는
 * 이유다. 테스트 설정은 끄고 T1 테스트가 runOnce를 직접 부른다(D-3 가).
 */
public class BookingExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpireScheduler.class);

    private final ExpireDueBookings expireDueBookings;

    public BookingExpireScheduler(ExpireDueBookings expireDueBookings) {
        this.expireDueBookings = expireDueBookings;
    }

    // 건별 실패는 runOnce가 격리한다. 여기서 잡는 것은 due 목록 조회 같은 바퀴 전체의 실패다
    @Scheduled(fixedDelayString = "${o2o.booking.expire-scan-interval:PT1S}")
    public void scan() {
        try {
            expireDueBookings.runOnce();
        } catch (RuntimeException e) {
            log.error("TTL 만료 스캔 실패. 다음 주기에 다시 돈다", e);
        }
    }
}
