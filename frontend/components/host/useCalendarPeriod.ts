"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useMemo } from "react";
import { periodFromParams, todaySeoul, type Period } from "@/lib/calendar";

// H4와 H5가 공유하는 기간. URL의 ?from&to(양끝 포함)가 정본이라 탭을 오가도 같은 기간이 유지된다(인계 문서 113행)
export function useCalendarPeriod() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  const period = useMemo(() => periodFromParams(params, todaySeoul()), [params]);
  const query = `?from=${period.from}&to=${period.to}`;

  const setPeriod = useCallback(
    (next: Period) => {
      router.replace(`${pathname}?from=${next.from}&to=${next.to}`);
    },
    [router, pathname],
  );

  return { period, query, setPeriod };
}
