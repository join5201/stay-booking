"use client";

import { Button } from "./Button";

export interface PaginationProps {
  // 0부터. Page 응답의 page와 totalPages 그대로
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
  disabled?: boolean;
}

export function Pagination({ page, totalPages, onChange, disabled }: PaginationProps) {
  const last = Math.max(0, totalPages - 1);
  return (
    <nav aria-label="페이지" className="flex items-center justify-center gap-3">
      <Button variant="outline" onClick={() => onChange(page - 1)} disabled={disabled || page <= 0}>
        이전
      </Button>
      <span className="text-13 text-ink-2 tabular-nums">
        {totalPages === 0 ? "0 / 0" : `${page + 1} / ${totalPages}`}
      </span>
      <Button variant="outline" onClick={() => onChange(page + 1)} disabled={disabled || page >= last}>
        다음
      </Button>
    </nav>
  );
}
