import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterAll, afterEach, beforeAll, beforeEach } from "vitest";
import type { ReactNode } from "react";
import { DEV_ACTOR_COOKIE } from "@/lib/dev-actor";
import { serializeCookie } from "@/lib/cookies";
import { resetServerClock } from "../server-clock";
import { clearRequestLog } from "./handlers";
import { server } from "./server";

// 훅 테스트 공통. MSW 서버 수명과 요청 기록과 쿠키와 서버 시각을 테스트마다 비운다
export function setupMockServer(): void {
  beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
  beforeEach(() => {
    clearRequestLog();
    resetServerClock();
    clearCookies();
  });
  afterEach(() => server.resetHandlers());
  afterAll(() => server.close());
}

export function setActorCookie(actor: string): void {
  document.cookie = serializeCookie(DEV_ACTOR_COOKIE, actor);
}

export function clearCookies(): void {
  for (const part of document.cookie.split(";")) {
    const name = part.trim().split("=")[0];
    if (name) document.cookie = `${name}=; Path=/; Max-Age=0`;
  }
}

// 테스트마다 새 QueryClient. 재시도 없음. gcTime 0이면 관찰자 없는 setQueryData 결과가 다음 매크로태스크에 지워져 단언이 흔들린다
export function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } },
  });
}

export function queryWrapper(client: QueryClient = createTestQueryClient()) {
  const Wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>;
  return { Wrapper, client };
}
