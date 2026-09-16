import type { ReactNode } from "react";

export interface FieldProps {
  label: string;
  htmlFor?: string;
  hint?: string;
  // 오류 표시 첫째 자리. 필드 아래 문구
  error?: string;
  required?: boolean;
  children: ReactNode;
}

export function Field({ label, htmlFor, hint, error, required, children }: FieldProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={htmlFor} className="text-13 font-semibold text-ink-2">
        {label}
        {required ? <span className="ml-1 text-ink-4">필수</span> : null}
      </label>
      {children}
      {error ? (
        <p role="alert" className="text-12 text-ink-2">
          {error}
        </p>
      ) : hint ? (
        <p className="text-12 text-ink-3">{hint}</p>
      ) : null}
    </div>
  );
}

// 입력 상자 공통 모양. 높이 38, 모서리 4. invalid면 주황 테두리
export function controlClass(invalid?: boolean, extra = ""): string {
  const border = invalid ? "border-warn" : "border-line-strong focus:border-devbar";
  return `h-(--h-input) w-full rounded-control border bg-surface px-3 text-14 text-ink outline-none disabled:bg-surface-missing disabled:text-ink-3 ${border} ${extra}`;
}
