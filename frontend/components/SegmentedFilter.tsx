"use client";

export interface SegmentedOption<V extends string> {
  value: V;
  label: string;
}

export interface SegmentedFilterProps<V extends string> {
  options: readonly SegmentedOption<V>[];
  value: V;
  onChange: (value: V) => void;
  ariaLabel?: string;
}

// 고정 선택지 필터. G6 상태 다섯, O1 사용 여부
export function SegmentedFilter<V extends string>({ options, value, onChange, ariaLabel }: SegmentedFilterProps<V>) {
  return (
    <div role="group" aria-label={ariaLabel} className="inline-flex rounded-control border border-line-strong bg-surface p-0.5">
      {options.map((o) => {
        const active = o.value === value;
        return (
          <button
            key={o.value}
            type="button"
            aria-pressed={active}
            onClick={() => onChange(o.value)}
            className={`h-8 rounded-control px-3 text-13 ${active ? "bg-ink text-white" : "text-ink-2 hover:bg-line-soft"}`}
          >
            {o.label}
          </button>
        );
      })}
    </div>
  );
}
