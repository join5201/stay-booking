// 응답 모델과 요청 본문. context.md 6절과 backend의 *Response.java, *Request.java 그대로.
// 시각은 UTC 문자열, 숙박 날짜는 YYYY-MM-DD 문자열, 돈은 KRW 정수

export type BookingStatus = "HELD" | "CONFIRMED" | "EXPIRED" | "CANCELED";
export type ExpirationReason = "TTL_EXPIRED" | "PAYMENT_FAILED";
export type AttemptStatus = "REQUESTED" | "APPROVED" | "FAILED";
export type RefundReason = "BOOKING_CANCELED" | "LATE_APPROVAL";
export type MockModeValue = "APPROVE" | "DECLINE" | "DEFER";

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PageParams {
  page?: number;
  size?: number;
}

// 숙소와 객실 (CAT)

export interface Property {
  id: string;
  hostId: string;
  name: string;
  regionCode: string;
  address: string;
  description: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface RegisterPropertyBody {
  name: string;
  regionCode: string;
  address: string;
  description?: string;
}

// version 필수, 바꿀 필드 1개 이상
export interface UpdatePropertyBody {
  version: number;
  name?: string;
  regionCode?: string;
  address?: string;
  description?: string;
}

export interface RoomType {
  id: string;
  propertyId: string;
  name: string;
  maxOccupancy: number;
  description: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface RegisterRoomTypeBody {
  name: string;
  maxOccupancy: number;
  description?: string;
}

export interface UpdateRoomTypeBody {
  version: number;
  name?: string;
  maxOccupancy?: number;
  description?: string;
}

// 재고와 요금 (INV, RATE)

export interface DailyInventory {
  roomTypeId: string;
  date: string;
  totalCount: number;
  heldCount: number;
  soldCount: number;
  availableCount: number;
  version: number;
}

export interface InventoryRange {
  roomTypeId: string;
  from: string;
  to: string;
  items: DailyInventory[];
  missingDates: string[];
}

export interface RegisterInventoryBody {
  date: string;
  totalCount: number;
}

// 최대 366일. 한 날짜라도 있으면 전부 409(계약 2-1절 2행)
export interface BulkInventoryBody {
  from: string;
  to: string;
  totalCount: number;
}

export interface AdjustInventoryBody {
  version: number;
  totalCount: number;
}

export interface DailyRate {
  roomTypeId: string;
  date: string;
  amount: number;
  currency: string;
  version: number;
}

export interface RateRange {
  roomTypeId: string;
  from: string;
  to: string;
  items: DailyRate[];
  missingDates: string[];
}

export interface RegisterRateBody {
  date: string;
  amount: number;
  currency: "KRW";
}

// version과 amount뿐. currency를 보내면 400(계약 2-1절 3행)
export interface AdjustRateBody {
  version: number;
  amount: number;
}

// 프로모션 (PROMO)

export interface Promotion {
  id: string;
  name: string;
  discountRate: number;
  campaignStartDate: string;
  campaignEndDate: string;
  stayStartDate: string | null;
  stayEndDate: string | null;
  minNights: number;
  regionCodes: string[];
  enabled: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePromotionBody {
  name: string;
  discountRate: number;
  campaignStartDate: string;
  campaignEndDate: string;
  stayStartDate?: string;
  stayEndDate?: string;
  minNights: number;
  regionCodes: string[];
  enabled?: boolean;
}

// stayStartDate와 stayEndDate는 함께. null 둘은 해제. 사용 끄기는 enabled false와 version만
export interface UpdatePromotionBody {
  version: number;
  name?: string;
  discountRate?: number;
  campaignStartDate?: string;
  campaignEndDate?: string;
  stayStartDate?: string | null;
  stayEndDate?: string | null;
  minNights?: number;
  regionCodes?: string[];
  enabled?: boolean;
}

export interface ApplicablePromotionItem {
  id: string;
  name: string;
  discountRate: number;
  discountAmount: number;
  selected: boolean;
}

export interface ApplicablePromotions {
  roomTypeId: string;
  checkIn: string;
  checkOut: string;
  guestCount: number;
  evaluatedAt: string;
  items: ApplicablePromotionItem[];
  selectedPromotionId: string | null;
}

// 검색과 가용성과 예상 금액 (SEARCH)

export interface StayParams {
  checkIn: string;
  checkOut: string;
  guestCount: number;
}

export interface SearchParams extends StayParams {
  regionCode: string;
}

export interface RoomSearchResult {
  roomTypeId: string;
  name: string;
  maxOccupancy: number;
  availableCount: number;
  totalAmount: number;
}

export interface PropertySearchResult {
  property: Property;
  lowestTotalAmount: number;
  currency: string;
  availableRoomTypes: RoomSearchResult[];
}

export type AvailabilityReason = "OCCUPANCY_EXCEEDED" | "INVENTORY_NOT_CONFIGURED" | "INVENTORY_UNAVAILABLE" | "RATE_NOT_CONFIGURED";

export interface AvailabilityDay {
  date: string;
  // null은 판매 안 함
  availableCount: number | null;
}

export interface Availability extends StayParams {
  roomTypeId: string;
  available: boolean;
  availableCount: number;
  days: AvailabilityDay[];
  missingInventoryDates: string[];
  missingRateDates: string[];
  reasons: AvailabilityReason[];
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

// 조회 시점 계산이고 확정 금액이 아니다
export interface PriceQuote extends StayParams {
  roomTypeId: string;
  nights: number;
  estimatedAt: string;
  price: PriceSnapshot;
}

// 예약과 결제 (BOOK, PAY)

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

export interface Booking extends StayParams {
  id: string;
  guestId: string;
  propertyId: string;
  roomTypeId: string;
  status: BookingStatus;
  expiresAt: string;
  expirationReason: ExpirationReason | null;
  priceSnapshot: PriceSnapshot;
  payment: PaymentSummary;
  cancellationReason: string | null;
  createdAt: string;
  updatedAt: string;
  confirmedAt: string | null;
  canceledAt: string | null;
  expiredAt: string | null;
  serverNow: string;
  version: number;
}

// 여섯 필드 전부 필수(인계 문서 68행)
export interface RequestBookingBody extends StayParams {
  roomTypeId: string;
  expectedTotalAmount: number;
  currency: "KRW";
}

export interface RequestPaymentBody {
  mockMode?: MockModeValue;
}

export interface CancelBookingBody {
  reason?: string;
}

export interface PaymentAttemptList {
  bookingId: string;
  attemptCount: number;
  items: PaymentAttempt[];
}
