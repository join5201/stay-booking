"use client";

import { useState, type FormEvent } from "react";
import type { RegionCode } from "@/lib/regions";
import { Button } from "./Button";
import { DateInput } from "./DateInput";
import { Field } from "./Field";
import { NumberStepper } from "./NumberStepper";
import { RegionCodeInput } from "./RegionCodeInput";

// G1의 검색 조건 넷. 검사(날짜 순서, 30박, 인원 1부터 100, 지역)는 T7의 화면 검사가 errors로 준다(W08)
export interface SearchValues {
  regionCode: RegionCode | "";
  checkIn: string;
  checkOut: string;
  guestCount: number | null;
}

export type SearchErrors = Partial<Record<keyof SearchValues, string>>;

export interface SearchFormProps {
  initial: SearchValues;
  // 제출 전 검사. 오류가 있으면 돌려주고 제출하지 않는다
  validate?: (values: SearchValues) => SearchErrors;
  onSubmit: (values: SearchValues) => void;
  submitting?: boolean;
}

export function SearchForm({ initial, validate, onSubmit, submitting }: SearchFormProps) {
  const [values, setValues] = useState<SearchValues>(initial);
  const [errors, setErrors] = useState<SearchErrors>({});

  const set = <K extends keyof SearchValues>(key: K, value: SearchValues[K]) => setValues((v) => ({ ...v, [key]: value }));

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const found = validate ? validate(values) : {};
    setErrors(found);
    if (Object.keys(found).length === 0) onSubmit(values);
  };

  return (
    <form onSubmit={submit} className="grid grid-cols-[1fr_1fr_1fr_auto_auto] items-end gap-3 rounded-card border border-line bg-surface p-4">
      <Field label="지역" htmlFor="search-region" error={errors.regionCode}>
        <RegionCodeInput id="search-region" value={values.regionCode} onChange={(v) => set("regionCode", v)} invalid={!!errors.regionCode} />
      </Field>
      <Field label="체크인" htmlFor="search-checkin" error={errors.checkIn}>
        <DateInput id="search-checkin" value={values.checkIn} onChange={(v) => set("checkIn", v)} invalid={!!errors.checkIn} />
      </Field>
      <Field label="체크아웃" htmlFor="search-checkout" error={errors.checkOut}>
        <DateInput id="search-checkout" value={values.checkOut} onChange={(v) => set("checkOut", v)} invalid={!!errors.checkOut} />
      </Field>
      <Field label="인원" htmlFor="search-guests" error={errors.guestCount}>
        <NumberStepper id="search-guests" value={values.guestCount} onChange={(v) => set("guestCount", v)} min={1} max={100} unit="명" invalid={!!errors.guestCount} />
      </Field>
      <Button type="submit" size="lg" loading={submitting}>
        검색
      </Button>
    </form>
  );
}
