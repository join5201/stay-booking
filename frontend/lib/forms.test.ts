import { describe, expect, it } from "vitest";
import { ApiError, NetworkError } from "./api/client";
import { errorViewOf } from "./error-view";
import { changedFields, integerError, mergeReloaded, textError } from "./forms";

describe("폼 검사 도우미", () => {
  it("글자 수. 필수와 한도와 공백", () => {
    expect(textError("", { label: "이름", required: true, max: 100 })).toBe("이름을(를) 입력하세요.");
    expect(textError("   ", { label: "이름", required: true, max: 100 })).toBe("이름을(를) 입력하세요.");
    expect(textError("", { label: "설명", max: 2000 })).toBeUndefined();
    expect(textError("a".repeat(2001), { label: "설명", max: 2000 })).toBe("설명은(는) 2000자 이하입니다.");
    expect(textError("a".repeat(2000), { label: "설명", max: 2000 })).toBeUndefined();
  });

  it("정수 범위. 빈 칸과 소수와 경계", () => {
    expect(integerError(null, { label: "최대 인원", min: 1, max: 100 })).toBe("최대 인원을(를) 입력하세요.");
    expect(integerError(1.5, { label: "최대 인원", min: 1, max: 100 })).toBe("최대 인원은(는) 정수입니다.");
    expect(integerError(0, { label: "최대 인원", min: 1, max: 100 })).toBe("최대 인원은(는) 1부터 100까지입니다.");
    expect(integerError(101, { label: "최대 인원", min: 1, max: 100 })).toBe("최대 인원은(는) 1부터 100까지입니다.");
    expect(integerError(1, { label: "최대 인원", min: 1, max: 100 })).toBeUndefined();
    expect(integerError(100, { label: "최대 인원", min: 1, max: 100 })).toBeUndefined();
  });

  it("바뀐 필드만", () => {
    expect(changedFields({ a: "1", b: 2, c: "x" }, { a: "1", b: 3, c: "y" })).toEqual({ b: 3, c: "y" });
    expect(changedFields({ a: "1" }, { a: "1" })).toEqual({});
  });

  it("새로 읽기 합치기. 내가 안 건드린 필드만 새 기준을 따른다", () => {
    const old = { name: "n0", address: "a0" };
    const fresh = { name: "n1", address: "a1" };
    expect(mergeReloaded({ name: "mine", address: "a0" }, old, fresh)).toEqual({ name: "mine", address: "a1" });
    expect(mergeReloaded({ name: "n0", address: "a0" }, old, fresh)).toEqual(fresh);
  });
});

describe("쓰기 실패의 모양", () => {
  const apiError = (code: string, status: number, details: { field: string; reason: string }[] = []) => new ApiError({ status, code, message: "m", details });

  it("VERSION_CONFLICT와 RESOURCE_NOT_FOUND는 따로, 나머지는 placement대로", () => {
    expect(errorViewOf(apiError("VERSION_CONFLICT", 409))).toEqual({ kind: "conflict" });
    expect(errorViewOf(apiError("RESOURCE_NOT_FOUND", 404))).toMatchObject({ kind: "notFound" });
    expect(errorViewOf(apiError("INVALID_REQUEST", 400, [{ field: "name", reason: "이름은 1자 이상" }]))).toMatchObject({ kind: "field", fields: { name: "이름은 1자 이상" } });
    expect(errorViewOf(apiError("ACCESS_DENIED", 403))).toMatchObject({ kind: "notice" });
    expect(errorViewOf(apiError("INTERNAL_ERROR", 500))).toMatchObject({ kind: "banner", retryAfterSec: null });
    expect(errorViewOf(new NetworkError(null))).toMatchObject({ kind: "banner" });
  });
});
