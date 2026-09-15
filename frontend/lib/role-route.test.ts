import { describe, expect, it } from "vitest";
import { redirectTargetFor, requiredRoleOf } from "./role-route";

// W10. 쿠키 역할과 경로 앞부분이 어긋나면 역할 첫 화면으로. 여섯 조합
describe("redirectTargetFor (W10)", () => {
  it.each([
    ["host_001", "/bookings", "/host/properties"],
    ["host_001", "/operator/promotions/new", "/host/properties"],
    ["guest_001", "/host/properties", "/"],
    ["guest_001", "/operator/promotions", "/"],
    ["operator_001", "/host/room-types/rt_1/rates", "/operator/promotions"],
    ["operator_001", "/bookings/bk_1/pay", "/operator/promotions"],
  ])("어긋남 %s 가 %s 에 오면 %s 로", (actor, pathname, target) => {
    expect(redirectTargetFor(pathname, actor)).toBe(target);
  });

  it("public은 내 예약에 못 들어가고 검색으로 간다", () => {
    expect(redirectTargetFor("/bookings", "public")).toBe("/");
    expect(redirectTargetFor("/bookings", undefined)).toBe("/");
    expect(redirectTargetFor("/host/properties", "nobody_999")).toBe("/");
  });

  it.each([
    ["host_002", "/host/properties/p_1/edit"],
    ["operator_001", "/operator/promotions"],
    ["guest_002", "/bookings"],
    ["guest_001", "/room-types/rt_1/book"],
    ["host_001", "/"],
    ["public", "/properties/p_1"],
  ])("맞음 %s 가 %s 에 오면 통과", (actor, pathname) => {
    expect(redirectTargetFor(pathname, actor)).toBeNull();
  });

  it("앞부분은 경로 조각 단위로 본다", () => {
    expect(requiredRoleOf("/hostel")).toBeNull();
    expect(requiredRoleOf("/host")).toBe("host");
    expect(requiredRoleOf("/bookingsx")).toBeNull();
  });
});
