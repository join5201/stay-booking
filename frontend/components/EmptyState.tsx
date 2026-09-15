import type { ReactNode } from "react";

export interface EmptyStateProps {
  title: string;
  description?: string;
  // 버튼 등. 없으면 글자만
  action?: ReactNode;
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-card border border-dashed border-line-strong bg-surface-2 px-6 py-10 text-center">
      <p className="text-15 font-semibold text-ink-2">{title}</p>
      {description ? <p className="text-13 text-ink-3">{description}</p> : null}
      {action ? <div className="mt-2">{action}</div> : null}
    </div>
  );
}
