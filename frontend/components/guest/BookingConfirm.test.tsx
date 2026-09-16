import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { HttpResponse, delay, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { errorBody, heldBooking, quote, roomType } from "@/lib/api/mocks/fixtures";
import { requestLog } from "@/lib/api/mocks/handlers";
import { server } from "@/lib/api/mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "@/lib/api/mocks/test-utils";
import { BannerProvider } from "../Banner";
import type { SearchValues } from "../SearchForm";
import { BookingConfirm } from "./BookingConfirm";

setupMockServer();
afterEach(() => cleanup());

const STAY: SearchValues = { regionCode: "SEOUL", checkIn: "2026-10-01", checkOut: "2026-10-03", guestCount: 2 };

function renderConfirm() {
  setActorCookie("guest_001");
  const onHeld = vi.fn();
  const onBack = vi.fn();
  const { Wrapper } = queryWrapper();
  render(
    <Wrapper>
      <BannerProvider>
        <BookingConfirm roomTypeId={roomType.id} values={STAY} onHeld={onHeld} onBack={onBack} />
      </BannerProvider>
    </Wrapper>,
  );
  return { onHeld, onBack };
}

const confirmButton = () => screen.getByRole("button", { name: /에 예약 확인$/ }) as HTMLButtonElement;

describe("G4 예약 확인", () => {
  it("진입 시 SEARCH-03을 다시 받아 그 값을 expectedTotalAmount로. 확인이 BOOK-01이고 201이면 G5", async () => {
    let seenBody: unknown = null;
    let seenKey = "";
    server.use(
      http.post("*/api/v1/bookings", async ({ request }) => {
        seenBody = await request.json();
        seenKey = request.headers.get("idempotency-key") ?? "";
        await delay(150);
        return HttpResponse.json(heldBooking, { status: 201 });
      }),
    );
    const { onHeld } = renderConfirm();
    await screen.findByText("예상 금액이며 확정 금액이 아닙니다.");
    expect(requestLog.some((r) => r.path === `/api/v1/room-types/${roomType.id}/price-quote?checkIn=2026-10-01&checkOut=2026-10-03&guestCount=2`)).toBe(true);
    expect(confirmButton().textContent).toBe("216,000원에 예약 확인");

    fireEvent.click(confirmButton());
    // 요청 중 두 버튼 비활성
    await waitFor(() => expect(confirmButton().disabled).toBe(true));
    expect((screen.getByRole("button", { name: "돌아가기" }) as HTMLButtonElement).disabled).toBe(true);
    await waitFor(() => expect(onHeld).toHaveBeenCalledWith(heldBooking.id));
    expect(seenBody).toEqual({ roomTypeId: roomType.id, checkIn: "2026-10-01", checkOut: "2026-10-03", guestCount: 2, expectedTotalAmount: 216000, currency: "KRW" });
    expect(seenKey.length).toBeGreaterThanOrEqual(8);
  });

  it("W09. 네트워크 실패의 다시 시도는 같은 키", async () => {
    const keys: string[] = [];
    server.use(
      http.post("*/api/v1/bookings", ({ request }) => {
        keys.push(request.headers.get("idempotency-key") ?? "");
        return keys.length === 1 ? HttpResponse.error() : HttpResponse.json(heldBooking, { status: 201 });
      }),
    );
    const { onHeld } = renderConfirm();
    await screen.findByText("예상 금액이며 확정 금액이 아닙니다.");
    fireEvent.click(confirmButton());
    await screen.findByText("일시적인 오류입니다. 연결을 확인하고 다시 시도하세요.");
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    await waitFor(() => expect(onHeld).toHaveBeenCalledTimes(1));
    expect(keys).toHaveLength(2);
    expect(keys[1]).toBe(keys[0]);
  });

  it("W09. REQUEST_IN_PROGRESS는 Retry-After 뒤 같은 키, PRICE_CHANGED 뒤 다시 확인은 새 키와 새 금액", async () => {
    let reads = 0;
    const seen: { key: string; body: unknown }[] = [];
    server.use(
      http.get("*/api/v1/room-types/:id/price-quote", () => {
        reads += 1;
        return HttpResponse.json(reads === 1 ? quote : { ...quote, price: { ...quote.price, totalAmount: 230000, discountTotalAmount: 10000 } });
      }),
      http.post("*/api/v1/bookings", async ({ request }) => {
        const body = await request.json();
        seen.push({ key: request.headers.get("idempotency-key") ?? "", body });
        if (seen.length === 1) return HttpResponse.json(errorBody("REQUEST_IN_PROGRESS", "busy"), { status: 409, headers: { "Retry-After": "2" } });
        if (seen.length === 2) return HttpResponse.json(errorBody("PRICE_CHANGED", "changed"), { status: 409 });
        return HttpResponse.json(heldBooking, { status: 201 });
      }),
    );
    const { onHeld } = renderConfirm();
    await screen.findByText("예상 금액이며 확정 금액이 아닙니다.");
    fireEvent.click(confirmButton());
    await screen.findByText("같은 요청을 처리 중입니다. 잠시 뒤 다시 시도하세요.");
    // Retry-After 2초 동안 다시 시도가 잠긴다
    const retry = screen.getByRole("button", { name: "다시 시도" }) as HTMLButtonElement;
    expect(retry.disabled).toBe(true);
    await waitFor(() => expect(retry.disabled).toBe(false), { timeout: 4000 });
    fireEvent.click(retry);
    await screen.findByText("금액이 바뀌어 예약을 만들지 않았습니다. 새 금액으로 다시 확인하세요.");
    expect(seen[1].key).toBe(seen[0].key);
    expect(reads).toBe(2);
    await waitFor(() => expect(screen.getByRole("button", { name: "230,000원으로 다시 확인" })).toBeTruthy());
    expect(confirmButton().disabled).toBe(true);

    fireEvent.click(screen.getByRole("button", { name: "230,000원으로 다시 확인" }));
    await waitFor(() => expect(onHeld).toHaveBeenCalledTimes(1));
    expect(seen).toHaveLength(3);
    expect(seen[2].key).not.toBe(seen[0].key);
    expect((seen[2].body as { expectedTotalAmount: number }).expectedTotalAmount).toBe(230000);
  });

  it("INVENTORY_UNAVAILABLE 계열은 그사이 객실이 마감 Notice. 키 결함 코드는 새 키로 다시 시도", async () => {
    const keys: string[] = [];
    server.use(
      http.post("*/api/v1/bookings", ({ request }) => {
        keys.push(request.headers.get("idempotency-key") ?? "");
        if (keys.length === 1) return HttpResponse.json(errorBody("INVENTORY_UNAVAILABLE", "gone"), { status: 409 });
        if (keys.length === 2) return HttpResponse.json(errorBody("IDEMPOTENCY_KEY_REUSED", "reused"), { status: 409 });
        return HttpResponse.json(heldBooking, { status: 201 });
      }),
    );
    const { onHeld } = renderConfirm();
    await screen.findByText("예상 금액이며 확정 금액이 아닙니다.");
    fireEvent.click(confirmButton());
    await screen.findByText("그사이 객실이 마감됐습니다.");
    expect(onHeld).not.toHaveBeenCalled();

    fireEvent.click(confirmButton());
    await screen.findByText("요청 처리에 문제가 있습니다. 다시 시도하세요.");
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    await waitFor(() => expect(onHeld).toHaveBeenCalledTimes(1));
    expect(keys[1]).toBe(keys[0]);
    expect(keys[2]).not.toBe(keys[1]);
  });
});
