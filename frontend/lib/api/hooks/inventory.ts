"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../client";
import { keys } from "../keys";
import type { AdjustInventoryBody, AdjustRateBody, BulkInventoryBody, DailyInventory, DailyRate, InventoryRange, RateRange, RegisterInventoryBody, RegisterRateBody } from "../types";

// 재고 훅 다섯과 요금 훅 넷. INV-01부터 05, RATE-01부터 04(인계 문서 58행부터 62행)

function invalidateInventories(qc: ReturnType<typeof useQueryClient>, roomTypeId: string) {
  void qc.invalidateQueries({ queryKey: keys.inventoriesAll(roomTypeId) });
  void qc.invalidateQueries({ queryKey: keys.inventoryAll(roomTypeId) });
}

function invalidateRates(qc: ReturnType<typeof useQueryClient>, roomTypeId: string) {
  void qc.invalidateQueries({ queryKey: keys.ratesAll(roomTypeId) });
  void qc.invalidateQueries({ queryKey: keys.rateAll(roomTypeId) });
}

// INV-01
export function useCreateInventory(roomTypeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: RegisterInventoryBody) => api<DailyInventory>(`/room-types/${roomTypeId}/inventories`, { method: "POST", body }),
    onSuccess: () => invalidateInventories(qc, roomTypeId),
  });
}

// INV-02. 한 날짜라도 있으면 전부 409. 겹침은 보내기 전에 막고 409면 INV-04를 다시 읽는다(계약 2-1절 2행)
export function useBulkCreateInventory(roomTypeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: BulkInventoryBody) => api<InventoryRange>(`/room-types/${roomTypeId}/inventories/bulk`, { method: "POST", body }),
    onSettled: () => invalidateInventories(qc, roomTypeId),
  });
}

// INV-03
export function useUpdateInventory(roomTypeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ date, ...body }: AdjustInventoryBody & { date: string }) => api<DailyInventory>(`/room-types/${roomTypeId}/inventories/${date}`, { method: "PATCH", body }),
    onSuccess: () => invalidateInventories(qc, roomTypeId),
  });
}

// INV-04. 최대 366일
export function useInventories(roomTypeId: string | undefined, from: string, to: string) {
  return useQuery({
    queryKey: keys.inventories(roomTypeId ?? "", from, to),
    queryFn: async () => (await api<InventoryRange>(`/room-types/${roomTypeId}/inventories`, { query: { from, to } })).data,
    enabled: !!roomTypeId && !!from && !!to,
  });
}

// INV-05. 셀을 열 때만, staleTime 0(version 확보). BELOW_COMMITTED 문구의 숫자도 여기서
export function useInventory(roomTypeId: string | undefined, date: string | null) {
  return useQuery({
    queryKey: keys.inventory(roomTypeId ?? "", date ?? ""),
    queryFn: async () => (await api<DailyInventory>(`/room-types/${roomTypeId}/inventories/${date}`)).data,
    enabled: !!roomTypeId && !!date,
    staleTime: 0,
  });
}

// RATE-01. currency KRW를 보낸다
export function useCreateRate(roomTypeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Omit<RegisterRateBody, "currency">) => api<DailyRate>(`/room-types/${roomTypeId}/rates`, { method: "POST", body: { ...body, currency: "KRW" } satisfies RegisterRateBody }),
    onSuccess: () => invalidateRates(qc, roomTypeId),
  });
}

// RATE-02. version과 amount만. currency를 보내면 400(계약 2-1절 3행, W12)
export function useUpdateRate(roomTypeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ date, version, amount }: AdjustRateBody & { date: string }) => api<DailyRate>(`/room-types/${roomTypeId}/rates/${date}`, { method: "PATCH", body: { version, amount } satisfies AdjustRateBody }),
    onSuccess: () => invalidateRates(qc, roomTypeId),
  });
}

// RATE-03
export function useRates(roomTypeId: string | undefined, from: string, to: string) {
  return useQuery({
    queryKey: keys.rates(roomTypeId ?? "", from, to),
    queryFn: async () => (await api<RateRange>(`/room-types/${roomTypeId}/rates`, { query: { from, to } })).data,
    enabled: !!roomTypeId && !!from && !!to,
  });
}

// RATE-04
export function useRate(roomTypeId: string | undefined, date: string | null) {
  return useQuery({
    queryKey: keys.rate(roomTypeId ?? "", date ?? ""),
    queryFn: async () => (await api<DailyRate>(`/room-types/${roomTypeId}/rates/${date}`)).data,
    enabled: !!roomTypeId && !!date,
    staleTime: 0,
  });
}
