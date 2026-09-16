import { HttpResponse, http, type HttpHandler, type JsonBodyType } from "msw";
import * as f from "./fixtures";

// API 32개의 기본 핸들러. 조회는 본보기 응답, 쓰기는 받은 본문을 본보기에 얹어 돌려준다.
// 오류 분기는 테스트가 server.use로 덮어쓴다. 요청은 requestLog에 남겨 헤더와 본문을 대조한다

export interface LoggedRequest {
  method: string;
  path: string;
  headers: Record<string, string>;
  body: unknown;
}

export const requestLog: LoggedRequest[] = [];

export function clearRequestLog(): void {
  requestLog.length = 0;
}

export function lastRequest(): LoggedRequest {
  const last = requestLog[requestLog.length - 1];
  if (!last) throw new Error("기록된 요청이 없다");
  return last;
}

async function log(request: Request): Promise<unknown> {
  const url = new URL(request.url);
  const headers: Record<string, string> = {};
  request.headers.forEach((v, k) => {
    headers[k] = v;
  });
  const text = await request.clone().text();
  let body: unknown = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }
  requestLog.push({ method: request.method, path: url.pathname + url.search, headers, body });
  return body;
}

const B = "*/api/v1";

function json(data: JsonBodyType, init?: { status?: number; headers?: Record<string, string> }) {
  return HttpResponse.json(data, { status: init?.status ?? 200, headers: init?.headers });
}

function obj(body: unknown): Record<string, unknown> {
  return body && typeof body === "object" ? (body as Record<string, unknown>) : {};
}

export const handlers: HttpHandler[] = [
  // CAT
  http.post(`${B}/properties`, async ({ request }) => json({ ...f.property, ...obj(await log(request)), id: "prop_new" }, { status: 201, headers: { Location: "/api/v1/properties/prop_new" } })),
  http.get(`${B}/properties/:propertyId/room-types`, async ({ request }) => { await log(request); return json(f.page([f.roomType])); }),
  http.post(`${B}/properties/:propertyId/room-types`, async ({ request, params }) => json({ ...f.roomType, ...obj(await log(request)), id: "rt_new", propertyId: String(params.propertyId) }, { status: 201 })),
  http.get(`${B}/properties/:propertyId`, async ({ request, params }) => { await log(request); return json({ ...f.property, id: String(params.propertyId) }); }),
  http.patch(`${B}/properties/:propertyId`, async ({ request, params }) => { const body = obj(await log(request)); return json({ ...f.property, ...body, id: String(params.propertyId), version: Number(body.version ?? 0) + 1 }); }),
  http.get(`${B}/properties`, async ({ request }) => { await log(request); return json(f.page([f.property])); }),
  http.get(`${B}/host/properties`, async ({ request }) => { await log(request); return json(f.page([f.property])); }),
  http.get(`${B}/room-types/:roomTypeId`, async ({ request, params }) => { await log(request); return json({ ...f.roomType, id: String(params.roomTypeId) }); }),
  http.patch(`${B}/room-types/:roomTypeId`, async ({ request, params }) => { const body = obj(await log(request)); return json({ ...f.roomType, ...body, id: String(params.roomTypeId), version: Number(body.version ?? 0) + 1 }); }),

  // INV
  http.post(`${B}/room-types/:roomTypeId/inventories/bulk`, async ({ request }) => { const body = obj(await log(request)); return json({ ...f.inventoryRange, from: body.from, to: body.to }, { status: 201 }); }),
  http.post(`${B}/room-types/:roomTypeId/inventories`, async ({ request }) => json({ ...f.inventory, ...obj(await log(request)), heldCount: 0, soldCount: 0, version: 0 }, { status: 201 })),
  http.get(`${B}/room-types/:roomTypeId/inventories/:date`, async ({ request, params }) => { await log(request); return json({ ...f.inventory, date: String(params.date) }); }),
  http.patch(`${B}/room-types/:roomTypeId/inventories/:date`, async ({ request, params }) => { const body = obj(await log(request)); return json({ ...f.inventory, ...body, date: String(params.date), version: Number(body.version ?? 0) + 1 }); }),
  http.get(`${B}/room-types/:roomTypeId/inventories`, async ({ request }) => { await log(request); return json(f.inventoryRange); }),

  // RATE
  http.post(`${B}/room-types/:roomTypeId/rates`, async ({ request }) => json({ ...f.rate, ...obj(await log(request)), version: 0 }, { status: 201 })),
  http.get(`${B}/room-types/:roomTypeId/rates/:date`, async ({ request, params }) => { await log(request); return json({ ...f.rate, date: String(params.date) }); }),
  http.patch(`${B}/room-types/:roomTypeId/rates/:date`, async ({ request, params }) => { const body = obj(await log(request)); return json({ ...f.rate, ...body, date: String(params.date), version: Number(body.version ?? 0) + 1 }); }),
  http.get(`${B}/room-types/:roomTypeId/rates`, async ({ request }) => { await log(request); return json(f.rateRange); }),

  // PROMO
  http.post(`${B}/promotions`, async ({ request }) => json({ ...f.promotion, ...obj(await log(request)), id: "promo_new" }, { status: 201 })),
  http.get(`${B}/promotions/:promotionId`, async ({ request, params }) => { await log(request); return json({ ...f.promotion, id: String(params.promotionId) }); }),
  http.patch(`${B}/promotions/:promotionId`, async ({ request, params }) => { const body = obj(await log(request)); return json({ ...f.promotion, ...body, id: String(params.promotionId), version: Number(body.version ?? 0) + 1 }); }),
  http.get(`${B}/promotions`, async ({ request }) => { await log(request); return json(f.page([f.promotion])); }),
  http.get(`${B}/room-types/:roomTypeId/applicable-promotions`, async ({ request }) => { await log(request); return json(f.applicablePromotions); }),

  // SEARCH
  http.get(`${B}/search/properties`, async ({ request }) => { await log(request); return json(f.page([f.searchResult])); }),
  http.get(`${B}/room-types/:roomTypeId/availability`, async ({ request }) => { await log(request); return json(f.availability); }),
  http.get(`${B}/room-types/:roomTypeId/price-quote`, async ({ request }) => { await log(request); return json(f.quote); }),

  // BOOK, PAY
  http.post(`${B}/bookings/:bookingId/payment-attempts`, async ({ request, params }) => { await log(request); return json({ ...f.requestedAttempt, bookingId: String(params.bookingId) }, { status: 202, headers: { Location: `/api/v1/bookings/${String(params.bookingId)}/payment-attempts` } }); }),
  http.get(`${B}/bookings/:bookingId/payment-attempts`, async ({ request, params }) => { await log(request); return json({ bookingId: String(params.bookingId), attemptCount: 0, items: [] }); }),
  http.post(`${B}/bookings/:bookingId/cancellations`, async ({ request, params }) => { const body = obj(await log(request)); return json({ ...f.heldBooking, id: String(params.bookingId), status: "CANCELED", cancellationReason: body.reason ?? null, canceledAt: f.SERVER_NOW }); }),
  http.post(`${B}/bookings`, async ({ request }) => { const body = obj(await log(request)); return json({ ...f.heldBooking, ...body }, { status: 201, headers: { Location: `/api/v1/bookings/${f.heldBooking.id}` } }); }),
  http.get(`${B}/bookings/:bookingId`, async ({ request, params }) => { await log(request); return json({ ...f.heldBooking, id: String(params.bookingId) }); }),
  http.get(`${B}/bookings`, async ({ request }) => { await log(request); return json(f.page([f.heldBooking])); }),
];
