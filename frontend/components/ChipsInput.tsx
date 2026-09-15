"use client";

import type { ReactNode } from "react";

export interface ChipsInputProps {
  values: readonly string[];
  onChange: (values: string[]) => void;
  // 칩에 보일 글자. 기본은 값 그대로
  renderLabel?: (value: string) => string;
  max?: number;
  disabled?: boolean;
  // 값을 더하는 컨트롤. O2는 RegionCodeInput을 넣는다
  children?: ReactNode;
  emptyText?: string;
}

// 고른 값들을 칩으로 보여 주고 하나씩 뺀다. 더하는 컨트롤은 부모가 children으로 준다
export function ChipsInput({ values, onChange, renderLabel = (v) => v, max, disabled, children, emptyText = "없음" }: ChipsInputProps) {
  const remove = (v: string) => onChange(values.filter((x) => x !== v));
  return (
    <div className="flex flex-col gap-2">
      <div className="flex min-h-9 flex-wrap items-center gap-1.5">
        {values.length === 0 ? <span className="text-13 text-ink-3">{emptyText}</span> : null}
        {values.map((v) => (
          <span key={v} className="inline-flex items-center gap-1 rounded-badge border border-line bg-surface-2 px-2 py-0.5 text-13">
            {renderLabel(v)}
            <button type="button" onClick={() => remove(v)} disabled={disabled} aria-label={`${renderLabel(v)} 빼기`} className="text-ink-3 hover:text-ink">
              x
            </button>
          </span>
        ))}
      </div>
      {children}
      {max !== undefined ? (
        <p className="text-12 text-ink-3 tabular-nums">
          {values.length} / {max}
        </p>
      ) : null}
    </div>
  );
}
