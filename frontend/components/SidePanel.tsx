"use client";

import { useEffect, type ReactNode } from "react";

export interface SidePanelProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  // 저장 취소 버튼 줄
  footer?: ReactNode;
}

// 오른쪽에서 여는 패널. H3의 등록과 수정(?new, ?edit)
export function SidePanel({ open, title, onClose, children, footer }: SidePanelProps) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-40">
      <div className="absolute inset-0 bg-ink/30" onClick={onClose} aria-hidden />
      <aside role="dialog" aria-modal="true" aria-label={title} className="absolute inset-y-0 right-0 flex w-[480px] flex-col bg-surface shadow-dropdown">
        <header className="flex items-center justify-between border-b border-line px-6 py-4">
          <h2 className="text-18 font-semibold">{title}</h2>
          <button type="button" onClick={onClose} aria-label="닫기" className="text-ink-3 hover:text-ink">
            x
          </button>
        </header>
        <div className="flex-1 overflow-y-auto px-6 py-5">{children}</div>
        {footer ? <footer className="flex justify-end gap-2 border-t border-line px-6 py-4">{footer}</footer> : null}
      </aside>
    </div>
  );
}
