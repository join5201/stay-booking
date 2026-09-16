"use client";

import { useState } from "react";
import type { CreatePromotionBody, Promotion, UpdatePromotionBody } from "@/lib/api/types";
import { daysBetween, isYmd } from "@/lib/dates";
import { hasErrors, integerError, textError, type FieldErrors } from "@/lib/forms";
import { regionLabel, type RegionCode } from "@/lib/regions";
import { Button } from "../Button";
import { ChipsInput } from "../ChipsInput";
import { DateInput } from "../DateInput";
import { Field } from "../Field";
import { Notice } from "../Notice";
import { NumberStepper } from "../NumberStepper";
import { RegionCodeInput } from "../RegionCodeInput";
import { TextInput } from "../TextInput";
import { VersionConflictNotice } from "../VersionConflictNotice";

// O2 프로모션 등록과 수정 폼. 화면 검사가 서버보다 먼저(W08의 O2 몫). 수정은 version을 숨겨 두고 바뀐 필드만(인계 문서 111행).
// 제약은 backend CreatePromotionRequest와 document/11 PROMO-01 필드표. 이름 100, 할인율 1부터 99, 최소 박수 1부터 30,
// 캠페인 끝과 숙박 끝은 끝 날짜 제외, 숙박 기간은 둘 다 또는 둘 다 아님, 지역 코드 최대 100(빈 배열은 전체)

export interface PromotionValues {
  name: string;
  discountRate: number | null;
  campaignStartDate: string;
  campaignEndDate: string;
  // 빈 문자열은 제한 없음. 둘 다 넣거나 둘 다 비운다
  stayStartDate: string;
  stayEndDate: string;
  minNights: number | null;
  regionCodes: string[];
}

export type PromotionFieldErrors = FieldErrors<keyof PromotionValues>;

export const DISCOUNT_RULE = { label: "할인율", min: 1, max: 99 };
export const MIN_NIGHTS_RULE = { label: "최소 박수", min: 1, max: 30 };
export const REGION_CODES_MAX = 100;

export function promotionValuesOf(promotion: Promotion | null): PromotionValues {
  if (!promotion) return { name: "", discountRate: 10, campaignStartDate: "", campaignEndDate: "", stayStartDate: "", stayEndDate: "", minNights: 1, regionCodes: [] };
  return {
    name: promotion.name,
    discountRate: promotion.discountRate,
    campaignStartDate: promotion.campaignStartDate,
    campaignEndDate: promotion.campaignEndDate,
    stayStartDate: promotion.stayStartDate ?? "",
    stayEndDate: promotion.stayEndDate ?? "",
    minNights: promotion.minNights,
    regionCodes: [...promotion.regionCodes],
  };
}

function periodError(startLabel: string, endLabel: string, start: string, end: string): { start?: string; end?: string } {
  const out: { start?: string; end?: string } = {};
  if (!isYmd(start)) out.start = `${startLabel}을(를) 입력하세요.`;
  if (!isYmd(end)) out.end = `${endLabel}을(를) 입력하세요.`;
  if (!out.start && !out.end && daysBetween(start, end) <= 0) out.end = `${endLabel}은(는) ${startLabel}보다 뒤여야 합니다. 끝 날짜는 제외됩니다.`;
  return out;
}

export function validatePromotion(v: PromotionValues): PromotionFieldErrors {
  const campaign = periodError("캠페인 시작", "캠페인 끝", v.campaignStartDate, v.campaignEndDate);
  const errors: PromotionFieldErrors = {
    name: textError(v.name, { label: "이름", required: true, max: 100 }),
    discountRate: integerError(v.discountRate, DISCOUNT_RULE),
    campaignStartDate: campaign.start,
    campaignEndDate: campaign.end,
    minNights: integerError(v.minNights, MIN_NIGHTS_RULE),
    regionCodes: v.regionCodes.length > REGION_CODES_MAX ? `지역은 ${REGION_CODES_MAX}개 이하입니다.` : undefined,
  };
  const hasStart = v.stayStartDate !== "";
  const hasEnd = v.stayEndDate !== "";
  if (hasStart !== hasEnd) {
    // 둘 다 또는 둘 다 아님(PROMO-01 필드표)
    errors.stayStartDate = "숙박 기간은 시작과 끝을 둘 다 넣거나 둘 다 비웁니다.";
  } else if (hasStart) {
    const stay = periodError("숙박 시작", "숙박 끝", v.stayStartDate, v.stayEndDate);
    errors.stayStartDate = stay.start;
    errors.stayEndDate = stay.end;
  }
  return errors;
}

