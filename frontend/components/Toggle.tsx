"use client";

export interface ToggleProps {
  id?: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
  disabled?: boolean;
}

// 켜짐 꺼짐 스위치. 상태색은 파랑 하나
export function Toggle({ id, checked, onChange, label, disabled }: ToggleProps) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-2 text-14">
      <button
        id={id}
        type="button"
        role="switch"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={`relative h-5 w-9 rounded-full transition-colors disabled:opacity-50 ${checked ? "bg-devbar" : "bg-line-strong"}`}
      >
        <span className={`absolute top-0.5 size-4 rounded-full bg-surface transition-all ${checked ? "left-4.5" : "left-0.5"}`} />
      </button>
      {label ? <span>{label}</span> : null}
    </label>
  );
}
