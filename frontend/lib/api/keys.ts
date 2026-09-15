import type { PageParams, SearchParams, StayParams } from "./types";

// 쿼리 키. 앞부분이 같은 키를 한 번에 무효화한다(인계 문서 48행부터 73행의 invalidate 표)
export const keys = {
  property: (id: string) => ["property", id] as const,
  properties: (params: { regionCode?: string } & PageParams) => ["properties", params] as const,
  hostProperties: (params: PageParams) => ["host", "properties", params] as const,
  hostPropertiesAll: () => ["host", "properties"] as const,

  roomType: (id: string) => ["roomType", id] as const,
  roomTypes: (propertyId: string, params: PageParams) => ["roomTypes", propertyId, params] as const,
  roomTypesAll: (propertyId: string) => ["roomTypes", propertyId] as const,

  inventories: (roomTypeId: string, from: string, to: string) => ["inventories", roomTypeId, from, to] as const,
  inventoriesAll: (roomTypeId: string) => ["inventories", roomTypeId] as const,
  inventory: (roomTypeId: string, date: string) => ["inventory", roomTypeId, date] as const,
  inventoryAll: (roomTypeId: string) => ["inventory", roomTypeId] as const,

  rates: (roomTypeId: string, from: string, to: string) => ["rates", roomTypeId, from, to] as const,
  ratesAll: (roomTypeId: string) => ["rates", roomTypeId] as const,
  rate: (roomTypeId: string, date: string) => ["rate", roomTypeId, date] as const,
  rateAll: (roomTypeId: string) => ["rate", roomTypeId] as const,

  promotion: (id: string) => ["promotion", id] as const,
  promotions: (params: { enabled?: boolean } & PageParams) => ["promotions", params] as const,
  promotionsAll: () => ["promotions"] as const,
  applicablePromotions: (roomTypeId: string, params: StayParams) => ["applicablePromotions", roomTypeId, params] as const,

  search: (params: SearchParams & PageParams) => ["search", params] as const,
  availability: (roomTypeId: string, params: StayParams) => ["availability", roomTypeId, params] as const,
  quote: (roomTypeId: string, params: StayParams) => ["quote", roomTypeId, params] as const,

  booking: (id: string) => ["booking", id] as const,
  bookings: (params: { status?: string } & PageParams) => ["bookings", params] as const,
  bookingsAll: () => ["bookings"] as const,
  paymentAttempts: (bookingId: string) => ["paymentAttempts", bookingId] as const,
};
