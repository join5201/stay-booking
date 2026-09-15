"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "../client";
import { keys } from "../keys";
import type { Availability, Page, PageParams, PriceQuote, PropertySearchResult, SearchParams, StayParams } from "../types";
import { stayParamsComplete } from "./promotion";

// 검색 훅 셋. SEARCH-01부터 03(인계 문서 66행부터 67행)

export function searchParamsComplete(params: Partial<SearchParams> | null | undefined): params is SearchParams {
  return stayParamsComplete(params) && !!(params as Partial<SearchParams>).regionCode;
}

const EMPTY_SEARCH: SearchParams = { regionCode: "", checkIn: "", checkOut: "", guestCount: 0 };
const EMPTY_STAY: StayParams = { checkIn: "", checkOut: "", guestCount: 0 };

// SEARCH-01. 쿼리 넷이 다 있을 때만. staleTime 30초, Page 응답
export function useSearch(params: Partial<SearchParams> | null, page: PageParams = {}) {
  const complete = searchParamsComplete(params);
  return useQuery({
    queryKey: keys.search({ ...(complete ? params : EMPTY_SEARCH), ...page }),
    queryFn: async () => (await api<Page<PropertySearchResult>>("/search/properties", { query: { ...params, ...page } })).data,
    enabled: complete,
    staleTime: 30_000,
  });
}

// SEARCH-02. staleTime 0
export function useAvailability(roomTypeId: string | undefined, params: Partial<StayParams> | null) {
  const complete = !!roomTypeId && stayParamsComplete(params);
  return useQuery({
    queryKey: keys.availability(roomTypeId ?? "", complete ? params : EMPTY_STAY),
    queryFn: async () => (await api<Availability>(`/room-types/${roomTypeId}/availability`, { query: params ?? {} })).data,
    enabled: complete,
    staleTime: 0,
  });
}

// SEARCH-03. staleTime 0. G4는 진입할 때마다 다시 받아 그 값을 expectedTotalAmount로(P06)
export function useQuote(roomTypeId: string | undefined, params: Partial<StayParams> | null, options: { always?: boolean } = {}) {
  const complete = !!roomTypeId && stayParamsComplete(params);
  return useQuery({
    queryKey: keys.quote(roomTypeId ?? "", complete ? params : EMPTY_STAY),
    queryFn: async () => (await api<PriceQuote>(`/room-types/${roomTypeId}/price-quote`, { query: params ?? {} })).data,
    enabled: complete,
    staleTime: 0,
    refetchOnMount: options.always ? "always" : true,
  });
}
