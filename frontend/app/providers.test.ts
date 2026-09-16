import { describe, expect, it } from "vitest";
import { ApiError, NetworkError } from "@/lib/api/client";
import { retryQuery } from "./providers";

describe("조회 재시도 규칙", () => {
  it("4xx는 재시도하지 않고 5xx와 네트워크는 한 번만", () => {
    expect(retryQuery(0, new ApiError({ status: 404, code: "RESOURCE_NOT_FOUND", message: "" }))).toBe(false);
    expect(retryQuery(0, new ApiError({ status: 401, code: "ACTOR_REQUIRED", message: "" }))).toBe(false);
    expect(retryQuery(0, new ApiError({ status: 500, code: "INTERNAL_ERROR", message: "" }))).toBe(true);
    expect(retryQuery(1, new ApiError({ status: 500, code: "INTERNAL_ERROR", message: "" }))).toBe(false);
    expect(retryQuery(0, new NetworkError(null))).toBe(true);
    expect(retryQuery(1, new NetworkError(null))).toBe(false);
  });
});
