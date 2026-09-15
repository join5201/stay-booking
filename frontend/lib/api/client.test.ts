import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import { ApiError, NetworkError, api } from "./client";
import { errorBody, heldBooking } from "./mocks/fixtures";
import { lastRequest } from "./mocks/handlers";
import { server } from "./mocks/server";
import { setActorCookie, setupMockServer } from "./mocks/test-utils";
import { serverNowIso } from "./server-clock";

setupMockServer();

describe("W01 행위자 헤더", () => {
  it("쿠키 dev_actor를 X-Dev-Actor-Id로 붙인다", async () => {
    setActorCookie("host_001");
    await api("/host/properties");
    expect(lastRequest().headers["x-dev-actor-id"]).toBe("host_001");
  });

  it("public이면 헤더를 생략한다", async () => {
    setActorCookie("public");
    await api("/properties/prop_001");
    expect(lastRequest().headers["x-dev-actor-id"]).toBeUndefined();
  });

  it("쿠키가 없어도 헤더를 생략한다", async () => {
    await api("/properties/prop_001");
    expect(lastRequest().headers["x-dev-actor-id"]).toBeUndefined();
  });

  it("멱등키는 인자로 받아 Idempotency-Key로 보낸다", async () => {
    setActorCookie("guest_001");
    await api("/bookings", { method: "POST", body: { roomTypeId: "rt_001" }, idempotencyKey: "key-abc-12345" });
    expect(lastRequest().headers["idempotency-key"]).toBe("key-abc-12345");
    expect(lastRequest().headers["content-type"]).toBe("application/json");
  });
});

describe("W02 Idempotency-Replayed", () => {
  it.each([200, 201, 202])("%d에 헤더 true면 replayed가 true이고 성공", async (status) => {
    server.use(http.post("*/api/v1/bookings", () => HttpResponse.json(heldBooking, { status, headers: { "Idempotency-Replayed": "true" } })));
    const result = await api("/bookings", { method: "POST", body: {}, idempotencyKey: "k" });
    expect(result.status).toBe(status);
    expect(result.replayed).toBe(true);
    expect(result.data).toEqual(heldBooking);
  });

  it("헤더가 없으면 replayed가 false", async () => {
    const result = await api("/bookings", { method: "POST", body: {}, idempotencyKey: "k" });
    expect(result.replayed).toBe(false);
  });
});

describe("W03 오류 본문", () => {
  it("ApiError(status, code, message, traceId, details)로 바꾸고 details의 field로 필드 오류를 찾는다", async () => {
    server.use(
      http.post("*/api/v1/properties", () =>
        HttpResponse.json(errorBody("INVALID_REQUEST", "입력값이 잘못됐다", [{ field: "name", reason: "이름은 1자 이상 100자 이하" }, { field: "", reason: "본문을 읽지 못했다" }]), { status: 400 }),
      ),
    );
    const error = await api("/properties", { method: "POST", body: {} }).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(400);
    expect(apiError.code).toBe("INVALID_REQUEST");
    expect(apiError.message).toBe("입력값이 잘못됐다");
    expect(apiError.traceId).toBe("trace_test");
    expect(apiError.details).toEqual([{ field: "name", reason: "이름은 1자 이상 100자 이하" }, { field: "", reason: "본문을 읽지 못했다" }]);
    expect(apiError.fieldError("name")).toBe("이름은 1자 이상 100자 이하");
    expect(apiError.fieldError("address")).toBeUndefined();
    expect("fieldErrors" in apiError).toBe(false);
  });

  it("본문이 JSON이 아니면 HTTP_ERROR와 상태 코드", async () => {
    server.use(http.get("*/api/v1/properties/:id", () => new HttpResponse("<html>", { status: 502, headers: { "Content-Type": "text/html" } })));
    const error = (await api("/properties/x").catch((e: unknown) => e)) as ApiError;
    expect(error.status).toBe(502);
    expect(error.code).toBe("HTTP_ERROR");
  });

  it("fetch가 실패하면 NetworkError", async () => {
    server.use(http.get("*/api/v1/properties/:id", () => HttpResponse.error()));
    const error = await api("/properties/x").catch((e: unknown) => e);
    expect(error).toBeInstanceOf(NetworkError);
  });
});

describe("W04 Retry-After", () => {
  it("409 REQUEST_IN_PROGRESS에서만 retryAfterSec으로 읽는다", async () => {
    server.use(http.post("*/api/v1/bookings", () => HttpResponse.json(errorBody("REQUEST_IN_PROGRESS", "처리 중"), { status: 409, headers: { "Retry-After": "1" } })));
    const error = (await api("/bookings", { method: "POST", body: {}, idempotencyKey: "k" }).catch((e: unknown) => e)) as ApiError;
    expect(error.code).toBe("REQUEST_IN_PROGRESS");
    expect(error.retryAfterSec).toBe(1);
  });

  it("헤더 없는 503에서 초 카운트가 없다", async () => {
    server.use(http.post("*/api/v1/bookings", () => HttpResponse.json(errorBody("TEMPORARY_FAILURE", "일시 실패"), { status: 503 })));
    const error = (await api("/bookings", { method: "POST", body: {}, idempotencyKey: "k" }).catch((e: unknown) => e)) as ApiError;
    expect(error.status).toBe(503);
    expect(error.retryAfterSec).toBeNull();
  });

  it("헤더가 있어도 5xx에는 초 카운트를 기대하지 않는다", async () => {
    server.use(http.post("*/api/v1/bookings", () => HttpResponse.json(errorBody("INTERNAL_ERROR", "오류"), { status: 500, headers: { "Retry-After": "30" } })));
    const error = (await api("/bookings", { method: "POST", body: {}, idempotencyKey: "k" }).catch((e: unknown) => e)) as ApiError;
    expect(error.retryAfterSec).toBeNull();
  });
});

describe("W05 serverNow", () => {
  it("본문 serverNow가 우선이다. 둘 다 있는 응답에서 본문 값", async () => {
    server.use(http.get("*/api/v1/bookings/:id", () => HttpResponse.json({ ...heldBooking, serverNow: "2026-09-15T10:00:00.000Z" }, { headers: { Date: "Tue, 15 Sep 2026 12:00:00 GMT" } })));
    const result = await api("/bookings/bk_001");
    expect(result.serverNow).toBe("2026-09-15T10:00:00.000Z");
    expect(serverNowIso()?.slice(0, 16)).toBe("2026-09-15T10:00");
  });

  it("본문에 없으면 Date 헤더", async () => {
    server.use(http.get("*/api/v1/properties/:id", () => HttpResponse.json({ id: "prop_001" }, { headers: { Date: "Tue, 15 Sep 2026 12:00:00 GMT" } })));
    const result = await api("/properties/prop_001");
    expect(result.serverNow).toBe("2026-09-15T12:00:00.000Z");
  });

  it("둘 다 없으면 null", async () => {
    server.use(http.get("*/api/v1/properties/:id", () => HttpResponse.json({ id: "prop_001" })));
    const result = await api("/properties/prop_001");
    expect(result.serverNow).toBeNull();
  });
});

describe("쿼리 문자열", () => {
  it("비어 있거나 undefined인 값은 빼고 나머지를 붙인다", async () => {
    await api("/search/properties", { query: { regionCode: "SEOUL", checkIn: "2026-10-01", checkOut: "2026-10-03", guestCount: 2, page: undefined, size: "" } });
    expect(lastRequest().path).toBe("/api/v1/search/properties?regionCode=SEOUL&checkIn=2026-10-01&checkOut=2026-10-03&guestCount=2");
  });
});
