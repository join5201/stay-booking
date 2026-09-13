package com.o2o.booking.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.o2o.booking.application.ExpireDueBookings;

/**
 * TTL 스케줄러의 켜고 끄기. 설계 근거: 2차 계약 7절 D-3 가. 키 o2o.booking.expire-scheduler.enabled가
 * true(기본)일 때만 이 설정이 살고, 그때만 스케줄링 기반(@EnableScheduling)과 스케줄러 빈이 뜬다.
 * 이 저장소의 첫 스케줄러라 기반을 여기서 켠다. 끄면 백그라운드 스레드가 하나도 없어 테스트가
 * T6으로 놓은 만료 시각 지난 HELD를 먼저 집어 가지 않는다(테스트 설정의 enabled=false).
 *
 * 세 애너테이션은 빌드가 쓰는 jar에서 확인했다(2026-09-13). Scheduled와 EnableScheduling은
 * spring-context-7.0.9, ConditionalOnProperty(name, havingValue, matchIfMissing)는
 * spring-boot-autoconfigure-4.1.1. Scheduled.fixedDelayString의 기간 표기(PT1S)는 같은 jar의
 * ScheduledAnnotationBeanPostProcessor.toDuration(String, TimeUnit)이 푼다.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "o2o.booking.expire-scheduler.enabled", havingValue = "true",
        matchIfMissing = true)
public class BookingExpireSchedulerConfiguration {

    @Bean
    public BookingExpireScheduler bookingExpireScheduler(ExpireDueBookings expireDueBookings) {
        return new BookingExpireScheduler(expireDueBookings);
    }
}
