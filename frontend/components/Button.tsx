"use client";

import type { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "outline" | "ghost";
type Size = "md" | "lg";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
}

const VARIANT: Record<Variant, string> = {
  primary: "bg-devbar text-white hover:bg-devbar-2 disabled:bg-ink-4",
  outline: "border border-line-strong bg-surface text-ink hover:bg-surface-2 disabled:text-ink-4",
  ghost: "bg-transparent text-ink-2 hover:bg-line-soft disabled:text-ink-4",
};

const SIZE: Record<Size, string> = {
  md: "h-(--h-button) px-4 text-14",
  lg: "h-(--h-button-lg) px-5 text-15",
};

// 높이 38 또는 42, 모서리 4. loading이면 비활성이고 처리 중 표시
export function Button({ variant = "primary", size = "md", loading = false, disabled, className = "", children, type = "button", ...rest }: ButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={`inline-flex items-center justify-center gap-2 rounded-control font-semibold whitespace-nowrap disabled:cursor-not-allowed ${VARIANT[variant]} ${SIZE[size]} ${className}`}
      {...rest}
    >
      {loading ? <span aria-hidden className="inline-block size-3 animate-spin rounded-full border-2 border-current border-t-transparent" /> : null}
      {children}
    </button>
  );
}
