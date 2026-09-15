import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { lastRequest, requestLog } from "../mocks/handlers";
import { queryWrapper, setActorCookie, setupMockServer } from "../mocks/test-utils";
import { useBulkCreateInventory, useCreateRate, useInventories, useUpdateInventory, useUpdateRate } from "./inventory";

setupMockServer();

async function run<T>(fn: () => Promise<T>): Promise<T | unknown> {
  let out: unknown;
  await act(async () => {
    out = await fn().catch((e: unknown) => e);
  });
  return out;
}

describe("W12 요금 본문", () => {
  it("useUpdateRate는 version과 amount만 보낸다. currency 없음", async () => {
    setActorCookie("host_001");
    const { Wrapper } = queryWrapper();
    const { result } = renderHook(() => useUpdateRate("rt_001"), { wrapper: Wrapper });
    await run(() => result.current.mutateAsync({ date: "2026-10-01", version: 1, amount: 130000 }));
    expect(lastRequest().method).toBe("PATCH");
    expect(lastRequest().path).toBe("/api/v1/room-types/rt_001/rates/2026-10-01");
    expect(Object.keys(lastRequest().body as object).sort()).toEqual(["amount", "version"]);
    expect(lastRequest().body).toEqual({ version: 1, amount: 130000 });
  });

  it("useCreateRate는 date와 amount에 currency KRW를 붙인다", async () => {
    setActorCookie("host_001");
    const { Wrapper } = queryWrapper();
    const { result } = renderHook(() => useCreateRate("rt_001"), { wrapper: Wrapper });
    await run(() => result.current.mutateAsync({ date: "2026-10-01", amount: 120000 }));
    expect(lastRequest().method).toBe("POST");
    expect(lastRequest().path).toBe("/api/v1/room-types/rt_001/rates");
    expect(lastRequest().body).toEqual({ date: "2026-10-01", amount: 120000, currency: "KRW" });
  });
});

describe("재고 본문", () => {
  it("useUpdateInventory는 date를 경로로 보내고 본문은 version과 totalCount", async () => {
    setActorCookie("host_001");
    const { Wrapper } = queryWrapper();
    const { result } = renderHook(() => useUpdateInventory("rt_001"), { wrapper: Wrapper });
    await run(() => result.current.mutateAsync({ date: "2026-10-01", version: 3, totalCount: 6 }));
    expect(lastRequest().path).toBe("/api/v1/room-types/rt_001/inventories/2026-10-01");
    expect(lastRequest().body).toEqual({ version: 3, totalCount: 6 });
  });

  it("useBulkCreateInventory는 bulk 경로로 from과 to와 totalCount를 보내고 기간 조회를 다시 읽는다", async () => {
    setActorCookie("host_001");
    const { Wrapper } = queryWrapper();
    const range = renderHook(() => useInventories("rt_001", "2026-10-01", "2026-10-03"), { wrapper: Wrapper });
    await waitFor(() => expect(range.result.current.isSuccess).toBe(true));
    expect(range.result.current.data?.missingDates).toEqual(["2026-10-03"]);
    expect(lastRequest().path).toBe("/api/v1/room-types/rt_001/inventories?from=2026-10-01&to=2026-10-03");

    const bulk = renderHook(() => useBulkCreateInventory("rt_001"), { wrapper: Wrapper });
    await run(() => bulk.result.current.mutateAsync({ from: "2026-10-01", to: "2026-10-03", totalCount: 5 }));
    const bulkRequest = requestLog.find((r) => r.path.endsWith("/inventories/bulk"));
    expect(bulkRequest?.body).toEqual({ from: "2026-10-01", to: "2026-10-03", totalCount: 5 });
    await waitFor(() => expect(requestLog.filter((r) => r.method === "GET").length).toBe(2));
  });
});
