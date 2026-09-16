package com.o2o.promotion.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.o2o.catalog.domain.PropertyRepository;
import com.o2o.catalog.domain.RoomTypeRepository;
import com.o2o.inventory.domain.DailyRateRepository;
import com.o2o.promotion.domain.PricingService;
import com.o2o.promotion.domain.PromotionRepository;

/**
 * 도메인 서비스 PricingService의 빈 등록. 설계 근거: 계약 7절 D-3 가, backend/CLAUDE.md 레이어
 * 규칙(도메인은 스프링을 참조하지 않는다).
 *
 * PricingService에 @Service를 붙이면 도메인이 스프링을 참조한다. 그래서 등록을 인프라로
 * 내렸다. Clock은 shared의 ClockConfiguration이 주는 UTC 시계이고 테스트는 @Primary로 바꾼다.
 */
@Configuration
public class PricingServiceConfiguration {

    @Bean
    public PricingService pricingService(RoomTypeRepository roomTypeRepository,
                                         PropertyRepository propertyRepository,
                                         DailyRateRepository dailyRateRepository,
                                         PromotionRepository promotionRepository,
                                         Clock clock) {
        return new PricingService(roomTypeRepository, propertyRepository, dailyRateRepository,
                promotionRepository, clock);
    }
}
