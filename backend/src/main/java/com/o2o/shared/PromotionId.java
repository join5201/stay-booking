package com.o2o.shared;

import java.util.Objects;
import java.util.UUID;

/**
 * 프로모션 식별자. 설계 근거: 06-2 1절 Promotion 식별자 열, 06-1 4절 공유 커널.
 *
 * shared에 두는 근거는 RoomTypeId와 같다. 06-2 1절이 Booking의 PriceSnapshot 내부 요소에
 * promotionId를 적고 06-1 R5가 예약이 프로모션 식별자와 이름을 스냅샷에 복사한다고 적는다.
 * 프로모션 밖의 컨텍스트가 이 타입을 쓴다.
 *
 * 값 형식의 근거는 11 공통 요청과 응답 규칙이다. 최대 64자의 비어 있지 않은 문자열이고
 * 클라이언트는 내부 구조를 해석하지 않는다. promo_ 여섯 자에 하이픈 없는 UUID 32자로 38자다.
 * 명세의 promo_001은 예시이지 형식 규정이 아니다.
 */
public record PromotionId(String value) {

    private static final String PREFIX = "promo_";

    public PromotionId {
        Objects.requireNonNull(value, "PromotionId는 null일 수 없다");
        if (value.isBlank() || value.length() > 64) {
            throw new IllegalArgumentException("PromotionId는 1자 이상 64자 이하여야 한다: " + value);
        }
    }

    public static PromotionId newId() {
        return new PromotionId(PREFIX + UUID.randomUUID().toString().replace("-", ""));
    }

    public static PromotionId of(String value) {
        return new PromotionId(value);
    }
}
