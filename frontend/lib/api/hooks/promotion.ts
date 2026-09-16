"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../client";
import { keys } from "../keys";
import type { ApplicablePromotions, CreatePromotionBody, Page, PageParams, Promotion, StayParams, UpdatePromotionBody } from "../types";

// 프로모션 훅 다섯. PROMO-01부터 05(인계 문서 63행부터 65행)

// PROMO-01
export function useCreatePromotion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreatePromotionBody) => api<Promotion>("/promotions", { method: "POST", body }),
    onSuccess: (result) => {
      qc.setQueryData(keys.promotion(result.data.id), result.data);
      void qc.invalidateQueries({ queryKey: keys.promotionsAll() });
    },
  });
}

// PROMO-02. 사용 끄기는 enabled false와 version만(P11)
export function useUpdatePromotion(promotionId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdatePromotionBody) => api<Promotion>(`/promotions/${promotionId}`, { method: "PATCH", body }),
    onSuccess: (result) => {
      qc.setQueryData(keys.promotion(promotionId), result.data);
      void qc.invalidateQueries({ queryKey: keys.promotionsAll() });
    },
  });
}

// PROMO-03
export function usePromotion(promotionId: string | undefined) {
  return useQuery({
    queryKey: keys.promotion(promotionId ?? ""),
    queryFn: async () => (await api<Promotion>(`/promotions/${promotionId}`)).data,
    enabled: !!promotionId,
  });
}

// PROMO-04
export function usePromotions(params: { enabled?: boolean } & PageParams = {}) {
  return useQuery({
    queryKey: keys.promotions(params),
    queryFn: async () => (await api<Page<Promotion>>("/promotions", { query: { ...params } })).data,
  });
}

export function stayParamsComplete(params: Partial<StayParams> | null | undefined): params is StayParams {
  return !!params && !!params.checkIn && !!params.checkOut && typeof params.guestCount === "number" && params.guestCount > 0;
}

// PROMO-05. 조건 셋이 다 있을 때만. 선택은 서버가 한다(P04)
export function useApplicablePromotions(roomTypeId: string | undefined, params: Partial<StayParams> | null) {
  const complete = !!roomTypeId && stayParamsComplete(params);
  return useQuery({
    queryKey: keys.applicablePromotions(roomTypeId ?? "", complete ? params : { checkIn: "", checkOut: "", guestCount: 0 }),
    queryFn: async () => (await api<ApplicablePromotions>(`/room-types/${roomTypeId}/applicable-promotions`, { query: params ?? {} })).data,
    enabled: complete,
  });
}
