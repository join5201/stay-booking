// @vitest-environment node
import { NextRequest } from "next/server";
import { describe, expect, it } from "vitest";
import { config, proxy } from "./proxy";

// W10의 배선 몫. proxy가 쿠키를 읽어 redirect 응답을 낸다
function request(pathname: string, actor?: string): NextRequest {
  return new NextRequest(`http://localhost:3000${pathname}`, {
    headers: actor ? { cookie: `dev_actor=${actor}` } : {},
  });
}

describe("proxy (W10 배선)", () => {
  it("어긋나면 307과 역할 첫 화면", () => {
    const res = proxy(request("/bookings", "host_001"));
    expect(res.status).toBe(307);
    expect(res.headers.get("location")).toBe("http://localhost:3000/host/properties");
  });

  it("쿠키가 없으면 public이라 내 예약은 검색으로", () => {
    const res = proxy(request("/bookings/bk_1"));
    expect(res.status).toBe(307);
    expect(res.headers.get("location")).toBe("http://localhost:3000/");
  });

  it("맞으면 통과", () => {
    const res = proxy(request("/host/properties", "host_001"));
    expect(res.status).toBe(200);
    expect(res.headers.get("location")).toBeNull();
  });

  it("matcher가 역할 경로 셋만 본다", () => {
    expect(config.matcher).toEqual(["/host/:path*", "/operator/:path*", "/bookings/:path*"]);
  });
});