export function sameCodes(a: readonly string[], b: readonly string[]): boolean {
  if (a.length !== b.length) return false;
  const sorted = [...b].sort();
  return [...a].sort().every((v, i) => v === sorted[i]);
}

// 새로 읽기 뒤 합치기. 내가 안 건드린 필드만 새 기준을 따른다(H2와 같은 규칙. 지역 코드는 배열이라 값으로 비교)
export function mergePromotionValues(current: PromotionValues, oldBaseline: PromotionValues, newBaseline: PromotionValues): PromotionValues {
  const out = { ...current };
  for (const key of Object.keys(current) as (keyof PromotionValues)[]) {
    if (key === "regionCodes") {
      if (sameCodes(current.regionCodes, oldBaseline.regionCodes)) out.regionCodes = [...newBaseline.regionCodes];
    } else if (current[key] === oldBaseline[key]) {
      (out as Record<string, unknown>)[key] = newBaseline[key];
    }
  }
  return out;
}

export type PromotionSubmit = { kind: "create"; body: CreatePromotionBody } | { kind: "update"; body: UpdatePromotionBody };

// 검사를 통과한 값으로 본문을 만든다. 수정은 version과 바뀐 필드만이고 숙박 기간은 바뀌면 두 날짜를 함께(해제는 둘 다 null. PROMO-02 처리 규칙)
export function promotionBodyOf(values: PromotionValues, baseline: Promotion | null): PromotionSubmit | null {
  const name = values.name.trim();
  const discountRate = values.discountRate as number;
  const minNights = values.minNights as number;
  if (!baseline) {
    const body: CreatePromotionBody = { name, discountRate, campaignStartDate: values.campaignStartDate, campaignEndDate: values.campaignEndDate, minNights, regionCodes: values.regionCodes };
    if (values.stayStartDate) {
      body.stayStartDate = values.stayStartDate;
      body.stayEndDate = values.stayEndDate;
    }
    return { kind: "create", body };
  }
  const body: UpdatePromotionBody = { version: baseline.version };
  if (name !== baseline.name) body.name = name;
  if (discountRate !== baseline.discountRate) body.discountRate = discountRate;
  if (values.campaignStartDate !== baseline.campaignStartDate) body.campaignStartDate = values.campaignStartDate;
  if (values.campaignEndDate !== baseline.campaignEndDate) body.campaignEndDate = values.campaignEndDate;
  if (values.stayStartDate !== (baseline.stayStartDate ?? "") || values.stayEndDate !== (baseline.stayEndDate ?? "")) {
    body.stayStartDate = values.stayStartDate || null;
    body.stayEndDate = values.stayEndDate || null;
  }
  if (minNights !== baseline.minNights) body.minNights = minNights;
  if (!sameCodes(values.regionCodes, baseline.regionCodes)) body.regionCodes = values.regionCodes;
  if (Object.keys(body).length === 1) return null;
  return { kind: "update", body };
}

export interface PromotionFormProps {
  baseline: Promotion | null;
  saving: boolean;
  serverErrors: Record<string, string>;
  notice: string | null;
  conflict: boolean;
  onReload?: () => void;
  reloading?: boolean;
  onSubmit: (out: PromotionSubmit) => void;
  onCancel: () => void;
}

