package com.o2o.booking.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.o2o.shared.Money;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;

/**
 * 가격 스냅샷 값 객체. 설계 근거: 06-2 1절 PriceSnapshot VO(DailyPrice ×N, promotionId,
 * promotionName, totalAmount), 06-2 6절 PriceSnapshot CRC(생성 후 불변), 11 응답 모델 PriceSnapshot.
 *
 * 생성자가 지키는 불변식은 넷이다(06-2 3-1, 06-4 1-2 Booking 생성자의 불변식 열).
 * I10 할인 배분액 합 == 할인 총액.
 * I11 스냅샷 행 수 == 박수. of가 StayPeriod를 받아 날짜까지 대조한다.
 * I12 할인 배분액 > 0이면 promotionId 보유.
 * I15 totalAmount == 날짜별 (단가 - 할인 배분액)의 합. baseTotalAmount도 단가 합과 대조한다.
 *
 * 총액 셋을 계산하지 않고 받아서 대조하는 이유는 06-2 6절이 배분 합계와 총액의 일치를 만드는
 * 쪽을 PricingService로, 검증하는 쪽을 Booking 생성자로 갈라 적어서다. 여기서 계산해 버리면
 * 검증이 사라진다.
 *
 * 금액이 Money가 아니라 long인 이유는 Money의 상한이 1박 요금의 상한(10억)이고 30박 총액은
 * 그것을 넘을 수 있어서다(11 BOOK-01 expectedTotalAmount 상한 300억). 통화는 Money.KRW다.
 *
 * 불변이다. 값을 바꾸는 메서드가 없고 days는 방어 복사한 불변 목록으로 돌려준다(I4).
 */
@Embeddable
public class PriceSnapshot {

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "base_total_amount", nullable = false)
    private long baseTotalAmount;

    @Column(name = "discount_total_amount", nullable = false)
    private long discountTotalAmount;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "promotion_id", length = 64)
    private String promotionId;

    @Column(name = "promotion_name", length = 200)
    private String promotionName;

    @Column(name = "promotion_discount_rate")
    private Integer promotionDiscountRate;

    // EAGER인 이유는 예약을 읽는 모든 경로가 days를 같이 낸다는 것이다(11 응답 모델 Booking).
    // 목록 조회는 페이지 크기 100 이하라 로컬 v1에서 감당한다
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_price_day", joinColumns = @JoinColumn(name = "booking_id"))
    @OrderBy("date asc")
    private List<DailyPrice> days = new ArrayList<>();

    protected PriceSnapshot() {
    }

    private PriceSnapshot(String currency, List<DailyPrice> days, AppliedPromotion promotion,
                          long baseTotalAmount, long discountTotalAmount, long totalAmount) {
        this.currency = currency;
        this.days = new ArrayList<>(days);
        this.promotionId = promotion == null ? null : promotion.id();
        this.promotionName = promotion == null ? null : promotion.name();
        this.promotionDiscountRate = promotion == null ? null : promotion.discountRate();
        this.baseTotalAmount = baseTotalAmount;
        this.discountTotalAmount = discountTotalAmount;
        this.totalAmount = totalAmount;
    }

    /**
     * 스냅샷을 만들고 I10, I11, I12, I15를 검사한다. 위반은 InvalidSnapshot이다.
     * promotion이 null이면 할인이 전부 0이어야 한다.
     */
    public static PriceSnapshot of(StayPeriod period, String currency, List<DailyPrice> days,
                                   AppliedPromotion promotion, long baseTotalAmount,
                                   long discountTotalAmount, long totalAmount) {
        Objects.requireNonNull(period, "period는 null일 수 없다");
        Objects.requireNonNull(days, "days는 null일 수 없다");
        if (!Money.KRW.equals(currency)) {
            throw new InvalidPriceSnapshotException("통화는 KRW만 쓴다: " + currency);
        }
        // I11. 행 수와 날짜가 숙박 기간과 같다
        List<LocalDate> expected = period.dates();
        List<LocalDate> actual = days.stream().map(DailyPrice::date).toList();
        if (!expected.equals(actual)) {
            throw new InvalidPriceSnapshotException("행 수와 날짜가 숙박 기간과 다르다. 기간 " + expected
                    + ", 스냅샷 " + actual);
        }
        long baseSum = days.stream().mapToLong(DailyPrice::baseAmount).sum();
        long discountSum = days.stream().mapToLong(DailyPrice::discountAmount).sum();
        long finalSum = days.stream().mapToLong(DailyPrice::finalAmount).sum();
        // I10. 할인 배분액 합이 할인 총액이다
        if (discountSum != discountTotalAmount) {
            throw new InvalidPriceSnapshotException("할인 배분액 합 " + discountSum + "이 할인 총액 "
                    + discountTotalAmount + "과 다르다");
        }
        // I15. 총액은 날짜별 (단가 - 할인 배분액)의 합이다. 단가 합도 같은 자리에서 본다
        if (finalSum != totalAmount) {
            throw new InvalidPriceSnapshotException("날짜별 최종액 합 " + finalSum + "이 총액 "
                    + totalAmount + "과 다르다");
        }
        if (baseSum != baseTotalAmount) {
            throw new InvalidPriceSnapshotException("단가 합 " + baseSum + "이 할인 전 총액 "
                    + baseTotalAmount + "과 다르다");
        }
        // I12. 할인이 있으면 근거 프로모션이 있다
        if (discountSum > 0 && promotion == null) {
            throw new InvalidPriceSnapshotException("할인 " + discountSum + "이 있는데 프로모션이 없다");
        }
        return new PriceSnapshot(currency, days, promotion, baseTotalAmount, discountTotalAmount,
                totalAmount);
    }

    /** 이 스냅샷이 그 기간의 날짜를 정확히 덮는가. Booking 생성자가 I11을 다시 본다 */
    public boolean covers(StayPeriod period) {
        return period.dates().equals(days.stream().map(DailyPrice::date).toList());
    }

    public String currency() {
        return currency;
    }

    public long baseTotalAmount() {
        return baseTotalAmount;
    }

    public long discountTotalAmount() {
        return discountTotalAmount;
    }

    public long totalAmount() {
        return totalAmount;
    }

    public Optional<AppliedPromotion> appliedPromotion() {
        if (promotionId == null) {
            return Optional.empty();
        }
        return Optional.of(new AppliedPromotion(promotionId, promotionName, promotionDiscountRate));
    }

    /** 날짜 오름차순의 불변 목록. 바깥에서 바꿀 수 없다(I4) */
    public List<DailyPrice> days() {
        return List.copyOf(days);
    }
}
