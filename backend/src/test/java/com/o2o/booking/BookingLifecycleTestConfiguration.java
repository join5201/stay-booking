package com.o2o.booking;

import java.time.Clock;
import java.time.Instant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

import com.o2o.booking.application.BookingApplicationService;
import com.o2o.booking.application.BookingLifecycle;
import com.o2o.booking.domain.BookingRepository;
import com.o2o.catalog.application.CatalogApplicationService;
import com.o2o.inventory.application.InventoryApplicationService;
import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.payment.application.PaymentApplicationService;
import com.o2o.payment.domain.PaymentRepository;

/**
 * 2차 통합 테스트가 같이 쓰는 컨텍스트 조각. 시계와 구독자와 준비 도구와 훅 둘이다. 테스트
 * 클래스가 @Import로 가져오면 스프링이 컨텍스트 하나를 캐시해 클래스 사이에서 다시 띄우지 않는다.
 * 훅 둘은 무장하지 않으면 운영 빈과 같다.
 *
 * 시각의 기준은 1차와 같은 UTC 10월 1일 20시다. 서울의 오늘은 10월 2일이고 숙박 10월 10일은 그
 * 뒤다(T5). 만료 시각은 20시 10분이다(TTL 10분).
 */
@TestConfiguration
public class BookingLifecycleTestConfiguration {

    public static final Instant FIXED_NOW = Instant.parse("2026-10-01T20:00:00Z");

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock(FIXED_NOW);
    }

    @Bean
    CommittedBookingEvents committedBookingEvents() {
        return new CommittedBookingEvents();
    }

    @Bean
    BookingFixtures bookingFixtures(CatalogApplicationService catalogService,
                                    InventoryApplicationService inventoryService,
                                    BookingApplicationService bookingService,
                                    BookingRepository bookingRepository,
                                    DailyInventoryRepository inventoryRepository,
                                    PaymentRepository paymentRepository,
                                    PlatformTransactionManager transactionManager,
                                    MutableClock clock) {
        return new BookingFixtures(catalogService, inventoryService, bookingService,
                bookingRepository, inventoryRepository, paymentRepository, transactionManager, clock);
    }

    @Bean
    @Primary
    FailingOnceInventoryAllocationService failingOnceInventoryAllocationService(
            DailyInventoryRepository inventoryRepository) {
        return new FailingOnceInventoryAllocationService(inventoryRepository);
    }

    @Bean
    @Primary
    FailingOncePaymentOutcomeService failingOncePaymentOutcomeService(
            BookingRepository bookingRepository, BookingLifecycle lifecycle,
            PaymentApplicationService paymentService, Clock clock) {
        return new FailingOncePaymentOutcomeService(bookingRepository, lifecycle, paymentService, clock);
    }
}
