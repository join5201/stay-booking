import { act, renderHook, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import { ApiError } from "../client";
import { keys } from "../keys";
import { errorBody, heldBooking } from "../mocks/fixtures";
import { lastRequest, requestLog } from "../mocks/handlers";
import { server } from "../mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "../mocks/test-utils";
import type { RequestBookingBody } from "../types";
import { useBooking, useCancelBooking, useRequestBooking, useRequestPayment } from "./booking";

setupMockServer();

const body: RequestBookingBody = { roomTypeId: "rt_001", checkIn: "2026-10-01", checkOut: "2026-10-03", guestCount: 2, expectedTotalAmount: 216000, currency: "KRW" };

// mutateAsync를 act 안에서 돌리고 결과나 오류를 그대로 돌려준다
async function run<T>(fn: () => Promise<T>): Promise<T | unknown> {
  let out: unknown;
  await act(async () => {
    out = await fn().catch((e: unknown) => e);
  });
  return out;
}

describe("W09 멱등키 (T3 몫. 화면의 useRef 보관은 T7)", () => {
  it("useRequestBooking은 변수로 받은 키를 Idempotency-Key 헤더로 보내고 본문은 여섯 필드", async () => {
    setActorCookie("guest_001");
    const { Wrapper } = queryWrapper();
    const { result } = renderHook(() => useRequestBooking(), { wrapper: Wrapper });
    const res = (await run(() => result.current.mutateAsync({ idempotencyKey: "key-first", body }))) as Awaited<ReturnType<typeof result.current.mutateAsync>>;
    expect(res.status).toBe(201);
    expect(res.data.status).toBe("HELD");
    expect(lastRequest().headers["idempotency-key"]).toBe("key-first");
    expect(lastRequest().headers["x-dev-actor-id"]).toBe("guest_001");
    expect(lastRequest().body).toEqual(body);
  });

  it("실패 뒤 같은 키로 다시 시도하면 같은 헤더, 새 동작이면 새 헤더", async () => {
    setActorCookie("guest_001");
    // 덮어쓴 핸들러는 requestLog에 남지 않아 헤더를 따로 받는다
    const failedKeys: string[] = [];
    server.use(
      http.post(
        "*/api/v1/bookings",
        ({ request }) => {
          failedKeys.push(request.headers.get("idempotency-key") ?? "");
          return HttpResponse.json(errorBody("INTERNAL_ERROR", "오류"), { status: 500 });
        },
        { once: true },
      ),
    );
    const { Wrapper } = queryWrapper();
    const { result } = renderHook(() => useRequestBooking(), { wrapper: Wrapper });

    const failed = await run(() => result.current.mutateAsync({ idempotencyKey: "key-1", body }));
    expect(failed).toBeInstanceOf(ApiError);
    await run(() => result.current.mutateAsync({ idempotencyKey: "key-1", body }));
    await run(() => result.current.mutateAsync({ idempotencyKey: "key-2", body }));

    expect([...failedKeys, ...requestLog.map((r) => r.headers["idempotency-key"])]).toEqual(["key-1", "key-1", "key-2"]);
  });

  it("replayed 응답도 성공이고 캐시에 예약이 들어간다", async () => {
    server.use(http.post("*/api/v1/bookings", () => HttpResponse.json(heldBooking, { status: 201, headers: { "Idempotency-Replayed": "true", Location: "/api/v1/bookings/bk_001" } })));
    const { Wrapper, client } = queryWrapper();
    const { result } = renderHook(() => useRequestBooking(), { wrapper: Wrapper });
    const res = (await run(() => result.current.mutateAsync({ idempotencyKey: "key-r", body }))) as Awaited<ReturnType<typeof result.current.mutateAsync>>;
    expect(res.replayed).toBe(true);
    expect(client.getQueryData(keys.booking("bk_001"))).toEqual(heldBooking);
  });

  it("취소와 결제 요청도 키를 헤더로 보내고 예약 조회를 무효화한다", async () => {
    setActorCookie("guest_001");
    const { Wrapper, client } = queryWrapper();
    const booking = renderHook(() => useBooking("bk_001"), { wrapper: Wrapper });
    await waitFor(() => expect(booking.result.current.isSuccess).toBe(true));
    const readsBefore = requestLog.filter((r) => r.method === "GET").length;

    const pay = renderHook(() => useRequestPayment("bk_001"), { wrapper: Wrapper });
    await run(() => pay.result.current.mutateAsync({ idempotencyKey: "pay-key", body: { mockMode: "DECLINE" } }));
    const payRequest = requestLog.find((r) => r.path.endsWith("/payment-attempts"));
    expect(payRequest?.headers["idempotency-key"]).toBe("pay-key");
    expect(payRequest?.body).toEqual({ mockMode: "DECLINE" });

    const cancel = renderHook(() => useCancelBooking("bk_001"), { wrapper: Wrapper });
    await run(() => cancel.result.current.mutateAsync({ idempotencyKey: "cancel-key", body: { reason: "일정 변경" } }));
    const cancelRequest = requestLog.find((r) => r.path.endsWith("/cancellations"));
    expect(cancelRequest?.headers["idempotency-key"]).toBe("cancel-key");

    // 쓰기마다 BOOK-03을 다시 읽는다
    await waitFor(() => expect(requestLog.filter((r) => r.method === "GET" && r.path.endsWith("/bookings/bk_001")).length).toBeGreaterThanOrEqual(readsBefore + 2));
    expect(client.getQueryState(keys.booking("bk_001"))?.status).toBe("success");
  });
});
