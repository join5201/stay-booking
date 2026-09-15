import type { ApplicablePromotions, Availability, Booking, DailyInventory, DailyRate, InventoryRange, Page, PaymentAttempt, PriceQuote, Promotion, Property, PropertySearchResult, RateRange, RoomType } from "../types";

// 훅과 화면 테스트의 본보기 응답. 값은 document/11 응답 모델의 모양을 따르고 ID와 숫자는 임의다

export const SERVER_NOW = "2026-09-15T10:00:00.000Z";

export const property: Property = {
  id: "prop_001",
  hostId: "host_001",
  name: "한강 뷰 스테이",
  regionCode: "SEOUL",
  address: "서울 마포구 어딘가 1",
  description: "강이 보이는 숙소",
  version: 0,
  createdAt: "2026-09-01T00:00:00.000Z",
  updatedAt: "2026-09-01T00:00:00.000Z",
};

export const roomType: RoomType = {
  id: "rt_001",
  propertyId: property.id,
  name: "리버 트윈",
  maxOccupancy: 2,
  description: "트윈 침대",
  version: 0,
  createdAt: "2026-09-01T00:00:00.000Z",
  updatedAt: "2026-09-01T00:00:00.000Z",
};

export function page<T>(items: T[], p = 0, size = 20): Page<T> {
  return { items, page: p, size, totalElements: items.length, totalPages: items.length === 0 ? 0 : 1 };
}

export const inventory: DailyInventory = {
  roomTypeId: roomType.id,
  date: "2026-10-01",
  totalCount: 5,
  heldCount: 1,
  soldCount: 2,
  availableCount: 2,
  version: 3,
};

export const inventoryRange: InventoryRange = {
  roomTypeId: roomType.id,
  from: "2026-10-01",
  to: "2026-10-03",
  items: [inventory, { ...inventory, date: "2026-10-02", version: 0 }],
  missingDates: ["2026-10-03"],
};

export const rate: DailyRate = {
  roomTypeId: roomType.id,
  date: "2026-10-01",
  amount: 120000,
  currency: "KRW",
  version: 1,
};

export const rateRange: RateRange = {
  roomTypeId: roomType.id,
  from: "2026-10-01",
  to: "2026-10-03",
  items: [rate, { ...rate, date: "2026-10-02", version: 0 }],
  missingDates: ["2026-10-03"],
};

export const promotion: Promotion = {
  id: "promo_001",
  name: "가을 10%",
  discountRate: 10,
  campaignStartDate: "2026-09-01",
  campaignEndDate: "2026-12-01",
  stayStartDate: null,
  stayEndDate: null,
  minNights: 1,
  regionCodes: [],
  enabled: true,
  version: 0,
  createdAt: "2026-09-01T00:00:00.000Z",
  updatedAt: "2026-09-01T00:00:00.000Z",
};

export const applicablePromotions: ApplicablePromotions = {
  roomTypeId: roomType.id,
  checkIn: "2026-10-01",
  checkOut: "2026-10-03",
  guestCount: 2,
  evaluatedAt: SERVER_NOW,
  items: [{ id: promotion.id, name: promotion.name, discountRate: 10, discountAmount: 24000, selected: true }],
  selectedPromotionId: promotion.id,
};

export const searchResult: PropertySearchResult = {
  property,
  lowestTotalAmount: 216000,
  currency: "KRW",
  availableRoomTypes: [{ roomTypeId: roomType.id, name: roomType.name, maxOccupancy: 2, availableCount: 2, totalAmount: 216000 }],
};

export const availability: Availability = {
  roomTypeId: roomType.id,
  checkIn: "2026-10-01",
  checkOut: "2026-10-03",
  guestCount: 2,
  available: true,
  availableCount: 2,
  days: [
    { date: "2026-10-01", availableCount: 2 },
    { date: "2026-10-02", availableCount: 3 },
  ],
  missingInventoryDates: [],
  missingRateDates: [],
  reasons: [],
};

export const quote: PriceQuote = {
  roomTypeId: roomType.id,
  checkIn: "2026-10-01",
  checkOut: "2026-10-03",
  guestCount: 2,
  nights: 2,
  estimatedAt: SERVER_NOW,
  price: {
    currency: "KRW",
    baseTotalAmount: 240000,
    discountTotalAmount: 24000,
    totalAmount: 216000,
    appliedPromotion: { id: promotion.id, name: promotion.name, discountRate: 10 },
    days: [
      { date: "2026-10-01", baseAmount: 120000, discountAmount: 12000, finalAmount: 108000 },
      { date: "2026-10-02", baseAmount: 120000, discountAmount: 12000, finalAmount: 108000 },
    ],
  },
};

export const heldBooking: Booking = {
  id: "bk_001",
  guestId: "guest_001",
  propertyId: property.id,
  roomTypeId: roomType.id,
  checkIn: "2026-10-01",
  checkOut: "2026-10-03",
  guestCount: 2,
  status: "HELD",
  expiresAt: "2026-09-15T10:10:00.000Z",
  expirationReason: null,
  priceSnapshot: quote.price,
  payment: { attemptCount: 0, approvedAttemptId: null, attempts: [], refund: null },
  cancellationReason: null,
  createdAt: SERVER_NOW,
  updatedAt: SERVER_NOW,
  confirmedAt: null,
  canceledAt: null,
  expiredAt: null,
  serverNow: SERVER_NOW,
  version: 0,
};

export const requestedAttempt: PaymentAttempt = {
  id: "pa_001",
  bookingId: heldBooking.id,
  attemptNumber: 1,
  status: "REQUESTED",
  amount: 216000,
  currency: "KRW",
  pgTransactionId: "mock_001",
  mockMode: "APPROVE",
  requestedAt: SERVER_NOW,
  completedAt: null,
  failureCode: null,
};

export function errorBody(code: string, message: string, details: { field: string; reason: string }[] = []) {
  return { code, message, traceId: "trace_test", details };
}
