package com.o2o.promotion.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 오늘(서울 기준) 켜져 있는 프로모션을 한 번 읽어 둔 것. PricingService.activePromotions가 만든다.
 *
 * 검색이 객실마다 프로모션을 다시 읽지 않게 하려고 만들었다(이슈 223). today를 같이 싣는 이유는
 * 읽은 날과 적용 판정의 날이 같아야 하기 때문이다. 자정을 넘겨 둘이 갈리면 어제 켜진 목록을
 * 오늘 기준으로 판정하게 된다.
 */
public record ActivePromotions(LocalDate today, List<Promotion> promotions) {

    public ActivePromotions {
        Objects.requireNonNull(today, "today는 null일 수 없다");
        promotions = List.copyOf(promotions);
    }
}
