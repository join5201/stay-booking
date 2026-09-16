import { randomUUID } from "node:crypto";
import { expect, type APIRequestContext, type BrowserContext, type Page } from "@playwright/test";

// E2E 공통. 시험 코드가 부르는 API는 프론트의 rewrites를 지나 같은 백엔드로 간다(계약 2-1절 rewrites).
// 행위자는 쿠키 dev_actor(화면)와 헤더 X-Dev-Actor-Id(시험 코드의 직접 호출) 둘 다 개발용 값이다

export const BASE = process.env.E2E_BASE_URL ?? "http://localhost:3000";
// INTERNAL-01은 /api/v1 밖이라 프론트의 rewrites를 안 지난다. 백엔드 주소로 직접 부른다
export const BACKEND = process.env.E2E_BACKEND_URL ?? "http://localhost:8080";
export const MOCK_SYSTEM = "mock_001";
export const HOST = "host_001";
export const GUEST = "guest_001";
export type Actor = "public" | "guest_001" | "guest_002" | "host_001" | "host_002" | "operator_001" | "mock_001";
export type MockMode = "APPROVE" | "DECLINE" | "DEFER";

export const newKey = () => randomUUID();

// 서울 오늘부터 n일 뒤. 숙박 날짜는 서울 날짜 문자열이다(P03)
export function seoulDate(offsetDays: number): string {
  const now = new Date(Date.now() + offsetDays * 86_400_000);
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit" }).formatToParts(now);
  const get = (t: string) => parts.find((p) => p.type === t)?.value ?? "";
  return `${get("year")}-${get("month")}-${get("day")}`;
}

export interface ApiOptions {
  method?: "GET" | "POST" | "PATCH";
  base?: string;
  actor?: Actor;
  body?: unknown;
  idempotencyKey?: string;
}

export interface ApiResult<T> {
  status: number;
  data: T;
}

export async function api<T = Record<string, unknown>>(request: APIRequestContext, path: string, options: ApiOptions = {}): Promise<ApiResult<T>> {
  const headers: Record<string, string> = {};
  if (options.actor && options.actor !== "public") headers["X-Dev-Actor-Id"] = options.actor;
  if (options.idempotencyKey) headers["Idempotency-Key"] = options.idempotencyKey;
  const res = await request.fetch(`${options.base ?? BASE}${path}`, {
    method: options.method ?? "GET",
    headers,
    data: options.body === undefined ? undefined : (options.body as Record<string, unknown>),
  });
  const text = await res.text();
  return { status: res.status(), data: (text ? JSON.parse(text) : null) as T };
}

export async function setActor(context: BrowserContext, actor: Actor, mockMode: MockMode = "APPROVE") {
  await context.clearCookies();
  await context.addCookies([
    { name: "dev_actor", value: actor, url: BASE },
    { name: "dev_mock_mode", value: mockMode, url: BASE },
  ]);
}

export interface Stay {
  propertyId: string;
  propertyName: string;
  roomTypeId: string;
  roomTypeName: string;
  regionCode: string;
  checkIn: string;
  checkOut: string;
  nights: number;
  nightDates: string[];
  total: number;
  amountPerNight: number;
}

// 호스트가 API로 숙소와 객실과 재고와 요금을 만든다(E02부터 E06의 준비. E01은 화면으로 만든다)
export async function seedStay(request: APIRequestContext, tag: string, options: { checkInOffset?: number; nights?: number; total?: number; amount?: number } = {}): Promise<Stay> {
  const checkInOffset = options.checkInOffset ?? 10;
  const nights = options.nights ?? 2;
  const total = options.total ?? 3;
  const amount = options.amount ?? 100_000;
  const checkIn = seoulDate(checkInOffset);
  const checkOut = seoulDate(checkInOffset + nights);
  // 이름은 실행마다 다르게. 같은 DB에 여러 번 돌려도 화면의 이름 찾기가 하나만 잡는다
  const stamp = Date.now().toString(36);

  const property = await api<{ id: string; name: string }>(request, "/api/v1/properties", { method: "POST", actor: HOST, body: { name: `E2E ${tag} ${stamp} 스테이`, regionCode: "BUSAN", address: "부산 해운대구 E2E로 1", description: `E2E ${tag}` } });
  expect(property.status, "CAT-01").toBe(201);
  const roomType = await api<{ id: string; name: string }>(request, `/api/v1/properties/${property.data.id}/room-types`, { method: "POST", actor: HOST, body: { name: `E2E ${tag} ${stamp} 더블`, maxOccupancy: 2, description: "E2E" } });
  expect(roomType.status, "CAT-06").toBe(201);
  const bulk = await api(request, `/api/v1/room-types/${roomType.data.id}/inventories/bulk`, { method: "POST", actor: HOST, body: { from: checkIn, to: checkOut, totalCount: total } });
  // INV-02의 to는 제외 경계(계약 2-1절, T5 결정 첫째)
  expect(bulk.status, "INV-02").toBe(201);
  const nightDates = Array.from({ length: nights }, (_, i) => seoulDate(checkInOffset + i));
  for (const date of nightDates) {
    const rate = await api(request, `/api/v1/room-types/${roomType.data.id}/rates`, { method: "POST", actor: HOST, body: { date, amount, currency: "KRW" } });
    expect(rate.status, "RATE-01").toBe(201);
  }
  return { propertyId: property.data.id, propertyName: property.data.name, roomTypeId: roomType.data.id, roomTypeName: roomType.data.name, regionCode: "BUSAN", checkIn, checkOut, nights, nightDates, total, amountPerNight: amount };
}