export function PromotionForm({ baseline, saving, serverErrors, notice, conflict, onReload, reloading, onSubmit, onCancel }: PromotionFormProps) {
  const [values, setValues] = useState<PromotionValues>(() => promotionValuesOf(baseline));
  const [errors, setErrors] = useState<PromotionFieldErrors>({});
  const [unchanged, setUnchanged] = useState(false);
  const [regionPick, setRegionPick] = useState<RegionCode | "">("");

  // 새로 읽기(또는 머리의 사용 끄기 켜기)로 기준이 바뀌면 렌더 중에 입력값을 합친다
  const [seenBaseline, setSeenBaseline] = useState(baseline);
  if (baseline !== seenBaseline) {
    setSeenBaseline(baseline);
    setValues((v) => mergePromotionValues(v, promotionValuesOf(seenBaseline), promotionValuesOf(baseline)));
  }

  const set = <K extends keyof PromotionValues>(key: K, value: PromotionValues[K]) => {
    setValues((v) => ({ ...v, [key]: value }));
    setErrors((e) => ({ ...e, [key]: undefined }));
    setUnchanged(false);
  };

  const errorOf = (key: keyof PromotionValues) => errors[key] ?? serverErrors[key];

  const addRegion = (code: RegionCode | "") => {
    setRegionPick("");
    if (!code || values.regionCodes.includes(code)) return;
    set("regionCodes", [...values.regionCodes, code]);
  };

  const submit = () => {
    const next = validatePromotion(values);
    setErrors(next);
    if (hasErrors(next)) return;
    const out = promotionBodyOf(values, baseline);
    if (!out) {
      setUnchanged(true);
      return;
    }
    onSubmit(out);
  };

  return (
    <form
      id="promotion-form"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
      className="flex w-[720px] flex-col gap-5"
    >
      {conflict && onReload ? <VersionConflictNotice onReload={onReload} busy={reloading} /> : null}
      {notice ? <Notice>{notice}</Notice> : null}
      {unchanged ? <Notice>바뀐 내용이 없습니다.</Notice> : null}

      <Field label="이름" htmlFor="promotion-name" required error={errorOf("name")} hint="100자 이하">
        <TextInput id="promotion-name" value={values.name} onValueChange={(v) => set("name", v)} invalid={!!errorOf("name")} maxLength={100} autoComplete="off" />
      </Field>

      <div className="flex gap-6">
        <Field label="할인율" htmlFor="promotion-discount-rate" required error={errorOf("discountRate")} hint="1부터 99. 백분율">
          <NumberStepper id="promotion-discount-rate" value={values.discountRate} onChange={(v) => set("discountRate", v)} min={DISCOUNT_RULE.min} max={DISCOUNT_RULE.max} unit="%" invalid={!!errorOf("discountRate")} />
        </Field>
        <Field label="최소 박수" htmlFor="promotion-min-nights" required error={errorOf("minNights")} hint="1부터 30">
          <NumberStepper id="promotion-min-nights" value={values.minNights} onChange={(v) => set("minNights", v)} min={MIN_NIGHTS_RULE.min} max={MIN_NIGHTS_RULE.max} unit="박" invalid={!!errorOf("minNights")} />
        </Field>
      </div>

      <div className="flex gap-3">
        <Field label="캠페인 시작" htmlFor="promotion-campaign-start" required error={errorOf("campaignStartDate")} hint="적용 판단일. 포함">
          <DateInput id="promotion-campaign-start" value={values.campaignStartDate} onChange={(v) => set("campaignStartDate", v)} invalid={!!errorOf("campaignStartDate")} className="w-44" />
        </Field>
        <Field label="캠페인 끝" htmlFor="promotion-campaign-end" required error={errorOf("campaignEndDate")} hint="끝 날짜는 제외">
          <DateInput id="promotion-campaign-end" value={values.campaignEndDate} onChange={(v) => set("campaignEndDate", v)} invalid={!!errorOf("campaignEndDate")} className="w-44" />
        </Field>
      </div>

      <div className="flex gap-3">
        <Field label="숙박 시작" htmlFor="promotion-stay-start" error={errorOf("stayStartDate")} hint="비우면 숙박일 제한 없음. 둘 다 넣거나 둘 다 비움">
          <DateInput id="promotion-stay-start" value={values.stayStartDate} onChange={(v) => set("stayStartDate", v)} invalid={!!errorOf("stayStartDate")} className="w-44" />
        </Field>
        <Field label="숙박 끝" htmlFor="promotion-stay-end" error={errorOf("stayEndDate")} hint="끝 날짜는 제외">
          <DateInput id="promotion-stay-end" value={values.stayEndDate} onChange={(v) => set("stayEndDate", v)} invalid={!!errorOf("stayEndDate")} className="w-44" />
        </Field>
      </div>

      <Field label="지역" htmlFor="promotion-region" error={errorOf("regionCodes")} hint="비우면 전체 지역">
        <ChipsInput values={values.regionCodes} onChange={(v) => set("regionCodes", v)} renderLabel={regionLabel} max={REGION_CODES_MAX} emptyText="전체 지역">
          <RegionCodeInput id="promotion-region" value={regionPick} onChange={addRegion} placeholder="지역 추가" exclude={values.regionCodes} />
        </ChipsInput>
      </Field>

      <div className="flex justify-end gap-2 border-t border-line pt-4">
        <Button variant="outline" onClick={onCancel} disabled={saving}>
          취소
        </Button>
        <Button type="submit" loading={saving} disabled={conflict}>
          {baseline ? "저장" : "등록"}
        </Button>
      </div>
    </form>
  );
}
