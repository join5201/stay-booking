import { act, renderHook, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import { ApiError } from "../client";
import { keys } from "../keys";
import { errorBody, property } from "../mocks/fixtures";
import { lastRequest, requestLog } from "../mocks/handlers";
import { server } from "../mocks/server";
import { queryWrapper, setActorCookie, setupMockServer } from "../mocks/test-utils";
import { useCreateProperty, useMyProperties, useProperty, useUpdateProperty } from "./catalog";

setupMockServer();

async function run<T>(fn: () => Promise<T>): Promise<T | unknown> {
  let out: unknown;
  await act(async () => {
    out = await fn().catch((e: unknown) => e);
  });
  return out;
}

describe("조회 훅", () => {
  it("useProperty는 본보기 응답을 data로 내고 id가 없으면 부르지 않는다", async () => {
    const { Wrapper } = queryWrapper();
    const idle = renderHook(() => useProperty(undefined), { wrapper: Wrapper });
    expect(idle.result.current.fetchStatus).toBe("idle");

    const { result } = renderHook(() => useProperty("prop_001"), { wrapper: Wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(property);
    expect(requestLog).toHaveLength(1);
    expect(lastRequest().path).toBe("/api/v1/properties/prop_001");
  });

  it("useMyProperties는 page와 size를 쿼리로 붙이고 오류는 ApiError로 낸다", async () => {
    setActorCookie("host_001");
    const { Wrapper } = queryWrapper();
    const { result } = renderHook(() => useMyProperties({ page: 1, size: 10 }), { wrapper: Wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(lastRequest().path).toBe("/api/v1/host/properties?page=1&size=10");
    expect(result.current.data?.items[0]?.id).toBe("prop_001");

    server.use(http.get("*/api/v1/host/properties", () => HttpResponse.json(errorBody("ACTOR_REQUIRED", "행위자 필요"), { status: 401 })));
    const denied = renderHook(() => useMyProperties({ page: 2 }), { wrapper: Wrapper });
    await waitFor(() => expect(denied.result.current.isError).toBe(true));
    expect(denied.result.current.error).toBeInstanceOf(ApiError);
    expect((denied.result.current.error as ApiError).code).toBe("ACTOR_REQUIRED");
  });
});

describe("쓰기 훅과 캐시", () => {
  it("useCreateProperty는 결과를 캐시에 넣고 내 숙소 목록을 다시 읽는다", async () => {
    setActorCookie("host_001");
    const { Wrapper, client } = queryWrapper();
    const list = renderHook(() => useMyProperties(), { wrapper: Wrapper });
    await waitFor(() => expect(list.result.current.isSuccess).toBe(true));

    const create = renderHook(() => useCreateProperty(), { wrapper: Wrapper });
    await run(() => create.result.current.mutateAsync({ name: "새 숙소", regionCode: "BUSAN", address: "부산 어딘가" }));
    expect(requestLog.find((r) => r.method === "POST")?.body).toEqual({ name: "새 숙소", regionCode: "BUSAN", address: "부산 어딘가" });
    expect(client.getQueryData(keys.property("prop_new"))).toMatchObject({ id: "prop_new", name: "새 숙소", regionCode: "BUSAN" });
    await waitFor(() => expect(requestLog.filter((r) => r.method === "GET").length).toBe(2));
  });

  it("useUpdateProperty는 version과 바뀐 필드만 보내고 응답을 캐시에 덮어쓴다", async () => {
    setActorCookie("host_001");
    const { Wrapper, client } = queryWrapper();
    const read = renderHook(() => useProperty("prop_001"), { wrapper: Wrapper });
    // data를 먼저 읽어 둔다. useQuery는 읽힌 속성만 추적해 다시 그린다
    await waitFor(() => expect(read.result.current.data?.name).toBe("한강 뷰 스테이"));

    const update = renderHook(() => useUpdateProperty("prop_001"), { wrapper: Wrapper });
    await run(() => update.result.current.mutateAsync({ version: 0, name: "바뀐 이름" }));
    expect(lastRequest().method).toBe("PATCH");
    expect(lastRequest().body).toEqual({ version: 0, name: "바뀐 이름" });
    expect(client.getQueryData(keys.property("prop_001"))).toMatchObject({ name: "바뀐 이름", version: 1 });
    await waitFor(() => expect(read.result.current.data?.name).toBe("바뀐 이름"));
  });
});
