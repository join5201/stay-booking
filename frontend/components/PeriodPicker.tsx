"use client";

import { useState, type FormEvent } from "react";
import { daysBetween, isYmd } from "@/lib/dates";
import { Button } from "./Button";
import { DateInput } from "./DateInput";
import { Field } from "./Field";

export interface Period {
  from: string;
  to: string;
}

export interface PeriodPickerProps {
  value: Period;
  onChange: (period: Period) => void;
  // 양끝 포함 일수 상한. 재고와 요금 기간 조회는 366(P02)
  maxDays?: number;
}

// 화면 검사가 서버보다 먼저. 날짜 형식, 순서, 상한
export function validatePeriod(period: Period, maxDays: number): string | null {
  if (!isYmd(period.from) || !isYmd(period.to)) return "시작과 끝 날짜를 입력하세요.";
  if (daysBetween(period.from, period.to) < 0) return "끝 날짜가 시작 날짜보다 앞입니다.";
  if (daysBetween(period.from, period.to) + 1 > maxDays) return `기간은 최대 ${maxDays}일입니다.`;
  return null;
}

export function PeriodPicker({ value, onChange, maxDays = 366 }: PeriodPickerProps) {
  const [draft, setDraft] = useState<Period>(value);
  const [error, setError] = useState<string | null>(null);

  const apply = (e: FormEvent) => {
    e.preventDefault();
    const found = validatePeriod(draft, maxDays);
    setError(found);
    if (!found) onChange(draft);
  };

  return (
    <form onSubmit={apply} className="flex items-end gap-3">
      <Field label="시작" htmlFor="period-from" error={error ?? undefined}>
        <DateInput id="period-from" value={draft.from} onChange={(from) => setDraft((d) => ({ ...d, from }))} invalid={!!error} className="w-40" />
      </Field>
      <Field label="끝" htmlFor="period-to">
        <DateInput id="period-to" value={draft.to} onChange={(to) => setDraft((d) => ({ ...d, to }))} invalid={!!error} className="w-40" />
      </Field>
      <Button type="submit" variant="outline">
        기간 적용
      </Button>
    </form>
  );
}