export interface Inventory {
  totalCount: number;
  heldCount: number;
  soldCount: number;
  availableCount: number;
}

// INV-05. 재고 반환과 soldCount 증감의 근거
export async function inventoryOf(request: APIRequestContext, roomTypeId: string, date: string): Promise<Inventory> {
  const res = await api<Inventory>(request, `/api/v1/room-types/${roomTypeId}/inventories/${date}`, { actor: HOST });
  expect(res.status, "INV-05").toBe(200);
  return res.data;
}

export interface BookingView {
  id: string;
  status: string;
  expirationReason: string | null;
  payment: { attemptCount: number; attempts: { id: string; status: string; pgTransactionId: string; amount: number; currency: string }[]; refund: { amount: number } | null };
  version: number;
}

export async function bookingOf(request: APIRequestContext, bookingId: string): Promise<BookingView> {
  const res = await api<BookingView>(request, `/api/v1/bookings/${bookingId}`, { actor: GUEST });
  expect(res.status, "BOOK-03").toBe(200);
  return res.data;
}

// 게스트가 API로 선점을 만든다(BOOK-01). 화면 흐름이 아닌 시험의 준비
export async function holdBooking(request: APIRequestContext, stay: Stay, guestCount = 2): Promise<BookingView> {
  const quote = await api<{ price: { totalAmount: number } }>(request, `/api/v1/room-types/${stay.roomTypeId}/price-quote?checkIn=${stay.checkIn}&checkOut=${stay.checkOut}&guestCount=${guestCount}`, { actor: GUEST });
  expect(quote.status, "SEARCH-03").toBe(200);
  const res = await api<BookingView>(request, "/api/v1/bookings", {
    method: "POST",
    actor: GUEST,
    idempotencyKey: newKey(),
    body: { roomTypeId: stay.roomTypeId, checkIn: stay.checkIn, checkOut: stay.checkOut, guestCount, expectedTotalAmount: quote.data.price.totalAmount, currency: "KRW" },
  });
  expect(res.status, "BOOK-01").toBe(201);
  return res.data;
}

export const g1Query = (stay: Stay, guestCount = 2) => `?regionCode=${stay.regionCode}&checkIn=${stay.checkIn}&checkOut=${stay.checkOut}&guestCount=${guestCount}`;

// G3에서 예약하기, G4에서 확인, G5 도착. 예약 id를 URL에서 읽는다
export async function reserveThroughScreens(page: Page, stay: Stay, guestCount = 2): Promise<string> {
  await page.goto(`/room-types/${stay.roomTypeId}${g1Query(stay, guestCount)}`);
  return confirmFromG3(page);
}

// G3에 서 있는 상태에서 G4를 거쳐 G5까지. E01은 G1 카드로 G3에 오므로 여기서 이어진다
export async function confirmFromG3(page: Page): Promise<string> {
  await expect(page.getByText("예상 금액이며 확정 금액이 아닙니다.")).toBeVisible();
  await page.getByRole("button", { name: "예약하기" }).click();
  await expect(page.getByRole("heading", { name: "예약 확인" })).toBeVisible();
  await page.getByRole("button", { name: /에 예약 확인$/ }).click();
  await expect(page).toHaveURL(/\/bookings\/[^/]+\/pay$/);
  await expect(page.getByRole("heading", { name: "결제", exact: true })).toBeVisible();
  const match = page.url().match(/\/bookings\/([^/]+)\/pay$/);
  if (!match) throw new Error(`예약 id 없음: ${page.url()}`);
  return match[1];
}

// 화면이 부른 BOOK-03 요청을 센다. E04의 근거(화면이 스스로 만료로 바꾸지 않았음)
export function trackBookingReads(page: Page, bookingId: string) {
  const responses: { status: number; bookingStatus: string | null; at: number }[] = [];
  page.on("response", (res) => {
    const url = res.url();
    if (res.request().method() === "GET" && url.endsWith(`/api/v1/bookings/${bookingId}`)) {
      void res
        .json()
        .then((j: { status?: string }) => responses.push({ status: res.status(), bookingStatus: j.status ?? null, at: Date.now() }))
        .catch(() => responses.push({ status: res.status(), bookingStatus: null, at: Date.now() }));
    }
  });
  return responses;
}
