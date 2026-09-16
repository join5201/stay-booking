"use client";

import type { ReactNode } from "react";
import { Button } from "./Button";

export interface ConfirmSheetProps {
  open: boolean;
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  busy?: boolean;
  // 확인을 막을 때. 취소 사유 길이 초과 등
  confirmDisabled?: boolean;
  // 취소 사유 입력 같은 추가 칸
  children?: ReactNode;
}

// 되돌리기 어려운 동작 앞의 확인. G7 취소, O2 사용 끄기와 켜기
export function ConfirmSheet({ open, title, description, confirmLabel = "확인", cancelLabel = "취소", onConfirm, onCancel, busy, confirmDisabled, children }: ConfirmSheetProps) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-ink/30" onClick={busy ? undefined : onCancel} aria-hidden />
      <div role="dialog" aria-modal="true" aria-labelledby="confirm-sheet-title" className="relative w-[480px] rounded-card bg-surface p-6 shadow-dropdown">
        <h2 id="confirm-sheet-title" className="text-18 font-semibold">
          {title}
        </h2>
        {description ? <p className="mt-2 text-14 text-ink-2">{description}</p> : null}
        {children ? <div className="mt-4">{children}</div> : null}
        <div className="mt-6 flex justify-end gap-2">
          <Button variant="outline" onClick={onCancel} disabled={busy}>
            {cancelLabel}
          </Button>
          <Button onClick={onConfirm} loading={busy} disabled={confirmDisabled}>
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}
