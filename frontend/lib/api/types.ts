// 응답 모델 중 공통 컴포넌트가 그리는 것. context.md 6절과 document/11 응답 모델 절.
// 나머지 모델과 요청 본문은 T3에서 이 파일에 더한다

export type BookingStatus = "HELD" | "CONFIRMED" | "EXPIRED" | "CANCELED";
export type ExpirationReason = "TTL_EXPIRED" | "PAYMENT_FAILED";
export type AttemptStatus = "REQUESTED" | "APPROVED" | "FAILED";
export type RefundReason = "BOOKING_CANCELED" | "LATE_APPROVAL";

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AppliedPromotion {
  id: string;
  name: string;
  discountRate: number;
}

export interface PriceDay {
  date: string;
  baseAmount: number;
  discountAmount: number;
  finalAmount: number;
}

export interface PriceSnapshot {
  currency: string;
  baseTotalAmount: number;
  discountTotalAmount: number;
  totalAmount: number;
  appliedPromotion: AppliedPromotion | null;
  days: PriceDay[];
}

export interface PaymentAttempt {
  id: string;
  bookingId: string;
  attemptNumber: number;
  status: AttemptStatus;
  amount: number;
  currency: string;
  pgTransactionId: string | null;
  mockMode: string | null;
  requestedAt: string;
  completedAt: string | null;
  failureCode: string | null;
}

export interface Refund {
  id: string;
  paymentAttemptId: string;
  amount: number;
  currency: string;
  status: "REFUNDED";
  reason: RefundReason;
  refundedAt: string;
}

export interface PaymentSummary {
  attemptCount: number;
  approvedAttemptId: string | null;
  attempts: PaymentAttempt[];
  refund: Refund | null;
}
