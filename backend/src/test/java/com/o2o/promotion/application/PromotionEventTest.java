package com.o2o.promotion.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import com.o2o.promotion.domain.Promotion;
import com.o2o.promotion.domain.PromotionChanges;
import com.o2o.promotion.domain.PromotionCreated;
import com.o2o.promotion.domain.PromotionUpdated;
import com.o2o.promotion.domain.StayWindowChange;
import com.o2o.shared.VersionConflictException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 06-4 1-2 프로모션 계약표의 Post 열. create는 PromotionCreated, update는 PromotionUpdated 발행.
 *
 * 구독자가 없는 지금 발행을 빠뜨려도 다른 테스트는 전부 초록이다. 계약 7절 D-1 나가 미룬
 * 검색 프로젝션이 붙는 순간 조용히 깨진다. 계약표가 약속한 자리에서 확인한다. 도구와 짝의
 * 근거는 InventoryEventTest와 같다.
 */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class PromotionEventTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-01T00:00:00Z");
    private static final LocalDate START = LocalDate.parse("2026-10-02");

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private PromotionApplicationService promotionApplicationService;

    @Autowired
    private ApplicationEvents events;

    @Test
    void 등록하면_PromotionCreated가_난다() {
        Promotion created = 등록();

        assertEquals(1, events.stream(PromotionCreated.class).count());
        PromotionCreated event = events.stream(PromotionCreated.class).findFirst().orElseThrow();
        assertEquals(created.id(), event.promotionId());
        assertEquals(FIXED_NOW, event.occurredAt());
    }

    @Test
    void 수정하면_PromotionUpdated가_난다() {
        // 11 PROMO-02 처리 규칙의 수동 종료가 이벤트에 실린다
        Promotion created = 등록();

        promotionApplicationService.update(created.id(), 0L, 사용_해제());

        assertEquals(1, events.stream(PromotionUpdated.class).count());
        PromotionUpdated event = events.stream(PromotionUpdated.class).findFirst().orElseThrow();
        assertEquals(created.id(), event.promotionId());
        assertFalse(event.enabled());
        assertEquals(1L, event.version());
    }

    @Test
    void 거절된_수정은_이벤트를_내지_않는다() {
        // 발행 확인의 짝이다
        Promotion created = 등록();

        assertThrows(VersionConflictException.class,
                () -> promotionApplicationService.update(created.id(), 99L, 사용_해제()));

        assertEquals(0, events.stream(PromotionUpdated.class).count());
    }

    private Promotion 등록() {
        return promotionApplicationService.create(new PromotionDraft(
                "가을 할인", 10, START, START.plusDays(30), null, null, 1, List.of(), true));
    }

    private static PromotionChanges 사용_해제() {
        return new PromotionChanges(null, null, null, null, StayWindowChange.keep(), null, null,
                false);
    }
}
