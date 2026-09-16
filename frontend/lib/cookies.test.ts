import { describe, expect, it } from "vitest";
import { readCookie, serializeCookie } from "./cookies";
import { DEV_ACTOR_COOKIE, firstScreenOf, parseDevActor, parseMockMode, roleOf } from "./dev-actor";

describe("개발용 쿠키", () => {
  it("여러 쿠키 중 이름으로 찾는다", () => {
    expect(readCookie("dev_actor", "a=1; dev_actor=host_001; dev_mock_mode=DEFER")).toBe("host_001");
    expect(readCookie("dev_mock_mode", "dev_actor=host_001; dev_mock_mode=DEFER")).toBe("DEFER");
    expect(readCookie("none", "dev_actor=host_001")).toBeUndefined();
  });

  it("Path /, 30일, SameSite Lax, httpOnly 없음(계약 6절 개발용 쿠키 행)", () => {
    expect(serializeCookie(DEV_ACTOR_COOKIE, "guest_001")).toBe("dev_actor=guest_001; Path=/; Max-Age=2592000; SameSite=Lax");
  });

  it("모르는 값은 public과 APPROVE로 본다", () => {
    expect(parseDevActor("hacker")).toBe("public");
    expect(parseDevActor(undefined)).toBe("public");
    expect(parseMockMode("MAYBE")).toBe("APPROVE");
  });

  it("행위자 ID에서 역할과 첫 화면", () => {
    expect(roleOf("guest_002")).toBe("guest");
    expect(roleOf("host_002")).toBe("host");
    expect(roleOf("operator_001")).toBe("operator");
    expect(firstScreenOf(roleOf("public"))).toBe("/");
    expect(firstScreenOf("host")).toBe("/host/properties");
    expect(firstScreenOf("operator")).toBe("/operator/promotions");
  });
});
