import { describe, expect, it } from "vitest";
import nextConfig, { apiRewrites, backendUrl } from "./next.config";

describe("backendUrl", () => {
  it("BACKEND_URL이 없으면 로컬 8080", () => {
    expect(backendUrl({})).toBe("http://localhost:8080");
  });

  it("빈 문자열도 없는 것으로 본다", () => {
    expect(backendUrl({ BACKEND_URL: "  " })).toBe("http://localhost:8080");
  });

  it("설정값을 쓰고 끝 슬래시를 뗀다", () => {
    expect(backendUrl({ BACKEND_URL: "http://127.0.0.1:9090/" })).toBe("http://127.0.0.1:9090");
  });
});

describe("apiRewrites", () => {
  it("/api/v1 아래 전부를 백엔드의 같은 경로로 넘긴다", () => {
    expect(apiRewrites("http://localhost:8080")).toEqual([
      { source: "/api/v1/:path*", destination: "http://localhost:8080/api/v1/:path*" },
    ]);
  });

  it("next.config의 rewrites가 그 규칙을 낸다", async () => {
    const rules = await nextConfig.rewrites?.();
    expect(rules).toEqual(apiRewrites(backendUrl()));
  });
});
