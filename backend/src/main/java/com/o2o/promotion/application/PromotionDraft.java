package com.o2o.promotion.application;

import java.time.LocalDate;
import java.util.List;

/**
 * PROMO-01의 입력. 설계 근거: 11 PROMO-01 요청 필드표 아홉 칸.
 *
 * 컨트롤러의 요청 DTO를 응용 층에 그대로 넘기지 않기 위한 값이다. 요청 DTO는 형식 검증
 * 어노테이션을 지니고(06-4 1-4) 응용 층은 그것을 몰라야 한다. stayStartDate와 stayEndDate는
 * 둘 다 null이면 숙박 기간 제한 없음이고 하나만 null이면 도메인이 거부한다(V4).
 */
public record PromotionDraft(String name, int discountRate, LocalDate campaignStartDate,
                             LocalDate campaignEndDate, LocalDate stayStartDate,
                             LocalDate stayEndDate, int minNights, List<String> regionCodes,
                             boolean enabled) {
}
