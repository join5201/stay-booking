"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../client";
import { keys } from "../keys";
import type { Page, PageParams, Property, RegisterPropertyBody, RegisterRoomTypeBody, RoomType, UpdatePropertyBody, UpdateRoomTypeBody } from "../types";

// 숙소와 객실 훅 아홉. CAT-01부터 CAT-09(인계 문서 51행부터 57행)

const ONE_MINUTE = 60_000;

// CAT-01
export function useCreateProperty() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: RegisterPropertyBody) => api<Property>("/properties", { method: "POST", body }),
    onSuccess: (result) => {
      qc.setQueryData(keys.property(result.data.id), result.data);
      void qc.invalidateQueries({ queryKey: keys.hostPropertiesAll() });
    },
  });
}

// CAT-02. version 숨김 보관, 바뀐 필드만
export function useUpdateProperty(propertyId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdatePropertyBody) => api<Property>(`/properties/${propertyId}`, { method: "PATCH", body }),
    onSuccess: (result) => {
      qc.setQueryData(keys.property(propertyId), result.data);
      void qc.invalidateQueries({ queryKey: keys.hostPropertiesAll() });
    },
  });
}

// CAT-03. G6과 G7의 이름 표시에도 쓴다(Q1). staleTime 60초. 수정 폼(H2)은 fresh로 진입마다 다시 읽어 version을 새로 받는다
export function useProperty(propertyId: string | undefined, options: { fresh?: boolean } = {}) {
  return useQuery({
    queryKey: keys.property(propertyId ?? ""),
    queryFn: async () => (await api<Property>(`/properties/${propertyId}`)).data,
    enabled: !!propertyId,
    staleTime: options.fresh ? 0 : ONE_MINUTE,
  });
}

// CAT-04. 훅만, 화면 미사용
export function usePublicProperties(params: { regionCode?: string } & PageParams) {
  return useQuery({
    queryKey: keys.properties(params),
    queryFn: async () => (await api<Page<Property>>("/properties", { query: { ...params } })).data,
  });
}

// CAT-05
export function useMyProperties(params: PageParams = {}) {
  return useQuery({
    queryKey: keys.hostProperties(params),
    queryFn: async () => (await api<Page<Property>>("/host/properties", { query: { ...params } })).data,
  });
}

// CAT-06
export function useCreateRoomType(propertyId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: RegisterRoomTypeBody) => api<RoomType>(`/properties/${propertyId}/room-types`, { method: "POST", body }),
    onSuccess: (result) => {
      qc.setQueryData(keys.roomType(result.data.id), result.data);
      void qc.invalidateQueries({ queryKey: keys.roomTypesAll(propertyId) });
    },
  });
}

// CAT-07
export function useUpdateRoomType(roomTypeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateRoomTypeBody) => api<RoomType>(`/room-types/${roomTypeId}`, { method: "PATCH", body }),
    onSuccess: (result) => {
      qc.setQueryData(keys.roomType(roomTypeId), result.data);
      void qc.invalidateQueries({ queryKey: keys.roomTypesAll(result.data.propertyId) });
    },
  });
}

// CAT-08. staleTime 60초. 편집 패널(H3)은 fresh로 열릴 때마다 다시 읽는다
export function useRoomType(roomTypeId: string | undefined, options: { fresh?: boolean } = {}) {
  return useQuery({
    queryKey: keys.roomType(roomTypeId ?? ""),
    queryFn: async () => (await api<RoomType>(`/room-types/${roomTypeId}`)).data,
    enabled: !!roomTypeId,
    staleTime: options.fresh ? 0 : ONE_MINUTE,
  });
}

// CAT-09. Page 응답
export function useRoomTypes(propertyId: string | undefined, params: PageParams = {}) {
  return useQuery({
    queryKey: keys.roomTypes(propertyId ?? "", params),
    queryFn: async () => (await api<Page<RoomType>>(`/properties/${propertyId}/room-types`, { query: { ...params } })).data,
    enabled: !!propertyId,
  });
}
