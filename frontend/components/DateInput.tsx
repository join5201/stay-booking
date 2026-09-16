"use client";

import { controlClass } from "./Field";

export interface DateInputProps {
  id?: string;
  // YYYY-MM-DD 서울 날짜. 빈 문자열은 미입력
  value: string;
  onChange: (value: string) => void;
  min?: string;
  max?: string;
  invalid?: boolean;
  disabled?: boolean;
  className?: string;
}

export function DateInput({ id, value, onChange, min, max, invalid, disabled, className = "" }: DateInputProps) {
  return (
    <input
      id={id}
      type="date"
      value={value}
      min={min}
      max={max}
      disabled={disabled}
      aria-invalid={invalid || undefined}
      onChange={(e) => onChange(e.target.value)}
      className={controlClass(invalid, `tabular-nums ${className}`)}
    />
  );
}
