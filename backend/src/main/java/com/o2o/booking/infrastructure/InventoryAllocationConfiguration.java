package com.o2o.booking.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.inventory.domain.InventoryAllocationService;

/**
 * 재고 도메인 서비스의 빈 조립. InventoryAllocationService는 06-2 6절대로 재고와 요금
 * 컨텍스트의 도메인 서비스이고 스프링을 모르는 순수 클래스다. 부르는 쪽이 예약 앱 서비스
 * 하나라 하류인 예약 컨텍스트가 조립한다(06-1 R4). 재고의 application 패키지는 이 묶음이
 * 고치지 않는다(계약 3절).
 */
@Configuration
public class InventoryAllocationConfiguration {

    @Bean
    public InventoryAllocationService inventoryAllocationService(
            DailyInventoryRepository inventoryRepository) {
        return new InventoryAllocationService(inventoryRepository);
    }
}
