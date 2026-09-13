package com.o2o.booking.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.o2o.booking.domain.BookingId;
import com.o2o.booking.domain.BookingRepository;

/**
 * T1 TTL 만료의 스캔. 설계 근거: 11 P01과 상태 전이와 시간 경계(due 조건은 status HELD와 expiresAt
 * <= now, 공통 서버 Clock으로 판단), 06-4 2-2 TTL 만료 정책 카드, 08-3 결정 11의 11-4(배치 크기는
 * 설정값), 2차 계약 2절 T1 표.
 *
 * 잠금 없이 due 목록을 읽고 건마다 BookingExpirationService.expireIfDue를 부른다. 그 메서드가
 * REQUIRES_NEW라 한 건의 실패가 다른 건을 막지 않는다. 실패는 로그로 남기고 다음 주기에 다시
 * 잡힌다(그 예약은 여전히 HELD이고 due다). 여기가 트랜잭션을 열지 않는 이유는 그 REQUIRES_NEW를
 * 다른 빈의 프록시로 타기 위해서이고, 스캔 자체는 커밋된 행만 보면 된다.
 *
 * 스케줄 배선(주기, 켜고 끄기)은 인프라 몫이다(계약 7절 D-3 가). 여기는 부르면 한 바퀴 돈다.
 * 배치 크기 기본 100은 08-3 11-4의 가설이고 키 o2o.booking.expire-batch-size로 바꾼다.
 */
@Service
public class ExpireDueBookings {

    private static final Logger log = LoggerFactory.getLogger(ExpireDueBookings.class);

    /** 한 바퀴의 셈. 스케줄러 로그와 테스트가 읽는다 */
    public record Summary(int scanned, int expired, int confirmed, int skipped, int failed) {
    }

    private final BookingRepository bookingRepository;
    private final BookingExpirationService expirationService;
    private final Clock clock;
    private final int batchSize;

    public ExpireDueBookings(BookingRepository bookingRepository,
                             BookingExpirationService expirationService, Clock clock,
                             @Value("${o2o.booking.expire-batch-size:100}") int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("배치 크기는 1 이상이어야 한다: " + batchSize);
        }
        this.bookingRepository = bookingRepository;
        this.expirationService = expirationService;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    /** 한 바퀴. 지금 기준 due인 HELD를 오래된 것부터 배치 크기만큼 건별로 처리한다 */
    public Summary runOnce() {
        Instant now = Instant.now(clock);
        List<BookingId> due = bookingRepository.findDueIds(now, batchSize);
        int expired = 0;
        int confirmed = 0;
        int skipped = 0;
        int failed = 0;
        for (BookingId bookingId : due) {
            try {
                switch (expirationService.expireIfDue(bookingId)) {
                    case EXPIRED -> expired++;
                    case CONFIRMED -> confirmed++;
                    case SKIPPED -> skipped++;
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("TTL 만료 처리 실패. 다음 주기에 다시 본다. booking={}", bookingId.value(), e);
            }
        }
        Summary summary = new Summary(due.size(), expired, confirmed, skipped, failed);
        if (summary.scanned() > 0) {
            log.info("TTL 만료 한 바퀴. {}", summary);
        }
        return summary;
    }
}
