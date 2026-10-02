import type { NextConfig } from "next";

const DEFAULT_BACKEND_URL = "http://localhost:8080";

type Env = Readonly<Record<string, string | undefined>>;

// BACKEND_URL이 비어 있으면 로컬 백엔드 기본 포트. 끝 슬래시는 뗀다
export function backendUrl(env: Env = process.env): string {
  const value = env.BACKEND_URL?.trim();
  return value ? value.replace(/\/+$/, "") : DEFAULT_BACKEND_URL;
}

// /api/v1 아래 요청을 백엔드로 넘긴다. 백엔드에 CORS 설정이 없어 같은 출처로 부른다
export function apiRewrites(base: string) {
  return [{ source: "/api/v1/:path*", destination: `${base}/api/v1/:path*` }];
}

const nextConfig: NextConfig = {
  // 이미지용. 빌드가 실행에 필요한 파일만 .next/standalone에 모은다 (frontend/Dockerfile).
  // PC의 next start는 그대로 뜨고 standalone이면 server.js를 쓰라는 경고 한 줄이 나온다
  output: "standalone",
  async rewrites() {
    return apiRewrites(backendUrl());
  },
};

export default nextConfig;
