"use client";

import { controlClass } from "./Field";

export interface NumberStepperProps {
  id?: string;
  value: number | null;
  onChange: (value: number | null) => void;
  min?: number;
  max?: number;
  step?: number;
  invalid?: boolean;
  disabled?: boolean;
  // 값 뒤에 붙는 단위. 명, 박, 개
  unit?: string;
}

function clamp(v: number, min?: number, max?: number): number {
  if (min !== undefined && v < min) return min;
  if (max !== undefined && v > max) return max;
  return v;
}

// 정수 입력과 감소 증가 버튼. 빈 칸은 null. 범위 밖 입력은 그대로 두고 화면 검사가 문구를 낸다
export function NumberStepper({ id, value, onChange, min, max, step = 1, invalid, disabled, unit }: NumberStepperProps) {
  const base = value ?? min ?? 0;
  const dec = () => onChange(clamp(base - step, min, max));
  const inc = () => onChange(clamp(base + step, min, max));
  return (
    <div className="flex items-center gap-1">
      <button type="button" onClick={dec} disabled={disabled || (min !== undefined && base <= min)} aria-label="줄이기" className="h-(--h-input) w-9 rounded-control border border-line-strong bg-surface text-ink-2 disabled:text-ink-4">
        -
      </button>
      <input
        id={id}
        type="number"
        inputMode="numeric"
        value={value ?? ""}
        min={min}
        max={max}
        step={step}
        disabled={disabled}
        aria-invalid={invalid || undefined}
        onChange={(e) => onChange(e.target.value === "" ? null : Number(e.target.value))}
        className={controlClass(invalid, "w-24 text-center tabular-nums")}
      />
      <button type="button" onClick={inc} disabled={disabled || (max !== undefined && base >= max)} aria-label="늘리기" className="h-(--h-input) w-9 rounded-control border border-line-strong bg-surface text-ink-2 disabled:text-ink-4">
        +
      </button>
      {unit ? <span className="ml-1 text-13 text-ink-3">{unit}</span> : null}
    </div>
  );
}
