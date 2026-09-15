"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { isApiError } from "@/lib/errors";

// 4xx는 다시 보내도 같은 답이라 재시도하지 않는다. 5xx와 네트워크만 한 번 더(기본값 셋은 404 전면 표시를 7초 늦췄다. T4 실측)
export function retryQuery(failureCount: number, error: unknown): boolean {
  if (isApiError(error) && error.status < 500) return false;
  return failureCount < 1;
}

// QueryClient는 브라우저마다 하나. 렌더마다 새로 만들지 않도록 state에 둔다
export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(() => new QueryClient({ defaultOptions: { queries: { retry: retryQuery } } }));
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
