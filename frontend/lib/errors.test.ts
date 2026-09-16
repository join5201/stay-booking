import { describe, expect, it } from "vitest";
import { ApiError, NetworkError } from "./api/client";
import { fieldErrorsOf, messageOf, placementOf, retryAfterSecOf, retryPolicyOf } from "./errors";

function apiError(code: string, status = 400, details: { field: string; reason: string }[] = [], retryAfterSec: number | null = null) {
  return new ApiError({ status, code, message: `서버 문구 ${code}`, traceId: "t", details, retryAfterSec });
}

describe("placement 셋", () => {
  it.each(["INVALID_REQUEST", "INVALID_DATE_RANGE", "INVENTORY_BELOW_COMMITTED", "OCCUPANCY_EXCEEDED"])("%s는 field", (code) => {
    expect(placementOf(apiError(code))).toBe("field");
  });

  it.each([
    "ACTOR_REQUIRED",
    "ACCESS_DENIED",
    "RESOURCE_NOT_FOUND",
    "VERSION_CONFLICT",
    "RESOURCE_ALREADY_EXISTS",
    "PRICE_CHANGED",
    "INVENTORY_UNAVAILABLE",
    "INVENTORY_NOT_CONFIGURED",
    "RATE_NOT_CONFIGURED",
    "BOOKING_STATE_CONFLICT",
    "BOOKING_EXPIRED",
    "CANCELLATION_NOT_ALLOWED",
    "PAYMENT_ATTEMPTS_EXHAUSTED",
    "PAYMENT_IN_PROGRESS",
  ])("%s는 notice", (code) => {
    expect(placementOf(apiError(code, 409))).toBe("notice");
  });

  it.each(["REQUEST_IN_PROGRESS", "IDEMPOTENCY_KEY_REQUIRED", "IDEMPOTENCY_KEY_REUSED", "TEMPORARY_FAILURE", "INTERNAL_ERROR"])("%s는 banner", (code) => {
    expect(placementOf(apiError(code, 500))).toBe("banner");
  });

  it("네트워크 오류와 모르는 5xx와 모르는 값은 banner", () => {
    expect(placementOf(new NetworkError(new Error("x")))).toBe("banner");
    expect(placementOf(apiError("HTTP_ERROR", 502))).toBe("banner");
    expect(placementOf(apiError("SOMETHING_NEW", 418))).toBe("banner");
    expect(placementOf(new Error("plain"))).toBe("banner");
    expect(placementOf(undefined)).toBe("banner");
  });
});

describe("다시 시도 규칙", () => {
  it("409 처리 중과 5xx와 네트워크는 같은 키, 금액 변경과 멱등키 오류는 새 키, 나머지는 없음", () => {
    expect(retryPolicyOf(apiError("REQUEST_IN_PROGRESS", 409, [], 1))).toBe("same-key");
    expect(retryPolicyOf(apiError("INTERNAL_ERROR", 500))).toBe("same-key");
    expect(retryPolicyOf(apiError("HTTP_ERROR", 503))).toBe("same-key");
    expect(retryPolicyOf(new NetworkError(null))).toBe("same-key");
    expect(retryPolicyOf(apiError("PRICE_CHANGED", 409))).toBe("new-key");
    expect(retryPolicyOf(apiError("IDEMPOTENCY_KEY_REUSED", 422))).toBe("new-key");
    expect(retryPolicyOf(apiError("VERSION_CONFLICT", 409))).toBe("none");
    expect(retryPolicyOf(apiError("INVALID_REQUEST"))).toBe("none");
  });

  it("초 카운트는 ApiError의 retryAfterSec 그대로, 그 밖은 null", () => {
    expect(retryAfterSecOf(apiError("REQUEST_IN_PROGRESS", 409, [], 1))).toBe(1);
    expect(retryAfterSecOf(apiError("INTERNAL_ERROR", 500))).toBeNull();
    expect(retryAfterSecOf(new NetworkError(null))).toBeNull();
  });
});

describe("문구", () => {
  it("서버 message를 그대로 내지 않는다", () => {
    expect(messageOf(apiError("VERSION_CONFLICT", 409))).toContain("다른 곳에서 먼저 수정");
    expect(messageOf(apiError("VERSION_CONFLICT", 409))).not.toContain("서버 문구");
    expect(messageOf(new NetworkError(null))).toContain("연결을 확인");
  });

  it("필드 오류는 details의 field와 reason에서 만들고 빈 field는 뺀다", () => {
    const error = apiError("INVALID_REQUEST", 400, [
      { field: "name", reason: "이름은 1자 이상" },
      { field: "name", reason: "두 번째 이유는 무시" },
      { field: "", reason: "본문 전체" },
      { field: "totalCount", reason: "0 이상" },
    ]);
    expect(fieldErrorsOf(error)).toEqual({ name: "이름은 1자 이상", totalCount: "0 이상" });
    expect(fieldErrorsOf(new NetworkError(null))).toEqual({});
  });
});
