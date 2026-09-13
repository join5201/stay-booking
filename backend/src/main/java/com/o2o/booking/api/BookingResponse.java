package com.o2o.booking.api;

import java.time.Instant;
import java.util.List;

import com.o2o.booking.domain.AppliedPromotion;
import com.o2o.booking.domain.Booking;
import com.o2o.booking.domain.DailyPrice;
import com.o2o.booking.domain.PriceSnapshot;
import com.o2o.payment.application.PaymentSummaryView;
import com.o2o.payment.application.RefundView;

/**
 * 응답 모델 Booking. 설계 근거: 11 응답 모델 Booking(2557행)의 21개 필드 그대로. 필드를 더하지
 * 않는다(BN1). 이름은 API 이름(guestId, guestCount)이고 값은 도메인의 userId와 userCount다.
 *
 * 1차는 전이 다섯 필드(expirationReason, cancellationReason, confirmedAt, canceledAt, expiredAt)를
 * null로, payment를 시도 0에 빈 목록으로 고정했다. 2차가 둘을 채운다(2차 계약 2절 응답 모델 문단).
 * 전이 다섯은 Booking의 컬럼이고 payment는 결제의 attemptsOf 결과다. 결제 뷰를 응답으로 옮기는
 * 일은 이 층이 한다. 조회 하나에 attemptsOf 한 번이고 목록은 항목마다다(2차 계약 6절 Booking
 * 응답의 payment 채움 행).
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

    public static BookingResponse from(Booking booking, PaymentSummaryView payment, Instant serverNow) {
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
                booking.expirationReason() == null ? null : booking.expirationReason().name(),
                PriceSnapshotResponse.from(booking.priceSnapshot()),
                PaymentSummaryResponse.from(payment),
                booking.cancellationReason(),
                ApiFormat.time(booking.createdAt()),
                ApiFormat.time(booking.updatedAt()),
                optionalTime(booking.confirmedAt()),
                optionalTime(booking.canceledAt()),
                optionalTime(booking.expiredAt()),
                ApiFormat.time(serverNow),
                booking.version());
    }

    /** 전이 전에는 null인 시각(11 응답 모델 Booking의 timestamp/null 셋) */
    private static String optionalTime(Instant instant) {
        return instant == null ? null : ApiFormat.time(instant);
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
     * 응답 모델 PaymentSummary(2546행). 넷 그대로(BN1). 시도 없는 예약은 0과 null과 빈 배열과
     * null이다. approvedAttemptId는 환불 뒤에도 유지되고 refund는 환불 전 null이다(같은 모델 표).
     */
    public record PaymentSummaryResponse(int attemptCount, String approvedAttemptId,
                                         List<PaymentAttemptResponse> attempts, RefundResponse refund) {

        static PaymentSummaryResponse from(PaymentSummaryView view) {
            return new PaymentSummaryResponse(
                    view.attemptCount(),
                    view.approvedAttemptId(),
                    view.attempts().stream().map(PaymentAttemptResponse::from).toList(),
                    view.refund() == null ? null : RefundResponse.from(view.refund()));
        }
    }

    /** 응답 모델 Refund(2529행). 7개 필드 그대로(BN1). status는 REFUNDED 하나다 */
    public record RefundResponse(String id, String paymentAttemptId, long amount, String currency,
                                 String status, String reason, String refundedAt) {

        static RefundResponse from(RefundView view) {
            return new RefundResponse(view.id(), view.paymentAttemptId(), view.amount(),
                    view.currency(), view.status(), view.reason().name(),
                    ApiFormat.time(view.refundedAt()));
        }
    }
}
