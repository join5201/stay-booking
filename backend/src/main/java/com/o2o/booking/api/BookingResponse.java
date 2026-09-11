package com.o2o.booking.api;

import java.time.Instant;
import java.util.List;

import com.o2o.booking.domain.AppliedPromotion;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.DailyPrice;
import com.o2o.booking.domain.PriceSnapshot;

/**
 * 응답 모델 Booking. 설계 근거: 11 응답 모델 Booking(2557행)의 21개 필드 그대로. 필드를 더하지
 * 않는다(BN1). 이름은 API 이름(guestId, guestCount)이고 값은 도메인의 userId와 userCount다.
 *
 * 1차에서 값이 고정인 것(계약 2절). expirationReason, cancellationReason, confirmedAt,
 * canceledAt, expiredAt은 전이가 없어 null이고, payment는 결제 컨텍스트가 붙기 전이라 시도 0에
 * 빈 목록이다. 2차가 Booking의 전이 필드와 결제 세션의 attemptsOf로 채운다. 06-2 1절의 Booking
 * 필드 목록에도 그 다섯은 없다. 이 응답이 그 값을 상수로 내는 것은 모델에 없는 것을 지어내는
 * 것이 아니라 아직 일어나지 않은 전이의 부재를 그대로 적는 것이다.
 */
public record BookingResponse(
        String id,
        String guestId,
        String propertyId,
        String roomTypeId,
        String checkIn,
        String checkOut,
        int guestCount,
        String status,
        String expiresAt,
        String expirationReason,
        PriceSnapshotResponse priceSnapshot,
        PaymentSummaryResponse payment,
        String cancellationReason,
        String createdAt,
        String updatedAt,
        String confirmedAt,
        String canceledAt,
        String expiredAt,
        String serverNow,
        long version) {

    public static BookingResponse from(Booking booking, Instant serverNow) {
        return new BookingResponse(
                booking.id().value(),
                booking.userId().value(),
                booking.propertyId().value(),
                booking.roomTypeId().value(),
                ApiFormat.date(booking.period().checkIn()),
                ApiFormat.date(booking.period().checkOut()),
                booking.userCount(),
                booking.status().name(),
                ApiFormat.time(booking.expiresAt()),
                null,
                PriceSnapshotResponse.from(booking.priceSnapshot()),
                PaymentSummaryResponse.none(),
                null,
                ApiFormat.time(booking.createdAt()),
                ApiFormat.time(booking.updatedAt()),
                null,
                null,
                null,
                ApiFormat.time(serverNow),
                booking.version());
    }

    /** 응답 모델 PriceSnapshot(2412행) */
    public record PriceSnapshotResponse(
            String currency,
            long baseTotalAmount,
            long discountTotalAmount,
            long totalAmount,
            AppliedPromotionResponse appliedPromotion,
            List<PriceDayResponse> days) {

        static PriceSnapshotResponse from(PriceSnapshot snapshot) {
            return new PriceSnapshotResponse(
                    snapshot.currency(),
                    snapshot.baseTotalAmount(),
                    snapshot.discountTotalAmount(),
                    snapshot.totalAmount(),
                    snapshot.appliedPromotion().map(AppliedPromotionResponse::from).orElse(null),
                    snapshot.days().stream().map(PriceDayResponse::from).toList());
        }
    }

    /** 응답 모델 AppliedPromotion(2391행). 1차 어댑터는 만들지 않으므로 항상 null이 나간다 */
    public record AppliedPromotionResponse(String id, String name, int discountRate) {

        static AppliedPromotionResponse from(AppliedPromotion promotion) {
            return new AppliedPromotionResponse(promotion.id(), promotion.name(),
                    promotion.discountRate());
        }
    }

    /** 응답 모델 PriceDay(2401행) */
    public record PriceDayResponse(String date, long baseAmount, long discountAmount,
                                   long finalAmount) {

        static PriceDayResponse from(DailyPrice day) {
            return new PriceDayResponse(ApiFormat.date(day.date()), day.baseAmount(),
                    day.discountAmount(), day.finalAmount());
        }
    }

    /**
     * 응답 모델 PaymentSummary(2546행). 1차는 결제 컨텍스트가 없어 시도 0과 빈 목록이다.
     * attempts의 원소 모델 PaymentAttempt는 2차가 결제 세션의 모양으로 만든다.
     */
    public record PaymentSummaryResponse(int attemptCount, String approvedAttemptId,
                                         List<Object> attempts, Object refund) {

        static PaymentSummaryResponse none() {
            return new PaymentSummaryResponse(0, null, List.of(), null);
        }
    }
}
